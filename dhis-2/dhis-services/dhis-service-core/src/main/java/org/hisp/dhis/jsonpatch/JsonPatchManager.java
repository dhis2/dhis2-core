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
package org.hisp.dhis.jsonpatch;

import static org.hisp.dhis.util.JsonUtils.jsonToObject;

import java.util.Collection;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.EmbeddedObject;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Small manager class to handle the JSON patch processing, handles converting
 * from/to JSON nodes, and then applying the JSON patch.
 *
 * @author Morten Olav Hansen
 */
@Service
public class JsonPatchManager
{
    private final ObjectMapper jsonMapper;

    private final SchemaService schemaService;

    public JsonPatchManager(
        ObjectMapper jsonMapper,
        SchemaService schemaService )
    {
        this.jsonMapper = jsonMapper;
        this.schemaService = schemaService;
    }

    /**
     * Applies a JSON patch to any valid jackson java object. It uses
     * valueToTree to convert from the object into a tree like node structure,
     * and this is where the patch will be applied. This means that any property
     * renaming etc will be followed.
     *
     * @param patch JsonPatch object with the operations it should apply.
     * @param object Jackson Object to apply the patch to.
     * @return New instance of the object with the patch applied.
     */
    @Transactional( readOnly = true )
    @SuppressWarnings( "unchecked" )
    public <T> T apply( JsonPatch patch, T object )
        throws JsonPatchException
    {
        if ( patch == null || object == null )
        {
            return null;
        }

        Schema schema = schemaService.getSchema( object.getClass() );
        JsonNode node = jsonMapper.valueToTree( object );

        // since valueToTree does not properly handle our deeply nested classes,
        // we need to make another trip to make sure all collections are
        // correctly made into json nodes.
        handleCollectionUpdates( object, schema, (ObjectNode) node );

        node = patch.apply( node );
        return (T) jsonToObject( node, object.getClass(), jsonMapper,
            ex -> new JsonPatchException( ex.getMessage() ) );
    }

    private <T> void handleCollectionUpdates( T object, Schema schema, ObjectNode node )
    {
        for ( Property property : schema.getProperties() )
        {

            if ( property.isCollection() )
            {
                Object data = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

                Collection<?> collection = (Collection<?>) data;

                if ( CollectionUtils.isEmpty( collection ) )
                {
                    continue;
                }

                if ( BaseIdentifiableObject.class.isAssignableFrom( property.getItemKlass() )
                    && !EmbeddedObject.class.isAssignableFrom( property.getItemKlass() ) )
                {
                    ArrayNode arrayNode = jsonMapper.createArrayNode();

                    collection.forEach( item -> arrayNode.add( jsonMapper.valueToTree(
                        shallowCopyIdentifiableObject( (BaseIdentifiableObject) item ) ) ) );

                    node.set( property.getCollectionName(), arrayNode );
                }
                else
                {
                    node.set( property.getCollectionName(), jsonMapper.valueToTree( data ) );
                }
            }
        }
    }

    /**
     * Create a copy of given {@link BaseIdentifiableObject} but only with two
     * properties: {@link BaseIdentifiableObject#setId(long)} and
     * {@link BaseIdentifiableObject#setUid(String)}. No other properties will
     * be copied.
     *
     * @param source the BaseIdentifiableObject to be cloned.
     * @return a new BaseIdentifiableObject with id and uid properties.
     */
    private BaseIdentifiableObject shallowCopyIdentifiableObject( BaseIdentifiableObject source )
    {
        BaseIdentifiableObject clone = new BaseIdentifiableObject();
        clone.setId( source.getId() );
        clone.setUid( source.getUid() );
        return clone;
    }
}
