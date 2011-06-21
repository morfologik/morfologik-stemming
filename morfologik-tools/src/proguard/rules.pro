
-dontnote
-dontoptimize
-dontwarn

-renamepackage org.apache=>morfologik.dependencies
-renamepackage com.carrotsearch=>morfologik.dependencies
-repackageclasses morfologik.dependencies

-keep class morfologik.** {
    <methods>; <fields>;
}

-dontnote

-libraryjars <java.home>/lib/rt.jar(java/**)
