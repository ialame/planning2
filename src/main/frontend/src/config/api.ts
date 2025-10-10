// API Configuration for Pokemon Card Planning
// Automatically detects environment and uses appropriate API URL

/**
 * Determine API base URL based on environment
 * - Production: Use backend LoadBalancer external IP
 * - Development (localhost): Use localhost:8080
 * - Can be overridden with VITE_API_BASE_URL environment variable
 */
function getApiBaseUrl(): string {
  // Allow manual override via environment variable
  if (import.meta.env.VITE_API_BASE_URL !== undefined && import.meta.env.VITE_API_BASE_URL !== '') {
    console.log('🔧 Using VITE_API_BASE_URL from environment:', import.meta.env.VITE_API_BASE_URL)
    return import.meta.env.VITE_API_BASE_URL
  }

  // Check if we're running on localhost (development)
  const hostname = window.location.hostname
  const isLocalhost = hostname === 'localhost' ||
    hostname === '127.0.0.1' ||
    hostname.startsWith('192.168.') ||
    hostname.startsWith('10.')

  console.log('🌐 Detected hostname:', hostname)
  console.log('🏠 Is localhost:', isLocalhost)

  // In development, point to local backend
  if (isLocalhost) {
    console.log('✅ Using development API URL: http://localhost:8080')
    return 'http://localhost:8080'
  }

  // In production on Kubernetes/DigitalOcean
  // Use the backend LoadBalancer service on same IP but port 8080
  // This works because both frontend (port 80) and backend (port 8080)
  // are exposed via LoadBalancers on the same cluster
  const backendUrl = `http://${hostname}:8080`
  console.log('✅ Using production API URL:', backendUrl)

  return backendUrl
}

export const API_BASE_URL: string = getApiBaseUrl()

export const API_ENDPOINTS = {
  EMPLOYEES: `${API_BASE_URL}/api/employees`,
  PLANNING: `${API_BASE_URL}/api/planning`,
  ORDERS: `${API_BASE_URL}/api/orders`
} as const

// Log the final configuration
console.log('🔗 API Configuration loaded')
console.log('   Base URL:', API_BASE_URL || '(relative URL)')
console.log('   Employees:', API_ENDPOINTS.EMPLOYEES)
console.log('   Orders:', API_ENDPOINTS.ORDERS)
console.log('   Planning:', API_ENDPOINTS.PLANNING)
