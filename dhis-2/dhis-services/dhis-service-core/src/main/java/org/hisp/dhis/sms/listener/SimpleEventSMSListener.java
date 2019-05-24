package org.hisp.dhis.sms.listener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.models.SMSDataValue;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.SimpleEventSMSSubmission;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

public class SimpleEventSMSListener extends NewSMSListener {

	private static final Log log = LogFactory.getLog( SimpleEventSMSListener.class );
	
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
		
		Program program = programService.getProgram(subm.getEventProgram());
		if ( program == null ) {
			log.error("No program found for given UID: " + subm.getEventProgram());
			return;
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
            update( sms, SmsMessageStatus.FAILED, false );

            sendFeedback( "Multiple active program instances exists for program: " + program.getUid(),
                sms.getOriginator(), ERROR );

            return;
        }
        
		ProgramInstance programInstance = programInstances.get( 0 );
		Set<ProgramStage> programStages = programInstance.getProgram().getProgramStages();
		if ( programStages.size() > 1 ) {
			log.error("Multiple program stages found for program: " + subm.getEventProgram());
			return;
		}
		ProgramStage programStage = programStages.iterator().next();
		
		OrganisationUnit ou = organisationUnitService.getOrganisationUnit(subm.getOrgUnit());
		User user = userService.getUser(subm.getUserID());
		CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo(subm.getAttributeOptionCombo());
		
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setUid( subm.getEvent() );
        programStageInstance.setOrganisationUnit( ou );
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
        	DataElement de = dataElementService.getDataElement( dv.getDataElement() );
            EventDataValue eventDataValue = new EventDataValue( dv.getDataElement(), dv.getValue(), user.getUsername() );
            eventDataValue.setAutoFields();

            //Filter empty values out -> this is "adding/saving/creating", therefore, empty values are ignored
            if ( !StringUtils.isEmpty( eventDataValue.getValue() ))
            {
                dataElementsAndEventDataValues.put( de, eventDataValue );
            }
        }

        programStageInstanceService.saveEventDataValuesAndSaveProgramStageInstance( programStageInstance, dataElementsAndEventDataValues );

        update( sms, SmsMessageStatus.PROCESSED, true );

        sendFeedback( SUCCESS_MESSAGE, sms.getOriginator(), INFO );
	}

	@Override
	protected boolean handlesType(SubmissionType type) {
		return ( type == SubmissionType.SIMPLE_EVENT );
	}

}
