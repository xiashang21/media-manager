<template>
  <transition name="slide">
    <aside v-if="open" class="drawer glass">
      <header>
        <div>
          <p class="eyebrow">UPLOAD STUDIO</p>
          <h2>上传新视频</h2>
          <p class="subtle">以 <strong>{{ currentUser?.nickname || currentUser?.username }}</strong> 的身份发布</p>
        </div>
        <button class="btn ghost icon" type="button" @click="close">✕</button>
      </header>

      <div
        class="dropzone"
        :class="{ active: dragging, filled: !!file }"
        @dragenter.prevent="dragging = true"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
        @click="fileInput?.click()"
      >
        <input ref="fileInput" type="file" accept="video/*" hidden @change="onFileChange" />
        <div class="drop-icon">{{ file ? '✓' : '⇪' }}</div>
        <p v-if="!file">拖拽视频到这里，或点击选择文件</p>
        <p v-else class="file-name">{{ file.name }}</p>
        <small>{{ file ? formatSize(file.size) : '支持 mp4 / webm / mkv / mov / avi' }}</small>
      </div>

      <section class="cover-section">
        <div class="cover-copy">
          <div>
            <p class="eyebrow">COVER FRAME</p>
            <h3>视频封面</h3>
          </div>
          <span v-if="coverFile" class="cover-state">{{ coverSource === 'custom' ? '自定义图片' : '首帧自动生成' }}</span>
        </div>

        <div class="cover-workbench" :class="{ empty: !coverPreview }">
          <img v-if="coverPreview" :src="coverPreview" alt="视频封面预览" />
          <div v-else class="cover-placeholder">
            <span>{{ generating ? '…' : '▧' }}</span>
            <p>{{ generating ? '正在提取视频首帧' : '选择视频后自动生成封面' }}</p>
          </div>

          <div class="cover-tools">
            <button class="btn ghost" type="button" @click.stop="coverInput?.click()">
              {{ coverPreview ? '更换封面' : '选择图片' }}
            </button>
            <button v-if="coverFile" class="btn ghost danger-text" type="button" @click.stop="removeCover">不使用</button>
          </div>
          <input ref="coverInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="onCoverChange" />
          <span class="cover-ratio">16 : 9 · JPG / PNG / WebP · ≤ 5MB</span>
        </div>
      </section>

      <div class="form-grid">
        <label>
          <span>标题</span>
          <input v-model.trim="form.title" class="field" maxlength="200" placeholder="留空时使用原文件名" />
        </label>
        <label>
          <span>分类</span>
          <input v-model.trim="form.category" class="field" maxlength="50" list="category-options" placeholder="例如：教程 / 娱乐" />
          <datalist id="category-options">
            <option v-for="item in categories.filter((c) => c !== '全部')" :key="item" :value="item" />
          </datalist>
        </label>
        <label class="full">
          <span>简介</span>
          <textarea v-model.trim="form.description" class="field" maxlength="500" rows="3" placeholder="这段视频主要讲什么？"></textarea>
        </label>
        <label class="full">
          <span>标签</span>
          <input v-model.trim="form.tags" class="field" maxlength="255" placeholder="逗号分隔，例如 Vue,MyBatis" />
        </label>
      </div>

      <div v-if="uploading" class="progress">
        <div class="bar"><span :style="{ width: `${progress}%` }"></span></div>
        <div class="progress-meta">
          <strong>{{ progress }}%</strong>
          <span>正在上传视频与封面，请勿关闭页面</span>
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
import { onUnmounted, reactive, ref, watch } from 'vue'
import { uploadVideo } from '../api/video'

const props = defineProps({
  open: { type: Boolean, default: false },
  categories: { type: Array, default: () => [] },
  currentUser: { type: Object, default: null },
})
const emit = defineEmits(['close', 'uploaded', 'notify'])

const fileInput = ref(null)
const coverInput = ref(null)
const file = ref(null)
const coverFile = ref(null)
const coverPreview = ref('')
const coverSource = ref('')
const dragging = ref(false)
const generating = ref(false)
const uploading = ref(false)
const progress = ref(0)
const form = reactive({ title: '', description: '', category: '', tags: '' })

let coverObjectUrl = ''

watch(() => props.open, (open) => {
  if (open) {
    progress.value = 0
  } else if (!uploading.value) {
    reset()
  }
})

const clearPreview = () => {
  if (coverObjectUrl) URL.revokeObjectURL(coverObjectUrl)
  coverObjectUrl = ''
  coverPreview.value = ''
}

const close = () => {
  if (uploading.value) return
  reset()
  emit('close')
}

const reset = () => {
  file.value = null
  coverFile.value = null
  coverSource.value = ''
  generating.value = false
  clearPreview()
  dragging.value = false
  progress.value = 0
  Object.assign(form, { title: '', description: '', category: '', tags: '' })
  if (fileInput.value) fileInput.value.value = ''
  if (coverInput.value) coverInput.value.value = ''
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

const setCover = (blob, source, name = 'cover.jpg') => {
  clearPreview()
  const normalized = blob instanceof File ? blob : new File([blob], name, { type: blob.type || 'image/jpeg' })
  coverFile.value = normalized
  coverSource.value = source
  coverObjectUrl = URL.createObjectURL(normalized)
  coverPreview.value = coverObjectUrl
}

const captureVideoFrame = (selected) => new Promise((resolve) => {
  const video = document.createElement('video')
  const objectUrl = URL.createObjectURL(selected)
  let settled = false
  let timer

  const finish = (blob) => {
    if (settled) return
    settled = true
    clearTimeout(timer)
    URL.revokeObjectURL(objectUrl)
    resolve(blob)
  }

  const draw = () => {
    try {
      const width = 960
      const height = 540
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const context = canvas.getContext('2d')
      const sourceWidth = video.videoWidth || width
      const sourceHeight = video.videoHeight || height
      const scale = Math.max(width / sourceWidth, height / sourceHeight)
      const drawWidth = sourceWidth * scale
      const drawHeight = sourceHeight * scale
      context.drawImage(video, (width - drawWidth) / 2, (height - drawHeight) / 2, drawWidth, drawHeight)
      canvas.toBlob((blob) => finish(blob), 'image/jpeg', 0.88)
    } catch {
      finish(null)
    }
  }

  video.muted = true
  video.playsInline = true
  video.preload = 'auto'
  video.onloadedmetadata = () => {
    const target = Number.isFinite(video.duration) ? Math.min(Math.max(video.duration * 0.08, 0.1), 2) : 0.1
    try {
      video.currentTime = target
    } catch {
      draw()
    }
  }
  video.onseeked = draw
  video.onloadeddata = () => {
    if (video.readyState >= 2 && video.currentTime === 0) draw()
  }
  video.onerror = () => finish(null)
  timer = setTimeout(() => finish(null), 10000)
  video.src = objectUrl
})

const selectVideo = async (selected) => {
  if (!selected) return
  file.value = selected
  if (!form.title) form.title = selected.name.replace(/\.[^.]+$/, '')
  if (coverSource.value === 'custom') return

  generating.value = true
  const frame = await captureVideoFrame(selected)
  generating.value = false
  if (frame) setCover(frame, 'auto', 'video-frame.jpg')
  else removeCover()
}

const onDrop = (event) => {
  dragging.value = false
  selectVideo(event.dataTransfer?.files?.[0])
}

const onFileChange = (event) => {
  selectVideo(event.target.files?.[0] || null)
}

const onCoverChange = (event) => {
  const selected = event.target.files?.[0]
  if (!selected) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(selected.type)) {
    emit('notify', { type: 'error', message: '封面仅支持 JPG、PNG 或 WebP 图片' })
    return
  }
  if (selected.size > 5 * 1024 * 1024) {
    emit('notify', { type: 'error', message: '封面大小不能超过 5MB' })
    return
  }
  setCover(selected, 'custom', selected.name)
}

const removeCover = () => {
  coverFile.value = null
  coverSource.value = ''
  clearPreview()
  if (coverInput.value) coverInput.value.value = ''
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
    if (coverFile.value) payload.append('cover', coverFile.value)
    payload.append('title', form.title)
    payload.append('description', form.description)
    payload.append('category', form.category)
    payload.append('tags', form.tags)
    payload.append('duration', String(duration))
    await uploadVideo(payload, (event) => {
      progress.value = event.total ? Math.min(100, Math.round(event.loaded / event.total * 100)) : 0
    })
    emit('notify', { type: 'success', message: '视频与封面上传成功' })
    reset()
    emit('uploaded')
  } catch (error) {
    emit('notify', { type: 'error', message: error.message })
  } finally {
    uploading.value = false
  }
}

onUnmounted(clearPreview)
</script>

<style scoped>
.drawer { position: fixed; z-index: 70; right: 18px; top: 18px; bottom: 18px; width: min(560px, calc(100vw - 36px)); border-radius: var(--radius-xl); padding: 22px; overflow: auto; background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-lg); display: flex; flex-direction: column; gap: 17px; }
header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.eyebrow { margin: 0 0 5px; color: var(--accent); font-size: 10px; letter-spacing: .16em; font-weight: 800; }
h2 { margin: 0; font-size: 25px; letter-spacing: -.02em; }
.subtle { margin: 6px 0 0; color: var(--muted); font-size: 11px; }
.subtle strong { color: var(--text); }
.icon { padding: 8px 11px; border-radius: 10px; }
.dropzone { border: 1px dashed #9cc7ff; border-radius: 17px; padding: 22px 18px; display: grid; place-items: center; text-align: center; gap: 6px; background: #f5f9ff; transition: border-color .18s ease, transform .18s ease, background .18s ease; cursor: pointer; }
.dropzone.active { border-color: var(--accent); transform: scale(1.01); }
.dropzone.filled { background: #f3fbf7; border-color: #9adbb9; }
.drop-icon { width: 46px; height: 46px; border-radius: 14px; display: grid; place-items: center; font-size: 23px; background: var(--accent-soft); color: var(--accent); }
.dropzone.filled .drop-icon { color: var(--success); background: #e5f8ee; }
.dropzone p { margin: 0; color: var(--text); }
.dropzone small { color: var(--muted); }
.file-name { max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 700; }
.cover-section { display: grid; gap: 9px; }
.cover-copy { display: flex; align-items: flex-end; justify-content: space-between; gap: 10px; }
.cover-copy h3 { margin: 0; font-size: 15px; }
.cover-state { border-radius: 999px; padding: 5px 9px; color: var(--accent); background: var(--accent-soft); font-size: 10px; font-weight: 700; }
.cover-workbench { position: relative; aspect-ratio: 16 / 9; max-height: 230px; overflow: hidden; border-radius: 15px; border: 1px solid var(--line); background: #f5f8fd; }
.cover-workbench img { width: 100%; height: 100%; object-fit: cover; display: block; }
.cover-placeholder { height: 100%; display: grid; place-items: center; align-content: center; gap: 7px; color: var(--muted); background: linear-gradient(135deg, #f4f8ff, #eef4fb); }
.cover-placeholder span { font-size: 36px; color: #9bb9e0; }
.cover-placeholder p { margin: 0; font-size: 11px; }
.cover-tools { position: absolute; left: 10px; bottom: 10px; display: flex; gap: 7px; }
.cover-tools .btn { padding: 7px 10px; border-radius: 9px; font-size: 11px; background: rgba(255,255,255,.9); backdrop-filter: blur(8px); }
.danger-text { color: #d33c44; }
.cover-ratio { position: absolute; right: 10px; bottom: 12px; padding: 5px 8px; border-radius: 7px; color: rgba(255,255,255,.84); background: rgba(8, 17, 34, .55); font-size: 9px; backdrop-filter: blur(8px); }
.cover-workbench.empty .cover-ratio { color: #8494ac; background: transparent; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 13px; }
label { display: grid; gap: 7px; color: var(--muted); font-size: 12px; }
label.full { grid-column: 1 / -1; }
textarea { resize: vertical; min-height: 86px; }
.progress { display: grid; gap: 8px; }
.bar { height: 9px; border-radius: 999px; overflow: hidden; background: #eaf0f8; }
.bar span { display: block; height: 100%; background: linear-gradient(135deg, var(--accent), #7cb7ff); transition: width .2s ease; }
.progress-meta { display: flex; justify-content: space-between; color: var(--muted); font-size: 12px; }
.progress-meta strong { color: var(--accent); }
footer { margin-top: auto; display: flex; justify-content: flex-end; gap: 10px; padding-top: 3px; }
@media (max-width: 620px) { .form-grid { grid-template-columns: 1fr; } .drawer { inset: 10px; width: auto; } label.full { grid-column: auto; } .cover-ratio { display: none; } }
</style>
