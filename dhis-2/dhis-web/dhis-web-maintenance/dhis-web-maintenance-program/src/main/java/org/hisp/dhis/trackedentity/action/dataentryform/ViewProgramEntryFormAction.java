package org.hisp.dhis.trackedentity.action.dataentryform;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 * 
 * @version ViewProgramEntryFormAction.java 10:35:08 AM Jan 31, 2013 $
 */
public class ViewProgramEntryFormAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramService programService;

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
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

    private Integer programId;

    public void setProgramId( Integer programId )
    {
        this.programId = programId;
    }

    private Collection<TrackedEntityAttribute> attributes = new HashSet<>();

    public Collection<TrackedEntityAttribute> getAttributes()
    {
        return attributes;
    }

    private Collection<ProgramTrackedEntityAttribute> programAttributes = new ArrayList<>();

    public Collection<ProgramTrackedEntityAttribute> getProgramAttributes()
    {
        return programAttributes;
    }

    private DataEntryForm registrationForm;

    public DataEntryForm getRegistrationForm()
    {
        return registrationForm;
    }

    private Program program;

    public Program getProgram()
    {
        return program;
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
        List<Program> programs = programService.getAllPrograms();

        programs.removeAll( programService.getPrograms( ProgramType.WITHOUT_REGISTRATION ) );

        program = programService.getProgram( programId );
        programAttributes = program.getProgramAttributes();
        registrationForm = program.getDataEntryForm();

        // ---------------------------------------------------------------------
        // Get images
        // ---------------------------------------------------------------------

        flags = systemSettingManager.getFlags();

        autoSave = (Boolean) userSettingService.getUserSetting(
            UserSettingKey.AUTO_SAVE_TRACKED_ENTITY_REGISTRATION_ENTRY_FORM );

        return SUCCESS;
    }
}
