<template>
  <div class="min-h-screen bg-gray-100 p-6">
    <div class="max-w-7xl mx-auto">
      <!-- Header -->
      <div class="flex justify-between items-center mb-8">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">Pokemon Card Order Planning</h1>
          <p v-if="error" class="text-red-600 text-sm mt-1">{{ error }}</p>
        </div>
        <button
          @click="handleGeneratePlan"
          :disabled="loading"
          class="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition flex items-center gap-2"
        >
          <span v-if="loading" class="animate-spin">⚙️</span>
          {{ loading ? 'Generating...' : 'Generate Work Plan' }}
        </button>
      </div>

      <!-- Stats Cards -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div class="bg-white p-6 rounded-lg shadow">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">Pending Orders</p>
              <p class="text-2xl font-bold mt-1">{{ pendingOrders.length }}</p>
            </div>
            <PackageIcon class="h-12 w-12 text-blue-600" />
          </div>
        </div>

        <div class="bg-white p-6 rounded-lg shadow">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">Active Employees</p>
              <p class="text-2xl font-bold mt-1">{{ workload.length }}</p>
            </div>
            <UsersIcon class="h-12 w-12 text-green-600" />
          </div>
        </div>

        <div class="bg-white p-6 rounded-lg shadow">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">Total Assignments</p>
              <p class="text-2xl font-bold mt-1">{{ assignments.length }}</p>
            </div>
            <CalendarIcon class="h-12 w-12 text-purple-600" />
          </div>
        </div>

        <div class="bg-white p-6 rounded-lg shadow">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-gray-500 text-sm">Estimated Hours</p>
              <p class="text-2xl font-bold mt-1">{{ totalEstimatedHours }}</p>
            </div>
            <ClockIcon class="h-12 w-12 text-orange-600" />
          </div>
        </div>
      </div>

      <!-- Additional Stats Row -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div class="bg-white p-4 rounded-lg shadow">
          <p class="text-gray-500 text-sm">Total Pending Cards</p>
          <p class="text-xl font-bold mt-1">{{ totalPendingCards }}</p>
        </div>
        <div class="bg-white p-4 rounded-lg shadow">
          <p class="text-gray-500 text-sm">Average Utilization</p>
          <p class="text-xl font-bold mt-1">{{ averageUtilization }}%</p>
        </div>
        <div class="bg-white p-4 rounded-lg shadow">
          <p class="text-gray-500 text-sm">Most Utilized Employee</p>
          <p class="text-xl font-bold mt-1">
            {{ mostUtilizedEmployee?.employeeName || 'N/A' }}
          </p>
        </div>
      </div>

      <!-- Tabs -->
      <div class="bg-white rounded-lg shadow mb-6">
        <div class="border-b border-gray-200">
          <nav class="flex -mb-px">
            <button
              v-for="tab in tabs"
              :key="tab.id"
              @click="activeTab = tab.id"
              :class="[
                'py-4 px-6 text-sm font-medium transition',
                activeTab === tab.id
                  ? 'border-b-2 border-blue-600 text-blue-600'
                  : 'text-gray-500 hover:text-gray-700'
              ]"
            >
              {{ tab.label }}
            </button>
          </nav>
        </div>

        <!-- Tab Content -->
        <div class="p-6">
          <!-- Overview Tab -->
          <div v-if="activeTab === 'overview'" class="space-y-6">
            <div class="flex justify-between items-center mb-4">
              <h2 class="text-xl font-semibold">Employee Workload</h2>
              <button
                @click="refreshData"
                class="text-blue-600 hover:text-blue-700 text-sm flex items-center gap-1"
              >
                <RefreshCwIcon class="h-4 w-4" />
                Refresh
              </button>
            </div>
            <div class="space-y-4">
              <div v-for="emp in workload" :key="emp.employeeId" class="border rounded-lg p-4 hover:shadow-md transition">
                <div class="flex justify-between items-start mb-2">
                  <div>
                    <h3 class="font-semibold">{{ emp.employeeName }}</h3>
                    <p class="text-sm text-gray-500">{{ emp.roles.join(', ') }}</p>
                  </div>
                  <span :class="['px-3 py-1 rounded-full text-sm font-medium', getUtilizationColor(emp.utilizationPercentage)]">
                    {{ emp.utilizationPercentage.toFixed(1) }}% utilized
                  </span>
                </div>
                <div class="w-full bg-gray-200 rounded-full h-2 mb-2">
                  <div
                    class="h-2 rounded-full transition-all duration-300"
                    :class="[
                      emp.utilizationPercentage > 80 ? 'bg-red-600' :
                      emp.utilizationPercentage > 50 ? 'bg-yellow-600' :
                      'bg-green-600'
                    ]"
                    :style="{ width: Math.min(emp.utilizationPercentage, 100) + '%' }"
                  />
                </div>
                <div class="flex justify-between text-sm text-gray-600">
                  <span>{{ emp.scheduledMinutes }} / {{ emp.dailyCapacityMinutes }} minutes</span>
                  <span>{{ emp.assignmentCount }} assignments</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Assignments Tab -->
          <div v-if="activeTab === 'assignments'">
            <h2 class="text-xl font-semibold mb-4">Work Assignments</h2>
            <div class="overflow-x-auto">
              <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Order</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Employee</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Stage</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Cards</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Duration</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Scheduled</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Status</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                <tr v-for="assignment in assignments" :key="assignment.id" class="hover:bg-gray-50">
                  <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {{ assignment.orderNumber }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{ assignment.employeeName }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{ assignment.processingStage }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{ assignment.cardCount }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{ assignment.estimatedDurationMinutes }} min
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {{ formatDate(assignment.scheduledStart) }}
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                      <span :class="['px-2 py-1 text-xs font-medium rounded-full', getStatusColor(assignment.status)]">
                        {{ assignment.status }}
                      </span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm">
                    <button
                      v-if="assignment.status === 'SCHEDULED'"
                      @click="handleStartAssignment(assignment.id)"
                      class="text-blue-600 hover:text-blue-800"
                    >
                      Start
                    </button>
                    <button
                      v-else-if="assignment.status === 'IN_PROGRESS'"
                      @click="handleCompleteAssignment(assignment.id)"
                      class="text-green-600 hover:text-green-800"
                    >
                      Complete
                    </button>
                  </td>
                </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Employees Tab -->
          <div v-if="activeTab === 'employees'">
            <h2 class="text-xl font-semibold mb-4">Employee List</h2>
            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              <div v-for="emp in workload" :key="emp.employeeId" class="border rounded-lg p-4 hover:shadow-lg transition">
                <div class="flex items-center gap-3 mb-3">
                  <div class="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
                    <UserIcon class="h-6 w-6 text-blue-600" />
                  </div>
                  <div>
                    <h3 class="font-semibold text-lg">{{ emp.employeeName }}</h3>
                    <p class="text-xs text-gray-500">{{ emp.roles.join(', ') }}</p>
                  </div>
                </div>
                <div class="space-y-2 text-sm">
                  <div class="flex justify-between">
                    <span class="text-gray-500">Assignments:</span>
                    <span class="font-medium">{{ emp.assignmentCount }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-gray-500">Workload:</span>
                    <span class="font-medium">{{ emp.scheduledMinutes }} min</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-gray-500">Utilization:</span>
                    <span class="font-medium">{{ emp.utilizationPercentage.toFixed(1) }}%</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-gray-500">Capacity:</span>
                    <span class="font-medium">{{ emp.dailyCapacityMinutes }} min</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Orders Tab -->
          <div v-if="activeTab === 'orders'">
            <h2 class="text-xl font-semibold mb-4">Pending Orders</h2>
            <div class="space-y-4">
              <div v-for="order in pendingOrders" :key="order.id" class="border rounded-lg p-4 hover:shadow-md transition">
                <div class="flex justify-between items-start">
                  <div>
                    <h3 class="font-semibold text-lg">{{ order.orderNumber }}</h3>
                    <p class="text-sm text-gray-500">Customer: {{ order.customerName }}</p>
                    <p class="text-xs text-gray-400">{{ order.customerEmail }}</p>
                  </div>
                  <div class="text-right">
                    <p class="text-sm font-medium text-gray-900">{{ order.cardCount }} cards</p>
                    <p class="text-xs text-gray-500">~{{ order.estimatedProcessingMinutes }} min</p>
                  </div>
                </div>
                <div class="mt-3 flex items-center justify-between">
                  <div class="flex items-center text-sm">
                    <ClockIcon class="h-4 w-4 mr-1" :class="[isOverdue(order.deliveryDeadline) ? 'text-red-500' : 'text-orange-500']" />
                    <span :class="[isOverdue(order.deliveryDeadline) ? 'text-red-600' : 'text-gray-600']">
                      Deadline: {{ formatDate(order.deliveryDeadline) }}
                    </span>
                  </div>
                  <span class="text-xs text-gray-500">
                    {{ daysUntilDeadline(order.deliveryDeadline) }} days remaining
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import {
  PackageIcon,
  UsersIcon,
  CalendarIcon,
  ClockIcon,
  RefreshCwIcon,
  UserIcon
} from 'lucide-vue-next';
import { usePlanning, useDateFormatter, useStatusHelpers } from '../composables/usePlanning';
import { AssignmentStatus } from '../types';

// Use composables
const {
  assignments,
  workload,
  pendingOrders,
  loading,
  error,
  totalEstimatedHours,
  totalPendingCards,
  averageUtilization,
  mostUtilizedEmployee,
  loadData,
  generatePlan,
  updateAssignmentStatus,
  refreshData
} = usePlanning();

const { formatDate, isOverdue, daysUntilDeadline } = useDateFormatter();
const { getStatusColor, getUtilizationColor } = useStatusHelpers();

// Local state
const activeTab = ref('overview');

const tabs = [
  { id: 'overview', label: 'Overview' },
  { id: 'assignments', label: 'Assignments' },
  { id: 'employees', label: 'Employees' },
  { id: 'orders', label: 'Orders' }
];

// Methods
const handleGeneratePlan = async (): Promise<void> => {
  const result = await generatePlan();
  if (result) {
    alert(`Work plan generated! ${result.assignmentsCreated} assignments created.`);
  }
};

const handleStartAssignment = async (assignmentId: string): Promise<void> => {
  const success = await updateAssignmentStatus(assignmentId, AssignmentStatus.IN_PROGRESS);
  if (success) {
    alert('Assignment started!');
  }
};

const handleCompleteAssignment = async (assignmentId: string): Promise<void> => {
  const success = await updateAssignmentStatus(assignmentId, AssignmentStatus.COMPLETED);
  if (success) {
    alert('Assignment completed!');
  }
};

// Lifecycle
onMounted(() => {
  loadData();
});
</script>

<style scoped>
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
</style>
