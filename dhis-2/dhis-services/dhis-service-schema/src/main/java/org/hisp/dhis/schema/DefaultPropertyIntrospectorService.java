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
package org.hisp.dhis.schema;

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.introspection.GistPropertyIntrospector;
import org.hisp.dhis.schema.introspection.HibernatePropertyIntrospector;
import org.hisp.dhis.schema.introspection.JacksonPropertyIntrospector;
import org.hisp.dhis.schema.introspection.PropertyIntrospector;
import org.hisp.dhis.schema.introspection.PropertyPropertyIntrospector;
import org.hisp.dhis.schema.introspection.TranslatablePropertyIntrospector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Default {@link PropertyIntrospectorService} implementation that uses
 * Reflection and Jackson annotations for reading in properties.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.schema.PropertyIntrospectorService" )
public class DefaultPropertyIntrospectorService implements PropertyIntrospectorService
{
    private final Map<Class<?>, Map<String, Property>> classMapCache = new ConcurrentHashMap<>();

    private final PropertyIntrospector introspector;

    @Autowired
    public DefaultPropertyIntrospectorService( SessionFactory sessionFactory )
    {
        this( new HibernatePropertyIntrospector( sessionFactory )
            .then( new JacksonPropertyIntrospector() )
            .then( new TranslatablePropertyIntrospector() )
            .then( new PropertyPropertyIntrospector() )
            .then( new GistPropertyIntrospector() ) );
    }

    @Override
    public Map<String, Property> getPropertiesMap( Class<?> klass )
    {
        return classMapCache.computeIfAbsent( klass, this::scanClass );
    }

    /**
     * Introspect a class and return a map with key=property-name, and
     * value=Property class.
     *
     * @param klass Class to scan
     * @return Map with key=property-name, and value=Property class
     */
    private Map<String, Property> scanClass( Class<?> klass )
    {
        if ( klass.isInterface() && IdentifiableObject.class.isAssignableFrom( klass ) )
        {
            throw new IllegalArgumentException( "Use SchemaService#getConcreteClass to resolve base type: " + klass );
        }
        Map<String, Property> properties = new HashMap<>();
        introspector.introspect( klass, properties );
        return unmodifiableMap( properties );
    }

}
