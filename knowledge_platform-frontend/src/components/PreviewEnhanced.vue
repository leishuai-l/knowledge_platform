<template>
  <div class="preview-enhanced">
    <!-- 预览加载状态 -->
    <div v-if="loading" class="preview-loading">
      <el-skeleton :rows="5" animated />
      <div class="loading-text">
        <el-icon class="is-loading"><Loading /></el-icon>
        正在加载预览...
      </div>
    </div>

    <!-- 预览错误状态 -->
    <div v-else-if="error" class="preview-error">
      <el-result
        icon="error"
        title="预览失败"
        :sub-title="error"
      >
        <template #extra>
          <el-button type="primary" @click="retryPreview">
            <el-icon><Refresh /></el-icon>
            重试
          </el-button>
          <el-button @click="downloadFile" v-if="showDownload">
            <el-icon><Download /></el-icon>
            下载文件
          </el-button>
        </template>
      </el-result>
    </div>

    <!-- 预览成功状态 -->
    <div v-else class="preview-content">
      <!-- PDF预览 -->
      <div v-if="isPDF" class="pdf-preview-wrapper">
        <div class="pdf-toolbar">
          <div class="toolbar-left">
            <span class="file-info">PDF文档 • {{ formatFileSize(fileSize) }}</span>
          </div>
          <div class="toolbar-right">
            <el-button-group size="small">
              <el-button @click="zoomOut" :disabled="zoom <= 25">
                <el-icon><ZoomOut /></el-icon>
              </el-button>
              <el-button disabled>{{ zoom }}%</el-button>
              <el-button @click="zoomIn" :disabled="zoom >= 300">
                <el-icon><ZoomIn /></el-icon>
              </el-button>
            </el-button-group>
            <el-button size="small" @click="resetZoom">
              <el-icon><Refresh /></el-icon>
            </el-button>
            <el-button size="small" @click="toggleFullscreen">
              <el-icon><FullScreen /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="pdf-container">
          <iframe
            :src="previewUrl"
            class="pdf-iframe"
            :style="{ transform: `scale(${zoom / 100})` }"
            title="PDF预览"
            @load="handlePDFLoad"
            @error="handlePDFError"
          ></iframe>
        </div>
      </div>

      <!-- 图片预览 -->
      <div v-else-if="isImage" class="image-preview-wrapper">
        <div class="image-toolbar">
          <div class="toolbar-left">
            <span class="file-info">{{ fileExtension.toUpperCase() }}图片 • {{ formatFileSize(fileSize) }}</span>
          </div>
          <div class="toolbar-right">
            <el-button-group size="small">
              <el-button @click="zoomOut" :disabled="zoom <= 25">
                <el-icon><ZoomOut /></el-icon>
              </el-button>
              <el-button disabled>{{ zoom }}%</el-button>
              <el-button @click="zoomIn" :disabled="zoom >= 500">
                <el-icon><ZoomIn /></el-icon>
              </el-button>
            </el-button-group>
            <el-button size="small" @click="resetZoom">
              <el-icon><Refresh /></el-icon>
            </el-button>
            <el-button size="small" @click="rotateImage">
              <el-icon><RefreshRight /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="image-container" @wheel="handleWheel">
          <img
            :src="previewUrl"
            :alt="fileName"
            class="preview-image"
            :style="{
              transform: `scale(${zoom / 100}) rotate(${rotation}deg)`,
              cursor: isDragging ? 'grabbing' : 'grab'
            }"
            @load="handleImageLoad"
            @error="handleImageError"
            @mousedown="startDrag"
            @mousemove="drag"
            @mouseup="endDrag"
            @mouseleave="endDrag"
          />
        </div>
      </div>

      <!-- 文本文件预览 -->
      <div v-else-if="isTextFile" class="text-preview-wrapper">
        <div class="text-toolbar">
          <div class="toolbar-left">
            <span class="file-info">{{ fileExtension.toUpperCase() }}文本 • {{ formatFileSize(fileSize) }}</span>
          </div>
          <div class="toolbar-right">
            <el-select v-model="textTheme" size="small" style="width: 120px">
              <el-option label="浅色主题" value="light" />
              <el-option label="深色主题" value="dark" />
              <el-option label="护眼模式" value="green" />
            </el-select>
            <el-select v-model="fontSize" size="small" style="width: 80px; margin-left: 8px">
              <el-option label="12px" :value="12" />
              <el-option label="14px" :value="14" />
              <el-option label="16px" :value="16" />
              <el-option label="18px" :value="18" />
              <el-option label="20px" :value="20" />
            </el-select>
          </div>
        </div>
        <div class="text-container" :class="`theme-${textTheme}`">
          <pre
            class="text-content"
            :style="{ fontSize: fontSize + 'px' }"
            v-html="highlightedText"
          ></pre>
        </div>
      </div>

      <!-- Office文档预览 -->
      <div v-else-if="isOfficeFile" class="office-preview-wrapper">
        <div class="office-toolbar">
          <div class="toolbar-left">
            <span class="file-info">{{ fileExtension.toUpperCase() }}文档 • {{ formatFileSize(fileSize) }}</span>
          </div>
          <div class="toolbar-right">
            <el-button v-if="officePreviewUrl" size="small" @click="openInNewTab">
              <el-icon><Link /></el-icon>
              新窗口打开
            </el-button>
          </div>
        </div>
        <div class="office-container">
          <iframe
            v-if="officePreviewUrl"
            :src="officePreviewUrl"
            class="office-iframe"
            title="Office文档预览"
            @load="handleOfficeLoad"
            @error="handleOfficeError"
          ></iframe>
          <div v-else class="office-fallback">
            <OfficePreview
              :file-url="previewUrl"
              :file-name="fileName"
              :file-extension="fileExtension"
              :file-size="fileSize"
              :document-id="documentId || 0"
            />
          </div>
        </div>
      </div>

      <!-- 压缩包预览 -->
      <div v-else-if="isArchive" class="archive-preview-wrapper">
        <ArchivePreview
          :file-url="previewUrl"
          :file-name="fileName"
          :file-extension="fileExtension"
          :file-size="fileSize"
          :document-id="documentId || 0"
        />
      </div>

      <!-- 不支持的文件类型 -->
      <div v-else class="unsupported-preview">
        <el-result
          icon="warning"
          title="暂不支持预览"
          :sub-title="`${fileExtension.toUpperCase()} 格式的文件暂不支持在线预览`"
        >
          <template #extra>
            <el-button type="primary" @click="downloadFile" v-if="showDownload">
              <el-icon><Download /></el-icon>
              下载查看
            </el-button>
          </template>
        </el-result>
      </div>
    </div>

    <!-- 预览提示信息 -->
    <div class="preview-tips" v-if="showTips && !loading && !error">
      <el-alert
        :title="getTipTitle()"
        :type="getTipType()"
        :closable="false"
        show-icon
      >
        <template #default>
          <p>{{ getTipContent() }}</p>
          <div class="tip-actions" v-if="getTipActions().length">
            <el-button
              v-for="action in getTipActions()"
              :key="action.label"
              :type="action.type"
              size="small"
              @click="action.handler"
            >
              <el-icon>
                <component :is="action.icon" />
              </el-icon>
              {{ action.label }}
            </el-button>
          </div>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  Loading, Refresh, Download, ZoomOut, ZoomIn, FullScreen,
  RefreshRight, Link
} from '@element-plus/icons-vue'
import OfficePreview from './OfficePreview.vue'
import ArchivePreview from './ArchivePreview.vue'

interface Props {
  fileUrl: string
  fileName: string
  fileSize: number
  documentId?: number
  showDownload?: boolean
  showTips?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showDownload: true,
  showTips: true
})

const emit = defineEmits<{
  download: []
  retry: []
}>()

// 状态
const loading = ref(true)
const error = ref('')
const zoom = ref(100)
const rotation = ref(0)
const textTheme = ref('light')
const fontSize = ref(14)
const textContent = ref('')
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })

// 文件信息
const fileExtension = computed(() => {
  const ext = props.fileName.split('.').pop()
  return ext ? ext.toLowerCase() : ''
})

const previewUrl = computed(() => props.fileUrl)

// Office 文件的在线预览 URL（由后端转换的 PDF 或外部在线服务）
const officePreviewUrl = computed(() => {
  // Office 文件通过后端已转换为 PDF，通过 previewUrl 访问
  // 新窗口打开时直接使用预览 URL
  if (isOfficeFile.value && props.fileUrl) {
    return props.fileUrl
  }
  return ''
})

// 文件类型判断 - 与后端 FileTypeRegistryService 保持一致
const isPDF = computed(() => fileExtension.value === 'pdf')
const isImage = computed(() => {
  return ['jpg', 'jpeg', 'png', 'gif'].includes(fileExtension.value)
})
const isTextFile = computed(() => {
  // 与后端 ARCHIVE_PREVIEWABLE_TEXT_EXTENSIONS + FileTypeRegistryService TEXT 定义一致
  return ['txt', 'md', 'json', 'xml', 'csv', 'log', 'java', 'js', 'html', 'css', 'py', 'sql'].includes(fileExtension.value)
})
const isOfficeFile = computed(() => {
  return ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx'].includes(fileExtension.value)
})
const isArchive = computed(() => {
  // rar 不支持预览，tar/gz 不在 FileTypeRegistryService 定义中
  return ['zip', '7z'].includes(fileExtension.value)
})

// 文本高亮
const highlightedText = computed(() => {
  if (!textContent.value) return ''

  let content = escapeHtml(textContent.value)

  if (fileExtension.value === 'json') {
    // JSON 语法高亮 - 先 HTML 转义，再用安全方式注入高亮标记
    content = content
      .replace(/&quot;/g, '"') // 还原引号以便正则匹配
      .replace(/(&quot;|&lt;|&gt;)/g, (match) => ({ '&quot;': '"', '&lt;': '<', '&gt;': '>' }[match] || match))
    content = applyJsonSyntaxHighlight(content)
  }

  return content
})

// HTML 转义（防止 XSS）
function escapeHtml(text: string): string {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

// JSON 语法高亮（安全方式，避免正则替换破坏 HTML 结构）
function applyJsonSyntaxHighlight(text: string): string {
  // 先转义 HTML 特殊字符，再注入安全的 span 标记
  const escaped = escapeHtml(text)
  // 使用预编译的正则，分步应用高亮
  let result = escaped
  // 高亮字符串键（处理转义字符）
  result = result.replace(/"((?:[^"\\]|\\.)*)"\s*:/g, (_match, key) => {
    return `<span class="json-key">"${escapeHtml(key)}"</span>:`
  })
  // 高亮字符串值
  result = result.replace(/:\s*"((?:[^"\\]|\\.)*)"/g, (_match, value) => {
    return `: <span class="json-string">"${escapeHtml(value)}"</span>`
  })
  // 高亮数字
  result = result.replace(/:\s*(-?\d+\.?\d*([eE][+-]?\d+)?)/g, ': <span class="json-number">$1</span>')
  // 高亮布尔值和 null
  result = result.replace(/:\s*(true|false|null)/g, ': <span class="json-boolean">$1</span>')
  return result
}

// 方法
const loadPreview = async () => {
  try {
    loading.value = true
    error.value = ''

    if (isTextFile.value) {
      const response = await fetch(props.fileUrl)
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
      textContent.value = await response.text()
    }

    loading.value = false
  } catch (err: any) {
    error.value = err.message || '预览加载失败'
    loading.value = false
  }
}

const retryPreview = () => {
  loadPreview()
  emit('retry')
}

const downloadFile = () => {
  emit('download')
}

const zoomIn = () => {
  const maxZoom = isImage.value ? 500 : 300
  if (zoom.value < maxZoom) {
    zoom.value += 25
  }
}

const zoomOut = () => {
  if (zoom.value > 25) {
    zoom.value -= 25
  }
}

const resetZoom = () => {
  zoom.value = 100
  rotation.value = 0
}

const rotateImage = () => {
  rotation.value = (rotation.value + 90) % 360
}

const toggleFullscreen = () => {
  if (document.fullscreenElement === null) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

const openInNewTab = () => {
  window.open(officePreviewUrl.value, '_blank')
}

// 图片拖拽
const startDrag = (e: MouseEvent) => {
  if (!isImage.value) return
  isDragging.value = true
  dragStart.value = { x: e.clientX, y: e.clientY }
}

const drag = (e: MouseEvent) => {
  if (!isDragging.value || !isImage.value) return
  // 这里可以实现图片拖拽逻辑
}

const endDrag = () => {
  isDragging.value = false
}

const handleWheel = (e: WheelEvent) => {
  if (!isImage.value) return
  e.preventDefault()

  if (e.deltaY < 0) {
    zoomIn()
  } else {
    zoomOut()
  }
}

// 事件处理
const handlePDFLoad = () => {
  loading.value = false
}

const handlePDFError = () => {
  error.value = 'PDF文件加载失败'
  loading.value = false
}

const handleImageLoad = () => {
  loading.value = false
}

const handleImageError = () => {
  error.value = '图片加载失败'
  loading.value = false
}

const handleOfficeLoad = () => {
  loading.value = false
}

const handleOfficeError = () => {
  error.value = 'Office文档预览失败'
  loading.value = false
}

// 提示相关
const getTipTitle = () => {
  if (isPDF.value) return 'PDF预览提示'
  if (isImage.value) return '图片预览提示'
  if (isTextFile.value) return '文本预览提示'
  if (isOfficeFile.value) return 'Office文档预览提示'
  return '预览提示'
}

const getTipType = () => {
  if (isOfficeFile.value) return 'warning'
  return 'info'
}

const getTipContent = () => {
  if (isPDF.value) return '使用滚轮或工具栏按钮可以缩放PDF文档，F11键进入全屏模式'
  if (isImage.value) return '使用鼠标滚轮缩放图片，点击旋转按钮可以旋转图片'
  if (isTextFile.value) return '可以切换主题和调整字体大小来获得更好的阅读体验'
  if (isOfficeFile.value) return 'Office文档使用在线预览服务，可能需要一些时间加载'
  return ''
}

const getTipActions = () => {
  const actions: any[] = []

  if (isOfficeFile.value) {
    actions.push({
      label: '新窗口打开',
      type: 'primary',
      icon: Link,
      handler: openInNewTab
    })
  }

  if (props.showDownload) {
    actions.push({
      label: '下载文件',
      type: 'success',
      icon: Download,
      handler: downloadFile
    })
  }

  return actions
}

// 工具函数
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// 生命周期
onMounted(() => {
  loadPreview()
})

// 监听文件URL变化
watch(() => props.fileUrl, () => {
  if (props.fileUrl) {
    loadPreview()
  }
})
</script>

<style scoped>
.preview-enhanced {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 0;
  overflow: hidden;
}

.preview-loading {
  padding: 24px;
  text-align: center;
}

.loading-text {
  margin-top: 16px;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.preview-error,
.unsupported-preview {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* PDF预览样式 */
.pdf-preview-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.pdf-toolbar,
.image-toolbar,
.text-toolbar,
.office-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
  height: 44px;
}

.toolbar-left {
  display: flex;
  align-items: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.file-info {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.pdf-container,
.image-container,
.text-container,
.office-container {
  flex: 1;
  overflow: auto;
  position: relative;
  height: calc(100% - 44px);
}

.pdf-iframe,
.office-iframe {
  width: 100%;
  height: 100%;
  min-height: 100%;
  border: none;
  background: #fff;
}

/* 图片预览样式 */
.image-preview-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.image-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: #fafafa;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.3s ease;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 文本预览样式 */
.text-preview-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.text-content {
  padding: 16px;
  margin: 0;
  font-family: 'Courier New', 'Monaco', monospace;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  height: 100%;
  box-sizing: border-box;
}

.theme-light .text-content {
  background: #fff;
  color: #303133;
}

.theme-dark .text-content {
  background: #2d3748;
  color: #e2e8f0;
}

.theme-green .text-content {
  background: #f0f9f0;
  color: #2d4a2d;
}

/* JSON语法高亮 */
.text-content :deep(.json-key) {
  color: #e74c3c;
  font-weight: 600;
}

.text-content :deep(.json-string) {
  color: #27ae60;
}

.text-content :deep(.json-number) {
  color: #3498db;
}

.text-content :deep(.json-boolean) {
  color: #9b59b6;
}

/* Office预览样式 */
.office-preview-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.office-fallback {
  flex: 1;
  padding: 16px;
}

/* 预览提示样式 */
.preview-tips {
  padding: 12px;
  background: #fafcff;
  border-top: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.tip-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .pdf-toolbar,
  .image-toolbar,
  .text-toolbar,
  .office-toolbar {
    flex-direction: column;
    gap: 6px;
    padding: 6px 8px;
    height: auto;
  }

  .toolbar-left,
  .toolbar-right {
    width: 100%;
    justify-content: center;
  }

  .image-container {
    padding: 8px;
  }

  .text-content {
    padding: 12px;
    font-size: 12px;
  }

  .pdf-container,
  .image-container,
  .text-container,
  .office-container {
    height: calc(100% - 60px);
  }
}
</style>