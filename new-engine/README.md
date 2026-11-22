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

## Current simulation systems

- Four-season cycle with increased winter food consumption.
- Hunger lowers morale and gradually harms villagers.
- Population growth is limited by available huts and food reserves.
- The builder automatically repairs damaged buildings.
- Resource gathering shifts toward food when supplies are low.
- The HUD displays the current season and housing capacity.

These systems are part of the clean-room prototype and are still being balanced.

- Construction rules now include per-building food, wood, and stone costs, free-space checks, and unfinished building health.
- Guard recruitment converts an available gatherer into a guard and consumes food and hides, with a cap on guard numbers.

- Campaign milestones track gathering supplies, expanding housing, repelling the first raid, and unlocking core technologies.
- Technology progression unlocks improved tool use (faster gathering) and expanded storage capacity.

- Game state is serialized locally and restored automatically on launch; progress is autosaved during play and when the game surface closes.
- Touchscreen action panel supports huts, granaries, woodcamps, quarries, hunter camps, watchtowers, bonfires, palisades and guard recruitment; actions validate costs and save immediately.


### Оборона поселения
- Стражники автоматически преследуют ближайших налётчиков в радиусе обнаружения и возвращаются к поселению, если врагов рядом нет.
- Налётчики выбирают ближайший целый участок частокола, идут к нему и атакуют его; после разрушения переходят к следующему участку, а затем к постройкам.
- Повреждённые участки частокола отображают шкалу прочности и могут быть восстановлены строителем.
