/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi;

import static java.util.Collections.emptyList;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;

/**
 * Helper to stream write potentially deeply structured JSON directly to an {@link PrintWriter}
 * based on simple input data {@link Stream}s.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
public final class JsonWriter implements AutoCloseable {
  private final PrintWriter out;

  @Override
  public void close() {
    out.close();
  }

  /**
   * Writes structured JSON for a uniform sequence of potentially nesting members and a {@link
   * Stream} of entries each having a unique key and a list of values. Entry values correspond to
   * the given members by index.
   *
   * @param members a list of member names. Each member can use a nested path like {@code a.b.c}.
   *     Such nested members are written as JSON objects
   * @param entries a stream of entries, each entry has a unique key and a list of values
   *     corresponding to the members fields
   */
  public void writeEntries(
      List<String> members, Stream<? extends Entry<String, List<String>>> entries) {
    List<String> memberOpening = memberOpening(members);
    List<String> memberClosing = memberClosing(members);
    AtomicBoolean first = new AtomicBoolean(true);
    out.write("[");
    entries.forEachOrdered(
        entry -> {
          if (!first.compareAndSet(true, false)) {
            out.write(",");
          }
          out.write("{");
          out.write("\"key\":\"");
          out.write(entry.getKey());
          out.write('"');
          List<String> values = entry.getValue();
          for (int i = 0; i < values.size(); i++) {
            out.write(',');
            out.write(memberOpening.get(i));
            String value = values.get(i);
            out.write(value == null ? "null" : value);
            out.write(memberClosing.get(i));
          }
          out.write("}");
        });
    out.write("]");
  }

  private static List<String> memberOpening(List<String> members) {
    if (members.isEmpty()) {
      return emptyList();
    }
    List<String> openings = new ArrayList<>();
    String[] lastPath = new String[0];
    for (String member : members) {
      StringBuilder opening = new StringBuilder();
      String[] path = member.split("\\.");
      for (int i = 0; i < path.length; i++) {
        if (i >= lastPath.length || !Objects.equals(lastPath[i], path[i])) {
          if (i > 0 && (i >= lastPath.length || !Objects.equals(lastPath[i - 1], path[i - 1]))) {
            opening.append("{");
          }
          opening.append('"').append(path[i]).append("\":");
        }
      }
      openings.add(opening.toString());
      lastPath = path;
    }
    return openings;
  }

  private static List<String> memberClosing(List<String> members) {
    if (members.isEmpty()) {
      return emptyList();
    }
    List<String> closings = new ArrayList<>();
    for (int i = 0; i < members.size() - 1; i++) {
      String[] path = members.get(i).split("\\.");
      String[] nextPath = members.get(i + 1).split("\\.");
      int firstIndexNotEqual = firstIndexNotEqual(path, nextPath);
      closings.add(
          firstIndexNotEqual < path.length - 1
              ? "}".repeat(path.length - firstIndexNotEqual - 1)
              : "");
    }
    String[] path = members.get(members.size() - 1).split("\\.");
    closings.add(path.length > 1 ? "}".repeat(path.length - 1) : "");
    return closings;
  }

  private static int firstIndexNotEqual(String[] a, String[] b) {
    for (int i = 0; i < a.length; i++) {
      if (i >= b.length || !Objects.equals(a[i], b[i])) {
        return i;
      }
    }
    return a.length;
  }
}
