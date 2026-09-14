# Stickr 🎨✨

**Stickr** est une application Android native moderne de création et personnalisation de stickers (similaire à Sticker.ly), architecturée selon les principes de la **Clean Architecture** et du patron de conception **MVVM**.

Elle intègre l'intelligence artificielle locale via **Google MediaPipe Tasks Vision** pour le détourage automatique instantané de sujets et d'objets sans nécessiter de serveur distant.

---

## 🚀 Fonctionnalités Clés

- **Détourage Intelligent IA (Sur l'appareil)** : Segmentation et suppression d'arrière-plan haute précision grâce à Google MediaPipe Tasks Vision.
- **Calques Multiples & Graphisme Tactile** : Empilement interactif du sujet détouré, d'accessoires décoratifs (lunettes, flammes, couronnes) et de textes stylisés (style mème avec contour contrasté).
- **Moteur d'Aplatissement Graphique** : Fusion matricielle des calques en un Bitmap transparent 512x512 px avec compression stricte WebP < 100 Ko.
- **Gestion des Packs de Stickers** : Création, catégorisation, consultation et gestion de packs personnalisés persistés localement avec Room.
- **Exportation Officielle WhatsApp** : Intégration complète du protocole WhatsApp Stickers avec ContentProvider, validation stricte et tray icon automatique.

---

## 🛠️ Stack Technique & Dépendances

| Composant | Version / Outil | Rôle |
|---|---|---|
| **Langage** | Kotlin 2.0.21 | Nouveau compilateur K2 & support natif du plugin Compose |
| **UI** | Jetpack Compose BOM 2024.11.00 | Interface déclarative moderne avec Material 3 |
| **Architecture** | Clean Architecture + MVVM | Séparation stricte Domain / Data / Presentation |
| **Injection de Dépendances** | Dagger Hilt 2.52 | Gestion modulaire et testable du graphe de dépendances |
| **Base de Données** | Room 2.6.1 (avec KSP) | Persistance locale réactive avec Kotlin Flow |
| **Chargement d'Images** | Coil Compose 2.7.0 | Gestion asynchrone et performante du cache d'images |
| **Vision / IA Locale** | Google MediaPipe Tasks Vision 0.10.14 | Modèle de segmentation sémantique d'image local |
| **Navigation** | Navigation Compose 2.8.5 | Navigation fluide entre écrans |
| **Build & SDK** | AGP 8.7.3 / Gradle 8.11.1 / compileSdk 35 | Cible Android 15 (SDK 35), `compose = true`, `viewBinding = true` |

---

## 🏛️ Architecture du Projet

```text
com.stickr.app/
├── StickrApp.kt               # Application Hilt (@HiltAndroidApp)
├── MainActivity.kt            # Activité racine (@AndroidEntryPoint)
│
├── core/image/                # Moteur Graphique & IA
│   ├── ImageSegmenterHelper.kt    # Inférence locale IA MediaPipe Tasks Vision
│   ├── StickerBorderProcessor.kt  # Algorithme de contour die-cut (dilatation radiale)
│   ├── StickerExporter.kt         # Normalisation WebP 512x512 et compression WhatsApp <100 Ko
│   ├── StickerFlattener.kt        # Aplatissement multi-calques 512x512 ARGB_8888 natif
│   └── TrayIconHelper.kt          # Génération à la volée de l'icône de plateau 96x96 px
│
├── core/database/             # Persistance Room Locale
│   ├── StickrDatabase.kt          # Base de données Room
│   ├── dao/StickerPackDao.kt      # DAO pour packs et stickers
│   └── entity/                    # StickerPackEntity, StickerItemEntity, StickerPackWithStickers
│
├── core/provider/             # Intégration ContentProvider WhatsApp
│   ├── StickerContentProvider.kt  # Provider IPC conforme au protocole WhatsApp
│   └── StickerContentProviderContract.kt
│
├── feature/editor/model/      # Modèle de Calques Graphiques
│   └── EditorLayer.kt             # SubjectLayer, TextLayer, DecorationLayer
│
├── feature/dashboard/         # Tableau de bord des packs créés
│   ├── DashboardScreen.kt
│   ├── DashboardUiState.kt
│   └── DashboardViewModel.kt
│
├── feature/packdetail/        # Vue détaillée d'un pack de stickers
│   ├── PackDetailScreen.kt
│   ├── PackDetailUiState.kt
│   └── PackDetailViewModel.kt
│
└── presentation/screens/editor/ # Éditeur Tactile Multi-Calques
    ├── InteractiveCanvas.kt       # Canvas multitouch 60/120 FPS avec calques et cadre de sélection
    ├── EditorToolbar.kt           # Barre d'outils (IA, texte, accessoires, contour, undo/redo)
    ├── StickerEditorScreen.kt     # Écran d'édition complet
    ├── StickerEditorUiState.kt    # État UI réactif de l'éditeur
    ├── StickerEditorViewModel.kt  # ViewModel Hilt pour l'édition et l'enregistrement
    └── components/
        ├── TextEditDialog.kt              # Boîte de dialogue de style texte (mème, polices, contours)
        └── DecorationPickerBottomSheet.kt # Feuille de choix d'accessoires et emojis
```

---

## 🎨 Moteur Graphique & IA (`core:image`)

### 1. `ImageSegmenterHelper` (IA Locale)
- Exécute le modèle `selfie_segmenter.tflite` ou `deeplab_v3.tflite` via Google MediaPipe Tasks Vision.
- Fonction : `suspend fun segmentSubject(inputBitmap: Bitmap): Result<Bitmap>`
- Inférence asynchrone sur `Dispatchers.Default` produisant un Bitmap `ARGB_8888` transparent contenant uniquement le sujet extrait.

### 2. `StickerBorderProcessor` (Contour Die-Cut)
- Fonction : `fun addStickerBorder(source: Bitmap, borderSizePx: Float = 24f, @ColorInt borderColor: Int = Color.WHITE): Bitmap`
- Algorithme par dilatation radiale multi-passes sur masque alpha (`extractAlpha()`) sans flou parasite, produisant un contour net façon autocollant vinyle.

### 3. `StickerExporter` (Standard WhatsApp)
- Fonction : `suspend fun prepareForWhatsApp(source: Bitmap): ByteArray`
- Contraintes strictes respectées :
  * Canvas exact de **512x512 pixels** avec fond transparent et marge de sécurité (16px).
  * Encodage au format **WebP**.
  * Boucle de réduction dynamique de la qualité assurant un poids strictement **inférieur à 100 Ko** (102 400 octets).

### 4. `StickerFlattener` (Moteur d'Aplatissement Multi-Calques)
- Fonction : `fun flattenLayers(canvasSize: Int = 512, layers: List<EditorLayer>, marginPx: Int = 16): Bitmap`
- Fusionne l'ensemble des calques ordonnés (`SubjectLayer`, `DecorationLayer`, `TextLayer`) en un unique Bitmap 512x512 px `ARGB_8888` transparent.
- Applique les matrices de transformation (translation, zoom, rotation) pour chaque calque.
- Rendu double-passe pour le texte stylisé : contour externe (`Paint.Style.STROKE`) puis remplissage intérieur (`Paint.Style.FILL`) avec polices dynamiques (Impact mème, Sans-Serif, Serif, Monospace, Cursive).

---

## 🎭 Calques de Texte Stylisé & Accessoires (`feature:editor-layers`)

### 1. Modèle de Données (`EditorLayer`)
- **`SubjectLayer`** : Sujet de la photo détouré avec contour die-cut personnalisable, translation, rotation et zoom.
- **`TextLayer`** : Texte stylisé avec contour contrasté, choix de couleur de texte (Fill), couleur de trait (Stroke), épaisseur de contour, taille de police et police d'écriture.
- **`DecorationLayer`** : Accessoires et emojis populaires (😎, 👑, 🔥, 💬, 🍕, etc.) manipulables avec redimensionnement et rotation.

### 2. Dialogue d'Édition de Texte (`TextEditDialog`)
- Saisie de texte avec aperçu dynamique en direct sur fond damier.
- Sélection de polices : Impact, Sans-Serif, Serif, Monospace, Cursive.
- Palettes de couleurs indépendantes pour le remplissage et le contour.
- Sliders pour la taille de police (20 à 72 sp) et l'épaisseur du trait (0 à 16 px).

### 3. Sélecteur d'Accessoires (`DecorationPickerBottomSheet`)
- Catégories thématiques : *Accessoires*, *Mèmes & Réactions*, *Émotions & Bulles*, *Food & Objets*.
- Grille tactile avec insertion immédiate sur le canvas au centre.

### 4. Canvas Tactile Interactif Multi-Calques (`InteractiveCanvas`)
- Rendu GPU accéléré via `graphicsLayer`.
- Cadre de sélection visuel (bounding box cyan) autour du calque actif.
- Poignée de suppression rapide (X) sur le calque sélectionné.
- Bouton d'édition rapide (icône crayon) sur les calques de texte.
- Barre de sélection rapide de calques (FilterChips) pour basculer facilement entre calques superposés.
- Routage intelligent des gestes tactiles vers le calque sélectionné (ou le sujet principal par défaut).

---

## 🤖 Modèles IA MediaPipe dans `app/src/main/assets/`

Le modèle officiel suivant est déjà inclus dans le dépôt :
- [app/src/main/assets/selfie_segmenter.tflite](file:///root/Stickr/app/src/main/assets/selfie_segmenter.tflite) (~249 Ko) : Optimisé pour les portraits et découpes d'autocollants instantanées.

---

## 🟢 Intégration Officielle WhatsApp Stickers (`core:provider` & `core:util`)

### 1. `StickerContentProvider` (ContentProvider IPC)
- Déclaré avec l'autorité dynamique `${applicationId}.stickercontentprovider`.
- Expose les routes standardisées reconnues par le client officiel WhatsApp :
  * `/metadata` : Liste de tous les packs disponibles (format Cursor).
  * `/metadata/*` : Détails d'un pack spécifique.
  * `/stickers/*` : Liste des stickers d'un pack et leurs émojis associés.
  * `/stickers_asset/*/*` : Streaming binaire sécurisé du fichier WebP via `openFile`.
  * `/tray_asset/*` : Streaming binaire de l'icône de plateau 96x96 px (générée à la volée si absente).
- Protection robuste contre le path traversal (`canonicalPath` vérifié).

### 2. `WhatsAppStickerValidator` (Validation Stricte)
- Vérifie que chaque pack exporté respecte scrupuleusement les exigences de WhatsApp :
  * **Nombre de stickers** : Entre 3 et 30 inclus.
  * **Émojis** : Entre 1 et 3 émojis par sticker.
  * **Icône de plateau** : Exactement 96x96 pixels et strictement inférieure à 50 Ko.
  * **Stickers** : WebP 512x512 pixels et strictement inférieurs à 100 Ko.

### 3. `WhatsAppIntentHelper` (Intent d'Ajout)
- Méthodes `isWhatsAppInstalled` et `isAnyWhatsAppInstalled` pour détecter WhatsApp Consumer (`com.whatsapp`) et Business (`com.whatsapp.w4b`).
- Lance l'action officielle `com.whatsapp.intent.action.ENABLE_ADD_PACK` avec les extras :
  * `EXTRA_STICKER_PACK_ID`
  * `EXTRA_STICKER_PACK_AUTHORITY`
  * `EXTRA_STICKER_PACK_NAME`
- Déclaration `<queries>` dans [AndroidManifest.xml](file:///root/Stickr/app/src/main/AndroidManifest.xml) pour la compatibilité Android 11+ (API 30+).

---

## 🗄️ Persistance Room & Gestion des Packs (`feature:pack-management`)

### 1. Base de données Room (`core:database`)
- **`StickrDatabase`** : Configuration Room avec entités `StickerPackEntity` et `StickerItemEntity`.
- **Identifiants UUID** : Clés primaires `String` universelles s'alignant sur les identifiants requis par le protocole WhatsApp.
- **Suppression en Cascade** : Clé étrangère reliant chaque sticker à son pack (`ForeignKey.CASCADE`).
- **Relation 1-à-N `StickerPackWithStickers`** : Regroupement réactif du pack et de ses stickers associés.
- **`StickerPackDao`** :
  * Requêtes réactives `Flow<List<StickerPackWithStickers>>` pour l'interface Compose.
  * Requêtes synchrones pour le `StickerContentProvider` (WhatsApp IPC).
  * Réordonnancement et modification des émojis.

### 2. Dépôt & Nettoyage Disque (`StickerPackRepository`)
- **Gestion stricte des fichiers** : Suppression physique immédiate des fichiers `.webp` et des icônes de plateau `.png` sur le stockage interne de l'application (`filesDir`) lors de la suppression d'un sticker ou d'un pack (aucun fichier orphelin).
- **Génération automatique de Tray Icon** : Génère à la volée l'icône 96x96 px PNG requise par WhatsApp à partir du premier sticker du pack via `TrayIconHelper`.

### 3. Dashboard (`feature.dashboard`)
- **`DashboardScreen` & `DashboardViewModel`** :
  * Affichage réactif de la liste des packs créés avec miniature ou mosaïque.
  * Badges d'état dynamiques (vert si $\ge 3$ stickers, orange si $< 3$).
  * Bouton d'action directe *"Ajouter à WhatsApp"* sur la carte de pack.
  * Dialogue de création de pack avec saisie du nom et de l'auteur.

### 4. Détail du Pack (`feature.packdetail`)
- **`PackDetailScreen` & `PackDetailViewModel`** :
  * En-tête avec métadonnées éditables et prévisualisation de l'icône de plateau.
  * Grille des stickers avec suppression rapide et dialogue d'association d'émojis (1 à 3 émojis max pour WhatsApp).
  * Bouton (+) ouvrant l'éditeur tactile `StickerEditorScreen`.
  * Bouton fixe d'exportation officielle vers WhatsApp en pied d'écran.
