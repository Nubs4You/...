# Development Plan

## 1) Project Bootstrap
- Initialize a Fabric API 1.21.11-compatible mod layout.
- Configure Gradle, Fabric Loom, Fabric API, Yarn mappings, and Java toolchain.
- Add Lombok dependencies for getters/setters/constructors.

## 2) Core Mod Architecture
- Create a main mod class with a static singleton instance.
- Register command and tick handlers during mod initialization.
- Add central manager classes for bot state and build execution queues.

## 3) Bot Spawn + Command Flow
- Add `/buildbot <prompt>` command.
- Spawn a visible bot marker entity near the player when command runs.
- Validate command input and provide user-facing error handling.

## 4) LLM Build Planning Integration
- Add HTTP client service to call a configurable LLM endpoint.
- Request a normalized JSON plan format from the model.
- Parse and validate plan instructions with null safety and fallback behavior.

## 5) Build Execution Engine
- Convert plan instructions into world block placements.
- Execute placements over server ticks to avoid blocking.
- Keep bot metadata and task queues organized per server world.

## 6) Reliability + Developer Experience
- Add config defaults and robust logging around planning/build errors.
- Ensure consistent naming conventions (`this`, `final`, explicit variable names).
- Keep classes modular and ready for future pathfinding/AI expansion.

## 7) Verification + Delivery
- Run available Gradle checks/build tasks.
- Fix any compile/lint issues.
- Commit all changes and prepare PR notes.
