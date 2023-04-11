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
package org.hisp.dhis.organisationunit;

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.IdObjectDeletionHandler;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Component
public class OrganisationUnitDeletionHandler extends IdObjectDeletionHandler<OrganisationUnit>
{
    @Override
    protected void registerHandler()
    {
        whenDeleting( DataSet.class, this::deleteDataSet );
        whenDeleting( User.class, this::deleteUser );
        whenDeleting( Program.class, this::deleteProgram );
        whenDeleting( OrganisationUnitGroup.class, this::deleteOrganisationUnitGroup );
        whenDeleting( OrganisationUnit.class, this::deleteOrganisationUnit );
        whenVetoing( OrganisationUnit.class, this::allowDeleteOrganisationUnit );
    }

    private void deleteDataSet( DataSet dataSet )
    {
        dataSet.getSources().iterator().forEachRemaining( unit -> {
            unit.getDataSets().remove( dataSet );
            idObjectManager.updateNoAcl( unit );
        } );
    }

    private void deleteUser( User user )
    {
        user.getOrganisationUnits().iterator().forEachRemaining( unit -> {
            unit.getUsers().remove( user );
            idObjectManager.updateNoAcl( unit );
        } );
    }

    private void deleteProgram( Program program )
    {
        program.getOrganisationUnits().iterator().forEachRemaining( unit -> {
            unit.getPrograms().remove( program );
            idObjectManager.updateNoAcl( unit );
        } );
    }

    private void deleteOrganisationUnitGroup( OrganisationUnitGroup group )
    {
        group.getMembers().iterator().forEachRemaining( unit -> {
            unit.getGroups().remove( group );
            idObjectManager.updateNoAcl( unit );
        } );
    }

    private void deleteOrganisationUnit( OrganisationUnit unit )
    {
        if ( unit.getParent() != null )
        {
            unit.getParent().getChildren().remove( unit );
            idObjectManager.updateNoAcl( unit.getParent() );
        }
    }

    private DeletionVeto allowDeleteOrganisationUnit( OrganisationUnit unit )
    {
        return unit.getChildren().isEmpty()
            ? ACCEPT
            : new DeletionVeto( OrganisationUnit.class, unit.getChildren().stream()
                .map( IdentifiableObject::getName )
                .collect( joining( "," ) ) );
    }
}
