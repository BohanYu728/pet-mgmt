<script setup>
import { ref } from 'vue'
import { api, errorMessage, imageUrl } from '../api'
import AppIcon from '../components/AppIcon.vue'
import AppPhoto from '../components/AppPhoto.vue'
const query = ref(''), pets = ref([]), error = ref(''), searched = ref(false)
async function search() { error.value = ''; try { pets.value = (await api.get('/search', { params: { q: query.value } })).data.pets || []; searched.value = true } catch (e) { error.value = errorMessage(e) } }
</script>
<template>
  <div class="page-head"><div><span class="section-eyebrow"><AppIcon name="search" :size="15"/> 每一位家人，都有专属档案</span><h1>搜索宠物档案</h1><p>按昵称或品种，快速找到您的毛孩子</p></div></div>
  <form class="card inline-form search-panel" @submit.prevent="search"><span class="feature-symbol"><AppIcon name="search" :size="27"/></span><label class="field"><span>关键词</span><input v-model.trim="query" required placeholder="输入宠物昵称或品种，例如：旺财、金毛"></label><button class="btn"><AppIcon name="search" :size="17"/> 搜索</button></form>
  <div v-if="error" class="message error">{{ error }}</div>
  <div v-if="searched" class="search-results"><div class="result-count">找到 <strong>{{ pets.length }}</strong> 份相关档案</div><div v-if="pets.length" class="grid"><RouterLink v-for="pet in pets" :key="pet.id" class="card search-pet" :to="`/pets/${pet.id}/health`"><AppPhoto v-if="pet.avatar" class="pet-image" :src="imageUrl(pet.avatar)" :alt="pet.name"/><div v-else class="pet-placeholder"><AppIcon name="paw" :size="43"/></div><div class="search-pet-info"><div><h3>{{ pet.name }}</h3><p>{{ pet.breed || (pet.species === 'cat' ? '猫咪' : '狗狗') }}</p></div><AppIcon name="arrow" :size="18"/></div></RouterLink></div><div v-else class="empty"><AppIcon name="search" :size="35"/><strong>没有找到匹配的宠物档案</strong><p>检查一下昵称，或换一个品种关键词试试。</p></div></div>
  <div v-else class="search-guide"><AppIcon name="paw" :size="38"/><h2>找到它，继续记录它的故事</h2><p>搜索结果仅包含您自己的宠物档案。</p></div>
</template>
<style scoped>
.search-panel{align-items:center;gap:20px}.search-panel .field{max-width:640px}.search-panel .btn{align-self:end}.search-panel .feature-symbol{background:#eaf3f8}.search-results{margin-top:25px}.result-count{font-size:12px;color:var(--muted);margin-bottom:16px}.result-count strong{color:var(--accent);font-size:17px}.search-pet{padding:12px;transition:box-shadow .2s}.search-pet:hover{text-decoration:none;box-shadow:0 9px 30px #25324710}.search-pet-info{display:flex;justify-content:space-between;align-items:center;padding:17px 9px 7px}.search-pet-info h3{margin:0 0 6px}.search-pet-info p{font-size:12px;color:var(--muted);margin:0}.search-guide{text-align:center;padding:80px 15px;color:#94adb8}.search-guide h2{font-size:20px;margin:15px 0 8px}.search-guide p{font-size:12px;color:var(--muted)}@media(max-width:560px){.search-panel{gap:14px}.search-panel .feature-symbol{display:none}.search-panel .field{flex-basis:100%}.search-panel .btn{width:100%}.search-guide{padding:60px 15px}}
</style>
