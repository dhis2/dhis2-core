package org.hisp.dhis.common;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dbms.DbmsUtils;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.sharing.ObjectSharing;
import org.hisp.dhis.sharing.UserGroupSharing;
import org.hisp.dhis.sharing.UserSharing;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    @Test
    public void testSaveDEWithObjectSharings()
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
            UserSharing.builder().userUuid( user1.getUserCredentials().getUuid().toString() ).access( "--------" ).build() );

        userSharing.put( user2.getUserCredentials().getUuid().toString(),
            UserSharing.builder().userUuid( user2.getUserCredentials().getUuid().toString() ).access( "rw------" ).build() );

        userSharing.put( user3.getUserCredentials().getUuid().toString(),
                UserSharing.builder().userUuid( user3.getUserCredentials().getUuid().toString() ).access( "--------" ).build() );

        userSharing.put( user4.getUserCredentials().getUuid().toString(),
                UserSharing.builder().userUuid( user4.getUserCredentials().getUuid().toString() ).access( "--------" ).build() );

        Map<String, UserGroupSharing> userGroupSharing = new HashMap<>();
        userGroupSharing.put( userGroup1.getUuid().toString(),
            UserGroupSharing.builder().userGroupUuid( userGroup1.getUuid().toString() ).access( "rw------" ).build() );
        userGroupSharing.put( userGroup2.getUuid().toString(),
            UserGroupSharing.builder().userGroupUuid( userGroup2.getUuid().toString() ).access( "--------" ).build() );

        DataElement dataElement = createDataElement( 'A' );
        String dataElementUid = "deabcdefghA";
        dataElement.setUid(dataElementUid);
        dataElement.setUser(admin);

        ObjectSharing objectSharing = ObjectSharing.builder()
            .external( true )
            .publicAccess( "--------" )
            .owner( "testOwner" )
            .userGroups( userGroupSharing )
            .users( userSharing ).build();

        dataElement.setObjectSharing( objectSharing );
        dataElement.setPublicAccess(AccessStringHelper.DEFAULT);
        dataElement.setExternalAccess(false);
        dataElementService.addDataElement( dataElement );

        dataElement.setPublicAccess(AccessStringHelper.DEFAULT);
        dataElementService.updateDataElement(dataElement);
        dataElement = dataElementStore.getByUidNoAcl(dataElementUid);

        assertNotNull( dataElement.getObjectSharing() );
        assertEquals( 2, dataElement.getObjectSharing().getUserGroups().size() );
        assertEquals( 4, dataElement.getObjectSharing().getUsers().size() );

        assertNotNull( dataElementStore.getDataElement( dataElement.getUid(), user1 ) );
        assertNotNull( dataElementStore.getDataElement( dataElement.getUid(), user2 ) );
        assertNull( dataElementStore.getDataElement( dataElement.getUid(), user3 ) );
        assertNull( dataElementStore.getDataElement( dataElement.getUid(), user4 ) );
    }

}
