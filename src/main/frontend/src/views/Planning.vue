<template>
  <div class="planning-page">
    <h1 class="page-title">
      <span>📅</span>
      Global Planning
    </h1>

    <div class="controls-section">
      <div class="control-team">
        <label class="control-label">
          <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
          </svg>
          Start Date:
        </label>
        <input
          type="date"
          v-model="config.startDate"
          class="control-input"
        />
      </div>

      <div class="control-team">
        <label class="control-label">
          <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          Time/Card (min):
        </label>
        <input
          type="number"
          v-model.number="config.cardProcessingTime"
          min="1"
          max="10"
          class="control-input"
        />
      </div>

      <button @click="generatePlanning" class="generate-btn" :disabled="generating">
        <div v-if="generating" class="spinner"></div>
        <svg v-else class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
        </svg>
        {{ generating ? 'Generating...' : 'Generate Planning' }}
      </button>

      <button @click="refreshAllPanels" class="refresh-btn">
        <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
        Refresh All
      </button>
    </div>

    <!-- Four Panel View -->
    <div class="panels-container">
      <!-- Scanning Panel -->
      <div class="panel scanning-panel">
        <div class="panel-header">
          <h2 class="panel-title">
            <span class="panel-icon">📸</span>
            Scanning (A_SCANNER)
          </h2>
          <div v-if="scanningData.plannings" class="panel-stats">
            <span class="stat-badge">{{ scanningData.plannings.length }} tasks</span>
            <span class="stat-badge">{{ scanningData.summary?.totalCards || 0 }} cards</span>
            <span class="stat-badge">{{ scanningData.summary?.totalHours || 0 }}h</span>
          </div>
        </div>

        <div v-if="loadingScanning" class="panel-loading">
          <div class="spinner"></div>
          <p>Loading scanning tasks...</p>
        </div>

        <div v-else-if="!scanningData.plannings || scanningData.plannings.length === 0" class="panel-empty">
          <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
          <p>No scanning tasks scheduled</p>
        </div>

        <div v-else class="panel-content">
          <div
            v-for="planning in scanningData.plannings"
            :key="planning.id"
            :class="['planning-card', `delai-${getDelaiClass(planning.delai)}`]"
          >
            <div class="card-header">
              <span class="order-number">{{ planning.orderNumber }}</span>
              <span :class="['delai-badge', `delai-${getDelaiClass(planning.delai)}`]">
                {{ getDelaiLabel(planning.delai) }}
              </span>
            </div>
            <div class="card-body">
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                </svg>
                {{ planning.employeeName }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                </svg>
                {{ formatDate(planning.planningDate) }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                {{ formatTime(planning.startTime) }} - {{ formatTime(planning.endTime) }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343"/>
                </svg>
                {{ planning.cardCount }} cards • {{ planning.formattedDuration }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Grading Panel -->
      <div class="panel grading-panel">
        <div class="panel-header">
          <h2 class="panel-title">
            <span class="panel-icon">📝</span>
            Grading (A_NOTER)
          </h2>
          <div v-if="gradingData.plannings" class="panel-stats">
            <span class="stat-badge">{{ gradingData.plannings.length }} tasks</span>
            <span class="stat-badge">{{ gradingData.summary?.totalCards || 0 }} cards</span>
            <span class="stat-badge">{{ gradingData.summary?.totalHours || 0 }}h</span>
          </div>
        </div>

        <div v-if="loadingGrading" class="panel-loading">
          <div class="spinner"></div>
          <p>Loading grading tasks...</p>
        </div>

        <div v-else-if="!gradingData.plannings || gradingData.plannings.length === 0" class="panel-empty">
          <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2V9a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
          </svg>
          <p>No grading tasks scheduled</p>
        </div>

        <div v-else class="panel-content">
          <div
            v-for="planning in gradingData.plannings"
            :key="planning.id"
            :class="['planning-card', `delai-${getDelaiClass(planning.delai)}`]"
          >
            <div class="card-header">
              <span class="order-number">{{ planning.orderNumber }}</span>
              <span :class="['delai-badge', `delai-${getDelaiClass(planning.delai)}`]">
                {{ getDelaiLabel(planning.delai) }}
              </span>
            </div>
            <div class="card-body">
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                </svg>
                {{ planning.employeeName }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                </svg>
                {{ formatDate(planning.planningDate) }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                {{ formatTime(planning.startTime) }} - {{ formatTime(planning.endTime) }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343"/>
                </svg>
                {{ planning.cardCount }} cards • {{ planning.formattedDuration }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Certification Panel -->
      <div class="panel certification-panel">
        <div class="panel-header">
          <h2 class="panel-title">
            <span class="panel-icon">🔒</span>
            Certification (A_CERTIFIER)
          </h2>
          <div v-if="certificationData.plannings" class="panel-stats">
            <span class="stat-badge">{{ certificationData.plannings.length }} tasks</span>
            <span class="stat-badge">{{ certificationData.summary?.totalCards || 0 }} cards</span>
            <span class="stat-badge">{{ certificationData.summary?.totalHours || 0 }}h</span>
          </div>
        </div>

        <div v-if="loadingCertification" class="panel-loading">
          <div class="spinner"></div>
          <p>Loading certification tasks...</p>
        </div>

        <div v-else-if="!certificationData.plannings || certificationData.plannings.length === 0" class="panel-empty">
          <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
          </svg>
          <p>No certification tasks scheduled</p>
        </div>

        <div v-else class="panel-content">
          <div
            v-for="planning in certificationData.plannings"
            :key="planning.id"
            :class="['planning-card', `delai-${getDelaiClass(planning.delai)}`]"
          >
            <div class="card-header">
              <span class="order-number">{{ planning.orderNumber }}</span>
              <span :class="['delai-badge', `delai-${getDelaiClass(planning.delai)}`]">
                {{ getDelaiLabel(planning.delai) }}
              </span>
            </div>
            <div class="card-body">
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                </svg>
                {{ planning.employeeName }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                </svg>
                {{ formatDate(planning.planningDate) }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                {{ formatTime(planning.startTime) }} - {{ formatTime(planning.endTime) }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343"/>
                </svg>
                {{ planning.cardCount }} cards • {{ planning.formattedDuration }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Preparation Panel -->
      <div class="panel preparation-panel">
        <div class="panel-header">
          <h2 class="panel-title">
            <span class="panel-icon">📦</span>
            Preparation (A_PREPARER)
          </h2>
          <div v-if="preparationData.plannings" class="panel-stats">
            <span class="stat-badge">{{ preparationData.plannings.length }} tasks</span>
            <span class="stat-badge">{{ preparationData.summary?.totalCards || 0 }} cards</span>
            <span class="stat-badge">{{ preparationData.summary?.totalHours || 0 }}h</span>
          </div>
        </div>

        <div v-if="loadingPreparation" class="panel-loading">
          <div class="spinner"></div>
          <p>Loading preparation tasks...</p>
        </div>

        <div v-else-if="!preparationData.plannings || preparationData.plannings.length === 0" class="panel-empty">
          <svg class="empty-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/>
          </svg>
          <p>No preparation tasks scheduled</p>
        </div>

        <div v-else class="panel-content">
          <div
            v-for="planning in preparationData.plannings"
            :key="planning.id"
            :class="['planning-card', `delai-${getDelaiClass(planning.delai)}`]"
          >
            <div class="card-header">
              <span class="order-number">{{ planning.orderNumber }}</span>
              <span :class="['delai-badge', `delai-${getDelaiClass(planning.delai)}`]">
                {{ getDelaiLabel(planning.delai) }}
              </span>
            </div>
            <div class="card-body">
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                </svg>
                {{ planning.employeeName }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                </svg>
                {{ formatDate(planning.planningDate) }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
                </svg>
                {{ formatTime(planning.startTime) }} - {{ formatTime(planning.endTime) }}
              </div>
              <div class="card-info">
                <svg class="icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21a4 4 0 01-4-4V5a2 2 0 012-2h4a2 2 0 012 2v12a4 4 0 01-4 4zm0 0h12a2 2 0 002-2v-4a2 2 0 00-2-2h-2.343"/>
                </svg>
                {{ planning.cardCount }} cards • {{ planning.formattedDuration }}
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, inject } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const showNotification = inject('showNotification', (msg: string, type: string) => console.log(msg))

// State
const generating = ref(false)
const loadingGrading = ref(false)
const loadingCertification = ref(false)
const loadingPreparation = ref(false)
const loadingScanning = ref(false)

const config = ref({
  startDate: new Date().toISOString().split('T')[0],
  cardProcessingTime: 3
})

const gradingData = ref<any>({ plannings: [], summary: null })
const certificationData = ref<any>({ plannings: [], summary: null })
const preparationData = ref<any>({ plannings: [], summary: null })
const scanningData = ref<any>({ plannings: [], summary: null })

// Methods

const generatePlanning = async () => {
  if (generating.value) return

  generating.value = true

  try {
    console.log('🚀 Generating planning...')

    const response = await fetch(`${API_BASE_URL}/api/planning/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
      // ✅ L'endpoint ne prend pas de body selon le code Java
    })

    if (response.ok) {
      const result = await response.json()
      console.log('✅ Planning generated:', result)
      console.log('📊 Response structure:', {
        success: result.success,
        message: result.message,
        assignmentsCreated: result.assignmentsCreated,
        assignments: result.assignments?.length
      })

      if (result.success) {
        const tasksCount = result.assignmentsCreated || result.assignments?.length || 0
        showNotification(
          `✅ Planning generated: ${tasksCount} assignments created!`,
          'success'
        )

        // Reload all panels to show the new data
        await refreshAllPanels()
      } else {
        showNotification(`⚠️ ${result.message || 'Planning generation failed'}`, 'error')
      }
    } else {
      const errorText = await response.text()
      console.error('❌ HTTP error:', response.status, errorText)
      throw new Error(`HTTP ${response.status}: ${errorText}`)
    }
  } catch (error) {
    console.error('❌ Generation error:', error)
    showNotification(
      `❌ Failed to generate planning: ${error instanceof Error ? error.message : 'Unknown error'}`,
      'error'
    )
  } finally {
    generating.value = false
  }
}

// Replace these methods in Planning.vue

// Replace ALL the load methods in Planning.vue (around lines 60-120)

// Replace ALL the load methods in Planning.vue (around lines 60-120)

const loadGradingPlannings = async () => {
  loadingGrading.value = true
  try {
    console.log('📥 Loading grading assignments...')
    const response = await fetch(`${API_BASE_URL}/api/planning/assignments`)

    if (response.ok) {
      const assignments = await response.json()
      console.log(`✅ Total assignments: ${assignments.length}`)

      // Filter by processingStage = "GRADING"
      const gradingAssignments = assignments.filter((a: any) =>
        a.processingStage === 'GRADING'
      )

      // Transform to match the expected format for display
      const plannings = gradingAssignments.map((a: any) => ({
        id: a.id,
        orderNumber: a.orderNumber,
        employeeName: a.employeeName,
        cardCount: a.cardCount || 0,
        delai: 'F', // Default - you might want to fetch this from order
        planningDate: a.scheduledStart ? new Date(a.scheduledStart).toISOString().split('T')[0] : null,
        startTime: a.scheduledStart,
        endTime: a.scheduledEnd,
        formattedDuration: a.estimatedDurationMinutes ? `${Math.round(a.estimatedDurationMinutes / 60)}h` : '0h',
        status: a.status
      }))

      gradingData.value = {
        plannings: plannings,
        summary: {
          totalCards: plannings.reduce((sum: number, p: any) => sum + p.cardCount, 0),
          totalHours: Math.round(plannings.reduce((sum: number, p: any) =>
            sum + (p.formattedDuration ? parseInt(p.formattedDuration) : 0), 0))
        }
      }

      console.log(`✅ Grading: ${gradingData.value.plannings.length} tasks`)
    }
  } catch (error) {
    console.error('❌ Error loading grading plannings:', error)
  } finally {
    loadingGrading.value = false
  }
}

const loadCertificationPlannings = async () => {
  loadingCertification.value = true
  try {
    console.log('📥 Loading certification assignments...')
    const response = await fetch(`${API_BASE_URL}/api/planning/assignments`)

    if (response.ok) {
      const assignments = await response.json()

      // ✅ FIXED: Use "CERTIFYING" instead of "CERTIFICATION"
      const certificationAssignments = assignments.filter((a: any) =>
        a.processingStage === 'CERTIFYING'
      )

      const plannings = certificationAssignments.map((a: any) => ({
        id: a.id,
        orderNumber: a.orderNumber,
        employeeName: a.employeeName,
        cardCount: a.cardCount || 0,
        delai: 'F',
        planningDate: a.scheduledStart ? new Date(a.scheduledStart).toISOString().split('T')[0] : null,
        startTime: a.scheduledStart,
        endTime: a.scheduledEnd,
        formattedDuration: a.estimatedDurationMinutes ? `${Math.round(a.estimatedDurationMinutes / 60)}h` : '0h',
        status: a.status
      }))

      certificationData.value = {
        plannings: plannings,
        summary: {
          totalCards: plannings.reduce((sum: number, p: any) => sum + p.cardCount, 0),
          totalHours: Math.round(plannings.reduce((sum: number, p: any) =>
            sum + (p.formattedDuration ? parseInt(p.formattedDuration) : 0), 0))
        }
      }

      console.log(`✅ Certification: ${certificationData.value.plannings.length} tasks`)
    }
  } catch (error) {
    console.error('❌ Error loading certification plannings:', error)
  } finally {
    loadingCertification.value = false
  }
}

const loadPreparationPlannings = async () => {
  loadingPreparation.value = true
  try {
    console.log('📥 Loading preparation assignments...')
    const response = await fetch(`${API_BASE_URL}/api/planning/assignments`)

    if (response.ok) {
      const assignments = await response.json()

      // ✅ FIXED: Use "PACKAGING" instead of "PREPARATION"
      const preparationAssignments = assignments.filter((a: any) =>
        a.processingStage === 'PACKAGING'
      )

      const plannings = preparationAssignments.map((a: any) => ({
        id: a.id,
        orderNumber: a.orderNumber,
        employeeName: a.employeeName,
        cardCount: a.cardCount || 0,
        delai: 'F',
        planningDate: a.scheduledStart ? new Date(a.scheduledStart).toISOString().split('T')[0] : null,
        startTime: a.scheduledStart,
        endTime: a.scheduledEnd,
        formattedDuration: a.estimatedDurationMinutes ? `${Math.round(a.estimatedDurationMinutes / 60)}h` : '0h',
        status: a.status
      }))

      preparationData.value = {
        plannings: plannings,
        summary: {
          totalCards: plannings.reduce((sum: number, p: any) => sum + p.cardCount, 0),
          totalHours: Math.round(plannings.reduce((sum: number, p: any) =>
            sum + (p.formattedDuration ? parseInt(p.formattedDuration) : 0), 0))
        }
      }

      console.log(`✅ Preparation: ${preparationData.value.plannings.length} tasks`)
    }
  } catch (error) {
    console.error('❌ Error loading preparation plannings:', error)
  } finally {
    loadingPreparation.value = false
  }
}

const loadScanningPlannings = async () => {
  loadingScanning.value = true
  try {
    console.log('📥 Loading scanning assignments...')
    const response = await fetch(`${API_BASE_URL}/api/planning/assignments`)

    if (response.ok) {
      const assignments = await response.json()

      const scanningAssignments = assignments.filter((a: any) =>
        a.processingStage === 'SCANNING'
      )

      const plannings = scanningAssignments.map((a: any) => ({
        id: a.id,
        orderNumber: a.orderNumber,
        employeeName: a.employeeName,
        cardCount: a.cardCount || 0,
        delai: 'F',
        planningDate: a.scheduledStart ? new Date(a.scheduledStart).toISOString().split('T')[0] : null,
        startTime: a.scheduledStart,
        endTime: a.scheduledEnd,
        formattedDuration: a.estimatedDurationMinutes ? `${Math.round(a.estimatedDurationMinutes / 60)}h` : '0h',
        status: a.status
      }))

      scanningData.value = {
        plannings: plannings,
        summary: {
          totalCards: plannings.reduce((sum: number, p: any) => sum + p.cardCount, 0),
          totalHours: Math.round(plannings.reduce((sum: number, p: any) =>
            sum + (p.formattedDuration ? parseInt(p.formattedDuration) : 0), 0))
        }
      }

      console.log(`✅ Scanning: ${scanningData.value.plannings.length} tasks`)
    }
  } catch (error) {
    console.error('❌ Error loading scanning plannings:', error)
  } finally {
    loadingScanning.value = false
  }
}

// Keep this method as is
const refreshAllPanels = async () => {
  console.log('🔄 Refreshing all panels...')
  await Promise.all([
    loadGradingPlannings(),
    loadCertificationPlannings(),
    loadPreparationPlannings(),
    loadScanningPlannings()
  ])
  console.log('✅ All panels refreshed')
}

const formatDate = (date: any) => {
  if (!date) return 'N/A'
  try {
    return new Date(date).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  } catch (e) {
    return 'N/A'
  }
}

const formatTime = (time: any) => {
  if (!time) return 'N/A'
  try {
    if (typeof time === 'string' && time.match(/^\d{2}:\d{2}$/)) return time
    const date = new Date(time)
    return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: false })
  } catch (e) {
    return 'N/A'
  }
}

const getDelaiClass = (delai: string) => {
  const map: Record<string, string> = {
    'X': 'excelsior',
    'F+': 'fast-plus',
    'F': 'fast',
    'C': 'classic',
    'E': 'economy'
  }
  return map[delai] || 'default'
}

const getDelaiLabel = (delai: string) => {
  const map: Record<string, string> = {
    'X': '⚡ Excelsior',
    'F+': '🚀 Fast+',
    'F': '⏩ Fast',
    'C': '📦 Classic',
    'E': '🐌 Economy'
  }
  return map[delai] || delai
}

// Lifecycle
onMounted(() => {
  refreshAllPanels()
})
</script>

<style scoped>
.planning-page {
  max-width: 1800px;
  margin: 0 auto;
  padding: 24px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 24px 0;
}

.controls-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  flex-wrap: wrap;
}

.control-team {
  display: flex;
  align-items: center;
  gap: 8px;
}

.control-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  color: #374151;
  white-space: nowrap;
}

.control-input {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
}

.generate-btn, .refresh-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.generate-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.generate-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.generate-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-btn {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  color: #374151;
}

.refresh-btn:hover {
  background: #f3f4f6;
}

.icon {
  width: 18px;
  height: 18px;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.panels-container {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.panel {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 250px);
}

.grading-panel {
  border-top: 4px solid #3b82f6;
}

.certification-panel {
  border-top: 4px solid #8b5cf6;
}

.preparation-panel {
  border-top: 4px solid #10b981;
}

.scanning-panel {
  border-top: 4px solid #f59e0b;
}

.panel-header {
  padding: 20px;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  border-bottom: 1px solid #e5e7eb;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 12px 0;
}

.panel-icon {
  font-size: 24px;
}

.panel-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.stat-badge {
  padding: 4px 10px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}

.panel-loading, .panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #9ca3af;
}

.empty-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.planning-card {
  background: white;
  border: 2px solid #e5e7eb;
  border-left-width: 4px;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.2s;
}

.planning-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateX(2px);
}

.planning-card.delai-excelsior {
  border-left-color: #ef4444;
  background: linear-gradient(90deg, rgba(239, 68, 68, 0.05) 0%, white 100%);
}

.planning-card.delai-fast-plus {
  border-left-color: #f97316;
  background: linear-gradient(90deg, rgba(249, 115, 22, 0.05) 0%, white 100%);
}

.planning-card.delai-fast {
  border-left-color: #eab308;
  background: linear-gradient(90deg, rgba(234, 179, 8, 0.05) 0%, white 100%);
}

.planning-card.delai-classic {
  border-left-color: #3b82f6;
  background: linear-gradient(90deg, rgba(59, 130, 246, 0.05) 0%, white 100%);
}

.planning-card.delai-economy {
  border-left-color: #10b981;
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.05) 0%, white 100%);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.order-number {
  font-weight: 700;
  color: #111827;
  font-size: 15px;
}

.delai-badge {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.delai-badge.delai-excelsior {
  background: #fee2e2;
  color: #991b1b;
}

.delai-badge.delai-fast-plus {
  background: #ffedd5;
  color: #9a3412;
}

.delai-badge.delai-fast {
  background: #fef3c7;
  color: #92400e;
}

.delai-badge.delai-classic {
  background: #dbeafe;
  color: #1e40af;
}

.delai-badge.delai-economy {
  background: #d1fae5;
  color: #065f46;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
}

.card-info .icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

@media (max-width: 1600px) {
  .panels-container {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 900px) {
  .panels-container {
    grid-template-columns: 1fr;
  }
}
</style>
