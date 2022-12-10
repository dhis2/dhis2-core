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
package org.hisp.dhis.tracker.validation;

import static org.hisp.dhis.tracker.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.TrackerType.RELATIONSHIP;
import static org.hisp.dhis.tracker.TrackerType.TRACKED_ENTITY;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.Getter;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;

// TODO(DHIS2-14213) reword all javadocs
// TODO(DHIS2-14213) get rid of compiler warnings
/**
 * Determines whether entities can be persisted (created, updated, deleted)
 * taking into account the {@link TrackerImportStrategy} and the links between
 * entities. For example during {@link TrackerImportStrategy#CREATE} no valid
 * child of an invalid parent can be created (i.e. enrollment of trackedEntity
 * or event of enrollment). During {@link TrackerImportStrategy#UPDATE} a valid
 * child of an invalid parent can be updated if the parent exists.
 * <p>
 * Filtering is only concerned with
 * {@link org.hisp.dhis.tracker.AtomicMode#OBJECT} as only then do we need to
 * determine what is persistable despite errors in the payload. With
 * {@link org.hisp.dhis.tracker.AtomicMode#ALL} only one error suffices to
 * reject the entire payload.
 * </p>
 * <p>
 * This filter relies on validations having run beforehand. The following are
 * some assumptions the filtering relies on:
 * </p>
 * <ul>
 * <li>This does not validate whether the {@link TrackerImportStrategy} matches
 * the state of a given entity. This is expected to be validated by the
 * {@link org.hisp.dhis.tracker.validation.TrackerValidationHook}s. Currently,
 * it's done by
 * {@link org.hisp.dhis.tracker.validation.hooks.PreCheckExistenceValidationHook}
 * So for example a TEI can only be {@link TrackerImportStrategy#UPDATE}d if it
 * exists. Existence is only checked in relation to a parent or child. During
 * {@link TrackerImportStrategy#UPDATE} a valid enrollment can be updated if its
 * parent the TEI is invalid but exists. Same applies to
 * {@link TrackerImportStrategy#DELETE} as you cannot delete an entity that does
 * not yet exist.</li>
 * <li>An {@link Event} in an event program does not have an
 * {@link Event#getEnrollment()} (parent) set. {@link PersistablesFilter} relies
 * on validations having marked entities as invalid that should have a parent.
 * It does so by only checking the parent if it is set.</li>
 * </ul>
 */
// TODO(DHIS2-14213) naming. commit or persist? CommittablesFilter, PersistablesFilter?
// if the Checks/Check stick around think about a better name
class PersistablesFilter
{
    private static final Check<TrackedEntity> TRACKED_ENTITY_CHECK = new Check<>();

    private static final Check<Enrollment> ENROLLMENT_CHECK = new Check<>(
        List.of(
            en -> TrackedEntity.builder().trackedEntity( en.getTrackedEntity() ).build() // parent
        ) );

    private static final Check<Event> EVENT_CHECK = new Check<>(
        List.of(
            ev -> Enrollment.builder().enrollment( ev.getEnrollment() ).build() // parent
        ),
        parent -> StringUtils.isBlank( parent.getUid() ) // events in event programs have no enrollment
    );

    private static final Check<Relationship> RELATIONSHIP_CHECK = new Check<>(
        List.of(
            rel -> toTrackerDto( rel.getFrom() ), // parents
            rel -> toTrackerDto( rel.getTo() ) ) );

    // TODO(DHIS2-14213) in theory we could rely on result and errors we collect to figure out whether something
    // is persistable or deletable. These structures are just not designed for fast lookups.
    /**
     * Collects non-deletable parent entities on DELETE and persistable entities
     * otherwise. Checking each non-root "layer" depends on the knowledge
     * (marked entities) we gain from the previous layers. For example on DELETE
     * event, enrollment, trackedEntity entities cannot be deleted if an invalid
     * relationship points to them.
     */
    private final EnumMap<TrackerType, Set<String>> markedEntities = new EnumMap<>( Map.of(
        TRACKED_ENTITY, new HashSet<>(),
        ENROLLMENT, new HashSet<>(),
        EVENT, new HashSet<>(),
        RELATIONSHIP, new HashSet<>() ) );

    private final Result result = new Result();

    private final TrackerBundle bundle;

    private final TrackerPreheat preheat;

    private final EnumMap<TrackerType, Set<String>> invalidEntities;

    private final TrackerImportStrategy importStrategy;

    public static Result filter( TrackerBundle bundle,
        EnumMap<TrackerType, Set<String>> invalidEntities, TrackerImportStrategy importStrategy )
    {
        return new PersistablesFilter( bundle, invalidEntities, importStrategy ).result;
    }

    private PersistablesFilter( TrackerBundle bundle, EnumMap<TrackerType, Set<String>> invalidEntities,
        TrackerImportStrategy importStrategy )
    {
        this.bundle = bundle;
        this.preheat = bundle.getPreheat();
        this.invalidEntities = invalidEntities;
        this.importStrategy = importStrategy;

        filter();
    }

    private void filter()
    {
        if ( onDelete() )
        {
            // bottom-up
            collectPersistables( Relationship.class, RELATIONSHIP_CHECK, bundle.getRelationships() );
            collectPersistables( Event.class, EVENT_CHECK, bundle.getEvents() );
            collectPersistables( Enrollment.class, ENROLLMENT_CHECK, bundle.getEnrollments() );
            collectPersistables( TrackedEntity.class, TRACKED_ENTITY_CHECK, bundle.getTrackedEntities() );
        }
        else
        {
            // top-down
            collectPersistables( TrackedEntity.class, TRACKED_ENTITY_CHECK, bundle.getTrackedEntities() );
            collectPersistables( Enrollment.class, ENROLLMENT_CHECK, bundle.getEnrollments() );
            collectPersistables( Event.class, EVENT_CHECK, bundle.getEvents() );
            collectPersistables( Relationship.class, RELATIONSHIP_CHECK, bundle.getRelationships() );
        }
    }

    private <T extends TrackerDto> void collectPersistables( Class<T> type, Check<T> check, List<T> entities )
    {
        for ( T entity : entities )
        {
            if ( isValid( entity ) && (isDeletable( entity ) || isCreateOrUpdatable( check, entity )) )
            {
                if ( onCreateOrUpdate() )
                {
                    mark( entity ); // mark as persistable for later children
                }
                this.result.put( type, entity ); // persistable
                continue;
            }

            if ( onDelete() )
            {
                List<TrackerErrorReport> errors = addErrorsForParents( check, entity );
                errors.forEach( this::mark ); // mark parents as non-deletable for potential children
            }
            else
            {
                addErrorsForChildren( check, entity );
            }
        }
    }

    private boolean isNotValid( TrackerDto entity )
    {
        return !isValid( entity );
    }

    private boolean isValid( TrackerDto entity )
    {
        return !isContained( this.invalidEntities, entity );
    }

    private boolean isContained( EnumMap<TrackerType, Set<String>> map, TrackerDto entity )
    {
        return map.get( entity.getTrackerType() ).contains( entity.getUid() );
    }

    private <T extends TrackerDto> boolean isDeletable( T entity )
    {
        return onDelete() && !isMarked( entity );
    }

    private <T extends TrackerDto> boolean isCreateOrUpdatable( Check<T> check, T entity )
    {
        return onCreateOrUpdate() && !hasInvalidParents( check, entity );
    }

    private boolean onCreateOrUpdate()
    {
        return !onDelete();
    }

    private boolean onDelete()
    {
        return this.importStrategy == TrackerImportStrategy.DELETE;
    }

    private <T extends TrackerDto> void mark( T entity )
    {
        this.markedEntities.get( entity.getTrackerType() ).add( entity.getUid() );
    }

    private void mark( TrackerErrorReport error )
    {
        this.markedEntities.get( error.getTrackerType() ).add( error.getUid() );
    }

    private <T extends TrackerDto> boolean isMarked( T entity )
    {
        return isContained( this.markedEntities, entity );
    }

    private <T extends TrackerDto> boolean hasInvalidParents( Check<T> check, T entity )
    {
        return !parentConditions( check ).test( entity );
    }

    private <T extends TrackerDto> Predicate<T> parentConditions( Check<T> check )
    {
        final Predicate<TrackerDto> baseParentCondition = parent -> isMarked( parent )
            || this.preheat.exists( parent.getTrackerType(), parent.getUid() );
        final Predicate<TrackerDto> parentCondition = check.parentCondition.map( baseParentCondition::or )
            .orElse( baseParentCondition );

        return check.parents.stream()
            .map( p -> (Predicate<T>) t -> parentCondition.test( p.apply( t ) ) ) // children of invalid parents can only be persisted under certain conditions
            .reduce( Predicate::and )
            .orElse( t -> true ); // predicate always returning true for entities without parents
    }

    private <T extends TrackerDto> List<TrackerErrorReport> addErrorsForParents( Check<T> check, T entity )
    {
        // add error for parents with entity as reason (only to valid parents in the payload)
        List<TrackerErrorReport> errors = check.parents.stream()
            .map( p -> p.apply( entity ) )
            .filter( this::isValid ) // remove invalid parents
            .filter( this.bundle::exists ) // remove parents not in payload
            .map( p -> error( TrackerErrorCode.E5001, p, entity ) )
            .collect( Collectors.toList() );
        this.result.errors.addAll( errors );
        return errors;
    }

    private <T extends TrackerDto> void addErrorsForChildren( Check<T> check, T entity )
    {
        // add error for entity with parent as a reason
        List<TrackerErrorReport> errors = check.parents.stream()
            .map( p -> p.apply( entity ) )
            .filter( this::isNotValid ) // remove valid parents
            .map( p -> error( TrackerErrorCode.E5000, entity, p ) )
            .collect( Collectors.toList() );
        this.result.errors.addAll( errors );
    }

    private static TrackerErrorReport error( TrackerErrorCode code, TrackerDto notPersistable, TrackerDto reason )
    {
        String message = MessageFormat.format(
            code.getMessage(),
            notPersistable.getTrackerType().getName(),
            notPersistable.getUid(),
            reason.getTrackerType().getName(),
            reason.getUid() );

        return new TrackerErrorReport(
            message,
            code,
            notPersistable.getTrackerType(),
            notPersistable.getUid() );
    }

    /**
     * Captures UID and {@link TrackerType} information in a {@link TrackerDto}.
     * Valid {@link RelationshipItem} only has one of its 3 identifier fields
     * set. The RelationshipItem API does not enforce it since it needs to
     * capture invalid user input. Transform it to a shallow TrackerDto, so it
     * is easier to work with.
     *
     * @param item relationship item
     * @return a tracker dto only capturing the uid and type
     */
    private static TrackerDto toTrackerDto( RelationshipItem item )
    {
        if ( StringUtils.isNotEmpty( item.getTrackedEntity() ) )
        {
            return TrackedEntity.builder().trackedEntity( item.getTrackedEntity() ).build();
        }
        else if ( StringUtils.isNotEmpty( item.getEnrollment() ) )
        {
            return Enrollment.builder().enrollment( item.getEnrollment() ).build();
        }
        else if ( StringUtils.isNotEmpty( item.getEvent() ) )
        {
            return Event.builder().event( item.getEvent() ).build();
        }
        return null; // TODO(DHIS2-14213) isn't throwing better? if none of the above is set its not a valid item
    }

    /**
     * Result of {@link #filter(TrackerBundle, EnumMap, TrackerImportStrategy)}
     * operation indicating all entities that can be persisted. The meaning of
     * persisted comes from the context which includes the
     * {@link TrackerImportStrategy} and whether the entity existed or not (for
     * {@link TrackerImportStrategy#CREATE_AND_UPDATE}).
     */
    @Getter
    public static class Result
    {
        private final List<TrackedEntity> trackedEntities = new ArrayList<>();

        private final List<Enrollment> enrollments = new ArrayList<>();

        private final List<Event> events = new ArrayList<>();

        private final List<Relationship> relationships = new ArrayList<>();

        private final List<TrackerErrorReport> errors = new ArrayList<>();

        private <T extends TrackerDto> void put( Class<T> type, T instance )
        {
            get( Objects.requireNonNull( type ) ).add( instance );
        }

        @SuppressWarnings( "unchecked" )
        public <T extends TrackerDto> List<T> get( Class<T> type )
        {
            Objects.requireNonNull( type );
            if ( type == TrackedEntity.class )
            {
                return (List<T>) trackedEntities;
            }
            else if ( type == Enrollment.class )
            {
                return (List<T>) enrollments;
            }
            else if ( type == Event.class )
            {
                return (List<T>) events;
            }
            else if ( type == Relationship.class )
            {
                return (List<T>) relationships;
            }
            return Collections.emptyList();
        }
    }

    /**
     * <p>
     * Using {@link TrackerDto} in functions to parents {@link Check#parents} as
     * a tuple of UID and {@link TrackerType}. This makes working with the
     * different types (trackedEntity, enrollment, ...) easier.
     * </p>
     */
    private static class Check<T extends TrackerDto>
    {
        private final List<Function<T, TrackerDto>> parents;

        private final Optional<Predicate<TrackerDto>> parentCondition;

        public Check()
        {
            this( Collections.emptyList() );
        }

        public Check( List<Function<T, TrackerDto>> parents )
        {
            this.parents = parents;
            this.parentCondition = Optional.empty();
        }

        public Check( List<Function<T, TrackerDto>> parents, Predicate<TrackerDto> parentCondition )
        {
            this.parents = parents;
            this.parentCondition = Optional.of( parentCondition );
        }
    }
}
