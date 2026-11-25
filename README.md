# Edge Caching with ML-Based Content Prediction

An iFogSim2/CloudSim simulation comparing LRU, LFU, and online logistic-regression cache policies at edge nodes. It includes a Swing interface and a console entry point.

## Requirements

- JDK 11 or newer (JDK 17 recommended)
- Windows, macOS, or Linux
- The dependency JARs in `jars/` (included in this repository)
- A graphical desktop environment to use the Swing interface

See [REQUIREMENTS.md](REQUIREMENTS.md) for dependency details and build/run commands.

## Run in Eclipse

1. Import the repository using **File > Import > General > Existing Projects into Workspace**.
2. Confirm the project uses JDK 11 or newer.
3. Open `src/org/fog/cache/EdgeCacheInterface.java` and select **Run As > Java Application**.
4. Set the workload and cache parameters, then click **Run simulation**. Results are shown in the table and chart, and `results.csv` is written to the project working directory.

For the console workflow, run `org.fog.cache.EdgeCacheMain`. The optional first argument sets simulation duration in milliseconds. `org.fog.cache.EdgeCacheTests` contains checks for the policies and simulation.

## Build and run from a terminal

From the repository root, use the commands in `REQUIREMENTS.md`. The build output goes into the ignored `bin/` directory; the classpath includes all JARs under `jars/`.

## Simulated topology

`cloud (origin) -> regional proxy -> three edge nodes (LRU, LFU, ML-Predict) -> users per edge`.
All edges receive the same Zipf request stream with popularity shifts, allowing the cache policies to be compared on a common workload.

## Project contents

- `src/` — iFogSim/CloudSim source, edge caching implementation, and resources
- `jars/` — third-party libraries needed to compile and run
- `.classpath`, `.project` — Eclipse project configuration
- `LICENSE.txt` — project license
- `results.csv` — generated output (ignored by Git)

## License

See [LICENSE.txt](LICENSE.txt). Third-party libraries in `jars/` retain their respective licenses.
