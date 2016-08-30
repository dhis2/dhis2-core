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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;

import com.opensymphony.xwork2.Action;

public class SMSCommandAction
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
    // Input & Output
    // -------------------------------------------------------------------------

    private SMSCommand smsCommand;

    private List<DataElement> dataElements;

    private List<UserGroup> userGroupList;

    public List<UserGroup> getUserGroupList()
    {
        return userGroupList;
    }

    private List<Program> programList;

    public List<Program> getProgramList()
    {
        return programList;
    }

    public void setProgramList( List<Program> programList )
    {
        this.programList = programList;
    }

    private List<Program> programWithoutRegistration;

    public List<Program> getProgramWithoutRegistration()
    {
        return programWithoutRegistration;
    }

    public void setProgramWithoutRegistration( List<Program> programWithoutRegistration )
    {
        this.programWithoutRegistration = programWithoutRegistration;
    }

    private List<TrackedEntityAttribute> trackedEntityAttributeList;

    private Collection<ProgramStageDataElement> programStageDataElementList = new ArrayList<>();

    private int selectedCommandID = -1;

    public int getSelectedCommandID()
    {
        return selectedCommandID;
    }

    public void setSelectedCommandID( int selectedCommandID )
    {
        this.selectedCommandID = selectedCommandID;
    }

    private Map<String, String> codes = new HashMap<>();

    public Map<String, String> getCodes()
    {
        return codes;
    }

    public void setCodes( Map<String, String> codes )
    {
        this.codes = codes;
    }

    private Map<String, String> formulas = new HashMap<>();

    public Map<String, String> getFormulas()
    {
        return formulas;
    }

    public void setFormulas( Map<String, String> formulas )
    {
        this.formulas = formulas;
    }

    public ParserType[] getParserType()
    {
        return ParserType.values();
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( selectedCommandID > -1 )
        {
            smsCommand = smsCommandService.getSMSCommand( selectedCommandID );
        }

        if ( smsCommand != null && smsCommand.getCodes() != null )
        {

            for ( SMSCode smsCode : smsCommand.getCodes() )
            {
                if ( smsCommand.getParserType() == ParserType.TRACKED_ENTITY_REGISTRATION_PARSER
                    && smsCode.getTrackedEntityAttribute() != null )
                {
                    codes.put( "" + smsCode.getTrackedEntityAttribute().getId(), smsCode.getCode() );
                }
                else if ( smsCode.getDataElement() != null )
                {
                    codes.put( "" + smsCode.getDataElement().getId() + smsCode.getOptionId(), smsCode.getCode() );
                }

                if ( smsCode.getFormula() != null )
                {
                    formulas.put( "" + smsCode.getDataElement().getId() + smsCode.getOptionId(), smsCode.getFormula() );
                }

                if ( smsCommand.getParserType().equals(ParserType.EVENT_REGISTRATION_PARSER ))
                {
                    codes.put( "" + smsCode.getDataElement().getId(), smsCode.getCode() );
                }

            }
        }

        userGroupList = new ArrayList<>( userGroupService.getAllUserGroups() );

        programList = new ArrayList<>( programService.getPrograms( ProgramType.WITH_REGISTRATION ) );

        programWithoutRegistration = new ArrayList<>( programService.getPrograms( ProgramType.WITHOUT_REGISTRATION ) );

        return SUCCESS;
    }

    // -------------------------------------------------------------------------
    // Supporting methods
    // -------------------------------------------------------------------------

    public List<DataElement> getDataElements()
    {
        if ( smsCommand != null )
        {
            DataSet d = smsCommand.getDataset();
            if ( d != null )
            {
                dataElements = new ArrayList<>( d.getDataElements() );
                return dataElements;
            }
        }
        return null;
    }

    public Collection<DataSet> getDataSets()
    {
        return dataSetService.getAllDataSets();
    }

    public Collection<SMSCommand> getSMSCommands()
    {
        return smsCommandService.getSMSCommands();
    }

    public SMSCommand getSmsCommand()
    {
        return smsCommand;
    }

    public List<TrackedEntityAttribute> getTrackedEntityAttributeList()
    {
        if ( smsCommand != null )
        {
            Program program = smsCommand.getProgram();
            if ( program != null )
            {
                trackedEntityAttributeList = new ArrayList<>();
                for ( ProgramTrackedEntityAttribute programAttribute : program.getProgramAttributes() )
                {
                    trackedEntityAttributeList.add( programAttribute.getAttribute() );
                }
                return trackedEntityAttributeList;
            }

        }
        return null;
    }

    public Collection<ProgramStageDataElement> getProgramStageDataElementList()
    {
        if ( smsCommand != null )
        {

            Program program = smsCommand.getProgram();

            ProgramStage programStage = program.getProgramStages().iterator().next();

            if ( programStage != null )
            {
                programStageDataElementList = programStage.getProgramStageDataElements();
            }

            return programStageDataElementList;
        }

        return new ArrayList<ProgramStageDataElement>();
    }

    public void setTrackedEntityAttributeList( List<TrackedEntityAttribute> trackedEntityAttributeList )
    {
        this.trackedEntityAttributeList = trackedEntityAttributeList;
    }
}
