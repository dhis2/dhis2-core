/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.webapi.openapi.OpenApiHtmlUtils.escapeHtml;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * This is a simplified markdown to HTML helper used in {@link OpenApiRenderer} to render
 * descriptions where markdown is allowed.
 *
 * <p>The syntax supported is minimal only supporting inline markup, paragraphs, hashtag headers and
 * lists and rulers.
 *
 * <p>This means no embedded HTML is supported. HTML will be escaped.
 *
 * <p>The block logic or rules are also simplified. A paragraph starts and ends if a line starts
 * with another block element or indentation level change. A heading can only be a single line.
 *
 * @author Jan Bernitt
 */
@RequiredArgsConstructor
final class OpenApiMarkdown {

  private sealed interface MarkdownBlock {}

  private record MarkupText(List<MarkdownBlock> content) implements MarkdownBlock {}

  private record MarkupListing(MarkupListType type, List<MarkupText> items)
      implements MarkdownBlock {}

  private record MarkupRuler() implements MarkdownBlock {}

  private record MarkupHeading(int level, MarkupLine text) implements MarkdownBlock {}

  private record MarkupCodeBlock(String language, String verbatim) implements MarkdownBlock {}

  private record MarkupLine(List<MarkupSpan> spans) implements MarkdownBlock {}

  private record MarkupSpacer() implements MarkdownBlock {}

  private record MarkupSpan(MarkupSpanType type, String value, List<MarkupSpan> inner) {}

  private enum MarkupListType {
    BULLET,
    NUMBERED
  }

  private enum MarkupSpanType {
    PLAIN,
    BOLD,
    ITALIC,
    CODE,
    LINK,
    IMAGE
  }

  private final StringBuilder html;
  private final Set<String> keywords;

  /**
   * Is markdown which is just plain text as it does not have any characters that would trigger
   * markdown syntax. This must also exclude any character that cannot occur in HTML unescaped.
   */
  private static final Pattern PLAIN_TEXT = Pattern.compile("^[-a-zA-Z0-9 \t.,;:?#'~+/%()^°|]*$");

  public static String markdownToHTML(String markdown) {
    return markdownToHTML(markdown, Set.of());
  }

  public static String markdownToHTML(String markdown, Set<String> keywords) {
    if (markdown == null || markdown.isBlank()) return null;
    if (PLAIN_TEXT.matcher(markdown).matches()) return markdown;
    OpenApiMarkdown renderer = new OpenApiMarkdown(new StringBuilder(markdown.length()), keywords);
    renderer.renderText(MarkdownParser.parse(markdown));
    return renderer.html.toString();
  }

  private void render(MarkdownBlock block) {
    if (block instanceof MarkupRuler r) {
      renderRuler(r);
    } else if (block instanceof MarkupSpacer s) {
      renderSpacer(s);
    } else if (block instanceof MarkupHeading h) {
      renderHeading(h);
    } else if (block instanceof MarkupLine l) {
      renderLine(l);
    } else if (block instanceof MarkupText t) {
      renderText(t);
    } else if (block instanceof MarkupListing l) {
      renderListing(l);
    } else if (block instanceof MarkupCodeBlock b) {
      renderCodeBlock(b);
    } else throw new UnsupportedOperationException("Can't happen");
  }

  private void renderCodeBlock(MarkupCodeBlock block) {
    html.append("<pre>\n").append(escapeHtml(block.verbatim)).append("\n</pre>\n");
  }

  private void renderSpacer(MarkupSpacer spacer) {
    html.append("\n\n");
  }

  private void renderListing(MarkupListing listing) {
    String tag = listing.type == MarkupListType.BULLET ? "ul" : "ol";
    html.append("<").append(tag).append(">\n");
    listing.items.forEach(
        li -> {
          html.append("<li>");
          renderText(li);
          html.append("</li>");
        });
    html.append("</").append(tag).append(">\n");
  }

  private void renderText(MarkupText text) {
    List<MarkupLine> lines = new ArrayList<>();
    Runnable addPar =
        () -> {
          if (!lines.isEmpty()) {
            html.append("<p>");
            lines.forEach(this::renderLine);
            html.append("</p>");
            lines.clear();
          }
        };
    for (MarkdownBlock e : text.content) {
      if (e instanceof MarkupLine l) {
        lines.add(l);
      } else {
        addPar.run();
        render(e);
      }
    }
    addPar.run();
  }

  private void renderRuler(MarkupRuler ruler) {
    html.append("<hr/>\n");
  }

  private void renderHeading(MarkupHeading heading) {
    html.append("<h").append(heading.level).append(">");
    renderLine(heading.text);
    html.append("</h").append(heading.level).append(">\n");
  }

  private void renderLine(MarkupLine line) {
    line.spans.forEach(this::renderSpan);
  }

  private void renderSpan(MarkupSpan span) {
    switch (span.type) {
      case ITALIC -> {
        html.append("<em>");
        span.inner.forEach(this::renderSpan);
        html.append("</em>");
      }
      case BOLD -> {
        html.append("<strong>");
        span.inner.forEach(this::renderSpan);
        html.append("</strong>");
      }
      case IMAGE ->
          html.append("<img src=\"")
              .append(escapeHtml(span.value))
              .append("\" title=\"")
              .append(escapeHtml(span.inner.get(0).value))
              .append(" />");
      case LINK -> {
        html.append("<a target=\"_blank\" href=\"").append(escapeHtml(span.value)).append("\">");
        span.inner.forEach(this::renderSpan);
        html.append("</a>");
      }
      case PLAIN -> html.append(escapeHtml(span.value));
      case CODE -> {
        html.append("<code class=\"md");
        if (keywords.contains(span.value)) html.append(" url");
        html.append("\">").append(escapeHtml(span.value)).append("</code>");
      }
    }
  }

  @RequiredArgsConstructor
  private static final class MarkdownParser {

    final String[] lines;
    int lineNo;

    private static MarkupText parse(String markdown) {
      return new MarkdownParser(markdown.split("\\R")).parse();
    }

    private MarkupText parse() {
      List<MarkdownBlock> content = new ArrayList<>();
      while (lineNo < lines.length) {
        content.add(parseAutoDetect());
      }
      return new MarkupText(List.copyOf(content));
    }

    private static final Pattern REGEX_SPACER = Pattern.compile("^\\s*$");
    private static final Pattern REGEX_RULER = Pattern.compile("^\\s*[-_*]{3,}\\s*$");
    private static final Pattern REGEX_HEADING = Pattern.compile("^\\s*(#{1,6}) ");
    private static final Pattern REGEX_LISTING = Pattern.compile("^\\s*(?:-|\\*|\\d+\\.) ");
    private static final Pattern REGEX_CODE_BLOCK =
        Pattern.compile("^\\s*```(?:[-_a-zA-Z0-9]+)?\\s*$");

    private MarkdownBlock parseAutoDetect() {
      String line = lines[lineNo];
      if (REGEX_SPACER.matcher(line).matches()) return parseSpacer();
      if (REGEX_RULER.matcher(line).matches()) return parseRuler();
      if (REGEX_HEADING.matcher(line).matches()) return parseHeading();
      if (REGEX_LISTING.matcher(line).matches()) return parseListing();
      if (REGEX_CODE_BLOCK.matcher(line).matches()) return parseCodeBlock();
      return parseLine();
    }

    private MarkupSpacer parseSpacer() {
      lineNo++; // no need to parse any more details
      return new MarkupSpacer();
    }

    private MarkupCodeBlock parseCodeBlock() {
      StringBuilder verbatim = new StringBuilder();
      String language = lines[lineNo++].trim().substring(3);
      String line = lines[lineNo++];
      while (!REGEX_CODE_BLOCK.matcher(line).matches()) {
        verbatim.append(line).append('\n');
        line = lines[lineNo++];
      }
      return new MarkupCodeBlock(language, verbatim.toString());
    }

    private MarkupRuler parseRuler() {
      lineNo++; // no need to parse any more details
      return new MarkupRuler();
    }

    private MarkupListing parseListing() {

      return null;
    }

    private MarkupHeading parseHeading() {
      String line = lines[lineNo++];
      String type = REGEX_HEADING.matcher(line).group(1);
      int level = type.length();
      String inline = line.substring(line.indexOf(type) + level);
      return new MarkupHeading(level, parseLine(inline));
    }

    private MarkupLine parseLine() {
      return parseLine(lines[lineNo++]);
    }

    private MarkupLine parseLine(String line) {
      return new MarkupLine(parseSpans(line, 0, line.length()));
    }

    private List<MarkupSpan> parseSpans(String line, int from, int to) {
      if (line.isEmpty() || from >= line.length() || from >= to) return List.of();
      int i = from;
      List<MarkupSpan> res = new ArrayList<>();
      int from0 = i;
      StringBuilder plain = new StringBuilder();
      Runnable addPlain =
          () -> {
            if (!plain.isEmpty()) {
              res.add(new MarkupSpan(MarkupSpanType.PLAIN, plain.toString(), List.of()));
              plain.setLength(0);
            }
          };
      while (i < to) {
        from0 = i;
        while (i < to && !isSpanOpen(line.charAt(i))) i++;
        if (from0 < i) plain.append(line, from0, i);
        if (i >= to) {
          addPlain.run();
          return List.copyOf(res);
        }
        int openAt = i;
        char open = line.charAt(openAt);
        if (open == '<' && line.startsWith("<http", openAt)) {
          int closeAt = line.indexOf('>', openAt + 1);
          if (closeAt > 0 && closeAt < to) {
            addPlain.run();
            String url = line.substring(openAt + 1, closeAt);
            res.add(
                new MarkupSpan(
                    MarkupSpanType.LINK,
                    url,
                    List.of(new MarkupSpan(MarkupSpanType.PLAIN, url, List.of()))));
            i = closeAt + 1;
          }
        } else if (open == '`') {
          int closeAt = line.indexOf('`', openAt + 1);
          if (closeAt > 0 && closeAt < to) {
            addPlain.run();
            res.add(
                new MarkupSpan(
                    MarkupSpanType.CODE, line.substring(openAt + 1, closeAt), List.of()));
            i = closeAt + 1;
          }
        } else if (open == '[' || line.startsWith("![", openAt)) {
          MarkupSpanType type = open == '!' ? MarkupSpanType.IMAGE : MarkupSpanType.LINK;
          int endOfText = line.indexOf("](", openAt + 1);
          if (endOfText > 0 && endOfText < to) {
            int endOfUrl = line.indexOf(")", endOfText);
            if (endOfUrl > 0 && endOfUrl < to) {
              String url = line.substring(endOfText + 2, endOfUrl);
              int startOfText = line.indexOf('[', openAt) + 1;
              List<MarkupSpan> text =
                  type == MarkupSpanType.IMAGE
                      ? List.of(
                          new MarkupSpan(
                              MarkupSpanType.PLAIN,
                              line.substring(startOfText, endOfText),
                              List.of()))
                      : parseSpans(line, startOfText, endOfText);
              addPlain.run();
              res.add(new MarkupSpan(type, url, text));
              i = endOfUrl + 1;
            }
          }
        } else if (isSpanEmphasis(open)) {
          String marker3 = new String(new char[] {open, open, open});
          String marker2 = new String(new char[] {open, open});
          if (line.startsWith(marker3, openAt)) { // triple => italic + bold
            int closeAt = line.indexOf(marker3, openAt + 3);
            if (closeAt > 0 && closeAt < to) {
              MarkupSpan bold =
                  new MarkupSpan(MarkupSpanType.BOLD, "", parseSpans(line, openAt + 3, closeAt));
              addPlain.run();
              res.add(new MarkupSpan(MarkupSpanType.ITALIC, "", List.of(bold)));
              i = closeAt + 3;
            }
          } else if (line.startsWith(marker2, openAt)) { // double => bold
            int closeAt = line.indexOf(marker2, openAt + 2);
            if (closeAt > 0 && closeAt < to) {
              addPlain.run();
              res.add(
                  new MarkupSpan(MarkupSpanType.BOLD, "", parseSpans(line, openAt + 2, closeAt)));
              i = closeAt + 2;
            }
          } else { // single => italic
            int closeAt = line.indexOf(open, openAt + 1);
            if (closeAt > 0 && closeAt < to) {
              addPlain.run();
              res.add(
                  new MarkupSpan(MarkupSpanType.ITALIC, "", parseSpans(line, openAt + 1, closeAt)));
              i = closeAt + 1;
            }
          }
        }
        if (i == openAt) { // no working opener found
          plain.append(open);
          i++; // try next character
        }
      }
      addPlain.run();
      return List.copyOf(res);
    }

    private static boolean isSpanOpen(char c) {
      return isSpanEmphasis(c) || c == '`' || c == '[' || c == '<' || c == '!';
    }

    private static boolean isIndent(char c) {
      return c == ' ' || c == '\t';
    }

    private static boolean isSpanEmphasis(char c) {
      return c == '_' || c == '*';
    }
  }
}
