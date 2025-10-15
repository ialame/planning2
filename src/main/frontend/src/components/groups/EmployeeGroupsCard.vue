<template>
  <div class="employee-teams-card card">
    <!-- Header -->
    <div class="flex justify-between items-center mb-4">
      <div class="flex items-center space-x-3">
        <Shield class="w-6 h-6 text-blue-600" />
        <div>
          <h3 class="font-semibold text-gray-900">Groups & Roles</h3>
          <p class="text-sm text-gray-600">{{ employee.fullName }}</p>
        </div>
      </div>
      <button
        @click="openGroupManagement"
        class="text-blue-600 hover:text-blue-800 text-sm font-medium"
      >
        Manage Groups →
      </button>
    </div>

    <!-- Current Groups -->
    <div v-if="employeeGroups.length > 0" class="space-y-3 mb-4">
      <div
        v-for="team in employeeGroups"
        :key="team.id"
        class="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
      >
        <div class="flex items-center space-x-3">
          <div :class="[
            'w-10 h-10 rounded-full flex items-center justify-center text-white font-bold text-sm',
            getPermissionLevelColor(team.permissionLevel)
          ]">
            {{ getGroupIcon(team.permissionLevel) }}
          </div>
          <div>
            <p class="font-medium text-gray-900">{{ team.name }}</p>
            <p class="text-sm text-gray-600">{{ team.description }}</p>
            <span :class="[
              'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium mt-1',
              getPermissionBadgeColor(team.permissionLevel)
            ]">
              <Shield class="w-3 h-3 mr-1" />
              Level {{ team.permissionLevel }}
            </span>
          </div>
        </div>
        <button
          @click="removeFromGroup(team)"
          class="text-red-600 hover:text-red-800 p-1"
          :disabled="loading"
        >
          <UserMinus class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="text-center py-6 bg-gray-50 rounded-lg mb-4">
      <Users class="w-12 h-12 text-gray-400 mx-auto mb-2" />
      <p class="text-gray-500 mb-2">No teams assigned</p>
      <p class="text-sm text-gray-400">This employee has no role assignments</p>
    </div>

    <!-- Quick Stats -->
    <div class="grid grid-cols-3 gap-4 pt-4 border-t border-gray-200">
      <div class="text-center">
        <p class="text-lg font-bold text-gray-900">{{ employeeGroups.length }}</p>
        <p class="text-xs text-gray-600">Groups</p>
      </div>
      <div class="text-center">
        <p class="text-lg font-bold text-gray-900">{{ highestPermissionLevel }}</p>
        <p class="text-xs text-gray-600">Max Level</p>
      </div>
      <div class="text-center">
        <p :class="[
          'text-lg font-bold',
          isAdmin ? 'text-red-600' : isManager ? 'text-yellow-600' : 'text-blue-600'
        ]">
          {{ primaryRole }}
        </p>
        <p class="text-xs text-gray-600">Primary Role</p>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="mt-4 pt-4 border-t border-gray-200">
      <div class="flex space-x-2">
        <button
          @click="addToGroup('PROCESSOR')"
          :disabled="loading || hasTeam('PROCESSOR')"
          class="flex-1 text-sm bg-blue-50 text-blue-600 px-3 py-2 rounded hover:bg-blue-100 disabled:opacity-50"
        >
          + Processor
        </button>
        <button
          @click="addToGroup('SUPERVISOR')"
          :disabled="loading || hasTeam('SUPERVISOR')"
          class="flex-1 text-sm bg-yellow-50 text-yellow-600 px-3 py-2 rounded hover:bg-yellow-100 disabled:opacity-50"
        >
          + Supervisor
        </button>
        <button
          @click="addToGroup('MANAGER')"
          :disabled="loading || hasTeam('MANAGER')"
          class="flex-1 text-sm bg-red-50 text-red-600 px-3 py-2 rounded hover:bg-red-100 disabled:opacity-50"
        >
          + Manager
        </button>
      </div>
    </div>

    <!-- Employee Team Management Modal -->
    <EmployeeGroupManagementModal
      v-if="showGroupManagement"
      :employee="employee"
      :current-teams="employeeGroups"
      @close="showGroupManagement = false"
      @updated="onGroupsUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Shield, Users, UserMinus } from 'lucide-vue-next'
import EmployeeGroupManagementModal from './EmployeeGroupManagementModal.vue'

import { API_BASE_URL } from '@/config/api.ts'

// ========== INTERFACES ==========
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

interface Group {
  id: string
  name: string
  description: string
  permissionLevel: number
  active: boolean
}

// ========== PROPS & EMITS ==========
const props = defineProps<{
  employee: Employee
}>()

const emit = defineEmits<{
  updated: []
}>()

// ========== STATE ==========
const loading = ref(false)
const employeeGroups = ref<Group[]>([])
const showGroupManagement = ref(false)

// ========== COMPUTED ==========
const highestPermissionLevel = computed(() => {
  if (employeeGroups.value.length === 0) return 0
  return Math.max(...employeeGroups.value.map(g => g.permissionLevel))
})

const isAdmin = computed(() => {
  return employeeGroups.value.some(g => g.permissionLevel >= 8)
})

const isManager = computed(() => {
  return employeeGroups.value.some(g => g.permissionLevel >= 5 && g.permissionLevel < 8)
})

const primaryRole = computed(() => {
  const level = highestPermissionLevel.value
  if (level >= 8) return 'Admin'
  if (level >= 5) return 'Manager'
  if (level >= 3) return 'Processor'
  if (level >= 1) return 'Viewer'
  return 'No Role'
})

// ========== METHODS ==========
const loadEmployeeGroups = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/employee/${props.employee.id}`)
    if (response.ok) {
      const data = await response.json()
      employeeGroups.value = data.teams || []
    }
  } catch (error) {
    console.error('Error loading employee teams:', error)
  } finally {
    loading.value = false
  }
}

const addToGroup = async (groupName: string) => {
  try {
    // First find the team by name
    const groupsResponse = await fetch(`${API_BASE_URL}/api/v2/teams?search=${groupName}`)
    if (!groupsResponse.ok) return

    const groupsData = await groupsResponse.json()
    const team = groupsData.teams?.find((g: Group) => g.name === groupName)

    if (!team) {
      console.error(`Group ${groupName} not found`)
      return
    }

    // Add employee to team
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${team.id}/employees/${props.employee.id}`, {
      method: 'POST'
    })

    if (response.ok) {
      await loadEmployeeGroups()
      emit('updated')
    }
  } catch (error) {
    console.error('Error adding to team:', error)
  }
}

const removeFromGroup = async (team: Group) => {
  if (!confirm(`Remove ${props.employee.fullName} from ${team.name}?`)) return

  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/${team.id}/employees/${props.employee.id}`, {
      method: 'DELETE'
    })

    if (response.ok) {
      await loadEmployeeGroups()
      emit('updated')
    }
  } catch (error) {
    console.error('Error removing from team:', error)
  }
}

const hasTeam = (groupName: string) => {
  return employeeGroups.value.some(g => g.name === groupName)
}

const openGroupManagement = () => {
  showGroupManagement.value = true
}

const onGroupsUpdated = () => {
  showGroupManagement.value = false
  loadEmployeeGroups()
  emit('updated')
}

// ========== UTILITY FUNCTIONS ==========
const getPermissionLevelColor = (level: number) => {
  if (level >= 8) return 'bg-red-500'
  if (level >= 5) return 'bg-yellow-500'
  if (level >= 3) return 'bg-blue-500'
  return 'bg-gray-500'
}

const getPermissionBadgeColor = (level: number) => {
  if (level >= 8) return 'bg-red-100 text-red-800'
  if (level >= 5) return 'bg-yellow-100 text-yellow-800'
  if (level >= 3) return 'bg-blue-100 text-blue-800'
  return 'bg-gray-100 text-gray-800'
}

const getGroupIcon = (level: number) => {
  if (level >= 8) return '👑'
  if (level >= 5) return '🛡️'
  if (level >= 3) return '⚙️'
  return '👀'
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadEmployeeGroups()
})
</script>

<style scoped>
.card {
  @apply bg-white rounded-lg shadow-sm border p-6;
}
</style>
