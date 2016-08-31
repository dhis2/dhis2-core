package org.hisp.dhis.program;

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
import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.validation.ValidationCriteria;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class ProgramDeletionHandler
    extends DeletionHandler
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramService programService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;
    
    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return Program.class.getSimpleName();
    }

    @Override
    public void deleteValidationCriteria( ValidationCriteria validationCriteria )
    {
        Collection<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            if ( program.getValidationCriteria().remove( validationCriteria ) )
            {
                idObjectManager.updateNoAcl( program );
            }
        }
    }

    @Override
    public void deleteOrganisationUnit( OrganisationUnit unit )
    {
        Collection<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            if ( program.getOrganisationUnits().remove( unit ) )
            {
                idObjectManager.updateNoAcl( program );
            }
        }
    }

    @Override
    public void deleteUserAuthorityGroup( UserAuthorityGroup group )
    {
        Collection<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            if ( program.getUserRoles().remove( group ) )
            {
                idObjectManager.updateNoAcl( program );
            }
        }
    }

    @Override
    public String allowDeleteTrackedEntity( TrackedEntity trackedEntity )
    {
        Collection<Program> programs = programService.getProgramsByTrackedEntity( trackedEntity );

        return (programs != null && programs.size() > 0) ? ERROR : null;
    }

    @Override
    public void deleteTrackedEntityAttribute( TrackedEntityAttribute trackedEntityAttribute )
    {
        Collection<Program> programs = idObjectManager.getAllNoAcl( Program.class );

        for ( Program program : programs )
        {
            for ( ProgramTrackedEntityAttribute programAttribute : program.getProgramAttributes() )
            {
                if ( programAttribute.getAttribute().equals( trackedEntityAttribute ) )
                {
                    program.getProgramAttributes().remove( programAttribute );
                    idObjectManager.updateNoAcl( program );
                }
            }
        }
    }

    @Override
    public void deleteDataEntryForm( DataEntryForm dataEntryForm )
    {
        List<Program> associatedPrograms = programService.getProgramsByDataEntryForm( dataEntryForm );

        for ( Program program : associatedPrograms )
        {
            program.setDataEntryForm( null );
            idObjectManager.updateNoAcl( program );
        }
    }
}
