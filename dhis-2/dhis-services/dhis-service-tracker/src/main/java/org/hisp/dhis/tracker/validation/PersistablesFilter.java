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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.hisp.dhis.tracker.preheat.Finder;

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
     * Result of
     * {@link #filter(TrackerBundle, EnumMap, Finder, TrackerImportStrategy)}
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

        public <T extends TrackerDto> Result put( Class<T> type, T instance )
        {
            List<T> list = get( Objects.requireNonNull( type ) );
            list.add( instance );
            return this;
        }

        public <T extends TrackerDto> Result putAll( Class<T> type, Collection<T> instance )
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

    public static Result filter( TrackerBundle entities,
        EnumMap<TrackerType, Set<String>> invalidEntities, Finder finder, TrackerImportStrategy importStrategy )
    {
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

        EnumMap<TrackerType, Set<String>> persistables = new EnumMap<>( TrackerType.class );

        if ( importStrategy != TrackerImportStrategy.DELETE )
        {
            // if current == valid
            List<TrackedEntity> persistableTeis = entities.get( TrackedEntity.class ).stream()
                .filter( tei -> isValid( invalidEntities, tei ) )
                .collect( Collectors.toList() );
            persistables.put( TRACKED_ENTITY, collectUids( persistableTeis ) );

            // if current == valid && (parent == persistable || parent exists)
            List<Enrollment> persistableEnrollments = entities.get( Enrollment.class ).stream()
                .filter( en -> isValid( invalidEntities, en )
                    && (persistables.get( TRACKED_ENTITY ).contains( en.getTrackedEntity() )
                        || finder.findById( TrackedEntity.class, en.getTrackedEntity() ).isPresent()) )
                .collect( Collectors.toList() );
            persistables.put( ENROLLMENT, collectUids( persistableEnrollments ) );

            // if current == valid && (parent == null || parent == persistable || parent exists)
            // parent == null; accounts for event programs not having an enrollment set
            List<Event> persistableEvents = entities.get( Event.class ).stream()
                .filter( ev -> isValid( invalidEntities, ev )
                    && (ev.getEnrollment() == null
                        || persistables.get( ENROLLMENT ).contains( ev.getEnrollment() )
                        || finder.findById( Enrollment.class, ev.getEnrollment() ).isPresent()) )
                .collect( Collectors.toList() );
            persistables.put( EVENT, collectUids( persistableEvents ) );

            // TODO clean up this mess of transformations to TrackerDto

            // if current == valid && (from == persistable || from exists) && (to == persistable || to exists)
            List<Relationship> persistableRelationships = entities.get( Relationship.class ).stream()
                .filter( rel -> isValid( invalidEntities, rel )
                    && (persistables.get( toTrackerDto( rel.getFrom() ).getTrackerType() )
                        .contains( toTrackerDto( rel.getFrom() ).getUid() )
                        || finder.findById( toTrackerDto( rel.getFrom() ).getTrackerType().getKlass(),
                            toTrackerDto( rel.getFrom() ).getUid() ).isPresent())
                    && (persistables.get( toTrackerDto( rel.getTo() ).getTrackerType() )
                        .contains( toTrackerDto( rel.getTo() ).getUid() )
                        || finder.findById( toTrackerDto( rel.getTo() ).getTrackerType().getKlass(),
                            toTrackerDto( rel.getTo() ).getUid() ).isPresent()) )
                .collect( Collectors.toList() );
            return new Result()
                .putAll( TrackedEntity.class, persistableTeis )
                .putAll( Enrollment.class, persistableEnrollments )
                .putAll( Event.class, persistableEvents )
                .putAll( Relationship.class, persistableRelationships );
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
