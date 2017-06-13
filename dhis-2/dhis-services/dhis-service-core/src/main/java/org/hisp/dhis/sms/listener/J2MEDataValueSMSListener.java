package org.hisp.dhis.sms.listener;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Resource;

public class J2MEDataValueSMSListener
    implements IncomingSmsListener
{

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataElementCategoryService dataElementCategoryService;

    @Autowired
    private SMSCommandService smsCommandService;

    @Autowired
    private UserService userService;

    @Autowired
    private CompleteDataSetRegistrationService registrationService;

    @Autowired
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    // -------------------------------------------------------------------------
    // IncomingSmsListener implementation
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public boolean accept( IncomingSms sms )
    {
        return smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ), ParserType.J2ME_PARSER ) != null;
    }

    @Transactional
    @Override
    public void receive( IncomingSms sms )
    {
        String message = sms.getText();

        SMSCommand smsCommand = smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.J2ME_PARSER );

        String token[] = message.split( "!" );
        Map<String, String> parsedMessage = this.parse( token[1], smsCommand );

        String senderPhoneNumber = StringUtils.replace( sms.getOriginator(), "+", "" );
        Collection<OrganisationUnit> orgUnits = SmsUtils.getOrganisationUnitsByPhoneNumber( senderPhoneNumber,
            userService.getUsersByPhoneNumber( senderPhoneNumber ) );

        if ( orgUnits == null || orgUnits.size() == 0 )
        {
            if ( StringUtils.isEmpty( smsCommand.getNoUserMessage() ) )
            {
                throw new SMSParserException( SMSCommand.NO_USER_MESSAGE );
            }
            else
            {
                throw new SMSParserException( smsCommand.getNoUserMessage() );
            }
        }

        OrganisationUnit orgUnit = SmsUtils.selectOrganisationUnit( orgUnits, parsedMessage, smsCommand );
        Period period = this.getPeriod( token[0].trim(), smsCommand.getDataset().getPeriodType() );
        boolean valueStored = false;

        for ( SMSCode code : smsCommand.getCodes() )
        {
            if ( parsedMessage.containsKey( code.getCode().toUpperCase() ) )
            {
                storeDataValue( senderPhoneNumber, orgUnit, parsedMessage, code, smsCommand, period,
                    smsCommand.getDataset() );
                valueStored = true;
            }
        }

        if ( parsedMessage.isEmpty() || !valueStored )
        {
            if ( StringUtils.isEmpty( smsCommand.getDefaultMessage() ) )
            {
                throw new SMSParserException( "No values reported for command '" + smsCommand.getName() + "'" );
            }
            else
            {
                throw new SMSParserException( smsCommand.getDefaultMessage() );
            }
        }

        this.registerCompleteDataSet( smsCommand.getDataset(), period, orgUnit, "mobile" );

        this.sendSuccessFeedback( senderPhoneNumber, smsCommand, parsedMessage, period, orgUnit );
    }

    private Map<String, String> parse( String sms, SMSCommand smsCommand )
    {
        String[] keyValuePairs = null;

        if ( sms.indexOf( "#" ) > -1 )
        {
            keyValuePairs = sms.split( "#" );
        }
        else
        {
            keyValuePairs = new String[1];
            keyValuePairs[0] = sms;
        }

        Map<String, String> keyValueMap = new HashMap<>();
        for ( String keyValuePair : keyValuePairs )
        {
            String[] token = keyValuePair.split( Pattern.quote( smsCommand.getSeparator() ) );
            keyValueMap.put( token[0], token[1] );
        }

        return keyValueMap;
    }

    private void storeDataValue( String sender, OrganisationUnit orgUnit, Map<String, String> parsedMessage,
        SMSCode code, SMSCommand command, Period period, DataSet dataset )
    {
        String upperCaseCode = code.getCode().toUpperCase();

        String storedBy = SmsUtils.getUser( sender, command, userService.getUsersByPhoneNumber( sender ) )
            .getUsername();

        if ( StringUtils.isBlank( storedBy ) )
        {
            storedBy = "[unknown] from [" + sender + "]";
        }

        DataElementCategoryOptionCombo optionCombo = dataElementCategoryService
            .getDataElementCategoryOptionCombo( code.getOptionId() );

        DataValue dv = dataValueService.getDataValue( code.getDataElement(), period, orgUnit, optionCombo );

        String value = parsedMessage.get( upperCaseCode );
        if ( !StringUtils.isEmpty( value ) )
        {
            boolean newDataValue = false;
            if ( dv == null )
            {
                dv = new DataValue();
                dv.setCategoryOptionCombo( optionCombo );
                dv.setSource( orgUnit );
                dv.setDataElement( code.getDataElement() );
                dv.setPeriod( period );
                dv.setComment( "" );
                newDataValue = true;
            }

            if ( ValueType.BOOLEAN == dv.getDataElement().getValueType() )
            {
                if ( "Y".equals( value.toUpperCase() ) || "YES".equals( value.toUpperCase() ) )
                {
                    value = "true";
                }
                else if ( "N".equals( value.toUpperCase() ) || "NO".equals( value.toUpperCase() ) )
                {
                    value = "false";
                }
            }

            dv.setValue( value );
            dv.setLastUpdated( new java.util.Date() );
            dv.setStoredBy( storedBy );

            if ( ValidationUtils.dataValueIsValid( value, dv.getDataElement() ) != null )
            {
                return; // not a valid value for data element
            }

            if ( newDataValue )
            {
                dataValueService.addDataValue( dv );
            }
            else
            {
                dataValueService.updateDataValue( dv );
            }
        }
    }

    private void registerCompleteDataSet( DataSet dataSet, Period period, OrganisationUnit organisationUnit,
        String storedBy )
    {
        CompleteDataSetRegistration registration = new CompleteDataSetRegistration();

        DataElementCategoryOptionCombo optionCombo = dataElementCategoryService
            .getDefaultDataElementCategoryOptionCombo(); // TODO

        if ( registrationService.getCompleteDataSetRegistration( dataSet, period, organisationUnit,
            optionCombo ) == null )
        {
            registration.setDataSet( dataSet );
            registration.setPeriod( period );
            registration.setSource( organisationUnit );
            registration.setDate( new Date() );
            registration.setStoredBy( storedBy );
            registration.setPeriodName( registration.getPeriod().toString() );
            registrationService.saveCompleteDataSetRegistration( registration, false );
        }
    }

    private void sendSuccessFeedback( String sender, SMSCommand command, Map<String, String> parsedMessage,
        Period period, OrganisationUnit orgunit )
    {
        String reportBack = "Thank you! Values entered: ";
        String notInReport = "Missing values for: ";
        boolean missingElements = false;

        for ( SMSCode code : command.getCodes() )
        {
            DataElementCategoryOptionCombo optionCombo = dataElementCategoryService
                .getDataElementCategoryOptionCombo( code.getOptionId() );

            DataValue dv = dataValueService.getDataValue( code.getDataElement(), period, orgunit, optionCombo );

            if ( dv == null && !StringUtils.isEmpty( code.getCode() ) )
            {
                notInReport += code.getCode() + ",";
                missingElements = true;
            }
            else if ( dv != null )
            {
                String value = dv.getValue();

                if ( ValueType.BOOLEAN == dv.getDataElement().getValueType() )
                {
                    if ( "true".equals( value ) )
                    {
                        value = "Yes";
                    }
                    else if ( "false".equals( value ) )
                    {
                        value = "No";
                    }
                }

                reportBack += code.getCode() + "=" + value + " ";
            }
        }

        notInReport = notInReport.substring( 0, notInReport.length() - 1 );

        if ( missingElements )
        {
            reportBack += notInReport;
        }

        if ( command.getSuccessMessage() != null && !StringUtils.isEmpty( command.getSuccessMessage() ) )
        {
            reportBack = command.getSuccessMessage();
        }

        smsSender.sendMessage( null, reportBack, sender );
    }

    public Period getPeriod( String periodName, PeriodType periodType )
        throws IllegalArgumentException
    {
        if ( periodType instanceof DailyPeriodType )
        {
            return periodType.createPeriod( DateUtils.getMediumDate( periodName ) );
        }

        if ( periodType instanceof WeeklyPeriodType )
        {
            return periodType.createPeriod( DateUtils.getMediumDate( periodName ) );
        }

        if ( periodType instanceof MonthlyPeriodType )
        {
            int dashIndex = periodName.indexOf( '-' );

            if ( dashIndex < 0 )
            {
                return null;
            }

            int month = Integer.parseInt( periodName.substring( 0, dashIndex ) );
            int year = Integer.parseInt( periodName.substring( dashIndex + 1, periodName.length() ) );

            Calendar cal = Calendar.getInstance();
            cal.set( Calendar.YEAR, year );
            cal.set( Calendar.MONTH, month );

            return periodType.createPeriod( cal.getTime() );
        }

        if ( periodType instanceof YearlyPeriodType )
        {
            Calendar cal = Calendar.getInstance();
            cal.set( Calendar.YEAR, Integer.parseInt( periodName ) );

            return periodType.createPeriod( cal.getTime() );
        }

        if ( periodType instanceof QuarterlyPeriodType )
        {
            Calendar cal = Calendar.getInstance();

            int month = 0;

            if ( periodName.substring( 0, periodName.indexOf( " " ) ).equals( "Jan" ) )
            {
                month = 1;
            }
            else if ( periodName.substring( 0, periodName.indexOf( " " ) ).equals( "Apr" ) )
            {
                month = 4;
            }
            else if ( periodName.substring( 0, periodName.indexOf( " " ) ).equals( "Jul" ) )
            {
                month = 6;
            }
            else if ( periodName.substring( 0, periodName.indexOf( " " ) ).equals( "Oct" ) )
            {
                month = 10;
            }

            int year = Integer.parseInt( periodName.substring( periodName.lastIndexOf( " " ) + 1 ) );

            cal.set( Calendar.MONTH, month );
            cal.set( Calendar.YEAR, year );

            if ( month != 0 )
            {
                return periodType.createPeriod( cal.getTime() );
            }
        }

        throw new IllegalArgumentException(
            "Couldn't make a period of type " + periodType.getName() + " and name " + periodName );
    }
}