# Puffy

Android VLESS/Xray client. Minimal ocean-themed UI, subscription support
(paste a link, get traffic/expiry info and a server list), manual server
links, ping check per server, settings screen.

## Build via GitHub Actions (no PC needed)

`.github/workflows/build.yml` downloads a prebuilt `libv2ray.aar` from
`2dust/AndroidLibXrayLite` releases and runs `./gradlew assembleDebug`.
Push this repo to GitHub, open the **Actions** tab, download the
**Puffy-debug-apk** artifact once the run finishes.

## What's inside
```
app/src/main/kotlin/com/egor201/puffy/
  MainActivity.kt            — navigation, VPN permission, orchestration
  model/ServerProfile.kt      — one server (parsed from a vless:// link)
  model/Subscription.kt       — a subscription group + its metadata
  core/VlessUriParser.kt      — vless:// URI parsing
  core/SubscriptionParser.kt  — subscription body + userinfo headers
  core/XrayConfigBuilder.kt   — Xray JSON config, tun inbound
  core/XrayEngine.kt          — libv2ray.aar bridge (Rust FFI boundary later)
  core/PingTester.kt          — TCP connect-time latency check
  core/VpnCoreService.kt      — VpnService, tun interface, notification
  net/SubscriptionFetcher.kt  — plain java.net HTTP fetch
  data/AppRepository.kt       — DataStore-backed storage
  ui/                         — Compose screens (Home, Add, Settings)
  ui/theme/Theme.kt           — ocean color palette
```

## Roadmap notes
- Subscription auto-update currently runs a check on app launch (compares
  `lastUpdatedAt` + `updateIntervalHours`), not a true background job —
  add WorkManager later if periodic background refresh matters.
- Routing presets (e.g. RU domains direct, everything else tunneled) are
  not wired yet — `XrayConfigBuilder` currently ships an empty routing
  table; presets would fill `routing.rules` with geosite-based rules.
- `XrayEngine.kt` is the intended seam for a future Rust rewrite of the
  business logic — everything above it can move, `MainActivity`/
  `VpnCoreService` are the only pieces that must stay JVM.
