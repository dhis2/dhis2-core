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
package org.hisp.dhis.webapi.openapi;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * A utility to generate JSON based on simple {@link Runnable} callbacks for nesting of arrays and
 * objects.
 *
 * @author Jan Bernitt
 */
@RequiredArgsConstructor
public class JsonGenerator {
  @Value
  @Builder(toBuilder = true)
  static class Format {
    public static final Format PRETTY_PRINT = new Format(true, true, true, true, true, "  ");

    public static final Format SMALL = new Format(false, false, false, false, false, "");

    /** Item being an object member or an array element */
    boolean newLineBeforeItem;

    boolean newLineAfterObjectStart;

    boolean newLineBeforeObjectEnd;

    boolean newLineAfterArrayStart;

    boolean newLineBeforeArrayEnd;

    /** 1 level of indent */
    String itemIndent;
  }

  @Value
  @Builder(toBuilder = true)
  static class Language {

    public static final Language JSON =
        new Language(
            "[",
            "",
            "]",
            "",
            "{",
            ":",
            "}",
            "",
            ",",
            "\"",
            "\"",
            JsonGenerator::escapeJsonString,
            "\"",
            "\"",
            false,
            JsonGenerator::escapeJsonText,
            UnaryOperator.identity());

    public static final Language YAML =
        new Language(
            "",
            "- ",
            "",
            "[]",
            "",
            ": ",
            "",
            "{}",
            "",
            "",
            "",
            JsonGenerator::escapeYamlString,
            "|-",
            "",
            true,
            JsonGenerator::escapeYamlText,
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

    String objectStart;

    String objectItemStart;

    String objectEnd;

    String objectEmpty;

    String itemSeparator;

    String stringStart;

    String stringEnd;

    UnaryOperator<String> escapeString;

    String textStart;

    String textEnd;

    boolean textIndent;

    Function<String, List<String>> escapeText;

    UnaryOperator<Format> adjustFormat;
  }

  private static final JsonStringEncoder JSON_ESCAPE_STRING = JsonStringEncoder.getInstance();

  private final StringBuilder out;

  private final Format format;

  private final Language language;

  private String indent = "";

  private boolean arrayItemLast = false;

  @Override
  public final String toString() {
    return out.toString();
  }

  final void addRootObject(Runnable addMembers) {
    addObjectMember(null, addMembers);
    discardLastMemberSeparator(0, language.objectEmpty);
  }

  final void addObjectMember(String name, boolean condition, Runnable addMembers) {
    if (condition) {
      addObjectMember(name, addMembers);
    }
  }

  final <E> void addObjectMember(String name, Collection<E> members, Consumer<E> forEach) {
    if (!members.isEmpty()) {
      addObjectMember(name, () -> members.forEach(forEach));
    }
  }

  final void addObjectMember(String name, Runnable addMembers) {
    appendMemberName(name);
    out.append(language.objectStart);
    if (format.isNewLineAfterObjectStart()) out.append('\n');
    int length = out.length();
    if (format.isNewLineBeforeItem()) indent += format.getItemIndent();
    appendItems(addMembers);
    if (format.isNewLineBeforeItem())
      indent = indent.substring(0, indent.length() - format.getItemIndent().length());
    discardLastMemberSeparator(length, language.objectEmpty);
    if (format.isNewLineBeforeObjectEnd()) {
      out.append('\n');
      appendMemberIndent();
    }
    out.append(language.objectEnd);
    appendMemberSeparator();
  }

  final void addArrayMember(String name, Collection<String> values) {
    addArrayMember(name, values, value -> addStringMember(null, value));
  }

  final <E> void addArrayMember(String name, Collection<E> items, Consumer<E> forEach) {
    if (!items.isEmpty()) {
      addArrayMember(name, () -> items.forEach(forEach));
    }
  }

  final void addArrayMember(String name, Runnable addElements) {
    appendMemberName(name);
    out.append(language.arrayStart);
    if (format.isNewLineAfterArrayStart()) out.append('\n');
    int length = out.length();
    appendItems(addElements);
    discardLastMemberSeparator(length, language.arrayEmpty);
    if (format.isNewLineBeforeArrayEnd()) {
      out.append('\n');
      appendMemberIndent();
    }
    out.append(language.arrayEnd);
    appendMemberSeparator();
  }

  final void addStringMember(String name, String value) {
    if (value != null) {
      appendMemberName(name);
      appendString(value);
      appendMemberSeparator();
    }
  }

  final void addStringMultilineMember(String name, String value) {
    if (value != null) {
      appendMemberName(name);
      appendStringMultiline(value);
      appendMemberSeparator();
    }
  }

  final void addBooleanMember(String name, Boolean value) {
    if (value != null) {
      addBooleanMember(name, value.booleanValue());
    }
  }

  final void addBooleanMember(String name, boolean value) {
    appendMemberName(name);
    out.append(value ? "true" : "false");
    appendMemberSeparator();
  }

  final void addNumberMember(String name, Integer value) {
    if (value != null) {
      addNumberMember(name, value.intValue());
    }
  }

  final void addNumberMember(String name, int value) {
    appendMemberName(name);
    out.append(value);
    appendMemberSeparator();
  }

  private void appendItems(Runnable items) {
    if (items != null) {
      items.run();
    }
  }

  private StringBuilder appendString(String str) {
    return str == null
        ? out.append("null")
        : out.append(language.stringStart)
            .append(language.escapeString.apply(str))
            .append(language.stringEnd);
  }

  private void appendStringMultiline(String str) {
    if (str == null) {
      out.append("null");
      return;
    }
    out.append(language.textStart);
    for (String line : language.escapeText.apply(str)) {
      if (language.textIndent) out.append('\n').append(indent).append(format.itemIndent);
      out.append(line);
    }
    out.append(language.textEnd);
  }

  private void appendMemberName(String name) {
    if (name == null && out.length() == 0) return;
    appendMemberIndent();
    if (name != null) {
      arrayItemLast = false;
      appendString(name).append(language.objectItemStart);
    } else {
      arrayItemLast = !language.arrayItemStart.isEmpty();
      out.append(language.arrayItemStart);
    }
  }

  private void appendMemberIndent() {
    if (!arrayItemLast && format.isNewLineBeforeItem()) out.append('\n').append(indent);
  }

  private void appendMemberSeparator() {
    arrayItemLast = false;
    out.append(language.itemSeparator);
  }

  private void discardLastMemberSeparator(int length, String empty) {
    if (out.length() > length) {
      out.setLength(out.length() - language.itemSeparator.length()); // discard last ,
    } else {
      out.append(empty);
    }
  }

  private static List<String> escapeJsonText(String unescaped) {
    return List.of(escapeJsonString(unescaped));
  }

  private static String escapeJsonString(String unescaped) {
    return new String(JSON_ESCAPE_STRING.quoteAsString(unescaped));
  }

  private static String escapeYamlString(String unescaped) {
    if (unescaped.isEmpty()) return unescaped;
    if (Character.isDigit(unescaped.charAt(0))
        || "true".equals(unescaped)
        || "false".equals(unescaped)
        || unescaped.contains("#")
        || unescaped.startsWith("*")) return "\"" + unescaped + "\"";
    return unescaped;
  }

  private static List<String> escapeYamlText(String unescaped) {
    int start = 0;
    int end = unescaped.indexOf('\n');
    if (end < 0) return List.of(unescaped);
    List<String> lines = new ArrayList<>();
    while (end > 0 && end < unescaped.length()) {
      lines.add(unescaped.substring(start, end));
      start = end + 1;
      end = unescaped.indexOf('\n', start);
    }
    lines.add(unescaped.substring(start));
    return lines;
  }
}
