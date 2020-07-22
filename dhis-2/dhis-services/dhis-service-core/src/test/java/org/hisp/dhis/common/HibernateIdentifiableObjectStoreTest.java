package org.hisp.dhis.common;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.sharing.ObjectSharing;
import org.hisp.dhis.sharing.UserGroupSharing;
import org.hisp.dhis.sharing.UserSharing;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
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

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Test
    public void testSaveDEWithObjectSharings()
    {
        User user1 = new User();
        UserCredentials userCredentials1 = new UserCredentials();
        userCredentials1.setUuid( UUID.randomUUID() );
        user1.setUserCredentials( userCredentials1 );

        User user2 = new User();
        UserCredentials userCredentials2 = new UserCredentials();
        userCredentials1.setUuid( UUID.randomUUID() );
        user2.setUserCredentials( userCredentials2 );

        UserGroup userGroup1 = new UserGroup();
        userGroup1.setUuid( UUID.randomUUID() );

        UserGroup userGroup2 = new UserGroup(  );
        userGroup2.setUuid( UUID.randomUUID() );

        Map<String, UserSharing> userSharing = new HashMap<>();
        userSharing.put( user1.getUserCredentials().getUuid().toString(),
            UserSharing.builder().userUuid( user1.getUserCredentials().getUuid().toString() ).access( "rw------" ).build() );

        userSharing.put( user2.getUserCredentials().getUuid().toString(),
            UserSharing.builder().userUuid( user2.getUserCredentials().getUuid().toString() ).access( "--------" ).build() );

        Map<String, UserGroupSharing> userGroupSharing = new HashMap<>();
        userGroupSharing.put( userGroup1.getUuid().toString(),
            UserGroupSharing.builder().userGroupUuid( userGroup1.getUuid().toString() ).access( "rw------" ).build() );
        userGroupSharing.put( userGroup2.getUuid().toString(),
            UserGroupSharing.builder().userGroupUuid( userGroup2.getUuid().toString() ).access( "--------" ).build() );

        DataElement de = createDataElement( 'A' );
        ObjectSharing objectSharing = ObjectSharing.builder()
            .external( true )
            .publicAccess( "rw------" )
            .owner( "testOwner" )
            .userGroups( userGroupSharing )
            .users( userSharing ).build();

        de.setObjectSharing( objectSharing );

        dataElementStore.save( de );

        DataElement dataElement = dataElementStore.get( de.getId() );

        assertNotNull( de.getObjectSharing() );
        assertEquals( 2, dataElement.getObjectSharing().getUserGroups().size() );
        assertEquals( 2, dataElement.getObjectSharing().getUsers().size() );

        assertNotNull( dataElementStore.getDataElement( dataElement.getUid(), user1 ) );
        assertNull( dataElementStore.getDataElement( dataElement.getUid(), user2 ) );
    }

}
