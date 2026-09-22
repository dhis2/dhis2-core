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
package org.hisp.dhis.common.input;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.schema.annotation.Gist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FieldsTest {
  @Test
  void testFieldsOf_Simple() {
    assertFieldsEquals(
        List.of(new Fields.Field("foo"), new Fields.Field("bar")), Fields.of("foo,bar"));
  }

  @Test
  void testFieldsOf_Nest1() {
    assertFieldsEquals(List.of(new Fields.Field("foo.bar")), Fields.of("foo[bar]"));
  }

  @Test
  void testFieldsOf_Nest2() {
    assertFieldsEquals(
        List.of(new Fields.Field("foo.bar"), new Fields.Field("foo.baz.que")),
        Fields.of("foo[bar,baz[que]]"));
  }

  @Test
  void testFieldsOf_Nest3() {
    assertFieldsEquals(
        List.of(
            new Fields.Field("foo.bar.hey"),
            new Fields.Field("foo.bar.ho"),
            new Fields.Field("foo.baz.que")),
        Fields.of("foo[bar[hey,ho],baz[que]]"));
  }

  @Test
  void testFieldsOf_Nest3Rename1() {
    assertFieldsEquals(
        List.of(
            new Fields.Field("foo.bar.hey").withRenamedPath("x.bar.hey"),
            new Fields.Field("foo.bar.ho").withRenamedPath("x.bar.ho"),
            new Fields.Field("foo.baz.que").withRenamedPath("x.baz.que")),
        Fields.of("foo~rename(x)[bar[hey,ho],baz[que]]"));
  }

  @Test
  void testFieldsOf_Nest3Rename2() {
    assertFieldsEquals(
        List.of(
            new Fields.Field("foo.bar.hey").withRenamedPath("x.bar.y"),
            new Fields.Field("foo.bar.ho").withRenamedPath("x.bar.ho"),
            new Fields.Field("foo.baz.que").withRenamedPath("x.baz.que")),
        Fields.of("foo~rename(x)[bar[hey~rename(y),ho],baz[que]]"));
  }

  @Test
  void testFieldsOf_Pluck() {
    assertFieldsEquals(
        List.of(
            new Fields.Field("id"),
            new Fields.Field("userGroups", Gist.Transform.NOT_MEMBER, List.of("u1234567890")),
            new Fields.Field("userGroups", Gist.Transform.PLUCK, List.of("name", "foo"))),
        Fields.of("id,userGroups::not-member(u1234567890)::pluck(name,foo)"));
  }

  @Test
  void testFieldsOf_MultiTransform() {
    assertFieldsEquals(
        List.of(
            new Fields.Field("id"),
            new Fields.Field("name"),
            new Fields.Field("u1234567890")
                .withRenamedPath("geo")
                .withTransformation(Gist.Transform.PLUCK)),
        Fields.of("id,name,u1234567890::rename(geo)::pluck"));
  }

  @Test
  void testFieldsOf_Transform() {
    assertFieldsEquals(
        List.of(
            new Fields.Field("name"),
            new Fields.Field("users")
                .withTransformation(Gist.Transform.NOT_MEMBER, List.of("u1234567890"))),
        Fields.of("name,users::not-member(u1234567890)"));
  }

  @Test
  void testFieldsOf_Preset() {
    assertFieldsEquals(List.of(new Fields.Field(":unknown")), Fields.of(":unknown"));
    assertFieldsEquals(
        List.of(
            new Fields.Field(":all"),
            new Fields.Field(":all"),
            new Fields.Field("foo"),
            new Fields.Field("-bar"),
            new Fields.Field("!baz")),
        Fields.of(":all,*,foo,-bar,!baz"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "*",
        ":all",
        "!*",
        "!:all", // should this be invalid? the exclusion is ignored and the preset is applied
        ":simple",
        "!event",
        "event,dataValues",
        "event,!dataValues",
        "!dataValues",
        "!dataValues[]", // behaves like !dataValues
        // "!dataValues[value]", // bug in current field filter NPE Cannot invoke
        // "org.hisp.dhis.fieldfiltering.FieldPath.getPath()" because "fieldPath" is null
        // behaves like !dataValues in tracker filter
        "dataValues,!dataValues",
        "!dataValues,dataValues",
        // this is opening a block without a field is a 400 in tracker filter but ignored in the
        // current one
        // http://localhost:8080/api/organisationUnits?pageSize=1&fields=[id]
        // "[value]",
        "event,!dataValues,*",
        "dataValues[!value]",
        "dataValues, ,dataValues[!value], ",
        "dataValues,dataValues[!value,value)",
        "dataValues,dataValues[value]",
        "*,dataValues[value]",
        "dataValues[value]",
        "dataValues[value",
        "dataValues[dataElement,!value]",
        "dataValues::rename(values)[dataElement,!value]",
        "event,*,dataValues[!value]",
        "event,dataValues[dataElement,value]",
        "event,dataValues[*,!storedBy]",
        "event,dataValues[:all,!storedBy]",
        "event,dataValues[!*,!storedBy]",
        "event,dataValues[:simple,!storedBy]",
        "event,dataValues[:simple,!storedBy,:all]",
        "event,dataValues[!:all,!storedBy]",
        "*,!enrollment",
        "relationships,relationships[from]",
        "relationships[!from]",
        "relationships[  ]",
        "relationships[relationship,unknownfield]",
        // The current filter returns all if that field while the tracker filter returns {} as the
        // field
        // does not exist. The current behavior makes no sense as this suggests to a user that their
        // fields
        // input was correct.
        // "relationships[unknownfield]",
        "relationships[!unknownfield]",
        "relationships[from[trackedEntity[ :simple ]",
        // transformations
        "dataValues~isEmpty",
        "dataValues|isEmpty",
        "dataValues::isEmpty",
        "notes::isEmpty",
        "event::isEmpty",
        "dataValues::isNotEmpty",
        "notes::isNotEmpty",
        "event::isNotEmpty",
        "dataValues|rename(hasDataValues)~isEmpty", // rename must be applied last
        "notes[:all,value~rename(text)]",
        "notes::size",
        "event::size",
        "relationships[bidirectional::size]",
        "dataValues::pluck",
        "dataValues::pluck(value)",
        "event::pluck",
        // tracker does not have the field id which the key defaults to so this leads to {}
        "dataValues~keyBy[dataElement,value]",
        "dataValues~keyBy(dataElement)[dataElement,value]",
        "dataValues~keyBy(dataElement)[!value,:all]",
        // filtering is done before the transformation so this leads to {} as the key is filtered
        // out
        "dataValues~keyBy(dataElement)[!dataElement]",
        "event::keyBy",
        " id, ,, group[ , , id ,  ], code  ,"
      })
  void testFieldsOf_ParsesSuccessful(String fields) {
    assertDoesNotThrow(() -> Fields.of(fields));
  }

  private void assertFieldsEquals(List<Fields.Field> expected, Fields actual) {
    assertEquals(expected, actual.fields());
  }
}
