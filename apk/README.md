# QA APK (project-local)

Put the Vendor QA build here:

```text
apk/app-qa-release.apk
```

Install / refresh on the emulator:

```bash
adb install -r apk/app-qa-release.apk
```

`run-tests.sh` and `config.properties` (`app.path`) use this path only — not any external folder.

The `.apk` binary is gitignored (large). Keep this README so the folder stays in the repo.
