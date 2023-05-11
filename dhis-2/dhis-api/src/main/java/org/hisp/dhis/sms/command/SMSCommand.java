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
package org.hisp.dhis.sms.command;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.user.UserGroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

@JacksonXmlRootElement( localName = "smscommand", namespace = DxfNamespaces.DXF_2_0 )
public class SMSCommand
    extends BaseIdentifiableObject
    implements MetadataObject
{
    public static final String WRONG_FORMAT_MESSAGE = "Wrong command format";

    public static final String MORE_THAN_ONE_ORGUNIT_MESSAGE = "Found more than one org unit for this number. Please specify one organisation unit";

    public static final String NO_USER_MESSAGE = "No user associated with this phone number. Please contact your supervisor.";

    public static final String ALERT_FEEDBACK = "Your alert message sent";

    public static final String PARAMETER_MISSING = "Mandatory parameter is missing";

    public static final String SUCCESS_MESSAGE = "Command has been processed successfully";

    public static final String NO_OU_FOR_PROGRAM = "Program is not assigned to user organisation unit.";

    private ParserType parserType = ParserType.ALERT_PARSER;

    private DataSet dataset;

    private Set<SMSCode> codes = new HashSet<>();

    private UserGroup userGroup;

    private Program program;

    private ProgramStage programStage;

    private Set<SMSSpecialCharacter> specialCharacters = new HashSet<>();

    private boolean currentPeriodUsedForReporting = false;

    private CompletenessMethod completenessMethod = CompletenessMethod.AT_LEAST_ONE_DATAVALUE;

    private String defaultMessage;

    private String receivedMessage;

    private String wrongFormatMessage;

    private String noUserMessage;

    private String moreThanOneOrgUnitMessage;

    private String successMessage;

    private String separator;

    private String codeValueSeparator;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SMSCommand( String name, ParserType parserType, String separator, DataSet dataset,
        Set<SMSCode> codes, String codeSeparator, String defaultMessage, UserGroup userGroup, String receivedMessage,
        Set<SMSSpecialCharacter> specialCharacters )
    {
        super();
        this.name = name;
        this.parserType = parserType;
        this.separator = separator;
        this.dataset = dataset;
        this.codes = codes;
        this.codeValueSeparator = codeSeparator;
        this.defaultMessage = defaultMessage;
        this.userGroup = userGroup;
        this.receivedMessage = receivedMessage;
        this.specialCharacters = specialCharacters;
    }

    public SMSCommand()
    {

    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "dataset", namespace = DxfNamespaces.DXF_2_0 )
    public DataSet getDataset()
    {
        return dataset;
    }

    public void setDataset( DataSet dataset )
    {
        this.dataset = dataset;
    }

    @JsonProperty( value = "smsCodes" )
    @JacksonXmlElementWrapper( localName = "smsCodes", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "smsCode", namespace = DxfNamespaces.DXF_2_0 )
    public Set<SMSCode> getCodes()
    {
        return codes;
    }

    public void setCodes( Set<SMSCode> codes )
    {
        this.codes = codes;
    }

    @JsonProperty
    @JacksonXmlProperty
    public ParserType getParserType()
    {
        return parserType;
    }

    public void setParserType( ParserType parserType )
    {
        this.parserType = parserType;
    }

    @JsonProperty
    @JacksonXmlProperty
    public boolean isCurrentPeriodUsedForReporting()
    {
        return currentPeriodUsedForReporting;
    }

    public void setCurrentPeriodUsedForReporting( Boolean currentPeriodUsedForReporting )
    {
        this.currentPeriodUsedForReporting = currentPeriodUsedForReporting;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getReceivedMessage()
    {
        return receivedMessage != null ? receivedMessage : SUCCESS_MESSAGE;
    }

    public void setReceivedMessage( String receivedMessage )
    {
        this.receivedMessage = receivedMessage;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getSeparator()
    {
        return separator;
    }

    public void setSeparator( String separator )
    {
        this.separator = separator;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getCodeValueSeparator()
    {
        return codeValueSeparator;
    }

    public void setCodeValueSeparator( String codeSeparator )
    {
        this.codeValueSeparator = codeSeparator;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getDefaultMessage()
    {
        return defaultMessage;
    }

    public void setDefaultMessage( String defaultMessage )
    {
        this.defaultMessage = defaultMessage;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "specialCharacters", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "specialCharacter", namespace = DxfNamespaces.DXF_2_0 )
    public Set<SMSSpecialCharacter> getSpecialCharacters()
    {
        return specialCharacters;
    }

    public void setSpecialCharacters( Set<SMSSpecialCharacter> specialCharacters )
    {
        this.specialCharacters = specialCharacters;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getWrongFormatMessage()
    {
        return wrongFormatMessage != null ? wrongFormatMessage : WRONG_FORMAT_MESSAGE;
    }

    public void setWrongFormatMessage( String wrongFormatMessage )
    {
        this.wrongFormatMessage = wrongFormatMessage;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getNoUserMessage()
    {
        return noUserMessage != null ? noUserMessage : NO_USER_MESSAGE;
    }

    public void setNoUserMessage( String noUserMessage )
    {
        this.noUserMessage = noUserMessage;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getSuccessMessage()
    {
        return successMessage != null ? successMessage : SUCCESS_MESSAGE;
    }

    public void setSuccessMessage( String successMessage )
    {
        this.successMessage = successMessage;
    }

    @JsonProperty
    @JacksonXmlProperty
    public String getMoreThanOneOrgUnitMessage()
    {
        return moreThanOneOrgUnitMessage != null ? moreThanOneOrgUnitMessage : MORE_THAN_ONE_ORGUNIT_MESSAGE;
    }

    public void setMoreThanOneOrgUnitMessage( String moreThanOneOrgUnitMessage )
    {
        this.moreThanOneOrgUnitMessage = moreThanOneOrgUnitMessage;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "userGroup", namespace = DxfNamespaces.DXF_2_0 )
    public UserGroup getUserGroup()
    {
        return userGroup;
    }

    public void setUserGroup( UserGroup userGroup )
    {
        this.userGroup = userGroup;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "program", namespace = DxfNamespaces.DXF_2_0 )
    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlProperty( localName = "programStage", namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }

    @JsonProperty
    @JacksonXmlProperty
    public CompletenessMethod getCompletenessMethod()
    {
        return completenessMethod;
    }

    public void setCompletenessMethod( CompletenessMethod completenessMethod )
    {
        this.completenessMethod = completenessMethod;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "uid", uid )
            .add( "name", name )
            .add( "smscodes", codes )
            .add( "program", program )
            .add( "parsertype", parserType )
            .add( "separator", separator )
            .add( "dataset", dataset )
            .add( "usergroup", userGroup )
            .add( "programstage", programStage )
            .toString();
    }
}
