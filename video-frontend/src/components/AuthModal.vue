<template>
  <transition name="fade">
    <div v-if="open" class="auth-overlay" @click.self="close">
      <section class="auth-shell" aria-modal="true" role="dialog">
        <aside class="story-panel">
          <div class="story-brand">
            <span class="story-logo">V</span>
            <span>VideoHub</span>
          </div>

          <div class="story-copy">
            <p class="eyebrow">MEDIA MANAGER / V2</p>
            <h1>让每一次上传，<br />都有迹可循。</h1>
            <p>安全登录、缓存加速、封面呈现与链路日志，一站式管理你的视频内容。</p>
          </div>

          <div class="story-orbit" aria-hidden="true">
            <span class="orbit orbit-a"></span>
            <span class="orbit orbit-b"></span>
            <span class="orbit-dot"></span>
          </div>
        </aside>

        <main class="form-panel">
          <button class="panel-close" type="button" aria-label="关闭" @click="close">×</button>

          <div class="form-head">
            <p class="eyebrow">{{ isLogin ? 'WELCOME BACK' : 'CREATE ACCOUNT' }}</p>
            <h2>{{ isLogin ? '登录 VideoHub' : '创建你的账号' }}</h2>
            <p>{{ isLogin ? '继续管理、上传和分享你的视频。' : '注册后即可上传视频并管理个人内容。' }}</p>
          </div>

          <div class="tabs" role="tablist">
            <button type="button" :class="{ active: isLogin }" @click="switchMode('login')">登录</button>
            <button type="button" :class="{ active: !isLogin }" @click="switchMode('register')">注册</button>
          </div>

          <form class="auth-form" @submit.prevent="submit">
            <label>
              <span>用户名</span>
              <div class="input-wrap">
                <i>@</i>
                <input
                  v-model.trim="form.username"
                  autocomplete="username"
                  maxlength="32"
                  placeholder="3-32 位字母、数字或下划线"
                />
              </div>
            </label>

            <label v-if="!isLogin">
              <span>昵称 <em>选填</em></span>
              <div class="input-wrap">
                <i>✦</i>
                <input v-model.trim="form.nickname" maxlength="50" placeholder="展示在视频卡片上的名字" />
              </div>
            </label>

            <label>
              <span>密码</span>
              <div class="input-wrap">
                <i>◇</i>
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  :autocomplete="isLogin ? 'current-password' : 'new-password'"
                  maxlength="64"
                  placeholder="至少 6 位"
                />
                <button class="eye" type="button" @click="showPassword = !showPassword">
                  {{ showPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </label>

            <label v-if="!isLogin">
              <span>确认密码</span>
              <div class="input-wrap">
                <i>◇</i>
                <input
                  v-model="form.confirmPassword"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="new-password"
                  maxlength="64"
                  placeholder="再次输入密码"
                />
              </div>
            </label>

            <p v-if="error" class="form-error">{{ error }}</p>

            <button class="submit-btn" type="submit" :disabled="loading">
              <span v-if="loading" class="spinner"></span>
              {{ loading ? '请稍候...' : (isLogin ? '进入控制台' : '注册并登录') }}
            </button>
          </form>

          <p class="legal">登录即表示你同意遵循平台内容与版权规范。</p>
        </main>
      </section>
    </div>
  </transition>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { login, register } from '../api/auth'

const props = defineProps({
  open: { type: Boolean, default: false },
  mode: { type: String, default: 'login' },
})
const emit = defineEmits(['close', 'success'])

const form = reactive({ username: '', nickname: '', password: '', confirmPassword: '' })
const currentMode = ref(props.mode)
const loading = ref(false)
const error = ref('')
const showPassword = ref(false)
const isLogin = computed(() => currentMode.value === 'login')

watch(() => props.mode, (mode) => switchMode(mode))
watch(() => props.open, (open) => {
  if (open) {
    currentMode.value = props.mode
    error.value = ''
    showPassword.value = false
  }
})

const switchMode = (mode) => {
  currentMode.value = mode === 'register' ? 'register' : 'login'
  error.value = ''
}

const close = () => {
  if (loading.value) return
  emit('close')
}

const submit = async () => {
  error.value = ''
  if (!form.username) return (error.value = '请输入用户名')
  if (!form.password) return (error.value = '请输入密码')
  if (!isLogin.value && form.password.length < 6) return (error.value = '密码至少需要 6 位')
  if (!isLogin.value && form.password !== form.confirmPassword) return (error.value = '两次输入的密码不一致')

  loading.value = true
  try {
    const data = isLogin.value
      ? await login({ username: form.username, password: form.password })
      : await register({ username: form.username, password: form.password, nickname: form.nickname })
    emit('success', data)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-overlay {
  position: fixed; inset: 0; z-index: 120; padding: 22px;
  display: grid; place-items: center; background: rgba(5, 10, 24, .66); backdrop-filter: blur(12px);
}
.auth-shell {
  width: min(1040px, 100%); min-height: min(680px, calc(100vh - 44px)); max-height: calc(100vh - 44px);
  display: grid; grid-template-columns: 1.04fr .96fr; overflow: hidden; border-radius: 30px;
  background: #fff; box-shadow: 0 40px 120px rgba(0, 0, 0, .38); border: 1px solid rgba(255,255,255,.16);
}
.story-panel {
  position: relative; overflow: hidden; padding: 42px; color: #fff;
  background:
    radial-gradient(circle at 76% 18%, rgba(90, 199, 255, .36), transparent 28%),
    radial-gradient(circle at 20% 82%, rgba(66, 112, 255, .34), transparent 34%),
    linear-gradient(145deg, #071226 0%, #0c2350 54%, #0b54b4 100%);
}
.story-brand { display: flex; align-items: center; gap: 11px; font-size: 17px; font-weight: 800; letter-spacing: .02em; position: relative; z-index: 2; }
.story-logo { width: 38px; height: 38px; border-radius: 12px; display: grid; place-items: center; background: #fff; color: #0b5cd7; font-size: 19px; font-weight: 950; }
.story-copy { position: relative; z-index: 2; margin-top: 82px; max-width: 420px; }
.eyebrow { margin: 0 0 12px; font-size: 10px; font-weight: 800; letter-spacing: .2em; color: #8cc7ff; }
.story-copy .eyebrow { color: #8cc7ff; }
.story-copy h1 { margin: 0 0 20px; font-size: clamp(38px, 4.5vw, 58px); line-height: 1.05; letter-spacing: -.045em; }
.story-copy p:last-child { margin: 0; color: rgba(255,255,255,.64); line-height: 1.8; font-size: 14px; }
.story-orbit { position: absolute; width: 430px; height: 430px; right: -170px; bottom: -135px; opacity: .85; }
.orbit { position: absolute; inset: 0; border-radius: 50%; border: 1px solid rgba(255,255,255,.16); }
.orbit-b { inset: 72px; border-color: rgba(117, 208, 255, .28); }
.orbit-dot { position: absolute; width: 14px; height: 14px; top: 48px; left: 90px; border-radius: 50%; background: #a4e3ff; box-shadow: 0 0 32px 8px rgba(108, 204, 255, .44); }
.form-panel { position: relative; padding: 52px clamp(30px, 5vw, 62px) 36px; display: flex; flex-direction: column; justify-content: center; background: radial-gradient(circle at 100% 0%, #f0f7ff, transparent 40%), #fff; }
.panel-close { position: absolute; top: 20px; right: 20px; width: 36px; height: 36px; border: 1px solid var(--line); border-radius: 50%; background: #fff; color: var(--muted); font-size: 20px; transition: all .18s ease; }
.panel-close:hover { color: var(--text); border-color: #b8c7dd; transform: rotate(90deg); }
.form-head { margin-bottom: 24px; }
.form-head .eyebrow { color: var(--accent); }
.form-head h2 { margin: 0 0 9px; font-size: clamp(28px, 3.7vw, 38px); letter-spacing: -.035em; }
.form-head > p:last-child { margin: 0; color: var(--muted); font-size: 13px; }
.tabs { display: grid; grid-template-columns: 1fr 1fr; gap: 5px; padding: 4px; border-radius: 13px; background: #f1f5fa; margin-bottom: 20px; }
.tabs button { border: 0; border-radius: 9px; padding: 9px; color: var(--muted); background: transparent; font-size: 13px; }
.tabs button.active { color: var(--text); background: #fff; box-shadow: 0 3px 12px rgba(15, 23, 42, .08); font-weight: 800; }
.auth-form { display: grid; gap: 14px; }
.auth-form label { display: grid; gap: 7px; color: #4a5872; font-size: 11px; font-weight: 700; }
.auth-form label em { font-style: normal; color: #9aa6ba; font-weight: 400; }
.input-wrap { display: flex; align-items: center; gap: 9px; border: 1px solid #dce4ef; border-radius: 12px; padding: 0 12px; background: #fbfdff; transition: border-color .18s ease, box-shadow .18s ease, background .18s ease; }
.input-wrap:focus-within { border-color: var(--accent); background: #fff; box-shadow: 0 0 0 4px rgba(22, 119, 255, .09); }
.input-wrap i { color: #8ca0bd; font-style: normal; font-size: 13px; width: 14px; text-align: center; }
.input-wrap input { flex: 1; min-width: 0; border: 0; outline: 0; background: transparent; padding: 12px 0; color: var(--text); font-size: 14px; }
.input-wrap input::placeholder { color: #a9b4c6; }
.eye { border: 0; background: transparent; color: var(--accent); font-size: 10px; font-weight: 700; padding: 5px; }
.form-error { margin: 0; border-radius: 10px; padding: 9px 11px; color: #b4232b; background: #fff1f2; border: 1px solid #ffd3d6; font-size: 12px; line-height: 1.45; }
.submit-btn { margin-top: 4px; min-height: 46px; border: 0; border-radius: 12px; color: #fff; font-weight: 800; font-size: 14px; background: linear-gradient(135deg, #0b6cf2, #258cff); box-shadow: 0 14px 30px rgba(22, 119, 255, .24); display: flex; align-items: center; justify-content: center; gap: 9px; transition: transform .18s ease, box-shadow .18s ease; }
.submit-btn:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 18px 36px rgba(22, 119, 255, .3); }
.submit-btn:disabled { opacity: .68; cursor: wait; }
.spinner { width: 14px; height: 14px; border: 2px solid rgba(255,255,255,.35); border-top-color: #fff; border-radius: 50%; animation: spin .7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.legal { margin: 17px 0 0; text-align: center; color: #a0abc0; font-size: 10px; }
@media (max-width: 820px) {
  .auth-overlay { padding: 10px; }
  .auth-shell { grid-template-columns: 1fr; min-height: auto; max-height: calc(100vh - 20px); overflow-y: auto; border-radius: 22px; }
  .story-panel { min-height: 190px; padding: 25px; }
  .story-copy { margin-top: 28px; }
  .story-copy h1 { font-size: 31px; margin-bottom: 10px; }
  .story-copy p:last-child, .story-orbit { display: none; }
  .form-panel { padding: 32px 24px 28px; }
}
</style>
