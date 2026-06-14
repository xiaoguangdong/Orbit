# Orbit Monorepo Structure

This repository uses a single monorepo so product, clients, contracts, and backend can evolve together without splitting decisions across multiple repositories too early.

## Intent

- Keep the working Android app intact while preparing for iOS, HarmonyOS, and backend development.
- Share contracts and product rules before attempting shared UI code.
- Preserve native delivery per platform instead of forcing an early cross-platform UI stack.

## Boundaries

- `apps/android`
  - Production Android client.
  - Owns Android build files, Compose UI, Room, notifications, and packaging.
- `apps/ios`
  - Future Swift / SwiftUI client.
  - Should consume the same backend contracts, but keep native UI and platform services.
- `apps/harmony`
  - Future HarmonyOS / ArkUI client.
  - Treated as a first-class native target instead of an Android compatibility layer.
- `services/api`
  - Source of truth for accounts, sync, tasks, habits, milestones, and analytics events.
- `packages/contracts`
  - Shared schemas, API definitions, and payload examples used by clients and backend.
- `docs/product`
  - Product design and requirements that define behavior before platform-specific implementation details diverge.

## Recommended Implementation Order

1. Stabilize Android features and backend contracts.
2. Stand up the backend API around those contracts.
3. Start iOS and Harmony clients against the same contract layer.
4. Introduce deeper code sharing only where it reduces cost without weakening native UX.

## Deliberate Non-Goals

- No shared UI framework across Android, iOS, and HarmonyOS at this stage.
- No Kotlin Multiplatform-first architecture for the whole repo.
- No backend implementation mixed into the Android app tree.
