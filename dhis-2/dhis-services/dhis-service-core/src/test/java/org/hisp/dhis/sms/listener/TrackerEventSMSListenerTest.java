package org.hisp.dhis.sms.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SMSCompressionException;
import org.hisp.dhis.smscompression.SMSConsts.SMSEventStatus;
import org.hisp.dhis.smscompression.models.SMSDataValue;
import org.hisp.dhis.smscompression.models.TrackerEventSMSSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Sets;

public class TrackerEventSMSListenerTest
    extends
    NewSMSListenerTest
{
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    // Needed for parent

    @Mock
    private UserService userService;

    @Mock
    private IncomingSmsService incomingSmsService;

    @Mock
    private MessageSender smsSender;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private TrackedEntityTypeService trackedEntityTypeService;

    @Mock
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Mock
    private ProgramService programService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProgramStageInstanceService programStageInstanceService;

    private User user;

    private OutboundMessageResponse response = new OutboundMessageResponse();

    private IncomingSms updatedIncomingSms;

    private String message = "";

    // Needed for this test

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private ProgramStageService programStageService;

    TrackerEventSMSListener subject;

    private IncomingSms incomingSmsTrackerEvent;

    private OrganisationUnit organisationUnit;

    private CategoryOptionCombo categoryOptionCombo;

    private DataElement dataElement;

    private Program program;

    private ProgramStage programStage;

    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    @Before
    public void initTest()
        throws SMSCompressionException
    {
        subject = new TrackerEventSMSListener( incomingSmsService, smsSender, userService, trackedEntityTypeService,
            trackedEntityAttributeService, programService, organisationUnitService, categoryService, dataElementService,
            programStageInstanceService, programStageService, programInstanceService );

        setUpInstances();

        when( userService.getUser( anyString() ) ).thenReturn( user );
        when( smsSender.isConfigured() ).thenReturn( true );
        when( smsSender.sendMessage( any(), any(), anyString() ) ).thenAnswer( invocation -> {
            message = (String) invocation.getArguments()[1];
            return response;
        } );

        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( organisationUnit );
        when( programStageService.getProgramStage( anyString() ) ).thenReturn( programStage );
        when( programInstanceService.getProgramInstance( anyString() ) ).thenReturn( programInstance );
        when( dataElementService.getDataElement( anyString() ) ).thenReturn( dataElement );
        when( categoryService.getCategoryOptionCombo( anyString() ) ).thenReturn( categoryOptionCombo );

        doAnswer( invocation -> {
            updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
            return updatedIncomingSms;
        } ).when( incomingSmsService ).update( any() );
    }

    @Test
    public void testTrackerEvent()
    {
        subject.receive( incomingSmsTrackerEvent );

        assertNotNull( updatedIncomingSms );
        assertTrue( updatedIncomingSms.isParsed() );
        assertEquals( SUCCESS_MESSAGE, message );

        verify( incomingSmsService, times( 1 ) ).update( any() );
    }

    private void setUpInstances()
        throws SMSCompressionException
    {
        organisationUnit = createOrganisationUnit( 'O' );
        program = createProgram( 'P' );
        programStage = createProgramStage( 'S', program );

        user = createUser( 'U' );
        user.setPhoneNumber( ORIGINATOR );
        user.setOrganisationUnits( Sets.newHashSet( organisationUnit ) );

        categoryOptionCombo = createCategoryOptionCombo( 'C' );
        dataElement = createDataElement( 'D' );

        program.getOrganisationUnits().add( organisationUnit );
        HashSet<ProgramStage> stages = new HashSet<>();
        stages.add( programStage );
        program.setProgramStages( stages );

        programInstance = new ProgramInstance();
        programInstance.setAutoFields();
        programInstance.setProgram( program );

        programStageInstance = new ProgramStageInstance();
        programStageInstance.setAutoFields();

        incomingSmsTrackerEvent = createSMSFromSubmission( createTrackerEventSubmission() );
    }

    private TrackerEventSMSSubmission createTrackerEventSubmission()
    {
        TrackerEventSMSSubmission subm = new TrackerEventSMSSubmission();

        subm.setUserID( user.getUid() );
        subm.setOrgUnit( organisationUnit.getUid() );
        subm.setProgramStage( programStage.getUid() );
        subm.setAttributeOptionCombo( categoryOptionCombo.getUid() );
        subm.setEnrollment( programInstance.getUid() );
        subm.setEvent( programStageInstance.getUid() );
        subm.setEventStatus( SMSEventStatus.COMPLETED );
        subm.setTimestamp( new Date() );
        ArrayList<SMSDataValue> values = new ArrayList<>();
        values.add( new SMSDataValue( categoryOptionCombo.getUid(), dataElement.getUid(), "10" ) );
        subm.setValues( values );
        subm.setSubmissionID( 1 );

        return subm;
    }

}
