const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
(async () => {
  const browser = await chromium.launch({channel:'chrome',headless:true});
  try {
    const page = await browser.newPage();
    let valid = false;
    const product = () => ({ id: 999, name: '照片测试商品', image: valid ? '/images/product-toy.jpg' : '/broken-review-image.jpg', category:'toy', category_display:'宠物玩具', target_species_display:'狗狗', price:10, stock:2 });
    await page.route('**/broken-review-image.jpg', r => r.fulfill({status:404,body:''}));
    await page.route('**/api/**', async r => {
      const path = new URL(r.request().url()).pathname;
      let json;
      if (path.endsWith('/auth/profile')) json = {user:{id:999,username:'review',role:'user'}};
      else if (path.endsWith('/pets')) json = {pets:[{id:999,name:'照片测试宠物',species:'dog',avatar:'/broken-review-image.jpg'}],pages:1};
      else if (path.endsWith('/shop/products')) json = {items:[product()],pages:1};
      else if (path.endsWith('/shop/cart')) json = {items:[{id:999,product:product(),quantity:1}],item_count:1,total_amount:10};
      else json = {};
      await r.fulfill({json});
    });
    const checks=[];
    await page.goto('http://127.0.0.1:5173/pets');
    await page.getByText('默认示意',{exact:true}).waitFor();
    assert.equal(await page.locator('.pet-card-image img').getAttribute('alt'),'狗狗默认示意照片'); checks.push('broken pet URL has default badge and accurate alt');
    await page.goto('http://127.0.0.1:5173/shop/products');
    await page.getByText('分类示意图',{exact:true}).waitFor();
    assert.equal(await page.locator('.product-media img').getAttribute('alt'),'商品分类示意照片'); checks.push('broken product URL has sample badge and accurate alt');
    valid = true;
    await page.getByRole('button',{name:'搜索',exact:true}).click();
    await page.waitForFunction(() => document.querySelector('.product-media img')?.getAttribute('alt') === '照片测试商品');
    assert.equal(await page.locator('.product-media .fallback-label').count(),0); checks.push('same component resets badge and alt after source becomes valid');
    valid = false;
    await page.goto('http://127.0.0.1:5173/shop/cart');
    await page.locator('.cart-thumb .fallback-label.compact').waitFor();
    assert.equal(await page.locator('.cart-thumb img').getAttribute('alt'),'商品分类示意照片'); checks.push('cart fallback has compact visible badge and accurate alt');
    const result={passed:checks.length,checks};
    fs.writeFileSync('docs/evidence/ui-refresh/photo-fallback-review.json',JSON.stringify(result,null,2));
    console.log(JSON.stringify(result));
  } finally { await browser.close(); }
})().catch(e => {console.error(e);process.exitCode=1});
