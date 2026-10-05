# Contexte du projet : POC « Brawl Stars » en Java

## Objectif
POC d'un clone simplifié de Brawl Stars : Java, 2D, multijoueur. Le projet doit respecter les concepts de la POO (contrainte imposée).

Projet libGDX généré avec gdx-liftoff (modules `core` et `lwjgl3`, Gradle multi-modules). Lancement : `./gradlew lwjgl3:run`.

## 1. Faisabilité
Faisable en visant un périmètre réduit :
- **Moteur** : libGDX (temps réel, portage Android possible plus tard). JavaFX/Swing plus simples mais moins adaptés.
- **Multijoueur** : partie la plus difficile. Serveur autoritaire, transport UDP ou TCP (KryoNet ou Netty), tick fixe 20-30 Hz, interpolation, prédiction côté client éventuelle.
- **Périmètre** : une arène, 2 à 4 joueurs, un tir simple avec points de vie, mode « dernier survivant », pas de matchmaking ni de comptes.

## 2. Architecture POO
Diagrammes produits (classes, architecture client/serveur, séquence d'un tir).

- Hiérarchie : `GameObject` → `Brawler` → `Shelly` / `Colt`; plus `Projectile`, `GameWorld`, `GameServer`.
- Interface `Weapon` (`Shotgun`, `Revolver`) : pattern Strategy, un `Brawler` possède une `Weapon`.

Principes illustrés :
- **Héritage** : `GameObject` → `Brawler` → `Shelly` / `Colt`
- **Abstraction** : classes abstraites
- **Polymorphisme** : `GameServer` appelle `update(dt)` sur tous les `GameObject`
- **Composition / Strategy** : `Brawler` a une `Weapon`
- **Encapsulation** : attributs protégés, modifiés via méthodes

## 3. Source de vérité et rendu
- Le **serveur** est la source de vérité : il possède le vrai `GameServer` (positions, PV, collisions).
- Le **client** dessine une copie simplifiée de l'état reçu.
- Cycle ~30 Hz : inputs envoyés → simulation serveur → snapshot diffusé → mise à jour locale → rendu à 60 FPS.
- Latence : prédiction côté client (son propre mouvement) et interpolation (autres joueurs). Conseil : commencer sans prédiction.

## 4. Ordre de développement (solo d'abord, sans réseau)
1. Modèle de base (`core`) avec tests JUnit
2. Rendu libGDX minimal (un carré déplacé avec WASD)
3. Projectiles, collisions, points de vie
4. Séparation client/serveur en local (thread serveur + file en mémoire)
5. Vrai réseau (KryoNet ou Netty)
6. Interpolation, puis prédiction si besoin

## 5. Module `core` : packages
Racine `io.github.game2d`. Règle de dépendance : `model` ← `protocol` ← `server` / `net` ← `client`. Seul `client` dépend de libGDX ; seul `net.kryo` dépend de KryoNet. Les tests suivent la même arborescence (`core/src/test`).

- `model/` : domaine pur (`GameObject`, `Projectile`, `GameWorld`)
  - `model.arena` : `Arena`, `ArenaLoader`, `TileType`, `SpawnPoint`, `ArenaFormatException`
  - `model.brawler` : `Brawler`, `Shelly`, `Colt`
  - `model.weapon` : `Weapon`, `Shotgun`, `Revolver`
  - `model.collision` : `Hitbox`, `Circle`, `Rect`, `Collisions`
- `protocol/` : messages échangés client/serveur (`PlayerInput`, `JoinAccepted`, `WorldSnapshot`, `BrawlerState`, `ProjectileState`)
- `server/` : autorité de jeu (`GameServer`, `FixedTimestep`, `ServerLauncher`)
- `net/` : transport. Interfaces `ClientChannel` / `ServerChannel`, `LocalChannel` (en mémoire)
  - `net.kryo` : `NetworkServer`, `NetworkClient` (cycle de vie KryoNet), `NetworkClientChannel`, `NetworkServerChannel` (traducteurs de protocole), `NetworkRegistry`
- `client/` : libGDX (`Main`, `InputReader`, `WorldRenderer`)

Lancement : client `./gradlew lwjgl3:run`, serveur autonome `./gradlew core:runServer`.

## Reste à faire
- Séparer `Main` en lancement local / connexion distante (`GameClient` avec session locale ou distante)
- Interpolation côté client, puis prédiction si besoin
- Mode « dernier survivant »

## Structure du projet généré
- `core/` : logique de jeu, serveur et client libGDX (voir section 5)
- `lwjgl3/` : lanceur desktop (`Lwjgl3Launcher.java`, fenêtre 640x480, vsync, `StartupHelper`)
- `assets/` : ressources partagées entre plateformes

## Règles
- Ne jamais créer de commit sans demande explicite de l'utilisateur.
