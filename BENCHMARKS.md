# 📊 Performance Benchmarks

The OfflineAssistant utilizes `llama.cpp` heavily optimized with ARM KleidiAI and OpenMP for native on-device inference. 

Performance scales significantly depending on your device's RAM, memory bandwidth, and CPU architecture.

## How to Run the Benchmark
1. Launch the app and load a `.gguf` model.
2. Tap the **three-dot menu (⋮)** in the top right corner of the top app bar.
3. Tap **Run Benchmark**.
4. The engine will run iterations of prompt processing and text generation, then output the results directly into the chat window.

## Expected Performance 
*Note: These are estimates based on standard Android hardware running a Q4_K_M quantized model. Performance on an emulator will be artificially low because it lacks direct access to native ARM mobile GPU/NPU acceleration.*

| Device Tier | Model Size (GGUF) | Expected Throughput | TTFT (Time to First Token) |
|---|---|---|---|
| **Android Emulator (x86_64)** | 0.5B Parameters | ~2-5 tokens/sec | > 2000ms |
| **Budget Phone (4GB RAM)** | 1B Parameters | ~10-15 tokens/sec | ~800ms |
| **Mid-Range Phone (Snapdragon 7 series)** | 2B Parameters | ~15-25 tokens/sec | ~500ms |
| **Flagship Phone (Snapdragon 8 Gen 3 / Tensor G3)** | 4B Parameters | ~25-45+ tokens/sec | < 300ms |

### Benchmark Metrics Explained
*   **Throughput (Tokens per second):** How fast the AI can "type" out the answer after it finishes thinking. For context, average human reading speed is roughly 5-8 tokens per second.
*   **Latency (TTFT):** "Time to First Token". How long the AI takes to read and understand your prompt before it begins generating the very first word.

---
*If you run benchmarks on your own physical devices, feel free to submit a Pull Request with your device's specifications and results to update this table!*
