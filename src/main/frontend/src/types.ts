export interface WorkAssignment {
  id: string;
  employeeId: string;
  employeeName: string;
  symfonyOrderId?: string;  // ✅ NEW - From Spring Boot entity
  orderId: string;           // ✅ KEEP - For backward compatibility with Order entity
  orderNumber: string;
  // ...
}
