Kotlin/Compose Multiplatform project to show data from https://climatetrace.org/data.

Running on
* iOS
* Android
* Desktop
* Web

Development has only just started on the project and making it public at this point as reproducer for issue reported in https://kotlinlang.slack.com/archives/CDFP59223/p1701968310319679

<br>
Code uses Ktor to request data from remote endpoint and includes basic Compose Multiplatform UI (showing for Web and Desktop below).  Same Compose code also
runs on Android but needs to be updated to reflect screen size etc.  Likewise for iOS client once issue above is resolved.


<img width="1193" alt="Screenshot 2023-12-08 at 22 14 08" src="https://github.com/joreilly/ClimateTraceKMP/assets/6302/fecd89ee-d9b9-48ff-b8c6-c4af49b6d0d7">

<img width="1191" alt="Screenshot 2023-12-08 at 22 14 13" src="https://github.com/joreilly/ClimateTraceKMP/assets/6302/709e2722-5ca4-4c19-ae0a-7ca18d0170f2">
