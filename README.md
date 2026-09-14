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
│   ├── mediapipe/
│   │   └── ImageSegmentationHelper.kt # Traitement IA MediaPipe Tasks Vision
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

## 📦 Configuration Gradle

Le fichier [app/build.gradle.kts](file:///root/Stickr/app/build.gradle.kts) active les fonctionnalités demandées :

```kotlin
android {
    namespace = "com.stickr.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.stickr.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        ...
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
}
```

---

## 🤖 Modèle IA MediaPipe (Détourage d'image)

Pour activer la segmentation locale dans l'émulateur ou sur appareil physique :
1. Créez le dossier `app/src/main/assets/`.
2. Téléchargez le modèle TensorFlow Lite de segmentation MediaPipe (ex: `selfie_segmenter.tflite`).
3. Placez le fichier dans `app/src/main/assets/selfie_segmenter.tflite`.
