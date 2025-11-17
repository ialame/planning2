<template>
  <div class="employees-page">
    <!-- Header -->
    <div class="flex justify-between items-center mb-6">
      <h1 class="text-3xl font-bold text-gray-900">Team Management</h1>

      <!-- View Switcher -->
      <div class="flex space-x-2 bg-gray-100 rounded-lg p-1">
        <button
          @click="currentView = 'management'"
          :class="[
            'px-4 py-2 rounded-md text-sm font-medium transition-colors',
            currentView === 'management'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          ]"
        >
          👥 Management
        </button>
        <button
          @click="currentView = 'planning'"
          :class="[
            'px-4 py-2 rounded-md text-sm font-medium transition-colors',
            currentView === 'planning'
              ? 'bg-white text-blue-600 shadow-sm'
              : 'text-gray-600 hover:text-gray-900'
          ]"
        >
          📅 Planning
        </button>
      </div>
    </div>

    <!-- Date Selector (Planning view only) -->
    <div v-if="currentView === 'planning' && !selectedEmployeeId" class="mb-6">
      <div class="bg-white rounded-lg shadow-md p-4">
        <label class="block text-sm font-medium text-gray-700 mb-2">
          Select Date for Planning
        </label>
        <input
          v-model="selectedDate"
          type="date"
          @change="loadPlanningForSelectedDate"
          class="input-field max-w-xs"
        />
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
      <div class="card">
        <div class="flex items-center">
          <div class="bg-blue-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Total Employees</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.total }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-green-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Available</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.available }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-orange-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Busy</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.busy }}</p>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="flex items-center">
          <div class="bg-red-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Overloaded</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.overloaded }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Employee List -->
    <div v-if="!selectedEmployeeId">

      <!-- ✅ MANAGEMENT VIEW -->
      <div v-if="currentView === 'management'" class="bg-white rounded-lg shadow-md overflow-hidden">
        <div class="px-6 py-4 border-b border-gray-200">
          <div class="flex items-center justify-between">
            <h2 class="text-lg font-semibold text-gray-900">Team Members</h2>
            <button @click="showAddForm = !showAddForm" class="btn-primary">
              {{ showAddForm ? 'Cancel' : '+ Add Employee' }}
            </button>
          </div>
        </div>

        <!-- Add Employee Form -->
        <div v-if="showAddForm" class="p-6 bg-gray-50 border-b">
          <form @submit.prevent="addEmployee" class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <input
              v-model="newEmployee.firstName"
              placeholder="First Name"
              class="input-field"
              required
            >
            <input
              v-model="newEmployee.lastName"
              placeholder="Last Name"
              class="input-field"
              required
            >
            <input
              v-model="newEmployee.email"
              type="email"
              placeholder="Email"
              class="input-field"
              required
            >
            <div class="flex space-x-2">
              <button type="submit" class="btn-primary flex-1">Add</button>
              <button type="button" @click="showAddForm = false" class="btn-secondary">Cancel</button>
            </div>
          </form>
        </div>

        <!-- Employee Cards Grid (MANAGEMENT VIEW) -->
        <div class="p-6">
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div
              v-for="employee in managementEmployees"
              :key="employee.id"
              class="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow border border-gray-200"
            >
              <div class="p-6">
                <!-- Employee Header -->
                <div class="flex items-center justify-between mb-4">
                  <div class="flex items-center">
                    <!-- ✅ Avatar Component -->
                    <EmployeeAvatar
                      :key="`avatar-${employee.id}-${avatarKey}`"
                      :employeeId="employee.id"
                      :employeeName="`${employee.firstName} ${employee.lastName}`"
                      size="md"
                    />

                    <div class="ml-3">
                      <h3 class="text-lg font-semibold text-gray-900">
                        {{ employee.firstName }} {{ employee.lastName }}
                      </h3>
                      <p class="text-sm text-gray-600">{{ employee.email }}</p>
                    </div>
                  </div>

                  <span :class="[
                    'px-2 py-1 rounded-full text-xs font-medium',
                    employee.status === 'AVAILABLE' ? 'bg-green-100 text-green-800' :
                    employee.status === 'BUSY' ? 'bg-orange-100 text-orange-800' :
                    employee.status === 'OVERLOADED' ? 'bg-red-100 text-red-800' :
                    'bg-gray-100 text-gray-800'
                  ]">
                    {{ employee.status || 'AVAILABLE' }}
                  </span>
                </div>

                <!-- Employee Details -->
                <div class="space-y-2 mb-4">
                  <div class="flex justify-between text-sm">
                    <span class="text-gray-600">Work Hours:</span>
                    <span class="font-medium">{{ employee.workHoursPerDay }}h/day</span>
                  </div>
                  <div class="flex justify-between text-sm">
                    <span class="text-gray-600">Teams:</span>
                    <span class="font-medium">{{ employee.teams?.length || 0 }}</span>
                  </div>
                  <div class="flex justify-between text-sm">
                    <span class="text-gray-600">Active:</span>
                    <span :class="employee.active ? 'text-green-600' : 'text-red-600'">
                      {{ employee.active ? '✓ Yes' : '✗ No' }}
                    </span>
                  </div>
                </div>

                <!-- Action Buttons -->
                <div class="flex space-x-2">
                  <!-- Upload Photo Button -->
                  <button
                    @click="openPhotoUpload(employee.id)"
                    class="flex-1 bg-purple-50 text-purple-600 px-3 py-2 rounded text-sm font-medium hover:bg-purple-100 flex items-center justify-center gap-1"
                    title="Upload Photo"
                  >
                    📷 Photo
                  </button>

                  <!-- View Details Button -->
                  <button
                    @click="viewEmployeeDetails(employee.id)"
                    class="flex-1 bg-blue-50 text-blue-600 px-3 py-2 rounded text-sm font-medium hover:bg-blue-100"
                  >
                    👁️ Details
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ✅ PLANNING VIEW -->
      <div v-if="currentView === 'planning'" class="space-y-6">
        <div
          v-for="employee in employeesWithWorkload"
          :key="employee.id"
          class="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow"
        >
          <div class="p-6">
            <!-- Employee Info -->
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center">
                <EmployeeAvatar
                  :employeeId="employee.id"
                  :employeeName="`${employee.firstName} ${employee.lastName}`"
                  size="lg"
                />
                <div class="ml-4">
                  <h3 class="text-xl font-semibold text-gray-900">
                    {{ employee.firstName }} {{ employee.lastName }}
                  </h3>
                  <p class="text-sm text-gray-600">{{ employee.email }}</p>
                </div>
              </div>

              <!-- Workload Badge -->
              <div :class="[
                'px-4 py-2 rounded-lg text-sm font-semibold',
                employee.workload < 0.8 ? 'bg-green-100 text-green-800' :
                employee.workload < 1.0 ? 'bg-orange-100 text-orange-800' :
                'bg-red-100 text-red-800'
              ]">
                {{ Math.round(employee.workload * 100) }}% Workload
              </div>
            </div>

            <!-- Workload Bar -->
            <div class="mb-4">
              <div class="flex justify-between text-sm text-gray-600 mb-1">
                <span>Capacity</span>
                <span>{{ employee.estimatedHours }}h / {{ employee.workHoursPerDay }}h</span>
              </div>
              <div class="w-full bg-gray-200 rounded-full h-3">
                <div
                  :class="[
                    'h-3 rounded-full transition-all',
                    employee.workload < 0.8 ? 'bg-green-500' :
                    employee.workload < 1.0 ? 'bg-orange-500' :
                    'bg-red-500'
                  ]"
                  :style="{ width: Math.min(employee.workload * 100, 100) + '%' }"
                ></div>
              </div>
            </div>

            <!-- Stats -->
            <div class="grid grid-cols-3 gap-4 mb-4">
              <div class="text-center p-3 bg-gray-50 rounded-lg">
                <p class="text-2xl font-bold text-gray-900">{{ employee.totalCards || 0 }}</p>
                <p class="text-xs text-gray-600">Total Cards</p>
              </div>
              <div class="text-center p-3 bg-gray-50 rounded-lg">
                <p class="text-2xl font-bold text-gray-900">{{ employee.activeOrders || 0 }}</p>
                <p class="text-xs text-gray-600">Active Orders</p>
              </div>
              <div class="text-center p-3 bg-gray-50 rounded-lg">
                <p class="text-2xl font-bold text-gray-900">{{ selectedDate }}</p>
                <p class="text-xs text-gray-600">Planning Date</p>
              </div>
            </div>

            <!-- ✅ PLANNING ACTIONS -->
            <div class="flex space-x-2">
              <button
                @click="viewEmployeePlanning(employee.id)"
                class="flex-1 bg-green-50 text-green-600 px-3 py-2 rounded text-sm font-medium hover:bg-green-100"
              >
                👁️ View Orders & Cards
              </button>
              <button
                class="px-3 py-2 bg-blue-50 text-blue-600 rounded text-sm font-medium hover:bg-blue-100"
              >
                💳 {{ employee.totalCards || 0 }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-if="employees.length === 0 && !loading" class="bg-white rounded-lg shadow-md p-8 text-center">
        <svg class="mx-auto h-12 w-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z"></path>
        </svg>
        <h3 class="text-lg font-medium text-gray-900 mb-2">No employees found</h3>
        <p class="text-gray-600 mb-4">Start by adding your first team member</p>
        <button @click="showAddForm = true" class="btn-primary">
          + Add First Employee
        </button>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="bg-white rounded-lg shadow-md p-8 text-center">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <p class="text-gray-600">Loading employees...</p>
      </div>
    </div>

    <!-- ✅ EMPLOYEE DETAIL VIEW -->
    <div v-if="selectedEmployeeId" class="space-y-6">
      <EmployeeDetailPage
        :key="`${selectedEmployeeId}-${selectedDate}`"
        :employeeId="selectedEmployeeId"
        :selectedDate="selectedDate"
        :mode="currentView"
        @back="handleEmployeeBack"
        @refresh="loadEmployees"
      />
    </div>

    <!-- ✅ PHOTO UPLOAD MODAL -->
    <div
      v-if="showPhotoUploadModal"
      class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      @click.self="closePhotoUpload"
    >
      <div class="bg-white rounded-lg p-6 max-w-md w-full mx-4 shadow-2xl">
        <div class="flex justify-between items-center mb-4">
          <h3 class="text-lg font-semibold text-gray-900">Upload Employee Photo</h3>
          <button
            @click="closePhotoUpload"
            class="text-gray-400 hover:text-gray-600 text-2xl leading-none hover:bg-gray-100 rounded-full w-8 h-8 flex items-center justify-center transition-colors"
            title="Close"
          >
            ✕
          </button>
        </div>

        <!-- Photo Uploader Component -->
        <EmployeePhotoUploader
          v-if="selectedPhotoEmployeeId"
          :employeeId="selectedPhotoEmployeeId"
          :editable="true"
          @photo-updated="handlePhotoUploaded"
          @photo-deleted="handlePhotoDeleted"
        />

        <div class="mt-4 text-sm text-gray-500 text-center">
          <p>📸 Upload a photo (max 5MB)</p>
          <p class="mt-1">Supported formats: JPG, PNG, GIF</p>
        </div>

        <!-- Close Button -->
        <div class="mt-6 flex justify-end">
          <button
            @click="closePhotoUpload"
            class="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import EmployeeDetailPage from '../components/EmployeeDetailPage.vue'
import EmployeeAvatar from '@/components/EmployeeAvatar.vue'
import EmployeePhotoUploader from '@/components/EmployeePhotoUploader.vue'
import { API_BASE_URL, API_ENDPOINTS } from '@/config/api.ts'
import authService from "@/services/authService.ts";

// ========== INTERFACES ==========
interface Employee {
  id: string
  firstName: string
  lastName: string
  fullName?: string
  name?: string
  email: string
  workHoursPerDay: number
  active: boolean
  status?: string
  workload?: number
  estimatedHours?: number
  totalCards?: number
  activeOrders?: number
  teams?: any[]
}

interface NewEmployee {
  firstName: string
  lastName: string
  email: string
}

// ========== STATE ==========
const currentView = ref<'management' | 'planning'>('management')
const selectedEmployeeId = ref<string | null>(null)
const loading = ref(false)
const showAddForm = ref(false)
const selectedDate = ref(new Date().toISOString().split('T')[0])

// Photo upload modal state
const showPhotoUploadModal = ref(false)
const selectedPhotoEmployeeId = ref<string | null>(null)
const avatarKey = ref(0) // Key to force avatar reload

const employees = ref<Employee[]>([])

const error = ref<string | null>(null)

const newEmployee = ref<NewEmployee>({
  firstName: '',
  lastName: '',
  email: ''
})

/**
 * View employee details
 */
const viewEmployeeDetails = (employeeId: string) => {
  console.log('👁️ Viewing employee details:', employeeId)
  currentView.value = 'planning'  // ✅ Changer le mode en planning
  selectedEmployeeId.value = employeeId
}

// ========== COMPUTED ==========
const stats = computed(() => ({
  total: employees.value.length,
  available: employees.value.filter(e => e.workload && e.workload < 0.8).length,
  busy: employees.value.filter(e => e.workload && e.workload >= 0.8 && e.workload < 1.0).length,
  overloaded: employees.value.filter(e => e.workload && e.workload >= 1.0).length
}))

const managementEmployees = computed(() => {
  return employees.value.filter(emp => emp)
})

const employeesWithWorkload = computed(() => {
  if (currentView.value !== 'planning') {
    return []
  }

  return employees.value.map(emp => ({
    ...emp,
    workload: emp.workload || Math.random() * 1.2,
    estimatedHours: emp.estimatedHours || Math.round((emp.workload || 0) * (emp.workHoursPerDay || 8)),
    totalCards: emp.totalCards || Math.floor(Math.random() * 200),
    activeOrders: emp.activeOrders || Math.floor(Math.random() * 5)
  }))
})

// ========== METHODS ==========

/**
 * Load employees
 */
// ✅ Flag pour empêcher les appels multiples
let isLoadingEmployees = false

/**
 * Load employees
 */
const loadEmployees = async () => {
  // Protection contre les appels multiples
  if (isLoadingEmployees) {
    console.log('⏭️ Already loading employees, skipping...')
    return
  }

  isLoadingEmployees = true
  loading.value = true
  error.value = null

  try {
    console.log('📦 Loading employees...')

    const data = await authService.get('/api/employees')

    console.log('📥 API Response:', data)
    console.log('📊 Is Array?', Array.isArray(data))
    console.log('📊 Length:', Array.isArray(data) ? data.length : 'N/A')

    // Handle different response formats
    if (Array.isArray(data)) {
      employees.value = data
      console.log('✅ Loaded as array:', data.length, 'employees')
    } else if (data.content) {
      employees.value = data.content
      console.log('✅ Loaded from data.content:', data.content.length, 'employees')
    } else if (data.employees) {
      employees.value = data.employees
      console.log('✅ Loaded from data.employees:', data.employees.length, 'employees')
    } else {
      console.warn('⚠️ Unknown response format:', Object.keys(data))
      employees.value = []
    }

    console.log('✅ Final employees:', employees.value.length)

  } catch (err: any) {
    console.error('❌ Error loading employees:', err)
    error.value = err.message || 'Failed to load employees'
    employees.value = []
  } finally {
    loading.value = false
    isLoadingEmployees = false
    console.log('🏁 Loading complete')
  }
}

/**
 * Add new employee
 */
/**
 * Add new employee
 * FIXED: Use API_ENDPOINTS.EMPLOYEES constant for correct URL
 */
const addEmployee = async () => {
  try {
    console.log('➕ Adding employee:', newEmployee.value)

    // FIXED: Use the predefined API_ENDPOINTS constant
    // This ensures consistency across the application
    const response = await fetch(API_ENDPOINTS.EMPLOYEES, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newEmployee.value)
    })

    if (response.ok) {
      const data = await response.json()
      console.log('✅ Employee added successfully:', data)

      newEmployee.value = { firstName: '', lastName: '', email: '' }
      showAddForm.value = false
      await loadEmployees()
    } else {
      const errorData = await response.json()
      console.error('❌ Failed to add employee:', errorData)
      alert(`Error: ${errorData.message || 'Failed to add employee'}`)
    }
  } catch (error) {
    console.error('❌ Error adding employee:', error)
    alert('Network error: Could not connect to server')
  }
}

/**
 * Load planning for selected date
 */
const loadPlanningForSelectedDate = async () => {
  if (selectedEmployeeId.value && currentView.value === 'planning') {
    console.log('📅 Loading planning for date:', selectedDate.value)
  }
}

/**
 * View employee planning
 */
const viewEmployeePlanning = (employeeId: string) => {
  console.log('📅 Viewing employee planning:', employeeId)
  selectedEmployeeId.value = employeeId
}

/**
 * Handle back from employee detail
 */
const handleEmployeeBack = () => {
  selectedEmployeeId.value = null
  loadEmployees()
}

/**
 * Open photo upload modal
 */
const openPhotoUpload = (employeeId: string) => {
  console.log('📷 Opening photo upload for employee:', employeeId)
  selectedPhotoEmployeeId.value = employeeId
  showPhotoUploadModal.value = true
}

/**
 * Close photo upload modal
 */
const closePhotoUpload = () => {
  showPhotoUploadModal.value = false
  selectedPhotoEmployeeId.value = null
}

/**
 * Handle photo uploaded
 */
const handlePhotoUploaded = () => {
  closePhotoUpload()
  avatarKey.value++ // ✨ Force reload
  setTimeout(() => loadEmployees(), 300)
}

/**
 * Handle photo deleted
 */
const handlePhotoDeleted = () => {
  console.log('🗑️ Photo deleted')
  // Close modal
  closePhotoUpload()
  // Refresh employee list
  setTimeout(() => {
    loadEmployees()
  }, 500)
}

// Mock teams for display (temporary)
const getEmployeeRole = (email: string): string => {
  if (email.includes('admin')) return 'Admin'
  if (email.includes('manager')) return 'Manager'
  if (email.includes('grader')) return 'Grader'
  if (email.includes('certifier')) return 'Certifier'
  if (email.includes('scanner')) return 'Scanner'
  if (email.includes('preparer')) return 'Preparer'
  return 'Employee'
}
// ========== LIFECYCLE ==========
onMounted(() => {
  console.log('🔧 Employees component mounted')

  // ✅ Charger une seule fois
  if (authService.isAuthenticated()) {
    loadEmployees()
  } else {
    console.log('⚠️ Not authenticated')
  }
})
</script>

<style scoped>
.employees-page {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: white;
  color: #667eea;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: 500;
  border: 2px solid #667eea;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: #f0f4ff;
}

.input-field {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.2s;
}

.input-field:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}
</style>
