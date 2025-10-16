/**
 * TypeScript Type Definitions
 * Pokemon Card Order Planning System
 */

// Work Assignment interface
export interface WorkAssignment {
  id: string;
  employeeId: string;
  employeeName: string;
  orderId: string;
  orderNumber: string;
  processingStage: ProcessingStage;
  cardCount: number;
  estimatedDurationMinutes: number;
  scheduledStart: string;
  scheduledEnd: string;
  actualStart?: string;
  actualEnd?: string;
  status: AssignmentStatus;
  priorityScore: number;
}

// Employee Workload interface
export interface EmployeeWorkload {
  employeeId: string;
  employeeName: string;
  roles: EmployeeRole[];
  scheduledMinutes: number;
  dailyCapacityMinutes: number;
  utilizationPercentage: number;
  assignmentCount: number;
  nextAvailableTime?: string;
}

// Order interface
export interface Order {
  id: string;
  orderNumber: string;
  customerName: string;
  customerEmail: string;
  deliveryDeadline: string;
  orderDate: string;
  status: OrderStatus;
  cardCount: number;
  estimatedProcessingMinutes: number;
  priorityScore: number;
}

// Employee interface
export interface Employee {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  roles: EmployeeRole[];
  active: boolean;
  dailyCapacityMinutes: number;
}

// Team interface
export interface Team {
  id: string;
  name: string;
  description: string;
  permissionLevel: number;
  active: boolean;
  creationDate: string;
  modificationDate: string;
}

// Pokemon Card interface
export interface Card {
  id: string;
  orderId: string;
  cardName: string;
  cardSet: string;
  cardNumber: string;
  status: CardStatus;
  assignedGrade?: number;
}

// Planning Response interface
export interface PlanningResponse {
  success: boolean;
  message: string;
  assignmentsCreated: number;
  summary: WorkPlanSummary;
  assignments: WorkAssignment[];
}

// Work Plan Summary interface
export interface WorkPlanSummary {
  totalOrders: number;
  totalAssignments: number;
  employeesInvolved: number;
  earliestDeadline: string;
  latestDeadline: string;
  totalEstimatedMinutes: number;
  stageStatistics: StageStatistics[];
}

// Stage Statistics interface
export interface StageStatistics {
  stageName: ProcessingStage;
  assignmentCount: number;
  totalCards: number;
  totalMinutes: number;
  employeesAssigned: number;
}

// Create Order Request interface
export interface CreateOrderRequest {
  orderNumber: string;
  customerName: string;
  customerEmail: string;
  deliveryDeadline: string;
  cards: CardRequest[];
}

// Card Request interface
export interface CardRequest {
  cardName: string;
  cardSet: string;
  cardNumber: string;
}

// Enums
export enum ProcessingStage {
  GRADING = 'GRADING',
  CERTIFYING = 'CERTIFYING',
  SCANNING = 'SCANNING',
  PACKAGING = 'PACKAGING'
}

export enum AssignmentStatus {
  SCHEDULED = 'SCHEDULED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export enum OrderStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  GRADING = 'GRADING',
  CERTIFYING = 'CERTIFYING',
  SCANNING = 'SCANNING',
  PACKAGING = 'PACKAGING',
  COMPLETED = 'COMPLETED',
  DELIVERED = 'DELIVERED'
}

export enum CardStatus {
  PENDING = 'PENDING',
  GRADING = 'GRADING',
  GRADED = 'GRADED',
  CERTIFYING = 'CERTIFYING',
  CERTIFIED = 'CERTIFIED',
  SCANNING = 'SCANNING',
  SCANNED = 'SCANNED',
  PACKAGING = 'PACKAGING',
  PACKAGED = 'PACKAGED',
  COMPLETED = 'COMPLETED'
}

export enum EmployeeRole {
  GRADER = 'ROLE_GRADER',
  CERTIFIER = 'ROLE_CERTIFIER',
  SCANNER = 'ROLE_SCANNER',
  PACKAGER = 'ROLE_PACKAGER'
}

// API Response types
export interface ApiResponse<T> {
  data?: T;
  error?: string;
  success: boolean;
}

// Pagination interface
export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// Filter interfaces
export interface OrderFilter {
  status?: OrderStatus;
  customerName?: string;
  dateFrom?: string;
  dateTo?: string;
}

export interface AssignmentFilter {
  employeeId?: string;
  orderId?: string;
  status?: AssignmentStatus;
  stage?: ProcessingStage;
}

// Statistics interfaces
export interface DashboardStatistics {
  pendingOrders: number;
  activeEmployees: number;
  totalAssignments: number;
  estimatedHours: number;
  overdueOrders: number;
  completionRate: number;
}

// Utility types
export type DateString = string; // ISO 8601 format
export type UUID = string;

// Constants
export const MINUTES_PER_CARD = 3;
export const DEFAULT_DAILY_CAPACITY = 480; // 8 hours in minutes

// Role display names
export const ROLE_DISPLAY_NAMES: Record<EmployeeRole, string> = {
  [EmployeeRole.GRADER]: 'Grader',
  [EmployeeRole.CERTIFIER]: 'Certifier',
  [EmployeeRole.SCANNER]: 'Scanner',
  [EmployeeRole.PACKAGER]: 'Packager'
};

// Status color mappings
export const STATUS_COLORS: Record<AssignmentStatus, string> = {
  [AssignmentStatus.SCHEDULED]: 'blue',
  [AssignmentStatus.IN_PROGRESS]: 'yellow',
  [AssignmentStatus.COMPLETED]: 'green',
  [AssignmentStatus.CANCELLED]: 'red'
};

export const ORDER_STATUS_COLORS: Record<OrderStatus, string> = {
  [OrderStatus.PENDING]: 'gray',
  [OrderStatus.IN_PROGRESS]: 'blue',
  [OrderStatus.GRADING]: 'yellow',
  [OrderStatus.CERTIFYING]: 'orange',
  [OrderStatus.SCANNING]: 'purple',
  [OrderStatus.PACKAGING]: 'indigo',
  [OrderStatus.COMPLETED]: 'green',
  [OrderStatus.DELIVERED]: 'teal'
};
