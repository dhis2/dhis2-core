/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.commons.jackson.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.commons.jackson.filter.transformers.RenameFieldTransformer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * @author Morten Olav Hansen
 */
@Component
@RequiredArgsConstructor
public class FieldFilterManager
{
    @Qualifier( "jsonMapper" )
    private final ObjectMapper jsonMapper;

    public List<ObjectNode> toObjectNode( FieldFilterParams<?> params )
    {
        List<FieldPath> fieldPaths = FieldFilterParser.parse( params.getFilters() );

        SimpleFilterProvider filterProvider = getSimpleFilterProvider( fieldPaths );
        ObjectMapper objectMapper = jsonMapper.setFilterProvider( filterProvider );

        List<ObjectNode> objectNodes = new ArrayList<>();
        Map<String, FieldTransformer> transformerMap = getTransformerMap( fieldPaths );

        for ( Object object : params.getObjects() )
        {
            ObjectNode objectNode = objectMapper.valueToTree( object );

            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining( fieldNames::add );

            for ( String fieldName : fieldNames )
            {
                if ( transformerMap.containsKey( fieldName ) )
                {
                    FieldTransformer transformer = transformerMap.get( fieldName );
                    transformer.apply( fieldName, objectNode.get( fieldName ), objectNode );
                }
            }

            objectNodes.add( objectNode );
        }

        return objectNodes;
    }

    private SimpleFilterProvider getSimpleFilterProvider( List<FieldPath> fieldPaths )
    {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter( "field-filter", new FieldFilterSimpleBeanPropertyFilter( fieldPaths ) );

        return filterProvider;
    }

    private Map<String, FieldTransformer> getTransformerMap( List<FieldPath> fieldPaths )
    {
        Map<String, FieldTransformer> map = new HashMap<>();

        for ( FieldPath fieldPath : fieldPaths )
        {
            if ( fieldPath.getTransformer() != null )
            {
                switch ( fieldPath.getTransformer().getName() )
                {
                case "rename":
                    map.put( fieldPath.toFullPath(), new RenameFieldTransformer( fieldPath.getTransformer() ) );
                    break;
                default:
                    // invalid transformer
                    break;
                }
            }
        }

        return map;
    }
}
