# Contexte du projet : POC « Brawl Stars » en Java

## Objectif
POC d'un clone simplifié de Brawl Stars : Java, gameplay 2D (plan vu de dessus) affiché en 3D, multijoueur. Le projet doit respecter les concepts de la POO (contrainte imposée).

Projet libGDX généré avec gdx-liftoff (modules `core` et `lwjgl3`, Gradle multi-modules).

## 1. Faisabilité
Faisable en visant un périmètre réduit :
- **Moteur** : libGDX (temps réel, portage Android possible plus tard). JavaFX/Swing plus simples mais moins adaptés.
- **Multijoueur** : serveur autoritaire, KryoNet (TCP + UDP), tick fixe 30 Hz, interpolation côté client, prédiction éventuelle.
- **Périmètre** : plusieurs arènes, 2 à 6 joueurs en deux équipes (jusqu'à 3 contre 3), un tir simple avec points de vie, mode « Hors-jeu » (manches par élimination), un lobby (pseudo, équipe, map), pas de matchmaking ni de comptes.

## 2. Architecture POO
- Hiérarchie : `GameObject` (position, rayon) → `Brawler` → `Shelly` / `Colt` ; `GameObject` → `Projectile`.
- `Weapon` (classe abstraite : munitions, cadence, rechargement) → `Shotgun`, `Revolver` : pattern Strategy, un `Brawler` possède une `Weapon`.
- `GameSession` (interface) implémentée par `LocalSession` (solo, serveur en mémoire) et `RemoteSession` (réseau) ; `Screen` (libGDX) implémentée par `NameScreen`, `LobbyScreen`, `GameScreen`.
- `ClientChannel` / `ServerChannel` : transport en mémoire (`LocalChannel`) ou réseau (`net.kryo`).

Principes illustrés :
- **Héritage** : `GameObject` → `Brawler` → `Shelly` / `Colt`
- **Abstraction** : classes abstraites (`GameObject`, `Brawler`, `Weapon`) et interfaces (`GameSession`, canaux), créées seulement quand il y a plusieurs implémentations
- **Polymorphisme** : `GameWorld` appelle `update(dt, arena)` sur tous les `GameObject`, sans `instanceof` (chaque objet gère son déplacement et ses collisions avec le décor) ; sessions, canaux et écrans interchangeables
- **Composition / Strategy** : `Brawler` a une `Weapon`
- **Encapsulation** : attributs privés ou protégés, modifiés via méthodes (ex. position uniquement dans `Brawler.move`, privée, appelée par `update`)

## 3. Source de vérité et rendu
- Le **serveur** (`GameServer`) est la source de vérité : il possède le vrai `GameWorld` (positions, PV, collisions).
- Deux phases (`GameServer`) : `LOBBY` (pas de monde, pas de snapshot) puis `PLAYING`. Le monde est créé au lancement, sur la map choisie (`GameServer(maps, defaultMap, mapLoader)` : le chargeur est injecté, les tests passent leurs petites maps). Si tout le monde part, retour en `LOBBY`.
- Le serveur envoie **un snapshot par joueur** (`SnapshotBuilder.build(tick, lastProcessed, viewerId)`) : chacun ne reçoit que ce qu'il a le droit de voir.
- Le **client** dessine une copie de l'état reçu (`WorldSnapshot`), interpolée par `SnapshotBuffer`.
- Cycle 30 Hz : inputs envoyés → simulation serveur → snapshot diffusé → mise à jour locale → rendu à 60 FPS.
- Écrans du client : `Main extends Game` affiche un `Screen` à la fois. `Main.showScreen` libère l'ancien écran (`Game` ne le fait jamais). La session (`GameSession`) et le skin appartiennent à `Main` (`connect`, `startSolo`, `play(map)`) et survivent aux changements d'écran.
- Parcours : `NameScreen` (pseudo, adresse du serveur pré-remplie avec l'argument ou `localhost`, boutons « Rejoindre » et « Solo ») → `LobbyScreen` (équipes, brawler, map pour le chef, « Lancer ») → `GameScreen` sur la map de `LobbyState`. « Solo » va directement au jeu, contre une cible immobile.
- Pseudos et équipes en jeu : `PlayerRoster` (id → pseudo et équipe, et mon id) est construit par `GameScreen` à partir du dernier `LobbyState` (l'id du joueur est l'id du brawler). Couleurs relatives : pseudo et barre de vie (`BrawlerHud`) en vert pour moi, bleu pour mes alliés, rouge pour les ennemis ; balles bleues (moi et mes alliés) ou rouges (ennemis). Les brawlers 3D ne sont pas teintés. Pseudos et équipes ne passent jamais dans les snapshots.
- Apparence des balles : `ProjectileState` porte `ownerId`, `dirX`, `dirY` et `shooter` (le `BrawlerType` du tireur, `null` s'il a quitté la partie), remplis par `SnapshotBuilder`. `BulletLook` (interface `sealed`, Java pur, testé) décide pour chaque projectile : `Textured(type, ally)` ou `Sphere`. `BulletModels` cherche au démarrage les images de chaque `BrawlerType` et construit un rectangle texturé à plat ; `WorldRenderer3D` pose la tête de la balle sur la position et la tourne selon la direction.
- Ajouter les balles d'un brawler : déposer `assets/textures/projectiles/<type>_bullet_blue.png` et `<type>_bullet_red.png` (type en minuscules, ex. `colt_bullet_blue.png`, tête de la balle vers la droite). Sans ces deux images, le type garde la sphère. Ajouter une ligne dans `BulletStyles` seulement si la forme de l'image diffère (longueur, largeur, position de la tête).
- Interface Scene2D avec `UiSkin` (skin construit dans le code : police FreeType, fonds unis teintés). Les décisions d'affichage du lobby sont dans `LobbyView` (Java pur, testé) ; `RefusalMessages` transforme une raison de refus en message pour le joueur.

## 4. Collisions
- Toutes les collisions sont calculées côté serveur, dans `model`.
- Chaque objet a un rayon (`GameObject.getRadius()`).
- **Décor** : `Arena` (grille de cases) : `blocksMovement` / `blocksShots` testent un cercle contre les cases voisines (`Collisions.circleTouchesBox`). Mur : bloque tout ; eau : bloque la marche ; buisson : rien ; hors map = mur.
- **Brawlers** : `Brawler.update(dt, arena)` appelle `move`, qui avance en x puis en y (glissement le long des murs), en réduisant le pas jusqu'à 5 fois pour finir collé au mur.
- **Tirs** : `Projectile.update` s'arrête de lui-même dans un mur ; `GameWorld.resolveProjectileHits` gère les tirs sur les brawlers (pas de tir allié : un projectile traverse les alliés de son tireur) et `Collisions.sweptOverlaps` détecte un brawler touché le long du trajet du tir.
- **Équipes** : `GameWorld.addPlayer(brawler, team)` enregistre l'équipe de chaque joueur (donnée de la partie, pas du brawler) ; `areAllies(a, b)` (un joueur est son propre allié, un joueur inconnu n'a pas d'allié).
- Les tests utilisent une vraie arène (`TestArenas.open`, dans `src/test`).

## 5. Mode Hors-jeu (manches)
- `model.match.KnockoutMatch` : le premier à 2 manches gagne. Machine à états `MatchPhase` : `COUNTDOWN` (3 s, personne ne bouge) → `PLAYING` → `ROUND_OVER` (3 s) → manche suivante, ou `MATCH_OVER` (5 s) → retour au lobby.
- Une manche est gagnée quand l'équipe adverse n'a plus de brawler vivant (`GameWorld.aliveCount(team)`), sans temps limite ; si les deux équipes tombent au même tick, la manche est rejouée sans point. `forfeit(team)` : une équipe qui part perd le match.
- `update(dt, aliveCount)` renvoie un `MatchEvent` (`ROUND_STARTED` : nouvelle manche ; `MATCH_FINISHED` : retour au lobby). Java pur, testé (`KnockoutMatchTest`).
- Serveur (`GameServer`) : à chaque tick, si le match est en `PLAYING`, il applique les inputs et fait avancer le monde ; sinon il les accuse seulement (`InputApplier.acknowledge` : le numéro est confirmé pour la prédiction du client, sans déplacement ni tir) et le monde reste figé. Puis `match.update(dt, world::aliveCount)`.
- Nouvelle manche : `placePlayers()` vide le même monde (`GameWorld.clear()`, les ids restent uniques) et replace chacun sur un point de son équipe, PV pleins. Chacun joue le brawler choisi dans le lobby (`LobbyPlayer.brawler`) : il ne peut pas changer pendant le match, car le lobby refuse toute demande en jeu (`GAME_IN_PROGRESS`). Le dernier joueur d'une équipe qui part fait abandonner son équipe.
- Protocole : chaque `WorldSnapshot` porte un `MatchState` (phase, manche, score des Bleus et des Rouges, temps restant, gagnant de la manche et du match), recopié par `SnapshotBuffer` et `ClientGame`.
- Client : hors de `PLAYING`, `ClientGame` envoie un input neutre (`InputReader.idle()`, numéroté, accusé par le serveur) : ni le serveur ni la prédiction ne déplacent le brawler. `MatchView` (Java pur, testé) décide quoi afficher (mon score toujours à gauche en bleu, « ROUND n », décompte arrondi au-dessus, « Round won! » / « Round lost » / « Draw, replaying the round », « VICTORY » / « DEFEAT ») ; `MatchHud` le dessine par-dessus la 3D (polices du skin, bandeau `UiSkin.SHADE`).
- Fin du match : quand `LobbyState.started` repasse à faux, `GameScreen` affiche `LobbyScreen` avec la même session ; le chef peut relancer (un nouveau `GameScreen` repart de zéro). En solo, la map du lobby est `ArenaLoader.DEFAULT_MAP`, pour pouvoir la recharger et rejouer.

## 6. Buissons
- Un brawler dont le centre est sur un buisson (`~`) est caché aux adversaires à plus de 2 cases (`Arena.REVEAL_DISTANCE`). On se voit toujours soi-même, ses alliés restent visibles ; les projectiles restent visibles.
- `Arena.hidesBrawlerAt(x, y)` : la case sous le point cache-t-elle ? `GameWorld.isVisibleTo(target, viewer)` : la règle. `SnapshotBuilder` filtre selon cette règle.
- Filtrage côté serveur : un client ne reçoit jamais la position d'un ennemi caché (anti-triche).
- Côté client, les cases buisson dans ce même rayon autour du joueur sont dessinées transparentes (40 %) (`Arena.canSeeIntoBush`). Chaque buisson est une `ModelInstance` séparée (`ArenaModelFactory.createBush`) pour avoir sa propre opacité.

## 7. Maps et équipes
- Fichiers texte dans `assets/maps/` : `#` mur, `.` sol, `~` buisson, `W` eau, chiffres `1` à `6` = points d'apparition (sur du sol). La première ligne du fichier est le haut de la map.
- Équipes (`model.Team`) : points `1` à `3` = `BLUE` (à gauche), `4` à `6` = `RED` (à droite), en miroir. `SpawnPoint.team()` déduit l'équipe du numéro ; `Arena.getSpawnPoints(team)` renvoie les points d'une équipe triés par numéro.
- `ArenaLoader` vérifie le format (bordure en murs, symboles connus, points de 1 à 6 sans doublon, au moins 2 points). `ArenaLoader.availableMaps()` liste les maps à partir de `assets.txt` (généré à la compilation) : ajouter une map = déposer le fichier.
- Toute map de `assets/maps/` doit avoir 3 points par équipe (vérifié par `ArenaLoaderTest.everyAvailableMapHasThreeSpawnPointsPerTeam`).

## 8. Lobby
- `server.Lobby` : salle d'attente avant la partie, Java pur, sans réseau (utilisée sous le verrou de `GameServer`, donc pas thread-safe).
- Joueurs (`LobbyPlayer` : id, pseudo, équipe, brawler) dans l'ordre d'arrivée ; 6 au maximum, 3 par équipe. Un nouveau joueur va dans l'équipe la moins remplie (Bleus en cas d'égalité).
- Pseudo : espaces autour retirés, 1 à 16 caractères parmi lettres (accents compris), chiffres, `_` et `-`, unique sans tenir compte des majuscules.
- Brawler : à l'arrivée, `Lobby.DEFAULT_BRAWLER` (Colt) ; `chooseBrawler(id, type)` est permis à tous. Plusieurs joueurs peuvent jouer le même brawler, même dans une équipe ; le choix est gardé quand on change d'équipe et d'un match à l'autre. `LobbyPlayer` est un record immuable : `withTeam` / `withBrawler` renvoient une copie, et seul le `Lobby` remplace le joueur dans sa liste.
- Chef = premier de la liste : s'il part, le suivant devient chef. Lui seul choisit la map, parmi la liste donnée au constructeur (`ArenaLoader.availableMaps()`).
- `canStart()` : chaque équipe a entre 1 et 3 joueurs (1v1, 2v1, 3v3… mais pas d'équipe vide).
- Refus : `LobbyException` avec une raison `protocol.RefusalReason` (`INVALID_NAME`, `NAME_TAKEN`, `LOBBY_FULL`, `TEAM_FULL`, `NOT_LEADER`, `UNKNOWN_MAP`, `UNKNOWN_BRAWLER`, `TEAMS_NOT_READY`, `GAME_IN_PROGRESS`).
- `GameServer` : `join(canal, pseudo)`, `chooseTeam`, `chooseBrawler`, `chooseMap`, `requestStart` (chef + `canStart`), `leave`. Après chaque changement, `LobbyState` est envoyé à tous. Au lancement, le n-ième joueur d'une équipe apparaît sur le n-ième point de son équipe ; l'id du joueur est l'id de son brawler.
- Réseau (TCP) : client → serveur `JoinRequest`, `ChooseTeam`, `ChooseBrawler`, `ChooseMap`, `StartRequest` ; serveur → client `JoinAccepted`, `JoinRejected`, `LobbyState` (joueurs avec équipe et brawler, chef, map, maps, `canStart`, `started`). `NetworkServer` attend un `JoinRequest` avant d'accepter le reste ; les demandes refusées autres que le join sont ignorées.
- Client : `GameSession` porte les commandes (`join`, `chooseTeam`, `chooseBrawler`, `chooseMap`, `start`, `rejection`) ; `LocalSession` (solo : le joueur puis une cible immobile, lancé par `Main`, avec le brawler par défaut) appelle le serveur en mémoire, `RemoteSession` envoie les messages. Le lobby se lit avec `channel().latestLobby()`. `LobbyScreen` : liste « Brawler » remplie avec `BrawlerType.values()` (un nouveau brawler y apparaît seul) ; `LobbyView.brawlerLabel` (`COLT` → « Colt »), `myBrawler`, étiquettes « Louis (leader, you) · Colt ».

## 9. Module `core` : packages
Racine `io.github.game2d`. Dépendances : `model` ne dépend de rien ; `protocol` ne dépend que des enums simples de `model` (`Team`, `BrawlerType`) ; `time` ne dépend de rien ; `server` → `model`, `protocol`, `net`, `time` ; `net` → `protocol`, `server` ; `client` → tous. Seul `client` dépend de libGDX ; seul `net.kryo` dépend de KryoNet. Les tests suivent la même arborescence (`core/src/test`).

- `model/` : règles du jeu, Java pur (`GameObject`, `Projectile`, `GameWorld`, `Team`)
  - `model.arena` : `Arena`, `ArenaLoader`, `TileType`, `SpawnPoint`, `ArenaFormatException`
  - `model.brawler` : `Brawler`, `Shelly`, `Colt`, `BrawlerType`
  - `model.weapon` : `Weapon`, `Shotgun`, `Revolver`
  - `model.collision` : `Collisions`
  - `model.match` : `KnockoutMatch`, `MatchPhase`, `MatchEvent`
- `protocol/` : messages échangés client/serveur (`PlayerInput`, `WorldSnapshot`, `BrawlerState`, `ProjectileState`, `MatchState` ; lobby : `JoinRequest`, `JoinAccepted`, `JoinRejected`, `RefusalReason`, `ChooseTeam`, `ChooseBrawler`, `ChooseMap`, `StartRequest`, `LobbyState`, `LobbyPlayerState`)
- `time/` : `TickClock` (tick fixe partagé : `TICK_RATE` = 30, `STEP`, numéro de tick, `timeOfTick`)
- `server/` : autorité de jeu (`GameServer`, `ServerLauncher`), lobby (`Lobby`, `LobbyPlayer`, `LobbyException`) et traduction protocole ↔ modèle (`InputApplier`, `SnapshotBuilder`)
- `net/` : transport. Interfaces `ClientChannel` / `ServerChannel`, `LocalChannel` (en mémoire)
  - `net.kryo` : `NetworkEndpoint` (classe abstraite : thread d'update KryoNet, `stop`) → `NetworkServer`, `NetworkClient`, `NetworkClientChannel`, `NetworkServerChannel` (traducteurs de protocole), `NetworkRegistry`
- `client/` : libGDX. `Main` (`Game` libGDX : ouvre la session, qu'il possède et ferme, puis affiche un écran), écrans `NameScreen`, `LobbyScreen`, `GameScreen` (vue 3D, `InputReader`, `ClientGame`, `MatchHud` ; reçoit la session et la map, revient au lobby à la fin du match), interface `UiSkin`, `LobbyView`, `RefusalMessages`, `PlayerRoster`, `MatchView`, `MatchHud`, `ClientGame` (réception, prédiction, envoi des inputs), `GameSession` → `LocalSession` / `RemoteSession`, `InputReader`, `PredictedPlayer`, `SnapshotBuffer`, `ClientClock` (seule horloge du client : temps local, estimation du temps serveur, cadence des inputs) ; vue `WorldRenderer3D` + `ArenaModelFactory` (3D, textures dans `assets/textures/arena`) ; balles `BulletLook`, `BulletModels`, `BulletStyle`, `BulletStyles` (images dans `assets/textures/projectiles`) ; avatars des brawlers : `AvatarFactory` charge une fois les modèles glTF de `assets/models` (`shelly.glb`, `colt.glb`, sources Blender dans `assets-src`, guide d'animation dans `assets-src/ANIMATION.md`) et crée un `BrawlerAvatar` par brawler (`AnimatedAvatar`, qui choisit lui-même ses clips, ou `CapsuleAvatar` : capsule blanche pour un brawler sans modèle) ; HUD `BrawlerHud`, `HudAnchor`

## 10. Lancement (Makefile)
- `make solo` : partie locale ; `make server` : serveur multijoueur (TCP 54555 / UDP 54777) ; `make client [HOST=ip]` : client ; `make duo` : serveur + 2 fenêtres sur la même machine. Chaque client démarre sur l'écran du pseudo.
- `make test`, `make build`, `make clean`.
- Maps lues dans le classpath par `ArenaLoader.loadResource` (fonctionne aussi depuis le jar seul). Le serveur propose `ArenaLoader.availableMaps()` (par défaut `DEFAULT_MAP`, `maps/arena2.txt`) et charge celle choisie dans le lobby ; le client charge la map de `LobbyState` au lancement (le solo utilise `DEFAULT_MAP`).

## Reste à faire
- Lobby : retour au lobby en fin de partie ; gérer la perte de connexion au serveur ; connexion sans bloquer l'écran (jusqu'à 5 s aujourd'hui)
- Mode « Hors-jeu » : caméra qui suit un allié après sa mort, « GO! » à la fin du décompte, sons
- Buissons : se révéler en tirant ou en étant touché
- Caméra 3D : élargir la bande de décor pour que le joueur ne soit pas collé au coin au départ
- `StartupHelper` ne transmet pas les arguments sur macOS (le Makefile passe `-XstartOnFirstThread`)
- Soutenance : Javadoc, couverture JaCoCo, diagrammes UML, mise à jour du GDD (`docs/GDD.md`)
- 3D : type et visée dans `BrawlerState`, puis gdx-gltf, personnages animés, ombres
- Balles de Shelly : déposer `shelly_bullet_blue.png` / `shelly_bullet_red.png`

## Structure du projet généré
- `core/` : logique de jeu, serveur et client libGDX (voir section 9)
- `lwjgl3/` : lanceur desktop (`Lwjgl3Launcher.java`, fenêtre 1280x720, vsync, `StartupHelper`)
- `assets/` : ressources partagées entre plateformes (maps, textures, polices)

## Règles
- Ne jamais créer de commit sans demande explicite de l'utilisateur.
- Tout le texte du code est en anglais : interface du jeu, messages d'erreur, commentaires, Makefile (seules exceptions : des données de test comme un pseudo accentué).
