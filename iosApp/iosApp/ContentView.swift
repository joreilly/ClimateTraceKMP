import UIKit
import SwiftUI
import ComposeApp

struct CountryListView: UIViewControllerRepresentable {
    let onCountryClicked: (Country) -> Void
    
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.CountryListViewController { country in
            onCountryClicked(country)
        }
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}


struct CountryInfoDetailedView: UIViewControllerRepresentable {
    let country: Country
    
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.CountryInfoDetailedViewController(country: country)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}


struct ContentView: View {
    @State private var navigateToDetailsScreen = false
    @State private var selectedCountry: Country? = nil
    
    var body: some View {
        
        NavigationView {
            VStack {
                CountryListView() { country in
                    selectedCountry = country
                    navigateToDetailsScreen = true
                }
                
                // TODO: cleaner way to do this (currently based on https://www.swiftbysundell.com/articles/swiftui-programmatic-navigation/)
                NavigationLink("Navigate to country details", isActive: $navigateToDetailsScreen) {
                    if let selectedCountry {
                        CountryInfoDetailedView(country: selectedCountry)
                    }
                }
                .hidden()
            }
            .navigationBarTitle("ClimateTrace", displayMode: .inline)
        }
    }
}



