# ClimateTraceKMP

![kotlin-version](https://img.shields.io/badge/kotlin-2.4.10-blue?logo=kotlin)

Kotlin/Compose Multiplatform project to show climate related emission data from https://climatetrace.org/data.

Running on
* iOS (SwiftUI + shared Compose Multiplatform UI)
* Android
* Desktop
* Web (Wasm)
* Kotlin Notebook
* MCP Server
* AI Agents (Koog + Google ADK)

The iOS client as mentioned includes shared Compose Multiplatform UI code.  It also includes option to use either SwiftUI or Compose code for the Country List screen (in both cases selecting a country will navigate to shared Compose emissions details screen).


<img width="669" alt="Screenshot 2024-04-29 at 21 13 54" src="https://github.com/joreilly/ClimateTraceKMP/assets/6302/d5d2a147-20f4-430b-9a14-17bc5526957e">



Related posts:
* [Kotlin MCP 💜 Kotlin Multiplatform](https://johnoreilly.dev/posts/kotlin-mcp-kmp/)
* [Initial exploration of using Koog for developing Kotlin based AI agents](https://johnoreilly.dev/posts/kotlin-koog/)
* [Using Google's Agent Development Kit for Java from Kotlin code](https://johnoreilly.dev/posts/kotlin-adk/)


## Project structure

* `composeApp` - shared Kotlin Multiplatform code (data layer, view models and Compose Multiplatform UI) along with desktop and web entry points
* `androidApp` - Android app
* `iosApp` - iOS app (SwiftUI + shared Compose UI)
* `mcp-server` - MCP server that exposes emissions data as MCP tools (using the [Kotlin MCP SDK](https://github.com/modelcontextprotocol/kotlin-sdk))
* `agents` - Kotlin based AI agent built using Google's [Agent Development Kit](https://github.com/google/adk-java) (ADK)

Uses, amongst other things, [Ktor](https://ktor.io/), [Koin](https://insert-koin.io/), [kstore](https://github.com/xxfast/KStore), [Voyager](https://github.com/adrielcafe/voyager), [KoalaPlot](https://github.com/KoalaPlot/koalaplot-core) and [Koog](https://github.com/JetBrains/koog).


## Building and running

* Android: open the project in Android Studio, or run `./gradlew :androidApp:installDebug`
* iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run
* Desktop: `./gradlew :composeApp:run`
* Web (Wasm): `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
* MCP server: `./gradlew :mcp-server:shadowJar` (see MCP Server section below)
* ADK agent Dev UI: `./gradlew :agents:startDevUI`


### Android (Compose)


<img width="672" height="1496" alt="Screenshot_20260524_101722" src="https://github.com/user-attachments/assets/db9a4f57-f1a0-40de-b82b-755ceb47e429" />



### iOS (SwiftUI/Compose)


<img width="603" height="1311" alt="Simulator Screenshot - iPhone 17 Pro - 2026-05-24 at 10 18 46" src="https://github.com/user-attachments/assets/8464442f-d35c-40fd-a6f2-ec8247feb926" />



### Compose for Desktop 



<img width="1084" height="701" alt="Screenshot 2026-05-24 at 10 11 18" src="https://github.com/user-attachments/assets/660aaff6-69e6-4f57-82d7-446b5f03d687" />
<img width="1084" height="701" alt="Screenshot 2026-05-24 at 10 12 37" src="https://github.com/user-attachments/assets/80d9c80b-69db-4133-be03-be4e55448056" />


### Compose for Web (Wasm)


<img width="1084" height="701" alt="Screenshot 2026-05-24 at 10 10 29" src="https://github.com/user-attachments/assets/91f36296-7514-4065-857c-8c59e1c8dd16" />


### Kotlin Notebook

See [ClimateTrace.ipynb](composeApp/ClimateTrace.ipynb)

<img width="694" alt="Screenshot 2023-12-14 at 20 33 45" src="https://github.com/joreilly/ClimateTraceKMP/assets/6302/82ed364a-0284-4e5c-b81e-40fdfc58f312">

### MCP Server

The `mcp-server` module uses the [Kotlin MCP SDK](https://github.com/modelcontextprotocol/kotlin-sdk) to expose an MCP tools endpoint (returning per country emission data) that
can for example be plugged in to Claude Desktop as shown below.  That module uses same KMP shared code.

<img width="1608" alt="Screenshot 2025-07-06 at 17 24 20" src="https://github.com/user-attachments/assets/7e4fd599-3ade-47b7-bbe3-a4f36148c170" />


To integrate the MCP server with Claude Desktop for example you need to firstly run gradle `shadowJar` task and then select "Edit Config" under Developer Settings and add something 
like the following (update with your path)

```
{
  "mcpServers": {
    "climatetrace": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/ClimateTraceKMP/mcp-server/build/libs/serverAll.jar",
        "--stdio"
      ]
    }
  }
}
```


### AI Agents

The app includes an Agents screen, built using [Koog](https://github.com/JetBrains/koog), that allows querying emissions data using an LLM based agent.

The `agents` module also includes a Kotlin based agent built using Google's [Agent Development Kit](https://github.com/google/adk-java) (ADK), making use of the same
shared KMP code to provide emissions data as a tool.  Its dev UI can be run using `./gradlew :agents:startDevUI`.


## Full set of Kotlin Multiplatform/Compose/SwiftUI samples

*  PeopleInSpace (https://github.com/joreilly/PeopleInSpace)
*  GalwayBus (https://github.com/joreilly/GalwayBus)
*  Confetti (https://github.com/joreilly/Confetti)
*  BikeShare (https://github.com/joreilly/BikeShare)
*  FantasyPremierLeague (https://github.com/joreilly/FantasyPremierLeague)
*  ClimateTrace (https://github.com/joreilly/ClimateTraceKMP)
*  GeminiKMP (https://github.com/joreilly/GeminiKMP)
*  MortyComposeKMM (https://github.com/joreilly/MortyComposeKMM)
*  StarWars (https://github.com/joreilly/StarWars)
*  WordMasterKMP (https://github.com/joreilly/WordMasterKMP)
*  Chip-8 (https://github.com/joreilly/chip-8)
