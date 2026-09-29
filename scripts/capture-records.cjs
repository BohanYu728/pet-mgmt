const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname,'..');
const evidence = path.join(root,'docs/evidence');
const esc = value => String(value).replace(/[&<>"']/g, c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const style = `<style>body{margin:0;background:#f1f3f5;font:20px 'Microsoft YaHei',sans-serif;color:#172530}main{margin:40px;padding:32px;background:white;border:1px solid #cad2d8}h1{font-size:28px;margin:0 0 12px}p{color:#536370;font-size:17px;line-height:1.6}pre{white-space:pre-wrap;font:17px Consolas,'Microsoft YaHei',monospace;line-height:1.45;background:#f7f9fa;padding:20px}table{width:100%;border-collapse:collapse;font-size:18px}td,th{padding:13px;text-align:left;border-bottom:1px solid #d9e0e5}th{background:#eaf0f4}.pass{color:#1e624e;font-weight:bold}</style>`;
(async()=>{
  const browser=await chromium.launch({channel:'chrome',headless:true});
  try {
    const page=await browser.newPage({viewport:{width:1300,height:950},deviceScaleFactor:1});
    const log=fs.readFileSync(path.join(evidence,'git-exercise.txt'),'utf8');
    await page.setContent(`${style}<main><h1>Git 分支与冲突练习记录</h1><p>以下内容直接读取本地真实 git 命令日志，使用浏览器排版展示。练习在隔离仓库进行；不代表远程 Pull Request 或云端 CI。</p><pre>${esc(log)}</pre></main>`);
    await page.screenshot({path:path.join(evidence,'git-evidence.png'),fullPage:true});
    const data=JSON.parse(fs.readFileSync(path.join(evidence,'api-verification.json'),'utf8'));
    const rows=data.checks.filter(c=>/CSRF|HttpOnly|普通用户|不同账号|认证失效|令牌不进入|无效评分|输入长度/.test(c.name));
    await page.setContent(`${style}<main><h1>认证与权限的 HTTP 验证记录</h1><p>来源：隔离 H2 演示环境的实际 HTTP 检查。时间 ${esc(data.at)}。此图展示保存的验证结果，不包含 Cookie 或令牌的值。</p><table><tr><th>检查内容</th><th>结果</th><th>响应记录</th></tr>${rows.map(c=>`<tr><td>${esc(c.name)}</td><td class="pass">${c.passed?'通过':'失败'}</td><td>${esc(c.details)}</td></tr>`).join('')}</table></main>`);
    await page.screenshot({path:path.join(evidence,'security-evidence.png'),fullPage:true});
  } finally { await browser.close(); }
})();
