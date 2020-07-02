package org.hisp.dhis.webapi.webdomain.user;

import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserLookup
{
    @JsonProperty
    private String id;

    @JsonProperty
    private String firstName;

    @JsonProperty
    private String surname;

    @JsonProperty
    private String displayName;

    public static UserLookup fromUser( User user )
    {
        String displayName = String.format( "%s %s", user.getFirstName(), user.getSurname() );

        UserLookup lookup = new UserLookup();
        lookup.setId( user.getUid() ); // Will be changed to uuid
        lookup.setFirstName( user.getFirstName() );
        lookup.setSurname( user.getSurname() );
        lookup.setDisplayName( displayName );
        return lookup;
    }
}
