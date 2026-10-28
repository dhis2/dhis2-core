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

import static org.hisp.dhis.webapi.openapi.JsonGenerator.Format.PRETTY_PRINT;
import static org.hisp.dhis.webapi.openapi.JsonGenerator.Format.SMALL;
import static org.hisp.dhis.webapi.openapi.JsonGenerator.Language.JSON;
import static org.hisp.dhis.webapi.openapi.JsonGenerator.Language.YAML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Consumer;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.jsontree.TextBuilder;
import org.junit.jupiter.api.Test;

/**
 * Tests for the nesting and separator logic in {@link JsonGenerator}, focused on the behaviours
 * that the recent {@link org.hisp.dhis.jsontree.Appender} refactor touched:
 *
 * <ul>
 *   <li>{@code firstItemAtLevel} save/restore across nested objects and arrays — ensures the
 *       separator is written before every item except the first at each level
 *   <li>Empty-container detection — no separator and no trailing whitespace when a container has no
 *       children, with the language-specific empty marker ({@code []} or {@code {}})
 *   <li>{@code nothingWritten} tracking — the root-level member-name short-circuit skips the
 *       separator at the very start of the document
 *   <li>{@code atLineStart} tracking — pretty-print avoids adding a second newline when the
 *       previous write already ended with one
 * </ul>
 *
 * <p>Compact JSON ({@link JsonGenerator.Format#SMALL}) is asserted verbatim because it has no
 * whitespace formatting to obscure the separator logic. Pretty-print and YAML output use structural
 * assertions ({@code contains} / {@code assertFalse}) so the tests do not over-specify indentation,
 * which is not what they are covering.
 */
class JsonGeneratorTest {

  /*
   * Empty containers
   */

  @Test
  void testEmptyRootObject_JSON_Small() {
    assertEquals("{}", generate(SMALL, JSON, g -> {}));
  }

  @Test
  void testEmptyNestedObject_JSON_Small() {
    assertEquals("{\"a\":{}}", generate(SMALL, JSON, g -> g.addObjectMember("a", () -> {})));
  }

  @Test
  void testEmptyNestedArray_JSON_Small() {
    assertEquals("{\"a\":[]}", generate(SMALL, JSON, g -> g.addArrayMember("a", () -> {})));
  }

  /*
   * Separator placement — single level
   */

  @Test
  void testOneMember_JSON_Small() {
    assertEquals("{\"a\":1}", generate(SMALL, JSON, g -> g.addNumberMember("a", 1)));
  }

  @Test
  void testTwoMembers_JSON_Small() {
    assertEquals(
        "{\"a\":1,\"b\":2}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addNumberMember("a", 1);
              g.addNumberMember("b", 2);
            }));
  }

  @Test
  void testThreeMembers_JSON_Small() {
    assertEquals(
        "{\"a\":1,\"b\":2,\"c\":3}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addNumberMember("a", 1);
              g.addNumberMember("b", 2);
              g.addNumberMember("c", 3);
            }));
  }

  /*
   * Nesting — firstItemAtLevel save/restore and innerEmpty detection
   */

  @Test
  void testNestedObjectThenSibling_JSON_Small() {
    // The separator before "b" must appear even though "a" went through a begin/end of its own
    // inner state. This is the primary firstItemAtLevel save/restore check.
    assertEquals(
        "{\"a\":{\"x\":1},\"b\":2}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addObjectMember("a", () -> g.addNumberMember("x", 1));
              g.addNumberMember("b", 2);
            }));
  }

  @Test
  void testEmptyNestedObjectThenSibling_JSON_Small() {
    // Verifies innerEmpty correctly restores firstItemAtLevel to false when the nested object
    // had no children
    assertEquals(
        "{\"a\":{},\"b\":2}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addObjectMember("a", () -> {});
              g.addNumberMember("b", 2);
            }));
  }

  @Test
  void testNestedArrayThenSibling_JSON_Small() {
    assertEquals(
        "{\"a\":[1,2],\"b\":3}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addArrayMember(
                  "a",
                  () -> {
                    g.addNumberMember(null, 1);
                    g.addNumberMember(null, 2);
                  });
              g.addNumberMember("b", 3);
            }));
  }

  @Test
  void testEmptyNestedArrayThenSibling_JSON_Small() {
    assertEquals(
        "{\"a\":[],\"b\":2}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addArrayMember("a", () -> {});
              g.addNumberMember("b", 2);
            }));
  }

  @Test
  void testArrayOfObjects_JSON_Small() {
    assertEquals(
        "{\"a\":[{\"x\":1},{\"y\":2}]}",
        generate(
            SMALL,
            JSON,
            g ->
                g.addArrayMember(
                    "a",
                    () -> {
                      g.addObjectMember(null, () -> g.addNumberMember("x", 1));
                      g.addObjectMember(null, () -> g.addNumberMember("y", 2));
                    })));
  }

  @Test
  void testNestedObjectsInSiblings_JSON_Small() {
    assertEquals(
        "{\"a\":{\"x\":1},\"b\":{\"y\":2}}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addObjectMember("a", () -> g.addNumberMember("x", 1));
              g.addObjectMember("b", () -> g.addNumberMember("y", 2));
            }));
  }

  @Test
  void testDeeplyNested_JSON_Small() {
    assertEquals(
        "{\"a\":{\"b\":{\"c\":{\"d\":1}}}}",
        generate(
            SMALL,
            JSON,
            g ->
                g.addObjectMember(
                    "a",
                    () ->
                        g.addObjectMember(
                            "b", () -> g.addObjectMember("c", () -> g.addNumberMember("d", 1))))));
  }

  /*
   * Member types — verify each add method writes through the Appender correctly
   */

  @Test
  void testStringMember_JSON_Small() {
    assertEquals("{\"a\":\"hello\"}", generate(SMALL, JSON, g -> g.addStringMember("a", "hello")));
  }

  @Test
  void testStringMemberWithEscapes_JSON_Small() {
    assertEquals("{\"a\":\"a\\\"b\"}", generate(SMALL, JSON, g -> g.addStringMember("a", "a\"b")));
  }

  @Test
  void testBooleanMembers_JSON_Small() {
    assertEquals(
        "{\"a\":true,\"b\":false}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addBooleanMember("a", true);
              g.addBooleanMember("b", false);
            }));
  }

  @Test
  void testNullStringMember_Skipped_JSON_Small() {
    assertEquals(
        "{\"a\":1}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addStringMember("skipped", null);
              g.addNumberMember("a", 1);
            }));
  }

  @Test
  void testTrueMember_OnlyWhenTrue_JSON_Small() {
    assertEquals("{}", generate(SMALL, JSON, g -> g.addTrueMember("a", false)));
    assertEquals("{}", generate(SMALL, JSON, g -> g.addTrueMember("a", null)));
    assertEquals("{\"a\":true}", generate(SMALL, JSON, g -> g.addTrueMember("a", true)));
  }

  @Test
  void testRawMember_JSON_Small() {
    JsonValue value = JsonMixed.of("42");
    assertEquals("{\"a\":42}", generate(SMALL, JSON, g -> g.addRawMember("a", value)));
  }

  @Test
  void testRawMember_EmptyStringSkipped_JSON_Small() {
    JsonValue emptyString = JsonMixed.of("\"\"");
    assertEquals("{}", generate(SMALL, JSON, g -> g.addRawMember("a", emptyString)));
  }

  @Test
  void testRawMember_UndefinedSkipped_JSON_Small() {
    JsonValue undefined = JsonMixed.of("{}").get("missing");
    assertEquals("{}", generate(SMALL, JSON, g -> g.addRawMember("a", undefined)));
  }

  /*
   * Inline arrays
   */

  @Test
  void testInlineArray_JSON_Small() {
    assertEquals(
        "{\"a\":[\"x\",\"y\",\"z\"]}",
        generate(SMALL, JSON, g -> g.addInlineArrayMember("a", List.of("x", "y", "z"))));
  }

  @Test
  void testInlineArray_EmptyNotWritten_JSON_Small() {
    assertEquals("{}", generate(SMALL, JSON, g -> g.addInlineArrayMember("a", List.of())));
  }

  /*
   * Pretty print — structural checks
   */

  @Test
  void testPretty_HasNewlinesBetweenItems_JSON() {
    String json =
        generate(
            PRETTY_PRINT,
            JSON,
            g -> {
              g.addNumberMember("a", 1);
              g.addNumberMember("b", 2);
            });
    assertTrue(json.contains("\n"), json);
    assertTrue(json.contains("\"a\":1"), json);
    assertTrue(json.contains("\"b\":2"), json);
  }

  @Test
  void testPretty_NoDoubleNewlines_JSON() {
    // Regression for the atLineStart tracking — without it, appendNewline() would emit a
    // second newline after any write that already ended with one
    String json =
        generate(
            PRETTY_PRINT,
            JSON,
            g -> {
              g.addNumberMember("a", 1);
              g.addNumberMember("b", 2);
            });
    assertFalse(json.contains("\n\n"), json);
  }

  /*
   * YAML — language-specific empty markers, [] and {}
   */

  @Test
  void testEmptyArray_YAML() {
    String yaml = generate(PRETTY_PRINT, YAML, g -> g.addArrayMember("a", () -> {}));
    assertTrue(yaml.contains("[]"), yaml);
  }

  @Test
  void testEmptyObject_YAML() {
    String yaml = generate(PRETTY_PRINT, YAML, g -> g.addObjectMember("a", () -> {}));
    assertTrue(yaml.contains("{}"), yaml);
  }

  @Test
  void testNestedContainerWithMultipleChildren_JSON_Small() {
    // A nested container with more than one child — one of which is itself a container.
    // This is the case where firstItemAtLevel save/restore at nested levels is actually
    // exercised: the "," before "c" only fires if the restore after "b" leaves
    // firstItemAtLevel correctly at false.
    assertEquals(
        "{\"a\":{\"b\":{\"x\":1},\"c\":2}}",
        generate(
            SMALL,
            JSON,
            g ->
                g.addObjectMember(
                    "a",
                    () -> {
                      g.addObjectMember("b", () -> g.addNumberMember("x", 1));
                      g.addNumberMember("c", 2);
                    })));
  }

  @Test
  void testNestedContainerWithSiblingsOnBothSides_JSON_Small() {
    // Sibling before AND after a nested container within a nested level
    assertEquals(
        "{\"a\":{\"before\":0,\"nested\":{\"x\":1},\"after\":2}}",
        generate(
            SMALL,
            JSON,
            g ->
                g.addObjectMember(
                    "a",
                    () -> {
                      g.addNumberMember("before", 0);
                      g.addObjectMember("nested", () -> g.addNumberMember("x", 1));
                      g.addNumberMember("after", 2);
                    })));
  }

  @Test
  void testMultipleEmptyContainersInSequence_JSON_Small() {
    assertEquals(
        "{\"a\":{},\"b\":{},\"c\":{}}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addObjectMember("a", () -> {});
              g.addObjectMember("b", () -> {});
              g.addObjectMember("c", () -> {});
            }));
  }

  @Test
  void testMultipleEmptyArraysInSequence_JSON_Small() {
    assertEquals(
        "{\"a\":[],\"b\":[],\"c\":[]}",
        generate(
            SMALL,
            JSON,
            g -> {
              g.addArrayMember("a", () -> {});
              g.addArrayMember("b", () -> {});
              g.addArrayMember("c", () -> {});
            }));
  }

  @Test
  void testArrayWithMixedEmptyAndNonEmpty_JSON_Small() {
    // Empty and non-empty arrays as siblings in an array
    assertEquals(
        "{[[],[1],[]]}",
        generate(
            SMALL,
            JSON,
            g ->
                g.addArrayMember(
                    null,
                    () -> {
                      g.addArrayMember(null, () -> {});
                      g.addArrayMember(null, () -> g.addNumberMember(null, 1));
                      g.addArrayMember(null, () -> {});
                    })));
  }

  @Test
  void testArrayOfObjectsWithMultipleMembersEach_JSON_Small() {
    // Each object has 2 children — stresses the firstItemAtLevel restore inside the array loop
    assertEquals(
        "{[{\"x\":1,\"y\":2},{\"x\":3,\"y\":4}]}",
        generate(
            SMALL,
            JSON,
            g ->
                g.addArrayMember(
                    null,
                    () -> {
                      g.addObjectMember(
                          null,
                          () -> {
                            g.addNumberMember("x", 1);
                            g.addNumberMember("y", 2);
                          });
                      g.addObjectMember(
                          null,
                          () -> {
                            g.addNumberMember("x", 3);
                            g.addNumberMember("y", 4);
                          });
                    })));
  }

  @Test
  void testString_Newline_JSON_Small() {
    // JSON escapes \n inline; the string must not span two lines
    assertEquals(
        "{\"a\":\"line1\\nline2\"}",
        generate(SMALL, JSON, g -> g.addStringMember("a", "line1\nline2")));
  }

  @Test
  void testString_Tab_JSON_Small() {
    assertEquals("{\"a\":\"a\\tb\"}", generate(SMALL, JSON, g -> g.addStringMember("a", "a\tb")));
  }

  @Test
  void testString_Backslash_JSON_Small() {
    assertEquals("{\"a\":\"a\\\\b\"}", generate(SMALL, JSON, g -> g.addStringMember("a", "a\\b")));
  }

  @Test
  void testString_U2028_U2029_Escaped_JSON_Small() {
    // The whole reason for switching from Jackson to appendJsonEscaped:
    // U+2028 and U+2029 are escaped for JavaScript embedding safety. Jackson
    // does NOT escape these by default, so this test is the direct regression
    // guard for the behaviour change.
    assertEquals(
        "{\"a\":\"x\\u2028y\\u2029z\"}",
        generate(SMALL, JSON, g -> g.addStringMember("a", "x\u2028y\u2029z")));
  }

  @Test
  void testString_ControlChar_Escaped_JSON_Small() {
    // U+0001 must be \u0001-escaped
    assertEquals(
        "{\"a\":\"x\\u0001y\"}", generate(SMALL, JSON, g -> g.addStringMember("a", "x\u0001y")));
  }

  @Test
  void testString_NonAsciiPassedThrough_JSON_Small() {
    // é (U+00E9) and 😀 (U+1F600) are valid in JSON strings as UTF-8 bytes
    // with no escaping. The TextBuilder sink stores them as chars; the UTF-8
    // sink would encode them as multi-byte sequences.
    assertEquals(
        "{\"a\":\"héllo 😀\"}", generate(SMALL, JSON, g -> g.addStringMember("a", "héllo 😀")));
  }

  @Test
  void testStringMultiline_EqualsStringForJson_JSON_Small() {
    // In JSON there is no multiline string form — \n is escaped inline, so
    // appendStringMultiline and appendString must produce identical output
    String viaSingle = generate(SMALL, JSON, g -> g.addStringMember("a", "line1\nline2"));
    String viaMultiline =
        generate(SMALL, JSON, g -> g.addStringMultilineMember("a", "line1\nline2"));
    assertEquals(viaSingle, viaMultiline);
  }

  @Test
  void testString_PlainNoQuotes_YAML() {
    // Plain string — no quoting required
    String yaml = generate(SMALL, YAML, g -> g.addStringMember("a", "hello"));
    assertTrue(yaml.contains("a: hello"), yaml);
    assertFalse(yaml.contains("\"hello\""), yaml);
  }

  @Test
  void testString_DigitStart_Quoted_YAML() {
    // Starts with digit → quoted (otherwise YAML parser reads it as a number)
    String yaml = generate(SMALL, YAML, g -> g.addStringMember("a", "42abc"));
    assertTrue(yaml.contains("\"42abc\""), yaml);
  }

  @Test
  void testString_TrueLiteral_Quoted_YAML() {
    // "true" / "false" → quoted (otherwise read as booleans)
    String yamlTrue = generate(SMALL, YAML, g -> g.addStringMember("a", "true"));
    String yamlFalse = generate(SMALL, YAML, g -> g.addStringMember("a", "false"));
    assertTrue(yamlTrue.contains("\"true\""), yamlTrue);
    assertTrue(yamlFalse.contains("\"false\""), yamlFalse);
  }

  @Test
  void testString_HashInside_Quoted_YAML() {
    // " #" would start a comment in YAML — the current heuristic quotes
    // the whole value when it contains "#" anywhere
    String yaml = generate(SMALL, YAML, g -> g.addStringMember("a", "value # comment"));
    assertTrue(yaml.contains("\"value # comment\""), yaml);
  }

  @Test
  void testString_StarStart_Quoted_YAML() {
    // "*" prefix is an alias marker in YAML
    String yaml = generate(SMALL, YAML, g -> g.addStringMember("a", "*alias"));
    assertTrue(yaml.contains("\"*alias\""), yaml);
  }

  @Test
  void testString_NonAsciiPassedThrough_YAML() {
    String yaml = generate(SMALL, YAML, g -> g.addStringMember("a", "héllo"));
    assertTrue(yaml.contains("héllo"), yaml);
  }

  @Test
  void testString_Multiline_BlockScalar_YAML() {
    String yaml =
        generate(PRETTY_PRINT, YAML, g -> g.addStringMultilineMember("a", "line1\nline2"));
    assertTrue(yaml.contains("|-"), yaml); // strip mode marker
    assertTrue(yaml.contains("line1"), yaml);
    assertTrue(yaml.contains("line2"), yaml);
  }

  @Test
  void testString_Multiline_IndentedUnderKey_YAML() {
    // Each content line must be indented one level deeper than the key
    String yaml =
        generate(PRETTY_PRINT, YAML, g -> g.addStringMultilineMember("a", "line1\nline2"));
    // Find the two content lines and verify both are prefixed with at least 2 spaces
    String[] lines = yaml.split("\n");
    boolean sawLine1 = false, sawLine2 = false;
    for (String line : lines) {
      if (line.contains("line1")) {
        sawLine1 = line.matches("^\\s+line1\\s*$");
      }
      if (line.contains("line2")) {
        sawLine2 = line.matches("^\\s+line2\\s*$");
      }
    }
    assertTrue(sawLine1, "line1 not indented in: " + yaml);
    assertTrue(sawLine2, "line2 not indented in: " + yaml);
  }

  @Test
  void testString_Multiline_Nested_DoubleIndent_YAML() {
    // Nested content gets two levels of indent
    String yaml =
        generate(
            PRETTY_PRINT,
            YAML,
            g ->
                g.addObjectMember(
                    "outer", () -> g.addStringMultilineMember("inner", "line1\nline2")));
    // Verify "inner" line is more indented than "outer" line, and content is more indented
    // than "inner" line. Rather than asserting exact counts (which are a format decision),
    // assert that indent strictly increases from key level to content level.
    String[] lines = yaml.split("\n");
    int outerIndent = -1, innerIndent = -1, contentIndent = -1;
    for (String line : lines) {
      if (line.contains("outer:")) outerIndent = leadingSpaces(line);
      if (line.contains("inner:")) innerIndent = leadingSpaces(line);
      if (line.contains("line1")) contentIndent = leadingSpaces(line);
    }
    assertTrue(outerIndent >= 0, yaml);
    assertTrue(innerIndent > outerIndent, yaml);
    assertTrue(contentIndent > innerIndent, yaml);
  }

  @Test
  void testString_Multiline_EmptyLineInMiddle_YAML() {
    String yaml =
        generate(PRETTY_PRINT, YAML, g -> g.addStringMultilineMember("a", "para1\n\npara2"));
    // Empty line should not be padded with indent — it should be a bare newline
    assertFalse(yaml.contains("  \n"), "empty line was padded: " + yaml);
  }

  @Test
  void testString_Multiline_TrailingNewline_StrippedByBlockScalar_YAML() {
    // "|-" strip mode discards trailing newlines from the source. The output
    // should not end with a bare blank line before the next key.
    String yaml =
        generate(PRETTY_PRINT, YAML, g -> g.addStringMultilineMember("a", "line1\nline2\n"));
    assertTrue(yaml.contains("|-"), yaml);
    // Confirm no double blank line
    assertFalse(yaml.contains("\n\n\n"), yaml);
  }

  @Test
  void testString_NoNewline_AlsoUsesBlockScalar_YAML() {
    String yaml = generate(PRETTY_PRINT, YAML, g -> g.addStringMultilineMember("a", "single line"));
    assertEquals(
        """
        a: |-
          single line
      """,
        yaml);
  }

  @Test
  void testYaml_SingleLineValue_Exact() {
    String yaml = generate(PRETTY_PRINT, YAML, g -> g.addStringMultilineMember("desc", "?"));
    assertEquals(
        """
        desc: |-
          ?
      """,
        yaml);
  }

  @Test
  void testYaml_MultilineValue_Exact() {
    String yaml =
        generate(PRETTY_PRINT, YAML, g -> g.addStringMultilineMember("desc", "line1\nline2"));
    assertEquals(
        """
        desc: |-
          line1
          line2
      """,
        yaml);
  }

  @Test
  void testYaml_WholeDocument() {
    String yaml =
        generate(
            PRETTY_PRINT,
            YAML,
            g -> {
              g.addStringMultilineMember("info", "?");
              g.addObjectMember(
                  "paths",
                  () -> {
                    g.addObjectMember(
                        "/api/foo",
                        () -> {
                          g.addStringMultilineMember("description", "Note: x");
                          g.addArrayMember(
                              "tags",
                              () -> {
                                g.addStringMember(null, "- not-a-tag");
                                g.addStringMember(null, "regular");
                              });
                        });
                  });
            });
    assertEquals(
        """
        info: |-
          ?
        paths:\s
          /api/foo:\s
            description: |-
              Note: x
            tags:\s
              - "- not-a-tag"
              - regular
      """,
        yaml);
  }

  @Test
  void testPretty_ArrayElementsIndentedOneLevelDeeper_JSON() {
    // Regression: array elements were rendered at the parent object's indent level
    // instead of one level deeper, producing:
    //   {
    //     "arr": [
    //     1,
    //     2
    //     ]
    //   }
    // instead of the conventional layout.
    String json =
        generate(
            PRETTY_PRINT,
            JSON,
            g -> {
              g.addNumberMember("a", 1);
              g.addArrayMember(
                  "arr",
                  () -> {
                    g.addNumberMember(null, 1);
                    g.addNumberMember(null, 2);
                  });
            });
    int arrIndent = -1, firstElementIndent = -1, closingBracketIndent = -1;
    for (String line : json.split("\n")) {
      String trimmed = line.stripLeading();
      if (trimmed.startsWith("\"arr\":")) arrIndent = leadingSpaces(line);
      if (trimmed.equals("1,") || trimmed.equals("1")) firstElementIndent = leadingSpaces(line);
      if (trimmed.equals("]")) closingBracketIndent = leadingSpaces(line);
    }
    assertTrue(arrIndent >= 0, json);
    assertTrue(
        firstElementIndent > arrIndent,
        "array element must be indented deeper than the key: " + json);
    assertEquals(arrIndent, closingBracketIndent, "closing ] must match the key indent: " + json);
  }

  /*
   * Helper — mirrors OpenApiGenerator.generate, including the adjustFormat step
   */

  private static int leadingSpaces(String line) {
    int i = 0;
    while (i < line.length() && line.charAt(i) == ' ') i++;
    return i;
  }

  private static String generate(
      JsonGenerator.Format format, JsonGenerator.Language language, Consumer<JsonGenerator> body) {
    TextBuilder out = new TextBuilder();
    JsonGenerator gen = new JsonGenerator(out, language.getAdjustFormat().apply(format), language);
    gen.addRootObject(() -> body.accept(gen));
    return out.toString();
  }
}
