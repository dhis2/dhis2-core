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
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gatling feeder that streams pre-generated ndjson.gz payloads for tracker bulk imports.
 *
 * <h3>How chunking works</h3>
 *
 * Each line in the ndjson file is a complete JSON object (one tracked entity or one event). The
 * import scenario calls {@code .feed(feeder, linesPerRequest)} so Gatling's {@code FeedActor} polls
 * that many records per virtual user iteration. The session then contains {@code "payload"} as a
 * {@code List<String>} of JSON strings, which {@code wrapPayload} joins into the tracker import
 * envelope: {@code {"trackedEntities":[line1,line2,...lineN]}}.
 *
 * <p>The caller must ensure that the total number of lines consumed ({@code linesPerRequest *
 * requestsPerUser * importUsers}) does not exceed {@code lineCount()}. If the feeder runs out of
 * lines mid-request, Gatling crashes and stops the entire simulation.
 *
 * <h3>Line counting</h3>
 *
 * The constructor decompresses the gzip file twice: once to count lines, once to open the streaming
 * reader. This is needed because Gatling's {@code feed(feeder, N)} requires exactly N records per
 * call, and we must know the total to calculate the repeat count.
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

  private final int lineCount;
  private final BufferedReader reader;
  private String pendingLine;

  NdjsonFeeder(Path ndjsonGzFile) {
    try {
      // First pass: count lines so we can calculate how many chunk-sized requests fit.
      // This decompresses the gzip once without retaining the data. Avoids needing a sidecar
      // metadata file or coupling chunk size to generation time.
      int count = 0;
      try (var countReader =
          new BufferedReader(
              new InputStreamReader(
                  new GZIPInputStream(new FileInputStream(ndjsonGzFile.toFile())),
                  StandardCharsets.UTF_8))) {
        while (countReader.readLine() != null) count++;
      }
      this.lineCount = count;

      // Second pass: open for streaming. Records are read one at a time by Gatling's
      // FeedActor as import requests are built.
      this.reader =
          new BufferedReader(
              new InputStreamReader(
                  new GZIPInputStream(new FileInputStream(ndjsonGzFile.toFile())),
                  StandardCharsets.UTF_8));
      this.pendingLine = reader.readLine();
      logger.debug("ndjson feeder initialized: {} ({} lines)", ndjsonGzFile, lineCount);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to open ndjson feeder: " + ndjsonGzFile, e);
    }
  }

  /** Total number of lines (records) in the file. */
  int lineCount() {
    return lineCount;
  }

  @Override
  public boolean hasNext() {
    return pendingLine != null;
  }

  @Override
  public Map<String, Object> next() {
    String line = pendingLine;
    try {
      pendingLine = reader.readLine();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read ndjson line", e);
    }
    return Map.of("payload", line);
  }
}
