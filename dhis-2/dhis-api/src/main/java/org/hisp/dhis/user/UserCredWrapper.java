package org.hisp.dhis.user;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserCredWrapper extends User
{

//    @JsonIgnore
//    @Override public User getUserInfo()
//    {
//        return null;
//    }

    @JsonIgnore
    @Override public UserCredWrapper getUserCredentials()
    {
        return null;
    }
}
