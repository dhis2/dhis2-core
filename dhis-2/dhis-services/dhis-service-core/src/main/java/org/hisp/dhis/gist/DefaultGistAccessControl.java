package org.hisp.dhis.gist;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

import lombok.AllArgsConstructor;

/**
 * Encapsulates all access control related logic of gist API request processing.
 *
 * An instance is always related to the current {@link User} of the currently
 * processed gist API request.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
public class DefaultGistAccessControl implements GistAccessControl
{

    private final User currentUser;

    private final AclService aclService;

    @Override
    public User getCurrentUser()
    {
        return currentUser;
    }

    @Override
    public boolean isSuperuser()
    {
        return currentUser != null && !currentUser.isSuper();
    }

    @Override
    public boolean canRead( Class<? extends IdentifiableObject> type, Property field )
    {
        return false;
    }

    @Override
    public Access canAccess( Class<? extends IdentifiableObject> type, Sharing value )
    {
        BaseIdentifiableObject object = new BaseIdentifiableObject();
        object.setSharing( value );
        return aclService.getAccess( object, currentUser, type );
    }
}
