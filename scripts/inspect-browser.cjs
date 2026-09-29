const { chromium } = require('playwright');
const path = require('node:path');
(async()=>{
  const browser=await chromium.launch({channel:'chrome',headless:true});
  try {
    const page=await browser.newPage({viewport:{width:1440,height:1000}});
    const errors=[];page.on('pageerror',e=>errors.push(e.message));
    await page.goto('http://127.0.0.1:5173/login',{waitUntil:'networkidle'});
    console.log(await page.locator('main').innerText());
    console.log(await page.locator('input').evaluateAll(nodes=>nodes.map(n=>({type:n.type,label:n.closest('label')?.innerText}))));
    console.log('Page errors:',errors);
    await page.screenshot({path:path.resolve(__dirname,'../docs/evidence/ui-login.png'),fullPage:true});
    if(process.env.INSPECT_LOGIN==='1') {
      await page.getByLabel('用户名',{exact:true}).fill('demo');
      await page.getByLabel('密码',{exact:true}).fill('demo123');
      const responsePromise=page.waitForResponse(r=>r.url().endsWith('/api/auth/login'));
      await page.getByRole('button',{name:'登录',exact:true}).click();
      const response=await responsePromise;
      console.log('Login HTTP status:',response.status());
      if(response.status()!==200) console.log('Login error body:',await response.text());
      await page.waitForLoadState('networkidle');
      console.log('URL after submit:',page.url());
      console.log('Messages:',await page.locator('.message').allTextContents());
      await page.screenshot({path:path.resolve(__dirname,'../.local/login-diagnostic.png'),fullPage:true});
    }
  } finally { await browser.close(); }
})();
