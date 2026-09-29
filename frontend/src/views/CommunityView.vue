<script setup>
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { api, errorMessage } from '../api'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
const hospitals = ref([]), selected = ref(null), error = ref(''), message = ref(''), busy = ref(false)
const mapElement = ref(null), radius = ref(10)
let map, markers
const review = reactive({ rating: 5, content: '' })
function renderMarkers() { if (!map) return; markers.clearLayers(); for (const item of hospitals.value) { if (Number.isFinite(Number(item.latitude)) && Number.isFinite(Number(item.longitude))) { const pin = L.marker([item.latitude, item.longitude]).addTo(markers); pin.on('click', () => detail(item)) } } const bounds = markers.getBounds(); if (bounds.isValid()) map.fitBounds(bounds.pad(.2), { maxZoom: 13 }) }
async function load() { try { hospitals.value = (await api.get('/hospitals')).data.hospitals || []; await nextTick(); renderMarkers() } catch (e) { error.value = errorMessage(e) } }
function nearby() { if (!navigator.geolocation) { error.value = '浏览器不支持定位'; return } navigator.geolocation.getCurrentPosition(async pos => { try { const { latitude: lat, longitude: lng } = pos.coords; hospitals.value = (await api.get('/hospitals/nearby', { params: { lat, lng, radius: radius.value } })).data.hospitals || []; map?.setView([lat,lng], 12); renderMarkers() } catch (e) { error.value = errorMessage(e) } }, () => { error.value = '无法获取位置，请检查定位权限。' }) }
onMounted(async () => { map = L.map(mapElement.value).setView([35.86,104.19], 4); L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '© OpenStreetMap contributors' }).addTo(map); markers = L.featureGroup().addTo(map); await load() })
onUnmounted(() => map?.remove())
async function detail(item) { try { selected.value = (await api.get(`/hospitals/${item.id}`)).data } catch (e) { error.value = errorMessage(e) } }
async function submit() { busy.value = true; try { await api.post(`/hospitals/${selected.value.hospital.id}/reviews`, { rating: Number(review.rating), content: review.content }); message.value = '评价已发表'; review.content = ''; await detail(selected.value.hospital) } catch (e) { error.value = errorMessage(e) } finally { busy.value = false } }
</script>
<template>
  <div class="page-head"><div><h1>宠物医院</h1><p>查找附近的宠物医疗服务，分享就诊体验</p></div></div>
  <div class="card inline-form" style="margin-bottom:18px"><label class="field"><span>搜索半径 (公里)</span><input v-model.number="radius" type="number" min="1" max="100"></label><button class="btn" @click="nearby">查找附近医院</button><button class="btn secondary" @click="load">查看全部</button></div>
  <div ref="mapElement" style="height:340px;border-radius:16px;margin-bottom:19px;z-index:0" aria-label="宠物医院地图"></div>
  <div v-if="error" class="message error">{{ error }}</div><div v-if="message" class="message">{{ message }}</div>
  <div v-if="hospitals.length" class="grid"><div v-for="hospital in hospitals" :key="hospital.id" class="card"><div style="font-size:38px">🏥</div><h3>{{ hospital.name }}</h3><p class="muted small">{{ hospital.address }}</p><p v-if="hospital.phone" class="small">电话：{{ hospital.phone }}</p><p v-if="hospital.description" class="small">{{ hospital.description }}</p><button class="btn secondary" @click="detail(hospital)">详情与评价</button></div></div><div v-else class="empty">暂无医院信息。</div>
  <div v-if="selected" class="modal-backdrop" @click.self="selected=null"><div class="modal"><div class="page-head"><h2>{{ selected.hospital.name }}</h2><button class="text-button" @click="selected=null">关闭 ✕</button></div><p>{{ selected.hospital.address }} · {{ selected.hospital.phone }}</p><h3>就诊评价</h3><div v-for="item in selected.reviews" :key="item.id" class="list-row"><div><strong>{{ '★'.repeat(item.rating) }}</strong><p class="small prewrap">{{ item.content }}</p></div></div><p v-if="!selected.reviews?.length" class="muted">暂无评价。</p><form @submit.prevent="submit"><h3>发表评价</h3><div class="form-grid"><label class="field"><span>评分</span><select v-model.number="review.rating"><option v-for="n in 5" :key="n" :value="n">{{ n }} 星</option></select></label><label class="field full"><span>就诊体验</span><textarea v-model.trim="review.content" maxlength="2000" required></textarea></label></div><button class="btn" style="margin-top:14px" :disabled="busy">提交评价</button></form></div></div>
</template>
