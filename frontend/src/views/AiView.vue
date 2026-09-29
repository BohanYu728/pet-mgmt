<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { api, allPets, errorMessage, imageUrl } from '../api'
const props = defineProps({ section: String })
const page = computed(() => props.section || 'assistant')
const pets = ref([]), petId = ref(''), image = ref(null), question = ref(''), answer = ref(null), history = ref([]), error = ref(''), busy = ref(false)
const consultation = reactive({ species: 'dog', symptoms: '', duration: '', pet_id: null })
const options = [['assistant','宠物管家助手'],['breed','品种识别'],['emotion','情绪分析'],['health','健康报告'],['recommend','商品推荐'],['consultation','症状咨询']]
const title = computed(() => Object.fromEntries(options)[page.value] || '宠物管家助手')
async function loadPets() { try { pets.value = await allPets(); petId.value = pets.value[0]?.id || '' } catch {} }
async function loadHistory() { try { const url = page.value === 'breed' ? '/ai/breed/history' : page.value === 'consultation' ? '/consultation/history' : page.value === 'emotion' && petId.value ? `/ai/emotion/data/${petId.value}` : ''; const data = url ? (await api.get(url)).data : {}; history.value = data.items || data.records || data.emotions || [] } catch {} }
onMounted(async () => { await loadPets(); await loadHistory() }); watch(page, () => { answer.value = null; error.value = ''; loadHistory() }); watch(petId, () => { if(page.value === 'emotion')loadHistory() })
async function submit() { busy.value = true; error.value = ''; answer.value = null; try {
  let response
  if (page.value === 'assistant') response = await api.post('/assistant/ask', { question: question.value })
  else if (page.value === 'consultation') response = await api.post('/consultation/ask', { species: consultation.species, symptoms: consultation.symptoms, duration: consultation.duration, pet_id: consultation.pet_id || undefined })
  else if (page.value === 'health') response = await api.post(`/ai/health-report/${petId.value}`)
  else if (page.value === 'recommend') response = await api.post(`/ai/recommend/${petId.value}`)
  else { if (!image.value) throw new Error('请先选择照片'); const form = new FormData(); form.append('image', image.value); if (petId.value) form.append('petId', petId.value); response = await api.post(`/ai/${page.value}/recognize`, form) }
  answer.value = response.data; await loadHistory()
} catch (e) { error.value = e?.response ? errorMessage(e) : e.message } finally { busy.value = false } }
function resultText() { const a = answer.value; if (!a) return ''; if (page.value === 'assistant') return a.answer || a.message || '暂无回答'; if (page.value === 'health') return a.report || '暂无报告'; if (page.value === 'recommend') return a.recommendation || '暂无推荐'; if (page.value === 'consultation') return [a.advice, a.matched_conditions ? `参考情况：${a.matched_conditions}` : '', a.severity ? `紧急程度：${a.severity}` : ''].filter(Boolean).join('\n') || '暂无建议'; if (page.value === 'breed') return [a.name_zh || a.breed_name || '识别完成', a.confidence != null ? `置信度：${a.confidence}%` : '', a.description, a.care_tips ? `照护建议：${a.care_tips}` : ''].filter(Boolean).join('\n'); if (page.value === 'emotion') return [a.emotion ? `情绪：${a.emotion}` : '分析完成', a.confidence != null ? `置信度：${a.confidence}%` : '', a.comfort_advice].filter(Boolean).join('\n'); return a.message || '分析完成' }
function historyTitle(item) { return item.recognized_breed || item.emotion || item.symptoms || '分析记录' }
</script>
<template>
  <div class="page-head"><div><h1>{{ title }}</h1><p>{{ page === 'assistant' ? '基于项目养宠资料回答常见问题，并显示参考出处' : '原有智能宠物照护功能继续为您服务' }}</p></div></div>
  <div class="tabs"><RouterLink v-for="[key,name] in options" :key="key" :to="`/ai/${key}`">{{ name }}</RouterLink></div>
  <div v-if="error" class="message error">{{ error }}</div>
  <div class="two-col"><form class="card" @submit.prevent="submit"><h3>{{ title }}</h3>
    <label v-if="['breed','emotion','health','recommend'].includes(page)" class="field"><span>关联宠物{{ page === 'breed' ? '（可选）' : '' }}</span><select v-model="petId" :required="page !== 'breed'"><option v-if="page === 'breed'" value="">不关联</option><option v-for="pet in pets" :key="pet.id" :value="pet.id">{{ pet.name }}</option></select></label>
    <label v-if="['breed','emotion'].includes(page)" class="field" style="margin-top:14px"><span>上传宠物照片</span><input type="file" accept="image/*" required @change="image=$event.target.files[0]"></label>
    <label v-if="page === 'assistant'" class="field"><span>您的问题</span><textarea v-model.trim="question" maxlength="1000" required placeholder="例如：宠物疫苗到期后在哪里查看提醒？"></textarea></label>
    <template v-if="page === 'consultation'"><label class="field"><span>宠物种类</span><select v-model="consultation.species"><option value="dog">狗狗</option><option value="cat">猫咪</option></select></label><label class="field" style="margin-top:12px"><span>关联宠物</span><select v-model="consultation.pet_id"><option :value="null">不关联</option><option v-for="pet in pets" :key="pet.id" :value="pet.id">{{ pet.name }}</option></select></label><label class="field" style="margin-top:12px"><span>症状描述</span><textarea v-model.trim="consultation.symptoms" maxlength="2000" required placeholder="请描述观察到的症状"></textarea></label><label class="field" style="margin-top:12px"><span>持续时间</span><input v-model.trim="consultation.duration" placeholder="例如：两天"></label></template>
    <p v-if="page === 'assistant' || page === 'consultation' || page === 'health'" class="warning">本功能仅供参考；如有紧急健康问题，请及时咨询兽医。</p>
    <button class="btn" style="margin-top:17px" :disabled="busy || (['emotion','health','recommend'].includes(page) && !petId)">{{ busy ? '分析中…' : page === 'assistant' ? '发送问题' : '开始分析' }}</button>
  </form><div class="card"><h3>分析结果</h3><div v-if="answer"><div class="prewrap">{{ resultText() }}</div><img v-if="answer.image_url" :src="imageUrl(answer.image_url)" alt="分析照片" class="pet-image" style="margin-top:15px;max-width:300px"><div v-if="page === 'assistant' && answer.status" class="tag" style="margin-top:15px">状态：{{ answer.status }}</div><div v-if="answer.sources?.length" style="margin-top:20px"><strong>参考来源</strong><div v-for="(source,i) in answer.sources" :key="i" class="source"><strong>{{ source.title || source.source || source.file || source.id || '资料' }}</strong> · {{ source.section || source.chunk_id || '' }}<div v-if="source.excerpt" class="prewrap">{{ source.excerpt }}</div></div></div></div><div v-else class="muted">结果将在这里显示。</div></div></div>
  <div v-if="history.length" class="card" style="margin-top:20px"><h3>历史记录</h3><div v-for="item in history" :key="item.id" class="list-row"><div><strong>{{ historyTitle(item) }}</strong><p v-if="item.advice || item.care_tips || item.comfort_advice" class="small prewrap">{{ item.advice || item.care_tips || item.comfort_advice }}</p></div><span class="muted small">{{ item.created_at?.slice(0,16) }}</span></div></div>
</template>
