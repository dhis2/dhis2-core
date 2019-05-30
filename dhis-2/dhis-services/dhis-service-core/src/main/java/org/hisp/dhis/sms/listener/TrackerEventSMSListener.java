package org.hisp.dhis.sms.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.models.SMSDataValue;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.TrackerEventSMSSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class TrackerEventSMSListener extends NewSMSListener {

    @Autowired
    private UserService userService;

    @Autowired
    private ProgramStageService programStageService;
    
    @Autowired
    private ProgramStageInstanceService programStageInstanceService;
	
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;
    
    @Autowired
    private OrganisationUnitService organisationUnitService;    

    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private DataElementService dataElementService;    
    
	@Override
	protected void postProcess(IncomingSms sms, SMSSubmission submission) {
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
			throw new SMSProcessingException(SMSResponse.NO_PROGINST.set(teiid, stageid));
		}
		ProgramInstance programInstance = res.get();
				
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        //If we aren't given a UID for the event, it will be auto-generated
        if (subm.getEvent() != null) programStageInstance.setUid( subm.getEvent() ); 
        programStageInstance.setOrganisationUnit( orgUnit );
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setExecutionDate( sms.getSentDate() );
        programStageInstance.setDueDate( sms.getSentDate() );
        programStageInstance
            .setAttributeOptionCombo( aoc );
        programStageInstance.setCompletedBy( user.getUsername() );
        programStageInstance.setStoredBy( user.getUsername() );

        Map<DataElement, EventDataValue> dataElementsAndEventDataValues = new HashMap<>();
        for ( SMSDataValue dv : subm.getValues() )
        {
        	String deid = dv.getDataElement();
        	String val = dv.getValue(); 

        	DataElement de = dataElementService.getDataElement( deid );
        	if (de == null)
        	{
            	//TODO: We need to add this to the response
            	Log.warn("Given data element [" + deid + "] could not be found. Continuing with submission...");
            	continue;
        	} else if (val == null || StringUtils.isEmpty(val)) {
            	Log.warn("Value for atttribute [" + deid + "] is null or empty. Continuing with submission...");
            	continue;
            }

            EventDataValue eventDataValue = new EventDataValue( deid, dv.getValue(), user.getUsername() );
            eventDataValue.setAutoFields();
            dataElementsAndEventDataValues.put( de, eventDataValue );
        }

        programStageInstanceService.saveEventDataValuesAndSaveProgramStageInstance( programStageInstance, dataElementsAndEventDataValues );
	}
	
	
	@Override
	protected boolean handlesType(SubmissionType type) {
		return ( type == SubmissionType.TRACKER_EVENT );
	}

}
