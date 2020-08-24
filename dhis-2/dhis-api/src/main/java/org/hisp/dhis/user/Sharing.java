package org.hisp.dhis.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;
import org.hisp.dhis.common.DxfNamespaces;

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
        userAccesses.forEach( ua -> this.users.put( ua.getId(), ua ) );
    }

    public void setUserGroupAccess( Set<UserGroupAccess> userGroupAccesses )
    {
        this.userGroups.clear();
        userGroupAccesses.forEach( uga -> this.userGroups.put( uga.getId(), uga ) );
    }

    public void addUserAccess( UserAccess userAccess )
    {
        this.users.put( userAccess.getId(), userAccess );
    }

    public void addUserGroupAccess( UserGroupAccess userGroupAccess )
    {
        this.userGroups.put( userGroupAccess.getId(), userGroupAccess );
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

