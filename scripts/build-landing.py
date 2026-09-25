#!/usr/bin/env python3
"""Generate docs/index.html and docs/bugs.html from coverage.json + latest run results."""
from __future__ import annotations

import html
import json
from datetime import datetime, timezone, timedelta
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"
IST = timezone(timedelta(hours=5, minutes=30))

STATUS_LABEL = {
    "done": "Done",
    "in-progress": "In progress",
    "pending": "Pending",
}
STATUS_CLASS = {
    "done": "done",
    "in-progress": "progress",
    "pending": "pending",
}


def load_json(path: Path, default):
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def parse_allure_results(results_dir: Path) -> dict | None:
    files = list(results_dir.glob("*-result.json"))
    if not files:
        return None
    latest: dict[str, tuple[float, dict]] = {}
    for f in files:
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        key = str(data.get("fullName") or data.get("historyId") or f.name)
        mtime = f.stat().st_mtime
        prev = latest.get(key)
        if prev is None or mtime > prev[0]:
            latest[key] = (mtime, data)
    passed = failed = skipped = 0
    for _mtime, data in latest.values():
        status = str(data.get("status", "")).lower()
        if status == "passed":
            passed += 1
        elif status in ("failed", "broken"):
            failed += 1
        elif status in ("skipped", "unknown"):
            skipped += 1
    return {
        "passed": passed,
        "failed": failed,
        "skipped": skipped,
        "when": datetime.now(IST).isoformat(timespec="seconds"),
        "source": "allure-results-unique-latest",
        "note": (
            "Unique latest status per test (retries collapsed). "
            "Rental Booking 82/82 executed 24 Sep (S15+E4 BLOCKED). "
            "Known fail: QuickBookingAcceptTest.acceptOneRentalCard (BUGS_FOUND #16 busy-slot). "
            "Open Bookings bugs #27–#34 (Timer gate, Skip stuck, offline/stale Accept, Address not provided, "
            "decline reason). Free-operator UI Accept→Confirm PASSES (F7D066). "
            "OTP 23/24 retargeted to 9000000003."
        ),
    }


def parse_surefire(surefire_dir: Path) -> dict | None:
    path = surefire_dir / "testng-results.xml"
    if not path.exists():
        return None
    root = ET.parse(path).getroot()
    return {
        "passed": int(root.attrib.get("passed", 0)),
        "failed": int(root.attrib.get("failed", 0)),
        "skipped": int(root.attrib.get("skipped", 0)) + int(root.attrib.get("ignored", 0)),
        "when": datetime.now(IST).isoformat(timespec="seconds"),
        "source": "surefire",
    }


def latest_run(coverage_run: dict) -> dict:
    allure = parse_allure_results(ROOT / "target" / "allure-results")
    if allure and (allure["passed"] + allure["failed"] + allure["skipped"]) > 0:
        (DOCS / "last-run.json").write_text(json.dumps(allure, indent=2) + "\n", encoding="utf-8")
        return allure
    surefire = parse_surefire(ROOT / "target" / "surefire-reports")
    if surefire and (surefire["passed"] + surefire["failed"] + surefire["skipped"]) > 0:
        (DOCS / "last-run.json").write_text(json.dumps(surefire, indent=2) + "\n", encoding="utf-8")
        return surefire
    return coverage_run


def read_bugs() -> list[dict]:
    path = ROOT / "BUGS_FOUND.docx"
    if not path.exists():
        return []
    try:
        from docx import Document
    except ImportError:
        return []
    doc = Document(str(path))
    if not doc.tables:
        return []
    rows = []
    for i, row in enumerate(doc.tables[0].rows):
        cells = [c.text.strip() for c in row.cells]
        if i == 0 or not any(cells):
            continue
        rows.append(
            {
                "num": cells[0] if len(cells) > 0 else "",
                "module": cells[1] if len(cells) > 1 else "",
                "description": cells[2] if len(cells) > 2 else "",
                "severity": cells[3] if len(cells) > 3 else "",
                "found": cells[4] if len(cells) > 4 else "",
                "status": cells[5] if len(cells) > 5 else "",
            }
        )
    return rows


def fmt_when(raw: str) -> str:
    try:
        dt = datetime.fromisoformat(raw)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=IST)
        return dt.astimezone(IST).strftime("%d %b %Y, %I:%M %p IST")
    except ValueError:
        return raw


def cases_label(mod: dict) -> str:
    done = int(mod.get("casesDone") or 0)
    total = int(mod.get("casesTotal") or 0)
    if total <= 0:
        return "Cases not counted yet"
    return f"{done}/{total} cases"


def module_cards(modules: list[dict]) -> str:
    parts = []
    for mod in modules:
        status = mod.get("status", "pending")
        badge = STATUS_LABEL.get(status, status)
        note = (mod.get("note") or "").strip()
        note_html = (
            f'\n        <p class="card-note">{html.escape(note)}</p>'
            if note and status == "in-progress"
            else ""
        )
        parts.append(
            f"""      <article class="card status-{html.escape(STATUS_CLASS[status])}">
        <div class="card-top">
          <h3>{html.escape(mod["name"])}</h3>
          <span class="badge {html.escape(STATUS_CLASS[status])}">{html.escape(badge)}</span>
        </div>
        <p class="cases">{html.escape(cases_label(mod))}</p>{note_html}
      </article>"""
        )
    return "\n".join(parts)


def mermaid_node(mod: dict) -> str:
    label = mod["name"].replace('"', "'")
    return f'{mod["id"]}(["{label}"])'


def mermaid_free_node(node_id: str, label: str) -> str:
    safe = str(label).replace('"', "'")
    return f'{node_id}(["{safe}"])'


def mermaid_decision(node_id: str, label: str, shape: str = "diamond") -> str:
    safe = str(label).replace('"', "'")
    if shape == "round":
        return f'{node_id}(["{safe}"])'
    if shape in ("rect", "box"):
        return f'{node_id}["{safe}"]'
    return f'{node_id}{{"{safe}"}}'


def mermaid_edge(src: str, dst: str, label: str | None = None, indent: str = "    ") -> str:
    if label:
        safe = str(label).replace('"', "'")
        return f"{indent}{src} -->|{safe}| {dst}"
    return f"{indent}{src} --> {dst}"


def mermaid_graph(coverage: dict) -> str:
    by_id = {m["id"]: m for m in coverage["modules"]}
    launch = coverage["firstLaunch"]
    labels = coverage.get("firstLaunchEdgeLabels") or {}
    branches = coverage.get("firstLaunchBranches") or []
    after = coverage.get("afterOtp") or {}
    home_chrome = coverage.get("homeChrome") or {}
    drawer_leaf_ids: list[str] = []
    loggedin_ids = set(
        ["home"]
        + list(coverage.get("postLoginFromHome") or [])
        + list(coverage.get("postLoginFromSettings") or [])
        + list(coverage.get("loggedInExtra") or [])
    )
    if home_chrome:
        loggedin_ids.add("profileDrawer")
        for edge in home_chrome.get("fromHome") or []:
            if edge.get("to"):
                loggedin_ids.add(edge["to"])
        for edge in home_chrome.get("drawer") or []:
            tid = edge.get("to")
            if tid:
                loggedin_ids.add(tid)
                if tid not in by_id:
                    drawer_leaf_ids.append(tid)
    lines = ["flowchart TB", '  subgraph launch["First launch"]', "    direction TB"]
    declared = set()
    for i, mid in enumerate(launch):
        lines.append(f"    {mermaid_node(by_id[mid])}")
        declared.add(mid)
        if i > 0:
            prev = launch[i - 1]
            lines.append(mermaid_edge(prev, mid, labels.get(f"{prev}->{mid}")))
    for branch in branches:
        for mid in (branch.get("from"), branch.get("to")):
            if mid and mid not in declared and mid not in loggedin_ids and mid in by_id:
                lines.append(f"    {mermaid_node(by_id[mid])}")
                declared.add(mid)
    lines.append("  end")
    decision_ids = []
    if after:
        registered = after["registeredDecision"]
        existing = after["existing"]
        active = existing["activeDecision"]
        new_user = after["newUser"]
        decision_ids = [registered["id"], active["id"]]
        lines.append(f"  {mermaid_decision(registered['id'], registered['label'])}")
        lines.append(f"  otp --> {registered['id']}")
        lines.append(
            f"  {mermaid_decision(active['id'], active['label'], active.get('shape', 'round'))}"
        )
        lines.append(mermaid_edge(registered["id"], active["id"], existing.get("edge"), indent="  "))
        lines.append(mermaid_edge(active["id"], existing["yes"]["to"], existing["yes"].get("label"), indent="  "))
        lines.append(mermaid_edge(active["id"], existing["no"]["to"], existing["no"].get("label"), indent="  "))
        lines.append(mermaid_edge(registered["id"], new_user["to"], new_user.get("edge"), indent="  "))
    lines.append('  subgraph loggedin["After login"]')
    lines.append("    direction TB")
    loggedin_declared: set[str] = set()

    def declare_loggedin(node_id: str, label_override: str | None = None) -> None:
        if node_id in loggedin_declared:
            return
        if node_id in by_id:
            if label_override:
                lines.append(f"    {mermaid_free_node(node_id, label_override)}")
            else:
                lines.append(f"    {mermaid_node(by_id[node_id])}")
        elif label_override:
            lines.append(f"    {mermaid_free_node(node_id, label_override)}")
        else:
            lines.append(f"    {mermaid_free_node(node_id, node_id)}")
        loggedin_declared.add(node_id)

    for mid in coverage.get("loggedInExtra") or []:
        if mid in by_id:
            declare_loggedin(mid)
    declare_loggedin("home")

    if home_chrome:
        tabs = home_chrome.get("bookingsTabsLabel") or "Upcoming / Active / Completed"
        bookings_label = f"{by_id['bookings']['name']}<br/>{tabs}" if "bookings" in by_id else tabs
        declare_loggedin("profileDrawer", "Profile drawer")
        for edge in home_chrome.get("fromHome") or []:
            tid = edge.get("to")
            if not tid:
                continue
            if tid == "bookings":
                declare_loggedin("bookings", bookings_label)
            else:
                declare_loggedin(tid)
            lines.append(mermaid_edge("home", tid, edge.get("label")))
        for edge in home_chrome.get("drawer") or []:
            tid = edge.get("to")
            if not tid:
                continue
            leaf_label = edge.get("label") or tid
            if tid in by_id:
                declare_loggedin(tid)
            else:
                declare_loggedin(tid, leaf_label)
            lines.append(mermaid_edge("profileDrawer", tid, edge.get("label")))
        for mid in coverage.get("postLoginFromSettings") or []:
            declare_loggedin(mid)
            lines.append(mermaid_edge("settings", mid))
    else:
        for mid in coverage.get("postLoginFromHome") or []:
            declare_loggedin(mid)
            lines.append(f"    home --> {mid}")
        for mid in coverage.get("postLoginFromSettings") or []:
            declare_loggedin(mid)
            lines.append(f"    settings --> {mid}")

    lines.append("  end")
    for branch in branches:
        lines.append(mermaid_edge(branch["from"], branch["to"], branch.get("label"), indent="  "))
    link_otp_home = coverage.get("linkOtpToHome")
    if link_otp_home is None:
        link_otp_home = "signupCompleted" not in launch and not after
    if link_otp_home:
        lines.append("  otp -.->|valid OTP, not automated| home")
    for status, ids in (
        ("done", [m["id"] for m in coverage["modules"] if m["status"] == "done"]),
        ("progress", [m["id"] for m in coverage["modules"] if m["status"] == "in-progress"]),
        ("pending", [m["id"] for m in coverage["modules"] if m["status"] == "pending"]),
    ):
        if ids:
            lines.append(f"  class {','.join(ids)} {status}")
    # Mermaid-only drawer leaves + profileDrawer stay pending grey
    synthetic_pending = ["profileDrawer"] + sorted(set(drawer_leaf_ids))
    if synthetic_pending:
        lines.append(f"  class {','.join(synthetic_pending)} pending")
    if decision_ids:
        lines.append(f"  class {','.join(decision_ids)} decision")
    lines.append("  classDef done fill:#E8F5E9,stroke:#2E7D32,color:#145218,stroke-width:2px")
    lines.append("  classDef progress fill:#FFE4A8,stroke:#E39A1C,color:#5C3D00,stroke-width:3px")
    lines.append("  classDef pending fill:#F4F4F4,stroke:#8D8D8D,color:#3D3D3D,stroke-width:2px")
    lines.append("  classDef decision fill:#FFF8E1,stroke:#F9A825,color:#7A5200,stroke-width:2px")
    return "\n".join(lines)


def flow_shots(
    evidence: list,
    note: str | None = None,
    heading: str = "First login after OTP",
    extra_class: str = "",
) -> str:
    if not evidence:
        return ""
    note_html = html.escape(
        note
        or "Live-app screenshots, Sep."
    )
    shots_class = "shots" + (f" {extra_class}" if extra_class else "")
    parts = [
        f"    <h2>{html.escape(heading)}</h2>",
        f'    <p class="note">{note_html}</p>',
        f'    <div class="{shots_class}">',
    ]
    for shot in evidence:
        caption = html.escape(shot.get("caption") or "")
        src = shot.get("src")
        if src:
            parts.append(
                f"""      <figure>
        <img src="{html.escape(src)}" alt="{caption}">
        <figcaption>{caption}</figcaption>
      </figure>"""
            )
        else:
            parts.append(
                f"""      <figure class="missing">
        <div class="placeholder">Screenshot not captured yet</div>
        <figcaption>{caption}</figcaption>
      </figure>"""
            )
    parts.append("    </div>")
    return "\n".join(parts)


def whats_next(modules: list[dict], coverage: dict | None = None) -> str:
    override = (coverage or {}).get("whatsNext")
    if override:
        return str(override)
    for status in ("in-progress", "pending"):
        for mod in modules:
            if mod.get("status") == status:
                return mod["name"]
    return "All listed modules"


def blocker_banner(bugs: list[dict]) -> str:
    blockers = [
        b for b in bugs
        if (b.get("num") or "") == "16" and (b.get("status") or "").lower() == "open"
    ]
    if not blockers:
        return ""
    b = blockers[0]
    return f"""    <section class="blocker" aria-label="Functional blocker for developers">
      <p><strong>Dev blocker #{html.escape(b["num"])} ({html.escape(b["severity"])}):</strong>
      Assign machine Confirm stays disabled when operators are busy, so a vendor cannot complete
      Accept on a busy-slot rental. Free-operator Accept→Confirm works (F7D066, 23 Sep) — #16 is
      busy-slot only. The busy-sheet copy still says confirm the machine now and assign later,
      but Confirm stays grey. Functional blocker for busy slots — treat separately from the rest
      of the bug list.</p>
      <p><a href="bugs.html#bug-16">Open #16</a></p>
    </section>"""


def _status_class(by_id: dict, node_id: str) -> str:
    mod = by_id.get(node_id) or {}
    status = (mod.get("status") or "pending").replace("in-progress", "progress")
    if status not in ("done", "progress", "pending"):
        status = "pending"
    return status


def _flow_btn_status(by_id: dict, node_id: str) -> str:
    """Button color: green only when module fully done; else orange (pending)."""
    st = _status_class(by_id, node_id) if node_id in by_id else "pending"
    return "done" if st == "done" else "pending"


def _node_label(by_id: dict, node_id: str, fallback: str | None = None) -> str:
    mod = by_id.get(node_id)
    if mod and mod.get("name"):
        return str(mod["name"])
    return fallback or node_id


def flow_explorer(coverage: dict, mermaid: str) -> str:
    """Compact interactive App flow — primary UX; Mermaid stays optional."""
    by_id = {m["id"]: m for m in coverage.get("modules") or []}
    chrome = coverage.get("homeChrome") or {}
    from_home = chrome.get("fromHome") or []
    drawer = chrome.get("drawer") or []
    tabs_label = chrome.get("bookingsTabsLabel") or "Upcoming / Active / Completed"
    launch = coverage.get("firstLaunch") or []

    def chip(node_id: str, caption: str | None = None) -> str:
        label = html.escape(caption or _node_label(by_id, node_id))
        st = _status_class(by_id, node_id)
        return (
            f'<button type="button" class="flow-chip status-{st}" data-node="{html.escape(node_id)}">'
            f"{label}</button>"
        )

    launch_bits = []
    for i, mid in enumerate(launch):
        if i:
            launch_bits.append('<span class="flow-arrow" aria-hidden="true">→</span>')
        launch_bits.append(chip(mid))

    dest_meta: dict[str, dict] = {}
    for edge in from_home:
        tid = edge.get("to")
        if not tid:
            continue
        how = edge.get("label") or ""
        name = _node_label(by_id, tid, tid)
        detail = name
        if tid == "bookings":
            detail = f"{name} — tabs: {tabs_label}"
        elif tid == "profileDrawer":
            detail = "Profile drawer — Account, KYC, machines, team, Help, Language, Refer, FAQ, Terms, Policies, Settings"
        elif tid == "quickBooking":
            detail = "Quick Booking queue (pending Accept / Decline)"
        elif tid == "notifications":
            detail = "Notification — Unread / All (stack screen, Back to Home)"
        elif tid == "calendar":
            detail = "Schedule — month grid + day agenda; bottom tabs stay; Back or Home → Home"
        elif tid == "earning":
            detail = "Earning & Incentive — wallet, withdraw; bottom tabs stay"
        elif tid == "fleet":
            detail = "Your Fleet — same as drawer Your machines / Active Fleet See all"
        dest_meta[tid] = {
            "title": name if tid != "profileDrawer" else "Profile drawer",
            "how": how,
            "detail": detail,
            "status": _flow_btn_status(by_id, tid),
        }
    dest_meta["home"] = {
        "title": "Home",
        "how": "bottom tab (current)",
        "detail": "Rental Home — greeting, stats, Upcoming strip, Booking Orders feed",
        "status": _flow_btn_status(by_id, "home"),
    }

    def apply_page(dest_id: str, page_key: str) -> None:
        page = chrome.get(page_key) or {}
        if dest_id not in dest_meta or not page:
            return
        dest_meta[dest_id].update({
            "title": page.get("title") or dest_meta[dest_id]["title"],
            "detail": page.get("note") or dest_meta[dest_id]["detail"],
            "sections": page.get("sections") or [],
            "actions": page.get("actions") or [],
            "tabs": page.get("tabs") or [],
            "pageDetail": True,
        })

    apply_page("notifications", "notificationsPage")
    apply_page("calendar", "calendarPage")
    apply_page("home", "homePage")

    notif_page = chrome.get("notificationsPage") or {}
    home_page = chrome.get("homePage") or {}
    home_section_btns = []
    for item in home_page.get("sections") or []:
        if isinstance(item, dict):
            label = item.get("label") or ""
            mid = item.get("module") or ""
            st = item.get("status") or (_flow_btn_status(by_id, mid) if mid else "pending")
            if st == "progress":
                st = "pending"
        else:
            label = str(item)
            st = "done"
        if not label:
            continue
        badge = "done" if st == "done" else "pending"
        home_section_btns.append(
            f'<button type="button" class="home-section-btn status-{st}">'
            f'<span class="home-section-badge">{badge}</span>'
            f'<span class="home-section-label">{html.escape(label)}</span></button>'
        )
    home_section_btns = "".join(home_section_btns)

    drawer_pages = chrome.get("drawerPages") or {}
    status_word = {"done": "done", "progress": "in progress", "pending": "pending"}
    from_home_ids = {e.get("to") for e in from_home if e.get("to")}

    drawer_rows = []
    for edge in drawer:
        tid = edge.get("to")
        if not tid:
            continue
        label = edge.get("label") or _node_label(by_id, tid)
        st = _flow_btn_status(by_id, tid)
        page = drawer_pages.get(tid) or {}
        page_meta = {
            "title": page.get("title") or label,
            "how": "profile drawer",
            "detail": page.get("note") or label,
            "status": st,
            "sections": page.get("sections") or [],
            "actions": page.get("actions") or [],
            "tabs": page.get("tabs") or [],
            "drawerItem": True,
        }
        if tid in dest_meta and tid in from_home_ids:
            # Shared surface (e.g. Fleet): keep Home chrome how, attach drawer page facts
            dest_meta[tid]["sections"] = page_meta["sections"]
            dest_meta[tid]["actions"] = page_meta["actions"]
            dest_meta[tid]["tabs"] = page_meta["tabs"]
            dest_meta[tid]["drawerItem"] = True
            if page.get("note"):
                dest_meta[tid]["detail"] = page["note"]
            if page.get("title"):
                dest_meta[tid]["title"] = page["title"]
        else:
            dest_meta[tid] = page_meta
        badge = status_word.get(st, "pending")
        drawer_rows.append(
            f'<button type="button" class="drawer-chip status-{st}" data-dest="{html.escape(tid)}" '
            f'aria-label="{html.escape(label)}">'
            f'<span class="drawer-chip-badge">{html.escape(badge)}</span>'
            f'<span class="drawer-chip-label">{html.escape(label)}</span>'
            f"</button>"
        )

    # Keep profileDrawer detail listing current drawer labels
    if "profileDrawer" in dest_meta:
        dest_meta["profileDrawer"]["detail"] = (
            "Profile drawer — tap a glass row for page sections. Includes Log Out (do not confirm in tests)."
        )

    exit_btns = []
    for edge in from_home:
        tid = edge.get("to")
        if not tid or tid in ("calendar", "earning", "fleet", "notifications", "profileDrawer"):
            continue
        label = edge.get("label") or tid
        title = dest_meta.get(tid, {}).get("title", tid)
        st = dest_meta.get(tid, {}).get("status", "pending")
        exit_btns.append(
            f'<button type="button" class="flow-hotspot status-{st}" data-dest="{html.escape(tid)}" '
            f'aria-label="{html.escape(title)}">'
            f'<span class="flow-hotspot-title">{html.escape(title)}</span>'
            f'<span class="flow-hotspot-how">{html.escape(str(label))}</span></button>'
        )

    tab_status = {
        "calendar": _flow_btn_status(by_id, "calendar"),
        "earning": _flow_btn_status(by_id, "earning"),
        "fleet": _flow_btn_status(by_id, "fleet"),
        "notifications": _flow_btn_status(by_id, "notifications"),
        "profileDrawer": "pending",
        "home": _flow_btn_status(by_id, "home"),
    }
    booking_tab_chips = "".join(
        f'<button type="button" class="flow-chip status-done">{html.escape(t.strip())}</button>'
        for t in tabs_label.split("/")
        if t.strip()
    )
    notif_tabs = notif_page.get("tabs") or ["Unread", "All"]
    notif_st = dest_meta.get("notifications", {}).get("status", "pending")
    if notif_st == "progress":
        notif_st = "pending"
    notif_tab_chips = "".join(
        f'<button type="button" class="flow-chip status-{notif_st}">{html.escape(t)}</button>'
        for t in notif_tabs
        if t
    )
    bell_icon = (
        '<svg class="flow-bell-icon" viewBox="0 0 24 24" width="18" height="18" '
        'aria-hidden="true" focusable="false">'
        '<path fill="currentColor" d="M12 22a2.2 2.2 0 0 0 2.2-2.2h-4.4A2.2 2.2 0 0 0 12 22zm6.2-6.2V11a6.2 6.2 0 0 0-5-6.1V4.2a1.2 1.2 0 1 0-2.4 0v.7A6.2 6.2 0 0 0 5.8 11v4.8L4 17.6V18.8h16v-1.2l-1.8-1.8z"/>'
        "</svg>"
    )

    meta_json = html.escape(json.dumps(dest_meta, ensure_ascii=False))

    return f"""    <div class="flow-explorer" data-flow-meta="{meta_json}">
      <nav class="flow-tabs" role="tablist" aria-label="App flow sections">
        <button type="button" role="tab" id="tab-launch" aria-controls="panel-launch" aria-selected="false" data-panel="launch" class="status-done">First launch</button>
        <button type="button" role="tab" id="tab-otp" aria-controls="panel-otp" aria-selected="false" data-panel="otp" class="status-done">After OTP</button>
        <button type="button" role="tab" id="tab-home" aria-controls="panel-home" aria-selected="true" data-panel="home" class="status-progress is-active">On Home</button>
      </nav>
      <div class="legend flow-explorer-legend" aria-label="Coverage status">
        <span><i class="dot done"></i>Done — light green → dark green on tap</span>
        <span><i class="dot progress"></i>In progress</span>
        <span><i class="dot pending"></i>Pending — light orange → dark orange on tap</span>
      </div>

      <div class="flow-panels">
        <section class="flow-panel" id="panel-launch" role="tabpanel" aria-labelledby="tab-launch" hidden>
          <p class="flow-lead">Cold start path until OTP — tested.</p>
          <div class="flow-steps">{"".join(launch_bits)}</div>
        </section>

        <section class="flow-panel" id="panel-otp" role="tabpanel" aria-labelledby="tab-otp" hidden>
          <p class="flow-lead">Valid OTP then branches by registration + pending jobs — tested.</p>
          <div class="flow-branch-grid">
            <button type="button" class="flow-branch status-done">
              <h3>Existing user</h3>
              <ol>
                <li>Pending rental / material? <strong>Yes</strong> → Quick Booking first</li>
                <li>Pending rental / material? <strong>No</strong> → Home</li>
                <li>Quick Booking <strong>Close / Back</strong> → Home</li>
              </ol>
            </button>
            <button type="button" class="flow-branch status-done">
              <h3>New user</h3>
              <ol>
                <li>Sign Up Completed</li>
                <li><strong>Register Yourself</strong> → role → category / operator path → Home</li>
                <li><strong>Contact us</strong> → Help &amp; Support</li>
              </ol>
            </button>
          </div>
        </section>

        <section class="flow-panel is-active" id="panel-home" role="tabpanel" aria-labelledby="tab-home">
          <p class="flow-lead">Rental Home chrome — tap a control to see where it opens.</p>
          <div class="flow-home-layout">
            <div class="phone-chrome" aria-label="Rental Home chrome">
              <div class="phone-top">
                <button type="button" class="flow-hotspot icon status-{tab_status['profileDrawer']}" data-dest="profileDrawer" title="Profile photo">Profile</button>
                <span class="phone-title">Home</span>
                <button type="button" class="flow-hotspot icon status-{tab_status['notifications']}" data-dest="notifications" title="Notifications" aria-label="Notifications">{bell_icon}</button>
              </div>
              <div class="phone-body">
                {"".join(exit_btns) or '<p class="flow-empty">No Home body exits configured.</p>'}
              </div>
              <div class="phone-tabs" role="toolbar" aria-label="Bottom tabs">
                <button type="button" class="flow-hotspot tab status-{tab_status['calendar']}" data-dest="calendar">Calendar</button>
                <button type="button" class="flow-hotspot tab is-here status-{tab_status['home']}" data-dest="home">Home</button>
                <button type="button" class="flow-hotspot tab status-{tab_status['earning']}" data-dest="earning">Earning</button>
                <button type="button" class="flow-hotspot tab status-{tab_status['fleet']}" data-dest="fleet">Fleet</button>
              </div>
            </div>
            <aside class="flow-detail" id="flow-detail" aria-live="polite">
              <p class="flow-detail-kicker">Tap any control</p>
              <h3 class="flow-detail-title">Home</h3>
              <p class="flow-detail-how">You are on Rental Home</p>
              <p class="flow-detail-body">Bottom bar: Calendar · Home · Earning · Fleet. Header: profile photo (drawer) and bell (Notifications). Upcoming card opens Rental Booking ({html.escape(tabs_label)}). Booking Orders opens Quick Booking.</p>
              <div class="flow-home-sections" id="flow-home-sections" hidden>
                <p class="flow-detail-kicker">Home sections · tested</p>
                <div class="home-section-grid">
                {home_section_btns}
                </div>
              </div>
              <div class="flow-booking-tabs" id="flow-booking-tabs" hidden>
                <p class="flow-detail-kicker">Tabs on this screen</p>
                <div class="flow-steps">{booking_tab_chips}</div>
              </div>
              <div class="flow-booking-tabs" id="flow-notif-tabs" hidden>
                <p class="flow-detail-kicker">Tabs on this screen</p>
                <div class="flow-steps">{notif_tab_chips}</div>
                <p class="flow-notif-hint">Unread = unseen · All = unread + marked as read. No third tab on live build.</p>
              </div>
              <p class="flow-return" id="flow-return" hidden>Back → <strong>Home</strong></p>
              <div class="flow-drawer-list" id="flow-drawer-list" hidden>
                <p class="flow-detail-kicker">Profile drawer</p>
                <div class="drawer-chip-grid">
                {"".join(drawer_rows)}
                </div>
              </div>
              <div class="flow-page-meta" id="flow-page-meta" hidden>
                <p class="flow-detail-kicker">On this page</p>
                <p class="flow-page-label" id="flow-page-sections-label" hidden>Sections</p>
                <div class="flow-page-sections" id="flow-page-sections"></div>
                <p class="flow-page-label" id="flow-page-actions-label" hidden>Actions</p>
                <div class="flow-page-actions" id="flow-page-actions"></div>
                <p class="flow-page-label" id="flow-page-tabs-label" hidden>Tabs</p>
                <div class="flow-page-tabs" id="flow-page-tabs"></div>
              </div>
            </aside>
          </div>
        </section>
      </div>

      <details class="flow-map" id="flow-map-details">
        <summary>Full technical map (Mermaid) — optional</summary>
        <div class="flow flow-map-canvas">
          <pre class="mermaid">
{mermaid}
          </pre>
          <div class="legend">
            <span><i class="dot done"></i>Done</span>
            <span><i class="dot progress"></i>In progress</span>
            <span><i class="dot pending"></i>Pending</span>
            <span><i class="dot decision"></i>Decision</span>
          </div>
        </div>
      </details>
    </div>"""


def write_bugs_html(bugs: list[dict]) -> None:
    rows = []
    for b in bugs:
        status_l = (b.get("status") or "").lower()
        sev = (b.get("severity") or "").lower()
        num = b.get("num") or ""
        classes = []
        if status_l == "open":
            classes.append("open")
        if num == "16":
            classes.append("blocker")
        if sev == "high":
            classes.append("high")
        row_class = " ".join(classes)
        status_class = "status-open" if status_l == "open" else ""
        row_id = f' id="bug-{html.escape(num)}"' if num else ""
        rows.append(
            f'<tr{row_id} class="{row_class}">'
            f'<td data-label="#">{html.escape(num)}</td>'
            f'<td data-label="Module/Screen">{html.escape(b["module"])}</td>'
            f'<td data-label="Bug Description">{html.escape(b["description"])}</td>'
            f'<td data-label="Severity">{html.escape(b["severity"])}</td>'
            f'<td data-label="Found On">{html.escape(b["found"])}</td>'
            f'<td data-label="Status" class="{status_class}">{html.escape(b["status"])}</td>'
            "</tr>"
        )
    body = "\n".join(rows) if rows else '<tr><td colspan="6">No bugs recorded yet.</td></tr>'
    callout = ""
    if any((b.get("num") or "") == "16" for b in bugs):
        callout = """    <section class="blocker" id="blocker-16">
      <p><strong>Priority for dev — #16 (High).</strong> Vendors cannot finish Accept when
      Assign machine shows busy operators: Confirm stays disabled even though the copy says
      to confirm the machine now. Free-operator path works (scoped 23 Sep). Functional blocker
      for busy slots only. Please pick this up separately from the rest of the list.</p>
    </section>
"""
    (DOCS / "bugs.html").write_text(
        BUGS_HTML.replace("{{CALLOUT}}", callout).replace("{{ROWS}}", body),
        encoding="utf-8",
    )


def build() -> None:
    coverage = load_json(DOCS / "coverage.json", {})
    run = latest_run(load_json(DOCS / "last-run.json", {"passed": 0, "failed": 0, "skipped": 0, "when": ""}))
    modules = coverage.get("modules", [])
    total_screens = int(coverage.get("totalScreens", 72))
    automated = sum(int(m.get("screens") or 0) for m in modules if m.get("status") == "done")
    pct = (automated / total_screens * 100) if total_screens else 0
    pct_label = f"{pct:.1f}".rstrip("0").rstrip(".") if pct < 10 else str(int(round(pct)))
    bugs = read_bugs()
    write_bugs_html(bugs)

    page = INDEX_HTML
    replacements = {
        "{{AUTOMATED_SCREENS}}": str(automated),
        "{{TOTAL_SCREENS}}": str(total_screens),
        "{{COVERAGE_PCT}}": pct_label,
        "{{PASSED}}": str(int(run.get("passed") or 0)),
        "{{FAILED}}": str(int(run.get("failed") or 0)),
        "{{SKIPPED}}": str(int(run.get("skipped") or 0)),
        "{{WHATS_NEXT}}": html.escape(whats_next(modules, coverage)),
        "{{RUN_NOTE}}": html.escape(str(run.get("note") or "")),
        "{{BLOCKER}}": blocker_banner(bugs),
        "{{MODULE_CARDS}}": module_cards(modules),
        "{{FLOW_EXPLORER}}": flow_explorer(coverage, mermaid_graph(coverage)),
        "{{FLOW_NOTE}}": html.escape(
            "Tap On Home to explore live Rental chrome. First launch / After OTP tabs cover the path to Home. "
            "Open the optional Mermaid map only if you need the full technical graph."
        ),
        "{{FLOW_NOTE_FULL}}": html.escape(
            coverage.get("flowNote")
            or "Green is done. Amber is in progress. Grey is not started."
        ),
        "{{EXISTING_USER_SHOTS}}": flow_shots(
            coverage.get("existingUserEvidence") or [],
            coverage.get("existingUserEvidenceNote"),
            "After OTP — existing vs new user",
        ),
        "{{FLOW_SHOTS}}": flow_shots(
            coverage.get("flowEvidence") or [],
            coverage.get("flowEvidenceNote"),
            "New user after OTP",
        ),
        "{{ROLE_SHOTS}}": flow_shots(
            coverage.get("roleEvidence") or [],
            coverage.get("roleEvidenceNote"),
            "After Select your role",
        ),
        "{{CATEGORY_SHOTS}}": flow_shots(
            coverage.get("categoryEvidence") or [],
            coverage.get("categoryEvidenceNote"),
            "After Select category",
        ),
        "{{MATERIAL_SHOTS}}": flow_shots(
            coverage.get("materialEvidence") or [],
            coverage.get("materialEvidenceNote"),
            "Material Vendor registration",
            "filmstrip",
        ),
        "{{RENTAL_SHOTS}}": flow_shots(
            coverage.get("rentalEvidence") or [],
            coverage.get("rentalEvidenceNote"),
            "Rental Vendor registration",
            "filmstrip",
        ),
        "{{OPERATOR_SHOTS}}": flow_shots(
            coverage.get("operatorEvidence") or [],
            coverage.get("operatorEvidenceNote"),
            "Operator / Driver registration",
            "filmstrip",
        ),
        "{{BUGS_OPEN}}": str(sum(1 for b in bugs if b.get("status", "").lower() == "open")),
    }
    for key, value in replacements.items():
        page = page.replace(key, value)
    (DOCS / "index.html").write_text(page, encoding="utf-8")
    print(f"Wrote {DOCS / 'index.html'} and {DOCS / 'bugs.html'}")


INDEX_HTML = r"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Vendor App — Automation Testing</title>
  <link rel="icon" href="assets/ic_launcher.png">
  <style>
    :root {
      --brand: #FEB637;
      --brand-dark: #E39A1C;
      --cream: #FFFEF9;
      --bg: #F3E6D4;
      --card: rgba(255, 254, 249, 0.64);
      --text: #1A1A1A;
      --muted: #4F4F4F;
      --line: rgba(255, 255, 255, 0.48);
      --done: #145218;
      --done-bg: #E8F5E9;
      --progress: #7A5200;
      --progress-bg: #FFF6E5;
      --pending: #3D3D3D;
      --pending-bg: #F3F3F3;
      --fail: #B71C1C;
      --glass-blur: blur(14px) saturate(140%);
      --glass-shadow: 0 8px 28px rgba(80, 50, 10, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.72);
    }
    * { box-sizing: border-box; }
    html { color-scheme: light; }
    body {
      margin: 0;
      min-height: 100vh;
      color: var(--text);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
      line-height: 1.45;
      background:
        radial-gradient(900px 420px at 8% -8%, rgba(254, 182, 55, 0.28), transparent 55%),
        radial-gradient(820px 400px at 100% 0%, rgba(20, 82, 24, 0.10), transparent 52%),
        radial-gradient(720px 380px at 78% 100%, rgba(254, 164, 5, 0.12), transparent 50%),
        linear-gradient(180deg, #F7EFE3 0%, #F3E6D4 100%);
    }
    .wrap { max-width: 1100px; margin: 0 auto; padding: 20px 20px 64px; }
    header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 8px 0 20px;
      border-bottom: 1px solid rgba(80, 50, 10, 0.10);
    }
    .brand { display: flex; align-items: center; gap: 14px; min-width: 0; min-height: 0; }
    .brand img {
      height: 72px;
      width: auto;
      max-width: 200px;
      max-height: 72px;
      object-fit: contain;
      flex-shrink: 0;
      min-width: 0;
      min-height: 0;
    }
    .brand h1 { margin: 0; font-size: 1.35rem; font-weight: 700; letter-spacing: -0.02em; }
    .purpose { margin: 6px 0 0; color: var(--muted); font-size: 0.92rem; font-weight: 400; max-width: 36em; line-height: 1.4; }
    .role-banners {
      display: flex;
      gap: 10px;
      flex-shrink: 0;
      align-items: stretch;
    }
    .role-banner {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 214px;
      padding: 8px 12px 8px 8px;
      text-decoration: none;
      color: var(--text);
      background: var(--card);
      backdrop-filter: var(--glass-blur);
      -webkit-backdrop-filter: var(--glass-blur);
      border: 1px solid var(--line);
      border-radius: 16px;
      box-shadow: var(--glass-shadow);
      transition: transform 0.15s ease, border-color 0.15s ease;
    }
    .role-banner:hover {
      transform: translateY(-2px);
      border-color: var(--brand);
    }
    .role-banner img {
      width: 58px;
      height: 58px;
      object-fit: cover;
      border-radius: 12px;
      flex-shrink: 0;
      background: #F3E6D4;
    }
    .role-banner small {
      display: block;
      font-size: 0.68rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--muted);
    }
    .role-banner strong {
      display: block;
      margin-top: 2px;
      font-size: 0.98rem;
      font-weight: 750;
      letter-spacing: -0.02em;
    }
    .role-banner .go {
      display: block;
      margin-top: 3px;
      font-size: 0.78rem;
      font-weight: 650;
      color: var(--progress);
    }
    .next,
    .stat,
    .card,
    .flow {
      background: var(--card);
      backdrop-filter: var(--glass-blur);
      -webkit-backdrop-filter: var(--glass-blur);
      border: 1px solid var(--line);
      border-radius: 14px;
      box-shadow: var(--glass-shadow);
    }
    .next {
      margin: 18px 0 0;
      padding: 12px 16px;
      font-size: 0.98rem;
    }
    .next strong { color: var(--progress); }
    .stats {
      display: grid;
      grid-template-columns: repeat(5, 1fr);
      gap: 12px;
      margin: 22px 0 8px;
    }
    .stat { padding: 16px 16px 14px; }
    .stat .num { font-size: 1.85rem; font-weight: 750; letter-spacing: -0.03em; line-height: 1.1; color: var(--text); }
    .stat .num span { color: var(--muted); font-size: 1.1rem; font-weight: 600; }
    .stat .label { margin-top: 6px; color: var(--muted); font-size: 0.85rem; }
    .stat.fail .num { color: var(--fail); }
    .stat.ok .num { color: var(--done); }
    .stat.bugs {
      background: #FDECEA;
      border-color: rgba(183, 28, 28, 0.38);
      box-shadow: 0 0 0 3px rgba(183, 28, 28, 0.10), var(--glass-shadow);
    }
    .stat.bugs .num { color: var(--fail); }
    .stat.bugs .label { color: var(--fail); font-weight: 700; }
    h2 { font-size: 1.05rem; margin: 32px 0 12px; }
    .grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      column-gap: 28px;
      row-gap: 12px;
    }
    .card { padding: 14px 14px 12px; position: relative; }
    .grid .card:not(:last-child)::after {
      content: "→";
      position: absolute;
      left: 100%;
      top: 50%;
      width: 28px;
      transform: translateY(-50%);
      text-align: center;
      font-size: 1.15rem;
      font-weight: 700;
      line-height: 1;
      color: #8A5A00;
      pointer-events: none;
    }
    .grid .card:nth-child(4n)::after {
      content: none;
    }
    .card-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; }
    .card h3 { margin: 0; font-size: 1rem; color: var(--text); }
    .cases { margin: 8px 0 0; color: var(--muted); font-size: 0.88rem; }
    .badge {
      font-size: 0.72rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.03em;
      padding: 4px 8px;
      border-radius: 999px;
      white-space: nowrap;
    }
    .badge.done { background: var(--done-bg); color: var(--done); }
    .badge.progress {
      background: var(--brand);
      color: #3D2800;
      box-shadow: 0 0 0 1px rgba(227, 154, 28, 0.45);
    }
    .badge.pending { background: var(--pending-bg); color: var(--pending); }
    .card.status-done { border-color: rgba(46, 125, 50, 0.28); }
    .card.status-progress {
      border-color: rgba(227, 154, 28, 0.85);
      background: linear-gradient(145deg, rgba(255, 228, 168, 0.92), rgba(255, 254, 249, 0.78));
      box-shadow:
        0 0 0 3px rgba(254, 182, 55, 0.32),
        0 12px 30px rgba(212, 151, 10, 0.18),
        inset 0 1px 0 rgba(255, 255, 255, 0.8);
    }
    .card.status-progress::before {
      content: "";
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 5px;
      border-radius: 14px 0 0 14px;
      background: linear-gradient(180deg, #FEB637, #E39A1C);
    }
    .card.status-progress h3 { color: #7A5200; }
    .card.status-progress .cases { color: #7A5200; font-weight: 600; }
    .card-note {
      margin: 8px 0 0;
      font-size: 0.76rem;
      line-height: 1.35;
      color: #7A5200;
    }
    .shots {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: 14px;
      margin-top: 4px;
    }
    .shots figure {
      margin: 0;
      background: var(--card);
      backdrop-filter: var(--glass-blur);
      -webkit-backdrop-filter: var(--glass-blur);
      border: 1px solid var(--line);
      border-radius: 14px;
      box-shadow: var(--glass-shadow);
      overflow: hidden;
    }
    .shots img {
      width: 100%;
      max-height: 420px;
      object-fit: contain;
      object-position: top;
      background: #111;
      display: block;
    }
    .shots figcaption {
      padding: 10px 12px 12px;
      font-size: 0.88rem;
      color: var(--muted);
    }
    .shots .placeholder {
      min-height: 280px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--muted);
      background: rgba(0, 0, 0, 0.04);
      font-size: 0.9rem;
      text-align: center;
      padding: 16px;
    }
    .shots.filmstrip {
      grid-template-columns: 1fr;
    }
    .shots.filmstrip img {
      max-height: 240px;
      object-fit: contain;
      background: #1a1a1a;
    }
    .note { color: var(--muted); margin: 0 0 10px; font-size: 0.92rem; }
    .note.bugs-open {
      margin: 14px 0 10px;
      padding: 12px 16px;
      color: var(--fail);
      background: #FDECEA;
      border: 1.5px solid rgba(183, 28, 28, 0.38);
      border-radius: 14px;
      font-size: 1.02rem;
      font-weight: 750;
      box-shadow: 0 0 0 3px rgba(183, 28, 28, 0.08);
    }
    .note.bugs-open a { color: var(--fail); font-weight: 800; }
    .blocker {
      margin: 0 0 16px;
      padding: 14px 16px;
      background: rgba(183, 28, 28, 0.10);
      border: 1px solid rgba(183, 28, 28, 0.28);
      border-radius: 12px;
    }
    .blocker p { margin: 0 0 8px; }
    .blocker p:last-child { margin: 0; }
    .blocker a { color: #8A1C1C; font-weight: 800; }
    .run-note { color: var(--muted); margin: 0 0 16px; font-size: 0.88rem; }
    .flow { padding: 12px; overflow: auto; max-height: none; }
    .flow svg { background: transparent !important; max-width: none !important; width: auto !important; height: auto !important; }
    .flow .node { cursor: default; }
    .flow .node-visual {
      transition: transform 0.18s ease, filter 0.18s ease;
      transform-box: fill-box;
      transform-origin: center;
      filter: drop-shadow(0 2px 3px rgba(26, 26, 26, 0.12));
    }
    .flow .node:hover .node-visual {
      transform: translateY(-3px) scale(1.03);
      filter: drop-shadow(0 8px 12px rgba(26, 26, 26, 0.18));
    }
    .flow .node .label-container > path[stroke]:not([stroke="none"]) {
      stroke-width: 2.25px !important;
    }
    .flow .node:hover .label-container > path[stroke]:not([stroke="none"]) {
      stroke-width: 2.85px !important;
    }
    .flow .node .nodeLabel,
    .flow .node span,
    .flow .node p {
      font-weight: 650;
    }
    .flow .node.done .nodeLabel { color: #145218; }
    .flow .node.progress .nodeLabel { color: #7A5200; }
    .flow .node.pending .nodeLabel { color: #3D3D3D; }
    .flow .node.decision .nodeLabel { color: #7A5200; }
    .flow .node#activeBooking .nodeLabel p,
    .flow .node.decision .nodeLabel p,
    .flow .node.decision .nodeLabel span {
      white-space: normal;
      text-align: center;
      line-height: 1.35;
      font-size: 13px;
      max-width: 260px;
    }
    .legend { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 10px; color: var(--muted); font-size: 0.85rem; }
    .flow-note-more { margin: 0 0 14px; }
    .flow-note-more summary {
      cursor: pointer; font-weight: 650; color: var(--muted); font-size: 0.88rem;
    }
    .flow-explorer {
      margin: 0 0 28px;
      padding: 16px;
      border-radius: 18px;
      background: var(--card);
      border: 1px solid var(--line);
      box-shadow: var(--glass-shadow);
      backdrop-filter: var(--glass-blur);
      -webkit-backdrop-filter: var(--glass-blur);
    }
    .flow-tabs {
      display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px;
    }
    .flow-tabs button {
      appearance: none; border-radius: 14px; padding: 8px 14px;
      font-weight: 650; font-size: 0.88rem; cursor: pointer;
      border: 2px solid transparent; transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
    }
    /* Done tabs (First launch / After OTP / On Home): light green → dark green */
    .flow-tabs button.status-done {
      background: #E8F5E9; border-color: #81C784; color: #2E7D32;
    }
    .flow-tabs button.status-done.is-active,
    .flow-tabs button.status-done:focus-visible {
      background: #1B5E20; border-color: #0D3B12; color: #E8F5E9;
    }
    /* Pending tabs: light orange → dark orange */
    .flow-tabs button.status-pending {
      background: #FFF3E0; border-color: #FFCC80; color: #E65100;
    }
    .flow-tabs button.status-pending.is-active,
    .flow-tabs button.status-pending:focus-visible {
      background: #EF6C00; border-color: #E65100; color: #fff;
    }
    .flow-tabs button.status-progress {
      background: #FFF8E1; border-color: #FFD54F; color: #7A5200;
    }
    .flow-tabs button.status-progress.is-active,
    .flow-tabs button.status-progress:focus-visible {
      background: #F9A825; border-color: #F57F17; color: #1A1A1A;
    }
    .flow-lead { margin: 0 0 12px; color: var(--muted); font-size: 0.9rem; }
    .flow-steps {
      display: flex; flex-wrap: wrap; align-items: center; gap: 8px;
    }
    .flow-chip {
      appearance: none; cursor: pointer;
      display: inline-flex; align-items: center; padding: 7px 12px;
      border-radius: 14px; font-size: 0.84rem; font-weight: 650;
      border: 2px solid transparent;
      transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
    }
    .flow-chip.status-done {
      background: #E8F5E9; color: #2E7D32; border-color: #81C784;
    }
    .flow-chip.status-done.is-active,
    .flow-chip.status-done:focus-visible {
      background: #1B5E20; color: #E8F5E9; border-color: #0D3B12;
    }
    .flow-chip.status-progress {
      background: #FFF8E1; color: #7A5200; border-color: #FFD54F;
    }
    .flow-chip.status-progress.is-active,
    .flow-chip.status-progress:focus-visible {
      background: #F9A825; color: #1A1A1A; border-color: #F57F17;
    }
    .flow-chip.status-pending {
      background: #FFF3E0; color: #E65100; border-color: #FFCC80;
    }
    .flow-chip.status-pending.is-active,
    .flow-chip.status-pending:focus-visible {
      background: #EF6C00; color: #fff; border-color: #E65100;
    }
    .flow-arrow { color: var(--muted); font-weight: 700; }
    .flow-branch-grid {
      display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px;
    }
    .flow-branch {
      appearance: none; cursor: pointer; text-align: left; width: 100%;
      margin: 0; padding: 14px 16px; border-radius: 14px;
      border: 2px solid transparent;
      transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
      font: inherit; color: inherit;
    }
    .flow-branch.status-done {
      background: #E8F5E9; border-color: #81C784; color: #1A1A1A;
    }
    .flow-branch.status-done.is-active,
    .flow-branch.status-done:focus-visible {
      background: #1B5E20; border-color: #0D3B12; color: #E8F5E9;
    }
    .flow-branch.status-done.is-active h3,
    .flow-branch.status-done:focus-visible h3,
    .flow-branch.status-done.is-active strong,
    .flow-branch.status-done:focus-visible strong {
      color: #E8F5E9;
    }
    .flow-branch h3 { margin: 0 0 8px; font-size: 1rem; color: #2E7D32; }
    .flow-branch ol { margin: 0; padding-left: 18px; }
    .flow-branch li { margin: 6px 0; font-size: 0.9rem; }
    .flow-home-layout {
      display: grid; grid-template-columns: minmax(240px, 320px) minmax(0, 1fr); gap: 16px; align-items: start;
    }
    .phone-chrome {
      border-radius: 22px; border: 1px solid rgba(80,50,10,0.16);
      background: linear-gradient(180deg, #FFFDF8, #F7EFE3);
      overflow: hidden; box-shadow: 0 10px 28px rgba(80,50,10,0.08);
    }
    .phone-top, .phone-tabs {
      display: flex; align-items: center; justify-content: space-between; gap: 8px;
      padding: 10px 12px; background: rgba(255,255,255,0.72);
    }
    .phone-title { font-weight: 750; font-size: 0.95rem; }
    .phone-body { padding: 12px; display: grid; gap: 10px; min-height: 160px; }
    .phone-tabs { border-top: 1px solid rgba(80,50,10,0.10); }
    .flow-hotspot {
      appearance: none; width: 100%; text-align: left; cursor: pointer;
      border: 2px solid transparent; border-radius: 14px;
      padding: 10px 12px; color: var(--text);
      transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
    }
    .flow-hotspot.icon, .flow-hotspot.tab {
      width: auto; flex: 1; text-align: center; font-size: 0.78rem; font-weight: 700; padding: 8px 6px;
    }
    .flow-hotspot.icon {
      display: inline-flex; align-items: center; justify-content: center; flex: 0 0 auto;
      min-width: 40px; padding: 8px 10px;
    }
    .flow-bell-icon { display: block; }
    .flow-notif-hint {
      margin: 8px 0 0; font-size: 0.8rem; color: var(--muted); line-height: 1.35;
    }
    /* Pending: light orange → dark orange */
    .flow-hotspot.status-pending {
      background: #FFF3E0; border-color: #FFCC80; color: #E65100;
    }
    .flow-hotspot.status-pending:hover {
      background: #FFE0B2; border-color: #FFB74D;
    }
    .flow-hotspot.status-pending.is-active,
    .flow-hotspot.status-pending:focus-visible {
      background: #EF6C00; border-color: #E65100; color: #fff;
    }
    .flow-hotspot.status-pending.is-active .flow-hotspot-how,
    .flow-hotspot.status-pending:focus-visible .flow-hotspot-how {
      color: rgba(255,255,255,0.85);
    }
    /* Progress: light amber → deep amber */
    .flow-hotspot.status-progress {
      background: #FFF8E1; border-color: #FFD54F; color: #7A5200;
    }
    .flow-hotspot.status-progress.is-active,
    .flow-hotspot.status-progress:focus-visible {
      background: #F9A825; border-color: #F57F17; color: #1A1A1A;
    }
    /* Done: light green → dark green */
    .flow-hotspot.status-done {
      background: #E8F5E9; border-color: #81C784; color: #2E7D32;
    }
    .flow-hotspot.status-done:hover {
      background: #C8E6C9; border-color: #66BB6A;
    }
    .flow-hotspot.status-done.is-active,
    .flow-hotspot.status-done.is-here.is-active,
    .flow-hotspot.status-done:focus-visible {
      background: #1B5E20; border-color: #0D3B12; color: #E8F5E9;
    }
    .flow-hotspot.status-done.is-active .flow-hotspot-how,
    .flow-hotspot.status-done:focus-visible .flow-hotspot-how {
      color: rgba(232,245,233,0.85);
    }
    .flow-hotspot.tab.status-done.is-here:not(.is-active) {
      background: #E8F5E9; border-color: #81C784; color: #2E7D32;
    }
    .flow-home-sections { margin-top: 12px; }
    .home-section-grid {
      display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px;
    }
    .home-section-btn {
      appearance: none; cursor: pointer; text-align: left;
      display: flex; flex-direction: column; gap: 6px;
      padding: 10px 12px; border-radius: 14px;
      border: 2px solid transparent;
      transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
    }
    .home-section-btn.status-done {
      border-color: #81C784; background: #E8F5E9; color: #2E7D32;
    }
    .home-section-btn.status-done.is-active,
    .home-section-btn.status-done:focus-visible {
      background: #1B5E20; border-color: #0D3B12; color: #E8F5E9;
    }
    .home-section-btn.status-pending {
      border-color: #FFCC80; background: #FFF3E0; color: #E65100;
    }
    .home-section-btn.status-pending.is-active,
    .home-section-btn.status-pending:focus-visible {
      background: #EF6C00; border-color: #E65100; color: #fff;
    }
    .home-section-badge {
      align-self: flex-start;
      font-size: 0.68rem; font-weight: 750; letter-spacing: 0.03em;
      text-transform: uppercase; padding: 2px 8px; border-radius: 999px;
    }
    .home-section-btn.status-done .home-section-badge {
      background: #81C784; color: #0D3B12;
    }
    .home-section-btn.status-done.is-active .home-section-badge,
    .home-section-btn.status-done:focus-visible .home-section-badge {
      background: #E8F5E9; color: #1B5E20;
    }
    .home-section-btn.status-pending .home-section-badge {
      background: #FFE0B2; color: #E65100;
    }
    .home-section-btn.status-pending.is-active .home-section-badge,
    .home-section-btn.status-pending:focus-visible .home-section-badge {
      background: rgba(255,255,255,0.25); color: #fff;
    }
    .home-section-label { font-size: 0.86rem; font-weight: 700; line-height: 1.25; }
    @media (max-width: 800px) {
      .home-section-grid { grid-template-columns: 1fr; }
    }
    .flow-explorer-legend { margin: -6px 0 12px; }
    .flow-return {
      margin: 12px 0 0; padding: 8px 10px; border-radius: 10px;
      background: #FFF6E5; border: 1px solid rgba(254,182,55,0.45);
      font-size: 0.88rem; font-weight: 650; color: #7A5200;
    }
    .flow-booking-tabs { margin-top: 12px; }
    .flow-hotspot-title { display: block; font-weight: 750; font-size: 0.92rem; }
    .flow-hotspot-how { display: block; margin-top: 3px; color: var(--muted); font-size: 0.78rem; }
    .flow-detail {
      padding: 14px 16px; border-radius: 14px;
      background: rgba(255,255,255,0.55); border: 1px solid rgba(80,50,10,0.10);
      min-height: 180px;
    }
    .flow-detail-kicker {
      margin: 0 0 4px; font-size: 0.72rem; font-weight: 700; letter-spacing: 0.04em;
      text-transform: uppercase; color: var(--muted);
    }
    .flow-detail-title { margin: 0 0 6px; font-size: 1.15rem; }
    .flow-detail-how { margin: 0 0 8px; color: #7A5200; font-weight: 650; font-size: 0.9rem; }
    .flow-detail-body { margin: 0; color: var(--text); font-size: 0.92rem; }
    .flow-drawer-list { margin-top: 12px; }
    .drawer-chip-grid {
      display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px;
    }
    .drawer-chip {
      appearance: none; cursor: pointer; text-align: left;
      display: flex; flex-direction: column; gap: 6px;
      padding: 10px 12px; border-radius: 14px;
      border: 2px solid transparent;
      backdrop-filter: blur(12px) saturate(140%);
      -webkit-backdrop-filter: blur(12px) saturate(140%);
      box-shadow: 0 6px 18px rgba(80, 50, 10, 0.06), inset 0 1px 0 rgba(255,255,255,0.7);
      transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
    }
    .drawer-chip.status-pending {
      background: rgba(255, 243, 224, 0.85); border-color: #FFCC80; color: #E65100;
    }
    .drawer-chip.status-pending.is-active,
    .drawer-chip.status-pending:focus-visible {
      background: #EF6C00; border-color: #E65100; color: #fff;
    }
    .drawer-chip.status-progress {
      background: rgba(255, 248, 225, 0.9); border-color: #FFD54F; color: #7A5200;
    }
    .drawer-chip.status-progress.is-active,
    .drawer-chip.status-progress:focus-visible {
      background: #F9A825; border-color: #F57F17; color: #1A1A1A;
    }
    .drawer-chip.status-done {
      background: rgba(232, 245, 233, 0.9); border-color: #81C784; color: #2E7D32;
    }
    .drawer-chip.status-done.is-active,
    .drawer-chip.status-done:focus-visible {
      background: #1B5E20; border-color: #0D3B12; color: #E8F5E9;
    }
    .drawer-chip-badge {
      align-self: flex-start;
      font-size: 0.68rem; font-weight: 750; letter-spacing: 0.03em;
      text-transform: uppercase; padding: 2px 8px; border-radius: 999px;
    }
    .drawer-chip.status-pending .drawer-chip-badge { background: #FFE0B2; color: #E65100; }
    .drawer-chip.status-progress .drawer-chip-badge { background: #FFECB3; color: #7A5200; }
    .drawer-chip.status-done .drawer-chip-badge { background: #C8E6C9; color: #1B5E20; }
    .drawer-chip.status-pending.is-active .drawer-chip-badge,
    .drawer-chip.status-pending:focus-visible .drawer-chip-badge {
      background: rgba(255,255,255,0.25); color: #fff;
    }
    .drawer-chip.status-done.is-active .drawer-chip-badge,
    .drawer-chip.status-done:focus-visible .drawer-chip-badge {
      background: #E8F5E9; color: #1B5E20;
    }
    .drawer-chip-label { font-size: 0.88rem; font-weight: 700; }
    .flow-page-meta { margin-top: 12px; }
    .flow-page-label {
      margin: 10px 0 0; font-size: 0.72rem; font-weight: 750;
      text-transform: uppercase; letter-spacing: 0.04em; color: var(--muted);
    }
    .flow-page-sections, .flow-page-actions, .flow-page-tabs {
      display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px;
    }
    .flow-page-pill {
      display: inline-flex; padding: 5px 10px; border-radius: 999px;
      font-size: 0.78rem; font-weight: 650;
      background: rgba(255,255,255,0.7); border: 1px solid rgba(80,50,10,0.12);
    }
    .flow-page-pill.kind-action { background: #FFF3E0; border-color: #FFCC80; color: #E65100; }
    .flow-page-pill.kind-section { background: #E8F5E9; border-color: #81C784; color: #2E7D32; }
    .flow-page-pill.kind-tab { background: #E8F5E9; border-color: #81C784; color: #2E7D32; }
    @media (max-width: 800px) {
      .drawer-chip-grid { grid-template-columns: 1fr; }
    }
    .flow-how {
      font-size: 0.7rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.03em;
      color: var(--muted);
    }
    .flow-map { margin-top: 14px; }
    .flow-map summary {
      cursor: pointer; font-weight: 650; color: var(--muted); font-size: 0.9rem;
    }
    .flow-map-canvas {
      margin-top: 12px; max-height: 70vh; overflow: auto;
      border: 1px solid rgba(80,50,10,0.10); border-radius: 12px; padding: 10px;
      background: rgba(255,255,255,0.35);
    }
    .flow-map-canvas .flow { overflow: auto; }
    @media (max-width: 800px) {
      .flow-home-layout, .flow-branch-grid { grid-template-columns: 1fr; }
    }
    .dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 6px; vertical-align: middle; }
    .dot.done { background: var(--done); }
    .dot.progress { background: var(--brand); }
    .dot.pending { background: #8D8D8D; }
    .dot.decision { background: #F9A825; border-radius: 2px; transform: rotate(45deg); }
    footer {
      margin-top: 36px;
      padding-top: 20px;
      border-top: 1px solid rgba(80, 50, 10, 0.10);
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
    }
    .btn {
      display: inline-block;
      text-decoration: none;
      font-weight: 700;
      padding: 12px 18px;
      border-radius: 12px;
      border: 1px solid transparent;
    }
    .btn.primary { background: var(--brand); color: #1A1A1A; }
    .btn.primary:hover { background: var(--brand-dark); }
    .btn.ghost {
      background: var(--card);
      backdrop-filter: var(--glass-blur);
      -webkit-backdrop-filter: var(--glass-blur);
      color: var(--text);
      border-color: var(--line);
    }
    .btn.ghost:hover { border-color: var(--brand); }
    .btn.bugs {
      color: var(--fail);
      border-color: rgba(183, 28, 28, 0.4);
      background: #FDECEA;
    }
    .btn.bugs:hover { border-color: var(--fail); }
    @media (prefers-reduced-motion: reduce) {
      .flow .node-visual, .flow .node:hover .node-visual { transition: none; transform: none; }
    }
    @media (max-width: 800px) {
      .stats { grid-template-columns: repeat(2, 1fr); }
      header { flex-direction: column; align-items: flex-start; }
      .brand img { height: 56px; max-height: 56px; }
      .role-banners { width: 100%; }
      .role-banner { flex: 1; width: auto; }
      .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .grid .card:nth-child(4n)::after { content: "→"; }
      .grid .card:nth-child(2n)::after { content: none; }
      .shots { grid-template-columns: 1fr; }
    }
    @media (max-width: 480px) {
      .wrap { padding: 16px 14px 48px; }
      .stats { grid-template-columns: 1fr; }
      .brand h1 { font-size: 1.15rem; }
      .purpose { font-size: 0.88rem; }
      .grid {
        grid-template-columns: 1fr;
        row-gap: 28px;
      }
      .grid .card:not(:last-child)::after {
        content: "↓";
        left: 50%;
        top: 100%;
        width: auto;
        transform: translate(-50%, 4px);
      }
      .grid .card:nth-child(2n)::after { content: "↓"; }
      .grid .card:last-child::after { content: none; }
      .btn { width: 100%; text-align: center; }
      .role-banners { flex-direction: column; }
      .role-banner { width: 100%; }
    }
  </style>
</head>
<body>
  <div class="wrap">
    <header>
      <div class="brand">
        <img src="assets/ic_splash_logo.png" alt="L2B Partner App">
        <div>
          <h1>Vendor App — Automation Testing</h1>
          <p class="purpose">L2B Vendor is the partner app where vendors receive rental and material orders from customers, fulfil those jobs, and manage machines, bookings, fleet, team, and earnings in one place.</p>
        </div>
      </div>
      <nav class="role-banners" aria-label="Vendor testing reports">
        <a class="role-banner" href="rental-flow.html">
          <img src="assets/banner-excavator.png" alt="Excavator">
          <span>
            <small>Testing report</small>
            <strong>Rental Vendor</strong>
            <span class="go">Open excavator flow →</span>
          </span>
        </a>
        <a class="role-banner" href="material-flow.html">
          <img src="assets/banner-cement.png" alt="Cement bags">
          <span>
            <small>Testing report</small>
            <strong>Material Vendor</strong>
            <span class="go">Open cement flow →</span>
          </span>
        </a>
      </nav>
    </header>

    <p class="next">Next up: <strong>{{WHATS_NEXT}}</strong> — module-by-module edge cases, in first-launch order.</p>

    <section class="stats" aria-label="Overall progress">
      <div class="stat">
        <div class="num">{{AUTOMATED_SCREENS}} <span>/ {{TOTAL_SCREENS}}</span></div>
        <div class="label">Screens automated</div>
      </div>
      <div class="stat">
        <div class="num">{{COVERAGE_PCT}}%</div>
        <div class="label">Coverage</div>
      </div>
      <div class="stat ok">
        <div class="num">{{PASSED}}</div>
        <div class="label">Passed in latest run</div>
      </div>
      <div class="stat fail">
        <div class="num">{{FAILED}}</div>
        <div class="label">Failed in latest run ({{SKIPPED}} skipped)</div>
      </div>
      <div class="stat bugs">
        <div class="num">{{BUGS_OPEN}}</div>
        <div class="label">Open bugs found so far</div>
      </div>
    </section>
    <p class="note bugs-open">Open bugs: {{BUGS_OPEN}} — logged for developers. <a href="bugs.html">View the list</a> or download the Word file.</p>
{{BLOCKER}}
    <p class="run-note">{{RUN_NOTE}}</p>

    <h2>Modules</h2>
    <div class="grid">
{{MODULE_CARDS}}
    </div>

    <h2>App flow</h2>
    <p class="note">{{FLOW_NOTE}}</p>
    <details class="flow-note-more">
      <summary>Full coverage note</summary>
      <p class="note">{{FLOW_NOTE_FULL}}</p>
    </details>
{{FLOW_EXPLORER}}

{{EXISTING_USER_SHOTS}}

{{FLOW_SHOTS}}

{{ROLE_SHOTS}}

{{CATEGORY_SHOTS}}

{{MATERIAL_SHOTS}}

{{RENTAL_SHOTS}}

{{OPERATOR_SHOTS}}

    <footer>
      <a class="btn primary" href="https://charchilfromlink2build.github.io/L2B-Vendor-Appium-Automation/allure/">Open full Allure report</a>
      <a class="btn bugs" href="bugs.html">Bugs found ({{BUGS_OPEN}} open)</a>
      <a class="btn ghost" href="BUGS_FOUND.docx">Download BUGS_FOUND.docx</a>
    </footer>
  </div>
  <script type="module">
    import mermaid from "https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs";

    const explorer = document.querySelector(".flow-explorer");
    if (explorer) {
      const meta = JSON.parse(explorer.getAttribute("data-flow-meta") || "{}");
      const detailTitle = document.querySelector(".flow-detail-title");
      const detailHow = document.querySelector(".flow-detail-how");
      const detailBody = document.querySelector(".flow-detail-body");
      const detailKicker = document.querySelector(".flow-detail-kicker");
      const drawerList = document.getElementById("flow-drawer-list");
      const bookingTabs = document.getElementById("flow-booking-tabs");
      const notifTabs = document.getElementById("flow-notif-tabs");
      const homeSections = document.getElementById("flow-home-sections");
      const flowReturn = document.getElementById("flow-return");
      const pageMeta = document.getElementById("flow-page-meta");
      const pageSections = document.getElementById("flow-page-sections");
      const pageActions = document.getElementById("flow-page-actions");
      const pageTabs = document.getElementById("flow-page-tabs");
      const pageSectionsLabel = document.getElementById("flow-page-sections-label");
      const pageActionsLabel = document.getElementById("flow-page-actions-label");
      const pageTabsLabel = document.getElementById("flow-page-tabs-label");
      let fromDrawer = false;

      const fillPills = (el, labelEl, items, kind) => {
        if (!el) return;
        el.innerHTML = "";
        const list = items || [];
        if (labelEl) labelEl.hidden = list.length === 0;
        list.forEach((text) => {
          const span = document.createElement("span");
          span.className = `flow-page-pill kind-${kind}`;
          span.textContent = text;
          el.appendChild(span);
        });
      };

      const activateInGroup = (group, target) => {
        group.forEach((el) => el.classList.toggle("is-active", el === target));
      };

      explorer.querySelectorAll(".flow-tabs [data-panel]").forEach((tab) => {
        tab.addEventListener("click", () => {
          const id = tab.getAttribute("data-panel");
          explorer.querySelectorAll(".flow-tabs [data-panel]").forEach((t) => {
            const on = t === tab;
            t.classList.toggle("is-active", on);
            t.setAttribute("aria-selected", on ? "true" : "false");
          });
          explorer.querySelectorAll(".flow-panel").forEach((panel) => {
            const on = panel.id === `panel-${id}`;
            panel.classList.toggle("is-active", on);
            panel.hidden = !on;
          });
        });
      });

      explorer.querySelectorAll(".flow-chip").forEach((chip) => {
        chip.addEventListener("click", () => {
          const group = chip.closest(".flow-steps")?.querySelectorAll(".flow-chip") || [chip];
          activateInGroup([...group], chip);
        });
      });

      explorer.querySelectorAll(".flow-branch").forEach((branch) => {
        branch.addEventListener("click", () => {
          activateInGroup([...explorer.querySelectorAll(".flow-branch")], branch);
        });
      });

      explorer.querySelectorAll(".home-section-btn").forEach((btn) => {
        btn.addEventListener("click", () => {
          activateInGroup([...explorer.querySelectorAll(".home-section-btn")], btn);
        });
      });

      const showDest = (dest) => {
        const info = meta[dest] || { title: dest, how: "", detail: "", status: "pending" };
        if (detailKicker) {
          detailKicker.textContent = info.status === "done" ? "done"
            : info.status === "progress" ? "in progress" : "pending";
        }
        if (detailTitle) detailTitle.textContent = info.title || dest;
        if (detailHow) detailHow.textContent = info.how ? `Via: ${info.how}` : "";
        if (detailBody) detailBody.textContent = info.detail || "";
        if (drawerList) drawerList.hidden = !(dest === "profileDrawer" || fromDrawer);
        if (bookingTabs) bookingTabs.hidden = dest !== "bookings";
        if (notifTabs) notifTabs.hidden = dest !== "notifications";
        if (homeSections) homeSections.hidden = dest !== "home";
        if (flowReturn) {
          const showReturn = dest === "quickBooking" || dest === "notifications" || dest === "calendar";
          flowReturn.hidden = !showReturn;
          if (dest === "quickBooking") {
            flowReturn.innerHTML = 'Close / Back → <strong>Home</strong>';
          } else if (dest === "calendar") {
            flowReturn.innerHTML = 'Back or Home tab → <strong>Home</strong>';
          } else {
            flowReturn.innerHTML = 'Back → <strong>Home</strong>';
          }
        }
        const showPage = !!((info.drawerItem || info.pageDetail)
          && dest !== "home"
          && (info.sections?.length || info.actions?.length || info.tabs?.length));
        if (pageMeta) pageMeta.hidden = !showPage;
        if (showPage) {
          fillPills(pageSections, pageSectionsLabel, info.sections, "section");
          fillPills(pageActions, pageActionsLabel, info.actions, "action");
          // Tabs already shown as chips for bookings/notifications — skip duplicate pills there
          if (dest === "notifications" || dest === "bookings") {
            fillPills(pageTabs, pageTabsLabel, [], "tab");
          } else {
            fillPills(pageTabs, pageTabsLabel, info.tabs, "tab");
          }
        }
        explorer.querySelectorAll(".flow-hotspot[data-dest], .drawer-chip[data-dest]").forEach((btn) => {
          btn.classList.toggle("is-active", btn.getAttribute("data-dest") === dest && !btn.disabled);
        });
      };

      explorer.querySelectorAll(".flow-hotspot[data-dest]").forEach((btn) => {
        btn.addEventListener("click", () => {
          if (btn.disabled) return;
          fromDrawer = false;
          showDest(btn.getAttribute("data-dest"));
        });
      });
      explorer.querySelectorAll(".drawer-chip[data-dest]").forEach((btn) => {
        btn.addEventListener("click", () => {
          fromDrawer = true;
          showDest(btn.getAttribute("data-dest"));
        });
      });
      showDest("home");
    }

    mermaid.initialize({
      startOnLoad: false,
      theme: "base",
      themeVariables: {
        background: "transparent",
        primaryTextColor: "#1A1A1A",
        lineColor: "#B8B0A4",
        clusterBkg: "rgba(255,255,255,0.28)",
        clusterBorder: "rgba(255,255,255,0.45)",
        titleColor: "#5C5C5C",
        edgeLabelBackground: "rgba(255,254,249,0.85)"
      },
      flowchart: { htmlLabels: true, curve: "basis", padding: 12, wrappingWidth: 200, useMaxWidth: true }
    });

    let mermaidReady = false;
    const paintMermaid = async () => {
      if (mermaidReady) return;
      const nodes = document.querySelectorAll("#flow-map-details .mermaid");
      if (!nodes.length) return;
      await mermaid.run({ nodes });
      mermaidReady = true;
      const ns = "http://www.w3.org/2000/svg";
      document.querySelectorAll(".flow-map-canvas svg").forEach((svg) => {
        svg.style.maxWidth = "100%";
        svg.style.height = "auto";
        svg.removeAttribute("width");
        svg.removeAttribute("height");
        let defs = svg.querySelector("defs");
        if (!defs) {
          defs = document.createElementNS(ns, "defs");
          svg.prepend(defs);
        }
        if (!svg.querySelector("#l2b-grad-done")) {
          defs.insertAdjacentHTML("beforeend", `
            <linearGradient id="l2b-grad-done" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#F4FBF5"/>
              <stop offset="100%" stop-color="#C8E6C9"/>
            </linearGradient>
            <linearGradient id="l2b-grad-progress" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#FFFBF2"/>
              <stop offset="100%" stop-color="#FFE4A8"/>
            </linearGradient>
            <linearGradient id="l2b-grad-pending" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#FBFBFB"/>
              <stop offset="100%" stop-color="#E4E4E4"/>
            </linearGradient>
            <linearGradient id="l2b-grad-decision" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#FFFDF5"/>
              <stop offset="100%" stop-color="#FFE082"/>
            </linearGradient>`);
        }
        const paintFill = (status, fill) => {
          svg.querySelectorAll(`.node.${status} .label-container > path`).forEach((el) => {
            const current = el.getAttribute("fill");
            if (current && current !== "none") {
              el.setAttribute("fill", fill);
              el.style.setProperty("fill", fill, "important");
            }
          });
        };
        paintFill("done", "url(#l2b-grad-done)");
        paintFill("progress", "url(#l2b-grad-progress)");
        paintFill("pending", "url(#l2b-grad-pending)");
        paintFill("decision", "url(#l2b-grad-decision)");
      });
    };

    const mapDetails = document.getElementById("flow-map-details");
    if (mapDetails) {
      mapDetails.addEventListener("toggle", () => {
        if (mapDetails.open) paintMermaid();
      });
    }
  </script>
</body>
</html>
"""

BUGS_HTML = r"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Bugs found — L2B Vendor</title>
  <link rel="icon" href="assets/ic_launcher.png">
  <style>
    :root {
      --brand: #FEB637;
      --bg: #F3E6D4;
      --text: #1A1A1A;
      --muted: #4F4F4F;
      --line: rgba(255, 255, 255, 0.48);
      --card: rgba(255, 254, 249, 0.64);
    }
    body {
      margin: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
      color: var(--text);
      background:
        radial-gradient(900px 420px at 8% -8%, rgba(254, 182, 55, 0.28), transparent 55%),
        radial-gradient(820px 400px at 100% 0%, rgba(20, 82, 24, 0.10), transparent 52%),
        linear-gradient(180deg, #F7EFE3 0%, #F3E6D4 100%);
    }
    .wrap { max-width: 1100px; margin: 0 auto; padding: 24px 16px 64px; }
    a { color: #8A5A00; }
    table {
      width: 100%;
      border-collapse: collapse;
      background: var(--card);
      backdrop-filter: blur(14px) saturate(140%);
      -webkit-backdrop-filter: blur(14px) saturate(140%);
      border: 1px solid var(--line);
      border-radius: 14px;
      overflow: hidden;
      box-shadow: 0 8px 28px rgba(80, 50, 10, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.72);
    }
    th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid rgba(80, 50, 10, 0.08); vertical-align: top; font-size: 0.95rem; }
    th { background: rgba(255, 246, 229, 0.72); color: #1A1A1A; }
    .muted { color: var(--muted); }
    tr.open { background: rgba(253, 236, 234, 0.62); }
    tr.high td:nth-child(4) { font-weight: 800; color: #B71C1C; }
    tr.blocker { outline: 2px solid #B71C1C; outline-offset: -2px; background: rgba(183, 28, 28, 0.12); }
    .status-open { color: #B71C1C; font-weight: 750; }
    .blocker {
      margin: 0 0 18px;
      padding: 14px 16px;
      background: rgba(183, 28, 28, 0.10);
      border: 1px solid rgba(183, 28, 28, 0.28);
      border-radius: 12px;
    }
    .blocker p { margin: 0; }
    @media (max-width: 700px) {
      table, thead, tbody, th, td, tr { display: block; }
      thead { display: none; }
      tr { border-bottom: 8px solid var(--bg); }
      td { border: 0; }
      td::before { content: attr(data-label); display: block; font-weight: 700; margin-bottom: 4px; }
    }
  </style>
</head>
<body>
  <div class="wrap">
    <p><a href="index.html">← Back to dashboard</a></p>
    <h1>Bugs found</h1>
    <p class="muted">Human-readable list. <a href="./BUGS_FOUND.docx">Download Word file</a>.</p>
{{CALLOUT}}
    <table>
      <thead>
        <tr>
          <th>#</th><th>Module/Screen</th><th>Bug Description</th><th>Severity</th><th>Found On</th><th>Status</th>
        </tr>
      </thead>
      <tbody>
{{ROWS}}
      </tbody>
    </table>
  </div>
</body>
</html>
"""


if __name__ == "__main__":
    build()
