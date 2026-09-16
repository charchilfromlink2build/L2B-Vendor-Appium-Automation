# L2B Vendor Appium Automation

Black-box UI tests for the **L2B Vendor Partner** QA APK (`com.l2b.app.qa`).

Aligned with **L2B-Customer-Testing-App**: Appium 3 + Java 17 + TestNG + Maven + Allure, Compose clickable-View XPath, `Waits` / `DriverFactory` / `DriverManager` / `TestListener`.

This repo is separate from `L2B-Vendor-app-test` and `L2B-Vendor-backend-api-test`.

---

## Prerequisites

- JDK **17** — `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`
- Maven 3.9+
- Android SDK, AVD `Medium_Phone_API_36.1`
- Appium 3 (`appium -v` → 3.x)
- `appium driver install uiautomator2`
- QA APK installed: `adb install -r ../L2B-Vendor-app-test/apk/app-qa-release.apk`

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

---

## 1. Start the emulator

```bash
$ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_36.1
```

Wait until `adb devices` shows `emulator-5554	device`.

---

## 2. Start Appium

```bash
appium --address 127.0.0.1 --port 4723
```

Default URL: `http://127.0.0.1:4723/`

---

## 3. Run tests

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=$HOME/Library/Android/sdk
mvn test && ./report.sh
```

That sequence is required for the Allure **TREND** widget: `./report.sh` copies `target/allure-report/history` into the next results dir. `mvn allure:serve` skips history and will not grow the trend.

Default `mvn test` (`src/test/resources/testng.xml`) is the full committed suite: **SplashScreenTest**, **LanguageScreenTest**, onboarding **SmokeTest**, **OnboardingCarouselTest**, then **SignUpTest**. Isolated runs: `splash-screen.xml`, `language-screen.xml`, `onboarding-carousel.xml`, and `sign-up-screen.xml`.

**SmokeTest sets `noReset=false`.** That **clears Vendor app data** so first-launch (language → carousel → Sign up) can run. Do not run it if you still need the current logged-in session.

Smoke does **not** tap Get OTP.

---

## 4. Allure report

### Local preview (your machine)

```bash
mvn test && ./report.sh
allure open target/allure-report
```

Do **not** open HTML via Cursor Live Preview (`127.0.0.1:5500`) — it auto-reloads. `mvn allure:serve` also skips history.

### Company pack (does not disappear on `mvn clean`)

After tests:

```bash
./scripts/publish-allure.sh
```

That writes:

- `reports/company-allure/latest/index.html` — single-file report (Slack / email / Drive)
- `reports/company-allure/L2B-Vendor-Allure-<timestamp>.zip` — send this zip
- A dated copy under `reports/company-allure/<timestamp>/`

Company: unzip → open `index.html` in Chrome. If the page is blank, they should run `python3 -m http.server 8080` inside the unzipped folder and open http://127.0.0.1:8080.

### Live URL (company)

Same link every time. After `./run-tests.sh` or `./scripts/publish-allure.sh` it updates automatically:

**https://charchilfromlink2build.github.io/L2B-Vendor-Appium-Automation/** — status page (flow + coverage).  
**https://charchilfromlink2build.github.io/L2B-Vendor-Appium-Automation/allure/** — full Allure report.

Repo: https://github.com/charchilfromlink2build/L2B-Vendor-Appium-Automation (public GitHub Pages). Anyone with the URL can view screenshots from the last run.

To publish without re-running tests:

```bash
./scripts/deploy-allure-live.sh
```

Skip the live deploy: `DEPLOY_LIVE=0 ./scripts/publish-allure.sh`

---

## Layout

```
src/test/java/com/l2b/vendor/
  environment/          Config, device/Appium check, Allure metadata
  core/                 driver, waits, HTTP, BaseTest (session only)
  modules/<feature>/
    data/               Swagger API + DTOs
    domain/             *Environment (noReset, session, prepareSuite)
    presentation/       pages + tests
```

Swagger: `https://qa.waardian.com/api/v1/openapi.json`. Only **onboarding** UI tests run today.
