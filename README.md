# GovEye

> **FotMob for UK politics** — track MPs, votes, divisions, and bills.

A free, open-source Android app that makes UK Parliament as followable as a
football league. GovEye surfaces MP activity, Commons divisions, and bill
progress in a fast, neutral, mobile-first interface.

## Status

**Early development — not yet released.** No APKs are published yet. The app
is being built in the open; expect breaking changes until v1.0.

## Tech stack

- **Android Gradle Plugin** 9.3.0 / **Gradle** 9.7.0
- **Kotlin** 2.4.10
- **Jetpack Compose** + **Material 3**
- **Navigation 3**
- **Hilt** (dependency injection)
- **Room** (local persistence with FTS)
- **Retrofit / OkHttp** (networking)
- **DataStore** (preferences)

## Build

Requirements: **JDK 17+** and Android Studio (latest canary/beta for AGP 9
support).

```bash
./gradlew assembleDebug
```

## CI

Continuous integration runs on every pull request and push to `main`:

```bash
./gradlew spotlessCheck testDebugUnitTest assembleDebug lintDebug
```

Format code before pushing:

```bash
./gradlew spotlessApply
```

## Data sources & attribution

GovEye uses data from the UK Parliament APIs (Members API, Commons Votes API).
This data is under the Open Parliament Licence v3.0 and requires attribution:

> Contains Parliamentary information licensed under the Open Parliament
> Licence v3.0.

- Source: https://www.parliament.uk / https://members-api.parliament.uk/
- The Open Parliament Licence is available at
  https://www.parliament.uk/site-information/copyright/open-parliament-licence/

See [NOTICE.md](NOTICE.md) for full attribution details.

## License

GPL-3.0 — see [LICENSE](LICENSE).
