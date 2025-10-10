<template>
  <div class="orders-view">
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-3xl font-bold text-gray-900">📦 Orders Management</h1>
      <p class="text-gray-600 mt-1">View and manage all Pokemon card orders</p>
    </div>

    <!-- Statistiques rapides -->
    <!-- ✅ DELAI STATISTICS  -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6" v-if="statistics">
      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-red-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Excelsior (X)</p>
            <p class="text-2xl font-bold text-red-600">{{ statistics.X || 0 }}</p>
          </div>
          <div class="text-3xl text-red-600">🔴</div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-orange-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Fast+ (F+)</p>
            <p class="text-2xl font-bold text-orange-600">{{ statistics['F+'] || 0 }}</p>
          </div>
          <div class="text-3xl text-orange-600">🟠</div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-yellow-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Fast (F)</p>
            <p class="text-2xl font-bold text-yellow-600">{{ statistics.F || 0 }}</p>
          </div>
          <div class="text-3xl text-yellow-600">🟡</div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-lg shadow border-l-4 border-green-500">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-600">Classic (C)</p>
            <p class="text-2xl font-bold text-green-600">{{ statistics.C || 0 }}</p>
          </div>
          <div class="text-3xl text-green-600">🟢</div>
        </div>
      </div>
    </div>

    <!-- ✅ STATUS STATISTICS - Version avec style différencié -->
    <div class="bg-gray-50 rounded-lg p-6 mb-6">
      <h3 class="text-lg font-semibold text-gray-900 mb-4">📊 Orders by Status</h3>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
        <!-- To Receive -->
        <div class="bg-white p-4 rounded-lg shadow-sm hover:shadow-md transition-shadow">
          <div class="flex items-center justify-between mb-2">
            <span class="text-3xl">📦</span>
            <span class="text-xs font-medium text-blue-600 bg-blue-100 px-2 py-1 rounded">Active</span>
          </div>
          <p class="text-2xl font-bold text-gray-900">{{ statusStatistics?.toReceive || 0 }}</p>
          <p class="text-sm text-gray-600">To Receive</p>
        </div>

        <!-- Package Accepted -->
        <div class="bg-white p-4 rounded-lg shadow-sm hover:shadow-md transition-shadow">
          <div class="flex items-center justify-between mb-2">
            <span class="text-3xl">✅</span>
            <span class="text-xs font-medium text-indigo-600 bg-indigo-100 px-2 py-1 rounded">Active</span>
          </div>
          <p class="text-2xl font-bold text-gray-900">{{ statusStatistics?.packageAccepted || 0 }}</p>
          <p class="text-sm text-gray-600">Package Accepted</p>
        </div>

        <!-- In Processing -->
        <div class="bg-white p-4 rounded-lg shadow-sm hover:shadow-md transition-shadow">
          <div class="flex items-center justify-between mb-2">
            <span class="text-3xl">⚙️</span>
            <span class="text-xs font-medium text-yellow-600 bg-yellow-100 px-2 py-1 rounded">Active</span>
          </div>
          <p class="text-2xl font-bold text-gray-900">{{ statusStatistics?.inProcessing || 0 }}</p>
          <p class="text-sm text-gray-600">In Processing</p>
        </div>

        <!-- To Deliver -->
        <div class="bg-white p-4 rounded-lg shadow-sm hover:shadow-md transition-shadow">
          <div class="flex items-center justify-between mb-2">
            <span class="text-3xl">🚚</span>
            <span class="text-xs font-medium text-orange-600 bg-orange-100 px-2 py-1 rounded">Active</span>
          </div>
          <p class="text-2xl font-bold text-gray-900">{{ statusStatistics?.toDeliver || 0 }}</p>
          <p class="text-sm text-gray-600">To Deliver</p>
        </div>

        <!-- Completed -->
        <div class="bg-white p-4 rounded-lg shadow-sm hover:shadow-md transition-shadow">
          <div class="flex items-center justify-between mb-2">
            <span class="text-3xl">🎉</span>
            <span class="text-xs font-medium text-green-600 bg-green-100 px-2 py-1 rounded">Done</span>
          </div>
          <p class="text-2xl font-bold text-gray-900">{{ statusStatistics?.completed || 0 }}</p>
          <p class="text-sm text-gray-600">Completed</p>
        </div>
      </div>
    </div>


    <!-- Filters -->
    <div class="bg-white rounded-lg shadow p-6 mb-6">
      <h2 class="text-lg font-semibold text-gray-900 mb-4">🔍 Filters</h2>

      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Search</label>
          <input
            v-model="filters.search"
            @input="debouncedSearch"
            type="text"
            placeholder="Order number, client ref..."
            class="w-full border border-gray-300 rounded-md px-3 py-2"
          />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Status</label>
          <select v-model="filters.status" @change="loadOrders(0)" class="w-full border border-gray-300 rounded-md px-3 py-2">
            <option value="all">All Statuses</option>
            <option :value="1">To be received</option>
            <option :value="2">To be evaluated</option>
            <option :value="3">To be encapsulated</option>
            <option :value="4">To be prepared</option>
            <option :value="5">Sent</option>
            <option :value="8">Received</option>
          </select>
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">Priority (Delai)</label>
          <select v-model="filters.delai" @change="loadOrders(0)" class="w-full border border-gray-300 rounded-md px-3 py-2">
            <option value="all">All Delais</option>
            <option value="X">🔴 Excelsior (X)</option>
            <option value="F+">🟠 Fast+ (F+)</option>
            <option value="F">🟡 Fast (F)</option>
            <option value="C">🟢 Classic (C)</option>
          </select>
        </div>

        <div class="flex items-end">
          <button
            @click="loadOrders(0)"
            class="w-full bg-gray-600 text-white px-4 py-2 rounded-md hover:bg-gray-700 transition-colors"
          >
            🔄 Reload
          </button>
        </div>
      </div>
    </div>

    <!-- Pagination Info with Card Totals -->
    <div class="bg-white rounded-lg shadow p-4 mb-6" v-if="pagination">
      <div class="flex items-center justify-between">
        <!-- Left: Order range -->
        <div class="text-sm text-gray-600">
          Showing {{ (pagination.page * pagination.size) + 1 }} to {{ Math.min((pagination.page + 1) * pagination.size, pagination.total) }} of {{ pagination.total }} orders
        </div>

        <!-- Center: Card totals -->
        <div class="text-sm font-semibold text-blue-600">
          🃏 {{ formatNumber(pagination.pageCardTotal) }} / {{ formatNumber(pagination.totalCards) }} cards
        </div>

        <!-- Right: Page number -->
        <div class="text-sm text-gray-600">
          Page {{ pagination.page + 1 }} of {{ pagination.totalPages }}
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
      <span class="text-gray-600 mt-3 block">Loading orders...</span>
    </div>

    <!-- Table -->
    <div v-else-if="orders.length > 0" class="bg-white rounded-lg shadow overflow-hidden">
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Order</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Date</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Cards</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Delai</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Price</th>
          </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
          <tr v-for="order in orders" :key="order.id" class="hover:bg-gray-50">
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="text-sm font-medium text-gray-900">{{ order.orderNumber }}</div>
              <div class="text-sm text-gray-500">{{ order.clientOrderNumber }}</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
              {{ order.creationDate }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <div class="text-sm text-gray-900">{{ order.cardCount }} cards</div>
              <div class="text-sm text-gray-500">{{ order.cardsWithName }} with name ({{ order.namePercentage }}%)</div>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
                <span :class="['inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium', getDelaiColor(order.delai)]">
                  {{ getDelaiLabel(order.delai) }}
                </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span class="text-sm text-gray-900">{{ getStatusText(order.status) }}</span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
              €{{ formatPrice(order.totalPrice) }}
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Pagination Controls -->
    <div v-if="pagination && pagination.totalPages > 1" class="bg-white rounded-lg shadow p-4 mt-6">
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-2">
          <button
            @click="loadOrders(0)"
            :disabled="!pagination.hasPrevious"
            :class="[
              'px-3 py-2 text-sm rounded-md',
              pagination.hasPrevious
                ? 'bg-blue-500 text-white hover:bg-blue-600'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            ]"
          >
            First
          </button>

          <button
            @click="loadOrders(pagination.page - 1)"
            :disabled="!pagination.hasPrevious"
            :class="[
              'px-3 py-2 text-sm rounded-md',
              pagination.hasPrevious
                ? 'bg-blue-500 text-white hover:bg-blue-600'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            ]"
          >
            Previous
          </button>
        </div>

        <div class="flex items-center space-x-2">
          <span class="text-sm text-gray-600">Page {{ pagination.page + 1 }} of {{ pagination.totalPages }}</span>
        </div>

        <div class="flex items-center space-x-2">
          <button
            @click="loadOrders(pagination.page + 1)"
            :disabled="!pagination.hasNext"
            :class="[
              'px-3 py-2 text-sm rounded-md',
              pagination.hasNext
                ? 'bg-blue-500 text-white hover:bg-blue-600'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            ]"
          >
            Next
          </button>

          <button
            @click="loadOrders(pagination.totalPages - 1)"
            :disabled="!pagination.hasNext"
            :class="[
              'px-3 py-2 text-sm rounded-md',
              pagination.hasNext
                ? 'bg-blue-500 text-white hover:bg-blue-600'
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            ]"
          >
            Last
          </button>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else-if="!loading" class="text-center py-12">
      <p class="text-gray-500">No orders found</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject } from 'vue'

// Reactive data
const orders = ref([])
const loading = ref(false)
const pagination = ref(null)
const statistics = ref(null)
const statusStatistics = ref(null)

// Filters
const filters = ref({
  search: '',
  status: 'all',
  delai: 'all'
})

const showNotification = inject('showNotification')
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://146.190.204.228:8080'

// Helper functions
const getDelaiColor = (delai) => {
  switch (delai?.toUpperCase()) {
    case 'X': return 'bg-red-100 text-red-800'
    case 'F+': return 'bg-orange-100 text-orange-800'
    case 'F': return 'bg-yellow-100 text-yellow-800'
    case 'C':
    case 'E': return 'bg-green-100 text-green-800'
    default: return 'bg-gray-100 text-gray-800'
  }
}

const getDelaiLabel = (delai) => {
  switch (delai?.toUpperCase()) {
    case 'X': return '🔴 Excelsior'
    case 'F+': return '🟠 Fast+'
    case 'F': return '🟡 Fast'
    case 'C': return '🟢 Classic'
    case 'E': return '🟢 Economy'
    default: return '⚪ Unknown'
  }
}

const getStatusText = (status) => {
  const statusMap = {
    1: 'To be received',
    2: 'To be evaluated',
    3: 'To be encapsulated',
    4: 'To be prepared',
    5: 'Sent',
    8: 'Received'
  }
  return statusMap[status] || 'Unknown'
}

const formatPrice = (price) => {
  return price ? price.toFixed(2) : '0.00'
}

const formatNumber = (num) => {
  return num ? num.toLocaleString() : '0'
}

// Main load function
const loadOrders = async (page = 0) => {
  loading.value = true

  console.log('Loading with filters:', {
    delai: filters.value.delai,
    status: filters.value.status,
    search: filters.value.search
  })

  try {
    const params = new URLSearchParams({
      page: page.toString(),
      size: '500'
    })

    if (filters.value.delai !== 'all') {
      console.log('Adding delai filter:', filters.value.delai)
      params.append('delai', filters.value.delai)
    }

    if (filters.value.status !== 'all') {
      console.log('Adding status filter:', filters.value.status)
      params.append('status', filters.value.status)
    }

    if (filters.value.search.trim()) {
      params.append('search', filters.value.search.trim())
    }

    console.log('Loading orders with params:', params.toString())

    const response = await fetch(`${API_BASE_URL}/api/orders?${params}`)

    if (response.ok) {
      const data = await response.json()
      orders.value = data.orders || []
      pagination.value = data.pagination || {}
      statistics.value = data.delaiDistribution || {}
      statusStatistics.value = data.statusStats || {}

      console.log(`✅ Loaded ${orders.value.length} orders - Page cards: ${pagination.value.pageCardTotal}, Total cards: ${pagination.value.totalCards}`)

    } else {
      throw new Error(`HTTP ${response.status}`)
    }

  } catch (error) {
    console.error('❌ Error loading orders:', error)
    orders.value = []
    pagination.value = null
    statusStatistics.value = null

  } finally {
    loading.value = false
  }
}

// Debounced search
let searchTimeout = null
const debouncedSearch = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    loadOrders(0)
  }, 500)
}

// Lifecycle
onMounted(() => {
  console.log('Orders view mounted')
  loadOrders(0)
})
</script>

<style scoped>
.orders-view {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.hover\:bg-gray-50:hover {
  background-color: #f9fafb;
}

.transition-colors {
  transition-property: color, background-color, border-color;
  transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  transition-duration: 150ms;
}
</style>
