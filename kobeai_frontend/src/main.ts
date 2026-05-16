import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import './styles/theme.scss'
import './styles/main.scss'

import App from './App.vue'
import router from './router'

import './style.css'
import './assets/main.css'

// 创建应用实例
const app = createApp(App)
const pinia = createPinia()

// 使用插件
app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  size: 'default'
})

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 初始化设置
import { useSettingsStore } from './stores/settings'
const settingsStore = useSettingsStore()
settingsStore.applySettings(settingsStore.settings)

// 挂载应用
app.mount('#app')
