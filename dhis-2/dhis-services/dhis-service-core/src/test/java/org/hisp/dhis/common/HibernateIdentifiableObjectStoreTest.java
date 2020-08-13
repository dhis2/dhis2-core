package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.sharing.Sharing;
import org.hisp.dhis.sharing.UserGroupSharing;
import org.hisp.dhis.sharing.UserSharing;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class HibernateIdentifiableObjectStoreTest
    extends IntegrationTestBase
{
    @Autowired
    private DataElementStore dataElementStore;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private UserService _userService;

    @Before
    public void init()
    {
        userService = _userService;
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    /**
     * Test Metadata Read access
     * User and UserGroups mapping
     *          User1 | User2 | User3 | User 4
     * Group1     x   |       |       |
     * Group2     X   |       |       |  X
     *
     * DataElementA access defined for Users and UserGroups
     *                  User1 | User2 | User3 | UserGroup1 | UserGroup2
     * Can access DEA         |  X    |       |    X       |
     */
    @Test
    public void testSaveDEWithObjectSharing()
    {
        User admin = createAndInjectAdminUser();
        User user1 = new User();
        UserCredentials userCredentials1 = new UserCredentials();
        userCredentials1.setUuid( UUID.randomUUID() );
        user1.setUserCredentials( userCredentials1 );

        User user2 = new User();
        UserCredentials userCredentials2 = new UserCredentials();
        userCredentials2.setUuid( UUID.randomUUID() );
        user2.setUserCredentials( userCredentials2 );

        User user3 = new User();
        UserCredentials userCredentials3 = new UserCredentials();
        userCredentials3.setUuid( UUID.randomUUID() );
        user3.setUserCredentials( userCredentials3 );

        User user4 = new User();
        UserCredentials userCredentials4 = new UserCredentials();
        userCredentials3.setUuid( UUID.randomUUID() );
        user4.setUserCredentials( userCredentials4 );

        UserGroup userGroup1 = new UserGroup();
        userGroup1.setUuid( UUID.randomUUID() );

        UserGroup userGroup2 = new UserGroup(  );
        userGroup2.setUuid( UUID.randomUUID() );

        user1.getGroups().add(userGroup1);
        user1.getGroups().add(userGroup2);
        user4.getGroups().add(userGroup2);

        Map<String, UserSharing> userSharing = new HashMap<>();
        userSharing.put( user1.getUserCredentials().getUuid().toString(),
            UserSharing.builder().userUuid( user1.getUserCredentials().getUuid().toString() ).access( AccessStringHelper.DEFAULT ).build() );

        userSharing.put( user2.getUserCredentials().getUuid().toString(),
            UserSharing.builder().userUuid( user2.getUserCredentials().getUuid().toString() ).access( AccessStringHelper.READ ).build() );

        userSharing.put( user3.getUserCredentials().getUuid().toString(),
                UserSharing.builder().userUuid( user3.getUserCredentials().getUuid().toString() ).access( AccessStringHelper.DEFAULT ).build() );

        userSharing.put( user4.getUserCredentials().getUuid().toString(),
                UserSharing.builder().userUuid( user4.getUserCredentials().getUuid().toString() ).access( AccessStringHelper.DEFAULT ).build() );

        Map<String, UserGroupSharing> userGroupSharing = new HashMap<>();
        userGroupSharing.put( userGroup1.getUuid().toString(),
            UserGroupSharing.builder().userGroupUuid( userGroup1.getUuid().toString() ).access( AccessStringHelper.READ_WRITE ).build() );
        userGroupSharing.put( userGroup2.getUuid().toString(),
            UserGroupSharing.builder().userGroupUuid( userGroup2.getUuid().toString() ).access( AccessStringHelper.DEFAULT ).build() );

        DataElement dataElement = createDataElement( 'A' );
        String dataElementUid = "deabcdefghA";
        dataElement.setUid(dataElementUid);
        dataElement.setUser(admin);

        Sharing sharing = Sharing.builder()
            .external( true )
            .publicAccess( AccessStringHelper.DEFAULT )
            .owner( "testOwner" )
            .userGroups( userGroupSharing )
            .users( userSharing ).build();

        dataElement.setSharing( sharing );
        dataElement.setPublicAccess(AccessStringHelper.DEFAULT);
        dataElement.setExternalAccess(false);
        dataElementService.addDataElement( dataElement );

        dataElement.setPublicAccess(AccessStringHelper.DEFAULT);
        dataElementService.updateDataElement(dataElement);
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
        // User4 doesn't have access and it belong to UserGroup2 which also doesn't have access
        assertNull( dataElementStore.getDataElement( dataElement.getUid(), user4 ) );
    }

    @Test
    public void testDataRead()
    {
        User admin = createAndInjectAdminUser();
        User user1 = new User();
        UserCredentials userCredentials1 = new UserCredentials();
        userCredentials1.setUuid( UUID.randomUUID() );
        user1.setUserCredentials( userCredentials1 );

        User user2 = new User();
        UserCredentials userCredentials2 = new UserCredentials();
        userCredentials2.setUuid( UUID.randomUUID() );
        user2.setUserCredentials( userCredentials2 );

        User user3 = new User();
        UserCredentials userCredentials3 = new UserCredentials();
        userCredentials3.setUuid( UUID.randomUUID() );
        user3.setUserCredentials( userCredentials3 );

        User user4 = new User();
        UserCredentials userCredentials4 = new UserCredentials();
        userCredentials3.setUuid( UUID.randomUUID() );
        user4.setUserCredentials( userCredentials4 );

        UserGroup userGroup1 = new UserGroup();
        userGroup1.setUuid( UUID.randomUUID() );

        UserGroup userGroup2 = new UserGroup(  );
        userGroup2.setUuid( UUID.randomUUID() );

        user1.getGroups().add(userGroup1);
        user1.getGroups().add(userGroup2);
        user4.getGroups().add(userGroup2);

        Map<String, UserSharing> userSharing = new HashMap<>();
        userSharing.put( user1.getUserCredentials().getUuid().toString(),
            UserSharing.builder().userUuid( user1.getUserCredentials().getUuid().toString() ).access( AccessStringHelper.DEFAULT ).build() );

        userSharing.put( user2.getUserCredentials().getUuid().toString(),
            UserSharing.builder().userUuid( user2.getUserCredentials().getUuid().toString() ).access( AccessStringHelper.DATA_READ_WRITE ).build() );

        userSharing.put( user3.getUserCredentials().getUuid().toString(),
            UserSharing.builder().userUuid( user3.getUserCredentials().getUuid().toString() ).access( AccessStringHelper.DEFAULT ).build() );

        userSharing.put( user4.getUserCredentials().getUuid().toString(),
            UserSharing.builder().userUuid( user4.getUserCredentials().getUuid().toString() ).access( AccessStringHelper.DEFAULT ).build() );

        Map<String, UserGroupSharing> userGroupSharing = new HashMap<>();
        userGroupSharing.put( userGroup1.getUuid().toString(),
            UserGroupSharing.builder().userGroupUuid( userGroup1.getUuid().toString() ).access( AccessStringHelper.DATA_READ_WRITE ).build() );
        userGroupSharing.put( userGroup2.getUuid().toString(),
            UserGroupSharing.builder().userGroupUuid( userGroup2.getUuid().toString() ).access( AccessStringHelper.DEFAULT ).build() );

        DataElement dataElement = createDataElement( 'A' );
        String dataElementUid = "deabcdefghA";
        dataElement.setUid(dataElementUid);
        dataElement.setUser(admin);

        Sharing sharing = Sharing.builder()
            .external( true )
            .publicAccess( AccessStringHelper.DEFAULT )
            .owner( "testOwner" )
            .userGroups( userGroupSharing )
            .users( userSharing ).build();

        dataElement.setSharing( sharing );
        dataElement.setPublicAccess(AccessStringHelper.DEFAULT);
        dataElement.setExternalAccess(false);
        dataElementService.addDataElement( dataElement );

        dataElement.setPublicAccess(AccessStringHelper.DEFAULT);
        dataElementService.updateDataElement(dataElement);
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
        // User4 doesn't have access and it belong to UserGroup2 which also doesn't have access
        assertNull( dataElementStore.getDataElement( dataElement.getUid(), user4 ) );
    }
}
