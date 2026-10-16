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
package org.hisp.dhis.eventhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.ComponentType;
import org.hibernate.type.CustomType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.TextType;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.auth.HttpBasicAuthScheme;
import org.hisp.dhis.eventhook.targets.ConsoleTarget;
import org.hisp.dhis.eventhook.targets.JmsTarget;
import org.hisp.dhis.eventhook.targets.KafkaTarget;
import org.hisp.dhis.eventhook.targets.WebhookTarget;
import org.hisp.dhis.hibernate.jsonb.type.JsonBinaryType;
import org.hisp.dhis.hibernate.jsonb.type.JsonListBinaryType;
import org.hisp.dhis.hibernate.jsonb.type.JsonSetBinaryType;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.translation.Translation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class EventHookStoreTest extends PostgresIntegrationTestBase {
  @Autowired private EventHookStore store;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @DisplayName("every mapped property is written to its column and read back from it")
  void testColumnsRoundTrip() {
    EventHook eventHook = createEventHook('A');
    eventHook.setCode("HOOK_A");
    eventHook.setDescription("Sends metadata changes to a webhook");
    eventHook.setDisabled(true);
    eventHook.setTranslations(Set.of(new Translation(new Locale("fr"), "NAME", "Crochet A")));
    eventHook.addAttributeValue("AttrUid0001", "attribute value");
    eventHook.getSharing().setPublicAccess("rw------");
    EventHook saved = save(eventHook);

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select code, name, description, disabled, translations::text as translations,"
                + " sharing->>'public' as publicaccess,"
                + " attributevalues->'AttrUid0001'->>'value' as attr,"
                + " source->>'path' as path, source->>'fields' as fields,"
                + " targets->0->>'type' as targettype, targets->0->>'url' as url"
                + " from eventhook where eventhookid = ?",
            saved.getId());
    assertEquals("HOOK_A", row.get("code"));
    assertEquals("EventHookA", row.get("name"));
    assertEquals("Sends metadata changes to a webhook", row.get("description"));
    assertEquals(Boolean.TRUE, row.get("disabled"));
    assertTrue(((String) row.get("translations")).contains("\"Crochet A\""));
    assertEquals("rw------", row.get("publicaccess"));
    assertEquals("attribute value", row.get("attr"));
    assertEquals("metadata", row.get("path"));
    assertEquals("id,name", row.get("fields"));
    assertEquals(WebhookTarget.TYPE, row.get("targettype"));
    assertEquals("https://hooks.example.org/A", row.get("url"));

    EventHook reloaded = reload(saved);
    assertEquals("HOOK_A", reloaded.getCode());
    assertEquals("EventHookA", reloaded.getName());
    assertEquals("Sends metadata changes to a webhook", reloaded.getDescription());
    assertTrue(reloaded.isDisabled());
    assertEquals(
        Set.of(new Translation(new Locale("fr"), "NAME", "Crochet A")), reloaded.getTranslations());
    assertEquals("rw------", reloaded.getSharing().getPublicAccess());
    assertEquals("attribute value", reloaded.getAttributeValues().get("AttrUid0001"));
    assertEquals(new Source().setPath("metadata").setFields("id,name"), reloaded.getSource());
    assertEquals(saved.getTargets(), reloaded.getTargets());
  }

  @Test
  @DisplayName("each target type is stored and read back as its own subtype, in order")
  void testTargetSubtypesRoundTrip() {
    EventHook eventHook = createEventHook('B');
    JmsTarget jms = new JmsTarget().setBrokerUrl("tcp://broker:61616").setUseQueue(true);
    KafkaTarget kafka = new KafkaTarget().setBootstrapServers("kafka:9092").setTopic("hooks");
    ConsoleTarget console = new ConsoleTarget();
    eventHook.getTargets().addAll(List.of(console, jms, kafka));
    EventHook saved = save(eventHook);

    assertEquals(
        List.of("webhook", "console", "jms", "kafka"),
        jdbcTemplate.queryForList(
            "select t->>'type' from eventhook, jsonb_array_elements(targets) t"
                + " where eventhookid = ?",
            String.class,
            saved.getId()));

    List<Target> targets = reload(saved).getTargets();
    assertEquals(4, targets.size());
    assertInstanceOf(WebhookTarget.class, targets.get(0));
    assertEquals(console, assertInstanceOf(ConsoleTarget.class, targets.get(1)));
    assertEquals(jms, assertInstanceOf(JmsTarget.class, targets.get(2)));
    assertEquals(kafka, assertInstanceOf(KafkaTarget.class, targets.get(3)));
  }

  @Test
  @DisplayName("target secrets are stored as given, under their own keys")
  void testTargetSecretsStorageFormat() {
    EventHook eventHook = createEventHook('C');
    WebhookTarget webhook = (WebhookTarget) eventHook.getTargets().get(0);
    webhook.setAuth(new HttpBasicAuthScheme().setUsername("admin").setPassword("ENC(basic)"));
    eventHook
        .getTargets()
        .addAll(
            List.of(
                new JmsTarget().setUsername("jms").setPassword("ENC(jms)"),
                new KafkaTarget().setUsername("kafka").setPassword("ENC(kafka)")));
    EventHook saved = save(eventHook);

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select targets->0->'auth'->>'password' as basic,"
                + " targets->1->>'password' as jms, targets->2->>'password' as kafka"
                + " from eventhook where eventhookid = ?",
            saved.getId());
    assertEquals("ENC(basic)", row.get("basic"));
    assertEquals("ENC(jms)", row.get("jms"));
    assertEquals("ENC(kafka)", row.get("kafka"));

    List<Target> targets = reload(saved).getTargets();
    HttpBasicAuthScheme auth =
        assertInstanceOf(HttpBasicAuthScheme.class, ((WebhookTarget) targets.get(0)).getAuth());
    assertEquals("ENC(basic)", auth.getPassword());
    assertEquals("ENC(jms)", ((JmsTarget) targets.get(1)).getPassword());
    assertEquals("ENC(kafka)", ((KafkaTarget) targets.get(2)).getPassword());
  }

  @Test
  @DisplayName("a null translations column is read as no translations")
  void testNullTranslationsColumnIsReadAsEmpty() {
    EventHook saved = save(createEventHook('D'));
    jdbcTemplate.update(
        "update eventhook set translations = null where eventhookid = ?", saved.getId());

    EventHook reloaded = reload(saved);

    assertTrue(reloaded.getTranslations().isEmpty());
    assertEquals("EventHookD", reloaded.getDisplayName());
  }

  @Test
  @DisplayName("null sharing and attribute values columns are read as empty")
  void testNullSharingAndAttributeValuesColumnsAreReadAsEmpty() {
    EventHook saved = save(createEventHook('E'));
    jdbcTemplate.update(
        "update eventhook set sharing = null, attributevalues = null where eventhookid = ?",
        saved.getId());

    EventHook reloaded = reload(saved);

    assertNotNull(reloaded.getSharing());
    assertNull(reloaded.getSharing().getPublicAccess());
    assertTrue(reloaded.getAttributeValues().isEmpty());
  }

  @Test
  @DisplayName("source and targets use the jblEventHookSource and jblEventHookTargets jsonb types")
  void testSourceAndTargetsTypes() {
    CustomType source = assertInstanceOf(CustomType.class, persister().getPropertyType("source"));
    JsonBinaryType sourceType = assertInstanceOf(JsonBinaryType.class, source.getUserType());
    assertEquals(Source.class, sourceType.returnedClass());

    CustomType targets = assertInstanceOf(CustomType.class, persister().getPropertyType("targets"));
    JsonListBinaryType targetsType =
        assertInstanceOf(JsonListBinaryType.class, targets.getUserType());
    assertEquals(Target.class, targetsType.returnedClass());
  }

  @Test
  @DisplayName("description is mapped with the text type")
  void testDescriptionIsText() {
    assertInstanceOf(TextType.class, persister().getPropertyType("description"));
  }

  @Test
  @DisplayName("translations is an embedded jsonb set of type jblTranslations")
  void testTranslationsType() {
    ComponentType component =
        assertInstanceOf(ComponentType.class, persister().getPropertyType("translations"));
    CustomType type = assertInstanceOf(CustomType.class, component.getSubtypes()[0]);

    assertInstanceOf(JsonSetBinaryType.class, type.getUserType());
  }

  @Test
  @DisplayName("the id is generated by a sequence generator on hibernate_sequence")
  void testIdGeneratorUsesHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
  }

  @Test
  @DisplayName("createdBy is a lazy many-to-one")
  void testCreatedByIsLazy() {
    assertFalse(
        assertInstanceOf(ManyToOneType.class, persister().getPropertyType("createdBy"))
            .isEager(null));
  }

  @Test
  @DisplayName("name, source and targets are mapped not-null, the other own properties nullable")
  void testNullability() {
    assertNotNullable("name");
    assertNotNullable("source");
    assertNotNullable("targets");
    assertNullable("code");
    assertNullable("description");
    assertNullable("disabled");
    assertNullable("createdBy");
    assertNullable("sharing");
    assertNullable("attributeValues");
    assertNullable("translations");
  }

  @Test
  @DisplayName("the entity is second-level cached with read-write")
  void testCacheStrategy() {
    EntityPersister persister = persister();

    assertTrue(persister.canReadFromCache());
    assertEquals(AccessType.READ_WRITE, persister.getCacheAccessStrategy().getAccessType());
  }

  private EventHook createEventHook(char uniqueCharacter) {
    WebhookTarget webhook =
        new WebhookTarget().setUrl("https://hooks.example.org/" + uniqueCharacter);
    EventHook eventHook =
        new EventHook()
            .setSource(new Source().setPath("metadata").setFields("id,name"))
            .setTargets(new ArrayList<>(List.of(webhook)));
    eventHook.setAutoFields();
    eventHook.setName("EventHook" + uniqueCharacter);
    return eventHook;
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(EventHook.class);
  }

  private void assertNotNullable(String property) {
    assertFalse(isNullable(property), property + " is mapped nullable");
  }

  private void assertNullable(String property) {
    assertTrue(isNullable(property), property + " is mapped not-null");
  }

  /** {@link EntityPersister#getPropertyNullability()} holds a nullable flag per property. */
  private boolean isNullable(String property) {
    EntityPersister persister = persister();
    return persister
        .getPropertyNullability()[persister.getEntityMetamodel().getPropertyIndex(property)];
  }

  /** Saves without resetting the sharing, so a preset public access is stored as given. */
  private EventHook save(EventHook eventHook) {
    store.save(eventHook, false);
    entityManager.flush();
    return eventHook;
  }

  /** Reloads the event hook from the database, bypassing the session and the L2 cache. */
  private EventHook reload(EventHook eventHook) {
    clearSession();
    entityManagerFactory.getCache().evict(EventHook.class);
    return store.get(eventHook.getId());
  }
}
