<template>
  <div class="modern-dashboard">
    <!-- Hero Section with Gradient Animation -->
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-icon">🎴</div>
        <h1 class="hero-title">Pokemon Card Planning System</h1>
        <p class="hero-subtitle">Streamline your grading, certification, and scanning workflow</p>
        <div class="hero-timestamp">
          <span class="timestamp-icon">🕐</span>
          Last updated: {{ currentTime }}
        </div>
      </div>
      <div class="hero-decoration"></div>
    </div>

    <!-- Quick Stats Bar -->
    <div class="quick-stats">
      <div class="stat-item" v-for="stat in quickStats" :key="stat.label">
        <div class="stat-icon">{{ stat.icon }}</div>
        <div class="stat-content">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- Main Content Grid - Two Column Layout -->
    <div class="content-wrapper">
      <!-- Left Column -->
      <div class="content-left">
        <!-- System Status Card -->
        <div class="card card-status">
          <div class="card-header">
            <h3>
              <span class="header-icon">🔧</span>
              System Health
            </h3>
            <div :class="['status-badge', backendConnected ? 'status-online' : 'status-offline']">
              {{ backendConnected ? 'Online' : 'Offline' }}
            </div>
          </div>
          <div class="card-body">
            <div class="connection-status">
              <div class="status-item">
                <div class="status-indicator" :class="{ 'active': backendConnected }"></div>
                <div class="status-info">
                  <div class="status-title">Backend API</div>
                  <div class="status-desc">Spring Boot Service</div>
                </div>
              </div>
              <button @click="checkBackend" class="btn btn-outline" :disabled="checkingBackend">
                <span v-if="!checkingBackend">🔄 Test Connection</span>
                <span v-else>⏳ Testing...</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Priority Distribution Chart -->
        <div class="card card-chart">
          <div class="card-header">
            <h3>
              <span class="header-icon">📊</span>
              Orders by Priority
            </h3>
          </div>
          <div class="card-body">
            <div class="priority-chart">
              <div v-for="priority in priorityData" :key="priority.name" class="priority-bar-container">
                <div class="priority-info">
                  <span class="priority-name">{{ priority.name }}</span>
                  <span class="priority-count">{{ priority.count }}</span>
                </div>
                <div class="priority-bar-bg">
                  <div
                    class="priority-bar"
                    :class="`priority-${priority.level}`"
                    :style="{ width: `${(priority.count / maxPriorityCount) * 100}%` }"
                  ></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Quick Actions Card -->
        <div class="card card-actions card-stretch">
          <div class="card-header">
            <h3>
              <span class="header-icon">⚡</span>
              Quick Actions
            </h3>
          </div>
          <div class="card-body">
            <div class="action-grid">
              <button @click="generatePlanning" class="action-btn primary" :disabled="generatingPlanning">
                <div class="action-icon">🗓️</div>
                <div class="action-text">
                  <div class="action-title">Generate Planning</div>
                  <div class="action-desc">Create work schedule</div>
                </div>
              </button>
              <button @click="refreshStats" class="action-btn secondary" :disabled="loadingStats">
                <div class="action-icon">🔄</div>
                <div class="action-text">
                  <div class="action-title">Refresh Data</div>
                  <div class="action-desc">Update statistics</div>
                </div>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column -->
      <div class="content-right">
        <!-- Symfony Backend Status Card -->
        <div class="card card-symfony">
          <div class="card-header">
            <h3>
              <span class="header-icon">🔗</span>
              Symfony API
            </h3>
            <div :class="['status-badge', symfonyConnected ? 'status-online' : 'status-offline']">
              {{ symfonyConnected ? 'Online' : 'Offline' }}
            </div>
          </div>
          <div class="card-body">
            <div class="connection-status">
              <div class="status-item">
                <div class="status-indicator" :class="{ 'active': symfonyConnected }"></div>
                <div class="status-info">
                  <div class="status-title">Symfony Backend</div>
                  <div class="status-desc">Data Synchronization Service</div>
                </div>
              </div>

              <div class="sync-info" v-if="lastSyncDate">
                <div class="sync-label">Last Sync</div>
                <div class="sync-date">{{ formatSyncDate(lastSyncDate) }}</div>
                <div class="sync-time">{{ formatSyncTime(lastSyncDate) }}</div>
              </div>
              <div class="sync-info" v-else>
                <div class="sync-label">Last Sync</div>
                <div class="sync-never">Never synchronized</div>
              </div>

              <div class="symfony-actions">
                <button @click="checkSymfony" class="btn btn-outline" :disabled="checkingSymfony">
                  <span v-if="!checkingSymfony">🔄 Test</span>
                  <span v-else>⏳ Testing...</span>
                </button>
                <button @click="syncData" class="btn btn-sync" :disabled="syncing || !symfonyConnected">
                  <span v-if="!syncing">🔄 Sync Now</span>
                  <span v-else>⏳ Syncing...</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Activity Card -->
        <div class="card card-activity card-full-height">
          <div class="card-header">
            <h3>
              <span class="header-icon">📋</span>
              Recent Activity
            </h3>
          </div>
          <div class="card-body">
            <div class="activity-list">
              <div v-for="(employee, idx) in employees.slice(0, 5)" :key="employee.id" class="activity-item">
                <div class="activity-avatar">{{ employee.fullName.charAt(0) }}</div>
                <div class="activity-content">
                  <div class="activity-title">{{ employee.fullName }}</div>
                  <div class="activity-meta">
                    {{ employee.workHoursPerDay }}h/day •
                    <span :class="employee.active ? 'text-success' : 'text-muted'">
                      {{ employee.active ? 'Active' : 'Inactive' }}
                    </span>
                  </div>
                </div>
                <div class="activity-badge">{{ employee.email.split('@')[0] }}</div>
              </div>
              <div v-if="employees.length > 5" class="activity-more">
                + {{ employees.length - 5 }} more employees
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Navigation Cards -->
    <div class="nav-section">
      <h2 class="section-title">Navigate to</h2>
      <div class="nav-grid">
        <router-link
          v-for="navItem in navigationItems"
          :key="navItem.id"
          :to="navItem.path"
          class="nav-card"
        >
          <div class="nav-icon">{{ navItem.icon }}</div>
          <div class="nav-content">
            <div class="nav-title">{{ navItem.title }}</div>
            <div class="nav-count">{{ navItem.count }} items</div>
          </div>
          <div class="nav-arrow">→</div>
        </router-link>
      </div>
    </div>

    <!-- System Message Toast -->
    <transition name="slide-up">
      <div v-if="systemMessage" :class="['toast', `toast-${systemMessage.type}`]">
        <div class="toast-icon">{{ systemMessage.type === 'success' ? '✅' : '❌' }}</div>
        <div class="toast-content">
          <div class="toast-title">{{ systemMessage.title }}</div>
          <div class="toast-message">{{ systemMessage.message }}</div>
        </div>
        <button @click="systemMessage = null" class="toast-close">×</button>
      </div>
    </transition>
  </div>
</template>

<script>
export default {
  name: 'ModernDashboard',
  data() {
    return {
      currentTime: new Date().toLocaleString(),
      backendConnected: false,
      checkingBackend: false,
      symfonyConnected: false,
      checkingSymfony: false,
      syncing: false,
      lastSyncDate: null,
      loadingStats: false,
      generatingPlanning: false,
      systemMessage: null,
      stats: {
        ordersCount: 0,
        employeesCount: 0,
        planningCount: 0
      },
      employees: [],
      orders: []
    }
  },
  computed: {
    quickStats() {
      return [
        { icon: '📦', label: 'Total Orders', value: this.stats.ordersCount || 0 },
        { icon: '👥', label: 'Employees', value: this.stats.employeesCount || 0 },
        { icon: '📅', label: 'Active Plans', value: this.stats.planningCount || 0 },
        { icon: '✅', label: 'Completed', value: Math.floor((this.stats.ordersCount || 0) * 0.6) }
      ]
    },
    priorityData() {
      // Mock data - replace with real data from orders
      return [
        { name: 'Express', level: 'urgent', count: 15 },
        { name: 'Fast+', level: 'high', count: 28 },
        { name: 'Fast', level: 'medium', count: 42 },
        { name: 'Classic', level: 'low', count: 35 }
      ]
    },
    maxPriorityCount() {
      return Math.max(...this.priorityData.map(p => p.count))
    },
    navigationItems() {
      return [
        { id: 'orders', icon: '📋', title: 'Orders', path: '/orders', count: this.stats.ordersCount },
        { id: 'employees', icon: '👥', title: 'Employees', path: '/employees', count: this.stats.employeesCount },
        { id: 'planning', icon: '📅', title: 'Planning', path: '/planning', count: this.stats.planningCount },
        { id: 'teams', icon: '🎯', title: 'Teams', path: '/groups', count: 7 }
      ]
    }
  },
  mounted() {
    this.loadAllData()
    // Update time every second
    setInterval(() => {
      this.currentTime = new Date().toLocaleString()
    }, 1000)

    // Check Symfony connection
    this.checkSymfony()
    this.loadLastSyncDate()
  },
  methods: {
    async loadAllData() {
      await Promise.all([
        this.checkBackend(),
        this.loadStats(),
        this.loadEmployees()
      ])
    },
    async checkBackend() {
      this.checkingBackend = true
      try {
        const response = await fetch('http://localhost:8080/actuator/health')
        this.backendConnected = response.ok
      } catch (error) {
        this.backendConnected = false
      } finally {
        this.checkingBackend = false
      }
    },

    async checkSymfony() {
      this.checkingSymfony = true
      try {
        // Try to reach Symfony health endpoint
        const response = await fetch('http://localhost:8000/api/health', {
          method: 'GET',
          headers: { 'Accept': 'application/json' }
        })
        this.symfonyConnected = response.ok
      } catch (error) {
        console.error('Symfony connection error:', error)
        this.symfonyConnected = false
      } finally {
        this.checkingSymfony = false
      }
    },

    async syncData() {
      this.syncing = true
      try {
        const response = await fetch('http://localhost:8080/api/sync/all', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' }
        })

        if (response.ok) {
          this.lastSyncDate = new Date()
          this.saveLastSyncDate()
          this.showMessage('success', 'Sync Complete', 'Data synchronized successfully from Symfony API')
          await this.loadStats()
        } else {
          throw new Error('Sync failed')
        }
      } catch (error) {
        console.error('Sync error:', error)
        this.showMessage('error', 'Sync Failed', 'Failed to synchronize data from Symfony API')
      } finally {
        this.syncing = false
      }
    },

    loadLastSyncDate() {
      const savedDate = localStorage.getItem('lastSyncDate')
      if (savedDate) {
        this.lastSyncDate = new Date(savedDate)
      }
    },

    saveLastSyncDate() {
      if (this.lastSyncDate) {
        localStorage.setItem('lastSyncDate', this.lastSyncDate.toISOString())
      }
    },

    formatSyncDate(date) {
      if (!date) return 'Never'
      const now = new Date()
      const syncDate = new Date(date)
      const diffMs = now - syncDate
      const diffMins = Math.floor(diffMs / 60000)
      const diffHours = Math.floor(diffMs / 3600000)
      const diffDays = Math.floor(diffMs / 86400000)

      if (diffMins < 1) return 'Just now'
      if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`
      if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`
      if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`

      return syncDate.toLocaleDateString()
    },

    formatSyncTime(date) {
      if (!date) return ''
      return new Date(date).toLocaleTimeString()
    },
    async loadStats() {
      this.loadingStats = true
      try {
        const [orders, employees, planning] = await Promise.all([
          fetch('http://localhost:8080/api/orders?page=0&size=1'),
          fetch('http://localhost:8080/api/employees'),
          fetch('http://localhost:8080/api/planning/assignments')
        ])

        const ordersData = await orders.json()
        const employeesData = await employees.json()
        const planningData = await planning.json()

        this.stats = {
          ordersCount: ordersData.totalElements || 0,
          employeesCount: employeesData.length || 0,
          planningCount: planningData.length || 0
        }
      } catch (error) {
        console.error('Error loading stats:', error)
      } finally {
        this.loadingStats = false
      }
    },
    async loadEmployees() {
      try {
        const response = await fetch('http://localhost:8080/api/employees')
        this.employees = await response.json()
      } catch (error) {
        console.error('Error loading employees:', error)
      }
    },
    async refreshStats() {
      await this.loadStats()
      this.showMessage('success', 'Updated', 'Statistics refreshed successfully')
    },
    async generatePlanning() {
      this.generatingPlanning = true
      try {
        const response = await fetch('http://localhost:8080/api/planning/generate', {
          method: 'POST'
        })
        if (response.ok) {
          this.showMessage('success', 'Success', 'Planning generated successfully')
          await this.loadStats()
        } else {
          throw new Error('Failed to generate planning')
        }
      } catch (error) {
        this.showMessage('error', 'Error', 'Failed to generate planning')
      } finally {
        this.generatingPlanning = false
      }
    },
    showMessage(type, title, message) {
      this.systemMessage = { type, title, message }
      if (type === 'success') {
        setTimeout(() => {
          this.systemMessage = null
        }, 5000)
      }
    }
  }
}
</script>

<style scoped>
.modern-dashboard {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 2rem;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* Hero Section */
.hero-section {
  position: relative;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  padding: 3rem;
  margin-bottom: 2rem;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.hero-decoration {
  position: absolute;
  top: -50%;
  right: -10%;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(102, 126, 234, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  animation: pulse 4s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 0.8; }
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.hero-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
}

.hero-title {
  font-size: 2.5rem;
  font-weight: 800;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0 0 0.5rem 0;
}

.hero-subtitle {
  font-size: 1.125rem;
  color: #6b7280;
  margin: 0 0 1.5rem 0;
}

.hero-timestamp {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #f3f4f6;
  padding: 0.5rem 1rem;
  border-radius: 100px;
  font-size: 0.875rem;
  color: #4b5563;
}

.timestamp-icon {
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Quick Stats */
.quick-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.stat-item {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 1.5rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.stat-icon {
  font-size: 2.5rem;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}

.stat-label {
  font-size: 0.875rem;
  color: #6b7280;
  margin-top: 0.25rem;
}

/* Content Wrapper - Two Column Layout */
.content-wrapper {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.content-left {
  display: grid;
  grid-template-rows: auto auto 1fr;
  gap: 1.5rem;
}

.content-right {
  display: grid;
  grid-template-rows: auto 1fr;
  gap: 1.5rem;
}

/* Make Activity Card Fill Remaining Space */
.card-full-height {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.card-full-height .card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: auto;
}

.card-full-height .activity-list {
  flex: 1;
}

/* Card Stretch */
.card-stretch {
  display: flex;
  flex-direction: column;
}

.card-stretch .card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

/* Cards */
.card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.card-header {
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.header-icon {
  font-size: 1.25rem;
}

.card-body {
  padding: 1.5rem;
}

/* Status Badge */
.status-badge {
  padding: 0.375rem 0.75rem;
  border-radius: 100px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-online {
  background: #d1fae5;
  color: #065f46;
}

.status-offline {
  background: #fee2e2;
  color: #991b1b;
}

/* Connection Status */
.connection-status {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #ef4444;
  position: relative;
}

.status-indicator.active {
  background: #10b981;
}

.status-indicator.active::before {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid #10b981;
  animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;
}

@keyframes ping {
  75%, 100% {
    transform: scale(2);
    opacity: 0;
  }
}

.status-info {
  flex: 1;
}

.status-title {
  font-weight: 600;
  color: #1f2937;
}

.status-desc {
  font-size: 0.875rem;
  color: #6b7280;
}

/* Sync Info */
.sync-info {
  padding: 0.75rem;
  background: #f9fafb;
  border-radius: 8px;
  margin-top: 1rem;
}

.sync-label {
  font-size: 0.75rem;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.sync-date {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1f2937;
}

.sync-time {
  font-size: 0.75rem;
  color: #6b7280;
  margin-top: 0.125rem;
}

.sync-never {
  font-size: 0.875rem;
  color: #ef4444;
  font-style: italic;
}

/* Symfony Actions */
.symfony-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}

.symfony-actions .btn {
  flex: 1;
}

.btn-sync {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  padding: 0.625rem 1.25rem;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
}

.btn-sync:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  transform: translateY(-1px);
}

.btn-sync:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Priority Chart */
.priority-chart {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.priority-bar-container {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.priority-info {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
}

.priority-name {
  font-weight: 600;
  color: #1f2937;
}

.priority-count {
  font-weight: 700;
  color: #6b7280;
}

.priority-bar-bg {
  height: 8px;
  background: #f3f4f6;
  border-radius: 100px;
  overflow: hidden;
}

.priority-bar {
  height: 100%;
  border-radius: 100px;
  transition: width 0.5s ease;
}

.priority-urgent { background: linear-gradient(90deg, #ef4444, #dc2626); }
.priority-high { background: linear-gradient(90deg, #f59e0b, #d97706); }
.priority-medium { background: linear-gradient(90deg, #10b981, #059669); }
.priority-low { background: linear-gradient(90deg, #3b82f6, #2563eb); }

/* Action Grid */
.action-grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  border: 2px solid transparent;
  border-radius: 12px;
  background: #f9fafb;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.action-btn:hover:not(:disabled) {
  transform: translateX(4px);
  border-color: #667eea;
  background: #f3f4f6;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.action-btn.primary:hover:not(:disabled) {
  border-color: transparent;
  box-shadow: 0 8px 16px rgba(102, 126, 234, 0.3);
}

.action-icon {
  font-size: 2rem;
}

.action-text {
  flex: 1;
}

.action-title {
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.action-desc {
  font-size: 0.875rem;
  opacity: 0.8;
}

/* Activity List */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  background: #f9fafb;
  border-radius: 12px;
  transition: background 0.2s;
}

.activity-item:hover {
  background: #f3f4f6;
}

.activity-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.125rem;
}

.activity-content {
  flex: 1;
}

.activity-title {
  font-weight: 600;
  color: #1f2937;
}

.activity-meta {
  font-size: 0.875rem;
  color: #6b7280;
}

.activity-badge {
  padding: 0.25rem 0.75rem;
  background: #e5e7eb;
  border-radius: 100px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #4b5563;
}

.activity-more {
  text-align: center;
  padding: 0.75rem;
  color: #6b7280;
  font-size: 0.875rem;
  font-style: italic;
}

/* Navigation Section */
.nav-section {
  margin-bottom: 2rem;
}

.section-title {
  color: white;
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 1rem 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.nav-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  text-decoration: none;
  color: inherit;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.nav-card:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.2);
}

.nav-icon {
  font-size: 2.5rem;
}

.nav-content {
  flex: 1;
}

.nav-title {
  font-weight: 700;
  font-size: 1.125rem;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.nav-count {
  font-size: 0.875rem;
  color: #6b7280;
}

.nav-arrow {
  font-size: 1.5rem;
  color: #9ca3af;
  transition: transform 0.2s;
}

.nav-card:hover .nav-arrow {
  transform: translateX(4px);
  color: #667eea;
}

/* Buttons */
.btn {
  padding: 0.625rem 1.25rem;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-outline {
  background: transparent;
  border: 2px solid #667eea;
  color: #667eea;
}

.btn-outline:hover:not(:disabled) {
  background: #667eea;
  color: white;
}

/* Toast Notifications */
.toast {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  max-width: 400px;
  background: white;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  z-index: 1000;
}

.toast-success {
  border-left: 4px solid #10b981;
}

.toast-error {
  border-left: 4px solid #ef4444;
}

.toast-icon {
  font-size: 1.5rem;
}

.toast-content {
  flex: 1;
}

.toast-title {
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.25rem;
}

.toast-message {
  font-size: 0.875rem;
  color: #6b7280;
}

.toast-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #9ca3af;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.toast-close:hover {
  color: #4b5563;
}

/* Transitions */
.slide-up-enter-active, .slide-up-leave-active {
  transition: all 0.3s ease;
}

.slide-up-enter-from {
  transform: translateY(100%);
  opacity: 0;
}

.slide-up-leave-to {
  transform: translateY(20px);
  opacity: 0;
}

/* Utility Classes */
.text-success {
  color: #10b981;
}

.text-muted {
  color: #6b7280;
}

/* Responsive */
@media (max-width: 768px) {
  .modern-dashboard {
    padding: 1rem;
  }

  .hero-section {
    padding: 2rem 1.5rem;
  }

  .hero-title {
    font-size: 1.75rem;
  }

  .quick-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-wrapper {
    grid-template-columns: 1fr;
  }

  .nav-grid {
    grid-template-columns: 1fr;
  }

  .toast {
    left: 1rem;
    right: 1rem;
    bottom: 1rem;
  }
}
</style>
