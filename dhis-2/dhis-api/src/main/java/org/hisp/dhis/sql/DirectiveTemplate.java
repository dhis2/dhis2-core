/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.sql;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import javax.annotation.CheckForNull;
import org.hisp.dhis.jsontree.Text;
import org.hisp.dhis.jsontree.TextBuilder;

/**
 * A general-purpose processor for {@code -- @directive} lines. Handles conditional blocks,
 * replacement blocks, reorder blocks and single-line insertions. Directives must be on their own
 * line as {@code -- @name}, except for {@code -- @end --@<any>} which resumes mid-line at the
 * embedded directive.
 *
 * <h3>Built-in directives</h3>
 *
 * <pre>
 *   -- @if :name ... [-- @else ...] -- @end
 *   -- @replace :name [replacement] ... -- @end
 *   -- @reorder :name ... -- @end     (one or more consecutive siblings)
 *   -- @insert :name
 * </pre>
 *
 * <p>{@code @end} closes the innermost open block, whether the block was opened by {@code @if},
 * {@code @replace} or {@code @reorder}. {@code @endif} is accepted as an alias of {@code @end}. A
 * bare {@code @replace} (no {@code :condition}) is not valid; use {@code @end} to close a
 * replacement block.
 *
 * <p>Consecutive {@code @reorder :name ... @end} blocks form a reorder sequence: they are emitted
 * in the order given by {@link #reorders} (name → ordinal), not in template order. The blocks must
 * be adjacent; any non-{@code @reorder} line ends the sequence.
 */
public record DirectiveTemplate(
    Predicate<Text> directives,
    Predicate<String> tests,
    Function<String, Text> inserts,
    ToIntFunction<String> reorders) {

  public Text render(Text template) {
    TextBuilder out = new TextBuilder((int) (template.length() * 1.5));
    End end = processBlock(template, 0, out, true);
    if (end.type() != null) {
      String msg =
          end.type == Directive.END
              ? "@end without opening directive"
              : "Closing @end is missing for: " + end.type;
      throw new IllegalStateException(msg);
    }
    return out.textValue();
  }

  private enum Directive {
    IF,
    ELSE,
    END,
    INSERT,
    REPLACE,
    REORDER;

    static Directive of(Text name) {
      if (name == null) return null;
      if (name.contentEquals("if")) return IF;
      if (name.contentEquals("else")) return ELSE;
      if (name.contentEquals("end") || name.contentEquals("endif")) return END;
      if (name.contentEquals("insert")) return INSERT;
      if (name.contentEquals("replace")) return REPLACE;
      if (name.contentEquals("reorder")) return REORDER;
      throw new IllegalStateException("Unknown directive @" + name);
    }

    @Override
    public String toString() {
      return "@" + name().toLowerCase(Locale.ROOT);
    }
  }

  private record End(Directive type, int pos) {}

  private record ReorderBlock(String name, int startPos, int endPos) {}

  private End processBlock(Text input, int startPos, TextBuilder out, boolean emit) {
    int len = input.length();
    int pos = startPos;
    outer:
    while (pos < len) {
      Text line = nextLine(input, pos);
      int nextLineStart = Math.min(pos + line.length() + 1, len);

      Directive directive = null;
      if (directives.test(line)) directive = Directive.of(directiveName(line));

      if (directive == null) {
        if (emit) {
          out.appendEncoded(line);
          out.appendAscii('\n');
        }
        pos = nextLineStart;
        continue;
      }

      switch (directive) {
        case IF -> {
          pos = processIf(input, out, emit, line, nextLineStart).pos();
          continue outer;
        }
        case ELSE -> {
          requireBareDirective(directive, line);
          return new End(Directive.ELSE, nextLineStart);
        }
        case END -> {
          // "end --@<any>" resumes the enclosing frame at the embedded directive,
          // which is how sibling chains (@end --@if :b) are expressed.
          int space = line.indexOf(' ', line.indexOf('@'));
          return new End(Directive.END, space < 0 ? nextLineStart : pos + space);
        }
        case INSERT -> processInsert(out, emit, line);
        case REPLACE -> {
          pos = processReplace(input, out, emit, line, nextLineStart).pos();
          continue outer;
        }
        case REORDER -> {
          pos = processReorder(input, out, emit, line, nextLineStart).pos();
          continue outer;
        }
        default -> throw new IllegalStateException("Unknown directive @" + directive + ": " + line);
      }
      pos = nextLineStart;
    }
    return new End(null, len);
  }

  private void processInsert(TextBuilder out, boolean emit, Text line) {
    String name = requireDirectiveName(Directive.INSERT, line);
    if (emit) appendNonEmpty(out, inserts.apply(name));
  }

  private static void appendNonEmpty(TextBuilder out, Text inserted) {
    if (inserted != null && !inserted.isEmpty()) {
      out.appendEncoded(inserted);
      out.appendAscii('\n');
    }
  }

  /**
   * Processes a {@code @if :cond ... @end} block, including an optional {@code @else} branch. The
   * returned {@link End} always has type {@link Directive#END}; if the inner block ends with
   * anything else (an unmatched {@code @else}, the top of the input, or a stray {@code @end}), this
   * method throws. Since {@code @end} closes the innermost open block regardless of whether it was
   * opened by {@code @if}, {@code @replace} or {@code @reorder}, an {@code @if} nesting once
   * started can only be closed by its own matching {@code @end}.
   */
  private End processIf(Text input, TextBuilder out, boolean emit, Text line, int nextLineStart) {
    boolean cond = evalConditionExpression(line);
    End inner = processBlock(input, nextLineStart, out, emit && cond);
    if (inner.type() == Directive.END) {
      return inner;
    }
    if (inner.type() == Directive.ELSE) {
      End after = processBlock(input, inner.pos(), out, emit && !cond);
      if (after.type() == Directive.END) {
        return after;
      }
      throw new IllegalStateException(
          "@else block did not close with @end but with " + after.type() + ": " + line);
    }
    throw new IllegalStateException(
        "@if block did not close with @end but with " + inner.type() + ": " + line);
  }

  private boolean evalConditionExpression(Text line) {
    Text name = directiveName(line);
    if (name == null) {
      throw new IllegalStateException(Directive.IF + " requires ':name' argument: " + line);
    }
    int afterName = line.indexOf('@') + 1 + name.length();
    Text expr = line.subSequence(afterName, line.length()).trim();
    if (expr.isEmpty()) {
      throw new IllegalStateException(Directive.IF + " requires ':name' argument: " + line);
    }
    validateExpression(expr, line);
    return evalOr(expr);
  }

  private void validateExpression(Text expr, Text line) {
    int i = 0, len = expr.length();
    boolean expectTerm = true;
    boolean inTerm = false;
    while (i < len) {
      char c = expr.charAt(i);
      if (c == '&' || c == '|') {
        if (i + 1 >= len || expr.charAt(i + 1) != c) {
          throw new IllegalStateException(
              "Single '" + c + "' in @if expression (use '" + c + c + "'): " + line);
        }
        if (expectTerm) {
          throw new IllegalStateException(
              "Unexpected '" + c + c + "' at start of @if expression: " + line);
        }
        expectTerm = true;
        inTerm = false;
        i += 2;
      } else if (c == ' ' || c == '\t') {
        if (inTerm) inTerm = false;
        i++;
      } else {
        if (!expectTerm && !inTerm) {
          throw new IllegalStateException(
              "Missing operator between terms in @if (use && or ||): " + line);
        }
        if (expectTerm && c != ':') {
          throw new IllegalStateException(Directive.IF + " requires ':name' argument: " + line);
        }
        expectTerm = false;
        inTerm = true;
        i++;
      }
    }
    if (expectTerm) {
      throw new IllegalStateException("Expression ends with operator: " + line);
    }
  }

  private boolean evalOr(Text expr) {
    int i = 0, len = expr.length(), start = 0;
    while (i < len - 1) {
      if (expr.charAt(i) == '|' && expr.charAt(i + 1) == '|') {
        if (evalAnd(expr.subSequence(start, i).trim())) return true;
        i += 2;
        start = i;
      } else {
        i++;
      }
    }
    return evalAnd(expr.subSequence(start, len).trim());
  }

  private boolean evalAnd(Text expr) {
    int i = 0, len = expr.length(), start = 0;
    while (i < len - 1) {
      if (expr.charAt(i) == '&' && expr.charAt(i + 1) == '&') {
        if (!evalTerm(expr.subSequence(start, i).trim())) return false;
        i += 2;
        start = i;
      } else {
        i++;
      }
    }
    return evalTerm(expr.subSequence(start, len).trim());
  }

  private boolean evalTerm(Text term) {
    String name =
        term.charAt(0) == ':' ? term.subSequence(1, term.length()).toString() : term.toString();
    return tests.test(name);
  }

  /**
   * Processes a {@code @replace :cond [repl]} block. If the condition is true, the opening line is
   * replaced (with the inline text after {@code :cond}, or with the registered {@code insert} by
   * name when there is no inline text), and the body is discarded. If the condition is false, the
   * body is emitted as-is. Either way the returned {@link End} has type {@link Directive#END}.
   *
   * <p>The discard path uses recursion with {@code emit=false} rather than an open/close counter.
   * This keeps the code identical for the two branches, naturally rejects misplaced {@code @else}
   * inside a replacement body, and reports structural errors with the line that actually caused
   * them. No writes happen because every write site in {@link #processBlock} is gated on {@code
   * emit}.
   */
  private End processReplace(
      Text input, TextBuilder out, boolean emit, Text line, int nextLineStart) {
    String condition = requireDirectiveName(Directive.REPLACE, line);
    boolean replace = tests.test(condition);

    if (replace && emit) {
      // Emit replacement: inline text after ":cond", or fall back to inserts.apply(cond).
      int s = line.indexOf(':');
      s = skipAlphanumeric(line, s + 1);
      int e = skipIndent(line, s);
      if (e == line.length()) {
        appendNonEmpty(out, inserts.apply(condition));
      } else {
        appendNonEmpty(out, line.subSequence(e, line.length()).trim());
      }
    }

    End inner = processBlock(input, nextLineStart, out, !replace && emit);
    if (inner.type() != Directive.END) {
      throw new IllegalStateException(
          "@replace block did not close with @end but with " + inner.type() + ": " + line);
    }
    return inner;
  }

  /**
   * Processes one or more consecutive {@code @reorder :name ... @end} blocks. All adjacent siblings
   * are collected, then (when {@code emit} is true) sorted by {@link #reorders} and emitted in that
   * order; when {@code emit} is false, only the boundaries are located and the position past the
   * last block is returned.
   *
   * <p>Discovery runs {@link #processBlock} with {@code emit=false}, so the block body is traversed
   * exactly as for emission (nested directives behave identically) but writes are suppressed.
   * Emission then re-traverses each block from its recorded start offset with {@code emit=true}.
   * Double traversal is the price for not buffering each block in a separate {@link TextBuilder};
   * block bodies are small so this is cheaper than the allocation would be.
   *
   * <p>The returned {@link End} has type {@link Directive#END} and its position points past the
   * last block's {@code @end}.
   */
  private End processReorder(
      Text input, TextBuilder out, boolean emit, Text line, int nextLineStart) {
    int len = input.length();
    List<ReorderBlock> blocks = new ArrayList<>();

    String firstName = requireDirectiveName(Directive.REORDER, line);
    blocks.add(collectReorder(input, out, firstName, nextLineStart, line));

    while (blocks.get(blocks.size() - 1).endPos() < len) {
      int pos = blocks.get(blocks.size() - 1).endPos();
      Text next = nextLine(input, pos);
      if (!directives.test(next) || Directive.of(directiveName(next)) != Directive.REORDER) {
        break;
      }
      String name = requireDirectiveName(Directive.REORDER, next);
      int startPos = Math.min(pos + next.length() + 1, len);
      blocks.add(collectReorder(input, out, name, startPos, next));
    }

    // get end of last before reordering
    int end = blocks.get(blocks.size() - 1).endPos();

    if (emit) {
      blocks.sort(Comparator.comparingInt(b -> reorders.applyAsInt(b.name())));
      for (ReorderBlock block : blocks) {
        processBlock(input, block.startPos(), out, true);
      }
    }
    return new End(Directive.END, end);
  }

  /** Consumes a single {@code @reorder} body (from {@code startPos} through its {@code @end}). */
  private ReorderBlock collectReorder(
      Text input, TextBuilder out, String name, int startPos, Text opener) {
    End end = processBlock(input, startPos, out, false);
    if (end.type() != Directive.END) {
      throw new IllegalStateException(
          "@reorder block '"
              + name
              + "' did not close with @end but with "
              + end.type()
              + ": "
              + opener);
    }
    return new ReorderBlock(name, startPos, end.pos());
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
    if (start + 1 == end) return null;
    return line.subSequence(start + 1, end);
  }

  private static String requireDirectiveName(Directive name, Text line) {
    int s = line.indexOf(':');
    if (s < 0) throw new IllegalStateException(name + " requires ':name' argument: " + line);
    int e = skipAlphanumeric(line, s + 1);
    if (e == s) throw new IllegalStateException("@name argument must have a letter: " + line);
    Text arg = line.subSequence(s + 1, e);
    return arg.toString();
  }

  private static void requireBareDirective(Directive name, Text line) {
    int i = line.indexOf('@');
    if (i < 0) return;
    i = skipAlphanumeric(line, i + 1);
    i = skipIndent(line, i);
    if (i < line.length()) throw new IllegalStateException(name + " takes no argument: " + line);
  }

  private static int skipAlphanumeric(Text line, int i) {
    int len = line.length();
    while (i < len && isAlphanumeric(line.charAt(i))) i++;
    return i;
  }

  private static int skipIndent(Text line, int i) {
    int len = line.length();
    while (i < len && isIndent(line.charAt(i))) i++;
    return i;
  }

  private static boolean isAlphanumeric(char c) {
    return 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z' || '0' <= c && c <= '9';
  }

  private static boolean isIndent(char c) {
    return c == ' ' || c == '\t';
  }
}
