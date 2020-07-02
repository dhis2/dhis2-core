package org.hisp.dhis.webapi.controller.user;

import java.util.List;

import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.webdomain.user.UserLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.util.Lists;

/**
 * The user lookup API provides a minimal user information endpoint.
 *
 * @author Lars Helge Overland
 */
@RestController( value = UserLookupController.API_ENDPOINT )
public class UserLookupController
{
    static final String API_ENDPOINT = "/userLookup";

    @Autowired
    private UserService userService;


    @GetMapping( "/{id}" )
    public List<UserLookup> lookUpUser( String id )
    {
        if ( id != null )
        {
            User user = userService.getUser( id );

            if ( user != null )
            {
                return Lists.newArrayList( user );
            }


        }
        UserQueryParams params = new UserQueryParams();

        return null;
    }
}
