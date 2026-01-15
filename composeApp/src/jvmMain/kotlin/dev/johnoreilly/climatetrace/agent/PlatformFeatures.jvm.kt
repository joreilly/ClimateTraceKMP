package dev.johnoreilly.climatetrace.agent

import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.features.opentelemetry.attribute.CustomAttribute
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter


//actual fun GraphAIAgent.FeatureContext.installPlatformAgentFeatures() {
//
//    install(OpenTelemetry) {
//        setVerbose(true)
//        addLangfuseExporter(
//            langfusePublicKey = "pk-lf-4c29f91e-3351-4c75-bdd5-46dcd0ce9f82",
//            langfuseSecretKey = "sk-lf-7aeaefcc-5aa9-4ece-8eb0-af67cc0b544f",
//        )
//    }
//}


/*
// TODO add to local.properties and use BuildKonfig

LANGFUSE_SECRET_KEY = "sk-lf-7aeaefcc-5aa9-4ece-8eb0-af67cc0b544f"
LANGFUSE_PUBLIC_KEY = "pk-lf-4c29f91e-3351-4c75-bdd5-46dcd0ce9f82"
LANGFUSE_BASE_URL = "https://cloud.langfuse.com"
 */
