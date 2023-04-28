/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.sms.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockStatus;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.SMSSpecialCharacter;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;

/**
 * @author Zubair Asghar.
 */
@ExtendWith( MockitoExtension.class )
class DataValueListenerTest extends DhisConvenienceTest
{
    private static final String FETCHED_DATA_VALUE = "fetchedDataValue";

    private static final String STORED_BY = "CGhost";

    private static final String LAST_UPDATED_BY = "CGhost";

    private static final String DATA_ENTRY_COMMAND = "dataentrycommand";

    private static final String SUCCESS_MESSAGE = "data entered successfully";

    private static final String SMS_TEXT = DATA_ENTRY_COMMAND + " " + "de=sample";

    private static final String SMS_TEXT_FOR_CUSTOM_SEPARATOR = DATA_ENTRY_COMMAND + " " + "de.sample";

    private static final String SMS_TEXT_FOR_COMPULSORY = DATA_ENTRY_COMMAND + " " + "de=sample=deb=sample2";

    private static final String SMS_TEXT_FOR_COMPULSORY2 = DATA_ENTRY_COMMAND + " " + "de=sample|deb=sample2";

    private static final String ORIGINATOR = "474000000";

    private static final String WRONG_FORMAT = "WRONG_FORMAT";

    private static final String MORE_THAN_ONE_OU = "MORE_THAN_ONE_OU";

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private CategoryService dataElementCategoryService;

    @Mock
    private EventService eventService;

    @Mock
    private UserService userService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private IncomingSmsService incomingSmsService;

    @Mock
    private MessageSender smsSender;

    @Mock
    private CompleteDataSetRegistrationService registrationService;

    @Mock
    private DataValueService dataValueService;

    @Mock
    private SMSCommandService smsCommandService;

    @Mock
    private DataSetService dataSetService;

    @Mock
    private DataElementService dataElementService;

    private DataValueSMSListener subject;

    private CompleteDataSetRegistration fetchedCompleteDataSetRegistration;

    private CompleteDataSetRegistration deletedCompleteDataSetRegistration;

    private DataValue fetchedDataValue;

    private DataValue updatedDataValue;

    private DataElement dataElement;

    private DataElement dataElementB;

    private CategoryOptionCombo defaultCategoryOptionCombo;

    private CategoryOptionCombo categoryOptionCombo;

    private OrganisationUnit organisationUnitA;

    private OrganisationUnit organisationUnitB;

    private DataSet dataSet;

    private DataSet dataSetB;

    private Period period;

    private User user;

    private User userB;

    private User userC;

    private User userWithNoOu;

    private User userwithMultipleOu;

    private SMSCommand keyValueCommand;

    private SMSCode smsCode;

    private SMSCode smsCodeForcompulsory;

    private SMSSpecialCharacter smsSpecialCharacter;

    private IncomingSms incomingSms;

    private IncomingSms incomingSmsForCustomSeparator;

    private IncomingSms incomingSmsForCompulsoryCode;

    private IncomingSms updatedIncomingSms;

    private OutboundMessageResponse response;

    private boolean smsConfigured = true;

    private String message = "";

    @BeforeEach
    public void initTest()
    {
        subject = new DataValueSMSListener( programInstanceService, dataElementCategoryService,
            eventService, userService, currentUserService, incomingSmsService, smsSender,
            registrationService, dataValueService, dataElementCategoryService, smsCommandService, dataSetService,
            dataElementService );

        setUpInstances();
    }

    private void mockSmsSender()
    {
        // Mock for smsSender
        when( smsSender.isConfigured() ).thenReturn( smsConfigured );

        when( smsSender.sendMessage( any(), any(), anyString() ) ).thenAnswer( invocation -> {
            message = invocation.getArgument( 1 );
            return response;
        } );

    }

    private void mockServices()
    {
        // Mock for registrationService
        when( registrationService.getCompleteDataSetRegistration( any(), any(), any(), any() ) )
            .thenReturn( fetchedCompleteDataSetRegistration );

        doAnswer( invocation -> {
            deletedCompleteDataSetRegistration = (CompleteDataSetRegistration) invocation.getArguments()[0];
            return deletedCompleteDataSetRegistration;
        } ).when( registrationService ).deleteCompleteDataSetRegistration( any() );

        // Mock for dataValueService
        when( dataValueService.getDataValue( any(), any(), any(), any() ) )
            .thenReturn( fetchedDataValue );

        doAnswer( invocation -> {
            updatedDataValue = (DataValue) invocation.getArguments()[0];
            return updatedDataValue;
        } ).when( dataValueService ).updateDataValue( any() );

        // Mock for userService
        when( userService.getUser( anyString() ) ).thenReturn( user );

        // Mock for dataElementCategoryService
        when( dataElementCategoryService.getDefaultCategoryOptionCombo() ).thenReturn( defaultCategoryOptionCombo );

        Mockito.lenient().when( dataElementCategoryService.getCategoryOptionCombo( anyInt() ) )
            .thenReturn( categoryOptionCombo );

        // Mock for smsCommandService
        when( smsCommandService.getSMSCommand( anyString(), any() ) ).thenReturn( keyValueCommand );

        // Mock for dataSetService
        when( dataSetService.getLockStatus( any( DataSet.class ), any(), any(), any() ) )
            .thenReturn( LockStatus.OPEN );

        // Mock for incomingSmsService
        doAnswer( invocation -> {
            updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
            return updatedIncomingSms;
        } ).when( incomingSmsService ).update( any() );
    }

    @Test
    void testAccept()
    {
        // Mock for smsCommandService
        when( smsCommandService.getSMSCommand( anyString(), any() ) ).thenReturn( keyValueCommand );

        incomingSms.setCreatedBy( user );

        boolean result = subject.accept( incomingSms );

        assertTrue( result );

        result = subject.accept( null );

        assertFalse( result );
    }

    @Test
    void testReceive()
    {
        mockServices();
        incomingSms.setCreatedBy( user );
        subject.receive( incomingSms );

        assertNotNull( updatedIncomingSms );
        assertEquals( SmsMessageStatus.PROCESSED, updatedIncomingSms.getStatus() );
        assertTrue( updatedIncomingSms.isParsed() );
    }

    @Test
    void testIfDataSetIsLocked()
    {
        ArgumentCaptor<IncomingSms> incomingSmsCaptor = ArgumentCaptor.forClass( IncomingSms.class );

        mockSmsSender();

        // Mock for userService
        when( userService.getUser( anyString() ) ).thenReturn( user );

        // Mock for dataElementCategoryService
        when( dataElementCategoryService.getDefaultCategoryOptionCombo() ).thenReturn( defaultCategoryOptionCombo );

        // Mock for smsCommandService
        when( smsCommandService.getSMSCommand( anyString(), any() ) ).thenReturn( keyValueCommand );

        incomingSms.setUser( user );
        when( dataSetService.getLockStatus( any( DataSet.class ), any(), any(), any() ) )
            .thenReturn( LockStatus.OPEN );
        subject.receive( incomingSms );

        verify( smsCommandService, times( 1 ) ).getSMSCommand( anyString(), any() );
        verify( incomingSmsService, times( 1 ) ).update( incomingSmsCaptor.capture() );

        assertEquals( incomingSmsCaptor.getValue().getText(), incomingSms.getText() );
        assertFalse( incomingSmsCaptor.getValue().isParsed() );
    }

    @Test
    void testIfUserHasNoOu()
    {
        mockSmsSender();

        // Mock for userService
        when( userService.getUser( anyString() ) ).thenReturn( user );

        // Mock for smsCommandService
        when( smsCommandService.getSMSCommand( anyString(), any() ) ).thenReturn( keyValueCommand );

        incomingSms.setCreatedBy( userWithNoOu );
        when( userService.getUser( anyString() ) ).thenReturn( userWithNoOu );

        subject.receive( incomingSms );

        assertEquals( SMSCommand.NO_USER_MESSAGE, message );
        assertNull( updatedIncomingSms );
        verify( dataSetService, never() ).getLockStatus( any( DataSet.class ), any(), any(), any() );
    }

    @Test
    void testIfUserHasMultipleOUs()
    {
        mockSmsSender();

        // Mock for userService
        when( userService.getUser( anyString() ) ).thenReturn( user );

        // Mock for smsCommandService
        when( smsCommandService.getSMSCommand( anyString(), any() ) ).thenReturn( keyValueCommand );

        incomingSms.setCreatedBy( userwithMultipleOu );

        when( userService.getUser( anyString() ) ).thenReturn( userwithMultipleOu );
        when( userService.getUsersByPhoneNumber( anyString() ) )
            .thenReturn( Collections.singletonList( userwithMultipleOu ) );

        subject.receive( incomingSms );

        assertEquals( SMSCommand.MORE_THAN_ONE_ORGUNIT_MESSAGE, message );
        assertNull( updatedIncomingSms );
        verify( dataSetService, never() ).getLockStatus( any( DataSet.class ), any(), any(), any() );

        keyValueCommand.setMoreThanOneOrgUnitMessage( MORE_THAN_ONE_OU );

        subject.receive( incomingSms );

        // system will use custom message
        assertEquals( MORE_THAN_ONE_OU, message );
    }

    @Test
    void testIfDiffUsersHasSameOU()
    {
        mockSmsSender();
        mockServices();
        incomingSms.setCreatedBy( user );

        when( userService.getUser( anyString() ) ).thenReturn( user );
        when( userService.getUsersByPhoneNumber( anyString() ) ).thenReturn( Arrays.asList( user, userB ) );

        subject.receive( incomingSms );

        assertEquals( SUCCESS_MESSAGE, message );

        when( userService.getUsersByPhoneNumber( anyString() ) ).thenReturn( Arrays.asList( user, userC ) );

        keyValueCommand.setMoreThanOneOrgUnitMessage( MORE_THAN_ONE_OU );
        subject.receive( incomingSms );

        // system will use custom message
        assertEquals( MORE_THAN_ONE_OU, message );
    }

    @Test
    void testIfCommandHasCorrectFormat()
    {
        mockSmsSender();

        // Mock for smsCommandService
        when( smsCommandService.getSMSCommand( anyString(), any() ) ).thenReturn( keyValueCommand );

        subject.receive( incomingSmsForCustomSeparator );

        assertEquals( message, SMSCommand.WRONG_FORMAT_MESSAGE );
        assertNull( updatedIncomingSms );
        verify( dataSetService, never() ).getLockStatus( any( DataSet.class ), any(), any(), any() );

        keyValueCommand.setWrongFormatMessage( WRONG_FORMAT );
        subject.receive( incomingSmsForCustomSeparator );

        // system will use custom message
        assertEquals( WRONG_FORMAT, message );
    }

    @Test
    void testIfMandatoryParameterMissing()
    {
        mockSmsSender();
        mockServices();
        keyValueCommand.getCodes().add( smsCodeForcompulsory );
        keyValueCommand.setSeparator( null );
        keyValueCommand.setCodeValueSeparator( null );
        incomingSmsForCompulsoryCode.setText( SMS_TEXT );

        subject.receive( incomingSmsForCompulsoryCode );

        assertEquals( message, SMSCommand.PARAMETER_MISSING );

        incomingSmsForCompulsoryCode.setText( SMS_TEXT_FOR_COMPULSORY );

        subject.receive( incomingSmsForCompulsoryCode );

        assertEquals( keyValueCommand.getSuccessMessage(), message );

        incomingSmsForCompulsoryCode.setText( SMS_TEXT_FOR_COMPULSORY2 );

        subject.receive( incomingSmsForCompulsoryCode );

        assertEquals( keyValueCommand.getSuccessMessage(), message );
    }

    @Test
    void testIfOrgUnitNotInDataSet()
    {
        when( userService.getUser( anyString() ) ).thenReturn( user );
        when( smsCommandService.getSMSCommand( anyString(), any() ) ).thenReturn( keyValueCommand );
        doAnswer( invocation -> {
            updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
            return updatedIncomingSms;
        } ).when( incomingSmsService ).update( any() );

        keyValueCommand.setSeparator( null );
        keyValueCommand.setCodeValueSeparator( null );
        keyValueCommand.setDataset( dataSetB );

        subject.receive( incomingSms );

        assertNotNull( updatedIncomingSms );
        assertEquals( SmsMessageStatus.FAILED, updatedIncomingSms.getStatus() );
        assertFalse( updatedIncomingSms.isParsed() );
        verify( dataSetService, times( 0 ) ).getLockStatus( any( DataSet.class ),
            any( Period.class ), any( OrganisationUnit.class ), any( CategoryOptionCombo.class ) );
    }

    @Test
    void testDefaultSeparator()
    {
        mockServices();
        keyValueCommand.setSeparator( null );
        keyValueCommand.setCodeValueSeparator( null );

        // = is default separator
        subject.receive( incomingSms );

        assertNotNull( updatedIncomingSms );
        assertEquals( SmsMessageStatus.PROCESSED, updatedIncomingSms.getStatus() );
        assertTrue( updatedIncomingSms.isParsed() );
    }

    @Test
    void testCustomSeparator()
    {
        mockSmsSender();
        mockServices();
        keyValueCommand.setSeparator( "." );
        keyValueCommand.setCodeValueSeparator( "." );
        keyValueCommand.setWrongFormatMessage( null );

        subject.receive( incomingSmsForCustomSeparator );

        assertNotNull( updatedIncomingSms );
        assertEquals( SmsMessageStatus.PROCESSED, updatedIncomingSms.getStatus() );
        assertTrue( updatedIncomingSms.isParsed() );

        // when custom separator is empty space
        keyValueCommand.setSeparator( " " );
        keyValueCommand.setCodeValueSeparator( " " );
        subject.receive( incomingSmsForCustomSeparator );

        assertEquals( SMSCommand.WRONG_FORMAT_MESSAGE, message );
    }

    private void setUpInstances()
    {
        organisationUnitA = createOrganisationUnit( 'O' );
        organisationUnitB = createOrganisationUnit( 'P' );
        dataSet = createDataSet( 'D' );
        dataSetB = createDataSet( 'B' );
        dataSet.addOrganisationUnit( organisationUnitA );
        dataSet.addOrganisationUnit( organisationUnitB );
        period = createPeriod( new Date(), new Date() );

        user = makeUser( "U" );
        user.setPhoneNumber( ORIGINATOR );
        user.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        userB = makeUser( "B" );
        userB.setPhoneNumber( ORIGINATOR );
        userB.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        userC = makeUser( "C" );
        userC.setPhoneNumber( ORIGINATOR );
        userC.setOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );

        userWithNoOu = makeUser( "V" );
        userWithNoOu.setPhoneNumber( null );
        userWithNoOu.setOrganisationUnits( null );

        userwithMultipleOu = makeUser( "W" );
        userwithMultipleOu.setPhoneNumber( ORIGINATOR );
        userwithMultipleOu.setOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );

        dataElement = createDataElement( 'D' );
        dataElement.setValueType( ValueType.TEXT );
        defaultCategoryOptionCombo = createCategoryOptionCombo( 'D' );
        categoryOptionCombo = createCategoryOptionCombo( 'C' );

        dataElementB = createDataElement( 'B' );
        dataElementB.setValueType( ValueType.TEXT );

        fetchedDataValue = createDataValue( dataElement, period, organisationUnitA, FETCHED_DATA_VALUE,
            categoryOptionCombo );

        fetchedCompleteDataSetRegistration = new CompleteDataSetRegistration( dataSet, period, organisationUnitA,
            categoryOptionCombo, new Date(), STORED_BY, new Date(), LAST_UPDATED_BY, true );

        smsCode = new SMSCode();
        smsCode.setCode( "de" );
        smsCode.setDataElement( dataElement );
        smsCode.setOptionId( defaultCategoryOptionCombo );

        smsCodeForcompulsory = new SMSCode();
        smsCodeForcompulsory.setCode( "deb" );
        smsCodeForcompulsory.setDataElement( dataElementB );
        smsCodeForcompulsory.setOptionId( categoryOptionCombo );
        smsCodeForcompulsory.setCompulsory( true );

        smsSpecialCharacter = new SMSSpecialCharacter();
        smsSpecialCharacter.setName( "special" );
        smsSpecialCharacter.setValue( "spc1" );

        keyValueCommand = new SMSCommand();
        keyValueCommand.setName( DATA_ENTRY_COMMAND );
        keyValueCommand.setParserType( ParserType.KEY_VALUE_PARSER );
        keyValueCommand.setDataset( dataSet );
        keyValueCommand.setCodes( Sets.newHashSet( smsCode ) );
        keyValueCommand.setSuccessMessage( SUCCESS_MESSAGE );

        response = new OutboundMessageResponse();
        response.setResponseObject( GatewayResponse.SUCCESS );
        response.setOk( true );

        incomingSms = new IncomingSms();
        incomingSms.setText( SMS_TEXT );
        incomingSms.setOriginator( ORIGINATOR );
        incomingSms.setCreatedBy( user );

        incomingSmsForCompulsoryCode = new IncomingSms();
        incomingSmsForCompulsoryCode.setText( SMS_TEXT_FOR_COMPULSORY );
        incomingSmsForCompulsoryCode.setOriginator( ORIGINATOR );
        incomingSmsForCompulsoryCode.setCreatedBy( user );

        incomingSmsForCustomSeparator = new IncomingSms();
        incomingSmsForCustomSeparator.setText( SMS_TEXT_FOR_CUSTOM_SEPARATOR );
        incomingSmsForCustomSeparator.setOriginator( ORIGINATOR );
        incomingSmsForCustomSeparator.setCreatedBy( user );
    }
}
