package org.hisp.dhis.sms.listener;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.smscompression.Consts;
import org.hisp.dhis.smscompression.models.AttributeValue;
import org.hisp.dhis.smscompression.models.EnrollmentSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.trackedentity.*;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
public class EnrollmentSMSListener extends NewSMSListener {
    @Autowired
    private TrackedEntityInstanceService teiService;

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
    private ProgramInstanceService programInstanceService;

    @Override
    protected void postProcess(IncomingSms sms, SMSSubmission submission) {
        EnrollmentSMSSubmission subm = (EnrollmentSMSSubmission) submission;

        Date submTimestamp = subm.getTimestamp();
        String sender = StringUtils.replace(sms.getOriginator(), "+", "");
        Program program = programService.getProgram(subm.getTrackerProgram());
        OrganisationUnit ou = organisationUnitService.getOrganisationUnit(subm.getOrgUnit());
        TrackedEntityType entityType = trackedEntityTypeService.getTrackedEntityType(subm.getTrackedEntityType());
        User user = userService.getUser(subm.getUserID());

        if (!program.hasOrganisationUnit(ou)) {
            sendFeedback(NO_OU_FOR_PROGRAM, sender, WARNING);
            //TODO: Change the exception with one better suited
            throw new SMSParserException(NO_OU_FOR_PROGRAM);
        }

        if (!userHasOrgUnit(user, ou)) {
            sendFeedback(NO_OU_FOR_USER, sender, WARNING);
            throw new SMSParserException(NO_OU_FOR_USER);
        }

        TrackedEntityInstance entityInstance = new TrackedEntityInstance();
        entityInstance.setOrganisationUnit(ou);
        entityInstance.setTrackedEntityType(entityType);

        Set<TrackedEntityAttributeValue> attributeValues = subm.getValues()
                .stream()
                .filter(this::isValidAttribute)
                .map(v -> createTrackedEntityValue(v, entityInstance))
                .collect(Collectors.toSet());

        if (attributeValues.isEmpty()) {
            sendFeedback(NO_TRACKED_ATTRIBUTES_FOUND, sender, WARNING);
            // TODO: Should we throw an error here or continue as is?
        }

        entityInstance.setTrackedEntityAttributeValues(attributeValues);
        int teiID = teiService.addTrackedEntityInstance(entityInstance);

        TrackedEntityInstance tei = teiService.getTrackedEntityInstance(teiID);

        Date enrollmentDate = new Date();
        programInstanceService.enrollTrackedEntityInstance(tei, program, enrollmentDate, submTimestamp, ou);

        update(sms, SmsMessageStatus.PROCESSED, true);
        sendFeedback(SUCCESS_MESSAGE, sender, INFO);
    }

    @Override
    protected boolean handlesType(Consts.SubmissionType type) {
        return (type == Consts.SubmissionType.ENROLLMENT);
    }

    private TrackedEntityAttributeValue createTrackedEntityValue(AttributeValue attributeValue, TrackedEntityInstance tei) {
        TrackedEntityAttribute attribute = trackedEntityAttributeService.getTrackedEntityAttribute(attributeValue.getAttribute());
        TrackedEntityAttributeValue trackedEntityAttributeValue = new TrackedEntityAttributeValue();
        trackedEntityAttributeValue.setAttribute(attribute);
        trackedEntityAttributeValue.setEntityInstance(tei);
        trackedEntityAttributeValue.setValue(attributeValue.getValue());
        return trackedEntityAttributeValue;
    }

    private boolean userHasOrgUnit(User user, OrganisationUnit ou) {
        return user.getOrganisationUnits().contains(ou);
    }

    private boolean isValidAttribute(AttributeValue value) {
        return value.getValue() != null && value.getAttribute() != null;
    }
}