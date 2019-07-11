package org.hisp.dhis.sms.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.models.EnrollmentSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSAttributeValue;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
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
    private static final String SUCCESS_MESSAGE = "Command has been processed successfully";

    private static final String TEI_REGISTRATION_COMMAND = "tei";

    private static final String SMS_TEXT = TEI_REGISTRATION_COMMAND + " " + "attr=sample";

    private static final String ORIGINATOR = "47400000";

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
    private UserService userService;

    @Mock
    private MessageSender smsSender;

    @Mock
    private IncomingSmsService incomingSmsService;

    @InjectMocks
    private EnrollmentSMSListener enrollmentSMSListener;

    private IncomingSms incomingSms;

    private IncomingSms updatedIncomingSms;

    private OrganisationUnit organisationUnit;

    private User user;

    private String message = "";

    @Before
    public void initTest()
    {
        setUpInstances();

        when( smsSender.isConfigured() ).thenReturn( true );
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
    {
        organisationUnit = createOrganisationUnit( 'O' );

        user = createUser( 'U' );
        user.setPhoneNumber( ORIGINATOR );
        user.setOrganisationUnits( Sets.newHashSet( organisationUnit ) );

        incomingSms = new IncomingSms();
        incomingSms.setText( SMS_TEXT );
        incomingSms.setOriginator( ORIGINATOR );
        incomingSms.setUser( user );
    }

    private EnrollmentSMSSubmission createEnrollmentSubmission()
    {
        EnrollmentSMSSubmission subm = new EnrollmentSMSSubmission();

        subm.setUserID( "GOLswS44mh8" );
        subm.setOrgUnit( "DiszpKrYNg8" );
        subm.setTrackerProgram( "IpHINAT79UW" );
        subm.setTrackedEntityType( "nEenWmSyUEp" );
        subm.setTrackedEntityInstance( "T2bRuLEGoVN" );
        subm.setEnrollment( "p7M1gUFK37W" );
        subm.setTimestamp( new Date() );
        ArrayList<SMSAttributeValue> values = new ArrayList<>();
        values.add( new SMSAttributeValue( "w75KJ2mc4zz", "Harold" ) );
        subm.setValues( values );
        subm.setSubmissionID( 1 );

        return subm;
    }
}
