package org.hisp.dhis.sms.listener;

import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.models.DeleteSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.springframework.beans.factory.annotation.Autowired;

public class DeleteEventSMSListener extends NewSMSListener {

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;
	
	@Override
	protected SMSResponse postProcess(IncomingSms sms, SMSSubmission submission) {
		DeleteSMSSubmission subm = ( DeleteSMSSubmission ) submission;

		String eventid = subm.getUid();
		ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance(eventid);
		if (psi == null)
		{
			throw new SMSProcessingException(SMSResponse.INVALID_EVENT.set(eventid));
		}
		
		programStageInstanceService.deleteProgramStageInstance(psi);
		
		return SMSResponse.SUCCESS;
	}

	@Override
	protected boolean handlesType(SubmissionType type) {
		return ( type == SubmissionType.DELETE );
	}

}
