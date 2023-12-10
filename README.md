Kotlin/Compose Multiplatform project to show climate related emission data from https://climatetrace.org/data. Development just started so very much work in progress right now!
Have started with showing sector emission data per country but ton of other info available as well (ideas very welcome!).

Running on
* iOS 
* Android
* Desktop
* Web (Wasm)


Code is very minimal right now with Compose code using remote apis directly. Will choose/add persistence library once those libraries support Wasm along 
with associated Repository. Also need to add some sort of shared view model/state holder. Have started using Voyager for navigation in the Android client
and need to evaulate whether this could/should be used for other clients (again needs Wasm support first).


### Android (Compose)


![Screenshot_20231210_180223](https://github.com/joreilly/ClimateTraceKMP/assets/6302/7ae517ec-ef48-4f85-a267-5b4bdef2e25f)



### iOS (SwiftUI/Compose)

![Simulator Screenshot - iPhone 15 Pro - 2023-12-10 at 19 31 59](https://github.com/joreilly/ClimateTraceKMP/assets/6302/ed0f6b1c-ce30-4f99-98d5-9bbdae49bcd3)




### Compose for Desktop 

<img width="1157" alt="Screenshot 2023-12-10 at 18 01 30" src="https://github.com/joreilly/ClimateTraceKMP/assets/6302/dc60bd59-789d-47a5-aa9c-02a636735f58">


### Compose for Web (Wasm)

<img width="1157" alt="Screenshot 2023-12-10 at 18 00 16" src="https://github.com/joreilly/ClimateTraceKMP/assets/6302/3cb377c1-16a3-4250-8e98-b8e033d38db3">

