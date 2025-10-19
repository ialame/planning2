<template>
  <div class="container mx-auto p-6">
    <!-- Header with Mode Toggle -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-3xl font-bold text-gray-900">👥 Employee Management & Planning</h1>
        <p class="text-gray-600 mt-1">
          {{ currentView === 'management' ? 'Manage team members and their information' : 'View employee schedules and workload distribution' }}
        </p>
      </div>

      <!-- Mode Toggle -->
      <div class="flex bg-white rounded-lg shadow-sm border p-1">
        <button
          @click="currentView = 'management'"
          :class="[
            'px-4 py-2 rounded-md text-sm font-medium transition-colors',
            currentView === 'management' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:text-gray-900'
          ]"
        >
          👤 Management
        </button>
        <button
          @click="currentView = 'planning'"
          :class="[
            'px-4 py-2 rounded-md text-sm font-medium transition-colors',
            currentView === 'planning' ? 'bg-blue-600 text-white' : 'text-gray-600 hover:text-gray-900'
          ]"
        >
          📋 Planning View
        </button>
      </div>
    </div>

    <!-- Stats Dashboard -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
      <div class="card">
        <div class="flex items-center">
          <div class="bg-blue-500 rounded-lg p-3 mr-4">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
          </div>
          <div>
            <p class="text-sm text-gray-600">Total Team</p>
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


        <!-- Employee Cards Grid (NORMAL FORMAT) -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <div
            v-for="employee in managementEmployees"
            :key="employee.id"
            class="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow"
          >
            <div class="p-6">
              <!-- Employee Header -->
              <div class="flex items-center justify-between mb-4">
                <div class="flex items-center">
                  <div class="w-12 h-12 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
              <span class="text-white font-bold">
<!--                {{ getInitials(employee) }}-->
                <!-- ✅ NOUVEAU : Composant Avatar -->
                  <EmployeeAvatar
                    :employeeId="employee.id"
                    :employeeName="`${employee.firstName} ${employee.lastName}`"
                    size="md"
                  />
              </span>
                  </div>
                  <div class="ml-3">
                    <h3 class="text-lg font-semibold text-gray-900">{{ employee.firstName }} {{ employee.lastName }}</h3>
                    <p class="text-sm text-gray-600">{{ employee.email }}</p>
                  </div>
                </div>

                <span :class="[
            'px-2 py-1 rounded-full text-xs font-medium',
            employee.status === 'AVAILABLE' ? 'bg-green-100 text-green-800' :
            employee.status === 'BUSY' ? 'bg-orange-100 text-orange-800' :
            'bg-gray-100 text-gray-800'
          ]">
            {{ employee.status || 'AVAILABLE' }}
          </span>
              </div>

              <!-- Employee Stats -->
              <div class="space-y-2 mb-4">
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">Work Hours:</span>
                  <span class="font-medium">{{ employee.workHoursPerDay || 8 }}h/day</span>
                </div>
                <div class="flex justify-between text-sm">
                  <span class="text-gray-600">Active Orders:</span>
                  <span class="font-medium">{{ employee.activeOrders || 0 }}</span>
                </div>
              </div>

              <!-- ✅ CORRECT ACTIONS (View Details + View Planning) -->
              <div class="flex space-x-2">
                <button
                  @click="viewEmployee(employee.id)"
                  class="flex-1 bg-blue-50 text-blue-600 px-3 py-2 rounded text-sm font-medium hover:bg-blue-100"
                >
                  👁️ View Details
                </button>
                <button
                  @click="viewEmployeePlanning(employee.id)"
                  class="flex-1 bg-green-50 text-green-600 px-3 py-2 rounded text-sm font-medium hover:bg-green-100"
                >
                  📋 View Planning
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>


      <!-- ✅ PLANNING VIEW -->
      <!-- Planning Employee Grid (WORKLOAD FORMAT) -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="employee in employeesWithWorkload"
          :key="employee.id"
          class="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow cursor-pointer"
        >
          <div class="p-6">
            <!-- Employee Header -->
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center">
                <div class="w-16 h-16 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
              <span class="text-white font-bold text-xl">
                {{ getInitials(employee) }}
              </span>
                </div>
                <div class="ml-4">
                  <h3 class="text-xl font-bold text-gray-900">{{ employee.firstName }} {{ employee.lastName }}</h3>
                  <p class="text-gray-600">{{ employee.activeOrders || 0 }} active tasks</p>
                </div>
              </div>

              <span :class="[
            'px-3 py-1 rounded-full text-sm font-medium',
            getWorkloadColor(employee.workload || 0)
          ]">
            {{ getWorkloadStatus(employee.workload || 0) }}
          </span>
            </div>

            <!-- Workload Progress -->
            <div class="mb-4">
              <div class="flex justify-between text-sm mb-2">
                <span class="text-gray-600">Daily Workload</span>
                <span class="font-medium">{{ Math.round((employee.workload || 0) * 100) }}%</span>
              </div>
              <div class="w-full bg-gray-200 rounded-full h-2">
                <div
                  class="h-2 rounded-full transition-all duration-300"
                  :class="getWorkloadBarColor(employee.workload || 0)"
                  :style="{ width: `${Math.min((employee.workload || 0) * 100, 100)}%` }"
                ></div>
              </div>
              <div class="text-xs text-gray-500 mt-1">
                {{ employee.estimatedHours || 0 }}h / {{ employee.workHoursPerDay || 8 }}h
              </div>
            </div>

            <!-- Date Filter for Planning View -->
            <div v-if="currentView === 'planning'" class="bg-white rounded-lg shadow p-4 mb-6">
              <div class="flex items-center justify-between">
                <div>
                  <h3 class="text-lg font-semibold text-gray-900">📅 Planning Date</h3>
                  <p class="text-sm text-gray-600">Select date to view planning assignments</p>
                </div>
                <div class="flex items-center space-x-4">
                  <label class="text-sm font-medium text-gray-700">Date:</label>
                  <input
                    type="date"
                    v-model="selectedDate"
                    class="border border-gray-300 rounded-md px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500"
                  />
                  <div class="text-sm text-gray-500">
                    Current: {{ selectedDate }}
                  </div>
                </div>
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import EmployeeDetailPage from '../components/EmployeeDetailPage.vue'
import { API_BASE_URL, API_ENDPOINTS } from '@/config/api'
import EmployeePhotoUploader from "@/components/EmployeePhotoUploader.vue";
import EmployeeAvatar from "@/components/EmployeeAvatar.vue";


// ========== STATE ==========
const currentView = ref<'management' | 'planning'>('management')
const selectedEmployeeId = ref<string | null>(null)
const loading = ref(false)
const showAddForm = ref(false)
const selectedDate = ref(new Date().toISOString().split('T')[0])

const isCheckingOtherDates = ref(false) // ← NOUVEAU : empêche la récursion
// Employee data
const employees = ref<Employee[]>([])
const newEmployee = ref<NewEmployee>({
  firstName: '',
  lastName: '',
  email: ''
})

const loadPlanningForSelectedDate = async () => {
  if (selectedEmployeeId.value && currentView.value === 'planning') {
    await loadEmployeePlannings(selectedEmployeeId.value)
  }
}

// ========== COMPUTED ==========
const stats = computed(() => ({
  total: employees.value.length,
  available: employees.value.filter(e => e.workload && e.workload < 0.8).length,
  busy: employees.value.filter(e => e.workload && e.workload >= 0.8 && e.workload < 1.0).length,
  overloaded: employees.value.filter(e => e.workload && e.workload >= 1.0).length
}))

// For management view (normal employee cards)
const managementEmployees = computed(() => {
  return employees.value.filter(emp => emp)  // Just return all employees normally
})

// For planning view (employees with workload data)
const employeesWithWorkload = computed(() => {
  if (currentView.value !== 'planning') {
    return []  // Don't compute workload data unless in planning view
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
const loadEmployees = async () => {
  loading.value = true
  try {
    console.log('👥 Loading employees in management mode')

    const response = await fetch(`${API_BASE_URL}/api/employees`, { method: 'GET' })

    if (response.ok) {
      const data = await response.json()

      // ✅ Load employees in NORMAL format (not workload format)
      employees.value = (data.employees || data || []).map(emp => ({
        id: emp.id,
        firstName: emp.firstName,
        lastName: emp.lastName,
        email: emp.email,
        status: emp.status || 'AVAILABLE',
        workHoursPerDay: emp.workHoursPerDay || 8,
        activeOrders: emp.activeOrders || 0,
        // Don't include workload data in management view
      }))

      console.log(`✅ Loaded ${employees.value.length} employees in management mode`)
    } else {
      console.error('Failed to load employees:', response.status)
    }
  } catch (error) {
    console.error('Error loading employees:', error)
  } finally {
    loading.value = false
  }
}


const loadPlanningData = async () => {
  if (currentView.value === 'planning') {
    try {
      await generateUnifiedPlanning()
      // const response = await fetch(`/api/frontend/employees/planning-data?date=${selectedDate.value}`) // ✅ Nouveau endpoint
      // if (response.ok) {
      //   const data = await response.json()
      //   employees.value = data.employees || employees.value
      //   console.log('✅ Planning data loaded:', data.employees?.length, 'employees')
      // }
    } catch (error) {
      console.error('Error loading planning data:', error)
    }
  }
}
const generateUnifiedPlanning = async () => {
  try {
    loading.value = true
    console.log('🔄 Generating unified planning...')

    // Afficher un message de progression
    showNotification('Generating planning, please wait...', 'info')

    const response = await fetch(`${API_BASE_URL}/api/planning/generate-unified`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        startDate: '2025-06-01',
        planningDate: selectedDate.value,
        timePerCard: 3,
        cleanFirst: true
      })
    })

    if (response.ok) {
      const result = await response.json()
      console.log('✅ Unified planning result:', result)

      if (result.success) {
        // Mettre à jour les données des employés
        if (result.employeeAssignments) {
          employees.value = result.employeeAssignments
        }

        const totalAssigned = result.totalOrdersAssigned || 0
        showNotification(`Planning generated! ${totalAssigned} orders assigned`, 'success')

      } else {
        showNotification(result.message || 'Planning generation failed', 'error')
      }
    } else {
      throw new Error(`HTTP ${response.status}`)
    }

  } catch (error) {
    console.error('❌ Error generating unified planning:', error)
    showNotification('Error generating unified planning. Please try again.', 'error')
  } finally {
    loading.value = false
  }
}


const addEmployee = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/employees`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newEmployee.value)
    })

    if (response.ok) {
      newEmployee.value = { firstName: '', lastName: '', email: '' }
      showAddForm.value = false
      await loadEmployees()
    }
  } catch (error) {
    console.error('Error adding employee:', error)
  }
}

const handleEmployeeBack = () => {
  console.log('🔙 Handling back from employee detail')

  // ✅ CRITICAL: Reset to management view and clear selection
  currentView.value = 'management'  // NOT 'planning'!
  selectedEmployeeId.value = null

  // Reload employees in normal format (not workload format)
  loadEmployees()
}


const viewEmployee = (employeeId: string) => {
  console.log('👁️ Viewing employee details:', employeeId)
  selectedEmployeeId.value = employeeId
  // Stay in management view for detail viewing
}


const viewEmployeePlanning = async (employeeId: string) => {
  console.log('📋 Viewing employee planning:', employeeId)

  try {
    // Set to planning mode and select employee
    currentView.value = 'planning'
    selectedEmployeeId.value = employeeId

    // Load planning data if needed
    await loadEmployeePlannings(employeeId)

  } catch (error) {
    console.error('❌ Error viewing employee planning:', error)
    if (showNotification) {
      showNotification('Error loading employee planning', 'error')
    }
  }
}
const loadEmployeePlannings = async (employeeId: string) => {
  try {
    loading.value = true
    console.log('📋 Loading plannings for employee:', employeeId, 'for date:', selectedDate.value)

    const response = await fetch(`${API_BASE_URL}/api/planning/employee/${employeeId}?date=${selectedDate.value}`)

    if (response.ok) {
      const data = await response.json()
      console.log('✅ Employee plannings loaded:', data)

      // Update the display with employee plannings
      const employee = employees.value.find(emp => emp.id === employeeId)
      if (employee) {
        employee.plannings = data.plannings || []
        employee.assignedOrders = data.orders || []
        employee.totalWorkload = data.totalMinutes || 0
      }

      const orderCount = data.orders?.length || 0

      // ✅ FIX: Only check other dates if not already checking
      if (orderCount === 0 && !isCheckingOtherDates.value) {
        await checkForOrdersOnOtherDates(employeeId)
      } else if (orderCount > 0) {
        showNotification(`✅ Loaded ${orderCount} orders for employee on ${selectedDate.value}`, 'success')
      } else if (isCheckingOtherDates.value) {
        // Already checked, no orders found anywhere
        showNotification('No orders found for this employee on any date. Generate planning first.', 'info')
      }

    } else if (response.status === 404) {
      console.log('ℹ️ No plannings found for employee:', employeeId, 'on date:', selectedDate.value)

      // ✅ FIX: Only check other dates if not already checking
      if (!isCheckingOtherDates.value) {
        await checkForOrdersOnOtherDates(employeeId)
      } else {
        showNotification('No orders found for this employee on any date. Generate planning first.', 'info')
      }
    } else {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

  } catch (error) {
    console.error('❌ Error loading employee plannings:', error)
    showNotification('Error loading employee planning. Please try again.', 'error')
  } finally {
    loading.value = false
  }
}


// ========== 3. ADD FUNCTION TO CHECK OTHER DATES ==========

// Dans Employees.vue, section <script setup>
// Remplacez COMPLÈTEMENT la fonction checkForOrdersOnOtherDates par celle-ci :

const checkForOrdersOnOtherDates = async (employeeId: string) => {
  // Protection contre récursion
  if (isCheckingOtherDates.value) {
    console.log('⚠️ Already checking other dates, skipping...')
    return
  }

  try {
    isCheckingOtherDates.value = true
    console.log('🔍 Checking for orders on other dates for employee:', employeeId)

    // Chercher sans filtre de date
    const response = await fetch(`${API_BASE_URL}/api/planning/employee/${employeeId}`)

    if (response.ok) {
      const data = await response.json()
      const totalOrders = data.orders?.length || 0

      if (totalOrders > 0) {
        // Trouver les dates avec des commandes
        const orderDates = [...new Set(data.orders.map((order: any) => order.planningDate))]
          .sort()

        console.log('📅 Found orders on dates:', orderDates)

        if (orderDates.length > 0) {
          const firstDate = orderDates[0]
          console.log(`🎯 Auto-selecting first available date: ${firstDate}`)

          showNotification(
            `Found ${totalOrders} orders. Auto-loading date: ${firstDate}`,
            'info'
          )

          // ✅ Changer seulement la date - la key fera le reste
          selectedDate.value = firstDate
        }
      } else {
        console.log('ℹ️ No orders found on any date')
        showNotification(
          'No orders found for this employee on any date. Generate planning first.',
          'info'
        )
      }
    }
  } catch (error) {
    console.error('❌ Error checking other dates:', error)
  } finally {
    // ✅ Toujours réinitialiser le flag
    isCheckingOtherDates.value = false
  }
}


const viewEmployeeOrders = (employeeId: string) => {
  selectedEmployeeId.value = employeeId
}

const refreshEmployees = () => {
  loadEmployees()
  if (currentView.value === 'planning') {
    loadPlanningData()
  }
}

// Helper functions
const getInitials = (employee: Employee) => {
  return `${employee.firstName?.charAt(0) || ''}${employee.lastName?.charAt(0) || ''}`
}

const getWorkloadStatus = (workload: number) => {
  if (workload < 0.5) return '🟢 Available'
  if (workload < 0.8) return '🟡 Moderate'
  if (workload < 1.0) return '🟠 Busy'
  return '🔴 Overloaded'
}

const getWorkloadColor = (workload: number) => {
  if (workload < 0.5) return 'bg-green-100 text-green-800'
  if (workload < 0.8) return 'bg-yellow-100 text-yellow-800'
  if (workload < 1.0) return 'bg-orange-100 text-orange-800'
  return 'bg-red-100 text-red-800'
}

const getWorkloadProgressColor = (workload: number) => {
  if (workload < 0.8) return 'bg-green-500'
  if (workload < 1.0) return 'bg-yellow-500'
  return 'bg-red-500'
}

const showNotification = (message: string, type: 'success' | 'error' | 'info' = 'success') => {
  console.log(`${type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️'} ${message}`)

  // Créer une notification temporaire (optionnel)
  if (typeof window !== 'undefined') {
    const notification = document.createElement('div')
    notification.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 12px 20px;
      border-radius: 8px;
      color: white;
      font-weight: 500;
      z-index: 9999;
      max-width: 400px;
      background: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : '#3b82f6'};
    `
    notification.textContent = message
    document.body.appendChild(notification)

    // Supprimer après 3 secondes
    setTimeout(() => {
      if (notification.parentNode) {
        notification.parentNode.removeChild(notification)
      }
    }, 3000)
  }
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
  @apply bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors;
}

.btn-secondary {
  @apply bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400 transition-colors;
}

.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500;
}

.transition-colors {
  transition: background-color 0.2s, color 0.2s;
}

.transition-shadow {
  transition: box-shadow 0.2s ease;
}
</style>
