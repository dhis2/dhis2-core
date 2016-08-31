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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;

import com.opensymphony.xwork2.Action;

/**
 * @author Bharath Kumar
 * @modify Tran Thanh Tri 13 Oct 2010
 * @version $Id$
 */
public class SaveDataEntryFormAction
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

    private ProgramStageService programStageService;

    public void setProgramStageService( ProgramStageService programStageService )
    {
        this.programStageService = programStageService;
    }
    
    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    private String name;

    public void setName( String name )
    {
        this.name = name;
    }

    private String designTextarea;

    public void setDesignTextarea( String designTextarea )
    {
        this.designTextarea = designTextarea;
    }

    private Integer programId;
    
    public Integer getProgramId()
    {
        return programId;
    }

    private Integer programStageId;

    public void setProgramStageId( Integer programStageId )
    {
        this.programStageId = programStageId;
    }

    private Integer dataEntryFormId;

    public void setDataEntryFormId( Integer dataEntryFormId )
    {
        this.dataEntryFormId = dataEntryFormId;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        ProgramStage programStage = programStageService.getProgramStage( programStageId );
        
        Program program = programStage.getProgram();
        
        programId = program.getId();
        
        DataEntryForm dataEntryForm = null;

        // ---------------------------------------------------------------------
        // Get data-entry-form
        // ---------------------------------------------------------------------

        if ( dataEntryFormId == null )
        {
            dataEntryForm = programStage.getDataEntryForm();
        }
        else
        {
            dataEntryForm = dataEntryFormService.getDataEntryForm( dataEntryFormId );
        }

        // ---------------------------------------------------------------------
        // Save data-entry-form
        // ---------------------------------------------------------------------

        if ( dataEntryForm == null || !dataEntryForm.getHtmlCode().equals( designTextarea ) )
        {
            program.increaseVersion();
        }
        
        designTextarea = dataEntryFormService.prepareDataEntryFormForSave( designTextarea );

        if ( dataEntryForm == null )
        {
            program.increaseVersion();
            
            dataEntryForm = new DataEntryForm( StringUtils.trimToNull( name ), designTextarea );
            dataEntryFormService.addDataEntryForm( dataEntryForm );
        }
        else
        {  
            dataEntryForm.setName( StringUtils.trimToNull( name ) );
            dataEntryForm.setHtmlCode( designTextarea );
            dataEntryFormService.updateDataEntryForm( dataEntryForm );
        }          

        programStage.setDataEntryForm( dataEntryForm );
        programStageService.updateProgramStage( programStage );

        return SUCCESS;
    }
}
