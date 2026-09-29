const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root=path.resolve(__dirname,'..'), evidence=path.join(root,'docs/evidence');
const results=[], errors=[];
let browser;
async function inspect(page, route, screenshot) {
  await page.goto('http://127.0.0.1:5173'+route,{waitUntil:'networkidle'});
  const heading=await page.locator('main h1').innerText();
  const failures=await page.locator('main .message.error').allTextContents();
  results.push({route,heading,passed:!failures.length,errors:failures});
  if(screenshot) await page.screenshot({path:path.join(evidence,screenshot),fullPage:true});
  assert.equal(failures.length,0,route+': '+failures.join(' / '));
}
async function login(page, username,password) {
  await page.goto('http://127.0.0.1:5173/login',{waitUntil:'networkidle'});
  await page.getByLabel('用户名',{exact:true}).fill(username);
  await page.getByLabel('密码',{exact:true}).fill(password);
  await Promise.all([page.waitForURL(/dashboard|merchant\/products/),page.getByRole('button',{name:'登录',exact:true}).click()]);
  await page.waitForLoadState('networkidle');
  results.push({route:'/login',name:username,passed:true});
}
(async()=>{
  browser=await chromium.launch({channel:'chrome',headless:true});
  const context=await browser.newContext({viewport:{width:1440,height:1000}});
  const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
  await login(page,'demo','demo123');
  const safe=await page.evaluate(()=>!localStorage.getItem('token')&&!sessionStorage.getItem('token')&&!document.cookie.split(';').some(c=>c.trim().startsWith('token=')));
  assert.ok(safe,'JWT should not be readable from JS');results.push({name:'浏览器 JavaScript 无可读 JWT',passed:safe});
  await inspect(page,'/dashboard','ui-dashboard.png');
  await inspect(page,'/pets','ui-pets.png');
  await inspect(page,'/pets/1/health','ui-journal.png');
  for(const section of ['weights','logs','photos','reminders']) await inspect(page,'/pets/1/'+section);
  for(const route of ['/reminders','/profile','/shop/products','/shop/cart','/shop/orders','/community/hospitals']) await inspect(page,route,route==='/shop/products'?'ui-shop.png':route==='/community/hospitals'?'ui-hospitals.png':null);
  for(const section of ['breed','emotion','health','recommend','consultation']) await inspect(page,'/ai/'+section);
  await inspect(page,'/ai/assistant');
  await page.getByLabel('您的问题').fill('如何设置疫苗提醒？');
  await page.getByRole('button',{name:'发送问题'}).click();
  await page.getByText('参考来源',{exact:true}).waitFor();
  await page.screenshot({path:path.join(evidence,'ui-assistant.png'),fullPage:true});
  results.push({name:'RAG 页面显示回答及引用',passed:true});
  await page.setViewportSize({width:390,height:844});await inspect(page,'/dashboard','ui-mobile.png');
  const overflow=await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1);
  assert.ok(!overflow,'Mobile page overflows horizontally');results.push({name:'移动视口无横向溢出',passed:!overflow});
  const admin=await browser.newContext({viewport:{width:1440,height:1000}});const adminPage=await admin.newPage();adminPage.on('pageerror',e=>errors.push(e.message));
  await login(adminPage,'admin','admin123');
  await inspect(adminPage,'/admin/users','ui-users.png');
  await inspect(adminPage,'/admin/pets');await inspect(adminPage,'/admin/merchants');
  const merchant=await browser.newContext({viewport:{width:1440,height:1000}});const merchantPage=await merchant.newPage();merchantPage.on('pageerror',e=>errors.push(e.message));
  await login(merchantPage,'demo_shop','demoShop123');
  await inspect(merchantPage,'/merchant/products','ui-merchant.png');
  await inspect(merchantPage,'/merchant/orders');await inspect(merchantPage,'/merchant/billing');
  assert.equal(errors.length,0,'Browser page errors: '+errors.join('; '));
})().catch(error=>{console.error(error.stack);results.push({name:'浏览器检查中断',passed:false,error:error.message});process.exitCode=1;}).finally(async()=>{
  if(browser) await browser.close();
  fs.writeFileSync(path.join(evidence,'browser-verification.json'),JSON.stringify({at:new Date().toISOString(),results,errors},null,2));
  console.log(`${results.filter(r=>r.passed).length}/${results.length} browser checks passed`);
});
