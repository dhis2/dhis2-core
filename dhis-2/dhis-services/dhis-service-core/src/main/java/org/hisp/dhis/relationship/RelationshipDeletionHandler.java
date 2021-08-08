/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.relationship;

import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import java.util.Collection;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.stereotype.Component;

/**
 * @author Chau Thu Tran
 */
@Component( "org.hisp.dhis.relationship.RelationshipDeletionHandler" )
public class RelationshipDeletionHandler
    extends DeletionHandler
{
    private static final DeletionVeto VETO = new DeletionVeto( Relationship.class );

    private final RelationshipService relationshipService;

    public RelationshipDeletionHandler( RelationshipService relationshipService )
    {
        this.relationshipService = relationshipService;
    }

    @Override
    protected void register()
    {
        whenDeleting( TrackedEntityInstance.class, this::deleteTrackedEntityInstance );
        whenDeleting( ProgramStageInstance.class, this::deleteProgramStageInstance );
        whenDeleting( ProgramInstance.class, this::deleteProgramInstance );
        whenVetoing( RelationshipType.class, this::allowDeleteRelationshipType );
    }

    private void deleteTrackedEntityInstance( TrackedEntityInstance entityInstance )
    {
        Collection<Relationship> relationships = relationshipService
            .getRelationshipsByTrackedEntityInstance( entityInstance, false );

        if ( relationships != null )
        {
            for ( Relationship relationship : relationships )
            {
                relationshipService.deleteRelationship( relationship );
            }
        }
    }

    private void deleteProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        Collection<Relationship> relationships = relationshipService
            .getRelationshipsByProgramStageInstance( programStageInstance, false );

        if ( relationships != null )
        {
            for ( Relationship relationship : relationships )
            {
                relationshipService.deleteRelationship( relationship );
            }
        }
    }

    private void deleteProgramInstance( ProgramInstance programInstance )
    {
        Collection<Relationship> relationships = relationshipService
            .getRelationshipsByProgramInstance( programInstance, false );

        if ( relationships != null )
        {
            for ( Relationship relationship : relationships )
            {
                relationshipService.deleteRelationship( relationship );
            }
        }
    }

    private DeletionVeto allowDeleteRelationshipType( RelationshipType relationshipType )
    {
        Collection<Relationship> relationships = relationshipService
            .getRelationshipsByRelationshipType( relationshipType );

        return relationships.isEmpty() ? ACCEPT : VETO;
    }
}
