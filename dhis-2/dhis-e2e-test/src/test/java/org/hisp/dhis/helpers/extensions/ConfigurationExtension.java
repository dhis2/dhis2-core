package org.hisp.dhis.helpers.extensions;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class ConfigurationExtension
    implements BeforeAllCallback
{
    @Override
    public void beforeAll( ExtensionContext context )
        throws Exception
    {
        RestAssured.baseURI = "http://localhost:8070/api";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.defaultParser = Parser.JSON;
        RestAssured.requestSpecification = defaultRequestSpecification();
    }

    private RequestSpecification defaultRequestSpecification()
    {
        RequestSpecBuilder requestSpecification = new RequestSpecBuilder();

        requestSpecification.addFilter( new CookieFilter() );
        requestSpecification.addFilter( new SessionFilter() );
        requestSpecification.setContentType( ContentType.JSON );

        return requestSpecification.build();
    }
}
