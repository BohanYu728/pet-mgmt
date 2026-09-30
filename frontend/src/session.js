import { ref } from 'vue'
import { api } from './api'
export const currentUser = ref(null)
export async function refreshUser() {
  try { currentUser.value = (await api.get('/auth/profile')).data.user; return currentUser.value }
  catch { currentUser.value = null; return null }
}
