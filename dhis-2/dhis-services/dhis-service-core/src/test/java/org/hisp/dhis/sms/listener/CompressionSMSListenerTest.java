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
package org.hisp.dhis.sms.listener;

import java.util.Base64;
import java.util.Date;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.SmsSubmissionWriter;
import org.hisp.dhis.smscompression.models.SmsMetadata;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.user.User;

abstract class CompressionSMSListenerTest extends DhisConvenienceTest {
  protected static final String SUCCESS_MESSAGE = "1:0::Submission has been processed successfully";

  protected static final String NOVALUES_MESSAGE =
      "1:2::The submission did not include any data values";

  protected static final String NOATTRIBS_MESSAGE =
      "1:3::The submission did not include any attribute values";

  protected static final String ORIGINATOR = "47400000";

  protected static final String ATTRIBUTE_VALUE = "TEST";

  protected IncomingSms createSMSFromSubmission(SmsSubmission subm) throws SmsCompressionException {
    User user = createUser('U');
    SmsMetadata meta = new SmsMetadata();
    meta.lastSyncDate = new Date();
    SmsSubmissionWriter writer = new SmsSubmissionWriter(meta);
    String smsText = Base64.getEncoder().encodeToString(writer.compress(subm));

    IncomingSms incomingSms = new IncomingSms();
    incomingSms.setText(smsText);
    incomingSms.setOriginator(ORIGINATOR);
    incomingSms.setCreatedBy(user);

    return incomingSms;
  }
}
