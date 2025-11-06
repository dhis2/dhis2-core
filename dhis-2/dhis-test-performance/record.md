# Recording DHIS2 Traffic with Gatling Recorder

Use Gatling Recorder to capture DHIS2 traffic and generate performance test simulations.

## What is Gatling Recorder?

Gatling Recorder captures HTTP requests as you interact with DHIS2, then generates Java code that
replays those interactions as a performance test simulation.

This allows you to capture production workflows and replay them in a testing environment to evaluate
performance after a DHIS2 upgrade or conduct load testing.

## Prerequisites

* **DHIS2 Instance:** Access to a running DHIS2 instance (e.g., `http://localhost:8080` or a remote
  server)
* **Java & Maven:** Required to run Gatling. See the [Gatling
  documentation](https://docs.gatling.io/reference/install/oss/) for installation requirements

## Recording Methods: Choose Your Approach

You can capture DHIS2 traffic using two methods:

1. **[Method 1: Proxy Recording](#method-1-proxy-recording)**
   * **Best for:** When you can configure browser proxy settings
   * **Pros:** Direct capture with powerful filtering, no file handling
   * **Cons:** Requires proxy configuration

2. **[Method 2: HAR Import](#method-2-har-import)**
   * **Best for:** When you cannot configure proxy (e.g., restricted permissions)
   * **Pros:** No proxy setup, works with any browser
   * **Cons:** Extra export step, browser network filtering less powerful than Gatling's filters

---

## Method 1: Proxy Recording

### Step 1: Start Gatling Recorder

```sh
mvn gatling:recorder
```

Configuration is pre-set in `src/test/resources/recorder.conf` (port 8000, filters to capture only
certain `/api/*` requests, ...).

Click **Start** in the GUI.

### Step 2: Configure Browser Proxy

**Firefox:** Settings → Network Settings → Manual proxy: `localhost:8000`

**Chrome:**

```sh
google-chrome --proxy-server="http://localhost:8000" \
  --proxy-bypass-list="<-loopback>" \
  --user-data-dir=/tmp/chrome-proxy-profile
```

### Step 3: Perform Your Workflow

See [Performing Your Workflow](#performing-your-workflow-both-methods) section below.

### Step 4: Stop & Save

Click **Stop & Save** in Recorder GUI. The Gatling simulation is generated at:

```
src/test/java/org/hisp/dhis/test/generated/RecordedSimulation.java
```

**Reset browser proxy** when done.

---

## Method 2: HAR Import

### Step 1: Perform Your Workflow

See [Performing Your Workflow](#performing-your-workflow-both-methods) section below, but keep
browser DevTools open with Network tab active and "Preserve log" enabled.

### Step 2: Export HAR File

1. Open DevTools (F12) → Network tab
2. Enable "Preserve log"
3. Perform your workflow (see section below)
4. Right-click in Network tab → "Save all as HAR" → save as `traffic.har`

### Step 3: Convert HAR to Simulation

1. Open Gatling Recorder:
   ```sh
   mvn gatling:recorder
   ```
2. In the Recorder GUI:
   * **IMPORTANT:** Uncheck **"Save preferences"** at the bottom (workaround for Gatling bug)
   * Select **"HAR Converter"** mode from dropdown (top right)
   * Click folder icon to browse and select your `traffic.har` file
   * Click **"Start"** to convert

The simulation will be generated with a default name based on the HAR file, likely at:

```
src/test/java/RecordedSimulation.java
```

**Note:** The HAR converter does not respect the `package` or `className` settings from
`recorder.conf`. You may need to move the generated file to the expected location and update the
package declaration.

**Known Issue:** HAR converter crashes with `StackOverflowError` if "Save preferences" is enabled.
This is a bug in Gatling 4.x where saving HAR converter settings causes infinite recursion in the
config serialization. Workaround: disable "Save preferences" before starting the conversion.

---

## Performing Your Workflow (Both Methods)

**IMPORTANT:** Simulations require authentication. Choose one:

* **Include login in recording:** Login during your workflow and the simulation will replay it
* **Add authentication manually:** Skip login during recording and add authentication to the
  generated code after generation (see `TrackerTest.java` for an example)

1. Navigate to your DHIS2 instance (e.g., `http://localhost:8080`)
2. (Optional) Login with valid credentials
3. Perform your test workflow (navigate, search, create data, etc.)
4. Watch requests being captured (in Recorder GUI for Method 1, in DevTools for Method 2)

---

## Post-Generation: Preparing Your Simulation

After recording (either method), complete these steps before running your simulation:

### 1. Fix Request Body File Paths (Required for POST/PUT/PATCH)

**Known Issue:** Gatling 4.x has a bug where request body files (`*_request.json`) are placed at the
root of `src/test/resources/` instead of in the package subdirectory where the generated code
expects them. See [gatling/gatling#3706](https://github.com/gatling/gatling/issues/3706).

Move the files to match your package structure. For example, if your simulation is in package
`org.hisp.dhis.test.generated` with class `RecordedSimulation`:

```sh
# Create the directory matching: src/test/resources/<package>/<classname-lowercase>
mkdir -p src/test/resources/org/hisp/dhis/test/generated/recordedsimulation
mv src/test/resources/*_request.json src/test/resources/org/hisp/dhis/test/generated/recordedsimulation/
```

This applies when recording POST/PUT/PATCH requests with request bodies.

### 2. Configure Authentication (If Needed)

If you didn't include login in your recording, add authentication to the generated code. See
`TrackerTest.java` for an example. Without authentication credentials in the recording, the
generated simulation will fail with 401 Unauthorized errors.

### 3. Update Target Server (Important for Production Recordings)

**Important:** Change the base URL if you recorded against a production or remote server and want to
replay against a different environment.

Edit the generated simulation file:

```java
// Change from recorded server to your test environment
String baseUrl = System.getProperty("base.url", "http://localhost:8080");

HttpProtocolBuilder httpProtocol = http
  .baseUrl(baseUrl)
  // ... rest of configuration
```

Now you can run with different targets:

```sh
mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.generated.RecordedSimulation \
  -Dbase.url=http://test-server:8080
```

**Note:** If you enabled "Use HTTP method and URI as request postfix" in the recorder settings, the
generated request names will contain the full recorded URL. You may want to search/replace the
server hostname in these request names throughout the simulation file for clarity.

### 4. Refine Simulation (Optional)

See `TrackerTest.java` for examples. Key refinements:

1. **Add load injection:** Replace `atOnceUsers(1)` with a profile that matches your implementation.
   See [Gatling injection documentation](https://docs.gatling.io/reference/script/core/injection/)
for available profiles and patterns.
2. **Clean up headers:** Remove browser-specific headers and ETags
3. **Add assertions:** `global().successfulRequests().percent().gte(99.0)`
4. **Use groups:** Organize related requests with `group("Search TEs").on(...)`

---

## Running Your Simulation

```sh
mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.generated.RecordedSimulation
```

View report: `target/gatling/recordedsimulation-<timestamp>/index.html`

## Configuration

Edit `src/test/resources/recorder.conf` to customize port, output location, or filters. See `mvn
gatling:help -Ddetail=true -Dgoal=recorder` for all options.

## Troubleshooting

* **No requests captured:** Check proxy settings, ensure DHIS2 instance is accessible and filters
are correct (they are Java regexes)
* **Browser can't connect:** Verify Recorder shows "Listening on port 8000"
* **File not generated:** Click "Stop & Save", don't just close window
* **Tests fail with 401 Unauthorized:** Recording doesn't include login. Add add authentication to the
  generated code (see `TrackerTest.java` for an example).
