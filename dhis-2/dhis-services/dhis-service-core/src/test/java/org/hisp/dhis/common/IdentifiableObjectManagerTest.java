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
package org.hisp.dhis.common;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.hibernate.exception.CreateAccessDeniedException;
import org.hisp.dhis.hibernate.exception.DeleteAccessDeniedException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class IdentifiableObjectManagerTest extends TransactionalIntegrationTest
{

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private UserService _userService;

    @Override
    protected void setUpTest()
        throws Exception
    {
        this.userService = _userService;
    }

    @Test
    void testGetObjectWithIdScheme()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementService.addDataElement( dataElementA );
        assertEquals( dataElementA, idObjectManager.get( DataDimensionItem.DATA_DIMENSION_CLASSES,
            IdScheme.CODE, dataElementA.getCode() ) );
        assertEquals( dataElementA, idObjectManager.get( DataDimensionItem.DATA_DIMENSION_CLASSES,
            IdScheme.UID, dataElementA.getUid() ) );
    }

    @Test
    void testGetObject()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementService.addDataElement( dataElementA );
        long dataElementIdA = dataElementA.getId();
        dataElementService.addDataElement( dataElementB );
        long dataElementIdB = dataElementB.getId();
        DataElementGroup dataElementGroupA = createDataElementGroup( 'A' );
        DataElementGroup dataElementGroupB = createDataElementGroup( 'B' );
        dataElementService.addDataElementGroup( dataElementGroupA );
        long dataElementGroupIdA = dataElementGroupA.getId();
        dataElementService.addDataElementGroup( dataElementGroupB );
        long dataElementGroupIdB = dataElementGroupB.getId();
        assertEquals( dataElementA,
            idObjectManager.getObject( dataElementIdA, DataElement.class.getSimpleName() ) );
        assertEquals( dataElementB,
            idObjectManager.getObject( dataElementIdB, DataElement.class.getSimpleName() ) );
        assertEquals( dataElementGroupA,
            idObjectManager.getObject( dataElementGroupIdA, DataElementGroup.class.getSimpleName() ) );
        assertEquals( dataElementGroupB,
            idObjectManager.getObject( dataElementGroupIdB, DataElementGroup.class.getSimpleName() ) );
    }

    @Test
    void testLoad()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementService.addDataElement( dataElementA );

        assertEquals( dataElementA, idObjectManager.load( DataElement.class, dataElementA.getUid() ) );

        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> idObjectManager.load( DataElement.class, "nonExisting" ) );
        assertEquals( ErrorCode.E1113, ex.getErrorCode() );
    }

    @Test
    void testLoadByCode()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementService.addDataElement( dataElementA );

        assertEquals( dataElementA, idObjectManager.loadByCode( DataElement.class, "DataElementCodeA" ) );

        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> idObjectManager.load( DataElement.class, "nonExisting" ) );
        assertEquals( ErrorCode.E1113, ex.getErrorCode() );
    }

    @Test
    void testLoadWithErrorCode()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementService.addDataElement( dataElementA );

        assertEquals( dataElementA,
            idObjectManager.load( DataElement.class, ErrorCode.E1100, dataElementA.getUid() ) );

        IllegalQueryException exA = assertThrows( IllegalQueryException.class,
            () -> idObjectManager.load( DataElement.class, ErrorCode.E1100, "nonExisting" ) );
        assertEquals( ErrorCode.E1100, exA.getErrorCode() );

        IllegalQueryException exB = assertThrows( IllegalQueryException.class,
            () -> idObjectManager.load( OrganisationUnit.class, ErrorCode.E1102, "nonExisting" ) );
        assertEquals( ErrorCode.E1102, exB.getErrorCode() );
    }

    @Test
    void testGetWithClasses()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        Set<Class<? extends IdentifiableObject>> classes = ImmutableSet.<Class<? extends IdentifiableObject>> builder()
            .add( Indicator.class ).add( DataElement.class ).add( DataElementOperand.class ).build();
        assertEquals( dataElementA, idObjectManager.get( classes, dataElementA.getUid() ) );
        assertEquals( dataElementB, idObjectManager.get( classes, dataElementB.getUid() ) );
    }

    @Test
    void testGetByUidWithClassesAndUids()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        OrganisationUnit unitA = createOrganisationUnit( 'A' );
        OrganisationUnit unitB = createOrganisationUnit( 'B' );
        idObjectManager.save( unitA );
        idObjectManager.save( unitB );
        Set<Class<? extends IdentifiableObject>> classes = ImmutableSet.<Class<? extends IdentifiableObject>> builder()
            .add( DataElement.class ).add( OrganisationUnit.class ).build();
        Set<String> uids = ImmutableSet.of( dataElementA.getUid(), unitB.getUid() );
        assertEquals( 2, idObjectManager.getByUid( classes, uids ).size() );
        assertTrue( idObjectManager.getByUid( classes, uids ).contains( dataElementA ) );
        assertTrue( idObjectManager.getByUid( classes, uids ).contains( unitB ) );
        assertFalse( idObjectManager.getByUid( classes, uids ).contains( dataElementB ) );
        assertFalse( idObjectManager.getByUid( classes, uids ).contains( unitA ) );
    }

    @Test
    void publicAccessSetIfNoUser()
    {
        DataElement dataElement = createDataElement( 'A' );
        idObjectManager.save( dataElement );
        assertNotNull( dataElement.getPublicAccess() );
        assertFalse( AccessStringHelper.canRead( dataElement.getPublicAccess() ) );
        assertFalse( AccessStringHelper.canWrite( dataElement.getPublicAccess() ) );
    }

    @Test
    void getCount()
    {
        idObjectManager.save( createDataElement( 'A' ) );
        idObjectManager.save( createDataElement( 'B' ) );
        idObjectManager.save( createDataElement( 'C' ) );
        idObjectManager.save( createDataElement( 'D' ) );
        assertEquals( 4, idObjectManager.getCount( DataElement.class ) );
    }

    @Test
    void getEqualToName()
    {
        DataElement dataElement = createDataElement( 'A' );
        idObjectManager.save( dataElement );
        assertNotNull( idObjectManager.getByName( DataElement.class, "DataElementA" ) );
        assertNull( idObjectManager.getByName( DataElement.class, "DataElementB" ) );
        assertEquals( dataElement, idObjectManager.getByName( DataElement.class, "DataElementA" ) );
    }

    @Test
    void getAllOrderedName()
    {
        idObjectManager.save( createDataElement( 'D' ) );
        idObjectManager.save( createDataElement( 'B' ) );
        idObjectManager.save( createDataElement( 'C' ) );
        idObjectManager.save( createDataElement( 'A' ) );
        List<DataElement> dataElements = new ArrayList<>( idObjectManager.getAllSorted( DataElement.class ) );
        assertEquals( 4, dataElements.size() );
        assertEquals( "DataElementA", dataElements.get( 0 ).getName() );
        assertEquals( "DataElementB", dataElements.get( 1 ).getName() );
        assertEquals( "DataElementC", dataElements.get( 2 ).getName() );
        assertEquals( "DataElementD", dataElements.get( 3 ).getName() );
    }

    @Test
    void userIsCurrentIfNoUserSet()
    {
        User user = createUserAndInjectSecurityContext( true );
        DataElement dataElement = createDataElement( 'A' );
        idObjectManager.save( dataElement );
        assertNotNull( dataElement.getCreatedBy() );
        assertEquals( user, dataElement.getCreatedBy() );
    }

    @Test
    void userCanCreatePublic()
    {
        createUserAndInjectSecurityContext( false, "F_DATAELEMENT_PUBLIC_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        idObjectManager.save( dataElement );
        assertNotNull( dataElement.getPublicAccess() );
        assertTrue( AccessStringHelper.canRead( dataElement.getPublicAccess() ) );
        assertTrue( AccessStringHelper.canWrite( dataElement.getPublicAccess() ) );
    }

    @Test
    void userCanCreatePrivate()
    {
        createUserAndInjectSecurityContext( false, "F_DATAELEMENT_PRIVATE_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        idObjectManager.save( dataElement );
        assertNotNull( dataElement.getPublicAccess() );
        assertFalse( AccessStringHelper.canRead( dataElement.getPublicAccess() ) );
        assertFalse( AccessStringHelper.canWrite( dataElement.getPublicAccess() ) );
    }

    @Test
    void userDeniedCreateObject()
    {
        createUserAndInjectSecurityContext( false );
        assertThrows( CreateAccessDeniedException.class,
            () -> idObjectManager.save( createDataElement( 'A' ) ) );
    }

    @Test
    void publicUserModifiedPublicAccessDEFAULT()
    {
        createUserAndInjectSecurityContext( false, "F_DATAELEMENT_PUBLIC_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        idObjectManager.save( dataElement, false );
        assertFalse( AccessStringHelper.canRead( dataElement.getPublicAccess() ) );
        assertFalse( AccessStringHelper.canWrite( dataElement.getPublicAccess() ) );
    }

    @Test
    void publicUserModifiedPublicAccessRW()
    {
        createUserAndInjectSecurityContext( false, "F_DATAELEMENT_PUBLIC_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );
        idObjectManager.save( dataElement, false );
    }

    @Test
    void privateUserModifiedPublicAccessRW()
    {
        createUserAndInjectSecurityContext( false, "F_DATAELEMENT_PRIVATE_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertThrows( CreateAccessDeniedException.class, () -> idObjectManager.save( dataElement, false ) );
    }

    @Test
    void privateUserModifiedPublicAccessDEFAULT()
    {
        createUserAndInjectSecurityContext( false, "F_DATAELEMENT_PRIVATE_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        idObjectManager.save( dataElement, false );
    }

    @Test
    void updateForPrivateUserDeniedAfterChangePublicAccessRW()
    {
        createUserAndInjectSecurityContext( false, "F_DATAELEMENT_PRIVATE_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        idObjectManager.save( dataElement, false );
        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertThrows( UpdateAccessDeniedException.class, () -> idObjectManager.update( dataElement ) );
    }

    @Test
    void userDeniedForPublicAdd()
    {
        createUserAndInjectSecurityContext( false );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertThrows( CreateAccessDeniedException.class, () -> idObjectManager.save( dataElement, false ) );
    }

    @Test
    void userDeniedDeleteObject()
    {
        createUserAndInjectSecurityContext( false, "F_DATAELEMENT_PUBLIC_ADD", "F_USER_ADD" );
        User user = makeUser( "B" );
        idObjectManager.save( user );
        DataElement dataElement = createDataElement( 'A' );
        idObjectManager.save( dataElement );
        dataElement.setOwner( user.getUid() );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        sessionFactory.getCurrentSession().update( dataElement );
        assertThrows( DeleteAccessDeniedException.class, () -> idObjectManager.delete( dataElement ) );
    }

    @Test
    void objectsWithNoUser()
    {
        idObjectManager.save( createDataElement( 'A' ) );
        idObjectManager.save( createDataElement( 'B' ) );
        idObjectManager.save( createDataElement( 'C' ) );
        idObjectManager.save( createDataElement( 'D' ) );
        assertEquals( 4, idObjectManager.getCount( DataElement.class ) );
        assertEquals( 4, idObjectManager.getAll( DataElement.class ).size() );
    }

    @Test
    void readPrivateObjects()
    {
        createUserAndInjectSecurityContext( false, "F_DATAELEMENT_PUBLIC_ADD", "F_USER_ADD" );
        User user = makeUser( "B" );
        idObjectManager.save( user );
        idObjectManager.save( createDataElement( 'A' ) );
        idObjectManager.save( createDataElement( 'B' ) );
        idObjectManager.save( createDataElement( 'C' ) );
        idObjectManager.save( createDataElement( 'D' ) );
        assertEquals( 4, idObjectManager.getAll( DataElement.class ).size() );
        List<DataElement> dataElements = new ArrayList<>( idObjectManager.getAll( DataElement.class ) );
        for ( DataElement dataElement : dataElements )
        {
            dataElement.setOwner( user.getUid() );
            dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
            sessionFactory.getCurrentSession().update( dataElement );
        }
        assertEquals( 0, idObjectManager.getCount( DataElement.class ) );
        assertEquals( 0, idObjectManager.getAll( DataElement.class ).size() );
    }

    @Test
    void readUserGroupSharedObjects()
    {
        User loginUser = createUserAndInjectSecurityContext( false, "F_DATAELEMENT_PUBLIC_ADD", "F_USER_ADD",
            "F_USERGROUP_PUBLIC_ADD" );
        User user = makeUser( "B" );
        idObjectManager.save( user );
        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( loginUser ) );
        idObjectManager.save( userGroup );
        user.getGroups().add( userGroup );
        loginUser.getGroups().add( userGroup );
        idObjectManager.save( loginUser );
        idObjectManager.save( user );
        idObjectManager.save( createDataElement( 'A' ) );
        idObjectManager.save( createDataElement( 'B' ) );
        idObjectManager.save( createDataElement( 'C' ) );
        idObjectManager.save( createDataElement( 'D' ) );
        assertEquals( 4, idObjectManager.getCount( DataElement.class ) );
        assertEquals( 4, idObjectManager.getAll( DataElement.class ).size() );
        List<DataElement> dataElements = new ArrayList<>( idObjectManager.getAll( DataElement.class ) );
        for ( DataElement dataElement : dataElements )
        {
            dataElement.getSharing().setOwner( user );
            dataElement.getSharing().setPublicAccess( AccessStringHelper.newInstance().build() );
            dataElement.getSharing().addUserGroupAccess( new UserGroupAccess( userGroup, AccessStringHelper.READ ) );
            sessionFactory.getCurrentSession().update( dataElement );
        }
        idObjectManager.flush();
        assertEquals( 4, idObjectManager.getCount( DataElement.class ) );
        assertEquals( 4, idObjectManager.getAll( DataElement.class ).size() );
    }

    @Test
    void getByUidTest()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );
        idObjectManager.save( dataElementA );
        idObjectManager.save( dataElementB );
        idObjectManager.save( dataElementC );
        idObjectManager.save( dataElementD );
        List<DataElement> ab = idObjectManager.getByUid( DataElement.class,
            Arrays.asList( dataElementA.getUid(), dataElementB.getUid() ) );
        List<DataElement> cd = idObjectManager.getByUid( DataElement.class,
            Arrays.asList( dataElementC.getUid(), dataElementD.getUid() ) );
        assertTrue( ab.contains( dataElementA ) );
        assertTrue( ab.contains( dataElementB ) );
        assertFalse( ab.contains( dataElementC ) );
        assertFalse( ab.contains( dataElementD ) );
        assertFalse( cd.contains( dataElementA ) );
        assertFalse( cd.contains( dataElementB ) );
        assertTrue( cd.contains( dataElementC ) );
        assertTrue( cd.contains( dataElementD ) );
    }

    @Test
    void getAndValidateByUidTest()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        idObjectManager.save( dataElementA );
        idObjectManager.save( dataElementB );
        idObjectManager.save( dataElementC );
        List<DataElement> ab = idObjectManager.getAndValidateByUid( DataElement.class,
            Arrays.asList( dataElementA.getUid(), dataElementB.getUid() ) );
        assertTrue( ab.contains( dataElementA ) );
        assertTrue( ab.contains( dataElementB ) );
        assertFalse( ab.contains( dataElementC ) );
    }

    @Test
    void getAndValidateByUidExceptionTest()
    {
        DataElement dataElementA = createDataElement( 'A' );
        idObjectManager.save( dataElementA );
        IllegalQueryException ex = assertThrows( IllegalQueryException.class, () -> idObjectManager
            .getAndValidateByUid( DataElement.class, Arrays.asList( dataElementA.getUid(), "xhjG82jHaky" ) ) );
        assertEquals( ErrorCode.E1112, ex.getErrorCode() );
    }

    @Test
    void getOrderedUidIdSchemeTest()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );
        idObjectManager.save( dataElementA );
        idObjectManager.save( dataElementB );
        idObjectManager.save( dataElementC );
        idObjectManager.save( dataElementD );
        List<String> uids = Arrays.asList( dataElementA.getUid(), dataElementC.getUid(), dataElementB.getUid(),
            dataElementD.getUid() );
        List<DataElement> expected = new ArrayList<>(
            Arrays.asList( dataElementA, dataElementC, dataElementB, dataElementD ) );
        List<DataElement> actual = new ArrayList<>(
            idObjectManager.getOrdered( DataElement.class, IdScheme.UID, uids ) );
        assertEquals( expected, actual );
    }

    @Test
    void getOrderedCodeIdSchemeTest()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );
        idObjectManager.save( dataElementA );
        idObjectManager.save( dataElementB );
        idObjectManager.save( dataElementC );
        idObjectManager.save( dataElementD );
        List<String> codes = Arrays.asList( dataElementA.getCode(), dataElementC.getCode(), dataElementB.getCode(),
            dataElementD.getCode() );
        List<DataElement> expected = new ArrayList<>(
            Arrays.asList( dataElementA, dataElementC, dataElementB, dataElementD ) );
        List<DataElement> actual = new ArrayList<>(
            idObjectManager.getOrdered( DataElement.class, IdScheme.CODE, codes ) );
        assertEquals( expected, actual );
    }

    @Test
    void getByUidOrderedTest()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );
        idObjectManager.save( dataElementA );
        idObjectManager.save( dataElementB );
        idObjectManager.save( dataElementC );
        idObjectManager.save( dataElementD );
        List<String> uids = Arrays.asList( dataElementA.getUid(), dataElementC.getUid(), dataElementB.getUid(),
            dataElementD.getUid() );
        List<DataElement> expected = new ArrayList<>(
            Arrays.asList( dataElementA, dataElementC, dataElementB, dataElementD ) );
        List<DataElement> actual = new ArrayList<>(
            idObjectManager.getByUidOrdered( DataElement.class, uids ) );
        assertEquals( expected, actual );
    }

    @Test
    void testGetByCode()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        DataElement dataElementD = createDataElement( 'D' );
        dataElementA.setCode( "DE_A" );
        dataElementB.setCode( "DE_B" );
        dataElementC.setCode( "DE_C" );
        dataElementD.setCode( "DE_D" );
        idObjectManager.save( dataElementA );
        idObjectManager.save( dataElementB );
        idObjectManager.save( dataElementC );
        idObjectManager.save( dataElementD );
        List<DataElement> ab = idObjectManager.getByCode( DataElement.class,
            Arrays.asList( dataElementA.getCode(), dataElementB.getCode() ) );
        List<DataElement> cd = idObjectManager.getByCode( DataElement.class,
            Arrays.asList( dataElementC.getCode(), dataElementD.getCode() ) );
        assertTrue( ab.contains( dataElementA ) );
        assertTrue( ab.contains( dataElementB ) );
        assertFalse( ab.contains( dataElementC ) );
        assertFalse( ab.contains( dataElementD ) );
        assertFalse( cd.contains( dataElementA ) );
        assertFalse( cd.contains( dataElementB ) );
        assertTrue( cd.contains( dataElementC ) );
        assertTrue( cd.contains( dataElementD ) );
    }

    @Test
    void getByUidNoAcl()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        DataElement dataElementC = createDataElement( 'C' );
        dataElementA.setCode( "DE_A" );
        dataElementB.setCode( "DE_B" );
        dataElementC.setCode( "DE_C" );
        OrganisationUnit unit1 = createOrganisationUnit( 'A' );
        idObjectManager.save( unit1 );
        idObjectManager.save( dataElementA );
        idObjectManager.save( dataElementB );
        idObjectManager.save( dataElementC );
        List<String> uids = Lists.newArrayList( dataElementA.getUid(), dataElementB.getUid(), dataElementC.getUid() );
        List<DataElement> dataElements = idObjectManager.getNoAcl( DataElement.class, uids );
        assertEquals( 3, dataElements.size() );
        assertTrue( dataElements.contains( dataElementA ) );
        assertTrue( dataElements.contains( dataElementB ) );
        assertTrue( dataElements.contains( dataElementC ) );
    }

    @Test
    void testGetObjects()
    {
        OrganisationUnit unit1 = createOrganisationUnit( 'A' );
        OrganisationUnit unit2 = createOrganisationUnit( 'B' );
        OrganisationUnit unit3 = createOrganisationUnit( 'C' );
        idObjectManager.save( unit1 );
        idObjectManager.save( unit2 );
        idObjectManager.save( unit3 );
        Set<String> codes = Sets.newHashSet( unit2.getCode(), unit3.getCode() );
        List<OrganisationUnit> units = idObjectManager.getObjects( OrganisationUnit.class,
            IdentifiableProperty.CODE, codes );
        assertEquals( 2, units.size() );
        assertTrue( units.contains( unit2 ) );
        assertTrue( units.contains( unit3 ) );
    }

    @Test
    void testGetIdMapIdScheme()
    {
        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        Map<String, DataElement> map = idObjectManager.getIdMap( DataElement.class, IdScheme.CODE );
        assertEquals( dataElementA, map.get( "DataElementCodeA" ) );
        assertEquals( dataElementB, map.get( "DataElementCodeB" ) );
        assertNull( map.get( "DataElementCodeX" ) );
    }

    @Test
    void testRemoveUserGroupFromSharing()
    {
        User userA = makeUser( "A" );
        userService.addUser( userA );
        UserGroup userGroupA = createUserGroup( 'A', Sets.newHashSet( userA ) );
        idObjectManager.save( userGroupA );
        String userGroupUid = userGroupA.getUid();
        DataElement de = createDataElement( 'A' );
        Sharing sharing = new Sharing();
        sharing.setUserGroupAccess( singleton( new UserGroupAccess( "rw------", userGroupA.getUid() ) ) );
        de.setSharing( sharing );
        idObjectManager.save( de, false );
        de = idObjectManager.get( de.getUid() );
        assertEquals( 1, de.getSharing().getUserGroups().size() );
        idObjectManager.delete( userGroupA );
        idObjectManager.removeUserGroupFromSharing( userGroupUid );
        dbmsManager.clearSession();
        de = idObjectManager.get( de.getUid() );
        assertEquals( 0, de.getSharing().getUserGroups().size() );
    }
}
