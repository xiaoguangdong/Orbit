# Orbit / 星筹

Orbit is now organized as a multi-platform monorepo for the Orbit habit app, whose Chinese product name is `星筹`.

The repository is structured to support:

- `Android` delivery today
- future `iOS` and `HarmonyOS` native apps
- a backend API and async workers
- shared contracts, product docs, and design decisions

## Workspace Layout

```text
Orbit/
  apps/
    android/      # Kotlin + Jetpack Compose app
    ios/          # SwiftUI app placeholder
    harmony/      # HarmonyOS / ArkUI app placeholder
  services/
    api/          # Backend API placeholder
  packages/
    contracts/    # Shared API contracts and schemas
  docs/
    product/      # Product design and requirements
```

## Current Status

- `apps/android` contains the working Android MVP.
- `docs/product` contains the current product design and requirements documents.
- `apps/ios`, `apps/harmony`, `services/api`, and `packages/contracts` are scaffolded as the next implementation targets.

## Build Android

From the Android app root:

```bash
cd apps/android
./gradlew :app:assembleDebug
```

If local Java or SDK discovery needs overrides on this machine, use the same environment variables already validated for the Android build.

## Key Docs

- [Product Design Document](docs/product/orbit-product-design.md)
- [Requirements Document](docs/product/orbit-requirements.md)
- [Monorepo Structure Note](docs/engineering/monorepo-structure.md)

## Reference Material

The `images/` directory contains reference screenshots used during early product definition.
