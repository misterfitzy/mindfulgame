"""
Smoke Algorithm Debug Implementation
Exact Python translation of Kotlin smoke simulation for performance optimization
"""

import numpy as np
import matplotlib.pyplot as plt
import matplotlib.animation as animation
from matplotlib.colors import LinearSegmentedColormap
import time
import cProfile
import pstats
from dataclasses import dataclass
from typing import Tuple, List, Optional
import math
import random

@dataclass
class TouchForce:
    grid_x: int
    grid_y: int
    force_x: float
    force_y: float
    strength: float
    age: float = 0.0

class ResolutionMode:
    PERFORMANCE = (64, 48)
    STANDARD = (96, 72)
    HIGH = (128, 96)
    ULTRA = (192, 144)

class PerformanceMonitor:
    def __init__(self):
        self.frame_times = []
        self.component_times = {}
        self.start_time = 0
        
    def start_frame(self):
        self.start_time = time.perf_counter()
        
    def end_frame(self):
        frame_time = time.perf_counter() - self.start_time
        self.frame_times.append(frame_time)
        if len(self.frame_times) > 100:  # Keep last 100 frames
            self.frame_times.pop(0)
            
    def start_component(self, name: str):
        self.component_times[name] = time.perf_counter()
        
    def end_component(self, name: str):
        if name in self.component_times:
            elapsed = time.perf_counter() - self.component_times[name]
            if f"{name}_history" not in self.component_times:
                self.component_times[f"{name}_history"] = []
            self.component_times[f"{name}_history"].append(elapsed)
            
    def get_average_fps(self) -> float:
        if not self.frame_times:
            return 0.0
        avg_frame_time = sum(self.frame_times) / len(self.frame_times)
        return 1.0 / avg_frame_time if avg_frame_time > 0 else 0.0
        
    def print_performance_report(self):
        print(f"\n=== Performance Report ===")
        print(f"Average FPS: {self.get_average_fps():.1f}")
        print(f"Average Frame Time: {(sum(self.frame_times) / len(self.frame_times) * 1000):.2f}ms")
        
        for component, times in self.component_times.items():
            if component.endswith("_history") and isinstance(times, list):
                component_name = component.replace("_history", "")
                avg_time = sum(times) / len(times) * 1000
                print(f"{component_name}: {avg_time:.3f}ms")

class SmokeEffects:
    """Exact translation of SmokeEffects.kt"""
    
    def __init__(self):
        random.seed(42)
        
    def calculate_smoke_color(self, white_intensity: float, black_intensity: float, 
                            turbulence: float, position: Tuple[float, float]) -> Tuple[int, int, int, int]:
        x, y = position
        total_intensity = white_intensity + black_intensity
        
        if total_intensity <= 0.0:
            return (0, 0, 0, 0)  # Transparent
            
        # Calculate base mixing
        mix_ratio = black_intensity / total_intensity
        density = max(0.0, min(1.0, total_intensity))
        
        # Smoke density affects opacity and color
        base_opacity = int(max(0, min(255, density * 255)))
        
        # Color temperature based on density and turbulence
        temperature = density * (1.0 + turbulence * 0.3)
        
        # Warmer colors in dense, turbulent areas
        red = int(max(0, min(255, 128 + temperature * 80 + math.sin(x * 10) * 10)))
        green = int(max(0, min(255, 128 + temperature * 60 + math.cos(y * 12) * 8)))
        blue = int(max(0, min(255, 128 + temperature * 40 + math.sin(x * 8 + y * 8) * 6)))
        
        # Apply mixing ratio
        final_red = int(max(0, min(255, red * (1 - mix_ratio) + 50 * mix_ratio)))
        final_green = int(max(0, min(255, green * (1 - mix_ratio) + 50 * mix_ratio)))
        final_blue = int(max(0, min(255, blue * (1 - mix_ratio) + 50 * mix_ratio)))
        
        return (final_red, final_green, final_blue, base_opacity)
    
    def apply_volumetric_lighting(self, base_color: Tuple[int, int, int, int], depth: float,
                                light_direction: Tuple[float, float] = (0.3, -0.7)) -> Tuple[int, int, int, int]:
        light_x, light_y = light_direction
        
        # Simulate light scattering through smoke
        scattering = max(0.7, min(1.0, 1.0 - depth * 0.3))
        
        red = int(max(0, min(255, base_color[0] * scattering)))
        green = int(max(0, min(255, base_color[1] * scattering)))
        blue = int(max(0, min(255, base_color[2] * scattering)))
        
        return (red, green, blue, base_color[3])
    
    def calculate_turbulence_intensity(self, velocity_x: float, velocity_y: float, 
                                     neighbors: np.ndarray) -> float:
        # Calculate vorticity (curl) from velocity field
        curl = abs(velocity_x - velocity_y) * 0.5
        
        # Add spatial turbulence from color gradients
        if len(neighbors) >= 4:
            variance = np.var(neighbors)
            gradient_variance = math.sqrt(variance)
        else:
            gradient_variance = 0.0
            
        return max(0.0, min(1.0, curl + gradient_variance * 2.0))
    
    def calculate_density(self, white_intensity: float, black_intensity: float) -> float:
        total_intensity = white_intensity + black_intensity
        return max(0.0, min(1.0, total_intensity))
    
    def calculate_mixing_activity(self, white_intensity: float, black_intensity: float) -> float:
        if white_intensity > 0.05 and black_intensity > 0.05:
            return min(white_intensity, black_intensity) * 2.0
        return 0.0
    
    def generate_smoke_noise(self, x: float, y: float, time: float, octaves: int = 3) -> float:
        noise = 0.0
        amplitude = 1.0
        frequency = 1.0
        max_value = 0.0
        
        for i in range(octaves):
            noise += math.sin(x * frequency + time) * math.cos(y * frequency + time * 0.7) * amplitude
            max_value += amplitude
            amplitude *= 0.5
            frequency *= 2.0
            
        return noise / max_value if max_value > 0 else 0.0
    
    def apply_temperature_gradient(self, base_color: Tuple[int, int, int, int], 
                                 temperature: float) -> Tuple[int, int, int, int]:
        warmth = max(0.0, min(1.0, temperature))
        
        red = int(max(0, min(255, base_color[0] * (1.0 + warmth * 0.2))))
        green = int(max(0, min(255, base_color[1] * (1.0 + warmth * 0.1))))
        blue = int(max(0, min(255, base_color[2] * (1.0 - warmth * 0.1))))
        
        return (red, green, blue, base_color[3])

class FluidDynamicsEngine:
    """Exact translation of FluidDynamicsEngine.kt"""
    
    def __init__(self, width: int = 128, height: int = 96):
        # Grid configuration - Enhanced resolution for smoke-like detail
        self.grid_width = width
        self.grid_height = height
        self.cell_size = 1.0
        
        # Initialize arrays
        self.size = self.grid_width * self.grid_height
        self.velocity_x = np.zeros(self.size, dtype=np.float32)
        self.velocity_y = np.zeros(self.size, dtype=np.float32)
        self.prev_velocity_x = np.zeros(self.size, dtype=np.float32)
        self.prev_velocity_y = np.zeros(self.size, dtype=np.float32)
        self.pressure = np.zeros(self.size, dtype=np.float32)
        self.divergence = np.zeros(self.size, dtype=np.float32)
        
        # Color field arrays
        self.color_field = np.zeros(self.size, dtype=np.float32)
        self.prev_color_field = np.zeros(self.size, dtype=np.float32)
        self.turbulence_field = np.zeros(self.size, dtype=np.float32)
        
        # Boundary representation
        self.boundary_resolution = 100
        self.boundary_points = np.zeros(self.boundary_resolution * 2, dtype=np.float32)
        
        # Simulation parameters - Enhanced for smoke-like behavior
        self.viscosity = 0.001
        self.diffusion = 0.0001
        self.vorticity = 0.5
        self.time_step = 0.016
        self.density_decay = 0.999
        
        # Enhanced turbulence parameters
        self.turbulence_octaves = 5
        self.turbulence_frequencies = np.array([1.0, 2.0, 4.0, 8.0, 16.0])
        self.turbulence_amplitudes = np.array([1.0, 0.6, 0.36, 0.216, 0.1296])
        
        # Mixing parameters
        self.mixing_intensity = 0.0
        self.max_mixing_intensity = 1.0
        self.mixing_growth_rate = 0.1
        self.global_time = 0.0
        
        # Touch interaction
        self.active_touches: List[TouchForce] = []
        self.touch_strength = 50.0
        self.touch_radius = 8.0
        
        # Turbulence generation
        self.noise_offset = 0.0
        
        # Performance monitoring
        self.monitor = PerformanceMonitor()
        
        self.reset()
    
    def get_index(self, x: int, y: int) -> int:
        """Convert 2D coordinates to 1D array index"""
        return x + y * self.grid_width
    
    def update_boundary(self, delta_time: float):
        """Main update loop - exact translation of Kotlin method"""
        self.monitor.start_frame()
        
        self.global_time += delta_time
        
        # Gradually increase mixing intensity
        if self.mixing_intensity < self.max_mixing_intensity:
            self.mixing_intensity = min(self.max_mixing_intensity, 
                                      self.mixing_intensity + self.mixing_growth_rate * delta_time)
        
        # Age and remove old touches
        self.active_touches = [touch for touch in self.active_touches 
                              if (setattr(touch, 'age', touch.age + delta_time) or touch.age <= 3.0)]
        
        # Add continuous turbulence injection
        self.monitor.start_component("turbulence_injection")
        self.inject_turbulence(delta_time)
        self.monitor.end_component("turbulence_injection")
        
        # Apply touch forces
        self.monitor.start_component("touch_forces")
        self.apply_touch_forces()
        self.monitor.end_component("touch_forces")
        
        # Run fluid simulation steps
        self.monitor.start_component("velocity_step")
        self.velocity_step(delta_time)
        self.monitor.end_component("velocity_step")
        
        self.monitor.start_component("color_step")
        self.color_step(delta_time)
        self.monitor.end_component("color_step")
        
        # Update boundary representation
        self.monitor.start_component("boundary_update")
        self.update_boundary_representation()
        self.monitor.end_component("boundary_update")
        
        self.monitor.end_frame()
    
    def inject_turbulence(self, delta_time: float):
        """Inject turbulence along the center boundary - exact translation"""
        self.noise_offset += delta_time * 0.5
        
        # Inject turbulence along the center boundary and spreading outward
        center_x = self.grid_width // 2
        injection_radius = int(self.mixing_intensity * self.grid_width * 0.3)
        
        for y in range(self.grid_height):
            for x in range(max(0, center_x - injection_radius), 
                          min(self.grid_width, center_x + injection_radius + 1)):
                
                index = self.get_index(x, y)
                distance_from_center = abs(x - center_x)
                normalized_distance = distance_from_center / max(1, injection_radius)
                
                if normalized_distance <= 1.0:
                    noise_x = x / self.grid_width
                    noise_y = y / self.grid_height
                    
                    # Multi-octave turbulence for smoke-like detail
                    combined_noise = 0.0
                    for octave in range(self.turbulence_octaves):
                        freq = self.turbulence_frequencies[octave]
                        amp = self.turbulence_amplitudes[octave]
                        
                        noise = (math.sin((noise_x * freq + self.noise_offset) * math.pi * 2) * 
                                math.cos((noise_y * freq + self.noise_offset * 0.7) * math.pi * 2))
                        combined_noise += noise * amp
                    
                    falloff = (1.0 - normalized_distance) * self.mixing_intensity
                    turbulent_force = combined_noise * falloff * 25.0
                    
                    # Add curl-like motion for realistic fluid flow
                    self.velocity_x[index] += turbulent_force * math.cos(self.global_time + noise_y * math.pi)
                    self.velocity_y[index] += turbulent_force * math.sin(self.global_time + noise_x * math.pi)
                    
                    # Micro-vortices for fine detail
                    if random.random() < 0.015 * delta_time * self.mixing_intensity:
                        self.add_micro_vortex(x, y, (random.random() - 0.5) * 15.0)
    
    def add_micro_vortex(self, center_x: int, center_y: int, strength: float):
        """Add micro-vortex for fine detail"""
        radius = 3  # Smaller than regular vortices for fine detail
        
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                x = center_x + dx
                y = center_y + dy
                
                if x < 0 or x >= self.grid_width or y < 0 or y >= self.grid_height:
                    continue
                
                distance = math.sqrt(dx * dx + dy * dy)
                if distance < radius and distance > 0:
                    index = self.get_index(x, y)
                    falloff = (1.0 - distance / radius) * strength
                    
                    # Create rotational velocity field for micro-scale detail
                    self.velocity_x[index] += -dy / distance * falloff
                    self.velocity_y[index] += dx / distance * falloff
    
    def apply_touch_forces(self):
        """Apply touch forces to velocity field"""
        for touch in self.active_touches:
            radius = int(self.touch_radius)
            
            for dy in range(-radius, radius + 1):
                for dx in range(-radius, radius + 1):
                    x = touch.grid_x + dx
                    y = touch.grid_y + dy
                    
                    if x < 0 or x >= self.grid_width or y < 0 or y >= self.grid_height:
                        continue
                    
                    distance = math.sqrt(dx * dx + dy * dy)
                    if distance < self.touch_radius:
                        index = self.get_index(x, y)
                        falloff = (1.0 - distance / self.touch_radius) * touch.strength * (1.0 - touch.age / 3.0)
                        
                        self.velocity_x[index] += touch.force_x * falloff
                        self.velocity_y[index] += touch.force_y * falloff
                        
                        # Create swirling motion around touch
                        swirl = falloff * 10.0
                        self.velocity_x[index] += -dy * swirl / self.touch_radius
                        self.velocity_y[index] += dx * swirl / self.touch_radius
    
    def velocity_step(self, delta_time: float):
        """Velocity step - exact translation"""
        # Store previous velocity
        np.copyto(self.prev_velocity_x, self.velocity_x)
        np.copyto(self.prev_velocity_y, self.velocity_y)
        
        # Diffusion
        self.diffuse(self.velocity_x, self.prev_velocity_x, self.viscosity, delta_time)
        self.diffuse(self.velocity_y, self.prev_velocity_y, self.viscosity, delta_time)
        
        # Projection (make velocity field divergence-free)
        self.project()
        
        # Store velocity for advection
        np.copyto(self.prev_velocity_x, self.velocity_x)
        np.copyto(self.prev_velocity_y, self.velocity_y)
        
        # Advection
        self.advect(self.velocity_x, self.prev_velocity_x, self.prev_velocity_x, self.prev_velocity_y, delta_time)
        self.advect(self.velocity_y, self.prev_velocity_y, self.prev_velocity_x, self.prev_velocity_y, delta_time)
        
        # Final projection
        self.project()
        
        # Vorticity confinement
        self.vorticity_confinement(delta_time)
        
        # Apply velocity decay
        self.velocity_x *= 0.99
        self.velocity_y *= 0.99
    
    def color_step(self, delta_time: float):
        """Color step - exact translation"""
        # Store previous color field
        np.copyto(self.prev_color_field, self.color_field)
        
        # Diffusion
        self.diffuse(self.color_field, self.prev_color_field, self.diffusion, delta_time)
        
        # Store for advection
        np.copyto(self.prev_color_field, self.color_field)
        
        # Advection
        self.advect(self.color_field, self.prev_color_field, self.velocity_x, self.velocity_y, delta_time)
        
        # Apply mixing at boundaries between different colors
        self.apply_mixing(delta_time)
    
    def apply_mixing(self, delta_time: float):
        """Apply color mixing - exact translation"""
        mixing_rate = self.mixing_intensity * 0.5 * delta_time
        
        for y in range(1, self.grid_height - 1):
            for x in range(1, self.grid_width - 1):
                index = self.get_index(x, y)
                current = self.color_field[index]
                
                # Calculate gradient (measure of color difference)
                grad_x = (self.color_field[self.get_index(x + 1, y)] - 
                         self.color_field[self.get_index(x - 1, y)]) * 0.5
                grad_y = (self.color_field[self.get_index(x, y + 1)] - 
                         self.color_field[self.get_index(x, y - 1)]) * 0.5
                gradient_magnitude = math.sqrt(grad_x * grad_x + grad_y * grad_y)
                
                # Apply mixing where gradients are high
                if gradient_magnitude > 0.1:
                    neighbors = np.array([
                        self.color_field[self.get_index(x - 1, y)],
                        self.color_field[self.get_index(x + 1, y)],
                        self.color_field[self.get_index(x, y - 1)],
                        self.color_field[self.get_index(x, y + 1)]
                    ])
                    
                    average_neighbor = np.mean(neighbors)
                    self.color_field[index] = current + (average_neighbor - current) * mixing_rate * gradient_magnitude
    
    def diffuse(self, field: np.ndarray, prev: np.ndarray, diffusion_rate: float, delta_time: float):
        """Diffusion using Gauss-Seidel relaxation - exact translation"""
        a = delta_time * diffusion_rate * self.grid_width * self.grid_height
        
        # Gauss-Seidel relaxation
        for iteration in range(4):
            for y in range(1, self.grid_height - 1):
                for x in range(1, self.grid_width - 1):
                    index = self.get_index(x, y)
                    
                    neighbors_sum = (field[self.get_index(x - 1, y)] + 
                                   field[self.get_index(x + 1, y)] +
                                   field[self.get_index(x, y - 1)] + 
                                   field[self.get_index(x, y + 1)])
                    
                    field[index] = (prev[index] + a * neighbors_sum) / (1 + 4 * a)
            
            self.set_boundary(field)
    
    def advect(self, field: np.ndarray, prev: np.ndarray, vel_x: np.ndarray, vel_y: np.ndarray, delta_time: float):
        """Advection with bilinear interpolation - exact translation"""
        dt0 = delta_time * self.grid_width
        
        for y in range(1, self.grid_height - 1):
            for x in range(1, self.grid_width - 1):
                index = self.get_index(x, y)
                
                # Trace particle backwards
                back_x = x - dt0 * vel_x[index]
                back_y = y - dt0 * vel_y[index]
                
                # Clamp to grid bounds
                back_x = max(0.5, min(self.grid_width - 1.5, back_x))
                back_y = max(0.5, min(self.grid_height - 1.5, back_y))
                
                # Bilinear interpolation
                i0 = int(back_x)
                i1 = i0 + 1
                j0 = int(back_y)
                j1 = j0 + 1
                
                s1 = back_x - i0
                s0 = 1 - s1
                t1 = back_y - j0
                t0 = 1 - t1
                
                field[index] = (s0 * (t0 * prev[self.get_index(i0, j0)] + t1 * prev[self.get_index(i0, j1)]) +
                               s1 * (t0 * prev[self.get_index(i1, j0)] + t1 * prev[self.get_index(i1, j1)]))
        
        self.set_boundary(field)
    
    def project(self):
        """Pressure projection to make velocity field divergence-free - exact translation"""
        # Calculate divergence
        for y in range(1, self.grid_height - 1):
            for x in range(1, self.grid_width - 1):
                index = self.get_index(x, y)
                
                self.divergence[index] = -0.5 * (
                    self.velocity_x[self.get_index(x + 1, y)] - self.velocity_x[self.get_index(x - 1, y)] +
                    self.velocity_y[self.get_index(x, y + 1)] - self.velocity_y[self.get_index(x, y - 1)]
                ) / self.grid_width
                
                self.pressure[index] = 0.0
        
        self.set_boundary(self.divergence)
        self.set_boundary(self.pressure)
        
        # Solve for pressure
        for iteration in range(10):
            for y in range(1, self.grid_height - 1):
                for x in range(1, self.grid_width - 1):
                    index = self.get_index(x, y)
                    
                    neighbors_sum = (self.pressure[self.get_index(x - 1, y)] + 
                                   self.pressure[self.get_index(x + 1, y)] +
                                   self.pressure[self.get_index(x, y - 1)] + 
                                   self.pressure[self.get_index(x, y + 1)])
                    
                    self.pressure[index] = (self.divergence[index] + neighbors_sum) / 4.0
            
            self.set_boundary(self.pressure)
        
        # Subtract pressure gradient
        for y in range(1, self.grid_height - 1):
            for x in range(1, self.grid_width - 1):
                index = self.get_index(x, y)
                
                self.velocity_x[index] -= 0.5 * (self.pressure[self.get_index(x + 1, y)] - 
                                               self.pressure[self.get_index(x - 1, y)]) * self.grid_width
                self.velocity_y[index] -= 0.5 * (self.pressure[self.get_index(x, y + 1)] - 
                                               self.pressure[self.get_index(x, y - 1)]) * self.grid_width
        
        self.set_boundary(self.velocity_x)
        self.set_boundary(self.velocity_y)
    
    def vorticity_confinement(self, delta_time: float):
        """Vorticity confinement - exact translation"""
        # Calculate vorticity
        for y in range(1, self.grid_height - 1):
            for x in range(1, self.grid_width - 1):
                index = self.get_index(x, y)
                
                self.turbulence_field[index] = ((self.velocity_y[self.get_index(x + 1, y)] - 
                                               self.velocity_y[self.get_index(x - 1, y)]) * 0.5 -
                                              (self.velocity_x[self.get_index(x, y + 1)] - 
                                               self.velocity_x[self.get_index(x, y - 1)]) * 0.5)
        
        # Apply vorticity confinement force
        for y in range(1, self.grid_height - 1):
            for x in range(1, self.grid_width - 1):
                index = self.get_index(x, y)
                
                dwdx = (abs(self.turbulence_field[self.get_index(x + 1, y)]) - 
                       abs(self.turbulence_field[self.get_index(x - 1, y)])) * 0.5
                dwdy = (abs(self.turbulence_field[self.get_index(x, y + 1)]) - 
                       abs(self.turbulence_field[self.get_index(x, y - 1)])) * 0.5
                
                length = math.sqrt(dwdx * dwdx + dwdy * dwdy) + 1e-5
                
                fx = dwdy / length * self.turbulence_field[index] * self.vorticity * delta_time
                fy = -dwdx / length * self.turbulence_field[index] * self.vorticity * delta_time
                
                self.velocity_x[index] += fx
                self.velocity_y[index] += fy
    
    def set_boundary(self, field: np.ndarray):
        """Set boundary conditions - exact translation"""
        # Set boundary conditions (no-slip for walls)
        for i in range(1, self.grid_width - 1):
            field[self.get_index(i, 0)] = field[self.get_index(i, 1)]  # Top
            field[self.get_index(i, self.grid_height - 1)] = field[self.get_index(i, self.grid_height - 2)]  # Bottom
        
        for j in range(1, self.grid_height - 1):
            field[self.get_index(0, j)] = field[self.get_index(1, j)]  # Left
            field[self.get_index(self.grid_width - 1, j)] = field[self.get_index(self.grid_width - 2, j)]  # Right
        
        # Corners
        field[self.get_index(0, 0)] = 0.5 * (field[self.get_index(1, 0)] + field[self.get_index(0, 1)])
        field[self.get_index(self.grid_width - 1, 0)] = 0.5 * (field[self.get_index(self.grid_width - 2, 0)] + 
                                                              field[self.get_index(self.grid_width - 1, 1)])
        field[self.get_index(0, self.grid_height - 1)] = 0.5 * (field[self.get_index(1, self.grid_height - 1)] + 
                                                               field[self.get_index(0, self.grid_height - 2)])
        field[self.get_index(self.grid_width - 1, self.grid_height - 1)] = 0.5 * (
            field[self.get_index(self.grid_width - 2, self.grid_height - 1)] + 
            field[self.get_index(self.grid_width - 1, self.grid_height - 2)])
    
    def update_boundary_representation(self):
        """Update boundary representation for rendering compatibility"""
        center_x = self.grid_width // 2
        
        for i in range(self.boundary_resolution):
            y = i / (self.boundary_resolution - 1)
            grid_y = int(max(0, min(self.grid_height - 1, y * (self.grid_height - 1))))
            
            # Find the boundary position by looking for the 0.5 color value
            boundary_x = 0.5
            for x in range(self.grid_width - 1):
                index1 = self.get_index(x, grid_y)
                index2 = self.get_index(x + 1, grid_y)
                color1 = self.color_field[index1]
                color2 = self.color_field[index2]
                
                # Check if boundary crosses between these cells
                if (color1 <= 0.5 <= color2) or (color1 >= 0.5 >= color2):
                    # Linear interpolation to find exact boundary position
                    if abs(color2 - color1) > 1e-6:
                        t = (0.5 - color1) / (color2 - color1)
                        boundary_x = (x + t) / self.grid_width
                        break
            
            self.boundary_points[i * 2] = boundary_x
            self.boundary_points[i * 2 + 1] = y
    
    def apply_touch_input(self, x: float, y: float, pressure: float):
        """Apply touch input"""
        grid_x = int(max(0, min(self.grid_width - 1, x * self.grid_width)))
        grid_y = int(max(0, min(self.grid_height - 1, y * self.grid_height)))
        
        # Calculate force direction (towards center for restoration effect)
        center_x = self.grid_width / 2.0
        force_x = (center_x - grid_x) * 0.1
        force_y = 0.0
        
        self.active_touches.append(TouchForce(grid_x, grid_y, force_x, force_y, pressure * self.touch_strength))
        
        # Limit active touches for performance
        if len(self.active_touches) > 15:
            self.active_touches.pop(0)
    
    def get_boundary_points(self) -> np.ndarray:
        """Get boundary points for rendering"""
        return self.boundary_points
    
    def get_restoration_progress(self) -> float:
        """Calculate mixing progress based on color field uniformity"""
        target_color = 0.5
        total_variance = np.sum(np.abs(self.color_field - target_color))
        max_variance = self.size * 0.5
        return max(0.0, min(1.0, 1.0 - (total_variance / max_variance)))
    
    def reset(self):
        """Reset simulation state"""
        self.mixing_intensity = 0.0
        self.global_time = 0.0
        self.active_touches.clear()
        
        # Initialize color field: left half white (1.0), right half black (0.0)
        for y in range(self.grid_height):
            for x in range(self.grid_width):
                index = self.get_index(x, y)
                self.color_field[index] = 1.0 if x < self.grid_width // 2 else 0.0
                self.prev_color_field[index] = self.color_field[index]
                
                self.velocity_x[index] = 0.0
                self.velocity_y[index] = 0.0
                self.prev_velocity_x[index] = 0.0
                self.prev_velocity_y[index] = 0.0
                self.pressure[index] = 0.0
                self.divergence[index] = 0.0
                self.turbulence_field[index] = 0.0
        
        # Initialize boundary points
        for i in range(self.boundary_resolution):
            y = i / (self.boundary_resolution - 1)
            self.boundary_points[i * 2] = 0.5
            self.boundary_points[i * 2 + 1] = y
    
    def get_left_region_intensity(self, x: float, y: float) -> float:
        """Get left region (white) intensity at normalized coordinates"""
        grid_x = int(max(0, min(self.grid_width - 1, x * self.grid_width)))
        grid_y = int(max(0, min(self.grid_height - 1, y * self.grid_height)))
        index = self.get_index(grid_x, grid_y)
        return self.color_field[index]
    
    def get_right_region_intensity(self, x: float, y: float) -> float:
        """Get right region (black) intensity at normalized coordinates"""
        grid_x = int(max(0, min(self.grid_width - 1, x * self.grid_width)))
        grid_y = int(max(0, min(self.grid_height - 1, y * self.grid_height)))
        index = self.get_index(grid_x, grid_y)
        return 1.0 - self.color_field[index]

class FluidRenderer:
    """Simplified renderer for Python visualization"""
    
    def __init__(self):
        self.smoke_effects = SmokeEffects()
        self.quality = 1  # 0=low, 1=medium, 2=high
    
    def render_to_array(self, engine: FluidDynamicsEngine, width: int, height: int) -> np.ndarray:
        """Render fluid field to RGB array"""
        image = np.zeros((height, width, 3), dtype=np.uint8)
        
        for y in range(height):
            for x in range(width):
                # Convert pixel coordinates to normalized coordinates
                norm_x = x / width
                norm_y = y / height
                
                # Sample color with interpolation
                white_intensity = engine.get_left_region_intensity(norm_x, norm_y)
                black_intensity = engine.get_right_region_intensity(norm_x, norm_y)
                
                if self.quality >= 2:
                    # Multi-sampling for highest quality
                    color = self.sample_color_with_multi_sampling(x, y, width, height, engine)
                else:
                    # Simple color calculation
                    color = self.calculate_enhanced_mixed_color(white_intensity, black_intensity, norm_x, norm_y)
                
                image[y, x] = color[:3]  # RGB only
        
        return image
    
    def sample_color_with_multi_sampling(self, pixel_x: int, pixel_y: int, width: int, height: int, 
                                       engine: FluidDynamicsEngine) -> Tuple[int, int, int]:
        """Multi-sampling for anti-aliasing"""
        sample_offsets = [(-0.25, -0.25), (0.25, -0.25), (-0.25, 0.25), (0.25, 0.25)]
        
        total_red = total_green = total_blue = 0.0
        
        for offset_x, offset_y in sample_offsets:
            sample_x = (pixel_x + offset_x) / width
            sample_y = (pixel_y + offset_y) / height
            
            white_intensity = engine.get_left_region_intensity(sample_x, sample_y)
            black_intensity = engine.get_right_region_intensity(sample_x, sample_y)
            
            color = self.calculate_enhanced_mixed_color(white_intensity, black_intensity, sample_x, sample_y)
            
            total_red += color[0]
            total_green += color[1]
            total_blue += color[2]
        
        return (int(total_red / 4), int(total_green / 4), int(total_blue / 4))
    
    def calculate_enhanced_mixed_color(self, white_intensity: float, black_intensity: float, 
                                     x: float, y: float) -> Tuple[int, int, int]:
        """Calculate enhanced mixed color using smoke effects"""
        total_intensity = white_intensity + black_intensity
        if total_intensity <= 0.0:
            return (128, 128, 128)  # Gray
        
        # Calculate turbulence using noise generation
        turbulence = self.smoke_effects.generate_smoke_noise(x, y, time.time())
        
        # Use SmokeEffects for advanced color calculation
        smoke_color = self.smoke_effects.calculate_smoke_color(
            white_intensity, black_intensity, turbulence, (x, y)
        )
        
        # Apply volumetric lighting for depth
        depth = self.smoke_effects.calculate_density(white_intensity, black_intensity)
        lit_color = self.smoke_effects.apply_volumetric_lighting(smoke_color, depth)
        
        # Apply temperature gradient for realism
        temperature = self.smoke_effects.calculate_mixing_activity(white_intensity, black_intensity)
        final_color = self.smoke_effects.apply_temperature_gradient(lit_color, temperature)
        
        return final_color[:3]  # RGB only

def run_performance_test():
    """Run performance test with profiling"""
    print("Starting Smoke Algorithm Performance Test...")
    
    # Test different resolutions
    resolutions = [
        ("Performance", ResolutionMode.PERFORMANCE),
        ("Standard", ResolutionMode.STANDARD), 
        ("High", ResolutionMode.HIGH),
        ("Ultra", ResolutionMode.ULTRA)
    ]
    
    for name, (width, height) in resolutions:
        print(f"\n=== Testing {name} Resolution ({width}x{height}) ===")
        
        engine = FluidDynamicsEngine(width, height)
        renderer = FluidRenderer()
        
        # Run simulation for 60 frames
        frame_count = 60
        delta_time = 1.0 / 60.0
        
        start_time = time.perf_counter()
        
        for frame in range(frame_count):
            engine.update_boundary(delta_time)
            
            # Render every 10th frame to test rendering performance
            if frame % 10 == 0:
                image = renderer.render_to_array(engine, width // 2, height // 2)
        
        total_time = time.perf_counter() - start_time
        avg_fps = frame_count / total_time
        
        print(f"Total time: {total_time:.3f}s")
        print(f"Average FPS: {avg_fps:.1f}")
        print(f"Frame time: {(total_time / frame_count * 1000):.2f}ms")
        
        # Print component breakdown
        engine.monitor.print_performance_report()

def run_profiled_test():
    """Run detailed profiling of the algorithm"""
    print("\n=== Running Detailed Profiling ===")
    
    def profile_target():
        engine = FluidDynamicsEngine(128, 96)  # High resolution
        renderer = FluidRenderer()
        delta_time = 1.0 / 60.0
        
        for frame in range(30):  # 30 frames for profiling
            engine.update_boundary(delta_time)
            if frame % 5 == 0:
                image = renderer.render_to_array(engine, 64, 48)
    
    # Run with cProfile
    profiler = cProfile.Profile()
    profiler.enable()
    profile_target()
    profiler.disable()
    
    # Print top 20 functions by cumulative time
    stats = pstats.Stats(profiler)
    stats.sort_stats('cumulative')
    stats.print_stats(20)

if __name__ == "__main__":
    # Run performance tests
    run_performance_test()
    
    # Run detailed profiling
    run_profiled_test()
    
    print("\n=== Performance Analysis Complete ===")
    print("Use this data to identify bottlenecks and optimize the Kotlin implementation.")
