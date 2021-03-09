package org.hisp.dhis.helpers.extensions;



import io.restassured.authentication.NoAuthScheme;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class AuthFilter
    implements io.restassured.spi.AuthFilter
{
    private String lastLoggedInUser = "";
    private String lastLoggedInUserPsw= "";

    @Override
    public Response filter( FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
        FilterContext ctx )
    {
        if ( requestSpec.getAuthenticationScheme() instanceof NoAuthScheme )
        {
            if ( hasSessionCookie( requestSpec ) )
            {
                requestSpec.removeCookies();
            }

            lastLoggedInUser = "";
            lastLoggedInUserPsw = "";
        }

        if ( requestSpec.getAuthenticationScheme() instanceof PreemptiveBasicAuthScheme && (
            ((PreemptiveBasicAuthScheme) requestSpec.getAuthenticationScheme()).getUserName() != lastLoggedInUser ||
            ((PreemptiveBasicAuthScheme) requestSpec.getAuthenticationScheme()).getPassword() != lastLoggedInUserPsw ) )
        {
            if ( hasSessionCookie( requestSpec ) )
            {
                requestSpec.removeCookies();
            }

            lastLoggedInUser = ((PreemptiveBasicAuthScheme) requestSpec.getAuthenticationScheme()).getUserName();
            lastLoggedInUserPsw = ((PreemptiveBasicAuthScheme) requestSpec.getAuthenticationScheme()).getPassword();
        }

        final Response response = ctx.next( requestSpec, responseSpec );
        return response;
    }

    private boolean hasSessionCookie( FilterableRequestSpecification requestSpec )
    {
        return requestSpec.getCookies().hasCookieWithName( "JSESSIONID" ) ||
            requestSpec.getCookies().hasCookieWithName( "SESSION" );
    }
}
