/**
 * API Service Layer
 * Centralized API calls for Pokemon Card Order Planning System
 */

import type {
  WorkAssignment,
  EmployeeWorkload,
  Order,
  Employee,
  Team,
  PlanningResponse,
  CreateOrderRequest,
  AssignmentStatus,
  ApiResponse
} from './types';

// API Configuration
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/**
 * Generic fetch wrapper with error handling
 */
async function fetchApi<T>(
  endpoint: string,
  options?: RequestInit
): Promise<ApiResponse<T>> {
  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
      ...options,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    return { data, success: true };
  } catch (error) {
    console.error(`API Error [${endpoint}]:`, error);
    return {
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error occurred'
    };
  }
}

/**
 * Planning API Service
 */
export const planningApi = {
  /**
   * Generate work plan for all pending orders
   */
  async generateWorkPlan(): Promise<ApiResponse<PlanningResponse>> {
    return fetchApi<PlanningResponse>('/planning/generate', {
      method: 'POST',
    });
  },

  /**
   * Get all work assignments
   */
  async getAssignments(): Promise<ApiResponse<WorkAssignment[]>> {
    return fetchApi<WorkAssignment[]>('/planning/assignments');
  },

  /**
   * Get assignments for specific employee
   */
  async getEmployeeAssignments(employeeId: string): Promise<ApiResponse<WorkAssignment[]>> {
    return fetchApi<WorkAssignment[]>(`/planning/assignments/employee/${employeeId}`);
  },

  /**
   * Get assignments for specific order
   */
  async getOrderAssignments(orderId: string): Promise<ApiResponse<WorkAssignment[]>> {
    return fetchApi<WorkAssignment[]>(`/planning/assignments/order/${orderId}`);
  },

  /**
   * Update assignment status
   */
  async updateAssignmentStatus(
    assignmentId: string,
    status: AssignmentStatus
  ): Promise<ApiResponse<WorkAssignment>> {
    return fetchApi<WorkAssignment>(
      `/planning/assignments/${assignmentId}/status?status=${status}`,
      { method: 'PUT' }
    );
  },

  /**
   * Get employee workload summary
   */
  async getEmployeeWorkload(): Promise<ApiResponse<EmployeeWorkload[]>> {
    return fetchApi<EmployeeWorkload[]>('/planning/workload');
  },

  /**
   * Get pending orders sorted by priority
   */
  async getPendingOrders(): Promise<ApiResponse<Order[]>> {
    return fetchApi<Order[]>('/planning/orders/pending');
  },

  /**
   * Get overdue orders
   */
  async getOverdueOrders(): Promise<ApiResponse<Order[]>> {
    return fetchApi<Order[]>('/planning/orders/overdue');
  },
};

/**
 * Order API Service
 */
export const orderApi = {
  /**
   * Get all orders
   */
  async getOrders(): Promise<ApiResponse<Order[]>> {
    return fetchApi<Order[]>('/orders');
  },

  /**
   * Get order by ID
   */
  async getOrderById(orderId: string): Promise<ApiResponse<Order>> {
    return fetchApi<Order>(`/orders/${orderId}`);
  },

  /**
   * Create new order
   */
  async createOrder(order: CreateOrderRequest): Promise<ApiResponse<Order>> {
    return fetchApi<Order>('/orders', {
      method: 'POST',
      body: JSON.stringify(order),
    });
  },

  /**
   * Update order
   */
  async updateOrder(orderId: string, order: Partial<Order>): Promise<ApiResponse<Order>> {
    return fetchApi<Order>(`/orders/${orderId}`, {
      method: 'PUT',
      body: JSON.stringify(order),
    });
  },

  /**
   * Delete order
   */
  async deleteOrder(orderId: string): Promise<ApiResponse<void>> {
    return fetchApi<void>(`/orders/${orderId}`, {
      method: 'DELETE',
    });
  },
};

/**
 * Employee API Service
 */
export const employeeApi = {
  /**
   * Get all employees
   */
  async getEmployees(): Promise<ApiResponse<Employee[]>> {
    return fetchApi<Employee[]>('/employees');
  },

  /**
   * Get employee by ID
   */
  async getEmployeeById(employeeId: string): Promise<ApiResponse<Employee>> {
    return fetchApi<Employee>(`/employees/${employeeId}`);
  },

  /**
   * Create new employee
   */
  async createEmployee(employee: Omit<Employee, 'id'>): Promise<ApiResponse<Employee>> {
    return fetchApi<Employee>('/employees', {
      method: 'POST',
      body: JSON.stringify(employee),
    });
  },

  /**
   * Update employee
   */
  async updateEmployee(
    employeeId: string,
    employee: Partial<Employee>
  ): Promise<ApiResponse<Employee>> {
    return fetchApi<Employee>(`/employees/${employeeId}`, {
      method: 'PUT',
      body: JSON.stringify(employee),
    });
  },

  /**
   * Delete employee
   */
  async deleteEmployee(employeeId: string): Promise<ApiResponse<void>> {
    return fetchApi<void>(`/employees/${employeeId}`, {
      method: 'DELETE',
    });
  },

  /**
   * Get employees by role
   */
  async getEmployeesByRole(role: string): Promise<ApiResponse<Employee[]>> {
    return fetchApi<Employee[]>(`/employees/role/${role}`);
  },
};

/**
 * Team API Service
 */
export const teamApi = {
  /**
   * Get all teams
   */
  async getTeams(): Promise<ApiResponse<Team[]>> {
    return fetchApi<Team[]>('/teams');
  },

  /**
   * Get team by ID
   */
  async getTeamById(teamId: string): Promise<ApiResponse<Team>> {
    return fetchApi<Team>(`/teams/${teamId}`);
  },

  /**
   * Create new team
   */
  async createTeam(team: Omit<Team, 'id'>): Promise<ApiResponse<Team>> {
    return fetchApi<Team>('/teams', {
      method: 'POST',
      body: JSON.stringify(team),
    });
  },

  /**
   * Update team
   */
  async updateTeam(teamId: string, team: Partial<Team>): Promise<ApiResponse<Team>> {
    return fetchApi<Team>(`/teams/${teamId}`, {
      method: 'PUT',
      body: JSON.stringify(team),
    });
  },

  /**
   * Delete team
   */
  async deleteTeam(teamId: string): Promise<ApiResponse<void>> {
    return fetchApi<void>(`/teams/${teamId}`, {
      method: 'DELETE',
    });
  },
};

/**
 * Export all API services
 */
export default {
  planning: planningApi,
  orders: orderApi,
  employees: employeeApi,
  teams: teamApi,
};
