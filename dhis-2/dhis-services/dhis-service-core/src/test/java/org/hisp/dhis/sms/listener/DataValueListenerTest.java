package org.hisp.dhis.sms.listener;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.Sets;
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
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.SMSSpecialCharacter;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @Author Zubair Asghar.
 */
public class DataValueListenerTest extends DhisConvenienceTest
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

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CompleteDataSetRegistrationService registrationService;

    @Mock
    private DataValueService dataValueService;

    @Mock
    private CategoryService dataElementCategoryService;

    @Mock
    private SMSCommandService smsCommandService;

    @Mock
    private DataSetService dataSetService;

    @Mock
    private IncomingSmsService incomingSmsService;

    @Mock
    private DataElementService dataElementService;

    @Mock
    private MessageSender smsSender;

    @Mock
    private UserService userService;

    @InjectMocks
    private LegacyDataValueSMSListener dataValueSMSListener;

    private CompleteDataSetRegistration fetchedCompleteDataSetRegistration;
    private CompleteDataSetRegistration savedCompleteDataSetRegistration;
    private CompleteDataSetRegistration deletedCompleteDataSetRegistration;

    private DataValue fetchedDataValue;
    private DataValue addedDataValue;
    private DataValue updatedDataValue;

    private DataElement dataElement;
    private DataElement dataElementB;

    private CategoryOptionCombo defaultCategoryOptionCombo;
    private CategoryOptionCombo categoryOptionCombo;

    private OrganisationUnit organisationUnitA;
    private OrganisationUnit organisationUnitB;
    private DataSet dataSet;
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

    private boolean locked = false;
    private boolean smsConfigured = true;

    private String message = "";

    @Before
    public void initTest()
    {
        setUpInstances();

        // Mock for registrationService
        when( registrationService.getCompleteDataSetRegistration( any(), any(), any(), any() ) )
            .thenReturn( fetchedCompleteDataSetRegistration );

        doAnswer( invocation -> {
            deletedCompleteDataSetRegistration = (CompleteDataSetRegistration) invocation.getArguments()[0];
            return deletedCompleteDataSetRegistration;
        }).when( registrationService ).deleteCompleteDataSetRegistration( any() );

        // Mock for dataValueService
        when( dataValueService.getDataValue( any(), any(), any(), any() ) )
            .thenReturn( fetchedDataValue );


        doAnswer( invocation -> {
            updatedDataValue = (DataValue) invocation.getArguments()[0];
            return updatedDataValue;
        }).when( dataValueService ).updateDataValue( any() );

        // Mock for userService
        when( userService.getUser( anyString() ) ).thenReturn( user );

        // Mock for dataElementCategoryService
        when( dataElementCategoryService.getDefaultCategoryOptionCombo() ).thenReturn( defaultCategoryOptionCombo );

        when ( dataElementCategoryService.getCategoryOptionCombo( anyInt() ) ).thenReturn( categoryOptionCombo );

        // Mock for smsCommandService
        when( smsCommandService.getSMSCommand( anyString(), any() ) ).thenReturn( keyValueCommand );

        // Mock for dataSetService
        when( dataSetService.isLocked( any(), any( DataSet.class ), any(), any(), any(), any() ) ).thenReturn( locked );

        // Mock for incomingSmsService
        doAnswer( invocation -> {
            updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
            return updatedIncomingSms;
        }).when( incomingSmsService ).update( any() );

        // Mock for smsSender
        when( smsSender.isConfigured() ).thenReturn( smsConfigured );

        when( smsSender.sendMessage( any(), any(), anyString() ) ).thenAnswer( invocation -> {
            message = invocation.getArgument(1);
            return response;
        });
    }

    @Test
    public void testAccept()
    {
        incomingSms.setUser( user );
        boolean result = dataValueSMSListener.accept( incomingSms );

        assertTrue( result );

        result = dataValueSMSListener.accept( null );

        assertFalse( result );
    }

    @Test
    public void testReceive()
    {
        incomingSms.setUser( user );
        dataValueSMSListener.receive( incomingSms );

        assertNotNull( updatedIncomingSms );
        assertEquals( SmsMessageStatus.PROCESSED, updatedIncomingSms.getStatus() );
        assertTrue( updatedIncomingSms.isParsed() );
    }

    @Test( expected = SMSParserException.class )
    public void testIfDataSetIsLocked() throws Exception
    {
        incomingSms.setUser( user );
        when( dataSetService.isLocked( any(), any(DataSet.class ), any(), any(), any(), any() ) ).thenReturn( true );
        dataValueSMSListener.receive( incomingSms );

        verify( smsCommandService, times( 1 ) ).getSMSCommand( anyString(), any() );
        verify( dataSetService, times( 1 ) ).isLocked( user, any(DataSet.class ), any(), any(), any(), any() );
        verify( incomingSmsService, never() ).update( any() );
    }

    @Test
    public void testIfUserHasNoOu()
    {
        incomingSms.setUser( userWithNoOu );
        when( userService.getUser( anyString() ) ).thenReturn( userWithNoOu );
        dataValueSMSListener.receive( incomingSms );

        assertEquals( message, SMSCommand.NO_USER_MESSAGE );
        assertNull( updatedIncomingSms );
        verify( dataSetService, never() ).isLocked( any(), any(DataSet.class ), any(), any(), any(), any() );
    }

    @Test
    public void testIfUserHasMultipleOUs()
    {
        incomingSms.setUser( userwithMultipleOu );

        when( userService.getUser( anyString() ) ).thenReturn( userwithMultipleOu );
        when( userService.getUsersByPhoneNumber( anyString() ) ).thenReturn( Arrays.asList( userwithMultipleOu ) );

        dataValueSMSListener.receive( incomingSms );

        assertEquals( message, SMSCommand.MORE_THAN_ONE_ORGUNIT_MESSAGE );
        assertNull( updatedIncomingSms );
        verify( dataSetService, never() ).isLocked( any(), any(DataSet.class ), any(), any(), any(), any() );

        keyValueCommand.setMoreThanOneOrgUnitMessage( MORE_THAN_ONE_OU );
        dataValueSMSListener.receive( incomingSms );

        // system will use custom message
        assertEquals( message, MORE_THAN_ONE_OU );
    }

    @Test
    public void testIfDiffUsersHasSameOU()
    {
        incomingSms.setUser( user );

        when( userService.getUser( anyString() ) ).thenReturn( user );
        when( userService.getUsersByPhoneNumber( anyString() ) ).thenReturn( Arrays.asList( user, userB ) );

        dataValueSMSListener.receive( incomingSms );

        assertEquals( message, SUCCESS_MESSAGE );

        when( userService.getUsersByPhoneNumber( anyString() ) ).thenReturn( Arrays.asList( user, userC ) );

        keyValueCommand.setMoreThanOneOrgUnitMessage( MORE_THAN_ONE_OU );
        dataValueSMSListener.receive( incomingSms );

        // system will use custom message
        assertEquals( message, MORE_THAN_ONE_OU );
    }

    @Test
    public void testIfCommandHasCorrectFormat()
    {
        dataValueSMSListener.receive( incomingSmsForCustomSeparator );

        assertEquals( message, SMSCommand.WRONG_FORMAT_MESSAGE );
        assertNull( updatedIncomingSms );
        verify( dataSetService, never() ).isLocked( any(), any(DataSet.class ), any(), any(), any(), any() );

        keyValueCommand.setWrongFormatMessage( WRONG_FORMAT );
        dataValueSMSListener.receive( incomingSmsForCustomSeparator );

        // system will use custom message
        assertEquals( WRONG_FORMAT, message );
    }

    @Test
    public void testIfMandatoryParameterMissing()
    {
        keyValueCommand.getCodes().add( smsCodeForcompulsory );
        keyValueCommand.setSeparator( null );
        keyValueCommand.setCodeValueSeparator( null );
        incomingSmsForCompulsoryCode.setText( SMS_TEXT );

        dataValueSMSListener.receive( incomingSmsForCompulsoryCode );

        assertEquals( message, SMSCommand.PARAMETER_MISSING );

        incomingSmsForCompulsoryCode.setText( SMS_TEXT_FOR_COMPULSORY );

        dataValueSMSListener.receive( incomingSmsForCompulsoryCode );

        assertEquals( keyValueCommand.getSuccessMessage(), message );

        incomingSmsForCompulsoryCode.setText( SMS_TEXT_FOR_COMPULSORY2 );

        dataValueSMSListener.receive( incomingSmsForCompulsoryCode );

        assertEquals( keyValueCommand.getSuccessMessage(), message );
    }

    @Test
    public void testDefaultSeparator()
    {
        keyValueCommand.setSeparator( null );
        keyValueCommand.setCodeValueSeparator( null );

        // = is default separator
        dataValueSMSListener.receive( incomingSms );

        assertNotNull( updatedIncomingSms );
        assertEquals( SmsMessageStatus.PROCESSED, updatedIncomingSms.getStatus() );
        assertTrue( updatedIncomingSms.isParsed() );
    }

    @Test
    public void testCustomSeparator()
    {
        keyValueCommand.setSeparator( "." );
        keyValueCommand.setCodeValueSeparator( "." );
        keyValueCommand.setWrongFormatMessage( null );

        dataValueSMSListener.receive( incomingSmsForCustomSeparator );

        assertNotNull( updatedIncomingSms );
        assertEquals( SmsMessageStatus.PROCESSED, updatedIncomingSms.getStatus() );
        assertTrue( updatedIncomingSms.isParsed() );

        // when custom separator is empty space
        keyValueCommand.setSeparator( " " );
        keyValueCommand.setCodeValueSeparator( " " );
        dataValueSMSListener.receive( incomingSmsForCustomSeparator );

        assertEquals( message, SMSCommand.WRONG_FORMAT_MESSAGE );
    }

    private void setUpInstances()
    {
        organisationUnitA = createOrganisationUnit( 'O' );
        organisationUnitB = createOrganisationUnit( 'P' );
        dataSet = createDataSet( 'D' );
        period = createPeriod( new Date(), new Date() );

        user = createUser( 'U' );
        user.setPhoneNumber( ORIGINATOR );
        user.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        userB = createUser( 'B' );
        userB.setPhoneNumber( ORIGINATOR );
        userB.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );

        userC = createUser( 'C' );
        userC.setPhoneNumber( ORIGINATOR );
        userC.setOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );

        userWithNoOu = createUser( 'V' );
        userWithNoOu.setPhoneNumber( null );
        userWithNoOu.setOrganisationUnits( null );

        userwithMultipleOu = createUser( 'W' );
        userwithMultipleOu.setPhoneNumber( ORIGINATOR );
        userwithMultipleOu.setOrganisationUnits( Sets.newHashSet( organisationUnitA, organisationUnitB ) );

        dataElement = createDataElement( 'D' );
        dataElement.setValueType(ValueType.TEXT );
        defaultCategoryOptionCombo = createCategoryOptionCombo( 'D' );
        categoryOptionCombo = createCategoryOptionCombo( 'C' );

        dataElementB = createDataElement( 'B' );
        dataElementB.setValueType(ValueType.TEXT );

        fetchedDataValue = createDataValue( dataElement, period, organisationUnitA, FETCHED_DATA_VALUE, categoryOptionCombo );

        fetchedCompleteDataSetRegistration = new CompleteDataSetRegistration( dataSet, period, organisationUnitA,
            categoryOptionCombo, new Date(), STORED_BY, new Date(), LAST_UPDATED_BY, true );

        smsCode = new SMSCode();
        smsCode.setCode( "de" );
        smsCode.setDataElement( dataElement );

        smsCodeForcompulsory = new SMSCode();
        smsCodeForcompulsory.setCode( "deb" );
        smsCodeForcompulsory.setDataElement( dataElementB );
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
        incomingSms.setUser( user );

        incomingSmsForCompulsoryCode = new IncomingSms();
        incomingSmsForCompulsoryCode.setText( SMS_TEXT_FOR_COMPULSORY );
        incomingSmsForCompulsoryCode.setOriginator( ORIGINATOR );
        incomingSmsForCompulsoryCode.setUser( user );

        incomingSmsForCustomSeparator = new IncomingSms();
        incomingSmsForCustomSeparator.setText( SMS_TEXT_FOR_CUSTOM_SEPARATOR );
        incomingSmsForCustomSeparator.setOriginator( ORIGINATOR );
        incomingSmsForCustomSeparator.setUser( user );
    }
}
