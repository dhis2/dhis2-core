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
package org.hisp.dhis.system.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Robert White <robert.white.13@ucl.ac.uk>
 */
class SmsUtilsTest {

  private User userA;

  private User userB;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private OrganisationUnit organisationUnitC;

  private IncomingSms incomingSms;

  private SMSCommand keyValueCommand;

  private String phoneNumber;

  private String email;

  @BeforeEach
  void setup() {
    phoneNumber = "0123456789";
    email = "test@example.com";
    userA = new User();
    userA.setAutoFields();
    userA.setPhoneNumber(phoneNumber);
    userA.setEmail(email);
    organisationUnitA = new OrganisationUnit();
    organisationUnitA.setAutoFields();
    organisationUnitA.setId(1);
    organisationUnitA.setCode("TESTORGA");
    userA.addOrganisationUnit(organisationUnitA);
    userB = new User();
    organisationUnitB = new OrganisationUnit();
    organisationUnitB.setAutoFields();
    organisationUnitB.setId(2);
    organisationUnitB.setCode("TESTORGB");
    userB.addOrganisationUnit(organisationUnitB);
    organisationUnitC = new OrganisationUnit();
    organisationUnitC.setAutoFields();
    organisationUnitC.setId(3);
    organisationUnitC.setCode("TESTORGC");
    incomingSms = new IncomingSms();
    incomingSms.setCreatedBy(userA);
    keyValueCommand = new SMSCommand();
    keyValueCommand.setParserType(ParserType.KEY_VALUE_PARSER);
  }

  @Test
  void testGetCommandStringWithSms() {
    incomingSms = new IncomingSms();
    incomingSms.setText("000testcommandstring");
    assertEquals("testcommandstring", SmsUtils.getCommandString(incomingSms));
  }

  @Test
  void testGetCommandStringWithText() {
    assertEquals("testcommandstring", SmsUtils.getCommandString("000testcommandstring"));
  }

  @Test
  void testIsBase64() {
    incomingSms = new IncomingSms();
    incomingSms.setText("0b976c484577437bbba6794d0e7ebde0");
    assertTrue(SmsUtils.isBase64(incomingSms));
    incomingSms.setText("50be58982413465f91b9aced950fc3ab");
    assertTrue(SmsUtils.isBase64(incomingSms));
    incomingSms.setText("Jjg3j3-412-1435-342-jajg8234f");
    assertFalse(SmsUtils.isBase64(incomingSms));
    incomingSms.setText("6cafdc73_2ca4_4c52-8a0a-d38adec33b24");
    assertFalse(SmsUtils.isBase64(incomingSms));
    incomingSms.setText("e1809673dbf3482d8f84e493c65f74d9");
    assertTrue(SmsUtils.isBase64(incomingSms));
  }

  @Test
  void testGetBytes() {
    incomingSms.setText("0b976c484577437bbba6794d0e7ebde0");
    assertArrayEquals(
        Base64.getDecoder().decode("0b976c484577437bbba6794d0e7ebde0"),
        SmsUtils.getBytes(incomingSms));
    incomingSms.setText("50be58982413465f91b9aced950fc3ab");
    assertArrayEquals(
        Base64.getDecoder().decode("50be58982413465f91b9aced950fc3ab"),
        SmsUtils.getBytes(incomingSms));
    incomingSms.setText("e1809673dbf3482d8f84e493c65f74d9");
    assertArrayEquals(
        Base64.getDecoder().decode("e1809673dbf3482d8f84e493c65f74d9"),
        SmsUtils.getBytes(incomingSms));
  }

  @Test
  void testGetOrganisationUnitsByPhoneNumber() {
    Collection<User> params = Collections.singleton(userA);
    Map<String, Set<OrganisationUnit>> expected =
        ImmutableMap.of(userA.getUid(), ImmutableSet.of(organisationUnitA));
    assertEquals(expected, SmsUtils.getOrganisationUnitsByPhoneNumber("sender", params));
  }

  @Test
  void testLookForDate() throws ParseException {
    GregorianCalendar gc = new GregorianCalendar(2019, 12, 31);
    SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
    Date reference = format.parse("31/12/2019");
    if (reference.equals(gc.getTime())) {
      // test 1...
      Date d = SmsUtils.lookForDate("000 3112 XXX");
      assertEquals(reference, d);
    }
    // test 2...
    Date d = SmsUtils.lookForDate("000 3011 XXX");
    assertNotEquals(reference, d);
  }

  @Test
  void testGetUser() {
    User returnedUser = SmsUtils.getUser("", new SMSCommand(), Lists.newArrayList(userA));
    assertEquals(userA, returnedUser);
    assertThrows(
        SMSParserException.class,
        () -> SmsUtils.getUser("", new SMSCommand(), Lists.newArrayList(userA, userB)));
  }

  @Test
  void testSplitLongUnicodeString() {
    List<String> result = new ArrayList<>();
    assertEquals(
        Lists.newArrayList(
            "000000000000000000000000000000000000000000000000000000000000000000red-green-blue",
            "red.green.blue000000000000000000000000000000000000000000000000000000000000000000"),
        SmsUtils.splitLongUnicodeString(
            "000000000000000000000000000000000000000000000000000000000000000000red-green-blue red.green.blue"
                + "000000000000000000000000000000000000000000000000000000000000000000",
            result));
    result = new ArrayList<>();
    assertEquals(
        Lists.newArrayList(
            "000000000000000000000000000000000000000000000000000000000000000000red-green-blue",
            "red.green.blue000000000000000000000000000000000000000000000000000000000000000000",
            "000000000000000000000000000000000000000000000000000000000000000000red.green.blue"),
        SmsUtils.splitLongUnicodeString(
            "000000000000000000000000000000000000000000000000000000000000000000red-green-blue red.green.blue"
                + "000000000000000000000000000000000000000000000000000000000000000000 "
                + "000000000000000000000000000000000000000000000000000000000000000000red.green.blue",
            result));
  }

  @Test
  void testGetRecipientsPhoneNumber() {
    assertTrue(SmsUtils.getRecipientsPhoneNumber(Lists.newArrayList(userA)).contains(phoneNumber));
  }

  @Test
  void testGetRecipientsEmail() {
    assertTrue(SmsUtils.getRecipientsEmail(Lists.newArrayList(userA)).contains(email));
  }

  @Test
  void testSelectOrganisationUnit() {
    OrganisationUnit expected = organisationUnitA;
    Map<String, String> parsedMessage = Maps.newHashMap(ImmutableMap.of("ORG", "TESTORGA"));
    SMSCommand smsCommand = new SMSCommand();
    assertEquals(
        expected,
        SmsUtils.selectOrganisationUnit(
            Lists.newArrayList(organisationUnitA), parsedMessage, smsCommand));
    assertThrows(
        SMSParserException.class,
        () ->
            SmsUtils.selectOrganisationUnit(
                Lists.newArrayList(organisationUnitB, organisationUnitC),
                parsedMessage,
                smsCommand));
  }

  @Test
  void testRemovePhoneNumberPrefix() {
    assertEquals(phoneNumber, SmsUtils.removePhoneNumberPrefix("00" + phoneNumber));
    assertEquals(phoneNumber, SmsUtils.removePhoneNumberPrefix("+" + phoneNumber));
  }

  @Test
  void testSMSTextEncoding() {
    assertEquals("Hi+User", SmsUtils.encode("Hi User"));
    assertEquals("Jeg+er+p%C3%A5+universitetet", SmsUtils.encode("Jeg er på universitetet"));
    assertEquals("endelig+oppn%C3%A5+m%C3%A5let", SmsUtils.encode("endelig oppnå målet"));
    assertEquals("%D8%B4%D9%83%D8%B1%D8%A7+%D9%84%D9%83%D9%85", SmsUtils.encode("شكرا لكم"));
    assertEquals(" ", SmsUtils.encode(" "));
    assertNull(SmsUtils.encode(null));
  }
}
