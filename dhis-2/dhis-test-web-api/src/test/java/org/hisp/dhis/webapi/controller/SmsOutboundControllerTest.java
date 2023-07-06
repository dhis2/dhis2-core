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

import org.hisp.dhis.sms.outbound.OutboundSms;
import org.hisp.dhis.sms.outbound.OutboundSmsService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.sms.SmsOutboundController} using (mocked) REST
 * requests.
 *
 * @author Jan Bernitt
 */
class SmsOutboundControllerTest extends DhisControllerConvenienceTest {

  @Autowired private OutboundSmsService outboundSmsService;

  @Test
  void testSendSMSMessage() {
    assertWebMessage(
        "Internal Server Error",
        500,
        "ERROR",
        "No default gateway configured",
        POST("/sms/outbound?recipient=" + getSuperuserUid() + "&message=text")
            .content(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void testSendSMSMessage_NoRecipient() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Recipient must be specified",
        POST("/sms/outbound?recipient=&message=text").content(HttpStatus.CONFLICT));
  }

  @Test
  void testSendSMSMessage_NoMessage() {
    assertWebMessage(
        "Conflict",
        409,
        "ERROR",
        "Message must be specified",
        POST("/sms/outbound?recipient=xyz&message=").content(HttpStatus.CONFLICT));
  }

  @Test
  void testSendSMSMessageWithBody() {
    assertWebMessage(
        "Internal Server Error",
        500,
        "ERROR",
        "No default gateway configured",
        POST(
                "/sms/outbound",
                "{"
                    + "'recipients':[{'id':'"
                    + getSuperuserUid()
                    + "'}],"
                    + "'message':'text'"
                    + "}")
            .content(HttpStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void testDeleteOutboundMessage() {
    OutboundSms sms = new OutboundSms();
    outboundSmsService.save(sms);
    assertWebMessage(
        "OK",
        200,
        "OK",
        "OutboundSms with " + sms.getUid() + " deleted",
        DELETE("/sms/outbound/" + sms.getUid()).content(HttpStatus.OK));
  }

  @Test
  void testDeleteOutboundMessage_NoSuchObject() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "No OutboundSms with id 'xyz' was found.",
        DELETE("/sms/outbound/xyz").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testDeleteOutboundMessages() {
    OutboundSms sms = new OutboundSms();
    outboundSmsService.save(sms);
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Objects deleted",
        DELETE("/sms/outbound/?ids=" + sms.getUid()).content(HttpStatus.OK));
  }
}
