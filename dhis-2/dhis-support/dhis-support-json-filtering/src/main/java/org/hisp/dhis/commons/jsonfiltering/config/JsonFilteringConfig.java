/*
 * Copyright (c) 2004-2004-2020, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;

import org.hisp.dhis.commons.jsonfiltering.bean.BeanInfoIntrospector;
import org.hisp.dhis.commons.jsonfiltering.filter.JsonFilteringPropertyFilter;
import org.hisp.dhis.commons.jsonfiltering.parser.JsonFilteringParser;
import org.hisp.dhis.commons.jsonfiltering.view.PropertyView;

import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;

/**
 * Provides access to various configuration values that the Squiggly library
 * uses.
 * <p>
 * Users can override the default configuration by putting a
 * json-filtering.properties in their classpath.
 */
public class JsonFilteringConfig
{

    private static final SortedMap<String, String> PROPS_MAP;

    private static final SortedMap<String, String> SOURCE_MAP;

    private static final boolean filterImplicitlyIncludeBaseFields;

    private static final boolean filterImplicitlyIncludeBaseFieldsInView;

    private static final CacheBuilderSpec filterPathCacheSpec;

    private static final boolean filterPropagateViewToNestedFilters;

    private static final CacheBuilderSpec parserNodeCacheSpec;

    private static final CacheBuilderSpec propertyDescriptorCacheSpec;

    private static final boolean propertyAddNonAnnotatedFieldsToBaseView;

    static
    {
        Map<String, String> propsMap = Maps.newHashMap();
        Map<String, String> sourceMap = Maps.newHashMap();

        loadProps( propsMap, sourceMap, "json-filtering.default.properties" );
        loadProps( propsMap, sourceMap, "json-filtering.properties" );

        PROPS_MAP = ImmutableSortedMap.copyOf( propsMap );
        SOURCE_MAP = ImmutableSortedMap.copyOf( sourceMap );

        filterImplicitlyIncludeBaseFields = getBool( PROPS_MAP, "filter.implicitlyIncludeBaseFields" );
        filterImplicitlyIncludeBaseFieldsInView = getBool( PROPS_MAP, "filter.implicitlyIncludeBaseFieldsInView" );
        filterPathCacheSpec = getCacheSpec( PROPS_MAP, "filter.pathCache.spec" );
        filterPropagateViewToNestedFilters = getBool( PROPS_MAP, "filter.propagateViewToNestedFilters" );
        parserNodeCacheSpec = getCacheSpec( PROPS_MAP, "parser.nodeCache.spec" );
        propertyAddNonAnnotatedFieldsToBaseView = getBool( PROPS_MAP, "property.addNonAnnotatedFieldsToBaseView" );
        propertyDescriptorCacheSpec = getCacheSpec( PROPS_MAP, "property.descriptorCache.spec" );
    }

    private JsonFilteringConfig()
    {
    }

    private static CacheBuilderSpec getCacheSpec( Map<String, String> props, String key )
    {
        String value = props.get( key );

        if ( value == null )
        {
            value = "";
        }

        return CacheBuilderSpec.parse( value );
    }

    private static boolean getBool( Map<String, String> props, String key )
    {
        return "true".equals( props.get( key ) );
    }

    private static int getInt( Map<String, String> props, String key )
    {
        try
        {
            return Integer.parseInt( props.get( key ) );
        }
        catch ( NumberFormatException e )
        {
            throw new RuntimeException( "Unable to convert " + props.get( key ) + " to int for key " + key );
        }
    }

    private static void loadProps( Map<String, String> propsMap, Map<String, String> sourceMap, String file )
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource( file );

        if ( url == null )
        {
            return;
        }

        Properties fileProps = new Properties();
        InputStream inputStream = null;

        try
        {
            inputStream = url.openStream();
            fileProps.load( inputStream );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "Unable to load properties from classpath resource " + file, e );
        }
        finally
        {
            try
            {
                if ( inputStream != null )
                {
                    inputStream.close();
                }
            }
            catch ( Exception eat )
            {
                // ignore
            }
        }

        for ( Map.Entry<Object, Object> entry : fileProps.entrySet() )
        {
            propsMap.put( entry.getKey().toString(), entry.getValue().toString() );
            sourceMap.put( entry.getKey().toString(), url.toString() );
        }
    }

    /**
     * Determines whether or not to include base fields for nested objects
     *
     * @return true if includes, false if not
     * @see PropertyView
     */
    public static boolean isFilterImplicitlyIncludeBaseFields()
    {
        return filterImplicitlyIncludeBaseFields;
    }

    /**
     * Determines whether or not filters that specify a view also include "base"
     * fields.
     *
     * @return true if includes, false if not
     */
    public static boolean isFilterImplicitlyIncludeBaseFieldsInView()
    {
        return filterImplicitlyIncludeBaseFieldsInView;
    }

    /**
     * Get the {@link CacheBuilderSpec} of the path cache in the json-filtering
     * filter.
     *
     * @return spec
     * @see JsonFilteringPropertyFilter
     */
    public static CacheBuilderSpec getFilterPathCacheSpec()
    {
        return filterPathCacheSpec;
    }

    /**
     * Determines whether or not filters that specify a view also propagtes that
     * view to nested filters.
     * <p>
     * For example, given a view called "full", does the full view also apply to
     * the nested objects or does the nested object only include base fields.
     *
     * @return true if includes, false if not
     */
    public static boolean isFilterPropagateViewToNestedFilters()
    {
        return filterPropagateViewToNestedFilters;
    }

    /**
     * Get the {@link CacheBuilderSpec} of the node cache in the json-filtering
     * parser.
     *
     * @return spec
     * @see JsonFilteringParser
     */
    public static CacheBuilderSpec getParserNodeCacheSpec()
    {
        return parserNodeCacheSpec;
    }

    /**
     * Determines whether or not non-annotated fields are added to the "base"
     * view.
     *
     * @return true/false
     * @see BeanInfoIntrospector
     */
    public static boolean isPropertyAddNonAnnotatedFieldsToBaseView()
    {
        return propertyAddNonAnnotatedFieldsToBaseView;
    }

    /**
     * Get the {@link CacheBuilderSpec} of the descriptor cache in the property
     * view introspector.
     *
     * @return spec
     * @see BeanInfoIntrospector
     */
    public static CacheBuilderSpec getPropertyDescriptorCacheSpec()
    {
        return propertyDescriptorCacheSpec;
    }

    /**
     * Gets all the config as a map.
     *
     * @return map
     */
    public static SortedMap<String, String> asMap()
    {
        return PROPS_MAP;
    }

    /**
     * Gets a map of all the config keys and whose values are the location where
     * that key was read from.
     *
     * @return source map
     */
    public static SortedMap<String, String> asSourceMap()
    {
        return SOURCE_MAP;
    }

    public static void main( String[] args )
    {
        System.out.println( JsonFilteringConfig.asMap() );
    }
}
