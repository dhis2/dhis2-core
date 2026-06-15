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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.schema.annotation.Gist;
import org.junit.jupiter.api.Test;

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
            new Fields.Field("*"),
            new Fields.Field("foo"),
            new Fields.Field("-bar"),
            new Fields.Field("!baz")),
        Fields.of(":all,*,foo,-bar,!baz"));
  }

  private void assertFieldsEquals(List<Fields.Field> expected, Fields actual) {
    assertEquals(expected, actual.fields());
  }
}
