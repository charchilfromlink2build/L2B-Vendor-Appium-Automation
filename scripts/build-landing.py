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
    passed = failed = skipped = 0
    for f in files:
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
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
        "source": "allure-results",
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
        parts.append(
            f"""      <article class="card status-{html.escape(STATUS_CLASS[status])}">
        <div class="card-top">
          <h3>{html.escape(mod["name"])}</h3>
          <span class="badge {html.escape(STATUS_CLASS[status])}">{html.escape(badge)}</span>
        </div>
        <p class="cases">{html.escape(cases_label(mod))}</p>
      </article>"""
        )
    return "\n".join(parts)


def mermaid_node(mod: dict) -> str:
    label = mod["name"].replace('"', "'")
    return f'{mod["id"]}(["{label}"])'


def mermaid_graph(coverage: dict) -> str:
    by_id = {m["id"]: m for m in coverage["modules"]}
    launch = coverage["firstLaunch"]
    lines = ["flowchart TB", '  subgraph launch["First launch"]', "    direction TB"]
    for i, mid in enumerate(launch):
        lines.append(f"    {mermaid_node(by_id[mid])}")
        if i > 0:
            lines.append(f"    {launch[i-1]} --> {mid}")
    lines.append("  end")
    lines.append('  subgraph loggedin["After login — not started"]')
    lines.append("    direction TB")
    lines.append(f"    {mermaid_node(by_id['home'])}")
    for mid in coverage["postLoginFromHome"]:
        lines.append(f"    {mermaid_node(by_id[mid])}")
        lines.append(f"    home --> {mid}")
    for mid in coverage.get("postLoginFromSettings", []):
        lines.append(f"    {mermaid_node(by_id[mid])}")
        lines.append(f"    settings --> {mid}")
    lines.append("  end")
    lines.append("  otp -.->|valid OTP, not automated| home")
    for status, ids in (
        ("done", [m["id"] for m in coverage["modules"] if m["status"] == "done"]),
        ("progress", [m["id"] for m in coverage["modules"] if m["status"] == "in-progress"]),
        ("pending", [m["id"] for m in coverage["modules"] if m["status"] == "pending"]),
    ):
        if ids:
            lines.append(f"  class {','.join(ids)} {status}")
    lines.append("  classDef done fill:#E8F5E9,stroke:#2E7D32,color:#145218,stroke-width:2px")
    lines.append("  classDef progress fill:#FFF6E5,stroke:#D4970A,color:#7A5200,stroke-width:2px")
    lines.append("  classDef pending fill:#F4F4F4,stroke:#8D8D8D,color:#3D3D3D,stroke-width:2px")
    return "\n".join(lines)


def whats_next(modules: list[dict]) -> str:
    for status in ("in-progress", "pending"):
        for mod in modules:
            if mod.get("status") == status:
                return mod["name"]
    return "All listed modules"


def write_bugs_html(bugs: list[dict], updated: str) -> None:
    rows = []
    for b in bugs:
        status_l = (b.get("status") or "").lower()
        row_class = "open" if status_l == "open" else ""
        status_class = "status-open" if status_l == "open" else ""
        rows.append(
            f'<tr class="{row_class}">'
            f'<td data-label="#">{html.escape(b["num"])}</td>'
            f'<td data-label="Module/Screen">{html.escape(b["module"])}</td>'
            f'<td data-label="Bug Description">{html.escape(b["description"])}</td>'
            f'<td data-label="Severity">{html.escape(b["severity"])}</td>'
            f'<td data-label="Found On">{html.escape(b["found"])}</td>'
            f'<td data-label="Status" class="{status_class}">{html.escape(b["status"])}</td>'
            "</tr>"
        )
    body = "\n".join(rows) if rows else '<tr><td colspan="6">No bugs recorded yet.</td></tr>'
    (DOCS / "bugs.html").write_text(
        BUGS_HTML.replace("{{UPDATED}}", html.escape(updated)).replace("{{ROWS}}", body),
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
    updated = datetime.now(IST).strftime("%d %b %Y")
    write_bugs_html(bugs, updated)

    page = INDEX_HTML
    replacements = {
        "{{AUTOMATED_SCREENS}}": str(automated),
        "{{TOTAL_SCREENS}}": str(total_screens),
        "{{COVERAGE_PCT}}": pct_label,
        "{{PASSED}}": str(int(run.get("passed") or 0)),
        "{{FAILED}}": str(int(run.get("failed") or 0)),
        "{{SKIPPED}}": str(int(run.get("skipped") or 0)),
        "{{WHATS_NEXT}}": html.escape(whats_next(modules)),
        "{{MODULE_CARDS}}": module_cards(modules),
        "{{MERMAID}}": mermaid_graph(coverage),
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
    .purpose { margin: 6px 0 0; color: var(--muted); font-size: 0.92rem; font-weight: 400; max-width: 42em; line-height: 1.4; }
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
    .badge.progress { background: var(--progress-bg); color: var(--progress); }
    .badge.pending { background: var(--pending-bg); color: var(--pending); }
    .card.status-done { border-color: rgba(46, 125, 50, 0.28); }
    .card.status-progress { border-color: rgba(212, 151, 10, 0.35); }
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
    .flow { padding: 12px; overflow-x: auto; }
    .flow svg { background: transparent !important; }
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
    .legend { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 10px; color: var(--muted); font-size: 0.85rem; }
    .dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 6px; vertical-align: middle; }
    .dot.done { background: var(--done); }
    .dot.progress { background: var(--brand); }
    .dot.pending { background: #8D8D8D; }
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
      .grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
      .grid .card:nth-child(4n)::after { content: "→"; }
      .grid .card:nth-child(2n)::after { content: none; }
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

    <h2>Modules</h2>
    <div class="grid">
{{MODULE_CARDS}}
    </div>

    <h2>App flow</h2>
    <p class="note">Green is done. Amber is in progress. Grey is not started. After a valid OTP the app lands on Home.</p>
    <div class="flow">
      <pre class="mermaid">
{{MERMAID}}
      </pre>
      <div class="legend">
        <span><i class="dot done"></i>Done</span>
        <span><i class="dot progress"></i>In progress</span>
        <span><i class="dot pending"></i>Pending</span>
      </div>
    </div>

    <footer>
      <a class="btn primary" href="https://charchilfromlink2build.github.io/L2B-Vendor-Appium-Automation/allure/">Open full Allure report</a>
      <a class="btn bugs" href="bugs.html">Bugs found ({{BUGS_OPEN}} open)</a>
      <a class="btn ghost" href="BUGS_FOUND.docx">Download BUGS_FOUND.docx</a>
    </footer>
  </div>
  <script type="module">
    import mermaid from "https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs";
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
      flowchart: { htmlLabels: true, curve: "basis", padding: 12 }
    });
    await mermaid.run({ querySelector: ".mermaid" });
    const ns = "http://www.w3.org/2000/svg";
    document.querySelectorAll(".flow svg").forEach((svg) => {
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
          </linearGradient>`);
      }
      const paintFill = (status, fill) => {
        svg.querySelectorAll(`.node.${status} .label-container > path`).forEach((el) => {
          const current = el.getAttribute("fill");
          if (current && current !== "none") {
            el.setAttribute("fill", fill);
            el.style.setProperty("fill", fill, "important");
          }
          el.style.removeProperty("stroke-width");
        });
        svg.querySelectorAll(`.node.${status} .label rect`).forEach((el) => {
          el.setAttribute("fill", "transparent");
          el.setAttribute("stroke", "none");
          el.style.setProperty("fill", "transparent", "important");
          el.style.setProperty("stroke", "none", "important");
        });
      };
      paintFill("done", "url(#l2b-grad-done)");
      paintFill("progress", "url(#l2b-grad-progress)");
      paintFill("pending", "url(#l2b-grad-pending)");
      svg.querySelectorAll(".node").forEach((node) => {
        if (node.querySelector(":scope > .node-visual")) return;
        const wrap = document.createElementNS(ns, "g");
        wrap.setAttribute("class", "node-visual");
        while (node.firstChild) wrap.appendChild(node.firstChild);
        node.appendChild(wrap);
      });
    });
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
    .status-open { color: #B71C1C; font-weight: 750; }
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
    <p class="muted">Human-readable list. Last generated {{UPDATED}}. <a href="./BUGS_FOUND.docx">Download Word file</a>.</p>
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
