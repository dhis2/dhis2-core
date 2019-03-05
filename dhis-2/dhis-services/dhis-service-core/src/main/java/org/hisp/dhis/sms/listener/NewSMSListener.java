package org.hisp.dhis.sms.listener;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.Consts.SubmissionType;
import org.hisp.dhis.smscompression.SMSSubmissionReader;
import org.hisp.dhis.smscompression.models.Metadata;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.SMSSubmissionHeader;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public abstract class NewSMSListener extends BaseSMSListener {
	
	private static final Log log = LogFactory.getLog( NewSMSListener.class );
	static final String SUCCESS_MESSAGE = "Submission has been processed successfully";
	static final String NO_OU_FOR_PROGRAM = "Program is not assigned to organisation unit.";
	static final String NO_OU_FOR_USER = "User is not associated with organisation unit";
	static final String NO_TRACKED_ATTRIBUTES_FOUND = "No TrackedEntityAttributes found";
	
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
		try {
			SMSSubmissionReader reader = new SMSSubmissionReader();
			SMSSubmissionHeader header = reader.readHeader(SmsUtils.getBytes(sms));
			Metadata meta = getMetadata(header.getLastSyncDate());
			SMSSubmission submission = reader.readSubmission(SmsUtils.getBytes(sms), meta);
			postProcess(sms, submission);
		} catch ( Exception e ) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
    
	private SMSSubmissionHeader getHeader( IncomingSms sms ) {
		byte[] smsBytes = SmsUtils.getBytes(sms);
		SMSSubmissionReader reader = new SMSSubmissionReader();
		try {
			return reader.readHeader(smsBytes);
		} catch ( Exception e ) {
			log.error(e.getMessage());
			e.printStackTrace();
			return null;
		}
	}
	
	public Metadata getMetadata( Date lastSyncDate ) {
		Metadata meta = new Metadata();
		meta.dataElements = getAllDataElements(lastSyncDate);
		meta.categoryOptionCombos = getAllCatOptionCombos(lastSyncDate);
		meta.organisationUnits = getAllUserIds(lastSyncDate);
		meta.trackedEntityTypes = getAllTrackedEntityTypeIds(lastSyncDate);
		meta.trackedEntityAttributes = getAllTrackedEntityAttributeIds(lastSyncDate);
		meta.programs = getAllProgramIds(lastSyncDate);
		meta.organisationUnits = getAllOrgUnitIds(lastSyncDate);
				
		return meta;
	}

    public List<Metadata.ID> getAllUserIds (Date lastSyncDate) {
        List<User> users = userService.getAllUsers();
        
        return users
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }
    
    public List<Metadata.ID> getAllTrackedEntityTypeIds (Date lastSyncDate) {
        List<TrackedEntityType> teTypes = trackedEntityTypeService.getAllTrackedEntityType();
        
        return teTypes
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }
    
    public List<Metadata.ID> getAllTrackedEntityAttributeIds (Date lastSyncDate) {
        List<TrackedEntityAttribute> teiAttributes = trackedEntityAttributeService.getAllTrackedEntityAttributes();
        
        return teiAttributes
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }
        
    public List<Metadata.ID> getAllProgramIds (Date lastSyncDate) {
        List<Program> programs = programService.getAllPrograms();
        
        return programs
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }
    
    public List<Metadata.ID> getAllOrgUnitIds (Date lastSyncDate) {
        List<OrganisationUnit> orgUnits = organisationUnitService.getAllOrganisationUnits();
        
        return orgUnits
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }
    
    public List<Metadata.ID> getAllDataElements (Date lastSyncDate) {
        List<DataElement> dataElements = dataElementService.getAllDataElements();
        
        return dataElements
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }
    
    public List<Metadata.ID> getAllCatOptionCombos (Date lastSyncDate) {
        List<CategoryOptionCombo> catOptionCombos = categoryService.getAllCategoryOptionCombos();
        
        return catOptionCombos
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
				.filter(Objects::nonNull)
        		.collect(Collectors.toList());
    }    
    
    public Metadata.ID getIdFromMetadata(IdentifiableObject obj, Date lastSyncDate) {
    	if ( obj.getCreated().after(lastSyncDate) ) {
    		return null;
    	} else {
			return new Metadata.ID(obj.getUid());
    	}
    }
	
    protected abstract void postProcess( IncomingSms sms, SMSSubmission submission );
    protected abstract boolean handlesType( SubmissionType type );
	
}
