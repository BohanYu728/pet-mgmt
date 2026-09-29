import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import AuthView from './views/AuthView.vue'
import DashboardView from './views/DashboardView.vue'
import PetsView from './views/PetsView.vue'
import JournalView from './views/JournalView.vue'
import ShopView from './views/ShopView.vue'
import CommunityView from './views/CommunityView.vue'
import AiView from './views/AiView.vue'
import AdminView from './views/AdminView.vue'
import SearchView from './views/SearchView.vue'
import './style.css'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', component: AuthView, props: { mode: 'login' } },
  { path: '/register', component: AuthView, props: { mode: 'register' } },
  { path: '/profile', component: AuthView, props: { mode: 'profile' } },
  { path: '/dashboard', component: DashboardView },
  { path: '/pets', component: PetsView },
  { path: '/search', component: SearchView },
  { path: '/pets/:id/:section', component: JournalView, props: true },
  { path: '/reminders', component: JournalView, props: { section: 'reminders' } },
  { path: '/photos', component: JournalView, props: { section: 'photos' } },
  { path: '/logs', component: JournalView, props: { section: 'logs' } },
  { path: '/shop/:section?', component: ShopView, props: true },
  { path: '/community/:section?', component: CommunityView, props: true },
  { path: '/ai/:section?', component: AiView, props: true },
  { path: '/admin/:section?', component: AdminView, props: true },
  { path: '/merchant/:section?', component: AdminView, props: true }
]
const router = createRouter({ history: createWebHistory(), routes })
createApp(App).use(router).mount('#app')
