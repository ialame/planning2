<template>
  <div id="app" class="min-h-screen bg-gray-100">
    <!-- Navigation -->
    <nav class="bg-blue-600 text-white shadow-lg">
      <div class="max-w-7xl mx-auto px-4">
        <div class="flex justify-between items-center h-16">
          <div class="flex items-center">
            <h1 class="text-xl font-bold">🃏 Pokemon Card Planning</h1>
          </div>
          <div class="flex space-x-4">
            <button
              v-for="tab in tabs"
              :key="tab.id"
              @click="changeTab(tab.id)"
              :class="[
                'px-4 py-2 rounded-md transition-colors',
                activeTab === tab.id
                  ? 'bg-blue-700 text-white'
                  : 'text-blue-100 hover:text-white hover:bg-blue-500'
              ]"
            >
              {{ tab.label }}
            </button>
          </div>
        </div>
      </div>
    </nav>

    <!-- Contenu principal -->
    <main class="max-w-7xl mx-auto px-4 py-6">
      <!-- Dashboard -->
      <DashboardView v-if="activeTab === 'dashboard'" @go-to-tab="changeTab" />

      <!-- Orders -->
      <OrdersView v-if="activeTab === 'orders'" />

      <!-- Employees with Planning integrated -->
      <EmployeesView v-if="activeTab === 'employees'" />

      <!-- Global Planning -->
      <PlanningView v-if="activeTab === 'planning'" />

      <!-- NOUVEAU -->
      <TeamsView v-if="activeTab === 'teams'" />
      <!-- NOUVEAU -->
      <SyncView v-if="activeTab === 'sync'" />


    </main>

    <!-- Notifications -->
    <div
      v-if="notification.show"
      :class="[
        'fixed top-4 right-4 p-4 rounded-lg shadow-lg transition-all z-50',
        notification.type === 'success' ? 'bg-green-500 text-white' : 'bg-red-500 text-white'
      ]"
    >
      {{ notification.message }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, provide } from 'vue'
import DashboardView from './views/Dashboard.vue'
import OrdersView from './views/Orders.vue'
import EmployeesView from './views/Employees.vue'
import PlanningView from './views/Planning.vue'
import TeamsView from "./views/Teams.vue";
import SyncView from "./views/DataSync.vue";

// État global
const activeTab = ref('dashboard')
const notification = ref({
  show: false,
  message: '',
  type: 'success'
})

const tabs = [
  { id: 'dashboard', label: '📊 Dashboard' },
  { id: 'orders', label: '📋 Orders' },
  { id: 'employees', label: '👥 Employees' },
  { id: 'planning', label: '📅 Planning' },
  { id: 'teams', label: '👥 Teams' },
  { id: 'sync', label: 'ApiSync' },
]
  // Fonction pour changer d'onglet
const changeTab = (tabId: string) => {
  console.log('Changing to tab:', tabId)
  activeTab.value = tabId
}

// Fonction pour afficher les notifications
const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
  notification.value = { show: true, message, type }
  setTimeout(() => {
    notification.value.show = false
  }, 3000)
}

// Provide pour les composants enfants
provide('showNotification', showNotification)
provide('changeTab', changeTab)
</script>

<style>
#app {
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
