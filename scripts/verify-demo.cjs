const {chromium} = require('playwright');
const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const output = path.resolve(__dirname, '../docs/evidence/demo');
fs.mkdirSync(output, {recursive:true});
(async () => {
 const browser = await chromium.launch({channel:'chrome',headless:true});
 const page = await browser.newPage({viewport:{width:1440,height:960}});
 const errors = [], checks = []; page.on('pageerror', e => errors.push(e.message));
 async function goto(route) {await page.goto('http://127.0.0.1:5173'+route,{waitUntil:'networkidle'});}
 async function shot(name) {await page.evaluate(async()=>{await Promise.all([...document.images].map(async image=>{image.loading='eager';try{await image.decode()}catch{}}))});await page.screenshot({path:path.join(output,name+'.png'),fullPage:name!=='showcase'});}
 async function enter(label,route) {await goto('/demo');await page.getByRole('button',{name:'体验'+label,exact:true}).click();await page.waitForURL('**'+route);await page.waitForLoadState('networkidle');assert.equal(await page.locator('main .message.error').count(),0);checks.push(label+'快捷登录');}
 await goto('/demo'); await shot('showcase');
 await enter('宠物主人','/dashboard');
 await goto('/pets/1/weights');assert.equal(await page.locator('svg circle').count(),3);checks.push('预置三次体重');await shot('weights');
 await goto('/logs');await page.getByText('旺财的一天（样例）',{exact:true}).waitFor();
 await page.getByRole('button',{name:'编辑',exact:true}).first().click();await page.getByLabel('日志内容').fill('样例可操作验证：今天按时饮水和玩耍。');await page.getByRole('button',{name:'保存',exact:true}).click();await page.getByText('样例可操作验证：今天按时饮水和玩耍。',{exact:true}).waitFor();checks.push('样例日志编辑保存');await shot('logs');
 await goto('/shop/orders');await page.getByRole('button',{name:'查看详情',exact:true}).first().click();await page.getByText('样例收件人',{exact:false}).first().waitFor();await shot('order');checks.push('样例订单明细');
 await enter('商户','/merchant/products');await goto('/merchant/billing');await page.getByText('DEMO-RENT-001',{exact:false}).first().waitFor();await shot('billing');checks.push('样例租金账单');
 await goto('/merchant/orders');await page.locator('main .data-table tbody tr').first().waitFor();checks.push('商户查看样例订单');
 await enter('管理员','/admin/dashboard');await goto('/admin/users');await page.getByText('demo_shop',{exact:true}).first().waitFor();checks.push('管理员查看样例账号');
 await page.setViewportSize({width:390,height:844});await goto('/demo');assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await shot('mobile');checks.push('手机展示无横向溢出');
 assert.deepEqual(errors,[]);fs.writeFileSync(path.join(output,'verification.json'),JSON.stringify({checkedAt:new Date().toISOString(),checks,errors},null,2));
 console.log(JSON.stringify({passed:checks.length,errors}));await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
