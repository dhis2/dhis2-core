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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
class CombinationGeneratorTest {

  private IdentifiableObject a = new DataElementGroup("A");

  private IdentifiableObject b = new DataElementGroup("B");

  private IdentifiableObject c = new DataElementGroup("C");

  private IdentifiableObject d = new DataElementGroup("D");

  @Test
  void testGetNextA() {
    List<IdentifiableObject> a1 = Lists.newArrayList(a, b, c, d);
    List<IdentifiableObject> a2 = Lists.newArrayList(a, b, c);
    CombinationGenerator<IdentifiableObject> generator =
        CombinationGenerator.newInstance(asList(a1, a2));
    assertTrue(equals(generator.getNext(), a, a));
    assertTrue(equals(generator.getNext(), a, b));
    assertTrue(equals(generator.getNext(), a, c));
    assertTrue(equals(generator.getNext(), b, a));
    assertTrue(equals(generator.getNext(), b, b));
    assertTrue(equals(generator.getNext(), b, c));
    assertTrue(equals(generator.getNext(), c, a));
    assertTrue(equals(generator.getNext(), c, b));
    assertTrue(equals(generator.getNext(), c, c));
    assertTrue(equals(generator.getNext(), d, a));
    assertTrue(equals(generator.getNext(), d, b));
    assertTrue(equals(generator.getNext(), d, c));
    assertNull(generator.getNext());
  }

  @Test
  void testGetNextB() {
    List<IdentifiableObject> a1 = Lists.newArrayList(a, b);
    List<IdentifiableObject> a2 = Lists.newArrayList(a, b);
    List<IdentifiableObject> a3 = Lists.newArrayList(a, b, c);
    CombinationGenerator<IdentifiableObject> generator =
        CombinationGenerator.newInstance(asList(a1, a2, a3));
    assertTrue(equals(generator.getNext(), a, a, a));
    assertTrue(equals(generator.getNext(), a, a, b));
    assertTrue(equals(generator.getNext(), a, a, c));
    assertTrue(equals(generator.getNext(), a, b, a));
    assertTrue(equals(generator.getNext(), a, b, b));
    assertTrue(equals(generator.getNext(), a, b, c));
    assertTrue(equals(generator.getNext(), b, a, a));
    assertTrue(equals(generator.getNext(), b, a, b));
    assertTrue(equals(generator.getNext(), b, a, c));
    assertTrue(equals(generator.getNext(), b, b, a));
    assertTrue(equals(generator.getNext(), b, b, b));
    assertTrue(equals(generator.getNext(), b, b, c));
    assertNull(generator.getNext());
  }

  @Test
  void testGetNextC() {
    List<IdentifiableObject> a1 = Lists.newArrayList(a, b);
    CombinationGenerator<IdentifiableObject> generator =
        CombinationGenerator.newInstance(asList(a1));
    assertTrue(equals(generator.getNext(), a));
    assertTrue(equals(generator.getNext(), b));
    assertNull(generator.getNext());
    assertNull(generator.getNext());
  }

  @Test
  void testHasNextA() {
    List<IdentifiableObject> a1 = Lists.newArrayList(a, b, c, d);
    List<IdentifiableObject> a2 = Lists.newArrayList(a, b, c);
    CombinationGenerator<IdentifiableObject> generator =
        CombinationGenerator.newInstance(asList(a1, a2));
    for (int i = 0; i < 11; i++) {
      assertNotNull(generator.getNext());
      assertTrue(generator.hasNext());
    }
    // Last
    assertNotNull(generator.getNext());
    assertFalse(generator.hasNext());
    assertNull(generator.getNext());
    assertFalse(generator.hasNext());
    assertNull(generator.getNext());
    assertFalse(generator.hasNext());
  }

  @Test
  void testHasNextB() {
    List<IdentifiableObject> a1 = Lists.newArrayList(a, b);
    List<IdentifiableObject> a2 = Lists.newArrayList(a, b);
    List<IdentifiableObject> a3 = Lists.newArrayList(a, b, c);
    CombinationGenerator<IdentifiableObject> generator =
        CombinationGenerator.newInstance(asList(a1, a2, a3));
    while (generator.hasNext()) {
      assertNotNull(generator.getNext());
    }
    assertNull(generator.getNext());
    assertFalse(generator.hasNext());
    assertNull(generator.getNext());
    assertFalse(generator.hasNext());
  }

  @Test
  void testGetCombinationsA() {
    List<IdentifiableObject> a1 = Lists.newArrayList(a, b, c, d);
    List<IdentifiableObject> a2 = Lists.newArrayList(a, b, c);
    CombinationGenerator<IdentifiableObject> generator =
        CombinationGenerator.newInstance(asList(a1, a2));
    List<List<IdentifiableObject>> objects = generator.getCombinations();
    assertEquals(12, objects.size());
    assertTrue(equals(objects.get(0), a, a));
    assertTrue(equals(objects.get(1), a, b));
    assertTrue(equals(objects.get(2), a, c));
    assertTrue(equals(objects.get(3), b, a));
    assertTrue(equals(objects.get(4), b, b));
    assertTrue(equals(objects.get(5), b, c));
    assertTrue(equals(objects.get(6), c, a));
    assertTrue(equals(objects.get(7), c, b));
    assertTrue(equals(objects.get(8), c, c));
    assertTrue(equals(objects.get(9), d, a));
    assertTrue(equals(objects.get(10), d, b));
    assertTrue(equals(objects.get(11), d, c));
  }

  @Test
  void testGetCombinationsB() {
    List<IdentifiableObject> a1 = Lists.newArrayList(a, b);
    List<IdentifiableObject> a2 = Lists.newArrayList(a, b);
    List<IdentifiableObject> a3 = Lists.newArrayList(a, b, c);
    CombinationGenerator<IdentifiableObject> generator =
        CombinationGenerator.newInstance(asList(a1, a2, a3));
    List<List<IdentifiableObject>> objects = generator.getCombinations();
    assertEquals(12, objects.size());
    assertTrue(equals(objects.get(0), a, a, a));
    assertTrue(equals(objects.get(1), a, a, b));
    assertTrue(equals(objects.get(2), a, a, c));
    assertTrue(equals(objects.get(3), a, b, a));
    assertTrue(equals(objects.get(4), a, b, b));
    assertTrue(equals(objects.get(5), a, b, c));
    assertTrue(equals(objects.get(6), b, a, a));
    assertTrue(equals(objects.get(7), b, a, b));
    assertTrue(equals(objects.get(8), b, a, c));
    assertTrue(equals(objects.get(9), b, b, a));
    assertTrue(equals(objects.get(10), b, b, b));
    assertTrue(equals(objects.get(11), b, b, c));
    assertNull(generator.getNext());
  }

  @Test
  void testGetCombinationsC() {
    List<IdentifiableObject> a1 = Lists.newArrayList();
    CombinationGenerator<IdentifiableObject> generator =
        CombinationGenerator.newInstance(asList(a1));
    List<List<IdentifiableObject>> objects = generator.getCombinations();
    assertEquals(0, objects.size());
    assertNull(generator.getNext());
  }

  private static boolean equals(List<IdentifiableObject> objects, IdentifiableObject... integers) {
    return objects.equals(Arrays.asList(integers));
  }

  @SafeVarargs
  private static <T> List<List<T>> asList(List<T>... lists) {
    List<List<T>> output = new ArrayList<>();
    for (List<T> list : lists) {
      output.add(list);
    }
    return output;
  }
}
