package org.hisp.dhis.tracker;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.hibernate.HibernateUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Specialized User Service for Tracker that executes User look-up in a read-only transaction
 *
 * @author Luciano Fiandesio
 */
@Service
public class TrackerUserService
{
    private final CurrentUserService currentUserService;

    private final IdentifiableObjectManager manager;

    public TrackerUserService( CurrentUserService currentUserService, IdentifiableObjectManager manager )
    {
        this.currentUserService = currentUserService;
        this.manager = manager;
    }

    /**
     * Fetch a User by user uid
     * @param userUid a User uid
     * @return a User
     */
    @Transactional( readOnly = true )
    public User getUser( String userUid )
    {
        User user = null;

        if ( !StringUtils.isEmpty( userUid ) )
        {
            user = manager.get( User.class, userUid );
        }
        if ( user == null )
        {
            user = currentUserService.getCurrentUser();
        }

        initUser( user );

        return user;
    }

    /**
     * Make sure the User object has all collection initialized, since Tracker creates new threads and there is
     * no Hibernate Session available.
     */
    private void initUser( final User user )
    {
        if (user == null) return;

        UserCredentials userCredentials = user.getUserCredentials();
        // Init all collections
        HibernateUtils.initializeProxy( userCredentials );

        // Trigger additional collections which are not covered by `initializeProxy`.
        // This is not very "elegant" but it avoids opening a transaction in other parts of Tracker
        // just to fetch user data
        user.isSuper();
        user.getOrganisationUnits().size();
    }
}
