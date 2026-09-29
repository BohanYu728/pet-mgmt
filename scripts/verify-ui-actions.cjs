// Destructive UI actions run only against the isolated, disposable demo profile.
// Start the demo backend on :8080 and the Vite frontend on :5173 before running.
const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '..');
const evidence = path.join(root, 'docs/evidence');
const origin = 'http://127.0.0.1:5173';
const checks = [];
const errors = [];
const suffix = Date.now().toString().slice(-8);
let browser;

async function check(name, action) {
  await action();
  checks.push({ name, passed: true });
  console.log('PASS', name);
}

async function open(page, route) {
  await page.goto(origin + route, { waitUntil: 'networkidle', timeout: 20000 });
  const error = await page.locator('main .message.error').allTextContents();
  assert.deepEqual(error, [], `${route}: ${error.join(' / ')}`);
}

async function login(page, username, password) {
  await open(page, '/login');
  await page.getByLabel('用户名', { exact: true }).fill(username);
  await page.getByLabel('密码', { exact: true }).fill(password);
  await Promise.all([
    page.waitForURL(/\/(dashboard|merchant\/products)$/, { timeout: 20000 }),
    page.getByRole('button', { name: '登录', exact: true }).click()
  ]);
}

function createPage(context) {
  return context.newPage().then(page => {
    page.setDefaultTimeout(12000);
    page.on('pageerror', error => errors.push(error.message));
    page.on('dialog', dialog => dialog.accept());
    return page;
  });
}

async function adminFlow(page) {
  const name = `ui_check_${suffix}`;
  await login(page, 'admin', 'admin123');
  await open(page, '/admin/users');
  await check('管理员在页面新增用户', async () => {
    await page.getByRole('button', { name: /新增用户/ }).click();
    const modal = page.locator('.modal');
    await modal.getByLabel('用户名').fill(name);
    await modal.getByLabel('邮箱').fill(`${name}@example.invalid`);
    await modal.getByLabel('密码').fill('Check123!');
    await modal.getByRole('button', { name: '保存' }).click();
    await page.getByRole('row').filter({ hasText: name }).waitFor();
    assert.equal(await page.locator('main .message.error').count(), 0);
  });
  await check('管理员在页面修改用户', async () => {
    const row = page.getByRole('row').filter({ hasText: name });
    await row.getByRole('button', { name: '编辑' }).click();
    const modal = page.locator('.modal');
    await modal.getByLabel('电话').fill('13800000000');
    await modal.getByRole('button', { name: '保存' }).click();
    await page.getByRole('row').filter({ hasText: '13800000000' }).waitFor();
    assert.equal(await page.locator('main .message.error').count(), 0);
  });
  await check('管理员在页面删除测试用户', async () => {
    await page.getByRole('row').filter({ hasText: name }).getByRole('button', { name: '删除' }).click();
    await page.getByRole('row').filter({ hasText: name }).waitFor({ state: 'detached' });
    assert.equal(await page.locator('main .message.error').count(), 0);
  });
}

async function petFlow(page) {
  const name = `联调猫咪${suffix}`;
  const renamed = `成长档案${suffix}`;
  await login(page, 'demo', 'demo123');
  await open(page, '/pets');
  await check('宠物主人在页面新增宠物', async () => {
    await page.getByRole('button', { name: /添加宠物/ }).click();
    const modal = page.locator('.modal');
    await modal.getByLabel('昵称 *').fill(name);
    await modal.getByLabel('种类 *').selectOption('cat');
    await modal.getByLabel('品种').fill('英短');
    await modal.getByRole('button', { name: '保存档案' }).click();
    await page.locator('main .grid .card').filter({ hasText: name }).waitFor();
    assert.equal(await page.locator('main .message.error').count(), 0);
  });
  await check('宠物主人在页面修改宠物', async () => {
    await page.locator('main .grid .card').filter({ hasText: name }).getByRole('button', { name: '编辑' }).click();
    const modal = page.locator('.modal');
    await modal.getByLabel('昵称 *').fill(renamed);
    await modal.getByRole('button', { name: '保存档案' }).click();
    await page.locator('main .grid .card').filter({ hasText: renamed }).waitFor();
    assert.equal(await page.locator('main .message.error').count(), 0);
  });
  await check('宠物主人在页面删除测试宠物', async () => {
    await page.locator('main .grid .card').filter({ hasText: renamed }).getByRole('button', { name: '删除' }).click();
    await page.locator('main .grid .card').filter({ hasText: renamed }).waitFor({ state: 'detached' });
    assert.equal(await page.locator('main .message.error').count(), 0);
  });
}

async function shopFlow(page) {
  await open(page, '/shop/products');
  await check('买家在页面加入购物车', async () => {
    const button = page.getByRole('button', { name: '加入购物车' }).first();
    await button.waitFor();
    assert.equal(await button.isDisabled(), false, '演示商品应有库存');
    await button.click();
    await page.getByText(/已加入购物车/).waitFor();
    assert.equal(await page.locator('main .message.error').count(), 0);
  });
  await open(page, '/shop/cart');
  await check('买家在页面提交订单', async () => {
    assert.ok(await page.locator('main .list-row').count() > 0, '购物车应包含刚添加的商品');
    await page.getByLabel('收件人').fill('UI 验收');
    await page.getByLabel('联系电话').fill('13800000000');
    await page.getByLabel('省份').fill('北京');
    await page.getByLabel('城市').fill('北京');
    await page.getByLabel('详细地址').fill('演示环境测试地址');
    await page.getByRole('button', { name: '确认下单' }).click();
    await page.getByText(/下单成功/).waitFor();
    assert.equal(await page.locator('main .message.error').count(), 0);
  });
  await open(page, '/shop/orders');
  await check('买家在页面取消测试订单', async () => {
    const row = page.locator('main .card .list-row').first();
    const orderText = await row.innerText();
    assert.match(orderText, /订单 #\d+/);
    assert.match(orderText, /待付款/);
    await row.getByRole('button', { name: '取消订单' }).click();
    await page.locator('main .card .list-row').first().getByText('已取消').waitFor();
    assert.equal(await page.locator('main .message.error').count(), 0);
  });
}

(async () => {
  const health = await Promise.all([
    fetch(origin).then(response => response.ok),
    fetch('http://127.0.0.1:8080/api/hospitals').then(response => response.ok)
  ]);
  assert.ok(health.every(Boolean), 'Start both demo services before UI action checks');
  browser = await chromium.launch({ channel: 'chrome', headless: true });
  const admin = await createPage(await browser.newContext({ viewport: { width: 1365, height: 900 } }));
  await adminFlow(admin);
  const demo = await createPage(await browser.newContext({ viewport: { width: 1365, height: 900 } }));
  await petFlow(demo);
  await shopFlow(demo);
  assert.deepEqual(errors, [], 'Uncaught browser errors');
})().catch(error => {
  console.error(error.stack);
  checks.push({ name: 'UI 操作验收中断', passed: false, error: error.message });
  process.exitCode = 1;
}).finally(async () => {
  if (browser) await browser.close();
  fs.mkdirSync(evidence, { recursive: true });
  fs.writeFileSync(path.join(evidence, 'ui-actions-verification.json'), JSON.stringify({ at: new Date().toISOString(), environment: 'isolated H2 demo', checks, errors }, null, 2));
  console.log(`${checks.filter(item => item.passed).length}/${checks.length} UI action checks passed`);
});
