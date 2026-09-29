<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage, imageUrl } from '../api'
import AppIcon from '../components/AppIcon.vue'
import AppPhoto from '../components/AppPhoto.vue'

const data = ref({ pets: [], upcoming_reminders: [], pet_count: 0 })
const error = ref('')
onMounted(async () => {
  try { data.value = (await api.get('/dashboard')).data }
  catch (e) { error.value = errorMessage(e) }
})
</script>

<template>
  <div class="dashboard">
    <section class="welcome-hero" aria-labelledby="welcome-title">
      <div class="hero-copy">
        <span class="eyebrow"><AppIcon name="paw" :size="16" /> 宠物管家 · 每天都值得好好记录</span>
        <h1 id="welcome-title">把每一天的陪伴，<br><span>照顾得更好。</span></h1>
        <p>从健康档案到生活点滴，和毛孩子有关的重要时刻，都在这里。</p>
        <RouterLink class="btn hero-action" to="/pets">查看我的宠物 <AppIcon name="arrow" :size="17" /></RouterLink>
      </div>
      <div class="hero-photo-wrap"><AppPhoto class="hero-photo" src="/images/dog-lifestyle.jpg" alt="一只狗狗" loading="eager" /><span class="photo-caption">给陪伴，多一点用心</span></div>
    </section>
    <div v-if="error" class="message error">{{ error }} <RouterLink to="/login">前往登录</RouterLink></div>

    <section class="overview" aria-label="今日照护概览">
      <div><span class="section-kicker">TODAY / 今日照护</span><h2>从这里开始，照看每一天</h2><p>你的宠物档案和近期提醒，一眼就能看到。</p></div>
      <div class="overview-stats">
        <RouterLink class="overview-stat" to="/pets"><span class="stat-icon orange"><AppIcon name="pet" :size="22" /></span><span class="stat-copy"><strong>{{ data.pet_count }}</strong><small>我的宠物</small></span><AppIcon name="arrow" :size="16" class="arrow" /></RouterLink>
        <RouterLink class="overview-stat" to="/reminders"><span class="stat-icon teal"><AppIcon name="calendar" :size="22" /></span><span class="stat-copy"><strong>{{ data.upcoming_reminders?.length || 0 }}</strong><small>近期提醒</small></span><AppIcon name="arrow" :size="16" class="arrow" /></RouterLink>
      </div>
    </section>

    <section class="quick-section" aria-labelledby="quick-title">
      <div class="section-heading"><div><span class="section-kicker">SHORTCUTS / 快捷入口</span><h2 id="quick-title">照护，从一件小事开始</h2></div></div>
      <div class="quick-grid">
        <RouterLink class="quick-link" to="/reminders"><span class="quick-icon"><AppIcon name="calendar" :size="22" /></span><span><strong>添加照护提醒</strong><small>记住疫苗、驱虫与复诊</small></span><AppIcon name="arrow" :size="17" class="arrow" /></RouterLink>
        <RouterLink class="quick-link" to="/logs"><span class="quick-icon"><AppIcon name="heart" :size="22" /></span><span><strong>记录日常点滴</strong><small>留下今天的健康与心情</small></span><AppIcon name="arrow" :size="17" class="arrow" /></RouterLink>
        <RouterLink class="quick-link" to="/ai/assistant"><span class="quick-icon"><AppIcon name="sparkles" :size="22" /></span><span><strong>问问宠物管家</strong><small>有疑问时获取照护建议</small></span><AppIcon name="arrow" :size="17" class="arrow" /></RouterLink>
      </div>
    </section>

    <div class="dashboard-columns">
      <section aria-labelledby="pets-title">
        <div class="section-heading"><div><span class="section-kicker">MY PETS / 我的家人</span><h2 id="pets-title">我的毛孩子</h2></div><RouterLink class="section-more" to="/pets">管理宠物 <AppIcon name="arrow" :size="16" /></RouterLink></div>
        <div v-if="data.pets?.length" class="content-list"><RouterLink v-for="pet in data.pets" :key="pet.id" class="pet-row" :to="`/pets/${pet.id}/health`"><AppPhoto class="pet-avatar" :src="pet.avatar ? imageUrl(pet.avatar) : ''" :alt="pet.avatar ? `${pet.name}的照片` : `${pet.name}的默认头像`" /><span class="row-copy"><strong>{{ pet.name }}</strong><small>{{ pet.breed || (pet.species === 'cat' ? '猫咪' : '狗狗') }} · {{ pet.age_display || '年龄未填写' }}</small></span><AppIcon name="arrow" :size="17" class="arrow" /></RouterLink></div>
        <div v-else class="empty pet-empty"><span class="empty-icon"><AppIcon name="pet" :size="27" /></span><strong>还没有宠物档案</strong><span>为毛孩子建立档案，开始记录成长。</span><RouterLink class="btn" to="/pets"><AppIcon name="plus" :size="17" /> 添加宠物</RouterLink></div>
      </section>
      <section aria-labelledby="reminders-title">
        <div class="section-heading"><div><span class="section-kicker">UP NEXT / 近期安排</span><h2 id="reminders-title">即将到来的提醒</h2></div><RouterLink class="section-more" to="/reminders">查看全部 <AppIcon name="arrow" :size="16" /></RouterLink></div>
        <div class="content-list"><div v-for="item in data.upcoming_reminders" :key="item.id" class="reminder-row"><span class="reminder-icon"><AppIcon name="clock" :size="18" /></span><span class="row-copy"><strong>{{ item.title }}</strong><small>{{ item.pet_name }}</small></span><span class="reminder-date">{{ item.reminder_date }}</span></div><div v-if="!data.upcoming_reminders?.length" class="reminder-empty"><AppIcon name="check" :size="24" /><strong>近期没有待办提醒</strong><span>安排新提醒，让重要的照护日子不被忘记。</span><RouterLink to="/reminders">去设置提醒 <AppIcon name="arrow" :size="15" /></RouterLink></div></div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.dashboard{max-width:1370px;margin:auto}.welcome-hero{display:grid;grid-template-columns:1.02fr .98fr;min-height:340px;overflow:hidden;border-radius:24px;background:#eaf3f8;box-shadow:0 12px 34px #2532470a}.hero-copy{padding:clamp(30px,4vw,62px);align-self:center;z-index:1}.eyebrow,.section-kicker{display:inline-flex;align-items:center;gap:7px;color:var(--accent,#2a7e86);font-size:11px;font-weight:800;letter-spacing:.12em}.hero-copy h1{font-size:clamp(29px,3.3vw,49px);line-height:1.3;letter-spacing:-.055em;color:var(--ink,#253247);margin:19px 0 16px;font-weight:800}.hero-copy h1 span{color:var(--accent,#2a7e86)}.hero-copy p{max-width:450px;color:#637486;font-size:15px;line-height:1.85;margin:0 0 27px}.hero-action{min-height:45px;padding-inline:21px}.hero-photo-wrap{position:relative;min-height:340px}.hero-photo{width:100%;height:100%;object-fit:cover;object-position:center 43%;display:block}.hero-photo-wrap:before{content:"";position:absolute;z-index:1;inset:0 auto 0 0;width:18%;background:linear-gradient(90deg,#eaf3f8,transparent);pointer-events:none}.photo-caption{position:absolute;z-index:2;right:23px;bottom:21px;background:#ffffffed;border-radius:999px;padding:8px 13px;color:#425768;font-size:11px;font-weight:700;box-shadow:0 6px 18px #25324715}.overview{display:grid;grid-template-columns:minmax(230px,1fr) minmax(300px,1.1fr);gap:24px;align-items:center;margin:32px 0}.overview h2,.section-heading h2{color:var(--ink,#253247);font-size:22px;line-height:1.35;letter-spacing:-.035em;margin:9px 0 0}.overview p{color:var(--muted,#738195);font-size:13px;margin:9px 0 0}.overview-stats{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:13px}.overview-stat,.quick-link{display:flex;align-items:center;gap:13px;background:#fff;border:1px solid var(--line,#e5ebf1);border-radius:18px;padding:17px;color:var(--ink,#253247);box-shadow:0 5px 18px #25324707;transition:transform .18s,box-shadow .18s}.overview-stat:hover,.quick-link:hover,.pet-row:hover{transform:translateY(-2px);box-shadow:0 12px 28px #25324712;text-decoration:none}.stat-icon,.quick-icon,.empty-icon{display:grid;place-items:center;flex:none;border-radius:13px}.stat-icon{height:47px;width:47px}.orange{background:#fff1e8;color:#c85d1c}.teal{background:#e7f3f3;color:#2a7e86}.stat-copy,.quick-link>span:nth-child(2),.row-copy{display:flex;flex-direction:column;gap:5px;min-width:0}.stat-copy strong{font-size:25px;line-height:1;font-variant-numeric:tabular-nums}.stat-copy small,.quick-link small,.row-copy small{font-size:11px;color:var(--muted,#738195)}.arrow{margin-left:auto;color:#a8b5c2;flex:none}.quick-section{margin:37px 0}.section-heading{display:flex;align-items:end;justify-content:space-between;gap:15px;margin-bottom:17px}.quick-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:13px}.quick-link{min-width:0;border-radius:17px;padding:18px 17px}.quick-icon{height:43px;width:43px;background:#eaf3f8;color:var(--accent,#2a7e86)}.quick-link:nth-child(2) .quick-icon{background:#fff1e8;color:#c85d1c}.quick-link:nth-child(3) .quick-icon{background:#eef0fa;color:#6571a2}.quick-link strong,.row-copy strong{font-size:13px}.quick-link small{line-height:1.5}.dashboard-columns{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:25px}.section-more{display:inline-flex;align-items:center;gap:5px;color:var(--accent,#2a7e86);font-size:12px;font-weight:700;white-space:nowrap}.content-list{border:1px solid var(--line,#e5ebf1);border-radius:19px;background:#fff;overflow:hidden;min-height:215px;box-shadow:0 5px 18px #25324707}.pet-row,.reminder-row{display:flex;align-items:center;gap:13px;padding:14px 17px;border-bottom:1px solid var(--line,#e5ebf1)}.pet-row:last-child,.reminder-row:last-child{border-bottom:0}.pet-row{color:var(--ink,#253247);transition:transform .18s,box-shadow .18s}.pet-avatar{height:53px;width:53px;flex:none;border-radius:13px;overflow:hidden;background:#edf3f7}.pet-empty{min-height:215px;display:flex;align-items:center;justify-content:center;flex-direction:column;gap:7px;border:1px dashed #cfdbe4}.pet-empty strong{color:var(--ink,#253247);font-size:15px}.pet-empty span:not(.empty-icon){font-size:12px}.pet-empty .btn{margin-top:12px}.empty-icon{width:43px;height:43px;background:#eaf3f8;color:var(--accent,#2a7e86)}.reminder-icon{display:grid;place-items:center;width:39px;height:39px;flex:none;border-radius:12px;background:#fff2e8;color:#d2692a}.reminder-date{font-size:11px;white-space:nowrap;color:var(--accent,#2a7e86);background:#e7f3f3;border-radius:999px;padding:5px 9px;margin-left:auto;font-variant-numeric:tabular-nums}.reminder-empty{min-height:215px;padding:25px;display:flex;align-items:center;justify-content:center;flex-direction:column;gap:8px;text-align:center;color:var(--muted,#738195);font-size:12px}.reminder-empty>svg{color:var(--accent,#2a7e86)}.reminder-empty strong{color:var(--ink,#253247);font-size:15px}.reminder-empty a{display:inline-flex;align-items:center;gap:4px;font-weight:700;margin-top:7px;color:var(--accent,#2a7e86)}
@media(max-width:1100px){.quick-grid,.dashboard-columns{grid-template-columns:1fr}.quick-link{min-height:68px}}@media(max-width:760px){.welcome-hero{grid-template-columns:1fr}.hero-copy{padding:29px 25px}.hero-copy h1{font-size:35px}.hero-photo-wrap{min-height:210px;max-height:240px}.hero-photo-wrap:before{inset:0 0 auto;width:auto;height:18%;background:linear-gradient(#eaf3f8,transparent)}.overview{grid-template-columns:1fr;gap:18px;margin:27px 0}.quick-section{margin:31px 0}}@media(max-width:410px){.hero-copy h1{font-size:30px}.overview-stats{gap:8px}.overview-stat{padding:12px 10px;gap:8px}.stat-icon{width:37px;height:37px}.stat-copy strong{font-size:22px}.overview-stat .arrow{display:none}.section-heading h2{font-size:20px}.reminder-date{font-size:10px;padding:4px 6px}}@media(prefers-reduced-motion:reduce){.overview-stat,.quick-link,.pet-row{transition:none}}
.pet-avatar :deep(.photo-empty){min-height:0}.pet-avatar :deep(.photo-empty span){display:none}.pet-avatar :deep(.photo-empty svg){width:23px;height:23px}.hero-photo{position:absolute;inset:0}.hero-photo :deep(img){object-position:center 43%}
</style>
