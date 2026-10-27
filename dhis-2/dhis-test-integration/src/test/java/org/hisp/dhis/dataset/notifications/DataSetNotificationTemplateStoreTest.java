/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dataset.notifications;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.collection.AbstractCollectionPersister;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.TextType;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetStore;
import org.hisp.dhis.notification.SendStrategy;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataSetNotificationTemplateStoreTest extends PostgresIntegrationTestBase {

  @Autowired private DataSetNotificationTemplateStore store;
  @Autowired private DataSetStore dataSetStore;
  @Autowired private UserGroupStore userGroupStore;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testGetNotificationsByTriggerTypeAndDataSet() {
    DataSet dataSet = createDataSet('A');
    dataSetStore.save(dataSet);

    DataSetNotificationTemplate template =
        createDataSetNotificationTemplate(
            "A",
            DataSetNotificationRecipient.USER_GROUP,
            DataSetNotificationTrigger.DATA_SET_COMPLETION,
            1,
            SendStrategy.SINGLE_NOTIFICATION);
    template.setDataSets(Set.of(dataSet));
    store.save(template);

    assertEquals(
        1,
        store
            .getNotificationsByTriggerType(dataSet, DataSetNotificationTrigger.DATA_SET_COMPLETION)
            .size());
  }

  @Test
  void testGetScheduledNotificationsValid() {
    DataSetNotificationTemplate template =
        createDataSetNotificationTemplate(
            "A",
            DataSetNotificationRecipient.USER_GROUP,
            DataSetNotificationTrigger.DATA_SET_COMPLETION,
            1,
            SendStrategy.SINGLE_NOTIFICATION);
    store.save(template);

    assertEquals(
        1, store.getScheduledNotifications(DataSetNotificationTrigger.DATA_SET_COMPLETION).size());
  }

  @Test
  void testGetScheduledNotificationsNotExist() {
    DataSetNotificationTemplate template =
        createDataSetNotificationTemplate(
            "A",
            DataSetNotificationRecipient.USER_GROUP,
            DataSetNotificationTrigger.DATA_SET_COMPLETION,
            1,
            SendStrategy.SINGLE_NOTIFICATION);
    store.save(template);

    assertEquals(
        0, store.getScheduledNotifications(DataSetNotificationTrigger.SCHEDULED_DAYS).size());
  }

  @Test
  void shouldMapTemplateToItsTableAndColumns() {
    AbstractEntityPersister persister = persister();

    assertEquals("datasetnotificationtemplate", persister.getTableName());
    assertArrayEquals(
        new String[] {"datasetnotificationtemplateid"}, persister.getIdentifierColumnNames());
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister.getIdentifierGenerator());
    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());

    Map<String, String> columns =
        Map.ofEntries(
            Map.entry("uid", "uid"),
            Map.entry("code", "code"),
            Map.entry("created", "created"),
            Map.entry("lastUpdated", "lastupdated"),
            Map.entry("lastUpdatedBy", "lastupdatedby"),
            Map.entry("name", "name"),
            Map.entry("messageTemplate", "messagetemplate"),
            Map.entry("subjectTemplate", "subjecttemplate"),
            Map.entry("relativeScheduledDays", "relativescheduleddays"),
            Map.entry("dataSetNotificationTrigger", "datasetnotificationtrigger"),
            Map.entry("notificationRecipient", "notificationrecipienttype"),
            Map.entry("recipientUserGroup", "usergroupid"),
            Map.entry("sendStrategy", "sendstrategy"),
            Map.entry("notifyUsersInHierarchyOnly", "notifyusersinhierarchyonly"),
            Map.entry("notifyParentOrganisationUnitOnly", "notifyparentorganisationunitonly"),
            Map.entry("translations", "translations"));
    columns.forEach(
        (property, column) ->
            assertEquals(
                column,
                persister.getPropertyColumnNames(property)[0].toLowerCase(),
                property + " column"));

    AbstractCollectionPersister dataSets = collectionPersister("dataSets");
    assertEquals("datasetnotification_datasets", dataSets.getTableName());
    assertArrayEquals(new String[] {"datasetnotificationtemplateid"}, dataSets.getKeyColumnNames());
    assertArrayEquals(new String[] {"datasetid"}, dataSets.getElementColumnNames());
    assertFalse(dataSets.isInverse());

    AbstractCollectionPersister deliveryChannels = collectionPersister("deliveryChannels");
    assertEquals("datasetnotificationtemplate_deliverychannel", deliveryChannels.getTableName());
    assertArrayEquals(
        new String[] {"datasetnotificationtemplateid"}, deliveryChannels.getKeyColumnNames());
    assertArrayEquals(new String[] {"deliverychannel"}, deliveryChannels.getElementColumnNames());
  }

  @Test
  void shouldMapTemplateNullabilityTypesAndLaziness() {
    AbstractEntityPersister persister = persister();

    for (String property : List.of("uid", "name", "messageTemplate", "created", "lastUpdated")) {
      assertFalse(isNullable(property), property + " is mapped nullable");
    }
    for (String property :
        List.of(
            "code",
            "subjectTemplate",
            "relativeScheduledDays",
            "dataSetNotificationTrigger",
            "notificationRecipient",
            "recipientUserGroup",
            "sendStrategy",
            "notifyUsersInHierarchyOnly",
            "notifyParentOrganisationUnitOnly",
            "lastUpdatedBy",
            "translations")) {
      assertTrue(isNullable(property), property + " is mapped not-null");
    }
    assertInstanceOf(TextType.class, persister.getPropertyType("messageTemplate"));
    assertManyToOneLazy("recipientUserGroup", UserGroup.class);
    assertManyToOneLazy("lastUpdatedBy", User.class);
    assertFalse(
        persister
            .getPropertyUpdateability()[
            persister.getEntityMetamodel().getPropertyIndex("created")]);
    assertFalse(persister.canWriteToCache());
    assertTrue(collectionPersister("dataSets").isLazy());
    assertTrue(collectionPersister("deliveryChannels").isLazy());
  }

  @Test
  void shouldWriteAndReadTemplateColumns() {
    User admin = getAdminUser();
    UserGroup userGroup = createUserGroup('A', Set.of());
    userGroupStore.save(userGroup);
    DataSet dataSet = createDataSet('B');
    dataSetStore.save(dataSet);
    DataSetNotificationTemplate template =
        createDataSetNotificationTemplate(
            "Template B",
            DataSetNotificationRecipient.USER_GROUP,
            DataSetNotificationTrigger.SCHEDULED_DAYS,
            -3,
            SendStrategy.COLLECTIVE_SUMMARY);
    template.setCode("TEMPLATE_B");
    template.setSubjectTemplate("Subject B");
    template.setMessageTemplate("Message ".repeat(100));
    template.setRecipientUserGroup(userGroup);
    template.setNotifyUsersInHierarchyOnly(true);
    template.setNotifyParentOrganisationUnitOnly(false);
    template.setLastUpdatedBy(admin);
    template.setDataSets(new HashSet<>(Set.of(dataSet)));
    template.setDeliveryChannels(new HashSet<>(Set.of(DeliveryChannel.EMAIL)));
    store.save(template);
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select uid, code, name, subjecttemplate, messagetemplate, relativescheduleddays,"
                + " datasetnotificationtrigger, notificationrecipienttype, sendstrategy,"
                + " usergroupid, notifyusersinhierarchyonly, notifyparentorganisationunitonly,"
                + " lastupdatedby, translations::text as translations"
                + " from datasetnotificationtemplate where datasetnotificationtemplateid = ?",
            template.getId());
    assertEquals(template.getUid(), row.get("uid"));
    assertEquals("TEMPLATE_B", row.get("code"));
    assertEquals("Template B", row.get("name"));
    assertEquals("Subject B", row.get("subjecttemplate"));
    assertEquals("Message ".repeat(100), row.get("messagetemplate"));
    assertEquals(-3, row.get("relativescheduleddays"));
    assertEquals("SCHEDULED_DAYS", row.get("datasetnotificationtrigger"));
    assertEquals("USER_GROUP", row.get("notificationrecipienttype"));
    assertEquals("COLLECTIVE_SUMMARY", row.get("sendstrategy"));
    assertEquals(userGroup.getId(), ((Number) row.get("usergroupid")).longValue());
    assertEquals(true, row.get("notifyusersinhierarchyonly"));
    assertEquals(false, row.get("notifyparentorganisationunitonly"));
    assertEquals(admin.getId(), ((Number) row.get("lastupdatedby")).longValue());
    assertEquals("[]", row.get("translations"));
    assertEquals(
        List.of(dataSet.getId()),
        jdbcTemplate.queryForList(
            "select datasetid from datasetnotification_datasets"
                + " where datasetnotificationtemplateid = ?",
            Long.class,
            template.getId()));
    assertEquals(
        List.of("EMAIL"),
        jdbcTemplate.queryForList(
            "select deliverychannel from datasetnotificationtemplate_deliverychannel"
                + " where datasetnotificationtemplateid = ?",
            String.class,
            template.getId()));

    DataSetNotificationTemplate loaded = reload(template);
    assertEquals("TEMPLATE_B", loaded.getCode());
    assertEquals("Template B", loaded.getName());
    assertEquals("Message ".repeat(100), loaded.getMessageTemplate());
    assertEquals("Subject B", loaded.getSubjectTemplate());
    assertEquals(-3, loaded.getRelativeScheduledDays());
    assertEquals(DataSetNotificationTrigger.SCHEDULED_DAYS, loaded.getDataSetNotificationTrigger());
    assertEquals(DataSetNotificationRecipient.USER_GROUP, loaded.getNotificationRecipient());
    assertEquals(SendStrategy.COLLECTIVE_SUMMARY, loaded.getSendStrategy());
    assertEquals(userGroup.getUid(), loaded.getRecipientUserGroup().getUid());
    assertTrue(loaded.getNotifyUsersInHierarchyOnly());
    assertFalse(loaded.getNotifyParentOrganisationUnitOnly());
    assertEquals(admin.getUid(), loaded.getLastUpdatedBy().getUid());
    assertEquals(Set.of(dataSet.getUid()), uids(loaded.getDataSets()));
    assertEquals(Set.of(DeliveryChannel.EMAIL), loaded.getDeliveryChannels());
  }

  @Test
  void shouldReplaceDataSetAndDeliveryChannelRowsAndDeleteThemWithTemplate() {
    DataSet dataSetA = createDataSet('C');
    DataSet dataSetB = createDataSet('D');
    dataSetStore.save(dataSetA);
    dataSetStore.save(dataSetB);
    DataSetNotificationTemplate template =
        createDataSetNotificationTemplate(
            "Template C",
            DataSetNotificationRecipient.ORGANISATION_UNIT_CONTACT,
            DataSetNotificationTrigger.DATA_SET_COMPLETION,
            0,
            SendStrategy.SINGLE_NOTIFICATION);
    template.setDataSets(new HashSet<>(Set.of(dataSetA)));
    template.setDeliveryChannels(new HashSet<>(Set.of(DeliveryChannel.SMS)));
    store.save(template);
    entityManager.flush();

    DataSetNotificationTemplate loaded = reload(template);
    loaded.getDataSets().clear();
    loaded.getDataSets().add(dataSetStore.getByUid(dataSetB.getUid()));
    loaded.getDeliveryChannels().add(DeliveryChannel.EMAIL);
    store.update(loaded);
    entityManager.flush();

    assertEquals(
        List.of(dataSetB.getId()),
        jdbcTemplate.queryForList(
            "select datasetid from datasetnotification_datasets"
                + " where datasetnotificationtemplateid = ?",
            Long.class,
            template.getId()));
    assertEquals(
        List.of("EMAIL", "SMS"),
        jdbcTemplate.queryForList(
            "select deliverychannel from datasetnotificationtemplate_deliverychannel"
                + " where datasetnotificationtemplateid = ? order by deliverychannel",
            String.class,
            template.getId()));

    store.delete(loaded);
    entityManager.flush();

    assertEquals(0, countRows("datasetnotification_datasets", template.getId()));
    assertEquals(0, countRows("datasetnotificationtemplate_deliverychannel", template.getId()));
    assertEquals(1, dataSetStore.getAll().stream().filter(dataSetB::equals).count());
  }

  @Test
  void shouldWriteAndReadTranslations() {
    DataSetNotificationTemplate template = newTemplate("Template E");
    template.setTranslations(
        new HashSet<>(Set.of(new Translation(Locale.FRENCH, "SUBJECT_TEMPLATE", "Sujet"))));
    store.save(template);
    entityManager.flush();

    DataSetNotificationTemplate loaded = reload(template);

    assertEquals(
        Set.of(new Translation(Locale.FRENCH, "SUBJECT_TEMPLATE", "Sujet")),
        loaded.getTranslations());
  }

  @Test
  void shouldLoadNullTranslationsColumnAsEmptyTranslations() {
    DataSetNotificationTemplate template = newTemplate("Template F");
    store.save(template);
    entityManager.flush();
    jdbcTemplate.update(
        "update datasetnotificationtemplate set translations = null"
            + " where datasetnotificationtemplateid = ?",
        template.getId());

    DataSetNotificationTemplate loaded = reload(template);

    assertTrue(loaded.getTranslations().isEmpty());
    assertEquals("Template F", loaded.getDisplayName());
    assertEquals("Subject", loaded.getDisplaySubjectTemplate());
  }

  @Test
  void shouldEqualUninitializedProxyOfSameTemplate() {
    DataSetNotificationTemplate template = newTemplate("Template G");
    template.setCode("TEMPLATE_G");
    store.save(template);
    entityManager.flush();
    clearSession();

    DataSetNotificationTemplate proxy =
        entityManager.getReference(DataSetNotificationTemplate.class, template.getId());

    assertFalse(Hibernate.isInitialized(proxy));
    assertEquals(proxy, template);
    assertEquals(template, proxy);
    assertEquals(template.hashCode(), proxy.hashCode());
  }

  private DataSetNotificationTemplate newTemplate(String name) {
    return createDataSetNotificationTemplate(
        name,
        DataSetNotificationRecipient.ORGANISATION_UNIT_CONTACT,
        DataSetNotificationTrigger.DATA_SET_COMPLETION,
        0,
        SendStrategy.SINGLE_NOTIFICATION);
  }

  private DataSetNotificationTemplate reload(DataSetNotificationTemplate template) {
    entityManager.flush();
    clearSession();
    return store.get(template.getId());
  }

  private int countRows(String table, long templateId) {
    return jdbcTemplate.queryForObject(
        "select count(*) from " + table + " where datasetnotificationtemplateid = ?",
        Integer.class,
        templateId);
  }

  private static Set<String> uids(Set<DataSet> dataSets) {
    Set<String> uids = new HashSet<>();
    dataSets.forEach(dataSet -> uids.add(dataSet.getUid()));
    return uids;
  }

  private void assertManyToOneLazy(String property, Class<?> target) {
    ManyToOneType type =
        assertInstanceOf(ManyToOneType.class, persister().getPropertyType(property));
    assertEquals(target.getName(), type.getAssociatedEntityName(), property);
    assertFalse(type.isEager(null), property + " is eager");
  }

  private boolean isNullable(String property) {
    AbstractEntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  private AbstractEntityPersister persister() {
    return (AbstractEntityPersister)
        sessionFactory().getMetamodel().entityPersister(DataSetNotificationTemplate.class);
  }

  private AbstractCollectionPersister collectionPersister(String property) {
    return (AbstractCollectionPersister)
        sessionFactory()
            .getMetamodel()
            .collectionPersister(DataSetNotificationTemplate.class.getName() + "." + property);
  }

  private SessionFactoryImplementor sessionFactory() {
    return entityManagerFactory.unwrap(SessionFactoryImplementor.class);
  }
}
