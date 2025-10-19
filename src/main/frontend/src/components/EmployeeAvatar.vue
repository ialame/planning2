<template>
  <div
    :class="['employee-avatar', sizeClass]"
    :style="customStyle"
  >
    <img
      v-if="photoUrl"
      :src="photoUrl"
      :alt="employeeName"
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
  employeeName?: string
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

// Load photo
const loadPhoto = async () => {
  if (imageError.value) return

  try {
    const response = await fetch(
      `${API_BASE_URL}/api/employees/${props.employeeId}/photo/exists`
    )

    if (response.ok) {
      const data = await response.json()

      if (data.hasPhoto) {
        photoUrl.value = `${API_BASE_URL}/api/employees/${props.employeeId}/photo?t=${Date.now()}`
      } else {
        photoUrl.value = null
      }
    }
  } catch (err) {
    console.error('Error loading photo:', err)
    photoUrl.value = null
  }
}

// Handle image loading error
const handleImageError = () => {
  imageError.value = true
  photoUrl.value = null
}

// Load on mount
onMounted(() => {
  loadPhoto()
})

// Reload when employee ID changes
watch(() => props.employeeId, () => {
  imageError.value = false
  loadPhoto()
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
