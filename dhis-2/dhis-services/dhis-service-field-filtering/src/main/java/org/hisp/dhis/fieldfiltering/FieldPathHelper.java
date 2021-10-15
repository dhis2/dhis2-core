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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hisp.dhis.schema.SchemaService;

/**
 * @author Morten Olav Hansen
 */
public class FieldPathHelper
{
    public static List<FieldPath> applyPresets( List<FieldPath> fieldPaths, Class<?> klass,
        SchemaService schemaService )
    {
        List<FieldPath> presets = fieldPaths.stream().filter( FieldPath::isPreset ).collect( Collectors.toList() );

        return fieldPaths;
    }

    public static List<FieldPath> applyExclusions( List<FieldPath> fieldPaths )
    {
        List<FieldPath> exclusions = fieldPaths.stream().filter( FieldPath::isExclude ).collect( Collectors.toList() );
        Map<String, FieldPath> mappedByPath = fieldPaths.stream()
            .filter( fp -> !(fp.isPreset() || fp.isExclude()) )
            .collect( Collectors.toMap( FieldPath::toFullPath, Function.identity() ) );

        exclusions.forEach( ex -> mappedByPath.remove( ex.toFullPath() ) );

        return new ArrayList<>( mappedByPath.values() );
    }
}
