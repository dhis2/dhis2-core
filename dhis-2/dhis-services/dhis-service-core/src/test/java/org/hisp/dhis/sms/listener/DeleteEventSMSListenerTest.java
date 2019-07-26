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

import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SMSCompressionException;
import org.hisp.dhis.smscompression.models.DeleteSMSSubmission;
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

public class DeleteEventSMSListenerTest
    extends
    NewSMSListenerTest
{
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    // Needed for all

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

    DeleteEventSMSListener subject;

    private IncomingSms incomingSmsDelete;

    private ProgramStageInstance programStageInstance;

    @Before
    public void initTest()
        throws SMSCompressionException
    {
        subject = new DeleteEventSMSListener( incomingSmsService, smsSender, userService, trackedEntityTypeService,
            trackedEntityAttributeService, programService, organisationUnitService, categoryService, dataElementService,
            programStageInstanceService );

        setUpInstances();

        when( userService.getUser( anyString() ) ).thenReturn( user );
        when( smsSender.isConfigured() ).thenReturn( true );
        when( smsSender.sendMessage( any(), any(), anyString() ) ).thenAnswer( invocation -> {
            message = (String) invocation.getArguments()[1];
            return response;
        } );
        when( programStageInstanceService.getProgramStageInstance( anyString() ) ).thenReturn( programStageInstance );

        doAnswer( invocation -> {
            updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
            return updatedIncomingSms;
        } ).when( incomingSmsService ).update( any() );
    }

    @Test
    public void testDeleteEvent()
    {
        subject.receive( incomingSmsDelete );

        assertNotNull( updatedIncomingSms );
        assertTrue( updatedIncomingSms.isParsed() );
        assertEquals( SUCCESS_MESSAGE, message );

        verify( incomingSmsService, times( 1 ) ).update( any() );
    }

    private void setUpInstances()
        throws SMSCompressionException
    {
        user = createUser( 'U' );
        user.setPhoneNumber( ORIGINATOR );

        programStageInstance = new ProgramStageInstance();
        programStageInstance.setAutoFields();

        incomingSmsDelete = createSMSFromSubmission( createDeleteSubmission() );
    }

    private DeleteSMSSubmission createDeleteSubmission()
    {
        DeleteSMSSubmission subm = new DeleteSMSSubmission();

        subm.setUserID( user.getUid() );
        subm.setEvent( programStageInstance.getUid() );
        subm.setSubmissionID( 1 );

        return subm;
    }

}
