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
package org.hisp.dhis.datastore;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.hisp.dhis.datastore.DatastoreQuery.normalisePath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.datastore.DatastoreQuery.Field;
import org.junit.jupiter.api.Test;

/**
 * Tests the parsing and manipulation of {@link DatastoreQuery}s.
 *
 * @author Jan Bernitt
 */
class DatastoreQueryTest {

  @Test
  void testParseFields_FlatSingle() {
    assertFields("name", "name");
  }

  @Test
  void testParseFields_FlatMultiple() {
    assertFields("name,code", "name", "code");
    assertFields("name,code,desc", "name", "code", "desc");
  }

  @Test
  void testParseFields_NestedSingle() {
    assertFields("elements[name]", "elements.name");
  }

  @Test
  void testParseFields_NestedMultiple() {
    assertFields("elements[name,code]", "elements.name", "elements.code");
  }

  @Test
  void testParseFields_NestedMultipleList() {
    assertFields(
        "elements[name,code],desc,parent[name]",
        "elements.name",
        "elements.code",
        "desc",
        "parent.name");
  }

  @Test
  void testParseFields_DeepSingle() {
    assertFields("elements[parents[name]]", "elements.parents.name");
  }

  @Test
  void testParseFields_DeepMultiple() {
    assertFields(
        "elements[parents[name,code],desc]",
        "elements.parents.name",
        "elements.parents.code",
        "elements.desc");
  }

  @Test
  void testParseFields_DeepMultipleList() {
    assertFields("a[x,y[Q]],b,c[z[1,2.t]],d.w", "a.x", "a.y.Q", "b", "c.z.1", "c.z.2.t", "d.w");
  }

  @Test
  void testNormalisePath_DotIsValidRootValuePath() {
    assertEquals(".", normalisePath("."));
  }

  @Test
  void testNormalisePath_UnderscoreIsValidKeyPath() {
    assertEquals("_", normalisePath("_"));
  }

  @Test
  void testNormalisePath_simplePropertyIsValid() {
    assertEquals("name", normalisePath("name"));
  }

  @Test
  void testNormalisePath_deepPropertyIsValid() {
    assertEquals("parent.child.name", normalisePath("parent.child.name"));
  }

  @Test
  void testNormalisePath_arraySyntaxIsNormalisedToDotSyntax() {
    assertEquals("parent.0.name", normalisePath("parent[0].name"));
  }

  @Test
  void testNormalisePath_nullIsInvalid() {
    assertThrows(IllegalQueryException.class, () -> normalisePath(null));
  }

  @Test
  void testNormalisePath_mostSymbolsAreInvalid() {
    assertEquals("name-with-x", normalisePath("name-with-x"), "to show this would be valid");
    for (char symbol : "'\"+~*/&$!(){}[]?;:#".toCharArray()) {
      assertThrows(IllegalQueryException.class, () -> normalisePath("name-with-" + symbol));
    }
  }

  @Test
  void testNormalisePath_longPathsAreInvalid() {
    String tooLongIndividualProperty = "no".repeat(100);
    assertThrows(IllegalQueryException.class, () -> normalisePath(tooLongIndividualProperty));

    String tooManyNestingLevelsProperty = ".name".repeat(10).substring(1);
    assertThrows(IllegalQueryException.class, () -> normalisePath(tooManyNestingLevelsProperty));
  }

  private static void assertFields(String expression, String... expectedPaths) {
    List<Field> fields = DatastoreQuery.parseFields(expression);
    assertEquals(
        List.of(expectedPaths), fields.stream().map(Field::getPath).collect(toUnmodifiableList()));
  }
}
