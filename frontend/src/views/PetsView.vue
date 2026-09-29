<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppIcon from '../components/AppIcon.vue'
import AppPhoto from '../components/AppPhoto.vue'
import { api, allPets, errorMessage, imageUrl, unwrap } from '../api'

const pets = ref([]), error = ref(''), message = ref(''), editor = ref(false), current = ref(null), busy = ref(false)
const form = reactive({ name: '', species: 'dog', breed: '', gender: 'unknown', birth_date: '', weight: '', color: '', description: '' })
const countLabel = computed(() => `${pets.value.length} 位家庭成员`)
const fallback = pet => pet.species === 'cat' ? '/images/cat-lifestyle.jpg' : '/images/dog-lifestyle.jpg'
const speciesLabel = pet => pet.species === 'cat' ? '猫咪' : '狗狗'

async function load() { try { pets.value = await allPets() } catch (e) { error.value = errorMessage(e) } }
onMounted(load)
function open(pet = null) {
  current.value = pet
  Object.assign(form, { name: '', species: 'dog', breed: '', gender: 'unknown', birth_date: '', weight: '', color: '', description: '' }, pet || {})
  editor.value = true
}
async function save() {
  busy.value = true; error.value = ''
  try {
    const payload = { ...form, weight: form.weight === '' ? null : Number(form.weight) }
    unwrap(current.value ? await api.put(`/pets/${current.value.id}`, payload) : await api.post('/pets', payload))
    editor.value = false; message.value = '宠物档案已保存'; await load()
  } catch (e) { error.value = errorMessage(e) } finally { busy.value = false }
}
async function remove(pet) {
  if (!confirm(`确定删除 ${pet.name} 的档案及关联记录吗？`)) return
  try { unwrap(await api.delete(`/pets/${pet.id}`)); message.value = '宠物档案已删除'; await load() }
  catch (e) { error.value = errorMessage(e) }
}
async function avatar(pet, event) {
  const file = event.target.files?.[0]
  if (!file) return
  const data = new FormData(); data.append('avatar', file)
  try { unwrap(await api.post(`/pets/${pet.id}/avatar`, data)); message.value = '照片已更新'; await load() }
  catch (e) { error.value = errorMessage(e) }
}
</script>

<template>
  <section class="pet-page">
    <div class="pet-heading page-head">
      <div>
        <p class="eyebrow"><AppIcon name="paw" :size="15" /> 家庭档案 · {{ countLabel }}</p>
        <h1>我的宠物</h1>
        <p>留存每一张照片，也照顾好它的每一天。</p>
      </div>
      <button class="btn add-pet" @click="open()"><AppIcon name="plus" :size="18" /> 添加宠物</button>
    </div>

    <div v-if="error" class="message error">{{ error }}</div>
    <div v-if="message" class="message">{{ message }}</div>

    <div v-if="pets.length" class="pet-grid grid">
      <article v-for="pet in pets" :key="pet.id" class="pet-card card">
        <div class="pet-card-image">
          <AppPhoto :src="pet.avatar ? imageUrl(pet.avatar) : ''" :fallback="fallback(pet)" :fallback-alt="`${speciesLabel(pet)}默认示意照片`" fallback-label="默认示意" :alt="pet.avatar ? `${pet.name}的照片` : `${speciesLabel(pet)}默认示意照片`" />

          <span class="species-tag">{{ speciesLabel(pet) }}</span>
        </div>
        <div class="pet-card-body">
          <div class="pet-name-row"><h2>{{ pet.name }}</h2><span class="pet-gender">{{ pet.gender === 'male' ? '男孩' : pet.gender === 'female' ? '女孩' : '性别未填' }}</span></div>
          <p class="pet-breed">{{ pet.breed || '品种未填写' }}<span class="divider">/</span>{{ pet.color || '毛色未填写' }}</p>
          <div class="pet-facts"><span><AppIcon name="calendar" :size="16" /> {{ pet.age_display || '年龄未填写' }}</span><span><AppIcon name="heart" :size="16" /> {{ pet.weight ? `${pet.weight} kg` : '体重未填写' }}</span></div>
          <p v-if="pet.description" class="pet-story">{{ pet.description }}</p>
          <div class="pet-card-actions actions">
            <RouterLink class="btn care-link" :to="`/pets/${pet.id}/health`">查看照护档案 <AppIcon name="arrow" :size="16" /></RouterLink>
            <button class="btn secondary" @click="open(pet)"><AppIcon name="edit" :size="16" /> 编辑</button>
            <button class="btn danger" @click="remove(pet)"><AppIcon name="trash" :size="16" /> 删除</button>
          </div>
          <label class="photo-upload"><AppIcon name="photo" :size="15" /> 更换照片 <input type="file" accept="image/*" @change="avatar(pet,$event)"></label>
        </div>
      </article>
    </div>
    <div v-else class="empty pet-empty">
      <AppIcon name="pet" :size="42" />
      <h2>为毛孩子建一份档案</h2>
      <p>从昵称和照片开始，慢慢记录它的成长。</p>
      <button class="btn" @click="open()">添加宠物</button>
    </div>

    <div v-if="editor" class="modal-backdrop" @click.self="editor=false">
      <form class="modal pet-modal" @submit.prevent="save">
        <div class="page-head"><div><p class="eyebrow">宠物档案</p><h2>{{ current ? '编辑宠物' : '添加宠物' }}</h2></div><button type="button" class="text-button" @click="editor=false">关闭 ✕</button></div>
        <div class="form-grid">
          <label class="field"><span>昵称 *</span><input v-model.trim="form.name" required maxlength="64"></label>
          <label class="field"><span>种类 *</span><select v-model="form.species"><option value="dog">狗狗</option><option value="cat">猫咪</option></select></label>
          <label class="field"><span>品种</span><input v-model.trim="form.breed"></label>
          <label class="field"><span>性别</span><select v-model="form.gender"><option value="unknown">未知</option><option value="male">公</option><option value="female">母</option></select></label>
          <label class="field"><span>出生日期</span><input v-model="form.birth_date" type="date"></label>
          <label class="field"><span>体重 (kg)</span><input v-model="form.weight" type="number" min="0" step="0.01"></label>
          <label class="field"><span>毛色</span><input v-model="form.color"></label>
          <label class="field full"><span>关于它</span><textarea v-model="form.description" placeholder="写下它的性格和故事…"></textarea></label>
        </div>
        <div class="actions modal-actions"><button type="button" class="btn secondary" @click="editor=false">取消</button><button class="btn" :disabled="busy">保存档案</button></div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.pet-page{max-width:1240px;margin:0 auto}.pet-heading{align-items:flex-end;margin-bottom:26px}.pet-heading h1{font-size:clamp(28px,3vw,38px);letter-spacing:-.03em}.pet-heading p:not(.eyebrow){margin-top:9px}.eyebrow{display:flex;align-items:center;gap:7px;color:var(--brand,#E87832);font-size:12px;font-weight:800;letter-spacing:.12em;margin:0 0 9px}.add-pet{min-height:44px}.pet-grid{grid-template-columns:repeat(auto-fill,minmax(270px,1fr));gap:22px}.pet-card{padding:0;overflow:hidden;border:1px solid var(--line,#e0e8ef);box-shadow:0 14px 35px #243b5610;transition:transform .2s,box-shadow .2s}.pet-card:hover{transform:translateY(-3px);box-shadow:0 18px 42px #243b5619}.pet-card-image{height:210px;position:relative;overflow:hidden;background:#eaf3f8}.pet-card-image :deep(img){display:block;width:100%;height:100%;object-fit:cover}.sample-label,.species-tag{position:absolute;top:14px;border-radius:999px;padding:5px 10px;font-size:11px;font-weight:700;backdrop-filter:blur(8px)}.sample-label{right:14px;color:#435875;background:#ffffffdc}.species-tag{left:14px;background:#fff;color:var(--ink,#253247)}.pet-card-body{padding:21px}.pet-name-row{display:flex;align-items:baseline;justify-content:space-between;gap:12px}.pet-name-row h2{font-size:23px;line-height:1.2;margin:0;color:var(--ink,#253247)}.pet-gender{font-size:12px;color:var(--muted,#6b7b90)}.pet-breed{font-size:13px;color:var(--muted,#6b7b90);margin:9px 0 17px}.divider{color:#ccd6e0;margin:0 8px}.pet-facts{display:flex;gap:16px;flex-wrap:wrap;padding:13px 0;border-top:1px solid var(--line,#e0e8ef);border-bottom:1px solid var(--line,#e0e8ef);font-size:12px;color:var(--ink,#253247)}.pet-facts span{display:flex;align-items:center;gap:6px}.pet-facts :deep(svg){color:var(--accent,#2A7E86)}.pet-story{font-size:13px;line-height:1.6;color:var(--muted,#6b7b90);margin:13px 0 0;max-height:42px;overflow:hidden}.pet-card-actions{gap:8px;margin-top:18px}.pet-card-actions .btn{padding:8px 11px;font-size:12px}.care-link{flex:1;justify-content:space-between;white-space:nowrap}.photo-upload{display:flex;align-items:center;gap:6px;margin-top:16px;color:var(--accent,#2A7E86);font-size:12px;font-weight:700;cursor:pointer}.photo-upload input{position:absolute;width:1px;height:1px;opacity:0}.pet-empty{padding:74px 24px}.pet-empty :deep(svg){color:var(--accent,#2A7E86)}.pet-empty h2{font-size:23px;color:var(--ink,#253247);margin:14px 0 6px}.pet-empty p{margin:0 0 20px}.pet-modal{max-width:640px}.pet-modal h2{margin:0}.modal-actions{justify-content:flex-end;margin-top:22px}@media(max-width:640px){.pet-heading{align-items:flex-start}.pet-grid{grid-template-columns:1fr}.pet-card-image{height:225px}.pet-card-actions{flex-wrap:wrap}.care-link{flex-basis:100%}}@media(prefers-reduced-motion:reduce){.pet-card{transition:none}.pet-card:hover{transform:none}}
</style>
<style scoped>
.pet-card-image :deep(.photo-frame){height:100%}
</style>
