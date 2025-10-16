package com.pcagrade.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * TeamDto - Data Transfer Objects for Team entities
 * Contains nested classes for different use cases (Request, Response)
 */
public class TeamDto {

    /**
     * Request DTO for creating or updating a Team
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String name;
        private String description;
        private Integer permissionLevel;
    }

    /**
     * Response DTO for returning Team data
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String name;
        private String description;
        private Integer permissionLevel;
        private Boolean active;
        private LocalDateTime creationDate;
        private LocalDateTime modificationDate;
    }

    /**
     * Summary DTO for list views (minimal data)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private UUID id;
        private String name;
        private Integer memberCount;
        private Boolean active;
    }

    /**
     * Detailed DTO with member information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Detailed extends Response {
        private Integer memberCount;
        private Integer activeAssignments;
    }
}