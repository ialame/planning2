package com.pcagrade.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for employee profile pictures
 * Handles upload, download, and deletion of employee photos
 */
@Slf4j
@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EmployeePhotoController {

    private final JdbcTemplate jdbcTemplate;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int THUMBNAIL_SIZE = 200; // pixels

    /**
     * Upload employee profile picture
     * POST /api/employees/{employeeId}/photo
     */
    @PostMapping("/{employeeId}/photo")
    public ResponseEntity<Map<String, Object>> uploadPhoto(
            @PathVariable String employeeId,
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("📸 Uploading photo for employee: {}", employeeId);

            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                response.put("success", false);
                response.put("error", "File size exceeds 5MB limit");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("error", "File must be an image (JPEG, PNG, GIF)");
                return ResponseEntity.badRequest().body(response);
            }

            // Read and resize image
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (originalImage == null) {
                response.put("success", false);
                response.put("error", "Invalid image file");
                return ResponseEntity.badRequest().body(response);
            }

            // Create thumbnail
            BufferedImage thumbnail = resizeImage(originalImage, THUMBNAIL_SIZE, THUMBNAIL_SIZE);

            // Convert to bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            // Format employee ID for database
            String formattedId = formatEmployeeId(employeeId);

            // Update database
            String sql = "UPDATE employee SET profile_picture = ? WHERE HEX(id) = ?";
            int updated = jdbcTemplate.update(sql, imageBytes, formattedId);

            if (updated == 0) {
                response.put("success", false);
                response.put("error", "Employee not found");
                return ResponseEntity.notFound().build();
            }

            response.put("success", true);
            response.put("message", "Photo uploaded successfully");
            response.put("size", imageBytes.length);

            log.info("✅ Photo uploaded for employee: {} ({} bytes)", employeeId, imageBytes.length);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error uploading photo for employee: {}", employeeId, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Download employee profile picture
     * GET /api/employees/{employeeId}/photo
     */
    @GetMapping("/{employeeId}/photo")
    public ResponseEntity<byte[]> downloadPhoto(@PathVariable String employeeId) {
        try {
            log.debug("📥 Downloading photo for employee: {}", employeeId);

            String formattedId = formatEmployeeId(employeeId);

            String sql = "SELECT profile_picture FROM employee WHERE HEX(id) = ?";

            byte[] photoData = jdbcTemplate.query(sql,
                    rs -> {
                        if (rs.next()) {
                            return rs.getBytes("profile_picture");
                        }
                        return null;
                    },
                    formattedId
            );

            if (photoData == null || photoData.length == 0) {
                log.debug("No photo found for employee: {}", employeeId);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(photoData);

        } catch (Exception e) {
            log.error("❌ Error downloading photo for employee: {}", employeeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete employee profile picture
     * DELETE /api/employees/{employeeId}/photo
     */
    @DeleteMapping("/{employeeId}/photo")
    public ResponseEntity<Map<String, Object>> deletePhoto(@PathVariable String employeeId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("🗑️ Deleting photo for employee: {}", employeeId);

            String formattedId = formatEmployeeId(employeeId);

            String sql = "UPDATE employee SET profile_picture = NULL WHERE HEX(id) = ?";
            int updated = jdbcTemplate.update(sql, formattedId);

            if (updated == 0) {
                response.put("success", false);
                response.put("error", "Employee not found");
                return ResponseEntity.notFound().build();
            }

            response.put("success", true);
            response.put("message", "Photo deleted successfully");

            log.info("✅ Photo deleted for employee: {}", employeeId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error deleting photo for employee: {}", employeeId, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Check if employee has a photo
     * GET /api/employees/{employeeId}/photo/exists
     */
    @GetMapping("/{employeeId}/photo/exists")
    public ResponseEntity<Map<String, Object>> hasPhoto(@PathVariable String employeeId) {
        Map<String, Object> response = new HashMap<>();

        try {
            String formattedId = formatEmployeeId(employeeId);

            String sql = "SELECT LENGTH(photo_url) as size FROM employee WHERE HEX(id) = ?";

            Integer photoSize = jdbcTemplate.query(sql,
                    rs -> {
                        if (rs.next()) {
                            return rs.getInt("size");
                        }
                        return 0;
                    },
                    formattedId
            );

            response.put("success", true);
            response.put("hasPhoto", photoSize != null && photoSize > 0);
            response.put("size", photoSize);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error checking photo for employee: {}", employeeId, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Resize image to fit within specified dimensions while maintaining aspect ratio
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Calculate new dimensions maintaining aspect ratio
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if (originalWidth > maxWidth) {
            newWidth = maxWidth;
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        if (newHeight > maxHeight) {
            newHeight = maxHeight;
            newWidth = (newHeight * originalWidth) / originalHeight;
        }

        // Create resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();

        // High quality rendering
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resizedImage;
    }

    /**
     * Format employee ID (remove hyphens if present)
     */
    private String formatEmployeeId(String employeeId) {
        return employeeId.replaceAll("-", "").toUpperCase();
    }
}