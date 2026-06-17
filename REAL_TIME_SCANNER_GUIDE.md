# 🎫 Real-Time Scanner Implementation Guide

## Overview
This document explains the complete real-time scanner system for **Book My Ticket** with three core features:

1. **Real-time Ticket Verification** - Verify tickets at entrance gates
2. **Real-time Seat Scanner** - Monitor seat availability live
3. **Admin Ticket Scanner** - Command-line interface for entry gate staff

---

## 📋 Components Added

### 1. **RealTimeScanner** (`util/RealTimeScanner.java`)
Core scanner utility that reads and parses QR codes.

**Methods:**
- `scanQRCode()` - Reads QR code from scanner/console
- `isValidQRFormat()` - Validates QR code format
- `parseQRCode()` - Extracts data from QR (MovieName | TheaterName | ShowTiming | Seats)
- `continuousScan()` - Batch scanning mode

---

### 2. **TicketVerificationService** (`service/TicketVerificationService.java`)
Handles ticket validation against database.

**Key Methods:**
```java
verifyTicket(QRData qrData) // Verify single ticket
bulkVerify(QRData[] qrDataArray) // Verify multiple tickets
getStats() // Get verification statistics
```

**Return Object:**
```java
TicketVerificationResult {
  - isValid: boolean
  - status: String (VALID, NOT_FOUND, ALREADY_USED)
  - message: String
  - ticket: BookedTicket
  - verifiedAt: LocalDateTime
}
```

---

### 3. **SeatAvailabilityService** (`service/SeatAvailabilityService.java`)
Provides real-time seat status monitoring.

**Key Methods:**
```java
getShowAvailability(Long showId) // Get availability snapshot
getAllShowsAvailability() // Get all shows status
getSeatLayout(Long showId) // Get detailed seat layout
getRecentChanges(Long showId, LocalDateTime lastCheck) // Delta updates
```

---

### 4. **ScannerController** (`controller/ScannerController.java`)
REST API endpoints for scanner operations.

**Endpoints:**

#### Ticket Verification
- `POST /api/scanner/verify` - Verify single ticket
- `POST /api/scanner/verify-bulk` - Verify multiple tickets
- `GET /api/scanner/stats` - Get verification stats

#### Seat Availability
- `GET /api/scanner/seats/{showId}/availability` - Get show availability
- `GET /api/scanner/seats/availability/all` - Get all shows availability
- `GET /api/scanner/seats/{showId}/layout` - Get detailed seat layout
- `GET /api/scanner/seats/{showId}/changes` - Get recent seat changes

#### Health
- `GET /api/scanner/health` - Service health check

---

### 5. **AdminTicketScanner** (`util/AdminTicketScanner.java`)
Command-line interactive scanner for entry gate staff.

**Features:**
- Single ticket scanning
- Bulk ticket verification
- Live statistics display
- Real-time feedback

---

## 🗄️ Database Changes

### BookedTicket Entity
**New Fields Added:**
```java
private boolean isUsed = false;      // Track if ticket is used/verified
private LocalDateTime usedAt;         // Timestamp of verification
```

### TicketRepository
**New Methods:**
```java
findByMovieNameAndTheaterNameAndSeatNumberContaining()
countByIsUsedTrue()
countUsedTickets()
```

---

## 🚀 How to Use

### 1️⃣ Enable Admin Ticket Scanner (Optional)

Edit `AdminTicketScanner.java`:
```java
// Change this:
private static final boolean ENABLED = false;

// To this:
private static final boolean ENABLED = true;
```

Or use environment variable:
```bash
export ENABLE_TICKET_SCANNER=true
```

When enabled, you'll see interactive menu on application startup:
```
╔════════════════════════════════════════╗
║  🎫 REAL-TIME TICKET SCANNER           ║
║  Entry Gate Verification System        ║
╚════════════════════════════════════════╝

📋 Scanner Commands:
   scan  - Scan a single ticket
   bulk  - Scan multiple tickets
   stats - Show verification statistics
   exit  - Exit scanner
```

---

### 2️⃣ REST API Usage

#### Verify a Single Ticket
```bash
curl -X POST http://localhost:8080/api/scanner/verify \
  -H "Content-Type: application/json" \
  -d '{
    "movieName": "Inception",
    "theaterName": "INOX",
    "showTiming": "08:00 PM",
    "seatNumbers": "A1,A2"
  }'
```

**Response:**
```json
{
  "isValid": true,
  "status": "VALID",
  "message": "✅ Ticket verified! Entry granted.",
  "verifiedAt": "2026-05-08T20:15:30",
  "ticket": {
    "id": 1,
    "movieName": "Inception",
    "theaterName": "INOX",
    "seatNumber": ["A1", "A2"],
    "showDate": "2026-05-08",
    "showTiming": "08:00 PM"
  }
}
```

#### Get Real-Time Seat Availability
```bash
curl http://localhost:8080/api/scanner/seats/1/availability
```

**Response:**
```json
{
  "showId": 1,
  "movieName": "Inception",
  "screenName": "Screen 1",
  "showTiming": "08:00 PM",
  "totalSeats": 100,
  "bookedSeats": 35,
  "availableSeats": 65,
  "occupancyPercentage": 35.0,
  "categoryStatus": {
    "STANDARD": {
      "category": "STANDARD",
      "totalSeats": 60,
      "bookedSeats": 20,
      "availableSeats": 40
    },
    "PREMIUM": {
      "category": "PREMIUM",
      "totalSeats": 30,
      "bookedSeats": 12,
      "availableSeats": 18
    }
  },
  "lastUpdated": "2026-05-08T20:15:30"
}
```

#### Get Detailed Seat Layout
```bash
curl http://localhost:8080/api/scanner/seats/1/layout
```

**Response:**
```json
{
  "showId": 1,
  "seatLayout": {
    "A": {
      "1": {
        "seatNumber": "A1",
        "isBooked": true,
        "category": "STANDARD"
      },
      "2": {
        "seatNumber": "A2",
        "isBooked": true,
        "category": "STANDARD"
      },
      "3": {
        "seatNumber": "A3",
        "isBooked": false,
        "category": "STANDARD"
      }
    }
  },
  "lastUpdated": "2026-05-08T20:15:30"
}
```

#### Get Verification Statistics
```bash
curl http://localhost:8080/api/scanner/stats
```

**Response:**
```json
{
  "totalTickets": 150,
  "usedTickets": 95,
  "availableTickets": 55,
  "scanTime": "2026-05-08T20:15:30"
}
```

---

## 🎯 Typical Flow

### Entry Gate Verification Flow:
```
1. Visitor arrives at gate
2. Staff scans QR code on ticket
3. Scanner reads: "Inception | INOX | 8:00 PM | A1,A2"
4. System verifies against database
5. If valid: ✅ Entry GRANTED
   If invalid: ❌ Entry DENIED with reason
6. Ticket marked as "used" with timestamp
```

### Real-Time Availability Monitoring:
```
1. Box office staff opens booking page
2. Frontend calls GET /api/scanner/seats/{showId}/availability
3. Shows live seat count and occupancy %
4. As bookings happen, page auto-refreshes (polling or WebSocket)
5. Shows available seats in real-time
```

---

## 🔒 Security Considerations

1. **Ticket Tampering**: QR codes are generated server-side and validated against database
2. **Double Entry**: Tickets marked as `isUsed` prevent duplicate entries
3. **Authorization**: Verification endpoints should be protected (add `@PreAuthorize`)
4. **Audit Trail**: All verifications logged with timestamp and staff ID

---

## 📱 Frontend Integration Examples

### JavaScript - Verify Ticket
```javascript
async function verifyTicket(qrData) {
  const response = await fetch('/api/scanner/verify', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(qrData)
  });
  
  const result = await response.json();
  
  if (result.isValid) {
    showSuccess(`✅ ${result.message}`);
  } else {
    showError(`❌ ${result.message}`);
  }
}
```

### JavaScript - Real-Time Seat Availability (Poll every 5 seconds)
```javascript
setInterval(async () => {
  const response = await fetch(`/api/scanner/seats/${showId}/availability`);
  const availability = await response.json();
  
  document.getElementById('available').textContent = availability.availableSeats;
  document.getElementById('booked').textContent = availability.bookedSeats;
  document.getElementById('occupancy').textContent = 
    availability.occupancyPercentage.toFixed(1) + '%';
}, 5000);
```

---

## 🧪 Testing

### Test 1: Single Ticket Verification
```bash
# Manual scan via command line
Command: scan
Enter: Inception | INOX | 8:00 PM | A1,A2
Expected: ✅ Ticket verified
```

### Test 2: Already Used Ticket
```bash
# Scan same ticket twice
Expected: ⚠️ Ticket already used on: [timestamp]
```

### Test 3: Invalid QR Format
```bash
# Enter invalid data
Enter: Invalid data here
Expected: ❌ Invalid QR format!
```

### Test 4: Real-Time Availability
```bash
# Open two terminals
Terminal 1: POST /api/scanner/verify (book seats)
Terminal 2: GET /api/scanner/seats/1/availability (check availability)
Expected: availableSeats decreases in real-time
```

---

## 📊 Database Migration

Since new fields were added to `BookedTicket`, create migration script:

```sql
ALTER TABLE booked_ticket ADD COLUMN is_used BOOLEAN DEFAULT FALSE;
ALTER TABLE booked_ticket ADD COLUMN used_at TIMESTAMP NULL;
CREATE INDEX idx_is_used ON booked_ticket(is_used);
```

For JPA with Hibernate, it will auto-create columns on application startup if using:
```properties
spring.jpa.hibernate.ddl-auto=update
```

---

## ⚙️ Configuration

Add to `application.properties`:
```properties
# Scanner settings
scanner.enabled=true
scanner.qr.format=FULL_DETAILS
scanner.verification.timeout=30s

# Real-time updates
seat.update.interval=5000
seat.cache.enabled=true
```

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Scanner not reading QR | Check System.in is accessible; disable buffering |
| "Ticket not found" error | Ensure ticket was successfully booked; check database |
| Duplicate entries | Verify isUsed field is being updated |
| Slow availability updates | Enable caching; use delta updates instead of full refresh |
| API returns 404 | Check endpoint path and showId exists in database |

---

## 📈 Performance Optimization

1. **Caching**: Cache availability for 5-10 seconds
2. **Batch Queries**: Use `bulkVerify()` for high volume
3. **Async Processing**: Mark verification as async for web
4. **WebSocket**: Consider WebSocket for real-time updates instead of polling

---

## 🎓 Next Steps

1. Run Hibernate migrations to create new DB columns
2. Enable scanner via `AdminTicketScanner.ENABLED = true`
3. Test with `curl` commands provided
4. Integrate with frontend (Vue/React)
5. Add role-based access control to endpoints
6. Monitor verification logs for analytics

---

## 📞 Support

For issues or improvements:
- Check logs in `target/` directory
- Review entity mappings in `BookedTicket.java`
- Verify repository methods in `TicketRepository.java`

