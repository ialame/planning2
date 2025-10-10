import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// ✅ IMPORTANT: Importation du CSS Tailwind
import './style.css'

console.log('🚀 Starting Pokemon Card Planning App...')

const app = createApp(App)

app.use(router)

app.mount('#app')

console.log('✅ Vue.js application mounted successfully')
console.log('✅ Tailwind CSS imported and active')
