// Requires local disposable demo backend on 8080 and Vite on 5173.
const {chromium}=require('playwright');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const out=path.resolve(__dirname,'../docs/evidence/ai-samples');
fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try {
  const page=await browser.newPage({viewport:{width:1440,height:1000}}),errors=[],checks=[],shots=[];
  page.on('pageerror',e=>errors.push(e.message));
  let writes=0;page.on('request',r=>{if(r.method()==='POST' && /\/(ai|assistant|consultation)\//.test(r.url()))writes++});
  await page.goto('http://127.0.0.1:5173/demo',{waitUntil:'networkidle'});
  await page.getByRole('button',{name:'体验宠物主人',exact:true}).click();await page.waitForURL('**/dashboard');
  for(const [section,title,endpoint] of [['assistant','资料问答','/assistant/ask'],['breed','品种识别','/ai/breed/recognize'],['emotion','情绪分析','/ai/emotion/recognize'],['health','健康报告','/ai/health-report/'],['recommend','商品推荐','/ai/recommend/'],['consultation','症状咨询','/consultation/ask']]){
   await page.goto('http://127.0.0.1:5173/demo',{waitUntil:'networkidle'});
   await page.getByRole('button',{name:'体验'+title,exact:true}).click();await page.waitForURL('**/ai/'+section);
   const beforeFill=writes;
   await page.getByRole('button',{name:'填入样例',exact:true}).click();
   await page.getByText('样例已填入，可修改后提交。',{exact:true}).waitFor();
   assert.equal(writes,beforeFill,'填入样例不自动提交');
   if(['breed','emotion'].includes(section)) await page.getByRole('img',{name:'待分析的样例照片'}).waitFor();
   if(section==='consultation') assert.ok((await page.getByLabel('症状描述').inputValue()).includes('虚构'));
   const pending=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().includes(endpoint));
   await page.getByRole('button',{name:section==='assistant'?'发送问题':'开始分析',exact:true}).click();
   const response=await pending;assert.ok(response.ok(),title+' HTTP '+response.status());
   await page.locator('.assistant-result .prewrap').first().waitFor();
   assert.ok((await page.locator('.assistant-result .prewrap').first().innerText()).length>5);
   assert.equal(await page.locator('main .message.error').count(),0);
   const result=await response.json();checks.push({section,passed:true,status:response.status(),mode:result.status||'local_or_configured'});
   await page.evaluate(async()=>{await Promise.all([...document.images].map(async i=>{i.loading='eager';try{await i.decode()}catch{}}))});
   const box=await page.locator('.assistant-workspace').boundingBox();
   await page.screenshot({path:path.join(out,section+'.png'),clip:{x:0,y:0,width:1440,height:Math.ceil(box.y+box.height+24)}});shots.push({section,title});
  }
  await page.setViewportSize({width:390,height:844});await page.goto('http://127.0.0.1:5173/ai/breed',{waitUntil:'networkidle'});
  await page.getByRole('button',{name:'填入样例',exact:true}).click();await page.getByText('样例已填入，可修改后提交。',{exact:true}).waitFor();
  assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  await page.getByLabel('上传宠物照片').setInputFiles({name:'user-cat.jpg',mimeType:'image/jpeg',buffer:fs.readFileSync(path.resolve(__dirname,'../frontend/public/images/cat-lifestyle.jpg'))});
  assert.equal(await page.getByRole('img',{name:'待分析的样例照片'}).count(),0);
  await page.getByText('已选择：user-cat.jpg',{exact:false}).waitFor();
  const replaced=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().includes('/ai/breed/recognize'));
  await page.getByRole('button',{name:'开始分析',exact:true}).click();assert.ok((await replaced).ok());
  await page.goto('http://127.0.0.1:5173/ai/breed',{waitUntil:'networkidle'});
  await page.route('**/images/cat-lifestyle.jpg',r=>r.fulfill({status:503,body:'Unavailable'}));
  await page.getByRole('button',{name:'填入样例',exact:true}).click();await page.getByText('样例照片加载失败，请重试或上传自己的图片。',{exact:true}).waitFor();
  await page.unroute('**/images/cat-lifestyle.jpg');await page.getByRole('button',{name:'填入样例',exact:true}).click();await page.getByText('样例已填入，可修改后提交。',{exact:true}).waitFor();
  await page.route('**/api/demo/status',r=>r.fulfill({json:{enabled:false}}));await page.reload({waitUntil:'networkidle'});
  assert.equal(await page.getByRole('button',{name:'填入样例',exact:true}).count(),0);
  assert.deepEqual(errors,[]);
  fs.writeFileSync(path.join(out,'verification.json'),JSON.stringify({checkedAt:new Date().toISOString(),checks,shots,mobileOverflow:false,extraChecks:['six hub entries','no automatic submissions','manual photo replacement','photo failure and retry','non-demo entry hidden'],errors},null,2));
  console.log('6 AI sample submissions passed; mobile layout and 5 extra checks passed; no page errors');
 } finally {await browser.close()}
})().catch(e=>{console.error(e);process.exit(1)});
