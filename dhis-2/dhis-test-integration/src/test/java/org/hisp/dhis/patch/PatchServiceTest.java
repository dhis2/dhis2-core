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
package org.hisp.dhis.patch;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class PatchServiceTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    private PatchService patchService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    @Autowired
    private ObjectMapper jsonMapper;

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
    }

    @Test
    void testUpdateName()
    {
        DataElement dataElement = createDataElement( 'A' );
        Patch patch = new Patch().addMutation( new Mutation( "name", "Updated Name" ) );
        patchService.apply( patch, dataElement );
        assertEquals( "Updated Name", dataElement.getName() );
    }

    @Test
    void testAddDataElementToGroup()
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        manager.save( deA );
        manager.save( deB );
        assertTrue( dataElementGroup.getMembers().isEmpty() );
        Patch patch = new Patch().addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "dataElements", Lists.newArrayList( deA.getUid(), deB.getUid() ) ) );
        patchService.apply( patch, dataElementGroup );
        assertEquals( "Updated Name", dataElementGroup.getName() );
        assertEquals( 2, dataElementGroup.getMembers().size() );
    }

    @Test
    void testDeleteDataElementFromGroup()
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        manager.save( deA );
        manager.save( deB );
        dataElementGroup.addDataElement( deA );
        dataElementGroup.addDataElement( deB );
        assertEquals( 2, dataElementGroup.getMembers().size() );
        Patch patch = new Patch().addMutation( new Mutation( "name", "Updated Name" ) ).addMutation(
            new Mutation( "dataElements", Lists.newArrayList( deA.getUid() ), Mutation.Operation.DELETION ) );
        patchService.apply( patch, dataElementGroup );
        assertEquals( "Updated Name", dataElementGroup.getName() );
        assertEquals( 1, dataElementGroup.getMembers().size() );
        patch = new Patch().addMutation(
            new Mutation( "dataElements", Lists.newArrayList( deB.getUid() ), Mutation.Operation.DELETION ) );
        patchService.apply( patch, dataElementGroup );
        assertTrue( dataElementGroup.getMembers().isEmpty() );
    }

    @Test
    void testAddAggLevelsToDataElement()
    {
        DataElement dataElement = createDataElement( 'A' );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );
        Patch patch = new Patch().addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "aggregationLevels", 1 ) )
            .addMutation( new Mutation( "aggregationLevels", 2 ) );
        patchService.apply( patch, dataElement );
        assertEquals( 2, dataElement.getAggregationLevels().size() );
    }

    @Test
    void testUpdateValueTypeEnumFromString()
    {
        DataElement dataElement = createDataElement( 'A' );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );
        Patch patch = new Patch().addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "domainType", "TRACKER" ) )
            .addMutation( new Mutation( "valueType", "BOOLEAN" ) );
        patchService.apply( patch, dataElement );
        assertEquals( DataElementDomain.TRACKER, dataElement.getDomainType() );
        assertEquals( ValueType.BOOLEAN, dataElement.getValueType() );
    }

    @Test
    void testUpdateUserOnDataElement()
    {
        User user = makeUser( "A" );
        manager.save( user );
        createAndInjectAdminUser();
        DataElement dataElement = createDataElement( 'A' );
        manager.save( dataElement );
        Patch patch = new Patch().addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "createdBy", user.getUid() ) )
            .addMutation( new Mutation( "domainType", "TRACKER" ) )
            .addMutation( new Mutation( "valueType", "BOOLEAN" ) );
        patchService.apply( patch, dataElement );
        assertEquals( DataElementDomain.TRACKER, dataElement.getDomainType() );
        assertEquals( ValueType.BOOLEAN, dataElement.getValueType() );
        assertEquals( user.getUid(), dataElement.getCreatedBy().getUid() );
    }

    @Test
    void testAddStringAggLevelsToDataElement()
    {
        DataElement dataElement = createDataElement( 'A' );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );
        Patch patch = new Patch().addMutation( new Mutation( "name", "Updated Name" ) )
            .addMutation( new Mutation( "aggregationLevels", "1" ) )
            .addMutation( new Mutation( "aggregationLevels", "abc" ) )
            .addMutation( new Mutation( "aggregationLevels", "def" ) );
        patchService.apply( patch, dataElement );
        assertTrue( dataElement.getAggregationLevels().isEmpty() );
    }

    @Test
    void testUpdateUser()
    {
        User user = createAndInjectAdminUser();
        assertEquals( "admin", user.getUsername() );
        Patch patch = new Patch().addMutation( new Mutation( "username", "dhis" ) );
        patchService.apply( patch, user );
        assertEquals( "dhis", user.getUsername() );
    }

    @Test
    void testSimpleDiff()
    {
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        Patch patch = patchService.diff( new PatchParams( deA, deB ) );
        patchService.apply( patch, deA );
        assertEquals( deA.getName(), deB.getName() );
        assertEquals( deA.getShortName(), deB.getShortName() );
        assertEquals( deA.getDescription(), deB.getDescription() );
    }

    @Test
    void testSimpleCollectionDiff()
    {
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        deA.getAggregationLevels().add( 1 );
        deB.getAggregationLevels().add( 2 );
        deB.getAggregationLevels().add( 3 );
        Patch patch = patchService.diff( new PatchParams( deA, deB ) );
        checkCount( patch, "aggregationLevels", Mutation.Operation.ADDITION, 2 );
        checkCount( patch, "aggregationLevels", Mutation.Operation.DELETION, 1 );
        patchService.apply( patch, deA );
        assertEquals( deA.getName(), deB.getName() );
        assertEquals( deA.getShortName(), deB.getShortName() );
        assertEquals( deA.getDescription(), deB.getDescription() );
        assertEquals( deA.getAggregationLevels(), deB.getAggregationLevels() );
    }

    @Test
    void testSimpleIdObjectCollectionDiff()
    {
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        DataElementGroup degA = createDataElementGroup( 'C' );
        DataElementGroup degB = createDataElementGroup( 'D' );
        manager.save( degA );
        manager.save( degB );
        deA.getGroups().add( degA );
        manager.update( degA );
        deB.getGroups().add( degB );
        deA.getAggregationLevels().add( 1 );
        deA.getAggregationLevels().add( 2 );
        deB.getAggregationLevels().add( 2 );
        deB.getAggregationLevels().add( 3 );
        deB.getAggregationLevels().add( 4 );
        Patch patch = patchService.diff( new PatchParams( deA, deB ) );
        checkCount( patch, "dataElementGroups", Mutation.Operation.ADDITION, 1 );
        checkCount( patch, "dataElementGroups", Mutation.Operation.DELETION, 1 );
        checkCount( patch, "aggregationLevels", Mutation.Operation.ADDITION, 2 );
        checkCount( patch, "aggregationLevels", Mutation.Operation.DELETION, 1 );
        patchService.apply( patch, deA );
        assertEquals( deA.getName(), deB.getName() );
        assertEquals( deA.getShortName(), deB.getShortName() );
        assertEquals( deA.getDescription(), deB.getDescription() );
        assertEquals( deA.getAggregationLevels(), deB.getAggregationLevels() );
        assertEquals( deA.getGroups(), deB.getGroups() );
    }

    @Test
    void testEmbeddedObjectEquality()
    {
        User adminUser = createAndInjectAdminUser();
        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( adminUser ) );
        manager.save( userGroup );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        deA.getSharing().addUserGroupAccess( new UserGroupAccess( userGroup, "rw------" ) );
        deA.getSharing().addUserAccess( new UserAccess( adminUser, "rw------" ) );
        deB.getSharing().addUserGroupAccess( new UserGroupAccess( userGroup, "rw------" ) );
        deB.getSharing().addUserAccess( new UserAccess( adminUser, "rw------" ) );
        Patch diff = patchService.diff( new PatchParams( deA, deB ) );
        assertTrue( diff.getMutations().stream().map( Mutation::getPath ).collect( toList() ).containsAll(
            List.of( "displayDescription", "code", "dimensionItem", "displayName", "name", "description", "id",
                "shortName", "displayFormName", "displayShortName" ) ) );
    }

    @Test
    void testEmbeddedObjectCollectionDiff()
    {
        User adminUser = createAndInjectAdminUser();
        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( adminUser ) );
        manager.save( userGroup );
        DataElement deA = createDataElement( 'A' );
        DataElement deB = createDataElement( 'B' );
        deA.getAggregationLevels().add( 1 );
        deB.getAggregationLevels().add( 1 );
        deB.getAggregationLevels().add( 2 );
        deB.getAggregationLevels().add( 3 );
        deB.getSharing().addUserGroupAccess( new UserGroupAccess( userGroup, "rw------" ) );
        deB.getSharing().addUserAccess( new UserAccess( adminUser, "rw------" ) );
        Patch patch = patchService.diff( new PatchParams( deA, deB ) );
        patchService.apply( patch, deA );
        assertEquals( deA.getName(), deB.getName() );
        assertEquals( deA.getShortName(), deB.getShortName() );
        assertEquals( deA.getDescription(), deB.getDescription() );
        assertEquals( deA.getAggregationLevels(), deB.getAggregationLevels() );
        assertEquals( deA.getUserGroupAccesses(), deB.getUserGroupAccesses() );
        assertEquals( deA.getUserAccesses(), deB.getUserAccesses() );
    }

    @Test
    void testPatchFromJsonNode1()
    {
        JsonNode jsonNode = loadJsonNodeFromFile( "patch/simple.json" );
        DataElement dataElement = createDataElement( 'A' );
        Patch patch = patchService.diff( new PatchParams( jsonNode ) );
        assertEquals( 2, patch.getMutations().size() );
        patchService.apply( patch, dataElement );
        assertEquals( dataElement.getName(), "Updated Name" );
        assertEquals( dataElement.getShortName(), "Updated Short Name" );
    }

    @Test
    void testPatchFromJsonNode2()
    {
        JsonNode jsonNode = loadJsonNodeFromFile( "patch/id-collection.json" );
        DataElement dataElement = createDataElement( 'A' );
        DataElementGroup degA = createDataElementGroup( 'C' );
        DataElementGroup degB = createDataElementGroup( 'D' );
        manager.save( degA );
        manager.save( degB );
        Patch patch = patchService.diff( new PatchParams( jsonNode ) );
        patchService.apply( patch, dataElement );
        assertEquals( dataElement.getName(), "Updated Name" );
        assertEquals( dataElement.getShortName(), "Updated Short Name" );
        assertEquals( 2, dataElement.getGroups().size() );
    }

    @Test
    void testPatchFromJsonNode3()
    {
        JsonNode jsonNode = loadJsonNodeFromFile( "patch/complex.json" );
        Patch diff = patchService.diff( new PatchParams( jsonNode ) );
        assertFalse( diff.getMutations().isEmpty() );
    }

    private JsonNode loadJsonNodeFromFile( String path )
    {
        try
        {
            InputStream inputStream = new ClassPathResource( path ).getInputStream();
            return jsonMapper.readTree( inputStream );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        return null;
    }

    private void checkCount( Patch patch, String name, Mutation.Operation operation, int expected )
    {
        int count = 0;
        for ( Mutation mutation : patch.getMutations() )
        {
            if ( mutation.getOperation() == operation && mutation.getPath().equals( name ) )
            {
                if ( Collection.class.isInstance( mutation.getValue() ) )
                {
                    count += ((Collection<?>) mutation.getValue()).size();
                }
                else
                {
                    count++;
                }
            }
        }
        assertEquals( expected, count,
            "Did not find " + expected + " mutations of type " + operation + " on property " + name );
    }
}
