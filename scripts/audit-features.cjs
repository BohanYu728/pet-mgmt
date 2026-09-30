// Run only with a disposable H2 demo server on 8081; real UI served by Vite on 5173.
const {chromium}=require('playwright');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/evidence/features');
const backend='http://127.0.0.1:8081',origin='http://127.0.0.1:5173';
const checks=[],shots=[],errors=[];let browser;
fs.mkdirSync(out,{recursive:true});
function check(name,ok,detail=''){checks.push({name,passed:!!ok,detail});assert.ok(ok,name+' '+detail)}
async function session(){
 const context=await browser.newContext({viewport:{width:1440,height:960},geolocation:{latitude:31.23,longitude:121.47},permissions:['geolocation']});
 // Continue the browser request so multipart uploads retain their file bytes.
 await context.route('**/api/**',route=>route.continue({url:route.request().url().replace(origin,backend)}));
 const page=await context.newPage();page.setDefaultTimeout(20000);page.on('pageerror',e=>errors.push(e.message));page.on('dialog',d=>d.accept());
 return {context,page};
}
async function api(s,method,url,data,name,multipart=false){
 const headers={};if(method!=='GET'){const r=await s.context.request.get(backend+'/api/auth/csrf');const c=await r.json();headers[c.headerName]=c.token}
 const r=await s.context.request.fetch(backend+'/api'+url,{method,headers,...(data?(multipart?{multipart:data}:{data}):{})});
 let body;try{body=await r.json()}catch{body={}}
 check(name||method+' '+url,r.ok()&&!body.error,`HTTP ${r.status()} ${body.error||''}`);return body.data??body;
}
async function login(s,name,password){await api(s,'POST','/auth/login',{username:name,password},'登录 '+name)}
async function snap(s,id,title,route,note){
 if(route){await s.page.goto(origin+route,{waitUntil:'networkidle'});await s.page.locator('main h1').first().waitFor()}
 await s.page.evaluate(async()=>{await Promise.all([...document.images].map(async i=>{i.loading='eager';try{await i.decode()}catch{}}))});
 const failures=await s.page.locator('main .message.error').allTextContents();check('页面展示 '+title,!failures.length,failures.join(';'));
 await s.page.screenshot({path:path.join(out,id+'.png'),fullPage:true});shots.push({id,title,route:route||new URL(s.page.url()).pathname,note});
}
const upload=(file='cat-lifestyle.jpg')=>({name:file,mimeType:'image/jpeg',buffer:fs.readFileSync(path.join(root,'frontend/public/images',file))});
(async()=>{
 browser=await chromium.launch({channel:'chrome',headless:true});
 const u=await session(),a=await session(),m=await session(),guest=await session();
 await snap(guest,'01-register','普通用户注册','/register','支持账号注册；下文通过接口提交新账号并登录核验。');
 const stamp=Date.now().toString().slice(-7),account='show'+stamp;
 const registered=await api(guest,'POST','/auth/register',{username:account,email:account+'@example.invalid',password:'Show123!'},'注册普通用户');
 await login(guest,account,'Show123!');await api(guest,'POST','/auth/logout',null,'新账号退出');
 await snap(u,'02-login','账号登录','/login','实际使用演示账号完成登录；未登录请求和角色权限另有负向测试。');
 await u.page.getByLabel('用户名',{exact:true}).fill('demo');await u.page.getByLabel('密码',{exact:true}).fill('demo123');
 await Promise.all([u.page.waitForURL('**/dashboard'),u.page.getByRole('button',{name:'登录',exact:true}).click()]);check('浏览器登录跳转',true);
 await login(a,'admin','admin123');await login(m,'demo_shop','demoShop123');
 await api(u,'PUT','/auth/profile',{phone:'13800000000'},'保存个人资料');
 await api(u,'POST','/auth/avatar',{avatar:upload()},'上传账号头像',true);
 await api(u,'PUT','/auth/password',{old_password:'demo123',new_password:'Show456!'},'修改密码');
 await login(u,'demo','Show456!');await api(u,'PUT','/auth/password',{old_password:'Show456!',new_password:'demo123'},'恢复演示密码');
 await snap(u,'03-profile','个人资料 头像与密码','/profile','实际保存联系方式、上传头像，并以修改后的密码登录，随后恢复演示密码。');
 await api(u,'POST','/pets/1/avatar',{avatar:upload('dog-lifestyle.jpg')},'上传宠物头像',true);
 const pet=(await api(u,'POST','/pets',{name:'展示测试宠物',species:'cat',breed:'短毛猫',weight:4.1,birth_date:'2025-01-01'},'新增宠物')).pet;
 await api(u,'PUT',`/pets/${pet.id}`,{name:'展示档案已更新'},'编辑宠物');
 await snap(u,'04-pets','宠物档案与照片','/pets','展示新增、编辑后的档案及上传头像；截图后删除本次临时档案。');
 const health=(await api(u,'POST','/pets/1/health',{title:'年度体检展示',record_type:'checkup',record_date:'2026-09-29',next_due_date:'2026-10-29',description:'演示记录'},'新增健康记录')).record;
 await api(u,'PUT',`/pets/1/health/${health.id}`,{title:'年度体检复查展示'},'编辑健康记录');
 await snap(u,'05-health','健康记录','/pets/1/health','新增并修改健康记录，设置下次日期生成提醒；图中为演示资料。');
 const reminder=(await api(u,'POST','/pets/1/reminders',{title:'驱虫提醒展示',reminder_type:'deworming',reminder_date:'2026-10-01'},'新增提醒')).item;
 await api(u,'PUT',`/pets/1/reminders/${reminder.id}`,{title:'驱虫复查提醒',is_read:true},'修改并标记提醒已读');
 await snap(u,'06-reminders','照护提醒','/reminders','展示健康记录关联提醒与手动提醒；测试修改、标记已读和删除。');
 let weight;for(const [date,value] of [['2026-09-01',27.5],['2026-09-15',27.8],['2026-09-29',28.0]])weight=(await api(u,'POST','/pets/1/weights',{weight:value,record_date:date},'保存体重 '+date)).item;
 await snap(u,'07-weights','体重记录与趋势','/pets/1/weights','实际写入三次体重并展示趋势；删除接口另行验证。');
 const log=(await api(u,'POST','/pets/1/logs',{title:'今日散步',content:'演示：散步三十分钟，饮水正常。',log_date:'2026-09-29',log_type:'daily'},'新增日常日志')).item;
 await api(u,'PUT',`/pets/1/logs/${log.id}`,{content:'演示：散步三十分钟，完成喂食与饮水记录。'},'编辑日常日志');
 await snap(u,'08-logs','日常日志','/logs','新增并更新日常照护记录；图中为本次演示数据。');
 const photo=(await api(u,'POST','/pets/1/photos',{image:upload('dog-lifestyle.jpg'),caption:'户外生活示意照片'},'上传相册照片',true)).item;
 await snap(u,'09-photos','宠物相册','/photos','真实上传本地照片，展示图片与说明；素材仅用于功能演示。');
 await snap(u,'10-dashboard','照护首页与统计','/dashboard','首页展示当前账号的宠物、提醒、统计和快捷入口。');
 const search=await api(u,'GET','/search?q='+encodeURIComponent('旺财'),null,'搜索宠物档案');check('搜索包含目标宠物',search.pets.some(p=>p.id===1));
 await u.page.goto(origin+'/search');await u.page.locator('main input').fill('旺财');await u.page.getByRole('button',{name:'搜索',exact:true}).click();await u.page.getByRole('heading',{name:'旺财',exact:true}).waitFor();
 await snap(u,'11-search','搜索档案',null,'按关键词搜索当前账号宠物；当前接口不提供健康记录全文搜索。');
 await snap(u,'12-hospitals','医院地图与附近搜索','/community/hospitals','使用浏览器测试坐标查询附近医院；底图依赖外部地图服务，医院为虚构演示数据。');
 await u.page.getByRole('button',{name:'查找附近医院',exact:true}).click();await u.page.waitForResponse(r=>r.url().includes('/hospitals/nearby'));check('附近医院查询',await u.page.getByRole('button',{name:'详情与评价',exact:true}).count()>0);
 await u.page.getByRole('button',{name:'详情与评价',exact:true}).first().click();await u.page.getByLabel('就诊体验').fill('本次为功能演示评价，不代表真实就诊经历。');await u.page.getByRole('button',{name:'提交评价',exact:true}).click();await u.page.getByText('评价已发表',{exact:true}).waitFor();
 await snap(u,'13-hospital-review','医院详情与评价',null,'通过页面提交五星演示评价，并刷新详情读取结果。');
 await snap(u,'14-shop','商城筛选与商品浏览','/shop/products','按物种、分类和关键词筛选商品；价格和库存来自实际接口。');
 await api(u,'GET','/shop/products?q='+encodeURIComponent('常粮')+'&species=dog&category=food',null,'商城组合筛选');
 await u.page.getByRole('button',{name:'查看详情',exact:true}).first().click();await u.page.locator('.detail-modal').waitFor();await snap(u,'15-product-detail','商品详情',null,'查看商品说明、库存、价格和图片。');
 await api(u,'POST','/shop/cart',{product_id:1,quantity:2},'加入购物车');
 await snap(u,'16-cart','购物车与收货信息','/shop/cart','购物车显示商品、数量、合计及收货表单，实际接口下单验证如下。');
 const address={recipient_name:'演示收件人',recipient_phone:'13800000000',address_province:'上海市',address_city:'上海市',address_detail:'功能测试地址 不配送'};
 const order=(await api(u,'POST','/shop/orders',address,'提交订单')).order;
 await snap(u,'17-orders','订单列表与取消','/shop/orders','创建待付款订单，验证详情读取及买家取消。该流程未调用真实支付。');
 await u.page.getByRole('button',{name:'查看详情',exact:true}).first().click();await u.page.locator('.order-modal').waitFor();await snap(u,'18-order-detail','订单明细',null,'读取收货信息、商品行和金额；演示地址不用于配送。');
 await api(u,'PUT',`/shop/orders/${order.id}/status`,{status:'cancelled'},'取消待付款订单');
 for(const [section,title,note] of [['assistant','资料问答助手','返回项目资料和引用；未配置模型凭据时为 retrieval_only，不等于真实模型推理。'],['breed','品种识别与历史','上传猫照片并保存结果；现实现为颜色启发式猫分类且置信度固定 75%，不能视为可靠品种识别。'],['emotion','情绪分析与历史','上传图片并保存结果；基于颜色、边缘等启发式特征，不能证明宠物实际情绪。'],['health','健康报告','根据档案生成参考报告；本次验证本地降级结果，不代表医学诊断或远程大模型实测。'],['recommend','商品推荐','根据宠物资料和在售商品生成参考推荐；本次验证本地算法返回。'],['consultation','症状咨询与历史','提交描述并展示参考建议及历史；本次为本地规则结果，不替代兽医判断。']]){
  await u.page.goto(origin+'/ai/'+section,{waitUntil:'networkidle'});
  if(section==='assistant')await u.page.getByLabel('您的问题').fill('如何设置疫苗提醒？');
  if(['breed','emotion'].includes(section))await u.page.getByLabel('上传宠物照片').setInputFiles(path.join(root,'frontend/public/images/cat-lifestyle.jpg'));
  if(section==='consultation'){await u.page.getByLabel('症状描述').fill('演示：食欲下降，精神不振');await u.page.getByLabel('持续时间').fill('一天')}
  const response=u.page.waitForResponse(r=>r.request().method()==='POST'&&r.url().includes('/api/'));
  await u.page.getByRole('button',{name:section==='assistant'?'发送问题':'开始分析',exact:true}).click();const r=await response;check(title+'实际提交',r.ok());
  await u.page.locator('.assistant-result .prewrap').first().waitFor();check(title+'有返回内容',(await u.page.locator('.assistant-result .prewrap').first().innerText()).length>5);
  await snap(u,'ai-'+section,title,null,note);
 }
 await snap(a,'25-admin-overview','管理员统计','/admin/dashboard','统计用户、宠物、记录、商品、订单和分析次数。');
 await api(a,'PUT',`/admin/users/${registered.user.id}`,{username:account,email:account+'@example.invalid',phone:'13900000000',password:null,role:'user'},'管理员修改测试用户');
 await snap(a,'26-admin-users','管理员用户管理','/admin/users','真实用户列表；管理员修改并删除本次注册的临时账号。');
 await snap(a,'27-admin-pets','管理员宠物管理','/admin/pets','管理员查看宠物及所属用户，并删除临时测试档案。');
 await guest.page.goto(origin+'/register',{waitUntil:'networkidle'});await guest.page.getByLabel('注册类型').selectOption('merchant');
 await snap(guest,'28a-merchant-register','商户注册入口',null,'商户提交店铺名称及账号信息后进入待审核状态。');
 const merchant=(await api(guest,'POST','/auth/merchant/register',{username:'shop'+stamp,email:'shop'+stamp+'@example.invalid',password:'Show123!',storeName:'功能展示店铺'},'商户入驻申请')).user;
 await snap(a,'28-merchant-approval','商户入驻审核','/admin/merchants','展示待审核店铺，随后调用实际审核接口并验证商户可登录。');
 await api(a,'POST',`/admin/merchants/${merchant.id}/approve`,null,'管理员审核商户');await login(guest,'shop'+stamp,'Show123!');
 const product=(await api(m,'POST','/shop/products',{name:'演示护理梳',price:19.9,stock:10,category:'accessory',target_species:'both',description:'仅用于功能展示'},'商户新增商品')).product;
 await api(m,'PUT',`/shop/products/${product.id}`,{price:18.9,stock:12},'商户编辑商品');
 await api(m,'POST',`/shop/products/${product.id}/image`,{image:upload('product-care.jpg')},'商户上传商品图片',true);
 await snap(m,'29-merchant-products','商户商品管理','/merchant/products','新增商品、编辑价格库存并上传真实图片；另行验证下架与删除。');
 await api(u,'POST','/shop/cart',{product_id:1,quantity:1},'商户订单准备');const mo=(await api(u,'POST','/shop/orders',address,'创建商户订单')).order;
 for(const status of ['paid','shipped','delivered'])await api(m,'PUT',`/shop/orders/${mo.id}/status`,{status},'商户订单状态 '+status);
 await snap(m,'30-merchant-orders','商户订单处理','/merchant/orders','验证待付款到已付款、已发货、已完成的状态流转；已付款仅为人工状态，不代表支付网关收款。');
 await snap(m,'31-billing','租金账单查询','/merchant/billing','账单查询接口可用，展示一笔虚构租金账单；未提供在线缴费功能。');
 await api(m,'PUT',`/shop/products/${product.id}`,{is_active:false},'商品下架');await api(m,'DELETE',`/shop/products/${product.id}`,null,'删除测试商品');
 await api(u,'POST','/shop/cart',{product_id:1,quantity:1},'购物车移除准备');const cart=await api(u,'GET','/shop/cart',null,'读取购物车');await api(u,'DELETE',`/shop/cart/${cart.items[0].id}`,null,'移除购物车商品');
 await api(u,'POST','/shop/cart',{product_id:2,quantity:1},'购物车清空准备');await api(u,'POST','/shop/cart/clear',null,'清空购物车');
 const extra=(await api(a,'POST','/admin/users',{username:'adminCreated'+stamp,email:'adminCreated'+stamp+'@example.invalid',password:'Show123!',role:'user'},'管理员新增用户')).user;await api(a,'DELETE',`/admin/users/${extra.id}`,null,'删除管理员新增用户');
 const disposable=(await api(u,'POST','/pets',{name:'删除验证',species:'cat'},'宠物删除准备')).pet;await api(u,'DELETE',`/pets/${disposable.id}`,null,'宠物主人删除档案');
 await api(u,'PUT',`/pets/1/reminders/${reminder.id}`,{is_dismissed:true},'忽略提醒');
 await api(u,'DELETE',`/pets/1/photos/${photo.id}`,null,'删除相册照片');await api(u,'DELETE',`/pets/1/logs/${log.id}`,null,'删除日志');
 await api(u,'DELETE',`/pets/1/weights/${weight.id}`,null,'删除体重记录');await api(u,'DELETE',`/pets/1/reminders/${reminder.id}`,null,'删除提醒');await api(u,'DELETE',`/pets/1/health/${health.id}`,null,'删除健康记录');
 await api(a,'DELETE',`/admin/pets/${pet.id}`,null,'管理员删除测试宠物');await api(a,'DELETE',`/admin/users/${registered.user.id}`,null,'管理员删除测试用户');
 await u.page.getByRole('button',{name:'退出登录',exact:true}).click();await u.page.waitForURL('**/login');check('浏览器退出登录',true);
 await snap(u,'32-logout','退出登录',null,'点击退出后返回登录页，已清除认证状态。');
 check('无未捕获浏览器错误',errors.length===0,errors.join(';'));
})().catch(e=>{console.error(e.stack);checks.push({name:'执行中断',passed:false,detail:e.message});process.exitCode=1}).finally(async()=>{
 if(browser)await browser.close();fs.writeFileSync(path.join(out,'audit.json'),JSON.stringify({at:new Date().toISOString(),environment:'isolated H2 demo port 8081',checks,shots,errors},null,2));console.log(`${checks.filter(c=>c.passed).length}/${checks.length} checks; ${shots.length} feature demonstrations`)
});
