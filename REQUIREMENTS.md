# Build and Runtime Requirements

## Software

- **Java Development Kit:** version 11 or newer. Install a JDK (not only a JRE) so `javac` is available. Set `JAVA_HOME` and ensure `java` and `javac` are on `PATH`.
- **Operating system:** Windows, macOS, or Linux. A graphical desktop is required for `EdgeCacheInterface`; the console simulation can run headlessly.
- **Dependencies:** prebuilt Java libraries are included under `jars/`. No Python, Maven, Gradle, or network download is required for the documented build.

## Compile

Run from the repository root. The commands below compile every Java source into `bin/` using the bundled JARs.

### Windows PowerShell

```powershell
$classpath = (Get-ChildItem jars -Recurse -Filter *.jar | ForEach-Object { $_.FullName }) -join ';'
$sources = Get-ChildItem src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
New-Item -ItemType Directory -Force bin | Out-Null
javac -encoding UTF-8 -cp $classpath -d bin $sources
```

### macOS / Linux (Bash)

```bash
mkdir -p bin
classpath=$(find jars -name '*.jar' -print | paste -sd ':' -)
find src -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -cp "$classpath" -d bin
```

## Run

Append `bin` and the JARs to the classpath. From the repository root:

### Windows PowerShell

```powershell
$classpath = "bin;" + ((Get-ChildItem jars -Recurse -Filter *.jar | ForEach-Object { $_.FullName }) -join ';')
java -cp $classpath org.fog.cache.EdgeCacheMain
```

### macOS / Linux (Bash)

```bash
classpath="bin:$(find jars -name '*.jar' -print | paste -sd ':' -)"
java -cp "$classpath" org.fog.cache.EdgeCacheMain
```

The console entry point accepts an optional simulation duration in milliseconds, for example `org.fog.cache.EdgeCacheMain 60000`. To launch the GUI, replace the main class with `org.fog.cache.EdgeCacheInterface`; this requires a display. Simulation results are written to `results.csv` in the current working directory.

## Dependency inventory

The Eclipse classpath references the following bundled libraries: Commons IO, Commons Lang, Commons Net, EasyMock, GeoIP2, Guava, json-simple, JUnit Jupiter Engine, OpenCSV, Uncommons Maths, and Commons Math 3.5. Keep `jars/` in the repository so builds remain reproducible without external dependency resolution. Check each library's own license when redistributing the combined project.
