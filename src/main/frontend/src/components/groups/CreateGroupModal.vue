<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
      <!-- Header -->
      <div class="px-6 py-4 border-b border-gray-200">
        <div class="flex justify-between items-center">
          <h2 class="text-lg font-semibold text-gray-900">Create New Group</h2>
          <button
            @click="$emit('close')"
            class="text-gray-400 hover:text-gray-600"
          >
            <X class="w-6 h-6" />
          </button>
        </div>
      </div>

      <!-- Form -->
      <form @submit.prevent="createGroup" class="px-6 py-4">
        <!-- Team Name -->
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-2">
            Group Name <span class="text-red-500">*</span>
          </label>
          <input
            v-model="formData.name"
            type="text"
            placeholder="e.g., CARD_PROCESSORS"
            class="input-field"
            :class="{ 'border-red-500': errors.name }"
            @input="validateName"
            required
          >
          <p v-if="errors.name" class="text-red-500 text-sm mt-1">{{ errors.name }}</p>
          <p v-if="nameValidation.suggested" class="text-blue-600 text-sm mt-1">
            💡 Suggested format: <span class="font-mono bg-blue-50 px-1 rounded">{{ nameValidation.suggested }}</span>
          </p>
        </div>

        <!-- Description -->
        <div class="mb-4">
          <label class="block text-sm font-medium text-gray-700 mb-2">
            Description
          </label>
          <textarea
            v-model="formData.description"
            placeholder="Describe the role and responsibilities of this team..."
            rows="3"
            class="input-field resize-none"
            maxlength="255"
          ></textarea>
          <p class="text-gray-500 text-sm mt-1">{{ formData.description.length }}/255 characters</p>
        </div>

        <!-- Permission Level -->
        <div class="mb-6">
          <label class="block text-sm font-medium text-gray-700 mb-2">
            Permission Level <span class="text-red-500">*</span>
          </label>
          <select
            v-model="formData.permissionLevel"
            class="input-field"
            required
          >
            <option v-for="level in permissionLevels" :key="level.level" :value="level.level">
              Level {{ level.level }} - {{ level.name }}
            </option>
          </select>

          <!-- Permission Level Info -->
          <div v-if="selectedPermissionInfo" class="mt-3 p-3 rounded-lg bg-gray-50">
            <div class="flex items-center mb-2">
              <div :class="[
                'w-8 h-8 rounded-full flex items-center justify-center text-white font-bold text-sm mr-3',
                getPermissionLevelBgColor(selectedPermissionInfo.level)
              ]">
                {{ selectedPermissionInfo.level }}
              </div>
              <div>
                <p class="font-medium text-gray-900">{{ selectedPermissionInfo.name }}</p>
                <p class="text-sm text-gray-600">{{ selectedPermissionInfo.description }}</p>
              </div>
            </div>

            <!-- Permission Examples -->
            <div class="text-xs text-gray-500 space-y-1">
              <p><strong>Can do:</strong> {{ getPermissionExamples(selectedPermissionInfo.level) }}</p>
            </div>
          </div>
        </div>

        <!-- Preview -->
        <div v-if="formData.name" class="mb-6 p-4 border border-gray-200 rounded-lg bg-gray-50">
          <h3 class="text-sm font-medium text-gray-700 mb-2">👁️ Preview</h3>
          <div class="flex items-center space-x-3">
            <div :class="[
              'w-12 h-12 rounded-full flex items-center justify-center text-white font-bold',
              getPermissionLevelBgColor(formData.permissionLevel)
            ]">
              {{ getGroupIcon(formData.permissionLevel) }}
            </div>
            <div>
              <p class="font-semibold text-gray-900">{{ formData.name }}</p>
              <p class="text-sm text-gray-600">{{ formData.description || 'No description' }}</p>
              <span :class="[
                'inline-flex items-center px-2 py-1 rounded-full text-xs font-medium mt-1',
                getPermissionBadgeColor(formData.permissionLevel)
              ]">
                <Shield class="w-3 h-3 mr-1" />
                Level {{ formData.permissionLevel }} - {{ selectedPermissionInfo?.name }}
              </span>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex justify-end space-x-3">
          <button
            type="button"
            @click="$emit('close')"
            class="btn-secondary"
          >
            Cancel
          </button>
          <button
            type="submit"
            :disabled="!isFormValid || loading"
            class="btn-primary"
            :class="{ 'opacity-50 cursor-not-allowed': !isFormValid || loading }"
          >
            <span v-if="loading">⏳</span>
            <Plus v-else class="w-4 h-4 mr-2" />
            {{ loading ? 'Creating...' : 'Create Team' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { X, Plus, Shield } from 'lucide-vue-next'

import { API_BASE_URL } from '@/config/api.ts'

// ========== PROPS & EMITS ==========
interface PermissionLevelInfo {
  level: number
  name: string
  description: string
  colorCode: string
}

defineProps<{
  permissionLevels: PermissionLevelInfo[]
}>()

const emit = defineEmits<{
  close: []
  created: []
}>()

// ========== STATE ==========
const loading = ref(false)
const formData = ref({
  name: '',
  description: '',
  permissionLevel: 3
})

const errors = ref({
  name: ''
})

const nameValidation = ref({
  isValid: false,
  suggested: ''
})

// ========== COMPUTED ==========
const selectedPermissionInfo = computed(() => {
  return props.permissionLevels.find(p => p.level === formData.value.permissionLevel)
})

const isFormValid = computed(() => {
  return formData.value.name.trim() !== '' &&
    nameValidation.value.isValid &&
    !errors.value.name
})

// ========== METHODS ==========
const validateName = () => {
  const name = formData.value.name.trim()

  if (!name) {
    errors.value.name = 'Team name is required'
    nameValidation.value.isValid = false
    nameValidation.value.suggested = ''
    return
  }

  // Check format: uppercase with underscores
  const validFormat = /^[A-Z_][A-Z0-9_]*$/.test(name)

  if (!validFormat) {
    errors.value.name = 'Team name must be uppercase with underscores only (A-Z, 0-9, _)'
    nameValidation.value.isValid = false

    // Suggest corrected format
    const suggested = name
      .toUpperCase()
      .replace(/[^A-Z0-9_]/g, '_')
      .replace(/_{2,}/g, '_')
      .replace(/^_+|_+$/g, '')

    nameValidation.value.suggested = suggested || 'EXAMPLE_GROUP_NAME'
  } else {
    errors.value.name = ''
    nameValidation.value.isValid = true
    nameValidation.value.suggested = ''
  }
}

const applySuggestion = () => {
  if (nameValidation.value.suggested) {
    formData.value.name = nameValidation.value.suggested
    validateName()
  }
}

const createGroup = async () => {
  if (!isFormValid.value) return

  loading.value = true
  try {
    const response = await fetch(`${API_BASE_URL}/api/v2/groups`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData.value)
    })

    if (response.ok) {
      emit('created')
    } else {
      const error = await response.json()
      if (error.suggestedName) {
        nameValidation.value.suggested = error.suggestedName
        errors.value.name = error.error
      } else {
        errors.value.name = error.error || 'Error creating team'
      }
    }
  } catch (error) {
    errors.value.name = 'Network error. Please try again.'
  } finally {
    loading.value = false
  }
}

// ========== UTILITY FUNCTIONS ==========
const getPermissionLevelBgColor = (level: number) => {
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

const getGroupIcon = (level: number) => {
  if (level >= 8) return '👑'
  if (level >= 5) return '🛡️'
  if (level >= 3) return '⚙️'
  return '👀'
}

const getPermissionExamples = (level: number) => {
  if (level >= 8) return 'Full system administration, user management, system configuration'
  if (level >= 5) return 'Team management, planning oversight, employee assignments'
  if (level >= 3) return 'Process orders, update card status, view assigned work'
  return 'View reports, read-only access to system information'
}

// ========== WATCHERS ==========
watch(() => formData.value.name, validateName)
</script>

<style scoped>
.input-field {
  @apply w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500;
}

.btn-primary {
  @apply bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center;
}

.btn-secondary {
  @apply bg-gray-300 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-400 transition-colors;
}
</style>
