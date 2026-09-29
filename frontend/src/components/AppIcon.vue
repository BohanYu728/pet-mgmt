<script setup>
defineProps({ name: { type: String, default: 'paw' }, size: { type: [Number, String], default: 20 } })
const paths = {
  home: 'm3 10 9-7 9 7v10H15v-7H9v7H3Z',
  heart: 'M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z',
  search: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z',
  calendar: 'M8 2v4m8-4v4M3 10h18M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2ZM8 14h2m4 0h2m-8 4h2',
  clock: 'M12 8v5l3 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z',
  book: 'M12 5v16M3 3h5a4 4 0 0 1 4 3 4 4 0 0 1 4-3h5v17h-5a4 4 0 0 0-4 2 4 4 0 0 0-4-2H3Z',
  photo: 'M4 3h16a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Zm-1 13 5-5 4 4 4-5 5 6M8 7h.01',
  hospital: 'M4 21V5h16v16M9 21v-5h6v5M9 9h6m-3-3v6M2 21h20',
  shop: 'M3 10v11h18V10M2 10l2-7h16l2 7M2 10c0 4 5 4 5 0 0 4 5 4 5 0 0 4 5 4 5 0 0 4 5 4 5 0M9 21v-6h6v6',
  bag: 'M4 7h16l1 14H3L4 7Zm4 0V5a4 4 0 0 1 8 0v2',
  orders: 'M6 3h12v18l-3-2-3 2-3-2-3 2V3Zm3 5h6m-6 4h6',
  sparkles: 'm12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5L12 3ZM20 2v4m-2-2h4',
  scan: 'M8 3H3v5m13-5h5v5M3 16v5h5m13-5v5h-5M7 12h10M12 7v10',
  smile: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0ZM8 9h.01M16 9h.01M8 15c2 3 6 3 8 0',
  chart: 'M3 3v18h18M7 15l4-5 4 3 5-7',
  chat: 'M21 11a9 9 0 0 1-9 9H3l1.5-4.5A9 9 0 1 1 21 11ZM8 10h8m-8 4h5',
  user: 'M20 21v-2a8 8 0 0 0-16 0v2M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
  shield: 'm12 2 9 4v6c0 5-9 10-9 10S3 17 3 12V6l9-4Zm-4 10 3 3 5-6',
  plus: 'M12 5v14M5 12h14', arrow: 'M4 12h16m-6-6 6 6-6 6', chevron: 'm9 5 7 7-7 7', check: 'm5 12 4 4L19 6', close: 'm6 6 12 12M6 18 18 6', menu: 'M4 6h16M4 12h16M4 18h16', logout: 'M9 3H4v18h5m5-14 5 5-5 5M9 12h12', edit: 'm16 3 5 5-12 12-6 1 1-6L16 3Zm-2 2 5 5', trash: 'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7', filter: 'M4 6h16M7 12h10m-7 6h4', location: 'M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0ZM15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z'
}
</script>
<template>
  <svg :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false" class="app-icon">
    <template v-if="name === 'paw' || name === 'pet'"><ellipse cx="5.2" cy="8.7" rx="2.2" ry="2.8" transform="rotate(-25 5.2 8.7)"/><ellipse cx="10" cy="5.5" rx="2" ry="2.7"/><ellipse cx="15.6" cy="6.1" rx="2" ry="2.7" transform="rotate(20 15.6 6.1)"/><ellipse cx="19.2" cy="10.6" rx="2" ry="2.7" transform="rotate(35 19.2 10.6)"/><path d="M6.5 17c.7-1.7 2-2 3-4 1.2-2.4 4.2-2.2 5.2.2.8 1.8 2.4 2.6 2.8 4.3.7 3-2.3 4-4.2 3-1.2-.7-2-.7-3.2-.2-2.6 1.1-4.8-.4-3.6-3.3Z"/></template>
    <path v-else :d="paths[name] || paths.heart"/>
  </svg>
</template>
<style scoped>.app-icon { flex-shrink: 0; vertical-align: middle; }</style>
