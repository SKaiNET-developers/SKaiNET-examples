# Material Design 3 Implementation

This package implements Material Design 3 (Material You) for the MNIST Demo application. It provides a comprehensive theming system with the following features:

## Features

### 1. Custom Color System with Light/Dark Theme Support
- Predefined light and dark color schemes in `Color.kt`
- Semantic color naming following Material Design 3 guidelines
- Complete set of surface, container, and content colors

### 2. Dynamic Theming Based on Platform Capabilities
- Android: Uses Material You dynamic colors on Android 12+ devices
- Desktop and Web: Falls back to predefined color schemes
- Extensible architecture for future platform support

### 3. Consistent Typography Scale
- Complete Material Design 3 typography scale in `Typography.kt`
- Includes display, headline, title, body, and label text styles
- Proper scaling and spacing for all text components

### 4. Elevation and Shape System
- Consistent shape system with rounded corners in `Shape.kt`
- Different corner radii for different component sizes
- Support for Material Design 3 elevation through surface tinting

## Usage

### Basic Usage
Wrap your composables with the `AppTheme` composable:

```kotlin
AppTheme {
    // Your UI components here
}
```

### Using with Surfaces
For screens and large containers, use the `AppSurface` composable:

```kotlin
AppTheme {
    AppSurface {
        // Your screen content here
    }
}
```

### Accessing Theme Properties
Access theme properties through the MaterialTheme object:

```kotlin
Text(
    text = "Example Text",
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurface
)

Button(
    onClick = { /* action */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
) {
    Text("Button Text")
}
```

### Dynamic Color Information
You can check if dynamic colors are supported and enabled:

```kotlin
val dynamicColorInfo = MaterialTheme.dynamicColorInfo
if (dynamicColorInfo.isDynamicColorSupported) {
    // Device supports dynamic colors
}
```

## Platform-Specific Considerations

### Android
- Dynamic colors are automatically enabled on Android 12+ (API level 31+)
- Falls back to predefined color schemes on older Android versions

### Desktop
- Uses predefined color schemes
- Respects system light/dark mode settings

### Web (WebAssembly)
- Uses predefined color schemes
- Respects system light/dark mode settings where available