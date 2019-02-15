package org.hisp.dhis.sms.listener;

import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.parser.SmsMessage;
import org.hisp.dhis.smscompression.models.TrackerEventSMSSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;

public class TrackedEntitySMSListener extends AbstractSMSListener {

    private TrackedEntityInstanceService trackedEntityInstanceService;

    private TrackerEventSMSSubmission trackerEventSMSSubmission;

    @Override
    public boolean isAcceptable(SmsMessage message) {
        return false;
    }

    @Override
    protected void postProcess(IncomingSms sms) {

    }
}
