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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonDiff;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link OrgTreeController} against the reference tree described in the design docs.
 *
 * <p>Every org unit is created with the reference's short id as its <em>base</em> name ({@code
 * ou-1}, {@code ou-2}, …) and receives an {@code en} translation carrying the human-readable name
 * from the reference ("Zambia", "Central Province", …). Every query passes {@code locale=en}, so
 * the returned {@code displayName} is the translated English name — this way tree ordering in the
 * responses matches the reference examples while also exercising the translation path end-to-end (a
 * missing/ignored translation would surface as {@code "ou-1"} in the output).
 */
@Transactional
class OrgTreeControllerTest extends PostgresControllerIntegrationTestBase {

  // Reference short ids (ou-1..ou-19) rewritten as valid UIDs
  private static final String ZAMBIA = "ou000000001";
  private static final String CENTRAL = "ou000000002";
  private static final String KABWE = "ou000000003";
  private static final String KABWE_HC = "ou000000004";
  private static final String CHISEMBE = "ou000000005";
  private static final String SERENJE = "ou000000006";
  private static final String SERENJE_HOSP = "ou000000007";
  private static final String KANONA = "ou000000008";
  private static final String MULOBEZI = "ou000000009";
  private static final String CHIKUPI = "ou000000010";
  private static final String TUTA = "ou000000011";
  private static final String BALAMBA = "ou000000012";
  private static final String LUSAKA_PROV = "ou000000013";
  private static final String KAFUE = "ou000000014";
  private static final String KAFUE_CLINIC = "ou000000015";
  private static final String KAFUE_HOSP = "ou000000016";
  private static final String LUSAKA_DIST = "ou000000017";
  private static final String CHELSTONE = "ou000000018";
  private static final String KABWATA = "ou000000019";

  @BeforeEach
  void setUp() {
    // Base name = "ou-N" (sort id); en translation = the readable reference name
    addOrgUnit(ZAMBIA, "ou-1", null, "Zambia");
    addOrgUnit(CENTRAL, "ou-2", ZAMBIA, "Central Province");
    addOrgUnit(KABWE, "ou-3", CENTRAL, "Kabwe District");
    addOrgUnit(KABWE_HC, "ou-4", KABWE, "Kabwe Health Centre");
    addOrgUnit(CHISEMBE, "ou-5", KABWE, "Chisembe Clinic");
    addOrgUnit(SERENJE, "ou-6", CENTRAL, "Serenje District");
    addOrgUnit(SERENJE_HOSP, "ou-7", SERENJE, "Serenje Hospital");
    addOrgUnit(KANONA, "ou-8", SERENJE, "Kanona Clinic");
    addOrgUnit(MULOBEZI, "ou-9", SERENJE, "Mulobezi Mission");
    addOrgUnit(CHIKUPI, "ou-10", SERENJE, "Chikupi Mission");
    addOrgUnit(TUTA, "ou-11", SERENJE, "Tuta Mission");
    addOrgUnit(BALAMBA, "ou-12", SERENJE, "Balamba Mission");
    addOrgUnit(LUSAKA_PROV, "ou-13", ZAMBIA, "Lusaka Province");
    addOrgUnit(KAFUE, "ou-14", LUSAKA_PROV, "Kafue District");
    addOrgUnit(KAFUE_CLINIC, "ou-15", KAFUE, "Kafue Clinic");
    addOrgUnit(KAFUE_HOSP, "ou-16", KAFUE, "Kafue Hospital");
    addOrgUnit(LUSAKA_DIST, "ou-17", LUSAKA_PROV, "Lusaka District");
    addOrgUnit(CHELSTONE, "ou-18", LUSAKA_DIST, "Chelstone Clinic");
    addOrgUnit(KABWATA, "ou-19", LUSAKA_DIST, "Kabwata Health Post");

    // Reference examples assume the current user's tree root is Zambia
    User user = makeUser("t");
    OrganisationUnit zambia = manager.get(OrganisationUnit.class, ZAMBIA);
    user.setOrganisationUnits(Set.of(zambia));
    user.setDataViewOrganisationUnits(Set.of(zambia));
    user.setTeiSearchOrganisationUnits(Set.of(zambia));
    userService.addUser(user);
    switchToNewUser(user);
  }

  /**
   * @param uid the OU's UID
   * @param name the base name (= short sort id like {@code ou-1}); the assertion fails visibly with
   *     {@code "ou-N"} if the translation is not applied
   * @param parentUid the parent OU's UID, or {@code null} for a root
   * @param englishName the readable name, added as an {@code en}/{@code NAME} translation
   */
  private void addOrgUnit(String uid, String name, String parentUid, String englishName) {
    String parent = parentUid == null ? "" : ",'parent':{'id':'" + parentUid + "'}";
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            """
            {'id':'%s','name':'%s','shortName':'%s','code':'C%s','openingDate':'2021'%s}
            """
                .formatted(uid, name, name, uid, parent)));
    assertStatus(
        HttpStatus.NO_CONTENT,
        PUT(
            "/organisationUnits/" + uid + "/translations",
            "{'translations':[{'locale':'en','property':'NAME','value':'%s'}]}"
                .formatted(englishName)));
  }

  private static void assertJsonEquals(@Language("json") String expected, JsonMixed actual) {
    JsonDiff diff = JsonMixed.of(expected).diff(actual, JsonDiff.Mode.LENIENT);
    assertEquals(
        0,
        diff.differences().size(),
        () -> {
          StringBuilder str = new StringBuilder();
          str.append('\n');
          str.append(actual.toJson());
          str.append('\n');
          for (JsonDiff.Difference d : diff.differences()) str.append(d).append('\n');
          return str.toString();
        });
  }

  // ---------------------------------------------------------------------------------------
  // Case 1 — search across pages
  // ---------------------------------------------------------------------------------------

  @Test
  void testSearchAcrossPages_Page1() {
    JsonMixed actual = GET("/organisationUnits/tree?search=Clinic&pageSize=2&locale=en").content();
    assertJsonEquals(
        """
        {
          "pager": { "page": 1, "pageSize": 2, "total": 4, "pageCount": 2 },
          "organisationUnits": [
            { "id": "ou000000005", "displayName": "Chisembe Clinic", "level": 4, "path": "/ou000000001/ou000000002/ou000000003/ou000000005", "leaf": true },
            { "id": "ou000000008", "displayName": "Kanona Clinic",   "level": 4, "path": "/ou000000001/ou000000002/ou000000006/ou000000008", "leaf": true }
          ],
          "ancestors": [
            { "id": "ou000000001", "displayName": "Zambia",            "level": 1, "path": "/ou000000001",                            "leaf": false },
            { "id": "ou000000002", "displayName": "Central Province",  "level": 2, "path": "/ou000000001/ou000000002",                "leaf": false },
            { "id": "ou000000003", "displayName": "Kabwe District",    "level": 3, "path": "/ou000000001/ou000000002/ou000000003",    "leaf": false },
            { "id": "ou000000006", "displayName": "Serenje District",  "level": 3, "path": "/ou000000001/ou000000002/ou000000006",    "leaf": false }
          ]
        }
        """,
        actual);
  }

  /**
   * Case 1 page 2 — matching the reference URL, which does not pass a {@code pageCursor}. Note that
   * {@code OrgTreeParams}'s compact ctor throws {@code E8305} for any {@code page > 1} without a
   * cursor, so this test will currently fail with a 400 unless the pipeline special-cases offset
   * paging for search. If it does not, replace {@code &page=2} with a cursor pulled from page 1's
   * {@code pager.pageCursor[0]}.
   */
  @Test
  void testSearchAcrossPages_Page2() {
    JsonMixed page1 =
        GET("/organisationUnits/tree?search=Clinic&pageSize=2&page=1&locale=en").content();

    String cursor = page1.get("pager").get("pageCursor").get(0).string();
    JsonMixed actual =
        GET("/organisationUnits/tree?search=Clinic&pageSize=2&page=2&locale=en&pageCursor="
                + cursor)
            .content();
    assertJsonEquals(
        """
        {
          "pager": { "page": 2, "pageSize": 2, "total": 4, "pageCount": 2 },
          "organisationUnits": [
            { "id": "ou000000015", "displayName": "Kafue Clinic",     "level": 4, "path": "/ou000000001/ou000000013/ou000000014/ou000000015", "leaf": true },
            { "id": "ou000000018", "displayName": "Chelstone Clinic", "level": 4, "path": "/ou000000001/ou000000013/ou000000017/ou000000018", "leaf": true }
          ],
          "ancestors": [
            { "id": "ou000000001", "displayName": "Zambia",           "level": 1, "path": "/ou000000001",                            "leaf": false },
            { "id": "ou000000013", "displayName": "Lusaka Province",  "level": 2, "path": "/ou000000001/ou000000013",                "leaf": false },
            { "id": "ou000000014", "displayName": "Kafue District",   "level": 3, "path": "/ou000000001/ou000000013/ou000000014",    "leaf": false },
            { "id": "ou000000017", "displayName": "Lusaka District",  "level": 3, "path": "/ou000000001/ou000000013/ou000000017",    "leaf": false }
          ]
        }
        """,
        actual);
  }

  // ---------------------------------------------------------------------------------------
  // Case 2 — pre-opened tree (depth)
  // ---------------------------------------------------------------------------------------

  @Test
  void testDepthTree() {
    JsonMixed actual = GET("/organisationUnits/tree?depth=2&locale=en").content();
    assertJsonEquals(
        """
        {
          "pager": { "page": 1, "pageSize": 50, "total": 7, "pageCount": 1 },
          "organisationUnits": [
            { "id": "ou000000001", "displayName": "Zambia",            "level": 1, "path": "/ou000000001",                           "leaf": false },
            { "id": "ou000000002", "displayName": "Central Province",  "level": 2, "path": "/ou000000001/ou000000002",               "leaf": false },
            { "id": "ou000000003", "displayName": "Kabwe District",    "level": 3, "path": "/ou000000001/ou000000002/ou000000003",   "leaf": false },
            { "id": "ou000000006", "displayName": "Serenje District",  "level": 3, "path": "/ou000000001/ou000000002/ou000000006",   "leaf": false },
            { "id": "ou000000013", "displayName": "Lusaka Province",   "level": 2, "path": "/ou000000001/ou000000013",               "leaf": false },
            { "id": "ou000000014", "displayName": "Kafue District",    "level": 3, "path": "/ou000000001/ou000000013/ou000000014",   "leaf": false },
            { "id": "ou000000017", "displayName": "Lusaka District",   "level": 3, "path": "/ou000000001/ou000000013/ou000000017",   "leaf": false }
          ],
          "ancestors": []
        }
        """,
        actual);
  }

  // ---------------------------------------------------------------------------------------
  // Case 3 — custom roots
  // ---------------------------------------------------------------------------------------

  /**
   * The reference uses {@code rootIds=…}; the current {@code RootInput} record names the field
   * {@code roots}, so this parameter name may need a query alias on the controller side to bind. If
   * binding fails the endpoint falls back to the user's default hierarchy (Zambia), returning four
   * matches instead of two — the assertion will show that.
   */
  @Test
  void testCustomRoots() {
    JsonMixed actual =
        GET("/organisationUnits/tree?roots=ou000000006,ou000000003&search=Clinic&locale=en")
            .content();
    assertJsonEquals(
        """
        {
          "pager": { "page": 1, "pageSize": 50, "total": 2, "pageCount": 1 },
          "organisationUnits": [
            { "id": "ou000000005", "displayName": "Chisembe Clinic", "level": 4, "path": "/ou000000001/ou000000002/ou000000003/ou000000005", "leaf": true },
            { "id": "ou000000008", "displayName": "Kanona Clinic",   "level": 4, "path": "/ou000000001/ou000000002/ou000000006/ou000000008", "leaf": true }
          ],
          "ancestors": [
            { "id": "ou000000003", "displayName": "Kabwe District",   "level": 3, "path": "/ou000000001/ou000000002/ou000000003", "leaf": false },
            { "id": "ou000000006", "displayName": "Serenje District", "level": 3, "path": "/ou000000001/ou000000002/ou000000006", "leaf": false }
          ]
        }
        """,
        actual);
  }
}
