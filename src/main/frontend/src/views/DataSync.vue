<template>
  <div class="data-sync-container">
    <div class="sync-header">
      <h2>🔄 Data Synchronization</h2>
      <p class="subtitle">Sync data from Symfony backend (dev) to Planning system (dev-planning)</p>
    </div>

    <!-- ========== PROGRESS BAR - AVEC SSE ========== -->
    <div v-if="syncing" class="progress-card card">
      <div class="progress-content">
        <div class="progress-header">
          <span class="progress-operation">{{ currentOperation }}</span>
          <span class="progress-percentage">{{ progress }}%</span>
        </div>

        <div class="progress-bar-container">
          <div
            class="progress-bar-fill"
            :style="{ width: progress + '%' }"
            :class="{ 'progress-complete': progress === 100 }"
          ></div>
        </div>

        <div class="progress-details">
          <div class="progress-info">
            <span class="spinner" v-if="progress < 100">⏳</span>
            <span v-else>✅</span>
            <span>{{ progressMessage }}</span>
          </div>

          <div v-if="itemsProcessed > 0" class="progress-items">
            <span>{{ itemsProcessed.toLocaleString() }} / {{ totalItems.toLocaleString() }} items</span>
          </div>

          <div v-if="estimatedSecondsRemaining > 0" class="progress-estimate">
            <span>Estimated: ~{{ formatDuration(estimatedSecondsRemaining) }} remaining</span>
          </div>
        </div>

        <!-- Phase indicator -->
        <div class="progress-phase">
          <span :class="['phase-badge', phaseClass]">{{ phase }}</span>
        </div>
      </div>
    </div>

    <!-- Sync Status Card -->
    <div class="card status-card">
      <h3>📊 Sync Status</h3>
      <button
        @click="checkSyncStatus"
        :disabled="loadingStatus || syncing"
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
import { ref, computed, onUnmounted } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// State
const syncing = ref(false)
const loadingStatus = ref(false)
const syncStatus = ref<any>(null)
const syncHistory = ref<any[]>([])
const notification = ref<any>(null)

// Real-time progress state from SSE
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

// Computed phase class for styling
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

// Clean up SSE connection on unmount
onUnmounted(() => {
  closeSSEConnection()
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
  syncHistory.value = [
    {
      message,
      success,
      duration,
      timestamp: new Date()
    },
    ...syncHistory.value.slice(0, 9)
  ]
}

// Connect to SSE stream
const connectSSE = (syncId: string) => {
  closeSSEConnection()

  currentSyncId.value = syncId
  const sseUrl = `${API_BASE_URL}/api/sync/progress/stream/${syncId}`

  console.log('📡 Connecting to SSE:', sseUrl)

  eventSource = new EventSource(sseUrl)

  eventSource.addEventListener('progress', (event: MessageEvent) => {
    try {
      const progressData = JSON.parse(event.data)
      console.log('📊 Progress update:', progressData)

      progress.value = progressData.percentage || 0
      currentOperation.value = progressData.currentOperation || ''
      progressMessage.value = progressData.message || ''
      phase.value = progressData.phase || ''
      itemsProcessed.value = progressData.itemsProcessed || 0
      totalItems.value = progressData.totalItems || 0
      estimatedSecondsRemaining.value = progressData.estimatedSecondsRemaining || 0

      if (progressData.completed) {
        if (progressData.error) {
          showNotification(`❌ ${progressData.errorMessage}`, 'error')
        } else {
          showNotification(`✅ ${progressData.message}`, 'success')
        }

        setTimeout(() => {
          closeSSEConnection()
          syncing.value = false
          checkSyncStatus()
        }, 1000)
      }
    } catch (error) {
      console.error('Error parsing SSE data:', error)
    }
  })

  eventSource.onerror = (error) => {
    console.error('❌ SSE connection error:', error)
    closeSSEConnection()

    if (syncing.value && progress.value < 100) {
      showNotification('Connection lost. Please try again.', 'error')
      syncing.value = false
    }
  }

  eventSource.onopen = () => {
    console.log('✅ SSE connection established')
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

// Generic sync function with SSE
const performSync = async (endpoint: string, operationName: string) => {
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
    const response = await fetch(`${API_BASE_URL}/api/sync/${endpoint}?syncId=${syncId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })

    const result = await response.json()

    if (response.ok) {
      const duration = Math.floor((Date.now() - startTime) / 1000)
      const message = `${operationName} completed`
      addToHistory(message, true, duration)
    } else {
      throw new Error(result.error || `HTTP ${response.status}`)
    }
  } catch (error: any) {
    console.error('Sync error:', error)
    showNotification(`❌ ${operationName} failed: ${error.message}`, 'error')
    addToHistory(`${operationName} failed`, false)

    closeSSEConnection()
    syncing.value = false
    progress.value = 0
  }
}

// Check sync status
const checkSyncStatus = async () => {
  loadingStatus.value = true

  try {
    const response = await fetch(`${API_BASE_URL}/api/sync/status`)

    if (response.ok) {
      syncStatus.value = await response.json()
      showNotification('Status checked successfully', 'success')
    } else {
      throw new Error(`HTTP ${response.status}`)
    }
  } catch (error) {
    console.error('Error checking sync status:', error)
    showNotification('Failed to check sync status', 'error')
  } finally {
    loadingStatus.value = false
  }
}

// Sync operations
const syncAll = async () => {
  if (!confirm('⚠️ This will replace ALL data in dev-planning with data from dev. Continue?')) {
    return
  }
  await performSync('all', 'Full Synchronization')
}

const syncOrders = async () => {
  if (!confirm('⚠️ This will replace order data in dev-planning. Continue?')) {
    return
  }
  await performSync('orders', 'Orders Sync')
}

const syncCards = async () => {
  if (!confirm('⚠️ This will replace card data in dev-planning. Continue?')) {
    return
  }
  await performSync('cards', 'Cards Sync')
}

const syncIncremental = async () => {
  await performSync('incremental', 'Quick Sync')
}
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
  position: relative;
}

.progress-bar-fill::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.3),
    transparent
  );
  animation: shimmer 2s infinite;
}

.progress-complete {
  background: linear-gradient(90deg, #10b981, #059669) !important;
}

@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.progress-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
  color: #374151;
  font-weight: 500;
}

.progress-items, .progress-estimate {
  font-size: 0.875rem;
  color: #6b7280;
  padding-left: 1.5rem;
}

.spinner {
  animation: spin 2s linear infinite;
  display: inline-block;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.progress-phase {
  display: flex;
  justify-content: flex-start;
}

.phase-badge {
  padding: 0.375rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.phase-starting { background: #dbeafe; color: #1e40af; }
.phase-fetching { background: #fef3c7; color: #92400e; }
.phase-processing { background: #ddd6fe; color: #5b21b6; }
.phase-saving { background: #fbcfe8; color: #9f1239; }
.phase-completed { background: #d1fae5; color: #065f46; }
.phase-error { background: #fee2e2; color: #991b1b; }

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

/* Status Card */
.status-card h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1rem;
}

.status-table {
  margin-top: 1rem;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f9fafb;
}

th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-size: 0.75rem;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  border-bottom: 1px solid #e5e7eb;
}

td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f3f4f6;
}

.text-danger {
  color: #dc2626;
  font-weight: 600;
}

.text-muted {
  color: #6b7280;
  font-size: 0.875rem;
}

.mt-2 {
  margin-top: 0.5rem;
}

/* Sync Actions */
.sync-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.action-card h3 {
  font-size: 1.125rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
}

.action-card p {
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 1rem;
}

/* Buttons */
.btn {
  width: 100%;
  padding: 0.625rem 1rem;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #3b82f6;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(59, 130, 246, 0.3);
}

.btn-secondary {
  background: #e5e7eb;
  color: #374151;
}

.btn-secondary:hover:not(:disabled) {
  background: #d1d5db;
}

.btn-info {
  background: #06b6d4;
  color: white;
}

.btn-info:hover:not(:disabled) {
  background: #0891b2;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(6, 182, 212, 0.3);
}

.btn-success {
  background: #10b981;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #059669;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px rgba(16, 185, 129, 0.3);
}

/* Badges */
.badge {
  display: inline-flex;
  align-items: center;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
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

/* History Card */
.history-card h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1rem;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.history-entry {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-radius: 6px;
  border: 1px solid;
}

.history-entry.success {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.history-entry.error {
  background: #fef2f2;
  border-color: #fecaca;
}

.history-entry .time {
  font-size: 0.75rem;
  color: #6b7280;
}

.history-entry .message {
  flex: 1;
  margin: 0 1rem;
  font-size: 0.875rem;
}

.history-entry .duration {
  font-size: 0.75rem;
  color: #6b7280;
  font-style: italic;
}

/* Notification */
.notification {
  position: fixed;
  bottom: 1rem;
  right: 1rem;
  padding: 1rem 1.5rem;
  border-radius: 8px;
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
  color: white;
  font-weight: 500;
  animation: slideIn 0.3s ease-out;
  z-index: 1000;
  max-width: 400px;
}

.notification.success {
  background: #10b981;
}

.notification.error {
  background: #ef4444;
}
</style>
