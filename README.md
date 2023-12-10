Kotlin/Compose Multiplatform project to show data from https://climatetrace.org/data. Development just started so very much work in progress right now!

Running on
* iOS 
* Android
* Desktop
* Web (Wasm)


Code is very minimal right now with Compose code using remote apis directly. Will choose/add persistence library once those libraries support Wasm along 
with associated Repository. Also need to add some sort of shared view model/state holder. Have started using Voyager for navigation in the Android client
and need to evaulate whether this could/should be used for other clients (again needs Wasm support first).


### Android (Compose)


![Screenshot_20231210_112724](https://github.com/joreilly/ClimateTraceKMP/assets/6302/20a6ffef-1ce5-4580-a086-8027598a8ded)



### iOS (SwiftUI/Compose)

![Simulator Screenshot - iPad Pro (11-inch) (4th generation) - 2023-12-09 at 15 16 08](https://github.com/joreilly/ClimateTraceKMP/assets/6302/2225be51-2eba-4e48-977d-4d8c29bae361)


### Compose for Desktop 

<img width="1163" alt="Screenshot 2023-12-09 at 14 32 58" src="https://github.com/joreilly/ClimateTraceKMP/assets/6302/442ff726-9702-4dbc-8643-601c5b01abee">


### Compose for Web (Wasm)


<img width="1159" alt="Screenshot 2023-12-09 at 14 31 50" src="https://github.com/joreilly/ClimateTraceKMP/assets/6302/23087c01-82a4-4156-ae11-4ec9e03e7934">
