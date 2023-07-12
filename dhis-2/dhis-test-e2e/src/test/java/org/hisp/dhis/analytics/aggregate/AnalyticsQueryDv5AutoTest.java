package org.hisp.dhis.analytics.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Groups e2e tests for "/analytics" aggregate endpoint.
 */
public class AnalyticsQueryDv5AutoTest extends AnalyticsApiTest {

  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void queryAncByLast4QuartersIndicatorsSubtotalsAndTotals() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("skipData=false")
        .add("includeNumDen=true")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:Uvn6LCg7dVU;OdiHJayrsKo;sB79w2hiLp8;dwEq7wi6nXV;c8fABiNpT0B,pe:LAST_4_QUARTERS,ou:jUb8gELQApl;TEQlaapDQoK;eIQbndfxQMb;Vth0fbpFcsO;PMa2VCrupOd;O6uvpzGd5pu;bL4ooGhyHRQ;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;qhqAxPSTUXp;jmIPBj66vD6,J5jldMd8OHv:uYxK4wmcPqA;EYbopBOJWsW;RXL3lPSK8oG;tDZVQ1WtwpA;CXw2yu5fodb")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(10)))
        .body("rows", hasSize(equalTo(1062)))
        .body("height", equalTo(1062))
        .body("width", equalTo(10))
        .body("headerWidth", equalTo(10));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"J5jldMd8OHv\":{\"name\":\"Facility Type\"},\"OdiHJayrsKo\":{\"name\":\"ANC 2 Coverage\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"uYxK4wmcPqA\":{\"name\":\"CHP\"},\"2021Q4\":{\"name\":\"October - December 2021\"},\"tDZVQ1WtwpA\":{\"name\":\"Hospital\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"c8fABiNpT0B\":{\"name\":\"ANC IPT 2 Coverage\"},\"dx\":{\"name\":\"Data\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"RXL3lPSK8oG\":{\"name\":\"Clinic\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"CXw2yu5fodb\":{\"name\":\"CHC\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"2021Q2\":{\"name\":\"April - June 2021\"},\"2021Q3\":{\"name\":\"July - September 2021\"},\"2021Q1\":{\"name\":\"January - March 2021\"},\"pe\":{\"name\":\"Period\"},\"dwEq7wi6nXV\":{\"name\":\"ANC IPT 1 Coverage\"},\"EYbopBOJWsW\":{\"name\":\"MCHP\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\",\"OdiHJayrsKo\",\"sB79w2hiLp8\",\"dwEq7wi6nXV\",\"c8fABiNpT0B\"],\"pe\":[\"2021Q1\",\"2021Q2\",\"2021Q3\",\"2021Q4\"],\"ou\":[\"jUb8gELQApl\",\"TEQlaapDQoK\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"O6uvpzGd5pu\",\"bL4ooGhyHRQ\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"at6UHUQatSo\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"jmIPBj66vD6\"],\"J5jldMd8OHv\":[\"uYxK4wmcPqA\",\"EYbopBOJWsW\",\"RXL3lPSK8oG\",\"tDZVQ1WtwpA\",\"CXw2yu5fodb\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false,
        true);
    validateHeader(response, 4, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 5, "numerator", "Numerator", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 6, "denominator", "Denominator", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 7, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 8, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 9, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "jUb8gELQApl", "uYxK4wmcPqA", "71.0", "1371.0", "7827.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "jUb8gELQApl", "EYbopBOJWsW", "87.6", "967.0", "4476.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "jUb8gELQApl", "RXL3lPSK8oG", "36.2", "57.0", "639.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "jUb8gELQApl", "tDZVQ1WtwpA", "167.4", "206.0", "499.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "jUb8gELQApl", "CXw2yu5fodb", "87.6", "1028.0", "4758.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "TEQlaapDQoK", "uYxK4wmcPqA", "92.6", "1036.0", "4536.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "TEQlaapDQoK", "EYbopBOJWsW", "106.4", "3203.0", "12207.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "TEQlaapDQoK", "CXw2yu5fodb", "92.1", "1088.0", "4793.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "eIQbndfxQMb", "uYxK4wmcPqA", "81.2", "300.0", "1499.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "eIQbndfxQMb", "EYbopBOJWsW", "96.7", "2661.0", "11156.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "eIQbndfxQMb", "tDZVQ1WtwpA", "44.4", "141.0", "1289.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "eIQbndfxQMb", "CXw2yu5fodb", "115.3", "825.0", "2902.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "Vth0fbpFcsO", "uYxK4wmcPqA", "58.7", "466.0", "3222.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "Vth0fbpFcsO", "EYbopBOJWsW", "66.5", "1317.0", "8030.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "Vth0fbpFcsO", "RXL3lPSK8oG", "3.0", "10.0", "1340.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "Vth0fbpFcsO", "tDZVQ1WtwpA", "198.4", "611.0", "1249.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "Vth0fbpFcsO", "CXw2yu5fodb", "58.3", "457.0", "3181.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "PMa2VCrupOd", "uYxK4wmcPqA", "93.0", "685.0", "2987.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "PMa2VCrupOd", "EYbopBOJWsW", "99.6", "1857.0", "7562.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "PMa2VCrupOd", "RXL3lPSK8oG", "118.3", "171.0", "586.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "PMa2VCrupOd", "CXw2yu5fodb", "97.3", "442.0", "1843.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "O6uvpzGd5pu", "uYxK4wmcPqA", "94.2", "801.0", "3447.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "O6uvpzGd5pu", "EYbopBOJWsW", "92.8", "2175.0", "9501.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "O6uvpzGd5pu", "RXL3lPSK8oG", "105.0", "159.0", "614.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "O6uvpzGd5pu", "CXw2yu5fodb", "202.1", "4230.0", "8488.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "bL4ooGhyHRQ", "uYxK4wmcPqA", "96.6", "669.0", "2810.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "bL4ooGhyHRQ", "EYbopBOJWsW", "118.1", "1237.0", "4249.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "bL4ooGhyHRQ", "CXw2yu5fodb", "142.5", "1498.0", "4263.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "kJq2mPyFEHo", "uYxK4wmcPqA", "85.4", "1264.0", "6006.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "kJq2mPyFEHo", "EYbopBOJWsW", "91.8", "2410.0", "10643.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "kJq2mPyFEHo", "tDZVQ1WtwpA", "225.6", "168.0", "302.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "kJq2mPyFEHo", "CXw2yu5fodb", "95.8", "1661.0", "7030.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "fdc6uOvgoji", "uYxK4wmcPqA", "88.2", "852.0", "3919.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "fdc6uOvgoji", "EYbopBOJWsW", "84.1", "2111.0", "10184.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "fdc6uOvgoji", "RXL3lPSK8oG", "66.9", "272.0", "1649.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "fdc6uOvgoji", "tDZVQ1WtwpA", "173.5", "163.0", "381.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "fdc6uOvgoji", "CXw2yu5fodb", "95.9", "988.0", "4179.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "at6UHUQatSo", "uYxK4wmcPqA", "153.7", "1346.0", "3551.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "at6UHUQatSo", "EYbopBOJWsW", "120.3", "1640.0", "5530.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "at6UHUQatSo", "RXL3lPSK8oG", "93.3", "1565.0", "6800.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "at6UHUQatSo", "tDZVQ1WtwpA", "124.7", "1520.0", "4943.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "at6UHUQatSo", "CXw2yu5fodb", "122.9", "4036.0", "13315.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "lc3eMKXaEfw", "uYxK4wmcPqA", "103.0", "526.0", "2072.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "lc3eMKXaEfw", "EYbopBOJWsW", "95.8", "625.0", "2646.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "lc3eMKXaEfw", "RXL3lPSK8oG", "81.4", "61.0", "304.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "lc3eMKXaEfw", "CXw2yu5fodb", "88.5", "448.0", "2053.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "qhqAxPSTUXp", "uYxK4wmcPqA", "71.8", "296.0", "1671.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "qhqAxPSTUXp", "EYbopBOJWsW", "74.4", "1612.0", "8788.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "qhqAxPSTUXp", "RXL3lPSK8oG", "220.3", "44.0", "81.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "qhqAxPSTUXp", "CXw2yu5fodb", "73.5", "500.0", "2759.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "jmIPBj66vD6", "uYxK4wmcPqA", "142.2", "703.0", "2005.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "jmIPBj66vD6", "EYbopBOJWsW", "136.5", "2057.0", "6113.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "jmIPBj66vD6", "RXL3lPSK8oG", "115.2", "71.0", "250.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "jmIPBj66vD6", "tDZVQ1WtwpA", "39.2", "48.0", "496.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q1", "jmIPBj66vD6", "CXw2yu5fodb", "132.1", "1307.0", "4014.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "jUb8gELQApl", "uYxK4wmcPqA", "81.6", "1592.0", "7827.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "jUb8gELQApl", "EYbopBOJWsW", "95.3", "1063.0", "4476.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "jUb8gELQApl", "RXL3lPSK8oG", "136.8", "218.0", "639.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "jUb8gELQApl", "tDZVQ1WtwpA", "171.2", "213.0", "499.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "jUb8gELQApl", "CXw2yu5fodb", "84.0", "997.0", "4758.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "TEQlaapDQoK", "uYxK4wmcPqA", "116.1", "1313.0", "4536.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "TEQlaapDQoK", "EYbopBOJWsW", "141.1", "4293.0", "12207.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "TEQlaapDQoK", "CXw2yu5fodb", "122.8", "1468.0", "4793.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "eIQbndfxQMb", "uYxK4wmcPqA", "110.2", "412.0", "1499.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "eIQbndfxQMb", "EYbopBOJWsW", "133.9", "3723.0", "11156.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "eIQbndfxQMb", "tDZVQ1WtwpA", "38.6", "124.0", "1289.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "eIQbndfxQMb", "CXw2yu5fodb", "166.4", "1204.0", "2902.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "Vth0fbpFcsO", "uYxK4wmcPqA", "69.8", "561.0", "3222.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "Vth0fbpFcsO", "EYbopBOJWsW", "61.4", "1230.0", "8030.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "Vth0fbpFcsO", "RXL3lPSK8oG", "20.4", "68.0", "1340.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "Vth0fbpFcsO", "tDZVQ1WtwpA", "87.0", "271.0", "1249.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "Vth0fbpFcsO", "CXw2yu5fodb", "66.5", "527.0", "3181.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "PMa2VCrupOd", "uYxK4wmcPqA", "101.5", "756.0", "2987.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "PMa2VCrupOd", "EYbopBOJWsW", "106.4", "2006.0", "7562.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "PMa2VCrupOd", "RXL3lPSK8oG", "179.3", "262.0", "586.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "PMa2VCrupOd", "CXw2yu5fodb", "129.1", "593.0", "1843.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "O6uvpzGd5pu", "uYxK4wmcPqA", "78.3", "673.0", "3447.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "O6uvpzGd5pu", "EYbopBOJWsW", "102.7", "2432.0", "9501.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "O6uvpzGd5pu", "RXL3lPSK8oG", "128.0", "196.0", "614.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "O6uvpzGd5pu", "CXw2yu5fodb", "198.2", "4195.0", "8488.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "bL4ooGhyHRQ", "uYxK4wmcPqA", "94.6", "663.0", "2810.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "bL4ooGhyHRQ", "EYbopBOJWsW", "113.8", "1205.0", "4249.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "bL4ooGhyHRQ", "CXw2yu5fodb", "150.7", "1602.0", "4263.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "kJq2mPyFEHo", "uYxK4wmcPqA", "92.5", "1385.0", "6006.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "kJq2mPyFEHo", "EYbopBOJWsW", "107.0", "2838.0", "10643.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "kJq2mPyFEHo", "tDZVQ1WtwpA", "196.6", "148.0", "302.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "kJq2mPyFEHo", "CXw2yu5fodb", "99.3", "1740.0", "7030.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "fdc6uOvgoji", "uYxK4wmcPqA", "107.0", "1045.0", "3919.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "fdc6uOvgoji", "EYbopBOJWsW", "104.1", "2643.0", "10184.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "fdc6uOvgoji", "RXL3lPSK8oG", "87.6", "360.0", "1649.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "fdc6uOvgoji", "CXw2yu5fodb", "109.1", "1137.0", "4179.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "at6UHUQatSo", "uYxK4wmcPqA", "173.0", "1532.0", "3551.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "at6UHUQatSo", "EYbopBOJWsW", "159.8", "2203.0", "5530.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "at6UHUQatSo", "RXL3lPSK8oG", "91.9", "1558.0", "6800.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "at6UHUQatSo", "tDZVQ1WtwpA", "174.5", "2150.0", "4943.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "at6UHUQatSo", "CXw2yu5fodb", "172.3", "5720.0", "13315.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "lc3eMKXaEfw", "uYxK4wmcPqA", "117.9", "609.0", "2072.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "lc3eMKXaEfw", "EYbopBOJWsW", "111.1", "733.0", "2646.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "lc3eMKXaEfw", "RXL3lPSK8oG", "43.5", "33.0", "304.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "lc3eMKXaEfw", "CXw2yu5fodb", "149.5", "765.0", "2053.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "qhqAxPSTUXp", "uYxK4wmcPqA", "75.4", "314.0", "1671.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "qhqAxPSTUXp", "EYbopBOJWsW", "73.7", "1614.0", "8788.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "qhqAxPSTUXp", "RXL3lPSK8oG", "104.0", "21.0", "81.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "qhqAxPSTUXp", "CXw2yu5fodb", "85.6", "589.0", "2759.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "jmIPBj66vD6", "uYxK4wmcPqA", "134.2", "671.0", "2005.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "jmIPBj66vD6", "EYbopBOJWsW", "134.1", "2044.0", "6113.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "jmIPBj66vD6", "RXL3lPSK8oG", "118.7", "74.0", "250.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "jmIPBj66vD6", "tDZVQ1WtwpA", "33.2", "41.0", "496.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q2", "jmIPBj66vD6", "CXw2yu5fodb", "112.1", "1122.0", "4014.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "jUb8gELQApl", "uYxK4wmcPqA", "79.6", "1570.0", "7827.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "jUb8gELQApl", "EYbopBOJWsW", "84.5", "953.0", "4476.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "jUb8gELQApl", "RXL3lPSK8oG", "140.3", "226.0", "639.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "jUb8gELQApl", "tDZVQ1WtwpA", "101.8", "128.0", "499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "jUb8gELQApl", "CXw2yu5fodb", "84.0", "1007.0", "4758.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "TEQlaapDQoK", "uYxK4wmcPqA", "110.3", "1261.0", "4536.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "TEQlaapDQoK", "EYbopBOJWsW", "118.5", "3646.0", "12207.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "TEQlaapDQoK", "CXw2yu5fodb", "99.7", "1205.0", "4793.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "eIQbndfxQMb", "uYxK4wmcPqA", "103.2", "390.0", "1499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "eIQbndfxQMb", "EYbopBOJWsW", "133.3", "3749.0", "11156.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "eIQbndfxQMb", "tDZVQ1WtwpA", "28.9", "94.0", "1289.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "eIQbndfxQMb", "CXw2yu5fodb", "166.1", "1215.0", "2902.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "Vth0fbpFcsO", "uYxK4wmcPqA", "69.6", "565.0", "3222.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "Vth0fbpFcsO", "EYbopBOJWsW", "67.1", "1359.0", "8030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "Vth0fbpFcsO", "RXL3lPSK8oG", "21.9", "74.0", "1340.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "Vth0fbpFcsO", "CXw2yu5fodb", "51.8", "415.0", "3181.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "PMa2VCrupOd", "uYxK4wmcPqA", "91.1", "686.0", "2987.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "PMa2VCrupOd", "EYbopBOJWsW", "88.4", "1685.0", "7562.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "PMa2VCrupOd", "RXL3lPSK8oG", "130.7", "193.0", "586.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "PMa2VCrupOd", "CXw2yu5fodb", "99.7", "463.0", "1843.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "O6uvpzGd5pu", "uYxK4wmcPqA", "82.2", "714.0", "3447.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "O6uvpzGd5pu", "EYbopBOJWsW", "99.4", "2381.0", "9501.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "O6uvpzGd5pu", "RXL3lPSK8oG", "90.5", "140.0", "614.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "O6uvpzGd5pu", "CXw2yu5fodb", "232.7", "4979.0", "8488.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "bL4ooGhyHRQ", "uYxK4wmcPqA", "105.8", "749.0", "2810.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "bL4ooGhyHRQ", "EYbopBOJWsW", "107.7", "1153.0", "4249.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "bL4ooGhyHRQ", "CXw2yu5fodb", "120.7", "1297.0", "4263.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "kJq2mPyFEHo", "uYxK4wmcPqA", "79.3", "1200.0", "6006.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "kJq2mPyFEHo", "EYbopBOJWsW", "92.8", "2490.0", "10643.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "kJq2mPyFEHo", "tDZVQ1WtwpA", "168.2", "128.0", "302.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "kJq2mPyFEHo", "CXw2yu5fodb", "77.9", "1380.0", "7030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "fdc6uOvgoji", "uYxK4wmcPqA", "89.4", "883.0", "3919.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "fdc6uOvgoji", "EYbopBOJWsW", "82.4", "2115.0", "10184.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "fdc6uOvgoji", "RXL3lPSK8oG", "60.4", "251.0", "1649.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "fdc6uOvgoji", "tDZVQ1WtwpA", "256.2", "246.0", "381.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "fdc6uOvgoji", "CXw2yu5fodb", "95.6", "1007.0", "4179.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "at6UHUQatSo", "uYxK4wmcPqA", "170.0", "1522.0", "3551.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "at6UHUQatSo", "EYbopBOJWsW", "141.5", "1973.0", "5530.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "at6UHUQatSo", "RXL3lPSK8oG", "76.5", "1311.0", "6800.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "at6UHUQatSo", "tDZVQ1WtwpA", "120.6", "1502.0", "4943.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "at6UHUQatSo", "CXw2yu5fodb", "136.5", "4581.0", "13315.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "lc3eMKXaEfw", "uYxK4wmcPqA", "127.9", "668.0", "2072.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "lc3eMKXaEfw", "EYbopBOJWsW", "125.8", "839.0", "2646.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "lc3eMKXaEfw", "RXL3lPSK8oG", "63.9", "49.0", "304.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "lc3eMKXaEfw", "CXw2yu5fodb", "104.5", "541.0", "2053.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "qhqAxPSTUXp", "uYxK4wmcPqA", "114.9", "484.0", "1671.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "qhqAxPSTUXp", "EYbopBOJWsW", "74.1", "1642.0", "8788.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "qhqAxPSTUXp", "RXL3lPSK8oG", "78.4", "16.0", "81.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "qhqAxPSTUXp", "CXw2yu5fodb", "88.0", "612.0", "2759.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "jmIPBj66vD6", "uYxK4wmcPqA", "152.4", "770.0", "2005.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "jmIPBj66vD6", "EYbopBOJWsW", "127.1", "1958.0", "6113.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "jmIPBj66vD6", "RXL3lPSK8oG", "58.7", "37.0", "250.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "jmIPBj66vD6", "tDZVQ1WtwpA", "25.6", "32.0", "496.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q3", "jmIPBj66vD6", "CXw2yu5fodb", "122.6", "1240.0", "4014.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "jUb8gELQApl", "uYxK4wmcPqA", "75.2", "1484.0", "7827.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "jUb8gELQApl", "EYbopBOJWsW", "61.7", "696.0", "4476.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "jUb8gELQApl", "RXL3lPSK8oG", "105.5", "170.0", "639.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "jUb8gELQApl", "tDZVQ1WtwpA", "123.2", "155.0", "499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "jUb8gELQApl", "CXw2yu5fodb", "77.6", "931.0", "4758.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "TEQlaapDQoK", "uYxK4wmcPqA", "63.0", "720.0", "4536.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "TEQlaapDQoK", "EYbopBOJWsW", "67.3", "2070.0", "12207.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "TEQlaapDQoK", "CXw2yu5fodb", "60.8", "735.0", "4793.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "eIQbndfxQMb", "uYxK4wmcPqA", "137.9", "521.0", "1499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "eIQbndfxQMb", "EYbopBOJWsW", "135.8", "3819.0", "11156.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "eIQbndfxQMb", "tDZVQ1WtwpA", "26.8", "87.0", "1289.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "eIQbndfxQMb", "CXw2yu5fodb", "171.0", "1251.0", "2902.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "Vth0fbpFcsO", "uYxK4wmcPqA", "28.9", "235.0", "3222.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "Vth0fbpFcsO", "EYbopBOJWsW", "24.9", "504.0", "8030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "Vth0fbpFcsO", "RXL3lPSK8oG", "45.9", "155.0", "1340.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "Vth0fbpFcsO", "CXw2yu5fodb", "27.1", "217.0", "3181.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "PMa2VCrupOd", "uYxK4wmcPqA", "103.9", "782.0", "2987.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "PMa2VCrupOd", "EYbopBOJWsW", "104.0", "1983.0", "7562.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "PMa2VCrupOd", "RXL3lPSK8oG", "169.3", "250.0", "586.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "PMa2VCrupOd", "CXw2yu5fodb", "108.7", "505.0", "1843.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "O6uvpzGd5pu", "uYxK4wmcPqA", "85.1", "739.0", "3447.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "O6uvpzGd5pu", "EYbopBOJWsW", "88.4", "2116.0", "9501.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "O6uvpzGd5pu", "RXL3lPSK8oG", "91.8", "142.0", "614.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "O6uvpzGd5pu", "CXw2yu5fodb", "221.4", "4736.0", "8488.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "bL4ooGhyHRQ", "EYbopBOJWsW", "2.0", "21.0", "4249.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "kJq2mPyFEHo", "uYxK4wmcPqA", "70.6", "1069.0", "6006.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "kJq2mPyFEHo", "EYbopBOJWsW", "95.9", "2572.0", "10643.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "kJq2mPyFEHo", "tDZVQ1WtwpA", "105.1", "80.0", "302.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "kJq2mPyFEHo", "CXw2yu5fodb", "90.7", "1607.0", "7030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "fdc6uOvgoji", "uYxK4wmcPqA", "59.9", "592.0", "3919.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "fdc6uOvgoji", "EYbopBOJWsW", "47.9", "1229.0", "10184.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "fdc6uOvgoji", "RXL3lPSK8oG", "44.8", "186.0", "1649.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "fdc6uOvgoji", "tDZVQ1WtwpA", "146.8", "141.0", "381.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "fdc6uOvgoji", "CXw2yu5fodb", "57.9", "610.0", "4179.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "at6UHUQatSo", "uYxK4wmcPqA", "147.1", "1317.0", "3551.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "at6UHUQatSo", "EYbopBOJWsW", "138.1", "1925.0", "5530.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "at6UHUQatSo", "RXL3lPSK8oG", "107.2", "1838.0", "6800.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "at6UHUQatSo", "tDZVQ1WtwpA", "99.4", "1239.0", "4943.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "at6UHUQatSo", "CXw2yu5fodb", "124.9", "4193.0", "13315.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "lc3eMKXaEfw", "uYxK4wmcPqA", "37.1", "194.0", "2072.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "lc3eMKXaEfw", "EYbopBOJWsW", "33.7", "225.0", "2646.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "lc3eMKXaEfw", "RXL3lPSK8oG", "22.2", "17.0", "304.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "lc3eMKXaEfw", "CXw2yu5fodb", "37.7", "195.0", "2053.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "qhqAxPSTUXp", "uYxK4wmcPqA", "56.0", "236.0", "1671.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "qhqAxPSTUXp", "EYbopBOJWsW", "33.0", "732.0", "8788.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "qhqAxPSTUXp", "RXL3lPSK8oG", "14.7", "3.0", "81.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "qhqAxPSTUXp", "CXw2yu5fodb", "41.0", "285.0", "2759.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "jmIPBj66vD6", "uYxK4wmcPqA", "120.3", "608.0", "2005.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "jmIPBj66vD6", "EYbopBOJWsW", "109.6", "1689.0", "6113.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "jmIPBj66vD6", "RXL3lPSK8oG", "82.5", "52.0", "250.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "jmIPBj66vD6", "tDZVQ1WtwpA", "42.4", "53.0", "496.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "2021Q4", "jmIPBj66vD6", "CXw2yu5fodb", "105.9", "1071.0", "4014.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "jUb8gELQApl", "uYxK4wmcPqA", "81.0", "1564.0", "7827.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "jUb8gELQApl", "EYbopBOJWsW", "90.3", "997.0", "4476.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "jUb8gELQApl", "RXL3lPSK8oG", "57.8", "91.0", "639.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "jUb8gELQApl", "tDZVQ1WtwpA", "70.7", "87.0", "499.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "jUb8gELQApl", "CXw2yu5fodb", "89.1", "1045.0", "4758.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "TEQlaapDQoK", "uYxK4wmcPqA", "67.5", "755.0", "4536.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "TEQlaapDQoK", "EYbopBOJWsW", "80.8", "2433.0", "12207.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "TEQlaapDQoK", "CXw2yu5fodb", "75.6", "893.0", "4793.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "eIQbndfxQMb", "uYxK4wmcPqA", "72.8", "269.0", "1499.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "eIQbndfxQMb", "EYbopBOJWsW", "92.0", "2530.0", "11156.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "eIQbndfxQMb", "tDZVQ1WtwpA", "28.3", "90.0", "1289.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "eIQbndfxQMb", "CXw2yu5fodb", "89.6", "641.0", "2902.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "Vth0fbpFcsO", "uYxK4wmcPqA", "59.2", "470.0", "3222.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "Vth0fbpFcsO", "EYbopBOJWsW", "62.7", "1241.0", "8030.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "Vth0fbpFcsO", "RXL3lPSK8oG", "0.91", "3.0", "1340.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "Vth0fbpFcsO", "tDZVQ1WtwpA", "136.1", "419.0", "1249.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "Vth0fbpFcsO", "CXw2yu5fodb", "73.4", "576.0", "3181.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "PMa2VCrupOd", "uYxK4wmcPqA", "96.1", "708.0", "2987.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "PMa2VCrupOd", "EYbopBOJWsW", "95.5", "1781.0", "7562.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "PMa2VCrupOd", "RXL3lPSK8oG", "86.5", "125.0", "586.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "PMa2VCrupOd", "CXw2yu5fodb", "92.6", "421.0", "1843.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "O6uvpzGd5pu", "uYxK4wmcPqA", "107.4", "913.0", "3447.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "O6uvpzGd5pu", "EYbopBOJWsW", "139.6", "3271.0", "9501.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "O6uvpzGd5pu", "RXL3lPSK8oG", "95.1", "144.0", "614.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "O6uvpzGd5pu", "CXw2yu5fodb", "121.1", "2535.0", "8488.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "bL4ooGhyHRQ", "uYxK4wmcPqA", "85.7", "594.0", "2810.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "bL4ooGhyHRQ", "EYbopBOJWsW", "112.1", "1174.0", "4249.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "bL4ooGhyHRQ", "CXw2yu5fodb", "87.5", "920.0", "4263.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "kJq2mPyFEHo", "uYxK4wmcPqA", "110.9", "1642.0", "6006.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "kJq2mPyFEHo", "EYbopBOJWsW", "120.2", "3155.0", "10643.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "kJq2mPyFEHo", "tDZVQ1WtwpA", "151.7", "113.0", "302.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "kJq2mPyFEHo", "CXw2yu5fodb", "103.3", "1791.0", "7030.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "fdc6uOvgoji", "uYxK4wmcPqA", "78.0", "754.0", "3919.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "fdc6uOvgoji", "EYbopBOJWsW", "68.2", "1713.0", "10184.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "fdc6uOvgoji", "RXL3lPSK8oG", "52.1", "212.0", "1649.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "fdc6uOvgoji", "tDZVQ1WtwpA", "56.4", "53.0", "381.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "fdc6uOvgoji", "CXw2yu5fodb", "88.5", "912.0", "4179.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "at6UHUQatSo", "uYxK4wmcPqA", "92.5", "810.0", "3551.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "at6UHUQatSo", "EYbopBOJWsW", "96.7", "1319.0", "5530.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "at6UHUQatSo", "RXL3lPSK8oG", "83.6", "1402.0", "6800.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "at6UHUQatSo", "tDZVQ1WtwpA", "171.6", "2092.0", "4943.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "at6UHUQatSo", "CXw2yu5fodb", "95.5", "3136.0", "13315.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "lc3eMKXaEfw", "uYxK4wmcPqA", "80.6", "412.0", "2072.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "lc3eMKXaEfw", "EYbopBOJWsW", "90.3", "589.0", "2646.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "lc3eMKXaEfw", "RXL3lPSK8oG", "66.7", "50.0", "304.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "lc3eMKXaEfw", "CXw2yu5fodb", "96.6", "489.0", "2053.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "qhqAxPSTUXp", "uYxK4wmcPqA", "61.4", "253.0", "1671.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "qhqAxPSTUXp", "EYbopBOJWsW", "61.6", "1335.0", "8788.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "qhqAxPSTUXp", "RXL3lPSK8oG", "120.2", "24.0", "81.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "qhqAxPSTUXp", "CXw2yu5fodb", "60.1", "409.0", "2759.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "jmIPBj66vD6", "uYxK4wmcPqA", "114.1", "564.0", "2005.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "jmIPBj66vD6", "EYbopBOJWsW", "111.4", "1679.0", "6113.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "jmIPBj66vD6", "RXL3lPSK8oG", "124.9", "77.0", "250.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "jmIPBj66vD6", "tDZVQ1WtwpA", "76.9", "94.0", "496.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q1", "jmIPBj66vD6", "CXw2yu5fodb", "122.3", "1210.0", "4014.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "jUb8gELQApl", "uYxK4wmcPqA", "83.2", "1624.0", "7827.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "jUb8gELQApl", "EYbopBOJWsW", "103.1", "1151.0", "4476.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "jUb8gELQApl", "RXL3lPSK8oG", "202.7", "323.0", "639.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "jUb8gELQApl", "tDZVQ1WtwpA", "73.1", "91.0", "499.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "jUb8gELQApl", "CXw2yu5fodb", "90.9", "1078.0", "4758.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "TEQlaapDQoK", "uYxK4wmcPqA", "80.0", "905.0", "4536.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "TEQlaapDQoK", "EYbopBOJWsW", "102.2", "3109.0", "12207.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "TEQlaapDQoK", "CXw2yu5fodb", "80.8", "965.0", "4793.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "eIQbndfxQMb", "uYxK4wmcPqA", "99.5", "372.0", "1499.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "eIQbndfxQMb", "EYbopBOJWsW", "102.0", "2837.0", "11156.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "eIQbndfxQMb", "tDZVQ1WtwpA", "31.4", "101.0", "1289.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "eIQbndfxQMb", "CXw2yu5fodb", "111.0", "803.0", "2902.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "Vth0fbpFcsO", "uYxK4wmcPqA", "66.6", "535.0", "3222.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "Vth0fbpFcsO", "EYbopBOJWsW", "63.7", "1275.0", "8030.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "Vth0fbpFcsO", "RXL3lPSK8oG", "19.5", "65.0", "1340.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "Vth0fbpFcsO", "tDZVQ1WtwpA", "74.8", "233.0", "1249.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "Vth0fbpFcsO", "CXw2yu5fodb", "85.9", "681.0", "3181.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "PMa2VCrupOd", "uYxK4wmcPqA", "86.9", "647.0", "2987.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "PMa2VCrupOd", "EYbopBOJWsW", "93.5", "1762.0", "7562.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "PMa2VCrupOd", "RXL3lPSK8oG", "130.7", "191.0", "586.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "PMa2VCrupOd", "CXw2yu5fodb", "104.9", "482.0", "1843.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "O6uvpzGd5pu", "uYxK4wmcPqA", "80.3", "690.0", "3447.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "O6uvpzGd5pu", "EYbopBOJWsW", "185.8", "4400.0", "9501.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "O6uvpzGd5pu", "RXL3lPSK8oG", "129.3", "198.0", "614.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "O6uvpzGd5pu", "CXw2yu5fodb", "123.2", "2607.0", "8488.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "bL4ooGhyHRQ", "uYxK4wmcPqA", "84.8", "594.0", "2810.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "bL4ooGhyHRQ", "EYbopBOJWsW", "103.9", "1101.0", "4249.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "bL4ooGhyHRQ", "CXw2yu5fodb", "102.9", "1094.0", "4263.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "kJq2mPyFEHo", "uYxK4wmcPqA", "131.2", "1965.0", "6006.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "kJq2mPyFEHo", "EYbopBOJWsW", "137.7", "3655.0", "10643.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "kJq2mPyFEHo", "tDZVQ1WtwpA", "158.0", "119.0", "302.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "kJq2mPyFEHo", "CXw2yu5fodb", "125.2", "2195.0", "7030.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "fdc6uOvgoji", "uYxK4wmcPqA", "91.2", "891.0", "3919.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "fdc6uOvgoji", "EYbopBOJWsW", "88.2", "2239.0", "10184.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "fdc6uOvgoji", "RXL3lPSK8oG", "62.5", "257.0", "1649.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "fdc6uOvgoji", "CXw2yu5fodb", "95.7", "997.0", "4179.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "at6UHUQatSo", "uYxK4wmcPqA", "116.9", "1035.0", "3551.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "at6UHUQatSo", "EYbopBOJWsW", "126.0", "1737.0", "5530.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "at6UHUQatSo", "RXL3lPSK8oG", "79.0", "1340.0", "6800.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "at6UHUQatSo", "tDZVQ1WtwpA", "285.7", "3521.0", "4943.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "at6UHUQatSo", "CXw2yu5fodb", "133.4", "4427.0", "13315.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "lc3eMKXaEfw", "uYxK4wmcPqA", "96.6", "499.0", "2072.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "lc3eMKXaEfw", "EYbopBOJWsW", "122.9", "811.0", "2646.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "lc3eMKXaEfw", "RXL3lPSK8oG", "43.5", "33.0", "304.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "lc3eMKXaEfw", "CXw2yu5fodb", "126.2", "646.0", "2053.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "qhqAxPSTUXp", "uYxK4wmcPqA", "64.6", "269.0", "1671.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "qhqAxPSTUXp", "EYbopBOJWsW", "56.5", "1237.0", "8788.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "qhqAxPSTUXp", "RXL3lPSK8oG", "44.6", "9.0", "81.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "qhqAxPSTUXp", "CXw2yu5fodb", "64.1", "441.0", "2759.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "jmIPBj66vD6", "uYxK4wmcPqA", "118.2", "591.0", "2005.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "jmIPBj66vD6", "EYbopBOJWsW", "120.2", "1832.0", "6113.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "jmIPBj66vD6", "RXL3lPSK8oG", "126.7", "79.0", "250.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "jmIPBj66vD6", "tDZVQ1WtwpA", "49.3", "61.0", "496.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q2", "jmIPBj66vD6", "CXw2yu5fodb", "106.2", "1063.0", "4014.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "jUb8gELQApl", "uYxK4wmcPqA", "78.3", "1544.0", "7827.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "jUb8gELQApl", "EYbopBOJWsW", "87.8", "990.0", "4476.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "jUb8gELQApl", "RXL3lPSK8oG", "176.9", "285.0", "639.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "jUb8gELQApl", "tDZVQ1WtwpA", "69.2", "87.0", "499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "jUb8gELQApl", "CXw2yu5fodb", "79.5", "953.0", "4758.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "TEQlaapDQoK", "uYxK4wmcPqA", "93.5", "1069.0", "4536.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "TEQlaapDQoK", "EYbopBOJWsW", "98.4", "3027.0", "12207.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "TEQlaapDQoK", "CXw2yu5fodb", "98.9", "1195.0", "4793.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "eIQbndfxQMb", "uYxK4wmcPqA", "106.4", "402.0", "1499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "eIQbndfxQMb", "EYbopBOJWsW", "102.5", "2881.0", "11156.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "eIQbndfxQMb", "tDZVQ1WtwpA", "21.9", "71.0", "1289.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "eIQbndfxQMb", "CXw2yu5fodb", "105.7", "773.0", "2902.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "Vth0fbpFcsO", "uYxK4wmcPqA", "60.8", "494.0", "3222.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "Vth0fbpFcsO", "EYbopBOJWsW", "65.9", "1334.0", "8030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "Vth0fbpFcsO", "RXL3lPSK8oG", "18.1", "61.0", "1340.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "Vth0fbpFcsO", "CXw2yu5fodb", "50.4", "404.0", "3181.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "PMa2VCrupOd", "uYxK4wmcPqA", "97.6", "735.0", "2987.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "PMa2VCrupOd", "EYbopBOJWsW", "84.9", "1619.0", "7562.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "PMa2VCrupOd", "RXL3lPSK8oG", "119.8", "177.0", "586.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "PMa2VCrupOd", "CXw2yu5fodb", "85.9", "399.0", "1843.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "O6uvpzGd5pu", "uYxK4wmcPqA", "83.1", "722.0", "3447.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "O6uvpzGd5pu", "EYbopBOJWsW", "150.9", "3614.0", "9501.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "O6uvpzGd5pu", "RXL3lPSK8oG", "113.1", "175.0", "614.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "O6uvpzGd5pu", "CXw2yu5fodb", "135.4", "2896.0", "8488.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "bL4ooGhyHRQ", "uYxK4wmcPqA", "118.3", "838.0", "2810.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "bL4ooGhyHRQ", "EYbopBOJWsW", "109.1", "1168.0", "4249.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "bL4ooGhyHRQ", "CXw2yu5fodb", "89.5", "962.0", "4263.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "kJq2mPyFEHo", "uYxK4wmcPqA", "91.5", "1385.0", "6006.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "kJq2mPyFEHo", "EYbopBOJWsW", "123.7", "3318.0", "10643.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "kJq2mPyFEHo", "tDZVQ1WtwpA", "137.9", "105.0", "302.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "kJq2mPyFEHo", "CXw2yu5fodb", "89.8", "1591.0", "7030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "fdc6uOvgoji", "uYxK4wmcPqA", "75.5", "746.0", "3919.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "fdc6uOvgoji", "EYbopBOJWsW", "69.9", "1794.0", "10184.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "fdc6uOvgoji", "RXL3lPSK8oG", "61.4", "255.0", "1649.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "fdc6uOvgoji", "tDZVQ1WtwpA", "183.3", "176.0", "381.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "fdc6uOvgoji", "CXw2yu5fodb", "80.4", "847.0", "4179.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "at6UHUQatSo", "uYxK4wmcPqA", "141.6", "1267.0", "3551.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "at6UHUQatSo", "EYbopBOJWsW", "123.0", "1715.0", "5530.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "at6UHUQatSo", "RXL3lPSK8oG", "67.4", "1155.0", "6800.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "at6UHUQatSo", "tDZVQ1WtwpA", "139.1", "1733.0", "4943.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "at6UHUQatSo", "CXw2yu5fodb", "126.6", "4248.0", "13315.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "lc3eMKXaEfw", "uYxK4wmcPqA", "123.5", "645.0", "2072.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "lc3eMKXaEfw", "EYbopBOJWsW", "139.7", "932.0", "2646.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "lc3eMKXaEfw", "RXL3lPSK8oG", "41.8", "32.0", "304.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "lc3eMKXaEfw", "CXw2yu5fodb", "98.6", "510.0", "2053.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "qhqAxPSTUXp", "uYxK4wmcPqA", "101.1", "426.0", "1671.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "qhqAxPSTUXp", "EYbopBOJWsW", "60.3", "1336.0", "8788.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "qhqAxPSTUXp", "RXL3lPSK8oG", "39.2", "8.0", "81.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "qhqAxPSTUXp", "CXw2yu5fodb", "76.9", "535.0", "2759.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "jmIPBj66vD6", "uYxK4wmcPqA", "132.0", "667.0", "2005.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "jmIPBj66vD6", "EYbopBOJWsW", "117.0", "1802.0", "6113.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "jmIPBj66vD6", "RXL3lPSK8oG", "52.4", "33.0", "250.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "jmIPBj66vD6", "tDZVQ1WtwpA", "32.0", "40.0", "496.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q3", "jmIPBj66vD6", "CXw2yu5fodb", "109.0", "1103.0", "4014.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "jUb8gELQApl", "uYxK4wmcPqA", "82.4", "1626.0", "7827.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "jUb8gELQApl", "EYbopBOJWsW", "80.5", "908.0", "4476.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "jUb8gELQApl", "RXL3lPSK8oG", "134.7", "217.0", "639.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "jUb8gELQApl", "tDZVQ1WtwpA", "56.4", "71.0", "499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "jUb8gELQApl", "CXw2yu5fodb", "73.5", "881.0", "4758.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "TEQlaapDQoK", "uYxK4wmcPqA", "59.1", "676.0", "4536.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "TEQlaapDQoK", "EYbopBOJWsW", "58.5", "1801.0", "12207.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "TEQlaapDQoK", "CXw2yu5fodb", "64.4", "778.0", "4793.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "eIQbndfxQMb", "uYxK4wmcPqA", "117.8", "445.0", "1499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "eIQbndfxQMb", "EYbopBOJWsW", "115.3", "3243.0", "11156.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "eIQbndfxQMb", "tDZVQ1WtwpA", "21.9", "71.0", "1289.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "eIQbndfxQMb", "CXw2yu5fodb", "125.6", "919.0", "2902.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "Vth0fbpFcsO", "uYxK4wmcPqA", "31.2", "253.0", "3222.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "Vth0fbpFcsO", "EYbopBOJWsW", "24.2", "490.0", "8030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "Vth0fbpFcsO", "RXL3lPSK8oG", "32.3", "109.0", "1340.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "Vth0fbpFcsO", "CXw2yu5fodb", "23.2", "186.0", "3181.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "PMa2VCrupOd", "uYxK4wmcPqA", "93.9", "707.0", "2987.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "PMa2VCrupOd", "EYbopBOJWsW", "92.4", "1762.0", "7562.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "PMa2VCrupOd", "RXL3lPSK8oG", "132.0", "195.0", "586.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "PMa2VCrupOd", "CXw2yu5fodb", "83.5", "388.0", "1843.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "O6uvpzGd5pu", "uYxK4wmcPqA", "84.5", "734.0", "3447.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "O6uvpzGd5pu", "EYbopBOJWsW", "118.5", "2838.0", "9501.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "O6uvpzGd5pu", "RXL3lPSK8oG", "86.6", "134.0", "614.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "O6uvpzGd5pu", "CXw2yu5fodb", "141.8", "3033.0", "8488.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "bL4ooGhyHRQ", "EYbopBOJWsW", "3.1", "33.0", "4249.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "kJq2mPyFEHo", "uYxK4wmcPqA", "87.7", "1327.0", "6006.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "kJq2mPyFEHo", "EYbopBOJWsW", "128.3", "3442.0", "10643.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "kJq2mPyFEHo", "tDZVQ1WtwpA", "64.4", "49.0", "302.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "kJq2mPyFEHo", "CXw2yu5fodb", "122.0", "2162.0", "7030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "fdc6uOvgoji", "uYxK4wmcPqA", "56.2", "555.0", "3919.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "fdc6uOvgoji", "EYbopBOJWsW", "42.7", "1096.0", "10184.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "fdc6uOvgoji", "RXL3lPSK8oG", "38.3", "159.0", "1649.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "fdc6uOvgoji", "tDZVQ1WtwpA", "208.3", "200.0", "381.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "fdc6uOvgoji", "CXw2yu5fodb", "56.2", "592.0", "4179.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "at6UHUQatSo", "uYxK4wmcPqA", "121.6", "1088.0", "3551.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "at6UHUQatSo", "EYbopBOJWsW", "121.6", "1695.0", "5530.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "at6UHUQatSo", "RXL3lPSK8oG", "80.7", "1383.0", "6800.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "at6UHUQatSo", "tDZVQ1WtwpA", "213.7", "2663.0", "4943.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "at6UHUQatSo", "CXw2yu5fodb", "93.7", "3145.0", "13315.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "lc3eMKXaEfw", "uYxK4wmcPqA", "39.3", "205.0", "2072.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "lc3eMKXaEfw", "EYbopBOJWsW", "40.6", "271.0", "2646.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "lc3eMKXaEfw", "RXL3lPSK8oG", "15.7", "12.0", "304.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "lc3eMKXaEfw", "CXw2yu5fodb", "27.8", "144.0", "2053.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "qhqAxPSTUXp", "uYxK4wmcPqA", "34.9", "147.0", "1671.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "qhqAxPSTUXp", "EYbopBOJWsW", "26.5", "588.0", "8788.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "qhqAxPSTUXp", "RXL3lPSK8oG", "19.6", "4.0", "81.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "qhqAxPSTUXp", "CXw2yu5fodb", "30.6", "213.0", "2759.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "jmIPBj66vD6", "uYxK4wmcPqA", "93.0", "470.0", "2005.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "jmIPBj66vD6", "EYbopBOJWsW", "102.4", "1578.0", "6113.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "jmIPBj66vD6", "RXL3lPSK8oG", "58.7", "37.0", "250.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "jmIPBj66vD6", "tDZVQ1WtwpA", "34.4", "43.0", "496.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("OdiHJayrsKo", "2021Q4", "jmIPBj66vD6", "CXw2yu5fodb", "102.5", "1037.0", "4014.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "jUb8gELQApl", "uYxK4wmcPqA", "62.4", "1205.0", "7827.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "jUb8gELQApl", "EYbopBOJWsW", "69.5", "767.0", "4476.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "jUb8gELQApl", "RXL3lPSK8oG", "38.7", "61.0", "639.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "jUb8gELQApl", "tDZVQ1WtwpA", "56.1", "69.0", "499.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "jUb8gELQApl", "CXw2yu5fodb", "84.1", "987.0", "4758.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "TEQlaapDQoK", "uYxK4wmcPqA", "47.0", "526.0", "4536.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "TEQlaapDQoK", "EYbopBOJWsW", "42.4", "1277.0", "12207.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "TEQlaapDQoK", "CXw2yu5fodb", "56.9", "673.0", "4793.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "eIQbndfxQMb", "uYxK4wmcPqA", "39.8", "147.0", "1499.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "eIQbndfxQMb", "EYbopBOJWsW", "54.2", "1492.0", "11156.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "eIQbndfxQMb", "tDZVQ1WtwpA", "14.5", "46.0", "1289.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "eIQbndfxQMb", "CXw2yu5fodb", "47.0", "336.0", "2902.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "Vth0fbpFcsO", "uYxK4wmcPqA", "34.5", "274.0", "3222.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "Vth0fbpFcsO", "EYbopBOJWsW", "42.6", "843.0", "8030.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "Vth0fbpFcsO", "RXL3lPSK8oG", "1.2", "4.0", "1340.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "Vth0fbpFcsO", "tDZVQ1WtwpA", "70.1", "216.0", "1249.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "Vth0fbpFcsO", "CXw2yu5fodb", "53.3", "418.0", "3181.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "PMa2VCrupOd", "uYxK4wmcPqA", "69.9", "515.0", "2987.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "PMa2VCrupOd", "EYbopBOJWsW", "62.9", "1173.0", "7562.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "PMa2VCrupOd", "RXL3lPSK8oG", "88.6", "128.0", "586.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "PMa2VCrupOd", "CXw2yu5fodb", "58.3", "265.0", "1843.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "O6uvpzGd5pu", "uYxK4wmcPqA", "74.1", "630.0", "3447.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "O6uvpzGd5pu", "EYbopBOJWsW", "82.6", "1936.0", "9501.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "O6uvpzGd5pu", "RXL3lPSK8oG", "21.8", "33.0", "614.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "O6uvpzGd5pu", "CXw2yu5fodb", "71.1", "1488.0", "8488.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "bL4ooGhyHRQ", "uYxK4wmcPqA", "68.8", "477.0", "2810.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "bL4ooGhyHRQ", "EYbopBOJWsW", "83.6", "876.0", "4249.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "bL4ooGhyHRQ", "CXw2yu5fodb", "61.6", "647.0", "4263.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "kJq2mPyFEHo", "uYxK4wmcPqA", "68.2", "1010.0", "6006.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "kJq2mPyFEHo", "EYbopBOJWsW", "93.8", "2461.0", "10643.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "kJq2mPyFEHo", "tDZVQ1WtwpA", "151.7", "113.0", "302.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "kJq2mPyFEHo", "CXw2yu5fodb", "71.7", "1242.0", "7030.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "fdc6uOvgoji", "uYxK4wmcPqA", "51.4", "497.0", "3919.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "fdc6uOvgoji", "EYbopBOJWsW", "50.8", "1275.0", "10184.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "fdc6uOvgoji", "RXL3lPSK8oG", "30.5", "124.0", "1649.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "fdc6uOvgoji", "tDZVQ1WtwpA", "39.4", "37.0", "381.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "fdc6uOvgoji", "CXw2yu5fodb", "62.3", "642.0", "4179.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "at6UHUQatSo", "uYxK4wmcPqA", "72.2", "632.0", "3551.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "at6UHUQatSo", "EYbopBOJWsW", "70.8", "965.0", "5530.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "at6UHUQatSo", "RXL3lPSK8oG", "61.3", "1027.0", "6800.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "at6UHUQatSo", "tDZVQ1WtwpA", "55.2", "673.0", "4943.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "at6UHUQatSo", "CXw2yu5fodb", "69.1", "2268.0", "13315.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "lc3eMKXaEfw", "uYxK4wmcPqA", "65.2", "333.0", "2072.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "lc3eMKXaEfw", "EYbopBOJWsW", "69.4", "453.0", "2646.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "lc3eMKXaEfw", "RXL3lPSK8oG", "57.4", "43.0", "304.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "lc3eMKXaEfw", "CXw2yu5fodb", "58.9", "298.0", "2053.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "qhqAxPSTUXp", "uYxK4wmcPqA", "46.6", "192.0", "1671.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "qhqAxPSTUXp", "EYbopBOJWsW", "41.9", "908.0", "8788.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "qhqAxPSTUXp", "RXL3lPSK8oG", "75.1", "15.0", "81.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "qhqAxPSTUXp", "CXw2yu5fodb", "38.4", "261.0", "2759.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "jmIPBj66vD6", "uYxK4wmcPqA", "91.2", "451.0", "2005.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "jmIPBj66vD6", "EYbopBOJWsW", "90.8", "1369.0", "6113.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "jmIPBj66vD6", "RXL3lPSK8oG", "134.6", "83.0", "250.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "jmIPBj66vD6", "tDZVQ1WtwpA", "37.6", "46.0", "496.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q1", "jmIPBj66vD6", "CXw2yu5fodb", "117.1", "1159.0", "4014.0",
            "405.6", "36500", "90"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "jUb8gELQApl", "uYxK4wmcPqA", "68.5", "1337.0", "7827.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "jUb8gELQApl", "EYbopBOJWsW", "87.9", "981.0", "4476.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "jUb8gELQApl", "RXL3lPSK8oG", "41.4", "66.0", "639.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "jUb8gELQApl", "tDZVQ1WtwpA", "50.6", "63.0", "499.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "jUb8gELQApl", "CXw2yu5fodb", "87.4", "1037.0", "4758.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "TEQlaapDQoK", "uYxK4wmcPqA", "51.6", "584.0", "4536.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "TEQlaapDQoK", "EYbopBOJWsW", "56.5", "1721.0", "12207.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "TEQlaapDQoK", "CXw2yu5fodb", "57.9", "692.0", "4793.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "eIQbndfxQMb", "uYxK4wmcPqA", "66.1", "247.0", "1499.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "eIQbndfxQMb", "EYbopBOJWsW", "64.0", "1781.0", "11156.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "eIQbndfxQMb", "tDZVQ1WtwpA", "17.1", "55.0", "1289.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "eIQbndfxQMb", "CXw2yu5fodb", "63.4", "459.0", "2902.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "Vth0fbpFcsO", "uYxK4wmcPqA", "56.6", "455.0", "3222.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "Vth0fbpFcsO", "EYbopBOJWsW", "47.5", "951.0", "8030.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "Vth0fbpFcsO", "RXL3lPSK8oG", "10.2", "34.0", "1340.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "Vth0fbpFcsO", "tDZVQ1WtwpA", "33.7", "105.0", "1249.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "Vth0fbpFcsO", "CXw2yu5fodb", "62.4", "495.0", "3181.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "PMa2VCrupOd", "uYxK4wmcPqA", "71.7", "534.0", "2987.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "PMa2VCrupOd", "EYbopBOJWsW", "62.3", "1174.0", "7562.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "PMa2VCrupOd", "RXL3lPSK8oG", "90.3", "132.0", "586.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "PMa2VCrupOd", "CXw2yu5fodb", "69.9", "321.0", "1843.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "O6uvpzGd5pu", "uYxK4wmcPqA", "70.5", "606.0", "3447.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "O6uvpzGd5pu", "EYbopBOJWsW", "94.4", "2235.0", "9501.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "O6uvpzGd5pu", "RXL3lPSK8oG", "51.6", "79.0", "614.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "O6uvpzGd5pu", "CXw2yu5fodb", "85.8", "1815.0", "8488.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "bL4ooGhyHRQ", "uYxK4wmcPqA", "69.4", "486.0", "2810.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "bL4ooGhyHRQ", "EYbopBOJWsW", "76.2", "807.0", "4249.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "bL4ooGhyHRQ", "CXw2yu5fodb", "70.8", "753.0", "4263.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "kJq2mPyFEHo", "uYxK4wmcPqA", "87.6", "1311.0", "6006.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "kJq2mPyFEHo", "EYbopBOJWsW", "109.8", "2914.0", "10643.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "kJq2mPyFEHo", "tDZVQ1WtwpA", "164.7", "124.0", "302.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "kJq2mPyFEHo", "CXw2yu5fodb", "98.5", "1727.0", "7030.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "fdc6uOvgoji", "uYxK4wmcPqA", "59.4", "580.0", "3919.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "fdc6uOvgoji", "EYbopBOJWsW", "64.9", "1648.0", "10184.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "fdc6uOvgoji", "RXL3lPSK8oG", "36.0", "148.0", "1649.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "fdc6uOvgoji", "CXw2yu5fodb", "64.1", "668.0", "4179.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "at6UHUQatSo", "uYxK4wmcPqA", "69.4", "614.0", "3551.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "at6UHUQatSo", "EYbopBOJWsW", "88.6", "1221.0", "5530.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "at6UHUQatSo", "RXL3lPSK8oG", "54.2", "919.0", "6800.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "at6UHUQatSo", "tDZVQ1WtwpA", "91.1", "1123.0", "4943.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "at6UHUQatSo", "CXw2yu5fodb", "81.6", "2708.0", "13315.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "lc3eMKXaEfw", "uYxK4wmcPqA", "61.9", "320.0", "2072.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "lc3eMKXaEfw", "EYbopBOJWsW", "64.6", "426.0", "2646.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "lc3eMKXaEfw", "RXL3lPSK8oG", "31.7", "24.0", "304.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "lc3eMKXaEfw", "CXw2yu5fodb", "76.0", "389.0", "2053.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "qhqAxPSTUXp", "uYxK4wmcPqA", "48.0", "200.0", "1671.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "qhqAxPSTUXp", "EYbopBOJWsW", "43.5", "952.0", "8788.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "qhqAxPSTUXp", "RXL3lPSK8oG", "19.8", "4.0", "81.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "qhqAxPSTUXp", "CXw2yu5fodb", "44.9", "309.0", "2759.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "jmIPBj66vD6", "uYxK4wmcPqA", "93.0", "465.0", "2005.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "jmIPBj66vD6", "EYbopBOJWsW", "104.9", "1599.0", "6113.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "jmIPBj66vD6", "RXL3lPSK8oG", "144.4", "90.0", "250.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "jmIPBj66vD6", "tDZVQ1WtwpA", "35.6", "44.0", "496.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q2", "jmIPBj66vD6", "CXw2yu5fodb", "99.0", "991.0", "4014.0",
            "401.1", "36500", "91"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "jUb8gELQApl", "uYxK4wmcPqA", "69.3", "1368.0", "7827.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "jUb8gELQApl", "EYbopBOJWsW", "75.2", "848.0", "4476.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "jUb8gELQApl", "RXL3lPSK8oG", "90.0", "145.0", "639.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "jUb8gELQApl", "tDZVQ1WtwpA", "48.5", "61.0", "499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "jUb8gELQApl", "CXw2yu5fodb", "72.5", "869.0", "4758.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "TEQlaapDQoK", "uYxK4wmcPqA", "60.8", "695.0", "4536.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "TEQlaapDQoK", "EYbopBOJWsW", "53.8", "1656.0", "12207.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "TEQlaapDQoK", "CXw2yu5fodb", "62.5", "755.0", "4793.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "eIQbndfxQMb", "uYxK4wmcPqA", "70.1", "265.0", "1499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "eIQbndfxQMb", "EYbopBOJWsW", "61.9", "1740.0", "11156.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "eIQbndfxQMb", "tDZVQ1WtwpA", "15.4", "50.0", "1289.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "eIQbndfxQMb", "CXw2yu5fodb", "47.7", "349.0", "2902.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "Vth0fbpFcsO", "uYxK4wmcPqA", "46.5", "378.0", "3222.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "Vth0fbpFcsO", "EYbopBOJWsW", "50.6", "1025.0", "8030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "Vth0fbpFcsO", "RXL3lPSK8oG", "11.5", "39.0", "1340.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "Vth0fbpFcsO", "CXw2yu5fodb", "45.0", "361.0", "3181.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "PMa2VCrupOd", "uYxK4wmcPqA", "68.9", "519.0", "2987.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "PMa2VCrupOd", "EYbopBOJWsW", "51.9", "990.0", "7562.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "PMa2VCrupOd", "RXL3lPSK8oG", "120.5", "178.0", "586.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "PMa2VCrupOd", "CXw2yu5fodb", "70.4", "327.0", "1843.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "O6uvpzGd5pu", "uYxK4wmcPqA", "71.4", "620.0", "3447.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "O6uvpzGd5pu", "EYbopBOJWsW", "90.0", "2156.0", "9501.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "O6uvpzGd5pu", "RXL3lPSK8oG", "97.6", "151.0", "614.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "O6uvpzGd5pu", "CXw2yu5fodb", "92.8", "1986.0", "8488.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "bL4ooGhyHRQ", "uYxK4wmcPqA", "80.5", "570.0", "2810.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "bL4ooGhyHRQ", "EYbopBOJWsW", "84.6", "906.0", "4249.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "bL4ooGhyHRQ", "CXw2yu5fodb", "73.7", "792.0", "4263.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "kJq2mPyFEHo", "uYxK4wmcPqA", "66.8", "1011.0", "6006.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "kJq2mPyFEHo", "EYbopBOJWsW", "98.5", "2642.0", "10643.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "kJq2mPyFEHo", "tDZVQ1WtwpA", "93.3", "71.0", "302.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "kJq2mPyFEHo", "CXw2yu5fodb", "92.2", "1633.0", "7030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "fdc6uOvgoji", "uYxK4wmcPqA", "59.7", "590.0", "3919.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "fdc6uOvgoji", "EYbopBOJWsW", "53.1", "1362.0", "10184.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "fdc6uOvgoji", "RXL3lPSK8oG", "48.6", "202.0", "1649.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "fdc6uOvgoji", "tDZVQ1WtwpA", "313.4", "301.0", "381.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "fdc6uOvgoji", "CXw2yu5fodb", "51.7", "545.0", "4179.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "at6UHUQatSo", "uYxK4wmcPqA", "101.2", "906.0", "3551.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "at6UHUQatSo", "EYbopBOJWsW", "96.5", "1345.0", "5530.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "at6UHUQatSo", "RXL3lPSK8oG", "53.6", "919.0", "6800.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "at6UHUQatSo", "tDZVQ1WtwpA", "77.1", "961.0", "4943.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "at6UHUQatSo", "CXw2yu5fodb", "96.5", "3237.0", "13315.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "lc3eMKXaEfw", "uYxK4wmcPqA", "91.3", "477.0", "2072.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "lc3eMKXaEfw", "EYbopBOJWsW", "84.3", "562.0", "2646.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "lc3eMKXaEfw", "RXL3lPSK8oG", "41.8", "32.0", "304.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "lc3eMKXaEfw", "CXw2yu5fodb", "86.4", "447.0", "2053.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "qhqAxPSTUXp", "uYxK4wmcPqA", "73.1", "308.0", "1671.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "qhqAxPSTUXp", "EYbopBOJWsW", "43.6", "965.0", "8788.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "qhqAxPSTUXp", "RXL3lPSK8oG", "49.0", "10.0", "81.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "qhqAxPSTUXp", "CXw2yu5fodb", "63.3", "440.0", "2759.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "jmIPBj66vD6", "uYxK4wmcPqA", "100.7", "509.0", "2005.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "jmIPBj66vD6", "EYbopBOJWsW", "102.0", "1572.0", "6113.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "jmIPBj66vD6", "RXL3lPSK8oG", "74.6", "47.0", "250.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "jmIPBj66vD6", "tDZVQ1WtwpA", "17.6", "22.0", "496.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q3", "jmIPBj66vD6", "CXw2yu5fodb", "98.6", "998.0", "4014.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "jUb8gELQApl", "uYxK4wmcPqA", "69.3", "1368.0", "7827.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "jUb8gELQApl", "EYbopBOJWsW", "69.1", "780.0", "4476.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "jUb8gELQApl", "RXL3lPSK8oG", "106.8", "172.0", "639.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "jUb8gELQApl", "tDZVQ1WtwpA", "61.2", "77.0", "499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "jUb8gELQApl", "CXw2yu5fodb", "69.4", "832.0", "4758.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "TEQlaapDQoK", "uYxK4wmcPqA", "35.5", "406.0", "4536.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "TEQlaapDQoK", "EYbopBOJWsW", "38.6", "1188.0", "12207.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "TEQlaapDQoK", "CXw2yu5fodb", "37.8", "457.0", "4793.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "eIQbndfxQMb", "uYxK4wmcPqA", "75.7", "286.0", "1499.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "eIQbndfxQMb", "EYbopBOJWsW", "70.2", "1975.0", "11156.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "eIQbndfxQMb", "tDZVQ1WtwpA", "9.2", "30.0", "1289.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "eIQbndfxQMb", "CXw2yu5fodb", "69.7", "510.0", "2902.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "Vth0fbpFcsO", "uYxK4wmcPqA", "16.4", "133.0", "3222.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "Vth0fbpFcsO", "EYbopBOJWsW", "15.3", "310.0", "8030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "Vth0fbpFcsO", "RXL3lPSK8oG", "16.6", "56.0", "1340.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "Vth0fbpFcsO", "CXw2yu5fodb", "21.3", "171.0", "3181.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "PMa2VCrupOd", "uYxK4wmcPqA", "77.2", "581.0", "2987.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "PMa2VCrupOd", "EYbopBOJWsW", "58.4", "1114.0", "7562.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "PMa2VCrupOd", "RXL3lPSK8oG", "87.3", "129.0", "586.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "PMa2VCrupOd", "CXw2yu5fodb", "59.0", "274.0", "1843.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "O6uvpzGd5pu", "uYxK4wmcPqA", "74.2", "645.0", "3447.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "O6uvpzGd5pu", "EYbopBOJWsW", "166.2", "3981.0", "9501.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "O6uvpzGd5pu", "RXL3lPSK8oG", "64.6", "100.0", "614.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "O6uvpzGd5pu", "CXw2yu5fodb", "92.4", "1976.0", "8488.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "bL4ooGhyHRQ", "EYbopBOJWsW", "2.1", "22.0", "4249.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "kJq2mPyFEHo", "uYxK4wmcPqA", "64.5", "977.0", "6006.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "kJq2mPyFEHo", "EYbopBOJWsW", "93.1", "2498.0", "10643.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "kJq2mPyFEHo", "tDZVQ1WtwpA", "42.0", "32.0", "302.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "kJq2mPyFEHo", "CXw2yu5fodb", "95.5", "1692.0", "7030.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "fdc6uOvgoji", "uYxK4wmcPqA", "37.1", "366.0", "3919.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "fdc6uOvgoji", "EYbopBOJWsW", "32.6", "837.0", "10184.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "fdc6uOvgoji", "RXL3lPSK8oG", "31.5", "131.0", "1649.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "fdc6uOvgoji", "tDZVQ1WtwpA", "84.3", "81.0", "381.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "fdc6uOvgoji", "CXw2yu5fodb", "40.5", "427.0", "4179.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "at6UHUQatSo", "uYxK4wmcPqA", "89.9", "805.0", "3551.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "at6UHUQatSo", "EYbopBOJWsW", "101.6", "1416.0", "5530.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "at6UHUQatSo", "RXL3lPSK8oG", "52.7", "903.0", "6800.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "at6UHUQatSo", "tDZVQ1WtwpA", "54.6", "680.0", "4943.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "at6UHUQatSo", "CXw2yu5fodb", "74.6", "2503.0", "13315.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "lc3eMKXaEfw", "uYxK4wmcPqA", "26.8", "140.0", "2072.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "lc3eMKXaEfw", "EYbopBOJWsW", "38.2", "255.0", "2646.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "lc3eMKXaEfw", "RXL3lPSK8oG", "17.0", "13.0", "304.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "lc3eMKXaEfw", "CXw2yu5fodb", "26.7", "138.0", "2053.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "qhqAxPSTUXp", "uYxK4wmcPqA", "27.8", "117.0", "1671.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "qhqAxPSTUXp", "EYbopBOJWsW", "18.1", "402.0", "8788.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "qhqAxPSTUXp", "RXL3lPSK8oG", "24.5", "5.0", "81.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "qhqAxPSTUXp", "CXw2yu5fodb", "22.1", "154.0", "2759.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "jmIPBj66vD6", "uYxK4wmcPqA", "74.0", "374.0", "2005.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "jmIPBj66vD6", "EYbopBOJWsW", "89.4", "1378.0", "6113.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "jmIPBj66vD6", "RXL3lPSK8oG", "44.4", "28.0", "250.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "jmIPBj66vD6", "tDZVQ1WtwpA", "28.8", "36.0", "496.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("sB79w2hiLp8", "2021Q4", "jmIPBj66vD6", "CXw2yu5fodb", "86.5", "875.0", "4014.0",
            "396.7", "36500", "92"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "jUb8gELQApl", "uYxK4wmcPqA", "97.2", "1333.0", "1371.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "jUb8gELQApl", "EYbopBOJWsW", "111.0", "1073.0", "967.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "jUb8gELQApl", "RXL3lPSK8oG", "87.7", "50.0", "57.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "jUb8gELQApl", "CXw2yu5fodb", "101.2", "1040.0", "1028.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "TEQlaapDQoK", "uYxK4wmcPqA", "193.3", "2003.0", "1036.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "TEQlaapDQoK", "EYbopBOJWsW", "145.2", "4650.0", "3203.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "TEQlaapDQoK", "CXw2yu5fodb", "143.0", "1556.0", "1088.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "eIQbndfxQMb", "uYxK4wmcPqA", "95.7", "287.0", "300.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "eIQbndfxQMb", "EYbopBOJWsW", "140.3", "3733.0", "2661.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "eIQbndfxQMb", "tDZVQ1WtwpA", "79.4", "112.0", "141.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "eIQbndfxQMb", "CXw2yu5fodb", "73.8", "609.0", "825.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "Vth0fbpFcsO", "uYxK4wmcPqA", "90.6", "422.0", "466.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "Vth0fbpFcsO", "EYbopBOJWsW", "110.0", "1449.0", "1317.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "Vth0fbpFcsO", "RXL3lPSK8oG", "70.0", "7.0", "10.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "Vth0fbpFcsO", "tDZVQ1WtwpA", "62.0", "379.0", "611.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "Vth0fbpFcsO", "CXw2yu5fodb", "100.9", "461.0", "457.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "PMa2VCrupOd", "uYxK4wmcPqA", "70.7", "484.0", "685.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "PMa2VCrupOd", "EYbopBOJWsW", "100.8", "1871.0", "1857.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "PMa2VCrupOd", "RXL3lPSK8oG", "45.6", "78.0", "171.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "PMa2VCrupOd", "CXw2yu5fodb", "59.3", "262.0", "442.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "O6uvpzGd5pu", "uYxK4wmcPqA", "102.1", "818.0", "801.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "O6uvpzGd5pu", "EYbopBOJWsW", "135.8", "2953.0", "2175.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "O6uvpzGd5pu", "RXL3lPSK8oG", "97.5", "155.0", "159.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "O6uvpzGd5pu", "CXw2yu5fodb", "69.8", "2952.0", "4230.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "bL4ooGhyHRQ", "uYxK4wmcPqA", "123.0", "823.0", "669.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "bL4ooGhyHRQ", "EYbopBOJWsW", "112.1", "1387.0", "1237.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "bL4ooGhyHRQ", "CXw2yu5fodb", "79.6", "1192.0", "1498.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "kJq2mPyFEHo", "uYxK4wmcPqA", "149.4", "1888.0", "1264.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "kJq2mPyFEHo", "EYbopBOJWsW", "117.7", "2837.0", "2410.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "kJq2mPyFEHo", "RXL3lPSK8oG", "61.5", "16.0", "26.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "kJq2mPyFEHo", "tDZVQ1WtwpA", "116.1", "195.0", "168.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "kJq2mPyFEHo", "CXw2yu5fodb", "127.3", "2114.0", "1661.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "fdc6uOvgoji", "uYxK4wmcPqA", "72.4", "617.0", "852.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "fdc6uOvgoji", "EYbopBOJWsW", "98.4", "2077.0", "2111.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "fdc6uOvgoji", "RXL3lPSK8oG", "84.6", "230.0", "272.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "fdc6uOvgoji", "tDZVQ1WtwpA", "63.2", "103.0", "163.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "fdc6uOvgoji", "CXw2yu5fodb", "96.3", "951.0", "988.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "at6UHUQatSo", "uYxK4wmcPqA", "153.6", "2067.0", "1346.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "at6UHUQatSo", "EYbopBOJWsW", "141.5", "2320.0", "1640.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "at6UHUQatSo", "RXL3lPSK8oG", "72.5", "1135.0", "1565.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "at6UHUQatSo", "tDZVQ1WtwpA", "68.6", "1043.0", "1520.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "at6UHUQatSo", "CXw2yu5fodb", "116.0", "4680.0", "4036.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "lc3eMKXaEfw", "uYxK4wmcPqA", "117.5", "618.0", "526.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "lc3eMKXaEfw", "EYbopBOJWsW", "90.6", "566.0", "625.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "lc3eMKXaEfw", "RXL3lPSK8oG", "131.1", "80.0", "61.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "lc3eMKXaEfw", "CXw2yu5fodb", "126.3", "566.0", "448.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "qhqAxPSTUXp", "uYxK4wmcPqA", "191.9", "568.0", "296.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "qhqAxPSTUXp", "EYbopBOJWsW", "141.9", "2287.0", "1612.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "qhqAxPSTUXp", "RXL3lPSK8oG", "197.7", "87.0", "44.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "qhqAxPSTUXp", "CXw2yu5fodb", "86.8", "434.0", "500.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "jmIPBj66vD6", "uYxK4wmcPqA", "109.8", "772.0", "703.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "jmIPBj66vD6", "EYbopBOJWsW", "110.9", "2281.0", "2057.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "jmIPBj66vD6", "RXL3lPSK8oG", "301.4", "214.0", "71.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "jmIPBj66vD6", "tDZVQ1WtwpA", "81.3", "39.0", "48.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q1", "jmIPBj66vD6", "CXw2yu5fodb", "124.9", "1633.0", "1307.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "jUb8gELQApl", "uYxK4wmcPqA", "119.8", "1908.0", "1592.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "jUb8gELQApl", "EYbopBOJWsW", "113.9", "1211.0", "1063.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "jUb8gELQApl", "RXL3lPSK8oG", "40.8", "89.0", "218.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "jUb8gELQApl", "CXw2yu5fodb", "93.7", "934.0", "997.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "TEQlaapDQoK", "uYxK4wmcPqA", "170.4", "2237.0", "1313.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "TEQlaapDQoK", "EYbopBOJWsW", "159.4", "6841.0", "4293.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "TEQlaapDQoK", "CXw2yu5fodb", "109.5", "1608.0", "1468.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "eIQbndfxQMb", "uYxK4wmcPqA", "86.2", "355.0", "412.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "eIQbndfxQMb", "EYbopBOJWsW", "145.9", "5432.0", "3723.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "eIQbndfxQMb", "tDZVQ1WtwpA", "55.6", "69.0", "124.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "eIQbndfxQMb", "CXw2yu5fodb", "97.8", "1178.0", "1204.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "Vth0fbpFcsO", "uYxK4wmcPqA", "76.1", "427.0", "561.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "Vth0fbpFcsO", "EYbopBOJWsW", "133.3", "1640.0", "1230.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "Vth0fbpFcsO", "RXL3lPSK8oG", "83.8", "57.0", "68.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "Vth0fbpFcsO", "tDZVQ1WtwpA", "45.4", "123.0", "271.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "Vth0fbpFcsO", "CXw2yu5fodb", "89.4", "471.0", "527.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "PMa2VCrupOd", "uYxK4wmcPqA", "89.6", "677.0", "756.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "PMa2VCrupOd", "EYbopBOJWsW", "91.2", "1829.0", "2006.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "PMa2VCrupOd", "RXL3lPSK8oG", "81.3", "213.0", "262.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "PMa2VCrupOd", "CXw2yu5fodb", "57.7", "342.0", "593.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "O6uvpzGd5pu", "uYxK4wmcPqA", "135.1", "909.0", "673.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "O6uvpzGd5pu", "EYbopBOJWsW", "141.9", "3452.0", "2432.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "O6uvpzGd5pu", "RXL3lPSK8oG", "73.5", "144.0", "196.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "O6uvpzGd5pu", "CXw2yu5fodb", "63.5", "2662.0", "4195.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "bL4ooGhyHRQ", "uYxK4wmcPqA", "134.4", "891.0", "663.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "bL4ooGhyHRQ", "EYbopBOJWsW", "140.2", "1689.0", "1205.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "bL4ooGhyHRQ", "CXw2yu5fodb", "76.4", "1224.0", "1602.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "kJq2mPyFEHo", "uYxK4wmcPqA", "194.4", "2693.0", "1385.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "kJq2mPyFEHo", "EYbopBOJWsW", "125.7", "3566.0", "2838.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "kJq2mPyFEHo", "RXL3lPSK8oG", "38.5", "25.0", "65.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "kJq2mPyFEHo", "tDZVQ1WtwpA", "366.2", "542.0", "148.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "kJq2mPyFEHo", "CXw2yu5fodb", "117.0", "2036.0", "1740.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "fdc6uOvgoji", "uYxK4wmcPqA", "96.6", "1009.0", "1045.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "fdc6uOvgoji", "EYbopBOJWsW", "72.9", "1926.0", "2643.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "fdc6uOvgoji", "RXL3lPSK8oG", "82.2", "296.0", "360.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "fdc6uOvgoji", "CXw2yu5fodb", "83.0", "944.0", "1137.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "at6UHUQatSo", "uYxK4wmcPqA", "165.4", "2534.0", "1532.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "at6UHUQatSo", "EYbopBOJWsW", "144.8", "3190.0", "2203.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "at6UHUQatSo", "RXL3lPSK8oG", "85.6", "1333.0", "1558.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "at6UHUQatSo", "tDZVQ1WtwpA", "60.6", "1303.0", "2150.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "at6UHUQatSo", "CXw2yu5fodb", "146.5", "8378.0", "5720.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "lc3eMKXaEfw", "uYxK4wmcPqA", "84.9", "517.0", "609.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "lc3eMKXaEfw", "EYbopBOJWsW", "110.2", "808.0", "733.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "lc3eMKXaEfw", "RXL3lPSK8oG", "263.6", "87.0", "33.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "lc3eMKXaEfw", "CXw2yu5fodb", "154.4", "1181.0", "765.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "qhqAxPSTUXp", "uYxK4wmcPqA", "109.2", "343.0", "314.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "qhqAxPSTUXp", "EYbopBOJWsW", "108.6", "1753.0", "1614.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "qhqAxPSTUXp", "RXL3lPSK8oG", "181.0", "38.0", "21.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "qhqAxPSTUXp", "CXw2yu5fodb", "135.3", "797.0", "589.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "jmIPBj66vD6", "uYxK4wmcPqA", "119.2", "800.0", "671.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "jmIPBj66vD6", "EYbopBOJWsW", "126.0", "2575.0", "2044.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "jmIPBj66vD6", "RXL3lPSK8oG", "141.9", "105.0", "74.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "jmIPBj66vD6", "tDZVQ1WtwpA", "68.3", "28.0", "41.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q2", "jmIPBj66vD6", "CXw2yu5fodb", "120.6", "1353.0", "1122.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "jUb8gELQApl", "uYxK4wmcPqA", "97.1", "1524.0", "1570.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "jUb8gELQApl", "EYbopBOJWsW", "93.2", "888.0", "953.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "jUb8gELQApl", "RXL3lPSK8oG", "65.5", "148.0", "226.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "jUb8gELQApl", "CXw2yu5fodb", "65.9", "664.0", "1007.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "TEQlaapDQoK", "uYxK4wmcPqA", "174.7", "2203.0", "1261.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "TEQlaapDQoK", "EYbopBOJWsW", "162.6", "5927.0", "3646.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "TEQlaapDQoK", "CXw2yu5fodb", "126.3", "1522.0", "1205.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "eIQbndfxQMb", "uYxK4wmcPqA", "95.6", "373.0", "390.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "eIQbndfxQMb", "EYbopBOJWsW", "102.3", "3835.0", "3749.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "eIQbndfxQMb", "tDZVQ1WtwpA", "108.5", "102.0", "94.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "eIQbndfxQMb", "CXw2yu5fodb", "77.5", "942.0", "1215.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "Vth0fbpFcsO", "uYxK4wmcPqA", "60.4", "341.0", "565.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "Vth0fbpFcsO", "EYbopBOJWsW", "69.4", "943.0", "1359.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "Vth0fbpFcsO", "RXL3lPSK8oG", "45.9", "34.0", "74.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "Vth0fbpFcsO", "CXw2yu5fodb", "37.1", "154.0", "415.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "PMa2VCrupOd", "uYxK4wmcPqA", "89.2", "612.0", "686.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "PMa2VCrupOd", "EYbopBOJWsW", "85.6", "1443.0", "1685.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "PMa2VCrupOd", "RXL3lPSK8oG", "61.1", "118.0", "193.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "PMa2VCrupOd", "CXw2yu5fodb", "52.9", "245.0", "463.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "O6uvpzGd5pu", "uYxK4wmcPqA", "106.9", "763.0", "714.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "O6uvpzGd5pu", "EYbopBOJWsW", "157.5", "3751.0", "2381.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "O6uvpzGd5pu", "RXL3lPSK8oG", "220.0", "308.0", "140.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "O6uvpzGd5pu", "CXw2yu5fodb", "49.3", "2454.0", "4979.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "bL4ooGhyHRQ", "uYxK4wmcPqA", "167.6", "1255.0", "749.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "bL4ooGhyHRQ", "EYbopBOJWsW", "158.7", "1830.0", "1153.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "bL4ooGhyHRQ", "CXw2yu5fodb", "91.1", "1181.0", "1297.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "kJq2mPyFEHo", "uYxK4wmcPqA", "126.6", "1519.0", "1200.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "kJq2mPyFEHo", "EYbopBOJWsW", "80.3", "2000.0", "2490.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "kJq2mPyFEHo", "tDZVQ1WtwpA", "428.9", "549.0", "128.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "kJq2mPyFEHo", "CXw2yu5fodb", "68.9", "951.0", "1380.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "fdc6uOvgoji", "uYxK4wmcPqA", "65.2", "576.0", "883.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "fdc6uOvgoji", "EYbopBOJWsW", "69.7", "1474.0", "2115.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "fdc6uOvgoji", "RXL3lPSK8oG", "87.3", "219.0", "251.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "fdc6uOvgoji", "tDZVQ1WtwpA", "87.8", "216.0", "246.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "fdc6uOvgoji", "CXw2yu5fodb", "77.8", "783.0", "1007.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "at6UHUQatSo", "uYxK4wmcPqA", "173.9", "2647.0", "1522.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "at6UHUQatSo", "EYbopBOJWsW", "126.2", "2489.0", "1973.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "at6UHUQatSo", "RXL3lPSK8oG", "104.0", "1364.0", "1311.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "at6UHUQatSo", "tDZVQ1WtwpA", "419.4", "6300.0", "1502.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "at6UHUQatSo", "CXw2yu5fodb", "98.5", "4512.0", "4581.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "lc3eMKXaEfw", "uYxK4wmcPqA", "93.4", "624.0", "668.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "lc3eMKXaEfw", "EYbopBOJWsW", "98.1", "823.0", "839.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "lc3eMKXaEfw", "RXL3lPSK8oG", "122.4", "60.0", "49.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "lc3eMKXaEfw", "CXw2yu5fodb", "144.4", "781.0", "541.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "qhqAxPSTUXp", "uYxK4wmcPqA", "117.1", "567.0", "484.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "qhqAxPSTUXp", "EYbopBOJWsW", "115.8", "1902.0", "1642.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "qhqAxPSTUXp", "CXw2yu5fodb", "97.1", "594.0", "612.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "jmIPBj66vD6", "uYxK4wmcPqA", "110.1", "848.0", "770.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "jmIPBj66vD6", "EYbopBOJWsW", "119.6", "2341.0", "1958.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "jmIPBj66vD6", "RXL3lPSK8oG", "24.3", "9.0", "37.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q3", "jmIPBj66vD6", "CXw2yu5fodb", "95.6", "1186.0", "1240.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "jUb8gELQApl", "uYxK4wmcPqA", "156.7", "2325.0", "1484.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "jUb8gELQApl", "EYbopBOJWsW", "131.5", "915.0", "696.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "jUb8gELQApl", "RXL3lPSK8oG", "85.9", "146.0", "170.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "jUb8gELQApl", "CXw2yu5fodb", "116.3", "1083.0", "931.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "TEQlaapDQoK", "uYxK4wmcPqA", "186.8", "1345.0", "720.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "TEQlaapDQoK", "EYbopBOJWsW", "169.2", "3502.0", "2070.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "TEQlaapDQoK", "CXw2yu5fodb", "138.0", "1014.0", "735.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "eIQbndfxQMb", "uYxK4wmcPqA", "84.3", "439.0", "521.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "eIQbndfxQMb", "EYbopBOJWsW", "134.5", "5138.0", "3819.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "eIQbndfxQMb", "tDZVQ1WtwpA", "56.3", "49.0", "87.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "eIQbndfxQMb", "CXw2yu5fodb", "141.4", "1769.0", "1251.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "Vth0fbpFcsO", "uYxK4wmcPqA", "68.1", "160.0", "235.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "Vth0fbpFcsO", "EYbopBOJWsW", "118.7", "598.0", "504.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "Vth0fbpFcsO", "RXL3lPSK8oG", "61.3", "95.0", "155.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "Vth0fbpFcsO", "CXw2yu5fodb", "55.3", "120.0", "217.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "PMa2VCrupOd", "uYxK4wmcPqA", "112.0", "876.0", "782.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "PMa2VCrupOd", "EYbopBOJWsW", "125.1", "2480.0", "1983.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "PMa2VCrupOd", "RXL3lPSK8oG", "79.2", "198.0", "250.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "PMa2VCrupOd", "CXw2yu5fodb", "135.2", "683.0", "505.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "O6uvpzGd5pu", "uYxK4wmcPqA", "227.2", "1679.0", "739.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "O6uvpzGd5pu", "EYbopBOJWsW", "161.3", "3413.0", "2116.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "O6uvpzGd5pu", "RXL3lPSK8oG", "83.1", "118.0", "142.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "O6uvpzGd5pu", "CXw2yu5fodb", "78.0", "3696.0", "4736.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "bL4ooGhyHRQ", "EYbopBOJWsW", "223.8", "47.0", "21.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "kJq2mPyFEHo", "uYxK4wmcPqA", "124.8", "1334.0", "1069.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "kJq2mPyFEHo", "EYbopBOJWsW", "76.0", "1955.0", "2572.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "kJq2mPyFEHo", "tDZVQ1WtwpA", "300.0", "240.0", "80.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "kJq2mPyFEHo", "CXw2yu5fodb", "105.5", "1695.0", "1607.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "fdc6uOvgoji", "uYxK4wmcPqA", "107.8", "638.0", "592.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "fdc6uOvgoji", "EYbopBOJWsW", "92.0", "1131.0", "1229.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "fdc6uOvgoji", "RXL3lPSK8oG", "138.7", "258.0", "186.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "fdc6uOvgoji", "tDZVQ1WtwpA", "107.8", "152.0", "141.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "fdc6uOvgoji", "CXw2yu5fodb", "89.3", "545.0", "610.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "at6UHUQatSo", "uYxK4wmcPqA", "132.6", "1747.0", "1317.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "at6UHUQatSo", "EYbopBOJWsW", "125.8", "2421.0", "1925.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "at6UHUQatSo", "RXL3lPSK8oG", "77.1", "1417.0", "1838.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "at6UHUQatSo", "tDZVQ1WtwpA", "36.9", "457.0", "1239.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "at6UHUQatSo", "CXw2yu5fodb", "128.3", "5381.0", "4193.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "lc3eMKXaEfw", "uYxK4wmcPqA", "72.7", "141.0", "194.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "lc3eMKXaEfw", "EYbopBOJWsW", "93.3", "210.0", "225.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "lc3eMKXaEfw", "RXL3lPSK8oG", "17.6", "3.0", "17.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "lc3eMKXaEfw", "CXw2yu5fodb", "199.0", "388.0", "195.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "qhqAxPSTUXp", "uYxK4wmcPqA", "176.7", "417.0", "236.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "qhqAxPSTUXp", "EYbopBOJWsW", "96.2", "704.0", "732.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "qhqAxPSTUXp", "CXw2yu5fodb", "102.5", "292.0", "285.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "jmIPBj66vD6", "uYxK4wmcPqA", "112.2", "682.0", "608.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "jmIPBj66vD6", "EYbopBOJWsW", "117.3", "1981.0", "1689.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "jmIPBj66vD6", "RXL3lPSK8oG", "75.0", "39.0", "52.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("dwEq7wi6nXV", "2021Q4", "jmIPBj66vD6", "CXw2yu5fodb", "86.7", "929.0", "1071.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "jUb8gELQApl", "uYxK4wmcPqA", "69.9", "1094.0", "1564.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "jUb8gELQApl", "EYbopBOJWsW", "87.4", "871.0", "997.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "jUb8gELQApl", "RXL3lPSK8oG", "24.2", "22.0", "91.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "jUb8gELQApl", "CXw2yu5fodb", "79.1", "827.0", "1045.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "TEQlaapDQoK", "uYxK4wmcPqA", "126.9", "958.0", "755.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "TEQlaapDQoK", "EYbopBOJWsW", "122.5", "2981.0", "2433.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "TEQlaapDQoK", "CXw2yu5fodb", "107.7", "962.0", "893.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "eIQbndfxQMb", "uYxK4wmcPqA", "94.1", "253.0", "269.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "eIQbndfxQMb", "EYbopBOJWsW", "122.8", "3107.0", "2530.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "eIQbndfxQMb", "tDZVQ1WtwpA", "71.1", "64.0", "90.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "eIQbndfxQMb", "CXw2yu5fodb", "87.8", "563.0", "641.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "Vth0fbpFcsO", "uYxK4wmcPqA", "41.1", "193.0", "470.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "Vth0fbpFcsO", "EYbopBOJWsW", "78.9", "979.0", "1241.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "Vth0fbpFcsO", "RXL3lPSK8oG", "166.7", "5.0", "3.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "Vth0fbpFcsO", "tDZVQ1WtwpA", "20.8", "87.0", "419.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "Vth0fbpFcsO", "CXw2yu5fodb", "148.3", "854.0", "576.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "PMa2VCrupOd", "uYxK4wmcPqA", "52.1", "369.0", "708.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "PMa2VCrupOd", "EYbopBOJWsW", "81.6", "1454.0", "1781.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "PMa2VCrupOd", "RXL3lPSK8oG", "43.2", "54.0", "125.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "PMa2VCrupOd", "CXw2yu5fodb", "36.6", "154.0", "421.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "O6uvpzGd5pu", "uYxK4wmcPqA", "74.8", "683.0", "913.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "O6uvpzGd5pu", "EYbopBOJWsW", "124.0", "4055.0", "3271.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "O6uvpzGd5pu", "RXL3lPSK8oG", "352.1", "507.0", "144.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "O6uvpzGd5pu", "CXw2yu5fodb", "73.1", "1854.0", "2535.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "bL4ooGhyHRQ", "uYxK4wmcPqA", "107.6", "639.0", "594.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "bL4ooGhyHRQ", "EYbopBOJWsW", "105.0", "1233.0", "1174.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "bL4ooGhyHRQ", "CXw2yu5fodb", "92.6", "852.0", "920.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "kJq2mPyFEHo", "uYxK4wmcPqA", "110.3", "1811.0", "1642.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "kJq2mPyFEHo", "EYbopBOJWsW", "79.1", "2496.0", "3155.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "kJq2mPyFEHo", "RXL3lPSK8oG", "44.4", "4.0", "9.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "kJq2mPyFEHo", "tDZVQ1WtwpA", "111.5", "126.0", "113.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "kJq2mPyFEHo", "CXw2yu5fodb", "78.9", "1413.0", "1791.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "fdc6uOvgoji", "uYxK4wmcPqA", "72.0", "543.0", "754.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "fdc6uOvgoji", "EYbopBOJWsW", "87.4", "1498.0", "1713.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "fdc6uOvgoji", "RXL3lPSK8oG", "78.8", "167.0", "212.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "fdc6uOvgoji", "tDZVQ1WtwpA", "100.0", "53.0", "53.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "fdc6uOvgoji", "CXw2yu5fodb", "84.5", "771.0", "912.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "at6UHUQatSo", "uYxK4wmcPqA", "134.1", "1086.0", "810.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "at6UHUQatSo", "EYbopBOJWsW", "254.2", "3353.0", "1319.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "at6UHUQatSo", "RXL3lPSK8oG", "96.2", "1349.0", "1402.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "at6UHUQatSo", "tDZVQ1WtwpA", "63.0", "1317.0", "2092.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "at6UHUQatSo", "CXw2yu5fodb", "326.8", "10249.0", "3136.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "lc3eMKXaEfw", "uYxK4wmcPqA", "86.4", "356.0", "412.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "lc3eMKXaEfw", "EYbopBOJWsW", "116.5", "686.0", "589.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "lc3eMKXaEfw", "RXL3lPSK8oG", "108.0", "54.0", "50.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "lc3eMKXaEfw", "CXw2yu5fodb", "83.8", "410.0", "489.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "qhqAxPSTUXp", "uYxK4wmcPqA", "171.9", "435.0", "253.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "qhqAxPSTUXp", "EYbopBOJWsW", "121.6", "1623.0", "1335.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "qhqAxPSTUXp", "RXL3lPSK8oG", "391.7", "94.0", "24.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "qhqAxPSTUXp", "CXw2yu5fodb", "110.0", "450.0", "409.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "jmIPBj66vD6", "uYxK4wmcPqA", "114.0", "643.0", "564.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "jmIPBj66vD6", "EYbopBOJWsW", "105.2", "1766.0", "1679.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "jmIPBj66vD6", "RXL3lPSK8oG", "161.0", "124.0", "77.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "jmIPBj66vD6", "tDZVQ1WtwpA", "26.6", "25.0", "94.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q1", "jmIPBj66vD6", "CXw2yu5fodb", "89.9", "1088.0", "1210.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "jUb8gELQApl", "uYxK4wmcPqA", "74.0", "1201.0", "1624.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "jUb8gELQApl", "EYbopBOJWsW", "102.9", "1184.0", "1151.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "jUb8gELQApl", "RXL3lPSK8oG", "20.7", "67.0", "323.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "jUb8gELQApl", "CXw2yu5fodb", "66.0", "711.0", "1078.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "TEQlaapDQoK", "uYxK4wmcPqA", "127.3", "1152.0", "905.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "TEQlaapDQoK", "EYbopBOJWsW", "135.8", "4221.0", "3109.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "TEQlaapDQoK", "CXw2yu5fodb", "96.2", "928.0", "965.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "eIQbndfxQMb", "uYxK4wmcPqA", "85.2", "317.0", "372.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "eIQbndfxQMb", "EYbopBOJWsW", "125.3", "3554.0", "2837.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "eIQbndfxQMb", "tDZVQ1WtwpA", "19.8", "20.0", "101.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "eIQbndfxQMb", "CXw2yu5fodb", "105.7", "849.0", "803.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "Vth0fbpFcsO", "uYxK4wmcPqA", "39.8", "213.0", "535.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "Vth0fbpFcsO", "EYbopBOJWsW", "64.0", "816.0", "1275.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "Vth0fbpFcsO", "RXL3lPSK8oG", "23.1", "15.0", "65.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "Vth0fbpFcsO", "tDZVQ1WtwpA", "21.0", "49.0", "233.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "Vth0fbpFcsO", "CXw2yu5fodb", "40.5", "276.0", "681.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "PMa2VCrupOd", "uYxK4wmcPqA", "59.8", "387.0", "647.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "PMa2VCrupOd", "EYbopBOJWsW", "83.9", "1478.0", "1762.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "PMa2VCrupOd", "RXL3lPSK8oG", "84.3", "161.0", "191.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "PMa2VCrupOd", "CXw2yu5fodb", "52.5", "253.0", "482.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "O6uvpzGd5pu", "uYxK4wmcPqA", "74.2", "512.0", "690.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "O6uvpzGd5pu", "EYbopBOJWsW", "62.5", "2749.0", "4400.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "O6uvpzGd5pu", "RXL3lPSK8oG", "70.7", "140.0", "198.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "O6uvpzGd5pu", "CXw2yu5fodb", "62.4", "1626.0", "2607.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "bL4ooGhyHRQ", "uYxK4wmcPqA", "100.5", "597.0", "594.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "bL4ooGhyHRQ", "EYbopBOJWsW", "105.3", "1159.0", "1101.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "bL4ooGhyHRQ", "CXw2yu5fodb", "77.3", "846.0", "1094.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "kJq2mPyFEHo", "uYxK4wmcPqA", "134.4", "2640.0", "1965.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "kJq2mPyFEHo", "EYbopBOJWsW", "76.0", "2778.0", "3655.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "kJq2mPyFEHo", "RXL3lPSK8oG", "11.3", "9.0", "80.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "kJq2mPyFEHo", "tDZVQ1WtwpA", "124.4", "148.0", "119.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "kJq2mPyFEHo", "CXw2yu5fodb", "76.8", "1686.0", "2195.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "fdc6uOvgoji", "uYxK4wmcPqA", "89.6", "798.0", "891.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "fdc6uOvgoji", "EYbopBOJWsW", "70.0", "1568.0", "2239.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "fdc6uOvgoji", "RXL3lPSK8oG", "54.9", "141.0", "257.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "fdc6uOvgoji", "CXw2yu5fodb", "80.3", "801.0", "997.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "at6UHUQatSo", "uYxK4wmcPqA", "192.7", "1994.0", "1035.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "at6UHUQatSo", "EYbopBOJWsW", "129.0", "2241.0", "1737.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "at6UHUQatSo", "RXL3lPSK8oG", "68.7", "920.0", "1340.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "at6UHUQatSo", "tDZVQ1WtwpA", "14.7", "516.0", "3521.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "at6UHUQatSo", "CXw2yu5fodb", "124.4", "5507.0", "4427.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "lc3eMKXaEfw", "uYxK4wmcPqA", "67.5", "337.0", "499.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "lc3eMKXaEfw", "EYbopBOJWsW", "71.3", "578.0", "811.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "lc3eMKXaEfw", "RXL3lPSK8oG", "97.0", "32.0", "33.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "lc3eMKXaEfw", "CXw2yu5fodb", "81.1", "524.0", "646.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "qhqAxPSTUXp", "uYxK4wmcPqA", "104.1", "280.0", "269.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "qhqAxPSTUXp", "EYbopBOJWsW", "115.0", "1422.0", "1237.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "qhqAxPSTUXp", "RXL3lPSK8oG", "266.7", "24.0", "9.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "qhqAxPSTUXp", "CXw2yu5fodb", "131.7", "581.0", "441.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "jmIPBj66vD6", "uYxK4wmcPqA", "125.4", "741.0", "591.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "jmIPBj66vD6", "EYbopBOJWsW", "127.4", "2334.0", "1832.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "jmIPBj66vD6", "RXL3lPSK8oG", "113.9", "90.0", "79.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "jmIPBj66vD6", "tDZVQ1WtwpA", "11.5", "7.0", "61.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q2", "jmIPBj66vD6", "CXw2yu5fodb", "94.5", "1005.0", "1063.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "jUb8gELQApl", "uYxK4wmcPqA", "91.2", "1408.0", "1544.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "jUb8gELQApl", "EYbopBOJWsW", "87.6", "867.0", "990.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "jUb8gELQApl", "RXL3lPSK8oG", "38.9", "111.0", "285.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "jUb8gELQApl", "CXw2yu5fodb", "66.8", "637.0", "953.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "TEQlaapDQoK", "uYxK4wmcPqA", "186.2", "1991.0", "1069.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "TEQlaapDQoK", "EYbopBOJWsW", "129.8", "3930.0", "3027.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "TEQlaapDQoK", "CXw2yu5fodb", "82.3", "984.0", "1195.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "eIQbndfxQMb", "uYxK4wmcPqA", "81.3", "327.0", "402.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "eIQbndfxQMb", "EYbopBOJWsW", "97.2", "2799.0", "2881.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "eIQbndfxQMb", "tDZVQ1WtwpA", "98.6", "70.0", "71.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "eIQbndfxQMb", "CXw2yu5fodb", "69.0", "533.0", "773.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "Vth0fbpFcsO", "uYxK4wmcPqA", "45.7", "226.0", "494.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "Vth0fbpFcsO", "EYbopBOJWsW", "51.2", "683.0", "1334.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "Vth0fbpFcsO", "RXL3lPSK8oG", "59.0", "36.0", "61.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "Vth0fbpFcsO", "CXw2yu5fodb", "39.6", "160.0", "404.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "PMa2VCrupOd", "uYxK4wmcPqA", "66.7", "490.0", "735.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "PMa2VCrupOd", "EYbopBOJWsW", "65.3", "1058.0", "1619.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "PMa2VCrupOd", "RXL3lPSK8oG", "48.0", "85.0", "177.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "PMa2VCrupOd", "CXw2yu5fodb", "31.8", "127.0", "399.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "O6uvpzGd5pu", "uYxK4wmcPqA", "68.1", "492.0", "722.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "O6uvpzGd5pu", "EYbopBOJWsW", "84.0", "3037.0", "3614.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "O6uvpzGd5pu", "RXL3lPSK8oG", "101.1", "177.0", "175.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "O6uvpzGd5pu", "CXw2yu5fodb", "66.2", "1918.0", "2896.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "bL4ooGhyHRQ", "uYxK4wmcPqA", "127.3", "1067.0", "838.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "bL4ooGhyHRQ", "EYbopBOJWsW", "135.3", "1580.0", "1168.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "bL4ooGhyHRQ", "CXw2yu5fodb", "96.6", "929.0", "962.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "kJq2mPyFEHo", "uYxK4wmcPqA", "98.6", "1366.0", "1385.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "kJq2mPyFEHo", "EYbopBOJWsW", "46.0", "1527.0", "3318.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "kJq2mPyFEHo", "tDZVQ1WtwpA", "342.9", "360.0", "105.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "kJq2mPyFEHo", "CXw2yu5fodb", "61.3", "975.0", "1591.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "fdc6uOvgoji", "uYxK4wmcPqA", "60.5", "451.0", "746.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "fdc6uOvgoji", "EYbopBOJWsW", "65.3", "1171.0", "1794.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "fdc6uOvgoji", "RXL3lPSK8oG", "61.6", "157.0", "255.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "fdc6uOvgoji", "tDZVQ1WtwpA", "27.3", "48.0", "176.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "fdc6uOvgoji", "CXw2yu5fodb", "54.4", "461.0", "847.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "at6UHUQatSo", "uYxK4wmcPqA", "155.0", "1964.0", "1267.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "at6UHUQatSo", "EYbopBOJWsW", "123.8", "2123.0", "1715.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "at6UHUQatSo", "RXL3lPSK8oG", "70.4", "813.0", "1155.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "at6UHUQatSo", "tDZVQ1WtwpA", "136.6", "2368.0", "1733.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "at6UHUQatSo", "CXw2yu5fodb", "145.2", "6170.0", "4248.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "lc3eMKXaEfw", "uYxK4wmcPqA", "82.2", "530.0", "645.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "lc3eMKXaEfw", "EYbopBOJWsW", "77.0", "718.0", "932.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "lc3eMKXaEfw", "RXL3lPSK8oG", "109.4", "35.0", "32.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "lc3eMKXaEfw", "CXw2yu5fodb", "95.9", "489.0", "510.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "qhqAxPSTUXp", "uYxK4wmcPqA", "115.7", "493.0", "426.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "qhqAxPSTUXp", "EYbopBOJWsW", "106.0", "1416.0", "1336.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "qhqAxPSTUXp", "CXw2yu5fodb", "107.7", "576.0", "535.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "jmIPBj66vD6", "uYxK4wmcPqA", "95.4", "636.0", "667.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "jmIPBj66vD6", "EYbopBOJWsW", "108.8", "1960.0", "1802.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q3", "jmIPBj66vD6", "CXw2yu5fodb", "87.9", "969.0", "1103.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "jUb8gELQApl", "uYxK4wmcPqA", "119.4", "1942.0", "1626.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "jUb8gELQApl", "EYbopBOJWsW", "96.6", "877.0", "908.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "jUb8gELQApl", "RXL3lPSK8oG", "47.9", "104.0", "217.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "jUb8gELQApl", "CXw2yu5fodb", "103.3", "910.0", "881.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "TEQlaapDQoK", "uYxK4wmcPqA", "130.3", "881.0", "676.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "TEQlaapDQoK", "EYbopBOJWsW", "134.5", "2423.0", "1801.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "TEQlaapDQoK", "CXw2yu5fodb", "99.5", "774.0", "778.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "eIQbndfxQMb", "uYxK4wmcPqA", "65.6", "292.0", "445.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "eIQbndfxQMb", "EYbopBOJWsW", "100.0", "3244.0", "3243.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "eIQbndfxQMb", "tDZVQ1WtwpA", "33.8", "24.0", "71.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "eIQbndfxQMb", "CXw2yu5fodb", "118.8", "1092.0", "919.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "Vth0fbpFcsO", "uYxK4wmcPqA", "19.8", "50.0", "253.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "Vth0fbpFcsO", "EYbopBOJWsW", "67.1", "329.0", "490.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "Vth0fbpFcsO", "RXL3lPSK8oG", "56.9", "62.0", "109.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "Vth0fbpFcsO", "CXw2yu5fodb", "30.6", "57.0", "186.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "PMa2VCrupOd", "uYxK4wmcPqA", "79.9", "565.0", "707.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "PMa2VCrupOd", "EYbopBOJWsW", "139.9", "2465.0", "1762.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "PMa2VCrupOd", "RXL3lPSK8oG", "66.2", "129.0", "195.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "PMa2VCrupOd", "CXw2yu5fodb", "138.7", "538.0", "388.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "O6uvpzGd5pu", "uYxK4wmcPqA", "174.4", "1280.0", "734.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "O6uvpzGd5pu", "EYbopBOJWsW", "111.6", "3166.0", "2838.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "O6uvpzGd5pu", "RXL3lPSK8oG", "32.1", "43.0", "134.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "O6uvpzGd5pu", "CXw2yu5fodb", "80.6", "2444.0", "3033.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "bL4ooGhyHRQ", "EYbopBOJWsW", "118.2", "39.0", "33.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "kJq2mPyFEHo", "uYxK4wmcPqA", "95.9", "1272.0", "1327.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "kJq2mPyFEHo", "EYbopBOJWsW", "50.0", "1720.0", "3442.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "kJq2mPyFEHo", "tDZVQ1WtwpA", "300.0", "147.0", "49.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "kJq2mPyFEHo", "CXw2yu5fodb", "41.0", "886.0", "2162.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "fdc6uOvgoji", "uYxK4wmcPqA", "91.7", "509.0", "555.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "fdc6uOvgoji", "EYbopBOJWsW", "81.4", "892.0", "1096.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "fdc6uOvgoji", "RXL3lPSK8oG", "81.1", "129.0", "159.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "fdc6uOvgoji", "tDZVQ1WtwpA", "26.0", "52.0", "200.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "fdc6uOvgoji", "CXw2yu5fodb", "61.0", "361.0", "592.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "at6UHUQatSo", "uYxK4wmcPqA", "150.2", "1634.0", "1088.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "at6UHUQatSo", "EYbopBOJWsW", "130.4", "2211.0", "1695.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "at6UHUQatSo", "RXL3lPSK8oG", "79.5", "1100.0", "1383.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "at6UHUQatSo", "tDZVQ1WtwpA", "7.9", "210.0", "2663.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "at6UHUQatSo", "CXw2yu5fodb", "192.3", "6047.0", "3145.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "lc3eMKXaEfw", "uYxK4wmcPqA", "55.6", "114.0", "205.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "lc3eMKXaEfw", "EYbopBOJWsW", "66.8", "181.0", "271.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "lc3eMKXaEfw", "RXL3lPSK8oG", "108.3", "13.0", "12.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "lc3eMKXaEfw", "CXw2yu5fodb", "104.9", "151.0", "144.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "qhqAxPSTUXp", "uYxK4wmcPqA", "172.1", "253.0", "147.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "qhqAxPSTUXp", "EYbopBOJWsW", "92.9", "546.0", "588.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "qhqAxPSTUXp", "CXw2yu5fodb", "88.7", "189.0", "213.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "jmIPBj66vD6", "uYxK4wmcPqA", "110.4", "519.0", "470.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "jmIPBj66vD6", "EYbopBOJWsW", "112.2", "1770.0", "1578.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "jmIPBj66vD6", "RXL3lPSK8oG", "40.5", "15.0", "37.0",
            "100.0", "100", "1"));
    validateRow(response,
        List.of("c8fABiNpT0B", "2021Q4", "jmIPBj66vD6", "CXw2yu5fodb", "72.0", "747.0", "1037.0",
            "100.0", "100", "1"));
  }

  @Test
  public void queryAncIpt1CoverageLast12MonthsDistricts() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=dx:dwEq7wi6nXV")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=ou:jUb8gELQApl;TEQlaapDQoK;eIQbndfxQMb;Vth0fbpFcsO;PMa2VCrupOd;O6uvpzGd5pu;bL4ooGhyHRQ;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;qhqAxPSTUXp;jmIPBj66vD6;LEVEL-wjP19dkFeIk,pe:LAST_12_MONTHS")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(148)))
        .body("height", equalTo(148))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202109\":{\"name\":\"September 2021\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"wjP19dkFeIk\":{\"uid\":\"wjP19dkFeIk\",\"name\":\"District\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"202110\":{\"name\":\"October 2021\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"pe\":{\"name\":\"Period\"},\"dwEq7wi6nXV\":{\"name\":\"ANC IPT 1 Coverage\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"dwEq7wi6nXV\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("O6uvpzGd5pu", "202101", "109.6"));
    validateRow(response, List.of("O6uvpzGd5pu", "202102", "84.5"));
    validateRow(response, List.of("O6uvpzGd5pu", "202103", "84.8"));
    validateRow(response, List.of("O6uvpzGd5pu", "202104", "99.8"));
    validateRow(response, List.of("O6uvpzGd5pu", "202105", "90.1"));
    validateRow(response, List.of("O6uvpzGd5pu", "202106", "99.3"));
    validateRow(response, List.of("O6uvpzGd5pu", "202107", "99.9"));
    validateRow(response, List.of("O6uvpzGd5pu", "202108", "72.9"));
    validateRow(response, List.of("O6uvpzGd5pu", "202109", "98.5"));
    validateRow(response, List.of("O6uvpzGd5pu", "202110", "128.7"));
    validateRow(response, List.of("O6uvpzGd5pu", "202111", "109.1"));
    validateRow(response, List.of("O6uvpzGd5pu", "202112", "136.9"));
    validateRow(response, List.of("fdc6uOvgoji", "202101", "95.0"));
    validateRow(response, List.of("fdc6uOvgoji", "202102", "99.8"));
    validateRow(response, List.of("fdc6uOvgoji", "202103", "78.9"));
    validateRow(response, List.of("fdc6uOvgoji", "202104", "85.7"));
    validateRow(response, List.of("fdc6uOvgoji", "202105", "80.2"));
    validateRow(response, List.of("fdc6uOvgoji", "202106", "77.1"));
    validateRow(response, List.of("fdc6uOvgoji", "202107", "83.2"));
    validateRow(response, List.of("fdc6uOvgoji", "202108", "57.6"));
    validateRow(response, List.of("fdc6uOvgoji", "202109", "78.6"));
    validateRow(response, List.of("fdc6uOvgoji", "202110", "102.3"));
    validateRow(response, List.of("fdc6uOvgoji", "202112", "95.2"));
    validateRow(response, List.of("lc3eMKXaEfw", "202101", "136.0"));
    validateRow(response, List.of("lc3eMKXaEfw", "202102", "81.9"));
    validateRow(response, List.of("lc3eMKXaEfw", "202103", "111.0"));
    validateRow(response, List.of("lc3eMKXaEfw", "202104", "105.4"));
    validateRow(response, List.of("lc3eMKXaEfw", "202105", "146.2"));
    validateRow(response, List.of("lc3eMKXaEfw", "202106", "103.8"));
    validateRow(response, List.of("lc3eMKXaEfw", "202107", "109.1"));
    validateRow(response, List.of("lc3eMKXaEfw", "202108", "96.8"));
    validateRow(response, List.of("lc3eMKXaEfw", "202109", "116.9"));
    validateRow(response, List.of("lc3eMKXaEfw", "202111", "125.5"));
    validateRow(response, List.of("jUb8gELQApl", "202101", "97.1"));
    validateRow(response, List.of("jUb8gELQApl", "202102", "91.1"));
    validateRow(response, List.of("jUb8gELQApl", "202103", "100.3"));
    validateRow(response, List.of("jUb8gELQApl", "202104", "91.6"));
    validateRow(response, List.of("jUb8gELQApl", "202105", "102.5"));
    validateRow(response, List.of("jUb8gELQApl", "202106", "109.0"));
    validateRow(response, List.of("jUb8gELQApl", "202107", "87.5"));
    validateRow(response, List.of("jUb8gELQApl", "202108", "84.9"));
    validateRow(response, List.of("jUb8gELQApl", "202109", "75.8"));
    validateRow(response, List.of("jUb8gELQApl", "202110", "156.0"));
    validateRow(response, List.of("jUb8gELQApl", "202111", "118.2"));
    validateRow(response, List.of("jUb8gELQApl", "202112", "117.6"));
    validateRow(response, List.of("PMa2VCrupOd", "202101", "78.7"));
    validateRow(response, List.of("PMa2VCrupOd", "202102", "93.5"));
    validateRow(response, List.of("PMa2VCrupOd", "202103", "79.8"));
    validateRow(response, List.of("PMa2VCrupOd", "202104", "71.6"));
    validateRow(response, List.of("PMa2VCrupOd", "202105", "93.5"));
    validateRow(response, List.of("PMa2VCrupOd", "202106", "80.0"));
    validateRow(response, List.of("PMa2VCrupOd", "202107", "76.6"));
    validateRow(response, List.of("PMa2VCrupOd", "202108", "72.6"));
    validateRow(response, List.of("PMa2VCrupOd", "202109", "86.4"));
    validateRow(response, List.of("PMa2VCrupOd", "202110", "119.7"));
    validateRow(response, List.of("PMa2VCrupOd", "202111", "115.6"));
    validateRow(response, List.of("PMa2VCrupOd", "202112", "126.2"));
    validateRow(response, List.of("kJq2mPyFEHo", "202101", "115.1"));
    validateRow(response, List.of("kJq2mPyFEHo", "202102", "140.5"));
    validateRow(response, List.of("kJq2mPyFEHo", "202103", "115.3"));
    validateRow(response, List.of("kJq2mPyFEHo", "202104", "135.8"));
    validateRow(response, List.of("kJq2mPyFEHo", "202105", "137.4"));
    validateRow(response, List.of("kJq2mPyFEHo", "202106", "139.2"));
    validateRow(response, List.of("kJq2mPyFEHo", "202107", "91.6"));
    validateRow(response, List.of("kJq2mPyFEHo", "202108", "114.3"));
    validateRow(response, List.of("kJq2mPyFEHo", "202109", "75.3"));
    validateRow(response, List.of("kJq2mPyFEHo", "202110", "99.6"));
    validateRow(response, List.of("kJq2mPyFEHo", "202111", "81.6"));
    validateRow(response, List.of("kJq2mPyFEHo", "202112", "102.1"));
    validateRow(response, List.of("qhqAxPSTUXp", "202101", "136.2"));
    validateRow(response, List.of("qhqAxPSTUXp", "202102", "139.3"));
    validateRow(response, List.of("qhqAxPSTUXp", "202103", "137.6"));
    validateRow(response, List.of("qhqAxPSTUXp", "202104", "104.8"));
    validateRow(response, List.of("qhqAxPSTUXp", "202105", "120.0"));
    validateRow(response, List.of("qhqAxPSTUXp", "202106", "122.1"));
    validateRow(response, List.of("qhqAxPSTUXp", "202107", "98.1"));
    validateRow(response, List.of("qhqAxPSTUXp", "202108", "107.6"));
    validateRow(response, List.of("qhqAxPSTUXp", "202109", "131.9"));
    validateRow(response, List.of("qhqAxPSTUXp", "202111", "112.5"));
    validateRow(response, List.of("Vth0fbpFcsO", "202101", "81.6"));
    validateRow(response, List.of("Vth0fbpFcsO", "202102", "106.3"));
    validateRow(response, List.of("Vth0fbpFcsO", "202103", "88.8"));
    validateRow(response, List.of("Vth0fbpFcsO", "202104", "87.8"));
    validateRow(response, List.of("Vth0fbpFcsO", "202105", "162.1"));
    validateRow(response, List.of("Vth0fbpFcsO", "202106", "61.8"));
    validateRow(response, List.of("Vth0fbpFcsO", "202107", "73.5"));
    validateRow(response, List.of("Vth0fbpFcsO", "202108", "45.3"));
    validateRow(response, List.of("Vth0fbpFcsO", "202109", "160.4"));
    validateRow(response, List.of("Vth0fbpFcsO", "202112", "85.0"));
    validateRow(response, List.of("jmIPBj66vD6", "202101", "106.5"));
    validateRow(response, List.of("jmIPBj66vD6", "202102", "129.7"));
    validateRow(response, List.of("jmIPBj66vD6", "202103", "115.2"));
    validateRow(response, List.of("jmIPBj66vD6", "202104", "131.4"));
    validateRow(response, List.of("jmIPBj66vD6", "202105", "125.8"));
    validateRow(response, List.of("jmIPBj66vD6", "202106", "111.7"));
    validateRow(response, List.of("jmIPBj66vD6", "202107", "100.7"));
    validateRow(response, List.of("jmIPBj66vD6", "202108", "106.8"));
    validateRow(response, List.of("jmIPBj66vD6", "202109", "116.8"));
    validateRow(response, List.of("jmIPBj66vD6", "202110", "102.4"));
    validateRow(response, List.of("jmIPBj66vD6", "202111", "106.3"));
    validateRow(response, List.of("jmIPBj66vD6", "202112", "104.4"));
    validateRow(response, List.of("TEQlaapDQoK", "202101", "155.3"));
    validateRow(response, List.of("TEQlaapDQoK", "202102", "158.9"));
    validateRow(response, List.of("TEQlaapDQoK", "202103", "136.9"));
    validateRow(response, List.of("TEQlaapDQoK", "202104", "131.9"));
    validateRow(response, List.of("TEQlaapDQoK", "202105", "146.2"));
    validateRow(response, List.of("TEQlaapDQoK", "202106", "172.1"));
    validateRow(response, List.of("TEQlaapDQoK", "202107", "149.9"));
    validateRow(response, List.of("TEQlaapDQoK", "202108", "159.9"));
    validateRow(response, List.of("TEQlaapDQoK", "202109", "178.8"));
    validateRow(response, List.of("TEQlaapDQoK", "202110", "162.2"));
    validateRow(response, List.of("TEQlaapDQoK", "202111", "173.7"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202101", "101.5"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202102", "111.1"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202103", "101.1"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202104", "95.4"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202105", "132.9"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202106", "107.0"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202107", "125.0"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202108", "157.2"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202109", "152.2"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202110", "100.0"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202111", "225.0"));
    validateRow(response, List.of("bL4ooGhyHRQ", "202112", "366.7"));
    validateRow(response, List.of("eIQbndfxQMb", "202101", "128.1"));
    validateRow(response, List.of("eIQbndfxQMb", "202102", "111.2"));
    validateRow(response, List.of("eIQbndfxQMb", "202103", "116.8"));
    validateRow(response, List.of("eIQbndfxQMb", "202104", "133.8"));
    validateRow(response, List.of("eIQbndfxQMb", "202105", "125.7"));
    validateRow(response, List.of("eIQbndfxQMb", "202106", "113.6"));
    validateRow(response, List.of("eIQbndfxQMb", "202107", "99.8"));
    validateRow(response, List.of("eIQbndfxQMb", "202108", "88.6"));
    validateRow(response, List.of("eIQbndfxQMb", "202109", "102.2"));
    validateRow(response, List.of("eIQbndfxQMb", "202110", "137.3"));
    validateRow(response, List.of("eIQbndfxQMb", "202111", "113.9"));
    validateRow(response, List.of("eIQbndfxQMb", "202112", "132.8"));
    validateRow(response, List.of("at6UHUQatSo", "202101", "96.0"));
    validateRow(response, List.of("at6UHUQatSo", "202102", "117.2"));
    validateRow(response, List.of("at6UHUQatSo", "202103", "115.0"));
    validateRow(response, List.of("at6UHUQatSo", "202104", "119.6"));
    validateRow(response, List.of("at6UHUQatSo", "202105", "159.8"));
    validateRow(response, List.of("at6UHUQatSo", "202106", "91.6"));
    validateRow(response, List.of("at6UHUQatSo", "202107", "232.5"));
    validateRow(response, List.of("at6UHUQatSo", "202108", "124.3"));
    validateRow(response, List.of("at6UHUQatSo", "202109", "110.3"));
    validateRow(response, List.of("at6UHUQatSo", "202110", "106.7"));
    validateRow(response, List.of("at6UHUQatSo", "202111", "123.9"));
    validateRow(response, List.of("at6UHUQatSo", "202112", "127.9"));
  }

  @Test
  public void queryAncLlitnCoverageThisYeargauge() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=pe:THIS_YEAR")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add("dimension=dx:Tt5TAvdfdVK")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"Tt5TAvdfdVK\":{\"name\":\"ANC LLITN coverage\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"2022\":{\"name\":\"2022\"}},\"dimensions\":{\"dx\":[\"Tt5TAvdfdVK\"],\"pe\":[\"2022\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("Tt5TAvdfdVK", "43.0"));
  }

  @Test
  public void queryAncReportingRateCoverageAndVisitsLast4QuartersDualaxis() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=ou:ImspTQPwCqd")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:QX4ZTUbOt3a.REPORTING_RATE;Uvn6LCg7dVU;OdiHJayrsKo;fbfJHSPpUQD;cYeuwXTCPkU,pe:LAST_4_QUARTERS")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(20)))
        .body("height", equalTo(20))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"OdiHJayrsKo\":{\"name\":\"ANC 2 Coverage\"},\"fbfJHSPpUQD\":{\"name\":\"ANC 1st visit\"},\"2021Q4\":{\"name\":\"October - December 2021\"},\"2021Q2\":{\"name\":\"April - June 2021\"},\"2021Q3\":{\"name\":\"July - September 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2021Q1\":{\"name\":\"January - March 2021\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"pe\":{\"name\":\"Period\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"cYeuwXTCPkU\":{\"name\":\"ANC 2nd visit\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"QX4ZTUbOt3a.REPORTING_RATE\":{\"name\":\"Reproductive Health - Reporting rate\"}},\"dimensions\":{\"dx\":[\"QX4ZTUbOt3a.REPORTING_RATE\",\"Uvn6LCg7dVU\",\"OdiHJayrsKo\",\"fbfJHSPpUQD\",\"cYeuwXTCPkU\"],\"pe\":[\"2021Q1\",\"2021Q2\",\"2021Q3\",\"2021Q4\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("Uvn6LCg7dVU", "2021Q1", "100.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "2021Q2", "118.1"));
    validateRow(response, List.of("Uvn6LCg7dVU", "2021Q3", "108.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "2021Q4", "87.9"));
    validateRow(response, List.of("OdiHJayrsKo", "2021Q1", "92.0"));
    validateRow(response, List.of("OdiHJayrsKo", "2021Q2", "108.6"));
    validateRow(response, List.of("OdiHJayrsKo", "2021Q3", "100.8"));
    validateRow(response, List.of("OdiHJayrsKo", "2021Q4", "82.5"));
    validateRow(response, List.of("fbfJHSPpUQD", "2021Q4", "54062"));
    validateRow(response, List.of("cYeuwXTCPkU", "2021Q3", "61976"));
    validateRow(response, List.of("cYeuwXTCPkU", "2021Q4", "50749"));
    validateRow(response, List.of("cYeuwXTCPkU", "2021Q1", "55331"));
    validateRow(response, List.of("cYeuwXTCPkU", "2021Q2", "66033"));
    validateRow(response, List.of("fbfJHSPpUQD", "2021Q1", "60689"));
    validateRow(response, List.of("fbfJHSPpUQD", "2021Q2", "71850"));
    validateRow(response, List.of("fbfJHSPpUQD", "2021Q3", "66668"));
    validateRow(response, List.of("QX4ZTUbOt3a.REPORTING_RATE", "2021Q4", "72.1"));
    validateRow(response, List.of("QX4ZTUbOt3a.REPORTING_RATE", "2021Q3", "89.4"));
    validateRow(response, List.of("QX4ZTUbOt3a.REPORTING_RATE", "2021Q2", "88"));
    validateRow(response, List.of("QX4ZTUbOt3a.REPORTING_RATE", "2021Q1", "84.9"));
  }

  @Test
  public void queryAncVisits13Last12Months() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("skipData=false")
        .add("includeNumDen=true")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=pe:LAST_12_MONTHS,ou:jUb8gELQApl;TEQlaapDQoK;eIQbndfxQMb;Vth0fbpFcsO;PMa2VCrupOd;O6uvpzGd5pu;bL4ooGhyHRQ;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;qhqAxPSTUXp;jmIPBj66vD6,dx:Uvn6LCg7dVU;OdiHJayrsKo;sB79w2hiLp8")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(444)))
        .body("height", equalTo(444))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"OdiHJayrsKo\":{\"name\":\"ANC 2 Coverage\"},\"202109\":{\"name\":\"September 2021\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202107\":{\"name\":\"July 2021\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"pe\":{\"name\":\"Period\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\",\"OdiHJayrsKo\",\"sB79w2hiLp8\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"jUb8gELQApl\",\"TEQlaapDQoK\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"O6uvpzGd5pu\",\"bL4ooGhyHRQ\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"at6UHUQatSo\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"jmIPBj66vD6\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "jUb8gELQApl", "78.9", "1219.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "TEQlaapDQoK", "101.3", "2016.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "eIQbndfxQMb", "91.2", "1370.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "Vth0fbpFcsO", "50.7", "741.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "PMa2VCrupOd", "117.2", "1359.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "O6uvpzGd5pu", "129.9", "2539.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "bL4ooGhyHRQ", "124.1", "1244.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "kJq2mPyFEHo", "91.1", "1956.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "fdc6uOvgoji", "88.3", "1523.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "at6UHUQatSo", "94.5", "3163.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "lc3eMKXaEfw", "99.5", "617.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "qhqAxPSTUXp", "74.3", "839.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202101", "jmIPBj66vD6", "126.7", "1440.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "jUb8gELQApl", "82.1", "1146.0", "18199.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "TEQlaapDQoK", "98.7", "1774.0", "23423.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "eIQbndfxQMb", "106.2", "1441.0", "17693.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "Vth0fbpFcsO", "90.9", "1199.0", "17196.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "PMa2VCrupOd", "89.0", "932.0", "13649.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "O6uvpzGd5pu", "140.2", "2475.0", "23012.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "bL4ooGhyHRQ", "111.7", "1011.0", "11803.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "kJq2mPyFEHo", "91.4", "1773.0", "25277.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "fdc6uOvgoji", "84.5", "1317.0", "20312.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "at6UHUQatSo", "98.1", "2965.0", "39405.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "lc3eMKXaEfw", "96.6", "541.0", "7299.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "qhqAxPSTUXp", "77.7", "793.0", "13299.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202102", "jmIPBj66vD6", "138.2", "1419.0", "13384.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "jUb8gELQApl", "81.8", "1264.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "TEQlaapDQoK", "93.0", "1850.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "eIQbndfxQMb", "97.5", "1465.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "Vth0fbpFcsO", "65.9", "963.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "PMa2VCrupOd", "88.2", "1023.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "O6uvpzGd5pu", "131.8", "2575.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "bL4ooGhyHRQ", "130.3", "1306.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "kJq2mPyFEHo", "96.6", "2074.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "fdc6uOvgoji", "90.8", "1566.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "at6UHUQatSo", "149.1", "4989.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "lc3eMKXaEfw", "86.1", "534.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "qhqAxPSTUXp", "72.6", "820.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202103", "jmIPBj66vD6", "127.4", "1448.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "jUb8gELQApl", "81.4", "1217.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "TEQlaapDQoK", "87.3", "1681.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "eIQbndfxQMb", "96.9", "1409.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "Vth0fbpFcsO", "69.4", "981.0", "17196.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "PMa2VCrupOd", "95.7", "1074.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "O6uvpzGd5pu", "131.2", "2482.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "bL4ooGhyHRQ", "103.6", "1005.0", "11803.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "kJq2mPyFEHo", "85.6", "1778.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "fdc6uOvgoji", "82.7", "1380.0", "20312.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "at6UHUQatSo", "93.8", "3038.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "lc3eMKXaEfw", "87.2", "523.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "qhqAxPSTUXp", "76.9", "841.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202104", "jmIPBj66vD6", "106.1", "1167.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "jUb8gELQApl", "97.1", "1501.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "TEQlaapDQoK", "168.1", "3345.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "eIQbndfxQMb", "165.8", "2492.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "Vth0fbpFcsO", "56.0", "818.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "PMa2VCrupOd", "127.8", "1481.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "O6uvpzGd5pu", "157.4", "3076.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "bL4ooGhyHRQ", "136.0", "1363.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "kJq2mPyFEHo", "140.3", "3013.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "fdc6uOvgoji", "123.0", "2122.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "at6UHUQatSo", "204.3", "6836.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "lc3eMKXaEfw", "139.4", "864.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "qhqAxPSTUXp", "92.3", "1042.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202105", "jmIPBj66vD6", "132.7", "1508.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "jUb8gELQApl", "91.3", "1365.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "TEQlaapDQoK", "140.4", "2703.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "eIQbndfxQMb", "143.2", "2083.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "Vth0fbpFcsO", "64.6", "913.0", "17196.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "PMa2VCrupOd", "118.2", "1326.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "O6uvpzGd5pu", "145.3", "2748.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "bL4ooGhyHRQ", "139.0", "1348.0", "11803.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "kJq2mPyFEHo", "99.4", "2066.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "fdc6uOvgoji", "102.9", "1718.0", "20312.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "at6UHUQatSo", "145.2", "4702.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "lc3eMKXaEfw", "132.7", "796.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "qhqAxPSTUXp", "59.9", "655.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202106", "jmIPBj66vD6", "126.4", "1390.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "jUb8gELQApl", "88.0", "1360.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "TEQlaapDQoK", "118.0", "2347.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "eIQbndfxQMb", "113.7", "1709.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "Vth0fbpFcsO", "59.2", "865.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "PMa2VCrupOd", "101.0", "1171.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "O6uvpzGd5pu", "160.3", "3132.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "bL4ooGhyHRQ", "126.0", "1263.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "kJq2mPyFEHo", "86.4", "1854.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "fdc6uOvgoji", "92.7", "1599.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "at6UHUQatSo", "121.7", "4072.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "lc3eMKXaEfw", "120.8", "749.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "qhqAxPSTUXp", "66.0", "746.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202107", "jmIPBj66vD6", "131.0", "1489.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "jUb8gELQApl", "86.2", "1332.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "TEQlaapDQoK", "112.4", "2236.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "eIQbndfxQMb", "138.6", "2082.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "Vth0fbpFcsO", "51.1", "746.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "PMa2VCrupOd", "90.6", "1050.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "O6uvpzGd5pu", "147.1", "2875.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "bL4ooGhyHRQ", "106.2", "1065.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "kJq2mPyFEHo", "85.9", "1845.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "fdc6uOvgoji", "94.1", "1624.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "at6UHUQatSo", "125.7", "4208.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "lc3eMKXaEfw", "114.4", "709.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "qhqAxPSTUXp", "88.9", "1004.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202108", "jmIPBj66vD6", "108.0", "1228.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "jUb8gELQApl", "79.7", "1192.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "TEQlaapDQoK", "105.6", "2033.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "eIQbndfxQMb", "153.2", "2228.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "Vth0fbpFcsO", "62.0", "876.0", "17196.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "PMa2VCrupOd", "101.6", "1140.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "O6uvpzGd5pu", "169.9", "3213.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "bL4ooGhyHRQ", "106.8", "1036.0", "11803.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "kJq2mPyFEHo", "97.9", "2034.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "fdc6uOvgoji", "79.2", "1323.0", "20312.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "at6UHUQatSo", "123.6", "4004.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "lc3eMKXaEfw", "118.7", "712.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "qhqAxPSTUXp", "98.8", "1080.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202109", "jmIPBj66vD6", "130.6", "1437.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202110", "jUb8gELQApl", "73.2", "1131.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202110", "TEQlaapDQoK", "89.7", "1785.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202110", "eIQbndfxQMb", "144.8", "2176.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202110", "PMa2VCrupOd", "115.8", "1342.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202110", "O6uvpzGd5pu", "144.6", "2827.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202110", "bL4ooGhyHRQ", "0.7", "7.0", "11803.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202110", "kJq2mPyFEHo", "94.8", "2036.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202110", "fdc6uOvgoji", "79.9", "1379.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202110", "at6UHUQatSo", "118.7", "3974.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202110", "jmIPBj66vD6", "111.6", "1269.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "jUb8gELQApl", "84.6", "1266.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "TEQlaapDQoK", "104.0", "2003.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "eIQbndfxQMb", "148.9", "2165.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "PMa2VCrupOd", "100.5", "1127.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "O6uvpzGd5pu", "193.3", "3656.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "bL4ooGhyHRQ", "0.82", "8.0", "11803.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "kJq2mPyFEHo", "96.7", "2009.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "at6UHUQatSo", "132.5", "4290.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "lc3eMKXaEfw", "109.7", "658.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "qhqAxPSTUXp", "115.6", "1264.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202111", "jmIPBj66vD6", "113.2", "1245.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202112", "jUb8gELQApl", "74.5", "1152.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202112", "eIQbndfxQMb", "125.4", "1885.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202112", "Vth0fbpFcsO", "78.9", "1152.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202112", "PMa2VCrupOd", "111.7", "1295.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202112", "O6uvpzGd5pu", "110.9", "2167.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202112", "bL4ooGhyHRQ", "0.6", "6.0", "11803.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202112", "kJq2mPyFEHo", "88.9", "1909.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202112", "fdc6uOvgoji", "81.7", "1410.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202112", "at6UHUQatSo", "115.9", "3878.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("Uvn6LCg7dVU", "202112", "jmIPBj66vD6", "99.3", "1129.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "jUb8gELQApl", "79.4", "1228.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "TEQlaapDQoK", "71.6", "1424.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "eIQbndfxQMb", "73.6", "1106.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "Vth0fbpFcsO", "33.0", "482.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "PMa2VCrupOd", "103.7", "1202.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "O6uvpzGd5pu", "113.6", "2220.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "bL4ooGhyHRQ", "99.8", "1000.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "kJq2mPyFEHo", "107.3", "2304.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "fdc6uOvgoji", "72.1", "1244.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "at6UHUQatSo", "79.3", "2655.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "lc3eMKXaEfw", "83.1", "515.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "qhqAxPSTUXp", "62.6", "707.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202101", "jmIPBj66vD6", "104.0", "1182.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "jUb8gELQApl", "81.4", "1136.0", "18199.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "TEQlaapDQoK", "79.9", "1435.0", "23423.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "eIQbndfxQMb", "91.1", "1237.0", "17693.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "Vth0fbpFcsO", "96.4", "1272.0", "17196.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "PMa2VCrupOd", "94.4", "988.0", "13649.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "O6uvpzGd5pu", "144.6", "2552.0", "23012.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "bL4ooGhyHRQ", "95.5", "865.0", "11803.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "kJq2mPyFEHo", "117.7", "2282.0", "25277.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "fdc6uOvgoji", "77.5", "1207.0", "20312.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "at6UHUQatSo", "100.3", "3032.0", "39405.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "lc3eMKXaEfw", "95.0", "532.0", "7299.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "qhqAxPSTUXp", "63.2", "645.0", "13299.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202102", "jmIPBj66vD6", "127.1", "1305.0", "13384.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "jUb8gELQApl", "91.9", "1420.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "TEQlaapDQoK", "71.5", "1423.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "eIQbndfxQMb", "94.0", "1413.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "Vth0fbpFcsO", "69.4", "1013.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "PMa2VCrupOd", "87.9", "1019.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "O6uvpzGd5pu", "129.0", "2522.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "bL4ooGhyHRQ", "98.0", "982.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "kJq2mPyFEHo", "106.9", "2294.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "fdc6uOvgoji", "70.0", "1207.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "at6UHUQatSo", "115.3", "3858.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "lc3eMKXaEfw", "84.7", "525.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "qhqAxPSTUXp", "59.2", "669.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202103", "jmIPBj66vD6", "108.1", "1229.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "jUb8gELQApl", "77.1", "1154.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "TEQlaapDQoK", "70.7", "1362.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "eIQbndfxQMb", "85.7", "1246.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "Vth0fbpFcsO", "79.8", "1128.0", "17196.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "PMa2VCrupOd", "85.9", "964.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "O6uvpzGd5pu", "132.7", "2509.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "bL4ooGhyHRQ", "79.6", "772.0", "11803.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "kJq2mPyFEHo", "109.8", "2282.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "fdc6uOvgoji", "72.6", "1212.0", "20312.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "at6UHUQatSo", "102.7", "3326.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "lc3eMKXaEfw", "86.0", "516.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "qhqAxPSTUXp", "66.9", "731.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202104", "jmIPBj66vD6", "109.2", "1201.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "jUb8gELQApl", "101.6", "1571.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "TEQlaapDQoK", "99.3", "1975.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "eIQbndfxQMb", "112.9", "1697.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "Vth0fbpFcsO", "55.8", "815.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "PMa2VCrupOd", "100.7", "1167.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "O6uvpzGd5pu", "155.1", "3031.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "bL4ooGhyHRQ", "99.0", "992.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "kJq2mPyFEHo", "147.4", "3164.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "fdc6uOvgoji", "98.0", "1690.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "at6UHUQatSo", "147.7", "4942.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "lc3eMKXaEfw", "111.1", "689.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "qhqAxPSTUXp", "62.7", "708.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202105", "jmIPBj66vD6", "113.0", "1285.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "jUb8gELQApl", "103.1", "1542.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "TEQlaapDQoK", "102.0", "1964.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "eIQbndfxQMb", "110.2", "1602.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "Vth0fbpFcsO", "67.2", "950.0", "17196.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "PMa2VCrupOd", "107.3", "1204.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "O6uvpzGd5pu", "191.1", "3615.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "bL4ooGhyHRQ", "122.2", "1185.0", "11803.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "kJq2mPyFEHo", "140.7", "2924.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "fdc6uOvgoji", "89.9", "1501.0", "20312.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "at6UHUQatSo", "148.6", "4814.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "lc3eMKXaEfw", "139.2", "835.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "qhqAxPSTUXp", "47.3", "517.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202106", "jmIPBj66vD6", "113.7", "1251.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "jUb8gELQApl", "89.4", "1382.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "TEQlaapDQoK", "90.1", "1793.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "eIQbndfxQMb", "94.3", "1417.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "Vth0fbpFcsO", "64.7", "945.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "PMa2VCrupOd", "96.7", "1121.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "O6uvpzGd5pu", "148.3", "2898.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "bL4ooGhyHRQ", "103.6", "1039.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "kJq2mPyFEHo", "110.1", "2364.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "fdc6uOvgoji", "76.7", "1324.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "at6UHUQatSo", "123.3", "4125.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "lc3eMKXaEfw", "126.0", "781.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "qhqAxPSTUXp", "55.4", "626.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202107", "jmIPBj66vD6", "115.7", "1315.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "jUb8gELQApl", "85.8", "1326.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "TEQlaapDQoK", "90.2", "1794.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "eIQbndfxQMb", "111.9", "1681.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "Vth0fbpFcsO", "44.0", "643.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "PMa2VCrupOd", "92.9", "1077.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "O6uvpzGd5pu", "160.7", "3140.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "bL4ooGhyHRQ", "106.2", "1065.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "kJq2mPyFEHo", "103.4", "2219.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "fdc6uOvgoji", "78.5", "1355.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "at6UHUQatSo", "102.1", "3418.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "lc3eMKXaEfw", "115.0", "713.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "qhqAxPSTUXp", "72.3", "817.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202108", "jmIPBj66vD6", "102.5", "1165.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "jUb8gELQApl", "76.9", "1151.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "TEQlaapDQoK", "106.6", "2052.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "eIQbndfxQMb", "106.4", "1547.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "Vth0fbpFcsO", "57.2", "809.0", "17196.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "PMa2VCrupOd", "94.9", "1065.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "O6uvpzGd5pu", "150.9", "2854.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "bL4ooGhyHRQ", "109.9", "1066.0", "11803.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "kJq2mPyFEHo", "105.4", "2189.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "fdc6uOvgoji", "70.0", "1169.0", "20312.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "at6UHUQatSo", "113.4", "3674.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "lc3eMKXaEfw", "111.5", "669.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "qhqAxPSTUXp", "82.7", "904.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202109", "jmIPBj66vD6", "116.7", "1284.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202110", "jUb8gELQApl", "82.2", "1271.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202110", "TEQlaapDQoK", "81.8", "1628.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202110", "eIQbndfxQMb", "113.7", "1709.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202110", "PMa2VCrupOd", "92.4", "1071.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202110", "O6uvpzGd5pu", "109.6", "2143.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202110", "bL4ooGhyHRQ", "1.1", "11.0", "11803.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202110", "kJq2mPyFEHo", "124.2", "2666.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202110", "fdc6uOvgoji", "72.5", "1251.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202110", "at6UHUQatSo", "95.1", "3183.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202110", "jmIPBj66vD6", "103.8", "1180.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "jUb8gELQApl", "91.5", "1368.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "TEQlaapDQoK", "92.4", "1778.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "eIQbndfxQMb", "120.3", "1750.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "PMa2VCrupOd", "101.1", "1134.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "O6uvpzGd5pu", "179.4", "3393.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "bL4ooGhyHRQ", "1.6", "16.0", "11803.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "kJq2mPyFEHo", "112.6", "2339.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "at6UHUQatSo", "152.4", "4935.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "lc3eMKXaEfw", "108.7", "652.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "qhqAxPSTUXp", "88.7", "970.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202111", "jmIPBj66vD6", "101.6", "1118.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202112", "jUb8gELQApl", "76.4", "1181.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202112", "eIQbndfxQMb", "109.3", "1642.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202112", "Vth0fbpFcsO", "74.7", "1091.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202112", "PMa2VCrupOd", "92.5", "1072.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202112", "O6uvpzGd5pu", "109.8", "2146.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202112", "bL4ooGhyHRQ", "0.6", "6.0", "11803.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202112", "kJq2mPyFEHo", "114.2", "2451.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202112", "fdc6uOvgoji", "79.6", "1373.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202112", "at6UHUQatSo", "89.2", "2986.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("OdiHJayrsKo", "202112", "jmIPBj66vD6", "88.3", "1004.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "jUb8gELQApl", "62.2", "961.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "TEQlaapDQoK", "41.3", "821.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "eIQbndfxQMb", "41.7", "627.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "Vth0fbpFcsO", "19.0", "278.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "PMa2VCrupOd", "67.5", "783.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "O6uvpzGd5pu", "71.0", "1387.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "bL4ooGhyHRQ", "71.9", "721.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "kJq2mPyFEHo", "72.6", "1558.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "fdc6uOvgoji", "50.1", "865.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "at6UHUQatSo", "55.7", "1864.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "lc3eMKXaEfw", "70.3", "436.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "qhqAxPSTUXp", "39.6", "447.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202101", "jmIPBj66vD6", "92.8", "1055.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "jUb8gELQApl", "64.4", "899.0", "18199.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "TEQlaapDQoK", "46.4", "833.0", "23423.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "eIQbndfxQMb", "52.5", "712.0", "17693.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "Vth0fbpFcsO", "61.9", "817.0", "17196.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "PMa2VCrupOd", "66.0", "691.0", "13649.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "O6uvpzGd5pu", "71.9", "1269.0", "23012.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "bL4ooGhyHRQ", "74.7", "676.0", "11803.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "kJq2mPyFEHo", "84.8", "1645.0", "25277.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "fdc6uOvgoji", "53.7", "837.0", "20312.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "at6UHUQatSo", "63.1", "1907.0", "39405.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "lc3eMKXaEfw", "57.3", "321.0", "7299.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "qhqAxPSTUXp", "42.8", "437.0", "13299.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202102", "jmIPBj66vD6", "100.8", "1035.0", "13384.0", "1303.6",
            "36500", "28"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "jUb8gELQApl", "79.5", "1229.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "TEQlaapDQoK", "49.8", "990.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "eIQbndfxQMb", "52.4", "788.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "Vth0fbpFcsO", "47.9", "699.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "PMa2VCrupOd", "62.4", "723.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "O6uvpzGd5pu", "77.0", "1504.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "bL4ooGhyHRQ", "79.3", "795.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "kJq2mPyFEHo", "82.1", "1763.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "fdc6uOvgoji", "51.1", "882.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "at6UHUQatSo", "75.6", "2531.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "lc3eMKXaEfw", "62.1", "385.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "qhqAxPSTUXp", "43.6", "492.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202103", "jmIPBj66vD6", "101.4", "1153.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "jUb8gELQApl", "67.5", "1010.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "TEQlaapDQoK", "41.2", "794.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "eIQbndfxQMb", "56.3", "819.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "Vth0fbpFcsO", "58.4", "826.0", "17196.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "PMa2VCrupOd", "61.2", "687.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "O6uvpzGd5pu", "80.0", "1513.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "bL4ooGhyHRQ", "61.7", "599.0", "11803.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "kJq2mPyFEHo", "87.0", "1808.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "fdc6uOvgoji", "54.5", "910.0", "20312.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "at6UHUQatSo", "51.6", "1672.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "lc3eMKXaEfw", "48.3", "290.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "qhqAxPSTUXp", "42.9", "469.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202104", "jmIPBj66vD6", "97.3", "1070.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "jUb8gELQApl", "81.5", "1260.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "TEQlaapDQoK", "62.9", "1252.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "eIQbndfxQMb", "68.9", "1036.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "Vth0fbpFcsO", "39.8", "581.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "PMa2VCrupOd", "76.3", "884.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "O6uvpzGd5pu", "86.6", "1693.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "bL4ooGhyHRQ", "73.8", "740.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "kJq2mPyFEHo", "104.8", "2249.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "fdc6uOvgoji", "64.7", "1117.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "at6UHUQatSo", "84.2", "2819.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "lc3eMKXaEfw", "64.4", "399.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "qhqAxPSTUXp", "45.1", "509.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202105", "jmIPBj66vD6", "102.0", "1160.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "jUb8gELQApl", "81.2", "1214.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "TEQlaapDQoK", "62.4", "1202.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "eIQbndfxQMb", "62.0", "901.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "Vth0fbpFcsO", "48.4", "684.0", "17196.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "PMa2VCrupOd", "70.0", "785.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "O6uvpzGd5pu", "90.1", "1705.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "bL4ooGhyHRQ", "89.0", "863.0", "11803.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "kJq2mPyFEHo", "108.2", "2247.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "fdc6uOvgoji", "61.5", "1027.0", "20312.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "at6UHUQatSo", "92.2", "2985.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "lc3eMKXaEfw", "85.7", "514.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "qhqAxPSTUXp", "44.6", "487.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202106", "jmIPBj66vD6", "97.6", "1074.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "jUb8gELQApl", "75.4", "1166.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "TEQlaapDQoK", "54.3", "1081.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "eIQbndfxQMb", "49.2", "740.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "Vth0fbpFcsO", "46.6", "680.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "PMa2VCrupOd", "63.8", "740.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "O6uvpzGd5pu", "91.7", "1792.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "bL4ooGhyHRQ", "76.3", "765.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "kJq2mPyFEHo", "85.1", "1826.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "fdc6uOvgoji", "60.8", "1049.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "at6UHUQatSo", "85.5", "2863.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "lc3eMKXaEfw", "94.7", "587.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "qhqAxPSTUXp", "40.2", "454.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202107", "jmIPBj66vD6", "97.6", "1109.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "jUb8gELQApl", "71.6", "1107.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "TEQlaapDQoK", "57.3", "1140.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "eIQbndfxQMb", "64.9", "975.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "Vth0fbpFcsO", "35.3", "515.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "PMa2VCrupOd", "62.1", "720.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "O6uvpzGd5pu", "86.0", "1680.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "bL4ooGhyHRQ", "81.0", "812.0", "11803.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "kJq2mPyFEHo", "85.9", "1845.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "fdc6uOvgoji", "67.0", "1155.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "at6UHUQatSo", "79.2", "2649.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "lc3eMKXaEfw", "77.8", "482.0", "7299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "qhqAxPSTUXp", "49.3", "557.0", "13299.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202108", "jmIPBj66vD6", "90.2", "1025.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "jUb8gELQApl", "68.1", "1018.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "TEQlaapDQoK", "59.7", "1150.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "eIQbndfxQMb", "67.3", "978.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "Vth0fbpFcsO", "50.0", "707.0", "17196.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "PMa2VCrupOd", "71.7", "804.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "O6uvpzGd5pu", "102.1", "1931.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "bL4ooGhyHRQ", "89.0", "863.0", "11803.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "kJq2mPyFEHo", "92.2", "1916.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "fdc6uOvgoji", "49.0", "818.0", "20312.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "at6UHUQatSo", "86.8", "2810.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "lc3eMKXaEfw", "80.2", "481.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "qhqAxPSTUXp", "66.0", "721.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202109", "jmIPBj66vD6", "100.8", "1109.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202110", "jUb8gELQApl", "69.8", "1079.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202110", "TEQlaapDQoK", "52.6", "1047.0", "23423.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202110", "eIQbndfxQMb", "72.1", "1083.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202110", "PMa2VCrupOd", "67.1", "778.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202110", "O6uvpzGd5pu", "91.6", "1790.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202110", "bL4ooGhyHRQ", "0.6", "6.0", "11803.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202110", "kJq2mPyFEHo", "87.0", "1868.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202110", "fdc6uOvgoji", "56.6", "976.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202110", "at6UHUQatSo", "65.9", "2206.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202110", "jmIPBj66vD6", "87.3", "992.0", "13384.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "jUb8gELQApl", "82.4", "1233.0", "18199.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "TEQlaapDQoK", "57.8", "1112.0", "23423.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "eIQbndfxQMb", "68.7", "999.0", "17693.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "PMa2VCrupOd", "72.2", "810.0", "13649.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "O6uvpzGd5pu", "206.5", "3906.0", "23012.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "bL4ooGhyHRQ", "1.2", "12.0", "11803.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "kJq2mPyFEHo", "87.5", "1817.0", "25277.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "at6UHUQatSo", "84.3", "2730.0", "39405.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "lc3eMKXaEfw", "92.3", "554.0", "7299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "qhqAxPSTUXp", "62.8", "686.0", "13299.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202111", "jmIPBj66vD6", "92.4", "1017.0", "13384.0", "1216.7",
            "36500", "30"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202112", "jUb8gELQApl", "64.6", "999.0", "18199.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202112", "eIQbndfxQMb", "62.0", "931.0", "17693.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202112", "Vth0fbpFcsO", "47.3", "691.0", "17196.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202112", "PMa2VCrupOd", "57.7", "669.0", "13649.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202112", "O6uvpzGd5pu", "79.6", "1556.0", "23012.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202112", "bL4ooGhyHRQ", "0.4", "4.0", "11803.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202112", "kJq2mPyFEHo", "85.5", "1836.0", "25277.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202112", "fdc6uOvgoji", "51.2", "883.0", "20312.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202112", "at6UHUQatSo", "67.1", "2245.0", "39405.0", "1177.4",
            "36500", "31"));
    validateRow(response,
        List.of("sB79w2hiLp8", "202112", "jmIPBj66vD6", "72.2", "821.0", "13384.0", "1177.4",
            "36500", "31"));
  }

  @Test
  public void queryAncVisitsAndCommodities() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=ou:ImspTQPwCqd")
        .add("skipData=false")
        .add("includeNumDen=true")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:fbfJHSPpUQD.pq2XI5kz2BY;fbfJHSPpUQD.PT59n8BQbqM;cYeuwXTCPkU.pq2XI5kz2BY;cYeuwXTCPkU.PT59n8BQbqM;o15CyZiTvxa;f27B1G7B3m3;hJNC4Bu2Mkv,pe:LAST_12_MONTHS")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(84)))
        .body("height", equalTo(84))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"202109\":{\"name\":\"September 2021\"},\"fbfJHSPpUQD.pq2XI5kz2BY\":{\"name\":\"ANC 1st visit Fixed\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"rQLFnNXXIL0\":{\"name\":\"End Balance\"},\"dx\":{\"name\":\"Data\"},\"f27B1G7B3m3\":{\"name\":\"Commodities - Misoprostol\"},\"KPP63zJPkOu\":{\"name\":\"Quantity to be ordered\"},\"o15CyZiTvxa\":{\"name\":\"Commodities - Magnesium Sulfate\"},\"ou\":{\"name\":\"Organisation unit\"},\"cYeuwXTCPkU.PT59n8BQbqM\":{\"name\":\"ANC 2nd visit Outreach\"},\"hJNC4Bu2Mkv\":{\"name\":\"Commodities - Oxytocin\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"J2Qf1jtZuj8\":{\"name\":\"Consumption\"},\"pe\":{\"name\":\"Period\"},\"fbfJHSPpUQD.PT59n8BQbqM\":{\"name\":\"ANC 1st visit Outreach\"},\"cYeuwXTCPkU.pq2XI5kz2BY\":{\"name\":\"ANC 2nd visit Fixed\"}},\"dimensions\":{\"dx\":[\"fbfJHSPpUQD.pq2XI5kz2BY\",\"fbfJHSPpUQD.PT59n8BQbqM\",\"cYeuwXTCPkU.pq2XI5kz2BY\",\"cYeuwXTCPkU.PT59n8BQbqM\",\"o15CyZiTvxa\",\"f27B1G7B3m3\",\"hJNC4Bu2Mkv\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"rQLFnNXXIL0\",\"J2Qf1jtZuj8\",\"KPP63zJPkOu\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 3, "numerator", "Numerator", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 4, "denominator", "Denominator", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 5, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 7, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("f27B1G7B3m3", "202106", "338018", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202105", "281221", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202104", "275225", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202103", "337450", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202102", "333636", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202101", "282937", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202111", "326619", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202110", "333007", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202109", "357700", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202108", "327385", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202107", "351571", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202109", "358786", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202106", "338359", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202108", "326762", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202105", "279895", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202107", "351521", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202104", "274644", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202103", "337395", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202102", "333185", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202101", "283655", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202112", "346083", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202109", "357990", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202112", "343844", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202111", "327480", "", "", "", "", ""));
    validateRow(response, List.of("f27B1G7B3m3", "202110", "331789", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202108", "327724", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202107", "351604", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202106", "339639", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202112", "346149", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202105", "280994", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202111", "327714", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202104", "275807", "", "", "", "", ""));
    validateRow(response, List.of("hJNC4Bu2Mkv", "202110", "331829", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202103", "337982", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202102", "333164", "", "", "", "", ""));
    validateRow(response, List.of("o15CyZiTvxa", "202101", "283886", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202106", "4597", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202105", "4439", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202108", "4047", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202107", "4512", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202110", "14345", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202102", "4842", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202111", "14999", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202112", "13380", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202101", "4204", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202102", "13646", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202104", "4419", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202101", "13065", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202103", "4165", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202104", "13984", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202103", "15409", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202106", "19307", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202105", "19287", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202101", "4795", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202108", "16366", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202107", "16618", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202109", "16071", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202104", "14220", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202105", "24206", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202106", "18997", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202111", "4175", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202107", "17677", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202110", "3316", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202108", "17623", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202109", "17640", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202106", "4816", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202107", "4679", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202108", "4381", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202109", "4668", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202111", "15278", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202102", "4780", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202103", "4323", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202101", "15231", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202112", "2790", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202110", "12797", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202104", "4356", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202102", "14006", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202105", "5255", "", "", "", "", ""));
    validateRow(response,
        List.of("fbfJHSPpUQD.pq2XI5kz2BY", "202103", "17554", "", "", "", "", ""));
    validateRow(response,
        List.of("cYeuwXTCPkU.pq2XI5kz2BY", "202112", "12393", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202110", "3581", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202111", "4692", "", "", "", "", ""));
    validateRow(response, List.of("fbfJHSPpUQD.PT59n8BQbqM", "202112", "3065", "", "", "", "", ""));
    validateRow(response, List.of("cYeuwXTCPkU.PT59n8BQbqM", "202109", "4362", "", "", "", "", ""));
  }
}