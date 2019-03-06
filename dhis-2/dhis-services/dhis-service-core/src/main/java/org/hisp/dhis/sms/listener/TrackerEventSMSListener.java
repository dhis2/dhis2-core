package org.hisp.dhis.sms.listener;

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
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.smscompression.Consts.SubmissionType;
import org.hisp.dhis.smscompression.models.DataValue;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.TrackerEventSMSSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Transactional
public class TrackerEventSMSListener extends NewSMSListener {

	private static final Log log = LogFactory.getLog( TrackerEventSMSListener.class );

    @Autowired
    private UserService userService;
    
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
		
		TrackedEntityInstance tei = trackedEntityInstanceService
				.getTrackedEntityInstance(subm.getTrackedEntityInstance());
		
		if ( tei == null )
		{
			log.error("Given TEI ID cannot be found: " + subm.getTrackedEntityInstance());
			return;
		}

		ProgramInstance programInstance = null;
		ProgramStage programStage = null;
		
		for ( ProgramInstance pi : tei.getProgramInstances() )
		{
			for ( ProgramStage ps : pi.getProgram().getProgramStages() )
			{
				if ( ps.getUid().equals(subm.getProgramStage()) )
				{
					programInstance = pi;
					programStage = ps;
				}
			}
		}
		
		if ( programInstance == null || programStage == null )
		{
			log.error("In given TEI, cannot find Program Stage: " + subm.getProgramStage());
			return;			
		}
		
		OrganisationUnit ou = organisationUnitService.getOrganisationUnit(subm.getOrgUnit());
		User user = userService.getUser(subm.getUserID());
		CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo(subm.getAttributeOptionCombo());
		
		//TODO: Check whether this ou is valid for this program / user?
		
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        programStageInstance.setOrganisationUnit( ou );
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setExecutionDate( sms.getSentDate() );
        programStageInstance.setDueDate( sms.getSentDate() );
        programStageInstance
            .setAttributeOptionCombo( aoc );
        programStageInstance.setCompletedBy( user.getUsername() );
        programStageInstance.setStoredBy( user.getUsername() );

        Map<DataElement, EventDataValue> dataElementsAndEventDataValues = new HashMap<>();
        for ( DataValue dv : subm.getValues() )
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
		return ( type == SubmissionType.TRACKER_EVENT );
	}

}
