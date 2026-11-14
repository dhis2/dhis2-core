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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageEncoding;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests how incoming SMS are read and deleted through {@code /api/sms/inbound}. */
@Transactional
class IncomingSmsControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private IncomingSmsService incomingSmsService;

  @Autowired private SchemaService schemaService;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void testSchemaPropertiesAndPersistedProperties() {
    Schema schema = schemaService.getSchema(IncomingSms.class);

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "displayName",
            "gatewayid",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "originator",
            "receiveddate",
            "sentdate",
            "sharing",
            "smsencoding",
            "smsstatus",
            "text",
            "translation",
            "user"),
        propertyNames(schema.getProperties()));
    assertEquals(
        Set.of(
            "createdBy",
            "gatewayid",
            "id",
            "originator",
            "receiveddate",
            "sentdate",
            "smsencoding",
            "smsstatus",
            "text"),
        propertyNames(schema.getPersistedProperties().values()));
  }

  /** The key set served by play/dev for an incoming SMS without a user. */
  @Test
  void testGetMessageWithoutUserJson() {
    long id =
        saveSms("inSmsJson01", SmsMessageStatus.PROCESSED, new Date(1_600_000_000_000L), null);
    entityManager.flush();
    // the store sets createdBy to the current user on save; play/dev rows have no user
    jdbcTemplate.update("update incomingsms set userid = null where id = ?", id);
    flushAndClear();

    JsonObject sms = GET("/sms/inbound/inSmsJson01").content(HttpStatus.OK);

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "created",
            "gatewayid",
            "id",
            "lastUpdated",
            "originator",
            "receiveddate",
            "sentdate",
            "sharing",
            "smsencoding",
            "smsstatus",
            "text",
            "translations"),
        Set.copyOf(sms.names()));
    assertEquals("inSmsJson01", sms.getString("id").string());
    assertEquals("PROCESSED", sms.getString("smsstatus").string());
    assertEquals("ENC7BIT", sms.getString("smsencoding").string());
    assertEquals("+47inSmsJson01", sms.getString("originator").string());
    assertEquals("Text of inSmsJson01", sms.getString("text").string());
    assertEquals("gateway", sms.getString("gatewayid").string());
  }

  @Test
  void testGetMessageWithUserJson() {
    User sender = makeUser("S");
    userService.addUser(sender);
    saveSms("inSmsUser01", SmsMessageStatus.INCOMING, new Date(), sender);
    flushAndClear();

    JsonObject sms =
        GET("/sms/inbound/inSmsUser01?fields=id,createdBy,user").content(HttpStatus.OK);

    assertEquals(sender.getUid(), sms.getObject("createdBy").getString("id").string());
    assertEquals(sender.getUid(), sms.getObject("user").getString("id").string());
  }

  @Test
  void testGetMessageAsUserWithSendSmsAuthority() {
    saveSms("inSmsAuth01", SmsMessageStatus.INCOMING, new Date(), null);
    flushAndClear();
    switchToNewUser("smsReader", Authorities.F_MOBILE_SENDSMS.name());

    JsonObject sms = GET("/sms/inbound/inSmsAuth01?fields=id,text").content(HttpStatus.OK);

    assertEquals("inSmsAuth01", sms.getString("id").string());
    assertEquals("Text of inSmsAuth01", sms.getString("text").string());
  }

  @Test
  void testReceiveJsonBodyStoresMessageWithSenderAsCreatedBy() {
    User sender = createUserWithAuth("smsSender", Authorities.F_MOBILE_SETTINGS.name());
    sender.setPhoneNumber("+4798765432");
    userService.updateUser(sender);

    JsonWebMessage response =
        POST(
                "/sms/inbound",
                """
                {"originator": "+4798765432", "text": "hello inbound", "gatewayid": "gw1",
                 "smsencoding": "ENC8BIT"}
                """)
            .content(HttpStatus.OK)
            .as(JsonWebMessage.class);
    String uid = response.getMessage().replaceFirst("^Received SMS: ", "");
    entityManager.flush();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select originator, text, gatewayid, encoding, userid from incomingsms where uid = ?",
            uid);
    assertEquals(
        Map.of(
            "originator",
            "+4798765432",
            "text",
            "hello inbound",
            "gatewayid",
            "gw1",
            "encoding",
            SmsMessageEncoding.ENC8BIT.ordinal(),
            "userid",
            sender.getId()),
        row);
  }

  @Test
  void testListFilteredByStatusAndOrderedBySentDate() {
    saveSms("inSmsNew001", SmsMessageStatus.FAILED, new Date(1_700_000_000_000L), null);
    saveSms("inSmsOld001", SmsMessageStatus.FAILED, new Date(1_500_000_000_000L), null);
    saveSms("inSmsProc01", SmsMessageStatus.PROCESSED, new Date(1_600_000_000_000L), null);
    flushAndClear();

    JsonObject list =
        GET("/sms/inbound?fields=id,originator&filter=smsstatus:eq:FAILED&order=sentdate:asc")
            .content(HttpStatus.OK);

    assertEquals(
        List.of("inSmsOld001", "inSmsNew001"),
        list.getArray("inboundsmss").asList(JsonObject.class).stream()
            .map(sms -> sms.getString("id").string())
            .toList());
    assertEquals(
        "+47inSmsOld001",
        list.getArray("inboundsmss").getObject(0).getString("originator").string());
  }

  @Test
  void testDeleteRemovesMessage() {
    long id = saveSms("inSmsDel001", SmsMessageStatus.INCOMING, new Date(), null);
    flushAndClear();

    assertWebMessage(
        "OK",
        200,
        "OK",
        "IncomingSms with inSmsDel001 deleted",
        DELETE("/sms/inbound/inSmsDel001").content(HttpStatus.OK));
    entityManager.flush();

    assertEquals(
        0,
        jdbcTemplate.queryForObject(
            "select count(*) from incomingsms where id = ?", Integer.class, id));
  }

  private long saveSms(String uid, SmsMessageStatus status, Date sentDate, User createdBy) {
    IncomingSms sms = new IncomingSms();
    sms.setUid(uid);
    sms.setOriginator("+47" + uid);
    sms.setText("Text of " + uid);
    sms.setGatewayId("gateway");
    sms.setStatus(status);
    sms.setReceivedDate(sentDate);
    sms.setCreatedBy(createdBy);
    incomingSmsService.save(sms);
    return sms.getId();
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
    entityManagerFactory.getCache().evictAll();
  }

  private static Set<String> propertyNames(Collection<Property> properties) {
    return properties.stream().map(Property::getName).collect(Collectors.toSet());
  }
}
