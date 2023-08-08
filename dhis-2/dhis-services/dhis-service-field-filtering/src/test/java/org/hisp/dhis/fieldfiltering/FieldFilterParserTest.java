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
package org.hisp.dhis.fieldfiltering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen
 */
class FieldFilterParserTest {
  @Test
  void testDepth0Filters() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("id, name,    abc");

    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "name");
    assertFieldPathContains(fieldPaths, "abc");
  }

  @Test
  void testDepth1Filters() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("id,name,group[id,name]");

    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "name");
    assertFieldPathContains(fieldPaths, "group.id");
    assertFieldPathContains(fieldPaths, "group.name");
  }

  @Test
  void testDepthXFilters() {
    List<FieldPath> fieldPaths =
        FieldFilterParser.parse(
            "id,name,group[id,name],group[id,name,group[id,name,group[id,name]]]");

    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "name");
    assertFieldPathContains(fieldPaths, "group.id");
    assertFieldPathContains(fieldPaths, "group.name");
    assertFieldPathContains(fieldPaths, "group.group.id");
    assertFieldPathContains(fieldPaths, "group.group.name");
    assertFieldPathContains(fieldPaths, "group.group.group.id");
    assertFieldPathContains(fieldPaths, "group.group.group.name");
  }

  @Test
  void testOnlyBlockFilters() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("group[id,name]");

    assertFieldPathContains(fieldPaths, "group.id");
    assertFieldPathContains(fieldPaths, "group.name");
  }

  @Test
  void testOnlySpringBlockFilters() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("group[id,name]");

    assertFieldPathContains(fieldPaths, "group.id");
    assertFieldPathContains(fieldPaths, "group.name");
  }

  @Test
  void testParseWithPrefix1() {
    List<FieldPath> fieldPaths = FieldFilterParser.parseWithPrefix("a,b", "prefix");

    assertFieldPathContains(fieldPaths, "prefix.a");
    assertFieldPathContains(fieldPaths, "prefix.b");
  }

  @Test
  void testParseWithPrefix2() {
    List<FieldPath> fieldPaths = FieldFilterParser.parseWithPrefix("aaa[a],bbb[b]", "prefix");

    assertFieldPathContains(fieldPaths, "prefix.aaa.a");
    assertFieldPathContains(fieldPaths, "prefix.bbb.b");
  }

  @Test
  void testParseWithTransformer1() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::x(a;b),id~y(a;b;c),code|z(t)");

    assertFieldPathContains(fieldPaths, "name");
    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "code");
  }

  @Test
  void testParseWithTransformer2() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("groups[name::x(a;b)]");

    assertFieldPathContains(fieldPaths, "groups");
    assertFieldPathContains(fieldPaths, "groups.name");
  }

  @Test
  void testParseWithTransformer3() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("groups[name::x(a;b), code~y(a)]");

    assertFieldPathContains(fieldPaths, "groups");
    assertFieldPathContains(fieldPaths, "groups.name");
    assertFieldPathContains(fieldPaths, "groups.code");
  }

  @Test
  void testParseWithTransformer4() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::rename(n),groups[name]");

    assertFieldPathContains(fieldPaths, "name", true);
    assertFieldPathContains(fieldPaths, "groups");
    assertFieldPathContains(fieldPaths, "groups.name", false);
  }

  @Test
  void testParseWithTransformer5() {
    List<FieldPath> fieldPaths =
        FieldFilterParser.parse("name::rename(n),groups::rename(g)[name::rename(n)]");

    assertFieldPathContains(fieldPaths, "name", true);
    assertFieldPathContains(fieldPaths, "groups", true);
    assertFieldPathContains(fieldPaths, "groups.name", true);
  }

  @Test
  void testParseWithTransformer6() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::rename(n),groups::rename(g)[name]");

    assertFieldPathContains(fieldPaths, "name", true);
    assertFieldPathContains(fieldPaths, "groups", true);
    assertFieldPathContains(fieldPaths, "groups.name", false);
  }

  @Test
  void testParseWithTransformer7() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::size,group::isEmpty");

    assertFieldPathContains(fieldPaths, "name", true);
    assertFieldPathContains(fieldPaths, "group", true);
  }

  @Test
  void testParseWithTransformer8() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::rename(n)");

    assertFieldPathContains(fieldPaths, "name", true);
    FieldPathTransformer fieldPathTransformer = fieldPaths.get(0).getTransformers().get(0);
    assertEquals("rename", fieldPathTransformer.getName());
  }

  @Test
  void testParseWithMultipleTransformers() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("name::size::rename(n)");

    assertFieldPathContains(fieldPaths, "name", true);
    FieldPathTransformer fieldPathTransformer = fieldPaths.get(0).getTransformers().get(0);
    assertEquals("size", fieldPathTransformer.getName());
    fieldPathTransformer = fieldPaths.get(0).getTransformers().get(1);
    assertEquals("rename", fieldPathTransformer.getName());
  }

  @Test
  void testParseWithPresetAndExclude1() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("id,name,!code,:owner");

    FieldPath id = getFieldPath(fieldPaths, "id");
    assertNotNull(id);
    assertFalse(id.isExclude());
    assertFalse(id.isPreset());
    FieldPath name = getFieldPath(fieldPaths, "name");
    assertNotNull(name);
    assertFalse(name.isExclude());
    assertFalse(name.isPreset());
    FieldPath code = getFieldPath(fieldPaths, "code");
    assertNotNull(code);
    assertTrue(code.isExclude());
    assertFalse(code.isPreset());
    FieldPath owner = getFieldPath(fieldPaths, "owner");
    assertNotNull(owner);
    assertFalse(owner.isExclude());
    assertTrue(owner.isPreset());
  }

  @Test
  void testParseWithPresetAndExclude() {
    List<FieldPath> fieldPaths =
        FieldFilterParser.parse("id,name,!code,:owner,group[:owner,:all,!code,hello]");

    FieldPath id = getFieldPath(fieldPaths, "id");
    assertNotNull(id);
    assertFalse(id.isExclude());
    assertFalse(id.isPreset());
    FieldPath name = getFieldPath(fieldPaths, "name");
    assertNotNull(name);
    assertFalse(name.isExclude());
    assertFalse(name.isPreset());
    FieldPath code = getFieldPath(fieldPaths, "code");
    assertNotNull(code);
    assertTrue(code.isExclude());
    assertFalse(code.isPreset());
    FieldPath owner = getFieldPath(fieldPaths, "owner");
    assertNotNull(owner);
    assertFalse(owner.isExclude());
    assertTrue(owner.isPreset());
    FieldPath groupOwner = getFieldPath(fieldPaths, "group.owner");
    assertNotNull(groupOwner);
    assertFalse(groupOwner.isExclude());
    assertTrue(groupOwner.isPreset());
    FieldPath groupAll = getFieldPath(fieldPaths, "group.all");
    assertNotNull(groupAll);
    assertFalse(groupAll.isExclude());
    assertTrue(groupAll.isPreset());
    FieldPath groupCode = getFieldPath(fieldPaths, "group.code");
    assertNotNull(groupCode);
    assertTrue(groupCode.isExclude());
    assertFalse(groupCode.isPreset());
    FieldPath groupHello = getFieldPath(fieldPaths, "group.hello");
    assertNotNull(groupHello);
    assertFalse(groupHello.isExclude());
    assertFalse(groupHello.isPreset());
  }

  @Test
  void testParseWithAsterisk1() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("*,!code");

    FieldPath asterisk = getFieldPath(fieldPaths, "all");
    assertNotNull(asterisk);
    assertFalse(asterisk.isExclude());
    assertTrue(asterisk.isPreset());
    FieldPath code = getFieldPath(fieldPaths, "code");
    assertNotNull(code);
    assertTrue(code.isExclude());
    assertFalse(code.isPreset());
  }

  @Test
  void testParseWithAsterisk2() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("*,!code,group[*]");

    FieldPath asterisk = getFieldPath(fieldPaths, "all");
    assertNotNull(asterisk);
    assertFalse(asterisk.isExclude());
    assertTrue(asterisk.isPreset());
    FieldPath code = getFieldPath(fieldPaths, "code");
    assertNotNull(code);
    assertTrue(code.isExclude());
    assertFalse(code.isPreset());
    FieldPath groupAsterisk = getFieldPath(fieldPaths, "group.all");
    assertNotNull(groupAsterisk);
    assertFalse(groupAsterisk.isExclude());
    assertTrue(groupAsterisk.isPreset());
  }

  @Test
  void testMixedBlockSingleFields() {
    List<FieldPath> fieldPaths = FieldFilterParser.parse("id,name,group[id,name],code");

    assertEquals(6, fieldPaths.size());
    assertFieldPathContains(fieldPaths, "id");
    assertFieldPathContains(fieldPaths, "name");
    assertFieldPathContains(fieldPaths, "group");
    assertFieldPathContains(fieldPaths, "group.id");
    assertFieldPathContains(fieldPaths, "group.name");
    assertFieldPathContains(fieldPaths, "code");
  }

  private void assertFieldPathContains(
      List<FieldPath> fieldPaths, String expected, boolean isTransformer) {
    boolean condition = false;
    for (FieldPath fieldPath : fieldPaths) {
      String path = fieldPath.toFullPath();
      if (path.equals(expected)) {
        condition = fieldPath.isTransformer() == isTransformer;
        break;
      }
    }
    assertTrue(condition);
  }

  private void assertFieldPathContains(List<FieldPath> fieldPaths, String expected) {
    boolean condition = false;
    for (FieldPath fieldPath : fieldPaths) {
      String path = fieldPath.toFullPath();
      if (path.equals(expected)) {
        condition = true;
        break;
      }
    }
    assertTrue(condition);
  }

  private FieldPath getFieldPath(List<FieldPath> fieldPaths, String path) {
    for (FieldPath fieldPath : fieldPaths) {
      String fullPath = fieldPath.toFullPath();
      if (path.equals(fullPath)) {
        return fieldPath;
      }
    }
    return null;
  }
}
