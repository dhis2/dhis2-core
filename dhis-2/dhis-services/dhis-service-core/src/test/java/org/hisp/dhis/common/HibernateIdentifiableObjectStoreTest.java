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

import org.hisp.dhis.TransactionalIntegrationTestBase;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.user.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class HibernateIdentifiableObjectStoreTest
    extends TransactionalIntegrationTestBase
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
        user1.setUid( CodeGenerator.generateUid()  );

        User user2 = new User();
        user2.setUid( CodeGenerator.generateUid()  );

        User user3 = new User();
        user3.setUid( CodeGenerator.generateUid()  );

        User user4 = new User();
        user4.setUid( CodeGenerator.generateUid()  );

        UserGroup userGroup1 = new UserGroup();
        userGroup1.setUid( CodeGenerator.generateUid() );;

        UserGroup userGroup2 = new UserGroup(  );
        userGroup2.setUid( CodeGenerator.generateUid() );;

        user1.getGroups().add(userGroup1);
        user1.getGroups().add(userGroup2);
        user4.getGroups().add(userGroup2);

        Map<String, UserAccess> userSharing = new HashMap<>();
        userSharing.put( user1.getUid(), new UserAccess( user1, AccessStringHelper.DEFAULT ) );
        userSharing.put( user2.getUid(), new UserAccess( user2, AccessStringHelper.READ ) );
        userSharing.put( user3.getUid(), new UserAccess( user3, AccessStringHelper.DEFAULT ) );
        userSharing.put( user4.getUid(), new UserAccess( user4, AccessStringHelper.DEFAULT ) );

        Map<String, UserGroupAccess> userGroupSharing = new HashMap<>();
        userGroupSharing.put( userGroup1.getUid() , new UserGroupAccess( userGroup1, AccessStringHelper.READ_WRITE ) );
        userGroupSharing.put( userGroup2.getUid() , new UserGroupAccess( userGroup2, AccessStringHelper.DEFAULT ) );

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

        System.out.println("===================================");
        dataElementStore.getByUid( dataElementUid );
    }

//    @Test
    public void testDataRead()
    {
        User admin = createAndInjectAdminUser();
        User user1 = new User();
        UserCredentials userCredentials1 = new UserCredentials();
        userCredentials1.setUid( CodeGenerator.generateUid() );;
        user1.setUserCredentials( userCredentials1 );

        User user2 = new User();
        UserCredentials userCredentials2 = new UserCredentials();
        userCredentials2.setUid( CodeGenerator.generateUid() );;
        user2.setUserCredentials( userCredentials2 );

        User user3 = new User();
        UserCredentials userCredentials3 = new UserCredentials();
        userCredentials3.setUid( CodeGenerator.generateUid() );;
        user3.setUserCredentials( userCredentials3 );

        User user4 = new User();
        UserCredentials userCredentials4 = new UserCredentials();
        userCredentials3.setUid( CodeGenerator.generateUid() );;
        user4.setUserCredentials( userCredentials4 );

        UserGroup userGroup1 = new UserGroup();
        userGroup1.setUid( CodeGenerator.generateUid() );;

        UserGroup userGroup2 = new UserGroup(  );
        userGroup2.setUid( CodeGenerator.generateUid() );;

        user1.getGroups().add(userGroup1);
        user1.getGroups().add(userGroup2);
        user4.getGroups().add(userGroup2);

        Map<String, UserAccess> userSharing = new HashMap<>();
        userSharing.put( user1.getUid(), new UserAccess( user1, AccessStringHelper.DEFAULT ) );
        userSharing.put( user2.getUid(), new UserAccess( user2, AccessStringHelper.DATA_READ_WRITE ) );
        userSharing.put( user3.getUid(), new UserAccess( user3, AccessStringHelper.DEFAULT ) );
        userSharing.put( user4.getUid(), new UserAccess( user4, AccessStringHelper.DEFAULT ) );


        Map<String, UserGroupAccess> userGroupSharing = new HashMap<>();
        userGroupSharing.put( userGroup1.getUid() , new UserGroupAccess( userGroup1, AccessStringHelper.DATA_READ_WRITE ) );
        userGroupSharing.put( userGroup2.getUid() , new UserGroupAccess( userGroup2, AccessStringHelper.DEFAULT ) );

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
//        assertNotNull( dataElementStore.getDataElement( dataElement.getUid(), user1 ) );
        // User2 has access to DEA
//        assertNotNull( dataElementStore.getDataElement( dataElement.getUid(), user2 ) );
        // User3 doesn't have access and also does't belong to any groups
//        assertNull( dataElementStore.getDataElement( dataElement.getUid(), user3 ) );
        // User4 doesn't have access and it belong to UserGroup2 which also doesn't have access
//        assertNull( dataElementStore.getDataElement( dataElement.getUid(), user4 ) );


    }
}
