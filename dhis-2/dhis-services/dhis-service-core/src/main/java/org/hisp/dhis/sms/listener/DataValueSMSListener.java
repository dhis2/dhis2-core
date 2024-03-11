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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
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
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.sms.command.CompletenessMethod;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.SMSSpecialCharacter;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.system.util.SmsUtils;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Transactional
public class DataValueSMSListener
    extends BaseSMSListener
{
    private static final String DATASET_LOCKED = "Dataset: %s is locked for period: %s";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private CompleteDataSetRegistrationService registrationService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private CategoryService dataElementCategoryService;

    @Autowired
    private SMSCommandService smsCommandService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    @Override
    protected void postProcess( IncomingSms sms, SMSCommand smsCommand, Map<String, String> parsedMessage )
    {
        String message = sms.getText();

        Date date = SmsUtils.lookForDate( message );
        String senderPhoneNumber = StringUtils.replace( sms.getOriginator(), "+", "" );

        OrganisationUnit orgUnit = null;

        if ( getOrganisationUnits( sms ).iterator().hasNext() )
        {
           orgUnit  = getOrganisationUnits( sms ).iterator().next();
        }

        Period period = getPeriod( smsCommand, date );
        DataSet dataSet = smsCommand.getDataset();

        if ( dataSetService.isLocked( null, dataSet, period, orgUnit, dataElementCategoryService.getDefaultCategoryOptionCombo(), null ) )
        {
            sendFeedback( String.format( DATASET_LOCKED, dataSet.getUid(), period.getName() ), sms.getOriginator(), ERROR );

            throw new SMSParserException( String.format( DATASET_LOCKED, dataSet.getUid(), period.getName() ) );
        }

        boolean valueStored = false;

        for ( SMSCode code : smsCommand.getCodes() )
        {
            if ( parsedMessage.containsKey( code.getCode() ) )
            {
                valueStored = storeDataValue( sms, orgUnit, parsedMessage, code, smsCommand, date,
                    smsCommand.getDataset() );
            }
        }

        if ( parsedMessage.isEmpty() )
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
        else if ( !valueStored )
        {
            if ( StringUtils.isEmpty( smsCommand.getWrongFormatMessage() ) )
            {
                throw new SMSParserException( SMSCommand.WRONG_FORMAT_MESSAGE );
            }
            else
            {
                throw new SMSParserException( smsCommand.getWrongFormatMessage() );
            }
        }

        markCompleteDataSet( sms, orgUnit, parsedMessage, smsCommand, date );
        sendSuccessFeedback( senderPhoneNumber, smsCommand, parsedMessage, date, orgUnit );

        update( sms,  SmsMessageStatus.PROCESSED, true );
    }

    @Override
    protected SMSCommand getSMSCommand( IncomingSms sms )
    {
        return smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ), ParserType.KEY_VALUE_PARSER );
    }

    private Period getPeriod( SMSCommand command, Date date )
    {
        Period period = null;
        period = command.getDataset().getPeriodType().createPeriod();
        PeriodType periodType = period.getPeriodType();

        if ( command.isCurrentPeriodUsedForReporting() )
        {
            period = periodType.createPeriod( new Date() );
        }
        else
        {
            period = periodType.getPreviousPeriod( period );
        }

        if ( date != null )
        {
            period = periodType.createPeriod( date );
        }

        return period;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private boolean storeDataValue( IncomingSms sms, OrganisationUnit orgunit, Map<String, String> parsedMessage,
        SMSCode code, SMSCommand command, Date date, DataSet dataSet )
    {
        String sender = sms.getOriginator();
        String storedBy = SmsUtils.getUser( sender, command, Collections.singletonList( getUser( sms ) ) )
            .getUsername();

        if ( StringUtils.isBlank( storedBy ) )
        {
            storedBy = "[unknown] from [" + sender + "]";
        }

        CategoryOptionCombo optionCombo = dataElementCategoryService
            .getCategoryOptionCombo( code.getOptionId() );

        Period period = getPeriod( command, date );

        DataValue dv = dataValueService.getDataValue( code.getDataElement(), period, orgunit, optionCombo );

        String value = parsedMessage.get( code.getCode() );

        Set<SMSSpecialCharacter> specialCharacters = command.getSpecialCharacters();

        for ( SMSSpecialCharacter each : specialCharacters )
        {
            if ( each.getName().equalsIgnoreCase( value ) )
            {
                value = each.getValue();
                break;
            }
        }

        if ( !StringUtils.isEmpty( value ) )
        {
            boolean newDataValue = false;

            if ( dv == null )
            {
                dv = new DataValue();
                dv.setCategoryOptionCombo( optionCombo );
                dv.setSource( orgunit );
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
            else if ( dv.getDataElement().getValueType().isInteger() )
            {
                try
                {
                    Integer.parseInt( value );
                }
                catch ( NumberFormatException e )
                {
                    return false;
                }
            }

            dv.setValue( value );
            dv.setLastUpdated( new java.util.Date() );
            dv.setStoredBy( storedBy );

            if ( newDataValue )
            {
                dataValueService.addDataValue( dv );
            }
            else
            {
                dataValueService.updateDataValue( dv );
            }
        }

        if ( code.getFormula() != null )
        {
            try
            {
                String formula = code.getFormula();

                String targetDataElementId = formula.substring( 1, formula.length() );
                String operation = String.valueOf( formula.charAt( 0 ) );

                DataElement targetDataElement = dataElementService
                    .getDataElement( Integer.parseInt( targetDataElementId ) );

                if ( targetDataElement == null )
                {
                    return false;
                }

                DataValue targetDataValue = dataValueService.getDataValue( targetDataElement, period, orgunit,
                    dataElementCategoryService.getDefaultCategoryOptionCombo() );

                int targetValue = 0;
                boolean newTargetDataValue = false;

                if ( targetDataValue == null )
                {
                    targetDataValue = new DataValue();
                    targetDataValue.setCategoryOptionCombo(
                        dataElementCategoryService.getDefaultCategoryOptionCombo() );
                    targetDataValue.setSource( orgunit );
                    targetDataValue.setDataElement( targetDataElement );
                    targetDataValue.setPeriod( period );
                    targetDataValue.setComment( "" );
                    newTargetDataValue = true;
                }
                else
                {
                    targetValue = Integer.parseInt( targetDataValue.getValue() );
                }

                if ( operation.equals( "+" ) )
                {
                    targetValue = targetValue + Integer.parseInt( value );
                }
                else if ( operation.equals( "-" ) )
                {
                    targetValue = targetValue - Integer.parseInt( value );
                }

                targetDataValue.setValue( String.valueOf( targetValue ) );
                targetDataValue.setLastUpdated( new java.util.Date() );
                targetDataValue.setStoredBy( storedBy );

                if ( newTargetDataValue )
                {
                    dataValueService.addDataValue( targetDataValue );
                }
                else
                {
                    dataValueService.updateDataValue( targetDataValue );
                }

            }
            catch ( Exception e )
            {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private void markCompleteDataSet( IncomingSms sms, OrganisationUnit orgunit, Map<String, String> parsedMessage,
        SMSCommand command, Date date )
    {
        String sender = sms.getOriginator();

        Period period = null;
        int numberOfEmptyValue = 0;
        for ( SMSCode code : command.getCodes() )
        {

            CategoryOptionCombo optionCombo = dataElementCategoryService
                .getCategoryOptionCombo( code.getOptionId() );

            period = getPeriod( command, date );

            DataValue dv = dataValueService.getDataValue( code.getDataElement(), period, orgunit, optionCombo );

            if ( dv == null && !StringUtils.isEmpty( code.getCode() ) )
            {
                numberOfEmptyValue++;
            }
        }

        // Check completeness method
        if ( command.getCompletenessMethod() == CompletenessMethod.ALL_DATAVALUE )
        {
            if ( numberOfEmptyValue > 0 )
            {
                return;
            }
        }
        else if ( command.getCompletenessMethod() == CompletenessMethod.AT_LEAST_ONE_DATAVALUE )
        {
            if ( numberOfEmptyValue == command.getCodes().size() )
            {
                return;
            }
        }
        else if ( command.getCompletenessMethod() == CompletenessMethod.DO_NOT_MARK_COMPLETE )
        {
            return;
        }

        // Go through the complete process
        String storedBy = SmsUtils.getUser( sender, command, Collections.singletonList( getUser( sms ) ) )
            .getUsername();

        if ( StringUtils.isBlank( storedBy ) )
        {
            storedBy = "[unknown] from [" + sender + "]";
        }

        // If new values are submitted re-register as complete
        deregisterCompleteDataSet( command.getDataset(), period, orgunit );
        registerCompleteDataSet( command.getDataset(), period, orgunit, storedBy );
    }

    protected void sendSuccessFeedback( String sender, SMSCommand command, Map<String, String> parsedMessage, Date date,
        OrganisationUnit orgunit )
    {
        String reportBack = "Thank you! Values entered: ";
        String notInReport = "Missing values for: ";

        Period period = null;

        Map<String, DataValue> codesWithDataValues = new TreeMap<>();
        List<String> codesWithoutDataValues = new ArrayList<>();

        for ( SMSCode code : command.getCodes() )
        {

            CategoryOptionCombo optionCombo = dataElementCategoryService
                .getCategoryOptionCombo( code.getOptionId() );

            period = getPeriod( command, date );

            DataValue dv = dataValueService.getDataValue( code.getDataElement(), period, orgunit, optionCombo );

            if ( dv == null && !StringUtils.isEmpty( code.getCode() ) )
            {
                codesWithoutDataValues.add( code.getCode() );
            }
            else if ( dv != null )
            {
                codesWithDataValues.put( code.getCode(), dv );
            }
        }

        for ( String key : codesWithDataValues.keySet() )
        {
            DataValue dv = codesWithDataValues.get( key );
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
            reportBack += key + "=" + value + " ";
        }

        Collections.sort( codesWithoutDataValues );

        for ( String key : codesWithoutDataValues )
        {
            notInReport += key + ",";
        }

        notInReport = notInReport.substring( 0, notInReport.length() - 1 );

        if ( smsSender.isConfigured() )
        {
            if ( command.getSuccessMessage() != null && !StringUtils.isEmpty( command.getSuccessMessage() ) )
            {
                smsSender.sendMessage( null, command.getSuccessMessage(), sender );
            }
            else
            {
                smsSender.sendMessage( null, reportBack, sender );
            }
        }
        else
        {
            Log.info( "No sms configuration found." );
        }
    }

    private void registerCompleteDataSet( DataSet dataSet, Period period, OrganisationUnit organisationUnit,
        String storedBy )
    {
        CompleteDataSetRegistration registration = new CompleteDataSetRegistration();

        CategoryOptionCombo optionCombo = dataElementCategoryService
            .getDefaultCategoryOptionCombo(); // TODO

        if ( registrationService.getCompleteDataSetRegistration( dataSet, period, organisationUnit,
            optionCombo ) == null )
        {
            registration.setDataSet( dataSet );
            registration.setPeriod( period );
            registration.setSource( organisationUnit );
            registration.setDate( new Date() );
            registration.setStoredBy( storedBy );
            registration.setPeriodName( registration.getPeriod().toString() );
            registrationService.saveCompleteDataSetRegistration( registration );
        }
    }

    private void deregisterCompleteDataSet( DataSet dataSet, Period period, OrganisationUnit organisationUnit )
    {
        CategoryOptionCombo optionCombo = dataElementCategoryService
            .getDefaultCategoryOptionCombo(); // TODO

        CompleteDataSetRegistration registration = registrationService.getCompleteDataSetRegistration( dataSet, period,
            organisationUnit, optionCombo );

        if ( registration != null )
        {
            registrationService.deleteCompleteDataSetRegistration( registration );
        }
    }
}
