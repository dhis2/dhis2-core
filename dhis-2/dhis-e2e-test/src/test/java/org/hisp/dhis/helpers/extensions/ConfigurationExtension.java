package org.hisp.dhis.helpers.extensions;



import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.JsonConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.specification.RequestSpecification;
import org.hisp.dhis.helpers.config.TestConfiguration;
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
    {
        RestAssured.baseURI = TestConfiguration.get().baseUrl();

        RestAssured.config = RestAssuredConfig.config()
            .jsonConfig( new JsonConfig().numberReturnType( JsonPathConfig.NumberReturnType.BIG_DECIMAL ) );

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        RestAssured.defaultParser = Parser.JSON;
        RestAssured.requestSpecification = defaultRequestSpecification();
    }

    private RequestSpecification defaultRequestSpecification()
    {
        RequestSpecBuilder requestSpecification = new RequestSpecBuilder();

        requestSpecification.addFilter( new CookieFilter() );
        requestSpecification.addFilter( new SessionFilter() );
        requestSpecification.addFilter( new AuthFilter() );
        requestSpecification.setContentType( ContentType.JSON );

        return requestSpecification.build();
    }
}
