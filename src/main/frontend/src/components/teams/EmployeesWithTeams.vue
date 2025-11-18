<template>
  <div class="employees-with-teams">
    <!-- Header -->
    <div class="mb-8">
      <div class="flex justify-between items-center">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">👤 Employee Role Management</h1>
          <p class="text-gray-600 mt-1">Assign and manage employee team memberships</p>
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
            Clear Filters
          </button>
        </div>
      </div>
    </div>

    <!-- Employee List -->
    <div v-if="!loading && filteredEmployees.length > 0" class="space-y-4">
      <div
        v-for="employee in filteredEmployees"
        :key="employee.id"
        class="card hover:shadow-lg transition-shadow"
      >
        <div class="flex items-center">
          <!-- Employee Info -->
          <div class="flex items-center space-x-4 w-1/3">
            <div class="w-12 h-12 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-lg">
              {{ getEmployeeInitials(employee) }}
            </div>
            <div>
              <h3 class="font-semibold text-gray-900">{{ employee.fullName }}</h3>
              <p class="text-sm text-gray-600">{{ employee.email }}</p>
              <div class="mt-1">
                <span :class="[
                  'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium',
                  employee.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                ]">
                  {{ employee.active ? 'Active' : 'Inactive' }}
                </span>
              </div>
            </div>
          </div>

          <!-- Teams -->
          <div class="flex-1 mx-8">
            <div class="flex flex-wrap gap-2">
              <span
                v-for="team in employee.teams"
                :key="team.id"
                :class="[
                  'inline-flex items-center px-3 py-1 rounded-full text-sm font-medium',
                  getPermissionBadgeColor(team.permissionLevel)
                ]"
              >
                <span class="mr-1">{{ getTeamIcon(team.permissionLevel) }}</span>
                {{ team.name }}
                <span class="ml-1 text-xs">({{ team.permissionLevel }})</span>
              </span>
              <span
                v-if="employee.teams.length === 0"
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
              @click="manageEmployeeTeams(employee)"
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

    <!-- Employee Team Management Modal - FIXED -->
    <EmployeeTeamManagementModal
      v-if="selectedEmployee"
      :employee="selectedEmployee"
      :availableTeams="allTeams"
      @close="handleModalClose"
      @saved="handleModalSaved"
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
import EmployeeTeamManagementModal from './EmployeeTeamManagementModal.vue'

import { API_BASE_URL } from '@/config/api.ts'
import authService from "@/services/authService.ts";

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
  teams: Team[]
}

interface Team {
  id: string
  name: string
  description: string
  permissionLevel: number
  active: boolean
  employeeCount?: number
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
const allTeams = ref<Team[]>([])
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
    if (emp.teams.length === 0) {
      stats.unassigned++
    } else {
      const highestLevel = Math.max(...emp.teams.map(t => t.permissionLevel))
      if (highestLevel >= 8) stats.admins++
      else if (highestLevel >= 5) stats.managers++
      else stats.processors++
    }
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
      emp.teams.some(t => t.name.toLowerCase().includes(search))
    )
  }

  // Role filter
  if (roleFilter.value) {
    filtered = filtered.filter(emp => {
      if (roleFilter.value === 'unassigned') {
        return emp.teams.length === 0
      }

      const highestLevel = emp.teams.length > 0
        ? Math.max(...emp.teams.map(t => t.permissionLevel))
        : 0

      switch (roleFilter.value) {
        case 'admin': return highestLevel >= 8
        case 'manager': return highestLevel >= 5 && highestLevel < 8
        case 'processor': return highestLevel >= 3 && highestLevel < 5
        case 'viewer': return highestLevel > 0 && highestLevel < 3
        default: return true
      }
    })
  }

  return filtered
})

// ========== METHODS ==========
const loadEmployees = async () => {
  loading.value = true
  console.log('🔄 Starting to load employees...')

  try {
    // ✅ authService.get retourne directement les données
    const data = await authService.get('/api/employees')

    console.log('📦 Raw API response:', data)
    console.log('📦 Data type:', typeof data, 'Is array?', Array.isArray(data))

    let employeeList: Employee[] = []

    if (Array.isArray(data)) {
      employeeList = data.map(emp => ({
        id: emp.id,
        firstName: emp.firstName,
        lastName: emp.lastName,
        fullName: emp.fullName || `${emp.firstName} ${emp.lastName}`,
        email: emp.email,
        active: emp.active,
        workHoursPerDay: emp.workHoursPerDay,
        efficiencyRating: emp.efficiencyRating || 1.0,
        teams: []
      }))
      console.log('✅ Using direct array format, count:', employeeList.length)
    } else if (data.employees && Array.isArray(data.employees)) {
      employeeList = data.employees.map(emp => ({
        id: emp.id,
        firstName: emp.firstName,
        lastName: emp.lastName,
        fullName: emp.fullName || `${emp.firstName} ${emp.lastName}`,
        email: emp.email,
        active: emp.active,
        workHoursPerDay: emp.workHoursPerDay,
        efficiencyRating: emp.efficiencyRating || 1.0,
        teams: []
      }))
      console.log('✅ Using data.employees format, count:', employeeList.length)
    } else {
      console.error('❌ Unexpected response format:', data)
      console.error('❌ Available keys:', Object.keys(data))
      return
    }

    console.log('👥 Employee list sample:', employeeList.slice(0, 2))

    // Load teams for each employee
    const employeesWithTeams = await Promise.all(
      employeeList.map(async (emp: Employee) => {
        try {
          const formattedId = formatUUID(emp.id)

          // ✅ Utiliser authService au lieu de fetch
          const teamsData = await authService.get(`/api/v2/teams/employee/${formattedId}`)
          emp.teams = teamsData.teams || []

        } catch (error) {
          console.error(`❌ Error loading teams for employee ${emp.id}:`, error)
          emp.teams = []
        }
        return emp
      })
    )

    employees.value = employeesWithTeams
    console.log('✅ SUCCESS! Employees loaded:', employees.value.length)
    console.log('👤 First employee sample:', employees.value[0])
    console.log('🔍 filteredEmployees computed:', filteredEmployees.value.length)

  } catch (error) {
    console.error('❌ Exception during loadEmployees:', error)
  } finally {
    loading.value = false
    console.log('✅ loadEmployees completed, loading =', loading.value)
  }
}

const loadAllTeams = async () => {
  try {
    // ✅ authService.get retourne directement les données
    const data = await authService.get('/api/v2/teams')

    allTeams.value = data.teams || data || []
    console.log(`✅ Loaded ${allTeams.value.length} teams`)

  } catch (error) {
    console.error('❌ Error loading teams:', error)
  }
}
const manageEmployeeTeams = (employee: Employee) => {
  console.log('🔧 Opening modal for employee:', employee.fullName)
  selectedEmployee.value = employee
}

const handleModalClose = () => {
  console.log('❌ Modal closed')
  selectedEmployee.value = null
}

const handleModalSaved = async () => {
  console.log('✅ Modal saved, reloading employees')
  await loadEmployees()
  selectedEmployee.value = null
  emit('updated')
}

const viewEmployeeDetails = (employee: Employee) => {
  alert(`Employee Details:\n\nName: ${employee.fullName}\nEmail: ${employee.email}\nWork Hours: ${employee.workHoursPerDay}h/day\nTeams: ${employee.teams.length}`)
}

const clearFilters = () => {
  searchTerm.value = ''
  roleFilter.value = ''
}

const exportEmployeeRoles = () => {
  const headers = ['Name', 'Email', 'Teams', 'Highest Permission', 'Primary Role', 'Status']
  const rows = filteredEmployees.value.map(emp => [
    emp.fullName,
    emp.email,
    emp.teams.map(t => t.name).join('; '),
    emp.teams.length > 0 ? Math.max(...emp.teams.map(t => t.permissionLevel)) : '0',
    getPrimaryRole(emp),
    emp.active ? 'Active' : 'Inactive'
  ])

  const csv = [headers, ...rows].map(row =>
    row.map(cell => `"${cell}"`).join(',')
  ).join('\n')

  const blob = new Blob([csv], { type: 'text/csv' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `employee-roles-${new Date().toISOString().split('T')[0]}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

const getEmployeeInitials = (employee: Employee) => {
  return `${employee.firstName.charAt(0)}${employee.lastName.charAt(0)}`.toUpperCase()
}

const getPrimaryRole = (employee: Employee) => {
  if (employee.teams.length === 0) return 'No Role'

  const highestLevel = Math.max(...employee.teams.map(t => t.permissionLevel))
  if (highestLevel >= 8) return 'Administrator'
  if (highestLevel >= 5) return 'Manager'
  if (highestLevel >= 3) return 'Processor'
  return 'Viewer'
}

const getPrimaryRoleColor = (employee: Employee) => {
  if (employee.teams.length === 0) return 'text-gray-600'

  const highestLevel = Math.max(...employee.teams.map(t => t.permissionLevel))
  if (highestLevel >= 8) return 'text-red-600'
  if (highestLevel >= 5) return 'text-yellow-600'
  if (highestLevel >= 3) return 'text-blue-600'
  return 'text-gray-600'
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

// Helper function to format UUID with hyphens
const formatUUID = (uuid: string) => {
  if (!uuid) return uuid

  // Remove any existing hyphens and convert to lowercase
  const cleaned = uuid.replace(/-/g, '').toLowerCase()

  // Add hyphens in the correct positions for UUID format: 8-4-4-4-12
  if (cleaned.length === 32) {
    return `${cleaned.slice(0, 8)}-${cleaned.slice(8, 12)}-${cleaned.slice(12, 16)}-${cleaned.slice(16, 20)}-${cleaned.slice(20)}`
  }

  return uuid
}

// ========== LIFECYCLE ==========
onMounted(() => {
  loadEmployees()
  loadAllTeams()
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
