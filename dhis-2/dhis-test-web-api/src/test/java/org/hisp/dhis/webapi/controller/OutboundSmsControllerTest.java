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

import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests how outbound SMS are read and deleted through {@code /api/sms/outbound}. */
@Transactional
class OutboundSmsControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private OutboundSmsService outboundSmsService;

  @Autowired private SchemaService schemaService;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  /** The properties of the dynamic schema, as served before the annotation mapping. */
  @Test
  void testSchemaProperties() {
    Schema schema = schemaService.getSchema(OutboundSms.class);

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "date",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "message",
            "name",
            "recipients",
            "sender",
            "sharing",
            "status",
            "subject",
            "translation",
            "user"),
        propertyNames(schema.getProperties()));
    assertEquals(
        Set.of("date", "id", "message", "recipients", "sender", "status"),
        propertyNames(schema.getPersistedProperties().values()));
  }

  /**
   * The key set served by play/dev for an outbound SMS before the annotation mapping. created and
   * lastUpdated have no column: for a loaded row they hold the time it was loaded, as before.
   */
  @Test
  void testGetMessageJson() {
    saveSms("outSmsJson1", OutboundSmsStatus.SENT, new Date(), "+4711111111", "+4722222222");
    flushAndClear();

    JsonObject sms = GET("/sms/outbound/outSmsJson1").content(HttpStatus.OK);

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "created",
            "date",
            "id",
            "lastUpdated",
            "message",
            "recipients",
            "sender",
            "sharing",
            "status",
            "translations"),
        Set.copyOf(sms.names()));
    assertEquals("outSmsJson1", sms.getString("id").string());
    assertEquals("SENT", sms.getString("status").string());
    assertEquals("Message of outSmsJson1", sms.getString("message").string());
    assertEquals("Sender of outSmsJson1", sms.getString("sender").string());
    assertEquals(
        Set.of("+4711111111", "+4722222222"),
        Set.copyOf(sms.getArray("recipients").stringValues()));
  }

  @Test
  void testListFilteredByStatusAndOrderedByDate() {
    saveSms("outSmsNew01", OutboundSmsStatus.FAILED, new Date(1_700_000_000_000L), "+471");
    saveSms("outSmsOld01", OutboundSmsStatus.FAILED, new Date(1_500_000_000_000L), "+472");
    saveSms("outSmsSnt01", OutboundSmsStatus.SENT, new Date(1_600_000_000_000L), "+473");
    flushAndClear();

    JsonObject list =
        GET("/sms/outbound?fields=id,recipients&filter=status:eq:FAILED&order=date:asc")
            .content(HttpStatus.OK);

    assertEquals(
        List.of("outSmsOld01", "outSmsNew01"),
        list.getArray("outboundsmss").asList(JsonObject.class).stream()
            .map(sms -> sms.getString("id").string())
            .toList());
    assertEquals(
        List.of("+472"),
        list.getArray("outboundsmss").getObject(0).getArray("recipients").stringValues());
  }

  @Test
  void testDeleteRemovesMessageAndRecipients() {
    long id = saveSms("outSmsDel01", OutboundSmsStatus.SENT, new Date(), "+4733333333");
    flushAndClear();

    assertWebMessage(
        "OK",
        200,
        "OK",
        "OutboundSms with outSmsDel01 deleted",
        DELETE("/sms/outbound/outSmsDel01").content(HttpStatus.OK));
    entityManager.flush();

    assertEquals(0, count("select count(*) from outbound_sms where id = ?", id));
    assertEquals(
        0, count("select count(*) from outbound_sms_recipients where outbound_sms_id = ?", id));
  }

  private long saveSms(String uid, OutboundSmsStatus status, Date date, String... recipients) {
    OutboundSms sms = new OutboundSms();
    sms.setUid(uid);
    sms.setStatus(status);
    sms.setDate(date);
    sms.setMessage("Message of " + uid);
    sms.setSender("Sender of " + uid);
    sms.setRecipients(new HashSet<>(Set.of(recipients)));
    return outboundSmsService.save(sms);
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
    entityManagerFactory.getCache().evictAll();
  }

  private int count(String sql, long id) {
    return jdbcTemplate.queryForObject(sql, Integer.class, id);
  }

  private static Set<String> propertyNames(Collection<Property> properties) {
    return properties.stream().map(Property::getName).collect(Collectors.toSet());
  }
}
