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
package org.hisp.dhis.organisationunit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class OrgTreeTest {

  @Test
  void testSortedHierarchical() {
    assertSorted(List.of("a", "a/b", "a/c", "b", "b/a"), List.of("a", "b", "a/b", "a/c", "b/a"));
    assertSorted(
        List.of("a", "a/b", "a/c", "b", "b/a", "b/b"),
        List.of("a", "b", "a/b", "a/c", "b/a", "b/b"));
    assertSorted(
        List.of("a", "a/b", "a/b/x", "a/c", "b", "b/a", "c", "c/t"),
        List.of("a", "b", "c", "a/b", "a/c", "b/a", "c/t", "a/b/x"));
  }

  private void assertSorted(List<String> expected, List<String> actual) {
    List<OrgTree.OrgTreeEntry> actualEntries = namesToEntries(actual);
    List<OrgTree.OrgTreeEntry> actualSorted = OrgTree.sortedHierarchical(actualEntries);
    assertEquals(expected, actualSorted.stream().map(OrgTree.OrgTreeEntry::displayName).toList());
  }

  private static List<OrgTree.OrgTreeEntry> namesToEntries(List<String> paths) {
    return paths.stream().map(OrgTreeTest::nameToEntry).toList();
  }

  private static OrgTree.OrgTreeEntry nameToEntry(String name) {
    OrgUnitPath path = lettersToPath(name);
    return new OrgTree.OrgTreeEntry(path, name, path.length(), false);
  }

  private static OrgUnitPath lettersToPath(String path) {
    return OrgUnitPath.of(
        "/"
            + Stream.of(path.split("/"))
                .map(letter -> letter + "1234567890")
                .collect(Collectors.joining("/")));
  }
}
