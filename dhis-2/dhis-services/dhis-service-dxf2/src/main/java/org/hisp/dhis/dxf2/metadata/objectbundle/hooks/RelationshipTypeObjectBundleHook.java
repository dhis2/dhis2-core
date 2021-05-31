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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;

import java.util.function.Consumer;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.stereotype.Component;

/**
 * @author Stian Sandvold
 */
@Component( "org.hisp.dhis.dxf2.metadata.objectbundle.hooks.RelationshipTypeObjectBundleHook" )
public class RelationshipTypeObjectBundleHook
    extends AbstractObjectBundleHook
{
    private final TrackedEntityTypeService trackedEntityTypeService;

    private final ProgramService programService;

    private final ProgramStageService programStageService;

    public RelationshipTypeObjectBundleHook(
        TrackedEntityTypeService trackedEntityTypeService, ProgramService programService,
        ProgramStageService programStageService )
    {
        this.trackedEntityTypeService = trackedEntityTypeService;
        this.programService = programService;
        this.programStageService = programStageService;
    }

    @Override
    public <T extends IdentifiableObject> void validate( T object, ObjectBundle bundle,
        Consumer<ErrorReport> addReports )
    {
        if ( !(object instanceof RelationshipType) )
        {
            return;
        }

        validateRelationshipType( (RelationshipType) object, addReports );
    }

    @Override
    public void preCreate( IdentifiableObject object, ObjectBundle bundle )
    {
        if ( !(object instanceof RelationshipType) )
        {
            return;
        }

        handleRelationshipTypeReferences( (RelationshipType) object );
    }

    @Override
    public void preUpdate( IdentifiableObject object, IdentifiableObject persistedObject, ObjectBundle bundle )
    {
        if ( !(object instanceof RelationshipType) )
        {
            return;
        }

        handleRelationshipTypeReferences( (RelationshipType) object );

    }

    /**
     * Handles the references for RelationshipType, persisting any objects that
     * might end up in a transient state.
     */
    private void handleRelationshipTypeReferences( RelationshipType relationshipType )
    {
        handleRelationshipConstraintReferences( relationshipType.getFromConstraint() );
        handleRelationshipConstraintReferences( relationshipType.getToConstraint() );
    }

    /**
     * Handles the references for RelationshipConstraint, persisting any bject
     * that might end up in a transient state.
     */
    private void handleRelationshipConstraintReferences( RelationshipConstraint relationshipConstraint )
    {
        TrackedEntityType trackedEntityType = relationshipConstraint.getTrackedEntityType();
        Program program = relationshipConstraint.getProgram();
        ProgramStage programStage = relationshipConstraint.getProgramStage();

        if ( trackedEntityType != null )
        {
            trackedEntityType = trackedEntityTypeService.getTrackedEntityType( trackedEntityType.getUid() );
            relationshipConstraint.setTrackedEntityType( trackedEntityType );
        }

        if ( program != null )
        {
            program = programService.getProgram( program.getUid() );
            relationshipConstraint.setProgram( program );
        }

        if ( programStage != null )
        {
            programStage = programStageService.getProgramStage( programStage.getUid() );
            relationshipConstraint.setProgramStage( programStage );
        }

        sessionFactory.getCurrentSession().save( relationshipConstraint );
    }

    /**
     * Validates the RelationshipType. A type should have constraints for both
     * left and right side.
     */
    private void validateRelationshipType( RelationshipType relationshipType,
        Consumer<ErrorReport> addReports )
    {
        if ( relationshipType.getFromConstraint() == null )
        {
            addReports.accept( new ErrorReport( RelationshipType.class, ErrorCode.E4000, "leftConstraint" ) );
        }
        else
        {
            validateRelationshipConstraint( relationshipType.getFromConstraint(), addReports );
        }

        if ( relationshipType.getToConstraint() == null )
        {
            addReports.accept( new ErrorReport( RelationshipType.class, ErrorCode.E4000, "rightConstraint" ) );
        }
        else
        {
            validateRelationshipConstraint( relationshipType.getToConstraint(), addReports );
        }
    }

    /**
     * Validates RelationshipConstraint. Each constraint requires different
     * properties set or not set depending on the RelationshipEntity set for
     * this constraint.
     */
    private void validateRelationshipConstraint( RelationshipConstraint constraint,
        Consumer<ErrorReport> addReports )
    {
        switch ( constraint.getRelationshipEntity() )
        {
        case TRACKED_ENTITY_INSTANCE:
            validateTrackedEntityInstance( constraint, addReports );
            break;
        case PROGRAM_INSTANCE:
            validateProgramInstance( constraint, addReports );
            break;
        case PROGRAM_STAGE_INSTANCE:
            validateProgramStrageInstance( constraint, addReports );
            break;
        }
    }

    private void validateTrackedEntityInstance( RelationshipConstraint constraint, Consumer<ErrorReport> addReports )
    {
        // Should be not be null
        if ( constraint.getTrackedEntityType() == null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4024, "trackedEntityType",
                "relationshipEntity", TRACKED_ENTITY_INSTANCE ) );
        }

        // Should be null
        if ( constraint.getProgramStage() != null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4023, "programStage",
                "relationshipEntity", TRACKED_ENTITY_INSTANCE ) );
        }
    }

    private void validateProgramInstance( RelationshipConstraint constraint, Consumer<ErrorReport> addReports )
    {
        // Should be null
        if ( constraint.getTrackedEntityType() != null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4023, "trackedEntityType",
                "relationshipEntity", PROGRAM_INSTANCE ) );
        }

        // Should be not be null
        if ( constraint.getProgram() == null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4024, "program",
                "relationshipEntity", PROGRAM_INSTANCE ) );
        }

        // Should be null
        if ( constraint.getProgramStage() != null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4023, "programStage",
                "relationshipEntity", PROGRAM_INSTANCE ) );
        }
    }

    private void validateProgramStrageInstance( RelationshipConstraint constraint, Consumer<ErrorReport> addReports )
    {
        // Should be null
        if ( constraint.getTrackedEntityType() != null )
        {
            addReports.accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4023, "trackedEntityType",
                "relationshipEntity", PROGRAM_STAGE_INSTANCE ) );
        }

        // Should be null
        if ( constraint.getProgram() != null && constraint.getProgramStage() != null )
        {
            addReports.accept(
                new ErrorReport( RelationshipConstraint.class, ErrorCode.E4025, "program", "programStage" ) );
        }

        // Should be null
        if ( constraint.getProgram() == null && constraint.getProgramStage() == null )
        {
            addReports
                .accept( new ErrorReport( RelationshipConstraint.class, ErrorCode.E4026, "program", "programStage",
                    "relationshipEntity", PROGRAM_STAGE_INSTANCE ) );
        }
    }
}
