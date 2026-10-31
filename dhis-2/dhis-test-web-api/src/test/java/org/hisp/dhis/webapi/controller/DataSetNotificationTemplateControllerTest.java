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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests the metadata import, export and schema of data set notification templates. */
@Transactional
class DataSetNotificationTemplateControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String TEMPLATE_UID = "DsNotTmpl01";

  /**
   * The properties of a template except translations, which the schema names "translation" and the
   * JSON names "translations".
   */
  private static final Set<String> PROPERTIES_WITHOUT_TRANSLATIONS =
      Set.of(
          "access",
          "attributeValues",
          "code",
          "created",
          "createdBy",
          "dataSetNotificationTrigger",
          "deliveryChannels",
          "displayMessageTemplate",
          "displayName",
          "displaySubjectTemplate",
          "href",
          "id",
          "lastUpdated",
          "lastUpdatedBy",
          "messageTemplate",
          "name",
          "notificationRecipient",
          "notifyParentOrganisationUnitOnly",
          "notifyUsersInHierarchyOnly",
          "recipientUserGroup",
          "relativeScheduledDays",
          "sendStrategy",
          "sharing",
          "subjectTemplate",
          "user");

  @Autowired private EntityManagerFactory entityManagerFactory;

  @BeforeEach
  void setUp() {
    importMetadata(
        """
        {'dataSets':[
          {'id':'DsNotDsA001','name':'Data set A','shortName':'DSA','periodType':'Monthly'},
          {'id':'DsNotDsB001','name':'Data set B','shortName':'DSB','periodType':'Monthly'}],
         'userGroups':[{'id':'DsNotUgA001','name':'Group A'}]}
        """);
  }

  @Test
  void shouldCreateAndUpdateTemplateViaMetadataImport() {
    importMetadata(template("Template A", "DsNotDsA001", "EMAIL"));

    JsonObject created = getTemplate();
    assertEquals("TEMPLATE_A", created.getString("code").string());
    assertEquals("Template A", created.getString("name").string());
    assertEquals("Subject V{data_name}", created.getString("subjectTemplate").string());
    assertEquals("Message V{data_name}", created.getString("messageTemplate").string());
    assertEquals(-2, created.getNumber("relativeScheduledDays").intValue());
    assertEquals("SCHEDULED_DAYS", created.getString("dataSetNotificationTrigger").string());
    assertEquals("USER_GROUP", created.getString("notificationRecipient").string());
    assertEquals("COLLECTIVE_SUMMARY", created.getString("sendStrategy").string());
    assertTrue(created.getBoolean("notifyUsersInHierarchyOnly").booleanValue());
    assertFalse(created.getBoolean("notifyParentOrganisationUnitOnly").booleanValue());
    assertEquals("DsNotUgA001", created.getObject("recipientUserGroup").getString("id").string());
    assertEquals(Set.of("DsNotDsA001"), dataSetIds(created));
    assertEquals(Set.of("EMAIL"), deliveryChannels(created));

    importMetadata(template("Template B", "DsNotDsB001", "SMS"));

    JsonObject updated = getTemplate();
    assertEquals("Template B", updated.getString("name").string());
    assertEquals(Set.of("DsNotDsB001"), dataSetIds(updated));
    assertEquals(Set.of("SMS"), deliveryChannels(updated));
  }

  @Test
  void shouldUpdateTemplateWhenExportedTemplateWithChangesIsPut() {
    importMetadata(template("Template A", "DsNotDsA001", "EMAIL"));
    JsonObject exported = getTemplate();
    JsonObject changed = withMember(exported, "subjectTemplate", "\"Subject changed\"");
    changed = withMember(changed, "relativeScheduledDays", "5");
    changed = withMember(changed, "dataSets", "[{\"id\":\"DsNotDsB001\"}]");

    assertStatus(
        HttpStatus.OK, PUT("/dataSetNotificationTemplates/" + TEMPLATE_UID, changed.toJson()));

    JsonObject updated = getTemplate();
    assertEquals("Subject changed", updated.getString("subjectTemplate").string());
    assertEquals(5, updated.getNumber("relativeScheduledDays").intValue());
    assertEquals(Set.of("DsNotDsB001"), dataSetIds(updated));
    assertEquals(Set.of("EMAIL"), deliveryChannels(updated));
    assertEquals(
        exported.getString("messageTemplate").string(),
        updated.getString("messageTemplate").string());
    assertEquals("DsNotUgA001", updated.getObject("recipientUserGroup").getString("id").string());
  }

  @Test
  void shouldDeleteTemplate() {
    importMetadata(template("Template A", "DsNotDsA001", "EMAIL"));

    assertStatus(HttpStatus.OK, DELETE("/dataSetNotificationTemplates/" + TEMPLATE_UID));

    assertEquals(HttpStatus.NOT_FOUND, getTemplateResponse("").status());
    assertStatus(HttpStatus.OK, GET("/dataSets/DsNotDsA001"));
  }

  @Test
  void shouldTranslateTemplateDisplayProperties() {
    importMetadata(
        """
        {'dataSetNotificationTemplates':[{
          'id':'%s','name':'Template A','subjectTemplate':'Subject','messageTemplate':'Message',
          'dataSetNotificationTrigger':'DATA_SET_COMPLETION',
          'notificationRecipient':'ORGANISATION_UNIT_CONTACT',
          'translations':[
            {'locale':'fr','property':'NAME','value':'Modele A'},
            {'locale':'fr','property':'SUBJECT_TEMPLATE','value':'Sujet'},
            {'locale':'fr','property':'MESSAGE_TEMPLATE','value':'Corps'}]
        }]}
        """
            .formatted(TEMPLATE_UID));

    JsonObject translated =
        getTemplateResponse(
                "?fields=displayName,displaySubjectTemplate,displayMessageTemplate&locale=fr")
            .content(HttpStatus.OK);

    assertEquals("Modele A", translated.getString("displayName").string());
    assertEquals("Sujet", translated.getString("displaySubjectTemplate").string());
    assertEquals("Corps", translated.getString("displayMessageTemplate").string());
    assertEquals(3, getTemplate().getArray("translations").size());
  }

  @Test
  void shouldExportTemplateProperties() {
    importMetadata(template("Template A", "DsNotDsA001", "EMAIL"));

    JsonObject template = getTemplate();

    Set<String> names = new TreeSet<>(PROPERTIES_WITHOUT_TRANSLATIONS);
    names.removeAll(Set.of("createdBy", "user"));
    names.add("dataSets");
    names.add("translations");
    assertEquals(names, new TreeSet<>(template.names()));
    assertTrue(
        template
            .getString("href")
            .string()
            .endsWith("/dataSetNotificationTemplates/" + TEMPLATE_UID));
    assertEquals("Template A", template.getString("displayName").string());
    assertEquals("Subject V{data_name}", template.getString("displaySubjectTemplate").string());
  }

  @Test
  void shouldDescribeTemplateSchemaPropertyNames() {
    Set<String> names = new TreeSet<>(PROPERTIES_WITHOUT_TRANSLATIONS);
    names.add("dataSet");
    names.add("translation");
    assertEquals(names, schemaPropertyNames(property -> true));
  }

  @Test
  void shouldDescribeTemplateSchemaPersistedAndRequiredProperties() {
    assertEquals(
        Set.of(
            "code",
            "created",
            "dataSet",
            "dataSetNotificationTrigger",
            "deliveryChannels",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "messageTemplate",
            "name",
            "notificationRecipient",
            "notifyParentOrganisationUnitOnly",
            "notifyUsersInHierarchyOnly",
            "recipientUserGroup",
            "relativeScheduledDays",
            "sendStrategy",
            "subjectTemplate",
            "translation"),
        schemaPropertyNames(property -> property.getBoolean("persisted").booleanValue()));
    assertEquals(
        Set.of("messageTemplate", "name"),
        schemaPropertyNames(property -> property.getBoolean("required").booleanValue()));
  }

  @Test
  void shouldDescribeTemplateSchemaTypesAndLengths() {
    Map<String, JsonObject> properties =
        schemaProperties().collect(Collectors.toMap(p -> p.getString("name").string(), p -> p));

    assertEquals(230, properties.get("name").getNumber("length").intValue());
    assertEquals(100, properties.get("subjectTemplate").getNumber("length").intValue());
    assertEquals(50, properties.get("sendStrategy").getNumber("length").intValue());
    assertEquals(
        Integer.MAX_VALUE, properties.get("messageTemplate").getNumber("length").intValue());
    assertEquals(
        "MESSAGE_TEMPLATE", properties.get("messageTemplate").getString("translationKey").string());
    assertEquals(
        "SUBJECT_TEMPLATE", properties.get("subjectTemplate").getString("translationKey").string());
    assertEquals(
        "REFERENCE", properties.get("recipientUserGroup").getString("propertyType").string());
    assertEquals("COLLECTION", properties.get("dataSet").getString("propertyType").string());
    assertEquals("dataSets", properties.get("dataSet").getString("fieldName").string());
    assertEquals("URL", properties.get("href").getString("propertyType").string());
  }

  @Test
  void shouldDescribeTemplateSchemaNameAndFlags() {
    JsonObject schema = GET("/schemas/dataSetNotificationTemplate").content(HttpStatus.OK);

    assertEquals("dataSetNotificationTemplate", schema.getString("name").string());
    assertFalse(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private Set<String> schemaPropertyNames(Predicate<JsonObject> filter) {
    return schemaProperties()
        .filter(filter)
        .map(property -> property.getString("name").string())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private Stream<JsonObject> schemaProperties() {
    return GET("/schemas/dataSetNotificationTemplate")
        .content(HttpStatus.OK)
        .getArray("properties")
        .asList(JsonObject.class)
        .stream();
  }

  private static Set<String> dataSetIds(JsonObject template) {
    return template.getArray("dataSets").asList(JsonObject.class).stream()
        .map(dataSet -> dataSet.getString("id").string())
        .collect(Collectors.toSet());
  }

  private static Set<String> deliveryChannels(JsonObject template) {
    return Set.copyOf(template.getArray("deliveryChannels").stringValues());
  }

  private static String template(String name, String dataSetUid, String deliveryChannel) {
    return """
        {'dataSetNotificationTemplates':[{
          'id':'%s','code':'TEMPLATE_A','name':'%s',
          'subjectTemplate':'Subject V{data_name}','messageTemplate':'Message V{data_name}',
          'relativeScheduledDays':-2,'dataSetNotificationTrigger':'SCHEDULED_DAYS',
          'notificationRecipient':'USER_GROUP','sendStrategy':'COLLECTIVE_SUMMARY',
          'notifyUsersInHierarchyOnly':true,'notifyParentOrganisationUnitOnly':false,
          'recipientUserGroup':{'id':'DsNotUgA001'},
          'dataSets':[{'id':'%s'}],'deliveryChannels':['%s']
        }]}
        """
        .formatted(TEMPLATE_UID, name, dataSetUid, deliveryChannel);
  }

  /** Returns a copy of the object with the value of the given member replaced. */
  private static JsonObject withMember(JsonObject object, String member, String json) {
    return JsonMixed.of(object.get(member).node().replaceWith(json)).asObject();
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
    assertEquals("OK", response.content().getString("status").string());
  }

  private JsonObject getTemplate() {
    return getTemplateResponse("").content(HttpStatus.OK);
  }

  /** Requests the template in a new session with an empty L2 cache. */
  private HttpResponse getTemplateResponse(String query) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/dataSetNotificationTemplates/" + TEMPLATE_UID + query);
  }
}
