<template>
  <article class="card" @click="$emit('play', video)">
    <div class="cover">
      <img
        v-if="video.coverUrl && !coverFailed"
        :src="streamUrl(video.coverUrl)"
        :alt="video.title"
        @error="coverFailed = true"
      />
      <div v-else class="cover-fallback">
        <span>{{ video.title?.slice(0, 1) || 'V' }}</span>
      </div>
      <span v-if="isMine" class="mine-badge">我的视频</span>
      <div class="badges">
        <span class="badge">{{ video.format?.toUpperCase() || 'VIDEO' }}</span>
        <span class="badge time">{{ video.durationText }}</span>
      </div>
      <button class="play" type="button" aria-label="播放视频">▶</button>
    </div>

    <div class="body">
      <div class="headline">
        <h3 :title="video.title">{{ video.title }}</h3>
      </div>
      <p class="desc">{{ video.description || '暂无简介' }}</p>

      <div class="tags">
        <span class="tag category">{{ video.category || '默认' }}</span>
        <span v-for="tag in video.tags?.slice(0, 2)" :key="tag" class="tag">{{ tag }}</span>
      </div>

      <div class="footer">
        <span>👤 {{ video.uploader || 'anonymous' }}</span>
        <span>📦 {{ video.sizeText || '0 B' }}</span>
        <span>🕒 {{ formatDate(video.createTime) }}</span>
      </div>
      <div class="stats">
        <span>▶ {{ formatNumber(video.playCount) }}</span>
        <span>✦ {{ formatNumber(video.likeCount) }}</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { streamUrl } from '../api/video'

const props = defineProps({
  video: { type: Object, required: true },
  currentUserId: { type: [Number, String], default: null },
})
defineEmits(['play'])

const coverFailed = ref(false)
const isMine = computed(() => props.currentUserId && Number(props.video.ownerId) === Number(props.currentUserId))
watch(() => props.video.coverUrl, () => { coverFailed.value = false })

const formatNumber = (value) => {
  const number = Number(value || 0)
  return number >= 10000 ? `${(number / 10000).toFixed(1)}w` : number
}

const formatDate = (value) => {
  if (!value) return '-'
  return new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}
</script>

<style scoped>
.card { overflow: hidden; border-radius: var(--radius-lg); border: 1px solid var(--line); background: #fff; box-shadow: 0 6px 18px rgba(31,45,78,.05); transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease; cursor: pointer; }
.card:hover { transform: translateY(-4px); border-color: #c8ddff; box-shadow: 0 16px 36px rgba(22,119,255,.13); }
.cover { position: relative; height: 172px; overflow: hidden; background: #eaf3ff; }
.cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.cover-fallback { height: 100%; display: grid; place-items: center; background: linear-gradient(135deg, #eaf3ff, #dcecff 55%, #cfe2ff); }
.cover-fallback span { font-size: 58px; font-weight: 900; color: #1677ff; opacity: .55; }
.badges { position: absolute; left: 10px; right: 10px; top: 10px; display: flex; justify-content: space-between; }
.badge { border-radius: 999px; padding: 4px 9px; font-size: 11px; font-weight: 800; letter-spacing: .03em; color: #fff; background: var(--accent); }
.badge.time { color: #fff; background: rgba(15,23,42,.62); }
.mine-badge { position: absolute; left: 10px; bottom: 10px; border-radius: 999px; padding: 4px 9px; color: #fff; background: rgba(10, 105, 242, .82); font-size: 10px; font-weight: 800; letter-spacing: .03em; backdrop-filter: blur(8px); }
.play { position: absolute; right: 12px; bottom: 12px; width: 40px; height: 40px; border-radius: 50%; border: 0; background: #fff; color: var(--accent); font-size: 15px; display: grid; place-items: center; box-shadow: 0 8px 20px rgba(15,23,42,.2); }
.play:hover { background: var(--accent); color: #fff; }
.body { padding: 13px 14px 14px; }
.headline h3 { margin: 0 0 6px; font-size: 15px; line-height: 1.35; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.desc { min-height: 36px; margin: 0 0 9px; color: var(--muted); font-size: 12px; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 10px; }
.tag { border-radius: 999px; padding: 4px 8px; font-size: 11px; color: var(--muted); background: #f8fafc; border: 1px solid var(--line); }
.tag.category { color: var(--accent); border-color: #cfe2ff; background: var(--accent-soft); }
.footer, .stats { display: flex; align-items: center; gap: 10px; font-size: 11px; color: var(--muted); }
.footer { flex-wrap: wrap; }
.stats { margin-top: 9px; padding-top: 9px; border-top: 1px solid var(--line); justify-content: space-between; color: #334155; }
</style>
