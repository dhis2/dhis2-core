package org.hisp.dhis.analytics.trackedentity.aggregate;

import static org.hisp.dhis.analytics.ValidationHelper.validateResponseStructure;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderExistence;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowValueByName;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowExists;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeaderPropertiesByName;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.analytics.ValidationHelper;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsTrackedEntityActions;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsEnrollmentsActions;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.apache.commons.lang3.BooleanUtils;
/**
 * Groups e2e tests for "/trackedEntities/aggregate" endpoint.
 */
public class TrackedEntityAggregate4AutoTest extends AnalyticsApiTest {
  private final AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void aggregateAverageFirstEventValueByOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();
    
    // Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("aggregationType=AVERAGE")
    .add("totalPages=false")
    .add("pageSize=10")
    .add("dimension=ou:UAtEKSd5QTf;mVvEwzoFutG;uROAmk9ymNE;mMvt6zhCclb;IlMQTFvcq9r")
    .add("value=WSGAb5XwJ3Y.edqlbukwRfQ[1].vANAXwtLwcT")
    ;
    
        // When
        ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime 'expectPostgis' flag.
    validateResponseStructure(response, expectPostgis, 5, 2, 2); // Pass runtime flag, row count, and expected header counts
    
    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders = response.extractList("headers", Map.class).stream()
        .map(obj -> (Map<String, Object>) obj) // Ensure correct type
        .collect(Collectors.toList());
    
    
    // 3. Assert metaData.
    String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"uROAmk9ymNE\":{\"name\":\"Kindoyal Hospital\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"mVvEwzoFutG\":{\"name\":\"Nyandehun MCHP\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"UAtEKSd5QTf\":{\"name\":\"Konta (Gorama M) CHP\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"IlMQTFvcq9r\":{\"name\":\"Lowoma CHC\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"mMvt6zhCclb\":{\"name\":\"Manjama MCHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"IlMQTFvcq9r\",\"mMvt6zhCclb\",\"mVvEwzoFutG\",\"UAtEKSd5QTf\",\"uROAmk9ymNE\"]}}";
    String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);
    
    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
        validateHeaderPropertiesByName(response, actualHeaders,"ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
        validateHeaderPropertiesByName(response, actualHeaders,"value", "Value", "NUMBER", "java.lang.Double", false, false);
        
    
    // rowContext not found or empty in the response, skipping assertions.
    
    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("ou", "IlMQTFvcq9r", "value", "14.11"));
    
    // Validate row exists with values from original row index 2
    validateRowExists(response, actualHeaders, Map.of("ou", "mVvEwzoFutG", "value", "16.91"));
    
    // Validate row exists with values from original row index 4
    validateRowExists(response, actualHeaders, Map.of("ou", "uROAmk9ymNE", "value", "13.78"));
  }
}