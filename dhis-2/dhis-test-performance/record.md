# Recording DHIS2 Traffic with Gatling Recorder

Use Gatling Recorder to capture DHIS2 traffic and generate performance test simulations.

## Quick Start

### 1. Start DHIS2

```sh
docker compose up
```

### 2. Start Gatling Recorder

```sh
mvn gatling:recorder
```

Configuration is pre-set in `src/test/resources/recorder.conf`:
* **Mode:** HTTP Proxy on port 8000
* **Output:** `src/test/java/org/hisp/dhis/test/generated/RecordedSimulation.java`
* **Format:** Java 17
* **Filters:** Only captures `/api/*` requests (excludes .js, .css, images)

Click **Start** in the GUI.

### 3. Configure Browser Proxy

**Firefox:** Settings → Network Settings → Manual proxy: `localhost:8000`

**Chrome:**
```sh
google-chrome --proxy-server="http://localhost:8000" \
  --proxy-bypass-list="<-loopback>" \
  --user-data-dir=/tmp/chrome-proxy-profile
```

### 4. Record Workflow

1. Browse to `http://localhost:8080`
2. Login
3. Perform your test workflow
4. Watch requests appear in Recorder GUI

### 5. Stop & Save

Click **Stop & Save** in Recorder GUI. The Gatling simulation is generated at:
```
src/test/java/org/hisp/dhis/test/generated/RecordedSimulation.java
```

**Reset browser proxy** when done.

### 6. Move Request Body Files (Workaround for Gatling Bug)

**Known Issue:** Gatling 4.x has a bug where request body files (`*_request.json`) are placed at the
root of `src/test/resources/` instead of in the package subdirectory where the generated code
expects them. See [gatling/gatling#3706](https://github.com/gatling/gatling/issues/3706).

After recording, move the files to the correct location:

```sh
mkdir -p src/test/resources/org/hisp/dhis/test/generated/recordedsimulation
mv src/test/resources/*_request.json src/test/resources/org/hisp/dhis/test/generated/recordedsimulation/
```

This applies when recording POST/PUT/PATCH requests with request bodies.

### 7. Run Recorded Simulation

```sh
mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.generated.RecordedSimulation
```

View report: `target/gatling/recordedsimulation-<timestamp>/index.html`

## Refining Generated Simulations

See `TrackerTest.java` for examples. Key refinements:

1. **Add load injection:** Replace `atOnceUsers(1)` with a profile that matches your implementation
   `constantConcurrentUsers(10).during(60)`
2. **Clean up headers:** Remove browser-specific headers and ETags
3. **Add assertions:** `global().successfulRequests().percent().gte(99.0)`
4. **Use groups:** Organize related requests with `group("Search TEs").on(...)`

## Configuration

Edit `src/test/resources/recorder.conf` to customize:
* Port, output location, filters
* See `mvn gatling:help -Ddetail=true -Dgoal=recorder` for all options

## Alternative: HAR Import

Don't want to configure proxy? Use browser DevTools:

1. Open DevTools (F12) → Network tab
2. Enable "Preserve log"
3. Perform your workflow in the browser
4. Right-click in Network tab → "Save all as HAR" → save as `traffic.har`
5. Open Gatling Recorder:
   ```sh
   mvn gatling:recorder
   ```
6. In the Recorder GUI:
   * **IMPORTANT:** Uncheck **"Save preferences"** at the bottom (workaround for Gatling bug)
   * Select **"HAR Converter"** mode from dropdown (top right)
   * Click folder icon to browse and select your `traffic.har` file
   * Click **"Start"** to convert

The simulation will be generated at `src/test/java/org/hisp/dhis/test/generated/RecordedSimulation.java`

**Known Issue:** HAR converter crashes with `StackOverflowError` if "Save preferences" is enabled.
This is a bug in Gatling 4.x where saving HAR converter settings causes infinite recursion in the
config serialization. Workaround: disable "Save preferences" before starting the conversion.

## Troubleshooting

* **No requests captured:** Check proxy settings, ensure DHIS2 is running
* **Browser can't connect:** Verify Recorder shows "Listening on port 8000"
* **File not generated:** Click "Stop & Save", don't just close window
