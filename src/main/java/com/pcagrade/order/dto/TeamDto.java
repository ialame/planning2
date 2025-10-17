package com.pcagrade.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDto {
    private String id;
    private String name;
    private String description;
    private Integer permissionLevel;
    private Boolean active;
    private LocalDateTime creationDate;
    private LocalDateTime modificationDate;
    private Integer employeeCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String id;
        private String name;
        private String description;
        private Integer permissionLevel;
        private Boolean active;
        private LocalDateTime creationDate;
        private LocalDateTime modificationDate;
        private Integer employeeCount;
        private List<String> employeeIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        private String name;
        private String description;
        private Integer permissionLevel;
        private Boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private String id;
        private String name;
        private Integer permissionLevel;
        private Integer employeeCount;
        private Integer memberCount;  // ← Ajouté
        private Boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Detailed {
        private String id;
        private String name;
        private String description;
        private Integer permissionLevel;
        private Boolean active;
        private LocalDateTime creationDate;
        private LocalDateTime modificationDate;
        private Integer employeeCount;
        private Integer memberCount;  // ← Ajouté
        private Integer activeAssignments;  // ← Ajouté
        private List<EmployeeBasicInfo> employees;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class EmployeeBasicInfo {
            private String id;
            private String firstName;
            private String lastName;
            private String email;
        }
    }
}
