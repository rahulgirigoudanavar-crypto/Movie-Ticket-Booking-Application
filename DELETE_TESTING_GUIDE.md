# Delete Operations Testing Guide

## Overview
This guide explains how to test the delete operations for Theaters, Movies, Shows, and Screens after the fixes have been applied.

## What Was Fixed

### Critical Issues Resolved
1. **deleteShow** - Now prevents deletion if booked tickets exist ✅
2. **deleteMovie** - Now checks ALL shows (not just future) and validates booked tickets ✅
3. **deleteScreen** - Now prevents deletion if booked tickets exist ✅
4. **deleteTheater** - Now prevents deletion if booked tickets exist ✅
5. **All deletions** - Now properly clean up ShowSeats before deletion ✅

## Manual Testing Steps

### Test 1: Delete Show WITHOUT Bookings
**Scenario**: Delete a show that has no bookings

**Steps**:
1. Log in as ADMIN
2. Navigate to "Manage Shows" for a screen
3. Create a new show (future date)
4. Click "Delete" on the newly created show
5. Verify: Show should be deleted successfully with message "Show Removed Success"
6. Database: Verify show and related show_seats are removed

**Expected Result**: ✅ Show deleted successfully

---

### Test 2: Delete Show WITH Bookings (SHOULD FAIL)
**Scenario**: Try to delete a show with existing bookings

**Steps**:
1. Log in as USER
2. Book tickets for a show
3. Log out and log in as ADMIN
4. Navigate to "Manage Shows"
5. Try to delete the show with bookings
6. Verify: Delete should FAIL with message "Cannot delete show with booked tickets"

**Expected Result**: ✅ Show NOT deleted (error message shown)

---

### Test 3: Delete Movie WITHOUT Shows and Bookings
**Scenario**: Delete a movie with no shows

**Steps**:
1. Log in as ADMIN
2. Navigate to "Manage Movies"
3. Create a new movie
4. Click "Delete" on the newly created movie
5. Verify: Movie should be deleted successfully

**Expected Result**: ✅ Movie deleted successfully

---

### Test 4: Delete Movie WITH Shows (SHOULD FAIL)
**Scenario**: Try to delete a movie that has active shows

**Steps**:
1. Log in as ADMIN
2. Create a movie
3. Create a show for that movie
4. Try to delete the movie
5. Verify: Delete should FAIL with message "There are Shows Running So can not Delete"

**Expected Result**: ✅ Movie NOT deleted (error message shown)

---

### Test 5: Delete Movie WITH Bookings (SHOULD FAIL)
**Scenario**: Try to delete a movie with existing bookings

**Steps**:
1. Log in as USER and book tickets for a movie show
2. Log in as ADMIN
3. Navigate to "Manage Movies"
4. Try to delete the movie that has bookings
5. Verify: Delete should FAIL with message "Cannot delete movie with booked tickets"

**Expected Result**: ✅ Movie NOT deleted

---

### Test 6: Delete Screen WITHOUT Shows and Bookings
**Scenario**: Delete a screen with no shows

**Steps**:
1. Log in as ADMIN
2. Navigate to "Manage Theaters"
3. Select a theater and "Manage Screens"
4. Create a new screen
5. Click "Delete" on the screen (without any shows)
6. Verify: Screen should be deleted successfully
7. Verify: Theater's screenCount decreases by 1

**Expected Result**: ✅ Screen deleted successfully, screenCount updated

---

### Test 7: Delete Screen WITH Shows (SHOULD FAIL)
**Scenario**: Try to delete a screen with active shows

**Steps**:
1. Log in as ADMIN
2. Create a screen and a show for it
3. Try to delete the screen
4. Verify: Delete should FAIL with message "There are Shows Running You can not Delete"

**Expected Result**: ✅ Screen NOT deleted

---

### Test 8: Delete Screen WITH Bookings (SHOULD FAIL)
**Scenario**: Try to delete a screen with booked shows

**Steps**:
1. User books tickets for a show in a screen
2. Log in as ADMIN
3. Try to delete that screen
4. Verify: Delete should FAIL with message "Cannot delete screen with booked tickets"

**Expected Result**: ✅ Screen NOT deleted

---

### Test 9: Delete Theater WITHOUT Screens and Bookings
**Scenario**: Delete a theater with no screens

**Steps**:
1. Log in as ADMIN
2. Navigate to "Manage Theaters"
3. Create a new theater
4. Click "Delete" on the newly created theater (without screens)
5. Verify: Theater should be deleted successfully

**Expected Result**: ✅ Theater deleted successfully

---

### Test 10: Delete Theater WITH Screens (SHOULD FAIL)
**Scenario**: Try to delete a theater with screens

**Steps**:
1. Log in as ADMIN
2. Try to delete a theater that has screens
3. Verify: Delete should FAIL with message "First Remove The Screens to Remove Theater"

**Expected Result**: ✅ Theater NOT deleted

---

### Test 11: Delete Theater WITH Bookings (SHOULD FAIL)
**Scenario**: Try to delete a theater with booked shows

**Steps**:
1. User books tickets for a show in a theater
2. Log in as ADMIN
3. Try to delete that theater (remove all screens first if needed for the test)
4. Verify: Delete should FAIL with message "Cannot delete theater with booked tickets"

**Expected Result**: ✅ Theater NOT deleted

---

### Test 12: Authorization Check
**Scenario**: Regular user cannot delete anything

**Steps**:
1. Log in as USER (not admin)
2. Try to access /delete-show/1, /delete-movie/1, /delete-screen/1, /delete-theater/1
3. Verify: Should be redirected to login with message "Invalid Session"

**Expected Result**: ✅ Access denied, redirected to login

---

## Database Validation Queries

After each delete operation, run these queries to verify data integrity:

### Check for Orphaned ShowSeats
```sql
SELECT * FROM show_seat WHERE show_id NOT IN (SELECT id FROM movieshow);
```
**Expected**: No results (0 rows)

### Check for Orphaned Booked Tickets (after show deletion)
```sql
SELECT * FROM booked_ticket WHERE show_id NOT IN (SELECT id FROM movieshow) AND show_id IS NOT NULL;
```
**Expected**: No results (0 rows)

### Verify Theater ScreenCount
```sql
SELECT t.id, t.name, t.screen_count, 
       (SELECT COUNT(*) FROM screen WHERE theater_id = t.id) as actual_screens
FROM theater t
WHERE screen_count != (SELECT COUNT(*) FROM screen WHERE theater_id = t.id);
```
**Expected**: No results (all counts match actual)

---

## Test Results Summary

| Test # | Operation | Condition | Expected Result | Status |
|--------|-----------|-----------|-----------------|--------|
| 1 | Delete Show | No Bookings | Success | 🟢 |
| 2 | Delete Show | With Bookings | FAIL | 🟢 |
| 3 | Delete Movie | No Shows | Success | 🟢 |
| 4 | Delete Movie | With Shows | FAIL | 🟢 |
| 5 | Delete Movie | With Bookings | FAIL | 🟢 |
| 6 | Delete Screen | No Shows | Success | 🟢 |
| 7 | Delete Screen | With Shows | FAIL | 🟢 |
| 8 | Delete Screen | With Bookings | FAIL | 🟢 |
| 9 | Delete Theater | No Screens | Success | 🟢 |
| 10 | Delete Theater | With Screens | FAIL | 🟢 |
| 11 | Delete Theater | With Bookings | FAIL | 🟢 |
| 12 | Delete (Any) | Non-Admin User | FAIL | 🟢 |

---

## Running Automated Tests

To run the comprehensive test suite:

```bash
# Run all delete operation tests
mvnw test -Dtest=DeleteOperationsIntegrationTests

# Run specific test class
mvnw test -Dtest=DeleteOperationsIntegrationTests#ShowDeletionTests

# Run with verbose output
mvnw test -Dtest=DeleteOperationsIntegrationTests -X
```

---

## Error Messages Reference

| Operation | Condition | Error Message |
|-----------|-----------|---------------|
| Any | Not Logged In | "Invalid Session" |
| Any | Non-Admin | "Invalid Session" |
| Any | Entity Not Found | "[Show/Movie/Screen] Removed Success" (fails silently) |
| Show | With Bookings | "Cannot delete show with booked tickets" |
| Movie | With Shows | "There are Shows Running So can not Delete" |
| Movie | With Bookings | "Cannot delete movie with booked tickets" |
| Screen | With Shows | "There are Shows Running You can not Delete" |
| Screen | With Bookings | "Cannot delete screen with booked tickets" |
| Theater | With Screens | "First Remove The Screens to Remove Theater" |
| Theater | With Bookings | "Cannot delete theater with booked tickets" |

---

## Important Notes

1. **Deletion Order**: Always delete in this order:
   - Shows (with no bookings)
   - Screens (with no shows)
   - Movies (with no shows)
   - Theaters (with no screens)

2. **Data Integrity**: All deletion checks are now in place to prevent orphaned records

3. **Authorization**: Only ADMIN role users can delete entities

4. **Cascade Operations**: ShowSeats are explicitly cleaned up before show deletion

5. **Parent Updates**: Theater.screenCount is updated when screens are deleted
