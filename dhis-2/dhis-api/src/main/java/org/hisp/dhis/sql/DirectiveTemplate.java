package org.hisp.dhis.sql;

import java.util.function.Function;
import java.util.function.Predicate;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.jsontree.TextBuilder;

/**
 * A general-purpose processor for {@code -- @directive} lines. Handles conditional blocks,
 * replacement blocks and single-line insertions. Directives must be on their own line as
 * {@code -- @name}, except for {@code -- @endif --@<any>} which resumes mid-line at the
 * embedded directive.
 *
 * <h3>Built-in directives</h3>
 * <pre>
 *   -- @if :name ... [-- @else ...] -- @endif
 *   -- @replace :name [replacement] ... -- @replace
 *   -- @insert :name
 * </pre>
 */
public record DirectiveTemplate(
    Predicate<Text> isDirectiveLine,
    Predicate<String> isTrueCondition,
    Function<String, Text> insert) {

  public Text render(Text template) {
    TextBuilder out = new TextBuilder((int) (template.length() * 1.5));
    End end = processBlock(template, 0, out, true);
    if (end.type() != EndType.EOF) {
      throw new IllegalStateException(
          "Unbalanced directive at top level: " + end.type().name().toLowerCase());
    }
    return out.textValue();
  }

  private enum EndType {
    ELSE, ENDIF, REPLACE, EOF
  }

  private record End(EndType type, int pos) {}

  private End processBlock(Text input, int startPos, TextBuilder out, boolean emit) {
    int len = input.length();
    int pos = startPos;
    outer:
    while (pos < len) {
      int nl = input.indexOf('\n', pos);
      int lineEnd = nl < 0 ? len : nl;
      int nextLineStart = nl < 0 ? len : nl + 1;
      Text line = input.subSequence(pos, lineEnd);

      if (!isDirectiveLine.test(line)) {
        if (emit) {
          out.appendEncoded(line);
          out.appendAscii('\n');
        }
        pos = nextLineStart;
        continue;
      }

      String name = directive(line);

      switch (name) {
        case "if" -> {
          String arg = requireConditionName(name, line);
          boolean cond = isTrueCondition.test(arg);
          End inner = processBlock(input, nextLineStart, out, emit && cond);
          if (inner.type() == EndType.ENDIF) {
            pos = inner.pos();
            continue outer;
          }
          if (inner.type() == EndType.ELSE) {
            End after = processBlock(input, inner.pos(), out, emit && !cond);
            if (after.type() == EndType.ENDIF) {
              pos = after.pos();
              continue outer;
            }
            throw new IllegalStateException("Duplicate or unclosed @else in: " + line);
          }
          throw new IllegalStateException("Unclosed @if in: " + line);
        }
        case "else" -> {
          requireBareDirective(name, line);
          return new End(EndType.ELSE, nextLineStart);
        }
        case "endif" -> {
          int space = line.indexOf(' ', line.indexOf('@'));
          return new End(EndType.ENDIF, space < 0 ? nextLineStart : pos+space );
        }
        case "insert" -> {
          String arg = requireConditionName(name, line);
          if (emit) {
            Text inserted = insert.apply(arg);
            if (inserted != null) {
              out.appendEncoded(inserted);
              out.appendAscii('\n');
            }
          }
        }
        case "replace" -> {
          int colonIdx = line.indexOf(':');
          if (colonIdx < 0) return new End(EndType.REPLACE, nextLineStart);
          String arg = requireConditionName(name, line);

          if (isTrueCondition.test(arg)) {
            if (emit) {
              // Replace body with the text after ":name". Guarded against ":name" with
              // no replacement text (rest.indexOf(' ') == -1); in that case the whole
              // block is simply erased.
              int spaceIdx = line.indexOf(' ', colonIdx);
              if (spaceIdx >= 0) {
                Text replacement = line.subSequence(spaceIdx, line.length()).trim();
                if (!replacement.isEmpty()) {
                  out.appendEncoded(replacement);
                  out.appendAscii('\n');
                }
              }
            }
            pos = skipToClosingReplace(input, nextLineStart, line);
            continue outer;
          }

          // Keep the block: process it normally. The recursion will hit the closing bare
          // @replace and return End(REPLACE, …) at the right position.
          End inner = processBlock(input, nextLineStart, out, emit);
          if (inner.type() != EndType.REPLACE) {
            throw new IllegalStateException("Unclosed @replace in: " + line);
          }
          pos = inner.pos();
          continue outer;
        }
        default ->
            throw new IllegalStateException("Unknown directive @" + name + ": " + line);
      }
      pos = nextLineStart;
    }
    return new End(EndType.EOF, len);
  }

  /**
   * Skips raw lines forward from {@code from} until a bare {@code @replace} directive is
   * found (a {@code replace} directive with no argument). Returns the position immediately
   * after that line. Throws if the end of input is reached first.
   */
  private int skipToClosingReplace(Text input, int from, Text opener) {
    int len = input.length();
    int scanPos = from;
    while (scanPos < len) {
      int nl = input.indexOf('\n', scanPos);
      int lineEnd = nl < 0 ? len : nl;
      int next = nl < 0 ? len : nl + 1;
      Text scanLine = input.subSequence(scanPos, lineEnd);
      if (isDirectiveLine.test(scanLine)
          && "replace".equals(directive(scanLine))
          && directiveArgs(scanLine, "replace").isEmpty()) {
        return next;
      }
      scanPos = next;
    }
    throw new IllegalStateException("Unclosed @replace in: " + opener);
  }

  // ---- shared static helpers (package-private for reuse by DirectiveQueryBuilder) ----

  static String directive(Text line) {
    int start = line.indexOf('@');
    if (start < 0) return null;
    int end = line.indexOf(' ', start);
    if (end < 0) end = line.length();
    return line.subSequence(start + 1, end).toString();
  }

  /**
   * Returns the argument text after the directive name, trimmed. For {@code -- @if :foo}
   * with {@code name="if"}, this is {@code ":foo"}. For {@code -- @endif} or
   * {@code -- @endif --@if :x}, this is {@code ""} or {@code "--@if :x"} respectively.
   */
  static Text directiveArgs(Text line, String name) {
    int nameStart = line.indexOf('@');
    int argStart =
        Math.max(nameStart + 1 + name.length(), indexOfWhitespace(line, nameStart) + 1);
    return line.subSequence(Math.min(argStart, line.length()), line.length()).trim();
  }

  static int indexOfWhitespace(Text t, int from) {
    for (int i = from; i < t.length(); i++) if (Character.isWhitespace(t.charAt(i))) return i;
    return -1;
  }

  private static String requireConditionName(String name, Text line) {
    int s = line.indexOf(':');
    if (s < 0)
      throw new IllegalStateException("@" + name + " requires ':name' argument: " + line);
    int e = skipLetters(line, s+1);
    if (e == s)
      throw new IllegalStateException("@name argument must have a letter: "+line);
    Text arg = line.subSequence(s+1, e);
    return arg.toString();
  }

  private static void requireBareDirective(String name, Text line) {
    int i = line.indexOf('@');
    if (i < 0) return;
    i = skipLetters(line, i+1);
    i = skipIndent(line, i);
    if (i < line.length())
      throw new IllegalStateException("@" + name + " takes no argument: " + line);
  }

  private static int skipLetters(Text line, int i) {
    int len = line.length();
    while (i < len && isLetter(line.charAt(i))) i++;
    return i;
  }

  private static int skipIndent(Text line, int i) {
    int len = line.length();
    while (i < len && isIndent(line.charAt(i))) i++;
    return i;
  }

  private static boolean isLetter(char c) {
    return 'a' <= c && c <= 'z';
  }

  private static boolean isIndent(char c) {
    return c == ' ' || c == '\t';
  }
}