package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.json.domain.JsonGenerator.generateObject;
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertSeries;
import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonList;
import org.hisp.dhis.webapi.json.domain.JsonSchema;
import org.junit.Test;

public class SchemaBasedControllerTest extends DhisControllerConvenienceTest
{

    @Test
    public void testCreateAndDeleteSchemaObjects()
    {
        JsonList<JsonSchema> schemas = GET( "/schemas" )
            .content().getList( "schemas", JsonSchema.class );

        for ( JsonSchema schema : schemas )
        {
            if ( !schema.isIdentifiableObject() )
            {
                continue;
            }
            String endpoint = schema.getPlural();
            try
            {
                String body = generateObject( schema );
                System.out.println( schema.getName() );
                System.out.println( body );
                String uid = assertSeries( SUCCESSFUL, POST( "/" + endpoint, body ) );
                assertSeries( SUCCESSFUL, DELETE( "/" + endpoint + "/" + uid ) );
                System.out.println( "Deleted " + schema.getName() );
            }
            catch ( Throwable ex )
            {
                System.out.println( "Failed: " + ex.getMessage() );
            }
        }
    }
}
