# Tribal Age — clean-room Android engine

This is a separate, from-scratch Android project. It does not compile, package, decompile, link, or load the original game's JAR, classes, source code, or artwork.

## Current milestone

- Native Android Kotlin/Canvas renderer.
- Landscape immersive rendering with a 960×432 logical viewport, scaling to phone screens including POCO X7 Pro.
- Original vector-drawn terrain, settlement props and villagers.
- A small autonomous simulation loop for food, wood and population.
- No touch controls in this milestone, as requested.

This is an engine foundation and visual slice, **not yet a complete recreation**. Exact parity requires a verified reference specification for the original maps, sprites, unit/building roster, mission scripts, combat rules, UI and progression. The implementation will be expanded feature by feature.

## Build

Run `gradle :game:assembleDebug` from this directory with JDK 17 and Android SDK 35.
