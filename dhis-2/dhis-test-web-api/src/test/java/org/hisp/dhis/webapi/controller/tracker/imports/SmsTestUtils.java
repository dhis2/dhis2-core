/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.SmsSubmissionWriter;
import org.hisp.dhis.smscompression.models.SmsMetadata;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.smscompression.models.Uid;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;

class SmsTestUtils {
  private static final int SMS_COMPRESSION_VERSION = 2;

  private SmsTestUtils() {
    throw new IllegalStateException("Utility class");
  }

  static String encodeSms(SmsSubmission submission) throws SmsCompressionException {
    SmsSubmissionWriter smsSubmissionWriter = new SmsSubmissionWriter(new SmsMetadata());
    byte[] compressedText = smsSubmissionWriter.compress(submission, SMS_COMPRESSION_VERSION);
    return Base64.getEncoder().encodeToString(compressedText);
  }

  /** Get the the incoming SMS stored in the DB with the id from the HTTP response body. */
  static IncomingSms getSms(IncomingSmsService incomingSmsService, JsonWebMessage response) {
    assertStartsWith("Received SMS: ", response.getMessage());

    String smsUid = response.getMessage().replaceFirst("^Received SMS: ", "");
    IncomingSms sms = incomingSmsService.get(smsUid);
    assertNotNull(sms, "failed to find SMS in DB with UID " + smsUid);
    return sms;
  }

  static void assertSmsResponse(
      String expectedText, String expectedRecipient, MessageSender messageSender) {
    OutboundMessage expectedMessage =
        new OutboundMessage(null, expectedText, Set.of(expectedRecipient));
    assertContainsOnly(List.of(expectedMessage), messageSender.getAllMessages());
  }

  static void assertEqualUids(Uid expected, IdentifiableObject actual) {
    assertEquals(expected.getUid(), actual.getUid());
  }

  static void assertEqualUids(IdentifiableObject expected, IdentifiableObject actual) {
    assertEquals(expected.getUid(), actual.getUid());
  }
}
