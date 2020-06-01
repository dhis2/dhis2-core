package org.hisp.dhis.user;

import com.google.common.collect.Sets;
import org.hibernate.SessionFactory;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.jgroups.util.Util.assertEquals;

public class UserDeletionHandlerTest extends IntegrationTestBase
{
    @Autowired
    private UserService userService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void setUpTest()
    {
        super.userService = userService;
    }

    @Test
    public void testDeleteOrganisationUnitCleanUpOUScope()
    {
        OrganisationUnit OUa = createOrganisationUnit( "A" );
        organisationUnitService.addOrganisationUnit( OUa );

        User userA = createUser( "A" );
        userService.addUser( userA );

        userA.setTeiSearchOrganisationUnits( Sets.newHashSet( OUa ) );
        userA.setDataViewOrganisationUnits( Sets.newHashSet( OUa ) );
        userA.setOrganisationUnits( Sets.newHashSet( OUa ) );

        userService.updateUser( userA );

        assertEquals( 1, userService.getUser( userA.getUid() ).getTeiSearchOrganisationUnits().size() );

        organisationUnitService.deleteOrganisationUnit( OUa );

        sessionFactory.getCurrentSession().clear();

        assertEquals( 0, userService.getUser( userA.getUid() ).getTeiSearchOrganisationUnits().size() );
    }
}
