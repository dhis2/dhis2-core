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
import java.util.Base64;
import java.util.Date;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SMSCompressionException;
import org.hisp.dhis.smscompression.SMSSubmissionWriter;
import org.hisp.dhis.smscompression.models.EnrollmentSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSAttributeValue;
import org.hisp.dhis.smscompression.models.SMSMetadata;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Sets;

public class NewSMSListenerTest
    extends
    DhisConvenienceTest
{
    private static final String SUCCESS_MESSAGE = "1:0::Submission has been processed successfully";

    private static final String ORIGINATOR = "47400000";

    private static final String ATTRIBUTE_VALUE = "TEST";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private TrackedEntityInstanceService teiService;

    @Mock
    private TrackedEntityTypeService trackedEntityTypeService;

    @Mock
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Mock
    private ProgramService programService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private UserService userService;

    @Mock
    private MessageSender smsSender;

    @Mock
    private IncomingSmsService incomingSmsService;

    @InjectMocks
    private EnrollmentSMSListener enrollmentSMSListener;

    private IncomingSms incomingSms;

    private IncomingSms updatedIncomingSms;

    private OutboundMessageResponse response = new OutboundMessageResponse();

    private OrganisationUnit organisationUnit;

    private Program program;

    private ProgramInstance programInstance;

    private TrackedEntityInstance trackedEntityInstance;

    private TrackedEntityAttribute trackedEntityAttribute;

    private TrackedEntityAttributeValue trackedEntityAttributeValue;

    private ProgramTrackedEntityAttribute programTrackedEntityAttribute;

    private TrackedEntityType trackedEntityType;

    private User user;

    private SMSSubmissionWriter writer;

    private String message = "";

    @Before
    public void initTest()
        throws SMSCompressionException
    {
        setUpInstances();

        when( userService.getUser( anyString() ) ).thenReturn( user );
        when( programService.getProgram( anyString() ) ).thenReturn( program );
        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( organisationUnit );
        when( trackedEntityTypeService.getTrackedEntityType( anyString() ) ).thenReturn( trackedEntityType );
        when( trackedEntityAttributeService.getTrackedEntityAttribute( anyString() ) )
            .thenReturn( trackedEntityAttribute );
        when( programInstanceService.enrollTrackedEntityInstance( any(), any(), any(), any(), any() ) )
            .thenReturn( programInstance );
        when( smsSender.isConfigured() ).thenReturn( true );
        when( smsSender.sendMessage( any(), any(), anyString() ) ).thenAnswer( invocation -> {
            message = (String) invocation.getArguments()[1];
            return response;
        } );

        doAnswer( invocation -> {
            updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
            return updatedIncomingSms;
        } ).when( incomingSmsService ).update( any() );
    }

    @Test
    public void testEnrollment()
    {
        enrollmentSMSListener.receive( incomingSms );

        assertNotNull( updatedIncomingSms );
        assertTrue( updatedIncomingSms.isParsed() );
        assertEquals( SUCCESS_MESSAGE, message );

        verify( incomingSmsService, times( 1 ) ).update( any() );
    }

    private void setUpInstances()
        throws SMSCompressionException
    {
        trackedEntityType = createTrackedEntityType( 'T' );
        organisationUnit = createOrganisationUnit( 'O' );
        program = createProgram( 'P' );

        user = createUser( 'U' );
        user.setPhoneNumber( ORIGINATOR );
        user.setOrganisationUnits( Sets.newHashSet( organisationUnit ) );

        programTrackedEntityAttribute = createProgramTrackedEntityAttribute( 'Q' );
        trackedEntityAttribute = createTrackedEntityAttribute( 'A', ValueType.TEXT );
        program.getProgramAttributes().add( programTrackedEntityAttribute );
        program.getOrganisationUnits().add( organisationUnit );
        program.setTrackedEntityType( trackedEntityType );

        programInstance = new ProgramInstance();
        programInstance.setAutoFields();

        trackedEntityInstance = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstance.getTrackedEntityAttributeValues().add( trackedEntityAttributeValue );
        trackedEntityInstance.setOrganisationUnit( organisationUnit );

        trackedEntityAttributeValue = createTrackedEntityAttributeValue( 'A', trackedEntityInstance,
            trackedEntityAttribute );
        trackedEntityAttributeValue.setValue( ATTRIBUTE_VALUE );

        EnrollmentSMSSubmission subm = createEnrollmentSubmission();
        SMSMetadata meta = new SMSMetadata();
        meta.lastSyncDate = new Date();
        writer = new SMSSubmissionWriter( meta );
        String smsText = Base64.getEncoder().encodeToString( writer.compress( subm ) );

        incomingSms = new IncomingSms();
        incomingSms.setText( smsText );
        incomingSms.setOriginator( ORIGINATOR );
        incomingSms.setUser( user );
    }

    private EnrollmentSMSSubmission createEnrollmentSubmission()
    {
        EnrollmentSMSSubmission subm = new EnrollmentSMSSubmission();

        subm.setUserID( user.getUid() );
        subm.setOrgUnit( organisationUnit.getUid() );
        subm.setTrackerProgram( program.getUid() );
        subm.setTrackedEntityType( trackedEntityType.getUid() );
        subm.setTrackedEntityInstance( trackedEntityInstance.getUid() );
        subm.setEnrollment( programInstance.getUid() );
        subm.setTimestamp( new Date() );
        ArrayList<SMSAttributeValue> values = new ArrayList<>();
        values.add( new SMSAttributeValue( trackedEntityAttribute.getUid(), ATTRIBUTE_VALUE ) );
        subm.setValues( values );
        subm.setSubmissionID( 1 );

        return subm;
    }
}
