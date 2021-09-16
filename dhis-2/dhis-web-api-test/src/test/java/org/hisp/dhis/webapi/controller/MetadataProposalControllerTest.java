package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonResponse;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the
 * {@link org.hisp.dhis.webapi.controller.metadata.MetadataProposalController}.
 *
 * @author Jan Bernitt
 */
public class MetadataProposalControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testGetProposal()
    {
        assertStatus( HttpStatus.CREATED,
            POST( "/metadata/proposals/", "{" +
                "'type':'ADD'," +
                "'target':'ORGANISATION_UNIT'," +
                "'change':{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}" +
                "}" ) );

        JsonResponse content = GET( "/metadata/proposals/" ).content();
        System.out.println( content );
    }
}
