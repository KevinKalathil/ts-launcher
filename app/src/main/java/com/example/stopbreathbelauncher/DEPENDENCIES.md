# Dependencies to add to build.gradle (app module)

## DataStore
implementation "androidx.datastore:datastore-preferences:1.1.1"

## Already in your boilerplate (confirm these are present):
implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3"
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"

## Java 8+ time API (LocalDate etc) — add to android block if not present:
# android {
#     ...
#     compileOptions {
#         coreLibraryDesugaringEnabled true
#         sourceCompatibility JavaVersion.VERSION_1_8
#         targetCompatibility JavaVersion.VERSION_1_8
#     }
# }
# dependencies {
#     coreLibraryDesugaring 'com.android.tools.build:desugaring:2.0.4'
# }
