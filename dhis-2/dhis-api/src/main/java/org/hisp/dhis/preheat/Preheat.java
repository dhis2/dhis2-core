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
package org.hisp.dhis.preheat;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Preheat
{
    /**
     * User to use for import job (important for threaded imports).
     */
    private User user;

    /**
     * Internal map of all objects mapped by identifier => class type => uid.
     */
    private final Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>>> map = new EnumMap<>(
        PreheatIdentifier.class );

    /**
     * Internal map of all default object (like category option combo, etc).
     */
    private Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = new HashMap<>();

    /**
     * Map of unique columns, mapped by class type => uid => value.
     */
    private Map<Class<? extends IdentifiableObject>, Map<String, Map<Object, String>>> uniquenessMap = new HashMap<>();

    /**
     * All periods available.
     */
    private Map<String, Period> periodMap = new HashMap<>();

    /**
     * All periodTypes available.
     */
    private Map<String, PeriodType> periodTypeMap = new HashMap<>();

    /**
     * Map of all required attributes, mapped by class type.
     */
    private Map<Class<?>, Set<String>> mandatoryAttributes = new HashMap<>();

    /**
     * Map of all unique attributes, mapped by class type.
     */
    private Map<Class<?>, Set<String>> uniqueAttributes = new HashMap<>();

    /**
     * Map of all unique attributes values, mapped by class type => attribute
     * uid => object uid.
     */
    private Map<Class<?>, Map<String, Map<String, String>>> uniqueAttributeValues = new HashMap<>();

    /**
     * Map of all metadata attributes, mapped by class type.
     * <p>
     * Only Class which has attribute will be put into this map.
     */
    private Map<Class<? extends IdentifiableObject>, Map<String, Attribute>> attributesByTargetObjectType = new HashMap<>();

    /**
     * Map of all properties which are {@link org.hisp.dhis.common.SortableObject} mapped by class type.
     * Value is a set of Property names.
     */
    private Map<Class<? extends IdentifiableObject>, Set<String>> mapSortableObjectProperties = new HashMap<>();

    public Preheat()
    {
        for ( PreheatIdentifier identifier : PreheatIdentifier.values() )
        {
            map.put( identifier, new HashMap<>() );
        }
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

    public <T extends IdentifiableObject> T get( PreheatIdentifier identifier,
        Class<? extends IdentifiableObject> klass, IdentifiableObject object )
    {
        return object == null
            ? null
            : get( identifier, klass, identifier.getIdentifier( object ) );
    }

    public <T extends IdentifiableObject> T get( PreheatIdentifier identifier,
        Class<? extends IdentifiableObject> klass, String key )
    {
        Map<String, IdentifiableObject> byKey = getNullable( effectiveIdentifier( identifier, klass ), klass );
        if ( byKey == null )
        {
            return null;
        }
        @SuppressWarnings( "unchecked" )
        T res = (T) byKey.get( key );
        return res;
    }

    /**
     * Get objects from this context by using potentially "shallow" sample
     * objects to extract the keys.
     *
     * @param identifier type of {@link PreheatIdentifier} to use
     * @param samples objects used to extract the keys
     * @return a list of objects from this {@link Preheat} context in the order
     *         of given objects but not containing {@code null} values for
     *         objects not found in this context
     */
    public <T extends IdentifiableObject> List<T> getAll( PreheatIdentifier identifier, List<T> samples )
    {
        if ( samples == null || samples.isEmpty() )
        {
            return emptyList();
        }
        // Implementation Note: calling single get in a loop is intentionally
        // not used to gain less overhead
        Class<? extends IdentifiableObject> klass = getObjectType( samples.iterator().next() );
        identifier = effectiveIdentifier( identifier, klass );
        Map<String, IdentifiableObject> byKey = getNullable( identifier, klass );
        if ( byKey == null )
        {
            return emptyList();
        }
        List<T> objects = new ArrayList<>( samples.size() );
        for ( T sample : samples )
        {
            @SuppressWarnings( "unchecked" )
            T object = (T) byKey.get( identifier.getIdentifier( sample ) );

            if ( object != null )
            {
                objects.add( object );
            }
        }
        return objects;
    }

    public <T extends IdentifiableObject> T get( PreheatIdentifier identifier, T object )
    {
        if ( object == null )
        {
            return null;
        }

        Class<? extends IdentifiableObject> klass = getObjectType( object );
        identifier = effectiveIdentifier( identifier, klass );

        if ( PreheatIdentifier.UID == identifier )
        {
            return get( PreheatIdentifier.UID, klass, object.getUid() );
        }

        if ( PreheatIdentifier.CODE == identifier )
        {
            return get( PreheatIdentifier.CODE, klass, object.getCode() );
        }

        return null;
    }

    public boolean containsKey( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        Map<String, IdentifiableObject> byKey = getNullable( effectiveIdentifier( identifier, klass ), klass );
        return byKey != null && byKey.containsKey( key );
    }

    public boolean isEmpty()
    {
        return map.values().stream().allMatch( Map::isEmpty );
    }

    public boolean isEmpty( PreheatIdentifier identifier )
    {
        return map.get( identifier ).isEmpty();
    }

    public boolean isEmpty( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass )
    {
        Map<String, IdentifiableObject> byKey = map.get( identifier ).get( klass );
        return byKey == null || byKey.isEmpty();
    }

    public <T extends IdentifiableObject> Preheat put( PreheatIdentifier identifier, T object )
    {
        if ( object == null )
        {
            return this;
        }
        Class<? extends IdentifiableObject> klass = getObjectType( object );
        return put( effectiveIdentifier( identifier, klass ), klass, object );
    }

    private Preheat put( PreheatIdentifier identifier,
        Class<? extends IdentifiableObject> klass, IdentifiableObject object )
    {
        String key = identifier.getIdentifier( object );
        if ( StringUtils.isEmpty( key ) )
        {
            return this;
        }
        put( getOrCreate( identifier, klass ), identifier, key, object );
        return this;
    }

    private void put( Map<String, IdentifiableObject> byKey, PreheatIdentifier identifier, String key,
        IdentifiableObject object )
    {
        byKey.putIfAbsent( key, object );
    }

    public <T extends IdentifiableObject> Preheat replace( PreheatIdentifier identifier, T object )
    {
        if ( object == null )
        {
            return this;
        }
        Class<? extends IdentifiableObject> klass = getObjectType( object );
        return replace( effectiveIdentifier( identifier, klass ), klass, object );
    }

    private Preheat replace( PreheatIdentifier identifier,
        Class<? extends IdentifiableObject> klass, IdentifiableObject object )
    {
        String key = identifier.getIdentifier( object );
        if ( StringUtils.isEmpty( key ) )
        {
            return this;
        }
        getOrCreate( identifier, klass ).put( key, object );
        return this;
    }

    /**
     * Implementation Note: This might look a bit overly complicated since a
     * simple for loop calling the single element put would work as well. The
     * idea here is to avoid doing first two levels of lookup and initialisation
     * over and over since the list of objects might be large.
     */
    public <T extends IdentifiableObject> Preheat put( PreheatIdentifier identifier, Collection<T> objects )
    {
        if ( objects == null || objects.isEmpty() )
        {
            return this;
        }
        Class<? extends IdentifiableObject> klass = getObjectType( objects.iterator().next() );
        identifier = effectiveIdentifier( identifier, klass );
        Map<String, IdentifiableObject> byKey = getOrCreate( identifier, klass );
        for ( T object : objects )
        {
            if ( !isDefault( object ) )
            {
                String key = identifier.getIdentifier( object );
                if ( !StringUtils.isEmpty( key ) )
                {
                    put( byKey, identifier, key, object );
                }
            }
        }
        return this;
    }

    public Preheat remove( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        Map<String, IdentifiableObject> byKey = getNullable( identifier, klass );
        if ( byKey != null )
        {
            byKey.remove( key );
        }
        return this;
    }

    public Preheat remove( PreheatIdentifier identifier, IdentifiableObject object )
    {
        return object == null
            ? this
            : remove( identifier, getObjectType( object ), identifier.getIdentifier( object ) );
    }

    public Preheat remove( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass,
        Collection<String> keys )
    {
        Map<String, IdentifiableObject> byKey = getNullable( identifier, klass );
        if ( byKey != null )
        {
            keys.forEach( byKey::remove );
        }
        return this;
    }

    public Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults()
    {
        return defaults;
    }

    public void setDefaults( Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults )
    {
        this.defaults = defaults;
    }

    public void setUniquenessMap(
        Map<Class<? extends IdentifiableObject>, Map<String, Map<Object, String>>> uniquenessMap )
    {
        this.uniquenessMap = uniquenessMap;
    }

    public Map<Class<? extends IdentifiableObject>, Map<String, Map<Object, String>>> getUniquenessMap()
    {
        return uniquenessMap;
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

    public Map<Class<?>, Set<String>> getMandatoryAttributes()
    {
        return mandatoryAttributes;
    }

    public void setMandatoryAttributes( Map<Class<?>, Set<String>> mandatoryAttributes )
    {
        this.mandatoryAttributes = mandatoryAttributes;
    }

    public Map<Class<?>, Set<String>> getUniqueAttributes()
    {
        return uniqueAttributes;
    }

    public void setUniqueAttributes( Map<Class<?>, Set<String>> uniqueAttributes )
    {
        this.uniqueAttributes = uniqueAttributes;
    }

    public Map<Class<?>, Map<String, Map<String, String>>> getUniqueAttributeValues()
    {
        return uniqueAttributeValues;
    }

    public void setUniqueAttributeValues( Map<Class<?>, Map<String, Map<String, String>>> uniqueAttributeValues )
    {
        this.uniqueAttributeValues = uniqueAttributeValues;
    }

    public static boolean isDefaultClass( Class<?> klass )
    {
        return Category.class.isAssignableFrom( klass ) || CategoryOption.class.isAssignableFrom( klass )
            || CategoryCombo.class.isAssignableFrom( klass ) || CategoryOptionCombo.class.isAssignableFrom( klass );
    }

    public static boolean isDefaultObject( IdentifiableObject object )
    {
        return isDefaultClass( HibernateProxyUtils.getRealClass( object ) );
    }

    public boolean isDefault( IdentifiableObject object )
    {
        if ( !isDefaultObject( object ) )
        {
            return false;
        }

        IdentifiableObject defaultObject = getDefaults().get( getObjectType( object ) );

        return defaultObject != null && defaultObject.getUid().equals( object.getUid() );
    }

    /**
     * Get list of {@link Attribute} which the given klass has.
     *
     * @param klass Class to be used for querying.
     * @return Set of {@link Attribute} belong to given klass.
     */
    public Map<String, Attribute> getAttributesByClass( Class<? extends IdentifiableObject> klass )
    {
        return attributesByTargetObjectType.get( klass );
    }

    /**
     * Add given list of {@link Attribute} to map attributesByTargetObjectType.
     *
     * @param klass Class which has given list of {@link Attribute}.
     * @param attributes List of {@link Attribute} to be added.
     */
    public void addClassAttributes( Class<? extends IdentifiableObject> klass, Set<Attribute> attributes )
    {
        attributesByTargetObjectType.put( klass,
            attributes.stream().collect( Collectors.toMap( Attribute::getUid, Attribute -> Attribute ) ) );
    }

    /**
     * Add given {@link Attribute} to map attributesByTargetObjectType.
     *
     * @param klass Class which has given list of {@link Attribute}.
     * @param attribute {@link Attribute} to be added.
     */
    public void addClassAttribute( Class<? extends IdentifiableObject> klass, Attribute attribute )
    {
        if ( attributesByTargetObjectType.get( klass ) == null )
        {
            attributesByTargetObjectType.put( klass, new HashMap<>() );
        }

        attributesByTargetObjectType.get( klass ).put( attribute.getUid(), attribute );
    }

    /**
     * Get Set of all attribute ID of given klass which has given valueType.
     *
     * @param klass Class to be used for querying.
     * @param valueType {@link ValueType} to be used for querying.
     * @return Set of {@link Attribute} ID.
     */
    public Set<String> getAttributeIdsByValueType( Class<? extends IdentifiableObject> klass, ValueType valueType )
    {
        Map<String, Attribute> attributes = attributesByTargetObjectType.get( klass );

        if ( MapUtils.isEmpty( attributes ) )
        {
            return emptySet();
        }

        return attributes.values().stream()
            .filter( attribute -> attribute.getValueType() == valueType )
            .map( attribute -> attribute.getUid() ).collect( Collectors.toUnmodifiableSet() );
    }

    /*
     * For use in unit tests only (package private)
     */

    boolean hasKlassKeys( PreheatIdentifier identifier )
    {
        return getKlassKeyCount( identifier ) > 0;
    }

    int getKlassKeyCount( PreheatIdentifier identifier )
    {
        return map.get( identifier ).size();
    }

    int getIdentifierKeyCount( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass )
    {
        Map<String, IdentifiableObject> byKey = getNullable( identifier, klass );
        return byKey == null ? 0 : byKey.size();
    }

    private Map<String, IdentifiableObject> getNullable( PreheatIdentifier identifier,
        Class<? extends IdentifiableObject> klass )
    {
        return map.get( identifier ).get( klass );
    }

    private Map<String, IdentifiableObject> getOrCreate( PreheatIdentifier identifier,
        Class<? extends IdentifiableObject> klass )
    {
        return map.get( identifier ).computeIfAbsent( klass, key -> new HashMap<>() );
    }

    private static PreheatIdentifier effectiveIdentifier( PreheatIdentifier identifier,
        Class<? extends IdentifiableObject> klass )
    {
        return (klass == User.class
            || klass == UserRole.class)
                ? PreheatIdentifier.UID
                : identifier;
    }

    @SuppressWarnings( "unchecked" )
    private <T extends IdentifiableObject> Class<? extends IdentifiableObject> getObjectType( T object )
    {
        return HibernateProxyUtils.getRealClass( object );
    }
}
