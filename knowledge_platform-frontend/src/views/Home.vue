<template>
  <Layout>
    <div class="home-page">
      <!-- Hero Section -->
      <section class="hero-section">
        <div class="hero-background">
          <div class="floating-shapes">
            <div class="shape shape-1"></div>
            <div class="shape shape-2"></div>
            <div class="shape shape-3"></div>
            <div class="shape shape-4"></div>
          </div>
        </div>

        <div class="hero-content">
          <div class="hero-text">
            <h1 class="hero-title">
              <span class="gradient-text">知享校园</span>
              <br>
              知识分享平台
            </h1>
            <p class="hero-subtitle">
              探索知识边界，共建学习生态。汇聚校园智慧，让每一份资料都发挥最大价值。
            </p>

            <div class="hero-actions">
              <template v-if="!userStore.isLoggedIn">
                <el-button
                  type="primary"
                  size="large"
                  class="primary-btn"
                  @click="$router.push('/register')"
                >
                  <el-icon><UserFilled /></el-icon>
                  开始探索
                </el-button>
                <el-button
                  size="large"
                  class="secondary-btn"
                  @click="$router.push('/login')"
                >
                  <el-icon><Key /></el-icon>
                  登录账户
                </el-button>
              </template>
              <template v-else>
                <el-button
                  type="primary"
                  size="large"
                  class="primary-btn"
                  @click="$router.push('/documents')"
                >
                  <el-icon><Compass /></el-icon>
                  浏览文档
                </el-button>
                <el-button
                  size="large"
                  class="secondary-btn"
                  @click="$router.push('/upload')"
                >
                  <el-icon><Upload /></el-icon>
                  分享资料
                </el-button>
              </template>
            </div>
          </div>

          <div class="hero-visual">
            <div class="visual-container">
              <div class="visual-main">
                <el-icon :size="120" color="#00D4FF">
                  <DataBoard />
                </el-icon>
              </div>
              <div class="visual-orbit">
                <div class="orbit-item orbit-1">
                  <el-icon :size="32" color="#FF6B6B">
                    <Document />
                  </el-icon>
                </div>
                <div class="orbit-item orbit-2">
                  <el-icon :size="32" color="#4ECDC4">
                    <Star />
                  </el-icon>
                </div>
                <div class="orbit-item orbit-3">
                  <el-icon :size="32" color="#45B7D1">
                    <Trophy />
                  </el-icon>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Stats Section -->
      <section class="stats-section">
        <div class="content-container">
          <div class="stats-grid">
            <div class="stat-card" v-for="(stat, index) in platformStats" :key="index">
              <div class="stat-icon">
                <el-icon :size="48" :color="stat.color">
                  <component :is="stat.icon" />
                </el-icon>
              </div>
              <div class="stat-content">
                <div class="stat-number">{{ stat.value }}</div>
                <div class="stat-label">{{ stat.label }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Features & Categories Combined Section -->
      <section class="features-categories-section">
        <div class="content-container">
          <div class="features-categories-grid">
            <!-- Platform Features -->
            <div class="features-half">
              <div class="section-header">
                <h2 class="section-title">平台优势</h2>
                <p class="section-subtitle">为学习者和分享者打造的专业知识平台</p>
              </div>

              <div class="features-grid">
                <div
                  class="feature-card"
                  v-for="(feature, index) in platformFeatures"
                  :key="index"
                  :style="{ animationDelay: `${index * 0.1}s` }"
                >
                  <div class="feature-icon">
                    <el-icon :size="48" :color="feature.color">
                      <component :is="feature.icon" />
                    </el-icon>
                  </div>
                  <div class="feature-content">
                    <h3 class="feature-title">{{ feature.title }}</h3>
                    <p class="feature-description">{{ feature.description }}</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- Categories -->
            <div class="categories-half">
              <div class="section-header">
                <h2 class="section-title">学科分类</h2>
                <p class="section-subtitle">丰富的学科资源，满足不同学习需求</p>
              </div>

              <div class="categories-grid">
                <div
                  class="category-card"
                  v-for="category in categories"
                  :key="category.id"
                  @click="navigateToDocuments(category.id)"
                >
                  <div class="category-icon">
                    <el-icon :size="40" :color="category.color">
                      <component :is="category.icon" />
                    </el-icon>
                  </div>
                  <div class="category-info">
                    <h4 class="category-name">{{ category.name }}</h4>
                    <span class="category-count">{{ category.count }}+ 资源</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Recent Documents Section -->
      <section class="recent-section" v-if="userStore.isLoggedIn">
        <div class="content-container">
          <div class="section-header">
            <h2 class="section-title">最新资源</h2>
            <el-button
              text
              type="primary"
              class="view-more-btn"
              @click="$router.push('/documents')"
            >
              查看全部
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>

          <div class="documents-grid">
            <div
              class="document-card"
              v-for="doc in recentDocuments"
              :key="doc.id"
              @click="navigateToDocument(doc.id)"
            >
              <div class="document-header">
                <div class="document-icon">
                  <el-icon :size="32" color="#00D4FF">
                    <Document />
                  </el-icon>
                </div>
                <div class="document-meta">
                  <span class="document-size">{{ formatFileSize(doc.fileSize) }}</span>
                  <span class="document-points">{{ doc.downloadPoints }} 积分</span>
                </div>
              </div>
              <div class="document-content">
                <h4 class="document-title">{{ doc.title }}</h4>
                <div class="document-rating">
                  <el-rate v-model="doc.ratingAverage" disabled size="small" />
                  <span class="rating-text">{{ doc.ratingAverage }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </Layout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Document,
  Star,
  ArrowRight,
  UserFilled,
  Key,
  Compass,
  Upload,
  DataBoard,
  Trophy,
  TrendCharts,
  ChatDotRound,
  Lightning,
  Connection,
  Monitor,
  EditPen,
  MagicStick,
  School,
  Reading,
  Tools
} from '@element-plus/icons-vue'
import Layout from '@/components/Layout.vue'
import { useUserStore } from '@/stores/user'
import { documentApi, type PlatformStats } from '@/api/document'
import { categoryApi } from '@/api/category'
import type { Document as DocumentType, Category } from '@/types'

const router = useRouter()
const userStore = useUserStore()

// 平台统计数据
const platformStats = ref([
  {
    icon: Document,
    value: '0',
    label: '知识文档',
    color: '#00D4FF'
  },
  {
    icon: UserFilled,
    value: '0',
    label: '活跃用户',
    color: '#FF6B6B'
  },
  {
    icon: TrendCharts,
    value: '0',
    label: '下载次数',
    color: '#4ECDC4'
  },
  {
    icon: Star,
    value: '0.0',
    label: '平均评分',
    color: '#FFD93D'
  }
])

// 平台特色功能
const platformFeatures = ref([
  {
    icon: Lightning,
    title: '便捷搜索',
    description: '多维度搜索功能，快速找到所需学习资料',
    color: '#00D4FF'
  },
  {
    icon: Connection,
    title: '分类管理',
    description: '科学的分类体系，让知识整理更有序',
    color: '#FF6B6B'
  },
  {
    icon: ChatDotRound,
    title: '互动交流',
    description: '评论与评分系统，构建学习交流社区',
    color: '#4ECDC4'
  },
  {
    icon: Trophy,
    title: '积分体系',
    description: '完善的积分奖励机制，激发分享热情',
    color: '#FFD93D'
  }
])

// 学科分类 - 从后端获取真实数据
const categories = ref<Array<Category & {
  icon: any
  color: string
  count: number
}>>([])

// 分类图标和颜色映射
const categoryIconMap = {
  '计算机科学': { icon: Monitor, color: '#00D4FF' },
  '数学': { icon: Tools, color: '#FF6B6B' },
  '英语': { icon: EditPen, color: '#4ECDC4' },
  '物理': { icon: MagicStick, color: '#FFD93D' },
  '化学': { icon: School, color: '#A78BFA' },
  '其他专业课程': { icon: Reading, color: '#FB7185' }
}

// 最新文档数据
const recentDocuments = ref<DocumentType[]>([])

// 加载分类数据
const loadCategories = async () => {
  try {
    const categoriesWithStats = await categoryApi.getCategoriesWithStats()

    categories.value = categoriesWithStats.map(category => {
      const iconData = categoryIconMap[category.name as keyof typeof categoryIconMap]
        || { icon: Reading, color: '#888888' }

      return {
        ...category,
        icon: iconData.icon,
        color: iconData.color,
        count: category.documentCount || 0
      }
    })
  } catch (error) {
    console.error('Failed to load categories:', error)
    // 显示错误状态，不使用模拟数据
    categories.value = []
  }
}

// 加载真实的统计数据
const loadPlatformStats = async () => {
  try {
    const response = await documentApi.getPlatformStats()
    const stats = response.data.data

    // 更新统计数据显示
    platformStats.value = [
      {
        icon: Document,
        value: stats.documentCount.toLocaleString(),
        label: '知识文档',
        color: '#00D4FF'
      },
      {
        icon: UserFilled,
        value: stats.userCount.toLocaleString(),
        label: '活跃用户',
        color: '#FF6B6B'
      },
      {
        icon: TrendCharts,
        value: stats.downloadCount.toLocaleString(),
        label: '下载次数',
        color: '#4ECDC4'
      },
      {
        icon: Star,
        value: stats.averageRating.toFixed(1),
        label: '平均评分',
        color: '#FFD93D'
      }
    ]
  } catch (error) {
    console.error('Failed to load platform stats:', error)
    // 显示错误状态，不使用模拟数据
    platformStats.value.forEach(stat => {
      stat.value = 'Error'
    })
  }
}

// 加载最新文档
const loadRecentDocuments = async () => {
  try {
    const response = await documentApi.getRecentDocuments(4)
    recentDocuments.value = response.data.data.list
  } catch (error) {
    console.error('Failed to load recent documents:', error)
    // 显示错误状态，不使用模拟数据
    recentDocuments.value = []
  }
}

const navigateToDocuments = (categoryId?: number) => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }

  const query = categoryId ? { category: categoryId.toString() } : {}
  router.push({ name: 'Documents', query })
}

const navigateToDocument = (documentId: number) => {
  router.push(`/documents/${documentId}`)
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

onMounted(async () => {
  await Promise.all([
    loadPlatformStats(),
    loadRecentDocuments(),
    loadCategories()
  ])
})
</script>

<style scoped>
/* 全局样式 */
.home-page {
  background: linear-gradient(135deg, #0a0e27 0%, #1a1a2e 50%, #16213e 100%);
  min-height: 100vh;
  overflow-x: hidden;
}

/* Hero Section */
.hero-section {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  color: #fff;
  overflow: hidden;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1;
}

.floating-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(45deg, rgba(0, 212, 255, 0.1), rgba(255, 107, 107, 0.1));
  animation: float 20s ease-in-out infinite;
}

.shape-1 {
  width: 300px;
  height: 300px;
  top: 10%;
  left: -10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 200px;
  height: 200px;
  top: 60%;
  right: -5%;
  animation-delay: 5s;
}

.shape-3 {
  width: 150px;
  height: 150px;
  top: 30%;
  right: 20%;
  animation-delay: 10s;
}

.shape-4 {
  width: 100px;
  height: 100px;
  bottom: 20%;
  left: 10%;
  animation-delay: 15s;
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 40px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 80px;
  align-items: center;
}

.hero-text {
  max-width: 600px;
}

.hero-title {
  font-size: 4rem;
  font-weight: 800;
  line-height: 1.1;
  margin: 0 0 30px;
  letter-spacing: -0.02em;
}

.gradient-text {
  background: linear-gradient(135deg, #00D4FF 0%, #FF6B6B 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.25rem;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.8);
  margin: 0 0 40px;
}

.hero-actions {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.primary-btn {
  background: linear-gradient(135deg, #00D4FF 0%, #0099CC 100%);
  border: none;
  padding: 16px 32px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  transition: all 0.3s ease;
  box-shadow: 0 8px 25px rgba(0, 212, 255, 0.3);
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(0, 212, 255, 0.4);
}

.secondary-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 14px 30px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
}

.secondary-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.4);
  transform: translateY(-2px);
}

/* Hero Visual */
.hero-visual {
  display: flex;
  justify-content: center;
  align-items: center;
}

.visual-container {
  position: relative;
  width: 300px;
  height: 300px;
}

.visual-main {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 3;
}

.visual-orbit {
  position: relative;
  width: 100%;
  height: 100%;
  animation: rotate 30s linear infinite;
}

.orbit-item {
  position: absolute;
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.orbit-1 {
  top: 20px;
  right: 50px;
}

.orbit-2 {
  bottom: 20px;
  left: 50px;
}

.orbit-3 {
  top: 50%;
  right: 0;
  transform: translateY(-50%);
}

/* Stats Section */
.stats-section {
  padding: 100px 0;
  background: rgba(255, 255, 255, 0.02);
  backdrop-filter: blur(10px);
}

.content-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 40px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 30px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 40px 30px;
  text-align: center;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.stat-card:hover {
  transform: translateY(-10px);
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

.stat-icon {
  margin-bottom: 20px;
}

.stat-content {
  color: #fff;
}

.stat-number {
  font-size: 3rem;
  font-weight: 800;
  margin-bottom: 10px;
  background: linear-gradient(135deg, #00D4FF, #FF6B6B);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* Features & Categories Combined Section */
.features-categories-section {
  padding: 120px 0;
  background: rgba(0, 0, 0, 0.2);
}

.features-categories-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 80px;
  align-items: start;
}

.features-half,
.categories-half {
  width: 100%;
}

.features-half .section-header,
.categories-half .section-header {
  text-align: center;
  margin-bottom: 60px;
}

.features-half .features-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 30px;
}

.features-half .feature-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 30px 25px;
  display: flex;
  align-items: center;
  gap: 20px;
  transition: all 0.4s ease;
  backdrop-filter: blur(10px);
  animation: fadeInUp 0.6s ease forwards;
  opacity: 0;
  transform: translateY(30px);
}

.features-half .feature-card:hover {
  transform: translateY(-8px);
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.features-half .feature-icon {
  flex-shrink: 0;
}

.features-half .feature-content {
  flex: 1;
}

.features-half .feature-title {
  font-size: 1.3rem;
  font-weight: 600;
  color: #fff;
  margin: 0 0 12px;
}

.features-half .feature-description {
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.5;
  margin: 0;
  font-size: 0.95rem;
}

.categories-half .categories-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.categories-half .category-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 25px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.categories-half .category-card:hover {
  transform: translateY(-6px);
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.3);
}

.categories-half .category-icon {
  margin-bottom: 15px;
}

.categories-half .category-name {
  font-size: 1rem;
  font-weight: 600;
  color: #fff;
  margin: 0 0 6px;
}

.categories-half .category-count {
  font-size: 0.85rem;
  color: rgba(255, 255, 255, 0.6);
}

/* 通用样式 */
.section-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 20px;
  background: linear-gradient(135deg, #fff, rgba(255, 255, 255, 0.8));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.section-subtitle {
  font-size: 1.1rem;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  max-width: 500px;
  margin: 0 auto;
}
.recent-section {
  padding: 120px 0;
  background: rgba(0, 0, 0, 0.2);
}

.view-more-btn {
  color: #00D4FF;
  font-weight: 600;
}

.documents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 30px;
}

.document-card {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.document-card:hover {
  transform: translateY(-8px);
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.2);
}

.document-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.document-meta {
  display: flex;
  gap: 15px;
  font-size: 0.9rem;
}

.document-size {
  color: rgba(255, 255, 255, 0.6);
}

.document-points {
  color: #FFD93D;
  font-weight: 600;
}

.document-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #fff;
  margin: 0 0 15px;
  line-height: 1.4;
}

.document-rating {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-text {
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.9rem;
}

/* Animations */
@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-30px) rotate(180deg);
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes fadeInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .features-categories-grid {
    gap: 60px;
  }

  .hero-content {
    grid-template-columns: 1fr;
    gap: 60px;
    text-align: center;
  }

  .hero-title {
    font-size: 3rem;
  }

  .content-container {
    padding: 0 30px;
  }

  .categories-half .categories-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 18px;
  }
}

@media (max-width: 1024px) {
  .features-categories-grid {
    grid-template-columns: 1fr;
    gap: 80px;
  }

  .features-half .features-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }

  .features-half .feature-card {
    flex-direction: column;
    text-align: center;
    padding: 25px 20px;
  }

  .features-half .feature-icon {
    margin-bottom: 15px;
  }

  .features-half .feature-title {
    font-size: 1.2rem;
  }

  .features-half .feature-description {
    font-size: 0.9rem;
  }

  .categories-half .categories-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }

  .section-title {
    font-size: 2.2rem;
  }

  .content-container {
    padding: 0 25px;
  }
}

@media (max-width: 768px) {
  .features-categories-section {
    padding: 80px 0;
  }

  .hero-title {
    font-size: 2.5rem;
  }

  .hero-subtitle {
    font-size: 1.1rem;
  }

  .hero-actions {
    justify-content: center;
  }

  .section-title {
    font-size: 2rem;
  }

  .section-subtitle {
    font-size: 1rem;
  }

  .features-half .features-grid {
    grid-template-columns: 1fr;
    gap: 25px;
  }

  .categories-half .categories-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 15px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }

  .documents-grid {
    grid-template-columns: 1fr;
  }

  .content-container {
    padding: 0 20px;
  }
}

@media (max-width: 640px) {
  .features-categories-section {
    padding: 60px 0;
  }

  .hero-content {
    padding: 0 20px;
  }

  .hero-title {
    font-size: 2rem;
  }

  .hero-subtitle {
    font-size: 1rem;
  }

  .hero-actions {
    flex-direction: column;
    width: 100%;
    gap: 15px;
  }

  .primary-btn,
  .secondary-btn {
    width: 100%;
    padding: 14px 24px;
  }

  .section-title {
    font-size: 1.8rem;
  }

  .section-header {
    margin-bottom: 40px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .categories-half .categories-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }

  .categories-half .category-card {
    padding: 20px 15px;
  }

  .features-half .feature-card {
    padding: 20px 15px;
  }
}

@media (max-width: 480px) {
  .features-categories-section {
    padding: 50px 0;
  }

  .hero-title {
    font-size: 1.8rem;
  }

  .section-title {
    font-size: 1.6rem;
  }

  .content-container {
    padding: 0 15px;
  }

  .visual-container {
    width: 250px;
    height: 250px;
  }

  .features-categories-grid {
    gap: 60px;
  }
}
</style>