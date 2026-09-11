#!/usr/bin/env python3
"""Observe-only deep regression for com.l2b.app.qa. Does not modify the app."""
from __future__ import annotations

import json
import os
import re
import subprocess
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from xml.etree import ElementTree as ET

UDID = os.environ.get("DEVICE_UDID", "emulator-5554")
PKG = "com.l2b.app.qa"
ACTIVITY = "com.l2b.app.MainActivity"
ROOT = Path(__file__).resolve().parents[1] / "reports" / "deep-regression-2026-09-11"
SHOTS = ROOT / "screenshots"
DUMPS = ROOT / "dumps"
API = ROOT / "api"
LOGCAT = ROOT / "logcat"
PHONE = os.environ.get("L2B_USER_RENTAL_COMPANY_PHONE", "9000000001")
OTP = os.environ.get("L2B_QA_OTP", "")
BASE = "https://qa.waardian.com"

LANGUAGES = [
    {"key": "english", "label": "English"},
    {"key": "hindi", "label": "हिंदी"},
    {"key": "telugu", "label": "తెలుగు"},
    {"key": "kannada", "label": "ಕನ್ನಡ"},
]

ENGLISH_ONBOARDING = {
    "Grow Your Machine & Material Business With Us",
    "List your machines easily, get verified bookings, and receive fast secure payouts.",
    "Manage Everything In One Place",
    "Track orders, inventory, deliveries, and customers from a single dashboard.",
    "Skip",
    "Next",
    "Get started",
    "Are you Customer?",
    "Book Machines or order material",
    "Sign up",
    "Start your journey as a machine owner, material supplier, or operator.",
    "Mobile Number",
    "Enter mobile number",
    "I agree to the terms of service and privacy policy",
    "Get OTP",
}

results: dict = {
    "started_at": datetime.now(timezone.utc).isoformat(),
    "package": PKG,
    "build": "0.1.0-qa",
    "env": "qa.waardian.com",
    "splash": [],
    "languages": [],
    "onboarding_stress": {},
    "signup": {},
    "otp": {},
    "api": [],
    "stability": {"fatal": [], "anr": [], "process_died": []},
    "bug028": {"status": "UNTESTED", "reason": "requires authenticated Home"},
    "persistence": [],
    "notes": [],
}


def adb(*args: str, check: bool = True, timeout: int = 30) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["adb", "-s", UDID, *args],
        check=check,
        timeout=timeout,
        capture_output=True,
        text=True,
    )


def sleep(seconds: float, why: str = "") -> None:
    print(f"  wait {seconds:.1f}s {why}".rstrip())
    time.sleep(seconds)


def screenshot(name: str) -> Path:
    path = SHOTS / f"{name}.png"
    data = subprocess.check_output(["adb", "-s", UDID, "exec-out", "screencap", "-p"])
    path.write_bytes(data)
    return path


def dump_xml(name: str) -> Path:
    adb("shell", "uiautomator", "dump", "/sdcard/window_dump.xml", check=False)
    dest = DUMPS / f"{name}.xml"
    adb("pull", "/sdcard/window_dump.xml", str(dest), check=False)
    return dest


def parse_nodes(xml_path: Path) -> list[dict]:
    if not xml_path.exists() or xml_path.stat().st_size == 0:
        return []
    try:
        root = ET.parse(xml_path).getroot()
    except ET.ParseError:
        return []
    nodes = []
    for n in root.iter("node"):
        nodes.append(
            {
                "class": n.attrib.get("class", ""),
                "text": n.attrib.get("text", "") or "",
                "desc": n.attrib.get("content-desc", "") or "",
                "clickable": n.attrib.get("clickable"),
                "checked": n.attrib.get("checked"),
                "selected": n.attrib.get("selected"),
                "enabled": n.attrib.get("enabled"),
                "bounds": n.attrib.get("bounds", ""),
                "rid": n.attrib.get("resource-id", ""),
            }
        )
    return nodes


def texts_of(nodes: list[dict]) -> list[str]:
    out = []
    for n in nodes:
        t = n["text"].strip()
        if t and t not in out:
            out.append(t)
        d = n["desc"].strip()
        if d and d not in out:
            out.append(d)
    return out


def center(bounds: str) -> tuple[int, int] | None:
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not m:
        return None
    return (int(m.group(1)) + int(m.group(3))) // 2, (int(m.group(2)) + int(m.group(4))) // 2


def tap_xy(x: int, y: int) -> None:
    adb("shell", "input", "tap", str(x), str(y), check=False)


def tap_text(needle: str, xml_name: str) -> bool:
    nodes = parse_nodes(dump_xml(xml_name))
    for n in nodes:
        if n["text"] == needle or n["desc"] == needle:
            c = center(n["bounds"])
            if c:
                tap_xy(*c)
                return True
    return False


def tap_id_suffix(suffix: str, xml_name: str) -> bool:
    nodes = parse_nodes(dump_xml(xml_name))
    for n in nodes:
        if n["rid"].endswith(suffix):
            c = center(n["bounds"])
            if c:
                tap_xy(*c)
                return True
    return False


def current_activity() -> str:
    out = adb("shell", "dumpsys", "window", "windows", check=False).stdout
    m = re.search(r"mCurrentFocus=Window\{[^}]*\s([^\s}]+)\}", out)
    if m:
        return m.group(1)
    m = re.search(r"mFocusedApp=.*? ([\w.]+/[\w.]+)", out)
    return m.group(1) if m else ""


def pid() -> str:
    out = adb("shell", "pidof", PKG, check=False).stdout.strip()
    return out.split()[0] if out else ""


def dismiss_permission() -> None:
    sleep(1.5, "permission settle")
    if tap_id_suffix("permission_allow_button", "perm"):
        print("  dismissed permission Allow")
        sleep(2.0, "after Allow")
        return
    tap_text("Allow", "perm2")
    sleep(1.5, "after Allow text")


def launch_cold(clear: bool) -> float:
    if clear:
        adb("shell", "pm", "clear", PKG, check=False)
        sleep(1.0, "after pm clear")
    adb("shell", "am", "force-stop", PKG, check=False)
    sleep(0.8, "after force-stop")
    t0 = time.time()
    adb("shell", "am", "start", "-W", "-n", f"{PKG}/{ACTIVITY}", check=False)
    return time.time() - t0


def wait_for_any(needles: list[str], timeout: float = 20.0) -> tuple[float, list[str], Path]:
    t0 = time.time()
    last: list[str] = []
    xml = DUMPS / "wait.xml"
    while time.time() - t0 < timeout:
        xml = dump_xml("wait")
        last = texts_of(parse_nodes(xml))
        if any(n in last for n in needles):
            return time.time() - t0, last, xml
        time.sleep(0.4)
    return time.time() - t0, last, xml


def http_post(path: str, body: dict, tag: str) -> dict:
    data = json.dumps(body).encode()
    req = urllib.request.Request(
        BASE + path,
        data=data,
        method="POST",
        headers={"Content-Type": "application/json", "Accept": "application/json"},
    )
    t0 = time.time()
    rec: dict = {"tag": tag, "method": "POST", "url": BASE + path, "request": body}
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            raw = r.read().decode("utf-8", "replace")
            rec.update({"status": r.status, "ms": int((time.time() - t0) * 1000), "response": raw[:4000]})
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        rec.update({"status": e.code, "ms": int((time.time() - t0) * 1000), "response": raw[:4000]})
    except Exception as e:  # noqa: BLE001
        rec.update({"status": None, "ms": int((time.time() - t0) * 1000), "error": str(e)})
    if "otp" in json.dumps(body).lower():
        rec["request"] = {k: ("***" if "otp" in k else v) for k, v in body.items()}
    (API / f"{tag}.json").write_text(json.dumps(rec, indent=2, ensure_ascii=False))
    results["api"].append({k: rec[k] for k in rec if k != "response"} | {"response_excerpt": rec.get("response", rec.get("error", ""))[:240]})
    print(f"  API {tag} status={rec.get('status')} {rec.get('ms')}ms")
    return rec


def harvest_logcat(since_file: Path) -> None:
    out = adb("logcat", "-d", "-t", "400", check=False).stdout
    since_file.write_text(out)
    for line in out.splitlines():
        if "FATAL EXCEPTION" in line or "AndroidRuntime" in line and "FATAL" in line:
            results["stability"]["fatal"].append(line[:400])
        if "ANR in" in line or "Input dispatching timed out" in line:
            results["stability"]["anr"].append(line[:400])
        if "Process" in line and "died" in line and PKG in line:
            results["stability"]["process_died"].append(line[:400])


def verdict(ok: bool | None, blocked: str | None = None) -> str:
    if blocked:
        return "BLOCKED"
    if ok is None:
        return "UNTESTED"
    return "PASS" if ok else "FAIL"


def splash_trials(n: int = 3) -> None:
    print("=== SPLASH x3 ===")
    for i in range(1, n + 1):
        rec: dict = {"iteration": i}
        launch_s = launch_cold(clear=True)
        rec["am_start_s"] = round(launch_s, 2)
        rec["activity_immediately"] = current_activity()
        rec["pid"] = pid()
        screenshot(f"splash_{i}_t0")
        sleep(0.4, "splash frame")
        screenshot(f"splash_{i}_t400ms")
        to_lang, texts, _ = wait_for_any(
            ["Welcome to L2B", "Allow L2B Vendor to send you notifications?", "Choose your language"],
            timeout=18,
        )
        rec["time_to_first_stable_s"] = round(to_lang, 2)
        rec["first_texts"] = texts[:20]
        rec["activity_stable"] = current_activity()
        rec["pid_stable"] = pid()
        screenshot(f"splash_{i}_stable")
        dump_xml(f"splash_{i}_stable")
        blank = (not texts) or (texts == [""])
        rec["blank_screen"] = blank
        rec["crash"] = not bool(rec["pid_stable"])
        rec["result"] = verdict(bool(rec["pid_stable"]) and not blank and to_lang < 15)
        rec["note"] = "Splash is transient; first captured stable UI is permission or language."
        results["splash"].append(rec)
        print(f"  splash {i}: {rec['result']} t={rec['time_to_first_stable_s']}s act={rec['activity_stable']}")
        harvest_logcat(LOGCAT / f"splash_{i}.txt")
        dismiss_permission()
        sleep(1.5, "between splash trials")


def language_walk() -> None:
    print("=== LANGUAGE MATRIX ===")
    for lang in LANGUAGES:
        print(f"-- {lang['key']} --")
        rec: dict = {"language": lang["label"], "key": lang["key"]}
        launch_cold(clear=True)
        dismiss_permission()
        t, texts, _ = wait_for_any(["Welcome to L2B", "Choose your language", lang["label"]], 18)
        rec["language_screen_s"] = round(t, 2)
        rec["language_options_shown"] = [x["label"] for x in LANGUAGES if x["label"] in texts]
        rec["all_options_visible"] = all(x["label"] in texts for x in LANGUAGES)
        screenshot(f"lang_{lang['key']}_before")
        dump_xml(f"lang_{lang['key']}_before")
        nodes_before = parse_nodes(DUMPS / f"lang_{lang['key']}_before.xml")
        rec["selection_before"] = [
            {"text": n["text"], "checked": n["checked"], "selected": n["selected"]}
            for n in nodes_before
            if n["text"] in {x["label"] for x in LANGUAGES}
        ]
        tapped = tap_text(lang["label"], f"lang_{lang['key']}_tap")
        rec["selection_ui"] = verdict(tapped)
        sleep(1.8, "after language tap")
        screenshot(f"lang_{lang['key']}_after_select")
        dump_xml(f"lang_{lang['key']}_after_select")
        nodes_after = parse_nodes(DUMPS / f"lang_{lang['key']}_after_select.xml")
        rec["selection_after"] = [
            {"text": n["text"], "checked": n["checked"], "selected": n["selected"]}
            for n in nodes_after
            if n["text"] in {x["label"] for x in LANGUAGES}
        ]
        rec["checkbox_radio_state"] = (
            "FAIL"
            if all(n.get("checked") in (None, "false") and n.get("selected") in (None, "false") for n in rec["selection_after"])
            else "PASS"
        )
        rec["language_screen_texts"] = texts_of(nodes_after)
        continued = tap_text("Get started", f"lang_{lang['key']}_cta") or tap_text("शुरू करें", f"lang_{lang['key']}_cta2")
        rec["continue"] = verdict(continued)
        sleep(2.2, "onboarding 1")
        screenshot(f"lang_{lang['key']}_ob1")
        dump_xml(f"lang_{lang['key']}_ob1")
        ob1 = texts_of(parse_nodes(DUMPS / f"lang_{lang['key']}_ob1.xml"))
        rec["onboarding1_texts"] = ob1
        rec["onboarding1_still_english"] = [s for s in ob1 if s in ENGLISH_ONBOARDING]
        rec["onboarding_screen_1"] = verdict(any("Grow" in s or "Machine" in s or len(s) > 8 for s in ob1))
        if lang["key"] != "english":
            rec["onboarding_screen_1"] = verdict(bool(ob1) and not any(
                s in {"Grow Your Machine & Material Business With Us", "List your machines easily, get verified bookings, and receive fast secure payouts."}
                for s in ob1
            ))
        next_tapped = tap_text("Next", f"lang_{lang['key']}_next") or tap_text("आगे", f"lang_{lang['key']}_next2")
        sleep(2.2, "onboarding 2")
        screenshot(f"lang_{lang['key']}_ob2")
        dump_xml(f"lang_{lang['key']}_ob2")
        ob2 = texts_of(parse_nodes(DUMPS / f"lang_{lang['key']}_ob2.xml"))
        rec["onboarding2_texts"] = ob2
        rec["onboarding2_still_english"] = [s for s in ob2 if s in ENGLISH_ONBOARDING]
        rec["onboarding_screen_2"] = verdict(bool(ob2))
        if lang["key"] != "english":
            rec["onboarding_screen_2"] = verdict(bool(ob2) and "Manage Everything In One Place" not in ob2)
        rec["next_worked"] = verdict(next_tapped)
        gs = tap_text("Get started", f"lang_{lang['key']}_gs") or tap_text("शुरू करें", f"lang_{lang['key']}_gs2")
        sleep(2.4, "signup")
        screenshot(f"lang_{lang['key']}_signup")
        dump_xml(f"lang_{lang['key']}_signup")
        su = texts_of(parse_nodes(DUMPS / f"lang_{lang['key']}_signup.xml"))
        rec["signup_texts"] = su
        rec["signup_still_english"] = [s for s in su if s in ENGLISH_ONBOARDING]
        rec["sign_up"] = verdict("Sign up" in su or "+91" in su or any("OTP" in s or "मोबाइल" in s for s in su))
        if lang["key"] != "english":
            rec["sign_up"] = verdict(bool(su) and "Start your journey as a machine owner, material supplier, or operator." not in su)
        rec["other_reachable_screens"] = "UNTESTED"
        rec["cta_get_started"] = verdict(gs)

        adb("shell", "am", "force-stop", PKG, check=False)
        sleep(1.5, "kill for persistence")
        launch_cold(clear=False)
        dismiss_permission()
        sleep(2.5, "relaunch settle")
        screenshot(f"lang_{lang['key']}_relaunch")
        dump_xml(f"lang_{lang['key']}_relaunch")
        rel = texts_of(parse_nodes(DUMPS / f"lang_{lang['key']}_relaunch.xml"))
        rec["relaunch_texts"] = rel
        rec["language_persistence"] = verdict(bool(rel))
        if lang["key"] != "english":
            english_markers = {
                "Welcome to L2B",
                "Grow Your Machine & Material Business With Us",
                "Sign up",
                "Choose your language",
            }
            rec["language_persistence"] = verdict(not (english_markers & set(rel)) or lang["label"] in rel)
        results["languages"].append(rec)
        results["persistence"].append(
            {
                "language": lang["label"],
                "relaunch_texts": rel,
                "result": rec["language_persistence"],
            }
        )
        harvest_logcat(LOGCAT / f"lang_{lang['key']}.txt")


def onboarding_stress() -> None:
    print("=== ONBOARDING STRESS ===")
    rec: dict = {"iterations": 3, "white_screen": False, "rapid_nav": []}
    launch_cold(clear=True)
    dismiss_permission()
    wait_for_any(["Welcome to L2B"], 15)
    tap_text("English", "stress_en")
    sleep(1.2)
    tap_text("Get started", "stress_gs")
    sleep(2.0)
    screenshot("ob_stress_start")
    for i in range(3):
        dump_xml(f"ob_stress_{i}_a")
        t = texts_of(parse_nodes(DUMPS / f"ob_stress_{i}_a.xml"))
        rec["rapid_nav"].append({"i": i, "before": t[:8]})
        tap_text("Next", f"ob_stress_{i}_next")
        time.sleep(0.25)
        tap_text("Skip", f"ob_stress_{i}_skip")
        time.sleep(0.25)
        adb("shell", "input", "keyevent", "4", check=False)  # BACK
        sleep(0.8, "after back")
        dump_xml(f"ob_stress_{i}_b")
        after = texts_of(parse_nodes(DUMPS / f"ob_stress_{i}_b.xml"))
        rec["rapid_nav"][-1]["after_back"] = after[:8]
        rec["rapid_nav"][-1]["blank"] = len(after) == 0
        if not after:
            rec["white_screen"] = True
            screenshot(f"ob_stress_blank_{i}")
        tap_text("Next", f"ob_stress_{i}_next2")
        sleep(1.0)
    rec["pid"] = pid()
    rec["result"] = verdict(bool(rec["pid"]) and not rec["white_screen"])
    results["onboarding_stress"] = rec
    harvest_logcat(LOGCAT / "onboarding_stress.txt")


def signup_validation() -> None:
    print("=== SIGN UP VALIDATION ===")
    rec: dict = {}
    launch_cold(clear=True)
    dismiss_permission()
    wait_for_any(["Welcome to L2B"], 15)
    tap_text("English", "su_en")
    sleep(1.2)
    tap_text("Get started", "su_gs")
    sleep(1.8)
    tap_text("Next", "su_next")
    sleep(1.8)
    tap_text("Get started", "su_gs2")
    sleep(2.2)
    screenshot("signup_empty")
    dump_xml("signup_empty")
    nodes = parse_nodes(DUMPS / "signup_empty.xml")
    rec["texts"] = texts_of(nodes)
    rec["country_code_plus91"] = "+91" in rec["texts"]
    rec["checkbox_present"] = any(n["class"].endswith("CheckBox") for n in nodes)
    rec["get_otp_present"] = any("Get OTP" in n["text"] for n in nodes)
    otp_node = next((n for n in nodes if n["text"] == "Get OTP"), None)
    rec["get_otp_clickable_on_empty"] = otp_node["clickable"] if otp_node else None
    parent_clickable = any(
        n["clickable"] == "true" and "Get OTP" in (n["text"] + n["desc"]) for n in nodes
    )
    rec["get_otp_any_clickable"] = parent_clickable
    tap_text("Get OTP", "su_otp_empty")
    sleep(1.5, "empty Get OTP")
    screenshot("signup_empty_after_otp_tap")
    dump_xml("signup_empty_after_otp_tap")
    rec["still_on_signup_after_empty_otp"] = "Sign up" in texts_of(parse_nodes(DUMPS / "signup_empty_after_otp_tap.xml"))

    adb("shell", "input", "text", "123", check=False)
    sleep(1.0)
    screenshot("signup_invalid_123")
    dump_xml("signup_invalid_123")
    tap_text("Get OTP", "su_otp_invalid")
    sleep(1.5)
    screenshot("signup_invalid_after_otp")
    dump_xml("signup_invalid_after_otp")
    rec["still_on_signup_after_invalid"] = "Sign up" in texts_of(parse_nodes(DUMPS / "signup_invalid_after_otp.xml"))

    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END", check=False)
    for _ in range(6):
        adb("shell", "input", "keyevent", "KEYCODE_DEL", check=False)
    adb("shell", "input", "text", PHONE, check=False)
    sleep(1.2)
    screenshot("signup_valid_unchecked")
    dump_xml("signup_valid_unchecked")
    tap_text("Get OTP", "su_otp_unchecked")
    sleep(1.5)
    screenshot("signup_valid_unchecked_after")
    dump_xml("signup_valid_unchecked_after")
    rec["still_on_signup_unchecked"] = "Sign up" in texts_of(parse_nodes(DUMPS / "signup_valid_unchecked_after.xml"))

    if tap_id_suffix("CheckBox", "su_cb") or True:
        nodes = parse_nodes(dump_xml("su_cb_find"))
        cb = next((n for n in nodes if n["class"].endswith("CheckBox")), None)
        if cb and center(cb["bounds"]):
            tap_xy(*center(cb["bounds"]))
            rec["checkbox_toggled"] = True
        else:
            tap_text("I agree to the terms of service and privacy policy", "su_terms")
            rec["checkbox_toggled"] = True
    sleep(1.2)
    screenshot("signup_valid_checked")
    dump_xml("signup_valid_checked")
    cb_after = next((n for n in parse_nodes(DUMPS / "signup_valid_checked.xml") if n["class"].endswith("CheckBox")), None)
    rec["checkbox_checked_attr"] = cb_after["checked"] if cb_after else None

    tap_text("Get OTP", "su_otp_1")
    time.sleep(0.15)
    tap_text("Get OTP", "su_otp_2")
    sleep(3.0, "after double Get OTP")
    screenshot("signup_after_get_otp")
    dump_xml("signup_after_get_otp")
    after = texts_of(parse_nodes(DUMPS / "signup_after_get_otp.xml"))
    rec["after_get_otp_texts"] = after
    rec["otp_screen_opened"] = any("OTP" in t and "Get OTP" not in t for t in after) or any(
        "Enter" in t and "OTP" in t for t in after
    ) or any(re.fullmatch(r"\d", t) is None and "otp" in t.lower() for t in after)
    rec["left_signup"] = "Sign up" not in after
    adb("shell", "input", "keyevent", "4", check=False)
    sleep(1.2, "back from OTP/signup")
    screenshot("signup_after_back")
    dump_xml("signup_after_back")
    rec["after_back_texts"] = texts_of(parse_nodes(DUMPS / "signup_after_back.xml"))[:15]
    rec["pid"] = pid()
    rec["result"] = verdict(bool(rec["pid"]) and rec["country_code_plus91"] and rec["checkbox_present"])
    results["signup"] = rec
    harvest_logcat(LOGCAT / "signup.txt")


def otp_flow() -> None:
    print("=== OTP ===")
    rec: dict = {"ui": "UNTESTED", "api_wrong": None, "api_valid": "UNTESTED"}
    rec["api_wrong"] = http_post("/api/v1/auth/verify-otp", {"phone": PHONE, "otp_code": "0000"}, "verify_wrong")
    if not OTP:
        rec["ui"] = "BLOCKED"
        rec["reason"] = "L2B_QA_OTP not set; UI valid-OTP not attempted. Wrong OTP API captured."
        results["otp"] = rec
        return
    launch_cold(clear=True)
    dismiss_permission()
    wait_for_any(["Welcome to L2B"], 15)
    tap_text("English", "otp_en")
    sleep(1.0)
    tap_text("Get started", "otp_gs")
    sleep(1.6)
    tap_text("Next", "otp_next")
    sleep(1.6)
    tap_text("Get started", "otp_gs2")
    sleep(2.0)
    nodes = parse_nodes(dump_xml("otp_signup"))
    cb = next((n for n in nodes if n["class"].endswith("CheckBox")), None)
    if cb and center(cb["bounds"]):
        tap_xy(*center(cb["bounds"]))
    sleep(0.8)
    adb("shell", "input", "text", PHONE, check=False)
    sleep(1.0)
    tap_text("Get OTP", "otp_send")
    sleep(3.5, "OTP screen")
    screenshot("otp_screen")
    dump_xml("otp_screen")
    rec["otp_screen_texts"] = texts_of(parse_nodes(DUMPS / "otp_screen.xml"))
    rec["otp_screen_opened"] = any("OTP" in t for t in rec["otp_screen_texts"])
    adb("shell", "input", "text", "0000", check=False)
    sleep(1.5)
    screenshot("otp_invalid_ui")
    dump_xml("otp_invalid_ui")
    rec["invalid_otp_texts"] = texts_of(parse_nodes(DUMPS / "otp_invalid_ui.xml"))
    for _ in range(4):
        adb("shell", "input", "keyevent", "KEYCODE_DEL", check=False)
    adb("shell", "input", "text", OTP, check=False)
    sleep(3.0, "after valid OTP")
    screenshot("otp_valid_ui")
    dump_xml("otp_valid_ui")
    rec["valid_otp_texts"] = texts_of(parse_nodes(DUMPS / "otp_valid_ui.xml"))
    rec["ui"] = verdict(rec["otp_screen_opened"])
    api_ok = http_post("/api/v1/auth/verify-otp", {"phone": PHONE, "otp_code": OTP}, "verify_valid")
    rec["api_valid"] = {"status": api_ok.get("status"), "excerpt": (api_ok.get("response") or api_ok.get("error", ""))[:300]}
    results["otp"] = rec
    harvest_logcat(LOGCAT / "otp.txt")


def post_login_bug028() -> None:
    print("=== BUG-028 / HOME ===")
    dump_xml("post_login")
    texts = texts_of(parse_nodes(DUMPS / "post_login.xml"))
    screenshot("post_login")
    homeish = any(t in texts for t in ("Home", "Fleet", "Team", "Account", "Calendar", "Dashboard"))
    if not homeish:
        results["bug028"] = {
            "status": "BLOCKED",
            "reason": "Did not reach authenticated Home. Visible: " + ", ".join(texts[:12]),
            "iterations": 0,
        }
        return
    rec = {"status": "NOT REPRODUCED", "iterations": 0, "scenarios": []}
    labels = ["Team", "Account", "Fleet", "Calendar", "Home"]
    for label in labels:
        for i in range(3):
            rec["iterations"] += 1
            tap_text(label, f"b028_{label}_{i}")
            sleep(1.4, f"{label} {i}")
            screenshot(f"b028_{label}_{i}")
            dump_xml(f"b028_{label}_{i}")
            t = texts_of(parse_nodes(DUMPS / f"b028_{label}_{i}.xml"))
            blank = len(t) == 0
            adb("shell", "input", "keyevent", "4", check=False)
            sleep(0.9)
            dump_xml(f"b028_{label}_{i}_back")
            back = texts_of(parse_nodes(DUMPS / f"b028_{label}_{i}_back.xml"))
            scenario = {
                "scenario": f"Home→{label}→Back #{i+1}",
                "blank_on_open": blank,
                "blank_on_back": len(back) == 0,
                "pid": pid(),
                "texts_open": t[:10],
            }
            rec["scenarios"].append(scenario)
            if blank or not back:
                rec["status"] = "FAIL"
                screenshot(f"b028_fail_{label}_{i}")
    results["bug028"] = rec
    harvest_logcat(LOGCAT / "bug028.txt")


def main() -> None:
    for d in (SHOTS, DUMPS, API, LOGCAT):
        d.mkdir(parents=True, exist_ok=True)
    adb("logcat", "-c", check=False)
    splash_trials(3)
    language_walk()
    onboarding_stress()
    signup_validation()
    otp_flow()
    post_login_bug028()
    harvest_logcat(LOGCAT / "final.txt")
    results["ended_at"] = datetime.now(timezone.utc).isoformat()
    results["pid_final"] = pid()
    (ROOT / "results.json").write_text(json.dumps(results, indent=2, ensure_ascii=False))
    print("WROTE", ROOT / "results.json")


if __name__ == "__main__":
    main()
