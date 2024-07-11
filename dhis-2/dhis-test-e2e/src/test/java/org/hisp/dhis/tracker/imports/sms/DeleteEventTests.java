package org.hisp.dhis.tracker.imports.sms;

import org.aeonbits.owner.util.Base64;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.SmsSubmissionWriter;
import org.hisp.dhis.smscompression.models.DeleteSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsMetadata;
import org.junit.jupiter.api.Test;

public class DeleteEventTests {
  public static final int SMS_COMPRESSION_VERSION = 2;

  @Test
  void foo() throws SmsCompressionException {
    DeleteSmsSubmission submission = new DeleteSmsSubmission();
    submission.setSubmissionId(1);
    submission.setUserId("RXL3lPSK8oG");
    submission.setEvent("vI2csg55S9C");

    SmsMetadata smsMetadata = new SmsMetadata();
    SmsSubmissionWriter smsSubmissionWriter = new SmsSubmissionWriter(smsMetadata);
    byte[] compressedText = smsSubmissionWriter.compress(submission, SMS_COMPRESSION_VERSION);
    System.out.println(compressedText);
    String text = Base64.encode(compressedText);
    System.out.println(text);
  }
}
