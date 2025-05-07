package org.hisp.dhis.csv;

import lombok.ToString;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A helper to efficiently read lines from a {@link BufferedReader} and split them into column values.
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
    this.buf = new char[200 +  50 * header.size()];
  }

  boolean readLine(BufferedReader csv) throws IOException {
    int c;
    to = 0;
    while (!isEnd(c = csv.read()))
      buf[to++] = (char)c;
    while (c != -1 && to == buf.length) {
      // adjust buffer size
      buf = Arrays.copyOf(buf, buf.length * 2);
      while (!isEnd(c = csv.read()))
        buf[to++] = (char)c;
    }
    return to > 0;
  }

  private boolean isEnd(int c) {
    return c == -1 || c == '\n' || c == '\r';
  }

  List<String> split() {
    int from = skipIndent(0);
    List<String> res = new ArrayList<>(header.size());
    while (from < to) {
      char c = buf[from];
      if (c == ',') {
        res.add(null);
        from = skipIndent(from+1);
      } else if (c == '"') {
        int nextQuote = next('"', from+1);
        res.add(str(from+1, nextQuote));
        from = skipIndent(next(',', skipIndent(nextQuote+1)));
      } else {
        int nextComma = next(',', from);
        res.add(str(from, nextComma));
        from = skipIndent(nextComma+1);
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
