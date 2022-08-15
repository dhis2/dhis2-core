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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

class HibernateIdentifiableObjectStoreTest extends TransactionalIntegrationTest
{
    @Autowired
    private DataElementStore dataElementStore;

    @Autowired
    private DataValueStore dataValueStore;

    @Autowired
    private AggregateAccessManager accessManager;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    @BeforeEach
    void init()
    {
        userService = _userService;
    }

    /**
     * Test Metadata Read access User and UserGroups mapping User1 | User2 |
     * User3 | User 4 Group1 x | | | Group2 X | | | X
     *
     * DataElementA access defined for Users and UserGroups User1 | User2 |
     * User3 | UserGroup1 | UserGroup2 Can access DEA | X | | X |
     */
    @Test
    void testMetadataRead()
    {
        User admin = createAndInjectAdminUser();
        User user1 = createAndAddUser( "A" );
        User user2 = createAndAddUser( "B" );
        User user3 = createAndAddUser( "C" );
        User user4 = createAndAddUser( "D" );
        manager.save( user1 );
        manager.save( user2 );
        manager.save( user3 );
        manager.save( user4 );

        UserGroup userGroup1 = createUserGroup( 'A', Set.of( user1 ) );
        UserGroup userGroup2 = createUserGroup( 'B', Set.of( user1, user4 ) );
        manager.save( userGroup1 );
        manager.save( userGroup2 );

        // Create sharing settings
        Map<String, UserAccess> userSharing = new HashMap<>();
        userSharing.put( user1.getUid(), new UserAccess( user1, AccessStringHelper.DEFAULT ) );
        userSharing.put( user2.getUid(), new UserAccess( user2, AccessStringHelper.READ ) );
        userSharing.put( user3.getUid(), new UserAccess( user3, AccessStringHelper.DEFAULT ) );
        userSharing.put( user4.getUid(), new UserAccess( user4, AccessStringHelper.DEFAULT ) );
        Map<String, UserGroupAccess> userGroupSharing = new HashMap<>();
        userGroupSharing.put( userGroup1.getUid(), new UserGroupAccess( userGroup1, AccessStringHelper.READ_WRITE ) );
        userGroupSharing.put( userGroup2.getUid(), new UserGroupAccess( userGroup2, AccessStringHelper.DEFAULT ) );

        // Create DataElement with given sharing settings
        DataElement dataElement = createDataElement( 'A' );
        String dataElementUid = "deabcdefghA";
        dataElement.setUid( dataElementUid );
        dataElement.setCreatedBy( admin );
        dataElement.setSharing( Sharing.builder().external( false ).publicAccess( AccessStringHelper.DEFAULT )
            .owner( "testOwner" ).userGroups( userGroupSharing ).users( userSharing ).build() );
        dataElementStore.save( dataElement, false );

        dataElement = dataElementStore.getByUidNoAcl( dataElementUid );
        assertNotNull( dataElement.getSharing() );
        assertEquals( 2, dataElement.getSharing().getUserGroups().size() );
        assertEquals( 4, dataElement.getSharing().getUsers().size() );

        // User1 can't access but it belong to UserGroup1 which has access
        assertNotNull( dataElementStore.getDataElement( dataElement.getUid(), user1 ) );
        // User2 has access to DEA
        assertNotNull( dataElementStore.getDataElement( dataElement.getUid(), user2 ) );
        // User3 doesn't have access and also does't belong to any groups
        assertNull( dataElementStore.getDataElement( dataElement.getUid(), user3 ) );
        // User4 doesn't have access and it belong to UserGroup2 which also
        // doesn't have access
        assertNull( dataElementStore.getDataElement( dataElement.getUid(), user4 ) );
    }

    @Test
    void testDataRead()
    {
        User user1 = createUserWithAuth( "user1", "DATA_READ" );
        User user2 = createUserWithAuth( "user2", "DATA_READ" );
        User user3 = createUserWithAuth( "user3", "DATA_READ" );
        User user4 = createUserWithAuth( "user4", "DATA_READ" );
        UserGroup userGroup1 = createUserGroup( 'A', Sets.newHashSet( user1 ) );
        manager.save( userGroup1 );
        UserGroup userGroup2 = createUserGroup( 'B', Sets.newHashSet( user1, user4 ) );
        manager.save( userGroup2 );
        user1.getGroups().add( userGroup1 );
        user1.getGroups().add( userGroup2 );
        user4.getGroups().add( userGroup2 );
        Map<String, UserAccess> userSharing = new HashMap<>();
        userSharing.put( user1.getUid(), new UserAccess( user1, AccessStringHelper.DEFAULT ) );
        userSharing.put( user2.getUid(), new UserAccess( user2, AccessStringHelper.DATA_READ ) );
        userSharing.put( user3.getUid(), new UserAccess( user3, AccessStringHelper.DEFAULT ) );
        userSharing.put( user4.getUid(), new UserAccess( user4, AccessStringHelper.DEFAULT ) );
        Map<String, UserGroupAccess> userGroupSharing = new HashMap<>();
        userGroupSharing.put( userGroup1.getUid(),
            new UserGroupAccess( userGroup1, AccessStringHelper.DATA_READ_WRITE ) );
        userGroupSharing.put( userGroup2.getUid(), new UserGroupAccess( userGroup2, AccessStringHelper.DEFAULT ) );
        Sharing sharing = Sharing.builder().external( false ).publicAccess( AccessStringHelper.DEFAULT )
            .owner( "testOwner" ).userGroups( userGroupSharing ).users( userSharing ).build();
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setValueType( ValueType.TEXT );
        CategoryOptionCombo defaultCategoryOptionCombo = createCategoryOptionCombo( 'D' );
        OrganisationUnit organisationUnitA = createOrganisationUnit( 'A' );
        Period period = createPeriod( new Date(), new Date() );
        period.setPeriodType( PeriodType.getPeriodTypeByName( MonthlyPeriodType.NAME ) );
        manager.save( dataElement );
        manager.save( organisationUnitA );
        manager.save( period );
        manager.save( defaultCategoryOptionCombo );
        CategoryOption categoryOption = createCategoryOption( 'A' );
        categoryOption.setSharing( sharing );
        categoryOption.setCategoryOptionCombos( Sets.newHashSet( defaultCategoryOptionCombo ) );
        manager.save( categoryOption, false );
        defaultCategoryOptionCombo.getCategoryOptions().add( categoryOption );
        DataValue dataValue = createDataValue( dataElement, period, organisationUnitA, "test",
            defaultCategoryOptionCombo );
        dataValueStore.addDataValue( dataValue );
        // User1 can't access but it belongs to UserGroup1 which has access
        assertEquals( 0, accessManager.canRead( user1, dataValue ).size() );
        // User2 has access to DEA
        assertEquals( 0, accessManager.canRead( user2, dataValue ).size() );
        // User3 doesn't have access and also doesn't belong to any groups
        assertEquals( 1, accessManager.canRead( user3, dataValue ).size() );
        // User4 doesn't have access and it belong to UserGroup2 which also
        // doesn't have access
        assertEquals( 1, accessManager.canRead( user4, dataValue ).size() );
    }
}
