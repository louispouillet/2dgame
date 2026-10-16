# GDD Brawl Stars-like

## Concept

A top-down multiplayer arena shooter where two teams fight until one is wiped out. It is a proof of concept inspired by Brawl Stars, written in Java with libGDX: the game logic is a 2D plane, displayed in 3D with animated characters, like Brawl Stars.

**Design pillars**

- **Short, readable matches**: small arenas, teams of 1 to 3 players, rounds of a minute or two, rules understood in seconds.
- **Aim and dodge**: move with the keyboard, aim with the mouse, positioning matters more than reflexes.
- **Distinct brawlers**: each character's weapon changes how you play (close-range spread, long-range burst).

## Scope

| In the POC | Out of scope |
| --- | --- |
| Several arenas loaded from text files, chosen in the lobby | Map editor |
| Two teams, from 1v1 up to 3v3 | Matchmaking, accounts, rankings |
| A lobby: nickname, team, brawler, map, leader starts the match | Online play over the Internet |
| Two brawlers: Shelly and Colt, as animated 3D characters | Supers, gadgets, other brawlers |
| Shooting, ammo, reload, health, regeneration | Power cubes, gems, progression |
| Walls, water, bushes | Destructible walls |
| "Knockout" mode: rounds won by eliminating the enemy team | Other game modes |
| Solo practice against a static target | Bots with AI |
| Local network play | |

## Rules

1. **Nickname**: each player enters a nickname (1 to 16 letters, digits, `_` or `-`, unique in the lobby) and joins a server, or starts a solo game.
2. **Lobby**: players join the team with fewer players by default and can switch (3 players max per team), and pick their brawler. The first player is the leader: only they choose the map and start the match, once each team has between 1 and 3 players. If the leader leaves, the next player becomes leader.
3. **Spawn**: at the start of each round, the n-th player of a team appears on the n-th spawn point of their team, with full health and ammo.
4. **Fight**: players move, hide in bushes and shoot the enemy team.
5. **Elimination**: a brawler reaching 0 HP is removed from the arena until the next round.
6. **Round end**: when a whole team is eliminated, the other team wins the round. If the last brawlers of both teams die on the same tick, the round is a draw and is replayed.
7. **Match end**: the first team to win 2 rounds wins the match; the end screen shows the result, then everyone goes back to the lobby.

Additional rules:

- **Ammo**: each brawler has 3 charges; one charge refills over time while the reserve is not full.
- **Regeneration**: a brawler who neither shoots nor takes damage for 3 s regains 20 % of their max HP per second.
- **No friendly fire**: shots go through allies without hurting them.
- **Bushes**: a brawler standing in a bush is invisible to enemies more than 2 tiles away. Allies always see each other, and projectiles stay visible. Shooting or being hit reveals the brawler for a moment.
- **Leaving**: a player who disconnects is removed from the match; if everyone leaves, the server goes back to the lobby.

## Controls

| Action | Input |
| --- | --- |
| Move | W A S D (Z Q S D on AZERTY keyboards) |
| Aim | Mouse position |
| Shoot | Left click |
| Validate the nickname | Enter |

## Brawlers

| Brawler | Role | HP | Speed (tiles/s) | Weapon | Range (tiles) | Damage per attack | Cooldown (s) | Reload (s/charge) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Shelly | Close range, spread | 120 | 3.5 | Shotgun: 5 pellets fired at once in a 28° cone | 4 | 5 × 15 | 0.6 | 1.5 |
| Colt | Long range, burst | 100 | 3.0 | Two revolvers: a burst of 6 bullets over 0.5 s, alternating right and left gun | 8 | 6 × 10 | 1.0 | 2.0 |

Starting values, to be balanced through playtesting. Landing all of Shelly's pellets up close deals 75 damage; a full Colt burst deals 60.

**Characters and animations**

Each brawler is an animated 3D model. The legs and the upper body are animated separately, so a brawler can run and shoot at the same time:

- **Legs**: idle or running, depending on movement.
- **Upper body**: idle or running pose; when shooting, a shooting animation plays (Colt alternates his right and left revolver, one animation per bullet), then the brawler keeps aiming for a moment before lowering his arms.
- **Orientation**: the brawler faces where he is going; during an attack, he faces his target.

## Combat

- Every attack consumes one ammo charge. A new attack is possible once the weapon's cooldown is over.
- The shape of an attack is described by an **attack pattern**: Shelly's shotgun fires all its pellets at once in a fan; Colt's revolvers fire bullets one after another, each from the gun on the right or on the left of the aim. Delayed bullets leave from where the brawler is at that moment.
- Projectiles travel in a straight line at a fixed speed (10 tiles/s) and disappear when they hit an enemy brawler, a wall, or reach their maximum range. They fly over water and through bushes.
- A hit is detected along the projectile's path (swept collision), so a fast projectile cannot pass through a brawler between two ticks.
- Damage is applied by the server on the tick of the impact.

## Arenas

An arena is a grid of tiles described in a text file in `assets/maps/`. Adding a file adds a map to the lobby list, without recompiling. Both the server and the client read the same file.

| Symbol | Tile | Blocks movement | Blocks shots | Effect |
| --- | --- | --- | --- | --- |
| `.` | Floor | No | No | None |
| `#` | Wall | Yes | Yes | Cover against shots |
| `W` | Water | Yes | No | Obstacle you can shoot over |
| `~` | Bush | No | No | Hides the brawler from distant enemies |
| `1`–`3` | Blue spawn point | No | No | Starting position of the n-th blue player (left side) |
| `4`–`6` | Red spawn point | No | No | Starting position of the n-th red player (right side) |

Example arena (`arena1`, 16 × 9 tiles), mirrored for fairness:

```
################
#1.....~~.....4#
#..##......##..#
#..#...WW...#..#
#~~2...##...5~~#
#..#...WW...#..#
#..##......##..#
#3.....~~.....6#
################
```

When loading, the game checks that the border is closed by walls, that only known symbols are used, that spawn points go from 1 to 6 without duplicates, and that each team has 3 spawn points. Otherwise it reports a clear error with the line and column.

## User interface

| Screen | Content |
| --- | --- |
| Nickname | Nickname, server address, "Join" and "Solo" buttons, error messages (name taken, lobby full…) |
| Lobby | Blue and red teams (players, leader and "you" marks), team buttons, brawler choice, map choice and "Start" button for the leader |
| Match | 3D arena, animated brawlers and HUD |
| End of match | Winning team, score of the rounds, back to the lobby |

**In-match HUD**

- Above each brawler: nickname, health bar with the exact HP, and the ammo bar (one segment per charge, filling up while reloading).
- Bushes close to your brawler are drawn see-through, so you can see who hides in them.
- Bullets are drawn with their brawler's image: blue for yours and your allies', red for enemies' (Colt for now; other brawlers' shots are white spheres).
- Top of the screen: round score and remaining players in each team.

## Technical overview

- **Engine**: libGDX (desktop via LWJGL3). The game logic is a top-down 2D plane, rendered in 3D with a perspective camera that follows the player. Brawlers are glTF models (loaded with gdx-gltf) with skeletal animations made in Blender.
- **Game model**: plain Java with no libGDX dependency, so the same code runs on the server, on the client and in tests.
- **Authoritative server**: clients only send their inputs (movement, aim, shoot) and their lobby requests; the server checks everything, simulates the match at a fixed 30 ticks per second and sends each player a snapshot of what they are allowed to see (enemies hidden in bushes are filtered out). Snapshots also carry what the client needs to animate each brawler: where it faces, whether it moves, and a shot counter.
- **Smooth client**: the client interpolates the other brawlers between snapshots, and predicts its own movement immediately before correcting it with the server's answer (reconciliation).
- **Networking**: KryoNet (TCP for the lobby, UDP for inputs and snapshots). Solo games run the same server in memory.
- **OOP**: a class hierarchy for game objects (`GameObject` → `Brawler` → `Shelly` / `Colt`, `GameObject` → `Projectile`), composition (a `Brawler` has a `Weapon`), the Strategy pattern for attack shapes (`AttackPattern` → `SpreadPattern` / `BurstPattern`), and interfaces for interchangeable sessions, network channels and brawler avatars. UML class diagrams are in `docs/uml/`.
- **Quality**: JUnit tests on the model, the server, the lobby, the network layer and the animation logic, with JaCoCo coverage, and Javadoc on all public classes and methods.
