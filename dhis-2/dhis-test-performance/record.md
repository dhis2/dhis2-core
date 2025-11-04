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

### 6. Run Recorded Simulation

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

```sh
# 1. DevTools (F12) → Network → Preserve log
# 2. Perform workflow → Right-click → "Save all as HAR"
# 3. Convert:
mvn gatling:recorder -Dgatling.recorder.mode=Har \
  -Dgatling.recorder.harFilePath=traffic.har
```

## Troubleshooting

* **No requests captured:** Check proxy settings, ensure DHIS2 is running
* **Browser can't connect:** Verify Recorder shows "Listening on port 8000"
* **File not generated:** Click "Stop & Save", don't just close window
