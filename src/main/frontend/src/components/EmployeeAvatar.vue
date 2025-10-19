<template>
  <div class="employee-avatar" :class="sizeClass" :style="customStyle">
    <img
      v-if="photoUrl && !imageError"
      :src="photoUrl"
      :alt="props.employeeName"
      class="avatar-image"
      @error="handleImageError"
    />
    <div v-else class="avatar-initials">
      {{ initials }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

interface Props {
  employeeId: string
  employeeName: string
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  customSize?: string
}

const props = withDefaults(defineProps<Props>(), {
  employeeName: '',
  size: 'md'
})

const photoUrl = ref<string | null>(null)
const imageError = ref(false)

// Size classes mapping
const sizeClass = computed(() => `size-${props.size}`)

const customStyle = computed(() => {
  if (props.customSize) {
    return {
      width: props.customSize,
      height: props.customSize
    }
  }
  return {}
})

// Calculate initials from employee name
const initials = computed(() => {
  if (!props.employeeName) return '?'

  const names = props.employeeName.trim().split(' ')
  if (names.length >= 2) {
    return (names[0][0] + names[names.length - 1][0]).toUpperCase()
  }
  return props.employeeName.substring(0, 2).toUpperCase()
})

// Load photo - FIXED to get photoUrl from JSON response
const loadPhoto = async () => {
  if (imageError.value) return

  try {
    // First check if photo exists
    const existsResponse = await fetch(
      `${API_BASE_URL}/api/employees/${props.employeeId}/photo/exists`
    )

    if (!existsResponse.ok) {
      photoUrl.value = null
      return
    }

    const existsData = await existsResponse.json()

    if (!existsData.hasPhoto) {
      photoUrl.value = null
      return
    }

    // Get the photo URL from the JSON response
    const photoResponse = await fetch(
      `${API_BASE_URL}/api/employees/${props.employeeId}/photo`
    )

    if (photoResponse.ok) {
      const data = await photoResponse.json()
      // Use the photoUrl from the response
      photoUrl.value = data.photoUrl
      console.log(`✅ Loaded photo URL for ${props.employeeName}:`, data.photoUrl?.substring(0, 50) + '...')
    } else {
      photoUrl.value = null
    }
  } catch (err) {
    console.error('Error loading photo:', err)
    photoUrl.value = null
  }
}

// Handle image loading error
const handleImageError = () => {
  console.warn(`❌ Failed to load image for ${props.employeeName}`)
  imageError.value = true
  photoUrl.value = null
}

// Load on mount
onMounted(() => {
  loadPhoto()
})

// Expose reload method for parent components
defineExpose({
  reload: loadPhoto
})

// Reload when employee ID changes
watch(() => props.employeeId, () => {
  imageError.value = false
  photoUrl.value = null // Reset to force reload
  loadPhoto()
})

// Also watch for changes to employeeName (in case of external updates)
watch(() => props.employeeName, () => {
  if (photoUrl.value) {
    // Refresh photo if it exists
    loadPhoto()
  }
})
</script>

<style scoped>
.employee-avatar {
  position: relative;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* Size variants */
.size-xs {
  width: 32px;
  height: 32px;
}

.size-sm {
  width: 48px;
  height: 48px;
}

.size-md {
  width: 64px;
  height: 64px;
}

.size-lg {
  width: 80px;
  height: 80px;
}

.size-xl {
  width: 120px;
  height: 120px;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-initials {
  color: white;
  font-weight: 600;
  user-select: none;
}

/* Font sizes for initials based on avatar size */
.size-xs .avatar-initials {
  font-size: 12px;
}

.size-sm .avatar-initials {
  font-size: 16px;
}

.size-md .avatar-initials {
  font-size: 20px;
}

.size-lg .avatar-initials {
  font-size: 28px;
}

.size-xl .avatar-initials {
  font-size: 40px;
}
</style>
