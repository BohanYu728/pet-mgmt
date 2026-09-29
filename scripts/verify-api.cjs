// End-to-end HTTP checks against an isolated demo-profile server only.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const base = 'http://127.0.0.1:8080';
const checks = [];
function check(name, condition, details = '') {
  checks.push({ name, passed: Boolean(condition), details });
  assert.ok(condition, name + ': ' + details);
}
class Session {
  cookies = new Map();
  cookieFlags = null;
  csrf = null;
  async call(method, url, body, csrf = true) {
    if (!['GET', 'HEAD'].includes(method) && csrf) {
      const token = await this.call('GET', '/api/auth/csrf'); this.csrf = token.body;
    }
    const headers = {};
    if (this.cookies.size) headers.Cookie = [...this.cookies].map(([k,v]) => `${k}=${v}`).join('; ');
    if (!['GET','HEAD'].includes(method) && csrf) headers[this.csrf.headerName] = this.csrf.token;
    if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json';
    const response = await fetch(base + url, { method, headers, body: body ? (body instanceof FormData ? body : JSON.stringify(body)) : undefined });
    for (const cookie of response.headers.getSetCookie()) {
      const first = cookie.split(';')[0], split = first.indexOf('=');
      this.cookies.set(first.slice(0,split), first.slice(split+1));
      if (first.startsWith('token=')) this.cookieFlags = { httpOnly: /httponly/i.test(cookie), sameSiteLax: /samesite=lax/i.test(cookie), secure: /;\s*secure/i.test(cookie) };
    }
    const text = await response.text();
    let value; try { value = JSON.parse(text); } catch { value = text; }
    return { status: response.status, body: value, data: value?.data ?? value };
  }
  async login(username, password) {
    const result = await this.call('POST', '/api/auth/login', {username,password});
    check(`${username} 登录`, result.status === 200 && !result.body.error, `HTTP ${result.status}`);
    check(`${username} 令牌不进入 JSON`, !JSON.stringify(result.body).includes('"token"'));
    return result;
  }
}
async function run() {
  const anon = new Session(), user = new Session(), admin = new Session(), merchant = new Session();
  let r = await anon.call('GET','/api/pets'); check('未登录访问宠物被拒绝',r.status===401,`HTTP ${r.status}`);
  r=await anon.call('POST','/api/auth/login',{username:'demo',password:'demo123'},false);check('缺失 CSRF 被拒绝',r.status===403,`HTTP ${r.status}`);
  await user.login('demo','demo123'); await admin.login('admin','admin123'); await merchant.login('demo_shop','demoShop123');
  check('JWT Cookie 启用 HttpOnly 和 SameSite',user.cookieFlags.httpOnly && user.cookieFlags.sameSiteLax);
  r=await user.call('GET','/api/admin/users');check('普通用户不能进入管理接口',r.status===403,`HTTP ${r.status}`);
  const suffix=Date.now().toString().slice(-8), username='verify'+suffix;
  r=await admin.call('POST','/api/admin/users',{username,email:username+'@example.invalid',password:'Verify123!',role:'user'});
  check('MyBatis 新增用户',r.status===201,`HTTP ${r.status}`);const uid=r.data.user.id;
  r=await admin.call('PUT',`/api/admin/users/${uid}`,{username:username+'x',email:username+'@example.invalid',password:null,phone:'13800000000',role:'user'});
  check('MyBatis 更新用户',r.status===200&&r.data.user.username===username+'x',`HTTP ${r.status} ${JSON.stringify(r.body)}`);
  r=await admin.call('GET',`/api/admin/users/${uid}`);check('MyBatis 读取用户且无密码字段',r.status===200&&!JSON.stringify(r.body).includes('password'));
  r=await admin.call('DELETE',`/api/admin/users/${uid}`);check('MyBatis 删除无关联用户',r.status===200);
  r=await admin.call('DELETE','/api/admin/users/2');check('拒绝删除有关联宠物的用户',r.status===409);
  r=await user.call('POST','/api/pets',{name:'联调宠物'+suffix,species:'cat',breed:'英短',birth_date:'2025-01-01',weight:4.2});
  check('新增宠物',r.status===200&&!r.body.error);const pet=r.data.pet;
  r=await merchant.call('GET',`/api/pets/${pet.id}`);check('不同账号无法读取宠物',r.status===403);
  r=await user.call('PUT',`/api/pets/${pet.id}`,{name:'联调档案'+suffix});check('修改宠物',r.status===200);
  r=await user.call('POST',`/api/pets/${pet.id}/health`,{title:'联调体检',record_type:'checkup',record_date:'2026-09-01',next_due_date:'2027-09-01'});
  check('创建健康记录',r.status===200&&!r.body.error);const hid=r.data.record.id;
  r=await user.call('GET',`/api/pets/${pet.id}/reminders`);check('健康记录自动生成提醒',r.body.items.some(x=>x.title.includes('联调体检')));
  r=await user.call('POST',`/api/pets/${pet.id}/weights`,{weight:4.3,record_date:'2026-09-02'});check('记录体重',r.status===200);const wid=r.body.item.id;
  r=await user.call('POST',`/api/pets/${pet.id}/weights`,{weight:-1});check('拒绝负数体重',r.status===400);
  r=await user.call('POST',`/api/pets/${pet.id}/logs`,{title:'联调日记',content:'今天完成日常记录',log_date:'2026-09-02',log_type:'daily'});check('创建日常日志',r.status===200);const lid=r.body.item.id;
  r=await user.call('PUT',`/api/pets/${pet.id}/logs/${lid}`,{content:'已修改记录'});check('修改日常日志',r.status===200);
  r=await user.call('GET','/api/shop/products');check('公开商品接口有演示数据',r.status===200&&r.body.items.length>0);const product=r.body.items[0];
  r=await user.call('POST','/api/shop/cart',{product_id:product.id,quantity:-1});check('拒绝负数购物车数量',r.status===400,`HTTP ${r.status}`);
  r=await user.call('POST','/api/shop/cart',{product_id:product.id,quantity:1});check('加入购物车',r.status===200&&!r.body.error);
  r=await user.call('POST','/api/shop/orders',{recipient_name:'测试收件人',recipient_phone:'13800000000',address_detail:'演示地址'});check('提交订单',r.status===200&&!r.body.error);const order=r.data.order;
  r=await user.call('PUT',`/api/shop/orders/${order.id}/status`,{status:'cancelled'});check('买家取消待付款订单',r.status===200&&!r.body.error);
  r=await user.call('GET','/api/shop/merchant/products');check('普通用户不能访问商户商品管理',r.status===403);
  r=await merchant.call('GET','/api/shop/merchant/products');check('商户读取自己的商品',r.status===200&&r.body.items.length>0);
  r=await user.call('GET','/api/hospitals');check('医院查询',r.status===200&&r.body.hospitals.length>0);const hospital=r.body.hospitals[0];
  r=await user.call('POST',`/api/hospitals/${hospital.id}/reviews`,{rating:5,content:'本地接口验证记录'});check('创建医院评价',r.status===200);
  r=await user.call('POST',`/api/hospitals/${hospital.id}/reviews`,{rating:9});check('拒绝无效评分',r.status===400);
  r=await user.call('POST','/api/assistant/ask',{question:'如何设置疫苗提醒？'});check('RAG 返回引用资料与真实状态',r.status===200&&r.body.sources.length>0&&['retrieval_only','answered'].includes(r.body.status));
  r=await user.call('POST','/api/assistant/ask',{question:'x'.repeat(1001)});check('RAG 输入长度校验',r.status===400);
  await user.call('DELETE',`/api/pets/${pet.id}/logs/${lid}`);
  await user.call('DELETE',`/api/pets/${pet.id}/weights/${wid}`);
  r=await user.call('DELETE',`/api/pets/${pet.id}/health/${hid}`);check('删除健康记录及关联提醒',r.status===200);
  r=await user.call('DELETE',`/api/pets/${pet.id}`);check('删除测试宠物',r.status===200);
  r=await user.call('POST','/api/auth/logout');check('退出登录',r.status===200);
  r=await user.call('GET','/api/auth/profile');check('退出后认证失效',r.status===401);
}
run().catch(error=>{ console.error(error.message);process.exitCode=1; }).finally(()=>{
  fs.mkdirSync(path.join(root,'docs/evidence'),{recursive:true});
  fs.writeFileSync(path.join(root,'docs/evidence/api-verification.json'),JSON.stringify({at:new Date().toISOString(),environment:'isolated H2 demo',checks},null,2));
  console.log(`${checks.filter(c=>c.passed).length}/${checks.length} HTTP checks passed`);
});
