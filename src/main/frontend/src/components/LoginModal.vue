<template>
  <div
    v-if="!isAuthenticated"
    class="fixed inset-0 bg-gray-900 bg-opacity-50 flex items-center justify-center"
    style="z-index: 9999;"
  >
    <div class="bg-white rounded-lg shadow-2xl p-8 max-w-md w-full mx-4">
      <div class="text-center mb-6">
        <h2 class="text-3xl font-bold text-gray-800">🎴 Pokemon Card Planning</h2>
        <p class="text-gray-600 mt-2">Please login to continue</p>
      </div>

      <form @submit.prevent="handleLogin">
        <div class="mb-4">
          <label class="block text-gray-700 text-sm font-bold mb-2">
            Email
          </label>
          <input
            v-model="email"
            type="email"
            required
            autocomplete="email"
            class="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:border-blue-500 text-gray-900 bg-white placeholder-gray-400"
            placeholder="john.grader@pcagrade.com"
          />
        </div>

        <div class="mb-6">
          <label class="block text-gray-700 text-sm font-bold mb-2">
            Password
          </label>
          <input
            v-model="password"
            type="password"
            required
            autocomplete="current-password"
            class="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:border-blue-500 text-gray-900 bg-white placeholder-gray-400"
            placeholder="Enter your password"
          />
        </div>

        <div
          v-if="error"
          class="mb-4 p-4 bg-red-50 border-2 border-red-400 text-red-700 rounded-lg flex items-start"
        >
          <span class="mr-2">❌</span>
          <span>{{ error }}</span>
        </div>

        <button
          type="submit"
          :disabled="loading"
          class="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 px-4 rounded-lg focus:outline-none focus:ring-4 focus:ring-blue-300 disabled:opacity-50 disabled:cursor-not-allowed transition-all transform active:scale-95"
        >
          <span v-if="loading" class="flex items-center justify-center">
            <svg class="animate-spin h-5 w-5 mr-2" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Logging in...
          </span>
          <span v-else>🔐 Login</span>
        </button>
      </form>

      <div class="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
        <p class="font-semibold text-blue-900 mb-3 flex items-center">
          <span class="mr-2">ℹ️</span>
          Test Accounts
        </p>
        <div class="space-y-3 text-sm">
          <div class="bg-white p-3 rounded border border-blue-100">
            <div class="flex items-center mb-1">
              <span class="text-gray-600 w-20">👤 Grader:</span>
              <code class="text-blue-600 font-mono text-xs">john.grader@pcagrade.com</code>
            </div>
            <div class="flex items-center">
              <span class="text-gray-600 w-20">🔑 Pass:</span>
              <code class="text-gray-800 font-mono text-xs">password123</code>
            </div>
          </div>

          <div class="bg-white p-3 rounded border border-orange-100">
            <div class="flex items-center mb-1">
              <span class="text-gray-600 w-20">👔 Manager:</span>
              <code class="text-orange-600 font-mono text-xs">manager@pcagrade.com</code>
            </div>
            <div class="flex items-center">
              <span class="text-gray-600 w-20">🔑 Pass:</span>
              <code class="text-gray-800 font-mono text-xs">password123</code>
            </div>
          </div>

          <div class="bg-white p-3 rounded border border-red-100">
            <div class="flex items-center mb-1">
              <span class="text-gray-600 w-20">🔐 Admin:</span>
              <code class="text-red-600 font-mono text-xs">admin@pcagrade.com</code>
            </div>
            <div class="flex items-center">
              <span class="text-gray-600 w-20">🔑 Pass:</span>
              <code class="text-gray-800 font-mono text-xs">password123</code>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuth } from '@/composables/useAuth'

const { isAuthenticated, login } = useAuth()



interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  roles: string[]
}

// Emits
const emit = defineEmits<{
  (e: 'login-success', user: User): void
}>()

// Reactive state
const email = ref<string>('john.grader@pcagrade.com')
const password = ref<string>('password123')
const loading = ref<boolean>(false)
const error = ref<string | null>(null)



// Methods
async function handleLogin() {
  loading.value = true
  error.value = null

  try {
    const result = await login(email.value, password.value)

    if (result.success) {
      emit('login-success', result.user)
    } else {
      error.value = result.error || 'Login failed'
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'An error occurred'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* Force text visibility */
input {
  color: #1a202c !important;
  background-color: white !important;
}

input::placeholder {
  color: #a0aec0 !important;
}

/* Animation for spinner */
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>
