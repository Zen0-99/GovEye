# GovEye

> **Public servants, watched by the private citizen.**

A free, open-source Android app that makes UK Parliament as followable as a sports league. Track how your MP votes, what they say in debates, who's paying them, and what bills they're pushing — all in real time, right in your pocket.

## What you get

- **Follow MPs** — pick the MPs you care about and get notified the moment they vote, speak, or push a bill forward
- **Every vote** — see how any MP voted on any division, with full Aye/No breakdowns and rebellion stats
- **Bills in progress** — track bills as they move through Parliament, stage by stage, with plain-English summaries
- **Who's paying them** — registered financial interests: income, gifts, overseas visits, shareholdings, family lobbying
- **Expenses** — IPSA-published expense claims, broken down by category
- **Debates** — read what was said in Parliament, who said it, and when
- **Committees** — see which MPs sit on which committees
- **Find your MP** — postcode lookup, council information, party stats

Everything works offline once loaded. No account needed. No ads. No tracking.

## How it works

GovEye bundles a compact database directly in the app — all 650 MPs, every vote, every bill, every registered financial interest — so it's instant from the moment you open it. A background worker checks for updates throughout the day and downloads them silently.

The database is built and hosted in a separate repo: [goveye-data](https://github.com/Zen0-99/goveye-data). It fetches from UK Parliament's public APIs on a staggered schedule (votes every 6 hours, MPs daily, committees weekly) and publishes updates that the app downloads automatically.

## Status

**Early development — not yet released.** No APKs are published yet. The app
is being built in the open; expect breaking changes until v1.0.

## For contributors

### Tech stack

- **Android Gradle Plugin** 9.3.0 / **Gradle** 9.7.0
- **Kotlin** 2.4.10
- **Jetpack Compose** + **Material 3**
- **Navigation 3**
- **Hilt** (dependency injection)
- **Room** (local persistence with FTS)
- **Retrofit / OkHttp** (networking)
- **DataStore** (preferences)

### Build

Requirements: **JDK 17+** and Android Studio (latest canary/beta for AGP 9
support).

```bash
./gradlew assembleDebug
```

### CI

Continuous integration runs on every pull request and push to `main`:

```bash
./gradlew spotlessCheck testDebugUnitTest assembleDebug lintDebug
```

Format code before pushing:

```bash
./gradlew spotlessApply
```

### Data pipeline

The bundled seed database is built in the [goveye-data](https://github.com/Zen0-99/goveye-data) repo. See `goveye-data/AGENTS.md` for the full pipeline guide — build scripts, per-API DBs, merge order, and the critical "when NOT to run a build script" decision guide.

When adding a Room migration that changes derived data (e.g. re-mapping a `bucket` column), the same SQL should be run against the local per-API DB in goveye-data — do NOT re-run the build script (it re-fetches all 650 MPs from the API unnecessarily).

### Project notes

See [`AGENTS.md`](AGENTS.md) for project-specific conventions, build commands, and design preferences.

## Data sources & attribution

GovEye uses data from UK Parliament APIs (Members, Commons Votes, Lords Votes, Bills, Committees, Interests, Hansard), IPSA (expenses), and GOV.UK (publications). This data is under the Open Parliament Licence v3.0 and requires attribution:

> Contains Parliamentary information licensed under the Open Parliament
> Licence v3.0.

- Source: https://www.parliament.uk / https://members-api.parliament.uk/
- The Open Parliament Licence is available at
  https://www.parliament.uk/site-information/copyright/open-parliament-licence/

See [NOTICE.md](NOTICE.md) for full attribution details.

## License

GPL-3.0 — see [LICENSE](LICENSE).
