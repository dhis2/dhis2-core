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

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import org.hisp.dhis.commons.jsonfiltering.bean.BeanInfoIntrospector;
import org.hisp.dhis.commons.jsonfiltering.filter.JsonFilteringPropertyFilter;
import org.hisp.dhis.commons.jsonfiltering.parser.JsonFilteringParser;
import org.hisp.dhis.commons.jsonfiltering.view.PropertyView;

import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;

/**
 * Provides access to various configuration values that the JsonFiltering
 * library uses.
 * <p>
 * Users can override the default configuration by putting a
 * json-filtering.properties in their classpath.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class JsonFilteringConfig
{

    private static final SortedMap<String, String> PROPS_MAP;

    private static final SortedMap<String, String> SOURCE_MAP;

    private static final boolean FILTER_IMPLICITLY_INCLUDE_BASE_FIELDS;

    private static final boolean FILTER_IMPLICITLY_INCLUDE_BASE_FIELDS_IN_VIEW;

    private static final CacheBuilderSpec FILTER_PATH_CACHE_SPEC;

    private static final boolean FILTER_PROPAGATE_VIEW_TO_NESTED_FILTERS;

    private static final CacheBuilderSpec PARSER_NODE_CACHE_SPEC;

    private static final CacheBuilderSpec PROPERTY_DESCRIPTOR_CACHE_SPEC;

    private static final boolean PROPERTY_ADD_NON_ANNOTATED_FIELDS_TO_BASE_VIEW;

    static
    {
        Map<String, String> propsMap = Maps.newHashMap();
        Map<String, String> sourceMap = Maps.newHashMap();

        loadProps( propsMap, sourceMap, "json-filtering.default.properties" );
        loadProps( propsMap, sourceMap, "json-filtering.properties" );

        PROPS_MAP = ImmutableSortedMap.copyOf( propsMap );
        SOURCE_MAP = ImmutableSortedMap.copyOf( sourceMap );

        FILTER_IMPLICITLY_INCLUDE_BASE_FIELDS = getBool( "filter.implicitlyIncludeBaseFields" );
        FILTER_IMPLICITLY_INCLUDE_BASE_FIELDS_IN_VIEW = getBool( "filter.implicitlyIncludeBaseFieldsInView" );
        FILTER_PATH_CACHE_SPEC = getCacheSpec( "filter.pathCache.spec" );
        FILTER_PROPAGATE_VIEW_TO_NESTED_FILTERS = getBool( "filter.propagateViewToNestedFilters" );
        PARSER_NODE_CACHE_SPEC = getCacheSpec( "parser.nodeCache.spec" );
        PROPERTY_ADD_NON_ANNOTATED_FIELDS_TO_BASE_VIEW = getBool( "property.addNonAnnotatedFieldsToBaseView" );
        PROPERTY_DESCRIPTOR_CACHE_SPEC = getCacheSpec( "property.descriptorCache.spec" );
    }

    private static CacheBuilderSpec getCacheSpec( String key )
    {
        String value = JsonFilteringConfig.PROPS_MAP.get( key );

        if ( value == null )
        {
            value = "";
        }

        return CacheBuilderSpec.parse( value );
    }

    private static boolean getBool( String key )
    {
        return "true".equals( JsonFilteringConfig.PROPS_MAP.get( key ) );
    }

    @SneakyThrows
    private static void loadProps( Map<String, String> propsMap, Map<String, String> sourceMap, String file )
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource( file );

        if ( url == null )
        {
            return;
        }

        Properties fileProps = new Properties();

        try ( InputStream inputStream = url.openStream() )
        {
            fileProps.load( inputStream );
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
    public static boolean isFILTER_IMPLICITLY_INCLUDE_BASE_FIELDS()
    {
        return FILTER_IMPLICITLY_INCLUDE_BASE_FIELDS;
    }

    /**
     * Determines whether or not filters that specify a view also include "base"
     * fields.
     *
     * @return true if includes, false if not
     */
    public static boolean isFILTER_IMPLICITLY_INCLUDE_BASE_FIELDS_IN_VIEW()
    {
        return FILTER_IMPLICITLY_INCLUDE_BASE_FIELDS_IN_VIEW;
    }

    /**
     * Get the {@link CacheBuilderSpec} of the path cache in the json-filtering
     * filter.
     *
     * @return spec
     * @see JsonFilteringPropertyFilter
     */
    public static CacheBuilderSpec getFILTER_PATH_CACHE_SPEC()
    {
        return FILTER_PATH_CACHE_SPEC;
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
    public static boolean isFILTER_PROPAGATE_VIEW_TO_NESTED_FILTERS()
    {
        return FILTER_PROPAGATE_VIEW_TO_NESTED_FILTERS;
    }

    /**
     * Get the {@link CacheBuilderSpec} of the node cache in the json-filtering
     * parser.
     *
     * @return spec
     * @see JsonFilteringParser
     */
    public static CacheBuilderSpec getPARSER_NODE_CACHE_SPEC()
    {
        return PARSER_NODE_CACHE_SPEC;
    }

    /**
     * Determines whether or not non-annotated fields are added to the "base"
     * view.
     *
     * @return true/false
     * @see BeanInfoIntrospector
     */
    public static boolean isPROPERTY_ADD_NON_ANNOTATED_FIELDS_TO_BASE_VIEW()
    {
        return PROPERTY_ADD_NON_ANNOTATED_FIELDS_TO_BASE_VIEW;
    }

    /**
     * Get the {@link CacheBuilderSpec} of the descriptor cache in the property
     * view introspector.
     *
     * @return spec
     * @see BeanInfoIntrospector
     */
    public static CacheBuilderSpec getPROPERTY_DESCRIPTOR_CACHE_SPEC()
    {
        return PROPERTY_DESCRIPTOR_CACHE_SPEC;
    }

}
