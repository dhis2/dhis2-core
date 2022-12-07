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

        // TODO I could just replace this with an all args constructor
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

    @RequiredArgsConstructor
    public static class Checks
    {
        private static final Check<TrackedEntity> trackedEntityCheck = new Check<>( TrackedEntity.class );

        private static final Check<Enrollment> enrollmentCheck = new Check<>(
            List.of(
                en -> TrackedEntity.builder().trackedEntity( en.getTrackedEntity() ).build() // parent
            ) );

        private static final Check<Event> eventCheck = new Check<>(
            List.of(
                ev -> Enrollment.builder().enrollment( ev.getEnrollment() ).build() // parent
            ),
            parent -> StringUtils.isBlank( parent.getUid() ) // events in event programs have no enrollment
        );

        private static final Check<Relationship> relationshipsCheck = new Check<>(
            List.of(
                rel -> toTrackerDto( rel.getFrom() ), // parents
                rel -> toTrackerDto( rel.getTo() ) ) );

        // TODO can I reuse one for both purposes? what is the essence. on CREATE_UPDATE it contains the persistable parents
        // on DELETE it contains the non persistable parents. So in both cases these are parents just their meaning changes.
        private final EnumMap<TrackerType, Set<String>> persistables = new EnumMap<>( Map.of(
            TRACKED_ENTITY, new HashSet<>(),
            ENROLLMENT, new HashSet<>(),
            EVENT, new HashSet<>(),
            RELATIONSHIP, new HashSet<>() ) );

        private final EnumMap<TrackerType, Set<String>> nonPersistableParents = new EnumMap<>( Map.of(
            TRACKED_ENTITY, new HashSet<>(),
            ENROLLMENT, new HashSet<>(),
            EVENT, new HashSet<>(),
            RELATIONSHIP, new HashSet<>() ) );

        private final TrackerBundle entities;

        private final EnumMap<TrackerType, Set<String>> invalidEntities;

        private final TrackerImportStrategy importStrategy;

        @SuppressWarnings( "unchecked" )
        public <T extends TrackerDto> Check<T> get( Class<T> type )
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

        public Result apply()
        {
            TrackerPreheat preheat = entities.getPreheat();

            Result result = new Result();

            if ( importStrategy != TrackerImportStrategy.DELETE )
            {
                List<TrackerType> topDown = TrackerType.getOrderedByPriority();
                for ( TrackerType type : topDown )
                {
                    List<? extends TrackerDto> persistableEntities = persistable( get( type.getKlass() ),
                        entities.get( type.getKlass() ), preheat );
                    result.putAll( type.getKlass(), persistableEntities );
                    // TODO can I move this inside persistable?
                    persistables.put( type, collectUids( persistableEntities ) );
                }
            }
            else
            {
                List<TrackerType> bottomUp = TrackerType.getOrderedByPriority();
                Collections.reverse( bottomUp );
                for ( TrackerType type : bottomUp )
                {
                    List<? extends TrackerDto> persistableEntities = persistable( get( type.getKlass() ),
                        entities.get( type.getKlass() ), preheat );
                    result.putAll( type.getKlass(), persistableEntities );
                    // TODO can I move this inside persistable? if so I can make this method super simple :)
                    List<? extends TrackerDto> nonPersist = nonDeletableParents( get( type.getKlass() ),
                        entities.get( type.getKlass() ) );
                    nonPersist.stream()
                        .forEach( t -> nonPersistableParents.get( t.getTrackerType() ).add( t.getUid() ) );
                }
            }
            return result;
        }

        public <T extends TrackerDto> List<T> persistable( Check<T> check, List<T> entities, TrackerPreheat preheat )
        {
            Predicate<T> entityConditions = baseCondition();
            Predicate<T> deleteCondition = deleteCondition();

            // TODO improve/move to a function? and reuse in nonDeletableParents?
            if ( importStrategy == TrackerImportStrategy.DELETE )
            {
                entityConditions = entityConditions.and( deleteCondition ); // parents of invalid children cannot be deleted
            }

            Predicate<T> parentConditions = t -> true;
            if ( importStrategy != TrackerImportStrategy.DELETE )
            {
                Predicate<T> baseParentCondition = parent -> isContained( persistables, parent )
                    || preheat.exists( parent.getTrackerType(), parent.getUid() );
                final Predicate<T> parentCondition;
                parentCondition = check.parentCondition.map( baseParentCondition::or ).orElse( baseParentCondition );
                parentConditions = check.parents.stream()
                    .map( p -> (Predicate<T>) t -> parentCondition.test( (T) p.apply( t ) ) ) // children of invalid parents can only be persisted under certain conditions
                    .reduce( Predicate::and )
                    .orElse( t -> true ); // predicate always returning true for entities without parents
            }

            return entities.stream()
                .filter( entityConditions )
                .filter( parentConditions )
                .collect( Collectors.toList() );
        }

        private <T extends TrackerDto> Predicate<T> baseCondition()
        {
            return t -> isValid( this.invalidEntities, t );
        }

        private <T extends TrackerDto> Predicate<T> deleteCondition()
        {
            return t -> !isContained( nonPersistableParents, t );
        }

        public <T extends TrackerDto> List<? extends TrackerDto> nonDeletableParents( Check<T> check, List<T> entities )
        {
            // TODO improve
            Predicate<T> entityConditions = baseCondition();
            Predicate<T> deleteCondition = deleteCondition();
            entityConditions = entityConditions.and( deleteCondition ); // parents of invalid children cannot be deleted
            // copied this and applied not() to show it's the inverse; which makes sense ;)
            // question is how can we take advantage of this
            entityConditions = Predicate.not( entityConditions );

            return entities.stream()
                .filter( entityConditions )
                .map( t -> check.parents.stream().map( p -> p.apply( t ) ).collect( Collectors.toList() ) ) // parents of invalid children
                .flatMap( Collection::stream )
                .collect( Collectors.toList() );
        }

    }

    private static class Check<T extends TrackerDto>
    {
        private final List<Function<T, ? extends TrackerDto>> parents;

        private final Optional<Predicate<? super TrackerDto>> parentCondition;

        public Check( Class<T> type )
        {
            this( Collections.emptyList() );
        }

        public Check( List<Function<T, ? extends TrackerDto>> parents )
        {
            this.parents = parents;
            this.parentCondition = Optional.empty();
        }

        public Check( List<Function<T, ? extends TrackerDto>> parents, Predicate<? super TrackerDto> parentCondition )
        {
            this.parents = parents;
            this.parentCondition = Optional.of( parentCondition );
        }
    }

    public static Result filter( TrackerBundle entities,
        EnumMap<TrackerType, Set<String>> invalidEntities, TrackerImportStrategy importStrategy )
    {
        return new Checks( entities, invalidEntities, importStrategy ).apply();

    }

    // TODO variations in logic to consider when refactoring
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

    private static boolean isValid( EnumMap<TrackerType, Set<String>> invalidEntities, TrackerDto entity )
    {
        return !isNotValid( invalidEntities, entity );
    }

    private static boolean isNotValid( EnumMap<TrackerType, Set<String>> invalidEntities, TrackerDto entity )
    {
        return isContained( invalidEntities, entity );
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
