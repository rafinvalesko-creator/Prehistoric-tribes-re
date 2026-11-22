# Племя: Каменный век — native Android remake

A clean-room native Android implementation inspired by the core simulation loop of the supplied Prehistoric Tribes game. This project does not embed the original JAR, decompiled code, proprietary graphics, music, or other original assets.

## First playable slice
- Landscape Android app with direct touch.
- Small living settlement with villagers, needs, autonomous idle behavior and task assignment.
- Gather food, wood and stone; fish and hunt.
- Place buildings with resource costs.
- Day/night cycle, simple raid events, and a starter objective tracker.
- Custom Canvas renderer and touch hit-testing; no emulator or virtual cursor.

## Build
Open this directory in Android Studio or run `gradle assembleDebug` with Android SDK 35 installed.
