import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    
	init() {
		Koin_iosKt.doInitKoin()
	}
    
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
