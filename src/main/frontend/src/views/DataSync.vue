<template>
  <div class="data-sync-container">
    <!-- Header -->
    <div class="sync-header">
      <h2>🔄 Database Synchronization</h2>
      <p class="subtitle">Sync data between Symfony (dev) and Spring Boot (dev-planning)</p>
    </div>

    <!-- Real-time Progress (only shown during sync) -->
    <div v-if="syncing" class="card progress-card">
      <div class="progress-content">
        <div class="progress-header">
          <span class="progress-operation">{{ currentOperation }}</span>
          <span class="progress-percentage">{{ Math.round(progress) }}%</span>
        </div>

        <div class="progress-bar-container">
          <div class="progress-bar-fill" :style="{ width: progress + '%' }"></div>
        </div>

        <div class="progress-details">
          <div class="detail-row">
            <span class="detail-label">Status:</span>
            <span :class="['detail-value', phaseClass]">{{ progressMessage }}</span>
          </div>
          <div class="detail-row" v-if="totalItems > 0">
            <span class="detail-label">Progress:</span>
            <span class="detail-value">{{ itemsProcessed.toLocaleString() }} / {{ totalItems.toLocaleString() }} items</span>
          </div>
          <div class="detail-row" v-if="estimatedSecondsRemaining > 0">
            <span class="detail-label">Time remaining:</span>
            <span class="detail-value">~{{ Math.ceil(estimatedSecondsRemaining) }}s</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Status Check -->
    <div class="card status-card">
      <h3>📊 Current Synchronization Status</h3>
      <p class="text-muted">Compare data between Symfony (dev) and Spring Boot (dev-planning)</p>

      <button
        @click="checkSyncStatus"
        :disabled="loadingStatus"
        class="btn btn-secondary"
      >
        {{ loadingStatus ? '⏳ Checking...' : '🔍 Check Status' }}
      </button>

      <div v-if="syncStatus" class="status-table">
        <table>
          <thead>
          <tr>
            <th>Table</th>
            <th>Dev (Symfony)</th>
            <th>Dev-Planning</th>
            <th>Difference</th>
            <th>Status</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="table in syncStatus.tableComparison" :key="table.table">
            <td><strong>{{ table.table }}</strong></td>
            <td>{{ table.symfony?.toLocaleString() || 0 }}</td>
            <td>{{ table.local?.toLocaleString() || 0 }}</td>
            <td :class="{ 'text-danger': table.difference !== 0 }">
              {{ table.difference }}
            </td>
            <td>
              <span v-if="table.inSync" class="badge badge-success">✅ In Sync</span>
              <span v-else class="badge badge-warning">⚠️ Out of Sync</span>
            </td>
          </tr>
          </tbody>
        </table>
        <p class="text-muted mt-2">Last checked: {{ formatTime(syncStatus.timestamp) }}</p>
      </div>
    </div>

    <!-- Sync Actions -->
    <div class="sync-actions">
      <div class="card action-card">
        <h3>🔄 Full Synchronization</h3>
        <p>Sync all tables (orders, cards, translations, certifications)</p>
        <button
          @click="syncAll"
          :disabled="syncing"
          class="btn btn-primary"
        >
          {{ syncing ? '⏳ Syncing...' : '🚀 Sync All Data' }}
        </button>
      </div>

      <div class="card action-card">
        <h3>📦 Sync Orders Only</h3>
        <p>Sync only the orders table</p>
        <button
          @click="syncOrders"
          :disabled="syncing"
          class="btn btn-info"
        >
          {{ syncing ? '⏳ Syncing...' : '📦 Sync Orders' }}
        </button>
      </div>

      <div class="card action-card">
        <h3>🎴 Sync Cards Only</h3>
        <p>Sync cards, translations, and certifications</p>
        <button
          @click="syncCards"
          :disabled="syncing"
          class="btn btn-info"
        >
          {{ syncing ? '⏳ Syncing...' : '🎴 Sync Cards' }}
        </button>
      </div>

      <div class="card action-card">
        <h3>⚡ Incremental Sync</h3>
        <p>Sync only recent changes (last 24 hours)</p>
        <button
          @click="syncIncremental"
          :disabled="syncing"
          class="btn btn-success"
        >
          {{ syncing ? '⏳ Syncing...' : '⚡ Quick Sync' }}
        </button>
      </div>
    </div>

    <!-- Sync History -->
    <div v-if="syncHistory.length > 0" class="card history-card">
      <h3>📜 Sync History</h3>
      <div class="history-list">
        <div
          v-for="(entry, index) in syncHistory"
          :key="index"
          :class="['history-entry', entry.success ? 'success' : 'error']"
        >
          <span class="time">{{ formatTime(entry.timestamp) }}</span>
          <span class="message">{{ entry.message }}</span>
          <span v-if="entry.duration" class="duration">({{ formatDuration(entry.duration) }})</span>
          <span v-if="entry.success" class="badge badge-success">✅ Success</span>
          <span v-else class="badge badge-danger">❌ Failed</span>
        </div>
      </div>
    </div>

    <!-- Notifications -->
    <div v-if="notification" :class="['notification', notification.type]">
      {{ notification.message }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import authService from '@/services/authService'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// State
const syncing = ref(false)
const loadingStatus = ref(false)
const syncStatus = ref<any>(null)
const syncHistory = ref<any[]>([])
const notification = ref<any>(null)

// Real-time progress state
const progress = ref(0)
const currentOperation = ref('')
const progressMessage = ref('')
const phase = ref('')
const itemsProcessed = ref(0)
const totalItems = ref(0)
const estimatedSecondsRemaining = ref(0)
const currentSyncId = ref('')

// SSE connection
let eventSource: EventSource | null = null

// Computed
const phaseClass = computed(() => {
  const phaseMap: Record<string, string> = {
    'STARTING': 'phase-starting',
    'FETCHING': 'phase-fetching',
    'PROCESSING': 'phase-processing',
    'SAVING': 'phase-saving',
    'COMPLETED': 'phase-completed',
    'ERROR': 'phase-error'
  }
  return phaseMap[phase.value] || 'phase-default'
})

// Format time
const formatTime = (timestamp: any) => {
  return new Date(timestamp).toLocaleString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// Format duration
const formatDuration = (seconds: number) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return mins > 0 ? `${mins}m ${secs}s` : `${secs}s`
}

// Show notification
const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
  notification.value = { message, type }
  setTimeout(() => {
    notification.value = null
  }, 5000)
}

// Add to history
const addToHistory = (message: string, success: boolean, duration?: number) => {
  syncHistory.value.unshift({
    message,
    success,
    duration,
    timestamp: new Date().toISOString()
  })
  if (syncHistory.value.length > 10) {
    syncHistory.value.pop()
  }
}

// Close SSE connection
const closeSSEConnection = () => {
  if (eventSource) {
    console.log('🔌 Closing SSE connection')
    eventSource.close()
    eventSource = null
  }
}

// Connect to SSE for real-time progress
const connectSSE = (syncId: string) => {
  closeSSEConnection()

  currentSyncId.value = syncId

  // ✅ Add JWT token to URL (SSE doesn't support headers)
  const token = localStorage.getItem('jwt_token')
  const url = `${API_BASE_URL}/api/sync/progress/stream/${syncId}?token=${token}`

  console.log('🔌 Connecting to SSE:', url)

  eventSource = new EventSource(url)

  eventSource.addEventListener('progress', (event: MessageEvent) => {
    try {
      const data = JSON.parse(event.data)
      console.log('📊 Progress update:', data)

      progress.value = data.percentage || 0
      currentOperation.value = data.currentOperation || ''
      progressMessage.value = data.message || ''
      phase.value = data.phase || ''
      itemsProcessed.value = data.itemsProcessed || 0
      totalItems.value = data.totalItems || 0
      estimatedSecondsRemaining.value = data.estimatedSecondsRemaining || 0

      if (data.completed) {
        if (data.error) {
          showNotification(`❌ ${data.errorMessage || 'Sync failed'}`, 'error')
        } else {
          showNotification('✅ Synchronization completed', 'success')
        }

        setTimeout(() => {
          closeSSEConnection()
          syncing.value = false
          progress.value = 100
          checkSyncStatus()
        }, 2000)
      }
    } catch (error) {
      console.error('Error parsing SSE:', error)
    }
  })

  eventSource.onerror = (error) => {
    console.error('❌ SSE error:', error)
    if (eventSource?.readyState === EventSource.CLOSED) {
      if (syncing.value && progress.value < 100) {
        showNotification('Connection lost', 'error')
        syncing.value = false
      }
    }
  }

  eventSource.onopen = () => {
    console.log('✅ SSE connected')
  }
}

// Generic sync function
const performSync = async (endpoint: string, operationName: string) => {
  if (syncing.value) {
    console.warn('⚠️ Sync already in progress')
    return
  }

  syncing.value = true
  progress.value = 0
  currentOperation.value = operationName
  progressMessage.value = 'Initializing...'
  phase.value = 'STARTING'
  itemsProcessed.value = 0
  totalItems.value = 0
  estimatedSecondsRemaining.value = 0

  const syncId = `sync-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  connectSSE(syncId)
  const startTime = Date.now()

  try {
    // ✅ Use authService for authenticated requests
    const result = await authService.post(`/api/sync/${endpoint}?syncId=${syncId}`)

    const duration = Math.floor((Date.now() - startTime) / 1000)
    addToHistory(`${operationName} completed`, true, duration)
    showNotification(`✅ ${operationName} completed`, 'success')

    progress.value = 100
    progressMessage.value = 'Completed!'

    setTimeout(() => {
      closeSSEConnection()
      syncing.value = false
      checkSyncStatus()
    }, 2000)

  } catch (error: any) {
    console.error('Sync error:', error)
    showNotification(`❌ ${operationName} failed: ${error.message}`, 'error')
    addToHistory(`${operationName} failed`, false)
    closeSSEConnection()
    syncing.value = false
  }
}

// Check sync status
const checkSyncStatus = async () => {
  loadingStatus.value = true

  try {
    // ✅ Use authService for authenticated requests
    const data = await authService.get('/api/sync/status')

    syncStatus.value = data
    showNotification('Status checked successfully', 'success')

  } catch (error: any) {
    console.error('Error checking sync status:', error)
    showNotification(`Failed to check sync status: ${error.message}`, 'error')
  } finally {
    loadingStatus.value = false
  }
}

// Sync operations
const syncAll = async () => {
  if (!confirm('⚠️ This will replace ALL data. Continue?')) return
  await performSync('all', 'Full Synchronization')
}

const syncOrders = async () => {
  if (!confirm('⚠️ This will replace order data. Continue?')) return
  await performSync('orders', 'Orders Sync')
}

const syncCards = async () => {
  if (!confirm('⚠️ This will replace card data. Continue?')) return
  await performSync('cards', 'Cards Sync')
}

const syncIncremental = async () => {
  await performSync('incremental', 'Incremental Sync')
}

// Lifecycle
onMounted(async () => {
  console.log('📊 DataSync mounted')
  setTimeout(async () => {
    await checkSyncStatus()
  }, 100)
})

onUnmounted(() => {
  console.log('🔌 DataSync unmounting')
  closeSSEConnection()
})
</script>

<style scoped>
.data-sync-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

.sync-header {
  margin-bottom: 2rem;
}

.sync-header h2 {
  font-size: 2rem;
  font-weight: bold;
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.subtitle {
  color: #6b7280;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

/* Progress Card */
.progress-card {
  border-left: 4px solid #3b82f6;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.progress-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-operation {
  font-weight: 600;
  color: #374151;
  font-size: 1.1rem;
}

.progress-percentage {
  font-weight: 700;
  color: #3b82f6;
  font-size: 1.5rem;
}

.progress-bar-container {
  width: 100%;
  height: 16px;
  background: #e5e7eb;
  border-radius: 9999px;
  overflow: hidden;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.06);
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6, #2563eb);
  border-radius: 9999px;
  transition: width 0.5s ease-out;
}

.progress-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding-top: 0.5rem;
  border-top: 1px solid #e5e7eb;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.detail-label {
  color: #6b7280;
  font-weight: 500;
}

.detail-value {
  color: #1f2937;
  font-weight: 600;
}

/* Phase classes */
.phase-starting {
  color: #f59e0b;
}

.phase-fetching {
  color: #3b82f6;
}

.phase-processing {
  color: #8b5cf6;
}

.phase-saving {
  color: #10b981;
}

.phase-completed {
  color: #059669;
}

.phase-error {
  color: #ef4444;
}

/* Status Card */
.status-card h3 {
  margin-bottom: 0.5rem;
  color: #1f2937;
}

.text-muted {
  color: #6b7280;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.status-table {
  margin-top: 1.5rem;
}

.status-table table {
  width: 100%;
  border-collapse: collapse;
}

.status-table th {
  background: #f9fafb;
  padding: 0.75rem;
  text-align: left;
  font-weight: 600;
  color: #374151;
  border-bottom: 2px solid #e5e7eb;
}

.status-table td {
  padding: 0.75rem;
  border-bottom: 1px solid #e5e7eb;
}

.text-danger {
  color: #ef4444;
  font-weight: 600;
}

/* Badges */
.badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.badge-success {
  background: #d1fae5;
  color: #065f46;
}

.badge-warning {
  background: #fef3c7;
  color: #92400e;
}

.badge-danger {
  background: #fee2e2;
  color: #991b1b;
}

/* Buttons */
.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
}

.btn-secondary {
  background: #6b7280;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background: #4b5563;
}

.btn-info {
  background: #0ea5e9;
  color: white;
}

.btn-info:hover:not(:disabled) {
  background: #0284c7;
}

.btn-success {
  background: #10b981;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #059669;
}

/* Sync Actions */
.sync-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.action-card {
  text-align: center;
}

.action-card h3 {
  font-size: 1.125rem;
  margin-bottom: 0.5rem;
  color: #1f2937;
}

.action-card p {
  color: #6b7280;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

/* History */
.history-card h3 {
  margin-bottom: 1rem;
  color: #1f2937;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.history-entry {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 0.875rem;
}

.history-entry.success {
  background: #f0fdf4;
  border-left: 3px solid #10b981;
}

.history-entry.error {
  background: #fef2f2;
  border-left: 3px solid #ef4444;
}

.history-entry .time {
  color: #6b7280;
  font-size: 0.75rem;
  white-space: nowrap;
}

.history-entry .message {
  flex: 1;
  color: #1f2937;
  font-weight: 500;
}

.history-entry .duration {
  color: #6b7280;
  font-size: 0.75rem;
}

/* Notifications */
.notification {
  position: fixed;
  top: 1rem;
  right: 1rem;
  padding: 1rem 1.5rem;
  border-radius: 6px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  font-weight: 500;
  z-index: 1000;
  animation: slideInRight 0.3s ease-out;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.notification.success {
  background: #d1fae5;
  color: #065f46;
  border-left: 4px solid #10b981;
}

.notification.error {
  background: #fee2e2;
  color: #991b1b;
  border-left: 4px solid #ef4444;
}

.mt-2 {
  margin-top: 0.5rem;
}
</style>
