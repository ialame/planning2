<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white rounded-lg shadow-xl max-w-3xl w-full mx-4 max-h-[90vh] overflow-hidden">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-200">
        <div class="flex justify-between items-center">
          <div class="flex items-center space-x-3">
            <div class="w-12 h-12 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-lg">
              {{ getEmployeeInitials(employee) }}
            </div>
            <div>
              <h2 class="text-lg font-semibold text-gray-900">Manage Teams</h2>
              <p class="text-sm text-gray-600">{{ employee.fullName }} - {{ employee.email }}</p>
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

      <!-- Current Status -->
      <div class="px-6 py-4 bg-gray-50 border-b border-gray-200">
        <div class="grid grid-cols-4 gap-4 text-center">
          <div>
            <p class="text-lg font-bold text-blue-600">{{ selectedTeams.length }}</p>
            <p class="text-xs text-gray-600">Teams Assigned</p>
          </div>
          <div>
            <p class="text-lg font-bold text-purple-600">{{ highestPermissionLevel }}</p>
            <p class="text-xs text-gray-600">Highest Level</p>
          </div>
          <div>
            <p class="text-lg font-bold text-green-600">{{ primaryRole }}</p>
            <p class="text-xs text-gray-600">Primary Role</p>
          </div>
          <div>
            <p :class="[
              'text-lg font-bold',
              hasChanges ? 'text-orange-600' : 'text-gray-600'
            ]">
              {{ hasChanges ? 'Modified' : 'Unchanged' }}
            </p>
            <p class="text-xs text-gray-600">Status</p>
          </div>
        </div>
      </div>

      <!-- Content -->
      <div class="p-6">
        <!-- Search and Filter -->
        <div class="mb-6">
          <div class="flex space-x-4">
            <div class="flex-1">
              <div class="relative">
                <Search class="w-4 h-4 absolute left-3 top-3 text-gray-400" />
                <input
                  v-model="searchTerm"
                  type="text"
                  placeholder="Search teams..."
                  class="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                >
              </div>
            </div>
            <select
              v-model="permissionFilter"
              class="border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option :value="null">All Levels</option>
              <option v-for="level in [10, 9, 8, 7, 6, 5, 4, 3, 2, 1]" :key="level" :value="level">
                Level {{ level }}+
              </option>
            </select>
          </div>
        </div>

        <!-- Teams Grid -->
        <div class="max-h-96 overflow-y-auto">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div
              v-for="team in filteredTeams"
              :key="team.id"
              :class="[
                'p-4 border-2 rounded-lg cursor-pointer transition-all',
                isTeamSelected(team)
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 hover:border-gray-300'
              ]"
              @click="toggleTeam(team)"
            >
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center space-x-3">
                  <div :class="[
                    'w-10 h-10 rounded-full flex items-center justify-center text-white font-bold',
                    getPermissionLevelColor(team.permissionLevel)
                  ]">
                    {{ getTeamIcon(team.permissionLevel) }}
                  </div>
                  <div>
                    <h4 class="font-medium text-gray-900">{{ team.name }}</h4>
                    <p class="text-sm text-gray-600 truncate">{{ team.description }}</p>
                  </div>
                </div>
                <div class="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    :checked="isTeamSelected(team)"
                    @click.stop
                    @change="toggleTeam(team)"
                    class="h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                  >
                </div>
              </div>

              <div class="flex justify-between items-center">
                <span :class="[
                  'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
                  getPermissionBadgeColor(team.permissionLevel)
                ]">
                  <Shield class="w-3 h-3 mr-1" />
                  Level {{ team.permissionLevel }}
                </span>
                <span class="text-xs text-gray-500">
                  {{ team.employeeCount }} members
                </span>
              </div>

              <!-- Change Indicator -->
              <div v-if="getTeamChangeType(team)" class="mt-2">
                <span :class="[
                  'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
                  getTeamChangeType(team) === 'added' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                ]">
                  {{ getTeamChangeType(team) === 'added' ? '+ Will be added' : '- Will be removed' }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div v-if="filteredTeams.length === 0" class="text-center py-8">
          <Users class="w-12 h-12 text-gray-400 mx-auto mb-2" />
          <p class="text-gray-500">No teams found</p>
          <p class="text-sm text-gray-400">Try adjusting your search or filters</p>
        </div>
      </div>

      <!-- Footer -->
      <div class="px-6 py-4 border-t border-gray-200 bg-gray-50">
        <div class="flex justify-between items-center">
          <div class="text-sm text-gray-600">
            <span class="font-medium">{{ selectedTeams.length }}</span> teams selected
            <span v-if="hasChanges" class="ml-2 text-orange-600 font-medium">
              ({{ changesCount }} changes pending)
            </span>
          </div>
          <div class="flex space-x-3">
            <button
              @click="resetChanges"
              :disabled="!hasChanges"
              class="btn-secondary"
              :class="{ 'opacity-50 cursor-not-allowed': !hasChanges }"
            >
              Reset
            </button>
            <button
              @click="$emit('close')"
              class="btn-secondary"
            >
              Cancel
            </button>
            <button
              @click="saveChanges"
              :disabled="!hasChanges || loading"
              class="btn-primary"
              :class="{ 'opacity-50 cursor-not-allowed': !hasChanges || loading }"
            >
              <span v-if="loading">⏳</span>
              <Save v-else class="w-4 h-4 mr-2" />
              {{ loading ? 'Saving...' : `Save Changes (${changesCount})` }}
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
  employee: Employee
  currentTeams: Team[]
}>()

const emit = defineEmits<{
  close: []
  updated: []
}>()

// ========== STATE ==========
const loading = ref(false)
const searchTerm = ref('')
const permissionFilter = ref<number | null>(null)
const allTeams = ref<Team[]>([])
const selectedTeams = ref<Team[]>([])
const originalTeamIds = ref<string[]>([])

// ========== COMPUTED ==========
const filteredTeams = computed(() => {
  let filtered = allTeams.value.filter(g => g.active)

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

  return filtered.sort((a, b) => b.permissionLevel - a.permissionLevel)
})

const highestPermissionLevel = computed(() => {
  if (selectedTeams.value.length === 0) return 0
  return Math.max(...selectedTeams.value.map(g => g.permissionLevel))
})

const primaryRole = computed(() => {
  const level = highestPermissionLevel.value
  if (level >= 8) return 'Admin'
  if (level >= 5) return 'Manager'
  if (level >= 3) return 'Processor'
  if (level >= 1) return 'Viewer'
  return 'No Role'
})

const hasChanges = computed(() => {
  const currentIds = selectedTeams.value.map(g => g.id).sort()
  const originalIds = [...originalTeamIds.value].sort()
  return JSON.stringify(currentIds) !== JSON.stringify(originalIds)
})

const changesCount = computed(() => {
  const currentIds = new Set(selectedTeams.value.map(g => g.id))
  const originalIds = new Set(originalTeamIds.value)

  const added = selectedTeams.value.filter(g => !originalIds.has(g.id)).length
  const removed = originalTeamIds.value.filter(id => !currentIds.has(id)).length

  return added + removed
})

// ========== METHODS ==========

/**
 * Convertit un UUID HEX (32 caractères) en UUID standard avec tirets
 */
const formatUuidFromHex = (hexUuid: string): string => {
  if (!hexUuid || hexUuid.length !== 32) {
    console.error('Invalid HEX UUID format:', hexUuid)
    return hexUuid
  }

  const hex = hexUuid.toLowerCase()
  return `${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}`
}

/**
 * Charge tous les teames disponibles
 */
const loadAllTeams = async () => {
  loading.value = true
  try {
    console.log('🔄 Loading all available teams...')
    const response = await fetch(`${API_BASE_URL}/api/v2/teams?includeEmployeeCount=true`)
    if (response.ok) {
      const data = await response.json()
      allTeams.value = data.teams || []
      console.log('✅ Loaded all teams:', allTeams.value.length)
    } else {
      console.error('❌ Failed to load teams:', response.status)
    }
  } catch (error) {
    console.error('❌ Error loading teams:', error)
  } finally {
    loading.value = false
  }
}

/**
 * Charge les teames actuels de l'employé
 */
const loadEmployeeCurrentTeams = async () => {
  try {
    console.log('🔄 Loading current teams for employee:', props.employee.id)

    // ✅ MÊME PATTERN : Utiliser l'ID directement comme pour les employés
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/employee/${props.employee.id}`)

    if (response.ok) {
      const data = await response.json()
      const currentTeams = data.teams || []
      console.log('✅ Loaded current teams for employee:', currentTeams)

      selectedTeams.value = [...currentTeams]
      originalTeamIds.value = currentTeams.map(g => g.id)

      console.log('📋 Selected teams initialized:', selectedTeams.value.length)
    } else {
      console.log('ℹ️ No current teams found for employee')
      selectedTeams.value = []
      originalTeamIds.value = []
    }
  } catch (error) {
    console.error('❌ Error loading employee current teams:', error)
    selectedTeams.value = []
    originalTeamIds.value = []
  }
}
const isTeamSelected = (team: Team) => {
  return selectedTeams.value.some(g => g.id === team.id)
}

const toggleTeam = (team: Team) => {
  if (isTeamSelected(team)) {
    selectedTeams.value = selectedTeams.value.filter(g => g.id !== team.id)
    console.log('➖ Removed team:', team.name)
  } else {
    selectedTeams.value.push(team)
    console.log('➕ Added team:', team.name)
  }
  console.log('📋 Current selection:', selectedTeams.value.map(g => g.name))
}

const getTeamChangeType = (team: Team) => {
  const wasSelected = originalTeamIds.value.includes(team.id)
  const isSelected = isTeamSelected(team)

  if (!wasSelected && isSelected) return 'added'
  if (wasSelected && !isSelected) return 'removed'
  return null
}

const getEmployeeInitials = (employee: Employee) => {
  return `${employee.firstName?.charAt(0) || ''}${employee.lastName?.charAt(0) || ''}`
}

const getPermissionLevelColor = (level: number) => {
  if (level >= 8) return 'bg-red-500'
  if (level >= 5) return 'bg-orange-500'
  if (level >= 3) return 'bg-yellow-500'
  return 'bg-blue-500'
}

const getPermissionBadgeColor = (level: number) => {
  if (level >= 8) return 'bg-red-100 text-red-800'
  if (level >= 5) return 'bg-orange-100 text-orange-800'
  if (level >= 3) return 'bg-yellow-100 text-yellow-800'
  return 'bg-blue-100 text-blue-800'
}

const getTeamIcon = (level: number) => {
  if (level >= 8) return '👑'
  if (level >= 5) return '🛡️'
  if (level >= 3) return '⭐'
  return '👤'
}

const saveChanges = async () => {
  if (!hasChanges.value) return

  loading.value = true
  try {
    console.log('🔄 Saving team changes for employee:', props.employee.id)
    console.log('📋 Teams to assign:', selectedTeams.value.map(g => g.id))

    const currentIds = new Set(selectedTeams.value.map(g => g.id))
    const originalIds = new Set(originalTeamIds.value)

    // Teams to add
    const teamsToAdd = selectedTeams.value.filter(g => !originalIds.has(g.id))
    // Teams to remove
    const teamsToRemove = originalTeamIds.value.filter(id => !currentIds.has(id))

    console.log('➕ Adding to teams:', teamsToAdd.map(g => g.name))
    console.log('➖ Removing from teams:', teamsToRemove.map(id => {
      const team = allTeams.value.find(g => g.id === id)
      return team ? team.name : id
    }))

    const promises = []

    // Add to new teams
    for (const team of teamsToAdd) {
      console.log(`📤 POST ${API_BASE_URL}/api/v2/teams/${team.id}/employees/${props.employee.id}`)
      promises.push(
        fetch(`${API_BASE_URL}/api/v2/teams/${team.id}/employees/${props.employee.id}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }
        }).then(async response => {
          if (!response.ok) {
            const errorText = await response.text()
            console.error(`❌ Failed to add to team ${team.name}:`, response.status, errorText)
            throw new Error(`Failed to add to team ${team.name}: ${response.status} - ${errorText}`)
          }
          console.log(`✅ Successfully added to team: ${team.name}`)
          return response.json()
        })
      )
    }

    // Remove from old teams
    for (const teamId of teamsToRemove) {
      const team = allTeams.value.find(g => g.id === teamId)
      const teamName = team ? team.name : teamId

      console.log(`📤 DELETE ${API_BASE_URL}/api/v2/teams/${teamId}/employees/${props.employee.id}`)
      promises.push(
        fetch(`${API_BASE_URL}/api/v2/teams/${teamId}/employees/${props.employee.id}`, {
          method: 'DELETE',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          }
        }).then(async response => {
          if (!response.ok) {
            const errorText = await response.text()
            console.error(`❌ Failed to remove from team ${teamName}:`, response.status, errorText)
            throw new Error(`Failed to remove from team ${teamName}: ${response.status} - ${errorText}`)
          }
          console.log(`✅ Successfully removed from team: ${teamName}`)
          return response.json()
        })
      )
    }

    // Execute all operations
    console.log(`🚀 Executing ${promises.length} individual operations...`)
    if (promises.length === 0) {
      console.log('ℹ️ No changes to apply')
      // Même si pas de changements, on peut fermer le modal
      emit('updated')
      return
    }

    await Promise.all(promises)
    console.log('✅ All individual operations completed successfully!')

    // ✅ Mise à jour de l'état local - CORRECT
    originalTeamIds.value = selectedTeams.value.map(g => g.id)

    console.log('🔄 Updated original teams to:', originalTeamIds.value)
    console.log('💾 Team assignment completed successfully!')

    // ✅ Émettre l'événement de mise à jour
    emit('updated')

  } catch (error) {
    console.error('❌ Error saving changes:', error)

    let errorMessage = 'Unknown error'
    if (error instanceof Error) {
      errorMessage = error.message
    } else if (typeof error === 'string') {
      errorMessage = error
    }

    alert(`Failed to save team changes: ${errorMessage}`)
  } finally {
    loading.value = false
  }
}
// ========== LIFECYCLE ==========
onMounted(async () => {
  console.log('🚀 Modal mounted for employee:', props.employee.fullName)
  console.log('📋 Received current teams:', props.currentTeams)

  // Charger tous les teames disponibles ET les teames actuels de l'employé
  await Promise.all([
    loadAllTeams(),
    loadEmployeeCurrentTeams()
  ])

  console.log('✅ Modal initialization complete')
})
</script>

<style scoped>
.btn-primary {
  @apply bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center;
}

.btn-secondary {
  @apply bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400 transition-colors;
}
</style>
