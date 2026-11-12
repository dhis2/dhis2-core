/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.helpers.extensions;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Tags every REST-assured request with an {@code X-Request-ID} identifying the test phase that
 * issued it, so any server-side request-scoped diagnostics (application logs, MDC, SQL-context
 * comments) can be attributed back to the exact test — without touching individual tests, only the
 * shared request pipeline. The id for the current phase is held in a thread-local that {@link
 * RequestIdFilter} reads and sends as the header.
 *
 * <p>Coverage spans the whole class lifecycle, so class-level setup and cleanup are attributed too:
 *
 * <ul>
 *   <li>class setup (before any test) → {@code <ClassCapitals>_setup}
 *   <li>a test body → {@code <ClassCapitals>_<method>}
 *   <li>between tests and class teardown/cleanup → {@code <ClassCapitals>_teardown}
 * </ul>
 *
 * e.g. {@code MetadataImportExportControllerTest#getMetadata} → {@code MIECT_getMetadata}. Because
 * setup traffic is stamped in {@code beforeAll}, this extension must be registered <b>before</b>
 * any extension that issues requests during setup (e.g. {@code MetadataSetupExtension}).
 *
 * <p>Ids are constrained to the grammar the server accepts for {@code X-Request-ID} ({@code
 * [-_a-zA-Z0-9]{1,36}}): anything outside it is stripped and the result is capped at 36 characters.
 *
 * <p>Because the ids are abbreviated, a lookup mapping each id to its full {@code ClassName#method}
 * (or {@code ClassName (setup/teardown)}) label is written as JSON at the end of every class to
 * {@code target/request-id-map.json} (override with {@code -Dtest.requestIdMap=<path>}). Tooling
 * that reads the abbreviated ids back out of server output can use this file to restore
 * human-readable test names.
 */
public class RequestIdExtension
    implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {
  private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

  /** id -> full "ClassName#method" (or phase) label, accumulated across all test classes. */
  private static final Map<String, String> REGISTRY = new ConcurrentHashMap<>();

  /** DHIS2's RequestIdFilter accepts at most 36 characters. */
  private static final int MAX_LENGTH = 36;

  private static final String MAP_FILE =
      System.getProperty("test.requestIdMap", "target/request-id-map.json");

  @Override
  public void beforeAll(ExtensionContext context) {
    set(classPhaseId(context, "setup"), simpleName(context) + " (setup)");
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    String method = context.getTestMethod().map(Method::getName).orElse("");
    set(testId(context), simpleName(context) + (method.isEmpty() ? "" : "#" + method));
  }

  @Override
  public void afterEach(ExtensionContext context) {
    // Not removed: between-test and @AfterAll cleanup traffic (e.g. TestCleanUp
    // deletes) then carries the class-level teardown id rather than nothing.
    set(classPhaseId(context, "teardown"), simpleName(context) + " (teardown)");
  }

  @Override
  public void afterAll(ExtensionContext context) {
    writeRegistry();
    CURRENT.remove();
  }

  /** The current phase's request id, or {@code null} when none applies. */
  public static String currentRequestId() {
    return CURRENT.get();
  }

  private static void set(String id, String label) {
    CURRENT.set(id);
    if (id != null) {
      REGISTRY.put(id, label);
    }
  }

  private static String simpleName(ExtensionContext context) {
    return context.getTestClass().map(Class::getSimpleName).orElse("");
  }

  private static String classInitials(ExtensionContext context) {
    return simpleName(context).replaceAll("[^A-Z]", "");
  }

  private static String testId(ExtensionContext context) {
    String initials = classInitials(context);
    String method = context.getTestMethod().map(Method::getName).orElse("");
    String slug = initials;
    if (!method.isEmpty()) {
      slug = slug.isEmpty() ? method : slug + "_" + method;
    }
    return sanitize(slug);
  }

  private static String classPhaseId(ExtensionContext context, String phase) {
    String initials = classInitials(context);
    return sanitize(initials.isEmpty() ? phase : initials + "_" + phase);
  }

  private static String sanitize(String slug) {
    // Keep only characters DHIS2's RequestIdFilter allows, then cap the length.
    slug = slug.replaceAll("[^-_a-zA-Z0-9]", "");
    if (slug.isEmpty()) {
      return null;
    }
    return slug.length() > MAX_LENGTH ? slug.substring(0, MAX_LENGTH) : slug;
  }

  /** Writes the id -> label map as JSON. Best-effort: a failure never fails a test. */
  private static synchronized void writeRegistry() {
    if (REGISTRY.isEmpty()) {
      return;
    }
    List<String> lines = new ArrayList<>(REGISTRY.size());
    REGISTRY.forEach(
        (id, label) -> lines.add("  \"" + escape(id) + "\": \"" + escape(label) + "\""));
    String json = "{\n" + String.join(",\n", lines) + "\n}\n";
    try {
      Path path = Path.of(MAP_FILE);
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      Files.write(path, json.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      System.err.println("RequestIdExtension: could not write " + MAP_FILE + ": " + e.getMessage());
    }
  }

  private static String escape(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
