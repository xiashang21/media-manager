<template>
  <div class="overlay" @click.self="$emit('close')">
    <section class="player glass">
      <div class="video-shell">
        <video
          ref="videoRef"
          :src="streamUrl(video.url)"
          controls
          autoplay
          @loadedmetadata="restorePosition"
          @timeupdate="savePosition"
          @ended="clearPosition"
          @play="$emit('play')"
        ></video>
      </div>

      <div class="details">
        <div class="title-row">
          <div>
            <p class="eyebrow">NOW PLAYING</p>
            <h2>{{ video.title }}</h2>
          </div>
          <button class="btn ghost close" type="button" @click="$emit('close')">✕</button>
        </div>

        <p class="description">{{ video.description || '暂无简介' }}</p>

        <div class="tags">
          <span class="tag category">{{ video.category || '默认' }}</span>
          <span v-for="tag in video.tags || []" :key="tag" class="tag">{{ tag }}</span>
        </div>

        <div class="meta-grid">
          <div class="meta-item"><span>上传者</span><strong>{{ video.uploader || 'anonymous' }}</strong></div>
          <div class="meta-item"><span>文件大小</span><strong>{{ video.sizeText || '0 B' }}</strong></div>
          <div class="meta-item"><span>时长</span><strong>{{ video.durationText || '--:--' }}</strong></div>
          <div class="meta-item"><span>格式</span><strong>{{ video.format?.toUpperCase() || '-' }}</strong></div>
        </div>

        <div class="actions">
          <button class="btn like" type="button" @click="$emit('like')">✦ 点赞 {{ video.likeCount }}</button>
          <span class="pill">▶ {{ video.playCount }} 次播放</span>
          <button class="btn danger delete" type="button" @click="$emit('delete')">删除视频</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { streamUrl } from '../api/video'

const props = defineProps({
  video: { type: Object, required: true },
})
defineEmits(['close', 'like', 'delete', 'play'])

const videoRef = ref(null)
const storageKey = () => `media-manager:play-position:${props.video.id}`

const restorePosition = () => {
  const saved = Number(localStorage.getItem(storageKey()))
  if (!videoRef.value || !Number.isFinite(saved) || saved <= 0) return
  const duration = videoRef.value.duration
  if (Number.isFinite(duration) && saved >= duration - 1) return
  videoRef.value.currentTime = saved
}

const savePosition = () => {
  if (!videoRef.value) return
  const seconds = Math.floor(videoRef.value.currentTime || 0)
  if (seconds > 0) localStorage.setItem(storageKey(), String(seconds))
}

const clearPosition = () => {
  localStorage.removeItem(storageKey())
}
</script>

<style scoped>
.overlay { position: fixed; inset: 0; z-index: 80; padding: 24px; background: rgba(15,23,42,.52); backdrop-filter: blur(6px); display: grid; place-items: center; }
.player { width: min(1040px, 100%); max-height: 92vh; overflow: auto; border-radius: var(--radius-xl); padding: 18px; background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-lg); }
.video-shell { border-radius: 16px; overflow: hidden; background: #000; box-shadow: 0 14px 40px rgba(15,23,42,.26); }
video { width: 100%; max-height: 56vh; display: block; background: #000; }
.details { padding: 17px 4px 4px; }
.title-row { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.eyebrow { margin: 0 0 5px; color: var(--accent); font-size: 11px; letter-spacing: .14em; font-weight: 800; }
h2 { margin: 0; font-size: clamp(20px, 3vw, 28px); }
.close { padding: 8px 11px; border-radius: 10px; }
.description { color: var(--muted); line-height: 1.7; margin: 11px 0 13px; }
.tags { display: flex; flex-wrap: wrap; gap: 7px; margin-bottom: 15px; }
.tag { padding: 5px 9px; border-radius: 999px; border: 1px solid var(--line); color: var(--muted); font-size: 12px; background: #f8fafc; }
.tag.category { color: var(--accent); border-color: #cfe2ff; background: var(--accent-soft); }
.meta-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 10px; }
.meta-item { border: 1px solid var(--line); background: #f8fafc; border-radius: 13px; padding: 11px 12px; }
.meta-item span { display: block; color: var(--muted); font-size: 11px; margin-bottom: 5px; }
.meta-item strong { font-size: 13px; }
.actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-top: 16px; }
.like { border-color: #cfe2ff; color: var(--accent); background: var(--accent-soft); }
.delete { margin-left: auto; }
@media (max-width: 720px) { .overlay { padding: 10px; } .player { padding: 12px; } }
</style>
