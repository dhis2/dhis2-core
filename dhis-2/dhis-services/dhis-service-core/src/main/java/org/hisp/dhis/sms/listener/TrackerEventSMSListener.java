package org.hisp.dhis.sms.listener;

import java.util.Optional;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.TrackerEventSMSSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TrackerEventSMSListener extends NewSMSListener {

    @Autowired
    private UserService userService;

    @Autowired
    private ProgramStageService programStageService;
    	
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;
    
    @Autowired
    private OrganisationUnitService organisationUnitService;    

    @Autowired
    private CategoryService categoryService;
    
	@Override
	protected SMSResponse postProcess(IncomingSms sms, SMSSubmission submission) {
		TrackerEventSMSSubmission subm = ( TrackerEventSMSSubmission ) submission;
		
		String ouid = subm.getOrgUnit();
		String stageid = subm.getProgramStage();
		String teiid = subm.getTrackedEntityInstance();
		String aocid = subm.getAttributeOptionCombo();
		
		OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(ouid);
		User user = userService.getUser(subm.getUserID());
		
		TrackedEntityInstance tei = trackedEntityInstanceService
				.getTrackedEntityInstance(teiid);
		if (tei == null)
		{
			throw new SMSProcessingException(SMSResponse.INVALID_TEI.set(teiid));
		}
		
		ProgramStage programStage = programStageService.getProgramStage(stageid);
		if (programStage == null)
		{
			throw new SMSProcessingException(SMSResponse.INVALID_STAGE.set(teiid));
		}
		
		CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo(aocid);
		if (aoc == null) {
			throw new SMSProcessingException(SMSResponse.INVALID_AOC.set(aocid));
		}
		
		
		Optional<ProgramInstance> res = tei.getProgramInstances()
				.stream()
				.filter(pi -> pi.getStatus() == ProgramStatus.ACTIVE)
				.filter(pi -> pi.getProgram().getProgramStages().contains(programStage))
				.findFirst();
				
		if (!res.isPresent())
		{
			throw new SMSProcessingException(SMSResponse.NO_ENROLL.set(teiid, stageid));
		}
		ProgramInstance programInstance = res.get();
				
		saveNewEvent(subm.getEvent(), orgUnit, programStage, programInstance, sms, aoc, user, subm.getValues());
		
		return SMSResponse.SUCCESS;
	}
	
	
	@Override
	protected boolean handlesType(SubmissionType type) {
		return ( type == SubmissionType.TRACKER_EVENT );
	}

}
