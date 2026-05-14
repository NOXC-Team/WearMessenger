# Keep TDLib classes
-keep class org.drinkless.tdlib.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
