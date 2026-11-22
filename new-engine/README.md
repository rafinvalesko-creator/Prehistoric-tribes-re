# Tribal Age — clean-room Android engine

This is a separate, from-scratch Android project. It does not compile, package, decompile, link, or load the original game's JAR, classes, source code, or artwork.

## Current milestone

- Native Android Kotlin/Canvas renderer.
- Landscape immersive rendering with a 960×432 logical viewport, scaling to phone screens including POCO X7 Pro.
- Original vector-drawn terrain, settlement props and villagers.
- A small autonomous simulation loop for food, wood and population.
- Touchscreen controls for selecting units, moving groups, and issuing combat/defensive orders.

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


Combat behavior:
- Injured guards below 35 health retreat to the great hall and recover gradually before returning to patrol.


Resource exhaustion:
- Gatherers safely idle near the settlement if all resource nodes have been depleted; the simulation no longer indexes an invalid map tile.


## Дополнительная надёжность и управление

- Персонажи обходят водные клетки: при встрече с рекой движение пытается скользить вдоль берега, а не проходить сквозь воду.
- Если сохранение повреждено или не читается, игра восстанавливает новую стартовую партию вместо частично загруженного мира.
- Обновление симуляции, отрисовка, касания и автосохранение синхронизированы, чтобы избежать одновременного изменения списков юнитов и построек.
- Пауза включается и выключается касанием небольшой панели «АВТОНОМНАЯ СИМУЛЯЦИЯ» над списком строительства. Состояние паузы сохраняется.

- Defeat is triggered if the Great Hall is destroyed or the population reaches zero; touching the screen starts a fresh settlement.


Combat update:
- Watchtowers automatically target the nearest living raider within 6.5 map units and deal ranged damage every combat volley. Defeated raiders are removed immediately and their reward is granted once.

## Управление отрядами касанием

- Коротко коснитесь жителя, чтобы выбрать одного; удерживайте палец на жителе полсекунды, чтобы выбрать всех жителей той же роли.
- Выбранные персонажи отмечаются светлыми кольцами, сведения о группе отображаются в верхней панели.
- Коснитесь свободной суши, чтобы переместить выбранного персонажа или группу; группа строится в компактный строй.
- Удерживайте палец на свободной суше, чтобы приказать группе занять позицию и удерживать оборону.
- Коснитесь налётчика выбранной стражей, чтобы отдать приказ атаковать именно эту цель; стража старается не переключаться на другого врага.
- Если выбранная цель погибнет, стражники автоматически выберут ближайшего доступного противника.
- Стражники на оборонительной позиции поддерживают бой на расстоянии; при критическом ранении они отходят к главному залу.
- Приказ на обычное перемещение действует до 18 секунд; затем юниты возвращаются к обычным обязанностям.
- Прямые боевые приказы доступны только стражникам; башни продолжают автоматически поддерживать оборону.
- Состояние оборонительной позиции и текущие приказы сохраняются вместе с игрой.
