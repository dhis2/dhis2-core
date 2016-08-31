package org.hisp.dhis.schema;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.SessionFactory;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.translation.TranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.OrderComparator;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultSchemaService implements SchemaService
{
    private Map<Class<?>, Schema> classSchemaMap = new HashMap<>();

    private Map<String, Schema> singularSchemaMap = new HashMap<>();

    private Map<Class<?>, Schema> dynamicClassSchemaMap = new HashMap<>();

    @Autowired
    private PropertyIntrospectorService propertyIntrospectorService;

    @Autowired
    private List<SchemaDescriptor> descriptors = Lists.newArrayList();

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private TranslationService translationService;

    @PostConstruct
    public void init()
    {
        for ( SchemaDescriptor descriptor : descriptors )
        {
            Schema schema = descriptor.getSchema();

            if ( sessionFactory.getClassMetadata( schema.getKlass() ) != null )
            {
                schema.setPersisted( true );
                schema.setTranslated( translationService.haveTranslations( schema.getKlass().getSimpleName() ) );
            }

            schema.setDisplayName( beautify( schema.getName() ) );

            if ( schema.getProperties().isEmpty() )
            {
                schema.setPropertyMap( Maps.newHashMap( propertyIntrospectorService.getPropertiesMap( schema.getKlass() ) ) );
            }

            classSchemaMap.put( schema.getKlass(), schema );
            singularSchemaMap.put( schema.getSingular(), schema );

            updateSelf( schema );
        }
    }

    @Override
    public Schema getSchema( Class<?> klass )
    {
        if ( klass == null )
        {
            return null;
        }

        klass = ReflectionUtils.getRealClass( klass );

        if ( classSchemaMap.containsKey( klass ) )
        {
            return classSchemaMap.get( klass );
        }

        if ( dynamicClassSchemaMap.containsKey( klass ) )
        {
            return dynamicClassSchemaMap.get( klass );
        }

        return null;
    }

    @Override
    public Schema getDynamicSchema( Class<?> klass )
    {
        if ( klass == null )
        {
            return null;
        }

        Schema schema = getSchema( klass );

        if ( schema != null )
        {
            return schema;
        }

        klass = propertyIntrospectorService.getConcreteClass( ReflectionUtils.getRealClass( klass ) );

        String name = getName( klass );

        schema = new Schema( klass, name, name + "s" );
        schema.setDisplayName( beautify( schema.getName() ) );
        schema.setPropertyMap( new HashMap<>( propertyIntrospectorService.getPropertiesMap( schema.getKlass() ) ) );
        schema.setMetadata( false );

        updateSelf( schema );

        dynamicClassSchemaMap.put( klass, schema );

        return schema;
    }

    private String getName( Class<?> klass )
    {
        if ( klass.isAnnotationPresent( JacksonXmlRootElement.class ) )
        {
            JacksonXmlRootElement rootElement = klass.getAnnotation( JacksonXmlRootElement.class );

            if ( !StringUtils.isEmpty( rootElement.localName() ) )
            {
                return rootElement.localName();
            }
        }

        return CaseFormat.UPPER_CAMEL.to( CaseFormat.LOWER_CAMEL, klass.getSimpleName() );
    }

    @Override
    public Schema getSchemaBySingularName( String name )
    {
        return singularSchemaMap.get( name );
    }

    @Override
    public List<Schema> getSchemas()
    {
        return Lists.newArrayList( classSchemaMap.values() );
    }

    @Override
    public List<Schema> getSortedSchemas()
    {
        List<Schema> schemas = Lists.newArrayList( classSchemaMap.values() );
        Collections.sort( schemas, OrderComparator.INSTANCE );

        return schemas;
    }

    @Override
    public List<Schema> getMetadataSchemas()
    {
        List<Schema> schemas = getSchemas();

        Iterator<Schema> iterator = schemas.iterator();

        while ( iterator.hasNext() )
        {
            Schema schema = iterator.next();

            if ( !schema.isMetadata() )
            {
                iterator.remove();
            }
        }

        Collections.sort( schemas, OrderComparator.INSTANCE );

        return schemas;
    }

    private void updateSelf( Schema schema )
    {
        if ( schema.haveProperty( "__self__" ) )
        {
            Property property = schema.getProperty( "__self__" );
            schema.setName( property.getName() );
            schema.setCollectionName( schema.getPlural() );
            schema.setNamespace( property.getNamespace() );

            schema.getPropertyMap().remove( "__self__" );
        }
    }

    private String beautify( String name )
    {
        String[] camelCaseWords = org.apache.commons.lang3.StringUtils.capitalize( name ).split( "(?=[A-Z])" );
        return org.apache.commons.lang3.StringUtils.join( camelCaseWords, " " ).trim();
    }
}
