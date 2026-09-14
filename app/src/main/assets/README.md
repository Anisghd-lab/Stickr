# Modèles IA Google MediaPipe Tasks Vision pour Stickr 🧠📸

Pour effectuer le détourage d'image localement (on-device) et hors-ligne sans serveur externe, l'application utilise les modèles TensorFlow Lite de Google MediaPipe Tasks Vision.

## Modèles supportés

### 1. `selfie_segmenter.tflite` (Recommandé - Déjà inclus)
- **Rôle** : Segmentation ultra-rapide et précise des personnes et portraits (effet sticker parfait).
- **Taille** : ~249 Ko (quantifié Float16).
- **Format de sortie** : Deux masques de confiance (0: arrière-plan, 1: personne).
- **Source officielle Google** : [selfie_segmenter.tflite](https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite)

### 2. `deeplab_v3.tflite` (Alternative multi-catégories)
- **Rôle** : Segmentation sémantique de 21 catégories d'objets (personnes, animaux de compagnie, véhicules, etc.).
- **Taille** : ~2.7 Mo.
- **Source officielle Google** : [deeplab_v3.tflite](https://storage.googleapis.com/mediapipe-models/image_segmenter/deeplab_v3/float32/latest/deeplab_v3.tflite)

## Emplacement
Les modèles doivent être placés dans :
```text
app/src/main/assets/selfie_segmenter.tflite
app/src/main/assets/deeplabv3.tflite
```

Dans le code, configurez le nom du fichier dans `ImageSegmenterHelper` via le paramètre `modelPath` (par défaut : `"selfie_segmenter.tflite"`).
