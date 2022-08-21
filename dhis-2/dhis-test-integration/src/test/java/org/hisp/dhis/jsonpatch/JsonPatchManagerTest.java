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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.test.integration.NonTransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Morten Olav Hansen
 */
class JsonPatchManagerTest extends NonTransactionalIntegrationTest
{

    private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

    @Autowired
    private JsonPatchManager jsonPatchManager;

    @Autowired
    private IdentifiableObjectManager manager;

    @Test
    void testSimpleAddPatchNoPersist()
        throws Exception
    {
        Constant constant = createConstant( 'A', 1.0d );
        assertEquals( "ConstantA", constant.getName() );
        assertEquals( constant.getValue(), 0, 1.0d );
        JsonPatch patch = jsonMapper.readValue( "[" + "{\"op\": \"add\", \"path\": \"/name\", \"value\": \"updated\"},"
            + "{\"op\": \"add\", \"path\": \"/value\", \"value\": 5.0}" + "]", JsonPatch.class );
        assertNotNull( patch );
        Constant patchedConstant = jsonPatchManager.apply( patch, constant );
        assertEquals( "ConstantA", constant.getName() );
        assertEquals( constant.getValue(), 0, 1.0d );
        assertEquals( "updated", patchedConstant.getName() );
        assertEquals( patchedConstant.getValue(), 0, 5.0d );
    }

    @Test
    void testSimpleAddPatch()
        throws Exception
    {
        Constant constant = createConstant( 'A', 1.0d );
        assertEquals( "ConstantA", constant.getName() );
        assertEquals( constant.getValue(), 0, 1.0d );
        JsonPatch patch = jsonMapper.readValue( "[" + "{\"op\": \"add\", \"path\": \"/name\", \"value\": \"updated\"},"
            + "{\"op\": \"add\", \"path\": \"/value\", \"value\": 5.0}" + "]", JsonPatch.class );
        assertNotNull( patch );
        Constant patchedConstant = jsonPatchManager.apply( patch, constant );
        patchedConstant.setUid( CodeGenerator.generateUid() );
        assertEquals( "ConstantA", constant.getName() );
        assertEquals( constant.getValue(), 0, 1.0d );
        assertEquals( "updated", patchedConstant.getName() );
        assertEquals( patchedConstant.getValue(), 0, 5.0d );
    }

    @Test
    void testCollectionAddPatchNoPersist()
        throws Exception
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );
        assertEquals( "DataElementGroupA", dataElementGroup.getName() );
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementGroup.getMembers().add( dataElementA );
        dataElementGroup.getMembers().add( dataElementB );
        assertEquals( 2, dataElementGroup.getMembers().size() );
        JsonPatch patch = jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"/name\", \"value\": \"updated\"},"
                + "{\"op\": \"add\", \"path\": \"/dataElements/-\", \"value\": {\"id\": \"my-uid\"}}" + "]",
            JsonPatch.class );
        assertNotNull( patch );
        DataElementGroup patchedDataElementGroup = jsonPatchManager.apply( patch, dataElementGroup );
        assertEquals( "DataElementGroupA", dataElementGroup.getName() );
        assertEquals( 2, dataElementGroup.getMembers().size() );
        assertEquals( "updated", patchedDataElementGroup.getName() );
        assertEquals( 3, patchedDataElementGroup.getMembers().size() );
    }

    @Test
    void testAddAndReplaceSharingUser()
        throws JsonProcessingException,
        JsonPatchException
    {
        User userA = makeUser( "A" );
        manager.save( userA );
        DataElement dataElementA = createDataElement( 'A' );
        manager.save( dataElementA );
        assertEquals( 0, dataElementA.getSharing().getUsers().size() );
        JsonPatch patch = jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"/sharing/users\", \"value\": " + "{" + "\"" + userA.getUid()
                + "\": { \"access\":\"rw------\",\"id\": \"" + userA.getUid() + "\" }" + "}" + "}" + "]",
            JsonPatch.class );
        assertNotNull( patch );
        DataElement patchedDE = jsonPatchManager.apply( patch, dataElementA );
        assertEquals( 1, patchedDE.getSharing().getUsers().size() );
        assertEquals( "rw------", patchedDE.getSharing().getUsers().get( userA.getUid() ).getAccess() );
        JsonPatch replacedPatch = jsonMapper.readValue(
            "[" + "{\"op\": \"replace\", \"path\": \"/sharing/users\", \"value\": " + "{" + "\"" + userA.getUid()
                + "\": { \"access\":\"r-------\",\"id\": \"" + userA.getUid() + "\" }" + "}" + "}" + "]",
            JsonPatch.class );
        DataElement replacePatchedDE = jsonPatchManager.apply( replacedPatch, patchedDE );
        assertEquals( 1, replacePatchedDE.getSharing().getUsers().size() );
        assertEquals( "r-------", replacePatchedDE.getSharing().getUsers().get( userA.getUid() ).getAccess() );
    }

    @Test
    void testAddAndRemoveSharingUser()
        throws JsonProcessingException,
        JsonPatchException
    {
        User userA = makeUser( "A" );
        manager.save( userA );
        DataElement dataElementA = createDataElement( 'A' );
        manager.save( dataElementA );
        assertEquals( 0, dataElementA.getSharing().getUsers().size() );
        JsonPatch patch = jsonMapper.readValue(
            "[" + "{\"op\": \"add\", \"path\": \"/sharing/users\", \"value\": " + "{" + "\"" + userA.getUid()
                + "\": { \"access\":\"rw------\",\"id\": \"" + userA.getUid() + "\" }" + "}" + "}" + "]",
            JsonPatch.class );
        assertNotNull( patch );
        DataElement patchedDE = jsonPatchManager.apply( patch, dataElementA );
        assertEquals( 1, patchedDE.getSharing().getUsers().size() );
        assertEquals( "rw------", patchedDE.getSharing().getUsers().get( userA.getUid() ).getAccess() );
        JsonPatch removePatch = jsonMapper.readValue(
            "[" + "{\"op\": \"remove\", \"path\": \"/sharing/users/" + userA.getUid() + "\" } ]", JsonPatch.class );
        DataElement removedPatchedDE = jsonPatchManager.apply( removePatch, patchedDE );
        assertEquals( 0, removedPatchedDE.getSharing().getUsers().size() );
    }
}
