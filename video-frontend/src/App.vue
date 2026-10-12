<template>
  <div class="page">
    <header class="topbar">
      <div class="brand">
        <div class="logo">V</div>
        <div class="brand-text">
          <strong>VideoHub</strong>
          <span>media-manager</span>
        </div>
      </div>

      <div class="search">
        <span class="search-icon">⌕</span>
        <input v-model.trim="query.keyword" placeholder="搜索视频标题或简介..." @keyup.enter="reload" />
        <button class="btn primary" type="button" @click="reload">搜索</button>
      </div>

      <div class="top-actions">
        <select v-model="query.category" class="select" @change="reload">
          <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
        </select>
        <select v-model="query.sortBy" class="select" @change="reload">
          <option value="create_time">最新上传</option>
          <option value="play_count">播放最多</option>
          <option value="like_count">点赞最多</option>
          <option value="size">文件最大</option>
        </select>
        <button class="btn ghost" type="button" @click="toggleOrder">
          {{ query.order === 'desc' ? '↓ 降序' : '↑ 升序' }}
        </button>
        <button class="btn primary" type="button" @click="showUpload = true">＋ 上传视频</button>
      </div>
    </header>

    <nav class="category-bar">
      <button
        v-for="item in categories"
        :key="item"
        type="button"
        class="category-chip"
        :class="{ active: query.category === item }"
        @click="selectCategory(item)"
      >
        {{ item }}
      </button>
    </nav>

    <section v-if="heroVideo" class="hero">
      <div class="hero-copy">
        <span class="pill accent">🔥 热门推荐</span>
        <h1>{{ heroVideo.title }}</h1>
        <p>{{ heroVideo.description || '选择一个视频，立即开始播放。' }}</p>
        <div class="hero-meta">
          <span>{{ heroVideo.category }}</span>
          <span>▶ {{ heroVideo.playCount }} 次播放</span>
          <span>✦ {{ heroVideo.likeCount }} 点赞</span>
          <span>{{ heroVideo.durationText }}</span>
        </div>
        <div class="hero-actions">
          <button class="btn primary" type="button" @click="openPlayer(heroVideo)">立即播放</button>
          <button class="btn ghost" type="button" @click="query.sortBy = 'play_count'; reload()">查看热门</button>
        </div>
      </div>
      <div class="hero-art" @click="openPlayer(heroVideo)">
        <img v-if="heroVideo.coverUrl" :src="heroVideo.coverUrl" :alt="heroVideo.title" />
        <div v-else class="hero-fallback"><span>{{ heroVideo.title?.slice(0, 1) }}</span></div>
        <div class="hero-play">▶</div>
      </div>
    </section>

    <div class="content">
      <main class="catalog">
        <div class="section-head">
          <div>
            <p class="eyebrow">LIBRARY</p>
            <h2>{{ query.category === '全部' ? '全部视频' : query.category }}</h2>
          </div>
          <span class="pill">{{ total }} 个结果</span>
        </div>

        <div v-if="loading" class="video-grid">
          <div v-for="n in 6" :key="n" class="skeleton"></div>
        </div>
        <div v-else-if="!videos.length" class="empty glass">
          <strong>没有找到视频</strong>
          <span>换个关键词，或者上传一个新的视频试试。</span>
        </div>
        <div v-else class="video-grid">
          <VideoCard v-for="video in videos" :key="video.id" :video="video" @play="openPlayer" />
        </div>

        <div v-if="totalPages > 1" class="pagination">
          <button class="btn ghost" type="button" :disabled="query.page <= 1" @click="changePage(query.page - 1)">上一页</button>
          <span>{{ query.page }} / {{ totalPages }}</span>
          <button class="btn ghost" type="button" :disabled="query.page >= totalPages" @click="changePage(query.page + 1)">下一页</button>
        </div>
      </main>

      <aside class="hot-panel glass">
        <div class="section-head compact">
          <div>
            <p class="eyebrow">TRENDING</p>
            <h2>播放榜</h2>
          </div>
        </div>
        <button
          v-for="(video, index) in hotVideos"
          :key="video.id"
          class="hot-item"
          type="button"
          @click="openPlayer(video)"
        >
          <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
          <span class="hot-info">
            <strong>{{ video.title }}</strong>
            <small>▶ {{ video.playCount }} · {{ video.sizeText }}</small>
          </span>
        </button>
      </aside>
    </div>

    <UploadDrawer
      :open="showUpload"
      :categories="categories"
      @close="showUpload = false"
      @uploaded="handleUploaded"
      @notify="notify"
    />

    <PlayerModal
      v-if="current"
      :video="current"
      @close="closePlayer"
      @play="handlePlay"
      @like="handleLike"
      @delete="handleDelete"
    />

    <div class="toast-stack">
      <transition-group name="slide">
        <div v-for="toast in toasts" :key="toast.id" class="toast glass" :class="toast.type">
          {{ toast.message }}
        </div>
      </transition-group>
    </div>
  </div>
</template>
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import VideoCard from './components/VideoCard.vue'
import UploadDrawer from './components/UploadDrawer.vue'
import PlayerModal from './components/PlayerModal.vue'
import {
  fetchCategories,
  fetchHotVideos,
  fetchVideoDetail,
  fetchVideoPage,
  increaseLike,
  increasePlay,
  removeVideo,
} from './api/video'

const videos = ref([])
const categories = ref(['全部'])
const hotVideos = ref([])
const loading = ref(false)
const total = ref(0)
const showUpload = ref(false)
const current = ref(null)
const toasts = ref([])

const query = reactive({
  page: 1,
  size: 9,
  keyword: '',
  category: '全部',
  sortBy: 'create_time',
  order: 'desc',
})

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / query.size)))
const heroVideo = computed(() => hotVideos.value[0] || videos.value[0] || null)

const notify = ({ type = 'success', message }) => {
  const id = Date.now() + Math.random()
  toasts.value.push({ id, type, message })
  setTimeout(() => {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }, 2800)
}

const loadVideos = async () => {
  loading.value = true
  try {
    const data = await fetchVideoPage(query)
    videos.value = data.records || []
    total.value = data.total || 0
  } catch (error) {
    notify({ type: 'error', message: error.message })
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    categories.value = await fetchCategories()
  } catch (error) {
    notify({ type: 'error', message: error.message })
  }
}

const loadHot = async () => {
  try {
    hotVideos.value = await fetchHotVideos(6)
  } catch {
    hotVideos.value = []
  }
}

const reload = () => {
  query.page = 1
  loadVideos()
}

const selectCategory = (category) => {
  query.category = category
  reload()
}

const toggleOrder = () => {
  query.order = query.order === 'desc' ? 'asc' : 'desc'
  reload()
}

const changePage = (page) => {
  query.page = page
  loadVideos()
  window.scrollTo({ top: 360, behavior: 'smooth' })
}

const syncVideo = (id, patch) => {
  videos.value = videos.value.map((item) => item.id === id ? { ...item, ...patch } : item)
  hotVideos.value = hotVideos.value.map((item) => item.id === id ? { ...item, ...patch } : item)
  if (current.value?.id === id) current.value = { ...current.value, ...patch }
}

const openPlayer = async (video) => {
  try {
    current.value = await fetchVideoDetail(video.id)
  } catch {
    current.value = { ...video }
  }
}

const closePlayer = () => {
  current.value = null
}

const handlePlay = async () => {
  if (!current.value) return
  try {
    await increasePlay(current.value.id)
    syncVideo(current.value.id, { playCount: Number(current.value.playCount || 0) + 1 })
  } catch {
    // 播放量失败不影响观看
  }
}

const handleLike = async () => {
  if (!current.value) return
  try {
    await increaseLike(current.value.id)
    syncVideo(current.value.id, { likeCount: Number(current.value.likeCount || 0) + 1 })
    loadHot()
    notify({ message: '点赞成功' })
  } catch (error) {
    notify({ type: 'error', message: error.message })
  }
}

const handleDelete = async () => {
  if (!current.value) return
  if (!window.confirm(`确定删除《${current.value.title}》吗？`)) return
  try {
    await removeVideo(current.value.id)
    notify({ message: '视频已删除' })
    current.value = null
    await Promise.all([loadVideos(), loadCategories(), loadHot()])
  } catch (error) {
    notify({ type: 'error', message: error.message })
  }
}

const handleUploaded = () => {
  showUpload.value = false
  Promise.all([loadVideos(), loadCategories(), loadHot()])
}

onMounted(() => {
  loadVideos()
  loadCategories()
  loadHot()
})
</script>

<style scoped>
.page { width: min(1440px, 100%); margin: 0 auto; padding: 18px 18px 40px; }
.topbar {
  position: sticky; top: 0; z-index: 30; display: flex; align-items: center; gap: 18px;
  padding: 14px 16px; margin-bottom: 16px; border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, .96); border: 1px solid var(--line); box-shadow: var(--shadow);
  backdrop-filter: blur(16px);
}
.brand { display: flex; align-items: center; gap: 11px; flex: 0 0 auto; }
.logo {
  width: 42px; height: 42px; border-radius: 13px; display: grid; place-items: center;
  color: #fff; font-size: 20px; font-weight: 900; background: linear-gradient(135deg, var(--accent), #7cb7ff);
  box-shadow: 0 10px 24px rgba(22, 119, 255, .25);
}
.brand-text strong { display: block; font-size: 17px; }
.brand-text span { display: block; color: var(--muted); font-size: 11px; letter-spacing: .08em; }
.search {
  flex: 1; min-width: 260px; display: flex; align-items: center; gap: 10px;
  padding: 5px 6px 5px 13px; border: 1px solid var(--line-strong); border-radius: 14px; background: var(--panel-soft);
}
.search-icon { color: var(--accent); font-size: 19px; }
.search input { flex: 1; min-width: 120px; border: 0; background: transparent; outline: none; color: var(--text); padding: 9px 0; }
.top-actions { display: flex; align-items: center; gap: 9px; }
.top-actions .select { width: auto; min-width: 124px; padding: 9px 12px; }
.category-bar { display: flex; gap: 9px; overflow-x: auto; padding: 3px 2px 14px; }
.category-chip {
  flex: 0 0 auto; border: 1px solid var(--line); background: #fff; color: var(--muted);
  border-radius: 999px; padding: 8px 15px; transition: all .18s ease;
}
.category-chip:hover { color: var(--accent); border-color: #bcd7ff; }
.category-chip.active { color: #fff; border-color: var(--accent); background: var(--accent); box-shadow: 0 8px 18px rgba(22, 119, 255, .2); }
.hero {
  display: grid; grid-template-columns: 1.2fr .8fr; gap: 20px; align-items: center;
  border-radius: var(--radius-xl); padding: 24px; margin-bottom: 20px;
  color: #fff; background: linear-gradient(135deg, #1677ff 0%, #4c9bff 52%, #75b5ff 100%);
  box-shadow: 0 18px 50px rgba(22, 119, 255, .22);
}
.hero-copy { display: grid; gap: 13px; }
.hero .pill { width: fit-content; color: #fff; border-color: rgba(255,255,255,.42); background: rgba(255,255,255,.16); }
.hero h1 { margin: 0; font-size: clamp(26px, 4vw, 44px); line-height: 1.12; letter-spacing: -.02em; }
.hero p { margin: 0; max-width: 720px; color: rgba(255,255,255,.84); line-height: 1.75; }
.hero-meta { display: flex; flex-wrap: wrap; gap: 9px; font-size: 12px; color: rgba(255,255,255,.9); }
.hero-meta span { border: 1px solid rgba(255,255,255,.3); border-radius: 999px; padding: 6px 10px; background: rgba(255,255,255,.12); }
.hero-actions { display: flex; gap: 10px; }
.hero-actions .btn.ghost { color: #fff; border-color: rgba(255,255,255,.56); background: rgba(255,255,255,.1); }
.hero-actions .btn.primary { color: var(--accent); background: #fff; border-color: #fff; }
.hero-art { position: relative; min-height: 240px; border-radius: 18px; overflow: hidden; cursor: pointer; background: rgba(255,255,255,.14); }
.hero-art img { width: 100%; height: 100%; object-fit: cover; display: block; }
.hero-fallback { min-height: 240px; display: grid; place-items: center; background: linear-gradient(135deg, rgba(255,255,255,.24), rgba(255,255,255,.08)); }
.hero-fallback span { font-size: 84px; font-weight: 900; color: rgba(255,255,255,.86); }
.hero-play { position: absolute; right: 16px; bottom: 16px; width: 54px; height: 54px; border-radius: 50%; display: grid; place-items: center; color: var(--accent); background: #fff; box-shadow: 0 12px 30px rgba(0,0,0,.18); }
.content { display: grid; grid-template-columns: 1fr 300px; gap: 20px; align-items: start; }
.catalog { min-width: 0; display: grid; gap: 16px; }
.section-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 12px; }
.section-head.compact { margin-bottom: 9px; }
.eyebrow { margin: 0 0 4px; color: var(--accent); font-size: 11px; letter-spacing: .14em; font-weight: 800; }
.section-head h2 { margin: 0; font-size: 23px; }
.video-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 16px; }
.skeleton { height: 318px; border-radius: var(--radius-lg); background: linear-gradient(100deg, #eef2f8, #f8fafc, #eef2f8); background-size: 200% 100%; animation: shine 1.2s infinite; }
@keyframes shine { to { background-position: -200% 0; } }
.empty { border-radius: var(--radius-lg); padding: 58px 20px; text-align: center; display: grid; gap: 8px; color: var(--muted); background: #fff; }
.empty strong { color: var(--text); font-size: 18px; }
.pagination { display: flex; justify-content: center; align-items: center; gap: 12px; color: var(--muted); padding-top: 6px; }
.hot-panel { position: sticky; top: 86px; border-radius: var(--radius-lg); background: #fff; padding: 16px; }
.hot-item { width: 100%; border: 0; border-bottom: 1px solid var(--line); background: transparent; color: inherit; padding: 11px 4px; display: flex; align-items: center; gap: 11px; text-align: left; }
.hot-item:last-child { border-bottom: 0; }
.hot-item:hover strong { color: var(--accent); }
.rank { width: 26px; height: 26px; border-radius: 8px; display: grid; place-items: center; background: #f1f5f9; color: var(--muted); font-weight: 800; font-size: 12px; flex: 0 0 auto; }
.rank.top { color: #fff; background: linear-gradient(135deg, var(--accent), #66a8ff); }
.hot-info { min-width: 0; display: grid; gap: 4px; }
.hot-info strong { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.hot-info small { color: var(--muted); }
.toast-stack { position: fixed; z-index: 100; right: 22px; top: 22px; display: grid; gap: 10px; }
.toast { border-radius: 13px; padding: 12px 15px; min-width: 180px; max-width: 360px; background: #fff; color: var(--text); border-color: #cfe2ff; }
.toast.error { border-color: #f4c7c9; color: var(--danger); }
@media (max-width: 1120px) {
  .content { grid-template-columns: 1fr; }
  .hot-panel { position: static; }
}
@media (max-width: 860px) {
  .topbar { flex-wrap: wrap; }
  .search { order: 3; width: 100%; flex-basis: 100%; }
  .hero { grid-template-columns: 1fr; }
}
@media (max-width: 640px) {
  .page { padding: 10px 10px 30px; }
  .top-actions { width: 100%; flex-wrap: wrap; }
  .video-grid { grid-template-columns: 1fr; }
}
</style>

