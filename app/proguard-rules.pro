# R8 / ProGuard rules for BP Tracker

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class **$$serializer {
    public static final **$$serializer INSTANCE;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable static ** Companion;
}

# Data DTOs
-keep class ua.vn.home.bptracker.data.dto.** { *; }

# Retrofit interfaces
-keep interface ua.vn.home.bptracker.data.api.** { *; }

# ONNX Runtime
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# Room Database & Entities
-keep class ua.vn.home.bptracker.data.local.entity.** { *; }
-keep interface ua.vn.home.bptracker.data.local.dao.** { *; }

# WorkManager Workers
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
