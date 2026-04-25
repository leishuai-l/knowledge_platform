import { ref, computed } from 'vue'

interface CacheItem<T> {
  data: T
  timestamp: number
  expiry: number
}

class DataCache {
  private cache = new Map<string, CacheItem<any>>()
  private readonly defaultTTL = 5 * 60 * 1000 // 5分钟

  set<T>(key: string, data: T, ttl = this.defaultTTL): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      expiry: Date.now() + ttl
    })
  }

  get<T>(key: string): T | null {
    const item = this.cache.get(key)
    if (!item) return null

    if (Date.now() > item.expiry) {
      this.cache.delete(key)
      return null
    }

    return item.data
  }

  has(key: string): boolean {
    const item = this.cache.get(key)
    if (!item) return false

    if (Date.now() > item.expiry) {
      this.cache.delete(key)
      return false
    }

    return true
  }

  delete(key: string): void {
    this.cache.delete(key)
  }

  clear(): void {
    this.cache.clear()
  }

  size(): number {
    // 清理过期项
    const now = Date.now()
    for (const [key, item] of this.cache) {
      if (now > item.expiry) {
        this.cache.delete(key)
      }
    }
    return this.cache.size
  }

  getKeys(): string[] {
    return Array.from(this.cache.keys())
  }

  getCacheIterator() {
    return this.cache.keys()
  }
}

const globalCache = new DataCache()

export function useCache() {
  const loading = ref(false)
  const error = ref<string | null>(null)

  const fetchWithCache = async <T>(
    key: string,
    fetcher: () => Promise<T>,
    ttl?: number
  ): Promise<T> => {
    // 先检查缓存
    if (globalCache.has(key)) {
      return globalCache.get<T>(key)!
    }

    // 缓存中没有，发起请求
    loading.value = true
    error.value = null

    try {
      const data = await fetcher()
      globalCache.set(key, data, ttl)
      return data
    } catch (err) {
      error.value = err instanceof Error ? err.message : '请求失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  const invalidateCache = (pattern?: string) => {
    if (pattern) {
      // 如果提供了模式，删除匹配的键
      const regex = new RegExp(pattern)
      for (const key of globalCache.getCacheIterator()) {
        if (regex.test(key)) {
          globalCache.delete(key)
        }
      }
    } else {
      // 清空所有缓存
      globalCache.clear()
    }
  }

  const getCacheInfo = computed(() => ({
    size: globalCache.size(),
    keys: globalCache.getKeys()
  }))

  return {
    loading: computed(() => loading.value),
    error: computed(() => error.value),
    fetchWithCache,
    invalidateCache,
    cacheInfo: getCacheInfo
  }
}

export { globalCache }