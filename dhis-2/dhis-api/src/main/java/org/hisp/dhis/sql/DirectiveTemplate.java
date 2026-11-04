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

  private enum Directive {
    IF, ELSE, ENDIF, INSERT, REPLACE;

    static Directive of(Text name) {
      if (name == null) throw new IllegalArgumentException("Directive must not be null");
      if (name.contentEquals("if")) return IF;
      if (name.contentEquals("else")) return ELSE;
      if (name.contentEquals("endif")) return ENDIF;
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
    ELSE, ENDIF, REPLACE, EOF
  }

  private record End(EndType type, int pos) {}

  private End processBlock(Text input, int startPos, TextBuilder out, boolean emit) {
    int len = input.length();
    int pos = startPos;
    outer:
    while (pos < len) {
      Text line = nextLine(input, pos);
      int nextLineStart = Math.min(pos + line.length() + 1, len);

      if (!isDirectiveLine.test(line)) {
        if (emit) {
          out.appendEncoded(line);
          out.appendAscii('\n');
        }
        pos = nextLineStart;
        continue;
      }

      Directive directive = directive(line);

      switch (directive) {
        case IF -> {
          String name = requireDirectiveName(directive, line);
          boolean cond = isTrueCondition.test(name);
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
        case ELSE -> {
          requireBareDirective(directive, line);
          return new End(EndType.ELSE, nextLineStart);
        }
        case ENDIF -> {
          int space = line.indexOf(' ', line.indexOf('@'));
          return new End(EndType.ENDIF, space < 0 ? nextLineStart : pos + space);
        }
        case INSERT -> {
          String name = requireDirectiveName(directive, line);
          if (emit) {
            Text inserted = insert.apply(name);
            if (inserted != null) {
              out.appendEncoded(inserted);
              out.appendAscii('\n');
            }
          }
        }
        case REPLACE -> {
          int colonIdx = line.indexOf(':');
          if (colonIdx < 0) return new End(EndType.REPLACE, nextLineStart);
          String condition = requireDirectiveName(directive, line);

          if (isTrueCondition.test(condition)) {
            if (emit) {
              // Replace body with the text after ":name". Guarded against ":name" with
              // no replacement text (line.indexOf(' ', colonIdx) == -1); in that case
              // the whole block is simply erased.
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
          if (inner.type() != EndType.REPLACE)
            throw new IllegalStateException("Unclosed @replace in: " + line);
          pos = inner.pos();
          continue outer;
        }
        default ->
            throw new IllegalStateException("Unknown directive @" + directive + ": " + line);
      }
      pos = nextLineStart;
    }
    return new End(EndType.EOF, len);
  }

  private int skipToClosingReplace(Text input, int pos, Text opener) {
    int len = input.length();
    while (pos < len) {
      Text line = nextLine(input, pos);
      int nextLineStart = Math.min(pos + line.length() + 1, len);
      if (isDirectiveLine.test(line) && directive(line) == Directive.REPLACE) {
        requireBareDirective(Directive.REPLACE, line);
        return nextLineStart;
      }
      pos = nextLineStart;
    }
    throw new IllegalStateException("Unclosed @replace in: " + opener);
  }

  // ---- shared helpers ----

  private static Text nextLine(Text input, int pos) {
    int nl = input.indexOf('\n', pos);
    int lineEnd = nl < 0 ? input.length() : nl;
    return input.subSequence(pos, lineEnd);
  }

  private static Directive directive(Text line) {
    return Directive.of(directiveName(line));
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