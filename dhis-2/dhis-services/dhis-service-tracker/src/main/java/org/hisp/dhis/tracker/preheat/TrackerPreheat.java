package org.hisp.dhis.tracker.preheat;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.util.StringUtils;

import com.google.api.client.util.Lists;
import com.scalified.tree.TreeNode;
import com.scalified.tree.multinode.ArrayMultiTreeNode;

import javassist.util.proxy.ProxyFactory;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerPreheat
{
    /**
     * User to use for import job (important for threaded imports).
     */
    @Getter
    @Setter
    private User user;

    /**
     * Internal map of all metadata objects mapped by class type => [id] The value
     * of each id can be either the metadata object's uid, code, name or attribute value
     */
    @Getter
    private Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>> map = new HashMap<>();

    /**
     * Internal tree of all payload references which are not present in the
     * database. This map is required to allow the validation stage to reference
     * root objects (TEI, PS, PSI) which are present in the payload but not stored
     * in the pre-heat object (since they do not exist in the db yet).
     */
    private TreeNode<String> referenceTree = new ArrayMultiTreeNode<>( "ROOT" );

    /**
     * Internal map of all default object (like category option combo, etc).
     */
    @Getter
    @Setter
    private Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = new HashMap<>();

    /**
     * All periods available.
     */
    @Getter
    private Map<String, Period> periodMap = new HashMap<>();

    /**
     * All periodTypes available.
     */
    @Getter
    private Map<String, PeriodType> periodTypeMap = new HashMap<>();

    /**
     * Internal map of all preheated tracked entities, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    @Getter
    @Setter
    private Map<TrackerIdScheme, Map<String, TrackedEntityInstance>> trackedEntities = new HashMap<>();

    /**
     * Internal map of all preheated tracked entity attributes, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    @Getter
    @Setter
    private Map<TrackerIdScheme, Map<String, TrackedEntityAttributeValue>> trackedEntityAttributes = new HashMap<>();

    /**
     * Internal map of all preheated enrollments, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    @Getter
    @Setter
    private Map<TrackerIdScheme, Map<String, ProgramInstance>> enrollments = new HashMap<>();

    /**
     * Internal map of all preheated events, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    @Getter
    @Setter
    private Map<TrackerIdScheme, Map<String, ProgramStageInstance>> events = new HashMap<>();

    /**
     * Internal map of all preheated relationships, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    @Getter
    @Setter
    private Map<TrackerIdScheme, Map<String, Relationship>> relationships = new EnumMap<>( TrackerIdScheme.class );

    /**
     * Internal map of all preheated notes (events and enrollments)
     */
    private Map<TrackerIdScheme, Map<String, TrackedEntityComment>> notes = new EnumMap<>( TrackerIdScheme.class );

    /**
     * A Map of event uid and preheated {@see ProgramInstance}. The value is a List,
     * because the system may return multiple ProgramInstance, which will be
     * detected by validation
     */
    @Getter
    @Setter
    private Map<String, List<ProgramInstance>> programInstances = new HashMap<>();

    /**
     * A Map of program uid and without registration {@see ProgramInstance}.
     */
    private Map<String, ProgramInstance> programInstancesWithoutRegistration = new HashMap<>();

    /**
     * A list of valid usernames that are present in the payload. A username not
     * available in this cache means, payload's username is invalid.
     * These users are primarily used to represent the ValueType.USERNAME of
     * tracked entity attributes, used in validation and persisting TEIs.
     */
    @Getter
    @Setter
    private List<String> usernames = Lists.newArrayList();

    /**
     * A list of all unique attribute values that are both present in the payload
     * and in the database. This is going to be used to validate the uniqueness of
     * attribute values in the Validation phase.
     */
    @Getter
    @Setter
    private List<UniqueAttributeValue> uniqueAttributeValues = Lists.newArrayList();

    /**
     * A list of all Program Instance UID having at least one Event that is not
     * deleted.
     */
    @Getter
    @Setter
    private List<String> programInstanceWithOneOrMoreNonDeletedEvent = Lists.newArrayList();

    /**
     * A list of Program Stage UID having 1 or more Events
     */
    @Getter
    @Setter
    private List<Pair<String, String>> programStageWithEvents = Lists.newArrayList();
    
    /**
     * Identifier map
     */
    @Getter
    @Setter
    private TrackerIdentifierParams identifiers = new TrackerIdentifierParams();

    /**
     * Map of Program ID (primary key) and List of Org Unit ID associated to each
     * program. Note that the List only contains the Org Unit ID of the Org Units
     * that are specified in the import payload.
     */
    @Getter
    @Setter
    private  Map<Long, List<Long>> programWithOrgUnitsMap;
    
    public TrackerPreheat()
    {
    }

    public String getUsername()
    {
        return User.username( user );
    }

    /**
     * Fetch a metadata object from the pre-heat, based on the type of the object
     * and the cached identifier.
     *
     * @param klass The metadata class to fetch
     * @param key The key used during the pre-heat creation
     * @return A metadata object or null
     */
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( Class<? extends IdentifiableObject> klass,
        String key )
    {
        return (T) map.getOrDefault( klass, new HashMap<>() ).get( key );
    }

    /**
     * Fetch all the metadata objects from the pre-heat, by object type
     * 
     * @param klass The metadata class to fetch
     *
     * @return a List of pre-heated object or empty list
     */
    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getAll( Class<T> klass )
    {
        return new ArrayList<>( (Collection<? extends T>) map.getOrDefault( klass, new HashMap<>() ).values() );
    }    

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> TrackerPreheat put( TrackerIdentifier identifier, T object )
    {
        if ( object == null )
        {
            return this;
        }

        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) getRealClass(
            object.getClass() );

        if ( !map.containsKey( klass ) )
        {
            map.put( klass, new HashMap<>() );
        }

        if ( User.class.isAssignableFrom( klass ) )
        {
            if ( !map.containsKey( UserCredentials.class ) )
            {
                map.put( UserCredentials.class, new HashMap<>() );
            }

            User user = (User) object;

            Map<String, IdentifiableObject> identifierMap = map.get( UserCredentials.class );

            if ( !StringUtils.isEmpty( identifier.getIdentifier( user ) ) &&
                !identifierMap.containsKey( identifier.getIdentifier( user ) ) )
            {
                identifierMap.put( identifier.getIdentifier( user ), user.getUserCredentials() );
            }
        }

        resolveKey( identifier, object ).ifPresent( k -> map.get( klass ).put( k, object ) );

        return this;
    }

    public <T extends IdentifiableObject> TrackerPreheat put( TrackerIdentifier identifier, Collection<T> objects )
    {
        for ( T object : objects )
        {
            put( identifier, object );
        }

        return this;
    }

    public TrackedEntityInstance getTrackedEntity( TrackerIdScheme identifier, String trackedEntity )
    {
        if ( !trackedEntities.containsKey( identifier ) )
        {
            return null;
        }

        return trackedEntities.get( identifier ).get( trackedEntity );
    }

    public void putTrackedEntities( TrackerIdScheme identifier, List<TrackedEntityInstance> trackedEntityInstances,
        List<String> allEntities )
    {
        putTrackedEntities( identifier, trackedEntityInstances );

        List<String> uidOnDB = trackedEntityInstances.stream()
            .map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toList() );

        allEntities
            .stream()
            .filter( t -> !uidOnDB.contains( t ) )
            .map( t -> new ReferenceTrackerEntity( t, null ) )
            .forEach( u -> this.addReference( TrackerType.TRACKED_ENTITY, u ) );
    }

    public void putTrackedEntities( TrackerIdScheme identifier, List<TrackedEntityInstance> trackedEntityInstances )
    {

        trackedEntityInstances.forEach( te -> putTrackedEntity( identifier, te.getUid(), te ) );
    }

    private void putTrackedEntity( TrackerIdScheme identifier, String trackedEntity,
        TrackedEntityInstance trackedEntityInstance )
    {
        if ( !trackedEntities.containsKey( identifier ) )
        {
            trackedEntities.put( identifier, new HashMap<>() );
        }

        trackedEntities.get( identifier ).put( trackedEntity, trackedEntityInstance );
    }

    public ProgramInstance getEnrollment( TrackerIdScheme identifier, String enrollment )
    {
        if ( !enrollments.containsKey( identifier ) )
        {
            return null;
        }

        return enrollments.get( identifier ).get( enrollment );
    }

    public void putEnrollments( TrackerIdScheme identifier, List<ProgramInstance> programInstances,
        List<Enrollment> allEntities )
    {
        putEnrollments( identifier, programInstances );
        List<String> uidOnDB = programInstances.stream().map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toList() );

        allEntities
            .stream()
            .filter( t -> !uidOnDB.contains( t.getEnrollment() ) )
            .map( t -> new ReferenceTrackerEntity( t.getEnrollment(), t.getTrackedEntity() ) )
            .forEach( pi -> this.addReference( TrackerType.ENROLLMENT, pi ) );
    }

    public void putEnrollments( TrackerIdScheme identifier, List<ProgramInstance> programInstances )
    {
        programInstances.forEach( pi -> putEnrollment( identifier, pi.getUid(), pi ) );
    }

    public void putEnrollment( TrackerIdScheme identifier, String enrollment, ProgramInstance programInstance )
    {
        if ( !enrollments.containsKey( identifier ) )
        {
            enrollments.put( identifier, new HashMap<>() );
        }

        enrollments.get( identifier ).put( enrollment, programInstance );
    }

    public ProgramStageInstance getEvent( TrackerIdScheme identifier, String event )
    {
        if ( !events.containsKey( identifier ) )
        {
            return null;
        }

        return events.get( identifier ).get( event );
    }

    public void putEvents( TrackerIdScheme identifier, List<ProgramStageInstance> programStageInstances, List<Event> allEntities)
    {
        putEvents( identifier, programStageInstances );

        List<String> uidOnDB = programStageInstances.stream().map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toList() );

        allEntities
            .stream()
            .filter( t -> !uidOnDB.contains( t.getEvent() ) )
            .map( t -> new ReferenceTrackerEntity( t.getEvent(), t.getEnrollment() ) )
            .forEach( psi -> this.addReference( TrackerType.EVENT, psi ) );
    }

    public void putEvents( TrackerIdScheme identifier, List<ProgramStageInstance> programStageInstances)
    {
        programStageInstances.forEach( psi -> putEvent( identifier, psi.getUid(), psi ) );
    }

    public void putEvent( TrackerIdScheme identifier, String event, ProgramStageInstance programStageInstance )
    {
        if ( !events.containsKey( identifier ) )
        {
            events.put( identifier, new HashMap<>() );
        }

        events.get( identifier ).put( event, programStageInstance );
    }

    public void putNotes( List<TrackedEntityComment> trackedEntityComments )
    {
        // Notes are always using UID scheme
        notes.put( TrackerIdScheme.UID, trackedEntityComments.stream().collect(
            Collectors.toMap( TrackedEntityComment::getUid, Function.identity() ) ) );
    }

    public Optional<TrackedEntityComment> getNote( String uid )
    {
        return Optional.ofNullable( notes.getOrDefault( TrackerIdScheme.UID, new HashMap<>() ).get( uid ) );
    }

    public Relationship getRelationship( TrackerIdScheme identifier, String relationship )
    {
        if ( !relationships.containsKey( identifier ) )
        {
            return null;
        }

        return relationships.get( identifier ).get( relationship );
    }

    public void putRelationships( TrackerIdScheme identifier, List<Relationship> relationships )
    {
        relationships.forEach( r -> putRelationship( identifier, r.getUid(), r ) );
    }

    public void putRelationship( TrackerIdScheme identifier, String relationshipUid, Relationship relationship )
    {
        if ( !relationships.containsKey( identifier ) )
        {
            relationships.put( identifier, new HashMap<>() );
        }

        relationships.get( identifier ).put( relationshipUid, relationship );
    }

    public static Class<?> getRealClass( Class<?> klass )
    {
        if ( ProxyFactory.isProxyClass( klass ) )
        {
            klass = klass.getSuperclass();
        }

        return klass;
    }

    public static boolean isDefaultClass( IdentifiableObject object )
    {
        return object != null && isDefaultClass( getRealClass( object.getClass() ) );
    }

    public static boolean isDefaultClass( Class<?> klass )
    {
        klass = getRealClass( klass );

        return Category.class.isAssignableFrom( klass ) || CategoryOption.class.isAssignableFrom( klass )
            || CategoryCombo.class.isAssignableFrom( klass ) || CategoryOptionCombo.class.isAssignableFrom( klass );
    }

    public boolean isDefault( IdentifiableObject object )
    {
        if ( !isDefaultClass( object ) )
        {
            return false;
        }

        Class<?> klass = getRealClass( object.getClass() );
        IdentifiableObject defaultObject = getDefaults().get( klass );

        return defaultObject != null && defaultObject.getUid().equals( object.getUid() );
    }
    
    public ProgramInstance getProgramInstancesWithoutRegistration( String programUid )
    {
        return programInstancesWithoutRegistration.get( programUid );
    }

    public void putProgramInstancesWithoutRegistration( String programUid, ProgramInstance programInstance )
    {
        this.programInstancesWithoutRegistration.put( programUid, programInstance );
    }

    private void addReference( TrackerType trackerType, ReferenceTrackerEntity referenceTrackerEntity )
    {
        if ( trackerType.equals( TrackerType.TRACKED_ENTITY ) )
        {
            referenceTree.add( new ArrayMultiTreeNode<>( referenceTrackerEntity.getUid() ) );

        }
        else if ( trackerType.equals( TrackerType.ENROLLMENT ) || trackerType.equals( TrackerType.EVENT ) )
        {
            final TreeNode<String> node = referenceTree.find( referenceTrackerEntity.getParentUid() );

            if ( node != null )
            {
                node.add( new ArrayMultiTreeNode<>( referenceTrackerEntity.getUid() ) );
            }
            else
            {
                referenceTree.add( new ArrayMultiTreeNode<>( referenceTrackerEntity.getUid() ) );
            }
        } // TODO luciano what about relationship?
    }

    public Optional<ReferenceTrackerEntity> getReference( String uid )
    {
        final TreeNode<String> node = referenceTree.find( uid );
        if ( node != null )
        {
            return Optional.of( new ReferenceTrackerEntity( uid, node.parent().data() ) );
        }
        return Optional.empty();
    }

    @Override
    public String toString()
    {
        return new StringJoiner( ", ", TrackerPreheat.class.getSimpleName() + "[", "]" )
            .add( "map=" + map )
            .toString();
    }

    private <T extends IdentifiableObject> Optional<String> resolveKey( TrackerIdentifier identifier, T object )
    {
        if ( identifier.getIdScheme().equals( TrackerIdScheme.UID ) )
        {
            return Optional.ofNullable( object.getUid() );
        }
        else if ( identifier.getIdScheme().equals( TrackerIdScheme.CODE ) )
        {
            return Optional.ofNullable( object.getCode() );
        }
        else if ( identifier.getIdScheme().equals( TrackerIdScheme.NAME ) )
        {
            return Optional.ofNullable( object.getName() );
        }
        else if ( identifier.getIdScheme().equals( TrackerIdScheme.ATTRIBUTE ) )
        {
            return Optional.ofNullable( identifier.getIdentifier( object ) );
        }
        // TODO TrackerIdScheme.AUTO ??

        return Optional.empty();

    }

}
