package org.hisp.dhis.sms.listener;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.hisp.dhis.common.IdentifiableObject;
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
		SMSSubmissionReader reader = new SMSSubmissionReader();
		SMSSubmissionHeader header = getHeader(sms);
		Metadata meta = getMetadata(header.getLastSyncDate());

		try {
			SMSSubmission submission = reader.readSubmission(header, meta);
			postProcess(sms, submission);
		} catch ( Exception e ) {
			//TODO: Handle exception
		}
	}
    
	private SMSSubmissionHeader getHeader( IncomingSms sms ) {
		byte[] smsBytes = SmsUtils.getBytes(sms);
		SMSSubmissionReader reader = new SMSSubmissionReader();
		try {
			SMSSubmissionHeader header = reader.readHeader(smsBytes);
			return header;
		} catch ( Exception e ) {
			//TODO: Handle exception
			return null;
		}
	}
	
	public Metadata getMetadata( Date lastSyncDate ) {
		Metadata meta = new Metadata();
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
        		.filter(o -> o != null)
        		.collect(Collectors.toList());
    }
    
    public List<Metadata.ID> getAllTrackedEntityTypeIds (Date lastSyncDate) {
        List<TrackedEntityType> teTypes = trackedEntityTypeService.getAllTrackedEntityType();
        
        return teTypes
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
        		.filter(o -> o != null)
        		.collect(Collectors.toList());
    }
    
    public List<Metadata.ID> getAllTrackedEntityAttributeIds (Date lastSyncDate) {
        List<TrackedEntityAttribute> teiAttributes = trackedEntityAttributeService.getAllTrackedEntityAttributes();
        
        return teiAttributes
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
        		.filter(o -> o != null)
        		.collect(Collectors.toList());
    }
        
    public List<Metadata.ID> getAllProgramIds (Date lastSyncDate) {
        List<Program> programs = programService.getAllPrograms();
        
        return programs
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
        		.filter(o -> o != null)
        		.collect(Collectors.toList());
    }
    
    public List<Metadata.ID> getAllOrgUnitIds (Date lastSyncDate) {
        List<OrganisationUnit> orgUnits = organisationUnitService.getAllOrganisationUnits();
        
        return orgUnits
        		.stream()
        		.map(o -> getIdFromMetadata(o, lastSyncDate))
        		.filter(o -> o != null)
        		.collect(Collectors.toList());
    }
    
    public Metadata.ID getIdFromMetadata(IdentifiableObject obj, Date lastSyncDate) {
    	if ( obj.getCreated().after(lastSyncDate) ) {
    		return null;
    	} else {
    		Metadata.ID id = new Metadata.ID(obj.getUid());
    		return id;
    	}
    }
	
    protected abstract void postProcess( IncomingSms sms, SMSSubmission submission );
    protected abstract boolean handlesType( SubmissionType type );
	
}
