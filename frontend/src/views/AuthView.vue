<script setup>
import { computed, inject, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api, errorMessage, unwrap, imageUrl } from '../api'

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
      router.push(result?.user?.role === 'merchant' ? '/merchant/products' : '/dashboard')
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
  <div class="auth-card">
    <div class="page-head"><div><h1>{{ isProfile ? '个人资料' : mode === 'register' ? '加入宠物管家' : '欢迎回来' }}</h1><p>{{ isProfile ? '管理您的账号信息与头像' : mode === 'register' ? '为毛孩子建立一份完整的成长档案' : '登录后继续记录毛孩子的每一天' }}</p></div></div>
    <div v-if="message" class="message">{{ message }}</div><div v-if="failure" class="message error">{{ failure }}</div>
    <form class="card form-grid" @submit.prevent="submit">
      <label class="field"><span>用户名</span><input v-model.trim="form.username" required maxlength="64" autocomplete="username"></label>
      <label v-if="mode !== 'login'" class="field"><span>邮箱</span><input v-model.trim="form.email" type="email" required autocomplete="email"></label>
      <label v-if="mode !== 'login'" class="field"><span>手机号</span><input v-model.trim="form.phone" type="tel" autocomplete="tel"></label>
      <label v-if="!isProfile" class="field"><span>密码</span><input v-model="form.password" type="password" required minlength="6" :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"></label>
      <template v-if="mode === 'register'">
        <label class="field"><span>确认密码</span><input v-model="form.confirm_password" type="password" required minlength="6" autocomplete="new-password"></label>
        <label class="field"><span>注册类型</span><select v-model="form.role"><option value="user">宠物主人</option><option value="merchant">商户</option></select></label>
        <template v-if="form.role === 'merchant'"><label class="field"><span>店铺名称</span><input v-model.trim="form.store_name" required></label></template>
      </template>
      <div class="field full"><button class="btn" :disabled="busy">{{ busy ? '请稍候…' : isProfile ? '保存资料' : mode === 'register' ? '立即注册' : '登录' }}</button></div>
    </form>
    <p v-if="mode === 'login'" class="small muted">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
    <p v-if="mode === 'register'" class="small muted">已有账号？<RouterLink to="/login">前往登录</RouterLink></p>
    <div v-if="isProfile" class="two-col" style="margin-top:20px">
      <div class="card"><h3>头像</h3><img v-if="session.user.value?.avatar" :src="imageUrl(session.user.value.avatar)" alt="我的头像" style="width:90px;height:90px;object-fit:cover;border-radius:50%"><p><input type="file" accept="image/*" @change="uploadAvatar"></p></div>
      <form class="card" @submit.prevent="changePassword"><h3>修改密码</h3><label class="field"><span>原密码</span><input v-model="form.old_password" type="password" required></label><label class="field" style="margin-top:12px"><span>新密码</span><input v-model="form.new_password" type="password" minlength="6" required></label><button class="btn secondary" style="margin-top:15px">更新密码</button></form>
    </div>
  </div>
</template>
