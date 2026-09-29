<script setup>
import { ref } from 'vue'
import { api, errorMessage, imageUrl } from '../api'
const query = ref(''), pets = ref([]), error = ref(''), searched = ref(false)
async function search() { error.value=''; try { pets.value=(await api.get('/search',{params:{q:query.value}})).data.pets || []; searched.value=true } catch(e){error.value=errorMessage(e)} }
</script>
<template><div class="page-head"><div><h1>搜索宠物档案</h1><p>按昵称或品种查找您的毛孩子</p></div></div><form class="card inline-form" @submit.prevent="search"><label class="field"><span>关键词</span><input v-model.trim="query" required placeholder="昵称或品种"></label><button class="btn">搜索</button></form><div v-if="error" class="message error">{{ error }}</div><div v-if="searched" style="margin-top:20px"><div v-if="pets.length" class="grid"><RouterLink v-for="pet in pets" :key="pet.id" class="card" :to="`/pets/${pet.id}/health`"><img v-if="pet.avatar" class="pet-image" :src="imageUrl(pet.avatar)" :alt="pet.name"><div v-else class="pet-placeholder">{{ pet.species==='cat'?'🐈':'🐕' }}</div><h3>{{ pet.name }}</h3><p class="muted small">{{ pet.breed }}</p></RouterLink></div><div v-else class="empty">没有找到匹配的宠物档案。</div></div></template>
