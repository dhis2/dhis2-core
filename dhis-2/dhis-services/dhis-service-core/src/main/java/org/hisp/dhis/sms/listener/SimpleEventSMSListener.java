package org.hisp.dhis.sms.listener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.models.SMSDataValue;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.SimpleEventSMSSubmission;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;

public class SimpleEventSMSListener extends NewSMSListener {
	
    @Autowired
    private UserService userService;
	
	@Autowired
	private ProgramService programService;
    
	@Autowired
	private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;
    
    @Autowired
    private OrganisationUnitService organisationUnitService;    

    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private DataElementService dataElementService;
    
	@Override
	protected void postProcess(IncomingSms sms, SMSSubmission submission) {
		SimpleEventSMSSubmission subm = ( SimpleEventSMSSubmission ) submission;
		
		String ouid = subm.getOrgUnit();
		String aocid = subm.getAttributeOptionCombo();
		String progid = subm.getEventProgram();
		
		OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(ouid);
		User user = userService.getUser(subm.getUserID());
		
		Program program = programService.getProgram(subm.getEventProgram());
        if (program == null)
        {
        	throw new SMSProcessingException(SMSResponse.INVALID_PROGRAM.set(progid));
        }
        
		CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo(aocid);
		if (aoc == null) {
			throw new SMSProcessingException(SMSResponse.INVALID_AOC.set(aocid));
		}
		
        if (!program.hasOrganisationUnit(orgUnit)) 
        {
        	throw new SMSProcessingException(SMSResponse.OU_NOTIN_PROGRAM.set(ouid, progid));
        }
		
        List<ProgramInstance> programInstances = new ArrayList<>(
                programInstanceService.getProgramInstances( program, ProgramStatus.ACTIVE ) );
		
        // For Simple Events, the Program should have one Program Instance
        // If it doesn't exist, this is the first event, we can create it here
        if ( programInstances.isEmpty() )
        {
            ProgramInstance pi = new ProgramInstance();
            pi.setEnrollmentDate( new Date() );
            pi.setIncidentDate( new Date() );
            pi.setProgram( program );
            pi.setStatus( ProgramStatus.ACTIVE );

            programInstanceService.addProgramInstance( pi );

            programInstances.add( pi );
        }
        else if ( programInstances.size() > 1 )
        {
        	//TODO: Are we sure this is a problem we can't recover from?
        	throw new SMSProcessingException(SMSResponse.MULTI_PROGRAMS.set(progid));
        }
        
		ProgramInstance programInstance = programInstances.get( 0 );
		Set<ProgramStage> programStages = programInstance.getProgram().getProgramStages();
		if ( programStages.size() > 1 ) 
		{
			throw new SMSProcessingException(SMSResponse.MULTI_STAGES.set(progid));
		}
		ProgramStage programStage = programStages.iterator().next();
				
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
        programStageInstance.setCompletedBy( "DHIS 2" );
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
		return ( type == SubmissionType.SIMPLE_EVENT );
	}

}
