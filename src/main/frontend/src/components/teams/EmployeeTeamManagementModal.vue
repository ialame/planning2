<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-hidden flex flex-col">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-200">
        <div class="flex justify-between items-center">
          <div class="flex items-center space-x-3">
            <div class="w-12 h-12 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-lg">
              {{ getEmployeeInitials(employee) }}
            </div>
            <div>
              <h2 class="text-lg font-semibold text-gray-900">Manage Team Assignments</h2>
              <p class="text-sm text-gray-600">{{ employee.fullName }} - {{ employee.email }}</p>
            </div>
          </div>
          <button
            @click="$emit('close')"
            class="text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X class="w-6 h-6" />
          </button>
        </div>
      </div>

      <!-- Current Status Bar -->
      <div class="px-6 py-4 bg-gradient-to-r from-blue-50 to-purple-50 border-b border-gray-200">
        <div class="grid grid-cols-4 gap-4 text-center">
          <div>
            <p class="text-2xl font-bold text-blue-600">{{ selectedTeams.length }}</p>
            <p class="text-xs text-gray-600 font-medium">Teams Assigned</p>
          </div>
          <div>
            <p class="text-2xl font-bold text-purple-600">{{ highestPermissionLevel }}</p>
            <p class="text-xs text-gray-600 font-medium">Highest Level</p>
          </div>
          <div>
            <p class="text-2xl font-bold text-green-600">{{ primaryRole }}</p>
            <p class="text-xs text-gray-600 font-medium">Primary Role</p>
          </div>
          <div>
            <p :class="[
              'text-2xl font-bold',
              hasChanges ? 'text-orange-600' : 'text-gray-400'
            ]">
              {{ changesCount }}
            </p>
            <p class="text-xs text-gray-600 font-medium">
              {{ hasChanges ? 'Unsaved Changes' : 'No Changes' }}
            </p>
          </div>
        </div>
      </div>

      <!-- Search and Filter Bar -->
      <div class="px-6 py-4 bg-gray-50 border-b border-gray-200">
        <div class="flex gap-3">
          <div class="flex-1 relative">
            <Search class="w-5 h-5 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
            <input
              v-model="searchTerm"
              type="text"
              placeholder="Search teams by name or description..."
              class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
          </div>
          <select
            v-model="permissionFilter"
            class="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option :value="null">All Levels</option>
            <option :value="8">Admin (8+)</option>
            <option :value="5">Manager (5+)</option>
            <option :value="3">Processor (3+)</option>
            <option :value="1">Viewer (1+)</option>
          </select>
        </div>
      </div>

      <!-- Teams Grid -->
      <div class="flex-1 overflow-y-auto px-6 py-4">
        <div v-if="loading" class="flex items-center justify-center py-12">
          <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        </div>

        <div v-else-if="filteredTeams.length === 0" class="text-center py-12">
          <Users class="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <h3 class="text-lg font-medium text-gray-900 mb-2">No teams found</h3>
          <p class="text-gray-600">Try adjusting your search or filters</p>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div
            v-for="team in filteredTeams"
            :key="team.id"
            :class="[
              'p-4 border-2 rounded-lg cursor-pointer transition-all duration-200',
              isTeamSelected(team)
                ? 'border-blue-500 bg-blue-50 shadow-md'
                : 'border-gray-200 hover:border-blue-300 hover:shadow-sm'
            ]"
            @click="toggleTeam(team)"
          >
            <div class="flex items-start justify-between mb-3">
              <div class="flex items-center space-x-3 flex-1 min-w-0">
                <div :class="[
                  'w-12 h-12 rounded-full flex items-center justify-center text-white font-bold text-lg flex-shrink-0',
                  getPermissionLevelColor(team.permissionLevel)
                ]">
                  {{ getTeamIcon(team.permissionLevel) }}
                </div>
                <div class="flex-1 min-w-0">
                  <h4 class="font-semibold text-gray-900 truncate">{{ team.name }}</h4>
                  <p class="text-sm text-gray-600 truncate">{{ team.description }}</p>
                </div>
              </div>
              <input
                type="checkbox"
                :checked="isTeamSelected(team)"
                @click.stop
                @change="toggleTeam(team)"
                class="h-5 w-5 text-blue-600 border-gray-300 rounded focus:ring-blue-500 flex-shrink-0 mt-1"
              >
            </div>

            <div class="flex justify-between items-center">
              <span :class="[
                'inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold',
                getPermissionBadgeColor(team.permissionLevel)
              ]">
                <Shield class="w-3 h-3 mr-1" />
                Level {{ team.permissionLevel }}
              </span>
              <span class="text-xs text-gray-500 font-medium">
                {{ team.employeeCount || 0 }} members
              </span>
            </div>

            <!-- Change Indicator -->
            <div v-if="getTeamChangeType(team)" class="mt-3 pt-3 border-t border-gray-200">
              <span :class="[
                'inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold',
                getTeamChangeType(team) === 'added'
                  ? 'bg-green-100 text-green-800'
                  : 'bg-red-100 text-red-800'
              ]">
                {{ getTeamChangeType(team) === 'added' ? '➕ Will be added' : '➖ Will be removed' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer with Actions -->
      <div class="px-6 py-4 bg-gray-50 border-t border-gray-200">
        <div class="flex items-center justify-between">
          <div class="text-sm text-gray-600">
            <span v-if="hasChanges" class="font-medium text-orange-600">
              {{ changesCount }} unsaved {{ changesCount === 1 ? 'change' : 'changes' }}
            </span>
            <span v-else class="text-gray-500">No changes to save</span>
          </div>
          <div class="flex space-x-3">
            <button
              @click="$emit('close')"
              :disabled="loading"
              class="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors font-medium disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              @click="saveChanges"
              :disabled="loading || !hasChanges"
              class="px-5 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-2"
            >
              <Save class="w-4 h-4" />
              <span>{{ loading ? 'Saving...' : `Save Changes (${changesCount})` }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { X, Search, Shield, Users, Save } from 'lucide-vue-next'

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

interface Team {
  id: string
  name: string
  description: string
  permissionLevel: number
  active: boolean
  employeeCount: number
}

// ========== PROPS & EMITS ==========
const props = defineProps<{
  employee: Employee | null
  availableTeams: Team[]
}>()

const emit = defineEmits<{
  close: []
  saved: []
}>()

// ========== STATE ==========
const loading = ref(false)
const searchTerm = ref('')
const permissionFilter = ref<number | null>(null)
const selectedTeams = ref<Team[]>([])
const originalTeamIds = ref<string[]>([])

// ========== COMPUTED ==========
const filteredTeams = computed(() => {
  let filtered = props.availableTeams.filter(t => t.active)

  if (searchTerm.value) {
    const search = searchTerm.value.toLowerCase()
    filtered = filtered.filter(team =>
      team.name.toLowerCase().includes(search) ||
      team.description.toLowerCase().includes(search)
    )
  }

  if (permissionFilter.value !== null) {
    filtered = filtered.filter(team => team.permissionLevel >= permissionFilter.value!)
  }

  return filtered.sort((a, b) => {
    // Selected teams first
    const aSelected = isTeamSelected(a)
    const bSelected = isTeamSelected(b)
    if (aSelected && !bSelected) return -1
    if (!aSelected && bSelected) return 1

    // Then by permission level (descending)
    if (a.permissionLevel !== b.permissionLevel) {
      return b.permissionLevel - a.permissionLevel
    }

    // Finally by name
    return a.name.localeCompare(b.name)
  })
})

const highestPermissionLevel = computed(() => {
  if (selectedTeams.value.length === 0) return 0
  return Math.max(...selectedTeams.value.map(t => t.permissionLevel))
})

const primaryRole = computed(() => {
  const level = highestPermissionLevel.value
  if (level >= 8) return 'Admin'
  if (level >= 5) return 'Manager'
  if (level >= 3) return 'Processor'
  if (level >= 1) return 'Viewer'
  return 'None'
})

const hasChanges = computed(() => {
  const currentIds = selectedTeams.value.map(t => t.id).sort()
  const originalIds = [...originalTeamIds.value].sort()

  if (currentIds.length !== originalIds.length) return true

  return currentIds.some((id, index) => id !== originalIds[index])
})

const changesCount = computed(() => {
  const currentIds = new Set(selectedTeams.value.map(t => t.id))
  const originalIds = new Set(originalTeamIds.value)

  let count = 0

  // Count additions
  currentIds.forEach(id => {
    if (!originalIds.has(id)) count++
  })

  // Count removals
  originalIds.forEach(id => {
    if (!currentIds.has(id)) count++
  })

  return count
})

// ========== METHODS ==========
const isTeamSelected = (team: Team) => {
  return selectedTeams.value.some(t => t.id === team.id)
}

const toggleTeam = (team: Team) => {
  const index = selectedTeams.value.findIndex(t => t.id === team.id)

  if (index > -1) {
    selectedTeams.value.splice(index, 1)
  } else {
    selectedTeams.value.push(team)
  }
}

const getTeamChangeType = (team: Team): 'added' | 'removed' | null => {
  const isCurrentlySelected = isTeamSelected(team)
  const wasOriginallySelected = originalTeamIds.value.includes(team.id)

  if (isCurrentlySelected && !wasOriginallySelected) return 'added'
  if (!isCurrentlySelected && wasOriginallySelected) return 'removed'
  return null
}

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

const getTeamIcon = (level: number) => {
  if (level >= 8) return '👑'
  if (level >= 5) return '🛡️'
  if (level >= 3) return '⚙️'
  return '👀'
}

const getEmployeeInitials = (employee: Employee | null) => {
  if (!employee) return '?'
  return `${employee.firstName.charAt(0)}${employee.lastName.charAt(0)}`.toUpperCase()
}

const formatUUID = (uuid: string) => {
  if (!uuid) return uuid
  const cleaned = uuid.replace(/-/g, '').toLowerCase()
  if (cleaned.length === 32) {
    return `${cleaned.slice(0, 8)}-${cleaned.slice(8, 12)}-${cleaned.slice(12, 16)}-${cleaned.slice(16, 20)}-${cleaned.slice(20)}`
  }
  return uuid
}

const saveChanges = async () => {
  if (!props.employee || !hasChanges.value) return

  loading.value = true

  const employeeId = props.employee.id.toUpperCase()

  console.log('🔄 Saving team changes for employee:', employeeId)
  console.log('📋 Teams to assign:', selectedTeams.value.map(t => t.id))

  const currentIds = new Set(selectedTeams.value.map(t => t.id))
  const originalIds = new Set(originalTeamIds.value)

  const teamsToAdd = selectedTeams.value.filter(t => !originalIds.has(t.id))
  const teamsToRemove = originalTeamIds.value.filter(id => !currentIds.has(id))

  console.log('➕ Adding to teams:', teamsToAdd.map(t => t.name))
  console.log('➖ Removing from teams:', teamsToRemove.map(id => {
    const team = props.availableTeams.find(t => t.id === id)
    return team ? team.name : id
  }))

  try {
    const operations = []

    // Add to new teams
    for (const team of teamsToAdd) {
      const formattedTeamId = formatUUID(team.id)
      const url = `/api/v2/teams/${formattedTeamId}/employees/${employeeId}`

      console.log('📤 POST', `http://localhost:8080${url}`)

      operations.push(
        fetch(`http://localhost:8080${url}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' }
        }).then(async response => {
          if (!response.ok) {
            const errorData = await response.text()
            console.error(`❌ Failed to add to team ${team.name}:`, response.status, errorData)
            throw new Error(`Failed to add to team ${team.name}: ${response.status} - ${errorData}`)
          }
          return response
        })
      )
    }

    // Remove from old teams
    for (const teamId of teamsToRemove) {
      const formattedTeamId = formatUUID(teamId)
      const url = `/api/v2/teams/${formattedTeamId}/employees/${employeeId}`
      const teamName = props.availableTeams.find(t => t.id === teamId)?.name || teamId

      console.log('📤 DELETE', `http://localhost:8080${url}`)

      operations.push(
        fetch(`http://localhost:8080${url}`, {
          method: 'DELETE',
          headers: { 'Content-Type': 'application/json' }
        }).then(async response => {
          if (!response.ok) {
            const errorData = await response.text()
            console.error(`❌ Failed to remove from team ${teamName}:`, response.status, errorData)
            throw new Error(`Failed to remove from team ${teamName}: ${response.status} - ${errorData}`)
          }
          return response
        })
      )
    }

    if (operations.length === 0) {
      console.log('ℹ️ No changes to save')
      return
    }

    console.log(`🚀 Executing ${operations.length} individual operations...`)
    await Promise.all(operations)

    console.log('✅ All operations completed successfully')

    // Update original state
    originalTeamIds.value = selectedTeams.value.map(t => t.id)

    // Notify parent and close
    setTimeout(() => {
      emit('saved')
    }, 500)

  } catch (error) {
    console.error('❌ Error saving changes:', error)
    alert(`Failed to save team assignments: ${error instanceof Error ? error.message : 'Unknown error'}`)
  } finally {
    loading.value = false
  }
}

// ========== LIFECYCLE ==========
onMounted(() => {
  if (props.employee) {
    // Load current teams from employee data
    const currentTeams = props.employee.teams || []
    selectedTeams.value = props.availableTeams.filter(team =>
      currentTeams.some((ct: any) => ct.id === team.id)
    )
    originalTeamIds.value = selectedTeams.value.map(t => t.id)

    console.log('🔄 Modal initialized with teams:', selectedTeams.value.map(t => t.name))
  }
})
</script>

<style scoped>
/* Animations pour les transitions */
@keyframes slideIn {
  from {
    transform: translateY(-10px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.fixed {
  animation: slideIn 0.2s ease-out;
}
</style>
