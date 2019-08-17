package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import com.google.common.base.Preconditions;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserGroup;
import org.springframework.transaction.annotation.Transactional;

public class UserAuthorityGroupObjectBundleHook
    extends AbstractObjectBundleHook
{
    private SessionFactory sessionFactory;

    public UserAuthorityGroupObjectBundleHook( SessionFactory sessionFactory )
    {
        Preconditions.checkNotNull( sessionFactory );
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional
    public void preCommit( ObjectBundle bundle )
    {
        bundle.getObjects( UserAuthorityGroup.class, false ).forEach( object -> saveNotPersistedUserGroup( object, bundle ) );
        bundle.getObjects( UserAuthorityGroup.class, true ).forEach( object -> saveNotPersistedUserGroup( object, bundle ) );
    }

    private void saveNotPersistedUserGroup( IdentifiableObject object, ObjectBundle bundle )
    {
        UserAuthorityGroup userAuthorityGroup = (UserAuthorityGroup) object;

        if ( !CollectionUtils.isEmpty( userAuthorityGroup.getUserGroupAccesses() ) )
        {
            userAuthorityGroup.getUserGroupAccesses().forEach( uga -> {
                if ( uga.getUserGroup().getId() == 0 )
                {
                    UserGroup userGroup = uga.getUserGroup();
                    userGroup.getMembers().clear();
                    sessionFactory.getCurrentSession().save( userGroup );

                    if ( bundle.getPreheat().containsKey( bundle.getPreheatIdentifier(), UserGroup.class, bundle.getPreheatIdentifier().getIdentifier( userGroup ) ) )
                    {
                        bundle.getPreheat().replace( bundle.getPreheatIdentifier(), userGroup );
                    }
                    else
                    {
                        bundle.getPreheat().put( PreheatIdentifier.UID, userGroup );
                    }
                }
            } );
        }
    }
}
