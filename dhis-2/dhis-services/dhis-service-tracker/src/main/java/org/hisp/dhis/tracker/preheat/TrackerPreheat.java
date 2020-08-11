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

import javassist.util.proxy.ProxyFactory;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerPreheat
{
    /**
     * User to use for import job (important for threaded imports).
     */
    private User user;

    /**
     * Internal map of all objects mapped by identifier => class type => uid.
     */
    private Map<TrackerIdScheme, Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>>> map = new HashMap<>();

    /**
     * Internal map of all default object (like category option combo, etc).
     */
    private Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = new HashMap<>();

    /**
     * All periods available.
     */
    private Map<String, Period> periodMap = new HashMap<>();

    /**
     * All periodTypes available.
     */
    private Map<String, PeriodType> periodTypeMap = new HashMap<>();

    /**
     * Set of UIDs of all unique tracked entity attributes.
     */
    private Set<String> uniqueTrackedEntityAttributes = new HashSet<>();

    /**
     * Maps program => attribute for mandatory PTEA.
     */
    private Map<String, String> mandatoryProgramAttributes = new HashMap<>();

    /**
     * Internal map of all preheated tracked entities, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    private Map<TrackerIdScheme, Map<String, TrackedEntityInstance>> trackedEntities = new HashMap<>();

    /**
     * Internal map of all preheated tracked entity attributes, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    private Map<TrackerIdScheme, Map<String, TrackedEntityAttributeValue>> trackedEntityAttributes = new HashMap<>();

    /**
     * Internal map of all preheated enrollments, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    private Map<TrackerIdScheme, Map<String, ProgramInstance>> enrollments = new HashMap<>();

    /**
     * Internal map of all preheated events, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    private Map<TrackerIdScheme, Map<String, ProgramStageInstance>> events = new HashMap<>();

    /**
     * Internal map of all preheated relationships, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    private Map<TrackerIdScheme, Map<String, Relationship>> relationships = new EnumMap<>( TrackerIdScheme.class );

    /**
     * A Map of event uid and preheated {@see ProgramInstance}. The value is a List,
     * because the system may return multiple ProgramInstance, which will be
     * detected by validation
     */
    private Map<String, List<ProgramInstance>> programInstances = new HashMap<>();

    /**
     * Identifier map
     */
    private TrackerIdentifierParams identifiers = new TrackerIdentifierParams();

    public TrackerPreheat()
    {
    }

    public User getUser()
    {
        return user;
    }

    public String getUsername()
    {
        return User.username( user );
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public <T extends IdentifiableObject> T get( TrackerIdentifier identifier,
        Class<? extends IdentifiableObject> klass, IdentifiableObject object )
    {
        return get( identifier.getIdScheme(), klass, identifier.getIdentifier( object ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( TrackerIdScheme identifier, Class<? extends IdentifiableObject> klass,
        String key )
    {
        if ( !containsKey( identifier, klass, key ) )
        {
            return null;
        }

        return (T) map.get( identifier ).get( klass ).get( key );
    }

    public <T extends IdentifiableObject> List<T> getAll( TrackerIdentifier identifier, List<T> keys )
    {
        List<T> objects = new ArrayList<>();

        for ( T key : keys )
        {
            T identifiableObject = get( identifier, key );

            if ( identifiableObject != null )
            {
                objects.add( identifiableObject );
            }
        }

        return objects;
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getAll( TrackerIdScheme identifier, Class<T> klass )
    {
        if ( !map.containsKey( identifier ) || !map.get( identifier ).containsKey( klass ) )
        {
            return new ArrayList<>();
        }

        return new ArrayList<>( (Collection<? extends T>) map.get( identifier ).get( klass ).values() );
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( TrackerIdentifier identifier, T object )
    {
        if ( object == null )
        {
            return null;
        }

        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) getRealClass(
            object.getClass() );

        return get( identifier.getIdScheme(), klass, identifier.getIdentifier( object ) );
    }

    public boolean containsKey( TrackerIdScheme identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        return !(isEmpty() || isEmpty( identifier ) || isEmpty( identifier, klass )) &&
            map.get( identifier ).get( klass ).containsKey( key );
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean isEmpty( TrackerIdScheme identifier )
    {
        return !map.containsKey( identifier ) || map.get( identifier ).isEmpty();
    }

    public boolean isEmpty( TrackerIdScheme identifier, Class<? extends IdentifiableObject> klass )
    {
        return isEmpty( identifier ) || !map.get( identifier ).containsKey( klass ) ||
            map.get( identifier ).get( klass ).isEmpty();
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> TrackerPreheat put( TrackerIdentifier identifier, T object )
    {
        TrackerIdScheme idScheme = identifier.getIdScheme();
        if ( object == null )
            return this;

        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) getRealClass(
            object.getClass() );

        if ( !map.containsKey( idScheme ) )
            map.put( idScheme, new HashMap<>() );
        if ( !map.get( idScheme ).containsKey( klass ) )
            map.get( idScheme ).put( klass, new HashMap<>() );

        if ( User.class.isAssignableFrom( klass ) )
        {
            if ( !map.get( idScheme ).containsKey( UserCredentials.class ) )
            {
                map.get( idScheme ).put( UserCredentials.class, new HashMap<>() );
            }

            User user = (User) object;

            Map<String, IdentifiableObject> identifierMap = map.get( idScheme ).get( UserCredentials.class );

            if ( !StringUtils.isEmpty( identifier.getIdentifier( user ) ) &&
                !identifierMap.containsKey( identifier.getIdentifier( user ) ) )
            {
                identifierMap.put( identifier.getIdentifier( user ), user.getUserCredentials() );
            }
        }

        Map<String, IdentifiableObject> identifierMap = map.get( idScheme ).get( klass );
        String key = identifier.getIdentifier( object );

        if ( !StringUtils.isEmpty( key ) && !identifierMap.containsKey( key ) )
        {
            identifierMap.put( key, object );
        }

        return this;
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> TrackerPreheat replace( TrackerIdentifier identifier, T object )
    {
        TrackerIdScheme idScheme = identifier.getIdScheme();
        if ( object == null )
            return this;

        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) getRealClass(
            object.getClass() );

        if ( !map.containsKey( idScheme ) )
            map.put( idScheme, new HashMap<>() );
        if ( !map.get( idScheme ).containsKey( klass ) )
            map.get( idScheme ).put( klass, new HashMap<>() );

        if ( User.class.isAssignableFrom( klass ) )
        {
            if ( !map.get( idScheme ).containsKey( UserCredentials.class ) )
            {
                map.get( idScheme ).put( UserCredentials.class, new HashMap<>() );
            }

            User user = (User) object;

            Map<String, IdentifiableObject> identifierMap = map.get( idScheme ).get( UserCredentials.class );

            if ( !StringUtils.isEmpty( identifier.getIdentifier( user ) ) &&
                !identifierMap.containsKey( identifier.getIdentifier( user ) ) )
            {
                identifierMap.put( identifier.getIdentifier( user ), user.getUserCredentials() );
            }
        }

        Map<String, IdentifiableObject> identifierMap = map.get( idScheme ).get( klass );
        String key = identifier.getIdentifier( object );

        if ( !StringUtils.isEmpty( key ) )
        {
            identifierMap.put( key, object );
        }

        return this;
    }

    public <T extends IdentifiableObject> TrackerPreheat put( TrackerIdentifier identifier, Collection<T> objects )
    {
        for ( T object : objects )
        {
            boolean isDefault = isDefault( object );
            if ( isDefault )
            {
                continue;
            }

            put( identifier, object );
        }

        return this;
    }

    public TrackerPreheat remove( TrackerIdScheme identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        if ( containsKey( identifier, klass, key ) )
        {
            map.get( identifier ).get( klass ).remove( key );
        }

        return this;
    }

    @SuppressWarnings( "unchecked" )
    public TrackerPreheat remove( TrackerIdentifier identifier, IdentifiableObject object )
    {
        TrackerIdScheme idScheme = identifier.getIdScheme();
        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) getRealClass(
            object.getClass() );

        String key = identifier.getIdentifier( object );

        if ( containsKey( idScheme, klass, key ) )
        {
            map.get( idScheme ).get( klass ).remove( key );
        }

        return this;
    }

    public TrackerPreheat remove( TrackerIdScheme identifier, Class<? extends IdentifiableObject> klass,
        Collection<String> keys )
    {
        for ( String key : keys )
        {
            remove( identifier, klass, key );
        }

        return this;
    }

    public Map<TrackerIdScheme, Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>>> getMap()
    {
        return map;
    }

    public Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults()
    {
        return defaults;
    }

    public void setDefaults( Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults )
    {
        this.defaults = defaults;
    }

    public Map<String, Period> getPeriodMap()
    {
        return periodMap;
    }

    public void setPeriodMap( Map<String, Period> periodMap )
    {
        this.periodMap = periodMap;
    }

    public Map<String, PeriodType> getPeriodTypeMap()
    {
        return periodTypeMap;
    }

    public void setPeriodTypeMap( Map<String, PeriodType> periodTypeMap )
    {
        this.periodTypeMap = periodTypeMap;
    }

    public Set<String> getUniqueTrackedEntityAttributes()
    {
        return uniqueTrackedEntityAttributes;
    }

    public void setUniqueTrackedEntityAttributes( Set<String> uniqueTrackedEntityAttributes )
    {
        this.uniqueTrackedEntityAttributes = uniqueTrackedEntityAttributes;
    }

    public Map<String, String> getMandatoryProgramAttributes()
    {
        return mandatoryProgramAttributes;
    }

    public void setMandatoryProgramAttributes( Map<String, String> mandatoryProgramAttributes )
    {
        this.mandatoryProgramAttributes = mandatoryProgramAttributes;
    }

    public Map<TrackerIdScheme, Map<String, TrackedEntityInstance>> getTrackedEntities()
    {
        return trackedEntities;
    }

    public void setTrackedEntities( Map<TrackerIdScheme, Map<String, TrackedEntityInstance>> trackedEntities )
    {
        this.trackedEntities = trackedEntities;
    }

    public TrackedEntityInstance getTrackedEntity( TrackerIdScheme identifier, String trackedEntity )
    {
        if ( !trackedEntities.containsKey( identifier ) )
        {
            return null;
        }

        return trackedEntities.get( identifier ).get( trackedEntity );
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

    public Map<TrackerIdScheme, Map<String, TrackedEntityAttributeValue>> getTrackedEntityAttributes()
    {
        return trackedEntityAttributes;
    }

    public void setTrackedEntityAttributes(
        Map<TrackerIdScheme, Map<String, TrackedEntityAttributeValue>> trackedEntityAttributes )
    {
        this.trackedEntityAttributes = trackedEntityAttributes;
    }

    public Map<TrackerIdScheme, Map<String, ProgramInstance>> getEnrollments()
    {
        return enrollments;
    }

    public void setEnrollments( Map<TrackerIdScheme, Map<String, ProgramInstance>> enrollments )
    {
        this.enrollments = enrollments;
    }

    public ProgramInstance getEnrollment( TrackerIdScheme identifier, String enrollment )
    {
        if ( !enrollments.containsKey( identifier ) )
        {
            return null;
        }

        return enrollments.get( identifier ).get( enrollment );
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

    public Map<TrackerIdScheme, Map<String, ProgramStageInstance>> getEvents()
    {
        return events;
    }

    public void setEvents( Map<TrackerIdScheme, Map<String, ProgramStageInstance>> events )
    {
        this.events = events;
    }

    public ProgramStageInstance getEvent( TrackerIdScheme identifier, String event )
    {
        if ( !events.containsKey( identifier ) )
        {
            return null;
        }

        return events.get( identifier ).get( event );
    }

    public void putEvents( TrackerIdScheme identifier, List<ProgramStageInstance> programStageInstances )
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

    public Map<TrackerIdScheme, Map<String, Relationship>> getRelationships()
    {
        return relationships;
    }

    public void setRelationships( Map<TrackerIdScheme, Map<String, Relationship>> relationships )
    {
        this.relationships = relationships;
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

    public TrackerIdentifierParams getIdentifiers()
    {
        return identifiers;
    }

    public void setIdentifiers( TrackerIdentifierParams identifiers )
    {
        this.identifiers = identifiers;
    }

    public Map<String, List<ProgramInstance>> getProgramInstances()
    {
        return programInstances;
    }

    public void setProgramInstances(Map<String, List<ProgramInstance>> programInstances)
    {
        this.programInstances = programInstances;
    }
    
    @Override
    public String toString()
    {
        return new StringJoiner( ", ", TrackerPreheat.class.getSimpleName() + "[", "]" )
            .add( "map=" + map )
            .toString();
    }
}
