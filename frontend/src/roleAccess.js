export const publicPages = ['/login', '/register', '/demo']
export function roleHome(role) {
  return { user: '/dashboard', merchant: '/merchant/products', admin: '/admin/dashboard' }[role] || '/login'
}
export function canVisit(role, path) {
  if (publicPages.includes(path)) return true
  if (!['user', 'merchant', 'admin'].includes(role)) return false
  if (path === '/profile') return true
  if (/^\/admin(?:\/|$)/.test(path)) return role === 'admin'
  if (/^\/merchant(?:\/|$)/.test(path)) return role === 'merchant'
  return role === 'user' && /^\/(dashboard|pets|search|reminders|photos|logs|shop|community|ai)(?:\/|$)/.test(path)
}
