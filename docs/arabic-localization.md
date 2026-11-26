# Arabic localization

Arabic is the default display language on this branch. The LunaMacro brand and technical names such as Shizuku, DeepSeek and API identifiers are preserved.

Scope: Compose screens, onboarding, system labels, floating overlays, permission and privacy text, schedules, and Genshin data collection display labels.

Display-only adapters preserve the original internal values used for screen type annotations, collection scene keys, schedule serialization, Stellar result checks, and game names. No changes to the game, input, model, macro, or accessibility engine sources are intended. Chinese wake-word examples, model prompts, stored/custom data, log output and arbitrary remote responses remain in their original language. Arabic display does not establish Arabic voice recognition or Arabic instruction support.

Original layout direction is retained rather than globally mirroring coordinate-sensitive UI. The debug APK uses the upstream build configuration and may be signed differently from official releases.

Build validation: GitHub Actions runs on pushes to arabic-localization. Device-level validation is still needed.
