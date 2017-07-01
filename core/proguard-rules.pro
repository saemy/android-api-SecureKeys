-keepclassmembers class android.content.Context {
    ** getPackageManager();
    ** getPackageName();
}

-keepclassmembers class android.content.pm.PackageManager {
    public static final int GET_SIGNATURES;
    ** getPackageInfo(java.lang.String, int);
    ** getInstallerPackageName(java.lang.String);
}

-keepclassmembers class android.content.pm.PackageInfo {
    ** signatures;
}

-keepclassmembers class android.content.pm.Signature {
    int hashCode();
}

-dontnote org.apache.http.**
-dontnote android.net.http.**
