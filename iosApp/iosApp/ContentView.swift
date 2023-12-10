import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.CountryListViewController { countryCode in
            print(countryCode)
            
            // TODO navigate to country emissions detail screen
        }
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        
        NavigationView {
            VStack {
                ComposeView()
            }
            .navigationBarTitle("ClimateTrace", displayMode: .inline)
        }
    }
}



