# Rental Booking — test plan (Vendor app)

**Prepared** 22 Sep 2026 · **Status** executed complete 24 Sep 2026 — **82/82** (S15 + E4 BLOCKED)
**Account** `9000000001` (rental company, OTP `1234`) · **Package** `com.l2b.app.qa` · **Env** `https://qa.waardian.com`
**Zero-booking control** `9000000003`
**Execution** one isolated suite at a time, against real bookings placed from the customer web.

**Result summary (24 Sep):** RB-L 16 · RB-I 16 · RB-S 16 (S15 BLOCKED) · RB-E 20 (E4 BLOCKED) · RB-A 14.
Open Bookings bugs **#27–#34**. S15/E4 need a near-expiry rental Timer seed (Timer restarts on relaunch — #28).

This plan covers the rental booking lifecycle from the **vendor** side only. Material orders
(`9000000017`) stay out of this track. Quick Booking screen ownership stays with the Quick Booking
module; this plan uses it only as the surface where an incoming rental request appears.

---

## 1. Where the code lives

| Piece | Path |
|---|---|
| Page object (read-only) | `src/test/java/.../modules/bookings/presentation/pages/RentalBookingsPage.java` |
| Base entry (on hold) | `.../tests/rentalbooking/RentalBookingBaseTest.java` |
| RB-L landing | `.../tests/rentalbooking/RentalBookingLandingTest.java` |
| RB-I interact | `.../tests/rentalbooking/RentalBookingInteractTest.java` |
| RB-S lifecycle | `.../tests/rentalbooking/RentalBookingLifecycleTest.java` |
| RB-E edges | `.../tests/rentalbooking/RentalBookingEdgeTest.java` |
| RB-A API | `.../tests/rentalbooking/RentalBookingApiTest.java` |
| Isolated suites | `src/test/resources/bookings/booking-rental-*.xml` (82 files) |
| Existing API surface | `.../modules/bookings/data/api/BookingsApi.java` (unchanged) |
| Existing environment | `.../modules/bookings/domain/BookingsEnvironment.java` (unchanged) |

All 82 plan cases are implemented and have been run (isolated suites). `RentalBookingApiTest`
stubs remain `enabled = false` only as redirects to `RentalBookingApiGapTest` /
`RentalBookingLifecycleApiTest`. S15 and E4 throw `SkipException` BLOCKED until a near-expiry
Timer seed exists.

---

## 2. Screen inventory (from live dumps, 21 Sep)

Source: `/tmp/l2b-rental-flow-0001-20260921/` — `11-see-all-upcoming`, `12-bookings-active`,
`13-bookings-completed`, `14-bookings-upcoming`, `16-first-upcoming-card`, `17-see-all-booking-orders`.

**Chrome:** `Back` (content-desc) · header title · `Help` · tabs `Upcoming` / `Active` / `Completed`
· scrollable list body.

**Header title tracks the tab:** `Upcoming Booking` · `Active Order` · `Completed Order`.

**Upcoming card:** machine title (`Excavator 20 Tonnes`, `14 Ft Truck`) · source tag `Quick Booking`
· `Amount · Online Mode ₹102,150` · `27 Aug 11:36 AM - 29 Aug 2026 11:36 AM (2 days)` · Google Map
tile · `Get Direction` · either `Operator : <name>` + `Change` or `Operator Not Assigned` + `Assign`
· plate (`KA13Z2117`, `MH12SD4444`) · `Booking for` + address · `View More Details`.

**Active tab:** empty state `No active bookings.`

**Completed card:** machine · `31 Aug - 31 Aug 2026 (1 day)` · `₹1,02,000` (no actions).

**Already visible oddities to confirm on execution day** — each becomes its own new Bookings number,
never merged into an existing bug:

- `Address not provided` on an upcoming card (`16-first-upcoming-card`).
- Upcoming uses Western grouping `₹102,150`, Completed uses Indian grouping `₹1,02,000`.
- Completed cards carry no booking reference or customer identity.

---

## 3. User activities (what a vendor can do)

| # | User activity | Surface |
|---|---|---|
| UA-1 | Open Bookings from Home → Upcoming Booking → See all | Home |
| UA-2 | Switch between Upcoming / Active / Completed | Bookings |
| UA-3 | Scroll the list | Bookings |
| UA-4 | Expand / collapse View More Details | Booking card |
| UA-5 | Tap Get Direction or the map tile | Booking card |
| UA-6 | Tap Assign on an unassigned booking, pick machine + operator, Confirm | Assign sheet |
| UA-7 | Tap Change on an assigned booking, pick another operator | Operator sheet |
| UA-8 | Accept an incoming request | Booking Orders / Quick Booking |
| UA-9 | Decline an incoming request and pick a reason | Decline dialog |
| UA-10 | Accept or decline an extend-time request | Extend-time dialog (#15 path) |
| UA-11 | Leave via header Back or device Back | Bookings |
| UA-12 | Open Help from the Bookings header | Bookings |
| UA-13 | Refresh (pull / re-enter / relaunch) | Bookings, Home |
| UA-14 | Tap a Completed card | Completed tab |
| UA-15 | Background, foreground, rotate, go offline mid-action | Device level |

## 4. App activities (what the client does)

| # | App activity | Observable |
|---|---|---|
| AA-1 | Fetch the booking list on entry and on tab switch | Skeleton → cards, or empty state |
| AA-2 | Bucket bookings into the three tabs by status | Tab contents, header title |
| AA-3 | Format amounts and date ranges | `₹` grouping, `(N days)` suffix |
| AA-4 | Render map tile per booking location | Google Map TextureView |
| AA-5 | Decide assigned vs unassigned operator row | `Change` vs `Assign` |
| AA-6 | Run the 30:00 countdown on incoming requests | Timer text |
| AA-7 | Submit accept / decline / assign and reconcile the list | Card disappears or updates |
| AA-8 | Refresh Home stats after a state change | Upcoming Booking count, Earning Projected |
| AA-9 | Handle offline, error, and empty responses | Error copy vs empty copy |
| AA-10 | Restore state after background, rotation, relaunch | Tab, scroll, in-flight action |
| AA-11 | Handle token refresh and expiry | Re-auth vs silent empty list |
| AA-12 | Receive push / in-app notification of a new request | Notification, Booking Orders feed |

## 5. Backend activities (what must be true server-side)

| # | Backend activity | Endpoint |
|---|---|---|
| BA-1 | Serve the vendor's bookings with status | `GET /api/v1/rentals/vendor/bookings` |
| BA-2 | Serve one booking's detail | `GET /api/v1/rentals/bookings/{booking_id}` |
| BA-3 | Transition pending → accepted, once only | `POST .../bookings/{id}/accept` |
| BA-4 | Record a decline with reason | `POST .../bookings/{id}/decline` |
| BA-5 | Persist machine + operator assignment | `POST .../bookings/{id}/assign` |
| BA-6 | Expire an untouched request at 00:00 | list status |
| BA-7 | Reflect customer-side cancellation | list / detail |
| BA-8 | Keep dashboard and stats in step | `GET /api/v1/rentals/vendor/dashboard`, `/stats` |
| BA-9 | Reflect the slot on the vendor schedule | `GET /api/v1/rentals/vendor/schedule` |
| BA-10 | Reflect projected and settled earnings | `GET /api/v1/wallet/earning-summary`, `/earnings` |
| BA-11 | Mark the operator busy for the slot | `GET /api/v1/rentals/vendor/team`, operator schedule |
| BA-12 | Produce an invoice for a completed booking | `GET .../bookings/{id}/invoice` |
| BA-13 | Enforce ownership and auth on every booking route | 401 / 403 / 404 |
| BA-14 | Emit the vendor notification for a new request | `GET /api/v1/notifications/feed` |

---

## 6. Case list — 82 cases

### RB-L · list and visibility (16, read-only)

| ID | Case |
|---|---|
| RB-L1 | Home Upcoming See all opens the Bookings list |
| RB-L2 | Three tabs exist, Upcoming is the default |
| RB-L3 | Bookings is not Home and not Quick Booking |
| RB-L4 | Upcoming card core fields: machine, source tag, amount, date range |
| RB-L5 | Map tile and Get Direction render |
| RB-L6 | Operator row is assigned-or-unassigned, never both |
| RB-L7 | Plate matches the fleet plate shape |
| RB-L8 | Booking for address is present and non-empty |
| RB-L9 | View More Details appears once per card |
| RB-L10 | Active tab renders a list or `No active bookings.` |
| RB-L11 | Header switches to Active Order |
| RB-L12 | Completed card shows machine, date range, amount, no actions |
| RB-L13 | Rupee grouping is consistent across tabs |
| RB-L14 | Upcoming card count matches the Home stat |
| RB-L15 | Scroll keeps tabs and header pinned |
| RB-L16 | Seeded booking lands in the correct bucket |

### RB-I · interaction and navigation (16, no state change)

| ID | Case |
|---|---|
| RB-I1 | Upcoming tab selects the upcoming list |
| RB-I2 | Active tab swaps list and header |
| RB-I3 | Completed tab swaps list and header |
| RB-I4 | Rapid tab cycling settles on one tab |
| RB-I5 | View More Details expands only its own card |
| RB-I6 | Expanded card collapses again |
| RB-I7 | Get Direction opens navigation, Back returns |
| RB-I8 | Tapping the map tile is safe |
| RB-I9 | Header Back returns to Home |
| RB-I10 | Device Back matches header Back, no launcher exit |
| RB-I11 | Help opens support and returns |
| RB-I12 | Tapping a Completed card |
| RB-I13 | Change opens the operator sheet and cancels cleanly |
| RB-I14 | Assign opens the assignment sheet and cancels cleanly |
| RB-I15 | Last card in a long list is reachable and tappable |
| RB-I16 | Entry path independence: queue first, then Bookings |

### RB-S · lifecycle (16, state-changing, seed bookings only)

| ID | Case |
|---|---|
| RB-S1 | Booking placed on customer web reaches the vendor app |
| RB-S2 | Incoming request shows countdown plus Decline and Accept |
| RB-S3 | Accept opens Assign machine |
| RB-S4 | Machine + operator enables Confirm and commits |
| RB-S5 | Accepted booking leaves the incoming queue |
| RB-S6 | Accepted booking appears under Upcoming |
| RB-S7 | Home stats move after acceptance |
| RB-S8 | Decline with reason removes the request |
| RB-S9 | Declined booking is in no bucket |
| RB-S10 | Assign on an unassigned upcoming booking flips the row |
| RB-S11 | Change operator propagates to calendar and team |
| RB-S12 | Upcoming → Active at start time |
| RB-S13 | Active → Completed at completion |
| RB-S14 | Completed booking is immutable |
| RB-S15 | Untouched request expires at 00:00 and is not booked |
| RB-S16 | Extend-time decision updates the booking end date |

### RB-E · edges (20)

| ID | Case |
|---|---|
| RB-E1 | Double-tap Accept commits once |
| RB-E2 | Double-tap Decline commits once |
| RB-E3 | Accept and Decline race resolves to one outcome |
| RB-E4 | Accept at the moment the timer hits 00:00 |
| RB-E5 | Accept while offline |
| RB-E6 | Decline while offline |
| RB-E7 | Accept a booking already resolved elsewhere |
| RB-E8 | Accept a booking the customer cancelled mid-view |
| RB-E9 | Cancelled booking disappears on refresh |
| RB-E10 | Tab switching while the list is loading |
| RB-E11 | Bookings list while offline shows error, not empty |
| RB-E12 | Airplane toggle mid-list self-heals |
| RB-E13 | Background and foreground during an Accept |
| RB-E14 | Force-stop and relaunch from the list |
| RB-E15 | Rotation on the Bookings list |
| RB-E16 | Long machine name and long address stay in the card |
| RB-E17 | Missing or placeholder field values |
| RB-E18 | All three tabs on a zero-booking account |
| RB-E19 | Expired session while on Bookings |
| RB-E20 | Rapid Confirm taps on Assign produce one assignment |

### RB-A · API and backend consistency (14)

| ID | Case |
|---|---|
| RB-A1 | List returns 200 with a usable shape |
| RB-A2 | API statuses map onto the three tabs |
| RB-A3 | List counts agree with dashboard and Home |
| RB-A4 | Detail matches the card |
| RB-A5 | Accept transitions the booking server-side |
| RB-A6 | Second accept is rejected or idempotent |
| RB-A7 | Decline persists the reason |
| RB-A8 | Assign persists machine and operator |
| RB-A9 | Accept without an assignment |
| RB-A10 | Accept on an expired booking is refused |
| RB-A11 | Endpoints reject a missing or expired token |
| RB-A12 | Another vendor's booking id is not reachable |
| RB-A13 | Invoice for a completed booking |
| RB-A14 | Accepted booking propagates to schedule and earnings |

---

## 7. Execution protocol (tomorrow)

1. Customer web credentials arrive; place **named seed bookings**, one per state-changing case, and
   record booking id, machine, dates, amount, and address for each.
2. Enable one case at a time (`enabled = true`), write its steps, run its isolated suite:
   `mvn -Dsurefire.suiteXmlFiles=src/test/resources/bookings/booking-rental-landing-l1.xml test`
3. Report the real result. No case is marked done on inspection alone.
4. Order: RB-L → RB-I → RB-S → RB-E → RB-A. RB-S1 gates everything downstream.
5. A failure gets a **new sequential bug number** in `BUGS_FOUND.docx`, then `docs/bugs.html` is
   rebuilt. Never merge into #13–#16 (Quick Booking) or #17–#26 (Home).
6. Pre-existing 0001 queue items stay untouched. Accept / Decline / Assign only on seed bookings.
   Log Out is never tapped.

## 8. Known dependencies and risks

| Risk | Effect | Handling |
|---|---|---|
| #15 extend-time dialog blocks Home | Entry path to Bookings breaks | Base entry already fails naming #15 |
| #16 Confirm disabled when operators are busy | RB-S4 cannot commit | Free one operator on the seed slot, else record as blocked by #16 |
| Short-slot seeding | RB-S12 / RB-S13 need a start and end inside the session | Book the shortest slot the customer site allows |
| 30:00 countdown | RB-S15 and RB-E4 take real time | Schedule them as long-running, run other cases meanwhile |
| Second vendor account | RB-A12 needs a foreign booking id | Use a `9000000017` or `9000000003` booking id |

## Live confirmation (23 Sep 2026)

Customer `9110981038` → vendor `9000000001` → operator `9000000002`.

| Step | Evidence |
|---|---|
| Place order | `L2B-RNT-2026-1837E2` Excavator 20T, site `RB-FULL-FLOW`, Juspay captured |
| Vendor queue | Pending card on Quick Booking after relaunch (Accept / Decline / Timer) |
| UI Accept (busy) | Opens Assign machine; Confirm blocked when `all_operators_busy` (**#16**) |
| API Accept | `POST .../accept` → `confirmed` |
| API Assign | `POST .../assign` + `allow_operator_overlap=true` → `operator_assigned` (Nauman / KA13Z2117) |
| Start OTP | Customer `9477` → operator `POST .../start` → `in_progress` |
| End OTP | Customer `5400` → operator `POST .../end` → `completed` (customer shows Job finished) |

### Free-operator UI happy path (23 Sep 2026) — scopes #16

Seed `L2B-RNT-2026-F7D066` / site `RB-FREE-OP-UI` / Oct 12–13 Excavator 20T.  
API candidates: `all_operators_busy=false`, Nauman `is_available=true`.

| Step | Evidence |
|---|---|
| Appium login | `9000000001` + OTP `1234` → Quick Booking with seed card |
| Accept | Assign machine opens; **no** busy banner; helper = “Select an operator or tap Skip to continue” |
| Select operator | Nauman Majid Pathan, row enabled |
| Confirm | `confirmEnabledAfterAssign=true` → sheet closes → toast “Order accepted successfully” |
| Backend | `GET .../bookings/{id}` → `operator_assigned` |

Suite: `src/test/resources/quickbooking/accept-free-operator.xml`  
`mvn test -Dsurefire.suiteXmlFiles=src/test/resources/quickbooking/accept-free-operator.xml`

**Verdict:** #16 is busy-slot only. Free-operator Confirm works. Do not treat Confirm as globally broken.

Executable API suite (no Appium): `src/test/resources/bookings/rental-booking-lifecycle-api.xml`  
`mvn test -Dsurefire.suiteXmlFiles=src/test/resources/bookings/rental-booking-lifecycle-api.xml`  
Optional Start/End: `-Dl2b.bookingId=… -Dl2b.startOtp=… -Dl2b.endOtp=…`

