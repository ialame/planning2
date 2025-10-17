import { ApiService } from '../services/api';

declare global {
  interface Window {
    apiService?: ApiService;
  }
  // Définir l'interface Order
  interface Order {
    id: string;
    orderNumber: string;
    reference: string;
    cardCount: number;
    totalPrice: number;
    delai: 'EXCELSIOR' | 'FAST_PLUS' | 'FAST' | 'CLASSIC';
    status: number; // Now using integer status codes
    statusText: string;
    estimatedTimeMinutes: number;
    estimatedTimeHours: number;
    creationDate: string;
    orderDate: string;
    deadline: string;
    qualityIndicator: number;
    minimumGrade: number;
    type: string;
    unsealing: boolean;
  }

// Update the ORDER_STATUS constants to match your backend
  export const ORDER_STATUS = {
    A_RECEPTIONNER: 1,      // To be received
    COLIS_ACCEPTE: 9,       // Package accepted
    A_SCANNER: 10,          // To be scanned
    A_OUVRIR: 11,           // To be opened
    A_NOTER: 2,             // To be evaluated
    A_CERTIFIER: 3,         // To be encapsulated
    A_PREPARER: 4,          // To be prepared
    A_DESCELLER: 7,         // To be unsealed
    A_VOIR: 6,              // To be seen
    A_DISTRIBUER: 41,       // To be delivered
    A_ENVOYER: 42,          // To be sent
    ENVOYEE: 5,             // Sent
    RECU: 8                 // Received
  } as const;

// Add status helper functions
  export const STATUS_GROUPS = {
    RECEPTION: [1, 9],           // Reception stage
    PROCESSING: [10, 11, 2, 3, 4, 7, 6], // Processing stage
    SHIPPING: [41, 42],          // Shipping stage
    COMPLETED: [5, 8]            // Completed stage
  } as const;

// Status display names
  export const STATUS_NAMES = {
    1: 'To be received',
    9: 'Package accepted',
    10: 'To be scanned',
    11: 'To be opened',
    2: 'To be evaluated',
    3: 'To be encapsulated',
    4: 'To be prepared',
    7: 'To be unsealed',
    6: 'To be seen',
    41: 'To be delivered',
    42: 'To be sent',
    5: 'Sent',
    8: 'Received'
  } as const;

// Status colors for UI
  export const STATUS_COLORS = {
    RECEPTION: 'bg-blue-100 text-blue-800',
    PROCESSING: 'bg-yellow-100 text-yellow-800',
    SHIPPING: 'bg-orange-100 text-orange-800',
    COMPLETED: 'bg-green-100 text-green-800',
    DEFAULT: 'bg-gray-100 text-gray-800'
  } as const;

  // ========== INTERFACES ==========
  interface Employee {
    id: string
    firstName: string
    lastName: string
    email: string
    status: 'AVAILABLE' | 'BUSY' | 'OFFLINE'
    workHoursPerDay?: number
    workload?: number
    activeOrders?: number
    estimatedHours?: number
    totalCards?: number
    plannings? : planning[]
    assignedOrders? : order[]
    totalWorkload? : number
  }

  interface NewEmployee {
    firstName: string
    lastName: string
    email: string
  }

  // ========== INTERFACES ==========
  interface Planning {
    id: string
    symfonyOrderId: string  // ✅ NEW - Matches backend
    orderId?: string         // ✅ DEPRECATED - Keep for compatibility
    employeeId: string
    employeeName?: string
    orderNumber?: string
    planningDate: string
    startTime: string
    endTime: string
    durationMinutes: number
    cardCount?: number
    priority?: string
    delai?: string          // ✅ ADD - Priority code (X, F+, F, C, E)
    status?: string | number // ✅ UPDATE - Can be string or integer
    completed?: boolean      // ✅ OPTIONAL - Not always present
    notes?: string
  }

  interface PlanningConfig {
    startDate: string
    cardProcessingTime: number
    priorityMode: string
    redistributeOverload: boolean
    respectPriorities: boolean
  }


}

export {};
