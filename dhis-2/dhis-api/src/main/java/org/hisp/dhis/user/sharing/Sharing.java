package org.hisp.dhis.user.sharing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.user.User;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Builder
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JacksonXmlRootElement( localName = "sharing", namespace = DxfNamespaces.DXF_2_0 )
public class Sharing
    implements Serializable
{
    private static final long serialVersionUID = 6977793211734844477L;

    @JsonProperty
    private String owner = null;

    @JsonProperty("public")
    private String publicAccess = null;

    @JsonProperty
    private boolean external = false;

    @JsonProperty
    private Map<String, UserAccess> users = new HashMap<>();

    @JsonProperty
    private Map<String, UserGroupAccess> userGroups = new HashMap<>();

    public Sharing copy()
    {
        return builder()
            .external( this.external )
            .publicAccess( this.publicAccess )
            .owner( this.owner )
            .users( new HashMap<>( users ) )
            .userGroups( new HashMap<>( userGroups ) ).build();
    }

    public void setOwner( User user )
    {
        this.owner =  user != null ? user.getUid() : null;
    }

    public void setOwner( String userId )
    {
        this.owner = userId;
    }

    public void setUserAccesses( Set<UserAccess> userAccesses )
    {
        this.users.clear();
        userAccesses.forEach( ua -> this.addUserAccess( ua ) );
    }

    public void setDtoUserAccesses( Set<org.hisp.dhis.user.UserAccess> userAccesses )
    {
        this.users.clear();

        if ( userAccesses != null && !userAccesses.isEmpty() )
        {
            userAccesses.forEach( ua ->  this.addUserAccess( new UserAccess( ua ) ) );
        }
    }

    public void setDtoUserGroupAccesses( Set<org.hisp.dhis.user.UserGroupAccess> userGroupAccesses )
    {
        this.userGroups.clear();

        if ( userGroupAccesses != null && !userGroupAccesses.isEmpty() )
        {
            userGroupAccesses.forEach( uga ->  this.addUserGroupAccess( new UserGroupAccess( uga ) ) );
        }
    }

    public void setUserGroupAccess( Set<UserGroupAccess> userGroupAccesses )
    {
        this.userGroups.clear();
        userGroupAccesses.forEach( uga -> this.addUserGroupAccess( uga ) );
    }

    public void addUserAccess( UserAccess userAccess )
    {
        if ( userAccess != null )
        {
            this.users.put( userAccess.getId(), userAccess );
        }
    }

    public void addDtoUserAccess( org.hisp.dhis.user.UserAccess userAccess )
    {
        this.users.put( userAccess.getUid(), new UserAccess( userAccess ) );
    }

    public void addDtoUserGroupAccess( org.hisp.dhis.user.UserGroupAccess userGroupAccess )
    {
        this.userGroups.put( userGroupAccess.getUid(), new UserGroupAccess( userGroupAccess ) );
    }

    public void addUserGroupAccess( UserGroupAccess userGroupAccess )
    {
        if ( userGroupAccess != null )
        {
            this.userGroups.put( userGroupAccess.getId(), userGroupAccess );
        }
    }

    public void resetUserAccesses()
    {
        this.users.clear();
    }

    public void resetUserGroupAccesses()
    {
        this.userGroups.clear();
    }

}

