# Mindful Game - MVP

A minimalistic Android game that captures the essence of a dream-like state where two rectangles (white and black) mix like fluids. The player's goal is to restore the boundary between them using intuitive touch controls.

## 🎮 Game Concept

- **Initial State**: Two rectangles, one solid white (left) and one solid black (right), with a clear boundary
- **Dynamic Mixing**: The boundary becomes noisy over time, simulating fluid-like mixing
- **Objective**: Use touch and drag gestures to restore the boundary to its original state
- **Experience**: Minimalistic design focused on creating a mindful and immersive experience

## 🏗️ Architecture

This MVP follows a **modular design** that makes it trivial to swap physics engines:

### Core Interfaces
- `IPhysicsEngine` - Abstraction for boundary simulation and interaction
- `IBoundaryRenderer` - Abstraction for rendering approaches  
- `ITouchProcessor` - Abstraction for touch input handling

### Current MVP Implementation
- **Physics**: `NoiseBasedEngine` - Procedural noise-based fluid simulation
- **Rendering**: `SimpleRenderer` - Canvas-based drawing
- **Input**: `TouchProcessor` - Multi-touch gesture handling

### Easy Physics Engine Swapping
```kotlin
// Current MVP
val physicsEngine: IPhysicsEngine = NoiseBasedEngine()

// Future upgrade (one line change)
val physicsEngine: IPhysicsEngine = LiquidFunEngine()
// or
val physicsEngine: IPhysicsEngine = CustomShaderEngine()
```

## 📁 Project Structure

```
com.mindful.game/
├── core/
│   ├── interfaces/          # Core abstractions
│   │   ├── IPhysicsEngine.kt
│   │   ├── IBoundaryRenderer.kt
│   │   └── ITouchProcessor.kt
│   └── GameEngine.kt        # Orchestrates all components
├── physics/
│   └── NoiseBasedEngine.kt  # MVP physics implementation
├── rendering/
│   └── SimpleRenderer.kt    # MVP renderer
├── input/
│   └── TouchProcessor.kt    # Touch input handling
├── audio/
│   └── AudioManager.kt      # Audio feedback (placeholder)
└── ui/
    ├── GameView.kt          # Custom SurfaceView
    └── MainActivity.kt      # Main activity
```

## 🚀 Features

- ✅ **Fluid Boundary Simulation** - Noise-based boundary degradation over time
- ✅ **Touch Controls** - Multi-touch restoration with pressure sensitivity
- ✅ **Visual Feedback** - Real-time progress tracking and smooth animations
- ✅ **Performance Optimized** - 60 FPS with SurfaceView rendering
- ✅ **Modular Design** - Easy component swapping for future enhancements
- ✅ **Quality Settings** - Adjustable rendering quality for different devices
- ✅ **Touch Sensitivity** - Configurable touch input sensitivity

## 🛠️ Requirements

- **Android API Level**: 24+ (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Language**: Kotlin
- **Build System**: Gradle with Android Plugin 8.2.0

## 📱 Installation

### Windows Users (Recommended)
1. **Follow the detailed [Windows Setup Guide](WINDOWS_SETUP_GUIDE.md)**
2. **Use the Windows build script**: Double-click `build.bat` or run `build.bat studio`

### General Installation
1. **Clone the repository**
2. **Open in Android Studio**
3. **Sync project with Gradle files**
4. **Run on device or emulator**

### Quick Start Commands

**Windows:**
```cmd
# Open in Android Studio
build.bat studio

# Build and run
build.bat run

# View logs
build.bat logs
```

**Linux/Mac:**
```bash
# Build and run
./build.sh run

# View logs
./build.sh logs
```

## 🎯 MVP Scope

This MVP demonstrates the core gameplay mechanics with a simplified but functional implementation:

### Included in MVP
- Basic fluid simulation using procedural noise
- Touch-based boundary restoration
- Progress tracking and visual feedback
- Smooth 60fps performance
- Modular architecture for easy upgrades

### Post-MVP Enhancements
- Real fluid physics (LiquidFun integration)
- Particle-based rendering
- Advanced shader effects
- Audio feedback with actual sound files
- Multiple difficulty levels
- Settings persistence

## 🔧 Physics Engine Migration

The modular design allows easy migration to more sophisticated physics engines:

### 1. LiquidFun Integration
```kotlin
class LiquidFunEngine : IPhysicsEngine {
    // Implement using Google's LiquidFun library
    // For real fluid dynamics simulation
}
```

### 2. Custom Shader Engine
```kotlin
class CustomShaderEngine : IPhysicsEngine {
    // GPU-accelerated fluid simulation
    // Using OpenGL ES compute shaders
}
```

### 3. Hybrid Approach
```kotlin
class HybridEngine : IPhysicsEngine {
    // Combine multiple approaches
    // Fallback system based on device capabilities
}
```

## 🎨 Rendering Upgrades

Similarly, rendering can be enhanced:

### Particle Renderer
```kotlin
class ParticleRenderer : IBoundaryRenderer {
    // Particle-based fluid visualization
    // Thousands of animated particles
}
```

### OpenGL Renderer
```kotlin
class OpenGLRenderer : IBoundaryRenderer {
    // Hardware-accelerated rendering
    // Custom shaders and effects
}
```

## 🎵 Audio Implementation

The AudioManager is currently a placeholder. Future implementation:

```kotlin
// Add actual audio files to res/raw/
touchSoundId = soundPool?.load(context, R.raw.touch_sound, 1)
progressSoundId = soundPool?.load(context, R.raw.progress_sound, 1)
completionSoundId = soundPool?.load(context, R.raw.completion_sound, 1)
```

## 🧪 Testing

The modular design facilitates easy testing:

```kotlin
// Mock physics engine for testing
class MockPhysicsEngine : IPhysicsEngine {
    // Predictable behavior for unit tests
}

// Test game engine with mock components
val gameEngine = GameEngine(
    MockPhysicsEngine(),
    MockRenderer(),
    MockTouchProcessor()
)
```

## 📈 Performance

- **Target**: 60 FPS on mid-range devices
- **Optimization**: SurfaceView with dedicated game thread
- **Quality Settings**: Adjustable rendering fidelity
- **Memory**: Efficient object pooling and reuse

## 🚀 Future Roadmap

1. **Phase 2**: LiquidFun physics integration
2. **Phase 3**: Advanced rendering with OpenGL
3. **Phase 4**: Audio implementation and sound design
4. **Phase 5**: Additional game modes and difficulty levels
5. **Phase 6**: Performance optimization and device compatibility

## 🤝 Contributing

The modular architecture makes it easy to contribute:

1. **Physics Engines**: Implement `IPhysicsEngine`
2. **Renderers**: Implement `IBoundaryRenderer`  
3. **Input Systems**: Implement `ITouchProcessor`
4. **Audio**: Enhance `AudioManager`

## 📄 License

This project serves as a reference implementation for modular game architecture and physics engine abstraction in Android development.
