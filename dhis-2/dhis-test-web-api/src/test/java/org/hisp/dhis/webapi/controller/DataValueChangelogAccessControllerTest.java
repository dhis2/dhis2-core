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

import static java.lang.String.format;
import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests that the aggregate data value change log ({@code /api/audits/dataValue}) and the data value
 * context ({@code /api/dataEntry/dataValueContext}) only reveal values the current user may read.
 * Each value is entered twice so that every line records at least one change log row for it. The
 * value pairs tell the rows apart.
 */
@Transactional
class DataValueChangelogAccessControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String PE = "2021-01";

  @Autowired private JdbcTemplate jdbcTemplate;

  private String ou;
  private String otherOu;
  private String coc;
  private String readableDe;
  private String metadataOnlyDe;
  private String restrictedCocDe;
  private String restrictedCoc;
  private String restrictedAocDe;
  private String attributeCombo;
  private String restrictedOption;
  private String orphanDe;
  private String metadataOnlyDs;
  private String optionNoDataRead;

  @BeforeEach
  void setUp() {
    JsonObject ccDefault =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0);
    String cc = ccDefault.getString("id").string();
    coc = ccDefault.getArray("categoryOptionCombos").getString(0).string();
    ou = addOrgUnit("OU1");
    otherOu = addOrgUnit("OU2");
    assertStatus(
        HttpStatus.OK,
        POST(
            "/users/{id}/organisationUnits",
            getCurrentUser().getUid(),
            Body(format("{'additions':[{'id':'%s'},{'id':'%s'}]}", ou, otherOu))));

    optionNoDataRead = addCategoryOption("No data read");
    restrictedOption = addCategoryOption("Restricted attribute");
    String disaggregation =
        addCategoryCombo("Restricted disaggregation", "DISAGGREGATION", optionNoDataRead);
    restrictedCoc = firstOptionCombo(disaggregation);
    attributeCombo = addCategoryCombo("Restricted attribute", "ATTRIBUTE", restrictedOption);

    readableDe = addDataElement("readable", "DE1", ValueType.INTEGER, null, cc);
    metadataOnlyDe = addDataElement("metadata only", "DE2", ValueType.INTEGER, null, cc);
    restrictedCocDe =
        addDataElement("restricted coc", "DE3", ValueType.INTEGER, null, disaggregation);
    restrictedAocDe = addDataElement("restricted aoc", "DE4", ValueType.INTEGER, null, cc);
    orphanDe = addDataElement("orphan", "DE5", ValueType.INTEGER, null, cc);

    addDataSetWithSharing("Readable", cc, "rwr-----", readableDe, restrictedCocDe);
    metadataOnlyDs = addDataSetWithSharing("Metadata only", cc, "rw------", metadataOnlyDe);
    addDataSetWithSharing("Restricted attribute", attributeCombo, "rwr-----", restrictedAocDe);
    String orphanDs = addDataSetWithSharing("Orphan", cc, "rwr-----", orphanDe);

    enterTwice(readableDe, ou, coc, null, "11", "12");
    enterTwice(readableDe, otherOu, coc, null, "21", "22");
    enterTwice(metadataOnlyDe, ou, coc, null, "31", "32");
    enterTwice(restrictedCocDe, ou, restrictedCoc, null, "41", "42");
    enterTwice(restrictedAocDe, ou, coc, restrictedOption, "51", "52");
    enterTwice(orphanDe, ou, coc, null, "61", "62");
    // the data element of the orphan values belongs to no data set any longer
    assertStatus(HttpStatus.OK, DELETE("/dataSets/" + orphanDs));
  }

  @Test
  @DisplayName("A data reader only sees change log rows of values they may read")
  void testChangelog_readerSeesOnlyReadableValues() {
    Set<String> all = getChangelogValues("");
    assertTrue(all.containsAll(Set.of("21", "31", "41", "51", "61")), all.toString());

    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, ou);
    User captureReader = createAndAddUser("captureReader", orgUnit);
    User dataViewReader = createAndAddUser(new HashSet<>(), new HashSet<>(Set.of(orgUnit)));
    for (User reader : List.of(captureReader, dataViewReader)) {
      switchToNewUser(reader);
      Set<String> visible = getChangelogValues("");
      assertTrue(visible.contains("11"), visible.toString());
      assertTrue(Set.of("11", "12").containsAll(visible), visible.toString());
      assertEquals(Set.of(), getChangelogValues("&de=" + metadataOnlyDe));
      assertEquals(Set.of(), getChangelogValues("&ou=" + otherOu));
      assertEquals(Set.of(), getChangelogValues("&de=" + orphanDe));
      assertEquals(Set.of(), getChangelogValues("&co=" + restrictedCoc));
      assertEquals(visible, getChangelogValues("&de=" + readableDe));
    }
  }

  @Test
  @DisplayName("Paging totals, skipPaging and paging=false only count readable values")
  void testChangelog_countsAndPagingModes() {
    int adminTotal = getChangelogTotal("");
    switchToNewUser(createAndAddUser("reader", manager.get(OrganisationUnit.class, ou)));
    int readerTotal = getChangelogTotal("");
    Set<String> skipPaging = getValues("/audits/dataValue?fields=value&skipPaging=true");
    Set<String> noPaging = getValues("/audits/dataValue?fields=value&paging=false");
    Set<String> paged = getValues("/audits/dataValue?fields=value&pageSize=1");

    assertTrue(readerTotal > 0 && readerTotal < adminTotal, readerTotal + " of " + adminTotal);
    assertTrue(Set.of("11", "12").containsAll(skipPaging), skipPaging.toString());
    assertEquals(skipPaging, noPaging);
    assertEquals(1, paged.size());
    assertEquals(0, getChangelogTotal("&de=" + metadataOnlyDe));
  }

  @Test
  @DisplayName("Users outside the org unit hierarchy and without data read see no change log")
  void testChangelog_outsiderSeesNothing() {
    OrganisationUnit unrelated = manager.get(OrganisationUnit.class, addOrgUnit("OU3"));
    switchToNewUser(createAndAddUser("outsider", unrelated));
    assertEquals(Set.of(), getChangelogValues(""));
    assertEquals(0, getChangelogTotal(""));
    assertEquals(Set.of(), getChangelogValues("&de=" + readableDe + "&ou=" + ou));
  }

  @Test
  @DisplayName("Filtering by a data set the user may not read data of is refused")
  void testChangelog_unreadableDataSetFilter() {
    switchToNewUser(createAndAddUser("reader", manager.get(OrganisationUnit.class, ou)));
    JsonObject error =
        GET("/audits/dataValue?ds={ds}", metadataOnlyDs).content(HttpStatus.CONFLICT);
    assertEquals("E2010", error.getString("errorCode").string());

    switchToAdminUser();
    assertTrue(getChangelogValues("&ds=" + metadataOnlyDs).contains("31"));
  }

  @Test
  @DisplayName("A user sharing entry without access string grants data read, as AclService does")
  void testChangelog_userSharingEntryWithoutAccessString() {
    User reader = createAndAddUser("reader", manager.get(OrganisationUnit.class, ou));
    setSharing(
        "dataset",
        metadataOnlyDs,
        format("{'public':'rw------','users':{'%1$s':{'id':'%1$s'}}}", reader.getUid()));

    switchToNewUser(reader);
    assertTrue(getChangelogValues("").contains("31"));
    assertTrue(getChangelogValues("&ds=" + metadataOnlyDs).contains("31"));
    assertStatus(
        HttpStatus.OK,
        GET(
            "/dataEntry/dataValueContext?de={de}&pe={pe}&ou={ou}&co={co}",
            metadataOnlyDe,
            PE,
            ou,
            coc));
  }

  @Test
  @DisplayName("A category option whose sharing has no user and group entries is not readable")
  void testChangelog_categoryOptionSharingWithoutEntries() {
    setSharing("categoryoption", optionNoDataRead, "{'public':'rw------'}");
    setSharing("categoryoption", restrictedOption, "{'public':'rw------'}");

    switchToNewUser(createAndAddUser("reader", manager.get(OrganisationUnit.class, ou)));
    Set<String> visible = getChangelogValues("");
    assertTrue(visible.contains("11"), visible.toString());
    assertTrue(Set.of("11", "12").containsAll(visible), visible.toString());
    assertContextForbidden(restrictedCocDe, ou, restrictedCoc, null);
    assertContextForbidden(restrictedAocDe, ou, coc, restrictedOption);
  }

  @Test
  @DisplayName("Owning a data set without data sharing does not grant data read")
  void testChangelog_ownerOfDataSetWithoutDataSharing() {
    User reader = createAndAddUser("reader", manager.get(OrganisationUnit.class, ou));
    setSharingAsStored(
        "dataset",
        metadataOnlyDs,
        format("{'owner':'%s','public':'rw------','users':{},'userGroups':{}}", reader.getUid()));

    switchToNewUser(reader);
    Set<String> visible = getChangelogValues("");
    assertTrue(visible.contains("11"), visible.toString());
    assertTrue(Set.of("11", "12").containsAll(visible), visible.toString());
    assertEquals(
        "E2010",
        GET("/audits/dataValue?ds={ds}", metadataOnlyDs)
            .content(HttpStatus.CONFLICT)
            .getString("errorCode")
            .string());
    assertContextForbidden(metadataOnlyDe, ou, coc, null);
  }

  @Test
  @DisplayName("An object without owner and without data sharing does not grant data read")
  void testChangelog_objectWithoutOwner() {
    setSharingAsStored("dataset", metadataOnlyDs, "{'public':'rw------','users':{}}");
    setSharingAsStored("categoryoption", optionNoDataRead, "{'public':'rw------','users':{}}");

    switchToNewUser(createAndAddUser("reader", manager.get(OrganisationUnit.class, ou)));
    Set<String> visible = getChangelogValues("");
    assertTrue(visible.contains("11"), visible.toString());
    assertTrue(Set.of("11", "12").containsAll(visible), visible.toString());
    assertContextForbidden(metadataOnlyDe, ou, coc, null);
    assertContextForbidden(restrictedCocDe, ou, restrictedCoc, null);
  }

  @Test
  @DisplayName("A stored public access string \"null\" does not grant data read, as AclService")
  void testChangelog_publicAccessStringNull() {
    setSharing("dataset", metadataOnlyDs, "{'public':'null','users':{},'userGroups':{}}");

    switchToNewUser(createAndAddUser("reader", manager.get(OrganisationUnit.class, ou)));
    Set<String> visible = getChangelogValues("");
    assertTrue(visible.contains("11"), visible.toString());
    assertTrue(Set.of("11", "12").containsAll(visible), visible.toString());
    assertContextForbidden(metadataOnlyDe, ou, coc, null);
  }

  @Test
  @DisplayName("With both option combos given, only values of readable combos are listed")
  void testChangelog_bothCombosGiven() {
    String restrictedAoc = firstOptionCombo(attributeCombo);
    switchToNewUser(createAndAddUser("reader", manager.get(OrganisationUnit.class, ou)));

    assertEquals(Set.of(), getChangelogValues(format("&co=%s&cc=%s", restrictedCoc, coc)));
    assertEquals(0, getChangelogTotal(format("&co=%s&cc=%s", restrictedCoc, coc)));
    assertEquals(Set.of(), getChangelogValues(format("&co=%s&cc=%s", coc, restrictedAoc)));
    assertEquals(0, getChangelogTotal(format("&co=%s&cc=%s", coc, restrictedAoc)));
    Set<String> readable = getChangelogValues(format("&co=%s&cc=%s", coc, coc));
    assertTrue(readable.contains("11"), readable.toString());
    assertTrue(Set.of("11", "12").containsAll(readable), readable.toString());
    assertEquals(readable.size(), getChangelogTotal(format("&co=%s&cc=%s", coc, coc)));
  }

  @Test
  @DisplayName("The default option combo is readable also when the default option has no data read")
  void testChangelog_defaultCategoryOptionWithoutDataRead() {
    String defaultOption =
        jdbcTemplate.queryForObject(
            "SELECT uid FROM categoryoption WHERE name = 'default'", String.class);
    setSharing("categoryoption", defaultOption, "{'public':'rw------','users':{},'userGroups':{}}");

    switchToNewUser(createAndAddUser("reader", manager.get(OrganisationUnit.class, ou)));
    Set<String> visible = getChangelogValues("");
    assertTrue(visible.contains("11"), visible.toString());
    assertTrue(Set.of("11", "12").containsAll(visible), visible.toString());
    assertStatus(
        HttpStatus.OK,
        GET(
            "/dataEntry/dataValueContext?de={de}&pe={pe}&ou={ou}&co={co}",
            readableDe,
            PE,
            ou,
            coc));
  }

  @Test
  @DisplayName("Sharing migrated with JSON null entries works for user group members")
  void testChangelog_groupMemberWithMigratedSharing() {
    User reader = createAndAddUser("reader", manager.get(OrganisationUnit.class, ou));
    String group =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/userGroups",
                format("{'name':'Readers','users':[{'id':'%s'}]}", reader.getUid())));
    setSharing(
        "categoryoption", optionNoDataRead, "{'public':'rw------','users':null,'userGroups':null}");
    setSharing(
        "dataset",
        metadataOnlyDs,
        format(
            "{'public':'rw------','users':null,'userGroups':{'%1$s':{'id':'%1$s','access':'rwr-----'}}}",
            group));

    switchToNewUser(userService.getUser(reader.getUid()));
    Set<String> visible = getChangelogValues("");
    assertTrue(visible.containsAll(Set.of("11", "31")), visible.toString());
    assertTrue(Set.of("11", "12", "31", "32").containsAll(visible), visible.toString());
    assertContextForbidden(restrictedCocDe, ou, restrictedCoc, null);
  }

  /** Stores sharing as it may be found in the database, with an owner other than the user. */
  private void setSharing(String table, String uid, String sharing) {
    setSharingAsStored(
        table, uid, sharing.replaceFirst("\\{", "{'owner':'" + getAdminUser().getUid() + "',"));
  }

  /** Stores the given sharing as is, written with single quotes. */
  private void setSharingAsStored(String table, String uid, String sharing) {
    dbmsManager.flushSession();
    jdbcTemplate.update(
        "UPDATE " + table + " SET sharing = cast(? as jsonb) WHERE uid = ?",
        sharing.replace('\'', '"'),
        uid);
    dbmsManager.clearSession();
  }

  @Test
  @DisplayName("The data value context is only served for values the user may read")
  void testDataValueContext() {
    switchToNewUser(createAndAddUser("reader", manager.get(OrganisationUnit.class, ou)));
    JsonObject context =
        GET("/dataEntry/dataValueContext?de={de}&pe={pe}&ou={ou}&co={co}", readableDe, PE, ou, coc)
            .content(HttpStatus.OK);
    assertTrue(context.getArray("audits").size() > 0);
    assertEquals("12", context.getArray("history").getObject(0).getString("value").string());

    assertContextForbidden(metadataOnlyDe, ou, coc, null);
    assertContextForbidden(readableDe, otherOu, coc, null);
    assertContextForbidden(restrictedCocDe, ou, restrictedCoc, null);
    assertContextForbidden(restrictedAocDe, ou, coc, restrictedOption);
    assertContextForbidden(orphanDe, ou, coc, null);

    switchToAdminUser();
    assertStatus(
        HttpStatus.OK,
        GET("/dataEntry/dataValueContext?de={de}&pe={pe}&ou={ou}&co={co}", orphanDe, PE, ou, coc));
  }

  private void assertContextForbidden(String de, String orgUnit, String co, String option) {
    String url = format("/dataEntry/dataValueContext?de=%s&pe=%s&ou=%s&co=%s", de, PE, orgUnit, co);
    if (option != null) url += format("&cc=%s&cp=%s", attributeCombo, option);
    assertEquals(HttpStatus.FORBIDDEN, GET(url).status(), url);
  }

  private Set<String> getChangelogValues(String filters) {
    return getValues("/audits/dataValue?fields=value&pageSize=1000" + filters);
  }

  private int getChangelogTotal(String filters) {
    return GET("/audits/dataValue?fields=value&totalPages=true" + filters)
        .content(HttpStatus.OK)
        .getObject("pager")
        .getNumber("total")
        .intValue();
  }

  private Set<String> getValues(String url) {
    return Set.copyOf(
        GET(url)
            .content(HttpStatus.OK)
            .getArray("dataValueAudits")
            .asList(JsonObject.class)
            .stream()
            .map(audit -> audit.getString("value").string())
            .toList());
  }

  private void enterTwice(
      String de, String orgUnit, String co, String option, String first, String second) {
    for (String value : List.of(first, second)) {
      String url =
          format("/dataValues?de=%s&pe=%s&ou=%s&co=%s&value=%s", de, PE, orgUnit, co, value);
      if (option != null) url += format("&cc=%s&cp=%s", attributeCombo, option);
      assertStatus(HttpStatus.CREATED, POST(url));
      // like a new request, the next write loads the stored value, which is what logs a change
      dbmsManager.flushSession();
      dbmsManager.clearSession();
    }
  }

  private String addOrgUnit(String name) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/organisationUnits",
            format(
                "{'name':'%s','shortName':'%s','code':'%s','openingDate':'2021-01-01'}",
                name, name, name)));
  }

  private String addCategoryOption(String name) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryOptions",
            format("{'name':'%s','shortName':'%s','sharing':{'public':'rw------'}}", name, name)));
  }

  private String addCategoryCombo(String name, String dimensionType, String option) {
    String category =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                format(
                    "{'name':'%s','shortName':'%s','dataDimensionType':'%s',"
                        + "'categoryOptions':[{'id':'%s'}]}",
                    name, name, dimensionType, option)));
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryCombos",
            format(
                "{'name':'%s','dataDimensionType':'%s','categories':[{'id':'%s'}]}",
                name, dimensionType, category)));
  }

  private String firstOptionCombo(String categoryCombo) {
    return GET("/categoryCombos/{id}?fields=categoryOptionCombos[id]", categoryCombo)
        .content()
        .getArray("categoryOptionCombos")
        .getObject(0)
        .getString("id")
        .string();
  }

  private String addDataSetWithSharing(
      String name, String categoryCombo, String publicAccess, String... dataElements) {
    String elements =
        String.join(
            ",",
            List.of(dataElements).stream().map("{'dataElement':{'id':'%s'}}"::formatted).toList());
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/dataSets",
            format(
                "{'name':'%s','shortName':'%s','periodType':'Monthly',"
                    + "'categoryCombo':{'id':'%s'},'sharing':{'public':'%s'},"
                    + "'dataSetElements':[%s],'organisationUnits':[{'id':'%s'},{'id':'%s'}]}",
                name, name, categoryCombo, publicAccess, elements, ou, otherOu)));
  }
}
