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
        private <T extends TrackerDto> Result putAll( Class<T> type, Collection<T> instance )
        {
            List<T> list = get( Objects.requireNonNull( type ) );
            list.addAll( instance );
            return this;
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
        private final Check<TrackedEntity> trackedEntityCheck;

        private final Check<Enrollment> enrollmentCheck;

        private final Check<Event> eventCheck;

        private final Check<Relationship> relationshipsCheck;

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
    }

    @RequiredArgsConstructor
    private static class Check<T extends TrackerDto>
    {

        // TODO lets see if I can remove this token
        private final Class<T> type;

        private final Predicate<T> condition;

        // TODO create a type for parents and parentCondition as they belong together?
        private final List<Function<T, ? extends TrackerDto>> parents;

        private final Predicate<? super TrackerDto> parentCondition;

        public List<T> persistable( List<T> entities )
        {
            Predicate<T> parentConditions = parents.stream()
                .map( p -> (Predicate<T>) t -> parentCondition.test( p.apply( t ) ) )
                .reduce( Predicate::and )
                .orElse( x -> true );

            return entities.stream()
                .filter( condition )
                .filter( parentConditions )
                .collect( Collectors.toList() );
        }

    }

    public static Result filter( TrackerBundle entities,
        EnumMap<TrackerType, Set<String>> invalidEntities, TrackerImportStrategy importStrategy )
    {
        TrackerPreheat preheat = entities.getPreheat();
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

        // this occurs multiple times, we pass a type and a uid: note its not the type of the entity we call get... on
        // as it's a link to another entity
        //                    && (persistables.get( TRACKED_ENTITY ).contains( en.getTrackedEntity() )
        //                        || preheat.exists( TRACKED_ENTITY, en.getTrackedEntity() )) )

        EnumMap<TrackerType, Set<String>> persistables = new EnumMap<>( TrackerType.class );

        // TODO remove this or try if I can make it work?
        // order is defined in the TrackerType either top down which is the default one defined by priority/then reverse needs to be implemented
        // list of functions to parents: given current dto -> getFrom, getTo
        Function<Enrollment, ? extends TrackerDto> enrollmentParent = en -> TrackedEntity.builder()
            .trackedEntity( en.getTrackedEntity() ).build();
        Function<Event, ? extends TrackerDto> eventParent = ev -> Enrollment.builder().enrollment( ev.getEnrollment() )
            .build();
        Function<Relationship, ? extends TrackerDto> relFrom = rel -> toTrackerDto( rel.getFrom() );
        Function<Relationship, ? extends TrackerDto> relTo = rel -> toTrackerDto( rel.getTo() );
        EnumMap<TrackerType, List<Function<? extends TrackerDto, ? extends TrackerDto>>> play = new EnumMap<>( Map.of(
            TRACKED_ENTITY, Collections.emptyList(),
            ENROLLMENT, List.of( enrollmentParent ),
            EVENT, List.of( eventParent ),
            RELATIONSHIP, List.of( relFrom, relTo ) ) );
        Predicate<? super TrackerDto> condition = parent -> persistables.get( parent.getTrackerType() )
            .contains( parent.getUid() )
            || preheat.exists( parent.getTrackerType(), parent.getUid() );
        // the issue is that I lose the information here since I have to cast here?
        //                List<? extends TrackerDto> currentPersistables = ((List<? extends TrackerDto>) entities.get(type.getKlass())).stream()
        //                        .filter(current -> isValid(invalidEntities, current))
        //                        .filter(current -> play.get(type).get(0).apply(type.getKlass().cast(current)).getUid().isEmpty())
        //                        .collect(Collectors.toList());

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

        if ( importStrategy != TrackerImportStrategy.DELETE )
        {
            // TODO how can I make this readable. Can I remove some of the Java clutter?
            // enrollment()
            // condition()
            // parents()
            //      .condition()
            //      .parent()
            //
            // new Check(Enrollment.class,
            //                    en -> isValid(invalidEntities, en),
            //                            new Parents(
            //
            //                            ),
            //             )
            Check<TrackedEntity> teiCheck = new Check<>(
                TrackedEntity.class,
                tei -> isValid( invalidEntities, tei ),
                Collections.emptyList(),
                __ -> true );
            Check<Enrollment> enCheck = new Check<>(
                Enrollment.class,
                en -> isValid( invalidEntities, en ),
                List.of( en -> TrackedEntity.builder().trackedEntity( en.getTrackedEntity() ).build() ),
                parent -> persistables.get( parent.getTrackerType() ).contains( parent.getUid() )
                    || preheat.exists( parent.getTrackerType(), parent.getUid() ) );
            Check<Event> evCheck = new Check<>(
                Event.class,
                ev -> isValid( invalidEntities, ev ),
                List.of( ev -> Enrollment.builder().enrollment( ev.getEnrollment() ).build() ),
                parent -> StringUtils.isBlank( parent.getUid() ) ||
                    persistables.get( parent.getTrackerType() ).contains( parent.getUid() ) ||
                    preheat.exists( parent.getTrackerType(), parent.getUid() ) );
            Check<Relationship> relCheck = new Check<>(
                Relationship.class,
                rel -> isValid( invalidEntities, rel ),
                List.of(
                    rel -> toTrackerDto( rel.getFrom() ),
                    rel -> toTrackerDto( rel.getTo() ) ),
                parent -> persistables.get( parent.getTrackerType() ).contains( parent.getUid() )
                    || preheat.exists( parent.getTrackerType(), parent.getUid() ) );
            Checks checks = new Checks( teiCheck, enCheck, evCheck, relCheck );

            Result result = new Result();
            // go top-down : TODO make the direction clear in the enum itself. priority does not really explain that we are going top-down in a tree
            for ( TrackerType type : TrackerType.getOrderedByPriority() )
            {
                List<? extends TrackerDto> persistableEntities = checks.get( type.getKlass() )
                    .persistable( entities.get( type.getKlass() ) );
                result.putAll( type.getKlass(), persistableEntities );
                persistables.put( type, collectUids( persistableEntities ) );
            }
            return result;
        }
        else
        {
            EnumMap<TrackerType, Set<String>> nonPersistableParents = new EnumMap<>( Map.of(
                TRACKED_ENTITY, new HashSet<>(),
                ENROLLMENT, new HashSet<>(),
                EVENT, new HashSet<>(),
                RELATIONSHIP, new HashSet<>() ) );

            // A relationship is a child of its from/to entities which can be any of trackedEntity, enrollment, event
            // it can thus be deleted independently of their state (valid/invalid). If a relationship is invalid its
            // parents cannot be deleted.

            // if current == valid
            List<Relationship> persistableRelationships = entities.get( Relationship.class ).stream()
                .filter( rel -> isValid( invalidEntities, rel ) )
                .collect( Collectors.toList() );
            persistables.put( RELATIONSHIP, collectUids( persistableRelationships ) );

            // TODO clean this up
            // what is the pattern?
            // I collect the links (to parents) for every current that is invalid
            Set<TrackerDto> nonPersistableFroms = entities.get( Relationship.class ).stream()
                .filter( rel -> isNotValid( invalidEntities, rel ) )
                .map( Relationship::getFrom )
                .map( PersistablesFilter::toTrackerDto )
                .collect( Collectors.toSet() );
            nonPersistableFroms.stream()
                .forEach( t -> nonPersistableParents.get( t.getTrackerType() ).add( t.getUid() ) );
            Set<TrackerDto> nonPersistableTos = entities.get( Relationship.class ).stream()
                .filter( rel -> isNotValid( invalidEntities, rel ) )
                .map( Relationship::getTo )
                .map( PersistablesFilter::toTrackerDto )
                .collect( Collectors.toSet() );
            nonPersistableTos.stream()
                .forEach( t -> nonPersistableParents.get( t.getTrackerType() ).add( t.getUid() ) );

            // if current == valid && child == persistable (child: relationships)
            List<Event> persistableEvents = entities.get( Event.class ).stream()
                .filter( ev -> isValid( invalidEntities, ev )
                    && !nonPersistableParents.get( EVENT ).contains( ev.getUid() ) )
                .collect( Collectors.toList() );
            persistables.put( EVENT, collectUids( persistableEvents ) );
            // collect all the enrollments (parents) of invalid events as they cannot be deleted
            Set<String> nonPersistableEnrollments = entities.get( Event.class ).stream()
                .filter( ev -> isNotValid( invalidEntities, ev ) )
                .map( Event::getEnrollment )
                .filter( StringUtils::isNotBlank ) // ignore events in event programs which do not have a parent
                .collect( Collectors.toSet() );
            nonPersistableParents.get( ENROLLMENT ).addAll( nonPersistableEnrollments );

            // if current == valid && child == persistable (child: events or relationships)
            List<Enrollment> persistableEnrollments = entities.get( Enrollment.class ).stream()
                .filter( en -> isValid( invalidEntities, en )
                    && !nonPersistableParents.get( ENROLLMENT ).contains( en.getUid() ) )
                .collect( Collectors.toList() );
            persistables.put( ENROLLMENT, collectUids( persistableEnrollments ) );
            // collect all the trackedEntities (parents) of invalid enrollments as they cannot be deleted
            Set<String> nonPersistableTeis = entities.get( Enrollment.class ).stream()
                .filter( en -> isNotValid( invalidEntities, en )
                    || nonPersistableParents.get( ENROLLMENT ).contains( en.getUid() ) )
                .map( Enrollment::getTrackedEntity )
                .collect( Collectors.toSet() );
            nonPersistableParents.get( TRACKED_ENTITY ).addAll( nonPersistableTeis );

            // if current == valid && child == persistable (child: enrollments or relationships)
            List<TrackedEntity> persistableTeis = entities.get( TrackedEntity.class ).stream()
                .filter( tei -> isValid( invalidEntities, tei )
                    && !nonPersistableParents.get( TRACKED_ENTITY ).contains( tei.getUid() ) )
                .collect( Collectors.toList() );
            persistables.put( TRACKED_ENTITY, collectUids( persistableTeis ) );

            return new Result()
                .putAll( TrackedEntity.class, persistableTeis )
                .putAll( Enrollment.class, persistableEnrollments )
                .putAll( Event.class, persistableEvents )
                .putAll( Relationship.class, persistableRelationships );
        }
    }

    private static boolean isValid( EnumMap<TrackerType, Set<String>> invalidEntities, TrackerDto entity )
    {
        return !isNotValid( invalidEntities, entity );
    }

    private static boolean isNotValid( EnumMap<TrackerType, Set<String>> invalidEntities, TrackerDto entity )
    {
        return invalidEntities.get( entity.getTrackerType() ).contains( entity.getUid() );
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
