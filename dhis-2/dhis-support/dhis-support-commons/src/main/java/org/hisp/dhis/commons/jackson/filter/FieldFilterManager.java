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

import org.hisp.dhis.commons.jackson.filter.transformers.IsEmptyFieldTransformer;
import org.hisp.dhis.commons.jackson.filter.transformers.IsNotEmptyFieldTransformer;
import org.hisp.dhis.commons.jackson.filter.transformers.PluckFieldTransformer;
import org.hisp.dhis.commons.jackson.filter.transformers.RenameFieldTransformer;
import org.hisp.dhis.commons.jackson.filter.transformers.SizeFieldTransformer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
        Map<String, List<FieldTransformer>> fieldTransformers = getTransformers( fieldPaths );

        List<ObjectNode> objectNodes = new ArrayList<>();

        for ( Object object : params.getObjects() )
        {
            ObjectNode objectNode = objectMapper.valueToTree( object );
            applyTransformers( objectNode, null, "", fieldTransformers );

            objectNodes.add( objectNode );
        }

        return objectNodes;
    }

    private void applyTransformers( JsonNode node, JsonNode parent, String path,
        Map<String, List<FieldTransformer>> fieldTransformers )
    {
        if ( parent != null && !parent.isArray() && !path.isEmpty() )
        {
            List<FieldTransformer> transformers = fieldTransformers.get( path.substring( 1 ) );
            transformers.forEach( t -> t.apply( path.substring( 1 ), node, parent ) );
        }

        if ( node.isObject() )
        {
            ObjectNode objectNode = (ObjectNode) node;

            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining( fieldNames::add );

            for ( String fieldName : fieldNames )
            {
                applyTransformers( objectNode.get( fieldName ), objectNode, path + "." + fieldName, fieldTransformers );
            }
        }
        else if ( node.isArray() )
        {
            ArrayNode arrayNode = (ArrayNode) node;

            for ( JsonNode item : arrayNode )
            {
                applyTransformers( item, arrayNode, path, fieldTransformers );
            }
        }
    }

    private SimpleFilterProvider getSimpleFilterProvider( List<FieldPath> fieldPaths )
    {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        filterProvider.addFilter( "field-filter", new FieldFilterSimpleBeanPropertyFilter( fieldPaths ) );

        return filterProvider;
    }

    private Map<String, List<FieldTransformer>> getTransformers( List<FieldPath> fieldPaths )
    {
        Map<String, List<FieldTransformer>> map = new HashMap<>();

        for ( FieldPath fieldPath : fieldPaths )
        {
            map.put( fieldPath.toFullPath(), new ArrayList<>() );

            for ( FieldPathTransformer fieldPathTransformer : fieldPath.getTransformers() )
            {
                switch ( fieldPathTransformer.getName() )
                {
                case "rename":
                    map.get( fieldPath.toFullPath() ).add( new RenameFieldTransformer( fieldPathTransformer ) );
                    break;
                case "size":
                    map.get( fieldPath.toFullPath() ).add( new SizeFieldTransformer() );
                    break;
                case "isEmpty":
                    map.get( fieldPath.toFullPath() ).add( new IsEmptyFieldTransformer() );
                    break;
                case "isNotEmpty":
                    map.get( fieldPath.toFullPath() ).add( new IsNotEmptyFieldTransformer() );
                    break;
                case "pluck":
                    map.get( fieldPath.toFullPath() ).add( new PluckFieldTransformer( fieldPathTransformer ) );
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
