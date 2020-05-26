package org.hisp.dhis.user;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.stereotype.Component;

import static com.google.api.client.util.Preconditions.checkNotNull;

/**
 * @author Stian Sandvold
 */
@Component( "org.hisp.dhis.user.UserAccessDeletionHandler" )
public class UserAccessDeletionHandler
    extends DeletionHandler
{
    private final IdentifiableObjectManager idObjectManager;

    public UserAccessDeletionHandler( IdentifiableObjectManager idObjectManager )
    {
        checkNotNull( idObjectManager );


        this.idObjectManager = idObjectManager;
    }

    @Override
    protected String getClassName()
    {
        return UserAccess.class.getSimpleName();
    }

    @Override
    public void deleteUser( User user )
    {
        user.getUserAccesses().clear();
        idObjectManager.updateNoAcl( user );
    }


}
