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
public class TrackedEntityAggregate3AutoTest extends AnalyticsApiTest {
    private final AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();
  @Test
  public void aggregateMinBirthWeightByOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();
    
    // Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("aggregationType=MIN")
    .add("totalPages=false")
    .add("pageSize=10")
    .add("dimension=ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m")
    .add("value=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU")
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
    String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);
    
    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
        validateHeaderPropertiesByName(response, actualHeaders,"ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
        validateHeaderPropertiesByName(response, actualHeaders,"value", "Value", "NUMBER", "java.lang.Double", false, false);
    

    
    // rowContext not found or empty in the response, skipping assertions.
    
    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("ou", "DiszpKrYNg8", "value", "2313"));
    
    // Validate row exists with values from original row index 2
    validateRowExists(response, actualHeaders, Map.of("ou", "Qr41Mw2MSjo", "value", "2519"));
    
    // Validate row exists with values from original row index 4
    validateRowExists(response, actualHeaders, Map.of("ou", "VpYAl8dXs6m", "value", "2576"));
  }
  @Test
  public void aggregateMaxBirthWeightByOrgUnit() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();
    
    // Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("aggregationType=MAX")
    .add("totalPages=false")
    .add("pageSize=10")
    .add("dimension=ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m")
    .add("value=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU")
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
    String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);
    
    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
        validateHeaderPropertiesByName(response, actualHeaders,"ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
        validateHeaderPropertiesByName(response, actualHeaders,"value", "Value", "NUMBER", "java.lang.Double", false, false);

    
    // rowContext not found or empty in the response, skipping assertions.
    
    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("ou", "DiszpKrYNg8", "value", "36282"));
    
    // Validate row exists with values from original row index 2
    validateRowExists(response, actualHeaders, Map.of("ou", "Qr41Mw2MSjo", "value", "3960"));
    
    // Validate row exists with values from original row index 4
    validateRowExists(response, actualHeaders, Map.of("ou", "VpYAl8dXs6m", "value", "3959"));
  }
  @Test
  public void aggregateAverageBirthWeightByOrgUnitAndGender() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();
    
    // Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("aggregationType=AVERAGE")
    .add("totalPages=false")
    .add("pageSize=20")
    .add("dimension=ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m,cejWyOfXge6")
    .add("value=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU")
    ;
    
        // When
        ApiResponse response = actions.aggregate().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    // 1. Validate Response Structure (Counts, Headers, Height/Width)
    //    This helper checks basic counts and dimensions, adapting based on the runtime 'expectPostgis' flag.
    validateResponseStructure(response, expectPostgis, 16, 3, 3); // Pass runtime flag, row count, and expected header counts
    
    // 2. Extract Headers into a List of Maps for easy access by name
    List<Map<String, Object>> actualHeaders = response.extractList("headers", Map.class).stream()
        .map(obj -> (Map<String, Object>) obj) // Ensure correct type
        .collect(Collectors.toList());
    
    
    // 3. Assert metaData.
    String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":20,\"isLastPage\":true},\"items\":{\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"pC3N9N77UmT\":{\"uid\":\"pC3N9N77UmT\",\"name\":\"Gender\",\"options\":[{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\"},{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\"}]},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"cejWyOfXge6\":{\"name\":\"Gender\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";
    String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);
    
    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
        validateHeaderPropertiesByName(response, actualHeaders,"ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
        validateHeaderPropertiesByName(response, actualHeaders,"cejWyOfXge6", "Gender", "TEXT", "java.lang.String", false, true);
        validateHeaderPropertiesByName(response, actualHeaders,"value", "Value", "NUMBER", "java.lang.Double", false, false);

    
    // rowContext not found or empty in the response, skipping assertions.
    
    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("ou", "DiszpKrYNg8", "cejWyOfXge6", "Female", "value", "5369.29"));
    
    // Validate row exists with values from original row index 3
    validateRowExists(response, actualHeaders, Map.of("ou", "DiszpKrYNg8", "cejWyOfXge6", "", "value", ""));
    
    // Validate row exists with values from original row index 6
    validateRowExists(response, actualHeaders, Map.of("ou", "QII5GqfDfO3", "cejWyOfXge6", "", "value", ""));
    
    // Validate row exists with values from original row index 9
    validateRowExists(response, actualHeaders, Map.of("ou", "Qr41Mw2MSjo", "cejWyOfXge6", "", "value", ""));
    
    // Validate row exists with values from original row index 12
    validateRowExists(response, actualHeaders, Map.of("ou", "QZzRkqdGjlm", "cejWyOfXge6", "", "value", ""));
    
    // Validate row exists with values from original row index 15
    validateRowExists(response, actualHeaders, Map.of("ou", "VpYAl8dXs6m", "cejWyOfXge6", "", "value", ""));
  }
  @Test
  public void aggregateAverageBirthWeightByOrgUnitFilteredByFemale() throws JSONException {
    // Read the 'expect.postgis' system property at runtime to adapt assertions.
    boolean expectPostgis = isPostgres();
    
    // Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=cejWyOfXge6:IN:Female")
    .add("aggregationType=AVERAGE")
    .add("totalPages=false")
    .add("pageSize=10")
    .add("dimension=ou:QII5GqfDfO3;DiszpKrYNg8;QZzRkqdGjlm;Qr41Mw2MSjo;VpYAl8dXs6m")
    .add("value=IpHINAT79UW.A03MvHHogjR.UXz7xuGCEhU")
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
    String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":10,\"isLastPage\":true},\"items\":{\"QII5GqfDfO3\":{\"name\":\"Ngiehun Kongo CHP\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"jdRD35YwbRH\":{\"name\":\"Sputum smear microscopy test\"},\"fDd25txQckK\":{\"name\":\"Provider Follow-up and Support Tool\"},\"VpYAl8dXs6m\":{\"name\":\"Bendoma (Malegohun) MCHP\"},\"PFDfvmGpsR3\":{\"name\":\"Care at birth\"},\"Qr41Mw2MSjo\":{\"name\":\"Senthai MCHP\"},\"lST1OZ5BDJ2\":{\"name\":\"Provider Follow-up and Support Tool\"},\"EPEcjy3FWmI\":{\"name\":\"Lab monitoring\"},\"ur1Edk5Oe2n\":{\"name\":\"TB program\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"PUZaKR0Jh2k\":{\"name\":\"Previous deliveries\"},\"WZbXY0S00lP\":{\"name\":\"First antenatal care visit\"},\"Xgk8Wvl0jHr\":{\"name\":\"Delivery\"},\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"bbKtnxRZKEP\":{\"name\":\"Postpartum care visit\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ou\":{\"name\":\"Organisation unit\"},\"edqlbukwRfQ\":{\"name\":\"Second antenatal care visit\"},\"ZkbAXlQUYJG\":{\"name\":\"TB visit\"},\"oRySG82BKE6\":{\"name\":\"PNC Visit\"},\"grIfo3oOf4Y\":{\"name\":\"ANC Visit (2-4+)\"},\"eaDHS084uMp\":{\"name\":\"ANC 1st visit\"},\"uy2gU8kT1jF\":{\"name\":\"MNCH \\/ PNC (Adult Woman)\"},\"WSGAb5XwJ3Y\":{\"name\":\"WHO RMNCH Tracker\"},\"QZzRkqdGjlm\":{\"name\":\"Mindohun CHP\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"DiszpKrYNg8\",\"QII5GqfDfO3\",\"Qr41Mw2MSjo\",\"QZzRkqdGjlm\",\"VpYAl8dXs6m\"]}}";
    String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);
    
    // 4. Validate Headers By Name (conditionally checking PostGIS headers).
        validateHeaderPropertiesByName(response, actualHeaders,"ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
        validateHeaderPropertiesByName(response, actualHeaders,"value", "Value", "NUMBER", "java.lang.Double", false, false);

    
    // rowContext not found or empty in the response, skipping assertions.
    
    // 7. Assert row existence by value (unsorted results - validates all columns).
    // Validate row exists with values from original row index 0
    validateRowExists(response, actualHeaders, Map.of("ou", "DiszpKrYNg8", "value", "5369.29"));
    
    // Validate row exists with values from original row index 2
    validateRowExists(response, actualHeaders, Map.of("ou", "Qr41Mw2MSjo", "value", "3430.79"));
    
    // Validate row exists with values from original row index 4
    validateRowExists(response, actualHeaders, Map.of("ou", "VpYAl8dXs6m", "value", "3225.63"));
  }
}