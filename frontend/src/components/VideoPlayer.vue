<template>
  <div class="video-wrap">
    <div ref="playerRoot" class="easy-player-box"></div>

    <div v-if="playerError" class="video-placeholder">
      <div class="video-overlay">
        <h3>{{ title || '实时视频监控' }}</h3>
        <p>当前通道：{{ cameraName || '未选择摄像头' }}</p>
        <p>播放地址：{{ url || '暂无播放地址' }}</p>
        <el-alert
          show-icon
          :closable="false"
          type="error"
          :title="playerError"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  url: { type: String, default: '' },
  title: { type: String, default: '' },
  cameraName: { type: String, default: '' },
  muted: { type: Boolean, default: true }
})

const playerRoot = ref(null)
const player = ref(null)
const playerError = ref('')

function destroyPlayer() {
  try {
    if (player.value?.destroy) player.value.destroy()
    if (player.value?.close) player.value.close()
  } catch (_) {}

  player.value = null

  if (playerRoot.value) {
    playerRoot.value.innerHTML = ''
  }
}

async function createPlayer() {
  destroyPlayer()
  playerError.value = ''

  if (!props.url) {
    playerError.value = '未获取到视频播放地址'
    return
  }

  await nextTick()

  if (!window.EasyPlayerPro) {
    playerError.value = '未加载 EasyPlayer-pro.js，请检查 public/libs/easyplayer 和 index.html'
    return
  }

  if (!playerRoot.value) {
    playerError.value = '播放器容器未初始化'
    return
  }

  try {
    player.value = new window.EasyPlayerPro(playerRoot.value, {
      isLive: true,
      isFlv: true,
      MSE: true,
      WASM: true,
      decoderPath: '/libs/easyplayer/',
      bufferTime: 0.2,
      stretch: true,
      hasAudio: !props.muted,
      isMute: props.muted,
      debug: true,
      isLogo: false,
      operateBtns: {
        fullscreen: true,
        screenshot: false,
        stretch: true,
        play: true,
        audio: true,
        record: false
      }
    })

    if (player.value?.play) {
      player.value.play(props.url)
    } else {
      playerError.value = 'EasyPlayerPro 已加载，但没有找到 play 方法'
    }
  } catch (err) {
    console.error('EasyPlayer 初始化失败：', err)
    playerError.value = `EasyPlayer 初始化失败：${err?.message || err}`
  }
}

watch(() => props.url, createPlayer, { immediate: true })
watch(() => props.muted, createPlayer)

onBeforeUnmount(destroyPlayer)
</script>

<style scoped>
.video-wrap {
  position: relative;
  width: 100%;
  min-height: 420px;
  border-radius: 18px;
  overflow: hidden;
  background: #0b1220;
}

.easy-player-box {
  width: 100%;
  height: 420px;
  background: #111827;
}

.video-placeholder {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.9);
  color: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.video-overlay {
  max-width: 760px;
  text-align: center;
}

.video-overlay p {
  word-break: break-all;
  color: #cbd5e1;
}
</style>