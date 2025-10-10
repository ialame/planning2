<template>
  <div class="employee-detail-page">
    <!-- Header with Back Button -->
    <div class="header-section">
      <button @click="goBack" class="back-button">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
        Back to Employees
      </button>
    </div>

    <!-- Employee Info Card -->
    <div class="employee-info-card">
      <div class="employee-header">
        <div class="employee-avatar">
          {{ getInitials(employeeData?.fullName || employeeData?.name) }}
        </div>
        <div class="employee-details">
          <h1 class="employee-name">{{ employeeData?.fullName || employeeData?.name || 'Loading...' }}</h1>
          <p class="employee-meta">
            <span class="meta-item">
              <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
              </svg>
              {{ employeeData?.email || 'N/A' }}
            </span>
            <span class="meta-item">
              <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              {{ employeeData?.workHoursPerDay || 8 }}h/day
            </span>
          </p>
        </div>
        <div class="employee-stats">
          <div class="stat-card">
            <div class="stat-value">{{ orders.length }}</div>
            <div class="stat-label">Orders</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ totalCards }}</div>
            <div class="stat-label">Cards</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ formatDuration(totalDuration) }}</div>
            <div class="stat-label">Duration</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Date Filter -->
    <div class="filter-section">
      <label class="filter-label">
        <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
        </svg>
        Filter by Date:
      </label>
      <input
        type="date"
        v-model="localSelectedDate"
        @change="loadEmployeeOrders"
        class="date-input"
      />
      <button @click="loadEmployeeOrders" class="refresh-btn">
        <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
        Refresh
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading orders...</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="orders.length === 0" class="empty-state">
      <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V9a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
      </svg>
      <h3>No Orders Assigned</h3>
      <p>No orders found for {{ selectedDate }}</p>
    </div>

      <!-- Orders Timeline -->
      <div v-else class="orders-timeline">
        <h2 class="timeline-title">
          <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V9a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/>
          </svg>
          Work Schedule
        </h2>

        <!-- FIXED: Correct v-for syntax for array of objects -->
        <div v-for="dayGroup in ordersByDay" :key="dayGroup.date" class="day-group">
          <div class="day-header">
            <div class="day-date">
              <!-- Use dayGroup.date instead of date -->
              <span class="day-name">{{ formatDayName(dayGroup.date) }}</span>
              <span class="day-full-date">{{ formatFullDate(dayGroup.date) }}</span>
            </div>
            <div class="day-summary">
              <!-- Use dayGroup.orders instead of dayOrders -->
              <span class="summary-badge">{{ dayGroup.orders.length }} orders</span>
              <span class="summary-badge">{{ calculateDayCards(dayGroup.orders) }} cards</span>
              <span class="summary-badge">{{ formatDuration(calculateDayDuration(dayGroup.orders)) }}</span>
            </div>
          </div>

          <!-- Orders for this day -->
          <div class="day-orders">
            <!-- Use dayGroup.orders instead of dayOrders -->
            <div
              v-for="order in dayGroup.orders"
              :key="order.planningId || order.id"
              :class="['order-card', `priority-${getDelaiClass(order.delai)}`]"
              @click="toggleOrderCards(order)"
            >
              <!-- Order Header -->
              <div class="order-header">
                <div class="order-time">
                  <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                  </svg>
                  <span class="time-range">
              {{ formatTime(order.startTime) }} - {{ formatTime(order.endTime) }}
            </span>
                  <span class="duration-badge">{{ formatDuration(order.duration || order.durationMinutes || 0) }}</span>
                </div>

                <div class="order-badges">
            <span :class="['priority-badge', `priority-${getDelaiClass(order.delai)}`]">
              {{ getDelaiLabel(order.delai) }}
            </span>
                  <span :class="['status-badge', `status-${order.status}`]">
              {{ getStatusLabel(order.status) }}
            </span>
                </div>
              </div>

              <!-- Order Content -->
              <div class="order-content">
                <div class="order-main">
                  <h3 class="order-number">
                    📦 Order {{ order.orderNumber || order.clientOrderNumber }}
                  </h3>
                  <div class="order-meta">
              <span class="meta-item">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343M11 7.343l1.657-1.657a2 2 0 012.828 0l2.829 2.829a2 2 0 010 2.828l-8.486 8.485M7 17h.01"/>
                </svg>
                {{ order.cardCount || 0 }} cards
              </span>
                    <span v-if="order.cardsWithName" class="meta-item">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                {{ order.cardsWithName }} named
              </span>
                    <span v-if="order.orderDate" class="meta-item">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                </svg>
                Created {{ formatDate(order.orderDate) }}
              </span>
                  </div>
                </div>

                <!-- Expand Cards Button -->
                <button
                  v-if="order.cardCount > 0"
                  class="expand-btn"
                  @click.stop="toggleOrderCards(order)"
                >
                  <svg v-if="!order.showCards" class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                  </svg>
                  <svg v-else class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 15l7-7 7 7"/>
                  </svg>
                  {{ order.showCards ? 'Hide' : 'Show' }} Cards
                </button>
              </div>

              <!-- Expanded Cards List -->
              <div v-if="order.showCards" class="cards-list">
                <div v-if="order.loadingCards" class="cards-loading">
                  <div class="spinner-sm"></div>
                  Loading cards...
                </div>
                <div v-else-if="order.cards && order.cards.length > 0" class="cards-grid">
                  <div v-for="card in order.cards" :key="card.id" class="card-item">
                    <div class="card-icon">🃏</div>
                    <div class="card-info">
                      <div class="card-name">{{ card.name || 'Unnamed Card' }}</div>
                      <div class="card-label">{{ card.label_name || card.code_barre }}</div>
                    </div>
                    <div class="card-duration">{{ card.duration || 3 }}min</div>
                  </div>
                </div>
                <div v-else class="no-cards">
                  No card details available
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://146.190.204.228:8080'

const props = defineProps<{
  employeeId: string
  selectedDate: string
  mode?: string
}>()

const emit = defineEmits<{
  back: []
  refresh: []
}>()

const employeeData = ref<any>(null)
const orders = ref<any[]>([])
const loading = ref(false)
const localSelectedDate = ref(props.selectedDate)

// Calculate total cards from orders
const totalCards = computed(() => {
  return orders.value.reduce((sum, order) => sum + (order.cardCount || 0), 0)
})

// Calculate total duration from orders
const totalDuration = computed(() => {
  return orders.value.reduce((sum, order) => {
    // Try different field names that the API might return
    const duration = order.duration ||
      order.durationMinutes ||
      order.estimatedDurationMinutes ||
      order.estimatedDuration ||
      0
    return sum + duration
  }, 0)
})

// Group orders by day with FIXED field mapping
const ordersByDay = computed(() => {
  const grouped: Record<string, any[]> = {}

  orders.value.forEach(order => {
    // Get the planning date
    const date = order.planningDate || order.date
    if (date) {
      // Extract just the date part (YYYY-MM-DD) from ISO timestamp
      const dateKey = typeof date === 'string' ?
        date.split('T')[0] : date.toString()

      if (!grouped[dateKey]) {
        grouped[dateKey] = []
      }
      grouped[dateKey].push(order)
    }
  })

  // Sort by date and create day objects
  return Object.keys(grouped)
    .sort()
    .map(date => ({
      date,
      orders: grouped[date],
      // FIXED: Use correct field names from API response
      totalCards: grouped[date].reduce((sum, o) => sum + (o.cardCount || 0), 0),
      totalDuration: grouped[date].reduce((sum, o) => {
        const duration = o.duration ||
          o.durationMinutes ||
          o.estimatedDurationMinutes ||
          o.estimatedDuration ||
          0
        return sum + duration
      }, 0)
    }))
})
// Methods
const goBack = () => {
  console.log('Going back to employees list')
  emit('back')
}

const loadEmployeeData = async () => {
  if (!props.employeeId) return
  try {
    const response = await fetch(`${API_BASE_URL}/api/employees/${props.employeeId}`)
    if (response.ok) {
      employeeData.value = await response.json()
    }
  } catch (error) {
    console.error('Error loading employee data:', error)
  }
}

const loadEmployeeOrders = async () => {
  if (!props.employeeId) return

  loading.value = true
  try {
    const url = `${API_BASE_URL}/api/planning/employee/${props.employeeId}?date=${localSelectedDate.value}`
    const response = await fetch(url)

    if (response.ok) {
      const data = await response.json()
      if (data && data.orders && Array.isArray(data.orders)) {
        orders.value = data.orders
      } else if (data && Array.isArray(data.plannings)) {
        orders.value = data.plannings
      } else if (Array.isArray(data)) {
        orders.value = data
      } else {
        orders.value = []
      }
    } else {
      orders.value = []
    }
  } catch (error) {
    console.error('Error loading orders:', error)
    orders.value = []
  } finally {
    loading.value = false  // Important: toujours exécuté
  }
}

const handleDateChange = () => {
  loadEmployeeOrders()  // Rechargement manuel uniquement
}

const getInitials = (name: string | undefined) => {
  if (!name) return '??'
  const parts = name.split(' ')
  if (parts.length >= 2) {
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
  }
  return name.substring(0, 2).toUpperCase()
}

// ========== FIXED DATE/TIME FORMATTING METHODS ==========
// Replace the existing formatting methods in EmployeeDetailPage.vue

// Format duration in minutes to hours/minutes
const formatDuration = (minutes: number) => {
  if (!minutes) return '0h'
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? `${hours}h${mins}m` : `${hours}h`
}

// Format date (e.g., "Jun 5, 2025")
const formatDate = (date: any) => {
  if (!date) return 'N/A'
  try {
    // Handle both ISO strings and Date objects
    const dateObj = typeof date === 'string' ? new Date(date) : date
    return dateObj.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    })
  } catch {
    return 'N/A'
  }
}

// Format time from ISO timestamp or time string
const formatTime = (time: any) => {
  if (!time) return 'N/A'
  try {
    // If it's already in HH:MM format, return as is
    if (typeof time === 'string' && time.match(/^\d{2}:\d{2}$/)) {
      return time
    }

    // Parse ISO timestamp (e.g., "2025-06-01T07:27:00.000+00:00")
    const date = new Date(time)

    // Check if date is valid
    if (isNaN(date.getTime())) {
      return 'N/A'
    }

    return date.toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    })
  } catch {
    return 'N/A'
  }
}

// Format day name (e.g., "Monday")
const formatDayName = (date: string) => {
  if (!date) return 'N/A'
  try {
    // Extract date part if ISO timestamp
    const dateStr = date.split('T')[0]
    const dateObj = new Date(dateStr + 'T00:00:00')

    if (isNaN(dateObj.getTime())) {
      return 'N/A'
    }

    return dateObj.toLocaleDateString('en-US', { weekday: 'long' })
  } catch {
    return 'N/A'
  }
}

// Format full date (e.g., "June 1, 2025")
const formatFullDate = (date: string) => {
  if (!date) return 'N/A'
  try {
    // Extract date part if ISO timestamp
    const dateStr = date.split('T')[0]
    const dateObj = new Date(dateStr + 'T00:00:00')

    if (isNaN(dateObj.getTime())) {
      return 'N/A'
    }

    return dateObj.toLocaleDateString('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric'
    })
  } catch {
    return 'N/A'
  }
}

// Calculate total cards for a day
const calculateDayCards = (dayOrders: any[]) => {
  if (!dayOrders || !Array.isArray(dayOrders)) return 0
  return dayOrders.reduce((sum, order) => sum + (order.cardCount || 0), 0)
}

// Calculate total duration for a day
const calculateDayDuration = (dayOrders: any[]) => {
  if (!dayOrders || !Array.isArray(dayOrders)) return 0
  return dayOrders.reduce((sum, order) => {
    const duration = order.duration ||
      order.durationMinutes ||
      order.estimatedDurationMinutes ||
      order.estimatedDuration ||
      0
    return sum + duration
  }, 0)
}

// Get CSS class for priority/delai
const getDelaiClass = (delai: string) => {
  if (!delai) return 'default'

  const map: Record<string, string> = {
    'X': 'excelsior',
    'F+': 'fast-plus',
    'F': 'fast',
    'C': 'classic',
    'E': 'economy'
  }
  return map[delai.toUpperCase()] || 'default'
}

// Get display label for priority/delai
const getDelaiLabel = (delai: string) => {
  if (!delai) return 'Unknown'

  const map: Record<string, string> = {
    'X': '⚡ Excelsior',
    'F+': '🚀 Fast+',
    'F': '⏩ Fast',
    'C': '📦 Classic',
    'E': '🐌 Economy'
  }
  return map[delai.toUpperCase()] || delai
}

// Get display label for order status
const getStatusLabel = (status: number | string) => {
  const statusMap: Record<number, string> = {
    1: '📥 To be received',
    9: '✅ Package accepted',
    10: '📸 To be scanned',
    11: '📂 To be opened',
    2: '📝 To be evaluated',
    3: '🔒 To be encapsulated',
    4: '📦 To be prepared',
    7: '🔓 To be unsealed',
    6: '👀 To be seen',
    41: '🚚 To be delivered',
    42: '📮 To be sent',
    5: '✈️ Sent',
    8: '🎉 Received'
  }

  const statusNum = typeof status === 'string' ? parseInt(status) : status
  return statusMap[statusNum] || `Status ${status}`
}

// Toggle order cards visibility and load if needed
const toggleOrderCards = async (order: any) => {
  // Toggle visibility
  order.showCards = !order.showCards

  // If hiding, just return
  if (!order.showCards) {
    return
  }

  // If already loaded, just show
  if (order.cards && order.cards.length > 0) {
    return
  }

  // If already loading, don't start another request
  if (order.loadingCards) {
    return
  }

  // Load cards from API
  order.loadingCards = true

  try {
    // Use orderId from the order object
    const orderId = order.orderId || order.id

    if (!orderId) {
      console.error('❌ No orderId found in order:', order)
      order.cards = []
      return
    }

    console.log('🃏 Loading cards for order:', orderId)

    // Try the planning endpoint first (more complete data)
    let url = `${API_BASE_URL}/api/planning/order/${orderId}/cards`
    console.log('🔗 Fetching from:', url)

    let response = await fetch(url)

    // If planning endpoint fails, try orders endpoint
    if (!response.ok) {
      console.log('⚠️ Planning endpoint failed, trying orders endpoint...')
      url = `${API_BASE_URL}/api/orders/${orderId}/cards`
      console.log('🔗 Fetching from:', url)
      response = await fetch(url)
    }

    if (response.ok) {
      const data = await response.json()
      console.log('✅ Cards API response:', data)

      // Handle different response formats
      if (data.success && Array.isArray(data.cards)) {
        // Format from planning endpoint: {success: true, cards: [...]}
        order.cards = data.cards
        console.log(`✅ Loaded ${order.cards.length} cards from planning endpoint`)
      } else if (Array.isArray(data)) {
        // Format from orders endpoint: [...]
        order.cards = data
        console.log(`✅ Loaded ${order.cards.length} cards from orders endpoint`)
      } else {
        console.warn('⚠️ Unexpected response format:', data)
        order.cards = []
      }

      // Ensure each card has the required fields
      order.cards = order.cards.map((card: any) => ({
        id: card.id,
        name: card.name || card.cardName || 'Unnamed Card',
        label_name: card.labelName || card.label_name || card.barcode || card.code_barre || '',
        code_barre: card.barcode || card.code_barre || '',
        duration: card.duration || 3,
        grade: card.grade || '',
        quantity: card.quantity || card.amount || 1
      }))

    } else {
      console.error('❌ API error:', response.status, response.statusText)
      order.cards = []
    }

  } catch (error) {
    console.error('❌ Error loading cards:', error)
    order.cards = []
  } finally {
    order.loadingCards = false
  }
}


// PAS DE WATCH - chargement uniquement au montage
onMounted(() => {
  loadEmployeeData()
  loadEmployeeOrders()
})
</script>


<style scoped>
.employee-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.header-section {
  margin-bottom: 24px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.back-button:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.back-button svg {
  width: 20px;
  height: 20px;
}

.employee-info-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 32px;
  margin-bottom: 24px;
  color: white;
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.3);
}

.employee-header {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.employee-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: bold;
  color: white;
  backdrop-filter: blur(10px);
  border: 3px solid rgba(255, 255, 255, 0.3);
}

.employee-details {
  flex: 1;
  min-width: 250px;
}

.employee-name {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
}

.employee-meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  opacity: 0.9;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.employee-stats {
  display: flex;
  gap: 16px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  padding: 16px 24px;
  text-align: center;
  min-width: 100px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  opacity: 0.8;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.filter-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.filter-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #374151;
}

.date-input {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: #5a67d8;
}

.icon {
  width: 16px;
  height: 16px;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #f3f4f6;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  color: #9ca3af;
}

.orders-timeline {
  background: white;
  border-radius: 12px;
  padding: 24px;
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 24px 0;
  padding-bottom: 16px;
  border-bottom: 2px solid #f3f4f6;
}

.day-group {
  margin-bottom: 32px;
}

.day-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border-radius: 12px;
  margin-bottom: 16px;
}

.day-date {
  display: flex;
  flex-direction: column;
}

.day-name {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.day-full-date {
  font-size: 14px;
  color: #6b7280;
}

.day-summary {
  display: flex;
  gap: 12px;
}

.summary-badge {
  padding: 6px 12px;
  background: white;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.day-orders {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-left: 20px;
}

.order-card {
  background: white;
  border: 2px solid #e5e7eb;
  border-left-width: 6px;
  border-radius: 12px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.order-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateX(4px);
}

.order-card.priority-excelsior {
  border-left-color: #ef4444;
  background: linear-gradient(90deg, rgba(239, 68, 68, 0.05) 0%, white 100%);
}

.order-card.priority-fast-plus {
  border-left-color: #f97316;
  background: linear-gradient(90deg, rgba(249, 115, 22, 0.05) 0%, white 100%);
}

.order-card.priority-fast {
  border-left-color: #eab308;
  background: linear-gradient(90deg, rgba(234, 179, 8, 0.05) 0%, white 100%);
}

.order-card.priority-classic {
  border-left-color: #3b82f6;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.05) 0%, white 100%);
}

.order-card.priority-economy {
  border-left-color: #10b981;
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.05) 0%, white 100%);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.order-time {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.time-range {
  color: #374151;
}

.duration-badge {
  padding: 4px 10px;
  background: #f3f4f6;
  border-radius: 6px;
  font-size: 13px;
  color: #6b7280;
}

.order-badges {
  display: flex;
  gap: 8px;
}

.priority-badge, .status-badge {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.priority-badge.priority-excelsior {
  background: #fee2e2;
  color: #991b1b;
}

.priority-badge.priority-fast-plus {
  background: #ffedd5;
  color: #9a3412;
}

.priority-badge.priority-fast {
  background: #fef3c7;
  color: #92400e;
}

.priority-badge.priority-classic {
  background: #dbeafe;
  color: #1e40af;
}

.priority-badge.priority-economy {
  background: #d1fae5;
  color: #065f46;
}

.status-badge {
  background: #e0e7ff;
  color: #3730a3;
}

.order-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.order-main {
  flex: 1;
}

.order-number {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px 0;
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 14px;
  color: #6b7280;
}

.expand-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.expand-btn:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.cards-list {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.cards-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
  padding: 20px;
  color: #6b7280;
}

.spinner-sm {
  width: 20px;
  height: 20px;
  border: 2px solid #f3f4f6;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 12px;
}

.card-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;
}

.card-item:hover {
  background: #f3f4f6;
  border-color: #d1d5db;
}

.card-icon {
  font-size: 24px;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-name {
  font-weight: 600;
  color: #111827;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-label {
  font-size: 12px;
  color: #6b7280;
}

.card-duration {
  padding: 4px 8px;
  background: white;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #667eea;
}

.no-cards {
  text-align: center;
  padding: 20px;
  color: #9ca3af;
  font-style: italic;
}

@media (max-width: 768px) {
  .employee-header {
    flex-direction: column;
    text-align: center;
  }

  .employee-stats {
    width: 100%;
    justify-content: center;
  }

  .day-header {
    flex-direction: column;
    gap: 12px;
    text-align: center;
  }

  .order-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
