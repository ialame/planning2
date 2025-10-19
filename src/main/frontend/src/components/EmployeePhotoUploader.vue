<template>
  <div class="photo-uploader">
    <!-- Photo Display -->
    <div class="photo-container">
      <div v-if="photoUrl" class="photo-wrapper">
        <img :src="photoUrl" alt="Employee Photo" class="photo-image" />
        <div class="photo-overlay">
          <button @click="triggerFileInput" class="overlay-button" title="Change Photo">
            📷
          </button>
          <button @click="deletePhoto" class="overlay-button delete" title="Delete Photo">
            🗑️
          </button>
        </div>
      </div>

      <div v-else class="photo-placeholder" @click="triggerFileInput">
        <div class="placeholder-content">
          <span class="placeholder-icon">👤</span>
          <span class="placeholder-text">Add Photo</span>
        </div>
      </div>
    </div>

    <!-- Hidden File Input -->
    <input
      ref="fileInput"
      type="file"
      accept="image/*"
      @change="handleFileSelect"
      style="display: none"
    />

    <!-- Upload Progress -->
    <div v-if="uploading" class="upload-progress">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
      </div>
      <p class="progress-text">Uploading... {{ uploadProgress }}%</p>
    </div>

    <!-- Error Message -->
    <div v-if="error" class="error-message">
      ⚠️ {{ error }}
    </div>

    <!-- Success Message -->
    <div v-if="success" class="success-message">
      ✅ {{ success }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

interface Props {
  employeeId: string
  size?: 'small' | 'medium' | 'large'
  editable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium',
  editable: true
})

const emit = defineEmits<{
  photoUpdated: []
  photoDeleted: []
}>()

// State
const photoUrl = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const error = ref<string | null>(null)
const success = ref<string | null>(null)

// Load photo on mount
onMounted(() => {
  loadPhoto()
})

// Reload photo when employeeId changes
watch(() => props.employeeId, () => {
  loadPhoto()
})

/**
 * Load employee photo from backend
 */
const loadPhoto = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/api/employees/${props.employeeId}/photo/exists`)

    if (response.ok) {
      const data = await response.json()

      if (data.hasPhoto) {
        // Add timestamp to prevent caching
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

/**
 * Trigger file input click
 */
const triggerFileInput = () => {
  if (props.editable && fileInput.value) {
    fileInput.value.click()
  }
}

/**
 * Handle file selection
 */
const handleFileSelect = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  // Validate file
  if (!file.type.startsWith('image/')) {
    showError('Please select an image file')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    showError('File size must be less than 5MB')
    return
  }

  await uploadPhoto(file)
}

/**
 * Upload photo to backend
 */
const uploadPhoto = async (file: File) => {
  uploading.value = true
  uploadProgress.value = 0
  error.value = null
  success.value = null

  try {
    const formData = new FormData()
    formData.append('file', file)

    const xhr = new XMLHttpRequest()

    // Track upload progress
    xhr.upload.addEventListener('progress', (e) => {
      if (e.lengthComputable) {
        uploadProgress.value = Math.round((e.loaded / e.total) * 100)
      }
    })

    // Handle completion
    xhr.addEventListener('load', () => {
      if (xhr.status === 200) {
        const response = JSON.parse(xhr.responseText)
        if (response.success) {
          showSuccess('Photo uploaded successfully!')
          loadPhoto()
          emit('photoUpdated')
        } else {
          showError(response.error || 'Upload failed')
        }
      } else {
        showError('Upload failed. Please try again.')
      }
      uploading.value = false
    })

    // Handle errors
    xhr.addEventListener('error', () => {
      showError('Upload failed. Please check your connection.')
      uploading.value = false
    })

    // Send request
    xhr.open('POST', `${API_BASE_URL}/api/employees/${props.employeeId}/photo`)
    xhr.send(formData)

  } catch (err) {
    console.error('Upload error:', err)
    showError('Upload failed. Please try again.')
    uploading.value = false
  }
}

/**
 * Delete photo
 */
const deletePhoto = async () => {
  if (!confirm('Are you sure you want to delete this photo?')) {
    return
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/employees/${props.employeeId}/photo`, {
      method: 'DELETE'
    })

    if (response.ok) {
      photoUrl.value = null
      showSuccess('Photo deleted successfully')
      emit('photoDeleted')
    } else {
      showError('Failed to delete photo')
    }
  } catch (err) {
    console.error('Delete error:', err)
    showError('Failed to delete photo')
  }
}

/**
 * Show error message
 */
const showError = (message: string) => {
  error.value = message
  setTimeout(() => {
    error.value = null
  }, 5000)
}

/**
 * Show success message
 */
const showSuccess = (message: string) => {
  success.value = message
  setTimeout(() => {
    success.value = null
  }, 3000)
}
</script>

<style scoped>
.photo-uploader {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.photo-container {
  position: relative;
  width: 200px;
  height: 200px;
}

.photo-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.photo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.photo-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  opacity: 0;
  transition: opacity 0.3s;
}

.photo-wrapper:hover .photo-overlay {
  opacity: 1;
}

.overlay-button {
  background: white;
  border: none;
  border-radius: 50%;
  width: 48px;
  height: 48px;
  font-size: 24px;
  cursor: pointer;
  transition: transform 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.overlay-button:hover {
  transform: scale(1.1);
}

.overlay-button.delete {
  background: #ef4444;
}

.photo-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.3s;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.photo-placeholder:hover {
  transform: scale(1.05);
}

.placeholder-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: white;
}

.placeholder-icon {
  font-size: 64px;
}

.placeholder-text {
  font-size: 14px;
  font-weight: 600;
}

.upload-progress {
  width: 100%;
  max-width: 300px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #e5e7eb;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea, #764ba2);
  transition: width 0.3s;
}

.progress-text {
  margin-top: 8px;
  font-size: 14px;
  color: #6b7280;
  text-align: center;
}

.error-message {
  padding: 12px 16px;
  background: #fee2e2;
  color: #991b1b;
  border-radius: 8px;
  font-size: 14px;
  text-align: center;
}

.success-message {
  padding: 12px 16px;
  background: #d1fae5;
  color: #065f46;
  border-radius: 8px;
  font-size: 14px;
  text-align: center;
}
</style>
