package org.hisp.dhis.user;

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@Component( "org.hisp.dhis.user.UserGroupPopulator" )
public class UserGroupPopulator
    extends TransactionContextStartupRoutine
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final IdentifiableObjectStore<UserGroup> userGroupStore;

    private final AclService aclService;

    public UserGroupPopulator( IdentifiableObjectStore<UserGroup> userGroupStore, AclService aclService )
    {
        checkNotNull( userGroupStore );
        checkNotNull( aclService );

        this.userGroupStore = userGroupStore;
        this.aclService = aclService;
    }

    // -------------------------------------------------------------------------
    // Execute
    // -------------------------------------------------------------------------

    @Override
    public void executeInTransaction()
    {
        aclService.getUserGroupCache().invalidateAll();

        List<UserGroup> groups = userGroupStore.getAllNoAcl();
        groups.forEach( group -> {
            group.getMembers();
            aclService.getUserGroupCache().put( group.getUid(), group );
        } );

        log.info( "Added " + groups.size() + " UserGroups into UserGroupCache" );
    }
}