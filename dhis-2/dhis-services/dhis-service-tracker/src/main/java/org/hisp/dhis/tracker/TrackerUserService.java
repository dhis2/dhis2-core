package org.hisp.dhis.tracker;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.tracker.preheat.mappers.FullUserMapper;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Specialized User Service for Tracker that executes User look-up in a
 * read-only transaction
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
     *
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
        // Make a copy of the user object, retaining only the properties required for
        // the import operation
        return FullUserMapper.INSTANCE.map( user );
    }
}
