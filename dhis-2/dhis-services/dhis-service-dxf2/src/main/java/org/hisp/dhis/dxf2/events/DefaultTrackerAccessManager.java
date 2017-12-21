package org.hisp.dhis.dxf2.events;

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

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultTrackerAccessManager implements TrackerAccessManager
{
    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private AclService aclService;

    @Override
    public List<String> canRead( User user, ProgramInstance programInstance )
    {
        List<String> errors = new ArrayList<>();

        if ( user.isSuper() )
        {
            return new ArrayList<>();
        }

        return errors;
    }

    @Override
    public List<String> canWrite( User user, ProgramInstance programInstance )
    {
        List<String> errors = new ArrayList<>();

        if ( user.isSuper() )
        {
            return new ArrayList<>();
        }

        return errors;
    }

    @Override
    public List<String> canRead( User user, ProgramStageInstance programStageInstance )
    {
        List<String> errors = new ArrayList<>();

        if ( user.isSuper() )
        {
            return new ArrayList<>();
        }

        OrganisationUnit ou = programStageInstance.getOrganisationUnit();

        if ( ou != null )
        { // ou should never be null, but needs to be checked for legacy reasons
            if ( !organisationUnitService.isInUserHierarchy( ou ) )
            {
                if ( !user.isAuthorized( "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS" ) )
                {
                    errors.add( "User has no access to organisation unit: " + ou.getUid() );
                }
            }
        }

        ProgramStage programStage = programStageInstance.getProgramStage();
        Program program = programStage.getProgram();

        if ( !aclService.canDataRead( user, program ) )
        {
            errors.add( "User has no access to program: " + program.getUid() );
        }

        if ( !aclService.canDataRead( user, programStage ) )
        {
            errors.add( "User has no access to program stage: " + programStageInstance.getUid() );
        }

        return errors;
    }

    @Override
    public List<String> canWrite( User user, ProgramStageInstance programStageInstance )
    {
        List<String> errors = new ArrayList<>();

        if ( user.isSuper() )
        {
            return new ArrayList<>();
        }

        return errors;
    }
}
