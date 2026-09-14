# Proguard rules for Stickr

# MediaPipe Tasks Vision
-keep class com.google.mediapipe.tasks.vision.** { *; }
-keep class com.google.mediapipe.framework.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.internal.GeneratedComponent

# Coil
-keep class coil.** { *; }
-dontwarn coil.**
