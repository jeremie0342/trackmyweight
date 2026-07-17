# Security Policy

## Supported versions

Only the latest major release receives security fixes. The project is under
active development — please update to the latest version.

| Version | Supported |
|---------|-----------|
| 1.x     | ✅        |
| < 1.0   | ❌        |

## Reporting a vulnerability

**Do NOT open a public issue for a security problem.**

Send a private report through one of the following channels:

1. **Preferred**: [GitHub Security Advisory](https://github.com/jeremie0342/trackmyweight/security/advisories/new)
2. **Alternative**: DM the main maintainer on GitHub (@jeremie0342)

Please include:
- Description of the issue and estimated impact
- Affected version
- Reproduction steps
- Suggested fix if you have one

## What to expect

- **Acknowledgement** within 72 hours
- **Initial assessment** within 7 days (impact, severity, remediation plan)
- **Fix** within a reasonable timeframe based on severity:
  - Critical: patch release within 7 days
  - High: within 30 days
  - Medium: in the next minor release
  - Low: in the next major release
- **Credit** in the CHANGELOG and the advisory published after the fix (unless you prefer to stay anonymous)

## Scope

The app is **local-first**: all your data stays on your phone. The only
network-facing components are:

- Health Connect (read only, Android sandbox data)
- No external APIs, no telemetry, no analytics

Security perimeter:

- ✅ AES-256 encryption of progress photos (Android Keystore)
- ✅ JSON backup exportable locally only (no auto cloud upload)
- ✅ No data sent to third-party servers
- ✅ Minimal permissions (camera opt-in, notifications opt-in, Health Connect opt-in)

## Out of scope

- Vulnerabilities in third-party dependencies (Room, Compose, Hilt) — please report directly to Google/JetBrains
- Attacks requiring physical unlocked access to the device
- Android OS vulnerabilities outside the app's sandbox
