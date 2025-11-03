// composables/useAuth.ts
// Reactive wrapper for authService

import { ref, computed } from 'vue'
import authService, { type User, type LoginResult } from '@/services/authService'

// Reactive state
const user = ref<User | null>(authService.getUser())
const token = ref<string | null>(authService.getToken())

// Computed
const isAuthenticated = computed(() => !!token.value)

export function useAuth() {
  /**
   * Login
   */
  async function login(email: string, password: string): Promise<LoginResult> {
    const result = await authService.login(email, password)

    if (result.success) {
      // ✅ Update reactive state
      user.value = authService.getUser()
      token.value = authService.getToken()
    }

    return result
  }

  /**
   * Logout
   */
  function logout(): void {
    authService.logout()

    // ✅ Update reactive state
    user.value = null
    token.value = null
  }

  /**
   * Restore from storage
   */
  function restore(): void {
    user.value = authService.getUser()
    token.value = authService.getToken()
  }

  return {
    // State
    user,
    token,
    isAuthenticated,

    // Methods
    login,
    logout,
    restore,

    // Direct access to authService for API calls
    authService
  }
}

// Export default instance for convenience
export default useAuth
