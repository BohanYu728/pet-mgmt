<script setup>
import { computed, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'
const props = defineProps({ src: String, fallback: String, alt: { type: String, default: '' }, fallbackAlt: { type: String, default: '示意照片' }, fallbackLabel: { type: String, default: '分类示意图' }, compact: Boolean, loading: { type: String, default: 'lazy' } })
const useFallback = ref(false), failed = ref(false)
const current = computed(() => useFallback.value ? props.fallback : props.src || props.fallback)
const showingFallback = computed(() => !!props.fallback && current.value === props.fallback && !failed.value)
watch(() => [props.src, props.fallback], () => { useFallback.value = false; failed.value = false })
function onError() { if (props.fallback && current.value !== props.fallback) useFallback.value = true; else failed.value = true }
</script>
<template><div class="photo-frame"><img v-if="current && !failed" :src="current" :alt="showingFallback ? fallbackAlt : alt" :loading="loading" decoding="async" @error="onError"><div v-else class="photo-empty" role="img" aria-label="暂无图片"><AppIcon name="photo" :size="32"/><span>暂无图片</span></div><span v-if="showingFallback" class="fallback-label" :class="{ compact }">{{ compact ? '示意图' : fallbackLabel }}</span><slot/></div></template>
<style scoped>
.photo-frame { position: relative; overflow: hidden; background: #eaf0f3; }
img { display: block; width: 100%; height: 100%; object-fit: cover; }
.photo-empty { display: flex; min-height: 0; height: 100%; gap: 8px; padding: 8px; align-items: center; justify-content: center; flex-direction: column; color: #657789; font-size: 11px; }
.fallback-label { position: absolute; top: 13px; right: 12px; border-radius: 999px; padding: 5px 9px; font-size: 11px; font-weight: 700; background: #fffffff0; color: #253247; }
.fallback-label.compact { top: auto; bottom: 2px; right: 2px; padding: 1px 4px; border-radius: 4px; font-size: 10px; }
</style>
