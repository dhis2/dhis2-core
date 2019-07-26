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
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SMSCompressionException;
import org.hisp.dhis.smscompression.models.RelationshipSMSSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class RelationshipSMSListenerTest
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

    RelationshipSMSListener subject;

    private IncomingSms incomingSmsRelationship;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private TrackedEntityInstanceService trackedEntityInstanceService;

    private ProgramInstance programInstance;

    private RelationshipType relationshipType;

    @Before
    public void initTest()
        throws SMSCompressionException
    {
        subject = new RelationshipSMSListener( incomingSmsService, smsSender, userService, trackedEntityTypeService,
            trackedEntityAttributeService, programService, organisationUnitService, categoryService, dataElementService,
            programStageInstanceService, relationshipService, relationshipTypeService, trackedEntityInstanceService,
            programInstanceService );

        setUpInstances();

        when( userService.getUser( anyString() ) ).thenReturn( user );
        when( smsSender.isConfigured() ).thenReturn( true );
        when( smsSender.sendMessage( any(), any(), anyString() ) ).thenAnswer( invocation -> {
            message = (String) invocation.getArguments()[1];
            return response;
        } );

        when( relationshipTypeService.getRelationshipType( anyString() ) ).thenReturn( relationshipType );
        when( programInstanceService.getProgramInstance( anyString() ) ).thenReturn( programInstance );

        doAnswer( invocation -> {
            updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
            return updatedIncomingSms;
        } ).when( incomingSmsService ).update( any() );
    }

    @Test
    public void testRelationship()
    {
        subject.receive( incomingSmsRelationship );

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

        programInstance = new ProgramInstance();
        programInstance.setAutoFields();

        relationshipType = new RelationshipType();
        relationshipType.setAutoFields();
        RelationshipConstraint relConstraint = new RelationshipConstraint();
        relConstraint.setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        relationshipType.setToConstraint( relConstraint );
        relationshipType.setFromConstraint( relConstraint );

        incomingSmsRelationship = createSMSFromSubmission( createRelationshipSubmission() );
    }

    private RelationshipSMSSubmission createRelationshipSubmission()
    {
        RelationshipSMSSubmission subm = new RelationshipSMSSubmission();

        subm.setUserID( user.getUid() );
        subm.setRelationshipType( relationshipType.getUid() );
        subm.setRelationship( "uf3svrmpzOj" );
        subm.setFrom( programInstance.getUid() );
        subm.setTo( programInstance.getUid() );
        subm.setSubmissionID( 1 );

        return subm;
    }
}
