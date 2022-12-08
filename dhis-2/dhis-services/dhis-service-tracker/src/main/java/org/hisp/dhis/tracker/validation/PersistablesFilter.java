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

import java.util.ArrayList;
import java.util.Collection;
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

import lombok.RequiredArgsConstructor;

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
// TODO naming. commit or persist? CommittablesFilter, PersistablesFilter?
// variations in logic to consider when refactoring
// != DELETE
//  direction: is top-down (TEI -> EN -> EV -> REL)
//  conditions: current is valid && includes parents and if parent exists or is persistable
// DELETE
//  direction: is bottom up (REL -> EV -> EN -> TEI)
//  conditions: current is valid && includes children and if children are persistable
// CASCADE
//  direction: does not matter
//  conditions: current is valid

// PATTERNS
// links are always comprised of TrackerType and UID; so what I am doing for RelationshipItem
// could be done for the other links as well; pack these into a TrackerDto only containing type and UID
// then pass that around

// != DELETE
// filter
//   current == valid &&
//   for each link (type and uid): persistable or exists
// collect in persistables

// == DELETE
// filter
//   current == invalid
//   for each link (type and uid)
// collect in nonPersistables

// filter
//   current == valid &&
//   current == persistable (not in nonPersistable)
// collect in persistables
// the difference to the != DELETE is that we also collect a nonPersistable structure with all the invalid parents

public class PersistablesFilter
{
    /**
     * Result of {@link #filter(TrackerBundle, EnumMap, TrackerImportStrategy)}
     * operation indicating all entities that can be persisted. The meaning of
     * persisted comes from the context which includes the
     * {@link TrackerImportStrategy} and whether the entity existed or not (for
     * {@link TrackerImportStrategy#CREATE_AND_UPDATE}).
     */
    public static class Result
    {
        private final List<TrackedEntity> trackedEntities = new ArrayList<>();

        private final List<Enrollment> enrollments = new ArrayList<>();

        private final List<Event> events = new ArrayList<>();

        private final List<Relationship> relationships = new ArrayList<>();

        private <T extends TrackerDto> void putAll( Class<T> type, Collection<T> instance )
        {
            List<T> list = get( Objects.requireNonNull( type ) );
            list.addAll( instance );
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
            return null;
        }
    }

    /**
     * <p>
     * Using {@link TrackerDto} in functions to parents {@link Check#parents} as
     * a tuple of UID and {@link TrackerType}. This makes working with the
     * different types (trackedEntity, enrollment, ...) easier.
     * </p>
     */
    @RequiredArgsConstructor
    private static class Checks
    {
        private static final Check<TrackedEntity> trackedEntityCheck = new Check<>( TrackedEntity.class );

        private static final Check<Enrollment> enrollmentCheck = new Check<>(
            Enrollment.class,
            List.of(
                en -> TrackedEntity.builder().trackedEntity( en.getTrackedEntity() ).build() // parent
            ) );

        private static final Check<Event> eventCheck = new Check<>(
            Event.class,
            List.of(
                ev -> Enrollment.builder().enrollment( ev.getEnrollment() ).build() // parent
            ),
            parent -> StringUtils.isBlank( parent.getUid() ) // events in event programs have no enrollment
        );

        private static final Check<Relationship> relationshipsCheck = new Check<>(
            Relationship.class,
            List.of(
                rel -> toTrackerDto( rel.getFrom() ), // parents
                rel -> toTrackerDto( rel.getTo() ) ) );

        /**
         * Collects non-deletable parent entities on DELETE and persistable
         * entities otherwise. Checking each non-root "layer" depends on the
         * knowledge (marked entities) we gain from the previous layers. For
         * example on DELETE event, enrollment, trackedEntity entities cannot be
         * deleted if an invalid relationship points to them.
         */
        private final EnumMap<TrackerType, Set<String>> markedEntities = new EnumMap<>( Map.of(
            TRACKED_ENTITY, new HashSet<>(),
            ENROLLMENT, new HashSet<>(),
            EVENT, new HashSet<>(),
            RELATIONSHIP, new HashSet<>() ) );

        private final TrackerBundle entities;

        private final EnumMap<TrackerType, Set<String>> invalidEntities;

        private final TrackerImportStrategy importStrategy;

        @SuppressWarnings( "unchecked" )
        private <T extends TrackerDto> Check<T> get( Class<T> type )
        {
            Objects.requireNonNull( type );
            if ( type == TrackedEntity.class )
            {
                return (Check<T>) trackedEntityCheck;
            }
            else if ( type == Enrollment.class )
            {
                return (Check<T>) enrollmentCheck;
            }
            else if ( type == Event.class )
            {
                return (Check<T>) eventCheck;
            }
            else if ( type == Relationship.class )
            {
                return (Check<T>) relationshipsCheck;
            }
            return null;
        }

        private Result apply()
        {
            List<TrackerType> traversalOrder = TrackerType.getOrderedByPriority(); // top-down
            if ( importStrategy == TrackerImportStrategy.DELETE )
            {
                Collections.reverse( traversalOrder ); // bottom-up
            }

            Result result = new Result();
            for ( TrackerType type : traversalOrder )
            {
                // TODO how can I make the compiler happy? Generic hell :joy:
                result.putAll( type.getKlass(), persistable( type, get( type.getKlass() ),
                    entities.get( type.getKlass() ), entities.getPreheat() ) );
            }
            return result;
        }

        private <T extends TrackerDto> List<T> persistable( TrackerType klass, Check<T> check, List<T> entities,
            TrackerPreheat preheat )
        {
            List<T> persistables = entities.stream()
                .filter( entityCondition() )
                .filter( parentConditions( check, preheat ) )
                .collect( Collectors.toList() );

            markEntities( klass, check, entities, persistables );

            return persistables;
        }

        private <T extends TrackerDto> Predicate<T> entityCondition()
        {
            Predicate<T> entityConditions = baseCondition();

            if ( this.importStrategy == TrackerImportStrategy.DELETE )
            {
                entityConditions = entityConditions.and( deleteCondition() ); // parents of invalid children cannot be deleted
            }

            return entityConditions;
        }

        /**
         * Every entity needs to be valid irrespective of
         * {@link TrackerImportStrategy} or whether it's a child/parent i.e. not
         * have any errors detected by our validation.
         *
         * @return predicate testing validity
         * @param <T> type to test
         */
        private <T extends TrackerDto> Predicate<T> baseCondition()
        {
            return t -> isValid( this.invalidEntities, t );
        }

        private <T extends TrackerDto> Predicate<T> deleteCondition()
        {
            return t -> !isContained( this.markedEntities, t );
        }

        private <T extends TrackerDto> Predicate<T> parentConditions( Check<T> check, TrackerPreheat preheat )
        {
            if ( this.importStrategy == TrackerImportStrategy.DELETE )
            {
                return t -> true; // on DELETE parents are checked via conditions on parent nodes instead of children
            }

            final Predicate<T> baseParentCondition = parent -> isContained( this.markedEntities, parent )
                || preheat.exists( parent.getTrackerType(), parent.getUid() );
            final Predicate<T> parentCondition = check.parentCondition.map( baseParentCondition::or )
                .orElse( baseParentCondition );

            return check.parents.stream()
                .map( p -> (Predicate<T>) t -> parentCondition.test( (T) p.apply( t ) ) ) // children of invalid parents can only be persisted under certain conditions
                .reduce( Predicate::and )
                .orElse( t -> true ); // predicate always returning true for entities without parents
        }

        private <T extends TrackerDto> void markEntities( TrackerType klass, Check<T> check, List<T> entities,
            List<T> persistables )
        {
            if ( this.importStrategy == TrackerImportStrategy.DELETE )
            {
                List<? extends TrackerDto> nonDeletable = nonDeletableParents( check, entities );
                nonDeletable.forEach( t -> this.markedEntities.get( t.getTrackerType() ).add( t.getUid() ) );
            }
            else
            {
                this.markedEntities.put( klass, collectUids( persistables ) );
            }
        }

        /**
         * Determines parents of invalid children. Such parents cannot be
         * deleted. Examples are a valid trackedEntity (parent) which has an
         * invalid enrollment (child) or the from and to (parents) of an invalid
         * relationship (child).
         *
         * @param check to be run
         * @param entities entities to find parents of invalid children
         * @return parents of entities of type T which cannot be deleted
         * @param <T> type to find non-deletable parents on
         */
        private <T extends TrackerDto> List<? extends TrackerDto> nonDeletableParents( Check<T> check,
            List<T> entities )
        {
            return entities.stream()
                .filter( Predicate.not( entityCondition() ) )
                .map( t -> check.parents.stream().map( p -> p.apply( t ) ).collect( Collectors.toList() ) ) // parents of invalid children
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );
        }
    }

    private static class Check<T extends TrackerDto>
    {
        private final List<Function<T, ? extends TrackerDto>> parents;

        private final Optional<Predicate<? super TrackerDto>> parentCondition;

        private final Class<T> type;

        public Check( Class<T> type )
        {
            this( type, Collections.emptyList() );
        }

        public Check( Class<T> type, List<Function<T, ? extends TrackerDto>> parents )
        {
            this.type = type;
            this.parents = parents;
            this.parentCondition = Optional.empty();
        }

        public Check( Class<T> type, List<Function<T, ? extends TrackerDto>> parents,
            Predicate<? super TrackerDto> parentCondition )
        {
            this.type = type;
            this.parents = parents;
            this.parentCondition = Optional.of( parentCondition );
        }
    }

    public static Result filter( TrackerBundle bundle,
        EnumMap<TrackerType, Set<String>> invalidEntities, TrackerImportStrategy importStrategy )
    {
        // TODO think about the design. This does not feel right.
        return new Checks( bundle, invalidEntities, importStrategy ).apply();
    }

    private static boolean isValid( EnumMap<TrackerType, Set<String>> invalidEntities, TrackerDto entity )
    {
        return !isContained( invalidEntities, entity );
    }

    private static boolean isContained( EnumMap<TrackerType, Set<String>> map, TrackerDto entity )
    {
        return map.get( entity.getTrackerType() ).contains( entity.getUid() );
    }

    private static <T extends TrackerDto> Set<String> collectUids( List<T> entities )
    {
        return entities.stream().map( TrackerDto::getUid ).collect( Collectors.toSet() );
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
        return null;
    }
}
