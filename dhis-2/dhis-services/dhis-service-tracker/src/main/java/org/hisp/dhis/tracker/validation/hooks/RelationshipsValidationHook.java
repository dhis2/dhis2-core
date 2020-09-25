package org.hisp.dhis.tracker.validation.hooks;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.hisp.dhis.relationship.RelationshipEntity.*;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Enrico Colasante
 */
@Component
public class RelationshipsValidationHook
    extends AbstractTrackerDtoValidationHook
{

    public RelationshipsValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( Relationship.class, TrackerImportStrategy.CREATE_AND_UPDATE, teAttrService );
    }

    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        TrackerBundle bundle = context.getBundle();

        if ( bundle.getImportStrategy().isDelete() )
        {
            return;
        }

        validateRelationshipType( reporter, relationship, bundle.getPreheat() );
        validateAutoRelationship( reporter, relationship );
        validateBidirectionalDuplicatedRelationships( reporter, bundle, relationship );

        return;
    }

    private void validateBidirectionalDuplicatedRelationships( ValidationErrorReporter reporter, TrackerBundle bundle,
        Relationship relationship )
    {
        List<Pair<RelationshipItem, RelationshipItem>> relationships =
            bundle.getRelationships()
                .stream()
                .map( r -> new ImmutablePair<>( r.getFrom(), r.getTo() ) )
                .collect( Collectors.toList() );

        List<ImmutablePair<RelationshipItem, RelationshipItem>> bidirectionalRelationship = Lists
            .newArrayList( relationship )
            .stream()
            .filter( r -> r.isBidirectional() )
            .map( r -> new ImmutablePair<>( r.getTo(), r.getFrom() ) )
            .collect( Collectors.toList() );

        relationships.addAll( bidirectionalRelationship );

        relationships
            .stream()
            .filter( r -> r.equals( new ImmutablePair<>( relationship.getFrom(), relationship.getTo() ) )
                || bidirectionalRelationship.contains( r ) )
            .filter( i -> Collections.frequency( relationships, i ) > 1 )
            .collect( Collectors.toSet() )
            .forEach( p -> reporter
                .addError( newReport( TrackerErrorCode.E4003 ).addArg( p.getLeft() ).addArg( p.getRight() ) ) );
    }

    private void validateAutoRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        if ( Objects.equals( relationship.getFrom(), relationship.getTo() ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E4000 ).addArg( relationship.getRelationship() ) );
        }
    }

    private void validateRelationshipType( ValidationErrorReporter reporter,
        Relationship relationship, TrackerPreheat preheat )
    {
        List<RelationshipType> relationshipsTypes = preheat.getAll( TrackerIdScheme.UID, RelationshipType.class );
        Optional<RelationshipType> optionalRelationshipType = relationshipsTypes.stream()
            .filter( type -> type.getUid().equals( relationship.getRelationshipType() ) ).findFirst();

        if ( !optionalRelationshipType.isPresent() )
        {
            reporter.addError( newReport( TrackerErrorCode.E4004 ).addArg( "relationshipType" ) );
            return;
        }

        if ( relationship.getFrom() == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E4004 ).addArg( "relationship.getFrom" ) );
        }
        else
        {
            if ( optionalRelationshipType.isPresent() )
            {
                validateRelationshipConstraint( relationship.getFrom(),
                    optionalRelationshipType.get().getFromConstraint() )
                    .stream()
                        .forEach( e -> reporter.addError( e ) );
            }
        }

        if ( relationship.getTo() == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E4004 ).addArg( "relationship.getTo" ) );
        }
        else
        {
            if ( optionalRelationshipType.isPresent() )
            {
                validateRelationshipConstraint( relationship.getTo(),
                    optionalRelationshipType.get().getToConstraint() )
                    .stream()
                    .forEach( e -> reporter.addError( e ) );
                ;
            }
        }

    }

    private List<TrackerErrorReport.TrackerErrorReportBuilder> validateRelationshipConstraint( RelationshipItem item,
        RelationshipConstraint relationshipType )
    {
        ArrayList<TrackerErrorReport.TrackerErrorReportBuilder> result = new ArrayList<>();

        if ( relationshipType.getRelationshipEntity().equals( TRACKED_ENTITY_INSTANCE ) )
        {

            // Should be not be null
            if ( item.getTrackedEntity() == null )
            {
                result.add( newReport( TrackerErrorCode.E4002 ).addArg( "trackedEntity" ).addArg( "relationshipType" )
                    .addArg( TRACKED_ENTITY_INSTANCE ) );
            }

            // Should be null
            if ( item.getEnrollment() != null )
            {
                result.add( newReport( TrackerErrorCode.E4001 ).addArg( "enrollment" ).addArg( "relationshipType" )
                    .addArg( TRACKED_ENTITY_INSTANCE ) );
            }

            // Should be null
            if ( item.getEvent() != null )
            {
                result.add( newReport( TrackerErrorCode.E4001 ).addArg( "event" ).addArg( "relationshipType" )
                    .addArg( TRACKED_ENTITY_INSTANCE ) );
            }

        }
        else if ( relationshipType.getRelationshipEntity().equals( PROGRAM_INSTANCE ) )
        {

            // Should be null
            if ( item.getTrackedEntity() != null )
            {
                result.add( newReport( TrackerErrorCode.E4001 ).addArg( "trackedEntity" ).addArg( "relationshipType" )
                    .addArg( PROGRAM_INSTANCE ) );
            }

            // Should not be null
            if ( item.getEnrollment() == null )
            {
                result.add( newReport( TrackerErrorCode.E4002 ).addArg( "enrollment" ).addArg( "relationshipType" )
                    .addArg( PROGRAM_INSTANCE ) );
            }

            // Should be null
            if ( item.getEvent() != null )
            {
                result.add( newReport( TrackerErrorCode.E4001 ).addArg( "event" ).addArg( "relationshipType" )
                    .addArg( PROGRAM_INSTANCE ) );
            }

        }
        else if ( relationshipType.getRelationshipEntity().equals( PROGRAM_STAGE_INSTANCE ) )
        {

            // Should be null
            if ( item.getTrackedEntity() != null )
            {
                result.add( newReport( TrackerErrorCode.E4001 ).addArg( "trackedEntity" ).addArg( "relationshipType" )
                    .addArg( PROGRAM_STAGE_INSTANCE ) );
            }

            // Should be null
            if ( item.getEnrollment() != null )
            {
                result.add( newReport( TrackerErrorCode.E4001 ).addArg( "enrollment" ).addArg( "relationshipType" )
                    .addArg( PROGRAM_STAGE_INSTANCE ) );
            }

            // Should not be null
            if ( item.getEvent() == null )
            {
                result.add( newReport( TrackerErrorCode.E4002 ).addArg( "event" ).addArg( "relationshipType" )
                    .addArg( PROGRAM_STAGE_INSTANCE ) );
            }

        }

        return result;
    }
}
