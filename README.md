# Stickr 🎨✨

**Stickr** est une application Android native moderne de création et personnalisation de stickers (similaire à Sticker.ly), architecturée selon les principes de la **Clean Architecture** et du patron de conception **MVVM**.

Elle intègre l'intelligence artificielle locale via **Google MediaPipe Tasks Vision** pour le détourage automatique instantané de sujets et d'objets sans nécessiter de serveur distant.

---

## 🚀 Fonctionnalités Clés

- **Détourage Intelligent IA (Sur l'appareil)** : Segmentation et suppression d'arrière-plan haute précision grâce à Google MediaPipe Tasks Vision.
- **Gestion des Packs de Stickers** : Création, catégorisation, consultation et gestion de packs personnalisés persistés localement avec Room.
- **Éditeur de Stickers** :
  - Importation d'images depuis la galerie ou la caméra.
  - Découpage automatique par IA ou manuel.
  - Ajout de bordures blanches de style sticker.
  - Aperçu instantané sur fond en damier de transparence.
- **Exportation & Partage** : Préparé pour l'exportation vers WhatsApp et Telegram via FileProvider sécurisé.

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
├── core/image/                # Moteur Graphique & IA (Étape 2)
│   ├── ImageSegmenterHelper.kt    # Inférence locale IA MediaPipe Tasks Vision
│   ├── StickerBorderProcessor.kt  # Algorithme de contour die-cut (dilatation radiale)
│   └── StickerExporter.kt         # Normalisation WebP 512x512 et compression WhatsApp <100 Ko
│
├── di/                        # Modules Hilt (Dagger)
│   ├── AppModule.kt           # Fournisseurs de Dispatchers et Contexte
│   ├── DatabaseModule.kt      # Configuration Room & DAOs
│   ├── MediaPipeModule.kt     # Initialisation du module Vision IA
│   └── RepositoryModule.kt    # Binding des interfaces de Repository
│
├── domain/                    # Couche Domaine (Pure Kotlin)
│   ├── model/                 # Modèles métiers (Sticker, StickerPack, SegmentationResult)
│   ├── repository/            # Contrats d'interfaces (StickerRepository, ImageSegmentationRepository)
│   └── usecase/               # Cas d'usage métier (SegmentImage, SaveSticker, GetPacks, CreatePack)
│
├── data/                      # Couche Données (Implémentations)
│   ├── local/
│   │   ├── AppDatabase.kt     # Définition RoomDatabase
│   │   ├── dao/               # StickerPackDao, StickerDao
│   │   └── entity/            # StickerPackEntity, StickerEntity
│   └── repository/
│       ├── StickerRepositoryImpl.kt
│       └── ImageSegmentationRepositoryImpl.kt
│
└── presentation/              # Couche Présentation (UI Jetpack Compose)
    ├── navigation/            # Graphe de navigation (Screen, StickrNavGraph)
    ├── theme/                 # Thème Material 3 (Color, Theme, Type)
    └── screens/
        ├── home/              # Écran d'accueil (Liste des packs)
        ├── editor/            # Écran d'édition (Détourage IA, bordure, aperçu)
        └── packdetail/        # Écran de détails d'un pack (Grille de stickers, export)
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
