# KYC — Happy Path + Edge Cases (pre-automation)

**Scope:** explore + case list only. Do **not** run Live L/I/E until this catalog is agreed and dump is refreshed on warm `9000000001`.

**Evidence used**
- Live Hindi dump `/tmp/l2b-lang-matrix-0001-20261001/d-kyc` (read-only **KYC Details**)
- Screens: `docs/assets/screens/kyc-details-empty.png`, `kyc-form.png`, `kyc-upload-picker.png`, `kyc-profile-verification.png`, `profile-verification.jpg`
- APIs: `DocumentsApi` — `/api/v1/documents/staged`, `/upload`, `/verification/verify/ifsc`
- Blocker: BUGS_FOUND **#52** (QA OTP / content API) can force error empty on drawer KYC

**Never-tap (all suites)**  
Accept · Decline · Log Out Confirm · Withdraw Confirm · Submit KYC final Confirm (unless disposable fixture) · Save Changes on Account

---

## Screens discovered

| Screen | Entry | Mode |
|--------|-------|------|
| **A. KYC Details** | Profile drawer → KYC | Read-only review (seeded approved vendor) |
| **B. KYC form** | Profile Verification → Update this step / registration | Editable + uploads + Continue |
| **C. Add Document Photo sheet** | Tap any Upload * on form | Take Photo / Gallery / Cancel |
| **D. Profile Verification** | Post-register / under-review Home | KYC & Payment Details step |

### A. KYC Details chrome (dump + empty screenshot)
- Header: Back · title **KYC Details** / हिंदी **केवाईसी विवरण**
- **Document details**: Document Type (Company PAN Card) · Document Number (value or —) · GST number
- **Bank details**: Bank name · IFSC · Branch address · Mobile linked to bank (masked `********74`) · UPI ID
- Dual path: full chrome **or** error empty + Retry (same pattern as FAQ/Terms when API down)

### B. KYC form chrome (`kyc-form.png`)
- Header: Back · **KYC** · green progress bar
- Identity: Aadhar number * · Upload Aadhar Card * (≤6MB jpeg/pdf) · PAN number * · Upload PAN Card * · Company GST number *
- **Bank Account Details**: IFSC * · Bank name * · Bank branch name * · Mobile no. * (Linked to bank) · Upload Cancelled Check * · UPI Id (optional)
- Footer: **Continue** (disabled until mandatory valid)

### C. Upload sheet (`kyc-upload-picker.png`)
- Title **Add Document Photo**
- **Take Photo** · **Choose from Gallery** · **Cancel**

### D. Profile Verification (`kyc-profile-verification.png`)
- Title + 24h copy
- Steps: Profile Details · Your Machine · **KYC & Payment Details** (Pending + “KYC skipped — complete from profile to receive orders.”)
- **Update this step** · bottom **Continue**

---

## A. Happy Path — KYC Details (drawer, seeded `9000000001`)

| ID | User action | Expected |
|----|-------------|----------|
| KYC-L1 | Profile → tap KYC | Opens KYC Details (or error empty dual-path) |
| KYC-L2 | Land | Title KYC Details + Back visible |
| KYC-L3 | Land | Full chrome **or** error + Retry (document #52 if error) |
| KYC-L4 | Error path: tap Retry | Stays on KYC; no crash; either recovers chrome or keeps error+Retry |
| KYC-L5 | Full path | Document details section / Company PAN Card row |
| KYC-L6 | Full path | PAN / document number when present |
| KYC-L7 | Full path / scroll | Bank details (name / IFSC / branch / mobile / UPI) |
| KYC-L8 | Any | Package stays Vendor (`l2b`) |
| KYC-L9 | Any | No Accept / Decline on KYC |
| KYC-L10 | Tap Back | Returns drawer or Home (Account / Current Earning) |
| KYC-I1 | Open KYC | Identity true |
| KYC-I2 | Retry when error | No crash; Vendor package |
| KYC-I3 | Scroll list up | Still Vendor; still KYC or bank chrome |
| KYC-I4 | Back | Drawer/Home |
| KYC-I5 | Back then re-open KYC | Opens again |
| KYC-I6 | Full path | Document chrome |
| KYC-I7 | Full path | Bank chrome |
| KYC-I8 | Double device Back | Safe (drawer/Home/launcher-safe); no crash |
| KYC-I9 | From KYC | Never open Log Out Confirm |
| KYC-I10 | Retry + scroll | Stay Vendor |

---

## A. Critical Edge — KYC Details

| ID | User action | Expected |
|----|-------------|----------|
| KYC-E1 | Device Back | Leaves KYC to drawer/Home |
| KYC-E2 | Force-stop + relaunch on KYC | No crash; Vendor or Language/Sign up |
| KYC-E3 | Background 2s → foreground | Vendor; KYC or Home/drawer |
| KYC-E4 | Rapid Retry ×3 | No crash; Vendor |
| KYC-E5 | Airplane / offline open | Chrome or error; Vendor |
| KYC-E6 | Rotate landscape → portrait | Vendor; no stuck Account drawer (compare Home #26) |
| KYC-E7 | Open KYC twice | Second open works |
| KYC-E8 | After open | Not launcher |
| KYC-E9 | Full bank chrome | Mobile masked (`****` / `********74`), not full number |
| KYC-E10 | Scroll | Still no Accept/Decline |
| KYC-E11 | Rapid Back ×3 | Vendor; not crash |
| KYC-E12 | Pull-to-refresh / Retry then Back | Clean exit |
| KYC-E13 | Hindi language (if set) | Title केवाईसी विवरण + bank labels; Log Out not required here |
| KYC-E14 | Empty dashes state (`kyc-details-empty`) | Sections visible with — values; Back works; no edit fields |
| KYC-E15 | Tap document / bank rows | Read-only — no unintended edit/upload sheet |
| KYC-E16 | Long press / multi-touch on Back | No crash; single navigate |
| KYC-E17 | Deep-link / cold start mid-KYC | Recover safely |
| KYC-E18 | API 401/404 mid-screen | Error empty + Retry (file bug if blank white) |

---

## B. Happy Path — KYC form (editable)

| ID | User action | Expected |
|----|-------------|----------|
| KYC-F-L1 | Update this step / registration KYC | Form opens: title KYC + progress + Continue disabled |
| KYC-F-L2 | Land | All * fields + Upload hints (6mb jpeg/pdf) visible |
| KYC-F-L3 | Land | UPI Id present and **optional** (no *) |
| KYC-F-L4 | Tap Back (empty form) | Leaves form; Prefer discard or keep draft — document live behavior |
| KYC-F-L5 | Enter valid Aadhar (12 digit) | Accepted / advances focus |
| KYC-F-L6 | Enter valid PAN | Accepted |
| KYC-F-L7 | Enter valid GST | Accepted |
| KYC-F-L8 | Enter valid IFSC | Bank name / branch may auto-fill (IFSC API) |
| KYC-F-L9 | Enter bank-linked mobile 10 digit | Accepted |
| KYC-F-L10 | Upload Aadhar + PAN + Cancelled Check (valid jpeg ≤6MB) | Filenames/previews shown |
| KYC-F-L11 | All mandatory valid → Continue enables | Continue tappable |
| KYC-F-L12 | Continue (happy) | Navigates next (Profile Verification / success) — **do not run destructive submit on prod seed without fixture** |
| KYC-F-I1 | Fill UPI only | Continue still disabled until * fields done |
| KYC-F-I2 | Clear one mandatory after enable | Continue disables again |
| KYC-F-I3 | Scroll form | Footer Continue still reachable / sticky |
| KYC-F-I4 | Tap each Upload | Opens Add Document Photo sheet |
| KYC-F-I5 | Sheet Cancel | Returns to form; field unchanged |
| KYC-F-I6 | Replace already-uploaded file | New file replaces old |

---

## B. Critical Edge — KYC form field validation & user input

| ID | User action | Expected |
|----|-------------|----------|
| KYC-F-E1 | Aadhar empty / Continue | Continue stays disabled |
| KYC-F-E2 | Aadhar &lt;12 / &gt;12 digits | Reject or inline error |
| KYC-F-E3 | Aadhar letters / spaces / emoji | Reject |
| KYC-F-E4 | Aadhar paste 12 digits with spaces | Normalize or reject — document |
| KYC-F-E5 | PAN empty | Continue disabled |
| KYC-F-E6 | PAN wrong length / lowercase | Reject or auto-uppercase |
| KYC-F-E7 | PAN personal vs company format | Document live rule (Company PAN expected for vendor) |
| KYC-F-E8 | GST empty / short / invalid checksum | Reject |
| KYC-F-E9 | IFSC empty | Continue disabled |
| KYC-F-E10 | IFSC invalid (not 11 / bad pattern) | Reject; bank fields not filled |
| KYC-F-E11 | IFSC valid but API fail | Error toast; bank name not silent-wrong |
| KYC-F-E12 | Bank name / branch empty when IFSC not auto | Continue disabled |
| KYC-F-E13 | Mobile empty / &lt;10 / &gt;10 | Reject |
| KYC-F-E14 | Mobile with +91 / leading 0 | Normalize or reject |
| KYC-F-E15 | UPI invalid (`@@`, spaces) | Soft warn or reject on Continue |
| KYC-F-E16 | UPI empty | Allowed (optional) |
| KYC-F-E17 | Very long paste into any field | No crash; truncate/error |
| KYC-F-E18 | Rapid type + Continue spam | No double submit; Vendor stable |
| KYC-F-E19 | Offline while typing then Continue | Clear offline/error; no silent success |
| KYC-F-E20 | Rotate mid-edit | Fields retain values; Vendor |
| KYC-F-E21 | Background/foreground mid-edit | Values retained or documented loss |
| KYC-F-E22 | Force-stop mid-edit | Relaunch safe; draft policy documented |
| KYC-F-E23 | Device Back with dirty form | Discard confirm **or** silent back — document; never crash |
| KYC-F-E24 | Continue with only uploads missing | Stays disabled / error on missing uploads |
| KYC-F-E25 | Prefilled read-only fields (if any) | Cannot corrupt masked values |

---

## C. Critical Edge — Upload picker & files

| ID | User action | Expected |
|----|-------------|----------|
| KYC-U-E1 | Open Upload Aadhar | Sheet: Take Photo · Gallery · Cancel |
| KYC-U-E2 | Cancel | Sheet closes; form unchanged |
| KYC-U-E3 | Device Back on sheet | Sheet closes (not whole form) preferred |
| KYC-U-E4 | Take Photo → deny camera permission | Soft deny UI; no crash |
| KYC-U-E5 | Take Photo → allow → capture | Preview/name on form |
| KYC-U-E6 | Gallery → deny storage/photos | Soft deny; no crash |
| KYC-U-E7 | Gallery → pick jpeg ≤6MB | Accepted |
| KYC-U-E8 | Gallery → pick pdf ≤6MB | Accepted |
| KYC-U-E9 | File &gt;6MB | Reject with size message |
| KYC-U-E10 | Unsupported type (png/docx/heic if blocked) | Reject |
| KYC-U-E11 | 0-byte / corrupt file | Reject |
| KYC-U-E12 | Rapid open sheet ×3 | One sheet; Vendor |
| KYC-U-E13 | Upload Cancelled Check same rules | Same as Aadhar/PAN uploads |
| KYC-U-E14 | Remove / replace upload | Can clear or replace; Continue updates |
| KYC-U-E15 | Offline during upload | Error; no fake success |
| KYC-U-E16 | Upload API 4xx/5xx | Error + Retry path; form stays |

---

## D. Happy Path + Edge — Profile Verification (KYC step)

| ID | User action | Expected |
|----|-------------|----------|
| KYC-PV-L1 | Open Profile Verification | Title + 24h copy + three steps |
| KYC-PV-L2 | KYC skipped pending | Shows Pending + “KYC skipped…” + **Update this step** |
| KYC-PV-L3 | Tap Update this step | Opens KYC form (B) |
| KYC-PV-L4 | Tap Continue while KYC Pending | Document: allow restricted Home vs block — live rule |
| KYC-PV-E1 | Rapid Update this step ×3 | Single navigation |
| KYC-PV-E2 | Back from KYC form to PV | Returns PV; status still Pending until submit |
| KYC-PV-E3 | After successful KYC submit | KYC step → Pending review / Approved (document) |
| KYC-PV-E4 | Continue with Machine Pending + KYC Pending | Document product rule |
| KYC-PV-E5 | Approved KYC step | No Update this step / or View only |

---

## Standing safety (all IDs)

1. Never Confirm Log Out from KYC suites.  
2. Never Accept/Decline booking chrome if overlapped.  
3. Prefer disposable vendor for real **Continue submit**; on `9000000001` stay chrome / Cancel / Back / validation-only unless product signs off.  
4. Masked bank mobile must stay masked in asserts (KYC-E9).  
5. Dual-path every open: full chrome **or** error+Retry (#52).

---

## Mapping to TestNG (after dump refresh)

| Catalog | Class | Notes |
|---------|-------|-------|
| KYC-L1–L10 | `KycLandingTest` | Exists (drawer Details) |
| KYC-I1–I10 | `KycInteractTest` | Exists |
| KYC-E1–E10 | `KycEdgeTest` | Exists; **E11–E18 to add** |
| KYC-F-* / KYC-U-* / KYC-PV-* | New form/PV classes | **Not coded yet** — write after live form dump |

## Next step (when you say go)

1. Warm login `9000000001` (needs #52 fixed) → dump drawer KYC Details again.  
2. If form reachable (skipped KYC account or Update this step) → dump form + upload sheet.  
3. Implement missing `KYC-E11+` then form/upload/PV suites.  
4. Run L → I → E; file bugs only from live fails.
