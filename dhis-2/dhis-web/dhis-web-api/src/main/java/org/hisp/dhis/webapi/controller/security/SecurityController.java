package org.hisp.dhis.webapi.controller.security;

import org.hisp.dhis.security.SecurityUtils;
import org.hisp.dhis.system.util.JacksonUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Henning Håkonsen
 */
@Controller
@RequestMapping( value = "/security" )
public class SecurityController
{
    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserService userService;

    @RequestMapping( value = "/2fa/qr", method = RequestMethod.GET, produces = "application/json" )
    public void getQrCode( HttpServletRequest request, HttpServletResponse response )
    {
        User currentUser = currentUserService.getCurrentUser();

        if ( currentUser == null )
        {
            throw new BadCredentialsException( "No current user" );
        }

        String url = SecurityUtils.generateQrUrl( currentUser );

        Map<String, Object> map = new HashMap<>();
        map.put( "url", url );

        JacksonUtils.fromObjectToReponse( response, map );
    }

    @RequestMapping( value = "/settwofa", method = RequestMethod.POST )
    public void set2FA( @RequestParam String uid, @RequestParam boolean twofa )
    {
        userService.set2FA( userService.getUser( uid ), twofa );
    }
}
