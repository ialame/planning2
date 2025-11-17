// services/authService.ts
// Authentication service for JWT token management with TypeScript

const API_BASE_URL = 'http://localhost:8080'

interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  roles: string[]
}

interface LoginResponse {
  accessToken?: string
  token?: string  // Some backends use 'token' instead of 'accessToken'
  refreshToken?: string
  user?: User
  // Flat structure (user fields at root level)
  email?: string
  firstName?: string
  lastName?: string
  roles?: string[]
  id?: string
}

interface LoginResult {
  success: boolean
  user?: User
  error?: string
}

interface ApiRequestOptions extends RequestInit {
  headers?: Record<string, string>
}

// JWT payload interface
interface JWTPayload {
  sub?: string      // subject (usually email or user ID)
  email?: string
  userId?: string
  id?: string
  firstName?: string
  lastName?: string
  roles?: string[]
  authorities?: string[]
  exp?: number
  iat?: number
}

class AuthService {
  private token: string | null = null
  private user: User | null = null

  constructor() {
    this.restoreFromStorage()
  }

  /**
   * Decode JWT token (without verification - only for reading payload)
   */
  private decodeJWT(token: string): JWTPayload | null {
    try {
      const parts = token.split('.')
      if (parts.length !== 3) {
        throw new Error('Invalid JWT format')
      }

      const payload = parts[1]
      const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
      return JSON.parse(decoded) as JWTPayload
    } catch (error) {
      console.error('❌ Failed to decode JWT:', error)
      return null
    }
  }

  /**
   * Extract user from JWT payload
   */
  private extractUserFromToken(token: string): User | null {
    const payload = this.decodeJWT(token)
    if (!payload) return null

    console.log('🔍 JWT Payload:', payload)

    // Try to construct User from JWT payload
    const email = payload.email || payload.sub || ''
    const userId = payload.userId || payload.id || payload.sub || ''
    const roles = payload.roles || payload.authorities || []

    // Extract names (may not be in JWT)
    const firstName = payload.firstName || email.split('@')[0] || 'User'
    const lastName = payload.lastName || ''

    if (!email || !userId) {
      console.warn('⚠️ Cannot extract user info from JWT')
      return null
    }

    return {
      id: userId,
      email: email,
      firstName: firstName,
      lastName: lastName,
      roles: roles
    }
  }

  /**
   * Login and store JWT token
   */
  async login(email: string, password: string): Promise<LoginResult> {
    try {
      console.log('🔐 Attempting login for:', email)

      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, password })
      })

      if (!response.ok) {
        const errorText = await response.text()
        console.error('❌ Login failed:', response.status, errorText)
        throw new Error(`Login failed: ${response.status}`)
      }

      const data: LoginResponse = await response.json()
      console.log('📥 Login response:', data)

      // Extract token (support both 'accessToken' and 'token' fields)
      const token = data.accessToken || data.token

      if (!token) {
        console.error('❌ No token in response')
        throw new Error('No token received from server')
      }

      this.token = token

      // Try to get user from response (nested or flat structure)
      if (data.user) {
        // Nested structure: { accessToken, user: {...} }
        console.log('✅ User from nested API response:', data.user)
        this.user = data.user
      } else if (data.email) {
        // Flat structure: { accessToken, email, firstName, lastName, roles }
        console.log('✅ User from flat API response')
        this.user = {
          id: data.id || data.email, // Use email as ID if no id provided
          email: data.email,
          firstName: data.firstName || '',
          lastName: data.lastName || '',
          roles: data.roles || []
        }
      } else {
        // Fallback: extract from JWT
        console.log('⚠️ No user in response, extracting from JWT...')
        this.user = this.extractUserFromToken(token)
      }

      if (!this.user) {
        console.error('❌ Could not determine user info')
        throw new Error('Could not determine user information')
      }

      // Store in localStorage
      localStorage.setItem('jwt_token', token)
      localStorage.setItem('user', JSON.stringify(this.user))

      console.log('✅ Logged in as:', this.user.email)
      console.log('👤 User info:', this.user)

      return { success: true, user: this.user }

    } catch (error) {
      console.error('❌ Login error:', error)
      return {
        success: false,
        error: error instanceof Error ? error.message : 'Login failed'
      }
    }
  }

  /**
   * Logout and clear token
   */
  logout(): void {
    this.token = null
    this.user = null
    localStorage.removeItem('jwt_token')
    localStorage.removeItem('user')
    console.log('👋 Logged out')
  }

  /**
   * Restore token from localStorage
   */
  private restoreFromStorage(): void {
    try {
      this.token = localStorage.getItem('jwt_token')
      const userStr = localStorage.getItem('user')

      if (userStr) {
        try {
          this.user = JSON.parse(userStr) as User
          console.log('✅ User restored from storage:', this.user?.email)
        } catch (e) {
          console.error('❌ Failed to parse user data:', e)
          console.warn('🧹 Cleaning corrupted data...')
          // Clean corrupted data
          localStorage.removeItem('user')
          localStorage.removeItem('jwt_token')
          this.token = null
          this.user = null
        }
      } else if (this.token) {
        // Try to extract user from token
        console.log('⚠️ No user in storage, extracting from token...')
        this.user = this.extractUserFromToken(this.token)
        if (this.user) {
          localStorage.setItem('user', JSON.stringify(this.user))
        }
      }
    } catch (error) {
      console.error('❌ Error restoring from storage:', error)
      this.token = null
      this.user = null
    }
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return !!this.token
  }

  /**
   * Get current user
   */
  getUser(): User | null {
    return this.user
  }

  /**
   * Get JWT token
   */
  getToken(): string | null {
    return this.token
  }

  /**
   * Make authenticated API call
   */
  async apiCall(endpoint: string, options: ApiRequestOptions = {}): Promise<Response> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options.headers
    }

    // Add JWT token if available
    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`
    }

    const url = `${API_BASE_URL}${endpoint}`
    console.log(`🌐 API Call: ${options.method || 'GET'} ${url}`)

    const response = await fetch(url, {
      ...options,
      headers
    })

    // Handle 401 Unauthorized - token expired
    if (response.status === 401) {
      console.warn('⚠️ Token expired (401), logging out')
      this.logout()
      throw new Error('Authentication required')
    }

    // Handle 403 Forbidden
    if (response.status === 403) {
      console.error('❌ Access forbidden (403)')
      throw new Error('Access forbidden')
    }

    // Handle other errors
    if (!response.ok) {
      const errorText = await response.text()
      console.error(`❌ API Error ${response.status}:`, errorText)
      throw new Error(`HTTP ${response.status}: ${errorText}`)
    }

    return response
  }

  /**
   * GET request
   */
  async get<T = any>(endpoint: string): Promise<T> {
    const response = await this.apiCall(endpoint, { method: 'GET' })
    return response.json()
  }

  /**
   * POST request
   */
  // Dans authService.ts
  async post(url: string, data?: any) {
    const token = this.getToken()

    console.log('🌐 POST Request:', url)
    console.log('🔑 Token present:', !!token)
    console.log('📦 Data:', data)

    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: data ? JSON.stringify(data) : undefined
    })

    console.log('📊 Response status:', response.status)

    if (!response.ok) {
      const errorText = await response.text()
      console.error('❌ Error response:', errorText)
      throw new Error(`HTTP ${response.status}: ${errorText}`)
    }

    return await response.json()
  }
  /**
   * PUT request
   */
  async put<T = any>(endpoint: string, data?: any): Promise<T> {
    const response = await this.apiCall(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined
    })
    return response.json()
  }

  /**
   * DELETE request
   */
  async delete<T = any>(endpoint: string): Promise<T> {
    const response = await this.apiCall(endpoint, { method: 'DELETE' })
    return response.json()
  }
}

// Export singleton instance
export const authService = new AuthService()

// Export default for convenience
export default authService

// Export types for use in other files
export type { User, LoginResponse, LoginResult }
