/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.common.collection;

import static org.hisp.dhis.common.collection.CollectionUtils.addIfNotNull;
import static org.hisp.dhis.common.collection.CollectionUtils.emptyIfNull;
import static org.hisp.dhis.common.collection.CollectionUtils.firstMatch;
import static org.hisp.dhis.common.collection.CollectionUtils.flatMapToSet;
import static org.hisp.dhis.common.collection.CollectionUtils.isEmpty;
import static org.hisp.dhis.common.collection.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.collection.CollectionUtils.mapToList;
import static org.hisp.dhis.common.collection.CollectionUtils.mapToSet;
import static org.hisp.dhis.common.collection.CollectionUtils.merge;
import static org.hisp.dhis.common.collection.CollectionUtils.union;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CollectionUtilsTest {
  @Test
  void testFlatMapToSet() {
    DataElement deA = new DataElement();
    DataElement deB = new DataElement();
    DataElement deC = new DataElement();
    DataSet dsA = new DataSet();
    DataSet dsB = new DataSet();

    deA.setAutoFields();
    deB.setAutoFields();
    deC.setAutoFields();
    dsA.setAutoFields();
    dsB.setAutoFields();

    dsA.addDataSetElement(deA);
    dsA.addDataSetElement(deB);
    dsB.addDataSetElement(deB);
    dsB.addDataSetElement(deC);

    List<DataSet> dataSets = List.of(dsA, dsB);

    Set<DataElement> dataElements = flatMapToSet(dataSets, DataSet::getDataElements);

    assertEquals(3, dataElements.size());
    assertTrue(dataElements.contains(deA));
  }

  @Test
  void testMapToSet() {
    DataElement deA = new DataElement();
    DataElement deB = new DataElement();
    DataElement deC = new DataElement();

    CategoryCombo ccA = new CategoryCombo();
    CategoryCombo ccB = new CategoryCombo();

    ccA.setAutoFields();
    ccB.setAutoFields();

    deA.setCategoryCombo(ccA);
    deB.setCategoryCombo(ccA);
    deC.setCategoryCombo(ccB);

    List<DataElement> dataElements = List.of(deA, deB, deC);

    Set<CategoryCombo> categoryCombos = mapToSet(dataElements, DataElement::getCategoryCombo);

    assertEquals(2, categoryCombos.size());
    assertTrue(categoryCombos.contains(ccA));
  }

  @Test
  void testFirstMatch() {
    List<String> collection = List.of("a", "b", "c");

    assertEquals("a", firstMatch(collection, "a"::equals));
    assertEquals("b", firstMatch(collection, "b"::equals));
    assertNull(firstMatch(collection, "x"::equals));
  }

  @ParameterizedTest
  @MethodSource("differenceInput")
  <T> void testDifference(Collection<T> col1, Collection<T> col2, Collection<T> expected) {
    List<T> difference = CollectionUtils.difference(col1, col2);

    assertEquals(expected.size(), difference.size());
    assertTrue(expected.containsAll(difference));
  }

  public static Stream<Arguments> differenceInput() {
    return Stream.of(
        Arguments.of(Set.of(1, 2, 3), Set.of(4, 5, 6), Set.of(1, 2, 3)),
        Arguments.of(Set.of(1, 2, 3), Set.of(1, 2), Set.of(3)),
        Arguments.of(Set.of(1, 2), Set.of(1, 2, 3), Set.of()),
        Arguments.of(Set.of(2), List.of(5, 6), Set.of(2)),
        Arguments.of(Set.of(), List.of(5, 6), Set.of()),
        Arguments.of(
            List.of("One", "Two", "Three"), List.of("One", "Two", "Four"), List.of("Three")),
        Arguments.of(Set.of("A"), Set.of("A", "B", "C"), Set.of()),
        Arguments.of(Set.of("A", "B", "C"), Set.of("A"), Set.of("B", "C")));
  }

  @Test
  void testConcat() {
    List<String> collection1 = List.of("a", "b", "c");
    List<String> collection2 = List.of("c", "d", "e");
    List<String> concat = CollectionUtils.concat(collection1, collection2);

    assertEquals(List.of("a", "b", "c", "c", "d", "e"), concat);
  }

  @Test
  void testMapToList() {
    List<String> collection = Lists.newArrayList("1", "2", "3");

    assertEquals(3, mapToList(collection, Integer::parseInt).size());
    assertEquals(1, mapToList(collection, Integer::parseInt).get(0));
  }

  @Test
  void testEmptyIfNullSet() {
    Set<String> setA = Set.of("One", "Two", "Three");
    Set<String> setB = null;

    assertEquals(setA, emptyIfNull(setA));
    assertEquals(new HashSet<>(), emptyIfNull(setB));
  }

  @Test
  void testEmptyIfNullList() {
    List<String> listA = List.of("One", "Two", "Three");
    List<String> listB = null;

    assertEquals(listA, emptyIfNull(listA));
    assertEquals(new ArrayList<>(), emptyIfNull(listB));
  }

  @Test
  void testAddIfNotNull() {
    List<String> list = new ArrayList<>();
    addIfNotNull(list, "One");
    addIfNotNull(list, null);
    addIfNotNull(list, "Three");

    assertEquals(2, list.size());
  }

  @Test
  void testIsEmpty() {
    assertTrue(isEmpty(List.of()));
    assertTrue(isEmpty(null));
    assertFalse(isEmpty(List.of("One", "Two")));
  }

  @Test
  void testIsNotEmpty() {
    assertFalse(isNotEmpty(List.of()));
    assertFalse(isNotEmpty(null));
    assertTrue(isNotEmpty(List.of("One", "Two")));
  }

  @Test
  void testUnion_Empty() {
    assertEquals(Set.of(), union(null, null));
    assertEquals(Set.of(), union(null, Set.of()));
    assertEquals(Set.of(), union(Set.of(), null));
    assertEquals(Set.of(), union(Set.of(), Set.of()));
  }

  @Test
  void testUnion_EmptyMore() {
    assertEquals(Set.of(), union(null, null, (Set<Object>) null));
    assertEquals(Set.of(), union(null, null, (Set<Object>[]) null));
    assertEquals(Set.of(), union(null, null, Set.of()));
    assertEquals(Set.of(), union(null, Set.of(), (Set<Object>) null));
    assertEquals(Set.of(), union(null, Set.of(), (Set<Object>[]) null));
    assertEquals(Set.of(), union(null, Set.of(), Set.of()));
    assertEquals(Set.of(), union(Set.of(), null, (Set<Object>) null));
    assertEquals(Set.of(), union(Set.of(), null, (Set<Object>[]) null));
    assertEquals(Set.of(), union(Set.of(), null, Set.of()));
    assertEquals(Set.of(), union(Set.of(), Set.of(), (Set<Object>) null));
    assertEquals(Set.of(), union(Set.of(), Set.of(), (Set<Object>[]) null));
    assertEquals(Set.of(), union(Set.of(), Set.of(), Set.of()));
  }

  @Test
  void testUnion_One() {
    assertEquals(Set.of("a"), union(Set.of("a"), null));
    assertEquals(Set.of("a"), union(Set.of("a"), Set.of()));
    assertEquals(Set.of("a"), union(null, Set.of("a")));
    assertEquals(Set.of("a"), union(Set.of(), Set.of("a")));
  }

  @Test
  void testUnion_Two() {
    assertEquals(Set.of("a", "b"), union(Set.of("a"), Set.of("b")));
    assertEquals(Set.of("a", "b"), union(Set.of("b"), Set.of("a")));
    assertEquals(Set.of("a", "b"), union(Set.of(), Set.of("b", "a")));
    assertEquals(Set.of("a", "b"), union(Set.of("b", "a"), Set.of()));
  }

  @Test
  void testUnion_More() {
    assertEquals(Set.of("a", "b"), union(null, Set.of("a"), Set.of("b")));
    assertEquals(Set.of("a", "b"), union(null, Set.of("b"), Set.of("a")));
    assertEquals(Set.of("a", "b"), union(null, Set.of(), Set.of("b", "a")));
    assertEquals(Set.of("a", "b"), union(null, Set.of("b", "a"), Set.of()));

    assertEquals(Set.of("a", "b"), union(Set.of("a"), null, Set.of("b")));
    assertEquals(Set.of("a", "b"), union(Set.of("b"), null, Set.of("a")));
    assertEquals(Set.of("a", "b"), union(Set.of(), null, Set.of("b", "a")));
    assertEquals(Set.of("a", "b"), union(Set.of("b", "a"), null, Set.of()));

    assertEquals(Set.of("a", "b", "c"), union(Set.of("a"), Set.of("c"), Set.of("b")));
    assertEquals(Set.of("a", "b", "c"), union(Set.of("b"), Set.of("c"), Set.of("a")));
    assertEquals(Set.of("a", "b", "c"), union(Set.of(), Set.of("c"), Set.of("b", "a")));
    assertEquals(Set.of("a", "b", "c"), union(Set.of("b", "a"), Set.of("c"), Set.of()));
  }

  @Test
  void testMerge() {
    assertEquals(Map.of(), merge(Map.of(), Map.of()));
    assertEquals(Map.of("a", "b"), merge(Map.of("a", "b"), Map.of()));
    assertEquals(Map.of("a", "b"), merge(Map.of(), Map.of("a", "b")));
    assertEquals(Map.of("a", "b", "c", "d"), merge(Map.of("c", "d"), Map.of("a", "b")));
    assertEquals(
        Map.of("a", "b", "c", "d", "e", "f"), merge(Map.of("c", "d"), Map.of("a", "b", "e", "f")));
  }

  @Test
  void testMerge_Override() {
    assertEquals(Map.of("a", "b", "c", "d"), merge(Map.of("c", "d", "a", "x"), Map.of("a", "b")));
  }

  @Test
  void testMerge_Null() {
    HashMap<String, String> m1 = new HashMap<>();
    m1.put("x", null);
    assertThrowsExactly(NullPointerException.class, () -> merge(Map.of("a", "b"), m1));
  }
}
