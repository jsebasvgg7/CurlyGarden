-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.curlygarden.app.push.CurlyMessagingService { *; }
-keep class com.curlygarden.app.push.ElevenReminderReceiver { *; }
-keep class com.curlygarden.app.push.BootReceiver { *; }
