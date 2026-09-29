const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const origin = 'http://127.0.0.1:5173';
const out = path.resolve(__dirname, '../docs/evidence/ui-refresh');
fs.mkdirSync(out, {recursive:true});
const results = [], errors = [];
let browser;
function check(name, condition, detail='') { results.push({name, passed:!!condition, detail}); assert.ok(condition, name + ' ' + detail); }
async function inspect(page, route, screenshot) {
  await page.goto(origin + route, {waitUntil:'networkidle'});
  await page.locator('main h1').first().waitFor();
  await page.evaluate(async () => {
    for (const img of document.querySelectorAll('main img')) {
      img.loading = 'eager';
      try { await img.decode(); } catch { /* Broken sources are checked below. */ }
      if (!img.complete) await new Promise(resolve => { img.addEventListener('load',resolve,{once:true}); img.addEventListener('error',resolve,{once:true}); setTimeout(resolve,4000); });
    }
  });
  const state = await page.evaluate(() => ({
    width: innerWidth, scroll: document.documentElement.scrollWidth,
    broken: [...document.querySelectorAll('main img')].filter(i => i.src.startsWith(location.origin) && i.complete && !i.naturalWidth).map(i=>i.getAttribute('src')),
    errors: [...document.querySelectorAll('main .message.error')].map(e=>e.textContent),
  }));
  check(`${state.width}px ${route}`, state.scroll <= state.width + 1 && !state.broken.length && !state.errors.length, JSON.stringify(state));
  if (screenshot) await page.screenshot({path:path.join(out,screenshot), fullPage:true});
}
async function makePage(username, password) {
  const context = await browser.newContext({viewport:{width:1440,height:960}});
  const page = await context.newPage(); page.setDefaultTimeout(15000);
  page.on('pageerror', error=>errors.push(error.message));
  if (username) {
    await page.goto(origin+'/login',{waitUntil:'networkidle'});
    await page.getByLabel('用户名',{exact:true}).fill(username);
    await page.getByLabel('密码',{exact:true}).fill(password);
    await Promise.all([page.waitForURL(/dashboard|merchant\/products/),page.getByRole('button',{name:'登录',exact:true}).click()]);
  }
  return page;
}
(async()=>{
  browser=await chromium.launch({channel:'chrome',headless:true});
  const guest=await makePage();
  await inspect(guest,'/login','login-desktop.png'); await inspect(guest,'/register','register-desktop.png');
  await guest.setViewportSize({width:360,height:800});
  await inspect(guest,'/login','login-mobile.png'); await inspect(guest,'/register');
  const demo=await makePage('demo','demo123');
  const routes=[['/dashboard','dashboard-desktop.png'],['/pets','pets-desktop.png'],['/search','search-desktop.png'],['/pets/1/health','health-desktop.png'],['/pets/1/weights'],['/pets/1/reminders'],['/pets/1/logs'],['/pets/1/photos'],['/reminders'],['/logs'],['/photos'],['/profile','profile-desktop.png'],['/shop/products','shop-desktop.png'],['/shop/cart'],['/shop/orders'],['/community/hospitals','hospitals-desktop.png'],['/ai/assistant','assistant-desktop.png'],['/ai/breed'],['/ai/emotion'],['/ai/health'],['/ai/recommend'],['/ai/consultation']];
  for (const [route,shot] of routes) await inspect(demo,route,shot);
  await demo.goto(origin+'/ai/assistant',{waitUntil:'networkidle'});
  await demo.getByLabel('您的问题').fill('如何设置疫苗提醒？');
  await demo.getByRole('button',{name:'发送问题',exact:true}).click();
  await demo.getByText('参考来源',{exact:true}).waitFor();
  check('助手回答及来源展示',true);
  await demo.screenshot({path:path.join(out,'assistant-answer.png'),fullPage:true});
  await demo.setViewportSize({width:390,height:844});
  for (const [route] of routes) await inspect(demo,route, ['/dashboard','/pets','/shop/products'].includes(route) ? route.split('/').filter(Boolean).join('-')+'-mobile.png' : undefined);
  await demo.goto(origin+'/dashboard',{waitUntil:'networkidle'});
  await demo.getByRole('button',{name:'打开导航',exact:true}).click();
  await demo.waitForFunction(() => document.querySelector('.sidebar').getBoundingClientRect().left >= 0);
  check('移动导航展开',await demo.locator('.sidebar').evaluate(e=>e.getBoundingClientRect().left>=0));
  await demo.keyboard.press('Escape');
  check('Escape关闭移动导航',await demo.getByRole('button',{name:'打开导航',exact:true}).getAttribute('aria-expanded') === 'false');
  await demo.getByRole('button',{name:'打开导航',exact:true}).click();
  await demo.locator('.sidebar').getByRole('link',{name:'我的宠物',exact:true}).click();
  check('导航后菜单收起',await demo.getByRole('button',{name:'打开导航',exact:true}).getAttribute('aria-expanded') === 'false');
  const admin=await makePage('admin','admin123');
  for(const route of ['/admin/dashboard','/admin/users','/admin/pets','/admin/merchants']) await inspect(admin,route,route.split('/').pop()+'-admin.png');
  await admin.setViewportSize({width:360,height:800});
  for(const route of ['/admin/dashboard','/admin/users','/admin/pets','/admin/merchants']) await inspect(admin,route);
  const merchant=await makePage('demo_shop','demoShop123');
  for(const route of ['/merchant/products','/merchant/orders','/merchant/billing']) await inspect(merchant,route,route.split('/').pop()+'-merchant.png');
  await merchant.setViewportSize({width:360,height:800});
  for(const route of ['/merchant/products','/merchant/orders','/merchant/billing']) await inspect(merchant,route);
  check('浏览器无未捕获错误',!errors.length,errors.join('; '));
})().catch(error=>{ console.error(error.stack); results.push({name:'UI检查中断',passed:false,detail:error.message}); process.exitCode=1; }).finally(async()=>{
  if(browser)await browser.close();
  fs.writeFileSync(path.join(out,'verification.json'),JSON.stringify({at:new Date().toISOString(),results,errors},null,2));
  console.log(`${results.filter(r=>r.passed).length}/${results.length} UI refresh checks passed`);
});
