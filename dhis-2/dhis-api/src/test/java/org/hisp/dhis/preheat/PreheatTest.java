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
package org.hisp.dhis.preheat;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class PreheatTest {

  private final Preheat preheat = new Preheat();

  private final DataElement de1 = new DataElement("dataElementA");

  private final DataElement de2 = new DataElement("dataElementB");

  private final DataElement de3 = new DataElement("dataElementC");

  @BeforeEach
  void setUp() {
    de1.setAutoFields();
    de2.setAutoFields();
    de3.setAutoFields();
    de1.setCode("Code1");
    de2.setCode("Code2");
    de3.setCode("Code3");
  }

  @Test
  void testIsEmpty_Empty() {
    assertTrue(preheat.isEmpty());
    assertTrue(preheat.isEmpty(PreheatIdentifier.UID));
    assertTrue(preheat.isEmpty(PreheatIdentifier.CODE));
    assertTrue(preheat.isEmpty(PreheatIdentifier.UID, User.class));
    assertTrue(preheat.isEmpty(PreheatIdentifier.CODE, User.class));
  }

  @Test
  void testIsEmpty_NonEmpty() {
    preheat.put(PreheatIdentifier.UID, asList(de1, de2, de3));
    assertFalse(preheat.isEmpty());
    assertFalse(preheat.isEmpty(PreheatIdentifier.UID));
    assertFalse(preheat.isEmpty(PreheatIdentifier.UID, DataElement.class));
    // but still:
    assertTrue(preheat.isEmpty(PreheatIdentifier.CODE));
    assertTrue(preheat.isEmpty(PreheatIdentifier.CODE, DataElement.class));
  }

  @Test
  void testGet_Null() {
    assertNull(preheat.get(PreheatIdentifier.UID, null));
    assertNull(preheat.get(PreheatIdentifier.CODE, null));
    assertNull(preheat.get(PreheatIdentifier.UID, DataElement.class, (String) null));
    assertNull(preheat.get(PreheatIdentifier.CODE, DataElement.class, (String) null));
    assertNull(preheat.get(PreheatIdentifier.UID, DataElement.class, (IdentifiableObject) null));
    assertNull(preheat.get(PreheatIdentifier.CODE, DataElement.class, (IdentifiableObject) null));
  }

  @Test
  void testGet_Empty() {
    assertNull(preheat.get(PreheatIdentifier.UID, User.class, "key"));
    assertNull(preheat.get(PreheatIdentifier.CODE, User.class, "key"));
    assertNull(preheat.get(PreheatIdentifier.UID, User.class, new User()));
    assertNull(preheat.get(PreheatIdentifier.CODE, User.class, new User()));
  }

  @Test
  void testGet_NonEmptyUID() {
    preheat.put(PreheatIdentifier.UID, asList(de1, de2, de3));
    assertSame(de1, preheat.get(PreheatIdentifier.UID, de1));
    assertSame(de2, preheat.get(PreheatIdentifier.UID, DataElement.class, de2));
    assertSame(de3, preheat.get(PreheatIdentifier.UID, DataElement.class, de3.getUid()));
  }

  @Test
  void testGet_NonEmptyCode() {
    preheat.put(PreheatIdentifier.CODE, asList(de1, de2, de3));
    assertSame(de1, preheat.get(PreheatIdentifier.CODE, de1));
    assertSame(de2, preheat.get(PreheatIdentifier.CODE, DataElement.class, de2));
    assertSame(de3, preheat.get(PreheatIdentifier.CODE, DataElement.class, de3.getCode()));
  }

  @Test
  void testGetAll_Null() {
    assertEquals(emptyList(), preheat.getAll(PreheatIdentifier.UID, null));
    assertEquals(emptyList(), preheat.getAll(PreheatIdentifier.CODE, null));
  }

  @Test
  void testGetAll_Empty() {
    assertEquals(emptyList(), preheat.getAll(PreheatIdentifier.UID, emptyList()));
    assertEquals(emptyList(), preheat.getAll(PreheatIdentifier.UID, singletonList(new User())));
  }

  @Test
  void testGetAll_NonEmptyUID() {
    preheat.put(PreheatIdentifier.UID, asList(de1, de2, de3));
    assertEquals(emptyList(), preheat.getAll(PreheatIdentifier.UID, emptyList()));
    assertEquals(singletonList(de1), preheat.getAll(PreheatIdentifier.UID, singletonList(de1)));
    assertEquals(asList(de1, de3), preheat.getAll(PreheatIdentifier.UID, asList(de1, de3)));
    DataElement deX = new DataElement("not-in-context");
    assertEquals(asList(de1, de3), preheat.getAll(PreheatIdentifier.UID, asList(de1, deX, de3)));
  }

  @Test
  void testGetAll_NonEmptyCode() {
    preheat.put(PreheatIdentifier.CODE, asList(de1, de2, de3));
    assertEquals(emptyList(), preheat.getAll(PreheatIdentifier.CODE, emptyList()));
    assertEquals(singletonList(de1), preheat.getAll(PreheatIdentifier.CODE, singletonList(de1)));
    assertEquals(asList(de1, de3), preheat.getAll(PreheatIdentifier.CODE, asList(de1, de3)));
    DataElement deX = new DataElement("not-in-context");
    deX.setCode("X");
    assertEquals(asList(de1, de3), preheat.getAll(PreheatIdentifier.CODE, asList(de1, deX, de3)));
  }

  @Test
  void testContainsKey_Null() {
    assertFalse(preheat.containsKey(PreheatIdentifier.UID, DataElement.class, null));
    assertFalse(preheat.containsKey(PreheatIdentifier.UID, null, "key"));
  }

  @Test
  void testContainsKey_Empty() {
    assertFalse(preheat.containsKey(PreheatIdentifier.UID, DataElement.class, "key"));
    assertFalse(preheat.containsKey(PreheatIdentifier.CODE, DataElement.class, "key"));
  }

  @Test
  void testContainsKey_NonEmptyUID() {
    preheat.put(PreheatIdentifier.UID, asList(de1, de2, de3));
    assertTrue(preheat.containsKey(PreheatIdentifier.UID, DataElement.class, de1.getUid()));
    assertFalse(preheat.containsKey(PreheatIdentifier.CODE, DataElement.class, de1.getUid()));
    assertFalse(preheat.containsKey(PreheatIdentifier.CODE, DataElement.class, de1.getCode()));
    assertFalse(preheat.containsKey(PreheatIdentifier.UID, User.class, de1.getUid()));
    assertFalse(preheat.containsKey(PreheatIdentifier.UID, DataElement.class, "not-contained"));
  }

  @Test
  void testContainsKey_NonEmptyCode() {
    preheat.put(PreheatIdentifier.CODE, asList(de1, de2, de3));
    assertTrue(preheat.containsKey(PreheatIdentifier.CODE, DataElement.class, de1.getCode()));
    assertFalse(preheat.containsKey(PreheatIdentifier.UID, DataElement.class, de1.getCode()));
    assertFalse(preheat.containsKey(PreheatIdentifier.UID, DataElement.class, de1.getUid()));
    assertFalse(preheat.containsKey(PreheatIdentifier.CODE, User.class, de1.getCode()));
    assertFalse(preheat.containsKey(PreheatIdentifier.CODE, DataElement.class, "not-contained"));
  }

  @Test
  void testPut_Null() {
    assertTrue(preheat.put(PreheatIdentifier.UID, (IdentifiableObject) null).isEmpty());
    assertTrue(preheat.put(PreheatIdentifier.CODE, (IdentifiableObject) null).isEmpty());
    assertTrue(
        preheat
            .put(PreheatIdentifier.UID, (Collection<? extends IdentifiableObject>) null)
            .isEmpty());
    assertTrue(
        preheat
            .put(PreheatIdentifier.CODE, (Collection<? extends IdentifiableObject>) null)
            .isEmpty());
  }

  @Test
  void testPut_SingleUID() {
    preheat.put(PreheatIdentifier.UID, de1);
    preheat.put(PreheatIdentifier.UID, de2);
    preheat.put(PreheatIdentifier.UID, de3);
    assertEquals(1, preheat.getKlassKeyCount(PreheatIdentifier.UID));
    assertEquals(3, preheat.getIdentifierKeyCount(PreheatIdentifier.UID, DataElement.class));
    assertSame(de1, preheat.get(PreheatIdentifier.UID, DataElement.class, de1.getUid()));
    assertSame(de2, preheat.get(PreheatIdentifier.UID, DataElement.class, de2.getUid()));
    assertSame(de3, preheat.get(PreheatIdentifier.UID, DataElement.class, de3.getUid()));
  }

  @Test
  void testPut_SingleCode() {
    preheat.put(PreheatIdentifier.CODE, de1);
    preheat.put(PreheatIdentifier.CODE, de2);
    preheat.put(PreheatIdentifier.CODE, de3);
    assertEquals(1, preheat.getKlassKeyCount(PreheatIdentifier.CODE));
    assertEquals(3, preheat.getIdentifierKeyCount(PreheatIdentifier.CODE, DataElement.class));
    assertSame(de1, preheat.get(PreheatIdentifier.CODE, DataElement.class, de1.getCode()));
    assertSame(de2, preheat.get(PreheatIdentifier.CODE, DataElement.class, de2.getCode()));
    assertSame(de3, preheat.get(PreheatIdentifier.CODE, DataElement.class, de3.getCode()));
  }

  @Test
  void testPut_NoReplaceUID() {
    preheat.put(PreheatIdentifier.UID, de1);
    // try to override
    DataElement de1copy = new DataElement(de1.getName());
    de1copy.setUid(de1.getUid());
    de1copy.setCode(de1.getCode());
    preheat.put(PreheatIdentifier.UID, de1copy);
    assertSame(de1, preheat.get(PreheatIdentifier.UID, DataElement.class, de1.getUid()));
  }

  @Test
  void testPut_NoReplaceCode() {
    preheat.put(PreheatIdentifier.CODE, de1);
    // try to override
    DataElement de1copy = new DataElement(de1.getName());
    de1copy.setUid(de1.getUid());
    de1copy.setCode(de1.getCode());
    preheat.put(PreheatIdentifier.CODE, de1copy);
    assertSame(de1, preheat.get(PreheatIdentifier.CODE, DataElement.class, de1.getCode()));
  }

  @Test
  void testPut_ManyUID() {
    preheat.put(PreheatIdentifier.UID, asList(de1, de2, de3));
    assertEquals(1, preheat.getKlassKeyCount(PreheatIdentifier.UID));
    assertEquals(3, preheat.getIdentifierKeyCount(PreheatIdentifier.UID, DataElement.class));
    assertSame(de1, preheat.get(PreheatIdentifier.UID, DataElement.class, de1.getUid()));
    assertSame(de2, preheat.get(PreheatIdentifier.UID, DataElement.class, de2.getUid()));
    assertSame(de3, preheat.get(PreheatIdentifier.UID, DataElement.class, de3.getUid()));
  }

  @Test
  void testPut_ManyCode() {
    preheat.put(PreheatIdentifier.CODE, asList(de1, de2, de3));
    assertEquals(1, preheat.getKlassKeyCount(PreheatIdentifier.CODE));
    assertEquals(3, preheat.getIdentifierKeyCount(PreheatIdentifier.CODE, DataElement.class));
    assertSame(de1, preheat.get(PreheatIdentifier.CODE, DataElement.class, de1.getCode()));
    assertSame(de2, preheat.get(PreheatIdentifier.CODE, DataElement.class, de2.getCode()));
    assertSame(de3, preheat.get(PreheatIdentifier.CODE, DataElement.class, de3.getCode()));
  }

  @Test
  void testReplace_Null() {
    assertTrue(preheat.replace(PreheatIdentifier.UID, null).isEmpty());
    assertTrue(preheat.replace(PreheatIdentifier.CODE, null).isEmpty());
    assertTrue(preheat.replace(PreheatIdentifier.UID, new DataElement()).isEmpty());
    assertTrue(preheat.replace(PreheatIdentifier.CODE, new DataElement()).isEmpty());
    DataElement empty = new DataElement();
    empty.setUid("");
    empty.setCode("");
    assertTrue(preheat.replace(PreheatIdentifier.UID, empty).isEmpty());
    assertTrue(preheat.replace(PreheatIdentifier.CODE, empty).isEmpty());
  }

  @Test
  void testReplace_UID() {
    preheat.replace(PreheatIdentifier.UID, de1);
    assertSame(de1, preheat.get(PreheatIdentifier.UID, DataElement.class, de1.getUid()));
    // try to override
    DataElement de1copy = new DataElement(de1.getName());
    de1copy.setUid(de1.getUid());
    de1copy.setCode(de1.getCode());
    preheat.replace(PreheatIdentifier.UID, de1copy);
    assertSame(de1copy, preheat.get(PreheatIdentifier.UID, DataElement.class, de1.getUid()));
  }

  @Test
  void testReplace_Code() {
    preheat.replace(PreheatIdentifier.CODE, de1);
    assertSame(de1, preheat.get(PreheatIdentifier.CODE, DataElement.class, de1.getCode()));
    // try to override
    DataElement de1copy = new DataElement(de1.getName());
    de1copy.setUid(de1.getUid());
    de1copy.setCode(de1.getCode());
    preheat.replace(PreheatIdentifier.CODE, de1copy);
    assertSame(de1copy, preheat.get(PreheatIdentifier.CODE, DataElement.class, de1.getCode()));
  }

  @Test
  void testRemove_Null() {
    assertTrue(preheat.remove(PreheatIdentifier.UID, null).isEmpty());
  }

  @Test
  void testRemove_Empty() {
    assertTrue(preheat.remove(PreheatIdentifier.UID, de1).isEmpty());
  }

  @Test
  void testRemove_NonEmptySingleUID() {
    preheat.put(PreheatIdentifier.UID, asList(de1, de2, de3));
    preheat.remove(PreheatIdentifier.UID, DataElement.class, de2.getUid());
    assertEquals(asList(de1, de3), preheat.getAll(PreheatIdentifier.UID, asList(de1, de2, de3)));
  }

  @Test
  void testRemove_NonEmptySingleCode() {
    preheat.put(PreheatIdentifier.CODE, asList(de1, de2, de3));
    preheat.remove(PreheatIdentifier.CODE, DataElement.class, de2.getCode());
    assertEquals(asList(de1, de3), preheat.getAll(PreheatIdentifier.CODE, asList(de1, de2, de3)));
  }

  @Test
  void testRemove_NonEmptyManyUID() {
    preheat.put(PreheatIdentifier.UID, asList(de1, de2, de3));
    preheat.remove(PreheatIdentifier.UID, DataElement.class, asList(de1.getUid(), de3.getUid()));
    assertEquals(singletonList(de2), preheat.getAll(PreheatIdentifier.UID, asList(de1, de2, de3)));
  }

  @Test
  void testRemove_NonEmptyManyCode() {
    preheat.put(PreheatIdentifier.CODE, asList(de1, de2, de3));
    preheat.remove(PreheatIdentifier.CODE, DataElement.class, asList(de1.getCode(), de3.getCode()));
    assertEquals(singletonList(de2), preheat.getAll(PreheatIdentifier.CODE, asList(de1, de2, de3)));
  }
}
