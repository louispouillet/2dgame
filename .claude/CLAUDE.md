# Contexte du projet : POC « Brawl Stars » en Java

## Objectif
POC d'un clone simplifié de Brawl Stars : Java, gameplay 2D (vue 2D ou 3D), multijoueur. Le projet doit respecter les concepts de la POO (contrainte imposée).

Projet libGDX généré avec gdx-liftoff (modules `core` et `lwjgl3`, Gradle multi-modules).

## 1. Faisabilité
Faisable en visant un périmètre réduit :
- **Moteur** : libGDX (temps réel, portage Android possible plus tard). JavaFX/Swing plus simples mais moins adaptés.
- **Multijoueur** : serveur autoritaire, KryoNet (TCP + UDP), tick fixe 30 Hz, interpolation côté client, prédiction éventuelle.
- **Périmètre** : plusieurs arènes, 2 à 6 joueurs en deux équipes (jusqu'à 3 contre 3), un tir simple avec points de vie, mode « Hors-jeu » (manches par élimination), un lobby (pseudo, équipe, map), pas de matchmaking ni de comptes.

## 2. Architecture POO
- Hiérarchie : `GameObject` (position, rayon) → `Brawler` → `Shelly` / `Colt` ; `GameObject` → `Projectile`.
- `Weapon` (classe abstraite : munitions, cadence, rechargement) → `Shotgun`, `Revolver` : pattern Strategy, un `Brawler` possède une `Weapon`.
- `GameView` (interface) implémentée par `WorldRenderer` (2D) et `WorldRenderer3D` (3D), interchangeables avec la touche `V`.
- `ClientChannel` / `ServerChannel` : transport en mémoire (`LocalChannel`) ou réseau (`net.kryo`).

Principes illustrés :
- **Héritage** : `GameObject` → `Brawler` → `Shelly` / `Colt`
- **Abstraction** : classes abstraites (`GameObject`, `Brawler`, `Weapon`) et interfaces (`GameView`, canaux), créées seulement quand il y a plusieurs implémentations
- **Polymorphisme** : `GameWorld` appelle `update(dt, arena)` sur tous les `GameObject`, sans `instanceof` (chaque objet gère son déplacement et ses collisions avec le décor) ; vues et canaux interchangeables
- **Composition / Strategy** : `Brawler` a une `Weapon`
- **Encapsulation** : attributs privés ou protégés, modifiés via méthodes (ex. position uniquement dans `Brawler.move`, privée, appelée par `update`)

## 3. Source de vérité et rendu
- Le **serveur** (`GameServer`) est la source de vérité : il possède le vrai `GameWorld` (positions, PV, collisions).
- Deux phases (`GameServer`) : `LOBBY` (pas de monde, pas de snapshot) puis `PLAYING`. Le monde est créé au lancement, sur la map choisie (`GameServer(maps, defaultMap, mapLoader)` : le chargeur est injecté, les tests passent leurs petites maps). Si tout le monde part, retour en `LOBBY`.
- Le serveur envoie **un snapshot par joueur** (`SnapshotBuilder.build(tick, lastProcessed, viewerId)`) : chacun ne reçoit que ce qu'il a le droit de voir.
- Le **client** dessine une copie de l'état reçu (`WorldSnapshot`), interpolée par `SnapshotBuffer`.
- Cycle 30 Hz : inputs envoyés → simulation serveur → snapshot diffusé → mise à jour locale → rendu à 60 FPS.

## 4. Collisions
- Toutes les collisions sont calculées côté serveur, dans `model`.
- Chaque objet a un rayon (`GameObject.getRadius()`).
- **Décor** : `Arena` (grille de cases) : `blocksMovement` / `blocksShots` testent un cercle contre les cases voisines (`Collisions.circleTouchesBox`). Mur : bloque tout ; eau : bloque la marche ; buisson : rien ; hors map = mur.
- **Brawlers** : `Brawler.update(dt, arena)` appelle `move`, qui avance en x puis en y (glissement le long des murs), en réduisant le pas jusqu'à 5 fois pour finir collé au mur.
- **Tirs** : `Projectile.update` s'arrête de lui-même dans un mur ; `GameWorld.resolveProjectileHits` gère les tirs sur les brawlers et `Collisions.sweptOverlaps` détecte un brawler touché le long du trajet du tir.
- Les tests utilisent une vraie arène (`TestArenas.open`, dans `src/test`).

## 5. Buissons
- Un brawler dont le centre est sur un buisson (`~`) est caché aux adversaires à plus de 2 cases (`Arena.REVEAL_DISTANCE`). On se voit toujours soi-même ; les projectiles restent visibles.
- `Arena.hidesBrawlerAt(x, y)` : la case sous le point cache-t-elle ? `GameWorld.isVisibleTo(target, viewer)` : la règle. `SnapshotBuilder` filtre selon cette règle.
- Filtrage côté serveur : un client ne reçoit jamais la position d'un ennemi caché (anti-triche).
- Côté client, les cases buisson dans ce même rayon autour du joueur sont dessinées transparentes (40 %), en 2D et en 3D (`Arena.canSeeIntoBush`). En 3D, chaque buisson est une `ModelInstance` séparée (`ArenaModelFactory.createBush`) pour avoir sa propre opacité.

## 6. Maps et équipes
- Fichiers texte dans `assets/maps/` : `#` mur, `.` sol, `~` buisson, `W` eau, chiffres `1` à `6` = points d'apparition (sur du sol). La première ligne du fichier est le haut de la map.
- Équipes (`model.Team`) : points `1` à `3` = `BLUE` (à gauche), `4` à `6` = `RED` (à droite), en miroir. `SpawnPoint.team()` déduit l'équipe du numéro ; `Arena.getSpawnPoints(team)` renvoie les points d'une équipe triés par numéro.
- `ArenaLoader` vérifie le format (bordure en murs, symboles connus, points de 1 à 6 sans doublon, au moins 2 points). `ArenaLoader.availableMaps()` liste les maps à partir de `assets.txt` (généré à la compilation) : ajouter une map = déposer le fichier.
- Toute map de `assets/maps/` doit avoir 3 points par équipe (vérifié par `ArenaLoaderTest.everyAvailableMapHasThreeSpawnPointsPerTeam`).

## 7. Lobby
- `server.Lobby` : salle d'attente avant la partie, Java pur, sans réseau (utilisée sous le verrou de `GameServer`, donc pas thread-safe).
- Joueurs (`LobbyPlayer` : id, pseudo, équipe) dans l'ordre d'arrivée ; 6 au maximum, 3 par équipe. Un nouveau joueur va dans l'équipe la moins remplie (Bleus en cas d'égalité).
- Pseudo : espaces autour retirés, 1 à 16 caractères parmi lettres (accents compris), chiffres, `_` et `-`, unique sans tenir compte des majuscules.
- Chef = premier de la liste : s'il part, le suivant devient chef. Lui seul choisit la map, parmi la liste donnée au constructeur (`ArenaLoader.availableMaps()`).
- `canStart()` : chaque équipe a entre 1 et 3 joueurs (1v1, 2v1, 3v3… mais pas d'équipe vide).
- Refus : `LobbyException` avec une raison `protocol.RefusalReason` (`INVALID_NAME`, `NAME_TAKEN`, `LOBBY_FULL`, `TEAM_FULL`, `NOT_LEADER`, `UNKNOWN_MAP`, `TEAMS_NOT_READY`, `GAME_IN_PROGRESS`).
- `GameServer` : `join(canal, pseudo)`, `chooseTeam`, `chooseMap`, `requestStart` (chef + `canStart`), `leave`. Après chaque changement, `LobbyState` est envoyé à tous. Au lancement, le n-ième joueur d'une équipe apparaît sur le n-ième point de son équipe ; l'id du joueur est l'id de son brawler.
- Réseau (TCP) : client → serveur `JoinRequest`, `ChooseTeam`, `ChooseMap`, `StartRequest` ; serveur → client `JoinAccepted`, `JoinRejected`, `LobbyState` (joueurs, chef, map, maps, `canStart`, `started`). `NetworkServer` attend un `JoinRequest` avant d'accepter le reste ; les demandes refusées autres que le join sont ignorées.
- Client : `GameSession` porte les commandes (`join`, `chooseTeam`, `chooseMap`, `start`, `rejection`) ; `LocalSession` (solo : le joueur puis une cible immobile, lancé par `Main`) appelle le serveur en mémoire, `RemoteSession` envoie les messages. Le lobby se lit avec `channel().latestLobby()`.

## 8. Module `core` : packages
Racine `io.github.game2d`. Dépendances : `model` ne dépend de rien ; `protocol` ne dépend que des enums simples de `model` (`Team`, `BrawlerType`) ; `time` ne dépend de rien ; `server` → `model`, `protocol`, `net`, `time` ; `net` → `protocol`, `server` ; `client` → tous. Seul `client` dépend de libGDX ; seul `net.kryo` dépend de KryoNet. Les tests suivent la même arborescence (`core/src/test`).

- `model/` : règles du jeu, Java pur (`GameObject`, `Projectile`, `GameWorld`, `Team`)
  - `model.arena` : `Arena`, `ArenaLoader`, `TileType`, `SpawnPoint`, `ArenaFormatException`
  - `model.brawler` : `Brawler`, `Shelly`, `Colt`, `BrawlerType`
  - `model.weapon` : `Weapon`, `Shotgun`, `Revolver`
  - `model.collision` : `Collisions`
- `protocol/` : messages échangés client/serveur (`PlayerInput`, `WorldSnapshot`, `BrawlerState`, `ProjectileState` ; lobby : `JoinRequest`, `JoinAccepted`, `JoinRejected`, `RefusalReason`, `ChooseTeam`, `ChooseMap`, `StartRequest`, `LobbyState`, `LobbyPlayerState`)
- `time/` : `TickClock` (tick fixe partagé : `TICK_RATE` = 30, `STEP`, numéro de tick, `timeOfTick`)
- `server/` : autorité de jeu (`GameServer`, `ServerLauncher`), lobby (`Lobby`, `LobbyPlayer`, `LobbyException`) et traduction protocole ↔ modèle (`InputApplier`, `SnapshotBuilder`)
- `net/` : transport. Interfaces `ClientChannel` / `ServerChannel`, `LocalChannel` (en mémoire)
  - `net.kryo` : `NetworkEndpoint` (classe abstraite : thread d'update KryoNet, `stop`) → `NetworkServer`, `NetworkClient`, `NetworkClientChannel`, `NetworkServerChannel` (traducteurs de protocole), `NetworkRegistry`
- `client/` : libGDX. `Main` (glue libGDX), `ClientGame` (réception, prédiction, envoi des inputs), `GameSession` → `LocalSession` / `RemoteSession`, `InputReader`, `PredictedPlayer`, `SnapshotBuffer`, `ClientClock` (seule horloge du client : temps local, estimation du temps serveur, cadence des inputs) ; vues `GameView`, `WorldRenderer` (2D), `WorldRenderer3D` + `ArenaModelFactory` (3D, textures dans `assets/textures/arena`) ; HUD `BrawlerHud`, `HudAnchor`

## 9. Lancement (Makefile)
- `make solo` : partie locale ; `make server` : serveur multijoueur (TCP 54555 / UDP 54777) ; `make client [HOST=ip]` : client ; `make duo` : serveur + 2 fenêtres sur la même machine. En attendant les écrans du lobby, seul `make solo` lance une partie (les clients réseau se connectent mais n'envoient pas encore de pseudo).
- `make test`, `make build`, `make clean`.
- Maps lues dans le classpath par `ArenaLoader.loadResource` (fonctionne aussi depuis le jar seul). Le serveur propose `ArenaLoader.availableMaps()` (par défaut `DEFAULT_MAP`, `maps/arena2.txt`) et charge celle choisie dans le lobby ; le client charge encore `DEFAULT_MAP` (il utilisera `LobbyState.map` avec les écrans).

## Reste à faire
- Lobby (dans l'ordre) : `Main` en écrans (`Game` + `Screen`) ; écrans Scene2D (pseudo et adresse du serveur, lobby) ; pseudo au-dessus des brawlers
- Règles d'équipe (pas de tir allié, alliés visibles dans les buissons) puis mode « Hors-jeu » (manches, score, fin de partie)
- Buissons : se révéler en tirant ou en étant touché
- Caméra 3D : élargir la bande de décor pour que le joueur ne soit pas collé au coin au départ
- `StartupHelper` ne transmet pas les arguments sur macOS (le Makefile passe `-XstartOnFirstThread`)
- Soutenance : Javadoc, couverture JaCoCo, diagrammes UML, mise à jour du GDD (`docs/GDD.md`)
- 3D : type et visée dans `BrawlerState`, puis gdx-gltf, personnages animés, ombres

## Structure du projet généré
- `core/` : logique de jeu, serveur et client libGDX (voir section 8)
- `lwjgl3/` : lanceur desktop (`Lwjgl3Launcher.java`, fenêtre 1280x720, vsync, `StartupHelper`)
- `assets/` : ressources partagées entre plateformes (maps, textures, polices)

## Règles
- Ne jamais créer de commit sans demande explicite de l'utilisateur.
