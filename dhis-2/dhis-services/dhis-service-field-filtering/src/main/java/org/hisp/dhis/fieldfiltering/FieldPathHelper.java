/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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
package org.hisp.dhis.fieldfiltering;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen
 */
@Component
@RequiredArgsConstructor
public class FieldPathHelper
{
    public static final String PRESET_ALL = "all";

    public static final String PRESET_OWNER = "owner";

    private final SchemaService schemaService;

    public void apply( List<FieldPath> fieldPaths, Class<?> rootKlass )
    {
        if ( rootKlass == null || fieldPaths.isEmpty() )
        {
            return;
        }

        List<FieldPath> presets = fieldPaths.stream().filter( FieldPath::isPreset ).collect( Collectors.toList() );
        List<FieldPath> exclusions = fieldPaths.stream().filter( FieldPath::isExclude ).collect( Collectors.toList() );

        fieldPaths.removeIf( FieldPath::isPreset );
        fieldPaths.removeIf( FieldPath::isExclude );

        Map<String, FieldPath> fieldPathMap = getFieldPathMap( fieldPaths );

        applyProperties( fieldPathMap.values(), rootKlass );
        applyPresets( presets, fieldPathMap, rootKlass );

        calculatePathCount( fieldPathMap.values() ).forEach( ( k, v ) -> {
            if ( v > 1L )
            {
                return;
            }

            applyDefaults( fieldPathMap.get( k ), rootKlass, fieldPathMap );
        } );

        applyExclusions( exclusions, fieldPathMap );

        fieldPaths.clear();
        fieldPaths.addAll( fieldPathMap.values() );
    }

    /**
     * Applies (recursively) default expansion on the remaining field paths, for
     * example the path 'dataElements.dataElementGroups' would get expanded to
     * 'dataElements.dataElementGroups.id' so that we expose the reference
     * identifier.
     */
    private void applyDefaults( FieldPath fieldPath, Class<?> klass, Map<String, FieldPath> fieldPathMap )
    {
        List<String> paths = new ArrayList<>( fieldPath.getPath() );
        paths.add( fieldPath.getName() );

        Schema schema = getSchemaByPath( paths, klass );

        if ( schema == null )
        {
            return;
        }

        Property property = fieldPath.getProperty();

        if ( property.is( PropertyType.COMPLEX ) || property.itemIs( PropertyType.COMPLEX ) )
        {
            expandComplex( fieldPathMap, paths, schema );
        }
        else if ( property.is( PropertyType.REFERENCE ) || property.itemIs( PropertyType.REFERENCE ) )
        {
            expandReference( fieldPathMap, paths, schema );
        }
    }

    private void applyDefault( FieldPath fieldPath, Map<String, FieldPath> fieldPathMap )
    {
        List<String> paths = new ArrayList<>( fieldPath.getPath() );
        paths.add( fieldPath.getName() );

        Property property = fieldPath.getProperty();
        fieldPathMap.put( fieldPath.toFullPath(), fieldPath );

        if ( property.isSimple() )
        {
            return;
        }

        Schema schema = schemaService
            .getDynamicSchema( property.isCollection() ? property.getItemKlass() : property.getKlass() );

        if ( schema == null )
        {
            return;
        }

        if ( property.is( PropertyType.COMPLEX ) || property.itemIs( PropertyType.COMPLEX ) )
        {
            expandComplex( fieldPathMap, paths, schema );
        }
        else if ( property.is( PropertyType.REFERENCE ) || property.itemIs( PropertyType.REFERENCE ) )
        {
            expandReference( fieldPathMap, paths, schema );
        }
    }

    private void expandReference( Map<String, FieldPath> fieldPathMap, List<String> paths, Schema schema )
    {
        Property idProperty = schema.getProperty( "id" );

        FieldPath fp = new FieldPath(
            idProperty.isCollection() ? idProperty.getCollectionName() : idProperty.getName(), paths );
        fp.setProperty( idProperty );

        fieldPathMap.put( fp.toFullPath(), fp );
    }

    private void expandComplex( Map<String, FieldPath> fieldPathMap, List<String> paths, Schema schema )
    {
        schema.getProperties().forEach( p -> {
            FieldPath fp = new FieldPath( p.isCollection() ? p.getCollectionName() : p.getName(), paths );
            fp.setProperty( p );

            // check if anything else needs to be expanded
            applyDefault( fp, fieldPathMap );
        } );
    }

    private void applyProperties( Collection<FieldPath> fieldPaths, Class<?> rootKlass )
    {
        fieldPaths.forEach( fp -> {
            Schema schema = getSchemaByPath( fp.getPath(), rootKlass );

            if ( schema != null )
            {
                fp.setProperty( schema.getProperty( fp.getName() ) );
            }
        } );
    }

    public void applyPresets( List<FieldPath> presets, Map<String, FieldPath> fieldPathMap,
        Class<?> rootKlass )
    {
        List<FieldPath> fieldPaths = new ArrayList<>();

        for ( FieldPath preset : presets )
        {
            Schema schema = getSchemaByPath( preset.getPath(), rootKlass );

            if ( schema == null )
            {
                continue;
            }

            if ( PRESET_ALL.equals( preset.getName() ) )
            {
                schema.getProperties()
                    .forEach( p -> fieldPaths.add( toFieldPath( preset.getPath(), p ) ) );
            }
            else if ( PRESET_OWNER.equals( preset.getName() ) )
            {
                schema.getProperties()
                    .stream().filter( Property::isOwner )
                    .forEach( p -> fieldPaths.add( toFieldPath( preset.getPath(), p ) ) );
            }
        }

        fieldPaths.forEach( fp -> fieldPathMap.put( fp.toFullPath(), fp ) );
    }

    private void applyExclusions( List<FieldPath> exclusions, Map<String, FieldPath> fieldPathMap )
    {
        for ( FieldPath exclusion : exclusions )
        {
            fieldPathMap.remove( exclusion.toFullPath() );
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Calculates a weighted map of paths to find candidates for default
     * expansion.
     */
    private Map<String, Long> calculatePathCount( Collection<FieldPath> fieldPaths )
    {
        Map<String, Long> pathCount = new HashMap<>();

        for ( FieldPath fieldPath : fieldPaths )
        {
            Property property = fieldPath.getProperty();

            if ( property == null )
            {
                continue;
            }

            List<String> paths = new ArrayList<>();

            for ( String path : fieldPath.getPath() )
            {
                paths.add( path );
                pathCount.compute( StringUtils.join( paths, FieldPath.FIELD_PATH_SEPARATOR ),
                    ( key, count ) -> count == null ? 1L : count + 1L );
            }

            if ( property.is( PropertyType.COMPLEX, PropertyType.REFERENCE )
                || property.itemIs( PropertyType.COMPLEX, PropertyType.REFERENCE ) )
            {
                pathCount.compute( fieldPath.toFullPath(),
                    ( key, count ) -> count == null ? 1L : count + 1L );
            }
        }

        return pathCount;
    }

    private FieldPath toFieldPath( List<String> path, Property property )
    {
        String name = property.isCollection() ? property.getCollectionName() : property.getName();

        FieldPath fieldPath = new FieldPath( name, path );
        fieldPath.setProperty( property );

        return fieldPath;
    }

    private Map<String, FieldPath> getFieldPathMap( List<FieldPath> fieldPaths )
    {
        return fieldPaths.stream().collect( Collectors.toMap( FieldPath::toFullPath, Function.identity() ) );
    }

    private Schema getSchemaByPath( List<String> paths, Class<?> klass )
    {
        requireNonNull( paths );
        requireNonNull( klass );

        // get root schema
        Schema schema = schemaService.getDynamicSchema( klass );
        Property currentProperty;

        for ( String path : paths )
        {
            currentProperty = schema.getProperty( path );

            if ( currentProperty == null )
            {
                return null; // invalid path
            }

            if ( currentProperty.isCollection() )
            {
                schema = schemaService.getDynamicSchema( currentProperty.getItemKlass() );
            }
            else
            {
                schema = schemaService.getDynamicSchema( currentProperty.getKlass() );
            }
        }

        return schema;
    }
}
