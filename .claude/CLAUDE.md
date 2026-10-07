# Contexte du projet : POC « Brawl Stars » en Java

## Objectif
POC d'un clone simplifié de Brawl Stars : Java, gameplay 2D (vue 2D ou 3D), multijoueur. Le projet doit respecter les concepts de la POO (contrainte imposée).

Projet libGDX généré avec gdx-liftoff (modules `core` et `lwjgl3`, Gradle multi-modules).

## 1. Faisabilité
Faisable en visant un périmètre réduit :
- **Moteur** : libGDX (temps réel, portage Android possible plus tard). JavaFX/Swing plus simples mais moins adaptés.
- **Multijoueur** : serveur autoritaire, KryoNet (TCP + UDP), tick fixe 30 Hz, interpolation côté client, prédiction éventuelle.
- **Périmètre** : une arène, 2 à 4 joueurs, un tir simple avec points de vie, mode « dernier survivant », pas de matchmaking ni de comptes.

## 2. Architecture POO
- Hiérarchie : `GameObject` (position, rayon) → `Brawler` → `Shelly` / `Colt` ; `GameObject` → `Projectile`.
- `Weapon` (classe abstraite : munitions, cadence, rechargement) → `Shotgun`, `Revolver` : pattern Strategy, un `Brawler` possède une `Weapon`.
- `GameView` (interface) implémentée par `WorldRenderer` (2D) et `WorldRenderer3D` (3D), interchangeables avec la touche `V`.
- `ClientChannel` / `ServerChannel` : transport en mémoire (`LocalChannel`) ou réseau (`net.kryo`).

Principes illustrés :
- **Héritage** : `GameObject` → `Brawler` → `Shelly` / `Colt`
- **Abstraction** : classes abstraites (`GameObject`, `Brawler`, `Weapon`) et interfaces (`GameView`, canaux), créées seulement quand il y a plusieurs implémentations
- **Polymorphisme** : `GameWorld` appelle `update(dt)` sur tous les `GameObject` ; vues et canaux interchangeables
- **Composition / Strategy** : `Brawler` a une `Weapon`
- **Encapsulation** : attributs privés ou protégés, modifiés via méthodes (ex. position uniquement dans `Brawler.move`)

## 3. Source de vérité et rendu
- Le **serveur** (`GameServer`) est la source de vérité : il possède le vrai `GameWorld` (positions, PV, collisions).
- Le **client** dessine une copie de l'état reçu (`WorldSnapshot`), interpolée par `SnapshotBuffer`.
- Cycle 30 Hz : inputs envoyés → simulation serveur → snapshot diffusé → mise à jour locale → rendu à 60 FPS.

## 4. Collisions
- Toutes les collisions sont calculées côté serveur, dans `model`.
- Chaque objet a un rayon (`GameObject.getRadius()`).
- **Décor** : `Arena` (grille de cases) : `blocksMovement` / `blocksShots` testent un cercle contre les cases voisines (`Collisions.circleTouchesBox`). Mur : bloque tout ; eau : bloque la marche ; buisson : rien ; hors map = mur.
- **Brawlers** : `Brawler.move(dt, arena)` avance en x puis en y (glissement le long des murs), en réduisant le pas jusqu'à 5 fois pour finir collé au mur.
- **Tirs** : `GameWorld` arrête un projectile dans un mur ; `Collisions.sweptOverlaps` détecte un brawler touché le long du trajet du tir.
- Les tests utilisent une vraie arène (`TestArenas.open`, dans `src/test`).

## 5. Module `core` : packages
Racine `io.github.game2d`. Dépendances : `model` ne dépend de rien ; `protocol` ne dépend de rien ; `server` → `model`, `protocol`, `net` ; `net` → `protocol`, `server` ; `client` → tous. Seul `client` dépend de libGDX ; seul `net.kryo` dépend de KryoNet. Les tests suivent la même arborescence (`core/src/test`).

- `model/` : règles du jeu, Java pur (`GameObject`, `Projectile`, `GameWorld`)
  - `model.arena` : `Arena`, `ArenaLoader`, `TileType`, `SpawnPoint`, `ArenaFormatException`
  - `model.brawler` : `Brawler`, `Shelly`, `Colt`
  - `model.weapon` : `Weapon`, `Shotgun`, `Revolver`
  - `model.collision` : `Collisions`
- `protocol/` : messages échangés client/serveur (`PlayerInput`, `JoinAccepted`, `WorldSnapshot`, `BrawlerState`, `ProjectileState`)
- `server/` : autorité de jeu (`GameServer`, `FixedTimestep`, `ServerLauncher`) et traduction protocole ↔ modèle (`InputApplier`, `SnapshotBuilder`)
- `net/` : transport. Interfaces `ClientChannel` / `ServerChannel`, `LocalChannel` (en mémoire)
  - `net.kryo` : `NetworkServer`, `NetworkClient` (cycle de vie KryoNet), `NetworkClientChannel`, `NetworkServerChannel` (traducteurs de protocole), `NetworkRegistry`
- `client/` : libGDX. `Main`, `InputReader`, `SnapshotBuffer` ; vues `GameView`, `WorldRenderer` (2D), `WorldRenderer3D` + `ArenaModelFactory` (3D, textures dans `assets/textures/arena`) ; HUD `BrawlerHud`, `HudAnchor`

## 6. Lancement (Makefile)
- `make solo` : partie locale ; `make server` : serveur multijoueur (TCP 54555 / UDP 54777) ; `make client [HOST=ip]` : client ; `make duo` : serveur + 2 fenêtres sur la même machine.
- `make test`, `make build`, `make clean`.
- Map chargée : `assets/maps/arena2.txt` (30x20), dans `client/Main` et `server/ServerLauncher` (garder les deux synchronisées).

## Reste à faire
- Mode « dernier survivant » (gagnant, fin de partie)
- Buissons qui cachent les brawlers (`TileType.hidesBrawler`), filtrés côté serveur
- Caméra 3D : élargir la bande de décor pour que le joueur ne soit pas collé au coin au départ
- Chemin de la map partagé entre client et serveur
- `StartupHelper` ne transmet pas les arguments sur macOS (le Makefile passe `-XstartOnFirstThread`)
- Soutenance : Javadoc, couverture JaCoCo, diagrammes UML, mise à jour du GDD (`docs/GDD.md`)
- 3D : type et visée dans `BrawlerState`, puis gdx-gltf, personnages animés, ombres

## Structure du projet généré
- `core/` : logique de jeu, serveur et client libGDX (voir section 5)
- `lwjgl3/` : lanceur desktop (`Lwjgl3Launcher.java`, fenêtre 1280x720, vsync, `StartupHelper`)
- `assets/` : ressources partagées entre plateformes (maps, textures, polices)

## Règles
- Ne jamais créer de commit sans demande explicite de l'utilisateur.
