<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[90vh] overflow-y-auto">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-200">
        <div class="flex justify-between items-center">
          <div class="flex items-center space-x-3">
            <div :class="[
              'w-12 h-12 rounded-full flex items-center justify-center text-white font-bold text-lg',
              getPermissionLevelColor(team.permissionLevel)
            ]">
              {{ getGroupIcon(team.permissionLevel) }}
            </div>
            <div>
              <h2 class="text-lg font-semibold text-gray-900">{{ team.name }}</h2>
              <p class="text-sm text-gray-600">{{ team.description }}</p>
            </div>
          </div>
          <button
            @click="$emit('close')"
            class="text-gray-400 hover:text-gray-600"
          >
            <X class="w-6 h-6" />
          </button>
        </div>
      </div>

      <!-- Tabs -->
      <div class="border-b border-gray-200">
        <nav class="flex">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            :class="[
              'px-6 py-3 text-sm font-medium border-b-2 transition-colors',
              activeTab === tab.id
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            ]"
          >
            <component :is="tab.icon" class="w-4 h-4 mr-2 inline" />
            {{ tab.label }}
          </button>
        </nav>
      </div>

      <!-- Content -->
      <div class="p-6">
        <!-- Overview Tab -->
        <div v-if="activeTab === 'overview'" class="space-y-6">
          <!-- Basic Info -->
          <div class="grid grid-cols-2 gap-6">
            <div class="space-y-4">
              <div>
                <h3 class="text-sm font-medium text-gray-700 mb-2">Group Information</h3>
                <div class="space-y-2">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Name:</span>
                    <span class="text-sm font-medium">{{ team.name }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Status:</span>
                    <span :class="[
                      'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
                      team.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                    ]">
                      {{ team.active ? '✅ Active' : '❌ Inactive' }}
                    </span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Members:</span>
                    <span class="text-sm font-medium">{{ groupMembers.length }}</span>
                  </div>
                </div>
              </div>

              <div>
                <h3 class="text-sm font-medium text-gray-700 mb-2">Permissions</h3>
                <div class="space-y-2">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Level:</span>
                    <span :class="[
                      'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
                      getPermissionBadgeColor(team.permissionLevel)
                    ]">
                      <Shield class="w-3 h-3 mr-1" />
                      {{ team.permissionLevel }} - {{ getPermissionInfo(team.permissionLevel).name }}
                    </span>
                  </div>
                  <div class="text-sm text-gray-600">
                    {{ getPermissionInfo(team.permissionLevel).description }}
                  </div>
                </div>
              </div>
            </div>

            <div class="space-y-4">
              <div>
                <h3 class="text-sm font-medium text-gray-700 mb-2">Timestamps</h3>
                <div class="space-y-2">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Created:</span>
                    <span class="text-sm">{{ formatDate(team.creationDate) }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Modified:</span>
                    <span class="text-sm">{{ formatDate(team.modificationDate) }}</span>
                  </div>
                </div>
              </div>

              <div>
                <h3 class="text-sm font-medium text-gray-700 mb-2">Statistics</h3>
                <div class="space-y-2">
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Active Members:</span>
                    <span class="text-sm font-medium">{{ activeMembers }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Avg. Efficiency:</span>
                    <span class="text-sm font-medium">{{ averageEfficiency }}x</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-sm text-gray-600">Total Work Hours:</span>
                    <span class="text-sm font-medium">{{ totalWorkHours }}h/day</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Members Preview -->
          <div>
            <div class="flex justify-between items-center mb-3">
              <h3 class="text-sm font-medium text-gray-700">Recent Members</h3>
              <button
                @click="activeTab = 'members'"
                class="text-blue-600 hover:text-blue-800 text-sm"
              >
                View all {{ groupMembers.length }} →
              </button>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div
                v-for="member in groupMembers.slice(0, 4)"
                :key="member.id"
                class="flex items-center space-x-2 p-2 bg-gray-50 rounded"
              >
                <div class="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center text-white text-sm font-medium">
                  {{ getEmployeeInitials(member) }}
                </div>
                <div class="flex-1 min-w-0">
                  <p class="text-sm font-medium text-gray-900 truncate">{{ member.fullName }}</p>
                  <p class="text-xs text-gray-600 truncate">{{ member.email }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Members Tab -->
        <div v-else-if="activeTab === 'members'" class="space-y-4">
          <div class="flex justify-between items-center">
            <h3 class="text-lg font-medium text-gray-900">Group Members ({{ groupMembers.length }})</h3>
            <button
              @click="openEmployeeAssignment"
              class="btn-primary text-sm"
            >
              <UserPlus class="w-4 h-4 mr-2" />
              Manage Members
            </button>
          </div>

          <div v-if="groupMembers.length === 0" class="text-center py-8">
            <Users class="w-12 h-12 text-gray-400 mx-auto mb-2" />
            <p class="text-gray-500 mb-4">No members assigned to this team</p>
            <button
              @click="openEmployeeAssignment"
              class="btn-primary"
            >
              Add First Member
            </button>
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="member in groupMembers"
              :key="member.id"
              class="flex items-center justify-between p-4 border border-gray-200 rounded-lg"
            >
              <div class="flex items-center space-x-3">
                <div class="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center text-white font-medium">
                  {{ getEmployeeInitials(member) }}
                </div>
                <div>
                  <p class="font-medium text-gray-900">{{ member.fullName }}</p>
                  <p class="text-sm text-gray-600">{{ member.email }}</p>
                  <div class="flex items-center space-x-2 mt-1">
                    <span class="text-xs bg-gray-100 text-gray-800 px-2 py-1 rounded">
                      {{ member.workHoursPerDay }}h/day
                    </span>
                    <span class="text-xs bg-green-100 text-green-800 px-2 py-1 rounded">
                      {{ member.efficiencyRating }}x efficiency
                    </span>
                    <span :class="[
                      'text-xs px-2 py-1 rounded',
                      member.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                    ]">
                      {{ member.active ? 'Active' : 'Inactive' }}
                    </span>
                  </div>
                </div>
              </div>
              <button
                @click="removeMemberFromGroup(member)"
                class="text-red-600 hover:text-red-800 p-2"
              >
                <UserMinus class="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>

        <!-- Settings Tab -->
        <div v-else-if="activeTab === 'settings'" class="space-y-6">
          <form @submit.prevent="updateGroup">
            <!-- Description -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                v-model="editForm.description"
                rows="3"
                class="input-field resize-none"
                maxlength="255"
              ></textarea>
              <p class="text-gray-500 text-sm mt-1">{{ editForm.description.length }}/255 characters</p>
            </div>

            <!-- Permission Level -->
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                Permission Level
              </label>
              <select
                v-model="editForm.permissionLevel"
                class="input-field"
              >
                <option v-for="level in permissionLevels" :key="level.level" :value="level.level">
                  Level {{ level.level }} - {{ level.name }}
                </option>
              </select>
              <p class="text-sm text-gray-600 mt-1">
                {{ getPermissionInfo(editForm.permissionLevel).description }}
              </p>
            </div>

            <!-- Active Status -->
            <div class="mb-6">
              <label class="flex items-center">
                <input
                  v-model="editForm.active"
                  type="checkbox"
                  class="h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                >
                <span class="ml-2 text-sm text-gray-700">Group is active</span>
              </label>
              <p class="text-xs text-gray-500 mt-1">
                Inactive teams cannot be assigned to employees
              </p>
            </div>

            <!-- Actions -->
            <div class="flex justify-between">
              <button
                type="button"
                @click="confirmDelete"
                class="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors flex items-center"
              >
                <Trash2 class="w-4 h-4 mr-2" />
                Delete Group
              </button>

              <div class="flex space-x-3">
                <button
                  type="button"
                  @click="resetForm"
                  class="btn-secondary"
                >
                  Reset
                </button>
                <button
                  type="submit"
                  :disabled="!hasChanges || loading"
                  class="btn-primary"
                  :class="{ 'opacity-50 cursor-not-allowed': !hasChanges || loading }"
                >
                  <span v-if="loading">⏳</span>
                  <Save v-else class="w-4 h-4 mr-2" />
                  {{ loading ? 'Saving...' : 'Save Changes' }}
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Employee Assignment Modal -->
    <EmployeeAssignmentModal
      v-if="showEmployeeAssignment"
      :team="team"
      :employees="allEmployees"
      @close="showEmployeeAssignment = false"
      @updated="onMembersUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  X,
  Eye,
  Users,
  Settings,
  Shield,
  UserPlus,
  UserMinus,
  Save,
  Trash2
} from 'lucide-vue-next'
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

// ========== PROPS & EMITS ==========
const props = defineProps<{
  team: Group
}>()

const emit = defineEmits<{
  close: []
  updated: []
  deleted: []
}>()

// ========== STATE ==========
const loading = ref(false)
const activeTab = ref('overview')
const groupMembers = ref<Employee[]>([])
const allEmployees = ref<Employee[]>([])
const showEmployeeAssignment = ref(false)

const editForm = ref({
  description: props.team.description,
  permissionLevel: props.team.permissionLevel,
  active: props.team.active
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

// ========== TABS ==========
const tabs = [
  { id: 'overview', label: 'Overview', icon: Eye },
  { id: 'members', label: 'Members', icon: Users },
  { id: 'settings', label: 'Settings', icon: Settings }
]

// ========== COMPUTED ==========
const activeMembers = computed(() => {
  return groupMembers.value.filter(m => m.active).length
})

const averageEfficiency = computed(() => {
  if (groupMembers.value.length === 0) return 0
  const sum = groupMembers.value.reduce((acc, m) => acc + m.efficiencyRating, 0)
  return Math.round((sum / groupMembers.value.length) * 100) / 100
})

const totalWorkHours = computed(() => {
  return groupMembers.value.reduce((acc, m) => acc + m.workHoursPerDay, 0)
})

const hasChanges = computed(() => {
  return editForm.value.description !== props.team.description ||
    editForm.value.permissionLevel !== props.team.permissionLevel ||
    editForm.value.active !== props.team.active
})

// ========== METHODS ==========
const loadGroupMembers = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${props.team.id}?includeEmployees=true`)
    if (response.ok) {
      const data = await response.json()
      groupMembers.value = data.employees || []
    }
  } catch (error) {
    console.error('Error loading team members:', error)
  } finally {
    loading.value = false
  }
}

const loadAllEmployees = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/employees`)
    if (response.ok) {
      const data = await response.json()
      allEmployees.value = data.employees || []
    }
  } catch (error) {
    console.error('Error loading employees:', error)
  }
}

const updateGroup = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${props.team.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editForm.value)
    })

    if (response.ok) {
      emit('updated')
    } else {
      console.error('Error updating team')
    }
  } catch (error) {
    console.error('Error updating team:', error)
  } finally {
    loading.value = false
  }
}

const confirmDelete = () => {
  if (confirm(`Are you sure you want to delete the team "${props.team.name}"? This action cannot be undone.`)) {
    deleteGroup()
  }
}

const deleteGroup = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${props.team.id}`, {
      method: 'DELETE'
    })

    if (response.ok) {
      emit('deleted')
    } else {
      console.error('Error deleting team')
    }
  } catch (error) {
    console.error('Error deleting team:', error)
  } finally {
    loading.value = false
  }
}

const removeMemberFromGroup = async (employee: Employee) => {
  if (!confirm(`Remove ${employee.fullName} from this team?`)) return

  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${props.team.id}/employees/${employee.id}`, {
      method: 'DELETE'
    })

    if (response.ok) {
      groupMembers.value = groupMembers.value.filter(m => m.id !== employee.id)
    }
  } catch (error) {
    console.error('Error removing member:', error)
  }
}

const openEmployeeAssignment = () => {
  showEmployeeAssignment.value = true
}

const onMembersUpdated = () => {
  showEmployeeAssignment.value = false
  loadGroupMembers()
  emit('updated')
}

const resetForm = () => {
  editForm.value = {
    description: props.team.description,
    permissionLevel: props.team.permissionLevel,
    active: props.team.active
  }
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

const getEmployeeInitials = (employee: Employee) => {
  return `${employee.firstName?.charAt(0) || ''}${employee.lastName?.charAt(0) || ''}`
}

const formatDate = (dateString: string) => {
  if (!dateString) return 'N/A'
  try {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return 'Invalid date'
  }
}

// ========== WATCHERS ==========
watch(() => props.team, (newGroup) => {
  if (newGroup) {
    editForm.value = {
      description: newGroup.description,
      permissionLevel: newGroup.permissionLevel,
      active: newGroup.active
    }
    loadGroupMembers()
  }
}, { immediate: true })

// ========== LIFECYCLE ==========
onMounted(() => {
  loadGroupMembers()
  loadAllEmployees()
})
</script>

<style scoped>
.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500;
}

.btn-primary {
  @apply bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center;
}

.btn-secondary {
  @apply bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400 transition-colors;
}
</style>
