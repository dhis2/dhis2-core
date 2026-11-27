/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.sms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BooleanType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.StringType;
import org.hibernate.type.TimestampType;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsStore;
import org.hisp.dhis.sms.incoming.SmsMessageEncoding;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsStatus;
import org.hisp.dhis.sms.outbound.OutboundSmsStore;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class IncomingSmsStoreTest extends PostgresIntegrationTestBase {

  @Autowired private IncomingSmsStore incomingSmsStore;

  @Autowired private OutboundSmsStore outboundSmsStore;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

  private User user;

  @BeforeAll
  void setUp() {
    user = makeUser("A");
    userService.addUser(user);
  }

  @Test
  void testGetIncomingSms() {
    IncomingSms sms = new IncomingSms();
    sms.setText("testMessage");
    sms.setOriginator("474000000");
    sms.setGatewayId("testGateway");
    sms.setCreatedBy(user);
    sms.setSentDate(new Date());
    sms.setReceivedDate(new Date());
    incomingSmsStore.save(sms);
    List<IncomingSms> incomingSmsList =
        incomingSmsStore.getSmsByStatus(SmsMessageStatus.INCOMING, "474");
    assertEquals(1, incomingSmsList.size());
    assertEquals("testMessage", incomingSmsList.get(0).getText());
    assertEquals(
        1, incomingSmsStore.getSmsByStatus(SmsMessageStatus.INCOMING, "474", 0, 10, false).size());
    assertEquals(1, incomingSmsStore.getSmsByOriginator("474000000").size());
  }

  @Test
  void testOutboundSms() {
    OutboundSms outboundSms = new OutboundSms();
    outboundSms.setDate(new Date());
    outboundSms.setMessage("testMessage");
    outboundSms.setSender("testSender");
    outboundSms.setStatus(OutboundSmsStatus.OUTBOUND);
    outboundSms.setSubject("testSubject");
    outboundSms.setRecipients(Sets.newHashSet("testRecipient"));
    outboundSmsStore.saveOutboundSms(outboundSms);
    assertEquals(1, outboundSmsStore.get(OutboundSmsStatus.OUTBOUND).size());
    assertEquals(1, outboundSmsStore.get(OutboundSmsStatus.OUTBOUND, 0, 10, false).size());
  }

  @Test
  void testIdIsGeneratedFromHibernateSequence() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
  }

  @Test
  void testPersistedPropertyNames() {
    assertEquals(
        Set.of(
            "uid",
            "originator",
            "encoding",
            "sentDate",
            "receivedDate",
            "text",
            "gatewayId",
            "status",
            "parsed",
            "statusMessage",
            "createdBy"),
        Set.of(persister().getPropertyNames()));
  }

  @Test
  void testAllColumnsAreWrittenAndReadBack() {
    Date sent = new Date(1_600_000_000_000L);
    Date received = new Date(1_600_000_060_000L);
    IncomingSms sms = sms("inSmsRndTr1", "+4711111111", sent);
    sms.setReceivedDate(received);
    sms.setEncoding(SmsMessageEncoding.ENCUCS2);
    sms.setStatus(SmsMessageStatus.PROCESSED);
    sms.setParsed(true);
    sms.setStatusMessage("Processed by command X");
    sms.setCreatedBy(user);
    incomingSmsStore.save(sms);
    entityManager.flush();

    Map<String, Object> expected = new HashMap<>();
    expected.put("uid", "inSmsRndTr1");
    expected.put("originator", "+4711111111");
    expected.put("encoding", SmsMessageEncoding.ENCUCS2.ordinal());
    expected.put("sentdate", new Timestamp(sent.getTime()));
    expected.put("receiveddate", new Timestamp(received.getTime()));
    expected.put("text", "Text of inSmsRndTr1");
    expected.put("gatewayid", "gatewayOf+4711111111");
    expected.put("status", SmsMessageStatus.PROCESSED.ordinal());
    expected.put("parsed", true);
    expected.put("statusmessage", "Processed by command X");
    expected.put("userid", user.getId());
    assertEquals(
        expected,
        jdbcTemplate.queryForMap(
            "select uid, originator, encoding, sentdate, receiveddate, text, gatewayid, status,"
                + " parsed, statusmessage, userid from incomingsms where id = ?",
            sms.getId()));

    clearSessionAndSecondLevelCache();

    IncomingSms reloaded = incomingSmsStore.get(sms.getId());
    assertEquals("inSmsRndTr1", reloaded.getUid());
    assertEquals("+4711111111", reloaded.getOriginator());
    assertEquals(SmsMessageEncoding.ENCUCS2, reloaded.getEncoding());
    assertEquals(sent.getTime(), reloaded.getSentDate().getTime());
    assertEquals(received.getTime(), reloaded.getReceivedDate().getTime());
    assertEquals("Text of inSmsRndTr1", reloaded.getText());
    assertEquals("gatewayOf+4711111111", reloaded.getGatewayId());
    assertEquals(SmsMessageStatus.PROCESSED, reloaded.getStatus());
    assertTrue(reloaded.isParsed());
    assertEquals("Processed by command X", reloaded.getStatusMessage());
    assertEquals(user.getUid(), reloaded.getCreatedBy().getUid());
  }

  @Test
  void testNotNullColumnsAreMappedNotNull() {
    EntityPersister persister = persister();
    boolean[] nullable = persister.getPropertyNullability();

    for (String property :
        List.of(
            "uid", "originator", "encoding", "sentDate", "receivedDate", "gatewayId", "status")) {
      assertFalse(nullable[propertyIndex(persister, property)], property);
    }
    for (String property : List.of("text", "parsed", "statusMessage", "createdBy")) {
      assertTrue(nullable[propertyIndex(persister, property)], property);
    }
  }

  @Test
  void testTextColumnsAreBoundAsStringsAndDatesAsTimestamps() {
    EntityPersister persister = persister();

    for (String property : List.of("uid", "originator", "text", "gatewayId", "statusMessage")) {
      assertInstanceOf(StringType.class, persister.getPropertyType(property), property);
    }
    assertInstanceOf(TimestampType.class, persister.getPropertyType("sentDate"));
    assertInstanceOf(TimestampType.class, persister.getPropertyType("receivedDate"));
    assertInstanceOf(BooleanType.class, persister.getPropertyType("parsed"));
  }

  @Test
  void testCreatedByIsALazyManyToOneToUser() {
    ManyToOneType type =
        assertInstanceOf(ManyToOneType.class, persister().getPropertyType("createdBy"));

    assertEquals(User.class.getName(), type.getAssociatedEntityName());
    assertFalse(type.isEager(null));
  }

  @Test
  void testEntityCanBeProxiedAndIsNotSecondLevelCached() {
    EntityPersister persister = persister();

    assertTrue(persister.hasProxy());
    assertFalse(persister.canReadFromCache());
    assertNull(persister.getCacheAccessStrategy());
  }

  @Test
  void testGetAllUnparsedMessagesReturnsOnlyUnparsed() {
    IncomingSms parsed = sms("inSmsParsd1", "+4722222222", new Date());
    parsed.setParsed(true);
    incomingSmsStore.save(parsed);
    incomingSmsStore.save(sms("inSmsUnprs1", "+4722222222", new Date()));
    clearSessionAndSecondLevelCache();

    assertEquals(
        List.of("inSmsUnprs1"),
        incomingSmsStore.getAllUnparsedMessages().stream().map(IncomingSms::getUid).toList());
  }

  @Test
  void testGetSmsByStatusOrdersBySentDateDescending() {
    incomingSmsStore.save(sms("inSmsOld001", "+4733333333", new Date(1_500_000_000_000L)));
    incomingSmsStore.save(sms("inSmsNew001", "+4733333333", new Date(1_700_000_000_000L)));
    IncomingSms failed = sms("inSmsFail01", "+4733333333", new Date(1_600_000_000_000L));
    failed.setStatus(SmsMessageStatus.FAILED);
    incomingSmsStore.save(failed);
    clearSessionAndSecondLevelCache();

    assertEquals(
        List.of("inSmsNew001", "inSmsOld001"),
        incomingSmsStore.getSmsByStatus(SmsMessageStatus.INCOMING, "+47333").stream()
            .map(IncomingSms::getUid)
            .toList());
  }

  /** Reloads must hit the database, not the second-level cache. */
  private void clearSessionAndSecondLevelCache() {
    clearSession();
    entityManagerFactory.getCache().evictAll();
  }

  private EntityPersister persister() {
    return entityManagerFactory
        .unwrap(SessionFactoryImplementor.class)
        .getMetamodel()
        .entityPersister(IncomingSms.class);
  }

  private static int propertyIndex(EntityPersister persister, String property) {
    return persister.getEntityMetamodel().getPropertyIndex(property);
  }

  private static IncomingSms sms(String uid, String originator, Date sentDate) {
    IncomingSms sms = new IncomingSms();
    sms.setUid(uid);
    sms.setOriginator(originator);
    sms.setText("Text of " + uid);
    sms.setGatewayId("gatewayOf" + originator);
    sms.setSentDate(sentDate);
    sms.setReceivedDate(sentDate);
    return sms;
  }
}
