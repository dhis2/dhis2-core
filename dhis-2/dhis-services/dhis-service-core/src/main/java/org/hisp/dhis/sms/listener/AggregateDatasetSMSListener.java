package org.hisp.dhis.sms.listener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.models.AggregateDatasetSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSDataValue;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;

public class AggregateDatasetSMSListener extends NewSMSListener {

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
	protected SMSResponse postProcess(IncomingSms sms, SMSSubmission submission) {
		AggregateDatasetSMSSubmission subm = ( AggregateDatasetSMSSubmission ) submission;

		String ouid = subm.getOrgUnit();
		String dsid = subm.getDataSet();
		String per = subm.getPeriod();
		String aocid = subm.getAttributeOptionCombo();
		
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(ouid);
		User user = userService.getUser(subm.getUserID());

        DataSet dataSet = dataSetService.getDataSet(dsid);
        if (dataSet == null) 
        {
        	throw new SMSProcessingException(SMSResponse.INVALID_DATASET.set(dsid));
        }
        
        Period period = PeriodType.getPeriodFromIsoString(per);
        if (period == null) 
        {
        	throw new SMSProcessingException(SMSResponse.INVALID_PERIOD.set(per));
        }
        
		CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo(aocid);
		if (aoc == null) {
			throw new SMSProcessingException(SMSResponse.INVALID_AOC.set(aocid));
		}
        
		//TODO: This seems a bit backwards, why is there no dataSet.hasOrganisationUnit?
		if (!orgUnit.getDataSets().contains(dataSet))
		{
			throw new SMSProcessingException(SMSResponse.OU_NOTIN_DATASET.set(ouid, dsid));
		}
		
        if ( dataSetService.isLocked( null, dataSet, period, orgUnit, aoc, null ) )
        {
        	throw new SMSProcessingException(SMSResponse.DATASET_LOCKED.set(dsid, per));
        }

        List<String> errorUIDs = submitDataValues(subm.getValues(), period, orgUnit, aoc, user);

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
        
		if (!errorUIDs.isEmpty())
		{
			return SMSResponse.WARN_DVERR.set(errorUIDs);
		} else if (subm.getValues().isEmpty()) {
			//TODO: Should we save if there are no data values?
			return SMSResponse.WARN_DVEMPTY;
		}
        
        return SMSResponse.SUCCESS;
	}
	
	private List<String> submitDataValues(List<SMSDataValue> values, Period period, OrganisationUnit orgUnit,
			CategoryOptionCombo aoc, User user) {
		ArrayList<String> errorUIDs = new ArrayList<>();
		
        for (SMSDataValue smsdv : values) {
        	String deid = smsdv.getDataElement();
        	String cocid = smsdv.getCategoryOptionCombo();
        	String combid = deid + "-" + cocid;
        	
        	DataElement de = dataElementService.getDataElement(deid);
        	if (de == null)
        	{
        		Log.warn("Data element [" + deid + "] does not exist. Continuing with submission...");
        		errorUIDs.add(combid);
        		continue;
        	}
        	
        	CategoryOptionCombo coc = categoryService.getCategoryOptionCombo(cocid);
        	if (coc == null)
        	{
        		Log.warn("Category Option Combo [" + cocid + "] does not exist. Continuing with submission...");
        		errorUIDs.add(combid);
        		continue;        		
        	}
        	
        	String val = smsdv.getValue();
        	if (val == null || StringUtils.isEmpty(val))
        	{
        		Log.warn("Value for [" + combid + "]  is null or empty. Continuing with submission...");
        		continue;
        	}
        	
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
            
            dv.setValue( val );
            dv.setLastUpdated( new java.util.Date() );
            dv.setStoredBy( user.getUsername() );
            
            if ( newDataValue )
            {
            	boolean addedDataValue = dataValueService.addDataValue( dv );
                if (!addedDataValue) {
             		Log.warn("Failed to submit data value [" + combid + "]. Continuing with submission...");
            		errorUIDs.add(combid);
                }
            }
            else
            {
                dataValueService.updateDataValue( dv );
            }            
        }
        
        return errorUIDs;
	}
	
	@Override
	protected boolean handlesType(SubmissionType type) {
		return ( type == SubmissionType.AGGREGATE_DATASET );
	}

}
