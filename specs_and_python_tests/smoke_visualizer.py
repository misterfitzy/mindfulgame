"""
Smoke Algorithm Visualization and Optimization Tool
Interactive visualization and optimization testing for the smoke simulation
"""

import numpy as np
import matplotlib.pyplot as plt
import matplotlib.animation as animation
from matplotlib.widgets import Slider, Button
import time
from smoke_debug import FluidDynamicsEngine, FluidRenderer, ResolutionMode, PerformanceMonitor

class SmokeVisualizer:
    """Interactive visualization tool for smoke simulation debugging"""
    
    def __init__(self, width=128, height=96):
        self.engine = FluidDynamicsEngine(width, height)
        self.renderer = FluidRenderer()
        
        # Setup matplotlib figure
        self.fig, (self.ax_main, self.ax_perf) = plt.subplots(1, 2, figsize=(16, 8))
        
        # Main simulation display
        self.ax_main.set_title('Smoke Simulation')
        self.ax_main.set_aspect('equal')
        
        # Performance display
        self.ax_perf.set_title('Performance Metrics')
        self.ax_perf.set_xlabel('Frame')
        self.ax_perf.set_ylabel('Time (ms)')
        
        # Initialize plots
        self.image_data = np.zeros((height//2, width//2, 3), dtype=np.uint8)
        self.im = self.ax_main.imshow(self.image_data, origin='lower')
        
        # Performance tracking
        self.frame_times = []
        self.component_times = {
            'turbulence_injection': [],
            'velocity_step': [],
            'color_step': [],
            'rendering': []
        }
        
        # Animation control
        self.is_running = True
        self.frame_count = 0
        
        # Create controls
        self.setup_controls()
        
    def setup_controls(self):
        """Setup interactive controls"""
        # Add sliders for parameters
        plt.subplots_adjust(bottom=0.25)
        
        # Viscosity slider
        ax_viscosity = plt.axes([0.2, 0.1, 0.3, 0.03])
        self.viscosity_slider = Slider(ax_viscosity, 'Viscosity', 0.0001, 0.01, 
                                      valinit=self.engine.viscosity, valfmt='%.4f')
        
        # Vorticity slider
        ax_vorticity = plt.axes([0.2, 0.05, 0.3, 0.03])
        self.vorticity_slider = Slider(ax_vorticity, 'Vorticity', 0.0, 2.0, 
                                      valinit=self.engine.vorticity, valfmt='%.2f')
        
        # Reset button
        ax_reset = plt.axes([0.6, 0.1, 0.1, 0.04])
        self.reset_button = Button(ax_reset, 'Reset')
        
        # Pause button
        ax_pause = plt.axes([0.6, 0.05, 0.1, 0.04])
        self.pause_button = Button(ax_pause, 'Pause')
        
        # Connect events
        self.viscosity_slider.on_changed(self.update_viscosity)
        self.vorticity_slider.on_changed(self.update_vorticity)
        self.reset_button.on_clicked(self.reset_simulation)
        self.pause_button.on_clicked(self.toggle_pause)
        
        # Mouse interaction
        self.fig.canvas.mpl_connect('button_press_event', self.on_mouse_press)
        
    def update_viscosity(self, val):
        """Update viscosity parameter"""
        self.engine.viscosity = val
        
    def update_vorticity(self, val):
        """Update vorticity parameter"""
        self.engine.vorticity = val
        
    def reset_simulation(self, event):
        """Reset simulation state"""
        self.engine.reset()
        self.frame_count = 0
        self.frame_times.clear()
        for key in self.component_times:
            self.component_times[key].clear()
        
    def toggle_pause(self, event):
        """Toggle simulation pause"""
        self.is_running = not self.is_running
        self.pause_button.label.set_text('Resume' if not self.is_running else 'Pause')
        
    def on_mouse_press(self, event):
        """Handle mouse press for touch interaction"""
        if event.inaxes == self.ax_main and event.button == 1:  # Left click
            # Convert screen coordinates to simulation coordinates
            x = event.xdata / self.image_data.shape[1] if event.xdata else 0.5
            y = event.ydata / self.image_data.shape[0] if event.ydata else 0.5
            
            # Apply touch input
            self.engine.apply_touch_input(x, y, 1.0)
            
    def animate(self, frame):
        """Animation update function"""
        if not self.is_running:
            return [self.im]
            
        start_time = time.perf_counter()
        
        # Update simulation
        delta_time = 1.0 / 30.0  # 30 FPS target
        self.engine.update_boundary(delta_time)
        
        # Render
        render_start = time.perf_counter()
        self.image_data = self.renderer.render_to_array(
            self.engine, 
            self.engine.grid_width // 2, 
            self.engine.grid_height // 2
        )
        render_time = (time.perf_counter() - render_start) * 1000
        
        # Update image
        self.im.set_array(self.image_data)
        
        # Track performance
        frame_time = (time.perf_counter() - start_time) * 1000
        self.frame_times.append(frame_time)
        self.component_times['rendering'].append(render_time)
        
        # Get component times from engine
        monitor = self.engine.monitor
        for component in ['turbulence_injection', 'velocity_step', 'color_step']:
            history_key = f"{component}_history"
            if history_key in monitor.component_times and monitor.component_times[history_key]:
                latest_time = monitor.component_times[history_key][-1] * 1000
                self.component_times[component].append(latest_time)
        
        # Update performance plot every 10 frames
        if self.frame_count % 10 == 0:
            self.update_performance_plot()
            
        self.frame_count += 1
        
        # Update title with FPS
        fps = monitor.get_average_fps()
        self.ax_main.set_title(f'Smoke Simulation - FPS: {fps:.1f}')
        
        return [self.im]
        
    def update_performance_plot(self):
        """Update performance metrics plot"""
        self.ax_perf.clear()
        
        if len(self.frame_times) > 1:
            frames = list(range(len(self.frame_times)))
            
            # Plot total frame time
            self.ax_perf.plot(frames, self.frame_times, 'b-', label='Total Frame', linewidth=2)
            
            # Plot component times
            colors = ['r-', 'g-', 'm-', 'c-']
            for i, (component, times) in enumerate(self.component_times.items()):
                if times and len(times) == len(frames):
                    self.ax_perf.plot(frames, times, colors[i % len(colors)], 
                                    label=component.replace('_', ' ').title(), alpha=0.7)
            
            self.ax_perf.set_xlabel('Frame')
            self.ax_perf.set_ylabel('Time (ms)')
            self.ax_perf.set_title('Performance Metrics')
            self.ax_perf.legend()
            self.ax_perf.grid(True, alpha=0.3)
            
            # Add target frame time line (33.33ms for 30 FPS)
            self.ax_perf.axhline(y=33.33, color='orange', linestyle='--', 
                               label='30 FPS Target', alpha=0.8)
        
    def run(self):
        """Start the interactive visualization"""
        # Create animation
        ani = animation.FuncAnimation(self.fig, self.animate, interval=33, blit=False, cache_frame_data=False)
        
        # Show plot
        plt.show()
        
        return ani

class OptimizationTester:
    """Tool for testing different optimization strategies"""
    
    def __init__(self):
        self.results = {}
        
    def test_resolution_scaling(self):
        """Test performance at different resolutions"""
        print("=== Testing Resolution Scaling ===")
        
        resolutions = [
            ("Tiny", 32, 24),
            ("Small", 64, 48), 
            ("Medium", 96, 72),
            ("Large", 128, 96),
            ("XLarge", 160, 120)
        ]
        
        results = {}
        
        for name, width, height in resolutions:
            print(f"\nTesting {name} ({width}x{height})...")
            
            engine = FluidDynamicsEngine(width, height)
            
            # Run for 60 frames
            times = []
            for frame in range(60):
                start = time.perf_counter()
                engine.update_boundary(1/60)
                times.append(time.perf_counter() - start)
            
            avg_time = sum(times) * 1000 / len(times)  # Convert to ms
            fps = 1000 / avg_time
            
            results[name] = {
                'resolution': (width, height),
                'avg_frame_time_ms': avg_time,
                'fps': fps,
                'cells': width * height
            }
            
            print(f"  Average frame time: {avg_time:.2f}ms")
            print(f"  FPS: {fps:.1f}")
            
        self.results['resolution_scaling'] = results
        return results
        
    def test_solver_iterations(self):
        """Test performance with different solver iteration counts"""
        print("\n=== Testing Solver Iterations ===")
        
        original_engine = FluidDynamicsEngine(128, 96)
        
        # Test different iteration counts for diffusion and pressure
        iteration_configs = [
            ("Very Low", 1, 3),
            ("Low", 2, 5), 
            ("Medium", 4, 10),  # Original
            ("High", 6, 15),
            ("Very High", 8, 20)
        ]
        
        results = {}
        
        for name, diffusion_iters, pressure_iters in iteration_configs:
            print(f"\nTesting {name} (diff:{diffusion_iters}, press:{pressure_iters})...")
            
            engine = FluidDynamicsEngine(128, 96)
            
            # Monkey patch to change iteration counts
            original_diffuse = engine.diffuse
            original_project = engine.project
            
            def modified_diffuse(field, prev, diffusion_rate, delta_time):
                a = delta_time * diffusion_rate * engine.grid_width * engine.grid_height
                
                for iteration in range(diffusion_iters):  # Modified iteration count
                    for y in range(1, engine.grid_height - 1):
                        for x in range(1, engine.grid_width - 1):
                            index = engine.get_index(x, y)
                            neighbors_sum = (field[engine.get_index(x - 1, y)] + 
                                           field[engine.get_index(x + 1, y)] +
                                           field[engine.get_index(x, y - 1)] + 
                                           field[engine.get_index(x, y + 1)])
                            field[index] = (prev[index] + a * neighbors_sum) / (1 + 4 * a)
                    engine.set_boundary(field)
            
            def modified_project():
                # Calculate divergence (same as original)
                for y in range(1, engine.grid_height - 1):
                    for x in range(1, engine.grid_width - 1):
                        index = engine.get_index(x, y)
                        engine.divergence[index] = -0.5 * (
                            engine.velocity_x[engine.get_index(x + 1, y)] - engine.velocity_x[engine.get_index(x - 1, y)] +
                            engine.velocity_y[engine.get_index(x, y + 1)] - engine.velocity_y[engine.get_index(x, y - 1)]
                        ) / engine.grid_width
                        engine.pressure[index] = 0.0
                
                engine.set_boundary(engine.divergence)
                engine.set_boundary(engine.pressure)
                
                # Solve for pressure with modified iteration count
                for iteration in range(pressure_iters):
                    for y in range(1, engine.grid_height - 1):
                        for x in range(1, engine.grid_width - 1):
                            index = engine.get_index(x, y)
                            neighbors_sum = (engine.pressure[engine.get_index(x - 1, y)] + 
                                           engine.pressure[engine.get_index(x + 1, y)] +
                                           engine.pressure[engine.get_index(x, y - 1)] + 
                                           engine.pressure[engine.get_index(x, y + 1)])
                            engine.pressure[index] = (engine.divergence[index] + neighbors_sum) / 4.0
                    engine.set_boundary(engine.pressure)
                
                # Subtract pressure gradient (same as original)
                for y in range(1, engine.grid_height - 1):
                    for x in range(1, engine.grid_width - 1):
                        index = engine.get_index(x, y)
                        engine.velocity_x[index] -= 0.5 * (engine.pressure[engine.get_index(x + 1, y)] - 
                                                         engine.pressure[engine.get_index(x - 1, y)]) * engine.grid_width
                        engine.velocity_y[index] -= 0.5 * (engine.pressure[engine.get_index(x, y + 1)] - 
                                                         engine.pressure[engine.get_index(x, y - 1)]) * engine.grid_width
                
                engine.set_boundary(engine.velocity_x)
                engine.set_boundary(engine.velocity_y)
            
            engine.diffuse = modified_diffuse
            engine.project = modified_project
            
            # Run test
            times = []
            for frame in range(30):
                start = time.perf_counter()
                engine.update_boundary(1/60)
                times.append(time.perf_counter() - start)
            
            avg_time = sum(times) * 1000 / len(times)
            fps = 1000 / avg_time
            
            results[name] = {
                'diffusion_iterations': diffusion_iters,
                'pressure_iterations': pressure_iters, 
                'avg_frame_time_ms': avg_time,
                'fps': fps
            }
            
            print(f"  Average frame time: {avg_time:.2f}ms")
            print(f"  FPS: {fps:.1f}")
            
        self.results['solver_iterations'] = results
        return results
        
    def test_turbulence_complexity(self):
        """Test performance with different turbulence complexity"""
        print("\n=== Testing Turbulence Complexity ===")
        
        octave_configs = [
            ("Minimal", 1),
            ("Low", 2),
            ("Medium", 3),
            ("High", 5),  # Original
            ("Ultra", 7)
        ]
        
        results = {}
        
        for name, octaves in octave_configs:
            print(f"\nTesting {name} ({octaves} octaves)...")
            
            engine = FluidDynamicsEngine(128, 96)
            engine.turbulence_octaves = octaves
            
            # Run test
            times = []
            for frame in range(30):
                start = time.perf_counter()
                engine.update_boundary(1/60)
                times.append(time.perf_counter() - start)
            
            avg_time = sum(times) * 1000 / len(times)
            fps = 1000 / avg_time
            
            results[name] = {
                'octaves': octaves,
                'avg_frame_time_ms': avg_time,
                'fps': fps
            }
            
            print(f"  Average frame time: {avg_time:.2f}ms")
            print(f"  FPS: {fps:.1f}")
            
        self.results['turbulence_complexity'] = results
        return results
        
    def print_optimization_recommendations(self):
        """Print optimization recommendations based on test results"""
        print("\n" + "="*60)
        print("OPTIMIZATION RECOMMENDATIONS")
        print("="*60)
        
        if 'resolution_scaling' in self.results:
            print("\n1. RESOLUTION SCALING:")
            resolution_results = self.results['resolution_scaling']
            
            # Find sweet spot (best fps/quality ratio)
            best_ratio = 0
            best_config = None
            
            for name, data in resolution_results.items():
                # Simple ratio: fps per 1000 cells
                ratio = data['fps'] / (data['cells'] / 1000)
                if ratio > best_ratio:
                    best_ratio = ratio
                    best_config = (name, data)
                    
            if best_config:
                name, data = best_config
                print(f"   Recommended: {name} ({data['resolution'][0]}x{data['resolution'][1]})")
                print(f"   FPS: {data['fps']:.1f}, Efficiency: {best_ratio:.2f} fps/k-cells")
                
        if 'solver_iterations' in self.results:
            print("\n2. SOLVER ITERATIONS:")
            solver_results = self.results['solver_iterations']
            
            # Find configuration with fps > 30 and lowest iteration count
            viable_configs = [(name, data) for name, data in solver_results.items() 
                            if data['fps'] >= 30]
            
            if viable_configs:
                # Sort by total iterations (lower is better for performance)
                viable_configs.sort(key=lambda x: x[1]['diffusion_iterations'] + x[1]['pressure_iterations'])
                name, data = viable_configs[0]
                
                print(f"   Recommended: {name}")
                print(f"   Diffusion iterations: {data['diffusion_iterations']}")
                print(f"   Pressure iterations: {data['pressure_iterations']}")
                print(f"   FPS: {data['fps']:.1f}")
            else:
                print("   Warning: No configuration achieves 30+ FPS")
                
        if 'turbulence_complexity' in self.results:
            print("\n3. TURBULENCE COMPLEXITY:")
            turbulence_results = self.results['turbulence_complexity']
            
            # Find minimum octaves that still give good fps
            for name, data in turbulence_results.items():
                if data['fps'] >= 30:
                    print(f"   Recommended: {name} ({data['octaves']} octaves)")
                    print(f"   FPS: {data['fps']:.1f}")
                    break
                    
        print("\n4. GENERAL RECOMMENDATIONS:")
        print("   - Use adaptive resolution based on device performance")
        print("   - Cache trigonometric calculations where possible")
        print("   - Consider using lookup tables for noise generation")
        print("   - Implement early exit conditions in iterative solvers")
        print("   - Use SIMD instructions for array operations")
        print("   - Profile on target devices for real-world performance")
        
    def run_full_optimization_test(self):
        """Run complete optimization test suite"""
        print("Starting Full Optimization Test Suite...")
        print("This will take several minutes...")
        
        # Run all tests
        self.test_resolution_scaling()
        self.test_solver_iterations() 
        self.test_turbulence_complexity()
        
        # Print recommendations
        self.print_optimization_recommendations()
        
        return self.results

def main():
    """Main function with menu options"""
    print("Smoke Algorithm Debug and Optimization Tool")
    print("==========================================")
    print("1. Interactive Visualization")
    print("2. Performance Test Suite")
    print("3. Optimization Test Suite")
    print("4. Quick Performance Benchmark")
    
    choice = input("\nSelect option (1-4): ").strip()
    
    if choice == "1":
        print("Starting interactive visualization...")
        print("Controls:")
        print("- Left click to add turbulence")
        print("- Use sliders to adjust parameters")
        print("- Close window to exit")
        
        visualizer = SmokeVisualizer(128, 96)
        ani = visualizer.run()
        
    elif choice == "2":
        print("Running performance test suite...")
        from smoke_debug import run_performance_test
        run_performance_test()
        
    elif choice == "3":
        print("Running optimization test suite...")
        tester = OptimizationTester()
        results = tester.run_full_optimization_test()
        
    elif choice == "4":
        print("Running quick benchmark...")
        engine = FluidDynamicsEngine(128, 96)
        
        times = []
        for frame in range(60):
            start = time.perf_counter()
            engine.update_boundary(1/60)
            times.append(time.perf_counter() - start)
            
        avg_time = sum(times) * 1000 / len(times)
        fps = 1000 / avg_time
        
        print(f"Average frame time: {avg_time:.2f}ms")
        print(f"Average FPS: {fps:.1f}")
        
        engine.monitor.print_performance_report()
        
    else:
        print("Invalid choice. Exiting.")

if __name__ == "__main__":
    main()
