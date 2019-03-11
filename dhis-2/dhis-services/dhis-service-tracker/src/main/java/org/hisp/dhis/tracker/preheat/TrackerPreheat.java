package org.hisp.dhis.tracker.preheat;

/*
 * Copyright (c) 2004-2019, University of Oslo
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
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
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
    private Map<TrackerIdentifier, Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>>> map = new HashMap<>();

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
    private Map<TrackerIdentifier, Map<String, TrackedEntityInstance>> trackedEntities = new HashMap<>();

    /**
     * Internal map of all preheated tracked entity attributes, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    private Map<TrackerIdentifier, Map<String, TrackedEntityAttributeValue>> trackedEntityAttributes = new HashMap<>();

    /**
     * Internal map of all preheated enrollments, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    private Map<TrackerIdentifier, Map<String, ProgramInstance>> enrollments = new HashMap<>();

    /**
     * Internal map of all preheated events, mainly used for confirming existence for updates, and used
     * for object merging.
     */
    private Map<TrackerIdentifier, Map<String, ProgramStageInstance>> events = new HashMap<>();

    public TrackerPreheat()
    {
    }

    public User getUser()
    {
        return user;
    }

    public String getUsername()
    {
        return user != null ? user.getUsername() : "system-process";
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public <T extends IdentifiableObject> T get( TrackerIdentifier identifier, Class<? extends IdentifiableObject> klass, IdentifiableObject object )
    {
        return get( identifier, klass, identifier.getIdentifier( object ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( TrackerIdentifier identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        if ( !containsKey( identifier, klass, key ) )
        {
            return null;
        }

        return (T) map.get( identifier ).get( klass ).get( key );
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getAll( TrackerIdentifier identifier, List<T> keys )
    {
        List<T> objects = new ArrayList<>();

        for ( T key : keys )
        {
            IdentifiableObject identifiableObject = get( identifier, key );

            if ( identifiableObject != null )
            {
                objects.add( (T) identifiableObject );
            }
        }

        return objects;
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( TrackerIdentifier identifier, T object )
    {
        if ( object == null )
        {
            return null;
        }

        T reference = null;

        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) getRealClass( object.getClass() );

        if ( TrackerIdentifier.UID == identifier || TrackerIdentifier.AUTO == identifier )
        {
            reference = get( TrackerIdentifier.UID, klass, object.getUid() );
        }

        if ( TrackerIdentifier.CODE == identifier || (reference == null && TrackerIdentifier.AUTO == identifier) )
        {
            reference = get( TrackerIdentifier.CODE, klass, object.getCode() );
        }

        return reference;
    }

    public boolean containsKey( TrackerIdentifier identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        return !(isEmpty() || isEmpty( identifier ) || isEmpty( identifier, klass )) && map.get( identifier ).get( klass ).containsKey( key );
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean isEmpty( TrackerIdentifier identifier )
    {
        return !map.containsKey( identifier ) || map.get( identifier ).isEmpty();
    }

    public boolean isEmpty( TrackerIdentifier identifier, Class<? extends IdentifiableObject> klass )
    {
        return isEmpty( identifier ) || !map.get( identifier ).containsKey( klass ) || map.get( identifier ).get( klass ).isEmpty();
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> TrackerPreheat put( TrackerIdentifier identifier, T object )
    {
        if ( object == null ) return this;

        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) getRealClass( object.getClass() );

        if ( TrackerIdentifier.UID == identifier || TrackerIdentifier.AUTO == identifier )
        {
            if ( !map.containsKey( TrackerIdentifier.UID ) ) map.put( TrackerIdentifier.UID, new HashMap<>() );
            if ( !map.get( TrackerIdentifier.UID ).containsKey( klass ) ) map.get( TrackerIdentifier.UID ).put( klass, new HashMap<>() );

            if ( User.class.isAssignableFrom( klass ) )
            {
                if ( !map.get( TrackerIdentifier.UID ).containsKey( UserCredentials.class ) )
                {
                    map.get( TrackerIdentifier.UID ).put( UserCredentials.class, new HashMap<>() );
                }

                User user = (User) object;

                Map<String, IdentifiableObject> identifierMap = map.get( TrackerIdentifier.UID ).get( UserCredentials.class );

                if ( !StringUtils.isEmpty( user.getUid() ) && !identifierMap.containsKey( user.getUid() ) )
                {
                    identifierMap.put( user.getUid(), user.getUserCredentials() );
                }
            }

            Map<String, IdentifiableObject> identifierMap = map.get( TrackerIdentifier.UID ).get( klass );
            String key = TrackerIdentifier.UID.getIdentifier( object );

            if ( !StringUtils.isEmpty( key ) && !identifierMap.containsKey( key ) )
            {
                identifierMap.put( key, object );
            }
        }

        if ( TrackerIdentifier.CODE == identifier || TrackerIdentifier.AUTO == identifier )
        {
            if ( !map.containsKey( TrackerIdentifier.CODE ) ) map.put( TrackerIdentifier.CODE, new HashMap<>() );
            if ( !map.get( TrackerIdentifier.CODE ).containsKey( klass ) ) map.get( TrackerIdentifier.CODE ).put( klass, new HashMap<>() );

            if ( User.class.isAssignableFrom( klass ) )
            {
                if ( !map.get( TrackerIdentifier.CODE ).containsKey( UserCredentials.class ) )
                {
                    map.get( TrackerIdentifier.CODE ).put( UserCredentials.class, new HashMap<>() );
                }

                User user = (User) object;

                Map<String, IdentifiableObject> identifierMap = map.get( TrackerIdentifier.CODE ).get( UserCredentials.class );
                identifierMap.put( user.getCode(), user.getUserCredentials() );
            }

            Map<String, IdentifiableObject> identifierMap = map.get( TrackerIdentifier.CODE ).get( klass );
            String key = TrackerIdentifier.CODE.getIdentifier( object );

            if ( !StringUtils.isEmpty( key ) && !identifierMap.containsKey( key ) )
            {
                identifierMap.put( key, object );
            }
        }

        return this;
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> TrackerPreheat replace( TrackerIdentifier identifier, T object )
    {
        if ( object == null ) return this;

        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) getRealClass( object.getClass() );

        if ( TrackerIdentifier.UID == identifier || TrackerIdentifier.AUTO == identifier )
        {
            if ( !map.containsKey( TrackerIdentifier.UID ) ) map.put( TrackerIdentifier.UID, new HashMap<>() );
            if ( !map.get( TrackerIdentifier.UID ).containsKey( klass ) ) map.get( TrackerIdentifier.UID ).put( klass, new HashMap<>() );

            if ( User.class.isAssignableFrom( klass ) )
            {
                if ( !map.get( TrackerIdentifier.UID ).containsKey( UserCredentials.class ) )
                {
                    map.get( TrackerIdentifier.UID ).put( UserCredentials.class, new HashMap<>() );
                }

                User user = (User) object;

                Map<String, IdentifiableObject> identifierMap = map.get( TrackerIdentifier.UID ).get( UserCredentials.class );

                if ( !StringUtils.isEmpty( user.getUid() ) && !identifierMap.containsKey( user.getUid() ) )
                {
                    identifierMap.put( user.getUid(), user.getUserCredentials() );
                }
            }

            Map<String, IdentifiableObject> identifierMap = map.get( TrackerIdentifier.UID ).get( klass );
            String key = TrackerIdentifier.UID.getIdentifier( object );

            if ( !StringUtils.isEmpty( key ) )
            {
                identifierMap.put( key, object );
            }
        }

        if ( TrackerIdentifier.CODE == identifier || TrackerIdentifier.AUTO == identifier )
        {
            if ( !map.containsKey( TrackerIdentifier.CODE ) ) map.put( TrackerIdentifier.CODE, new HashMap<>() );
            if ( !map.get( TrackerIdentifier.CODE ).containsKey( klass ) ) map.get( TrackerIdentifier.CODE ).put( klass, new HashMap<>() );

            if ( User.class.isAssignableFrom( klass ) )
            {
                if ( !map.get( TrackerIdentifier.CODE ).containsKey( UserCredentials.class ) )
                {
                    map.get( TrackerIdentifier.CODE ).put( UserCredentials.class, new HashMap<>() );
                }

                User user = (User) object;

                Map<String, IdentifiableObject> identifierMap = map.get( TrackerIdentifier.CODE ).get( UserCredentials.class );
                identifierMap.put( user.getCode(), user.getUserCredentials() );
            }

            Map<String, IdentifiableObject> identifierMap = map.get( TrackerIdentifier.CODE ).get( klass );
            String key = TrackerIdentifier.CODE.getIdentifier( object );

            if ( !StringUtils.isEmpty( key ) )
            {
                identifierMap.put( key, object );
            }
        }

        return this;
    }

    public <T extends IdentifiableObject> TrackerPreheat put( TrackerIdentifier identifier, Collection<T> objects )
    {
        for ( T object : objects )
        {
            if ( isDefault( object ) ) continue;
            put( identifier, object );
        }

        return this;
    }

    public TrackerPreheat remove( TrackerIdentifier identifier, Class<? extends IdentifiableObject> klass, String key )
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
        Class<? extends IdentifiableObject> klass = (Class<? extends IdentifiableObject>) getRealClass( object.getClass() );

        if ( TrackerIdentifier.UID == identifier || TrackerIdentifier.AUTO == identifier )
        {
            String key = TrackerIdentifier.UID.getIdentifier( object );

            if ( containsKey( TrackerIdentifier.UID, klass, key ) )
            {
                map.get( TrackerIdentifier.UID ).get( klass ).remove( key );
            }
        }

        if ( TrackerIdentifier.CODE == identifier || TrackerIdentifier.AUTO == identifier )
        {
            String key = TrackerIdentifier.CODE.getIdentifier( object );

            if ( containsKey( TrackerIdentifier.CODE, klass, key ) )
            {
                map.get( TrackerIdentifier.CODE ).get( klass ).remove( key );
            }
        }

        return this;
    }

    public TrackerPreheat remove( TrackerIdentifier identifier, Class<? extends IdentifiableObject> klass, Collection<String> keys )
    {
        for ( String key : keys )
        {
            remove( identifier, klass, key );
        }

        return this;
    }

    public Map<TrackerIdentifier, Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>>> getMap()
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

    public Map<TrackerIdentifier, Map<String, TrackedEntityInstance>> getTrackedEntities()
    {
        return trackedEntities;
    }

    public void setTrackedEntities( Map<TrackerIdentifier, Map<String, TrackedEntityInstance>> trackedEntities )
    {
        this.trackedEntities = trackedEntities;
    }

    public void putTrackedEntities( TrackerIdentifier identifier, List<TrackedEntityInstance> trackedEntityInstances )
    {
        trackedEntityInstances.forEach( te -> putTrackedEntity( identifier, te.getUid(), te ) );
    }

    private void putTrackedEntity( TrackerIdentifier identifier, String trackedEntity, TrackedEntityInstance trackedEntityInstance )
    {
        if ( !trackedEntities.containsKey( identifier ) )
        {
            trackedEntities.put( identifier, new HashMap<>() );
        }

        trackedEntities.get( identifier ).put( trackedEntity, trackedEntityInstance );
    }

    public Map<TrackerIdentifier, Map<String, TrackedEntityAttributeValue>> getTrackedEntityAttributes()
    {
        return trackedEntityAttributes;
    }

    public void setTrackedEntityAttributes( Map<TrackerIdentifier, Map<String, TrackedEntityAttributeValue>> trackedEntityAttributes )
    {
        this.trackedEntityAttributes = trackedEntityAttributes;
    }

    public Map<TrackerIdentifier, Map<String, ProgramInstance>> getEnrollments()
    {
        return enrollments;
    }

    public void setEnrollments( Map<TrackerIdentifier, Map<String, ProgramInstance>> enrollments )
    {
        this.enrollments = enrollments;
    }

    public ProgramInstance getEnrollment( TrackerIdentifier identifier, String enrollment )
    {
        if ( !enrollments.containsKey( identifier ) )
        {
            return null;
        }

        return enrollments.get( identifier ).get( enrollment );
    }

    public void putEnrollments( TrackerIdentifier identifier, List<ProgramInstance> programInstances )
    {
        programInstances.forEach( pi -> putEnrollment( identifier, pi.getUid(), pi ) );
    }

    public void putEnrollment( TrackerIdentifier identifier, String enrollment, ProgramInstance programInstance )
    {
        if ( !enrollments.containsKey( identifier ) )
        {
            enrollments.put( identifier, new HashMap<>() );
        }

        enrollments.get( identifier ).put( enrollment, programInstance );
    }

    public Map<TrackerIdentifier, Map<String, ProgramStageInstance>> getEvents()
    {
        return events;
    }

    public void setEvents( Map<TrackerIdentifier, Map<String, ProgramStageInstance>> events )
    {
        this.events = events;
    }

    public ProgramStageInstance getEvent( TrackerIdentifier identifier, String event )
    {
        if ( !events.containsKey( identifier ) )
        {
            return null;
        }

        return events.get( identifier ).get( event );
    }

    public void putEvents( TrackerIdentifier identifier, List<ProgramStageInstance> programStageInstances )
    {
        programStageInstances.forEach( psi -> putEvent( identifier, psi.getUid(), psi ) );
    }

    public void putEvent( TrackerIdentifier identifier, String event, ProgramStageInstance programStageInstance )
    {
        if ( !events.containsKey( identifier ) )
        {
            events.put( identifier, new HashMap<>() );
        }

        events.get( identifier ).put( event, programStageInstance );
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

    @Override
    public String toString()
    {
        return new StringJoiner( ", ", TrackerPreheat.class.getSimpleName() + "[", "]" )
            .add( "map=" + map )
            .toString();
    }
}
