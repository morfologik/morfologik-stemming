
-injar commons-cli-1.2.jar(!META-INF/**)
-injar commons-lang-2.6.jar(!META-INF/**)
-injar hppc-0.3.3.jar(!META-INF/**)
-injar morfologik-fsa-1.5.2-SNAPSHOT.jar(!META-INF/**)
-injar morfologik-stemming-1.5.2-SNAPSHOT.jar(!META-INF/**)
-injar morfologik-polish-1.5.2-SNAPSHOT.jar(!META-INF/**)
-injar morfologik-tools-1.5.2-SNAPSHOT.jar

-libraryjar c:\java\jdk\jre\lib\rt.jar(java/**)

-keep class morfologik.** {
    <methods>; <fields>;
}

-repackageclasses morfologik.dependencies
-dontnote

-outjar morfologik-tools-all.jar