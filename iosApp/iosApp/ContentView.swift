import UIKit
import SwiftUI
import ComposeApp
import KMPObservableViewModelCore
import KMPObservableViewModelSwiftUI


struct ContentView: View {
    @State private var navigateToDetailsScreen = false
    @State private var selectedCountry: Country? = nil
    
    var body: some View {
        CountryListView()
    }
}

// Comment in following if using shared Compose code for country list as well
/*
struct ContentView: View {
    @State private var navigateToDetailsScreen = false
    @State private var selectedCountry: Country? = nil
    
    var body: some View {
        
        NavigationView {
            VStack {
                //CountryListView()
                
                CountryListViewShared() { country in
                    selectedCountry = country
                    navigateToDetailsScreen = true
                }

                // TODO: cleaner way to do this (currently based on https://www.swiftbysundell.com/articles/swiftui-programmatic-navigation/)
                NavigationLink("Navigate to country details", isActive: $navigateToDetailsScreen) {
                    if let selectedCountry {
                        CountryInfoDetailedViewShared(country: selectedCountry)
                    }
                }
                .hidden()
 
            }
            .navigationBarTitle("ClimateTrace", displayMode: .inline)
        }
    }
}
*/


struct CountryListView: View {
    @StateViewModel var viewModel = ClimateTraceViewModel()
    @State var query: String = ""
    
    var body: some View {
            NavigationView {
                ZStack {
                    List {
                        ForEach(viewModel.countryList.filter { query.isEmpty || $0.name.contains(query) }, id: \.self) { country in
                            NavigationLink(destination: CountryInfoDetailedViewShared(country: country)) {
                                HStack {
                                    Text(country.name).font(.headline)
                                }
                            }
                        }
                    }
                    .searchable(text: $query)
                    .disableAutocorrection(true)

                    // Conditional display of the ProgressView
                    if let isLoading = viewModel.isLoadingCountries.value_ as? Bool, isLoading {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle())
                            .scaleEffect(1.5)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    } else if viewModel.countryList.filter({ query.isEmpty || $0.name.contains(query) }).isEmpty {
                        // Conditional display of text when query result is empty from search bar
                        Text("No Countries Found!")
                            .font(.headline)
                            .foregroundColor(.gray)
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .principal) {
                        VStack {
                            Text("ClimateTrace").font(.headline)
                        }
                    }
                }
            }
        }
}



struct CountryListViewShared: UIViewControllerRepresentable {
    let onCountryClicked: (Country) -> Void
    
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.CountryListViewController { country in
            onCountryClicked(country)
        }
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct CountryInfoDetailedViewShared: UIViewControllerRepresentable {
    let country: Country
    
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.CountryInfoDetailedViewController(country: country, year: "2022")
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
