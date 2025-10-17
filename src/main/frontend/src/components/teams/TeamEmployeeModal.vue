<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <!-- Header -->
      <div class="modal-header">
        <h3 class="modal-title">Manage Team Members</h3>
        <button @click="$emit('close')" class="close-button">×</button>
      </div>

      <!-- Team Info -->
      <div class="team-info">
        <h4 class="team-name">{{ team.name }}</h4>
        <p class="team-description">{{ team.description }}</p>
        <div class="team-meta">
          <span class="badge">Permission Level: {{ team.permissionLevel }}</span>
          <span class="badge">{{ currentMembers.length }} members</span>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>Loading employees...</p>
      </div>

      <!-- Error State -->
      <div v-if="error" class="error-message">
        {{ error }}
      </div>

      <!-- Employee List -->
      <div v-else class="employee-list">
        <div class="list-header">
          <h5>Available Employees ({{ availableEmployees.length }})</h5>
          <input
            v-model="searchQuery"
            type="text"
            placeholder="Search employees..."
            class="search-input"
          />
        </div>

        <div class="employees-grid">
          <div
            v-for="employee in filteredEmployees"
            :key="employee.id"
            class="employee-card"
            :class="{ assigned: isAssigned(employee.id) }"
          >
            <div class="employee-info">
              <div class="employee-avatar">
                {{ getInitials(employee.firstName, employee.lastName) }}
              </div>
              <div class="employee-details">
                <p class="employee-name">{{ employee.firstName }} {{ employee.lastName }}</p>
                <p class="employee-email">{{ employee.email }}</p>
              </div>
            </div>

            <button
              v-if="isAssigned(employee.id)"
              @click="removeEmployee(employee.id)"
              :disabled="processing"
              class="btn-remove"
            >
              Remove
            </button>
            <button
              v-else
              @click="addEmployee(employee.id)"
              :disabled="processing"
              class="btn-add"
            >
              Add
            </button>
          </div>
        </div>

        <!-- Empty State -->
        <div v-if="filteredEmployees.length === 0" class="empty-state">
          <p>No employees found</p>
        </div>
      </div>

      <!-- Footer -->
      <div class="modal-footer">
        <button @click="$emit('close')" class="btn-secondary">
          Close
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

const props = defineProps<{
  team: any
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'updated'): void
}>()

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// State
const loading = ref(true)
const processing = ref(false)
const error = ref('')
const searchQuery = ref('')
const availableEmployees = ref<any[]>([])
const currentMembers = ref<string[]>([])

// Computed
const filteredEmployees = computed(() => {
  if (!searchQuery.value) return availableEmployees.value

  const query = searchQuery.value.toLowerCase()
  return availableEmployees.value.filter(emp =>
    emp.firstName.toLowerCase().includes(query) ||
    emp.lastName.toLowerCase().includes(query) ||
    emp.email.toLowerCase().includes(query)
  )
})

// Methods
function isAssigned(employeeId: string): boolean {
  return currentMembers.value.includes(employeeId)
}

function getInitials(firstName: string, lastName: string): string {
  return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase()
}

async function loadData() {
  loading.value = true
  error.value = ''

  try {
    // Load all employees
    const empResponse = await fetch(`${API_BASE_URL}/employees`)
    if (!empResponse.ok) throw new Error('Failed to load employees')

    const empData = await empResponse.json()
    availableEmployees.value = Array.isArray(empData) ? empData : empData.employees || []

    // Load current team members (from team.employees)
    currentMembers.value = (props.team.employees || []).map((emp: any) => emp.id)

    console.log('✅ Loaded:', availableEmployees.value.length, 'employees,', currentMembers.value.length, 'assigned')
  } catch (e: any) {
    console.error('❌ Load error:', e)
    error.value = 'Failed to load data: ' + e.message
  } finally {
    loading.value = false
  }
}

async function addEmployee(employeeId: string) {
  processing.value = true
  error.value = ''

  try {
    console.log('➕ Adding employee', employeeId, 'to team', props.team.id)

    const response = await fetch(
      `${API_BASE_URL}/teams/assign?employeeId=${employeeId}&teamId=${props.team.id}`,
      { method: 'POST' }
    )

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.error || 'Failed to assign employee')
    }

    // Update local state
    currentMembers.value.push(employeeId)

    console.log('✅ Employee added successfully')
    emit('updated')

  } catch (e: any) {
    console.error('❌ Add error:', e)
    error.value = 'Failed to add employee: ' + e.message
  } finally {
    processing.value = false
  }
}

async function removeEmployee(employeeId: string) {
  processing.value = true
  error.value = ''

  try {
    console.log('➖ Removing employee', employeeId, 'from team', props.team.id)

    const response = await fetch(
      `${API_BASE_URL}/teams/remove?employeeId=${employeeId}&teamId=${props.team.id}`,
      { method: 'DELETE' }
    )

    if (!response.ok) {
      const errorData = await response.json()
      throw new Error(errorData.error || 'Failed to remove employee')
    }

    // Update local state
    currentMembers.value = currentMembers.value.filter(id => id !== employeeId)

    console.log('✅ Employee removed successfully')
    emit('updated')

  } catch (e: any) {
    console.error('❌ Remove error:', e)
    error.value = 'Failed to remove employee: ' + e.message
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background: white;
  border-radius: 0.5rem;
  width: 100%;
  max-width: 800px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid #e5e7eb;
}

.modal-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.close-button {
  width: 2rem;
  height: 2rem;
  border: none;
  background: none;
  font-size: 2rem;
  line-height: 1;
  cursor: pointer;
  color: #6b7280;
  transition: color 0.2s;
}

.close-button:hover {
  color: #111827;
}

.team-info {
  padding: 1.5rem;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.team-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: #111827;
  margin: 0 0 0.5rem 0;
}

.team-description {
  color: #6b7280;
  margin: 0 0 0.75rem 0;
}

.team-meta {
  display: flex;
  gap: 0.5rem;
}

.badge {
  padding: 0.25rem 0.75rem;
  background: #e5e7eb;
  color: #374151;
  border-radius: 9999px;
  font-size: 0.875rem;
}

.loading-state {
  padding: 3rem;
  text-align: center;
  color: #6b7280;
}

.spinner {
  width: 3rem;
  height: 3rem;
  border: 3px solid #e5e7eb;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-message {
  padding: 1rem;
  margin: 1rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 0.375rem;
  color: #991b1b;
}

.employee-list {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.list-header h5 {
  font-size: 1rem;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.search-input {
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  width: 200px;
}

.search-input:focus {
  outline: none;
  border-color: #3b82f6;
  ring: 2px solid #93c5fd;
}

.employees-grid {
  display: grid;
  gap: 0.75rem;
}

.employee-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  transition: all 0.2s;
}

.employee-card:hover {
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.employee-card.assigned {
  background: #eff6ff;
  border-color: #3b82f6;
}

.employee-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex: 1;
}

.employee-avatar {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  background: #3b82f6;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
}

.employee-details {
  flex: 1;
}

.employee-name {
  font-weight: 600;
  color: #111827;
  margin: 0 0 0.25rem 0;
}

.employee-email {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0;
}

.btn-add,
.btn-remove {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 0.375rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-add {
  background: #3b82f6;
  color: white;
}

.btn-add:hover:not(:disabled) {
  background: #2563eb;
}

.btn-remove {
  background: #ef4444;
  color: white;
}

.btn-remove:hover:not(:disabled) {
  background: #dc2626;
}

.btn-add:disabled,
.btn-remove:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-state {
  padding: 3rem;
  text-align: center;
  color: #6b7280;
}

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
}

.btn-secondary {
  padding: 0.5rem 1rem;
  border: 1px solid #d1d5db;
  background: white;
  color: #374151;
  border-radius: 0.375rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-secondary:hover {
  background: #f9fafb;
}
</style>
