package org.hisp.dhis.gist;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
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
    private static final Set<String> PUBLIC_USER_PROPERTY_NAMES = unmodifiableSet(
        new HashSet<>( asList( "id", "code", "displayName", "name" ) ) );

    private static final Set<String> PUBLIC_USER_CREDENTIALS_PROPERTY_NAMES = singleton( "username" );

    private static final Set<String> PUBLIC_PROPERTY_NAMES = unmodifiableSet(
        new HashSet<>( asList( "sharing", "access", "translations" ) ) );

    private final User currentUser;

    private final AclService aclService;

    private final UserService userService;

    private final IdentifiableObjectManager objectManager;

    @Override
    public boolean isSuperuser()
    {
        return currentUser != null && !currentUser.isSuper();
    }

    @Override
    public boolean canRead( Class<? extends IdentifiableObject> type )
    {
        return aclService.canRead( currentUser, type );
    }

    @Override
    public boolean canRead( Class<? extends IdentifiableObject> type, String uid )
    {
        // TODO this could also just load the Sharing of the object and use that
        // use GistService to fetch sharing?
        return aclService.canRead( currentUser, objectManager.get( type, uid ) );
    }

    @Override
    public boolean canRead( Class<? extends IdentifiableObject> type, Property field )
    {
        boolean isUserField = type == User.class;
        boolean isUserCredentialsField = type == UserCredentials.class;
        String name = field.getName();
        if ( isUserField && PUBLIC_USER_PROPERTY_NAMES.contains( name ) )
        {
            return true;
        }
        if ( isUserCredentialsField && PUBLIC_USER_CREDENTIALS_PROPERTY_NAMES.contains( name ) )
        {
            return true;
        }
        return aclService.canRead( currentUser, type );
    }

    @Override
    public boolean canFilterByAccessOfUser( String userUid )
    {
        User user = userService.getUser( userUid );
        return user != null
            && (currentUser.canManage( user ) || currentUser.getUid().equals( user.getCreatedBy().getUid() ));
    }

    @Override
    public Access asAccess( Class<? extends IdentifiableObject> type, Sharing value )
    {
        BaseIdentifiableObject object = new BaseIdentifiableObject();
        object.setSharing( value );
        return aclService.getAccess( object, currentUser, type );
    }

    @Override
    public String createAccessFilterHQL( String tableName )
    {
        return JpaQueryUtils.generateHqlQueryForSharingCheck( tableName, currentUser, AclService.LIKE_READ_METADATA );
    }
}
