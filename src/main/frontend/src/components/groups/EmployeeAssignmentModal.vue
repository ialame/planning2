<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-hidden">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-200">
        <div class="flex justify-between items-center">
          <div>
            <h2 class="text-lg font-semibold text-gray-900">Manage Group Members</h2>
            <p class="text-sm text-gray-600">{{ group?.name }} - {{ group?.description }}</p>
          </div>
          <button
            @click="$emit('close')"
            class="text-gray-400 hover:text-gray-600"
          >
            <X class="w-6 h-6" />
          </button>
        </div>
      </div>

      <!-- Content -->
      <div class="flex h-[calc(90vh-120px)]">
        <!-- Current Members (Left Side) -->
        <div class="w-1/2 border-r border-gray-200 flex flex-col">
          <div class="px-6 py-4 bg-gray-50">
            <div class="flex justify-between items-center">
              <h3 class="font-medium text-gray-900">
                Current Members ({{ currentMembers.length }})
              </h3>
              <button
                @click="removeAllMembers"
                :disabled="currentMembers.length === 0"
                class="text-red-600 hover:text-red-800 text-sm disabled:opacity-50"
              >
                Remove All
              </button>
            </div>
          </div>

          <div class="flex-1 overflow-y-auto px-6 py-4">
            <div v-if="currentMembers.length === 0" class="text-center py-8">
              <Users class="w-12 h-12 text-gray-400 mx-auto mb-2" />
              <p class="text-gray-500">No members assigned</p>
            </div>

            <div v-else class="space-y-3">
              <div
                v-for="employee in currentMembers"
                :key="employee.id"
                class="flex items-center justify-between p-3 bg-blue-50 rounded-lg"
              >
                <div class="flex items-center space-x-3">
                  <div class="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center text-white font-medium">
                    {{ getEmployeeInitials(employee) }}
                  </div>
                  <div>
                    <p class="font-medium text-gray-900">{{ employee.fullName }}</p>
                    <p class="text-sm text-gray-600">{{ employee.email }}</p>
                  </div>
                </div>
                <button
                  @click="removeMember(employee)"
                  class="text-red-600 hover:text-red-800 p-1"
                  :disabled="loading"
                >
                  <UserMinus class="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Available Employees (Right Side) -->
        <div class="w-1/2 flex flex-col">
          <div class="px-6 py-4 bg-gray-50">
            <div class="flex justify-between items-center mb-3">
              <h3 class="font-medium text-gray-900">
                Available Employees ({{ availableEmployees.length }})
              </h3>
              <button
                @click="addAllAvailable"
                :disabled="availableEmployees.length === 0"
                class="text-blue-600 hover:text-blue-800 text-sm disabled:opacity-50"
              >
                Add All
              </button>
            </div>

            <!-- Search -->
            <div class="relative">
              <Search class="w-4 h-4 absolute left-3 top-3 text-gray-400" />
              <input
                v-model="searchTerm"
                type="text"
                placeholder="Search employees..."
                class="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
              >
            </div>
          </div>

          <div class="flex-1 overflow-y-auto px-6 py-4">
            <div v-if="filteredAvailableEmployees.length === 0" class="text-center py-8">
              <UserPlus class="w-12 h-12 text-gray-400 mx-auto mb-2" />
              <p class="text-gray-500">
                {{ searchTerm ? 'No employees match your search' : 'No available employees' }}
              </p>
            </div>

            <div v-else class="space-y-3">
              <div
                v-for="employee in filteredAvailableEmployees"
                :key="employee.id"
                class="flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:bg-gray-50"
              >
                <div class="flex items-center space-x-3">
                  <div class="w-10 h-10 bg-gray-600 rounded-full flex items-center justify-center text-white font-medium">
                    {{ getEmployeeInitials(employee) }}
                  </div>
                  <div>
                    <p class="font-medium text-gray-900">{{ employee.fullName }}</p>
                    <p class="text-sm text-gray-600">{{ employee.email }}</p>
                    <div class="flex items-center space-x-2 mt-1">
                      <span class="text-xs bg-gray-100 text-gray-800 px-2 py-1 rounded">
                        {{ employee.workHoursPerDay }}h/day
                      </span>
                      <span class="text-xs bg-green-100 text-green-800 px-2 py-1 rounded">
                        {{ employee.efficiencyRating }}x efficiency
                      </span>
                    </div>
                  </div>
                </div>
                <button
                  @click="addMember(employee)"
                  class="text-blue-600 hover:text-blue-800 p-1"
                  :disabled="loading"
                >
                  <UserPlus class="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="px-6 py-4 border-t border-gray-200 bg-gray-50">
        <div class="flex justify-between items-center">
          <div class="text-sm text-gray-600">
            <span class="font-medium">{{ currentMembers.length }}</span> member(s) assigned to this group
          </div>
          <div class="flex space-x-3">
            <button
              @click="$emit('close')"
              class="btn-secondary"
            >
              Close
            </button>
            <button
              @click="saveChanges"
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
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  X,
  Users,
  UserPlus,
  UserMinus,
  Search,
  Save
} from 'lucide-vue-next'

import { API_BASE_URL } from '@/config/api.ts'

// ========== INTERFACES ==========
interface Group {
  id: string
  name: string
  description: string
  permissionLevel: number
  active: boolean
  employeeCount: number
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

// ========== PROPS & EMITS ==========
const props = defineProps<{
  group: Group | null
  employees: Employee[]
}>()

const emit = defineEmits<{
  close: []
  updated: []
}>()

// ========== STATE ==========
const loading = ref(false)
const searchTerm = ref('')
const currentMembers = ref<Employee[]>([])
const originalMemberIds = ref<string[]>([])

// ========== COMPUTED ==========
const availableEmployees = computed(() => {
  const currentMemberIds = currentMembers.value.map(m => m.id)
  return props.employees.filter(emp =>
    emp.active && !currentMemberIds.includes(emp.id)
  )
})

const filteredAvailableEmployees = computed(() => {
  if (!searchTerm.value) return availableEmployees.value

  const search = searchTerm.value.toLowerCase()
  return availableEmployees.value.filter(emp =>
    emp.fullName.toLowerCase().includes(search) ||
    emp.email.toLowerCase().includes(search)
  )
})

const hasChanges = computed(() => {
  const currentIds = currentMembers.value.map(m => m.id).sort()
  const originalIds = [...originalMemberIds.value].sort()

  return JSON.stringify(currentIds) !== JSON.stringify(originalIds)
})

// ========== METHODS ==========
const loadGroupMembers = async () => {
  if (!props.group) return

  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/groups/${props.group.id}?includeEmployees=true`)
    if (response.ok) {
      const data = await response.json()
      currentMembers.value = data.employees || []
      originalMemberIds.value = currentMembers.value.map(m => m.id)
    }
  } catch (error) {
    console.error('Error loading group members:', error)
  } finally {
    loading.value = false
  }
}

const addMember = (employee: Employee) => {
  if (!currentMembers.value.find(m => m.id === employee.id)) {
    currentMembers.value.push(employee)
  }
}

const removeMember = (employee: Employee) => {
  currentMembers.value = currentMembers.value.filter(m => m.id !== employee.id)
}

const addAllAvailable = () => {
  filteredAvailableEmployees.value.forEach(emp => {
    if (!currentMembers.value.find(m => m.id === emp.id)) {
      currentMembers.value.push(emp)
    }
  })
}

const removeAllMembers = () => {
  if (confirm('Are you sure you want to remove all members from this group?')) {
    currentMembers.value = []
  }
}

// ============= REMPLACEMENT COMPLET DE LA MÉTHODE saveChanges =============
// Dans EmployeeAssignmentModal.vue, remplacez ENTIÈREMENT la méthode saveChanges par ceci :

const saveChanges = async () => {
  if (!props.group || !hasChanges.value) return

  loading.value = true
  try {
    console.log('🔄 Saving group member changes...')
    console.log('Group:', props.group.id)
    console.log('Current members:', currentMembers.value.map(m => `${m.id} (${m.fullName})`))
    console.log('Original members:', originalMemberIds.value)

    const currentIds = new Set(currentMembers.value.map(m => m.id))
    const originalIds = new Set(originalMemberIds.value)

    // Employés à ajouter au groupe
    const employeesToAdd = currentMembers.value.filter(emp => !originalIds.has(emp.id))

    // Employés à retirer du groupe
    const employeesToRemove = originalMemberIds.value.filter(id => !currentIds.has(id))

    console.log('Adding employees:', employeesToAdd.map(e => `${e.id} (${e.fullName})`))
    console.log('Removing employees:', employeesToRemove)

    const promises = []

    // Ajouter les nouveaux employés
    for (const employee of employeesToAdd) {
      console.log(`➕ Adding ${employee.fullName} to group ${props.group.name}`)
      promises.push(
        fetch(`${API_BASE_URL}/api/v2/groups/${props.group.id}/employees/${employee.id}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' }
        }).then(async response => {
          if (!response.ok) {
            const errorText = await response.text()
            throw new Error(`Failed to add employee ${employee.fullName}: ${response.status} - ${errorText}`)
          }
          console.log(`✅ Successfully added ${employee.fullName} to group`)
          return response.json()
        })
      )
    }

    // Retirer les anciens employés
    for (const employeeId of employeesToRemove) {
      const employee = props.employees.find(e => e.id === employeeId)
      const employeeName = employee ? employee.fullName : employeeId

      console.log(`➖ Removing ${employeeName} from group ${props.group.name}`)
      promises.push(
        fetch(`${API_BASE_URL}/api/v2/groups/${props.group.id}/employees/${employeeId}`, {
          method: 'DELETE',
          headers: { 'Content-Type': 'application/json' }
        }).then(async response => {
          if (!response.ok) {
            const errorText = await response.text()
            throw new Error(`Failed to remove employee ${employeeName}: ${response.status} - ${errorText}`)
          }
          console.log(`✅ Successfully removed ${employeeName} from group`)
          return response.json()
        })
      )
    }

    // Exécuter toutes les opérations
    console.log(`🚀 Executing ${promises.length} operations...`)
    await Promise.all(promises)

    // Mettre à jour l'état local
    originalMemberIds.value = currentMembers.value.map(m => m.id)

    console.log('✅ All changes saved successfully!')
    emit('updated')

  } catch (error) {
    console.error('❌ Error saving changes:', error)

    // Afficher l'erreur complète pour le debug
    let errorMessage = 'Unknown error'
    if (error instanceof Error) {
      errorMessage = error.message
    } else if (typeof error === 'string') {
      errorMessage = error
    }

    alert(`Failed to save changes: ${errorMessage}`)
  } finally {
    loading.value = false
  }
}
const getEmployeeInitials = (employee: Employee) => {
  return `${employee.firstName?.charAt(0) || ''}${employee.lastName?.charAt(0) || ''}`
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadGroupMembers()
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
