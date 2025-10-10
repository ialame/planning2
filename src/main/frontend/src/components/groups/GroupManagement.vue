<template>
  <div class="group-management-page">
    <!-- Header -->
    <div class="mb-8">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">👥 Group Management</h1>
          <p class="text-gray-600 mt-1">Manage roles and permissions for your team</p>
        </div>
        <div class="flex space-x-3">
          <button
            @click="initializeDefaultGroups"
            class="btn-secondary"
            :disabled="loading"
          >
            🔧 Initialize Default Groups
          </button>
          <button
            @click="showCreateForm = true"
            class="btn-primary"
          >
            <Plus class="w-4 h-4 mr-2" />
            Create Group
          </button>
        </div>
      </div>
    </div>

    <!-- Notification -->
    <div v-if="notification" class="mb-6">
      <div :class="[
        'p-4 rounded-lg border',
        notification.type === 'success' ? 'bg-green-50 border-green-200 text-green-800' :
        notification.type === 'error' ? 'bg-red-50 border-red-200 text-red-800' :
        'bg-blue-50 border-blue-200 text-blue-800'
      ]">
        <div class="flex items-center">
          <CheckCircle v-if="notification.type === 'success'" class="w-5 h-5 mr-2" />
          <XCircle v-else-if="notification.type === 'error'" class="w-5 h-5 mr-2" />
          <span>{{ notification.message }}</span>
        </div>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
      <div class="card">
        <div class="flex items-center">
          <Users class="w-8 h-8 text-blue-600 mr-3" />
          <div>
            <p class="text-sm text-gray-600">Total Groups</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.totalGroups }}</p>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="flex items-center">
          <Crown class="w-8 h-8 text-red-600 mr-3" />
          <div>
            <p class="text-sm text-gray-600">Admin Groups</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.adminGroups }}</p>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="flex items-center">
          <Shield class="w-8 h-8 text-yellow-600 mr-3" />
          <div>
            <p class="text-sm text-gray-600">Manager Groups</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.managerGroups }}</p>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="flex items-center">
          <UserPlus class="w-8 h-8 text-green-600 mr-3" />
          <div>
            <p class="text-sm text-gray-600">Total Members</p>
            <p class="text-2xl font-bold text-gray-900">{{ stats.totalMembers }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="card mb-6">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="flex-1">
          <div class="relative">
            <Search class="w-5 h-5 absolute left-3 top-3 text-gray-400" />
            <input
              v-model="searchTerm"
              type="text"
              placeholder="Search groups..."
              class="input-field pl-10"
            >
          </div>
        </div>
        <div class="flex gap-2">
          <select
            v-model="permissionFilter"
            class="input-field min-w-[200px]"
          >
            <option :value="null">All Permission Levels</option>
            <option v-for="level in permissionLevels" :key="level.level" :value="level.level">
              {{ level.name }} ({{ level.level }})
            </option>
          </select>
          <button
            @click="clearFilters"
            class="btn-secondary"
          >
            <Filter class="w-4 h-4 mr-2" />
            Clear
          </button>
        </div>
      </div>
    </div>

    <!-- Groups Grid -->
    <div v-if="!loading && filteredGroups.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div
        v-for="group in filteredGroups"
        :key="group.id"
        class="card hover:shadow-lg transition-shadow cursor-pointer"
        @click="selectGroup(group)"
      >
        <!-- Group Header -->
        <div class="flex justify-between items-start mb-4">
          <div class="flex items-center space-x-3">
            <div :class="[
              'w-12 h-12 rounded-full flex items-center justify-center text-white font-bold text-lg',
              getPermissionLevelColor(group.permissionLevel)
            ]">
              {{ getGroupIcon(group.permissionLevel) }}
            </div>
            <div>
              <h3 class="font-semibold text-gray-900">{{ group.name }}</h3>
              <p class="text-sm text-gray-600 truncate">{{ group.description }}</p>
            </div>
          </div>
          <div class="flex space-x-1">
            <button
              @click.stop="editGroup(group)"
              class="p-1 text-gray-400 hover:text-blue-600"
            >
              <Edit3 class="w-4 h-4" />
            </button>
            <button
              @click.stop="deleteGroup(group.id)"
              class="p-1 text-gray-400 hover:text-red-600"
            >
              <Trash2 class="w-4 h-4" />
            </button>
          </div>
        </div>

        <!-- Permission Level Badge -->
        <div class="mb-3">
          <span :class="[
            'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium',
            getPermissionBadgeColor(group.permissionLevel)
          ]">
            <Shield class="w-3 h-3 mr-1" />
            Level {{ group.permissionLevel }} - {{ getPermissionInfo(group.permissionLevel).name }}
          </span>
        </div>

        <!-- Employee Count -->
        <div class="flex justify-between items-center mb-4">
          <span class="text-sm text-gray-600">Members:</span>
          <span class="font-medium text-gray-900">{{ group.employeeCount }}</span>
        </div>

        <!-- Status -->
        <div class="flex justify-between items-center">
          <span :class="[
            'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
            group.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
          ]">
            {{ group.active ? '✅ Active' : '❌ Inactive' }}
          </span>
          <button
            @click.stop="manageGroupEmployees(group)"
            class="text-blue-600 hover:text-blue-800 text-sm font-medium"
          >
            Manage Members →
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!loading && filteredGroups.length === 0" class="card text-center py-12">
      <Users class="w-16 h-16 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-medium text-gray-900 mb-2">No groups found</h3>
      <p class="text-gray-600 mb-4">
        {{ searchTerm || permissionFilter ? 'Try adjusting your filters' : 'Create your first group to get started' }}
      </p>
      <button
        v-if="!searchTerm && !permissionFilter"
        @click="showCreateForm = true"
        class="btn-primary"
      >
        + Create First Group
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="card text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
      <p class="text-gray-600">Loading groups...</p>
    </div>

    <!-- Create Group Modal -->
    <CreateGroupModal
      v-if="showCreateForm"
      @close="showCreateForm = false"
      @created="onGroupCreated"
      :permission-levels="permissionLevels"
    />

    <!-- Group Detail Modal -->
    <GroupDetailModal
      v-if="selectedGroup"
      :group="selectedGroup"
      @close="selectedGroup = null"
      @updated="onGroupUpdated"
      @deleted="onGroupDeleted"
    />

    <!-- Employee Assignment Modal -->
    <EmployeeAssignmentModal
      v-if="showEmployeeAssignment"
      :group="selectedGroupForAssignment"
      :employees="employees"
      @close="showEmployeeAssignment = false"
      @updated="onAssignmentUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  Users,
  Plus,
  Edit3,
  Trash2,
  Shield,
  Search,
  Filter,
  UserPlus,
  Crown,
  CheckCircle,
  XCircle
} from 'lucide-vue-next'
import CreateGroupModal from './CreateGroupModal.vue'
import GroupDetailModal from './GroupDetailModal.vue'
import EmployeeAssignmentModal from './EmployeeAssignmentModal.vue'

import { API_BASE_URL } from '@/config/api.ts'

// ========== INTERFACES ==========
interface Group {
  id: string
  name: string
  description: string
  permissionLevel: number
  active: boolean
  employeeCount: number
  creationDate: string
  modificationDate: string
}

interface Employee {
  id: string
  firstName: string
  lastName: string
  fullName: string
  email: string
  active: boolean
  workHoursPerDay: number
  efficiencyRating: number
}

interface PermissionLevelInfo {
  level: number
  name: string
  description: string
  colorCode: string
}

interface Notification {
  message: string
  type: 'success' | 'error' | 'info'
}

// ========== STATE ==========
const groups = ref<Group[]>([])
const employees = ref<Employee[]>([])
const selectedGroup = ref<Group | null>(null)
const selectedGroupForAssignment = ref<Group | null>(null)
const loading = ref(false)
const searchTerm = ref('')
const permissionFilter = ref<number | null>(null)
const showCreateForm = ref(false)
const showEmployeeAssignment = ref(false)
const notification = ref<Notification | null>(null)

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

// ========== COMPUTED ==========
const stats = computed(() => ({
  totalGroups: groups.value.length,
  adminGroups: groups.value.filter(g => g.permissionLevel >= 8).length,
  managerGroups: groups.value.filter(g => g.permissionLevel >= 5 && g.permissionLevel < 8).length,
  totalMembers: groups.value.reduce((sum, g) => sum + g.employeeCount, 0)
}))

const filteredGroups = computed(() => {
  let filtered = groups.value

  if (searchTerm.value) {
    const search = searchTerm.value.toLowerCase()
    filtered = filtered.filter(group =>
      group.name.toLowerCase().includes(search) ||
      group.description.toLowerCase().includes(search)
    )
  }

  if (permissionFilter.value !== null) {
    filtered = filtered.filter(group => group.permissionLevel === permissionFilter.value)
  }

  return filtered.sort((a, b) => b.permissionLevel - a.permissionLevel)
})

// ========== METHODS ==========
const loadGroups = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/groups?includeEmployeeCount=true`)
    if (response.ok) {
      const data = await response.json()
      groups.value = data.groups || []
    }
  } catch (error) {
    showNotification('Error loading groups', 'error')
  } finally {
    loading.value = false
  }
}

const loadEmployees = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/employees`)
    if (response.ok) {
      const data = await response.json()
      employees.value = data.employees || []
    }
  } catch (error) {
    showNotification('Error loading employees', 'error')
  }
}

const initializeDefaultGroups = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/groups/init-defaults`, {
      method: 'POST'
    })

    if (response.ok) {
      showNotification('Default groups initialized successfully!', 'success')
      await loadGroups()
    }
  } catch (error) {
    showNotification('Error initializing default groups', 'error')
  }
}

const deleteGroup = async (groupId: string) => {
  if (!confirm('Are you sure you want to delete this group?')) return

  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/groups/${groupId}`, {
      method: 'DELETE'
    })

    if (response.ok) {
      showNotification('Group deleted successfully!', 'success')
      await loadGroups()
    } else {
      showNotification('Error deleting group', 'error')
    }
  } catch (error) {
    showNotification('Error deleting group', 'error')
  }
}

const selectGroup = (group: Group) => {
  selectedGroup.value = group
}

const editGroup = (group: Group) => {
  selectedGroup.value = group
}

const manageGroupEmployees = (group: Group) => {
  selectedGroupForAssignment.value = group
  showEmployeeAssignment.value = true
}

const clearFilters = () => {
  searchTerm.value = ''
  permissionFilter.value = null
}

const showNotification = (message: string, type: 'success' | 'error' | 'info') => {
  notification.value = { message, type }
  setTimeout(() => {
    notification.value = null
  }, 5000)
}

// ========== UTILITY FUNCTIONS ==========
const getPermissionInfo = (level: number): PermissionLevelInfo => {
  return permissionLevels.find(p => p.level === level) || permissionLevels[permissionLevels.length - 1]
}

const getPermissionBadgeColor = (level: number) => {
  if (level >= 8) return 'bg-red-100 text-red-800'
  if (level >= 5) return 'bg-yellow-100 text-yellow-800'
  if (level >= 3) return 'bg-blue-100 text-blue-800'
  return 'bg-gray-100 text-gray-800'
}

const getPermissionLevelColor = (level: number) => {
  if (level >= 8) return 'bg-red-500'
  if (level >= 5) return 'bg-yellow-500'
  if (level >= 3) return 'bg-blue-500'
  return 'bg-gray-500'
}

const getGroupIcon = (level: number) => {
  if (level >= 8) return '👑'
  if (level >= 5) return '🛡️'
  if (level >= 3) return '⚙️'
  return '👀'
}

// ========== EVENT HANDLERS ==========
const onGroupCreated = () => {
  showCreateForm.value = false
  loadGroups()
}

const onGroupUpdated = () => {
  selectedGroup.value = null
  loadGroups()
}

const onGroupDeleted = () => {
  selectedGroup.value = null
  loadGroups()
}

const onAssignmentUpdated = () => {
  showEmployeeAssignment.value = false
  selectedGroupForAssignment.value = null
  loadGroups()
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadGroups()
  loadEmployees()
})
</script>

<style scoped>
.card {
  @apply bg-white rounded-lg shadow-sm border p-6;
}

.btn-primary {
  @apply bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors flex items-center;
}

.btn-secondary {
  @apply bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400 transition-colors flex items-center;
}

.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500;
}

.transition-shadow {
  transition: box-shadow 0.2s ease;
}
</style>
