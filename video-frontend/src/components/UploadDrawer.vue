<template>
  <transition name="slide">
    <aside v-if="open" class="drawer glass">
      <header>
        <div>
          <p class="eyebrow">UPLOAD STUDIO</p>
          <h2>上传新视频</h2>
        </div>
        <button class="btn ghost icon" type="button" @click="close">✕</button>
      </header>

      <div
        class="dropzone"
        :class="{ active: dragging }"
        @dragenter.prevent="dragging = true"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
        @click="fileInput?.click()"
      >
        <input ref="fileInput" type="file" accept="video/*" hidden @change="onFileChange" />
        <div class="drop-icon">⇪</div>
        <p v-if="!file">拖拽视频到这里，或点击选择文件</p>
        <p v-else class="file-name">{{ file.name }}</p>
        <small>{{ file ? formatSize(file.size) : '支持 mp4 / webm / mkv / mov / avi' }}</small>
      </div>

      <div class="form-grid">
        <label>
          <span>标题</span>
          <input v-model.trim="form.title" class="field" placeholder="留空时使用原文件名" />
        </label>
        <label>
          <span>分类</span>
          <input v-model.trim="form.category" class="field" list="category-options" placeholder="例如：教程 / 娱乐" />
          <datalist id="category-options">
            <option v-for="item in categories.filter((c) => c !== '全部')" :key="item" :value="item" />
          </datalist>
        </label>
        <label class="full">
          <span>简介</span>
          <textarea v-model.trim="form.description" class="field" rows="3" placeholder="这段视频主要讲什么？"></textarea>
        </label>
        <label>
          <span>标签</span>
          <input v-model.trim="form.tags" class="field" placeholder="逗号分隔，例如 Vue,MyBatis" />
        </label>
        <label>
          <span>上传者</span>
          <input v-model.trim="form.uploader" class="field" placeholder="你的昵称" />
        </label>
      </div>

      <div v-if="uploading" class="progress">
        <div class="bar"><span :style="{ width: `${progress}%` }"></span></div>
        <div class="progress-meta">
          <strong>{{ progress }}%</strong>
          <span>正在上传，请勿关闭页面</span>
        </div>
      </div>

      <footer>
        <button class="btn ghost" type="button" :disabled="uploading" @click="close">取消</button>
        <button class="btn primary" type="button" :disabled="uploading || !file" @click="submit">
          {{ uploading ? '上传中...' : '开始上传' }}
        </button>
      </footer>
    </aside>
  </transition>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { uploadVideo } from '../api/video'

const props = defineProps({
  open: { type: Boolean, default: false },
  categories: { type: Array, default: () => [] },
})
const emit = defineEmits(['close', 'uploaded', 'notify'])

const fileInput = ref(null)
const file = ref(null)
const dragging = ref(false)
const uploading = ref(false)
const progress = ref(0)
const form = reactive({ title: '', description: '', category: '', tags: '', uploader: '' })

watch(() => props.open, (open) => {
  if (open) progress.value = 0
})

const close = () => {
  if (uploading.value) return
  reset()
  emit('close')
}

const reset = () => {
  file.value = null
  dragging.value = false
  Object.assign(form, { title: '', description: '', category: '', tags: '', uploader: '' })
  if (fileInput.value) fileInput.value.value = ''
}

const onDrop = (event) => {
  dragging.value = false
  const dropped = event.dataTransfer?.files?.[0]
  if (dropped) file.value = dropped
}

const onFileChange = (event) => {
  file.value = event.target.files?.[0] || null
}

const formatSize = (size = 0) => {
  const units = ['B', 'KB', 'MB', 'GB']
  let value = size
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(index === 0 ? 0 : 2)} ${units[index]}`
}

const readDuration = (selected) => new Promise((resolve) => {
  const video = document.createElement('video')
  const url = URL.createObjectURL(selected)
  let settled = false
  let timer
  const finish = (value) => {
    if (settled) return
    settled = true
    clearTimeout(timer)
    URL.revokeObjectURL(url)
    const seconds = Number(value)
    resolve(Number.isFinite(seconds) && seconds > 0 ? Math.round(seconds) : 0)
  }
  video.preload = 'metadata'
  video.onloadedmetadata = () => finish(video.duration)
  video.onerror = () => finish(0)
  timer = setTimeout(() => finish(0), 6000)
  video.src = url
})

const submit = async () => {
  if (!file.value) return emit('notify', { type: 'error', message: '请先选择视频文件' })
  uploading.value = true
  progress.value = 0
  try {
    const duration = await readDuration(file.value)
    const payload = new FormData()
    payload.append('file', file.value)
    payload.append('title', form.title)
    payload.append('description', form.description)
    payload.append('category', form.category)
    payload.append('tags', form.tags)
    payload.append('uploader', form.uploader)
    payload.append('duration', String(duration))
    await uploadVideo(payload, (event) => {
      progress.value = event.total ? Math.min(100, Math.round(event.loaded / event.total * 100)) : 0
    })
    emit('notify', { type: 'success', message: '视频上传成功' })
    reset()
    emit('uploaded')
  } catch (error) {
    emit('notify', { type: 'error', message: error.message })
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.drawer { position: fixed; z-index: 70; right: 18px; top: 18px; bottom: 18px; width: min(520px, calc(100vw - 36px)); border-radius: var(--radius-xl); padding: 22px; overflow: auto; background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-lg); display: flex; flex-direction: column; gap: 18px; }
header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.eyebrow { margin: 0 0 5px; color: var(--accent); font-size: 11px; letter-spacing: .14em; font-weight: 800; }
h2 { margin: 0; font-size: 25px; }
.icon { padding: 8px 11px; border-radius: 10px; }
.dropzone { border: 1px dashed #9cc7ff; border-radius: 17px; padding: 26px 18px; display: grid; place-items: center; text-align: center; gap: 6px; background: #f5f9ff; transition: border-color .18s ease, transform .18s ease; cursor: pointer; }
.dropzone.active { border-color: var(--accent); transform: scale(1.01); }
.drop-icon { width: 48px; height: 48px; border-radius: 15px; display: grid; place-items: center; font-size: 24px; background: var(--accent-soft); color: var(--accent); }
.dropzone p { margin: 0; color: var(--text); }
.dropzone small { color: var(--muted); }
.file-name { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 700; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 13px; }
label { display: grid; gap: 7px; color: var(--muted); font-size: 12px; }
label.full { grid-column: 1 / -1; }
textarea { resize: vertical; min-height: 86px; }
.progress { display: grid; gap: 8px; }
.bar { height: 9px; border-radius: 999px; overflow: hidden; background: #eaf0f8; }
.bar span { display: block; height: 100%; background: linear-gradient(135deg, var(--accent), #7cb7ff); transition: width .2s ease; }
.progress-meta { display: flex; justify-content: space-between; color: var(--muted); font-size: 12px; }
.progress-meta strong { color: var(--accent); }
footer { margin-top: auto; display: flex; justify-content: flex-end; gap: 10px; }
@media (max-width: 620px) { .form-grid { grid-template-columns: 1fr; } .drawer { inset: 10px; width: auto; } }
</style>
