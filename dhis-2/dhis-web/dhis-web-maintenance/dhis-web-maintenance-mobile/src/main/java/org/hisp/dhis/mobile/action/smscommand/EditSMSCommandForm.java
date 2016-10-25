package org.hisp.dhis.mobile.action.smscommand;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.SMSSpecialCharacter;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.UserGroupService;

import com.opensymphony.xwork2.Action;

public class EditSMSCommandForm
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private SMSCommandService smsCommandService;

    public void setSmsCommandService( SMSCommandService smsCommandService )
    {
        this.smsCommandService = smsCommandService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private UserGroupService userGroupService;

    public void setUserGroupService( UserGroupService userGroupService )
    {
        this.userGroupService = userGroupService;
    }

    private TrackedEntityAttributeService trackedEntityAttributeService;

    public TrackedEntityAttributeService getTrackedEntityAttributeService()
    {
        return trackedEntityAttributeService;
    }

    public void setTrackedEntityAttributeService( TrackedEntityAttributeService trackedEntityAttributeService )
    {
        this.trackedEntityAttributeService = trackedEntityAttributeService;
    }

    // -------------------------------------------------------------------------
    // Input && Output
    // -------------------------------------------------------------------------

    private String name;

    private int selectedDataSetID;

    private Integer userGroupID;

    private String codeDataelementOption;

    private String specialCharactersInfo;

    private String trackedEntityAttributeCodes;

    private String programStageDataElementCodes;

    private String separator;

    private String codeSeparator;

    private String defaultMessage;

    private String receivedMessage;

    private String wrongFormatMessage;

    private String noUserMessage;

    private String moreThanOneOrgUnitMessage;

    private String successMessage;

    private Integer completenessMethod;

    private int selectedCommandID = -1;

    private boolean currentPeriodUsedForReporting = false;

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {

        Set<SMSCode> codeSet = new HashSet<>();
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readValue( codeDataelementOption, JsonNode.class );
        JsonNode codes = root.get( "codes" );

        if ( codes != null && codes.size() > 0 )
        {
            codes.iterator().forEachRemaining( code -> {
                if ( code.get( "code" ) != null && code.get( "dataElementId" ) != null && code.get( "optionId" ) != null
                    && code.get( "formula" ) != null )
                {
                    SMSCode c = new SMSCode();

                    c.setCode( code.get( "code" ).asText() );
                    c.setDataElement( dataElementService.getDataElement( code.get( "dataElementId" ).asInt() ) );
                    c.setOptionId( code.get( "optionId" ).asInt() );

                    if ( !StringUtils.isEmpty( code.get( "formula" ).asText() ) )
                    {
                        c.setFormula( code.get( "formula" ).asText() );
                    }
                    else
                    {
                        c.setFormula( null );
                    }

                    codeSet.add( c );
                }
            } );
        }

        Set<SMSSpecialCharacter> specialCharacterSet = new HashSet<>();

        root = mapper.readValue( specialCharactersInfo, JsonNode.class );
        JsonNode specialChars = root.get( "specialCharacters" );

        if ( specialChars != null && StringUtils.isNoneEmpty( specialChars.toString() ) )
        {
            specialCharacterSet = mapper.readValue( specialChars.toString(),
                new TypeReference<HashSet<SMSSpecialCharacter>>()
                {
                } );

            smsCommandService.saveSpecialCharacterSet( specialCharacterSet );
        }

        SMSCommand command = getSMSCommand();

        if ( selectedDataSetID > -1 && command != null )
        {
            if ( command.getParserType() == ParserType.TRACKED_ENTITY_REGISTRATION_PARSER )
            {
                root = mapper.readValue( trackedEntityAttributeCodes, JsonNode.class );
                JsonNode regCodes = root.get( "trackedEntityAttributeCodes" );

                if ( regCodes != null && regCodes.size() > 0 )
                {
                    regCodes.iterator().forEachRemaining( regCode -> {
                        if ( regCode.get( "code" ) != null && regCode.get( "trackedEntityAttributeId" ) != null )
                        {
                            SMSCode c = new SMSCode();
                            c.setCode( regCode.get( "code" ).asText() );
                            c.setTrackedEntityAttribute( trackedEntityAttributeService
                                .getTrackedEntityAttribute( regCode.get( "trackedEntityAttributeId" ).asInt() ) );
                            codeSet.add( c );
                        }
                    } );
                }
            }

            if ( command.getParserType() == ParserType.EVENT_REGISTRATION_PARSER )
            {
                root = mapper.readValue( programStageDataElementCodes, JsonNode.class );
                JsonNode regCodes = root.get( "programStageDataElementCodes" );
                                
                if ( regCodes != null && regCodes.size() > 0 )
                {
                    regCodes.iterator().forEachRemaining( regCode -> {
                        if ( regCode.get( "code" ) != null && regCode.get( "programStageDataElementId" ) != null )
                        {
                            SMSCode c = new SMSCode();
                            c.setCode( regCode.get( "code" ).asText() );      
                            c.setDataElement( dataElementService.getDataElement( regCode.get( "programStageDataElementId" ).asInt() ) );
                            c.setCompulsory( regCode.get( "compulsory" ).asBoolean() );
                            codeSet.add( c );
                        }
                    } );
                }
            }
        }

        if ( codeSet.size() > 0 )
        {
            smsCommandService.save( codeSet );
        }

        if ( selectedDataSetID > -1 && command != null )
        {
            command.setCurrentPeriodUsedForReporting( currentPeriodUsedForReporting );
            command.setName( name );
            command.setSeparator( separator );

            if ( completenessMethod != null )
            {
                command.setCompletenessMethod( completenessMethod );
            }

            // remove codes
            Set<SMSCode> toRemoveCodes = command.getCodes();
            smsCommandService.deleteCodeSet( toRemoveCodes );

            // remove special characters
            Set<SMSSpecialCharacter> toRemoveCharacters = command.getSpecialCharacters();
            smsCommandService.deleteSpecialCharacterSet( toRemoveCharacters );

            command.setCodes( codeSet );

            // message
            command.setDefaultMessage( defaultMessage );
            command.setReceivedMessage( receivedMessage );
            command.setMoreThanOneOrgUnitMessage( moreThanOneOrgUnitMessage );
            command.setNoUserMessage( noUserMessage );
            command.setWrongFormatMessage( wrongFormatMessage );
            command.setSuccessMessage( successMessage );

            if ( userGroupID != null && userGroupID > -1 )
            {
                command.setUserGroup( userGroupService.getUserGroup( userGroupID ) );
            }
            command.setSpecialCharacters( specialCharacterSet );
            smsCommandService.save( command );
        }

        return SUCCESS;
    }

    public Collection<DataSet> getDataSets()
    {
        return dataSetService.getAllDataSets();
    }

    public Set<DataElement> getDataSetElements()
    {
        DataSet d = dataSetService.getDataSet( selectedDataSetID );
        return d != null ? d.getDataElements() : null;
    }

    public SMSCommand getSMSCommand()
    {
        return smsCommandService.getSMSCommand( selectedCommandID );
    }

    public int getSelectedDataSetID()
    {
        return selectedDataSetID;
    }

    public void setSelectedDataSetID( int selectedDataSetID )
    {
        this.selectedDataSetID = selectedDataSetID;
    }

    public String getCodeDataelementOption()
    {
        return codeDataelementOption;
    }

    public void setCodeDataelementOption( String codeDataelementOption )
    {
        this.codeDataelementOption = codeDataelementOption;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public int getSelectedCommandID()
    {
        return selectedCommandID;
    }

    public void setSelectedCommandID( int selectedCommandID )
    {
        this.selectedCommandID = selectedCommandID;
    }

    public String getSeparator()
    {
        return separator;
    }

    public void setSeparator( String separator )
    {
        this.separator = separator;
    }

    public String getCodeSeparator()
    {
        return codeSeparator;
    }

    public void setCodeSeparator( String codeSeparator )
    {
        this.codeSeparator = codeSeparator;
    }

    public String getDefaultMessage()
    {
        return defaultMessage;
    }

    public void setDefaultMessage( String defaultMessage )
    {
        this.defaultMessage = defaultMessage;
    }

    public boolean isCurrentPeriodUsedForReporting()
    {
        return currentPeriodUsedForReporting;
    }

    public void setCurrentPeriodUsedForReporting( String currentPeriodUsedForReporting )
    {
        if ( !StringUtils.isEmpty( currentPeriodUsedForReporting ) )
        {
            this.currentPeriodUsedForReporting = true;
        }
        else
        {
            this.currentPeriodUsedForReporting = false;
        }
    }

    public void setUserGroupID( Integer userGroupID )
    {
        this.userGroupID = userGroupID;
    }

    public void setReceivedMessage( String receivedMessage )
    {
        this.receivedMessage = receivedMessage;
    }

    public String getSpecialCharactersInfo()
    {
        return specialCharactersInfo;
    }

    public void setSpecialCharactersInfo( String specialCharactersInfo )
    {
        this.specialCharactersInfo = specialCharactersInfo;
    }

    public String getWrongFormatMessage()
    {
        return wrongFormatMessage;
    }

    public void setWrongFormatMessage( String wrongFormatMessage )
    {
        this.wrongFormatMessage = wrongFormatMessage;
    }

    public String getNoUserMessage()
    {
        return noUserMessage;
    }

    public void setNoUserMessage( String noUserMessage )
    {
        this.noUserMessage = noUserMessage;
    }

    public String getMoreThanOneOrgUnitMessage()
    {
        return moreThanOneOrgUnitMessage;
    }

    public void setMoreThanOneOrgUnitMessage( String moreThanOneOrgUnitMessage )
    {
        this.moreThanOneOrgUnitMessage = moreThanOneOrgUnitMessage;
    }

    public int getCompletenessMethod()
    {
        return completenessMethod;
    }

    public void setCompletenessMethod( int completenessMethod )
    {
        this.completenessMethod = completenessMethod;
    }

    public String getSuccessMessage()
    {
        return successMessage;
    }

    public void setSuccessMessage( String successMessage )
    {
        this.successMessage = successMessage;
    }

    public String getTrackedEntityAttributeCodes()
    {
        return trackedEntityAttributeCodes;
    }

    public String getProgramStageDataElementCodes()
    {
        return programStageDataElementCodes;
    }

    public String setProgramStageDataElementCodes( String programStageDataElementCodes )
    {
        return this.programStageDataElementCodes = programStageDataElementCodes;
    }

    public void setTrackedEntityAttributeCodes( String trackedEntityAttributeCodes )
    {
        this.trackedEntityAttributeCodes = trackedEntityAttributeCodes;
    }

}
