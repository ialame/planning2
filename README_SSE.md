# 🚀 SSE Progress Bar Implementation Guide

## Overview

This guide explains how to implement real-time progress tracking for synchronization operations using **Server-Sent Events (SSE)**.

## Architecture

```
┌─────────────┐         SSE Stream          ┌──────────────┐
│   Frontend  │ ◄───────────────────────── │   Backend    │
│  (Vue.js)   │                             │ (Spring Boot)│
│             │                             │              │
│ EventSource │ ◄── Progress Events ────── │ SSE Publisher│
└─────────────┘                             └──────────────┘
                                                    │
                                                    ▼
                                            ┌──────────────┐
                                            │ Sync Service │
                                            │ (publishes   │
                                            │  progress)   │
                                            └──────────────┘
```

## 📦 Backend Implementation

### Step 1: Add Dependencies

Make sure you have Spring WebFlux for reactive support in your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### Step 2: Create Model Package (if not exists)

Create directory: `src/main/java/com/pcagrade/order/model/`

### Step 3: Add Files

Add these files to your backend:

1. **`src/main/java/com/pcagrade/order/model/SyncProgress.java`**
    - Model representing progress events
    - Copy from artifact "SyncProgress.java - Progress Event Model"

2. **`src/main/java/com/pcagrade/order/service/SyncProgressPublisher.java`**
    - Service managing SSE streams
    - Copy from artifact "SyncProgressPublisher.java - SSE Publisher Service"

3. **`src/main/java/com/pcagrade/order/controller/SyncProgressController.java`**
    - Controller exposing SSE endpoints
    - Copy from artifact "SyncProgressController.java - SSE Endpoint"

4. **Replace `src/main/java/com/pcagrade/order/controller/MinimalSyncController.java`**
    - Updated controller with progress publishing
    - Copy from artifact "MinimalSyncController.java - With SSE Progress"
    - ⚠️ **IMPORTANT**: Keep your existing `createOrUpdateOrder()` method implementation!

### Step 4: Update MinimalSyncController

In the updated `MinimalSyncController.java`, find the placeholder method:

```java
private Order createOrUpdateOrder(Map<String, Object> orderData) {
    // Your existing implementation here
    Order order = new Order();
    // ... populate order fields
    return order;
}
```

**Replace this with your actual implementation from your current MinimalSyncController!**

### Step 5: Verify Dependencies

The `MinimalSyncController` constructor should now include `SyncProgressPublisher`:

```java
public MinimalSyncController(
    OrderRepository orderRepository,
    CardCertificationSyncService cardSyncService,
    TranslationSyncService translationSyncService,
    SyncProgressPublisher progressPublisher,  // ← NEW
    RestTemplate restTemplate) {
    // ...
}
```

### Step 6: Restart Backend

```bash
./mvnw clean spring-boot:run
```

## 🎨 Frontend Implementation

### Step 1: Replace DataSync.vue

Replace your existing `src/main/frontend/src/views/DataSync.vue` with the content from artifact "DataSync.vue - With Real SSE Progress"

### Step 2: Verify API Base URL

Check that your `.env` file has the correct backend URL:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### Step 3: Restart Frontend

```bash
cd src/main/frontend
npm run dev
```

## 🧪 Testing

### Test 1: Check SSE Endpoint

```bash
curl http://localhost:8080/api/sync/progress/stats
```

**Expected response:**
```json
{
  "activeSessions": 0,
  "systemStatus": "IDLE"
}
```

### Test 2: Test SSE Stream

Open a new terminal and run:

```bash
curl -N http://localhost:8080/api/sync/progress/stream/test-123
```

This should establish an SSE connection. You should see keep-alive events every 15 seconds.

### Test 3: Full Sync Test

1. Open browser to `http://localhost:3000/sync`
2. Open browser DevTools → Network tab
3. Click "🚀 Sync All Data"
4. You should see:
    - A new `stream/sync-xxx` EventSource connection
    - Real-time progress updates in the UI
    - Progress events in the Network tab

### Test 4: Check Backend Logs

You should see logs like:

```
📡 Client connected to SSE stream for sync: sync-1697...
🔄 Starting complete synchronization with syncId: sync-1697...
📦 Received 1500 orders from Symfony
📊 Published progress for sync sync-1697...: 50% - Processing orders (750/1500)
✅ Orders sync completed: 1500 orders synced in 45000ms
✅ SSE stream completed for sync: sync-1697...
```

## 📡 API Endpoints

### SSE Stream Endpoint

```
GET /api/sync/progress/stream/{syncId}
Content-Type: text/event-stream
```

**Response:** Stream of ServerSentEvent with progress data

### Session Status

```
GET /api/sync/progress/status/{syncId}
```

**Response:**
```json
{
  "syncId": "sync-123",
  "active": true,
  "status": "ACTIVE"
}
```

### Statistics

```
GET /api/sync/progress/stats
```

**Response:**
```json
{
  "activeSessions": 2,
  "systemStatus": "SYNCING"
}
```

### Sync Operations (Modified)

All sync endpoints now accept an optional `syncId` parameter:

```
POST /api/sync/all?syncId=my-custom-id
POST /api/sync/orders?syncId=my-custom-id
POST /api/sync/cards?syncId=my-custom-id
POST /api/sync/incremental?syncId=my-custom-id
```

If `syncId` is not provided, one is automatically generated.

## 🎯 Progress Events Structure

### Event Types

**Starting:**
```json
{
  "syncId": "sync-123",
  "operationType": "ORDERS",
  "currentOperation": "Starting orders synchronization...",
  "phase": "STARTING",
  "percentage": 0,
  "message": "Starting orders synchronization...",
  "completed": false,
  "error": false
}
```

**Fetching:**
```json
{
  "syncId": "sync-123",
  "operationType": "ORDERS",
  "currentOperation": "Fetching orders from Symfony API...",
  "phase": "FETCHING",
  "percentage": 5,
  "message": "Fetching orders from Symfony API...",
  "completed": false,
  "error": false
}
```

**Processing:**
```json
{
  "syncId": "sync-123",
  "operationType": "ORDERS",
  "currentOperation": "Processing orders",
  "phase": "PROCESSING",
  "percentage": 50,
  "itemsProcessed": 750,
  "totalItems": 1500,
  "estimatedSecondsRemaining": 30,
  "message": "Processing orders (750/1500)",
  "completed": false,
  "error": false
}
```

**Completed:**
```json
{
  "syncId": "sync-123",
  "operationType": "ORDERS",
  "currentOperation": "Completed",
  "phase": "COMPLETED",
  "percentage": 100,
  "itemsProcessed": 1500,
  "totalItems": 1500,
  "message": "Synced 1500 orders in 45.0 seconds",
  "completed": true,
  "error": false
}
```

**Error:**
```json
{
  "syncId": "sync-123",
  "operationType": "ORDERS",
  "currentOperation": "Error occurred",
  "phase": "ERROR",
  "message": "Synchronization failed",
  "errorMessage": "Cannot connect to Symfony API",
  "completed": true,
  "error": true
}
```

## 🔧 Customization

### Adjust Progress Update Frequency

In `MinimalSyncController.java`, change the batch size for progress updates:

```java
// Publish progress every 100 orders (default)
if ((i + 1) % 100 == 0 || i == ordersData.size() - 1) {
    progressPublisher.publishProgress(syncId, ...);
}

// Publish more frequently (every 50 orders)
if ((i + 1) % 50 == 0 || i == ordersData.size() - 1) {
    progressPublisher.publishProgress(syncId, ...);
}
```

### Adjust Keep-Alive Interval

In `SyncProgressPublisher.java`:

```java
// Default: 15 seconds
private static final Duration KEEP_ALIVE_INTERVAL = Duration.ofSeconds(15);

// More frequent: 10 seconds
private static final Duration KEEP_ALIVE_INTERVAL = Duration.ofSeconds(10);
```

### Custom Progress Calculation

Modify the `SyncProgress.processing()` factory method to customize percentage calculation:

```java
// Current: 10% fetching, 80% processing, 10% saving
int processingPercentage = total > 0 ? (int) ((processed * 80.0) / total) : 0;
progress.setPercentage(10 + processingPercentage);

// Alternative: 5% fetching, 90% processing, 5% saving
int processingPercentage = total > 0 ? (int) ((processed * 90.0) / total) : 0;
progress.setPercentage(5 + processingPercentage);
```

## 🐛 Troubleshooting

### Problem: SSE Connection Fails

**Symptoms:** No progress updates in frontend

**Solutions:**
1. Check CORS settings in `SyncProgressController`
2. Verify backend is running and accessible
3. Check browser console for connection errors
4. Test SSE endpoint with curl: `curl -N http://localhost:8080/api/sync/progress/stream/test`

### Problem: Progress Stuck at 0%

**Symptoms:** Progress bar appears but stays at 0%

**Solutions:**
1. Check backend logs for progress publishing
2. Verify `SyncProgressPublisher` is being called
3. Check if syncId matches between frontend and backend
4. Look for errors in browser console

### Problem: Multiple Active Connections

**Symptoms:** Old SSE connections not closing

**Solutions:**
1. Frontend automatically closes connections on unmount
2. Manually check: `curl http://localhost:8080/api/sync/progress/stats`
3. Restart backend to clean up all sessions

### Problem: Progress Events Out of Order

**Symptoms:** Progress jumps around

**Solutions:**
1. SSE guarantees ordered delivery
2. Check for multiple concurrent syncs with same syncId
3. Ensure unique syncId per sync operation

## 📊 Performance Considerations

### Memory Usage

- Each active SSE connection uses ~1-2MB of memory
- Connections are cleaned up automatically when:
    - Sync completes
    - Client disconnects
    - Error occurs

### Network Bandwidth

- Progress events are small (~200-500 bytes each)
- Keep-alive events every 15 seconds (~100 bytes)
- For 100 concurrent syncs: ~1-2 Mbps total

### Scalability

For high-traffic scenarios:

1. **Use Redis for pub/sub**:
    - Store progress in Redis
    - Publish events via Redis pub/sub
    - Scale horizontally

2. **Implement connection pooling**:
    - Limit max concurrent SSE connections
    - Queue sync requests when limit reached

3. **Add rate limiting**:
    - Limit syncs per user
    - Implement backoff for failed syncs

## ✅ Checklist

- [ ] Backend dependencies added
- [ ] `SyncProgress.java` added
- [ ] `SyncProgressPublisher.java` added
- [ ] `SyncProgressController.java` added
- [ ] `MinimalSyncController.java` updated (with existing `createOrUpdateOrder` preserved)
- [ ] Backend restarted successfully
- [ ] `DataSync.vue` replaced
- [ ] Frontend restarted successfully
- [ ] Tested SSE endpoint with curl
- [ ] Tested full sync in browser
- [ ] Verified progress updates in DevTools
- [ ] Checked backend logs for progress publishing

## 🎉 Success Criteria

Your implementation is successful when:

1. ✅ Progress bar appears immediately when sync starts
2. ✅ Progress updates in real-time (not simulated)
3. ✅ Items processed counter shows accurate numbers
4. ✅ Estimated time remaining updates dynamically
5. ✅ Phase badges show current operation (FETCHING, PROCESSING, etc.)
6. ✅ Progress reaches 100% when sync completes
7. ✅ No console errors
8. ✅ Backend logs show SSE connection established/closed

## 📚 Additional Resources

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Server-Sent Events (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [EventSource API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/EventSource)
- [Reactor Core Documentation](https://projectreactor.io/docs/core/release/reference/)

## 🆘 Support

If you encounter issues:

1. Check this guide's troubleshooting section
2. Review backend logs for errors
3. Check browser DevTools console and Network tab
4. Test SSE endpoint independently with curl
5. Verify all files are in correct locations

---

**Happy Syncing! 🚀**