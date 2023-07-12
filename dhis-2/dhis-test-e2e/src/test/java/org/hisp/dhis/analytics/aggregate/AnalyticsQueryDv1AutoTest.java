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
public class AnalyticsQueryDv1AutoTest extends AnalyticsApiTest {

  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void query1And3CoverageYearly() throws JSONException {
// Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=pe:THIS_YEAR")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add("dimension=dx:Uvn6LCg7dVU;sB79w2hiLp8,ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN")
        .add("relativePeriodDate=2022-01-01");

// When 
    ApiResponse response = actions.get(params);

// Then 
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(28)))
        .body("height", equalTo(28))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData. 
    String expectedMetaData = "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"2022\":{\"name\":\"2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\",\"sB79w2hiLp8\"],\"pe\":[\"2022\"],\"ou\":[\"ImspTQPwCqd\",\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("Uvn6LCg7dVU", "ImspTQPwCqd", "101.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "O6uvpzGd5pu", "142.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "fdc6uOvgoji", "82.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "lc3eMKXaEfw", "90.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jUb8gELQApl", "81.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "PMa2VCrupOd", "102.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "kJq2mPyFEHo", "94.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "qhqAxPSTUXp", "67.0"));
    validateRow(response, List.of("Uvn6LCg7dVU", "Vth0fbpFcsO", "52.8"));
    validateRow(response, List.of("Uvn6LCg7dVU", "jmIPBj66vD6", "118.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "TEQlaapDQoK", "99.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "bL4ooGhyHRQ", "88.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "eIQbndfxQMb", "124.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "at6UHUQatSo", "124.7"));
    validateRow(response, List.of("sB79w2hiLp8", "ImspTQPwCqd", "65.8"));
    validateRow(response, List.of("sB79w2hiLp8", "O6uvpzGd5pu", "92.3"));
    validateRow(response, List.of("sB79w2hiLp8", "fdc6uOvgoji", "51.0"));
    validateRow(response, List.of("sB79w2hiLp8", "lc3eMKXaEfw", "59.7"));
    validateRow(response, List.of("sB79w2hiLp8", "jUb8gELQApl", "71.0"));
    validateRow(response, List.of("sB79w2hiLp8", "PMa2VCrupOd", "65.2"));
    validateRow(response, List.of("sB79w2hiLp8", "kJq2mPyFEHo", "86.8"));
    validateRow(response, List.of("sB79w2hiLp8", "qhqAxPSTUXp", "38.8"));
    validateRow(response, List.of("sB79w2hiLp8", "Vth0fbpFcsO", "36.9"));
    validateRow(response, List.of("sB79w2hiLp8", "jmIPBj66vD6", "92.4"));
    validateRow(response, List.of("sB79w2hiLp8", "TEQlaapDQoK", "47.8"));
    validateRow(response, List.of("sB79w2hiLp8", "bL4ooGhyHRQ", "56.9"));
    validateRow(response, List.of("sB79w2hiLp8", "eIQbndfxQMb", "58.7"));
    validateRow(response, List.of("sB79w2hiLp8", "at6UHUQatSo", "72.8"));
  }

  @Test
  public void query13DropoutRateYearly() throws JSONException {
// Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=pe:THIS_YEAR")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:ReUHfIn0pTQ,ou:O6uvpzGd5pu;fdc6uOvgoji;lc3eMKXaEfw;jUb8gELQApl;PMa2VCrupOd;kJq2mPyFEHo;qhqAxPSTUXp;Vth0fbpFcsO;jmIPBj66vD6;TEQlaapDQoK;bL4ooGhyHRQ;eIQbndfxQMb;at6UHUQatSo")
        .add("relativePeriodDate=2022-01-01");

// When 
    ApiResponse response = actions.get(params);

// Then 
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(13)))
        .body("height", equalTo(13))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData. 
    String expectedMetaData = "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"2022\":{\"name\":\"2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"ReUHfIn0pTQ\":{\"name\":\"ANC 1-3 Dropout Rate\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"ReUHfIn0pTQ\"],\"pe\":[\"2022\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("ReUHfIn0pTQ", "O6uvpzGd5pu", "34.4"));
    validateRow(response, List.of("ReUHfIn0pTQ", "fdc6uOvgoji", "38.0"));
    validateRow(response, List.of("ReUHfIn0pTQ", "lc3eMKXaEfw", "33.6"));
    validateRow(response, List.of("ReUHfIn0pTQ", "jUb8gELQApl", "13.0"));
    validateRow(response, List.of("ReUHfIn0pTQ", "PMa2VCrupOd", "36.6"));
    validateRow(response, List.of("ReUHfIn0pTQ", "kJq2mPyFEHo", "8.1"));
    validateRow(response, List.of("ReUHfIn0pTQ", "qhqAxPSTUXp", "42.1"));
    validateRow(response, List.of("ReUHfIn0pTQ", "Vth0fbpFcsO", "30.0"));
    validateRow(response, List.of("ReUHfIn0pTQ", "jmIPBj66vD6", "21.9"));
    validateRow(response, List.of("ReUHfIn0pTQ", "TEQlaapDQoK", "52.0"));
    validateRow(response, List.of("ReUHfIn0pTQ", "bL4ooGhyHRQ", "35.7"));
    validateRow(response, List.of("ReUHfIn0pTQ", "eIQbndfxQMb", "52.9"));
    validateRow(response, List.of("ReUHfIn0pTQ", "at6UHUQatSo", "41.6"));
  }

  @Test
  public void query13TrendLinesLast12Months() throws JSONException {
// Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=ou:ImspTQPwCqd")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add("dimension=dx:Uvn6LCg7dVU;OdiHJayrsKo;sB79w2hiLp8,pe:LAST_12_MONTHS")
        .add("relativePeriodDate=2022-01-01");

// When 
    ApiResponse response = actions.get(params);

// Then 
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(36)))
        .body("height", equalTo(36))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData. 
    String expectedMetaData = "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"ou\":{\"name\":\"Organisation unit\"},\"OdiHJayrsKo\":{\"name\":\"ANC 2 Coverage\"},\"202109\":{\"name\":\"September 2021\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202101\":{\"name\":\"January 2021\"},\"202112\":{\"name\":\"December 2021\"},\"202102\":{\"name\":\"February 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\",\"OdiHJayrsKo\",\"sB79w2hiLp8\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("Uvn6LCg7dVU", "202101", "96.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202102", "100.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202103", "105.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202104", "92.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202105", "142.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202106", "118.8"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202107", "107.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202108", "106.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202109", "111.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202110", "86.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202111", "98.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202112", "79.4"));
    validateRow(response, List.of("OdiHJayrsKo", "202101", "83.3"));
    validateRow(response, List.of("OdiHJayrsKo", "202102", "98.8"));
    validateRow(response, List.of("OdiHJayrsKo", "202103", "94.5"));
    validateRow(response, List.of("OdiHJayrsKo", "202104", "91.8"));
    validateRow(response, List.of("OdiHJayrsKo", "202105", "114.5"));
    validateRow(response, List.of("OdiHJayrsKo", "202106", "119.2"));
    validateRow(response, List.of("OdiHJayrsKo", "202107", "102.0"));
    validateRow(response, List.of("OdiHJayrsKo", "202108", "98.5"));
    validateRow(response, List.of("OdiHJayrsKo", "202109", "101.9"));
    validateRow(response, List.of("OdiHJayrsKo", "202110", "77.8"));
    validateRow(response, List.of("OdiHJayrsKo", "202111", "97.0"));
    validateRow(response, List.of("OdiHJayrsKo", "202112", "73.3"));
    validateRow(response, List.of("sB79w2hiLp8", "202101", "57.0"));
    validateRow(response, List.of("sB79w2hiLp8", "202102", "64.5"));
    validateRow(response, List.of("sB79w2hiLp8", "202103", "67.3"));
    validateRow(response, List.of("sB79w2hiLp8", "202104", "62.2"));
    validateRow(response, List.of("sB79w2hiLp8", "202105", "75.8"));
    validateRow(response, List.of("sB79w2hiLp8", "202106", "78.2"));
    validateRow(response, List.of("sB79w2hiLp8", "202107", "71.7"));
    validateRow(response, List.of("sB79w2hiLp8", "202108", "70.8"));
    validateRow(response, List.of("sB79w2hiLp8", "202109", "76.3"));
    validateRow(response, List.of("sB79w2hiLp8", "202110", "57.1"));
    validateRow(response, List.of("sB79w2hiLp8", "202111", "74.2"));
    validateRow(response, List.of("sB79w2hiLp8", "202112", "51.3"));
  }

  @Test
  public void query14VisitsByDistrictsThisYearstacked() throws JSONException {
// Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=pe:THIS_YEAR")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:cYeuwXTCPkU;Jtf34kNZhzP;hfdmMSPBgLG;fbfJHSPpUQD,ou:USER_ORGUNIT_CHILDREN")
        .add("relativePeriodDate=2022-01-01");

// When 
    ApiResponse response = actions.get(params);

// Then 
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(52)))
        .body("height", equalTo(52))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData. 
    String expectedMetaData = "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"hfdmMSPBgLG\":{\"name\":\"ANC 4th or more visits\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"Jtf34kNZhzP\":{\"name\":\"ANC 3rd visit\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"2022\":{\"name\":\"2022\"},\"fbfJHSPpUQD\":{\"name\":\"ANC 1st visit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"pe\":{\"name\":\"Period\"},\"cYeuwXTCPkU\":{\"name\":\"ANC 2nd visit\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"cYeuwXTCPkU\",\"Jtf34kNZhzP\",\"hfdmMSPBgLG\",\"fbfJHSPpUQD\"],\"pe\":[\"2022\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("hfdmMSPBgLG", "jmIPBj66vD6", "10309"));
    validateRow(response, List.of("hfdmMSPBgLG", "at6UHUQatSo", "21709"));
    validateRow(response, List.of("fbfJHSPpUQD", "jmIPBj66vD6", "16169"));
    validateRow(response, List.of("cYeuwXTCPkU", "at6UHUQatSo", "44948"));
    validateRow(response, List.of("cYeuwXTCPkU", "jmIPBj66vD6", "14519"));
    validateRow(response, List.of("fbfJHSPpUQD", "PMa2VCrupOd", "14320"));
    validateRow(response, List.of("Jtf34kNZhzP", "jUb8gELQApl", "13175"));
    validateRow(response, List.of("Jtf34kNZhzP", "eIQbndfxQMb", "10589"));
    validateRow(response, List.of("hfdmMSPBgLG", "Vth0fbpFcsO", "2846"));
    validateRow(response, List.of("fbfJHSPpUQD", "at6UHUQatSo", "50119"));
    validateRow(response, List.of("Jtf34kNZhzP", "PMa2VCrupOd", "9074"));
    validateRow(response, List.of("cYeuwXTCPkU", "Vth0fbpFcsO", "9148"));
    validateRow(response, List.of("hfdmMSPBgLG", "lc3eMKXaEfw", "2690"));
    validateRow(response, List.of("cYeuwXTCPkU", "kJq2mPyFEHo", "29478"));
    validateRow(response, List.of("cYeuwXTCPkU", "lc3eMKXaEfw", "6427"));
    validateRow(response, List.of("Jtf34kNZhzP", "at6UHUQatSo", "29281"));
    validateRow(response, List.of("hfdmMSPBgLG", "fdc6uOvgoji", "6108"));
    validateRow(response, List.of("hfdmMSPBgLG", "kJq2mPyFEHo", "14094"));
    validateRow(response, List.of("cYeuwXTCPkU", "jUb8gELQApl", "15730"));
    validateRow(response, List.of("cYeuwXTCPkU", "eIQbndfxQMb", "18047"));
    validateRow(response, List.of("Jtf34kNZhzP", "fdc6uOvgoji", "10558"));
    validateRow(response, List.of("hfdmMSPBgLG", "PMa2VCrupOd", "4506"));
    validateRow(response, List.of("hfdmMSPBgLG", "bL4ooGhyHRQ", "3898"));
    validateRow(response, List.of("cYeuwXTCPkU", "PMa2VCrupOd", "13084"));
    validateRow(response, List.of("cYeuwXTCPkU", "bL4ooGhyHRQ", "8999"));
    validateRow(response, List.of("cYeuwXTCPkU", "TEQlaapDQoK", "18628"));
    validateRow(response, List.of("Jtf34kNZhzP", "O6uvpzGd5pu", "21679"));
    validateRow(response, List.of("Jtf34kNZhzP", "kJq2mPyFEHo", "22378"));
    validateRow(response, List.of("Jtf34kNZhzP", "lc3eMKXaEfw", "4449"));
    validateRow(response, List.of("hfdmMSPBgLG", "jUb8gELQApl", "8235"));
    validateRow(response, List.of("hfdmMSPBgLG", "TEQlaapDQoK", "6199"));
    validateRow(response, List.of("hfdmMSPBgLG", "eIQbndfxQMb", "4220"));
    validateRow(response, List.of("fbfJHSPpUQD", "eIQbndfxQMb", "22505"));
    validateRow(response, List.of("cYeuwXTCPkU", "fdc6uOvgoji", "14646"));
    validateRow(response, List.of("cYeuwXTCPkU", "qhqAxPSTUXp", "7294"));
    validateRow(response, List.of("hfdmMSPBgLG", "qhqAxPSTUXp", "3810"));
    validateRow(response, List.of("fbfJHSPpUQD", "fdc6uOvgoji", "17031"));
    validateRow(response, List.of("Jtf34kNZhzP", "bL4ooGhyHRQ", "6856"));
    validateRow(response, List.of("fbfJHSPpUQD", "lc3eMKXaEfw", "6703"));
    validateRow(response, List.of("fbfJHSPpUQD", "bL4ooGhyHRQ", "10662"));
    validateRow(response, List.of("cYeuwXTCPkU", "O6uvpzGd5pu", "32412"));
    validateRow(response, List.of("Jtf34kNZhzP", "TEQlaapDQoK", "11422"));
    validateRow(response, List.of("hfdmMSPBgLG", "O6uvpzGd5pu", "13134"));
    validateRow(response, List.of("fbfJHSPpUQD", "O6uvpzGd5pu", "33398"));
    validateRow(response, List.of("fbfJHSPpUQD", "Vth0fbpFcsO", "9254"));
    validateRow(response, List.of("Jtf34kNZhzP", "jmIPBj66vD6", "12620"));
    validateRow(response, List.of("fbfJHSPpUQD", "jUb8gELQApl", "15145"));
    validateRow(response, List.of("fbfJHSPpUQD", "qhqAxPSTUXp", "9084"));
    validateRow(response, List.of("Jtf34kNZhzP", "qhqAxPSTUXp", "5259"));
    validateRow(response, List.of("fbfJHSPpUQD", "TEQlaapDQoK", "23773"));
    validateRow(response, List.of("fbfJHSPpUQD", "kJq2mPyFEHo", "24347"));
    validateRow(response, List.of("Jtf34kNZhzP", "Vth0fbpFcsO", "6478"));
  }

  @Test
  public void query14VisitsLast12Months() throws JSONException {
// Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=ou:ImspTQPwCqd")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add("dimension=dx:fbfJHSPpUQD;cYeuwXTCPkU;Jtf34kNZhzP;hfdmMSPBgLG,pe:LAST_12_MONTHS")
        .add("relativePeriodDate=2022-01-01");

// When 
    ApiResponse response = actions.get(params);

// Then 
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(48)))
        .body("height", equalTo(48))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData. 
    String expectedMetaData = "{\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"202109\":{\"name\":\"September 2021\"},\"202107\":{\"name\":\"July 2021\"},\"fbfJHSPpUQD\":{\"name\":\"ANC 1st visit\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202101\":{\"name\":\"January 2021\"},\"202112\":{\"name\":\"December 2021\"},\"202102\":{\"name\":\"February 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"hfdmMSPBgLG\":{\"name\":\"ANC 4th or more visits\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"pq2XI5kz2BY\":{\"name\":\"Fixed\"},\"pe\":{\"name\":\"Period\"},\"cYeuwXTCPkU\":{\"name\":\"ANC 2nd visit\"},\"Jtf34kNZhzP\":{\"name\":\"ANC 3rd visit\"},\"PT59n8BQbqM\":{\"name\":\"Outreach\"}},\"dimensions\":{\"dx\":[\"fbfJHSPpUQD\",\"cYeuwXTCPkU\",\"Jtf34kNZhzP\",\"hfdmMSPBgLG\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"pq2XI5kz2BY\",\"PT59n8BQbqM\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("Jtf34kNZhzP", "202101", "11803"));
    validateRow(response, List.of("Jtf34kNZhzP", "202109", "15306"));
    validateRow(response, List.of("cYeuwXTCPkU", "202110", "16113"));
    validateRow(response, List.of("Jtf34kNZhzP", "202108", "14662"));
    validateRow(response, List.of("cYeuwXTCPkU", "202111", "19453"));
    validateRow(response, List.of("Jtf34kNZhzP", "202107", "14852"));
    validateRow(response, List.of("cYeuwXTCPkU", "202112", "15183"));
    validateRow(response, List.of("Jtf34kNZhzP", "202106", "15688"));
    validateRow(response, List.of("Jtf34kNZhzP", "202105", "15699"));
    validateRow(response, List.of("Jtf34kNZhzP", "202104", "12467"));
    validateRow(response, List.of("fbfJHSPpUQD", "202101", "20026"));
    validateRow(response, List.of("Jtf34kNZhzP", "202103", "13934"));
    validateRow(response, List.of("fbfJHSPpUQD", "202102", "18786"));
    validateRow(response, List.of("Jtf34kNZhzP", "202102", "12079"));
    validateRow(response, List.of("fbfJHSPpUQD", "202103", "21877"));
    validateRow(response, List.of("fbfJHSPpUQD", "202104", "18576"));
    validateRow(response, List.of("fbfJHSPpUQD", "202105", "29461"));
    validateRow(response, List.of("fbfJHSPpUQD", "202106", "23813"));
    validateRow(response, List.of("hfdmMSPBgLG", "202112", "7260"));
    validateRow(response, List.of("fbfJHSPpUQD", "202107", "22356"));
    validateRow(response, List.of("fbfJHSPpUQD", "202108", "22004"));
    validateRow(response, List.of("fbfJHSPpUQD", "202109", "22308"));
    validateRow(response, List.of("hfdmMSPBgLG", "202111", "9163"));
    validateRow(response, List.of("hfdmMSPBgLG", "202110", "8167"));
    validateRow(response, List.of("cYeuwXTCPkU", "202106", "23904"));
    validateRow(response, List.of("Jtf34kNZhzP", "202112", "10635"));
    validateRow(response, List.of("cYeuwXTCPkU", "202107", "21130"));
    validateRow(response, List.of("Jtf34kNZhzP", "202111", "14876"));
    validateRow(response, List.of("cYeuwXTCPkU", "202108", "20413"));
    validateRow(response, List.of("Jtf34kNZhzP", "202110", "11825"));
    validateRow(response, List.of("cYeuwXTCPkU", "202109", "20433"));
    validateRow(response, List.of("cYeuwXTCPkU", "202102", "18488"));
    validateRow(response, List.of("cYeuwXTCPkU", "202103", "19574"));
    validateRow(response, List.of("cYeuwXTCPkU", "202104", "18403"));
    validateRow(response, List.of("cYeuwXTCPkU", "202105", "23726"));
    validateRow(response, List.of("cYeuwXTCPkU", "202101", "17269"));
    validateRow(response, List.of("hfdmMSPBgLG", "202109", "10456"));
    validateRow(response, List.of("fbfJHSPpUQD", "202110", "17926"));
    validateRow(response, List.of("fbfJHSPpUQD", "202111", "19691"));
    validateRow(response, List.of("fbfJHSPpUQD", "202112", "16445"));
    validateRow(response, List.of("hfdmMSPBgLG", "202104", "7669"));
    validateRow(response, List.of("hfdmMSPBgLG", "202103", "9448"));
    validateRow(response, List.of("hfdmMSPBgLG", "202102", "6912"));
    validateRow(response, List.of("hfdmMSPBgLG", "202101", "5639"));
    validateRow(response, List.of("hfdmMSPBgLG", "202108", "9039"));
    validateRow(response, List.of("hfdmMSPBgLG", "202107", "8875"));
    validateRow(response, List.of("hfdmMSPBgLG", "202106", "9359"));
    validateRow(response, List.of("hfdmMSPBgLG", "202105", "9590"));
  }

  @Test
  public void query1stAnd3rdTrendsMonthly() throws JSONException {
// Given 
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=ou:USER_ORGUNIT")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add("dimension=dx:Uvn6LCg7dVU;sB79w2hiLp8,pe:LAST_12_MONTHS")
        .add("relativePeriodDate=2022-01-01");

// When 
    ApiResponse response = actions.get(params);

// Then 
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(24)))
        .body("height", equalTo(24))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData. 
    String expectedMetaData = "{\"items\":{\"sB79w2hiLp8\":{\"name\":\"ANC 3 Coverage\"},\"ou\":{\"name\":\"Organisation unit\"},\"202109\":{\"name\":\"September 2021\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202101\":{\"name\":\"January 2021\"},\"202112\":{\"name\":\"December 2021\"},\"202102\":{\"name\":\"February 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"Uvn6LCg7dVU\":{\"name\":\"ANC 1 Coverage\"}},\"dimensions\":{\"dx\":[\"Uvn6LCg7dVU\",\"sB79w2hiLp8\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("Uvn6LCg7dVU", "202101", "96.7"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202102", "100.4"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202103", "105.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202104", "92.6"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202105", "142.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202106", "118.8"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202107", "107.9"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202108", "106.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202109", "111.3"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202110", "86.5"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202111", "98.2"));
    validateRow(response, List.of("Uvn6LCg7dVU", "202112", "79.4"));
    validateRow(response, List.of("sB79w2hiLp8", "202101", "57.0"));
    validateRow(response, List.of("sB79w2hiLp8", "202102", "64.5"));
    validateRow(response, List.of("sB79w2hiLp8", "202103", "67.3"));
    validateRow(response, List.of("sB79w2hiLp8", "202104", "62.2"));
    validateRow(response, List.of("sB79w2hiLp8", "202105", "75.8"));
    validateRow(response, List.of("sB79w2hiLp8", "202106", "78.2"));
    validateRow(response, List.of("sB79w2hiLp8", "202107", "71.7"));
    validateRow(response, List.of("sB79w2hiLp8", "202108", "70.8"));
    validateRow(response, List.of("sB79w2hiLp8", "202109", "76.3"));
    validateRow(response, List.of("sB79w2hiLp8", "202110", "57.1"));
    validateRow(response, List.of("sB79w2hiLp8", "202111", "74.2"));
    validateRow(response, List.of("sB79w2hiLp8", "202112", "51.3"));
  }
}