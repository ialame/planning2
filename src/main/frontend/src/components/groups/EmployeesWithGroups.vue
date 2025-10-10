<template>
  <div class="employees-with-groups">
    <!-- Header -->
    <div class="mb-8">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">👤 Employee Role Management</h1>
          <p class="text-gray-600 mt-1">Assign and manage employee group memberships</p>
        </div>
        <div class="flex space-x-3">
          <select
            v-model="roleFilter"
            class="border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">All Roles</option>
            <option value="admin">Administrators</option>
            <option value="manager">Managers</option>
            <option value="processor">Processors</option>
            <option value="viewer">Viewers</option>
            <option value="unassigned">Unassigned</option>
          </select>
          <button
            @click="exportEmployeeRoles"
            class="btn-secondary"
          >
            <Download class="w-4 h-4 mr-2" />
            Export
          </button>
        </div>
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="grid grid-cols-1 md:grid-cols-5 gap-4 mb-8">
      <div class="card text-center">
        <p class="text-2xl font-bold text-gray-900">{{ employeeStats.total }}</p>
        <p class="text-sm text-gray-600">Total Employees</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold text-red-600">{{ employeeStats.admins }}</p>
        <p class="text-sm text-gray-600">Administrators</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold text-yellow-600">{{ employeeStats.managers }}</p>
        <p class="text-sm text-gray-600">Managers</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold text-blue-600">{{ employeeStats.processors }}</p>
        <p class="text-sm text-gray-600">Processors</p>
      </div>
      <div class="card text-center">
        <p class="text-2xl font-bold text-gray-600">{{ employeeStats.unassigned }}</p>
        <p class="text-sm text-gray-600">Unassigned</p>
      </div>
    </div>

    <!-- Search and Filters -->
    <div class="card mb-6">
      <div class="flex flex-col sm:flex-row gap-4">
        <div class="flex-1">
          <div class="relative">
            <Search class="w-5 h-5 absolute left-3 top-3 text-gray-400" />
            <input
              v-model="searchTerm"
              type="text"
              placeholder="Search employees..."
              class="input-field pl-10"
            >
          </div>
        </div>
        <div class="flex gap-2">
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

    <!-- Employee List -->
    <div v-if="!loading && filteredEmployees.length > 0" class="space-y-4">
      <div
        v-for="employee in filteredEmployees"
        :key="employee.id"
        class="card hover:shadow-md transition-shadow"
      >
        <div class="flex items-center justify-between">
          <!-- Employee Info -->
          <div class="flex items-center space-x-4">
            <div class="w-16 h-16 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-xl">
              {{ getEmployeeInitials(employee) }}
            </div>
            <div>
              <h3 class="text-lg font-semibold text-gray-900">{{ employee.fullName }}</h3>
              <p class="text-gray-600">{{ employee.email }}</p>
              <div class="flex items-center space-x-2 mt-1">
                <span class="text-sm bg-gray-100 text-gray-800 px-2 py-1 rounded">
                  {{ employee.workHoursPerDay }}h/day
                </span>
                <span class="text-sm bg-green-100 text-green-800 px-2 py-1 rounded">
                  {{ employee.efficiencyRating }}x efficiency
                </span>
                <span :class="[
                  'text-sm px-2 py-1 rounded',
                  employee.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                ]">
                  {{ employee.active ? 'Active' : 'Inactive' }}
                </span>
              </div>
            </div>
          </div>

          <!-- Groups -->
          <div class="flex-1 mx-8">
            <div class="flex flex-wrap gap-2">
              <span
                v-for="group in employee.groups"
                :key="group.id"
                :class="[
                  'inline-flex items-center px-3 py-1 rounded-full text-sm font-medium',
                  getPermissionBadgeColor(group.permissionLevel)
                ]"
              >
                <span class="mr-1">{{ getGroupIcon(group.permissionLevel) }}</span>
                {{ group.name }}
                <span class="ml-1 text-xs">({{ group.permissionLevel }})</span>
              </span>
              <span
                v-if="employee.groups.length === 0"
                class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-gray-100 text-gray-800"
              >
                No roles assigned
              </span>
            </div>

            <!-- Primary Role -->
            <div class="mt-2">
              <span class="text-sm text-gray-600">
                Primary Role:
                <span :class="[
                  'font-medium',
                  getPrimaryRoleColor(employee)
                ]">
                  {{ getPrimaryRole(employee) }}
                </span>
              </span>
            </div>
          </div>

          <!-- Actions -->
          <div class="flex space-x-2">
            <button
              @click="manageEmployeeGroups(employee)"
              class="btn-primary text-sm"
            >
              <Settings class="w-4 h-4 mr-2" />
              Manage Roles
            </button>
            <button
              @click="viewEmployeeDetails(employee)"
              class="btn-secondary text-sm"
            >
              <Eye class="w-4 h-4 mr-2" />
              View Details
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!loading && filteredEmployees.length === 0" class="card text-center py-12">
      <Users class="w-16 h-16 text-gray-400 mx-auto mb-4" />
      <h3 class="text-lg font-medium text-gray-900 mb-2">No employees found</h3>
      <p class="text-gray-600 mb-4">
        {{ searchTerm || roleFilter ? 'Try adjusting your filters' : 'No employees available' }}
      </p>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="card text-center py-12">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
      <p class="text-gray-600">Loading employees...</p>
    </div>

    <!-- Employee Group Management Modal -->
    <EmployeeGroupManagementModal
      v-if="selectedEmployee"
      :employee="selectedEmployee"
      :current-groups="selectedEmployee.groups"
      @close="selectedEmployee = null"
      @updated="onEmployeeGroupsUpdated"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  Search,
  Filter,
  Download,
  Settings,
  Eye,
  Users
} from 'lucide-vue-next'
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
  groups: Group[]
}

interface Group {
  id: string
  name: string
  description: string
  permissionLevel: number
  active: boolean
}

// ========== PROPS & EMITS ==========
const emit = defineEmits<{
  updated: []
}>()

// ========== STATE ==========
const loading = ref(false)
const searchTerm = ref('')
const roleFilter = ref('')
const employees = ref<Employee[]>([])
const selectedEmployee = ref<Employee | null>(null)

// ========== COMPUTED ==========
const employeeStats = computed(() => {
  const stats = {
    total: employees.value.length,
    admins: 0,
    managers: 0,
    processors: 0,
    unassigned: 0
  }

  employees.value.forEach(emp => {
    const highestLevel = getHighestPermissionLevel(emp)
    if (highestLevel >= 8) stats.admins++
    else if (highestLevel >= 5) stats.managers++
    else if (highestLevel >= 3) stats.processors++
    else stats.unassigned++
  })

  return stats
})

const filteredEmployees = computed(() => {
  let filtered = employees.value

  // Search filter
  if (searchTerm.value) {
    const search = searchTerm.value.toLowerCase()
    filtered = filtered.filter(emp =>
      emp.fullName.toLowerCase().includes(search) ||
      emp.email.toLowerCase().includes(search) ||
      emp.groups.some(g => g.name.toLowerCase().includes(search))
    )
  }

  // Role filter
  if (roleFilter.value) {
    filtered = filtered.filter(emp => {
      const highestLevel = getHighestPermissionLevel(emp)
      switch (roleFilter.value) {
        case 'admin': return highestLevel >= 8
        case 'manager': return highestLevel >= 5 && highestLevel < 8
        case 'processor': return highestLevel >= 3 && highestLevel < 5
        case 'viewer': return highestLevel >= 1 && highestLevel < 3
        case 'unassigned': return emp.groups.length === 0
        default: return true
      }
    })
  }

  return filtered.sort((a, b) => {
    // Sort by highest permission level (descending), then by name
    const aLevel = getHighestPermissionLevel(a)
    const bLevel = getHighestPermissionLevel(b)
    if (aLevel !== bLevel) return bLevel - aLevel
    return a.fullName.localeCompare(b.fullName)
  })
})

// ========== METHODS ==========
const loadEmployees = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/employees`)
    if (response.ok) {
      const data = await response.json()

      // ✅ FIXED: Handle both response formats
      let employeeList = []
      if (data.employees && Array.isArray(data.employees)) {
        // Format: { employees: [...] }
        employeeList = data.employees
        console.log('✅ Loaded employees from data.employees:', employeeList.length)
      } else if (Array.isArray(data)) {
        // Format: [...]
        employeeList = data
        console.log('✅ Loaded employees from direct array:', employeeList.length)
      } else {
        console.error('❌ Unexpected response format:', data)
        return
      }

      // Load groups for each employee
      const employeesWithGroups = await Promise.all(
        employeeList.map(async (emp: Employee) => {
          try {
            const groupsResponse = await fetch(`${API_BASE_URL}/api/v2/groups/employee/${emp.id}`)
            if (groupsResponse.ok) {
              const groupsData = await groupsResponse.json()
              emp.groups = groupsData.groups || []
            } else {
              emp.groups = []
            }
          } catch (error) {
            console.error(`Error loading groups for employee ${emp.id}:`, error)
            emp.groups = []
          }
          return emp
        })
      )

      employees.value = employeesWithGroups
      console.log(`✅ Final result: ${employees.value.length} employees with groups loaded`)
    } else {
      console.error('❌ Failed to load employees:', response.status, response.statusText)
    }
  } catch (error) {
    console.error('❌ Error loading employees:', error)
  } finally {
    loading.value = false
  }
}

const onEmployeeGroupsUpdated = async () => {
  selectedEmployee.value = null
  await loadEmployees()
  emit('updated')
}

const clearFilters = () => {
  searchTerm.value = ''
  roleFilter.value = ''
}

const exportEmployeeRoles = () => {
  // Generate CSV export of employee roles
  const csvContent = generateEmployeeRolesCSV()
  downloadCSV(csvContent, 'employee-roles.csv')
}

// ========== UTILITY FUNCTIONS ==========
const getEmployeeInitials = (employee: Employee) => {
  return `${employee.firstName?.charAt(0) || ''}${employee.lastName?.charAt(0) || ''}`
}

const getHighestPermissionLevel = (employee: Employee) => {
  if (employee.groups.length === 0) return 0
  return Math.max(...employee.groups.map(g => g.permissionLevel))
}

const getPrimaryRole = (employee: Employee) => {
  const level = getHighestPermissionLevel(employee)
  if (level >= 8) return 'Administrator'
  if (level >= 5) return 'Manager'
  if (level >= 3) return 'Processor'
  if (level >= 1) return 'Viewer'
  return 'Unassigned'
}

const getPrimaryRoleColor = (employee: Employee) => {
  const level = getHighestPermissionLevel(employee)
  if (level >= 8) return 'text-red-600'
  if (level >= 5) return 'text-yellow-600'
  if (level >= 3) return 'text-blue-600'
  if (level >= 1) return 'text-gray-600'
  return 'text-gray-400'
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

const generateEmployeeRolesCSV = () => {
  const headers = ['Name', 'Email', 'Groups', 'Highest Permission Level', 'Primary Role', 'Status']
  const rows = employees.value.map(emp => [
    emp.fullName,
    emp.email,
    emp.groups.map(g => g.name).join('; '),
    getHighestPermissionLevel(emp),
    getPrimaryRole(emp),
    emp.active ? 'Active' : 'Inactive'
  ])

  return [headers, ...rows].map(row =>
    row.map(cell => `"${cell}"`).join(',')
  ).join('\n')
}

const downloadCSV = (content: string, filename: string) => {
  const blob = new Blob([content], { type: 'text/csv' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}
// Dans EmployeesWithGroups.vue, remplacez ces méthodes par :

const manageEmployeeGroups = (employee: Employee) => {
  console.log('🔧 Managing groups for employee:', employee)
  console.log('Employee data:', {
    id: employee.id,
    name: employee.fullName,
    email: employee.email,
    groups: employee.groups
  })

  selectedEmployee.value = employee
  console.log('✅ selectedEmployee set to:', selectedEmployee.value)
}

const viewEmployeeDetails = (employee: Employee) => {
  console.log('👁️ Viewing details for employee:', employee)
  // Pour l'instant, juste un log - vous pouvez implementer une modal de détails plus tard
  alert(`Employee Details:\n\nName: ${employee.fullName}\nEmail: ${employee.email}\nWork Hours: ${employee.workHoursPerDay}h/day\nGroups: ${employee.groups.length}`)
}
// ========== LIFECYCLE ==========
onMounted(() => {
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
</style>
