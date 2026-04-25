import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// 全局样式
import './assets/global.css'

import App from './App.vue'
import router from './router'
import { installIcons } from './utils/icons'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)

app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})

// 安装图标
installIcons(app)

// 性能监控
if (typeof window !== 'undefined') {
  window.addEventListener('load', () => {
    console.log('应用加载完成，耗时:', performance.now(), 'ms')
  })
}

app.mount('#app')