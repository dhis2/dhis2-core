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
package org.hisp.dhis.webapi.openapi;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.hisp.dhis.jsontree.Appender;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.jsontree.JsonValue;

@RequiredArgsConstructor
public class JsonGenerator {

  @Value
  @Builder(toBuilder = true)
  public static class Format {
    public static final Format PRETTY_PRINT = new Format(true, true, true, true, true, "  ");
    public static final Format SMALL = new Format(false, false, false, false, false, "");

    boolean newLineBeforeItem;
    boolean newLineAfterObjectStart;
    boolean newLineBeforeObjectEnd;
    boolean newLineAfterArrayStart;
    boolean newLineBeforeArrayEnd;
    String itemIndent;
  }

  /**
   * Escapes a potentially multi-line text value to a target {@link Appender}.
   *
   * <p>The {@code contentIndent} is the indent to prefix each line of a block-scalar with. It is
   * only meaningful for languages that have a block-scalar syntax (currently YAML). Languages
   * without one (JSON, which escapes newlines in the string itself) ignore the parameter.
   */
  @FunctionalInterface
  public interface AppendTextBlock {
    void escape(Appender out, CharSequence text, String contentIndent);
  }

  @Value
  @Builder(toBuilder = true)
  public static class Language {
    public static final Language JSON =
        new Language(
            "[",
            "",
            "]",
            "",
            "[",
            ",",
            "]",
            "{",
            ":",
            "}",
            "",
            ",",
            Appender::appendJsonEscaped,
            (appender, text, contentIndent) -> appender.appendJsonEscaped(text),
            "",
            UnaryOperator.identity());

    public static final Language YAML =
        new Language(
            "",
            "- ",
            "",
            "[]",
            "[",
            ",",
            "]",
            "",
            ": ",
            "",
            "{}",
            "",
            JsonGenerator::escapeYamlString,
            JsonGenerator::escapeYamlText,
            "\n",
            format ->
                format.toBuilder()
                    .itemIndent("  ")
                    .newLineBeforeItem(true)
                    .newLineAfterArrayStart(false)
                    .newLineBeforeArrayEnd(false)
                    .newLineAfterObjectStart(false)
                    .newLineBeforeObjectEnd(false)
                    .build());

    String arrayStart;
    String arrayItemStart;
    String arrayEnd;
    String arrayEmpty;
    String arrayInlineStart;
    String arrayInlineItemSeparator;
    String arrayInlineEnd;
    String objectStart;
    String objectItemStart;
    String objectEnd;
    String objectEmpty;
    String objectItemSeparator;
    BiConsumer<Appender, CharSequence> appendEscapedInline;
    AppendTextBlock appendEscapedBlock;
    String documentEnd;
    UnaryOperator<Format> adjustFormat;
  }

  private final Appender out;
  private final Format format;
  private final Language language;

  /** Current object nesting depth (number of enclosing object levels). */
  private int level = 0;

  /**
   * Lazy cache of indent strings, indexed by depth. {@code indentCache[n]} is {@code
   * format.itemIndent.repeat(n)} once computed. Grows on demand.
   */
  private String[] indentCache;

  private boolean arrayItemLast = false;
  private boolean atLineStart = true;
  private boolean firstItemAtLevel = true;
  private boolean nothingWritten = true;

  final void addRootObject(Runnable addMembers) {
    addObjectMember(null, addMembers);
    write(language.documentEnd);
  }

  final void addObjectMember(CharSequence name, boolean condition, Runnable addMembers) {
    if (condition) addObjectMember(name, addMembers);
  }

  final <E> void addObjectMember(CharSequence name, Collection<E> members, Consumer<E> forEach) {
    if (!members.isEmpty()) {
      addObjectMember(name, () -> members.forEach(forEach));
    }
  }

  final void addObjectMember(CharSequence name, Runnable addMembers) {
    appendMemberName(name);
    write(language.objectStart);
    if (format.isNewLineAfterObjectStart()) appendNewline();
    boolean outerFirst = firstItemAtLevel;
    firstItemAtLevel = true;
    if (format.isNewLineBeforeItem()) level++;
    appendItems(addMembers);
    if (format.isNewLineBeforeItem()) level--;
    boolean innerEmpty = firstItemAtLevel;
    firstItemAtLevel = outerFirst;
    if (innerEmpty) write(language.objectEmpty);
    if (format.isNewLineBeforeObjectEnd()) {
      appendNewline();
      appendMemberIndent();
    }
    write(language.objectEnd);
    finishItem();
  }

  private void appendNewline() {
    if (!atLineStart) {
      out.appendAscii('\n');
      atLineStart = true;
      nothingWritten = false;
    }
  }

  final void addArrayMember(CharSequence name, Collection<? extends CharSequence> values) {
    if (values.isEmpty()) return;
    addArrayMember(name, values, value -> addStringMember(null, value));
  }

  final <E> void addArrayMember(CharSequence name, Collection<E> items, Consumer<E> forEach) {
    if (items.isEmpty()) return;
    addArrayMember(name, () -> items.forEach(forEach));
  }

  final void addArrayMember(CharSequence name, Runnable addElements) {
    appendMemberName(name);
    write(language.arrayStart);
    if (format.isNewLineAfterArrayStart()) appendNewline();
    boolean outerFirst = firstItemAtLevel;
    firstItemAtLevel = true;
    if (format.isNewLineBeforeItem()) level++;
    appendItems(addElements);
    if (format.isNewLineBeforeItem()) level--;
    boolean innerEmpty = firstItemAtLevel;
    firstItemAtLevel = outerFirst;
    if (innerEmpty) write(language.arrayEmpty);
    if (format.isNewLineBeforeArrayEnd()) {
      appendNewline();
      appendMemberIndent();
    }
    write(language.arrayEnd);
    finishItem();
  }

  final void addInlineArrayMember(CharSequence name, Collection<? extends CharSequence> values) {
    if (values.isEmpty()) return;
    appendMemberName(name);
    write(language.arrayInlineStart);
    int i = 0;
    for (CharSequence value : values) {
      if (i > 0) write(language.arrayInlineItemSeparator);
      appendStringInline(value);
      i++;
    }
    write(language.arrayInlineEnd);
    finishItem();
  }

  final void addStringMember(CharSequence name, CharSequence value) {
    if (value != null) {
      appendMemberName(name);
      appendStringInline(value);
      finishItem();
    }
  }

  final void addStringMultilineMember(CharSequence name, CharSequence value) {
    if (value != null && !value.isEmpty()) {
      appendMemberName(name);
      appendStringBlock(value);
      finishItem();
    }
  }

  final void addTrueMember(CharSequence name, Boolean value) {
    if (value == Boolean.TRUE) addBooleanMember(name, true);
  }

  final void addBooleanMember(CharSequence name, Boolean value) {
    if (value != null) addBooleanMember(name, value.booleanValue());
  }

  final void addBooleanMember(CharSequence name, boolean value) {
    appendMemberName(name);
    write(value ? "true" : "false");
    finishItem();
  }

  final void addNumberMember(CharSequence name, Number value) {
    if (value != null) {
      if (value instanceof Double && value.doubleValue() % 1.0d != 0d) {
        addNumberMember(name, value.doubleValue());
      } else {
        addNumberMember(name, value.intValue());
      }
    }
  }

  final void addNumberMember(CharSequence name, Integer value) {
    if (value != null) addNumberMember(name, value.intValue());
  }

  final void addNumberMember(CharSequence name, double value) {
    appendMemberName(name);
    out.appendDouble(value);
    atLineStart = false;
    nothingWritten = false;
    finishItem();
  }

  final void addNumberMember(CharSequence name, int value) {
    appendMemberName(name);
    out.appendInt(value);
    atLineStart = false;
    nothingWritten = false;
    finishItem();
  }

  final void addRawMember(CharSequence name, JsonValue value) {
    if (value == null || value.isUndefined()) return;
    if (value.isString() && value.as(JsonString.class).string("").isEmpty()) return;
    appendMemberName(name);
    // The declaration is already valid JSON, so write it as-is rather than through
    // appendJsonEscaped (which would quote the whole thing as a string literal).
    writeEncoded(value.node().getDeclaration());
    finishItem();
  }

  private void appendItems(Runnable items) {
    if (items != null) items.run();
  }

  private void appendStringInline(CharSequence str) {
    if (str == null) {
      write("null");
      return;
    }
    language.appendEscapedInline.accept(out, str);
    atLineStart = false;
    nothingWritten = false;
  }

  private void appendStringBlock(CharSequence str) {
    if (str == null) {
      write("null");
      return;
    }
    language.appendEscapedBlock.escape(out, str, indentAt(level + 1));
    atLineStart = false;
    nothingWritten = false;
  }

  private void appendMemberName(CharSequence name) {
    if (name == null && nothingWritten) return; // root: nothing to write here
    if (!firstItemAtLevel) write(language.objectItemSeparator);
    appendMemberIndent();
    firstItemAtLevel = false;
    if (name != null) {
      arrayItemLast = false;
      appendStringInline(name);
      write(language.objectItemStart);
    } else {
      arrayItemLast = !language.arrayItemStart.isEmpty();
      write(language.arrayItemStart);
    }
  }

  private void appendMemberIndent() {
    if (!arrayItemLast && format.isNewLineBeforeItem()) {
      appendNewline();
      out.appendAscii(indentAt(level));
      atLineStart = false;
      nothingWritten = false;
    }
  }

  private void finishItem() {
    arrayItemLast = false;
  }

  /**
   * Returns the indent string for the given depth, lazily computing and caching it. For depths
   * beyond the current cache capacity the cache array is grown. When {@code itemIndent} is empty
   * (e.g. {@link Format#SMALL}) this short-circuits to {@code ""} without touching the cache.
   */
  private String indentAt(int level) {
    String itemIndent = format.getItemIndent();
    if (itemIndent.isEmpty() || level <= 0) return "";
    if (indentCache == null) {
      indentCache = new String[8];
    } else if (level >= indentCache.length) {
      indentCache = Arrays.copyOf(indentCache, Math.max(level + 1, indentCache.length * 2));
    }
    String cached = indentCache[level];
    if (cached == null) {
      cached = itemIndent.repeat(level);
      indentCache[level] = cached;
    }
    return cached;
  }

  /*
   * Write helpers that keep atLineStart / nothingWritten in sync with what has actually been
   * written. These replace the previous direct use of StringBuilder-specific methods
   * (isEmpty, charAt, length) with state that is maintained alongside the writes.
   */

  private void write(CharSequence s) {
    if (s.isEmpty()) return;
    out.appendAscii(s);
    atLineStart = s.charAt(s.length() - 1) == '\n';
    nothingWritten = false;
  }

  private void writeEncoded(CharSequence s) {
    if (s.isEmpty()) return;
    out.appendEncoded(s);
    atLineStart = s.charAt(s.length() - 1) == '\n';
    nothingWritten = false;
  }

  private static final char[] HEX = "0123456789ABCDEF".toCharArray();

  private static void escapeYamlString(Appender out, CharSequence text) {
    // Always emit YAML strings as double-quoted. Plain scalars have too many
    // context-dependent rules (case-insensitive true/false/null/yes/no/on/off/~,
    // leading indicators -, ?, :, #, *, &, !, |, >, %, @, `, [, ], {, }, comma,
    // ": " and " #" separators at any position, leading/trailing whitespace) and
    // getting any of them wrong silently changes the loaded type or makes the
    // document un-parseable. Quoting makes the emitted type unambiguously a string.
    // This is a machine-readable OpenAPI document, so prioritise correctness over
    // human readability.
    out.appendAscii('"');
    int len = text.length();
    int runStart = 0;
    for (int i = 0; i < len; i++) {
      char c = text.charAt(i);
      if (needsYamlEscape(c)) {
        if (i > runStart) out.appendEncoded(text, runStart, i);
        writeYamlEscape(out, c);
        runStart = i + 1;
      }
    }
    if (runStart < len) out.appendEncoded(text, runStart, len);
    out.appendAscii('"');
  }

  /**
   * Whether a character must be escaped inside a YAML double-quoted scalar. Only structural
   * characters need escaping — non-ASCII text (including supplementary-plane codepoints) is valid
   * as-is inside a double-quoted YAML string and is handled by the Appender's UTF-8 encoding.
   */
  private static boolean needsYamlEscape(char c) {
    return c < 0x20
        || c == 0x7F
        || c == '\\'
        || c == '"'
        || c == '\u0085' // NEL, a YAML line terminator
        || c == '\u2028' // LS, a YAML line terminator
        || c == '\u2029'; // PS, a YAML line terminator
  }

  /**
   * Writes the YAML double-quoted escape sequence for the given character. Uses the short escapes
   * where they exist ({@code \n}, {@code \t}, {@code \r}, {@code \0}, {@code \\}, {@code \"}) for
   * readability, and {@code \\uXXXX} for everything else. No {@code String} is allocated — all
   * output goes through {@link Appender#appendAscii(char)} one byte at a time, which is the
   * cheapest path a byte sink can offer.
   */
  private static void writeYamlEscape(Appender out, char c) {
    switch (c) {
      case '\\' -> out.appendAscii("\\\\");
      case '"' -> out.appendAscii("\\\"");
      case '\n' -> out.appendAscii("\\n");
      case '\t' -> out.appendAscii("\\t");
      case '\r' -> out.appendAscii("\\r");
      case '\0' -> out.appendAscii("\\0");
      default -> {
        out.appendAscii('\\');
        out.appendAscii('u');
        out.appendAscii(HEX[(c >> 12) & 0xF]);
        out.appendAscii(HEX[(c >> 8) & 0xF]);
        out.appendAscii(HEX[(c >> 4) & 0xF]);
        out.appendAscii(HEX[c & 0xF]);
      }
    }
  }

  private static void escapeYamlText(Appender out, CharSequence text, String contentIndent) {
    // Block scalar in strip mode |- so trailing newlines are not preserved. Used for every
    // addStringMultilineMember value regardless of whether the content contains a newline:
    // a single-line value emitted as a plain scalar is subject to YAML's plain-scalar
    // grammar, which rejects anything that starts with ?, :, -, [ ], { }, &, *, !, |, >, %,
    // @, `, #, or contains ": " or " #", or has leading/trailing whitespace. The block
    // scalar form has no such restrictions.
    out.appendAscii("|-\n");
    int start = 0;
    int end;
    while ((end = indexOf('\n', start, text)) >= 0) {
      if (end > start) {
        out.appendAscii(contentIndent);
        out.appendEncoded(text, start, end);
      }
      out.appendAscii('\n');
      start = end + 1;
    }
    int len = text.length();
    if (start < len) {
      out.appendAscii(contentIndent);
      out.appendEncoded(text, start, len);
    }
  }

  private static int indexOf(int c, int start, CharSequence cs) {
    int len = cs.length();
    for (int i = start; i < len; i++) if (cs.charAt(i) == c) return i;
    return -1;
  }
}
