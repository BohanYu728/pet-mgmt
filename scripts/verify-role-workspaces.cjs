// Uses disposable demo data on port 8080; captures updated report evidence.
const {chromium}=require('playwright');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/evidence/roles');fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try {
  const page=await browser.newPage({viewport:{width:1440,height:960}}),checks=[],errors=[];page.on('pageerror',e=>errors.push(e.message));
  async function go(url){await page.goto('http://127.0.0.1:5173'+url,{waitUntil:'networkidle'})}
  async function snap(file){await page.evaluate(async()=>{await Promise.all([...document.images].map(async i=>{i.loading='eager';try{await i.decode()}catch{}}))});await page.screenshot({path:path.join(root,'docs/evidence',file+'.png'),fullPage:true})}
  for(const [role,username,password,home,paths,blocked] of [
   ['user','demo','demo123','/dashboard',['/pets','/shop/orders','/ai/assistant'],['/admin/users','/merchant/products']],
   ['merchant','demo_shop','demoShop123','/merchant/products',['/merchant/products','/merchant/orders','/merchant/billing'],['/pets','/shop/orders','/ai/assistant','/admin/users']],
   ['admin','admin','admin123','/admin/dashboard',['/admin/dashboard','/admin/users','/admin/pets','/admin/merchants'],['/pets','/shop/orders','/ai/assistant','/merchant/orders']]
  ]){
   await page.setViewportSize({width:1440,height:960});await go('/login');
   await page.getByLabel('用户名',{exact:true}).fill(username);await page.getByLabel('密码',{exact:true}).fill(password);
   await page.getByRole('button',{name:'登录',exact:true}).click();await page.waitForURL('**'+home);await page.waitForLoadState('networkidle');
   let links=await page.locator('#site-navigation a').evaluateAll(items=>items.map(a=>a.getAttribute('href')));
   for(const p of paths)assert.ok(links.includes(p),role+' missing '+p);
   for(const p of blocked)assert.ok(!links.includes(p),role+' unrelated '+p);
   await snap('roles/'+role);checks.push(role+' normal login and navigation');
   for(const p of blocked){await go(p);assert.equal(new URL(page.url()).pathname,home)}checks.push(role+' cross-role direct URLs redirected');
   await go('/profile');assert.equal(new URL(page.url()).pathname,'/profile');await page.locator('.brand').click();await page.waitForURL('**'+home);checks.push(role+' profile and brand home');
   await go('/demo');const cards=await page.locator('[aria-labelledby="scenes-title"] .tag').allTextContents();assert.ok(cards.length);assert.ok(cards.every(t=>t===({user:'宠物主人',merchant:'商户',admin:'管理员'}[role])));assert.equal(await page.locator('#ai-samples-title').count(),role==='user'?1:0);checks.push(role+' filtered sample center');
   await page.setViewportSize({width:390,height:844});await go(home);await page.getByRole('button',{name:'打开导航'}).click();await page.locator('#site-navigation.open').waitFor({state:'visible'});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await snap('roles/'+role+'-mobile');checks.push(role+' mobile navigation');
   await page.setViewportSize({width:1440,height:960});
   const shots=role==='admin'?[['/admin/dashboard','features/25-admin-overview'],['/admin/users','features/26-admin-users'],['/admin/pets','features/27-admin-pets'],['/admin/merchants','features/28-merchant-approval']]:role==='merchant'?[['/merchant/products','features/29-merchant-products'],['/merchant/orders','features/30-merchant-orders'],['/merchant/billing','demo/billing']]:[];
   for(const [url,file] of shots){await go(url);assert.equal(await page.locator('main .message.error').count(),0);await snap(file)}
   await page.getByRole('button',{name:'退出登录',exact:true}).click();await page.waitForURL('**/login');
  }
  await go('/admin/users');assert.equal(new URL(page.url()).pathname,'/login');checks.push('guest protected route redirects to login');
  assert.deepEqual(errors,[]);fs.writeFileSync(path.join(out,'verification.json'),JSON.stringify({checkedAt:new Date().toISOString(),checks,errors},null,2));console.log(checks.length+' workspace checks passed');
 } finally {await browser.close()}
})().catch(e=>{console.error(e);process.exit(1)});
