/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.imports.validation.validator.relationship;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4012;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.enrollmentExist;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.eventExist;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.trackedEntityInstanceExist;
import static org.hisp.dhis.tracker.imports.validation.validator.relationship.ValidationUtils.relationshipItemValueType;

import java.util.Optional;

import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.Relationship;
import org.hisp.dhis.tracker.imports.domain.RelationshipItem;
import org.hisp.dhis.tracker.imports.domain.TrackedEntity;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;

public class ConstraintValidator implements Validator<Relationship>
{
    @Override
    public void validate( Reporter reporter, TrackerBundle bundle, Relationship relationship )
    {
        RelationshipType relationshipType = bundle.getPreheat()
            .getRelationshipType( relationship.getRelationshipType() );

        validateRelationshipConstraint( reporter, bundle, relationship, "from", relationship.getFrom(),
            relationshipType.getFromConstraint() );
        validateRelationshipConstraint( reporter, bundle, relationship, "to", relationship.getTo(),
            relationshipType.getToConstraint() );
    }

    private void validateRelationshipConstraint( Reporter reporter, TrackerBundle bundle,
        Relationship relationship,
        String relSide,
        RelationshipItem item,
        RelationshipConstraint constraint )
    {
        switch ( constraint.getRelationshipEntity() )
        {
        case TRACKED_ENTITY_INSTANCE:
            validateTrackedEntityInstanceRelationship( reporter, bundle, relationship, item, relSide, constraint );
            break;
        case PROGRAM_INSTANCE:
            validateProgramInstanceRelationship( reporter, bundle, relationship, item, relSide );
            break;
        case PROGRAM_STAGE_INSTANCE:
            validateEventRelationship( reporter, bundle, relationship, item, relSide );
            break;
        default:
            break;
        }
    }

    private void validateTrackedEntityInstanceRelationship( Reporter reporter, TrackerBundle bundle,
        Relationship relationship, RelationshipItem item, String relSide, RelationshipConstraint constraint )
    {
        if ( item.getTrackedEntity() == null )
        {
            reporter.addError( relationship, ValidationCode.E4010, relSide,
                TrackerType.TRACKED_ENTITY.getName(), relationshipItemValueType( item ).getName() );
        }
        else if ( !trackedEntityInstanceExist( bundle, item.getTrackedEntity() ) )
        {
            reporter.addError( relationship, E4012, TrackerType.TRACKED_ENTITY.getName(), item.getTrackedEntity() );
        }
        else
        {
            validateTrackedEntityInstanceType( item, reporter, relationship, relSide, bundle, constraint );
        }
    }

    private void validateProgramInstanceRelationship( Reporter reporter, TrackerBundle bundle,
        Relationship relationship, RelationshipItem item, String relSide )
    {
        if ( item.getEnrollment() == null )
        {
            reporter.addError( relationship, ValidationCode.E4010, relSide, TrackerType.ENROLLMENT.getName(),
                relationshipItemValueType( item ).getName() );
        }
        else if ( !enrollmentExist( bundle, item.getEnrollment() ) )
        {
            reporter.addError( relationship, E4012, TrackerType.ENROLLMENT.getName(), item.getEnrollment() );
        }
    }

    private static void validateEventRelationship( Reporter reporter, TrackerBundle bundle,
        Relationship relationship, RelationshipItem item, String relSide )
    {
        if ( item.getEvent() == null )
        {
            reporter.addError( relationship, ValidationCode.E4010, relSide,
                TrackerType.EVENT.getName(), relationshipItemValueType( item ).getName() );
        }
        else if ( !eventExist( bundle, item.getEvent() ) )
        {
            reporter.addError( relationship, E4012, TrackerType.EVENT.getName(), item.getEvent() );
        }
    }

    private void validateTrackedEntityInstanceType( RelationshipItem item, Reporter reporter, Relationship relationship,
        String relSide, TrackerBundle bundle, RelationshipConstraint constraint )
    {
        getRelationshipTypeUidFromTrackedEntity( bundle, item.getTrackedEntity() )
            .ifPresent( type -> {
                if ( !type.isEqualTo( constraint.getTrackedEntityType() ) )
                {
                    reporter.addError( relationship, ValidationCode.E4014, relSide,
                        type.identifierOf( constraint.getTrackedEntityType() ), type );
                }
            } );
    }

    private Optional<MetadataIdentifier> getRelationshipTypeUidFromTrackedEntity( TrackerBundle bundle, String uid )
    {
        return getTrackedEntityTypeFromTrackedEntity( bundle, uid )
            .or( () -> getTrackedEntityTypeFromTrackedEntityRef( bundle, uid ) );
    }

    private Optional<MetadataIdentifier> getTrackedEntityTypeFromTrackedEntity( TrackerBundle bundle, String uid )
    {
        final TrackedEntityInstance trackedEntity = bundle.getPreheat().getTrackedEntity( uid );

        return trackedEntity != null
            ? Optional
                .of( bundle.getPreheat().getIdSchemes().toMetadataIdentifier( trackedEntity.getTrackedEntityType() ) )
            : Optional.empty();
    }

    private Optional<MetadataIdentifier> getTrackedEntityTypeFromTrackedEntityRef( TrackerBundle bundle, String uid )
    {
        final Optional<TrackedEntity> payloadTei = bundle.findTrackedEntityByUid( uid );
        return payloadTei.map( TrackedEntity::getTrackedEntityType );
    }

    @Override
    public boolean needsToRun( TrackerImportStrategy strategy )
    {
        return strategy.isCreate();
    }
}
