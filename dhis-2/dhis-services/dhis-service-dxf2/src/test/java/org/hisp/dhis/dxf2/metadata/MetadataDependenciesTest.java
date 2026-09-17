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
package org.hisp.dhis.dxf2.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.option.OptionSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MetadataDependencies}, the monoid that multi-root dependency export is
 * built on. If these laws hold, "no root-level duplication" is a property of the union rather than
 * something the export has to remember to do.
 *
 * @author David Mackessy
 */
class MetadataDependenciesTest {

  @Test
  @DisplayName("The union of no closures at all is empty")
  void unionOfEmptyStreamIsEmpty() {
    assertTrue(
        Stream.<Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>>>of()
            .collect(MetadataDependencies.union())
            .isEmpty());
  }

  @Test
  @DisplayName("Left identity: merging into empty yields the original closure")
  void unionIsIdentityOnSingleElement() {
    DataSet dataSet = dataSet("dataSetAaaa", "DataSet A");
    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> closure =
        closureOf(DataSet.class, dataSet);

    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> result =
        Stream.<Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>>>of(closure)
            .collect(MetadataDependencies.union());

    assertEquals(closure, result);
  }

  @Test
  @DisplayName("Closures of different types are merged into one result")
  void unionMergesDisjointKeys() {
    DataSet dataSet = dataSet("dataSetAaaa", "DataSet A");
    OptionSet optionSet = optionSet("optionSetA", "OptionSet A");

    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> result =
        union(closureOf(DataSet.class, dataSet), closureOf(OptionSet.class, optionSet));

    assertEquals(Set.of(DataSet.class, OptionSet.class), result.keySet());
    assertEquals(Set.of(dataSet), result.get(DataSet.class));
    assertEquals(Set.of(optionSet), result.get(OptionSet.class));
  }

  @Test
  @DisplayName("An object reached from two closures is stored once, even as two instances")
  void unionDedupsEqualValuesUnderSameKey() {
    // two distinct instances of the same row, as two separate traversals could produce
    DataElement first = dataElement("dataElemAa", "Data Element A");
    DataElement second = dataElement("dataElemAa", "Data Element A");

    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> result =
        union(closureOf(DataElement.class, first), closureOf(DataElement.class, second));

    assertEquals(1, result.get(DataElement.class).size());
  }

  @Test
  @DisplayName("Distinct objects of the same type are all kept")
  void unionKeepsDistinctObjectsOfSameType() {
    DataSet a = dataSet("dataSetAaaa", "DataSet A");
    DataSet b = dataSet("dataSetBbbb", "DataSet B");

    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> result =
        union(closureOf(DataSet.class, a), closureOf(DataSet.class, b));

    assertEquals(Set.of(a, b), result.get(DataSet.class));
  }

  @Test
  @DisplayName("The inputs of a union are left untouched")
  void unionDoesNotMutateInputs() {
    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> first =
        closureOf(DataSet.class, dataSet("dataSetAaaa", "DataSet A"));
    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> second =
        closureOf(DataSet.class, dataSet("dataSetBbbb", "DataSet B"));

    union(first, second);

    assertEquals(1, first.get(DataSet.class).size(), "left input was modified");
    assertEquals(1, second.get(DataSet.class).size(), "right input was modified");
  }

  @Test
  @DisplayName("Union is associative, so roots can be folded in any grouping")
  void unionIsAssociative() {
    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> a =
        closureOf(DataSet.class, dataSet("dataSetAaaa", "DataSet A"));
    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> b =
        closureOf(DataSet.class, dataSet("dataSetBbbb", "DataSet B"));
    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> c =
        closureOf(OptionSet.class, optionSet("optionSetA", "OptionSet A"));

    assertEquals(
        union(union(a, b), c),
        union(a, union(b, c)),
        "(a union b) union c must equal a union (b union c)");
  }

  private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> union(
      Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> a,
      Map<Class<? extends IdentifiableObject>, Set<IdentifiableObject>> b) {
    return Stream.of(a, b).collect(MetadataDependencies.union());
  }

  private SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> closureOf(
      Class<? extends IdentifiableObject> type, IdentifiableObject object) {
    SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> closure =
        MetadataDependencies.empty();
    closure.putValue(type, object);
    return closure;
  }

  private DataSet dataSet(String uid, String name) {
    DataSet dataSet = new DataSet();
    dataSet.setUid(uid);
    dataSet.setName(name);
    return dataSet;
  }

  private DataElement dataElement(String uid, String name) {
    DataElement dataElement = new DataElement();
    dataElement.setUid(uid);
    dataElement.setName(name);
    return dataElement;
  }

  private OptionSet optionSet(String uid, String name) {
    OptionSet optionSet = new OptionSet();
    optionSet.setUid(uid);
    optionSet.setName(name);
    return optionSet;
  }
}
