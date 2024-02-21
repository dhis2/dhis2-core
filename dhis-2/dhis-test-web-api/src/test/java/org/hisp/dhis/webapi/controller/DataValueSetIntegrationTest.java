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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.web.WebClient.Accept;
import static org.hisp.dhis.web.WebClient.ContentType;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML_ADX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_XML;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.hisp.dhis.webapi.json.domain.JsonDataValue;
import org.hisp.dhis.webapi.json.domain.JsonDataValueSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Test for data elements which have been abandoned. This is taken to mean that there is no data
 * recorded against them, and they have not been updated in the last hundred days.
 *
 * <p>{@see
 * dhis-2/dhis-services/dhis-service-administration/src/main/resources/data-integrity-checks/data_elements/aggregate_des_abandoned.yaml
 * }
 *
 * @author Jason P. Pickering
 */
class DataValueSetIntegrationTest extends DhisControllerIntegrationTest {
  private static final String check = "data_elements_aggregate_abandoned";

  private static final String detailsIdType = "dataElements";

  private String orgUnitId;
  private String dataElementA;

  private String dataSetA;

  private String defaultCatCombo;

  private String defaultCatOptionCombo;

  private static final String period = "202212";

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired protected TransactionTemplate transactionTemplate;

  @Test
  void testNormalUserCanUpdateDataValue() {

    setUpTest();

    //We should still be acting as the superuser now



/*
  //Swtich to a user who cannot attribute data
    User dataEntryUser = createDataEntryUser(false);
    switchContextToUser(dataEntryUser);
    currentUser = dataEntryUser;
    assertStatus(
        HttpStatus.CREATED,
        postNewDataValue(period, "20", "Data Update", false, dataElementA, orgUnitId));
    storedValue = getDataValue(dataElementA, period, orgUnitId);
    assertEquals("\"20\"", storedValue);
    JsonDataValueSet updatedDataValueSet = getDataValueSet(dataSetA, period, orgUnitId);
    assertEquals(1, updatedDataValueSet.getDataValues().size());
    JsonDataValue updatedDataValue = updatedDataValueSet.getDataValues().get(0);
    assertEquals(dataElementA, updatedDataValue.getDataElement());
    assertEquals(period, updatedDataValueSet.getPeriod());
    assertEquals(orgUnitId, updatedDataValue.getOrgUnit());
    assertEquals("20", updatedDataValue.getValue());
    assertEquals("Data Update", updatedDataValue.getComment());
    assertEquals(dataEntryUser.getUsername(), updatedDataValue.getStoredBy());
    assertNull(updatedDataValue.getFollowUp());
    assertEquals(defaultCatOptionCombo, updatedDataValue.getCategoryOptionCombo());
    assertEquals(dataValue.getCreated().date(), updatedDataValue.getCreated().date());
    assertEquals(
        true, updatedDataValue.getLastUpdated().date().isAfter(dataValue.getLastUpdated().date()));
*/



    transactionTemplate.execute(
        status -> {
          assertStatus(
              HttpStatus.CREATED,
              postNewDataValue(period, "10", "Test Data", false, dataElementA, orgUnitId));

          LocalDateTime now = LocalDateTime.now();
          String storedValue = getDataValue(dataElementA, period, orgUnitId);
          assertEquals("\"10\"", storedValue);
          JsonDataValueSet dataValueSet = getDataValueSet(dataSetA, period, orgUnitId);
          assertEquals(1, dataValueSet.getDataValues().size());
          JsonDataValue dataValue = dataValueSet.getDataValues().get(0);
          assertEquals(dataElementA, dataValue.getDataElement());
          assertEquals(period, dataValue.getPeriod());
          assertEquals(orgUnitId, dataValue.getOrgUnit());
          assertEquals("10", dataValue.getValue());
          assertEquals("Test Data", dataValue.getComment());
          assertEquals(superUser.getUsername(), dataValue.getStoredBy());
          assertNull(dataValue.getFollowUp());
          assertEquals(defaultCatOptionCombo, dataValue.getCategoryOptionCombo());
          assertEquals(true, Duration.between(now, dataValue.getCreated().date()).toMillis() < 1000);
          assertEquals(true, Duration.between(now, dataValue.getLastUpdated().date()).toMillis() < 1000);

          //language=JSON
          String json = """
        { "dataValues": [
          { "dataElement": "%s",
           "period": "%s", 
           "orgUnit": "%s", 
           "value": "30",
           "comment": "DVS Update",
           "categoryOptionCombo": "%s" }
             ]}""".formatted(dataElementA, period, orgUnitId, defaultCatOptionCombo);

          assertEquals( HttpStatus.OK, POST("/dataValueSets", json).status());
          return null;
        });

  }

  @Test
  void testPostAdxDataValueSet()
  {
      setUpTest();
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit(orgUnitId);
            Set<OrganisationUnit> userOrgUnits= new HashSet<>(Arrays.asList(organisationUnit));
            User user = makeUser( "X", List.of( Authorities.F_DATAVALUE_ADD.name(), Authorities.F_EXPORT_DATA.name() ), userOrgUnits );
          userService.addUser( user );
          injectSecurityContextUser( user );
            String json = """
        { "dataValues": [
          { "dataElement": "%s",
           "period": "%s", 
           "orgUnit": "%s", 
           "value": "30",
           "comment": "DVS Update",
           "categoryOptionCombo": "%s" }
             ]}""".formatted(dataElementA, period, orgUnitId, defaultCatOptionCombo);
          HttpResponse response = POST("/38/dataValueSets/", json);
          assertEquals(HttpStatus.OK, response.status());

  }

  protected final HttpResponse postNewDataValue(
      String period,
      String value,
      String comment,
      boolean followup,
      String dataElementId,
      String orgUnitId) {
    String defaultCOC = categoryService.getDefaultCategoryOptionCombo().getUid();
    return POST(
        "/dataValues?de={de}&pe={pe}&ou={ou}&co={coc}&value={val}&comment={comment}&followUp={followup}",
        dataElementId,
        period,
        orgUnitId,
        defaultCOC,
        value,
        comment,
        followup);
  }

  protected final String getDataValue(String dataElementId, String period, String orgUnitId) {
    return GET("/dataValues?de={de}&pe={pe}&ou={ou}", dataElementId, period, orgUnitId)
        .content()
        .as(JsonArray.class)
        .get(0)
        .toString();
  }

  protected final JsonDataValueSet getDataValueSet(
      String dataSetId, String period, String orgUnitId) {
    return GET(
            "/dataValueSets?dataSet={dataSetId}&period={period}&orgUnit={orgUnit}",
            dataSetId,
            period,
            orgUnitId)
        .content()
        .as(JsonDataValueSet.class);
  }

  protected final String getDefaultCatCombo() {
    JsonObject ccDefault =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0);
    return ccDefault.getString("id").string();
  }

  protected final String getDefaultCOC() {
    JsonObject ccDefault =
        GET("/categoryCombos/gist?fields=id,categoryOptionCombos::ids&pageSize=1&headless=true&filter=name:eq:default")
            .content()
            .getObject(0);
    return ccDefault.getArray("categoryOptionCombos").getString(0).string();
  }

  protected final User createDataEntryUser(boolean canAddAttributeData) {

          UserRole dataEntryRole = new UserRole();
          dataEntryRole.setName("Data Entry");
          if (!canAddAttributeData) {
            dataEntryRole.setAuthorities(
                new HashSet<>(
                    Arrays.asList(Authorities.F_DATAVALUE_ADD.name(), Authorities.F_EXPORT_DATA.name())));
          } else {
            dataEntryRole.setAuthorities(
                new HashSet<>(
                    Arrays.asList(
                        Authorities.F_DATAVALUE_ADD.name(),
                        Authorities.F_EXPORT_DATA.name(),
                        Authorities.F_DATAVALUE_ATTRIBUTE.name())));
          }
          dataEntryRole.setAuthorities(
              new HashSet<>(
                  Arrays.asList(Authorities.F_DATAVALUE_ADD.name(), Authorities.F_EXPORT_DATA.name())));
          manager.save(dataEntryRole);

          User user = new User();
          user.setUid(CodeGenerator.generateUid());
          user.setFirstName("Data");
          user.setSurname("Entry");
          user.setUsername("dataentry");
          user.setPassword(DEFAULT_ADMIN_PASSWORD);
          user.getUserRoles().add(dataEntryRole);
          user.setLastUpdated(new Date());
          user.setCreated(new Date());
          OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit(orgUnitId);
          user.setOrganisationUnits(new HashSet<>(Arrays.asList(organisationUnit)));
          manager.persist(user);
          return user;

  }

  void setUpTest() {

    defaultCatCombo = getDefaultCatCombo();
    defaultCatOptionCombo = getDefaultCOC();
    dataSetA = generateUid();

    dataElementA =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'ANC2', 'shortName': 'ANC2', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));

    orgUnitId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    // add OU to users hierarchy
    assertStatus(
        HttpStatus.OK,
        POST(
            "/users/{id}/organisationUnits",
            getCurrentUser().getUid(),
            WebClient.Body("{'additions':[{'id':'" + orgUnitId + "'}]}")));

    String datasetMetadata =
        "{ 'id':'"
            + dataSetA
            + "', 'name': 'Test Monthly', 'shortName': 'Test Monthly', 'periodType' : 'Monthly',"
            + "'sharing' : {'public':'rwrw----'},"
            + "'categoryCombo' : {'id': '"
            + defaultCatCombo
            + "'}, "
            + "'dataSetElements' : [{'dataSet' : {'id':'"
            + dataSetA
            + "'}, 'id':'"
            + generateUid()
            + "', 'dataElement': {'id' : '"
            + dataElementA
            + "'}}]}";
    assertStatus(HttpStatus.CREATED, POST("/dataSets", datasetMetadata));

  }
}
