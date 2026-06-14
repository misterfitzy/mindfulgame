# MindfulGame

Native Android game implementing real-time Navier-Stokes fluid dynamics simulation.

## Physics Engine

Full 2D Navier-Stokes solver on a 128x96 grid, based on Jos Stam's "Stable Fluids" (1999):

- **Semi-Lagrangian advection** with bilinear interpolation for numerical stability
- **Gauss-Seidel pressure projection** (10 iterations) enforcing incompressibility
- **Vorticity confinement** preserving turbulent structures
- **5-octave multi-scale noise injection** with random micro-vortex spawning
- **Adaptive resolution** across 4 tiers (3K-28K cells) with FPS-based auto-scaling
- **Per-pixel rendering** with 4-sample MSAA anti-aliasing

## Cross-Platform Development

Python prototypes (matplotlib/numpy) for offline algorithm debugging before Kotlin port to Android.

## Tech Stack

Kotlin, Android SDK, computational physics, numerical methods, real-time rendering

## Project Structure

- `app/` — Android application with physics engine (`FluidDynamicsEngine.kt`)
- `specs_and_python_tests/` — Python prototypes for algorithm validation