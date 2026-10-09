# GDD Brawl Stars-like

## Concept

A top-down multiplayer arena shooter: the last brawler standing wins. It is a proof of concept inspired by Brawl Stars, written in Java with libGDX.

**Design pillars**

- **Short, readable matches**: one arena, one round of about 2 minutes, rules understood in seconds.
- **Aim and dodge**: move with the keyboard, aim with the mouse, positioning matters more than reflexes.
- **Distinct brawlers**: each character's weapon changes how you play (close range, long range, gadgets etc..).

## Scope

| In the POC | Out of scope |
| --- | --- |
| One arena loaded from a file | Map editor, multiple game modes |
| Two brawlers: Shelly and Colt | Supers, gadgets |
| Shooting, ammo, reload, health | Power cubes, gems, progression |
| Walls, bushes | Destructible walls |
| Last-man-standing | Matchmaking, accounts, rankings |
| Local network play | Online play over the Internet |

## Rules

1. **Lobby**: each player picks a brawler, then the host starts the match.
2. **Spawn**: each player appears on a spawn point with full health and ammo.
3. **Fight**: players move, hide in bushes and shoot each other.
4. **Elimination**: a brawler reaching 0 HP is removed from the arena and becomes a spectator.
5. **End**: when only one brawler is left, the match ends and the winner is shown.

Additional rules:

- **Ammo**: each brawler has some charges, one charge refills over time while the reserve is not full.
- **Regeneration**: a brawler who neither shoots nor takes damage for 3 s slowly regains health.
- **No friendly fire**: it is a free-for-all: a brawler cannot hit itself.
- **Draw**: if the last brawlers die on the same tick, the match is a draw.

## Controls

| Action | Input |
| --- | --- |
| Move | WASD (ZQSD on AZERTY keyboards) |
| Aim | Mouse position |
| Shoot | Left click |
| Pause menu | Escape |

## Brawlers

| Brawler | Role | HP | Speed (tiles/s) | Weapon | Range (tiles) | Damage per attack | Reload (s/charge) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Shelly | Close range, burst | 3600 | 3.0 | Shotgun: 5 pellets in a 30° cone | 4.5 | 5 × 400 | 1.5 |
| Colt | Long range, precision | 3000 | 3.0 | Revolver: 6 bullets in a line | 8 | 6 × 360 | 1.2 |

Starting values, to be balanced through playtesting. A single Shelly pellet deals 400 damage (Colt goes from 3000 to 2600 HP); landing all 5 pellets up close deals 2000.

## Combat

- Every attack consumes one ammo charge, with a minimum of 0.5 s between attacks.
- Projectiles travel in a straight line at a fixed speed and disappear when they hit a brawler, a wall, or reach their maximum range.
- A hit is detected when the projectile's hitbox overlaps a brawler's hitbox.
- Damage is applied by the server on the tick of the impact.

## Arena

The arena is a grid of tiles described in a text file (`assets/maps/arena1.txt`). It can be edited without recompiling, and both the server and the client read the same file.

| Symbol | Tile | Blocks movement | Blocks shots | Effect |
| --- | --- | --- | --- | --- |
| `.` | Floor | No | No | None |
| `#` | Wall | Yes | Yes | Cover against shots |
| `~` | Bush | No | No | Hides the brawler from distant enemies |
| `1`–`4` | Spawn point | No | No | Starting position of player N |

Example arena (15 × 7 tiles, symmetric for fairness):

```
###############
#1....~~~....2#
#..##.....##..#
#.....###.....#
#..##.....##..#
#3....~~~....4#
###############
```

When loading, the game checks that every row has the same length, that the border is closed by walls, and that there are at least 2 spawn points, otherwise it reports a clear error.

## User interface

| Screen | Content |
| --- | --- |
| Main menu | Host a match, join a match (IP address), quit |
| Lobby | Connected players, brawler selection, "Start" button for the host |
| Match | Arena, brawlers and HUD |
| End screen | Winner, elimination order, back to lobby |

**In-match HUD**

- Above each brawler: name and health bar (green for yourself, red for enemies).
- Under your own brawler: the 3 ammo charges, filling up while reloading.
- Top of the screen: number of players left and match timer.
- Bottom-left corner: brawler portrait and exact HP (e.g. 2600 / 3000).

## Technical overview

- **Engine**: libGDX (desktop via LWJGL3). The game logic is a top-down 2D plane, rendered with a Brawl Stars-style 3D view (perspective camera following the player).
- **Game model**: plain Java with no libGDX dependency, so the same code runs on the server, on the client and in tests.
- **Authoritative server**: clients only send their inputs (movement, aim, shoot); the server simulates the match at a fixed 30 ticks per second and broadcasts the world state to every client.
- **OOP**: a class hierarchy for game objects (`GameObject` → `Entity` → `Brawler` → `Shelly` / `Colt`), and design patterns such as Strategy for weapons. Detailed UML diagrams will be produced from this document.
- **Quality**: JUnit tests on the game model with JaCoCo coverage, and Javadoc on all public classes and methods.

