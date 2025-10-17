<template>
  <div class="team-overview">
    <!-- Header -->
    <div class="bg-white rounded-lg shadow-md p-6 mb-6">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="text-2xl font-bold text-gray-900">Teams Overview</h2>
          <p class="text-gray-600 mt-1">Manage employee teams and permissions</p>
        </div>
      </div>
    </div>

    <!-- Statistics Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
      <!-- Total Teams -->
      <div class="card">
        <div class="flex items-center">
          <div class="bg-blue-500 rounded-lg p-3 mr-4">
            <Users class="w-6 h-6 text-white" />
          </div>
          <div>
            <p class="text-sm text-gray-600">Total Teams</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.totalTeams }}</p>
          </div>
        </div>
      </div>

      <!-- Admin Teams -->
      <div class="card">
        <div class="flex items-center">
          <div class="bg-red-500 rounded-lg p-3 mr-4">
            <Crown class="w-6 h-6 text-white" />
          </div>
          <div>
            <p class="text-sm text-gray-600">Admin Teams</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.adminTeams }}</p>
          </div>
        </div>
      </div>

      <!-- Manager Teams -->
      <div class="card">
        <div class="flex items-center">
          <div class="bg-orange-500 rounded-lg p-3 mr-4">
            <Shield class="w-6 h-6 text-white" />
          </div>
          <div>
            <p class="text-sm text-gray-600">Manager Teams</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.managerTeams }}</p>
          </div>
        </div>
      </div>

      <!-- Total Members -->
      <div class="card">
        <div class="flex items-center">
          <div class="bg-green-500 rounded-lg p-3 mr-4">
            <Users class="w-6 h-6 text-white" />
          </div>
          <div>
            <p class="text-sm text-gray-600">Total Members</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.totalMembers }}</p>
          </div>
        </div>
      </div>

      <!-- Empty Teams -->
      <div class="card">
        <div class="flex items-center">
          <div class="bg-yellow-500 rounded-lg p-3 mr-4">
            <AlertTriangle class="w-6 h-6 text-white" />
          </div>
          <div>
            <p class="text-sm text-gray-600">Empty Teams</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.emptyTeams }}</p>
          </div>
        </div>
      </div>

      <!-- Average Permission -->
      <div class="card">
        <div class="flex items-center">
          <div class="bg-purple-500 rounded-lg p-3 mr-4">
            <Shield class="w-6 h-6 text-white" />
          </div>
          <div>
            <p class="text-sm text-gray-600">Avg Permission</p>
            <p class="text-2xl font-semibold text-gray-900">{{ stats.averagePermissionLevel.toFixed(1) }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Actions -->
    <div class="bg-white rounded-lg shadow-md p-6">
      <h3 class="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <button
          @click="$emit('view-teams')"
          class="p-4 border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-left"
        >
          <div class="flex items-center space-x-3 mb-2">
            <Users class="w-6 h-6 text-blue-600" />
            <span class="font-medium text-gray-900">Manage Teams</span>
          </div>
          <p class="text-sm text-gray-600">Create, edit, and organize teams</p>
        </button>

        <button
          @click="$emit('view-employees')"
          class="p-4 border border-green-200 rounded-lg hover:bg-green-50 transition-colors text-left"
        >
          <div class="flex items-center space-x-3 mb-2">
            <UserPlus class="w-6 h-6 text-green-600" />
            <span class="font-medium text-gray-900">Assign Roles</span>
          </div>
          <p class="text-sm text-gray-600">Assign employees to teams</p>
        </button>

        <button
          @click="initializeDefaultTeams"
          :disabled="loading"
          class="p-4 border border-purple-200 rounded-lg hover:bg-purple-50 transition-colors text-left disabled:opacity-50"
        >
          <div class="flex items-center space-x-3 mb-2">
            <Settings class="w-6 h-6 text-purple-600" />
            <span class="font-medium text-gray-900">Setup Defaults</span>
          </div>
          <p class="text-sm text-gray-600">Initialize standard teams</p>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  Users,
  Crown,
  Shield,
  UserPlus,
  Settings,
  AlertTriangle
} from 'lucide-vue-next'

import { API_BASE_URL } from '@/config/api.ts'

// ========== INTERFACES ==========
interface TeamStats {
  totalTeams: number
  adminTeams: number
  managerTeams: number
  totalMembers: number
  emptyTeams: number
  averagePermissionLevel: number
}

// ========== PROPS & EMITS ==========
const props = defineProps<{
  stats: TeamStats
}>()

const emit = defineEmits<{
  'view-teams': []
  'view-employees': []
}>()

// ========== STATE ==========
const loading = ref(false)

// ========== METHODS ==========
const initializeDefaultTeams = async () => {
  if (loading.value) return

  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/teams/initialize-defaults`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    if (response.ok) {
      // Success - teams initialized
      console.log('Default teams initialized successfully')
    } else {
      console.error('Failed to initialize default teams')
    }
  } catch (error) {
    console.error('Error initializing default teams:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.card {
  @apply bg-white rounded-lg shadow-md p-4;
}

.team-overview {
  @apply space-y-6;
}
</style>
