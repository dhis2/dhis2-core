package org.hisp.dhis.actions.metadata;




import io.restassured.matcher.RestAssuredMatchers;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;

import java.io.File;

import static org.hamcrest.CoreMatchers.not;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MetadataActions
    extends RestApiActions
{
    public MetadataActions()
    {
        super( "/metadata" );
    }

    public ApiResponse importMetadata( File file, String... queryParams )
    {
        QueryParamsBuilder queryParamsBuilder = new QueryParamsBuilder();
        queryParamsBuilder.addAll( queryParams );
        queryParamsBuilder.addAll( "atomicMode=OBJECT", "importReportMode=FULL" );

        ApiResponse response = postFile( file, queryParamsBuilder );
        response.validate().statusCode( 200 );

        return response;
    }

    public ApiResponse importAndValidateMetadata( File file, String... queryParams )
    {
        ApiResponse response = importMetadata( file, queryParams );

        response.validate().body( "stats.ignored", not( RestAssuredMatchers.equalToPath( "stats.total" ) ) );

        return response;
    }
}
