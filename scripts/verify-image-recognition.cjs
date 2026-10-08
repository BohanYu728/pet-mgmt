const {chromium}=require('playwright');
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),out=path.join(root,'docs/evidence/recognition-fix');
fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({channel:'chrome',headless:true});
 try {
  const page=await browser.newPage({viewport:{width:1440,height:1000}}),results=[],errors=[];
  page.on('pageerror',e=>errors.push(e.message));
  await page.goto('http://127.0.0.1:5173/demo',{waitUntil:'networkidle'});
  console.log('Demo buttons:',await page.getByRole('button').allTextContents());
  await page.getByRole('button',{name:'体验宠物主人',exact:true}).click();
  await page.waitForURL('**/dashboard');
  for(const [section,file,expected] of [['breed','border-collie-sample.jpg','dog'],['breed','dog-lifestyle.jpg','dog'],['breed','cat-lifestyle.jpg','cat'],['emotion','dog-lifestyle.jpg','visual'],['emotion','cat-lifestyle.jpg','visual']]) {
   await page.goto('http://127.0.0.1:5173/ai/'+section,{waitUntil:'networkidle'});
   console.log('Upload fields:',await page.getByLabel('上传宠物照片').count());
   if(file==='border-collie-sample.jpg') {
    await page.getByRole('button',{name:'填入样例',exact:true}).click();
    await page.getByText('样例已填入，可修改后提交。',{exact:true}).waitFor();
   } else await page.getByLabel('上传宠物照片').setInputFiles(path.join(root,'frontend/public/images',file));
   const response=page.waitForResponse(r=>r.url().includes('/ai/'+section+'/recognize')&&r.request().method()==='POST');
   await page.getByRole('button',{name:'开始分析',exact:true}).click();
   const r=await response,data=await r.json(); assert.equal(r.status(),200,JSON.stringify(data));
   if(section==='breed')assert.equal(data.species,expected);
   else {assert.equal(data.analysis_mode,'local_clip_zero_shot');assert.equal(data.candidates.length,6);}
   assert.ok(data.history_id);
   if(file==='border-collie-sample.jpg')assert.match(data.name_zh,/边境牧羊犬/);
   if(section==='emotion')assert.equal(data.confidence,null);
   await page.locator('.assistant-result .prewrap').waitFor();
   await page.evaluate(async()=>{
    window.scrollTo(0,0);
    await Promise.all([...document.images].map(async img=>{img.loading='eager';await img.decode();}));
   });
   assert.ok(await page.locator('.assistant-result img').evaluate(img=>img.naturalWidth>0));
   await page.screenshot({path:path.join(out,section+'-'+file.replace('.jpg','')+'.png'),fullPage:true});
   results.push({section,file,response:data});
  }
  for(const section of ['breed','emotion']) {
   await page.goto('http://127.0.0.1:5173/ai/'+section,{waitUntil:'networkidle'});
   await page.getByLabel('上传宠物照片').setInputFiles({name:'broken.jpg',mimeType:'image/jpeg',buffer:Buffer.from('not an image')});
   const response=page.waitForResponse(r=>r.url().includes('/ai/'+section+'/recognize')&&r.request().method()==='POST');
   await page.getByRole('button',{name:'开始分析',exact:true}).click();
   const r=await response;assert.equal(r.status(),400);await page.locator('.message.error').waitFor();
   results.push({section,invalidImageStatus:r.status()});
  }
  assert.deepEqual(errors,[]);
  fs.writeFileSync(path.join(out,'verification.json'),JSON.stringify({checks:results,errors},null,2));
  console.log(JSON.stringify({passed:results.length,results,errors}));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
