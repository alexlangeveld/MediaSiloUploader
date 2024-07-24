# Keep the main class (replace with your actual main class)
-keep class nl.alexflix.mediasilouploader.Main {
    public static void main(java.lang.String[]);
}

# Keep all annotations
-keepattributes *Annotation*

# Keep all public classes, methods, and fields
-keep public class * {
    public *;
}

# General ProGuard optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
