import SwiftUI
import os.log
import composeApp

@main
struct FinutsAppSwift: SwiftUI.App {

    private static let logger = Logger(
        subsystem: "com.finuts.app",
        category: "AppLifecycle"
    )

    init() {
        Self.logger.info("FinutsApp initializing...")

        // Initialize Kotlin/Compose app FIRST
        // This sets up Kermit logging and Koin DI
        Self.logger.info("Initializing IosEntry (Kotlin)...")
        IosEntry.shared.initialize()
        Self.logger.info("IosEntry initialized")

        // Register LLM bridge for on-device inference
        // This must be done before GGUFInferenceEngine is used
        Self.logger.info("Registering SwiftLLMBridge...")
        let bridge = SwiftLLMBridge()
        LLMBridgeRegistry.shared.register(bridge: bridge)

        let isRegistered = LLMBridgeRegistry.shared.isRegistered()
        Self.logger.info("LLMBridge registered: \(isRegistered)")

        Self.logger.info("FinutsApp initialization complete")
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
