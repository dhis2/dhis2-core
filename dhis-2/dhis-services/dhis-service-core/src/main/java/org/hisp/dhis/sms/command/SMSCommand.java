package org.hisp.dhis.sms.command;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.user.UserGroup;

import java.util.Set;

public class SMSCommand
{
    // Default message

    public static final String WRONG_FORMAT_MESSAGE = "Wrong format for command";

    public static final String MORE_THAN_ONE_ORGUNIT_MESSAGE = "Found more than one org unit for this number. Please specify one organisation unit";

    public static final String NO_USER_MESSAGE = "No user associated with this phone number. Please contact your supervisor.";

    public static final String ALERT_FEEDBACK = "Your alert message sent";

    // Completeness method code

    public static final int RECEIVE_ALL_DATAVALUE = 1;

    public static final int RECEIVE_AT_LEAST_ONE_DATAVALUE = 2;

    public static final int DO_NOT_MARK_COMPLETE = 3;

    private int id;

    private String name;

    private String parser; // message type

    private ParserType parserType;

    private String separator;

    // Dataset

    private DataSet dataset;

    private Set<SMSCode> codes;

    private String codeSeparator;

    // Usergroup

    private UserGroup userGroup;

    // Program

    private Program program;

    // Program Stage

    private ProgramStage programStage;

    private Set<SMSSpecialCharacter> specialCharacters;

    private boolean currentPeriodUsedForReporting = false; // default is prev

    private Integer completenessMethod;

    // Messages

    private String defaultMessage;

    private String receivedMessage;

    private String wrongFormatMessage;

    private String noUserMessage;

    private String moreThanOneOrgUnitMessage;

    private String successMessage;

    public SMSCommand( String name, String parser, ParserType parserType, String separator, DataSet dataset,
        Set<SMSCode> codes, String codeSeparator, String defaultMessage, UserGroup userGroup, String receivedMessage,
        Set<SMSSpecialCharacter> specialCharacters )
    {
        super();
        this.name = name;
        this.parser = parser;
        this.parserType = parserType;
        this.separator = separator;
        this.dataset = dataset;
        this.codes = codes;
        this.codeSeparator = codeSeparator;
        this.defaultMessage = defaultMessage;
        this.userGroup = userGroup;
        this.receivedMessage = receivedMessage;
        this.specialCharacters = specialCharacters;
    }

    public SMSCommand( String name, String parser, ParserType parserType, String separator, DataSet dataset,
        Set<SMSCode> codes, String codeSeparator, String defaultMessage, UserGroup userGroup, String receivedMessage )
    {
        super();
        this.name = name;
        this.parser = parser;
        this.parserType = parserType;
        this.separator = separator;
        this.dataset = dataset;
        this.codes = codes;
        this.codeSeparator = codeSeparator;
        this.defaultMessage = defaultMessage;
        this.userGroup = userGroup;
        this.receivedMessage = receivedMessage;
    }

    public SMSCommand( String name, String parser, ParserType parserType, String separator, DataSet dataset,
        Set<SMSCode> codes, String codeSeparator, String defaultMessage )
    {
        super();
        this.name = name;
        this.parser = parser;
        this.parserType = parserType;
        this.separator = separator;
        this.dataset = dataset;
        this.codes = codes;
        this.codeSeparator = codeSeparator;
        this.defaultMessage = defaultMessage;
    }

    public SMSCommand( String name, String parser, String separator, DataSet dataset, Set<SMSCode> codes,
        String codeSeparator )
    {
        this.name = name;
        this.parser = parser;
        this.separator = separator;
        this.dataset = dataset;
        this.codes = codes;
        this.codeSeparator = codeSeparator;
    }

    public SMSCommand( String name, String parser, String separator, DataSet dataset, Set<SMSCode> codes )
    {
        this.name = name;
        this.parser = parser;
        this.separator = separator;
        this.dataset = dataset;
        this.codes = codes;
    }

    public SMSCommand( String parser, String name, DataSet dataset, Set<SMSCode> codes )
    {
        this.parser = parser;
        this.name = name;
        this.dataset = dataset;
        this.codes = codes;
    }

    public SMSCommand( String parser, String name, DataSet dataset )
    {
        this.parser = parser;
        this.name = name;
        this.dataset = dataset;
    }

    public SMSCommand( String name, String parser )
    {
        this.name = name;
        this.parser = parser;
    }

    public SMSCommand()
    {

    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getParser()
    {
        return parser;
    }

    public void setParser( String parser )
    {
        this.parser = parser;
    }

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    public DataSet getDataset()
    {
        return dataset;
    }

    public void setDataset( DataSet dataset )
    {
        this.dataset = dataset;
    }

    public Set<SMSCode> getCodes()
    {
        return codes;
    }

    public void setCodes( Set<SMSCode> codes )
    {
        this.codes = codes;
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

    public ParserType getParserType()
    {
        if ( parserType == null )
        {
            return ParserType.KEY_VALUE_PARSER;
        }
        return parserType;
    }

    public void setParserType( ParserType parserType )
    {
        this.parserType = parserType;
    }

    public boolean isCurrentPeriodUsedForReporting()
    {
        return currentPeriodUsedForReporting;
    }

    public void setCurrentPeriodUsedForReporting( Boolean currentPeriodUsedForReporting )
    {
        if ( currentPeriodUsedForReporting == null )
        {
            this.currentPeriodUsedForReporting = false;
        }
        else
        {
            this.currentPeriodUsedForReporting = currentPeriodUsedForReporting;
        }
    }

    public UserGroup getUserGroup()
    {
        return userGroup;
    }

    public void setUserGroup( UserGroup userGroup )
    {
        this.userGroup = userGroup;
    }

    public String getReceivedMessage()
    {
        return receivedMessage;
    }

    public void setReceivedMessage( String receivedMessage )
    {
        this.receivedMessage = receivedMessage;
    }

    public Set<SMSSpecialCharacter> getSpecialCharacters()
    {
        return specialCharacters;
    }

    public void setSpecialCharacters( Set<SMSSpecialCharacter> specialCharacters )
    {
        this.specialCharacters = specialCharacters;
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

    public Integer getCompletenessMethod()
    {
        return completenessMethod;
    }

    public void setCompletenessMethod( Integer completenessMethod )
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

    public Program getProgram()
    {
        return program;
    }

    public void setProgram( Program program )
    {
        this.program = program;
    }

    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    public void setProgramStage( ProgramStage programStage )
    {
        this.programStage = programStage;
    }
}
