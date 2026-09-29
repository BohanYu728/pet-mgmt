<script setup>
import { onMounted, ref, provide } from 'vue'
import { useRouter } from 'vue-router'
import { api } from './api'

const router = useRouter()
const user = ref(null)
const menuOpen = ref(false)
async function refreshUser() {
  try { user.value = (await api.get('/auth/profile')).data.user }
  catch { user.value = null }
}
async function logout() {
  await api.post('/auth/logout')
  user.value = null
  router.push('/login')
}
provide('session', { user, refreshUser })
onMounted(refreshUser)

const links = [
  { to: '/dashboard', icon: '⌂', label: '首页' },
  { to: '/pets', icon: '♡', label: '我的宠物' },
  { to: '/search', icon: '⌕', label: '搜索档案' },
  { to: '/reminders', icon: '◷', label: '提醒' },
  { to: '/logs', icon: '▤', label: '日常日志' },
  { to: '/photos', icon: '▧', label: '宠物相册' },
  { to: '/community/hospitals', icon: '✚', label: '宠物医院' },
  { to: '/shop/products', icon: '▣', label: '宠物商城' },
  { to: '/shop/cart', icon: '◫', label: '购物车' },
  { to: '/shop/orders', icon: '▤', label: '我的订单' },
  { to: '/ai/assistant', icon: '✦', label: '宠物管家助手' },
  { to: '/ai/breed', icon: '◈', label: '品种识别' },
  { to: '/ai/emotion', icon: '☻', label: '情绪分析' },
  { to: '/ai/health', icon: '♡', label: '健康报告' },
  { to: '/ai/recommend', icon: '✧', label: '商品推荐' },
  { to: '/ai/consultation', icon: '◉', label: '症状咨询' }
]
</script>

<template>
  <div class="site-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/dashboard"><span class="brand-mark">🐾</span><span>宠物管家<small>陪伴每个毛孩子的日常</small></span></RouterLink>
      <button class="mobile-menu" @click="menuOpen = !menuOpen" aria-label="打开导航">☰</button>
      <nav class="top-actions">
        <RouterLink v-if="user?.role === 'merchant'" to="/merchant/products">商户中心</RouterLink>
        <RouterLink v-if="user?.role === 'admin'" to="/admin/dashboard">管理后台</RouterLink>
        <RouterLink v-if="user" to="/profile">{{ user.username }}</RouterLink>
        <button v-if="user" class="text-button" @click="logout">退出登录</button>
        <RouterLink v-else to="/login">登录 / 注册</RouterLink>
      </nav>
    </header>
    <div class="content-shell">
      <aside class="sidebar" :class="{ open: menuOpen }" @click="menuOpen = false">
        <div class="sidebar-caption">日常照护</div>
        <RouterLink v-for="link in links.slice(0, 10)" :key="link.to" :to="link.to"><span class="nav-icon">{{ link.icon }}</span>{{ link.label }}</RouterLink>
        <div class="sidebar-caption">智能服务</div>
        <RouterLink v-for="link in links.slice(10)" :key="link.to" :to="link.to"><span class="nav-icon">{{ link.icon }}</span>{{ link.label }}</RouterLink>
      </aside>
      <main class="main-content"><RouterView :key="$route.fullPath" /></main>
    </div>
    <footer>© {{ new Date().getFullYear() }} 宠物管家 · 用心记录每一天</footer>
  </div>
</template>
