<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { api, allPets, errorMessage, imageUrl, unwrap } from '../api'

const props = defineProps({ id: String, section: String })
const pets = ref([]), petId = ref(props.id || ''), items = ref([]), error = ref(''), message = ref(''), modal = ref(false), editing = ref(null), file = ref(null)
const currentPet = computed(() => pets.value.find(p => String(p.id) === String(petId.value)))
const weightPoints = computed(() => {
  const values = [...items.value].reverse().map(item => Number(item.weight)).filter(Number.isFinite)
  if (!values.length) return []
  const min = Math.min(...values), max = Math.max(...values), span = Math.max(max - min, 1)
  return values.map((value, index) => ({ x: values.length === 1 ? 300 : 35 + index * 530 / (values.length - 1), y: 155 - (value - min) * 115 / span, value }))
})
const section = computed(() => props.section || 'health')
const definitions = {
  health: { title: '健康记录', description: '记录疫苗、驱虫、就诊和体检', fields: [
    ['record_type','记录类型','select',[['vaccine','疫苗'],['deworming','驱虫'],['checkup','体检'],['treatment','就诊'],['other','其他']]],['title','标题','text'],['record_date','记录日期','date'],['next_due_date','下次日期','date'],['veterinarian','医生','text'],['description','详细说明','textarea'],['notes','备注','textarea']] },
  reminders: { title: '疫苗与驱虫提醒', description: '让重要的照护日程不再遗漏', fields: [
    ['reminder_type','提醒类型','select',[['vaccine','疫苗'],['deworming','驱虫'],['checkup','体检'],['other','其他']]],['title','提醒标题','text'],['reminder_date','提醒日期','date']] },
  weights: { title: '体重追踪', description: '观察毛孩子的体重变化', fields: [
    ['weight','体重 (kg)','number'],['record_date','记录日期','date'],['notes','备注','textarea']] },
  logs: { title: '日常日志', description: '收藏它成长中的每一天', fields: [
    ['log_type','日志类型','select',[['daily','日常'],['feeding','饮食'],['exercise','运动'],['mood','心情'],['other','其他']]],['title','标题','text'],['log_date','日期','date'],['content','日志内容','textarea'],['notes','备注','textarea']] },
  photos: { title: '宠物相册', description: '珍藏毛孩子的可爱瞬间', fields: [['caption','照片说明','text']] }
}
const config = computed(() => definitions[section.value] || definitions.health)
const form = reactive({})
const tabs = [['health','健康记录'],['reminders','提醒'],['weights','体重'],['logs','日常日志'],['photos','相册']]
async function loadPets() { try { pets.value = await allPets(); if (!petId.value && pets.value.length) petId.value = String(pets.value[0].id); await loadItems() } catch (e) { error.value = errorMessage(e) } }
async function loadItems() { if (!petId.value) { items.value = []; return }; try { items.value = (await api.get(`/pets/${petId.value}/${section.value}`)).data.items || [] } catch (e) { error.value = errorMessage(e) } }
onMounted(loadPets)
watch(() => props.id, v => { petId.value = v || petId.value; loadItems() })
watch(() => props.section, loadItems)
function open(item = null) { editing.value = item; file.value = null; Object.keys(form).forEach(k => delete form[k]); for (const [key,,type,options] of config.value.fields) form[key] = item?.[key] ?? (type === 'date' ? new Date().toISOString().slice(0,10) : type === 'select' ? options[0][0] : ''); modal.value = true }
async function save() { error.value = ''; try { const payload = { ...form }; if (section.value === 'weights') payload.weight = Number(payload.weight); if (section.value === 'photos') { if (!file.value) throw new Error('请选择照片'); const data = new FormData(); data.append('image', file.value); data.append('caption', payload.caption || ''); unwrap(await api.post(`/pets/${petId.value}/photos`, data)) } else { const url = `/pets/${petId.value}/${section.value}`; unwrap(editing.value ? await api.put(`${url}/${editing.value.id}`, payload) : await api.post(url, payload)) } modal.value = false; message.value = '已保存'; await loadItems() } catch (e) { error.value = e?.response ? errorMessage(e) : e.message } }
async function remove(item) { if (!confirm('确定删除这条记录吗？')) return; try { unwrap(await api.delete(`/pets/${petId.value}/${section.value}/${item.id}`)); await loadItems() } catch (e) { error.value = errorMessage(e) } }
async function mark(item, key) { try { unwrap(await api.put(`/pets/${petId.value}/reminders/${item.id}`, { [key]: true })); await loadItems() } catch (e) { error.value = errorMessage(e) } }
function label(item) { if (section.value === 'weights') return `${item.weight} kg`; return item.title || item.caption || '照片' }
function date(item) { return item.record_date || item.reminder_date || item.log_date || (item.created_at || '').slice(0,10) }
</script>
<template>
  <div class="page-head"><div><h1>{{ config.title }}</h1><p>{{ config.description }}</p></div><button v-if="petId" class="btn" @click="open()">＋ 新增{{ section === 'photos' ? '照片' : '记录' }}</button></div>
  <div v-if="error" class="message error">{{ error }}</div><div v-if="message" class="message">{{ message }}</div>
  <div class="card" style="margin-bottom:20px"><label class="field" style="max-width:320px"><span>选择宠物</span><select v-model="petId" @change="loadItems"><option v-for="pet in pets" :key="pet.id" :value="String(pet.id)">{{ pet.name }}</option></select></label></div>
  <div v-if="petId" class="tabs"><RouterLink v-for="[key,title] in tabs" :key="key" :to="`/pets/${petId}/${key}`">{{ title }}</RouterLink></div>
  <div v-if="section === 'weights' && weightPoints.length" class="card" style="margin-bottom:20px"><h3>体重趋势</h3><svg viewBox="0 0 600 195" role="img" aria-label="宠物体重趋势图" style="width:100%;max-width:720px"><line x1="35" y1="170" x2="565" y2="170" stroke="#cbded2"/><line x1="35" y1="30" x2="35" y2="170" stroke="#cbded2"/><polyline :points="weightPoints.map(p=>`${p.x},${p.y}`).join(' ')" fill="none" stroke="#238565" stroke-width="3"/><g v-for="(point,index) in weightPoints" :key="index"><circle :cx="point.x" :cy="point.y" r="5" fill="#238565"/><text :x="point.x" :y="point.y-10" text-anchor="middle" font-size="11" fill="#376650">{{ point.value }}kg</text></g></svg></div>
  <div v-if="!pets.length" class="empty">请先<RouterLink to="/pets">添加宠物档案</RouterLink>，再记录照护日常。</div>
  <div v-else-if="!items.length" class="empty">暂无{{ config.title }}，点击右上角开始记录。</div>
  <div v-else-if="section === 'photos'" class="grid"><div v-for="item in items" :key="item.id" class="card"><img class="pet-image" :src="imageUrl(item.image_url)" :alt="item.caption || '宠物照片'"><p>{{ item.caption || '美好的瞬间' }}</p><button class="btn danger" @click="remove(item)">删除照片</button></div></div>
  <div v-else class="card"><div v-for="item in items" :key="item.id" class="list-row"><div><strong>{{ label(item) }}</strong><div class="small muted" style="margin-top:5px">{{ date(item) }} <span v-if="item.pet_name"> · {{ item.pet_name }}</span><span v-if="item.veterinarian"> · {{ item.veterinarian }}</span></div><p v-if="item.content || item.description || item.notes" class="small prewrap">{{ item.content || item.description || item.notes }}</p><span v-if="item.is_read" class="tag">已读</span><span v-if="item.is_dismissed" class="tag">已忽略</span></div><div class="actions"><template v-if="section === 'reminders'"><button v-if="!item.is_read" class="btn secondary" @click="mark(item,'is_read')">已读</button><button v-if="!item.is_dismissed" class="btn secondary" @click="mark(item,'is_dismissed')">忽略</button></template><button v-if="section !== 'weights'" class="btn secondary" @click="open(item)">编辑</button><button class="btn danger" @click="remove(item)">删除</button></div></div></div>
  <div v-if="modal" class="modal-backdrop" @click.self="modal=false"><form class="modal" @submit.prevent="save"><div class="page-head"><h2>{{ editing ? '编辑' : '新增' }}{{ config.title }}</h2><button type="button" class="text-button" @click="modal=false">关闭 ✕</button></div><div class="form-grid"><label v-for="[key,title,type,options] in config.fields" :key="key" class="field" :class="{ full: type === 'textarea' }"><span>{{ title }}</span><select v-if="type === 'select'" v-model="form[key]"><option v-for="[value,display] in options" :key="value" :value="value">{{ display }}</option></select><textarea v-else-if="type === 'textarea'" v-model="form[key]"></textarea><input v-else v-model="form[key]" :type="type" :required="['title','weight','reminder_date'].includes(key)" :min="type === 'number' ? 0.01 : undefined" :step="type === 'number' ? 0.01 : undefined"></label><label v-if="section === 'photos'" class="field full"><span>选择照片 *</span><input type="file" accept="image/*" required @change="file=$event.target.files[0]"></label></div><div class="actions"><button class="btn">保存</button></div></form></div>
</template>
