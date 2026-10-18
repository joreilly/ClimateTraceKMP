import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    
	init() {
		di.initKoin()
	}
    
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
