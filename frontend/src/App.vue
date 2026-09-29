<script setup>
import { computed, onMounted, onUnmounted, ref, provide, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { api, errorMessage, imageUrl } from './api'
import AppIcon from './components/AppIcon.vue'
import AppPhoto from './components/AppPhoto.vue'
const router = useRouter(), route = useRoute()
const user = ref(null), menuOpen = ref(false), logoutError = ref('')
const authPage = computed(() => ['/login', '/register'].includes(route.path))
async function refreshUser() { try { user.value = (await api.get('/auth/profile')).data.user } catch { user.value = null } }
async function logout() { try { await api.post('/auth/logout'); user.value = null; router.push('/login') } catch (error) { logoutError.value = errorMessage(error) } }
provide('session', { user, refreshUser })
function escapeMenu(event) { if (event.key === 'Escape') menuOpen.value = false }
onMounted(() => { refreshUser(); window.addEventListener('keydown', escapeMenu) })
onUnmounted(() => window.removeEventListener('keydown', escapeMenu))
watch(() => route.fullPath, () => { menuOpen.value = false; logoutError.value = '' })
const groups = [
  { title: '日常照护', links: [['/dashboard','home','照护首页'],['/pets','paw','我的宠物'],['/search','search','搜索档案'],['/reminders','calendar','照护提醒'],['/logs','book','日常日志'],['/photos','photo','宠物相册']] },
  { title: '宠物生活', links: [['/community/hospitals','hospital','宠物医院'],['/shop/products','shop','宠物商城'],['/shop/cart','bag','购物车'],['/shop/orders','orders','我的订单']] },
  { title: '智能服务', links: [['/ai/assistant','sparkles','宠物管家助手'],['/ai/breed','scan','品种识别'],['/ai/emotion','smile','情绪分析'],['/ai/health','heart','健康报告'],['/ai/recommend','bag','商品推荐'],['/ai/consultation','chat','症状咨询']] }
]
const roleLinks = computed(() => user.value?.role === 'admin' ? [['/admin/dashboard','chart','管理概览'],['/admin/users','user','用户管理'],['/admin/pets','paw','宠物管理'],['/admin/merchants','shield','商户审核']] : user.value?.role === 'merchant' ? [['/merchant/products','shop','商品管理'],['/merchant/orders','orders','商户订单'],['/merchant/billing','book','租金账单']] : [])
const currentTitle = computed(() => [...groups.flatMap(group => group.links), ...roleLinks.value, ['/profile','user','个人资料']].find(link => link[0] === route.path)?.[2] || '宠物档案')
const roleLabel = computed(() => ({ admin: '管理员', merchant: '商户', user: '宠物主人' }[user.value?.role] || '欢迎来访'))
</script>
<template>
  <div class="site-shell" :class="{ 'auth-shell': authPage }">
    <a class="skip-link" href="#main-content">跳转到主要内容</a>
    <header class="topbar">
      <RouterLink class="brand" to="/dashboard"><span class="brand-mark"><AppIcon name="paw" :size="27"/></span><span>宠物管家<small>PET COMPANION</small></span></RouterLink>
      <div v-if="!authPage" class="top-context"><span>生活有它，日常有序</span><i>/</i><strong>{{ currentTitle }}</strong></div>
      <nav class="top-actions" aria-label="账号导航">
        <RouterLink v-if="user?.role === 'merchant'" class="role-link" to="/merchant/products">商户中心 <AppIcon name="arrow" :size="15"/></RouterLink>
        <RouterLink v-if="user?.role === 'admin'" class="role-link" to="/admin/dashboard">管理后台 <AppIcon name="arrow" :size="15"/></RouterLink>
        <RouterLink v-if="user" class="account-link" to="/profile"><AppPhoto v-if="user.avatar" class="account-avatar" :src="imageUrl(user.avatar)" alt="我的头像"/><span v-else class="account-monogram">{{ user.username?.slice(0, 1).toUpperCase() }}</span><span class="account-name">{{ user.username }}<small>{{ roleLabel }}</small></span></RouterLink>
        <button v-if="user" class="icon-button logout-button" @click="logout" aria-label="退出登录" title="退出登录"><AppIcon name="logout"/></button>
        <RouterLink v-else class="btn secondary account-entry" :to="authPage && route.path === '/login' ? '/register' : '/login'">{{ authPage && route.path === '/login' ? '创建账号' : '登录 / 注册' }} <AppIcon name="arrow" :size="16"/></RouterLink>
        <button v-if="!authPage" class="mobile-menu icon-button" @click="menuOpen = !menuOpen" :aria-expanded="menuOpen" aria-controls="site-navigation" :aria-label="menuOpen ? '关闭导航' : '打开导航'"><AppIcon :name="menuOpen ? 'close' : 'menu'"/></button>
      </nav>
    </header>
    <div class="content-shell">
      <button v-if="menuOpen && !authPage" class="menu-backdrop" aria-label="关闭导航菜单" @click="menuOpen = false"></button>
      <aside v-if="!authPage" id="site-navigation" class="sidebar" :class="{ open: menuOpen }"><nav aria-label="主要导航">
        <div v-if="roleLinks.length" class="nav-group"><div class="sidebar-caption">{{ user?.role === 'admin' ? '管理工作台' : '店铺工作台' }}</div><RouterLink v-for="[to, icon, label] in roleLinks" :key="to" :to="to"><AppIcon :name="icon" :size="19"/><span>{{ label }}</span><span class="active-dot"></span></RouterLink></div>
        <div v-for="group in groups" :key="group.title" class="nav-group"><div class="sidebar-caption">{{ group.title }}</div><RouterLink v-for="[to, icon, label] in group.links" :key="to" :to="to"><AppIcon :name="icon" :size="19"/><span>{{ label }}</span><span class="active-dot"></span></RouterLink></div>
      </nav><div class="sidebar-note"><AppIcon name="heart" :size="17"/><span>把每一份陪伴<br>记在日常里</span></div></aside>
      <main id="main-content" class="main-content" tabindex="-1"><div v-if="logoutError" class="message error" role="alert">{{ logoutError }}</div><RouterView :key="$route.fullPath"/></main>
    </div>
    <footer><span class="footer-brand"><AppIcon name="paw" :size="15"/> 宠物管家</span><span>认真照顾，自在陪伴</span><span>© {{ new Date().getFullYear() }}</span></footer>
  </div>
</template>
