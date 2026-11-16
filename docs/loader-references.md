# Verified expressive references

MetrolistGroup/Metrolist main: androidApp/src/main/kotlin/com/metrolist/android/ui/components/MiniPlayer.kt uses androidx Material3 LoadingIndicator (32dp); ProviderLoginShell.kt uses ContainedLoadingIndicator. SameeraSW/essentials main: MainActivity.kt and AppUpdatesActivity.kt use Material3 LoadingIndicator. Sources inspected from GitHub source archives; no app code copied.

AndroidX LoadingIndicator.kt: https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/LoadingIndicator.kt
Shape sequence SoftBurst, Cookie9Sided, Pentagon, Pill, Sunny, Cookie4Sided, Oval; morph interval 650ms, global rotation 4666ms; spring damping 0.6 stiffness 200. Current lightweight Canvas renderer approximates these shapes and motion; it is NOT the actual Compose component nor a pixel-identical implementation. No central dot. AndroidX Apache 2.0 source consulted, no Metrolist GPL source copied.
