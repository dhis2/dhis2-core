package org.hisp.dhis.web.uaa.oauth2;



import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping( value = "/oauth" )
public class OAuth2Controller
{
    @GetMapping( "/confirm_access" )
    public String confirmAccess( Model model, @RequestParam Map<String, String> rpParameters )
    {
        model.addAllAttributes( rpParameters );
        return "confirm_access";
    }

    @GetMapping( "/error" )
    public String error( Model model, @RequestParam Map<String, String> rpParameters )
    {
        model.addAllAttributes( rpParameters );
        return "error";
    }
}
