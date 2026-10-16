# Animer un brawler (Blender vers glTF)

Guide pour ajouter ou modifier les animations d'un modèle 3D (`assets-src/<nom>.blend`, exporté dans `assets/models/<nom>.glb`).
Le code client qui joue les clips est `AnimatedAvatar` (deux contrôleurs, et le choix des clips).

## Les clips attendus (noms exacts, sensibles à la casse)

| Clip | Contenu | Boucle |
|---|---|---|
| `Idle` | corps entier à l'arrêt | oui |
| `Run` | corps entier en course | oui |
| `IdleUpper` | haut du corps de `Idle` seulement | oui |
| `RunUpper` | haut du corps de `Run` seulement | oui |
| `Shoot` | un seul clip de tir, haut du corps seulement | non, joué une fois |

Il n'y a plus de clip `Aim`, ni de clips de tir par main (`ShootRight` / `ShootLeft`) : un seul `Shoot` par modèle.

## Comment le jeu les joue

- `AnimatedAvatar` a deux `AnimationController` sur le même modèle. Le premier joue `Idle` ou `Run` (selon `moving`). Le second passe par-dessus et ne touche que le haut du corps : `IdleUpper` / `RunUpper` en boucle, ou `Shoot` une fois à chaque nouveau tir.
- Un nouveau tir est détecté quand `BrawlerState.shotsFired` augmente. Le serveur ne compte **qu'une attaque** par tir, même si l'attaque contient plusieurs projectiles (une rafale de Colt compte pour 1). Le clip `Shoot` doit donc couvrir toute l'attaque, pas une seule balle.
- Fondus (dans `AnimatedAvatar`) : 0,05 s vers `Shoot`, 0,2 s de `Shoot` vers `IdleUpper` / `RunUpper`.

## Règle d'or : Shoot commence et finit sur la pose d'Idle

Les fondus sont courts. Si `Shoot` commence sur une autre pose que celle d'`IdleUpper`, l'avatar saute d'une pose à l'autre. Donc le clip doit contenir sa propre montée et sa propre descente :

1. **Image 0 = pose d'`IdleUpper`** (image 0). Pas la pose de tir.
2. **Montée** (environ 4 images à 30 fps) : interpoler de la pose d'Idle vers la pose de tir (armes pointées vers l'avant).
3. **Tir** : le geste, une fois pour un tir simple, ou toute la rafale (Colt : 6 balles espacées de 3 images, droite puis gauche).
4. **Descente** (environ 8 à 9 images) : revenir à la pose d'`IdleUpper`. **La dernière image = la pose d'Idle.**

Interpoler les rotations par `slerp` entre quaternions (et corriger le signe si le produit scalaire est négatif), avec un `smoothstep` pour le poids. Si la pose d'Idle d'un perso change (bras écartés, etc.), **refaire le `Shoot`** : sa première et sa dernière image doivent suivre.

## Composer une rafale (Colt)

- Une balle toutes les 3 images (0,1 s à 30 fps, comme `BurstPattern(6, 0.1, ...)`), côtés alternés.
- Un geste par balle (recul d'un bras). Quand deux gestes se chevauchent, **additionner leurs écarts au repos** (valeur = repos + somme des écarts), puis renormaliser les quaternions. La somme est continue : chaque geste vaut 0 à son début et à sa fin.
- Le geste d'un bras ne doit pas figer l'autre : chaque bras a son propre geste, décalé de 3 images.
- La durée du clip doit couvrir la rafale. Cadence et durée de l'arme : `Revolver` (`COOLDOWN` au moins égal à la durée de la rafale).

## Pièges

- **Os par clip.** Les clips du haut du corps ne doivent contenir QUE les os du haut (sinon ils figent les jambes, car le clip du haut passe après celui du corps). Colt : 14 os (`ChestBone_02`, `HatBone_026`, `HeadBone_022`, `NeckBone_021`, bras, avant-bras, mains, collets, armes G/D). `Idle` et `Run` : 21 os (haut + jambes). Vérifier le nombre d'os de chaque clip après export (aucun os `Leg`, `Pelvis` dans `*Upper` et `Shoot`).
- **Export Blender.** `export_force_sampling=False` obligatoire (sinon tous les os sont écrits dans tous les clips). Des clés Bézier sont exportées en `CUBICSPLINE` : rééchantillonner chaque courbe à chaque image en `LINEAR` avant l'export, dans un script Blender lancé en ligne de commande, sans enregistrer le blend.
- **Sens de la course.** Vérifier que le pied au point le plus bas recule par rapport au bassin pendant l'appui (sinon la course est à l'envers : jouer les clés des jambes de `Run` à l'envers dans le temps). C'était le cas pour Colt et Shelly à l'origine.
- **Limite d'os du shader** : `MAX_BONES = 64` dans `WorldRenderer3D`. Shelly en a exactement 64.
- **Pas de couleur d'équipe** sur le modèle : elle est dans le HUD uniquement. Un modèle sans entrée dans `AvatarFactory.MODELS` est dessiné comme une capsule blanche.
- **Pistes NLA** : le blend garde une piste NLA par action, mutées. Pour mesurer une pose ou rendre une image, mettre `animation_data.use_nla = False` et assigner l'action. Pour poser un os à la main puis le lire, détacher l'action (sinon l'évaluation écrase la pose).
- **Ne pas versionner** les `*.blend1` (sauvegardes automatiques de Blender).
- Si on ajoute un clip, il faut réexporter le glb (un patch des octets ne suffit pas).

## Procédure complète

1. Ouvrir `assets-src/<nom>.blend` (dans Blender ou en ligne de commande : `blender -b fichier.blend --python script.py`).
2. Créer ou modifier les actions dans le blend (garder `use_fake_user`) et leurs pistes NLA. Enregistrer le blend.
3. Exporter avec un script lancé par `blender -b assets-src/<nom>.blend --python script.py` : rééchantillonner chaque courbe à chaque image en `LINEAR` (en mémoire, sans enregistrer), sélectionner l'armature et ses meshes, puis `bpy.ops.export_scene.gltf(filepath='assets/models/<nom>.glb', export_format='GLB', use_selection=True, export_animations=True, export_animation_mode='ACTIONS', export_force_sampling=False, export_optimize_animation_size=True, export_yup=True, export_skins=True)`.
4. Vérifier le glb : liste des clips, nombre d'os par clip, durée de `Shoot`, et que les clips non modifiés sont identiques à l'ancien fichier.
5. Dans le client, `AvatarFactory.MODELS` donne le chemin du modèle ; les noms de clips sont des constantes de `AnimatedAvatar`.
6. Lancer `make solo` : regarder l'arrêt, la course, le tir à l'arrêt et le tir en courant, et le retour à l'Idle après le tir.

## Sources

- Shelly : clips dans `shelly.blend` (`Idle`, `IdleUpper`, `Run`, `RunUpper`, `Shoot`).
- Colt : clips dans `colt.blend` (bras écartés dans `Idle` / `IdleUpper`, `Shoot` composé de la rafale avec montée et descente).
