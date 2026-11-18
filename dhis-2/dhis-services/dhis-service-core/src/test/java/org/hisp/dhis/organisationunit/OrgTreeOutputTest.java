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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.common.UID;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

/** Tests for {@link OrgTreeOutput#toJson(OrgTree, java.io.OutputStream)}. */
class OrgTreeOutputTest {

  private static final UID ZAMBIA_UID = UID.of("aaaaaaaaaaa");
  private static final UID CENTRAL_UID = UID.of("bbbbbbbbbbb");
  private static final UID KABWE_UID = UID.of("ccccccccccc");
  private static final UID CLINIC_1_UID = UID.of("ddddddddddd");
  private static final UID CLINIC_2_UID = UID.of("eeeeeeeeeee");

  private static final OrgUnitPath ZAMBIA = new OrgUnitPath(null, ZAMBIA_UID);
  private static final OrgUnitPath CENTRAL = new OrgUnitPath(ZAMBIA, CENTRAL_UID);
  private static final OrgUnitPath KABWE = new OrgUnitPath(CENTRAL, KABWE_UID);
  private static final OrgUnitPath CLINIC_1 = new OrgUnitPath(KABWE, CLINIC_1_UID);
  private static final OrgUnitPath CLINIC_2 = new OrgUnitPath(KABWE, CLINIC_2_UID);

  // ---------- helpers ----------

  private static String toJson(OrgTree tree) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    OrgTreeOutput.toJson(tree, out);
    return out.toString(StandardCharsets.UTF_8);
  }

  /**
   * Asserts the exact JSON. Both sides are {@code strip()}'d so a trailing newline from the
   * expected text block does not affect the comparison; the JSON itself is compared verbatim.
   */
  private static void assertJson(@Language("json") String expected, OrgTree tree) {
    assertEquals(expected.strip(), toJson(tree).strip());
  }

  // ---------- tests ----------

  /**
   * No pager (the {@code null} branch): a single entry, no ancestors. This is the shape a
   * caller-side "expand one node" request would use.
   */
  @Test
  void noPagerOneEntryNoAncestors() {
    OrgTree tree =
        new OrgTree(
            null, Stream.of(new OrgTree.OrgTreeEntry(ZAMBIA, "Zambia", false)), Stream.empty());

    assertJson(
        """
        {"organisationUnits":[{"id":"aaaaaaaaaaa","displayName":"Zambia","path":"/aaaaaaaaaaa","level":1,"leaf":false}],"ancestors":[]}
        """,
        tree);
  }

  /**
   * Search-style response: pager with no page cursors, two matching clinics, and three ancestors.
   * Exercises the multi-element arrays for both {@code organisationUnits} and {@code ancestors},
   * the {@code pager} block, and the {@code pageCursor.isEmpty()} false branch of the pager.
   */
  @Test
  void pagerWithMatchesAndAncestors() {
    OrgTree tree =
        new OrgTree(
            new OrgTree.Pager(1, 2, 5, List.of()),
            Stream.of(
                new OrgTree.OrgTreeEntry(CLINIC_1, "Kabwe Clinic A", true),
                new OrgTree.OrgTreeEntry(CLINIC_2, "Kabwe Clinic B", true)),
            Stream.of(
                new OrgTree.OrgTreeEntry(ZAMBIA, "Zambia", false),
                new OrgTree.OrgTreeEntry(CENTRAL, "Central Province", false),
                new OrgTree.OrgTreeEntry(KABWE, "Kabwe District", false)));

    assertJson(
        """
        {"pager":{"page":1,"pageSize":2,"total":5,"pageCount":3},"organisationUnits":[\
        {"id":"ddddddddddd","displayName":"Kabwe Clinic A","path":"/aaaaaaaaaaa/bbbbbbbbbbb/ccccccccccc/ddddddddddd","level":4,"leaf":true},\
        {"id":"eeeeeeeeeee","displayName":"Kabwe Clinic B","path":"/aaaaaaaaaaa/bbbbbbbbbbb/ccccccccccc/eeeeeeeeeee","level":4,"leaf":true}\
        ],"ancestors":[\
        {"id":"aaaaaaaaaaa","displayName":"Zambia","path":"/aaaaaaaaaaa","level":1,"leaf":false},\
        {"id":"bbbbbbbbbbb","displayName":"Central Province","path":"/aaaaaaaaaaa/bbbbbbbbbbb","level":2,"leaf":false},\
        {"id":"ccccccccccc","displayName":"Kabwe District","path":"/aaaaaaaaaaa/bbbbbbbbbbb/ccccccccccc","level":3,"leaf":false}\
        ]}
        """,
        tree);
  }

  /**
   * Paging past the first page: the pager carries a non-empty {@code pageCursor}, which is the only
   * branch that emits the extra {@code pageCursor} array.
   */
  @Test
  void pagerWithPageCursor() {
    OrgTree tree =
        new OrgTree(
            new OrgTree.Pager(2, 2, 5, List.of(CENTRAL)),
            Stream.of(new OrgTree.OrgTreeEntry(KABWE, "Kabwe District", false)),
            Stream.of(new OrgTree.OrgTreeEntry(ZAMBIA, "Zambia", false)));

    assertJson(
        """
        {"pager":{"page":2,"pageSize":2,"total":5,"pageCount":3,"pageCursor":["/aaaaaaaaaaa/bbbbbbbbbbb"]},"organisationUnits":[\
        {"id":"ccccccccccc","displayName":"Kabwe District","path":"/aaaaaaaaaaa/bbbbbbbbbbb/ccccccccccc","level":3,"leaf":false}\
        ],"ancestors":[\
        {"id":"aaaaaaaaaaa","displayName":"Zambia","path":"/aaaaaaaaaaa","level":1,"leaf":false}\
        ]}
        """,
        tree);
  }
}
