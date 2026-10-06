# Canonical Vendor screens (from APPscreenshots)

Unique screens kept for automation. Source pack is `APPscreenshots/` (manual + emulator). `_SAMPLE_LAYOUT_ONLY.jpg` is a **Customer** app poster (May 2025) — not Vendor.

Duplicates skipped: Home twice at 12:13, Fleet empty twice, bug028 OTP/login repeats, emu vs 4 Sep carousel copies.

## First launch

| File | Screen |
|---|---|
| splash.jpg | L2B Partner App splash |
| language.jpg | Welcome to L2B · EN/HI/TE/KN · Get started |
| onboarding-slide-1.jpg | Grow Your Machine… · Skip · Next · Are you Customer? |
| onboarding-slide-2.jpg | Manage Everything… · Get started |
| signup.jpg | Sign up · +91 · terms · Get OTP disabled |
| otp.png | Verify your OTP (empty boxes) |

OTP is **not** in the 4 Sep Drive pack. This copy is `emu_07_otp.png`.

## After OTP — existing vs new user

Valid OTP then: is this mobile already registered?

**Existing user**
- Check for active rental or material booking (any assigned machine / material order) → if yes, Quick Booking first (same accept/reject page; 18 Sep drawing is rental, material uses the same pattern). Close/Back → Home.
- No active booking/order → Home.
- Operator-specific existing-user landing after OTP is **not specified yet**.

**New user**
- Sign Up Completed popup → Contact us to Register (Help & Support) or Register Yourself.

Invalid / wrong OTP stays on the OTP screen with an error.

| File | When |
|---|---|
| quick-booking-rental.png | **Existing vendor + active rental booking** — Quick Booking (9000000001, live Allure Landing-1) |
| quick-booking-material.png | **Existing vendor + active material order** — Quick Booking (9000000017, live Allure Material-Landing-1) |
| home-rental-with-fleet.png | **Existing vendor, no active job** — Home |
| ../flow/sign-up-completed.jpg | **New vendor** — Sign Up Completed dialog |
| ../flow/select-your-role.jpg | Register Yourself → Select your role |
| ../flow/help-and-support.jpg | Contact us → Help & Support |
| ../flow/select-category.jpg | **Vendor / Owner** → Please select the category (Materials / Rental) |
| ../flow/tell-us-about-yourself.jpg | **Operator / Driver** → Tell us (name, email, TPI, company checkbox) |
| ../flow/tell-us-materials.jpg | **Materials Vendor** → Tell us + warehouse location |
| ../flow/tell-us-rental.jpg | **Rental Vendor** → Tell us + company + machinery |
| ../flow/material-vendor-registration.jpg | Material path: Tell us → KYC variants → Profile Verification → skip/KYC → Inventory/Home |
| ../flow/rental-vendor-registration.jpg | Rental path: Tell us → List machineries → Skip → Home, or Complete KYC → Profile Verification → Home |
| ../flow/operator-registration.jpg | Operator path: Tell us → List skills → Skip → Home, or Complete KYC → Profile Verification → Home |
| profile-verification.jpg | **Returning / KYC pending** — Profile Verification hub |
| home-rental-under-review.jpg | Home (rental tabs) · profile under review · zeros |
| home-material.jpg | Home (material tabs: Orders / Inventory) · ₹10,944 |
| home-rental-with-fleet.png | Home after machines added · Active Fleet 2/20 |

## Rental (Calendar · Home · Earning · Fleet)

| File | Screen |
|---|---|
| upcoming-booking.jpg | Upcoming Booking empty · Upcoming/Active/Completed |
| calendar.jpg | Schedule · September 2026 |
| earning-rental.jpg | Earning & Incentive · wallet · Withdraw/Transfer/Add Money |
| fleet-empty.jpg | Your Fleet · No Machines Yet |
| fleet-with-machines.png | Four machines, all Under Review |
| add-machine.jpg | Add Fleet Machines form |

## Material (Orders · Home · Earning · Inventory)

| File | Screen |
|---|---|
| upcoming-orders.jpg | Upcoming Orders list (MAT-…) |
| earning-material.jpg | Earning · Gross/Deduction/Tax/Penalties |
| inventory.jpg | Inventory · out of stock vs in stock |
| add-product.jpg | Add Product form |

## Drawer / Profile

| File | Screen |
|---|---|
| drawer.jpg | Account, KYC, machines, team, Help, Language, Refer, FAQ, Terms, Policies, Settings, Log Out |
| account.jpg | Test Operator · 9430874685 |
| notifications.jpg | Duplicate “Account Under Review” |
| invite-team.jpg | Invite code L2B-5L707E |
| manage-team.png | Team member with machines (existing vendor) |
| language-settings.jpg | Choose your language · Save (in-app, not first-launch) |
| refer-earn.jpg | Referral code 9Y648V |
| faq.jpg | FAQs |
| terms.jpg | Terms & Services |
| policies.jpg | Policies |
| settings-permissions.jpg | Notification/GPS/Camera on; others off |
| logout-confirm.jpg | Log Out? dialog |

## Help

| File | Screen |
|---|---|
| live-chat.jpg | Live chat · ticket concern dropdown |
| raise-ticket.jpg | Raise Ticket form |
| ticket-history.jpg | No tickets |

## KYC

| File | Screen |
|---|---|
| kyc-details-empty.png | KYC Details read-only empty |
| kyc-profile-verification.png | Same hub as profile-verification.jpg (emulator) |
| kyc-form.png | Aadhaar/PAN/GST/bank upload form |
| kyc-upload-picker.png | Take Photo / Choose from Gallery |
