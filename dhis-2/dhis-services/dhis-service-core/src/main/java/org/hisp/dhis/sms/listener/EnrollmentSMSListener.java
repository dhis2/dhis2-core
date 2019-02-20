package org.hisp.dhis.sms.listener;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.listener.AbstractSMSListener;
import org.hisp.dhis.smscompression.SMSSubmissionReader;
import org.hisp.dhis.smscompression.models.EnrollmentSMSSubmission;
import org.hisp.dhis.smscompression.models.Metadata;
import org.hisp.dhis.smscompression.models.SMSSubmissionHeader;
import org.hisp.dhis.smscompression.models.TrackerEventSMSSubmission;
import org.hisp.dhis.trackedentity.*;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

@Transactional
public class EnrollmentSMSListener implements IncomingSmsListener {
    private TrackedEntityInstanceService trackedEntityInstanceService;

    private TrackerEventSMSSubmission trackerEventSMSSubmission;

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
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    @Override
    public boolean accept(IncomingSms sms) {
        return sms != null;
    }

    @Override
    public void receive(IncomingSms sms) {
        postProcess(sms);
    }

    public void postProcess (IncomingSms sms) {
        byte[] smsBytes = sms.getBytes();

        if (smsBytes == null) {
            smsBytes = Base64.getDecoder().decode(sms.getText());
        }

        Metadata metadata = getMetadata();
        SMSSubmissionReader reader = new SMSSubmissionReader();
        try {
             SMSSubmissionHeader header = reader.readHeader(smsBytes);
             EnrollmentSMSSubmission subm = (EnrollmentSMSSubmission) reader.readSubmission(header, metadata);
             subm.getTimestamp();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Metadata getMetadata () {
//        List<String> userIds = getAllUserIds();
//        List<String> typeIds = getTrackedEntityIds();
//        List<String> attributeIds = getTrackedEntityAttributeIds();
//        List<String> programIds = getAllProgramIds();
//        List<String> orgUnitIds = getAllOrgUnitIds();

        Metadata metadata = new Metadata();

        Metadata2 data = new Metadata2();

        data.users = getAllUserIds();
        data.trackedEntityTypes = getTrackedEntityIds();
        data.programs = getAllProgramIds();
        data.trackedEntityAttributes = getTrackedEntityAttributeIds();
        data.organisationUnits = getAllOrgUnitIds();

//        Type type = new TypeToken<Metadata2>() {}.getType();
        Gson gson = new Gson();
        String json = gson.toJson(data, Metadata2.class);
        Metadata meta = gson.fromJson(json, Metadata.class);
//        metadata.users[0] = ID
//        metadata.users = userIds.toArray(new ID[userIds.size()]);

        return meta;
    }

    public Metadata2.ID[] getAllUserIds () {
        List<User> users = userService.getAllUsers();
        Metadata2.ID[] userIds = new Metadata2.ID[users.size()];

        for (int i = 0; i < users.size(); i++) {
            Metadata2.ID id = new Metadata2.ID();
            id.id = users.get(i).getUid();
            userIds[i] =  id;
        }

        return userIds;
    }

    public Metadata2.ID[] getTrackedEntityIds () {
        List<TrackedEntityType> types = trackedEntityTypeService.getAllTrackedEntityType();
        Metadata2.ID[] typeIds = new Metadata2.ID[types.size()];

        for (int i = 0; i < types.size(); i++) {
            Metadata2.ID id = new Metadata2.ID();
            id.id = types.get(i).getUid();
            typeIds[i] =  id;
        }

        return typeIds;
    }


    public Metadata2.ID[] getTrackedEntityAttributeIds () {
        List<TrackedEntityAttribute> attributes = trackedEntityAttributeService.getAllTrackedEntityAttributes();
        Metadata2.ID[] ids = new Metadata2.ID[attributes.size()];

        for (int i = 0; i < attributes.size(); i++) {
            Metadata2.ID id = new Metadata2.ID();
            id.id = attributes.get(i).getUid();
            ids[i] =  id;
        }


        return ids;
    }

    public Metadata2.ID[] getAllProgramIds () {
        List<Program> programs = programService.getAllPrograms();
        Metadata2.ID[] ids = new Metadata2.ID[programs.size()];

        for (int i = 0; i < programs.size(); i++) {
            Metadata2.ID id = new Metadata2.ID();
            id.id = programs.get(i).getUid();
            ids[i] =  id;
        }


        return ids;
    }

    public Metadata2.ID[] getAllOrgUnitIds () {
        List<OrganisationUnit> orgUnits = organisationUnitService.getAllOrganisationUnits();
        Metadata2.ID[] ids = new Metadata2.ID[orgUnits.size()];

        for (int i = 0; i < orgUnits.size(); i++) {
            Metadata2.ID id = new Metadata2.ID();
            id.id = orgUnits.get(i).getUid();
            ids[i] =  id;
        }

        return ids;
    }
}