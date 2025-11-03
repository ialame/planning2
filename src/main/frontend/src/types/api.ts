// types/api.ts

export interface Order {
  id: string
  orderNumber: string
  customerName: string
  status: 'PENDING' | 'GRADING' | 'CERTIFYING' | 'SCANNING' | 'PACKAGING' | 'DELIVERED'
  delai: 'X' | 'F+' | 'F' | 'C' | 'E'
  totalCards: number
  date: string
  price: number
}

export interface Employee {
  id: string
  firstName: string
  lastName: string
  email: string
  roles: string[]
  active: boolean
}

export interface WorkAssignment {
  id: string
  processingStage: 'GRADING' | 'CERTIFYING' | 'SCANNING' | 'PACKAGING'
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
  cardCount: number
  estimatedDurationMinutes: number
  scheduledStart: string
  scheduledEnd: string
  employee: Employee
  order: Order
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}
