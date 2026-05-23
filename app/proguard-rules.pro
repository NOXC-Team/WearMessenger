# Keep TDLib classes
-keep class org.drinkless.tdlib.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Ktor - disable all obfuscation and optimization
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Netty - disable all obfuscation and optimization
-keep class io.netty.** { *; }
-keepclassmembers class io.netty.** { *; }
-dontwarn io.netty.**

# Netty - ignore missing optional dependencies
-dontwarn io.netty.internal.tcnative.**
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn java.lang.management.**
-dontwarn reactor.blockhound.**
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.google.protobuf.**
-dontwarn org.conscrypt.**
-dontwarn io.netty.handler.codec.marshalling.**
-dontwarn io.netty.handler.codec.protobuf.**
-dontwarn io.netty.handler.codec.xml.**
-dontwarn io.netty.channel.rxtx.**
-dontwarn io.netty.channel.sctp.**
-dontwarn io.netty.channel.udt.**
-dontwarn io.netty.handler.codec.spdy.**
-dontwarn io.netty.handler.codec.http2.HpackHuffmanEncoder
-dontwarn sun.misc.Unsafe
-dontwarn java.nio.Buffer
-dontwarn com.fasterxml.jackson.**
-dontwarn org.eclipse.jetty.**

# Keep Netty native classes if present
-keep class io.netty.handler.ssl.OpenSsl { *; }
-keep class io.netty.internal.tcnative.** { *; }
