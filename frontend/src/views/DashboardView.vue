<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage, imageUrl } from '../api'
const data = ref({ pets: [], upcoming_reminders: [], pet_count: 0 })
const error = ref('')
onMounted(async () => { try { data.value = (await api.get('/dashboard')).data } catch (e) { error.value = errorMessage(e) } })
</script>
<template>
  <div class="hero"><div><h1>欢迎回到宠物管家 🐾</h1><p>为毛孩子记录健康、成长与每一份快乐。</p></div><RouterLink class="btn" to="/pets">查看我的宠物 →</RouterLink></div>
  <div v-if="error" class="message error">{{ error }} <RouterLink to="/login">前往登录</RouterLink></div>
  <div class="three-col" style="margin-bottom:22px"><div class="card"><div class="muted">我的宠物</div><div class="stat">{{ data.pet_count }}</div></div><div class="card"><div class="muted">近期提醒</div><div class="stat">{{ data.upcoming_reminders?.length || 0 }}</div></div><div class="card"><div class="muted">照护入口</div><RouterLink class="btn secondary" style="margin-top:9px" to="/ai/assistant">问问宠物管家助手</RouterLink></div></div>
  <div class="page-head"><div><h2>我的毛孩子</h2><p>从这里进入每只宠物的专属档案</p></div><RouterLink class="btn" to="/pets">管理宠物</RouterLink></div>
  <div v-if="data.pets?.length" class="grid"><RouterLink v-for="pet in data.pets" :key="pet.id" class="card" :to="`/pets/${pet.id}/health`"><img v-if="pet.avatar" class="pet-image" :src="imageUrl(pet.avatar)" :alt="pet.name"><div v-else class="pet-placeholder">{{ pet.species === 'cat' ? '🐈' : '🐕' }}</div><h3 style="margin-top:15px">{{ pet.name }}</h3><span class="muted small">{{ pet.breed || (pet.species === 'cat' ? '猫咪' : '狗狗') }} · {{ pet.age_display || '年龄未填写' }}</span></RouterLink></div><div v-else class="empty">还没有宠物档案，<RouterLink to="/pets">现在添加一只吧</RouterLink>。</div>
  <div class="page-head" style="margin-top:30px"><div><h2>即将到来的提醒</h2></div><RouterLink to="/reminders">查看全部</RouterLink></div>
  <div class="card"><div v-for="item in data.upcoming_reminders" :key="item.id" class="list-row"><span>{{ item.title }} <span class="muted small">· {{ item.pet_name }}</span></span><span class="tag">{{ item.reminder_date }}</span></div><div v-if="!data.upcoming_reminders?.length" class="muted">暂无近期提醒。</div></div>
</template>
