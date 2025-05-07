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
package org.hisp.dhis.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.ToString;

/**
 * A helper to efficiently read lines from a {@link BufferedReader} and split them into column
 * values.
 *
 * @author Jan Bernitt
 */
@ToString
final class LineBuffer {

  private final List<String> header;
  private char[] buf;
  private int to;

  public static LineBuffer of(List<String> header) {
    return new LineBuffer(header);
  }

  private LineBuffer(List<String> header) {
    this.header = header;
    this.buf = new char[50 * header.size()];
  }

  boolean readLine(BufferedReader csv) throws IOException {
    int c;
    to = 0;
    while (!isEOL(c = csv.read())) {
      buf[to++] = (char) c;
      if (to >= buf.length) buf = Arrays.copyOf(buf, buf.length * 2);
    }
    return to > 0;
  }

  private boolean isEOL(int c) {
    return c == -1 || c == '\n' || c == '\r';
  }

  List<String> split() {
    int from = skipIndent(0);
    List<String> res = new ArrayList<>(header.size());
    while (from < to) {
      char c = buf[from];
      if (c == ',') {
        res.add(null);
        from = skipIndent(from + 1);
      } else if (c == '"') {
        int nextQuote = next('"', from + 1);
        res.add(str(from + 1, nextQuote));
        from = skipIndent(next(',', skipIndent(nextQuote + 1)));
      } else {
        int nextComma = next(',', from);
        res.add(str(from, nextComma));
        from = skipIndent(nextComma + 1);
      }
    }
    return res;
  }

  private String str(int from, int to) {
    return new String(buf, from, to - from);
  }

  private int next(char c, int from) {
    int i = from;
    while (i < to && buf[i] != c) i++;
    return i;
  }

  private int skipIndent(int from) {
    int i = from;
    while (i < to && (buf[i] == ' ' || buf[i] == '\t')) i++;
    return i;
  }
}
