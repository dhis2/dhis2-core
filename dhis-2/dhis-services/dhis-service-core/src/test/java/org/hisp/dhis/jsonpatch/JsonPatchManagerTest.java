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
package org.hisp.dhis.jsonpatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Morten Olav Hansen
 */
public class JsonPatchManagerTest
    extends IntegrationTestBase
{
    private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

    @Autowired
    private JsonPatchManager jsonPatchManager;

    @Autowired
    private IdentifiableObjectManager manager;

    @Test
    public void testSimpleAddPatchNoPersist()
        throws Exception
    {
        Constant constant = createConstant( 'A', 1.0d );
        assertEquals( "ConstantA", constant.getName() );
        assertEquals( 1.0d, constant.getValue(), 0 );

        JsonPatch patch = jsonMapper.readValue( "[" +
            "{\"op\": \"add\", \"path\": \"/name\", \"value\": \"updated\"}," +
            "{\"op\": \"add\", \"path\": \"/value\", \"value\": 5.0}" +
            "]", JsonPatch.class );

        assertNotNull( patch );

        Constant patchedConstant = jsonPatchManager.apply( patch, constant );

        assertEquals( "ConstantA", constant.getName() );
        assertEquals( 1.0d, constant.getValue(), 0 );

        assertEquals( "updated", patchedConstant.getName() );
        assertEquals( 5.0d, patchedConstant.getValue(), 0 );
    }

    @Test
    public void testSimpleAddPatchAddPersist()
        throws Exception
    {
        Constant constant = createConstant( 'A', 1.0d );
        manager.save( constant );

        assertEquals( "ConstantA", constant.getName() );
        assertEquals( 1.0d, constant.getValue(), 0 );

        JsonPatch patch = jsonMapper.readValue( "[" +
            "{\"op\": \"add\", \"path\": \"/name\", \"value\": \"updated\"}," +
            "{\"op\": \"add\", \"path\": \"/value\", \"value\": 5.0}" +
            "]", JsonPatch.class );

        assertNotNull( patch );

        Constant patchedConstant = jsonPatchManager.apply( patch, constant );
        patchedConstant.setUid( CodeGenerator.generateUid() );
        manager.save( patchedConstant );

        assertEquals( "ConstantA", constant.getName() );
        assertEquals( 1.0d, constant.getValue(), 0 );

        assertEquals( "updated", patchedConstant.getName() );
        assertEquals( 5.0d, patchedConstant.getValue(), 0 );
    }

    @Test
    public void testCollectionAddPatchNoPersist()
        throws Exception
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );
        assertEquals( "DataElementGroupA", dataElementGroup.getName() );

        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );

        dataElementGroup.getMembers().add( dataElementA );
        dataElementGroup.getMembers().add( dataElementB );

        assertEquals( 2, dataElementGroup.getMembers().size() );

        JsonPatch patch = jsonMapper.readValue( "[" +
            "{\"op\": \"add\", \"path\": \"/name\", \"value\": \"updated\"}," +
            "{\"op\": \"add\", \"path\": \"/dataElements/-\", \"value\": {\"id\": \"my-uid\"}}" +
            "]", JsonPatch.class );

        assertNotNull( patch );

        DataElementGroup patchedDataElementGroup = jsonPatchManager.apply( patch, dataElementGroup );

        assertEquals( "DataElementGroupA", dataElementGroup.getName() );
        assertEquals( 2, dataElementGroup.getMembers().size() );

        assertEquals( "updated", patchedDataElementGroup.getName() );
        assertEquals( 3, patchedDataElementGroup.getMembers().size() );
    }

    @Test
    @Ignore
    public void testCollectionAddPatchAddPersist()
        throws Exception
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );
        assertEquals( "DataElementGroupA", dataElementGroup.getName() );

        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );

        manager.save( dataElementA );
        manager.save( dataElementB );

        dataElementGroup.getMembers().add( dataElementA );
        dataElementGroup.getMembers().add( dataElementB );

        assertEquals( 2, dataElementGroup.getMembers().size() );

        manager.save( dataElementGroup );

        JsonPatch patch = jsonMapper.readValue( "[" +
            "{\"op\": \"add\", \"path\": \"/id\", \"value\": \"" + CodeGenerator.generateUid() + "\"}," +
            "{\"op\": \"add\", \"path\": \"/code\", \"value\": \"new code\"}," +
            "{\"op\": \"add\", \"path\": \"/name\", \"value\": \"new name\"}," +
            "{\"op\": \"add\", \"path\": \"/shortName\", \"value\": \"new short name\"}" +
            "]", JsonPatch.class );

        assertNotNull( patch );

        DataElementGroup patchedDataElementGroup = jsonPatchManager.apply( patch, dataElementGroup );
        manager.save( patchedDataElementGroup );
    }
}
