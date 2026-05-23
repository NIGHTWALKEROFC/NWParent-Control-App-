# PATH: nw-parent-app/app/proguard-rules.pro
-keep class com.nw.parentalcontrol.data.** { *; }
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes Signature
-keepattributes *Annotation*