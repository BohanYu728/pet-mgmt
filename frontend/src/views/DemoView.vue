<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, unwrap, errorMessage } from '../api'
import AppPhoto from '../components/AppPhoto.vue'
import { aiSamples } from '../aiSamples'
const session = inject('session'), router = useRouter()
const demo = ref(null), busy = ref(false), failure = ref('')
const roles = [
  { role: 'user', label: '宠物主人', image: 'dog-lifestyle', intro: '从旺财和咪咪的一天开始', steps: '查看档案 → 记录体重与照护 → 浏览商品并下单', route: '/dashboard' },
  { role: 'merchant', label: '商户', image: 'product-food', intro: '经营一家毛孩子生活馆', steps: '维护商品 → 处理样例订单 → 查看租金账单', route: '/merchant/products' },
  { role: 'admin', label: '管理员', image: 'cat-lifestyle', intro: '了解平台如何有序运转', steps: '查看统计 → 管理用户和宠物 → 审核新商户', route: '/admin/dashboard' }
]
const scenes = [
  ['日常照护', 'user', '/pets', '内置两只宠物、疫苗和驱虫记录。进入宠物档案继续添加自己的记录。'],
  ['成长趋势', 'user', '/pets/1/weights', '旺财已有三次称重样例，新增体重后即可观察趋势变化。'],
  ['生活日志', 'user', '/logs', '查看并编辑样例日志，也可以新建一条今天的记录。'],
  ['商城与订单', 'user', '/shop/orders', '已有待付款样例订单，可查看明细或取消；也可从商城重新下单。'],
  ['智能问答', 'user', '/ai/assistant', '试问“如何记录宠物体重？”查看项目资料与引用；未配置模型时显示资料检索结果。'],
  ['商品与库存', 'merchant', '/merchant/products', '三件样例商品可编辑；修改价格或库存后，在商城查看变化。'],
  ['订单处理', 'merchant', '/merchant/orders', '对样例订单体验状态流转。状态变化仅用于演示，未接入真实支付。'],
  ['账单查询', 'merchant', '/merchant/billing', '查看一笔虚构月租金账单，不会发生实际扣款。'],
  ['平台管理', 'admin', '/admin/users', '查看演示账号并体验新增用户；商户审核可配合注册页新建申请。']
]
const visibleScenes = computed(() => scenes.filter(scene => scene[1] === session.user.value?.role))
onMounted(async () => { try { demo.value = (await api.get('/demo/status')).data } catch (e) { failure.value = errorMessage(e) } })
async function enter(role, path) {
  if (busy.value || !demo.value?.enabled) return
  busy.value = true; failure.value = ''
  try {
    const account = demo.value.accounts.find(a => a.role === role)
    if (!account) throw new Error('演示账号暂不可用')
    unwrap(await api.post('/auth/login', { username: account.username, password: account.password }))
    await session.refreshUser(); await router.push(path)
  } catch (e) { failure.value = e.response ? errorMessage(e) : e.message }
  finally { busy.value = false }
}
</script>

<template>
  <div class="demo-page">
    <header class="demo-heading"><span class="eyebrow">PET LIFE / 样例体验</span><h1>带着样例，体验每一天。</h1><p>选择一个角色，从真实可操作的业务页面开始。返回这里，还可以继续体验其他场景。</p></header>
    <div v-if="failure" class="message error" role="alert">{{ failure }}</div>
    <p v-if="!demo && !failure" role="status">正在读取演示环境…</p>
    <div v-if="demo && !demo.enabled" class="card">当前为正式数据环境，快捷演示账号未启用。请使用自己的账号登录。</div>
    <template v-if="demo?.enabled">
      <div class="demo-notice">本地演示环境 · 账号、商品、地址与订单均为样例。修改立即生效，重启 demo 后恢复初始数据。切换体验将切换当前登录账号。</div>
      <section class="demo-roles" aria-label="选择体验角色"><article v-for="role in roles" :key="role.role" class="demo-role"><AppPhoto :src="`/images/${role.image}.jpg`" :alt="role.label + '体验配图'" class="demo-photo"/><div><span class="eyebrow">{{ role.label }}</span><h2>{{ role.intro }}</h2><p>{{ role.steps }}</p><button class="btn" :disabled="busy" @click="enter(role.role, role.route)">{{ busy ? '正在进入…' : '体验' + role.label }}</button></div></article></section>
      <section v-if="visibleScenes.length" aria-labelledby="scenes-title"><h2 id="scenes-title">按场景试一试</h2><div class="demo-scenes"><article v-for="[title, role, path, note] in visibleScenes" :key="title" class="card"><span class="tag">{{ roles.find(r => r.role === role).label }}</span><h3>{{ title }}</h3><p>{{ note }}</p><button class="btn secondary" :disabled="busy" @click="enter(role, path)">打开样例</button></article></div></section>
      <section v-if="session.user.value?.role === 'user'" aria-labelledby="ai-samples-title" style="margin-top:32px"><h2 id="ai-samples-title">AI 样例体验</h2><p class="muted">进入后点击“填入样例”，可编辑输入，再提交查看实际结果。</p><div class="demo-scenes"><article v-for="(sample,key) in aiSamples" :key="key" class="card"><span class="tag">智能服务</span><h3>{{ sample.title }}</h3><p>{{ sample.description }}</p><button class="btn secondary" :disabled="busy" @click="enter('user', '/ai/' + key)">体验{{ {assistant:'资料问答',breed:'品种识别',emotion:'情绪分析',health:'健康报告',recommend:'商品推荐',consultation:'症状咨询'}[key] }}</button></article></div></section>
    </template>
  </div>
</template>

<style scoped>
.demo-heading{max-width:760px;margin:8px 0 26px}.demo-heading h1{font-size:clamp(28px,4vw,42px);letter-spacing:-.04em;margin:12px 0}.demo-heading p,.demo-role p,.demo-scenes p{color:var(--muted);line-height:1.8}.eyebrow{font-size:11px;letter-spacing:.12em;color:var(--accent);font-weight:800}.demo-notice{padding:16px 20px;background:#eaf3f2;border-radius:14px;color:#37635e;line-height:1.7;margin-bottom:24px;font-size:13px}.demo-roles{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px;margin-bottom:38px}.demo-role{border:1px solid var(--line);border-radius:20px;overflow:hidden;background:white}.demo-photo{height:185px;width:100%}.demo-role>div:not(.photo-frame){padding:22px}.demo-photo :deep(img){object-position:center 30%}.demo-role h2{font-size:20px;margin:10px 0}.demo-role p{font-size:13px;min-height:46px}.demo-scenes{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}.demo-scenes .card{display:flex;flex-direction:column;align-items:flex-start}.demo-scenes h3{margin:14px 0 0}.demo-scenes p{font-size:13px;flex:1}.demo-scenes .btn{margin-top:8px}@media(max-width:1000px){.demo-roles,.demo-scenes{grid-template-columns:1fr 1fr}.demo-role:last-child{grid-column:1/-1}}@media(max-width:600px){.demo-roles,.demo-scenes{grid-template-columns:1fr}.demo-role:last-child{grid-column:auto}.demo-photo{height:170px}}
</style>
