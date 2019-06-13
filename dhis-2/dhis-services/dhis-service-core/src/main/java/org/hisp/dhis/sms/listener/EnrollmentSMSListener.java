package org.hisp.dhis.sms.listener;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.models.EnrollmentSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSAttributeValue;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class EnrollmentSMSListener extends NewSMSListener {
    @Autowired
    private TrackedEntityInstanceService teiService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Override
    protected SMSResponse postProcess(IncomingSms sms, SMSSubmission submission) {
        EnrollmentSMSSubmission subm = (EnrollmentSMSSubmission) submission;

        Date submissionTimestamp = subm.getTimestamp();
        String teiUID = subm.getTrackedEntityInstance();
        String progid = subm.getTrackerProgram();
        String tetid = subm.getTrackedEntityType();
        String ouid = subm.getOrgUnit();
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(ouid);
        
        Program program = programService.getProgram(progid);
        if (program == null)
        {
        	throw new SMSProcessingException(SMSResponse.INVALID_PROGRAM.set(progid));
        }
        
        TrackedEntityType entityType = trackedEntityTypeService.getTrackedEntityType(tetid);
        if (entityType == null) 
        {
        	throw new SMSProcessingException(SMSResponse.INVALID_TETYPE.set(tetid));
        }

        if (!program.hasOrganisationUnit(orgUnit)) 
        {
        	throw new SMSProcessingException(SMSResponse.OU_NOTIN_PROGRAM.set(ouid, progid));
        }

        TrackedEntityInstance entityInstance;
        boolean existsOnServer = teiService.trackedEntityInstanceExists(teiUID);

        if (existsOnServer) {
        	Log.info("Given TEI (" + teiUID + ") exists. Updating...");
            entityInstance = teiService.getTrackedEntityInstance(teiUID);
        } else {
        	Log.info("Given TEI (" + teiUID + ") does not exist. Creating...");
            entityInstance = new TrackedEntityInstance();
            entityInstance.setUid(teiUID);
            entityInstance.setOrganisationUnit(orgUnit);
            entityInstance.setTrackedEntityType(entityType);
        }

        Set<TrackedEntityAttributeValue> attributeValues = getSMSAttributeValues(subm, entityInstance);

        if (existsOnServer) {
            entityInstance.setTrackedEntityAttributeValues(attributeValues);
            teiService.updateTrackedEntityInstance(entityInstance);
        } else {
        	teiService.createTrackedEntityInstance( entityInstance, attributeValues );
        }

        TrackedEntityInstance tei = teiService.getTrackedEntityInstance(teiUID);

        Date enrollmentDate = new Date();
        //TODO: Should we check if the TEI is already enrolled? If so do we skip this?
        ProgramInstance enrollment = programInstanceService.enrollTrackedEntityInstance(tei, program, enrollmentDate, submissionTimestamp, orgUnit);
        if (enrollment == null) {
        	throw new SMSProcessingException(SMSResponse.ENROLL_FAILED.set(teiUID,progid));
        }
        
        if (attributeValues.isEmpty()) {
        	//TODO: Is this correct handling?
            return SMSResponse.WARN_AVEMPTY;
        }
        
        return SMSResponse.SUCCESS;
    }

    @Override
    protected boolean handlesType(SubmissionType type) {
        return (type == SubmissionType.ENROLLMENT);
    }

    private Set<TrackedEntityAttributeValue> getSMSAttributeValues(EnrollmentSMSSubmission submission, TrackedEntityInstance entityInstance) {
        return submission.getValues()
                .stream()
                .map(v -> createTrackedEntityValue(v, entityInstance))
                .collect(Collectors.toSet());
    }

    private TrackedEntityAttributeValue createTrackedEntityValue(SMSAttributeValue SMSAttributeValue, TrackedEntityInstance tei) {
    	String attribUID = SMSAttributeValue.getAttribute();
        String val = SMSAttributeValue.getValue();

        TrackedEntityAttribute attribute = trackedEntityAttributeService.getTrackedEntityAttribute(attribUID);
        if (attribute == null) {
        	throw new SMSProcessingException(SMSResponse.INVALID_ATTRIB.set(attribUID));
        } else if (val == null) {
        	//TODO: Is this an error we can't recover from?
        	throw new SMSProcessingException(SMSResponse.NULL_ATTRIBVAL.set(attribUID));
        }
        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setAttribute(attribute);
        trackedEntityAttributeValue.setEntityInstance(tei);
        trackedEntityAttributeValue.setValue(val);
        return trackedEntityAttributeValue;
    }
}