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

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SMSCompressionException;
import org.hisp.dhis.smscompression.models.AggregateDatasetSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSDataValue;
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

public class AggregateDatasetSMSListenerTest
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

    // Needed for this test

    @Mock
    private DataSetService dataSetService;

    @Mock
    private CompleteDataSetRegistrationService registrationService;

    @Mock
    private DataValueService dataValueService;

    private AggregateDatasetSMSListener subject;

    // Needed for all

    private User user;

    private OutboundMessageResponse response = new OutboundMessageResponse();

    private IncomingSms updatedIncomingSms;

    private String message = "";

    // Needed for this test

    private IncomingSms incomingSmsAggregate;

    private OrganisationUnit organisationUnit;

    private CategoryOptionCombo categoryOptionCombo;

    private DataElement dataElement;

    private DataSet dataSet;

    @Before
    public void initTest()
        throws SMSCompressionException
    {
        subject = new AggregateDatasetSMSListener( incomingSmsService, smsSender, userService, trackedEntityTypeService,
            trackedEntityAttributeService, programService, organisationUnitService, categoryService, dataElementService,
            programStageInstanceService, dataSetService, dataValueService, registrationService );

        setUpInstances();

        when( userService.getUser( anyString() ) ).thenReturn( user );
        when( smsSender.isConfigured() ).thenReturn( true );
        when( smsSender.sendMessage( any(), any(), anyString() ) ).thenAnswer( invocation -> {
            message = (String) invocation.getArguments()[1];
            return response;
        } );

        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( organisationUnit );
        when( dataSetService.getDataSet( anyString() ) ).thenReturn( dataSet );
        when( dataValueService.addDataValue( any() ) ).thenReturn( true );
        when( categoryService.getCategoryOptionCombo( anyString() ) ).thenReturn( categoryOptionCombo );
        when( dataElementService.getDataElement( anyString() ) ).thenReturn( dataElement );

        doAnswer( invocation -> {
            updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
            return updatedIncomingSms;
        } ).when( incomingSmsService ).update( any() );
    }

    @Test
    public void testAggregateDatasetListener()
    {
        subject.receive( incomingSmsAggregate );

        assertNotNull( updatedIncomingSms );
        assertTrue( updatedIncomingSms.isParsed() );
        assertEquals( SUCCESS_MESSAGE, message );

        verify( incomingSmsService, times( 1 ) ).update( any() );
    }

    private void setUpInstances()
        throws SMSCompressionException
    {
        organisationUnit = createOrganisationUnit( 'O' );
        user = createUser( 'U' );
        user.setPhoneNumber( ORIGINATOR );
        user.setOrganisationUnits( Sets.newHashSet( organisationUnit ) );
        dataSet = createDataSet( 'D' );
        organisationUnit.getDataSets().add( dataSet );
        categoryOptionCombo = createCategoryOptionCombo( 'C' );
        dataElement = createDataElement( 'D' );

        incomingSmsAggregate = createSMSFromSubmission( createAggregateDatasetSubmission() );
    }

    private AggregateDatasetSMSSubmission createAggregateDatasetSubmission()
    {
        AggregateDatasetSMSSubmission subm = new AggregateDatasetSMSSubmission();

        subm.setUserID( user.getUid() );
        subm.setOrgUnit( organisationUnit.getUid() );
        subm.setDataSet( dataSet.getUid() );
        subm.setComplete( true );
        subm.setAttributeOptionCombo( categoryOptionCombo.getUid() );
        subm.setPeriod( "2019W16" );
        ArrayList<SMSDataValue> values = new ArrayList<>();
        values.add( new SMSDataValue( categoryOptionCombo.getUid(), dataElement.getUid(), "12345678" ) );
        subm.setValues( values );
        subm.setSubmissionID( 1 );

        return subm;
    }
}
