/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static java.lang.String.format;
import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.datavalue.DataValueAuditStore;
import org.hisp.dhis.datavalue.hibernate.HibernateDataValueAuditStore;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.AopTestUtils;

/**
 * Tests the org unit and option combo checks of the aggregate data value change log ({@code
 * /api/audits/dataValue}) and the data value context ({@code /api/dataEntry/dataValueContext}) on a
 * hierarchy of several levels with two top-level units:
 *
 * <pre>
 * A ── B1 ── C1 ── D1        Z ── Z1
 *   │     └─ C2 ── D2
 *   └─ B2 ── C3 ── D3
 *               └─ D4
 * </pre>
 *
 * Each unit has four values: a data element with a disaggregation of an open and a restricted
 * option, and a data element in a data set whose attribute combo has an open and a restricted
 * option. The restricted options are shared with data read to one user group only. Every value is
 * entered twice so it has change log rows. The expected visible values are computed from the
 * hierarchy: a unit is visible when it is a capture or data view unit of the user, or below one.
 */
class DataValueChangelogAccessHierarchyControllerTest extends DhisControllerIntegrationTest {

  private static final String PE = "2021-01";

  /**
   * Start of the prev/nextPage links of a change log read with {@code fields=value}; the 2.41 test
   * client has no {@code /api} servlet path, as in the tracker export tests
   */
  private static final String PAGER_LINK = "http://localhost/audits/dataValue?fields=value";

  /** Kinds of values entered per unit, the digit of tens of the value */
  private static final int OPEN_COC = 1;

  private static final int RESTRICTED_COC = 2;
  private static final int OPEN_AOC = 3;
  private static final int RESTRICTED_AOC = 4;

  /** Most org units the store lists by ID: the default (users here have few) and none. */
  private static final List<Integer> MAX_ORG_UNIT_IDS =
      List.of(HibernateDataValueAuditStore.MAX_ORG_UNIT_IDS, 0);

  /**
   * The statements as on PostgreSQL 14 and later, and as before 14 (hashed subplans for the bound
   * lists), whatever the test database: both must read the same rows.
   */
  private static final List<Boolean> POSTGRES_14 = List.of(true, false);

  /** Units by code, in index order; the index is the hundreds of a value. */
  private final Map<String, String> units = new LinkedHashMap<>();

  private final Map<String, String> parents = new LinkedHashMap<>();

  private String disaggDe;
  private String attributeDe;
  private String defaultCoc;
  private String openCoc;
  private String restrictedCoc;
  private String attributeCombo;
  private String openOption;
  private String restrictedOption;
  private String group;

  @Autowired private DataValueAuditStore auditStore;

  private HibernateDataValueAuditStore store;

  @AfterEach
  void resetStore() {
    store.setMaxOrgUnitIds(HibernateDataValueAuditStore.MAX_ORG_UNIT_IDS);
    store.setPostgres14(null);
  }

  @BeforeEach
  void setUp() {
    store = AopTestUtils.getUltimateTargetObject(auditStore);
    addUnit("A", null);
    addUnit("B1", "A");
    addUnit("B2", "A");
    addUnit("C1", "B1");
    addUnit("C2", "B1");
    addUnit("C3", "B2");
    addUnit("D1", "C1");
    addUnit("D2", "C2");
    addUnit("D3", "C3");
    addUnit("D4", "C3");
    addUnit("Z", null);
    addUnit("Z1", "Z");
    assertStatus(
        HttpStatus.OK,
        POST(
            "/users/{id}/organisationUnits",
            getCurrentUser().getUid(),
            Body(
                format(
                    "{'additions':[{'id':'%s'},{'id':'%s'}]}", units.get("A"), units.get("Z")))));

    group = assertStatus(HttpStatus.CREATED, POST("/userGroups", "{'name':'Restricted readers'}"));
    String openCo = addCategoryOption("Open disaggregation", null);
    String restrictedCo = addCategoryOption("Restricted disaggregation", group);
    openOption = addCategoryOption("Open attribute", null);
    restrictedOption = addCategoryOption("Restricted attribute", group);
    String disaggregation =
        addCategoryCombo("Disaggregation", "DISAGGREGATION", openCo, restrictedCo);
    attributeCombo = addCategoryCombo("Attribute", "ATTRIBUTE", openOption, restrictedOption);
    openCoc = optionCombo(disaggregation, "Open disaggregation");
    restrictedCoc = optionCombo(disaggregation, "Restricted disaggregation");
    defaultCoc =
        GET("/categoryOptionCombos/gist?fields=id&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getString(0)
            .string();

    String defaultCc =
        GET("/categoryCombos/gist?fields=id&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getString(0)
            .string();
    disaggDe = addDataElement("disaggregated", "DE1", ValueType.INTEGER, null, disaggregation);
    attributeDe = addDataElement("attributed", "DE2", ValueType.INTEGER, null, defaultCc);
    addDataSet("Disaggregated", defaultCc, disaggDe);
    addDataSet("Attributed", attributeCombo, attributeDe);

    for (int unit = 0; unit < units.size(); unit++)
      for (int kind : List.of(OPEN_COC, RESTRICTED_COC, OPEN_AOC, RESTRICTED_AOC))
        enterTwice(unit, kind);
  }

  @Test
  @DisplayName("Several capture roots: only values in their hierarchies")
  void testMultipleRoots() {
    assertUserSees(Set.of("C1", "C3"), Set.of("C1", "C3"), false, Set.of("C1", "C3"));
  }

  @Test
  @DisplayName("Capture and data view roots both grant read, the union of their hierarchies")
  void testCaptureAndDataViewRoots() {
    assertUserSees(Set.of("D2"), Set.of("B2"), false, Set.of("D2", "B2"));
  }

  @Test
  @DisplayName("Data view roots only")
  void testDataViewRootsOnly() {
    assertUserSees(Set.of(), Set.of("C3"), false, Set.of("C3"));
  }

  @Test
  @DisplayName("A root at the deepest level reads only its own values")
  void testDeepRoot() {
    assertUserSees(Set.of("D4"), Set.of(), false, Set.of("D4"));
  }

  @Test
  @DisplayName("Roots below another root of the user add nothing")
  void testOverlappingRoots() {
    assertUserSees(Set.of("B1", "C1", "D1"), Set.of("D3"), false, Set.of("B1", "D3"));
  }

  @Test
  @DisplayName("A top-level root does not grant read of another top-level hierarchy")
  void testNationalRoot() {
    assertUserSees(Set.of("A"), Set.of("A"), false, Set.of("A"));
  }

  @Test
  @DisplayName("Roots in both top-level hierarchies read all units")
  void testAllTopLevelRoots() {
    assertUserSees(Set.of("A", "Z"), Set.of(), false, Set.of("A", "Z"));
  }

  @Test
  @DisplayName("A member of the group shared the restricted options reads restricted combos")
  void testRestrictedCombosForGroupMember() {
    assertUserSees(Set.of("B1", "Z1"), Set.of(), true, Set.of("B1", "Z1"));
  }

  @Test
  @DisplayName("Pinned option combos: restricted combos are not listed nor counted")
  void testPinnedCombos() {
    switchToNewUser(user(Set.of("A"), Set.of(), false));
    String restrictedAoc = attributeOptionCombo(restrictedOption);
    String openAoc = attributeOptionCombo(openOption);
    for (int max : MAX_ORG_UNIT_IDS)
      for (boolean is14 : POSTGRES_14) {
        store.setMaxOrgUnitIds(max);
        store.setPostgres14(is14);
        String mode = mode(max, is14);
        assertEquals(0, total(format("&co=%s&cc=%s", restrictedCoc, defaultCoc)), mode);
        assertEquals(0, total(format("&co=%s&cc=%s", defaultCoc, restrictedAoc)), mode);
        assertEquals(Set.of(), visible(format("&co=%s&cc=%s", defaultCoc, restrictedAoc)), mode);
        Set<String> open = visible(format("&co=%s&cc=%s", defaultCoc, openAoc));
        assertEquals(expected(Set.of("A"), false, OPEN_AOC), open, mode);
        assertEquals(
            expected(Set.of("A"), false, OPEN_COC),
            visible(format("&co=%s&cc=%s", openCoc, defaultCoc)),
            mode);
      }
  }

  @Test
  @DisplayName("A superuser reads all values")
  void testSuperuser() {
    assertEquals(expected(Set.of("A", "Z"), true), visible(""));
    assertEquals(rows("/audits/dataValue?fields=value&skipPaging=true"), total(""));
  }

  @Test
  @DisplayName("Pages of a user who is not a superuser end at entry 10,000, a superuser's do not")
  void testPageDepthLimit() {
    switchToNewUser(user(Set.of("A"), Set.of(), false));
    assertStatus(HttpStatus.OK, GET("/audits/dataValue?fields=value&page=200&pageSize=50"));
    assertStatus(HttpStatus.OK, GET("/audits/dataValue?fields=value&page=10&pageSize=1000"));
    JsonObject error =
        GET("/audits/dataValue?fields=value&page=201&pageSize=50").content(HttpStatus.CONFLICT);
    assertEquals("E2052", error.getString("errorCode").string());

    switchToSuperuser();
    assertStatus(HttpStatus.OK, GET("/audits/dataValue?fields=value&page=201&pageSize=50"));
  }

  @Test
  @DisplayName("The pager has a total only with totalPages=true and links the pages next to it")
  void testPagerTotals() {
    switchToNewUser(user(Set.of("A"), Set.of(), false));
    int rows = rows("/audits/dataValue?fields=value&skipPaging=true");
    assertTrue(rows > 4, "rows " + rows);
    int last = (rows + 1) / 2;

    JsonObject first = pager("&pageSize=2", 2);
    assertEquals(1, first.getNumber("page").intValue());
    assertEquals(2, first.getNumber("pageSize").intValue());
    assertHasNoMember(first, "total", "pageCount", "prevPage");
    assertPagerLink(first.getString("nextPage").string(), 2, 2, PAGER_LINK);

    JsonObject middle = pager("&page=2&pageSize=2", 2);
    assertEquals(2, middle.getNumber("page").intValue());
    assertHasNoMember(middle, "total", "pageCount");
    assertPagerLink(middle.getString("prevPage").string(), 1, 2, PAGER_LINK);
    assertPagerLink(middle.getString("nextPage").string(), 3, 2, PAGER_LINK);

    JsonObject lastPage = pager("&page=" + last + "&pageSize=2", rows - 2 * (last - 1));
    assertHasNoMember(lastPage, "total", "pageCount", "nextPage");
    assertPagerLink(lastPage.getString("prevPage").string(), last - 1, 2, PAGER_LINK);

    // a full last page: the extra entry read decides, not the page size
    assertHasNoMember(pager("&pageSize=" + rows, rows), "total", "pageCount", "nextPage");
    assertHasMember(pager("&pageSize=" + (rows - 1), rows - 1), "nextPage");

    JsonObject totals = pager("&page=2&pageSize=2&totalPages=true", 2);
    assertEquals(rows, totals.getNumber("total").intValue());
    assertEquals(last, totals.getNumber("pageCount").intValue());
    assertPagerLink(totals.getString("prevPage").string(), 1, 2, PAGER_LINK);
    assertPagerLink(totals.getString("nextPage").string(), 3, 2, PAGER_LINK);
    // with totals a page beyond the last reports the last page, and links from there
    JsonObject beyond = pager("&page=" + (last + 1) + "&pageSize=2&totalPages=true", 0);
    assertEquals(last, beyond.getNumber("page").intValue());
    assertHasNoMember(beyond, "nextPage");
    assertPagerLink(beyond.getString("prevPage").string(), last - 1, 2, PAGER_LINK);

    switchToSuperuser();
    assertHasNoMember(pager("&pageSize=2", 2), "total", "pageCount", "prevPage");
  }

  /** The pager of a page of the change log, checking the number of entries on the page. */
  private JsonObject pager(String params, int entries) {
    JsonObject page = GET("/audits/dataValue?fields=value" + params).content(HttpStatus.OK);
    assertEquals(entries, page.getArray("dataValueAudits").size(), params);
    return page.getObject("pager");
  }

  private void assertUserSees(
      Set<String> capture, Set<String> dataView, boolean groupMember, Set<String> roots) {
    switchToNewUser(user(capture, dataView, groupMember));
    Set<String> expected = expected(roots, groupMember);
    // org units listed by ID (few) and found inside the query (many), each as on PostgreSQL 14
    // and before
    for (int max : MAX_ORG_UNIT_IDS)
      for (boolean is14 : POSTGRES_14) {
        store.setMaxOrgUnitIds(max);
        store.setPostgres14(is14);
        String mode = mode(max, is14);
        assertEquals(expected, visible(""), mode);
        assertEquals(expected, values("/audits/dataValue?fields=value&skipPaging=true"), mode);
        assertEquals(expected, values("/audits/dataValue?fields=value&paging=false"), mode);
        assertEquals(rows("/audits/dataValue?fields=value&skipPaging=true"), total(""), mode);
        for (String code : units.keySet()) {
          Set<String> inUnit = new HashSet<>();
          expected.stream().filter(label -> label.startsWith(code + ":")).forEach(inUnit::add);
          assertEquals(inUnit, visible("&ou=" + units.get(code)), mode + " " + code);
        }
      }
    for (boolean is14 : POSTGRES_14) {
      store.setPostgres14(is14);
      int i = 0;
      for (Map.Entry<String, String> unit : units.entrySet()) {
        int index = i++;
        for (int kind : List.of(OPEN_COC, RESTRICTED_COC, OPEN_AOC, RESTRICTED_AOC)) {
          String label = label(unit.getKey(), kind);
          String message = label + (is14 ? ", PG 14" : ", before PG 14");
          HttpStatus status = GET(contextUrl(index, kind)).status();
          // 2.41 refuses an org unit outside the capture hierarchy (E2020) before any access check
          if (!isInHierarchy(unit.getKey(), capture))
            assertEquals(HttpStatus.CONFLICT, status, message);
          else
            assertEquals(
                expected.contains(label), status != HttpStatus.FORBIDDEN, message + " " + status);
        }
      }
    }
  }

  private static String mode(int maxOrgUnitIds, boolean postgres14) {
    return "max " + maxOrgUnitIds + (postgres14 ? ", PG 14" : ", before PG 14");
  }

  /** Change log rows the user should read: both values of each readable value of the units. */
  private Set<String> expected(Set<String> roots, boolean groupMember, int... kinds) {
    List<Integer> allowed =
        kinds.length > 0
            ? Stream.of(kinds[0]).toList()
            : groupMember
                ? List.of(OPEN_COC, RESTRICTED_COC, OPEN_AOC, RESTRICTED_AOC)
                : List.of(OPEN_COC, OPEN_AOC);
    Set<String> labels = new HashSet<>();
    for (String code : units.keySet())
      if (isInHierarchy(code, roots)) for (int kind : allowed) labels.add(label(code, kind));
    return labels;
  }

  private boolean isInHierarchy(String code, Set<String> roots) {
    for (String c = code; c != null; c = parents.get(c)) if (roots.contains(c)) return true;
    return false;
  }

  private static String label(String code, int kind) {
    return code + ":" + kind;
  }

  private Set<String> visible(String filters) {
    return values("/audits/dataValue?fields=value&pageSize=1000" + filters);
  }

  private int total(String filters) {
    return GET("/audits/dataValue?fields=value&totalPages=true" + filters)
        .content(HttpStatus.OK)
        .getObject("pager")
        .getNumber("total")
        .intValue();
  }

  /** The units and kinds of the change log rows, decoded from their values. */
  private Set<String> values(String url) {
    List<String> codes = new ArrayList<>(units.keySet());
    Set<String> labels = new HashSet<>();
    GET(url).content(HttpStatus.OK).getArray("dataValueAudits").asList(JsonObject.class).stream()
        .map(audit -> Integer.parseInt(audit.getString("value").string()))
        .forEach(value -> labels.add(label(codes.get(value / 100), value / 10 % 10)));
    return labels;
  }

  private int rows(String url) {
    return GET(url).content(HttpStatus.OK).getArray("dataValueAudits").size();
  }

  private User user(Set<String> capture, Set<String> dataView, boolean groupMember) {
    User user = createAndAddUser(orgUnits(capture), orgUnits(dataView));
    if (groupMember)
      assertStatus(HttpStatus.OK, POST("/userGroups/{uid}/users/{itemId}", group, user.getUid()));
    dbmsManager.flushSession();
    dbmsManager.clearSession();
    return userService.getUser(user.getUid());
  }

  private Set<OrganisationUnit> orgUnits(Set<String> codes) {
    Set<OrganisationUnit> orgUnits = new HashSet<>();
    for (String code : codes) orgUnits.add(manager.get(OrganisationUnit.class, units.get(code)));
    return orgUnits;
  }

  private String contextUrl(int unit, int kind) {
    String ou = new ArrayList<>(units.values()).get(unit);
    return switch (kind) {
      case OPEN_COC, RESTRICTED_COC ->
          format(
              "/dataEntry/dataValueContext?de=%s&pe=%s&ou=%s&co=%s",
              disaggDe, PE, ou, kind == OPEN_COC ? openCoc : restrictedCoc);
      default ->
          format(
              "/dataEntry/dataValueContext?de=%s&pe=%s&ou=%s&co=%s&cc=%s&cp=%s",
              attributeDe,
              PE,
              ou,
              defaultCoc,
              attributeCombo,
              kind == OPEN_AOC ? openOption : restrictedOption);
    };
  }

  private void enterTwice(int unit, int kind) {
    String context = contextUrl(unit, kind);
    for (int n = 1; n <= 2; n++) {
      int value = unit * 100 + kind * 10 + n;
      String url =
          context.replace("/dataEntry/dataValueContext?", "/dataValues?") + "&value=" + value;
      assertStatus(HttpStatus.CREATED, POST(url));
      // like a new request, the next write loads the stored value, which is what logs a change
      dbmsManager.flushSession();
      dbmsManager.clearSession();
    }
  }

  private void addUnit(String code, String parent) {
    String parentRef = parent == null ? "" : format(",'parent':{'id':'%s'}", units.get(parent));
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits",
                format(
                    "{'name':'%1$s','shortName':'%1$s','code':'%1$s','openingDate':'2021-01-01'%2$s}",
                    code, parentRef)));
    units.put(code, uid);
    parents.put(code, parent);
  }

  private String addCategoryOption(String name, String readGroup) {
    String groups =
        readGroup == null
            ? ""
            : format(",'userGroups':{'%1$s':{'id':'%1$s','access':'rwr-----'}}", readGroup);
    String publicAccess = readGroup == null ? "rwrw----" : "rw------";
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryOptions",
            format(
                "{'name':'%s','shortName':'%s','sharing':{'public':'%s'%s}}",
                name, name, publicAccess, groups)));
  }

  private String addCategoryCombo(String name, String dimensionType, String... options) {
    String category =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                format(
                    "{'name':'%s','shortName':'%s','dataDimensionType':'%s','categoryOptions':[%s]}",
                    name,
                    name,
                    dimensionType,
                    String.join(",", Stream.of(options).map("{'id':'%s'}"::formatted).toList()))));
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryCombos",
            format(
                "{'name':'%s','dataDimensionType':'%s','categories':[{'id':'%s'}]}",
                name, dimensionType, category)));
  }

  private String optionCombo(String categoryCombo, String name) {
    return GET("/categoryCombos/{id}?fields=categoryOptionCombos[id,name]", categoryCombo)
        .content()
        .getArray("categoryOptionCombos")
        .asList(JsonObject.class)
        .stream()
        .filter(coc -> coc.getString("name").string().equals(name))
        .map(coc -> coc.getString("id").string())
        .findFirst()
        .orElseThrow();
  }

  private String attributeOptionCombo(String option) {
    return optionCombo(
        attributeCombo,
        GET("/categoryOptions/{id}?fields=name", option).content().getString("name").string());
  }

  private void addDataSet(String name, String categoryCombo, String dataElement) {
    String orgUnits =
        String.join(",", units.values().stream().map("{'id':'%s'}"::formatted).toList());
    assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataSets",
            format(
                "{'name':'%s','shortName':'%s','periodType':'Monthly',"
                    + "'categoryCombo':{'id':'%s'},'sharing':{'public':'rwr-----'},"
                    + "'dataSetElements':[{'dataElement':{'id':'%s'}}],'organisationUnits':[%s]}",
                name, name, categoryCombo, dataElement, orgUnits)));
  }

  // 2.41's tracker JsonAssertions has no varargs assertHasNoMember and no assertPagerLink: copies
  // of
  // the 2.42 ones

  private static void assertHasNoMember(JsonObject json, String... names) {
    assertAll(
        String.format("Unexpected member(s) in %s", json),
        Arrays.stream(names)
            .map(
                name ->
                    () ->
                        assertFalse(
                            json.has(name), String.format("member \"%s\" unexpected ", name))));
  }

  private static void assertHasMember(JsonObject json, String name) {
    assertTrue(json.has(name), String.format("member \"%s\" should be in %s", name, json));
  }

  private static void assertPagerLink(String actual, int page, int pageSize, String start) {
    assertNotNull(actual, "expected a link to a prev/nextPage");
    assertAll(
        "asserting link to a prev/nextPage",
        () -> assertTrue(actual.startsWith(start), actual),
        () -> assertTrue(actual.contains("page=" + page), actual),
        () -> assertTrue(actual.contains("pageSize=" + pageSize), actual));
  }
}
