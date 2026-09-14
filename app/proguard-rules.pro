# ============================================================================
# ProGuard & R8 Configuration Rules for Stickr
# Target: Android 15 (SDK 35), Min SDK 26
# ============================================================================

# ----------------------------------------------------------------------------
# 1. Google MediaPipe Tasks Vision & TensorFlow Lite (AI Local Segmentation)
# ----------------------------------------------------------------------------
-keep class com.google.mediapipe.tasks.vision.** { *; }
-keep interface com.google.mediapipe.tasks.vision.** { *; }
-keep class com.google.mediapipe.framework.** { *; }
-keep interface com.google.mediapipe.framework.** { *; }
-keep class com.google.mediapipe.tasks.core.** { *; }
-keep class org.tensorflow.lite.** { *; }
-keep interface org.tensorflow.lite.** { *; }

# Préserver tous les appels JNI natifs requis par MediaPipe & TFLite
-keepclasseswithmembernames class * {
    native <methods>;
}

-dontwarn com.google.mediapipe.**
-dontwarn org.tensorflow.lite.**

# ----------------------------------------------------------------------------
# 2. Room Database & SQLite
# ----------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.migration.Migration
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

-dontwarn androidx.room.paging.**

# ----------------------------------------------------------------------------
# 3. Dagger Hilt & ViewModel
# ----------------------------------------------------------------------------
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.internal.GeneratedComponent
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# ----------------------------------------------------------------------------
# 4. Kotlin Coroutines
# ----------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ----------------------------------------------------------------------------
# 5. Coil Compose (Image Cache & Decoding)
# ----------------------------------------------------------------------------
-keep class coil.** { *; }
-dontwarn coil.**

# ----------------------------------------------------------------------------
# 6. AndroidX Core Splashscreen
# ----------------------------------------------------------------------------
-keep class androidx.core.splashscreen.** { *; }
-dontwarn androidx.core.splashscreen.**

# ----------------------------------------------------------------------------
# 7. Modèles métiers, entités Room, calques de l'éditeur & ContentProvider IPC
# ----------------------------------------------------------------------------
-keep class com.stickr.app.core.model.** { *; }
-keep class com.stickr.app.core.database.entity.** { *; }
-keep class com.stickr.app.feature.editor.model.** { *; }
-keep class com.stickr.app.core.provider.** { *; }
-keep class com.stickr.app.core.image.** { *; }

# Préserver les méthodes d'encodage Bitmap / WebP WhatsApp
-keepclassmembers class android.graphics.Bitmap {
    public boolean compress(android.graphics.Bitmap$CompressFormat, int, java.io.OutputStream);
}
-keepclassmembers class android.graphics.BitmapFactory { *; }

# ----------------------------------------------------------------------------
# 8. Suppress compile-time annotations & AutoValue / JavaPoet warnings in R8
# ----------------------------------------------------------------------------
-dontwarn javax.lang.model.**
-dontwarn javax.annotation.processing.**
-dontwarn autovalue.shaded.**
-dontwarn com.google.auto.value.**
-dontwarn com.google.auto.**

