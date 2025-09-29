import UIKit
import SwiftUI
import ComposeApp
import KMPObservableViewModelCore
import KMPObservableViewModelSwiftUI


struct ContentView: View {
    @State private var navigateToDetailsScreen = false
    @State private var selectedCountry: Country? = nil
    
    var body: some View {
        TabView {
            CountryListView()
                .tabItem {
                    Label("Climate", systemImage: "globe")
                }
            AgentsView()
                .tabItem {
                    Label("Agents", systemImage: "tree")
                }
        }
    }
}

struct AgentsView: View {
    var body: some View {
        AgentViewShared()
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
    @StateViewModel var viewModel = CountryListViewModel()
    @State var query: String = ""
    
    var body: some View {
        
        switch(viewModel.viewState) {
         
        case is CountryListUIState.Loading:
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(1.5)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case is CountryListUIState.Error:  Text("Error")
        case let state as CountryListUIState.Success:
            
            NavigationView {
                ZStack {
                    List {
                        ForEach(state.countryList.filter { query.isEmpty || $0.name.contains(query) }, id: \.self) { country in
                            NavigationLink(destination: CountryInfoDetailedViewShared(country: country)) {
                                HStack {
                                    Text(country.name).font(.headline)
                                }
                            }
                        }
                    }
                    .searchable(text: $query)
                    .disableAutocorrection(true)

                    if state.countryList.filter({ query.isEmpty || $0.name.contains(query) }).isEmpty {
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
        default: EmptyView()
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
        MainViewControllerKt.CountryInfoDetailedViewController(country: country)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct AgentViewShared: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.AgentViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
