# kotlinx.serialization and OkHttp ship their own consumer rules.
# OkHttp looks for optional TLS providers by name; they are not bundled.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
