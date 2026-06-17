# 🎫 Real-Time Scanner - Quick Start Guide

## ✅ What Was Added

You now have a **complete real-time scanner system** with three features:

### 1. **Real-Time Ticket Verification** ✅
- Scan QR codes at entrance gates
- Verify tickets instantly against database
- Prevent duplicate entries (tickets marked as "used")
- Works via API or command-line

### 2. **Real-Time Seat Availability** ✅
- Monitor seat availability live
- Get detailed seat layout with booking status
- Track occupancy percentage in real-time
- Perfect for box office displays

### 3. **Admin Command-Line Scanner** ✅
- No web UI needed for entry gate staff
- Single and bulk ticket verification
- Real-time statistics display
- Interactive menu-based interface

---

## 🗂️ Files Created

```
src/main/java/com/jsp/book/
├── util/
│   ├── RealTimeScanner.java              ← Core scanner for reading QR codes
│   └── AdminTicketScanner.java           ← CLI interface for staff
├── service/
│   ├── TicketVerificationService.java    ← Verify tickets against database
│   └── SeatAvailabilityService.java      ← Monitor seat availability
└── controller/
    └── ScannerController.java             ← REST API endpoints

Updated Files:
├── entity/BookedTicket.java              ← Added: isUsed, usedAt fields
└── repository/TicketRepository.java      ← Added: verification query methods
```

---

## 🚀 Getting Started (3 Steps)

### Step 1: Rebuild Project
```bash
mvn clean install
```

### Step 2: Run Application
```bash
mvn spring-boot:run
```

The application will auto-create new database columns (`is_used`, `used_at`) in `booked_ticket` table.

### Step 3: Test the API
```bash
# Test single ticket verification
curl -X POST http://localhost:8080/api/scanner/verify \
  -H "Content-Type: application/json" \
  -d '{
    "movieName": "Inception",
    "theaterName": "INOX",
    "showTiming": "08:00 PM",
    "seatNumbers": "A1,A2"
  }'

# Test real-time seat availability
curl http://localhost:8080/api/scanner/seats/1/availability

# Test service health
curl http://localhost:8080/api/scanner/health
```

---

## 🎯 Common Use Cases

### Use Case 1: Entry Gate Verification
```
1. Staff scans QR code from ticket
2. System verifies ticket validity
3. If valid → Entry GRANTED ✅
4. If used → Entry DENIED ⚠️
5. If invalid → Entry DENIED ❌
```

**REST Endpoint:** `POST /api/scanner/verify`

---

### Use Case 2: Box Office Real-Time Display
```
1. Box office staff opens booking page
2. Frontend displays: "65 seats available (35% full)"
3. As customers book → numbers update live
4. Helps staff know which shows have seats
```

**REST Endpoint:** `GET /api/scanner/seats/{showId}/availability`

---

### Use Case 3: Staff CLI Scanner (No Web UI)
```
Enable in AdminTicketScanner.java:
private static final boolean ENABLED = true;

On app startup, staff sees menu:
  📋 Scanner Commands:
     scan  - Scan a single ticket
     bulk  - Scan multiple tickets
     stats - Show verification statistics
     exit  - Exit scanner

Then just type: scan
Then scan QR code or paste data
```

---

## 📱 REST API Reference

### Verify Ticket
```
POST /api/scanner/verify
Body: {movieName, theaterName, showTiming, seatNumbers}
Response: {isValid, status, message, ticket, verifiedAt}
```

### Get Show Availability
```
GET /api/scanner/seats/{showId}/availability
Response: {totalSeats, bookedSeats, availableSeats, occupancyPercentage}
```

### Get Seat Layout
```
GET /api/scanner/seats/{showId}/layout
Response: {seatLayout} - Organized by row and position
```

### Get All Shows Availability
```
GET /api/scanner/seats/availability/all
Response: [Array of all shows with availability]
```

### Get Verification Stats
```
GET /api/scanner/stats
Response: {totalTickets, usedTickets, availableTickets}
```

### Health Check
```
GET /api/scanner/health
Response: "✅ Scanner service is running"
```

---

## 🔧 Configuration

Optional: Add to `application.properties`
```properties
# Scanner settings
scanner.enabled=true
scanner.qr.format=FULL_DETAILS

# Real-time updates
seat.update.interval=5000
seat.cache.enabled=true
```

---

## 🧪 Quick Test Scenarios

### Test 1: Book a Ticket, Then Verify It
1. User books seats (A1, A2) for "Inception" at "INOX" showing at "8:00 PM"
2. System generates QR code with: `Inception | INOX | 8:00 PM | A1,A2`
3. At gate, staff scans QR
4. API call: `POST /api/scanner/verify` with that QR data
5. Expected: `✅ Ticket verified! Entry granted.`

### Test 2: Try to Enter with Same Ticket Twice
1. First entry: Ticket verified ✅
2. Second entry (same ticket): `⚠️ Ticket already used on [time]`

### Test 3: Check Live Seat Availability
1. Show has 100 seats total
2. Call `GET /api/scanner/seats/1/availability`
3. See: "35 booked, 65 available (35% full)"
4. User books 5 more seats
5. Call API again after 5 seconds
6. See: "40 booked, 60 available (40% full)"

---

## 📊 Database Changes

The following columns were added to `booked_ticket` table:
- `is_used` (BOOLEAN) - Default: false
- `used_at` (TIMESTAMP) - Null until ticket is verified

Hibernate will create these automatically on startup.

---

## ⚡ Key Features Highlights

| Feature | Benefit |
|---------|---------|
| **Real-Time Verification** | Prevent fake/duplicate tickets at entry |
| **Instant Availability** | Staff knows seat counts without manual checking |
| **Audit Trail** | Every verification logged with timestamp |
| **API-Based** | Works with any frontend (web, mobile, kiosk) |
| **CLI Option** | No web UI needed for entry gate staff |
| **Scalable** | Can handle high-volume scanning |

---

## 🎓 Next: Integrate with Frontend

### React Example
```javascript
// Check seat availability
const [availability, setAvailability] = useState(null);

useEffect(() => {
  const interval = setInterval(async () => {
    const res = await fetch(`/api/scanner/seats/${showId}/availability`);
    const data = await res.json();
    setAvailability(data);
  }, 5000); // Refresh every 5 seconds
  
  return () => clearInterval(interval);
}, [showId]);

return <div>Available: {availability?.availableSeats}</div>;
```

---

## 📖 Full Documentation

See `REAL_TIME_SCANNER_GUIDE.md` for:
- Detailed component descriptions
- Complete API documentation
- Security considerations
- Performance optimization tips
- Troubleshooting guide

---

## ✅ Verification Checklist

- [x] RealTimeScanner.java created
- [x] TicketVerificationService.java created
- [x] SeatAvailabilityService.java created
- [x] ScannerController.java created
- [x] AdminTicketScanner.java created
- [x] BookedTicket.java updated (added isUsed, usedAt)
- [x] TicketRepository.java updated (added query methods)
- [x] Database migrations ready (auto-create via Hibernate)
- [x] REST API endpoints ready
- [x] Documentation complete

---

## 🚦 Status

**✅ READY TO USE!**

Simply run the application and start scanning:
```bash
mvn clean install
mvn spring-boot:run
```

Then test with:
```bash
curl http://localhost:8080/api/scanner/health
```

---

**Happy Scanning! 🎫**

