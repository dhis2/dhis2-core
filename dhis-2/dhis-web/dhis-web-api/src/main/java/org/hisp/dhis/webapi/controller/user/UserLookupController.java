package org.hisp.dhis.webapi.controller.user;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.webdomain.user.UserLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping( value = "/{id}", produces = "application/json" )
    public UserLookup lookUpUser( @PathVariable String id )
    {
        User user = userService.getUserByIdentifier( id );

        return user != null ? UserLookup.fromUser( user ) : null;
    }

    @GetMapping( produces = "application/json" )
    public List<UserLookup> lookUpUsers( @RequestParam String query )
    {
        UserQueryParams params = new UserQueryParams()
            .setQuery( query )
            .setMax( 25 );

        return userService.getUsers( params ).stream()
            .map( user -> UserLookup.fromUser( user ) )
            .collect( Collectors.toList() );
    }
}