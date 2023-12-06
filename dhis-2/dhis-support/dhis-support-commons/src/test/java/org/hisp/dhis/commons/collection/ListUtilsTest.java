package org.hisp.dhis.commons.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListUtilsTest {
  @Test
  void testDistinctUnion() {
    List<String> listA = List.of("One", "Two", "Three");
    List<String> listB = List.of("One", "Three", "Four");
    List<String> listC = List.of("Three", "Five");

    List<String> union = List.of("One", "Two", "Three", "Four", "Five");

    assertEquals(union, ListUtils.distinctUnion(listA, listB, listC));
  }
}
