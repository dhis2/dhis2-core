package org.hisp.dhis.tracker.message;


import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.actions.tracker.message.ProgramMessageActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.imports.databuilder.EnrollmentDataBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author Zubair Asghar
 */
class ProgramMessageTest extends TrackerApiTest {
    private String enrollmentUid;
    private ProgramMessageActions programMessageActions;

    @BeforeAll
    public void beforeAll() throws Exception {
        loginActions.loginAsSuperUser();
        programMessageActions = new ProgramMessageActions();
        new MetadataActions()
                .importAndValidateMetadata(
                        new File("src/test/resources/tracker/programs_with_program_rules.json"));

        String trackerProgramId = "U5HE4IRrZ7S";
        enrollmentUid = trackerImportExportActions
                .postAndGetJobReport(
                        new EnrollmentDataBuilder()
                                .setTrackedEntity(importTrackedEntity())
                                .setEnrollmentDate(Instant.now().plus(1, ChronoUnit.DAYS).toString())
                                .array(trackerProgramId, Constants.ORG_UNIT_IDS[0]))
                .extractImportedEnrollments()
                .get(0);

        String programOrgUnit = "g8upMTyEZGZ";
        programMessageActions.sendProgramMessage(enrollmentUid, programOrgUnit);
    }

    @Test
    void shouldGetProgramMessageForEnrollment(){
       QueryParamsBuilder params = new QueryParamsBuilder()
               .add("enrollment="+enrollmentUid);

       ApiResponse response = programMessageActions.get("", JSON,JSON,params);
        List<Object> objectList = response
                .validate()
                .statusCode(200).extract().body().jsonPath().get("enrollment.id");

        Assertions.assertEquals(  enrollmentUid,objectList.get(0));
    }

    @Test
    void shouldGetValidationErrorWhenEnrollmentAndEventNotPresent(){
        QueryParamsBuilder params = new QueryParamsBuilder()
                .add("enrollment="+"g8upMTyEeee");

        ApiResponse response = programMessageActions.get("", JSON,JSON,params);

        response
                .validate()
                .statusCode(409)
                .body("httpStatus", equalTo("Bad Request"))
                .body("status", equalTo("Conflict"))
                .body("message", equalTo("Enrollment does not exist."));
    }
}
