/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.test.tracker;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Circular Gatling feeder that loads pre-generated ndjson.gz payloads into memory for tracker bulk
 * imports.
 *
 * <h3>How chunking works</h3>
 *
 * Each line in the ndjson file is a complete JSON object (one tracked entity or one event). The
 * import scenario calls {@code .feed(feeder, linesPerRequest)} so Gatling's {@code FeedActor} polls
 * that many records per virtual user iteration. The session then contains {@code "payload"} as a
 * {@code List<String>} of JSON strings, which {@code wrapPayload} joins into the tracker import
 * envelope: {@code {"trackedEntities":[line1,line2,...lineN]}}.
 *
 * <h3>Circular behavior</h3>
 *
 * All lines are loaded into memory on construction. The feeder wraps around when it reaches the
 * end, so it never runs out of data. This allows duration-based import scenarios ({@code during()})
 * to run indefinitely. Since the payloads contain no entity UIDs, DHIS2 generates new UIDs for each
 * import request, so replayed data always creates new entities.
 *
 * <h3>Thread safety</h3>
 *
 * Provided by Gatling's {@code FeedActor} (single-threaded actor pattern). This feeder does not
 * need synchronization.
 *
 * <p>The ndjson files are pre-generated from Synthea patient data (see {@code SyntheaToNdjson}).
 */
class NdjsonFeeder implements Iterator<Map<String, Object>> {

  private static final Logger logger = LoggerFactory.getLogger(NdjsonFeeder.class);

  private final List<String> lines;
  private int index;

  NdjsonFeeder(Path ndjsonGzFile) {
    try (var reader =
        new BufferedReader(
            new InputStreamReader(
                new GZIPInputStream(new FileInputStream(ndjsonGzFile.toFile())),
                StandardCharsets.UTF_8))) {
      List<String> loaded = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        loaded.add(line);
      }
      this.lines = loaded;
      this.index = 0;
      logger.debug("ndjson feeder initialized: {} ({} lines)", ndjsonGzFile, lines.size());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load ndjson feeder: " + ndjsonGzFile, e);
    }
  }

  /** Total number of lines (records) in the file. */
  int lineCount() {
    return lines.size();
  }

  @Override
  public boolean hasNext() {
    return !lines.isEmpty();
  }

  @Override
  public Map<String, Object> next() {
    String line = lines.get(index);
    index = (index + 1) % lines.size();
    return Map.of("payload", line);
  }
}
