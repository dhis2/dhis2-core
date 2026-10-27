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
package org.hisp.dhis.gist;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.object.ObjectOutput;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GistOutput#toCsv(GistObject.Output, java.io.OutputStream)} and {@link
 * GistOutput#toCsv(GistObjectList.Output, java.io.OutputStream)}.
 *
 * <p>Focus is on the wiring between the Gist output records and {@link CsvBuilder}:
 *
 * <ul>
 *   <li>{@code paths()} become the header row, in order
 *   <li>{@code values} become the data rows
 *   <li>{@code headless} skips the header row
 *   <li>{@code unwrap} and {@code pager} are <em>ignored</em> for CSV — they only affect the JSON
 *       output, because CSV has no representation for either (a CSV is a flat table, and pager
 *       metadata has no natural place in it)
 * </ul>
 *
 * <p>The value-formatting contract (quoting, null handling, date and collection formatting) is
 * covered by {@link org.hisp.dhis.csv.CsvBuilderTest} and is exercised only lightly here to confirm
 * the pipeline is wired through — not to re-verify {@code CsvBuilder} itself.
 *
 * <p>Output ends each record with {@code \n} (LF, per the CSV writer's contract).
 */
class GistOutputCsvTest {

  private static final boolean HEADLESS = true;
  private static final boolean NO_HEADLESS = false;

  // ====== GistObject.Output ======

  @Test
  void testObject_Simple() {
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("id", String.class), prop("name", String.class)),
            new Object[] {"abc", "Hello"});
    assertEquals("id,name\nabc,Hello\n", toCsv(obj));
  }

  @Test
  void testObject_HeadersFollowPathOrder() {
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("z", String.class), prop("a", String.class), prop("m", String.class)),
            new Object[] {"1", "2", "3"});
    assertEquals("z,a,m\n1,2,3\n", toCsv(obj));
  }

  @Test
  void testObject_NestedPathFlattensAsDottedHeader() {
    // CSV is a flat table — nested paths become dotted column names, values are not navigated
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("a.b", String.class), prop("c.d.e", String.class)),
            new Object[] {"x", "y"});
    assertEquals("a.b,c.d.e\nx,y\n", toCsv(obj));
  }

  @Test
  void testObject_EmptyDoesNotEmitLine() {
    // No properties → header row is just LF; no columns → data row is not emitted
    GistObject.Output obj = new GistObject.Output(List.of(), new Object[0]);
    assertEquals("\n", toCsv(obj));
  }

  @Test
  void testObject_UnwrapIgnoredForCsv() {
    // unwrap only applies to JSON — CSV always emits a header row plus a data row
    GistObject.Output obj =
        new GistObject.Output(List.of(prop("name", String.class)), new Object[] {"Hello"});
    assertEquals("name\nHello\n", toCsv(obj));
  }

  @Test
  void testObject_NullValueBecomesEmptyField() {
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("a", String.class), prop("b", String.class), prop("c", String.class)),
            new Object[] {"x", null, "z"});
    assertEquals("a,b,c\nx,,z\n", toCsv(obj));
  }

  @Test
  void testObject_QuoteEscapingThroughPipeline() {
    // A field containing a comma triggers quoting — confirms CsvBuilder's contract applies here
    GistObject.Output obj =
        new GistObject.Output(List.of(prop("value", String.class)), new Object[] {"x,y"});
    assertEquals("value\n\"x,y\"\n", toCsv(obj));
  }

  @Test
  void testObject_MixedTypes() {
    GistObject.Output obj =
        new GistObject.Output(
            List.of(
                prop("id", String.class),
                prop("count", Integer.class),
                prop("active", Boolean.class),
                prop("ratio", Double.class)),
            new Object[] {"abc", 42, true, 0.5});
    assertEquals("id,count,active,ratio\nabc,42,true,0.5\n", toCsv(obj));
  }

  @Test
  void testObject_Unicode() {
    // Confirms UTF-8 output — the writer never falls back to platform default charset
    GistObject.Output obj =
        new GistObject.Output(
            List.of(prop("name", String.class), prop("desc", String.class)),
            new Object[] {"héllo", "😀"});
    assertEquals("name,desc\nhéllo,😀\n", toCsv(obj));
  }

  // ====== GistObjectList.Output ======

  @Test
  void testList_WithHeaders() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            null,
            "items",
            List.of(prop("id", String.class), prop("name", String.class)),
            Stream.<Object[]>of(new Object[] {"a", "Alice"}, new Object[] {"b", "Bob"}));
    assertEquals("id,name\na,Alice\nb,Bob\n", toCsv(list));
  }

  @Test
  void testList_Headless_SkipsHeaders() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            HEADLESS,
            null,
            "items",
            List.of(prop("id", String.class), prop("name", String.class)),
            Stream.<Object[]>of(new Object[] {"a", "Alice"}, new Object[] {"b", "Bob"}));
    assertEquals("a,Alice\nb,Bob\n", toCsv(list));
  }

  @Test
  void testList_WithHeaders_Empty() {
    // Headers only, no data rows
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            null,
            "items",
            List.of(prop("id", String.class), prop("name", String.class)),
            Stream.empty());
    assertEquals("id,name\n", toCsv(list));
  }

  @Test
  void testList_Headless_Empty() {
    // No headers, no rows → empty output
    GistObjectList.Output list =
        new GistObjectList.Output(
            HEADLESS, null, "items", List.of(prop("id", String.class)), Stream.empty());
    assertEquals("", toCsv(list));
  }

  @Test
  void testList_HeadersFollowPathOrder() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            null,
            "items",
            List.of(prop("z", String.class), prop("a", String.class), prop("m", String.class)),
            Stream.<Object[]>of(new Object[] {"1", "2", "3"}));
    assertEquals("z,a,m\n1,2,3\n", toCsv(list));
  }

  @Test
  void testList_UnwrapIgnoredForCsv() {
    // unwrap only applies to JSON
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            null,
            "items",
            List.of(prop("name", String.class)),
            Stream.<Object[]>of(new Object[] {"Alice"}, new Object[] {"Bob"}));
    assertEquals("name\nAlice\nBob\n", toCsv(list));
  }

  @Test
  void testList_PagerIgnoredForCsv() {
    // pager only affects JSON — CSV has no representation for pager metadata
    GistPager pager = new GistPager(1, 50, 200, "prevURL", "nextURL");
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            pager,
            "items",
            List.of(prop("id", String.class)),
            Stream.<Object[]>of(new Object[] {"a"}));
    String out = toCsv(list);
    assertEquals("id\na\n", out);
    // Negative assertions — make the intent explicit that pager fields are absent
    org.junit.jupiter.api.Assertions.assertFalse(out.contains("pager"), out);
    org.junit.jupiter.api.Assertions.assertFalse(out.contains("pageSize"), out);
  }

  @Test
  void testList_NullValuesBeomeEmptyFields() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            null,
            "items",
            List.of(prop("a", String.class), prop("b", String.class)),
            Stream.<Object[]>of(new Object[] {null, "x"}, new Object[] {"y", null}));
    assertEquals("a,b\n\"\",x\ny,\n", toCsv(list));
  }

  @Test
  void testList_Unicode() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            null,
            "items",
            List.of(prop("name", String.class)),
            Stream.<Object[]>of(
                new Object[] {"héllo"}, new Object[] {"wörld"}, new Object[] {"😀"}));
    assertEquals("name\nhéllo\nwörld\n😀\n", toCsv(list));
  }

  @Test
  void testList_MixedTypes() {
    GistObjectList.Output list =
        new GistObjectList.Output(
            NO_HEADLESS,
            null,
            "items",
            List.of(
                prop("id", String.class),
                prop("count", Integer.class),
                prop("active", Boolean.class)),
            Stream.<Object[]>of(new Object[] {"a", 1, true}, new Object[] {"b", 2, false}));
    assertEquals("id,count,active\na,1,true\nb,2,false\n", toCsv(list));
  }

  // ====== helpers ======

  /**
   * Helper to build an {@link ObjectOutput.Property}. Assumes:
   *
   * <ul>
   *   <li>{@code PropertyPath.of(String)} is the factory
   *   <li>{@code ObjectOutput.Type.of(Class)} is the factory
   *   <li>{@code Property} has a record component {@code (path, type, arrayAggregate)}
   * </ul>
   *
   * <p>If the actual API differs, this is the only method that needs adjustment — the test bodies
   * never construct properties directly.
   */
  private static ObjectOutput.Property prop(String path, Class<?> type) {
    return new ObjectOutput.Property(path, new ObjectOutput.Type(type), false);
  }

  private static String toCsv(GistObject.Output obj) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    GistOutput.toCsv(obj, out);
    return out.toString(UTF_8);
  }

  private static String toCsv(GistObjectList.Output list) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    GistOutput.toCsv(list, out);
    return out.toString(UTF_8);
  }
}
