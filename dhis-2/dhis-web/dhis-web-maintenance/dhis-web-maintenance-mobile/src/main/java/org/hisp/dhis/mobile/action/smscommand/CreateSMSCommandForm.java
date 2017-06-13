package org.hisp.dhis.mobile.action.smscommand;

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

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;

import com.opensymphony.xwork2.Action;

public class CreateSMSCommandForm
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

    private UserGroupService userGroupService;

    public void setUserGroupService( UserGroupService userGroupService )
    {
        this.userGroupService = userGroupService;
    }

    private ProgramService programService;

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    // -------------------------------------------------------------------------
    // Input && Output
    // -------------------------------------------------------------------------

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private ParserType parserType;

    public void setParserType( ParserType parserType )
    {
        this.parserType = parserType;
    }

    private Integer selectedDataSetID;

    public void setSelectedDataSetID( Integer selectedDataSetID )
    {
        this.selectedDataSetID = selectedDataSetID;
    }

    private Integer userGroupID;

    public void setUserGroupID( Integer userGroupID )
    {
        this.userGroupID = userGroupID;
    }

    private Integer selectedProgramId;

    public Integer getSelectedProgramId()
    {
        return selectedProgramId;
    }

    public void setSelectedProgramId( Integer selectedProgramId )
    {
        this.selectedProgramId = selectedProgramId;
    }

    private Integer selectedProgramIdWithoutRegistration;

    public Integer getSelectedProgramIdWithoutRegistration()
    {
        return selectedProgramIdWithoutRegistration;
    }

    public void setSelectedProgramIdWithoutRegistration( Integer selectedProgramIdWithoutRegistration )
    {
        this.selectedProgramIdWithoutRegistration = selectedProgramIdWithoutRegistration;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        SMSCommand command = new SMSCommand();
        command.setName( name );
        command.setParserType( parserType );
        if ( parserType.equals( ParserType.KEY_VALUE_PARSER ) || parserType.equals( ParserType.J2ME_PARSER ) )
        {
            DataSet dataset = dataSetService.getDataSet( selectedDataSetID );
            command.setDataset( dataset );
        }
        else if ( parserType.equals( ParserType.ALERT_PARSER ) || parserType.equals( ParserType.UNREGISTERED_PARSER ) )
        {
            UserGroup userGroup = new UserGroup();
            userGroup = userGroupService.getUserGroup( userGroupID );
            command.setUserGroup( userGroup );
        }
        else if ( parserType.equals( ParserType.TRACKED_ENTITY_REGISTRATION_PARSER ) )
        {
            Program program = programService.getProgram( selectedProgramId );
            command.setProgram( program );
        }
        else if ( parserType.equals( ParserType.EVENT_REGISTRATION_PARSER ) )
        {
            Program program = programService.getProgram( selectedProgramIdWithoutRegistration );

            command.setProgram( program );

            command.setProgramStage( program.getProgramStages().iterator().next() );
        }

        smsCommandService.save( command );

        return SUCCESS;
    }
}
