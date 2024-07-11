package org.hisp.dhis.webapi.controller.dataintegrity;

import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.json.domain.JsonDataIntegritySummary;
import org.junit.jupiter.api.Test;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DataIntegrityUserRolesNoUsers extends AbstractDataIntegrityIntegrationTest
{
    private static final String CHECK_NAME = "user_roles_with_no_users";

    private static final String DETAILS_ID_TYPE = "userRoles";

    private String userRoleUid;

    @Test
    void testUserRolesNoUsers() {
        userRoleUid = assertStatus(
            HttpStatus.CREATED, POST("/userRoles", "{ 'name': 'Test role', 'authorities': ['F_DATAVALUE_ADD'] }"));
        //Note that two user roles already exist as part of the setup in the AbstractDataIntegrityIntegrationTest class
        //Thus, there should be three roles in total, two of which are valid since they already have users associated with them.
        postSummary(CHECK_NAME);

        JsonDataIntegritySummary summary = getSummary(CHECK_NAME);
        assertEquals(1, summary.getCount());
        assertHasDataIntegrityIssues(DETAILS_ID_TYPE, CHECK_NAME, 33, userRoleUid, "Test role", null, true);
    }
}
