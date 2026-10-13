-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

-keep class com.music4music.tvguide.data.local.entity.** { *; }
-keep class com.music4music.tvguide.domain.model.** { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
