package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.events.event.AbstractEventService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.*;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ProgramObjectBundleHook extends AbstractObjectBundleHook
{
    private ProgramInstanceService programInstanceService;

    @Autowired
    public ProgramObjectBundleHook( ProgramInstanceService programInstanceService )
    {
        this.programInstanceService = programInstanceService;
    }

    @Override
    public void postCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !isProgram( object ) )
        {
            return;
        }

        syncSharingForEventProgram( (Program) object );

        addProgramInstance( (Program) object );
    }

    @Override
    public void postUpdate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !isProgram( object ) )
        {
            return;
        }

        syncSharingForEventProgram( (Program) object );
    }

    @Override
    public <T extends IdentifiableObject> List<ErrorReport> validate( T object, ObjectBundle bundle )
    {
        List<ErrorReport> errors = new ArrayList<>();

        if ( !(object instanceof Program) )
        {
            return errors;
        }

        Program program = (Program) object;

        if ( program.getId() != 0 && getProgramInstancesCount( program ) > 1 )
        {
            errors.add( new ErrorReport( Program.class, ErrorCode.E6000, program.getName() ) );
        }
        
        return errors;
    }

    private void syncSharingForEventProgram( Program program )
    {
        if ( ProgramType.WITH_REGISTRATION == program.getProgramType() || program.getProgramStages().isEmpty() )
        {
            return;
        }

        ProgramStage programStage = program.getProgramStages().iterator().next();
        AccessStringHelper.copySharing( program, programStage );

        programStage.setUser( program.getUser() );
        sessionFactory.getCurrentSession().update( programStage );
    }
    
    private void addProgramInstance( Program program )
    {

        if ( getProgramInstancesCount( program ) == 0 )
        {
            ProgramInstance pi = new ProgramInstance();
            pi.setEnrollmentDate( new Date() );
            pi.setIncidentDate( new Date() );
            pi.setProgram( program );
            pi.setStatus( ProgramStatus.ACTIVE );
            pi.setStoredBy( "system-process" );

            this.programInstanceService.addProgramInstance( pi );
        }

    }
    
    private int getProgramInstancesCount( Program program )
    {
        return programInstanceService.getProgramInstances( program, ProgramStatus.ACTIVE ).size();
    }
    
    private boolean isProgram( Object object )
    {
        return object instanceof Program;
    }
}
