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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.schema.Property;
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
        applyExclusions( exclusions, fieldPathMap );

        fieldPaths.clear();
        fieldPaths.addAll( fieldPathMap.values() );
    }

    private void applyProperties( Collection<FieldPath> fieldPaths, Class<?> rootKlass )
    {
        fieldPaths.forEach( fp -> {
            Schema schema = getSchemaByPath( fp, rootKlass );

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
            Schema schema = getSchemaByPath( preset, rootKlass );

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

    private Schema getSchemaByPath( FieldPath fieldPath, Class<?> klass )
    {
        checkNotNull( fieldPath );
        checkNotNull( fieldPath.getPath() );
        checkNotNull( klass );

        // get root schema
        Schema schema = schemaService.getDynamicSchema( klass );
        Property currentProperty;

        for ( String path : fieldPath.getPath() )
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
