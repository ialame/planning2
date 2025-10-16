/**
 * Vue 3 Composable for Planning Management
 * Reusable composition function for planning state and operations
 */

import { ref, computed, type Ref } from 'vue';
import { planningApi } from '../api.service.ts';
import type {
  WorkAssignment,
  EmployeeWorkload,
  Order,
  AssignmentStatus,
  PlanningResponse
} from '../types';

export interface UsePlanningReturn {
  // State
  assignments: Ref<WorkAssignment[]>;
  workload: Ref<EmployeeWorkload[]>;
  pendingOrders: Ref<Order[]>;
  loading: Ref<boolean>;
  error: Ref<string | null>;

  // Computed
  totalEstimatedHours: Ref<number>;
  totalPendingCards: Ref<number>;
  averageUtilization: Ref<number>;
  mostUtilizedEmployee: Ref<EmployeeWorkload | null>;

  // Methods
  loadData: () => Promise<void>;
  generatePlan: () => Promise<PlanningResponse | null>;
  updateAssignmentStatus: (assignmentId: string, status: AssignmentStatus) => Promise<boolean>;
  getEmployeeAssignments: (employeeId: string) => WorkAssignment[];
  getOrderAssignments: (orderId: string) => WorkAssignment[];
  refreshData: () => Promise<void>;
}

/**
 * Composable for managing planning data and operations
 */
export function usePlanning(): UsePlanningReturn {
  // State
  const assignments = ref<WorkAssignment[]>([]);
  const workload = ref<EmployeeWorkload[]>([]);
  const pendingOrders = ref<Order[]>([]);
  const loading = ref<boolean>(false);
  const error = ref<string | null>(null);

  // Computed properties
  const totalEstimatedHours = computed(() => {
    return Math.round(
      assignments.value.reduce((sum, a) => sum + a.estimatedDurationMinutes, 0) / 60
    );
  });

  const totalPendingCards = computed(() => {
    return pendingOrders.value.reduce((sum, order) => sum + order.cardCount, 0);
  });

  const averageUtilization = computed(() => {
    if (workload.value.length === 0) return 0;
    const total = workload.value.reduce(
      (sum, emp) => sum + emp.utilizationPercentage,
      0
    );
    return Math.round(total / workload.value.length);
  });

  const mostUtilizedEmployee = computed(() => {
    if (workload.value.length === 0) return null;
    return workload.value.reduce((max, emp) =>
      emp.utilizationPercentage > max.utilizationPercentage ? emp : max
    );
  });

  /**
   * Load all planning data
   */
  const loadData = async (): Promise<void> => {
    loading.value = true;
    error.value = null;

    try {
      const [assignmentsRes, workloadRes, ordersRes] = await Promise.all([
        planningApi.getAssignments(),
        planningApi.getEmployeeWorkload(),
        planningApi.getPendingOrders()
      ]);

      if (assignmentsRes.success && assignmentsRes.data) {
        assignments.value = assignmentsRes.data;
      } else {
        throw new Error(assignmentsRes.error || 'Failed to load assignments');
      }

      if (workloadRes.success && workloadRes.data) {
        workload.value = workloadRes.data;
      } else {
        throw new Error(workloadRes.error || 'Failed to load workload');
      }

      if (ordersRes.success && ordersRes.data) {
        pendingOrders.value = ordersRes.data;
      } else {
        throw new Error(ordersRes.error || 'Failed to load orders');
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error occurred';
      console.error('Error loading planning data:', err);
    } finally {
      loading.value = false;
    }
  };

  /**
   * Generate new work plan
   */
  const generatePlan = async (): Promise<PlanningResponse | null> => {
    loading.value = true;
    error.value = null;

    try {
      const response = await planningApi.generateWorkPlan();

      if (response.success && response.data) {
        // Reload data after successful generation
        await loadData();
        return response.data;
      } else {
        throw new Error(response.error || 'Failed to generate work plan');
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error occurred';
      console.error('Error generating work plan:', err);
      return null;
    } finally {
      loading.value = false;
    }
  };

  /**
   * Update assignment status
   */
  const updateAssignmentStatus = async (
    assignmentId: string,
    status: AssignmentStatus
  ): Promise<boolean> => {
    loading.value = true;
    error.value = null;

    try {
      const response = await planningApi.updateAssignmentStatus(assignmentId, status);

      if (response.success && response.data) {
        // Update local state
        const index = assignments.value.findIndex(a => a.id === assignmentId);
        if (index !== -1) {
          assignments.value[index] = response.data;
        }
        return true;
      } else {
        throw new Error(response.error || 'Failed to update assignment status');
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error occurred';
      console.error('Error updating assignment status:', err);
      return false;
    } finally {
      loading.value = false;
    }
  };

  /**
   * Get assignments for a specific employee
   */
  const getEmployeeAssignments = (employeeId: string): WorkAssignment[] => {
    return assignments.value.filter(a => a.employeeId === employeeId);
  };

  /**
   * Get assignments for a specific order
   */
  const getOrderAssignments = (orderId: string): WorkAssignment[] => {
    return assignments.value.filter(a => a.orderId === orderId);
  };

  /**
   * Refresh all data
   */
  const refreshData = async (): Promise<void> => {
    await loadData();
  };

  return {
    // State
    assignments,
    workload,
    pendingOrders,
    loading,
    error,

    // Computed
    totalEstimatedHours,
    totalPendingCards,
    averageUtilization,
    mostUtilizedEmployee,

    // Methods
    loadData,
    generatePlan,
    updateAssignmentStatus,
    getEmployeeAssignments,
    getOrderAssignments,
    refreshData
  };
}

/**
 * Composable for date formatting utilities
 */
export function useDateFormatter() {
  const formatDate = (dateString: string | undefined): string => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatDateShort = (dateString: string | undefined): string => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric'
    });
  };

  const formatTime = (dateString: string | undefined): string => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const isOverdue = (deadline: string): boolean => {
    return new Date(deadline) < new Date();
  };

  const daysUntilDeadline = (deadline: string): number => {
    const now = new Date();
    const target = new Date(deadline);
    const diffTime = target.getTime() - now.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  return {
    formatDate,
    formatDateShort,
    formatTime,
    isOverdue,
    daysUntilDeadline
  };
}

/**
 * Composable for status and color utilities
 */
export function useStatusHelpers() {
  const getStatusColor = (status: string): string => {
    const colors: Record<string, string> = {
      SCHEDULED: 'bg-blue-100 text-blue-800',
      IN_PROGRESS: 'bg-yellow-100 text-yellow-800',
      COMPLETED: 'bg-green-100 text-green-800',
      CANCELLED: 'bg-red-100 text-red-800',
      PENDING: 'bg-gray-100 text-gray-800'
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
  };

  const getUtilizationColor = (percentage: number): string => {
    if (percentage > 80) return 'bg-red-100 text-red-800';
    if (percentage > 50) return 'bg-yellow-100 text-yellow-800';
    return 'bg-green-100 text-green-800';
  };

  const getPriorityColor = (priorityScore: number): string => {
    // Lower score = higher priority
    if (priorityScore < 50) return 'bg-red-100 text-red-800';
    if (priorityScore < 100) return 'bg-orange-100 text-orange-800';
    return 'bg-green-100 text-green-800';
  };

  const getProgressColor = (progress: number): string => {
    if (progress >= 100) return 'bg-green-600';
    if (progress >= 75) return 'bg-blue-600';
    if (progress >= 50) return 'bg-yellow-600';
    return 'bg-gray-600';
  };

  return {
    getStatusColor,
    getUtilizationColor,
    getPriorityColor,
    getProgressColor
  };
}

/**
 * Composable for statistics calculations
 */
export function useStatistics(assignments: Ref<WorkAssignment[]>) {
  const totalDuration = computed(() => {
    return assignments.value.reduce(
      (sum, a) => sum + a.estimatedDurationMinutes,
      0
    );
  });

  const completedAssignments = computed(() => {
    return assignments.value.filter(a => a.status === 'COMPLETED').length;
  });

  const inProgressAssignments = computed(() => {
    return assignments.value.filter(a => a.status === 'IN_PROGRESS').length;
  });

  const scheduledAssignments = computed(() => {
    return assignments.value.filter(a => a.status === 'SCHEDULED').length;
  });

  const completionRate = computed(() => {
    if (assignments.value.length === 0) return 0;
    return Math.round((completedAssignments.value / assignments.value.length) * 100);
  });

  const averageDuration = computed(() => {
    if (assignments.value.length === 0) return 0;
    return Math.round(totalDuration.value / assignments.value.length);
  });

  return {
    totalDuration,
    completedAssignments,
    inProgressAssignments,
    scheduledAssignments,
    completionRate,
    averageDuration
  };
}
