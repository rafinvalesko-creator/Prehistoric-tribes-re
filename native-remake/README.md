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

## Gameplay study notes

The original is a small real-time tribe/village simulation rather than a conventional unit-selection RTS. Its defining loop is:
1. Keep a small population alive and productive.
2. Assign individual villagers to gathering food, wood and stone, fishing, hunting, cooking and construction.
3. Balance work with hunger, sleep, leisure and social behaviour; villagers can act autonomously when not assigned.
4. Expand a settlement with 13 building categories (reported in the original release descriptions), including storage, chief's hut, kitchen, gym/training and related utility buildings.
5. Prepare for hostile tribes and wildlife; missions include defence and attacks.

Research references used to identify the original gameplay:
- Pocket Gamer, 26 Jan 2008: https://www.pocketgamer.com/prehistoric-tribes/thq-wireless-goes-back-to-basics-with-prehistoric-tribes/
- Softpedia, 27 Jan 2008: https://news.softpedia.com/news/Prehistoric-Tribes-Clubbing-On-Your-Mobile-77284.shtml
- Phoneky game feature summary: https://mobile.phoneky.com/games/?id=j4j91492
- Captured gameplay video: https://www.youtube.com/watch?v=kt05kFeeDFI

## Development roadmap

- [x] Native Android shell, landscape layout and direct touch.
- [x] Initial village, individual villagers, resource nodes, task assignment, basic gathering and construction.
- [x] Save/load of the core village state.
- [ ] Complete villager needs and autonomous routines (food, sleep, social/leisure, relationships and family growth).
- [ ] Make every building type have a real gameplay function and progression requirements.
- [ ] Proper pathfinding, map expansion, exploration and resource regeneration.
- [ ] Wildlife AI, enemy tribes, raids, melee/ranged combat and defence.
- [ ] Mission/campaign framework, tutorial and win/lose conditions.
- [ ] Original art/audio commissioned or drawn for this remake, animation polish and device testing.
