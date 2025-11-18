package com.pcagrade.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Symfony API Client
 * Consumes data export API from Symfony backend
 */
@Service
public class SymfonyApiClient {

    private static final Logger log = LoggerFactory.getLogger(SymfonyApiClient.class);

    @Value("${symfony.api.base-url:http://localhost:8000}")
    private String symfonyApiBaseUrl;

    @Value("${symfony.api.locale:fr}")
    private String symfonyApiLocale;

    @Value("${symfony.api.timeout:30}")
    private int timeoutSeconds;


    /**
     * Build API URL with locale
     */
    private String buildApiUrl(String endpoint) {
        if (endpoint.startsWith("/")) {
            endpoint = endpoint.substring(1);
        }
        return String.format("%s/%s/%s", symfonyApiBaseUrl, symfonyApiLocale, endpoint);
    }


    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SymfonyApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetch all orders from Symfony API
     */
    public List<Map<String, Object>> fetchAllOrders() {
        return fetchAllPaginated("/api/export/orders");
    }

    /**
     * Fetch orders by status
     */
    public List<Map<String, Object>> fetchOrdersByStatus(int status) {
        return fetchAllPaginated("/api/export/orders?status=" + status);
    }

    /**
     * Fetch orders modified since a specific date (incremental sync)
     */
    public List<Map<String, Object>> fetchOrdersSince(String since) {
        return fetchAllPaginated("/api/export/orders?since=" + since);
    }

    /**
     * Fetch all cards from Symfony API
     */
    public List<Map<String, Object>> fetchAllCards() {
        return fetchAllPaginated("/api/export/cards");
    }

    /**
     * Fetch all card translations from Symfony API
     */
    public List<Map<String, Object>> fetchAllCardTranslations() {
        return fetchAllPaginated("/api/export/card-translations");
    }

    /**
     * Fetch all card certifications from Symfony API
     */
    public List<Map<String, Object>> fetchAllCardCertifications() {
        return fetchAllPaginated("/api/export/card-certifications");
    }

    /**
     * Fetch all card certification orders from Symfony API
     */
    public List<Map<String, Object>> fetchAllCardCertificationOrders() {
        return fetchAllPaginated("/api/export/card-certification-orders");
    }

    /**
     * Get statistics from Symfony API
     */
    public Map<String, Object> getStats() {
        try {
            String url = buildApiUrl("api/export/stats");
            log.info("📊 Fetching stats from: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());

                if (root.get("success").asBoolean()) {
                    return objectMapper.convertValue(root.get("data"), Map.class);
                } else {
                    throw new RuntimeException("API returned success=false");
                }
            } else {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            log.error("❌ Error fetching stats from Symfony API", e);
            throw new RuntimeException("Failed to fetch stats: " + e.getMessage(), e);
        }
    }

    /**
     * Check Symfony API health
     */
    public boolean isHealthy() {
        try {
            String url = buildApiUrl("api/export/health");
            log.debug("🏥 Checking Symfony API health: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.get("success").asBoolean();
            }

            return false;

        } catch (Exception e) {
            log.warn("⚠️ Symfony API health check failed", e);
            return false;
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Fetch all pages from a paginated endpoint
     */
    private List<Map<String, Object>> fetchAllPaginated(String endpoint) {
        List<Map<String, Object>> allData = new ArrayList<>();
        int offset = 0;
        int limit = 1000;
        boolean hasMore = true;

        try {
            log.info("🔄 Starting paginated fetch from: {}", endpoint);

            while (hasMore) {
                // ✅ Utiliser buildApiUrl() pour ajouter le locale
                String url = buildApiUrl(String.format("%s?limit=%d&offset=%d", endpoint, limit, offset));

                log.debug("📥 Fetching page: {} (offset: {})", url, offset);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.error("❌ HTTP {} from {}", response.statusCode(), url);
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());

                if (!root.path("success").asBoolean(false)) {
                    log.error("❌ API returned success=false");
                    break;
                }

                JsonNode dataNode = root.get("data");
                if (dataNode != null && dataNode.isArray()) {
                    List<Map<String, Object>> pageData = objectMapper.convertValue(
                            dataNode,
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                    );
                    allData.addAll(pageData);
                    log.debug("✅ Fetched {} items (total so far: {})", pageData.size(), allData.size());
                }

                JsonNode pagination = root.get("pagination");
                hasMore = pagination != null && pagination.path("hasMore").asBoolean(false);
                offset += limit;

                // Safety limit
//                if (allData.size() > 100000) {
//                    log.warn("⚠️ Reached safety limit of 100,000 records");
//                    break;
//                }
            }

            log.info("✅ Completed fetch from {}: {} total items", endpoint, allData.size());
            return allData;

        } catch (Exception e) {
            log.error("❌ Error during paginated fetch from {}", endpoint, e);
            return allData;
        }
    }
    /**
     * Fetch all invoices from Symfony API
     */
    public List<Map<String, Object>> fetchAllInvoices() {
        return fetchAllPaginated("/api/export/invoices");
    }

    /**
     * Fetch invoices for active orders only
     */
    public List<Map<String, Object>> fetchInvoicesForActiveOrders() {
        return fetchAllPaginated("/api/export/invoices-for-active-orders");
    }

    /**
     * Fetch a single page of invoices
     * Useful for large datasets
     */
    public Map<String, Object> fetchInvoicesPage(int limit, int offset) {
        try {
            String url = String.format("%s/api/export/invoices?limit=%d&offset=%d",
                    symfonyApiBaseUrl, limit, offset);

            log.debug("🔍 Fetching invoices page: limit={}, offset={}", limit, offset);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());

                Map<String, Object> result = new HashMap<>();
                result.put("data", objectMapper.convertValue(
                        root.get("data"),
                        List.class
                ));
                result.put("pagination", objectMapper.convertValue(
                        root.get("pagination"),
                        Map.class
                ));

                return result;
            } else {
                log.error("❌ Failed to fetch invoices: HTTP {}", response.statusCode());
                throw new RuntimeException("HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error fetching invoices page", e);
            throw new RuntimeException("Failed to fetch invoices: " + e.getMessage(), e);
        }
    }

    /**
     * Count invoices in Symfony database
     */
    public int countInvoices() {
        try {
            String url = buildApiUrl("api/export/invoices?limit=1&offset=0");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("pagination").path("total").asInt(0);
            }

            return 0;

        } catch (Exception e) {
            log.error("❌ Error counting invoices", e);
            return 0;
        }
    }



}
/*
On passe maintenant au frontend (views/DataSync.vue):
 je n'ai plus les statistiques de synchronisation. Voici une copie d'écran:

 */