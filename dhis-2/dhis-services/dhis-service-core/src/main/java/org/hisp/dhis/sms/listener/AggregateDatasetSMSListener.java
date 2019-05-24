package org.hisp.dhis.sms.listener;

import java.util.Date;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.models.AggregateDatasetSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSDataValue;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

public class AggregateDatasetSMSListener extends NewSMSListener {

	private static final String DATASET_LOCKED = "Dataset: %s is locked for period: %s";
	
    @Autowired
    private OrganisationUnitService organisationUnitService;    
	
    @Autowired
    private DataSetService dataSetService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private DataValueService dataValueService;
    
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private CompleteDataSetRegistrationService registrationService;
    
	@Override
	protected void postProcess(IncomingSms sms, SMSSubmission submission) {
		AggregateDatasetSMSSubmission subm = ( AggregateDatasetSMSSubmission ) submission;

        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(subm.getOrgUnit());
        DataSet dataSet = dataSetService.getDataSet(subm.getDataSet());
        Period period = PeriodType.getPeriodFromIsoString(subm.getPeriod());
		User user = userService.getUser(subm.getUserID());
		CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo(subm.getAttributeOptionCombo());
        
        if ( dataSetService.isLocked( null, dataSet, period, orgUnit, aoc, null ) )
        {
            sendFeedback( String.format( DATASET_LOCKED, dataSet.getUid(), period.getName() ), sms.getOriginator(), ERROR );

            throw new SMSParserException( String.format( DATASET_LOCKED, dataSet.getUid(), period.getName() ) );
        }

        for (SMSDataValue smsdv : subm.getValues()) {
        	DataElement de = dataElementService.getDataElement(smsdv.getDataElement());
        	CategoryOptionCombo coc = categoryService.getCategoryOptionCombo(smsdv.getCategoryOptionCombo());
        	DataValue dv = dataValueService.getDataValue(de, period, orgUnit, coc, aoc);
        	
            boolean newDataValue = false;
            if ( dv == null )
            {
                dv = new DataValue();
                dv.setCategoryOptionCombo( coc );
                dv.setSource( orgUnit );
                dv.setDataElement( de );
                dv.setPeriod( period );
                dv.setComment( "" );
                newDataValue = true;
            }
            
            dv.setValue( smsdv.getValue() );
            dv.setLastUpdated( new java.util.Date() );
            dv.setStoredBy( user.getUsername() );
            
            if ( newDataValue )
            {
                dataValueService.addDataValue( dv );
            }
            else
            {
                dataValueService.updateDataValue( dv );
            }            
        }

        if (subm.isComplete()) {
        	CompleteDataSetRegistration existingReg = registrationService.getCompleteDataSetRegistration(dataSet, period, orgUnit, aoc);
        	if (existingReg != null) {
        		registrationService.deleteCompleteDataSetRegistration(existingReg);
        	}
        	Date now = new Date();
        	String username = user.getUsername();
        	CompleteDataSetRegistration newReg = new CompleteDataSetRegistration( dataSet, period, orgUnit, aoc, now, username, now, username, true );
        	registrationService.saveCompleteDataSetRegistration(newReg);
        }
        sendFeedback( SUCCESS_MESSAGE, sms.getOriginator(), INFO );

        update( sms, SmsMessageStatus.PROCESSED, true );
	}
	
	@Override
	protected boolean handlesType(SubmissionType type) {
		return ( type == SubmissionType.AGGREGATE_DATASET );
	}

}
