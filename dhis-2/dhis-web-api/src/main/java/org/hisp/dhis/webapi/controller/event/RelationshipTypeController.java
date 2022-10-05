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
package org.hisp.dhis.webapi.controller.event;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.schema.descriptors.RelationshipTypeSchemaDescriptor;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = RelationshipTypeSchemaDescriptor.API_ENDPOINT )
public class RelationshipTypeController
    extends AbstractCrudController<RelationshipType>
{
    private final ProgramService programService;

    public RelationshipTypeController( ProgramService programService )
    {
        this.programService = programService;
    }

    @Override
    protected void preCreateEntity( RelationshipType entity )
        throws Exception
    {
        super.preCreateEntity( entity );

        handleEventProgramRelationshipConstraint( entity.getFromConstraint() );
        handleEventProgramRelationshipConstraint( entity.getToConstraint() );
    }

    @Override
    protected void preUpdateEntity( RelationshipType entity, RelationshipType newEntity )
        throws Exception
    {
        super.preUpdateEntity( entity, newEntity );

        handleEventProgramRelationshipConstraint( entity.getFromConstraint() );
        handleEventProgramRelationshipConstraint( entity.getToConstraint() );
    }

    /**
     * Update the constraint with a programStage if it meets the following
     * criteria: Program is set and is without registration ProgramStage is not
     * set
     *
     * We do this because programs without registration only have a single
     * program stage
     *
     * @param constraint
     */
    private void handleEventProgramRelationshipConstraint( RelationshipConstraint constraint )
    {
        if ( constraint.getProgram() != null && constraint.getProgramStage() == null )
        {
            Program program = programService.getProgram( constraint.getProgram().getUid() );

            if ( program.isWithoutRegistration() )
            {
                constraint.setProgramStage( program.getProgramStageByStage( 1 ) );
            }
        }
    }
}
