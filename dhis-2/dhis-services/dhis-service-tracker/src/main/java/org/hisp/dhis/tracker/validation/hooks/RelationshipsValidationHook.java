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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4000;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4001;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4009;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4011;
import static org.hisp.dhis.tracker.validation.hooks.RelationshipValidationUtils.getUidFromRelationshipItem;
import static org.hisp.dhis.tracker.validation.hooks.RelationshipValidationUtils.relationshipItemValueType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.springframework.stereotype.Component;

/**
 * @author Enrico Colasante
 */
@Component
public class RelationshipsValidationHook
    extends AbstractTrackerDtoValidationHook
{

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        TrackerBundle bundle = reporter.getBundle();

        boolean isValid = validateMandatoryData( reporter, relationship,
            bundle.getPreheat().getAll( RelationshipType.class ) );

        // No need to check additional data if there are missing information on
        // the
        // Relationship
        if ( isValid )
        {
            validateRelationshipLinkToOneEntity( reporter, relationship );
            validateRelationshipConstraint( reporter, relationship, bundle );

            validateAutoRelationship( reporter, relationship );

            validateReferences( reporter, relationship, relationship.getFrom() );
            validateReferences( reporter, relationship, relationship.getTo() );
        }

    }

    private void validateRelationshipLinkToOneEntity( ValidationErrorReporter reporter,
        Relationship relationship )
    {
        // make sure that both Relationship Item only contain *one* reference
        // (tei, enrollment or event)
        reporter.addErrorIf( () -> hasMoreThanOneReference( relationship.getFrom() ),
            relationship, E4001, "from", relationship.getRelationship() );
        reporter.addErrorIf( () -> hasMoreThanOneReference( relationship.getTo() ),
            relationship, E4001, "to", relationship.getRelationship() );
    }

    private void validateRelationshipConstraint( ValidationErrorReporter reporter, Relationship relationship,
        TrackerBundle bundle )
    {
        getRelationshipType( bundle.getPreheat().getAll( RelationshipType.class ),
            relationship.getRelationshipType() ).ifPresent( relationshipType -> {
                validateRelationshipConstraint( reporter, relationship, "from", relationship.getFrom(),
                    relationshipType.getFromConstraint() );
                validateRelationshipConstraint( reporter, relationship, "to", relationship.getTo(),
                    relationshipType.getToConstraint() );
            } );
    }

    private boolean validateMandatoryData( ValidationErrorReporter reporter, Relationship relationship,
        List<RelationshipType> relationshipsTypes )
    {
        reporter.addErrorIf(
            () -> getRelationshipType( relationshipsTypes, relationship.getRelationshipType() ).isEmpty(),
            relationship, E4009, relationship.getRelationshipType() );

        final Optional<TrackerErrorReport> any = reporter.getReportList().stream()
            .filter( r -> relationship.getRelationship().equals( r.getUid() ) ).findAny();

        return any.isEmpty();
    }

    private Optional<RelationshipType> getRelationshipType( List<RelationshipType> relationshipsTypes,
        String relationshipTypeUid )
    {
        return relationshipsTypes.stream().filter( r -> r.getUid().equals( relationshipTypeUid ) ).findFirst();
    }

    private void validateAutoRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        if ( Objects.equals( relationship.getFrom(), relationship.getTo() ) )
        {
            reporter.addError( relationship, E4000, relationship.getRelationship() );
        }
    }

    private void validateRelationshipConstraint( ValidationErrorReporter reporter, Relationship relationship,
        String relSide,
        RelationshipItem item,
        RelationshipConstraint constraint )
    {
        if ( relationshipItemValueType( item ) == null )
        {
            reporter.addError( relationship, TrackerErrorCode.E4013, relSide, TrackerType.TRACKED_ENTITY.getName() );
            return;
        }

        if ( constraint.getRelationshipEntity().equals( TRACKED_ENTITY_INSTANCE ) )
        {
            if ( item.getTrackedEntity() == null )
            {
                reporter.addError( relationship, TrackerErrorCode.E4010, relSide,
                    TrackerType.TRACKED_ENTITY.getName(), relationshipItemValueType( item ).getName() );
            }
            else
            {

                //
                // Check tracked entity type matches the type specified in the
                // constraint
                //
                getRelationshipTypeUidFromTrackedEntity( reporter.getBundle(),
                    item.getTrackedEntity() )
                        .ifPresent( type -> {

                            if ( !type.isEqualTo( constraint.getTrackedEntityType() ) )
                            {
                                reporter.addError( relationship,
                                    TrackerErrorCode.E4014, relSide,
                                    type.identifierOf( constraint.getTrackedEntityType() ),
                                    type.getIdentifierOrAttributeValue() );
                            }

                        } );
            }
        }
        else if ( constraint.getRelationshipEntity().equals( PROGRAM_INSTANCE ) )
        {
            if ( item.getEnrollment() == null )
            {
                reporter.addError( relationship,
                    TrackerErrorCode.E4010, relSide, TrackerType.ENROLLMENT.getName(),
                    relationshipItemValueType( item ).getName() );
            }

        }
        else if ( constraint.getRelationshipEntity().equals( PROGRAM_STAGE_INSTANCE ) && item.getEvent() == null )
        {
            reporter.addError( relationship, TrackerErrorCode.E4010, relSide,
                TrackerType.EVENT.getName(), relationshipItemValueType( item ).getName() );
        }
    }

    private boolean hasMoreThanOneReference( RelationshipItem item )
    {
        if ( item == null )
        {
            return false;
        }
        return Stream.of( item.getTrackedEntity(), item.getEnrollment(), item.getEvent() )
            .filter( StringUtils::isNotBlank )
            .count() > 1;
    }

    private void validateReferences( ValidationErrorReporter reporter, Relationship relationship,
        RelationshipItem item )

    {
        TrackerType trackerType = relationshipItemValueType( item );
        Optional<String> itemUid = getUidFromRelationshipItem( item );

        itemUid.ifPresent( s -> reporter.addErrorIf( () -> reporter.isInvalid( trackerType, s ),
            relationship, E4011, relationship.getRelationship(),
            trackerType.getName(), s ) );
    }

    private Optional<MetadataIdentifier> getRelationshipTypeUidFromTrackedEntity( TrackerBundle bundle, String uid )
    {
        return getTrackedEntityTypeFromTrackedEntity( bundle, uid ).map( Optional::of )
            .orElseGet( () -> getTrackedEntityTypeFromTrackedEntityRef( bundle, uid ) );
    }

    private Optional<MetadataIdentifier> getTrackedEntityTypeFromTrackedEntity( TrackerBundle bundle, String uid )
    {
        final TrackedEntityInstance trackedEntity = bundle.getTrackedEntityInstance( uid );

        return trackedEntity != null
            ? Optional
                .of( bundle.getPreheat().getIdSchemes().toMetadataIdentifier( trackedEntity.getTrackedEntityType() ) )
            : Optional.empty();
    }

    private Optional<MetadataIdentifier> getTrackedEntityTypeFromTrackedEntityRef( TrackerBundle bundle, String uid )
    {
        final Optional<TrackedEntity> payloadTei = bundle.getTrackedEntities().stream()
            .filter( t -> t.getTrackedEntity().equals( uid ) ).findFirst();
        return payloadTei.map( TrackedEntity::getTrackedEntityType );
    }

    @Override
    public boolean removeOnError()
    {
        return true;
    }
}