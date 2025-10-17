<template>
  <div class="teams-page">
    <GroupsNavigation @tab-change="currentTab = $event" :activeTab="currentTab" />

    <!-- Content Area -->
    <div class="teams-content">
      <!-- Teams List Tab -->
      <div v-if="currentTab === 'list'">
        <div class="bg-white rounded-lg shadow-md p-6 mb-6">
          <div class="flex items-center justify-between mb-6">
            <div>
              <h2 class="text-2xl font-bold text-gray-900">Teams Management</h2>
              <p class="text-gray-600 mt-1">Manage employee teams and permissions</p>
            </div>
            <button @click="currentTab = 'create'" class="btn-primary">
              + Create Team
            </button>
          </div>

          <!-- Loading State -->
          <div v-if="loading" class="text-center py-12">
            <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
            <p class="text-gray-600">Loading teams...</p>
          </div>

          <!-- Error State -->
          <div v-else-if="error" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p class="text-red-800">{{ error }}</p>
            <button @click="loadTeams" class="mt-2 text-sm text-red-600 hover:text-red-800">
              Try Again
            </button>
          </div>

          <!-- Empty State -->
          <div v-else-if="teams.length === 0" class="text-center py-12 bg-gray-50 rounded-lg">
            <svg class="w-16 h-16 mx-auto text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
            </svg>
            <h3 class="text-lg font-medium text-gray-900 mb-2">No Teams Yet</h3>
            <p class="text-gray-600 mb-4">Get started by creating your first team</p>
            <button @click="currentTab = 'create'" class="btn-primary">
              Create First Team
            </button>
          </div>

          <!-- Teams Grid -->
          <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div
              v-for="team in teams"
              :key="team.id"
              class="border rounded-lg p-4 hover:shadow-lg transition-shadow cursor-pointer"
              @click="viewTeam(team)"
            >
              <div class="flex items-start justify-between mb-3">
                <h3 class="text-lg font-semibold text-gray-900">{{ team.name }}</h3>
                <span
                  :class="[
                    'px-2 py-1 text-xs rounded-full',
                    team.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                  ]"
                >
                  {{ team.active ? 'Active' : 'Inactive' }}
                </span>
              </div>

              <p class="text-sm text-gray-600 mb-3">{{ team.description || 'No description' }}</p>

              <div class="flex items-center justify-between text-sm">
                <div class="flex items-center text-gray-500">
                  <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
                  </svg>
                  {{ team.employeeCount || 0 }} members
                </div>
                <button
                  @click.stop="manageTeamMembers(team)"
                  class="text-blue-600 hover:text-blue-800 font-medium"
                >
                  Manage
                </button>
              </div>

            </div>
          </div>
        </div>
      </div>

      <!-- Create Team Tab -->
      <div v-if="currentTab === 'create'">
        <div class="bg-white rounded-lg shadow-md p-6 max-w-2xl mx-auto">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">Create New Team</h2>

          <form @submit.prevent="createTeam" class="space-y-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Team Name *</label>
              <input
                v-model="newTeam.name"
                type="text"
                required
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="e.g., ROLE_NOTEUR"
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea
                v-model="newTeam.description"
                rows="3"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Describe the team's role and responsibilities"
              ></textarea>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Permission Level *</label>
              <select
                v-model="newTeam.permissionLevel"
                required
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Select level...</option>
                <option value="10">10 - Administrator</option>
                <option value="7">7 - Manager</option>
                <option value="5">5 - Grader/Certifier</option>
                <option value="4">4 - Scanner/Preparer</option>
                <option value="2">2 - Viewer</option>
              </select>
            </div>

            <div class="flex items-center">
              <input
                v-model="newTeam.active"
                type="checkbox"
                id="active"
                class="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <label for="active" class="ml-2 block text-sm text-gray-900">
                Active team
              </label>
            </div>

            <div class="flex items-center justify-end space-x-3 pt-4">
              <button
                type="button"
                @click="cancelCreate"
                class="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="submit"
                :disabled="creating"
                class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
              >
                {{ creating ? 'Creating...' : 'Create Team' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Employee Assignment Modal -->
    <TeamEmployeeModal
      v-if="showEmployeeModal && selectedTeam"
      :team="selectedTeam"
      @close="closeEmployeeModal"
      @updated="closeEmployeeModal"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import GroupsNavigation from '../components/teams/GroupsNavigation.vue'
import TeamEmployeeModal from '../components/teams/TeamEmployeeModal.vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

// State
const currentTab = ref('list')
const loading = ref(false)
const creating = ref(false)
const error = ref('')
const teams = ref<any[]>([])

const showEmployeeModal = ref(false)
const selectedTeam = ref<any>(null)

const newTeam = ref({
  name: '',
  description: '',
  permissionLevel: '',
  active: true
})

// Load teams on mount
onMounted(() => {
  loadTeams()
})

function manageTeamMembers(team: any) {
  selectedTeam.value = team
  showEmployeeModal.value = true
}

function closeEmployeeModal() {
  showEmployeeModal.value = false
  selectedTeam.value = null
  loadTeams() // Reload to get updated member counts
}


// Load teams from API
async function loadTeams() {
  loading.value = true
  error.value = ''

  try {
    const response = await fetch(`${API_BASE_URL}/teams`)

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const data = await response.json()
    // ✅ FIX: L'API retourne {teams: [...]} au lieu de [...]
    teams.value = data.teams || []
    console.log('✅ Loaded teams:', teams.value.length)
  } catch (e: any) {
    console.error('❌ Failed to load teams:', e)
    error.value = 'Failed to load teams. Please try again.'
    teams.value = []
  } finally {
    loading.value = false
  }
}

// Create new team
async function createTeam() {
  creating.value = true
  error.value = ''

  try {
    const payload = {
      name: newTeam.value.name,
      description: newTeam.value.description,
      permissionLevel: parseInt(newTeam.value.permissionLevel),
      active: newTeam.value.active
    }

    console.log('📤 Creating team:', payload)

    const response = await fetch(`${API_BASE_URL}/teams`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    })

    if (!response.ok) {
      const errorText = await response.text()
      console.error('❌ Create error:', response.status, errorText)
      throw new Error(`Failed to create team: ${response.status}`)
    }

    const result = await response.json()
    console.log('✅ Team created:', result)

    // Reset form
    newTeam.value = {
      name: '',
      description: '',
      permissionLevel: '',
      active: true
    }

    // Reload teams and switch to list tab
    await loadTeams()
    currentTab.value = 'list'

  } catch (e: any) {
    console.error('❌ Failed to create team:', e)
    error.value = `Failed to create team: ${e.message}`
  } finally {
    creating.value = false
  }
}

// Cancel create
function cancelCreate() {
  newTeam.value = {
    name: '',
    description: '',
    permissionLevel: '',
    active: true
  }
  currentTab.value = 'list'
}

// View team details (placeholder)
function viewTeam(team: any) {
  console.log('View team:', team)
  // TODO: Implement team detail view
}
</script>

<style scoped>
.teams-page {
  min-height: 100vh;
  background-color: #f9fafb;
  padding: 1.5rem;
}

.teams-content {
  max-width: 1400px;
  margin: 0 auto;
}

.btn-primary {
  padding: 0.5rem 1rem;
  background-color: #3b82f6;
  color: white;
  border-radius: 0.375rem;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn-primary:hover {
  background-color: #2563eb;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
