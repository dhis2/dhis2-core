package org.hisp.dhis.trackedentity.action.dataentryform;

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
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.program.ProgramDataEntryService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;

import com.opensymphony.xwork2.Action;

/**
 * @author Bharath Kumar
 * @modify Viet Nguyen 3-11-2009
 * @modify Tran Thanh Tri 13 Oct 2010
 * @version $Id$
 */
public class ViewDataEntryFormAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataEntryFormService dataEntryFormService;

    public void setDataEntryFormService( DataEntryFormService dataEntryFormService )
    {
        this.dataEntryFormService = dataEntryFormService;
    }

    private ProgramDataEntryService programDataEntryService;

    public void setProgramDataEntryService( ProgramDataEntryService programDataEntryService )
    {
        this.programDataEntryService = programDataEntryService;
    }

    private ProgramStageService programStageService;

    public void setProgramStageService( ProgramStageService programStageService )
    {
        this.programStageService = programStageService;
    }

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    private UserSettingService userSettingService;

    public void setUserSettingService( UserSettingService userSettingService )
    {
        this.userSettingService = userSettingService;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    private Integer programStageId;

    public void setProgramStageId( Integer programStageId )
    {
        this.programStageId = programStageId;
    }

    private DataEntryForm dataEntryForm;

    public DataEntryForm getDataEntryForm()
    {
        return dataEntryForm;
    }

    private ProgramStage programStage;

    public ProgramStage getProgramStage()
    {
        return programStage;
    }

    private List<DataEntryForm> existingDataEntryForms;

    public List<DataEntryForm> getExistingDataEntryForms()
    {
        return existingDataEntryForms;
    }

    private List<ProgramStage> programStages;

    public List<ProgramStage> getProgramStages()
    {
        return programStages;
    }

    private String dataEntryValue;

    public String getDataEntryValue()
    {
        return dataEntryValue;
    }

    private List<DataElement> dataElements;

    public List<DataElement> getDataElements()
    {
        return dataElements;
    }

    private List<String> flags;

    public List<String> getFlags()
    {
        return flags;
    }

    private boolean autoSave;

    public boolean getAutoSave()
    {
        return autoSave;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        programStage = programStageService.getProgramStage( programStageId );

        // ---------------------------------------------------------------------
        // Get dataEntryForm of selected program-stage
        // ---------------------------------------------------------------------

        dataEntryForm = programStage.getDataEntryForm();

        if ( dataEntryForm != null )
        {
            dataEntryValue = programDataEntryService.prepareDataEntryFormForEdit( dataEntryForm.getHtmlCode() );
        }
        else
        {
            dataEntryValue = "";
        }

        // ---------------------------------------------------------------------
        // Get existing Data Entry Forms
        // ---------------------------------------------------------------------

        List<Integer> listAssociationIds = new ArrayList<>();

        for ( ProgramStage ps : programStage.getProgram().getProgramStages() )
        {
            listAssociationIds.add( ps.getId() );
        }

        existingDataEntryForms = dataEntryFormService.listDistinctDataEntryFormByProgramStageIds( listAssociationIds );

        existingDataEntryForms.remove( dataEntryForm );

        // ---------------------------------------------------------------------
        // Get other program-stages into the program
        // ---------------------------------------------------------------------

        programStages = new ArrayList<>( programStage.getProgram().getProgramStages() );

        programStages.remove( programStage );

        Collections.sort( programStages );

        // ---------------------------------------------------------------------
        // Get selected program-stage
        // ---------------------------------------------------------------------

        dataElements = new ArrayList<>( programStage.getAllDataElements() );

        Collections.sort( dataElements );

        flags = systemSettingManager.getFlags();

        autoSave = (Boolean) userSettingService.getUserSetting( UserSettingKey.AUTO_SAVE_CASE_ENTRY_FORM );

        return SUCCESS;
    }
}
