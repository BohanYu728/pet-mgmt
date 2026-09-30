<script setup>
import { computed, inject, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api, errorMessage, unwrap, imageUrl } from '../api'
import AppIcon from '../components/AppIcon.vue'
import AppPhoto from '../components/AppPhoto.vue'
import { roleHome } from '../roleAccess'

const props = defineProps({ mode: String })
const router = useRouter()
const session = inject('session')
const form = reactive({ username: '', email: '', password: '', confirm_password: '', phone: '', role: 'user', store_name: '', store_phone: '', old_password: '', new_password: '' })
const message = ref('')
const failure = ref('')
const busy = ref(false)
const isProfile = computed(() => props.mode === 'profile')

async function loadProfile() {
  if (!isProfile.value) return
  try {
    const u = (await api.get('/auth/profile')).data.user
    Object.assign(form, { username: u.username || '', email: u.email || '', phone: u.phone || '', store_name: u.store_name || '', store_phone: u.store_phone || '' })
  } catch (e) { failure.value = errorMessage(e) }
}
onMounted(loadProfile)
watch(() => props.mode, loadProfile)

async function submit() {
  message.value = ''; failure.value = ''; busy.value = true
  try {
    if (isProfile.value) {
      unwrap(await api.put('/auth/profile', { username: form.username, email: form.email, phone: form.phone }))
      await session.refreshUser()
      message.value = '个人资料已更新'
    } else if (props.mode === 'register') {
      if (form.password !== form.confirm_password) throw new Error('两次输入的密码不一致')
      const path = form.role === 'merchant' ? '/auth/merchant/register' : '/auth/register'
      unwrap(await api.post(path, { username: form.username, email: form.email, password: form.password, phone: form.phone, storeName: form.store_name }))
      message.value = form.role === 'merchant' ? '商户已注册，请等待管理员审核后登录。' : '注册成功，请登录。'
      setTimeout(() => router.push('/login'), 800)
    } else {
      const result = unwrap(await api.post('/auth/login', { username: form.username, password: form.password }))
      await session.refreshUser()
      router.push(roleHome(result?.user?.role))
    }
  } catch (e) { failure.value = e?.response ? errorMessage(e) : e.message }
  finally { busy.value = false }
}
async function changePassword() {
  message.value = ''; failure.value = ''
  try { unwrap(await api.put('/auth/password', { old_password: form.old_password, new_password: form.new_password })); form.old_password = ''; form.new_password = ''; message.value = '密码已修改' }
  catch (e) { failure.value = errorMessage(e) }
}
async function uploadAvatar(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const data = new FormData(); data.append('avatar', file)
  try { unwrap(await api.post('/auth/avatar', data)); await session.refreshUser(); message.value = '头像已更新' }
  catch (e) { failure.value = errorMessage(e) }
}
</script>

<template>
  <div v-if="!isProfile" class="auth-layout">
    <section class="auth-story" aria-label="宠物管家介绍">
      <AppPhoto class="story-photo" src="/images/cat-lifestyle.jpg" alt="一只猫咪" loading="eager" />
      <div class="story-content"><span class="story-label"><AppIcon name="paw" :size="17" /> 宠物管家</span><h2>陪伴，从认真照顾<br>每一天开始。</h2><p>记录成长、安排照护，把和毛孩子的生活放在心上。</p></div>
    </section>
    <section class="auth-panel" :aria-labelledby="mode === 'register' ? 'register-title' : 'login-title'">
      <div class="auth-panel-inner">
        <span class="panel-kicker">{{ mode === 'register' ? 'CREATE ACCOUNT' : 'WELCOME BACK' }}</span>
        <h1 :id="mode === 'register' ? 'register-title' : 'login-title'">{{ mode === 'register' ? '加入宠物管家' : '欢迎回来' }}</h1>
        <p class="panel-intro">{{ mode === 'register' ? '为毛孩子建立一份完整的成长档案' : '登录后继续记录毛孩子的每一天' }}</p>
        <div v-if="message" class="message" role="status">{{ message }}</div><div v-if="failure" class="message error" role="alert">{{ failure }}</div>
        <form class="auth-form" @submit.prevent="submit">
          <label class="field"><span>用户名</span><input v-model.trim="form.username" required maxlength="64" autocomplete="username" placeholder="请输入用户名"></label>
          <label v-if="mode === 'register'" class="field"><span>邮箱</span><input v-model.trim="form.email" type="email" required autocomplete="email" placeholder="用于接收账号信息"></label>
          <label v-if="mode === 'register'" class="field"><span>手机号</span><input v-model.trim="form.phone" type="tel" autocomplete="tel" placeholder="选填"></label>
          <label class="field"><span>密码</span><input v-model="form.password" type="password" required minlength="6" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'" placeholder="至少 6 位字符"></label>
          <template v-if="mode === 'register'"><label class="field"><span>确认密码</span><input v-model="form.confirm_password" type="password" required minlength="6" autocomplete="new-password" placeholder="再次输入密码"></label><label class="field"><span>注册类型</span><select v-model="form.role"><option value="user">宠物主人</option><option value="merchant">商户</option></select></label><label v-if="form.role === 'merchant'" class="field"><span>店铺名称</span><input v-model.trim="form.store_name" required placeholder="请输入店铺名称"></label></template>
          <button class="btn submit-button" :disabled="busy">{{ busy ? '请稍候…' : mode === 'register' ? '立即注册' : '登录' }} <AppIcon name="arrow" :size="17" /></button>
        </form>
        <p v-if="mode === 'login'" class="switch-mode">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
        <p v-if="mode === 'register'" class="switch-mode">已有账号？<RouterLink to="/login">前往登录</RouterLink></p>
      </div>
    </section>
  </div>

  <div v-else class="profile-page">
    <header class="profile-heading"><span class="panel-kicker">ACCOUNT / 我的账号</span><h1>个人资料</h1><p>管理您的账号信息与头像，让照护记录始终属于您。</p></header>
    <div v-if="message" class="message" role="status">{{ message }}</div><div v-if="failure" class="message error" role="alert">{{ failure }}</div>
    <div class="profile-grid">
      <div class="profile-main">
        <div class="profile-identity"><AppPhoto class="profile-avatar" :src="session.user.value?.avatar ? imageUrl(session.user.value.avatar) : ''" :alt="session.user.value?.avatar ? '我的头像' : '默认头像'" /><div><span>账号资料</span><strong>{{ session.user.value?.username || '宠物主人' }}</strong><small>维护联系方式，方便管理您的宠物生活。</small></div></div>
        <form class="profile-form" @submit.prevent="submit"><div class="card-heading"><span class="heading-icon"><AppIcon name="user" :size="21" /></span><div><h2>基本信息</h2><p>更新您的名称和联系方式</p></div></div><div class="profile-fields"><label class="field"><span>用户名</span><input v-model.trim="form.username" required maxlength="64" autocomplete="username"></label><label class="field"><span>邮箱</span><input v-model.trim="form.email" type="email" required autocomplete="email"></label><label class="field"><span>手机号</span><input v-model.trim="form.phone" type="tel" autocomplete="tel"></label></div><button class="btn save-button" :disabled="busy">{{ busy ? '请稍候…' : '保存资料' }}</button></form>
      </div>
      <div class="profile-side"><section class="profile-card"><div class="card-heading"><span class="heading-icon"><AppIcon name="photo" :size="21" /></span><div><h2>头像</h2><p>使用您喜欢的照片</p></div></div><div class="avatar-upload"><AppPhoto class="upload-preview" :src="session.user.value?.avatar ? imageUrl(session.user.value.avatar) : ''" :alt="session.user.value?.avatar ? '我的头像' : '默认头像'" /><label class="upload-label"><span>上传新头像</span><input type="file" accept="image/*" @change="uploadAvatar"></label></div></section><form class="profile-card" @submit.prevent="changePassword"><div class="card-heading"><span class="heading-icon"><AppIcon name="shield" :size="21" /></span><div><h2>修改密码</h2><p>定期更新以保护账号</p></div></div><label class="field"><span>原密码</span><input v-model="form.old_password" type="password" required autocomplete="current-password"></label><label class="field"><span>新密码</span><input v-model="form.new_password" type="password" minlength="6" required autocomplete="new-password"></label><button class="btn secondary password-button">更新密码</button></form></div>
    </div>
  </div>
</template>

<style scoped>
.auth-layout{min-height:min(740px,calc(100vh - 120px));display:grid;grid-template-columns:minmax(0,1.05fr) minmax(430px,.95fr);background:#fff;border-radius:24px;overflow:hidden;box-shadow:0 20px 65px #2532470d}.auth-story{position:relative;min-height:650px;background:#ddebed;overflow:hidden}.auth-story img{width:100%;height:100%;position:absolute;inset:0;object-fit:cover;object-position:center;display:block}.auth-story:after{content:"";position:absolute;inset:35% 0 0;background:linear-gradient(transparent,#172d3fbb)}.story-content{position:absolute;z-index:1;bottom:50px;left:clamp(32px,5vw,72px);right:35px;color:white}.story-label{display:inline-flex;align-items:center;gap:8px;font-size:12px;font-weight:700;letter-spacing:.12em}.story-content h2{color:#fff;font-size:clamp(27px,3vw,42px);line-height:1.35;letter-spacing:-.04em;margin:17px 0 12px}.story-content p{font-size:14px;line-height:1.7;margin:0;color:#f1f6f9}.auth-panel{display:grid;place-items:center;padding:40px clamp(30px,5vw,72px)}.auth-panel-inner{width:min(100%,420px)}.panel-kicker{color:var(--accent,#2a7e86);font-size:11px;font-weight:800;letter-spacing:.16em}.auth-panel h1,.profile-heading h1{color:var(--ink,#253247);font-size:clamp(30px,3vw,40px);letter-spacing:-.045em;line-height:1.2;margin:12px 0}.panel-intro,.profile-heading p{color:var(--muted,#738195);font-size:14px;line-height:1.7;margin:0 0 30px}.auth-form{display:grid;gap:16px}.auth-form .field,.profile-fields .field,.profile-card .field{gap:7px;color:#45576b;font-size:13px;font-weight:700}.auth-form input,.auth-form select,.profile-page input{min-height:45px}.auth-form input::placeholder{color:#a4afba}.submit-button{min-height:46px;width:100%;justify-content:center;margin-top:6px}.switch-mode{text-align:center;color:var(--muted,#738195);font-size:13px;margin:24px 0 0}.switch-mode a{font-weight:800;color:var(--brand,#c75f20)}.profile-page{max-width:1120px;margin:0 auto}.profile-heading{margin:4px 0 26px}.profile-heading p{margin-bottom:0}.profile-grid{display:grid;grid-template-columns:minmax(0,1.4fr) minmax(290px,.8fr);gap:20px}.profile-main,.profile-side{display:grid;align-content:start;gap:20px}.profile-identity,.profile-form,.profile-card{background:#fff;border:1px solid var(--line,#e5ebf1);border-radius:19px;padding:25px;box-shadow:0 7px 25px #25324708}.profile-identity{display:flex;align-items:center;gap:19px}.profile-avatar{height:74px;width:74px;flex:none;border-radius:18px;overflow:hidden;background:#edf3f7}.profile-identity>div{display:flex;flex-direction:column;gap:5px;min-width:0}.profile-identity span{font-size:11px;color:var(--accent,#2a7e86);font-weight:800}.profile-identity strong{color:var(--ink,#253247);font-size:19px}.profile-identity small{color:var(--muted,#738195);font-size:12px;line-height:1.5}.card-heading{display:flex;align-items:center;gap:12px;margin-bottom:23px}.heading-icon{height:42px;width:42px;display:grid;place-items:center;border-radius:12px;background:#eaf3f8;color:var(--accent,#2a7e86);flex:none}.card-heading h2{font-size:17px;color:var(--ink,#253247);margin:0}.card-heading p{font-size:12px;color:var(--muted,#738195);margin:4px 0 0}.profile-fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:17px}.profile-fields .field:nth-child(3){grid-column:1/-1}.save-button{margin-top:24px}.avatar-upload{display:flex;align-items:center;gap:16px}.upload-preview{height:69px;width:69px;flex:none;border-radius:50%;overflow:hidden;background:#edf3f7}.upload-label{font-size:12px;color:var(--accent,#2a7e86);font-weight:700;min-width:0}.upload-label input{display:block;width:100%;max-width:210px;margin-top:8px;font-size:11px;color:var(--muted,#738195)}.profile-card .field+.field{margin-top:14px}.password-button{margin-top:18px}
@media(max-width:900px){.auth-layout{grid-template-columns:minmax(0,.8fr) minmax(0,1fr)}.auth-story{min-height:600px}.profile-grid{grid-template-columns:1fr}}
@media(max-width:700px){.auth-layout{grid-template-columns:1fr;min-height:0}.auth-story{min-height:170px;max-height:170px}.auth-story img{object-position:center 40%}.auth-story:after{inset:0;background:linear-gradient(90deg,#172d3f88,transparent)}.story-content{left:23px;right:18px;bottom:19px}.story-content h2{font-size:22px;margin:8px 0 0}.story-content p{display:none}.auth-panel{padding:32px 25px 38px}.panel-intro{margin-bottom:24px}}
@media(max-width:480px){.profile-identity,.profile-form,.profile-card{padding:20px}.profile-fields{grid-template-columns:1fr}.profile-fields .field:nth-child(3){grid-column:auto}.auth-panel{padding:28px 21px 35px}.auth-story{min-height:148px;max-height:148px}.story-content h2{font-size:20px}.profile-identity{gap:13px}.profile-avatar{width:58px;height:58px}}
.profile-avatar :deep(.photo-empty),.upload-preview :deep(.photo-empty){min-height:0}.profile-avatar :deep(.photo-empty span),.upload-preview :deep(.photo-empty span){display:none}.story-photo{position:absolute;inset:0;width:100%;height:100%}.story-photo :deep(img){object-position:center}@media(max-width:700px){.story-photo :deep(img){object-position:center 40%}}
</style>
