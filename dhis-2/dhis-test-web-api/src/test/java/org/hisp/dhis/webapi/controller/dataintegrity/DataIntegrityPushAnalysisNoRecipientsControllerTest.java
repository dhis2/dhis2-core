package org.hisp.dhis.webapi.controller.dataintegrity;

import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;

import static org.hisp.dhis.web.WebClientUtils.assertStatus;

class DataIntegrityPushAnalysisNoRecipientsControllerTest extends AbstractDataIntegrityIntegrationTest
{
    private static final String check = "push_analysis_no_recipients";

    private static final String detailsIdType = "pushAnalysis";

    @Test
    void testPushAnalysisNoRecipients()
    {

        String testDashboard = assertStatus( HttpStatus.CREATED,
            POST( "/dashboards", "{ 'name': 'Test Dashboard', 'description': 'Test Dashboard' }" ) );

        String testPushAnalysis = assertStatus( HttpStatus.CREATED, POST( "/pushAnalysis",
            "{ 'name': 'Foo', 'dashboard' : {'" + testDashboard + "'}, 'recipientUserGroups' : []}" ) );

        assertHasDataIntegrityIssues( detailsIdType,check, 100, testPushAnalysis, "Foo", null, true );
    }
}
