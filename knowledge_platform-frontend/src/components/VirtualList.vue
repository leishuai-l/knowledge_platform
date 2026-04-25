<template>
  <div
    class="virtual-list"
    :style="{ height: containerHeight + 'px' }"
    @scroll="handleScroll"
    ref="containerRef"
  >
    <div
      class="virtual-list-phantom"
      :style="{ height: totalHeight + 'px' }"
    ></div>

    <div
      class="virtual-list-content"
      :style="{ transform: `translateY(${startOffset}px)` }"
    >
      <div
        v-for="item in visibleItems"
        :key="getItemKey(item)"
        :style="{ height: itemHeight + 'px' }"
        class="virtual-list-item"
      >
        <slot :item="item" :index="item.index"></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'

interface Props {
  items: any[]
  itemHeight: number
  containerHeight: number
  overscan?: number
  keyField?: string
}

const props = withDefaults(defineProps<Props>(), {
  overscan: 5,
  keyField: 'id'
})

const containerRef = ref<HTMLElement>()
const scrollTop = ref(0)

// 计算总高度
const totalHeight = computed(() => props.items.length * props.itemHeight)

// 计算开始索引
const startIndex = computed(() => {
  return Math.floor(scrollTop.value / props.itemHeight)
})

// 计算结束索引
const endIndex = computed(() => {
  const index = startIndex.value + Math.ceil(props.containerHeight / props.itemHeight)
  return Math.min(index, props.items.length - 1)
})

// 计算可见区域的偏移量
const startOffset = computed(() => {
  return startIndex.value * props.itemHeight
})

// 计算可见的数据
const visibleItems = computed(() => {
  const start = Math.max(0, startIndex.value - props.overscan)
  const end = Math.min(props.items.length, endIndex.value + props.overscan)

  return props.items.slice(start, end + 1).map((item, index) => ({
    ...item,
    index: start + index
  }))
})

const getItemKey = (item: any) => {
  return item[props.keyField] || item.index
}

const handleScroll = (event: Event) => {
  const target = event.target as HTMLElement
  scrollTop.value = target.scrollTop
}

// 滚动到指定索引
const scrollToIndex = (index: number) => {
  if (!containerRef.value) return

  const targetScrollTop = index * props.itemHeight
  containerRef.value.scrollTop = targetScrollTop
}

// 滚动到顶部
const scrollToTop = () => {
  scrollToIndex(0)
}

// 滚动到底部
const scrollToBottom = () => {
  scrollToIndex(props.items.length - 1)
}

// 暴露方法
defineExpose({
  scrollToIndex,
  scrollToTop,
  scrollToBottom
})

// 防抖优化滚动事件
let scrollTimer: NodeJS.Timeout | null = null
const optimizedHandleScroll = (event: Event) => {
  if (scrollTimer) {
    clearTimeout(scrollTimer)
  }

  scrollTimer = setTimeout(() => {
    handleScroll(event)
  }, 16) // 约60fps
}

onMounted(() => {
  if (containerRef.value) {
    containerRef.value.addEventListener('scroll', optimizedHandleScroll, { passive: true })
  }
})

onUnmounted(() => {
  if (containerRef.value) {
    containerRef.value.removeEventListener('scroll', optimizedHandleScroll)
  }
  if (scrollTimer) {
    clearTimeout(scrollTimer)
  }
})
</script>

<style scoped>
.virtual-list {
  position: relative;
  overflow-y: auto;
  overflow-x: hidden;
}

.virtual-list-phantom {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: -1;
}

.virtual-list-content {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
}

.virtual-list-item {
  box-sizing: border-box;
}

/* 滚动条样式优化 */
.virtual-list::-webkit-scrollbar {
  width: 6px;
}

.virtual-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.virtual-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
  transition: background 0.2s;
}

.virtual-list::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}
</style>