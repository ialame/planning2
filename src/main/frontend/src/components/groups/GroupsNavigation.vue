<template>
  <div class="teams-navigation">
    <!-- Navigation Header -->
    <div class="bg-white shadow-sm border-b border-gray-200">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center py-4">
          <!-- Breadcrumb -->
          <nav class="flex items-center space-x-2 text-sm">
            <button
              @click="currentView = 'overview'"
              :class="[
                'px-3 py-1 rounded-lg transition-colors',
                currentView === 'overview'
                  ? 'bg-blue-100 text-blue-700 font-medium'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
              ]"
            >
              📊 Overview
            </button>
            <span class="text-gray-400">/</span>
            <button
              @click="currentView = 'teams'"
              :class="[
                'px-3 py-1 rounded-lg transition-colors',
                currentView === 'teams'
                  ? 'bg-blue-100 text-blue-700 font-medium'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
              ]"
            >
              👥 Groups
            </button>
            <span class="text-gray-400">/</span>
            <button
              @click="currentView = 'employees'"
              :class="[
                'px-3 py-1 rounded-lg transition-colors',
                currentView === 'employees'
                  ? 'bg-blue-100 text-blue-700 font-medium'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
              ]"
            >
              👤 Employees
            </button>
          </nav>

          <!-- Quick Actions -->
          <div class="flex items-center space-x-3">
            <div class="text-sm text-gray-600">
              Last updated: {{ lastUpdated }}
            </div>
            <button
              @click="refreshData"
              :disabled="loading"
              class="p-2 text-gray-400 hover:text-gray-600 transition-colors"
              :class="{ 'animate-spin': loading }"
            >
              <RefreshCw class="w-4 h-4" />
            </button>
            <div class="h-4 w-px bg-gray-300"></div>
            <button
              v-if="currentView === 'teams'"
              @click="showCreateGroup = true"
              class="btn-primary text-sm"
            >
              <Plus class="w-4 h-4 mr-2" />
              New Group
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Content Area -->
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
      <!-- Overview -->
      <div v-if="currentView === 'overview'">
        <GroupOverview
          :stats="stats"
          @view-teams="currentView = 'teams'"
          @view-employees="currentView = 'employees'"
        />
      </div>

      <!-- Groups Management -->
      <div v-else-if="currentView === 'teams'">
        <GroupManagement @updated="refreshData" />
      </div>

      <!-- Employees with Groups -->
      <div v-else-if="currentView === 'employees'">
        <EmployeesWithGroups @updated="refreshData" />
      </div>
    </div>

    <!-- Create Team Modal -->
    <CreateGroupModal
      v-if="showCreateGroup"
      @close="showCreateGroup = false"
      @created="onGroupCreated"
      :permission-levels="permissionLevels"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { RefreshCw, Plus } from 'lucide-vue-next'
import GroupManagement from './GroupManagement.vue'
import GroupOverview from './GroupOverview.vue'
import EmployeesWithGroups from './EmployeesWithGroups.vue'
import CreateGroupModal from './CreateGroupModal.vue'

import { API_BASE_URL } from '@/config/api.ts'

// ========== INTERFACES ==========
interface GroupStats {
  totalGroups: number
  adminGroups: number
  managerGroups: number
  totalMembers: number
  emptyGroups: number
  averagePermissionLevel: number
}

interface PermissionLevelInfo {
  level: number
  name: string
  description: string
  colorCode: string
}

// ========== STATE ==========
const currentView = ref<'overview' | 'teams' | 'employees'>('overview')
const loading = ref(false)
const lastUpdated = ref('')
const showCreateGroup = ref(false)
const stats = ref<GroupStats>({
  totalGroups: 0,
  adminGroups: 0,
  managerGroups: 0,
  totalMembers: 0,
  emptyGroups: 0,
  averagePermissionLevel: 0
})

// ========== PERMISSION LEVELS ==========
const permissionLevels: PermissionLevelInfo[] = [
  { level: 10, name: 'SUPER_ADMIN', description: 'Super administrator with unrestricted access', colorCode: '#6f42c1' },
  { level: 9, name: 'ADMIN', description: 'System administrator with full management rights', colorCode: '#dc3545' },
  { level: 8, name: 'SENIOR_ADMIN', description: 'Senior administrator with most privileges', colorCode: '#fd7e14' },
  { level: 7, name: 'MANAGER', description: 'Team manager with planning and oversight privileges', colorCode: '#ffc107' },
  { level: 6, name: 'SENIOR_SUPERVISOR', description: 'Senior supervisor with extended monitoring rights', colorCode: '#20c997' },
  { level: 5, name: 'SUPERVISOR', description: 'Supervisor with monitoring and basic management', colorCode: '#28a745' },
  { level: 4, name: 'SENIOR_PROCESSOR', description: 'Senior processor with advanced processing rights', colorCode: '#17a2b8' },
  { level: 3, name: 'PROCESSOR', description: 'Card processor with standard processing access', colorCode: '#007bff' },
  { level: 2, name: 'JUNIOR_PROCESSOR', description: 'Junior processor with limited processing access', colorCode: '#6c757d' },
  { level: 1, name: 'VIEWER', description: 'Read-only access for viewing and reports', colorCode: '#868e96' }
]

// ========== METHODS ==========
const loadStats = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/statistics`)
    if (response.ok) {
      const data = await response.json()

      stats.value = {
        totalGroups: data.totalGroups || 0,
        adminGroups: data.groupStatistics?.filter((g: any) => g.permissionLevel >= 8).length || 0,
        managerGroups: data.groupStatistics?.filter((g: any) => g.permissionLevel >= 5 && g.permissionLevel < 8).length || 0,
        totalMembers: data.totalEmployeesInGroups || 0,
        emptyGroups: data.emptyGroupsCount || 0,
        averagePermissionLevel: calculateAveragePermissionLevel(data.groupStatistics || [])
      }

      lastUpdated.value = new Date().toLocaleTimeString()
    }
  } catch (error) {
    console.error('Error loading stats:', error)
  } finally {
    loading.value = false
  }
}

const calculateAveragePermissionLevel = (groupStats: any[]) => {
  if (groupStats.length === 0) return 0

  const totalWeightedLevel = groupStats.reduce((sum, team) => {
    return sum + (team.permissionLevel * team.employeeCount)
  }, 0)

  const totalEmployees = groupStats.reduce((sum, team) => sum + team.employeeCount, 0)

  return totalEmployees > 0 ? Math.round((totalWeightedLevel / totalEmployees) * 10) / 10 : 0
}

const refreshData = async () => {
  await loadStats()
}

const onGroupCreated = () => {
  showCreateGroup.value = false
  refreshData()
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.btn-primary {
  @apply bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors flex items-center;
}
</style>
