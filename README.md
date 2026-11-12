# 2DGame

A simplified Brawl Stars clone: a top-down 2D game shown in 3D, with online multiplayer for 2 to 6 players in two teams. Written in Java with [libGDX](https://libgdx.com/) as an object-oriented programming project.

> **Disclaimer:** this is an unofficial fan project made for a school assignment at Epitech, with no commercial purpose. It is not affiliated with, endorsed, sponsored or approved by Supercell. Brawl Stars, its characters and its 3D models are trademarks and property of Supercell.

## Game

- **Mode**: Knockout. Two teams (up to 3 against 3) fight in rounds, the first team to win 2 rounds wins the match. A round is won when the other team has no brawler left alive.
- **Brawlers**: Colt (revolver, fires in bursts) and Shelly (shotgun). Each player picks one in the lobby.
- **Arenas**: several maps, chosen by the leader of the lobby. Walls block everything, water blocks walking only, bushes hide a brawler from the enemies who are more than 2 tiles away.
- **Flow**: nickname screen, lobby (team, brawler, map), match, then back to the lobby.

### Controls

| Action | Input |
|---|---|
| Move | `W` `A` `S` `D` |
| Aim | mouse |
| Shoot | left mouse button |

## Requirements

- Java 21 (Gradle downloads one if needed when you run `make solo`)
- `make`

## Running

```
make solo                   # solo game against a still target
make server                 # multiplayer server (TCP 54555 / UDP 54777)
make client [HOST=address]  # client connected to a server (localhost by default)
make duo                    # server and two clients on this computer
make test                   # runs the tests
make doc                   # generates the Javadoc (core/build/docs/javadoc)
make build                  # builds the runnable jar (lwjgl3/build/libs)
make clean                  # deletes the build files
```

The jar built by `make build` (`lwjgl3/build/libs/2DGame-1.0.0.jar`) is the same on Linux and macOS: send it to the players, who double-click it (or run `java -jar 2DGame-1.0.0.jar`). They need Java 21, and they type the server address on the nickname screen.

To play over the network, start `make server` on one computer, then `make client HOST=<server address>` on each player's computer. Every client starts on the nickname screen, where the server address can also be typed.

## How it works

The **server is the authority**: it owns the real world (positions, health, collisions) and runs it at a fixed 30 Hz tick. Each client sends its inputs, predicts its own brawler to feel instant, and draws the other players interpolated between the snapshots it receives. The server sends one snapshot per player and leaves out the enemies hidden in bushes, so a client never learns where they are.

The code follows object-oriented design:

- **Inheritance**: `GameObject`, then `Brawler`, then `Shelly` and `Colt`; `Projectile` also extends `GameObject`.
- **Strategy and composition**: a `Brawler` has a `Weapon` (`Shotgun`, `Revolver`).
- **Polymorphism**: `GameWorld` updates every `GameObject` without `instanceof`; `GameSession` (`LocalSession`, `RemoteSession`), the channels (`LocalChannel`, KryoNet) and the screens are interchangeable.
- **Encapsulation**: state is private or protected and changed through methods.

### Project layout

| Path | Content |
|---|---|
| `core/` | game logic, server and libGDX client (package `io.github.game2d`) |
| `lwjgl3/` | desktop launcher |
| `assets/` | maps (`maps/`), 3D models (`models/`), textures, fonts |
| `assets-src/` | Blender sources of the models |
| `docs/` | game design document (`GDD.md`) and UML diagrams (`uml/`) |

Packages of `core`: `model` (rules, plain Java), `protocol` (messages), `time` (fixed tick clock), `server` (authority and lobby), `net` (transport, KryoNet in `net.kryo`) and `client` (libGDX: screens, 3D view, prediction). Only `client` depends on libGDX and only `net.kryo` depends on KryoNet.

## Adding content

- **A map**: drop a text file in `assets/maps/`. `#` wall, `.` floor, `~` bush, `W` water, digits `1` to `6` spawn points (`1` to `3` for the blue team on the left, `4` to `6` for the red team on the right). The border must be walls, and each team needs 3 spawn points.
- **Bullets of a brawler**: drop `assets/textures/<type>_bullet_blue.png` and `<type>_bullet_red.png` (type in lowercase, bullet head towards the right). Without both images, the brawler shoots spheres.
- **A brawler model**: a glTF file in `assets/models/` (see `assets-src/ANIMATION.md` for the animation clips).

## Documentation

- [Game design document](docs/GDD.md)
- Javadoc: `make doc`, then open `core/build/docs/javadoc/index.html`
- UML diagrams in [`docs/uml`](docs/uml): `overview`, `model`, `server`, `net`, `protocol`, `client-screens`, `client-game`

## Gradle

The Gradle wrapper is included (`./gradlew`). Useful tasks: `lwjgl3:run` (starts the game), `lwjgl3:jar` (runnable jar), `core:test` (tests), `core:javadoc` (documentation), `clean`, `idea`, `eclipse`. Tasks that are not specific to one project can be run with a `name:` prefix, for example `core:clean`.
