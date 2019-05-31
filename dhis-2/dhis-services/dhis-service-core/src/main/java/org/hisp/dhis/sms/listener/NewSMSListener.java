package org.hisp.dhis.sms.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.SMSSubmissionReader;
import org.hisp.dhis.smscompression.models.SMSDataValue;
import org.hisp.dhis.smscompression.models.SMSMetadata;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.SMSSubmissionHeader;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Transactional
public abstract class NewSMSListener extends BaseSMSListener {
	
    @Autowired
    private ProgramStageInstanceService programStageInstanceService;
	
    protected abstract SMSResponse postProcess( IncomingSms sms, SMSSubmission submission );
    protected abstract boolean handlesType( SubmissionType type );

	//TODO: Should this be here?
	public enum SMSResponse {
		SUCCESS(0, "Submission has been processed successfully"),
		UNKNOWN_ERROR(101, "An unknown error occurred"),
		INVALID_USER(201, "User [%s] does not exist"),
		INVALID_ORGUNIT(202, "Organisation unit [%s] does not exist"),
		INVALID_PROGRAM(203, "Program [%s] does not exist"),
		INVALID_TETYPE(204, "Tracked Entity Type [%s] does not exist"),
		INVALID_DATASET(205, "DataSet [%s] does not exist"),
		INVALID_PERIOD(206, "Period [%s] is invalid"),
		INVALID_AOC(207, "Attribute Option Combo [%s] does not exist"),
		INVALID_TEI(208, "Tracked Entity Instance [%s] does not exist"),
		INVALID_STAGE(209, "Program stage [%s] does not exist"),
		INVALID_EVENT(210, "Event [%s] does not exist"),
		INVALID_RELTYPE(211, "Relationship Type [%s] does not exist"),
		INVALID_ENROLL(212, "Enrollment [%s] does not exist"),
		INVALID_ATTRIB(213, "Attribute [%s] does not exist"),
		USER_NOTIN_OU(301, "User [%s] does not not belong to organisation unit [%s]"),
		OU_NOTIN_PROGRAM(302, "Organisation unit [%s] is not assigned to program [%s]"),
		OU_NOTIN_DATASET(303, "Organisation unit [%s] is not assigned to dataSet [%s]"),
		ENROLL_FAILED(304, "Enrollment of TEI [%s] in program [%s] failed"),
		DATASET_LOCKED(305, "Dataset [%s] is locked for period [%s]"),
		MULTI_PROGRAMS(306, "Multiple active program instances exists for program [%s]"),
		MULTI_STAGES(307, "Multiple program stages found for event capture program [%s]"),
		NO_ENROLL(308, "No enrollment was found for tracked entity instance [%s] in program stage [%s]"),
		NULL_ATTRIBVAL(309, "Value for attribute [%s] was null"),
		;
	
		private final int code;
		private String description;
		private List<String> uids;
	
		private SMSResponse(int code, String description) {
			this.code = code;
			this.description = description;
			this.uids = new ArrayList<String>();
		}
	
		public SMSResponse set(String... args) {
			this.uids = Arrays.asList(args);
			this.description = String.format(description, (Object [])args);
			return this;
		}
		
		public String getDescription() {
			return description;
		}
		
		public int getCode() {
			return code;
		}
		
		@Override
		public String toString() {
			String uidDelim = uids.stream().collect(Collectors.joining(","));
			return code + ":" + uidDelim + ":" + description;
		}
	}
		
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    @Autowired
    private UserService userService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataElementService dataElementService;

	@Override
	public boolean accept( IncomingSms sms ) {
        if ( sms == null || !SmsUtils.isBase64(sms) )
        {
            return false;
        }

        return handlesType(getHeader(sms).getType());
	}

	@Override
	public void receive( IncomingSms sms ) {
		SMSSubmissionHeader header = null;
		
		try {
			SMSSubmissionReader reader = new SMSSubmissionReader();
			header = reader.readHeader(SmsUtils.getBytes(sms));
			SMSMetadata meta = getMetadata(header.getLastSyncDate());
			SMSSubmission subm = reader.readSubmission(SmsUtils.getBytes(sms), meta);
						
			//TODO: Can be removed - debugging line to check SMS submissions
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			Log.info("New received SMS submission decoded as: " + gson.toJson(subm));
			
			checkUserAndOrgUnit(subm);
			SMSResponse resp = postProcess(sms, subm);
			
	        update(sms, SmsMessageStatus.PROCESSED, true);
	        sendSMSResponse(resp, sms.getOriginator(), header.getSubmissionID());
		} catch ( SMSProcessingException e ) {
			Log.error(e.getMessage());
			update(sms, SmsMessageStatus.FAILED, true);
			sendSMSResponse(e.getResp(), sms.getOriginator(), header.getSubmissionID());
		} catch ( Exception e ) {
			//Exceptions caught here will come from reading the header and submission
			//itself. We may not even have a submission ID to respond to the app with.
			//TODO: We can handle exceptions in read submission assuming we have a
			// 		valid header. We can't do anything about CRC checks though.
			Log.error(e.getMessage());
			update(sms, SmsMessageStatus.FAILED, true);
			if (header != null) {
				sendSMSResponse(SMSResponse.UNKNOWN_ERROR, sms.getOriginator(), header.getSubmissionID());
			}
		}
	}

	private void checkUserAndOrgUnit( SMSSubmission subm )
	{
		String ouid = subm.getOrgUnit();
		String userid = subm.getUserID();
        OrganisationUnit ou = organisationUnitService.getOrganisationUnit(ouid);
        User user = userService.getUser(userid);

        if (ou == null) {
        	throw new SMSProcessingException(SMSResponse.INVALID_ORGUNIT.set(ouid));
        } else if (user == null) {
        	throw new SMSProcessingException(SMSResponse.INVALID_USER.set(userid));
        } else if (!user.getOrganisationUnits().contains(ou)) {
        	throw new SMSProcessingException(SMSResponse.USER_NOTIN_OU.set(userid, ouid));
        }
	}
	
	private SMSSubmissionHeader getHeader( IncomingSms sms ) {
		byte[] smsBytes = SmsUtils.getBytes(sms);
		SMSSubmissionReader reader = new SMSSubmissionReader();
		try {
			return reader.readHeader(smsBytes);
		} catch ( Exception e ) {
			Log.error(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	public SMSMetadata getMetadata( Date lastSyncDate ) {
		SMSMetadata meta = new SMSMetadata();
		meta.dataElements = getAllDataElements(lastSyncDate);
		meta.categoryOptionCombos = getAllCatOptionCombos(lastSyncDate);
		meta.organisationUnits = getAllUserIds(lastSyncDate);
		meta.trackedEntityTypes = getAllTrackedEntityTypeIds(lastSyncDate);
		meta.trackedEntityAttributes = getAllTrackedEntityAttributeIds(lastSyncDate);
		meta.programs = getAllProgramIds(lastSyncDate);
		meta.organisationUnits = getAllOrgUnitIds(lastSyncDate);

		return meta;
	}

    public List<SMSMetadata.ID> getAllUserIds (Date lastSyncDate) {
        List<User> users = userService.getAllUsers();

        return users
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }

    public List<SMSMetadata.ID> getAllTrackedEntityTypeIds (Date lastSyncDate) {
        List<TrackedEntityType> teTypes = trackedEntityTypeService.getAllTrackedEntityType();

        return teTypes
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }

    public List<SMSMetadata.ID> getAllTrackedEntityAttributeIds (Date lastSyncDate) {
        List<TrackedEntityAttribute> teiAttributes = trackedEntityAttributeService.getAllTrackedEntityAttributes();

        return teiAttributes
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }

    public List<SMSMetadata.ID> getAllProgramIds (Date lastSyncDate) {
        List<Program> programs = programService.getAllPrograms();

        return programs
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }

    public List<SMSMetadata.ID> getAllOrgUnitIds (Date lastSyncDate) {
        List<OrganisationUnit> orgUnits = organisationUnitService.getAllOrganisationUnits();

        return orgUnits
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }

    public List<SMSMetadata.ID> getAllDataElements (Date lastSyncDate) {
        List<DataElement> dataElements = dataElementService.getAllDataElements();

        return dataElements
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }

    public List<SMSMetadata.ID> getAllCatOptionCombos (Date lastSyncDate) {
        List<CategoryOptionCombo> catOptionCombos = categoryService.getAllCategoryOptionCombos();

        return catOptionCombos
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }

    public SMSMetadata.ID getIdFromMetadata(IdentifiableObject obj, Date lastSyncDate) {
    	if ( obj.getCreated().after(lastSyncDate) ) {
    		return null;
    	} else {
			SMSMetadata.ID id = new SMSMetadata.ID(obj.getUid());
			return id;
    	}
    }

    protected void saveNewEvent(String eventUid, OrganisationUnit orgUnit, ProgramStage programStage, 
    		ProgramInstance programInstance, IncomingSms sms, CategoryOptionCombo aoc, User user,
    		List<SMSDataValue> values) {
    	
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        //If we aren't given a UID for the event, it will be auto-generated
        if (eventUid != null) programStageInstance.setUid( eventUid ); 
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
        for ( SMSDataValue dv : values )
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
}
