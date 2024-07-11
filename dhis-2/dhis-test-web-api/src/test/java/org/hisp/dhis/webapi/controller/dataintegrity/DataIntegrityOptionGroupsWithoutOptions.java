package org.hisp.dhis.webapi.controller.dataintegrity;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.junit.jupiter.api.Test;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;


class DataIntegrityOptionGroupsWithoutOptions extends AbstractDataIntegrityIntegrationTest
{
    private static final String CHECK_NAME = "option_groups_empty";

    private static final String DETAILS_ID_TYPE = "optionGroups";
    @Test
    void testOptionGroupsWithNoOptions() {

        String goodOptionGroup = assertStatus(
            HttpStatus.CREATED, POST("/optionGroups", "{ 'name': 'Taste', 'shortName': 'Taste' }"));

        postSummary(CHECK_NAME);

        JsonDataIntegritySummary summary = getSummary(CHECK_NAME);

        assertHasDataIntegrityIssues( DETAILS_ID_TYPE, CHECK_NAME, 100, goodOptionGroup, "Taste", null, true);

    }

    @Test
    void testOptionGroupsWithOptions() {
        String goodOptionSet =
            assertStatus(
                HttpStatus.CREATED,
                POST("/optionSets", "{ 'name': 'Taste', 'shortName': 'Taste', 'valueType' : 'TEXT' }"));

        String sweetOption = assertStatus(
            HttpStatus.CREATED,
            POST(
                "/options",
                "{ 'code': 'SWEET',"
                    + "  'sortOrder': 1,"
                    + "  'name': 'Sweet',"
                    + "  'optionSet': { "
                    + "    'id': '"
                    + goodOptionSet
                    + "'"
                    + "  }}"));

        assertStatus(
            HttpStatus.CREATED, POST( "/optionGroups",
                "{ 'name': 'Taste', 'shortName': 'Taste' , 'optionSet' : { 'id' : '" + goodOptionSet + "' }, 'options' : [ { 'id' : '" + sweetOption + "' } ] }" ) );


            assertHasNoDataIntegrityIssues( DETAILS_ID_TYPE, CHECK_NAME, true);
    }
}
