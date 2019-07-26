package org.hisp.dhis.sms.listener;

import java.util.Base64;
import java.util.Date;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.SMSCompressionException;
import org.hisp.dhis.smscompression.SMSSubmissionWriter;
import org.hisp.dhis.smscompression.models.SMSMetadata;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.user.User;

public class NewSMSListenerTest
    extends
    DhisConvenienceTest
{
    protected static final String SUCCESS_MESSAGE = "1:0::Submission has been processed successfully";

    protected static final String ORIGINATOR = "47400000";

    protected static final String ATTRIBUTE_VALUE = "TEST";

    protected IncomingSms createSMSFromSubmission( SMSSubmission subm )
        throws SMSCompressionException
    {
        User user = createUser( 'U' );
        SMSMetadata meta = new SMSMetadata();
        meta.lastSyncDate = new Date();
        SMSSubmissionWriter writer = new SMSSubmissionWriter( meta );
        String smsText = Base64.getEncoder().encodeToString( writer.compress( subm ) );

        IncomingSms incomingSms = new IncomingSms();
        incomingSms.setText( smsText );
        incomingSms.setOriginator( ORIGINATOR );
        incomingSms.setUser( user );

        return incomingSms;
    }

}