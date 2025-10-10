<template>
  <div class="data-sync-container">
    <div class="sync-header">
      <h2>🔄 Data Synchronization</h2>
      <p class="subtitle">Sync data from Symfony backend (dev) to Planning system (dev-planning)</p>
    </div>

    <!-- Sync Status Card -->
    <div class="card status-card">
      <h3>📊 Sync Status</h3>
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
import { ref } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// State
const syncing = ref(false)
const loadingStatus = ref(false)
const syncStatus = ref<any>(null)
const syncHistory = ref<any[]>([])
const notification = ref<any>(null)

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

// Sync all data
const syncAll = async () => {
  if (!confirm('⚠️ This will replace ALL data in dev-planning with data from dev. Continue?')) {
    return
  }

  syncing.value = true

  try {
    const response = await fetch(`${API_BASE_URL}/api/sync/all`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })

    if (response.ok) {
      const result = await response.json()
      showNotification('✅ Full synchronization completed!', 'success')
      addToHistory(result.message, true)

      // Refresh status
      await checkSyncStatus()
    } else {
      throw new Error(`HTTP ${response.status}`)
    }
  } catch (error) {
    console.error('Error syncing all data:', error)
    showNotification('❌ Synchronization failed', 'error')
    addToHistory('Full sync failed: ' + error, false)
  } finally {
    syncing.value = false
  }
}

// Sync orders only
const syncOrders = async () => {
  if (!confirm('⚠️ This will replace order data in dev-planning. Continue?')) {
    return
  }

  syncing.value = true

  try {
    const response = await fetch(`${API_BASE_URL}/api/sync/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })

    if (response.ok) {
      const result = await response.json()
      showNotification('✅ Orders synchronized!', 'success')
      addToHistory(result.message, true)
      await checkSyncStatus()
    } else {
      throw new Error(`HTTP ${response.status}`)
    }
  } catch (error) {
    console.error('Error syncing orders:', error)
    showNotification('❌ Orders sync failed', 'error')
    addToHistory('Orders sync failed: ' + error, false)
  } finally {
    syncing.value = false
  }
}

// Sync cards only
const syncCards = async () => {
  if (!confirm('⚠️ This will replace card data in dev-planning. Continue?')) {
    return
  }

  syncing.value = true

  try {
    const response = await fetch(`${API_BASE_URL}/api/sync/cards`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })

    if (response.ok) {
      const result = await response.json()
      showNotification('✅ Cards synchronized!', 'success')
      addToHistory(result.message, true)
      await checkSyncStatus()
    } else {
      throw new Error(`HTTP ${response.status}`)
    }
  } catch (error) {
    console.error('Error syncing cards:', error)
    showNotification('❌ Cards sync failed', 'error')
    addToHistory('Cards sync failed: ' + error, false)
  } finally {
    syncing.value = false
  }
}

// Incremental sync
const syncIncremental = async () => {
  syncing.value = true

  try {
    const response = await fetch(`${API_BASE_URL}/api/sync/incremental`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })

    if (response.ok) {
      const result = await response.json()
      showNotification('✅ Incremental sync completed!', 'success')
      addToHistory(`Incremental sync: ${result.recordsUpdated} records updated`, true)
      await checkSyncStatus()
    } else {
      throw new Error(`HTTP ${response.status}`)
    }
  } catch (error) {
    console.error('Error in incremental sync:', error)
    showNotification('❌ Incremental sync failed', 'error')
    addToHistory('Incremental sync failed: ' + error, false)
  } finally {
    syncing.value = false
  }
}

// Helper functions
const showNotification = (message: string, type: 'success' | 'error' | 'info') => {
  notification.value = { message, type }
  setTimeout(() => {
    notification.value = null
  }, 5000)
}

const addToHistory = (message: string, success: boolean) => {
  syncHistory.value.unshift({
    timestamp: new Date(),
    message,
    success
  })

  // Keep only last 10 entries
  if (syncHistory.value.length > 10) {
    syncHistory.value = syncHistory.value.slice(0, 10)
  }
}

const formatTime = (timestamp: Date | string) => {
  const date = new Date(timestamp)
  return date.toLocaleString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    month: 'short',
    day: 'numeric'
  })
}

// Auto-check status on mount
checkSyncStatus()
</script>

<style scoped>
.data-sync-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.sync-header {
  text-align: center;
  margin-bottom: 30px;
}

.sync-header h2 {
  font-size: 2em;
  margin-bottom: 10px;
}

.subtitle {
  color: #666;
  font-size: 1em;
}

.card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.status-card h3,
.action-card h3,
.history-card h3 {
  margin-top: 0;
  margin-bottom: 15px;
}

.status-table {
  margin-top: 20px;
  overflow-x: auto;
}

.status-table table {
  width: 100%;
  border-collapse: collapse;
}

.status-table th,
.status-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.status-table th {
  background: #f8f9fa;
  font-weight: 600;
}

.text-danger {
  color: #dc3545;
  font-weight: 600;
}

.text-muted {
  color: #6c757d;
  font-size: 0.9em;
}

.mt-2 {
  margin-top: 0.5rem;
}

.badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.85em;
  font-weight: 600;
}

.badge-success {
  background: #d4edda;
  color: #155724;
}

.badge-warning {
  background: #fff3cd;
  color: #856404;
}

.badge-danger {
  background: #f8d7da;
  color: #721c24;
}

.sync-actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.action-card {
  text-align: center;
}

.action-card p {
  color: #666;
  margin-bottom: 15px;
  min-height: 40px;
}

.btn {
  padding: 12px 24px;
  border: none;
  border-radius: 6px;
  font-size: 1em;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  width: 100%;
  max-width: 250px;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #0056b3;
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover:not(:disabled) {
  background: #545b62;
}

.btn-info {
  background: #17a2b8;
  color: white;
}

.btn-info:hover:not(:disabled) {
  background: #138496;
}

.btn-success {
  background: #28a745;
  color: white;
}

.btn-success:hover:not(:disabled) {
  background: #218838;
}

.history-card {
  margin-top: 30px;
}

.history-list {
  max-height: 400px;
  overflow-y: auto;
}

.history-entry {
  display: flex;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #eee;
  gap: 12px;
}

.history-entry.success {
  background: #f8fff9;
}

.history-entry.error {
  background: #fff8f8;
}

.history-entry .time {
  font-size: 0.85em;
  color: #666;
  min-width: 120px;
}

.history-entry .message {
  flex: 1;
}

.notification {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 15px 25px;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 1000;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    transform: translateX(400px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.notification.success {
  background: #d4edda;
  color: #155724;
  border-left: 4px solid #28a745;
}

.notification.error {
  background: #f8d7da;
  color: #721c24;
  border-left: 4px solid #dc3545;
}

.notification.info {
  background: #d1ecf1;
  color: #0c5460;
  border-left: 4px solid #17a2b8;
}
</style>
