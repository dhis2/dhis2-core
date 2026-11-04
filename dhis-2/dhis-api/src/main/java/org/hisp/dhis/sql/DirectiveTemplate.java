package org.hisp.dhis.sql;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.CheckForNull;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.jsontree.TextBuilder;

/**
 * A general-purpose processor for {@code -- @directive} lines. Handles conditional blocks,
 * replacement blocks and single-line insertions. Directives must be on their own line as
 * {@code -- @name}, except for {@code -- @end --@<any>} which resumes mid-line at the
 * embedded directive.
 *
 * <h3>Built-in directives</h3>
 * <pre>
 *   -- @if :name ... [-- @else ...] -- @end
 *   -- @replace :name [replacement] ... -- @end
 *   -- @insert :name
 * </pre>
 *
 * <p>{@code @end} closes the innermost open block, whether the block was opened by
 * {@code @if} or {@code @replace}. {@code @endif} is accepted as an alias of {@code @end}.
 * A bare {@code @replace} (no {@code :condition}) is no longer valid; use {@code @end} to
 * close a replacement block.
 */
public record DirectiveTemplate(
    Predicate<Text> directives,
    Predicate<String> tests,
    Function<String, Text> inserts) {

  public Text render(Text template) {
    TextBuilder out = new TextBuilder((int) (template.length() * 1.5));
    End end = processBlock(template, 0, out, true);
    if (end.type() != EndType.EOF) {
      throw new IllegalStateException(
          "Unbalanced directive at top level: " + end.type().name().toLowerCase());
    }
    return out.textValue();
  }

  private enum Directive {
    IF, ELSE, END, INSERT, REPLACE;

    static Directive of(Text name) {
      if (name == null) throw new IllegalArgumentException("Directive must not be null");
      if (name.contentEquals("if")) return IF;
      if (name.contentEquals("else")) return ELSE;
      if (name.contentEquals("end") || name.contentEquals("endif")) return END;
      if (name.contentEquals("insert")) return INSERT;
      if (name.contentEquals("replace")) return REPLACE;
      throw new IllegalStateException("Unknown directive @" + name);
    }

    @Override
    public String toString() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  private enum EndType {
    ELSE, END, EOF
  }

  private record End(EndType type, int pos) {}

  private End processBlock(Text input, int startPos, TextBuilder out, boolean emit) {
    int len = input.length();
    int pos = startPos;
    outer:
    while (pos < len) {
      Text line = nextLine(input, pos);
      int nextLineStart = Math.min(pos + line.length() + 1, len);

      if (!directives.test(line)) {
        if (emit) {
          out.appendEncoded(line);
          out.appendAscii('\n');
        }
        pos = nextLineStart;
        continue;
      }

      Directive directive = Directive.of(directiveName(line));
      switch (directive) {
        case IF -> {
          pos = processIf(input, out, emit, line, nextLineStart).pos();
          continue outer;
        }
        case ELSE -> {
          requireBareDirective(directive, line);
          return new End(EndType.ELSE, nextLineStart);
        }
        case END -> {
          // "end --@<any>" resumes the enclosing frame at the embedded directive,
          // which is how sibling chains (@end --@if :b) are expressed.
          int space = line.indexOf(' ', line.indexOf('@'));
          return new End(EndType.END, space < 0 ? nextLineStart : pos + space);
        }
        case INSERT -> {
          String name = requireDirectiveName(directive, line);
          if (emit) {
            Text inserted = inserts.apply(name);
            if (inserted != null) {
              out.appendEncoded(inserted);
              out.appendAscii('\n');
            }
          }
        }
        case REPLACE -> {
          pos = processReplace(input, out, emit, line, nextLineStart).pos();
          continue outer;
        }
        default ->
            throw new IllegalStateException("Unknown directive @" + directive + ": " + line);
      }
      pos = nextLineStart;
    }
    return new End(EndType.EOF, len);
  }

  /**
   * Processes a {@code @if :cond ... @end} block, including an optional {@code @else}
   * branch. The returned {@link End} always has type {@link EndType#END}; if the inner
   * block ends with anything else (an unmatched {@code @else}, the top of the input, or a
   * stray {@code @end}), this method throws. Since {@code @end} closes the innermost open
   * block regardless of whether it was opened by {@code @if} or {@code @replace}, an
   * {@code @if} nesting once started can only be closed by its own matching {@code @end}.
   */
  private End processIf(
      Text input, TextBuilder out, boolean emit, Text line, int nextLineStart) {
    String name = requireDirectiveName(Directive.IF, line);
    boolean cond = tests.test(name);
    End inner = processBlock(input, nextLineStart, out, emit && cond);
    if (inner.type() == EndType.END) {
      return inner;
    }
    if (inner.type() == EndType.ELSE) {
      End after = processBlock(input, inner.pos(), out, emit && !cond);
      if (after.type() == EndType.END) {
        return after;
      }
      throw new IllegalStateException(
          "@else block did not close with @end but with " + after.type() + ": " + line);
    }
    throw new IllegalStateException(
        "@if block did not close with @end but with " + inner.type() + ": " + line);
  }

  /**
   * Processes a {@code @replace :cond [repl]} block. If the condition is true, the opening
   * line is replaced (with the inline text after {@code :cond}, or with the registered
   * {@code insert} by name when there is no inline text), and the body is discarded. If
   * the condition is false, the body is emitted as-is. Either way the returned {@link End}
   * has type {@link EndType#END}.
   *
   * <p>The discard path uses recursion with a throwaway sink rather than an open/close
   * counter. This keeps the code identical for the two branches, naturally rejects
   * misplaced {@code @else} inside a replacement body, and reports structural errors with
   * the line that actually caused them.
   */
  private End processReplace(
      Text input, TextBuilder out, boolean emit, Text line, int nextLineStart) {
    String condition = requireDirectiveName(Directive.REPLACE, line);
    boolean replace = tests.test(condition);

    if (replace && emit) {
      // Emit replacement: inline text after ":cond", or fall back to inserts.apply(cond).
      int s = line.indexOf(':');
      s = skipLetters(line, s + 1);
      int e = skipIndent(line, s);
      if (e == line.length()) {
        Text fromInsert = inserts.apply(condition);
        if (fromInsert != null) out.appendEncoded(fromInsert);
      } else {
        out.appendEncoded(line.subSequence(e, line.length()).trim());
      }
      out.appendAscii('\n');
    }

    End inner = processBlock(input, nextLineStart, out, !replace && emit);
    if (inner.type() != EndType.END) {
      throw new IllegalStateException(
          "@replace block did not close with @end but with " + inner.type() + ": " + line);
    }
    return inner;
  }

  // ---- shared helpers ----

  private static Text nextLine(Text input, int pos) {
    int nl = input.indexOf('\n', pos);
    int lineEnd = nl < 0 ? input.length() : nl;
    return input.subSequence(pos, lineEnd);
  }

  @CheckForNull
  static Text directiveName(Text line) {
    int start = line.indexOf('@');
    if (start < 0) return null;
    int end = line.indexOf(' ', start);
    if (end < 0) end = line.length();
    return line.subSequence(start + 1, end);
  }

  private static String requireDirectiveName(Directive name, Text line) {
    int s = line.indexOf(':');
    if (s < 0)
      throw new IllegalStateException("@" + name + " requires ':name' argument: " + line);
    int e = skipLetters(line, s + 1);
    if (e == s)
      throw new IllegalStateException("@name argument must have a letter: " + line);
    Text arg = line.subSequence(s + 1, e);
    return arg.toString();
  }

  private static void requireBareDirective(Directive name, Text line) {
    int i = line.indexOf('@');
    if (i < 0) return;
    i = skipLetters(line, i + 1);
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