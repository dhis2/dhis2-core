package org.hisp.dhis.sms.listener;

/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
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
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SMSCompressionException;
import org.hisp.dhis.smscompression.SMSConsts.SMSEventStatus;
import org.hisp.dhis.smscompression.SMSSubmissionWriter;
import org.hisp.dhis.smscompression.models.AggregateDatasetSMSSubmission;
import org.hisp.dhis.smscompression.models.DeleteSMSSubmission;
import org.hisp.dhis.smscompression.models.EnrollmentSMSSubmission;
import org.hisp.dhis.smscompression.models.RelationshipSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSAttributeValue;
import org.hisp.dhis.smscompression.models.SMSDataValue;
import org.hisp.dhis.smscompression.models.SMSMetadata;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.smscompression.models.SimpleEventSMSSubmission;
import org.hisp.dhis.smscompression.models.TrackerEventSMSSubmission;
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

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;

import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CompressionSMSListenerTest
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
    private ProgramStageService programStageService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private ProgramStageInstanceService programStageInstanceService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private RelationshipTypeService relationshipTypeService;

    @Mock
    private DataSetService dataSetService;

    @Mock
    private DataValueService dataValueService;

    @Mock
    private CompleteDataSetRegistrationService registrationService;

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

    @Mock
    private IdentifiableObjectManager identifiableObjectManager;

    @InjectMocks
    private AggregateDataSetSMSListener aggregateDatasetSMSListener;

    @InjectMocks
    private DeleteEventSMSListener deleteEventSMSListener;

    @InjectMocks
    private EnrollmentSMSListener enrollmentSMSListener;

    @InjectMocks
    private RelationshipSMSListener relationshipSMSListener;

    @InjectMocks
    private SimpleEventSMSListener simpleEventSMSListener;

    @InjectMocks
    private TrackerEventSMSListener trackerEventSMSListener;

    private CategoryOptionCombo categoryOptionCombo;

    private DataElement dataElement;

    private DataSet dataSet;

    private IncomingSms incomingSmsAggregate;

    private IncomingSms incomingSmsDelete;

    private IncomingSms incomingSmsEnrollment;

    private IncomingSms incomingSmsRelationship;

    private IncomingSms incomingSmsSimpleEvent;

    private IncomingSms incomingSmsTrackerEvent;

    private IncomingSms updatedIncomingSms;

    private OutboundMessageResponse response = new OutboundMessageResponse();

    private OrganisationUnit organisationUnit;

    private Program program;

    private ProgramStage programStage;

    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    private RelationshipType relationshipType;

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

    private void initDelete()
    {
        when( programStageInstanceService.getProgramStageInstance( anyString() ) ).thenReturn( programStageInstance );
    }

    private void initAggregate()
    {
        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( organisationUnit );
        when( dataSetService.getDataSet( anyString() ) ).thenReturn( dataSet );
        when( dataValueService.addDataValue( any() ) ).thenReturn( true );
        when( categoryService.getCategoryOptionCombo( anyString() ) ).thenReturn( categoryOptionCombo );
        when( dataElementService.getDataElement( anyString() ) ).thenReturn( dataElement );
    }

    private void initEnrollment()
    {
        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( organisationUnit );
        when( programService.getProgram( anyString() ) ).thenReturn( program );
        when( trackedEntityTypeService.getTrackedEntityType( anyString() ) ).thenReturn( trackedEntityType );
        when( trackedEntityAttributeService.getTrackedEntityAttribute( anyString() ) )
            .thenReturn( trackedEntityAttribute );
        when( programInstanceService.enrollTrackedEntityInstance( any(), any(), any(), any(), any() ) )
            .thenReturn( programInstance );
    }

    private void initSimpleEvent()
    {
        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( organisationUnit );
        when( programService.getProgram( anyString() ) ).thenReturn( program );
        when( dataElementService.getDataElement( anyString() ) ).thenReturn( dataElement );
        when( categoryService.getCategoryOptionCombo( anyString() ) ).thenReturn( categoryOptionCombo );
    }

    private void initTrackerEvent()
    {
        when( organisationUnitService.getOrganisationUnit( anyString() ) ).thenReturn( organisationUnit );
        when( programStageService.getProgramStage( anyString() ) ).thenReturn( programStage );
        when( programInstanceService.getProgramInstance( anyString() ) ).thenReturn( programInstance );
        when( dataElementService.getDataElement( anyString() ) ).thenReturn( dataElement );
        when( categoryService.getCategoryOptionCombo( anyString() ) ).thenReturn( categoryOptionCombo );
    }

    private void initRelationship()
    {
        when( relationshipTypeService.getRelationshipType( anyString() ) ).thenReturn( relationshipType );
        when( programInstanceService.getProgramInstance( anyString() ) ).thenReturn( programInstance );
    }

    @Test
    public void testAggregateDataset()
    {
        initAggregate();
        aggregateDatasetSMSListener.receive( incomingSmsAggregate );

        assertNotNull( updatedIncomingSms );
        assertTrue( updatedIncomingSms.isParsed() );
        assertEquals( SUCCESS_MESSAGE, message );

        verify( incomingSmsService, times( 1 ) ).update( any() );
    }

    @Test
    public void testDeleteEvent()
    {
        initDelete();
        deleteEventSMSListener.receive( incomingSmsDelete );

        assertNotNull( updatedIncomingSms );
        assertTrue( updatedIncomingSms.isParsed() );
        assertEquals( SUCCESS_MESSAGE, message );

        verify( incomingSmsService, times( 1 ) ).update( any() );
    }

    @Test
    public void testEnrollment()
    {
        initEnrollment();
        enrollmentSMSListener.receive( incomingSmsEnrollment );

        assertNotNull( updatedIncomingSms );
        assertTrue( updatedIncomingSms.isParsed() );
        assertEquals( SUCCESS_MESSAGE, message );

        verify( incomingSmsService, times( 1 ) ).update( any() );
    }

    @Test
    public void testRelationship()
    {
        initRelationship();
        relationshipSMSListener.receive( incomingSmsRelationship );

        assertNotNull( updatedIncomingSms );
        assertTrue( updatedIncomingSms.isParsed() );
        assertEquals( SUCCESS_MESSAGE, message );

        verify( incomingSmsService, times( 1 ) ).update( any() );
    }

    @Test
    public void testSimpleEvent()
    {
        initSimpleEvent();
        simpleEventSMSListener.receive( incomingSmsSimpleEvent );

        assertNotNull( updatedIncomingSms );
        assertTrue( updatedIncomingSms.isParsed() );
        assertEquals( SUCCESS_MESSAGE, message );

        verify( incomingSmsService, times( 1 ) ).update( any() );
    }

    @Test
    public void testTrackerEvent()
    {
        initTrackerEvent();
        trackerEventSMSListener.receive( incomingSmsTrackerEvent );

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
        programStage = createProgramStage( 'S', program );

        user = createUser( 'U' );
        user.setPhoneNumber( ORIGINATOR );
        user.setOrganisationUnits( Sets.newHashSet( organisationUnit ) );

        dataSet = createDataSet( 'D' );
        dataSet.getSources().add( organisationUnit );
        categoryOptionCombo = createCategoryOptionCombo( 'C' );
        dataElement = createDataElement( 'D' );

        programTrackedEntityAttribute = createProgramTrackedEntityAttribute( 'Q' );
        trackedEntityAttribute = createTrackedEntityAttribute( 'A', ValueType.TEXT );
        program.getProgramAttributes().add( programTrackedEntityAttribute );
        program.getOrganisationUnits().add( organisationUnit );
        program.setTrackedEntityType( trackedEntityType );
        HashSet<ProgramStage> stages = new HashSet<>();
        stages.add( programStage );
        program.setProgramStages( stages );

        programInstance = new ProgramInstance();
        programInstance.setAutoFields();
        programInstance.setProgram( program );

        programStageInstance = new ProgramStageInstance();
        programStageInstance.setAutoFields();

        relationshipType = new RelationshipType();
        relationshipType.setAutoFields();
        RelationshipConstraint relConstraint = new RelationshipConstraint();
        relConstraint.setRelationshipEntity( RelationshipEntity.PROGRAM_INSTANCE );
        relationshipType.setToConstraint( relConstraint );
        relationshipType.setFromConstraint( relConstraint );

        trackedEntityInstance = createTrackedEntityInstance( organisationUnit );
        trackedEntityInstance.getTrackedEntityAttributeValues().add( trackedEntityAttributeValue );
        trackedEntityInstance.setOrganisationUnit( organisationUnit );

        trackedEntityAttributeValue = createTrackedEntityAttributeValue( 'A', trackedEntityInstance,
            trackedEntityAttribute );
        trackedEntityAttributeValue.setValue( ATTRIBUTE_VALUE );

        incomingSmsAggregate = createSMSFromSubmission( createAggregateDatasetSubmission() );
        incomingSmsDelete = createSMSFromSubmission( createDeleteSubmission() );
        incomingSmsEnrollment = createSMSFromSubmission( createEnrollmentSubmission() );
        incomingSmsRelationship = createSMSFromSubmission( createRelationshipSubmission() );
        incomingSmsSimpleEvent = createSMSFromSubmission( createSimpleEventSubmission() );
        incomingSmsTrackerEvent = createSMSFromSubmission( createTrackerEventSubmission() );
    }

    private IncomingSms createSMSFromSubmission( SMSSubmission subm )
        throws SMSCompressionException
    {
        SMSMetadata meta = new SMSMetadata();
        meta.lastSyncDate = new Date();
        writer = new SMSSubmissionWriter( meta );
        String smsText = Base64.getEncoder().encodeToString( writer.compress( subm ) );

        IncomingSms incomingSms = new IncomingSms();
        incomingSms.setText( smsText );
        incomingSms.setOriginator( ORIGINATOR );
        incomingSms.setUser( user );

        return incomingSms;
    }

    private DeleteSMSSubmission createDeleteSubmission()
    {
        DeleteSMSSubmission subm = new DeleteSMSSubmission();

        subm.setUserID( user.getUid() );
        subm.setEvent( programStageInstance.getUid() );
        subm.setSubmissionID( 1 );

        return subm;
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

    private SimpleEventSMSSubmission createSimpleEventSubmission()
    {
        SimpleEventSMSSubmission subm = new SimpleEventSMSSubmission();

        subm.setUserID( user.getUid() );
        subm.setOrgUnit( organisationUnit.getUid() );
        subm.setEventProgram( program.getUid() );
        subm.setAttributeOptionCombo( categoryOptionCombo.getUid() );
        subm.setEvent( programStageInstance.getUid() );
        subm.setEventStatus( SMSEventStatus.COMPLETED );
        subm.setTimestamp( new Date() );
        ArrayList<SMSDataValue> values = new ArrayList<>();
        values.add( new SMSDataValue( categoryOptionCombo.getUid(), dataElement.getUid(), "true" ) );
        subm.setValues( values );
        subm.setSubmissionID( 1 );

        return subm;
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
