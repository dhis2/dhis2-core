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
public class AnalyticsQueryDv9AutoTest extends AnalyticsApiTest {

  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void queryPenta1CoverageByDistrictsLast10Years() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=dx:i7WSgSJpnfu")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=pe:LAST_10_YEARS,ou:jUb8gELQApl;TEQlaapDQoK;Vth0fbpFcsO;PMa2VCrupOd;O6uvpzGd5pu;kJq2mPyFEHo;fdc6uOvgoji;lc3eMKXaEfw;qhqAxPSTUXp;jmIPBj66vD6")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(100)))
        .body("height", equalTo(100))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"2012\":{\"name\":\"2012\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"LAST_10_YEARS\":{\"name\":\"Last 10 years\"},\"dx\":{\"name\":\"Data\"},\"i7WSgSJpnfu\":{\"name\":\"Penta 1 Coverage <1y\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"2021\":{\"name\":\"2021\"},\"2020\":{\"name\":\"2020\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"2019\":{\"name\":\"2019\"},\"2018\":{\"name\":\"2018\"},\"2017\":{\"name\":\"2017\"},\"2016\":{\"name\":\"2016\"},\"2015\":{\"name\":\"2015\"},\"pe\":{\"name\":\"Period\"},\"2014\":{\"name\":\"2014\"},\"2013\":{\"name\":\"2013\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"i7WSgSJpnfu\"],\"pe\":[\"2012\",\"2013\",\"2014\",\"2015\",\"2016\",\"2017\",\"2018\",\"2019\",\"2020\",\"2021\"],\"ou\":[\"jUb8gELQApl\",\"TEQlaapDQoK\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"O6uvpzGd5pu\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"jmIPBj66vD6\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("2012", "jUb8gELQApl", "35.4"));
    validateRow(response, List.of("2012", "TEQlaapDQoK", "24.6"));
    validateRow(response, List.of("2012", "Vth0fbpFcsO", "17.3"));
    validateRow(response, List.of("2012", "PMa2VCrupOd", "22.7"));
    validateRow(response, List.of("2012", "O6uvpzGd5pu", "47.7"));
    validateRow(response, List.of("2012", "kJq2mPyFEHo", "35.1"));
    validateRow(response, List.of("2012", "fdc6uOvgoji", "35.2"));
    validateRow(response, List.of("2012", "lc3eMKXaEfw", "30.3"));
    validateRow(response, List.of("2012", "qhqAxPSTUXp", "20.1"));
    validateRow(response, List.of("2012", "jmIPBj66vD6", "48.5"));
    validateRow(response, List.of("2013", "jUb8gELQApl", "36.3"));
    validateRow(response, List.of("2013", "TEQlaapDQoK", "25.3"));
    validateRow(response, List.of("2013", "Vth0fbpFcsO", "17.8"));
    validateRow(response, List.of("2013", "PMa2VCrupOd", "23.2"));
    validateRow(response, List.of("2013", "O6uvpzGd5pu", "45.7"));
    validateRow(response, List.of("2013", "kJq2mPyFEHo", "36.1"));
    validateRow(response, List.of("2013", "fdc6uOvgoji", "35.9"));
    validateRow(response, List.of("2013", "lc3eMKXaEfw", "30.7"));
    validateRow(response, List.of("2013", "qhqAxPSTUXp", "21.0"));
    validateRow(response, List.of("2013", "jmIPBj66vD6", "49.3"));
    validateRow(response, List.of("2014", "jUb8gELQApl", "36.1"));
    validateRow(response, List.of("2014", "TEQlaapDQoK", "25.0"));
    validateRow(response, List.of("2014", "Vth0fbpFcsO", "17.2"));
    validateRow(response, List.of("2014", "PMa2VCrupOd", "22.9"));
    validateRow(response, List.of("2014", "O6uvpzGd5pu", "45.2"));
    validateRow(response, List.of("2014", "kJq2mPyFEHo", "35.2"));
    validateRow(response, List.of("2014", "fdc6uOvgoji", "35.4"));
    validateRow(response, List.of("2014", "lc3eMKXaEfw", "30.5"));
    validateRow(response, List.of("2014", "qhqAxPSTUXp", "20.4"));
    validateRow(response, List.of("2014", "jmIPBj66vD6", "48.9"));
    validateRow(response, List.of("2015", "jUb8gELQApl", "35.7"));
    validateRow(response, List.of("2015", "TEQlaapDQoK", "24.6"));
    validateRow(response, List.of("2015", "Vth0fbpFcsO", "17.0"));
    validateRow(response, List.of("2015", "PMa2VCrupOd", "22.9"));
    validateRow(response, List.of("2015", "O6uvpzGd5pu", "43.9"));
    validateRow(response, List.of("2015", "kJq2mPyFEHo", "35.6"));
    validateRow(response, List.of("2015", "fdc6uOvgoji", "35.6"));
    validateRow(response, List.of("2015", "lc3eMKXaEfw", "30.4"));
    validateRow(response, List.of("2015", "qhqAxPSTUXp", "20.5"));
    validateRow(response, List.of("2015", "jmIPBj66vD6", "49.3"));
    validateRow(response, List.of("2016", "jUb8gELQApl", "35.2"));
    validateRow(response, List.of("2016", "TEQlaapDQoK", "24.2"));
    validateRow(response, List.of("2016", "Vth0fbpFcsO", "17.2"));
    validateRow(response, List.of("2016", "PMa2VCrupOd", "22.3"));
    validateRow(response, List.of("2016", "O6uvpzGd5pu", "42.7"));
    validateRow(response, List.of("2016", "kJq2mPyFEHo", "34.6"));
    validateRow(response, List.of("2016", "fdc6uOvgoji", "35.0"));
    validateRow(response, List.of("2016", "lc3eMKXaEfw", "29.7"));
    validateRow(response, List.of("2016", "qhqAxPSTUXp", "20.0"));
    validateRow(response, List.of("2016", "jmIPBj66vD6", "47.6"));
    validateRow(response, List.of("2017", "jUb8gELQApl", "35.3"));
    validateRow(response, List.of("2017", "TEQlaapDQoK", "24.0"));
    validateRow(response, List.of("2017", "Vth0fbpFcsO", "16.6"));
    validateRow(response, List.of("2017", "PMa2VCrupOd", "22.2"));
    validateRow(response, List.of("2017", "O6uvpzGd5pu", "41.3"));
    validateRow(response, List.of("2017", "kJq2mPyFEHo", "34.5"));
    validateRow(response, List.of("2017", "fdc6uOvgoji", "34.3"));
    validateRow(response, List.of("2017", "lc3eMKXaEfw", "29.4"));
    validateRow(response, List.of("2017", "qhqAxPSTUXp", "20.0"));
    validateRow(response, List.of("2017", "jmIPBj66vD6", "47.5"));
    validateRow(response, List.of("2018", "jUb8gELQApl", "34.1"));
    validateRow(response, List.of("2018", "TEQlaapDQoK", "23.9"));
    validateRow(response, List.of("2018", "Vth0fbpFcsO", "16.6"));
    validateRow(response, List.of("2018", "PMa2VCrupOd", "21.8"));
    validateRow(response, List.of("2018", "O6uvpzGd5pu", "40.9"));
    validateRow(response, List.of("2018", "kJq2mPyFEHo", "33.9"));
    validateRow(response, List.of("2018", "fdc6uOvgoji", "33.8"));
    validateRow(response, List.of("2018", "lc3eMKXaEfw", "29.0"));
    validateRow(response, List.of("2018", "qhqAxPSTUXp", "19.5"));
    validateRow(response, List.of("2018", "jmIPBj66vD6", "46.5"));
    validateRow(response, List.of("2019", "jUb8gELQApl", "30.2"));
    validateRow(response, List.of("2019", "TEQlaapDQoK", "20.9"));
    validateRow(response, List.of("2019", "Vth0fbpFcsO", "14.5"));
    validateRow(response, List.of("2019", "PMa2VCrupOd", "19.3"));
    validateRow(response, List.of("2019", "O6uvpzGd5pu", "39.5"));
    validateRow(response, List.of("2019", "kJq2mPyFEHo", "29.7"));
    validateRow(response, List.of("2019", "fdc6uOvgoji", "29.9"));
    validateRow(response, List.of("2019", "lc3eMKXaEfw", "25.4"));
    validateRow(response, List.of("2019", "qhqAxPSTUXp", "17.2"));
    validateRow(response, List.of("2019", "jmIPBj66vD6", "41.1"));
    validateRow(response, List.of("2020", "jUb8gELQApl", "29.5"));
    validateRow(response, List.of("2020", "TEQlaapDQoK", "20.4"));
    validateRow(response, List.of("2020", "Vth0fbpFcsO", "14.2"));
    validateRow(response, List.of("2020", "PMa2VCrupOd", "18.8"));
    validateRow(response, List.of("2020", "O6uvpzGd5pu", "38.9"));
    validateRow(response, List.of("2020", "kJq2mPyFEHo", "29.0"));
    validateRow(response, List.of("2020", "fdc6uOvgoji", "29.2"));
    validateRow(response, List.of("2020", "lc3eMKXaEfw", "24.8"));
    validateRow(response, List.of("2020", "qhqAxPSTUXp", "16.8"));
    validateRow(response, List.of("2020", "jmIPBj66vD6", "40.2"));
    validateRow(response, List.of("2021", "jUb8gELQApl", "29.0"));
    validateRow(response, List.of("2021", "TEQlaapDQoK", "20.1"));
    validateRow(response, List.of("2021", "Vth0fbpFcsO", "13.9"));
    validateRow(response, List.of("2021", "PMa2VCrupOd", "18.5"));
    validateRow(response, List.of("2021", "O6uvpzGd5pu", "37.1"));
    validateRow(response, List.of("2021", "kJq2mPyFEHo", "28.5"));
    validateRow(response, List.of("2021", "fdc6uOvgoji", "28.7"));
    validateRow(response, List.of("2021", "lc3eMKXaEfw", "24.4"));
    validateRow(response, List.of("2021", "qhqAxPSTUXp", "16.5"));
    validateRow(response, List.of("2021", "jmIPBj66vD6", "39.5"));
  }

  @Test
  public void queryTtDosesByLocationAndPregnantStatus() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=pe:LAST_4_QUARTERS")
        .add("skipData=false")
        .add("includeNumDen=true")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:zzHwXqxKYy1;DUTSJE8MGCw;jWaNoH0X0Am;tbudfYMNFS5;B2ocYtYkPBD,qzsxBXFf5yb:ZggZw2XNDaw;JR4RSJFjNYl,ou:at6UHUQatSo;bL4ooGhyHRQ;TEQlaapDQoK;O6uvpzGd5pu;kJq2mPyFEHo;jUb8gELQApl;eIQbndfxQMb;Vth0fbpFcsO;fdc6uOvgoji;jmIPBj66vD6;lc3eMKXaEfw;qhqAxPSTUXp;PMa2VCrupOd,fMZEcRHuamy:qkPbeWaFsnU;wbrDrL2aYEc")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(10)))
        .body("rows", hasSize(equalTo(260)))
        .body("height", equalTo(260))
        .body("width", equalTo(10))
        .body("headerWidth", equalTo(10));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"cBQmyRrEKo3\":{\"name\":\"Pregnant, Outreach\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"qkPbeWaFsnU\":{\"name\":\"Fixed\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"2021Q4\":{\"name\":\"October - December 2021\"},\"ZggZw2XNDaw\":{\"name\":\"Non-pregnant\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"ONiwc2Eg0R1\":{\"name\":\"Non-pregnant, School\"},\"tbudfYMNFS5\":{\"name\":\"TT4 doses given\"},\"JR4RSJFjNYl\":{\"name\":\"Pregnant\"},\"B2ocYtYkPBD\":{\"name\":\"TT5 doses given\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"dcguXUTwenI\":{\"name\":\"Pregnant, Fixed\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"fMZEcRHuamy\":{\"name\":\"Location Fixed/Outreach\"},\"DUTSJE8MGCw\":{\"name\":\"TT2 doses given\"},\"zzHwXqxKYy1\":{\"name\":\"TT1 doses given\"},\"wbrDrL2aYEc\":{\"name\":\"Outreach\"},\"xFRryVwuFWY\":{\"name\":\"Pregnant, School\"},\"jWaNoH0X0Am\":{\"name\":\"TT3 doses given\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"U1PHVSShuWj\":{\"name\":\"Non-pregnant, Outreach\"},\"2021Q2\":{\"name\":\"April - June 2021\"},\"2021Q3\":{\"name\":\"July - September 2021\"},\"2021Q1\":{\"name\":\"January - March 2021\"},\"pe\":{\"name\":\"Period\"},\"r8xySVHExGT\":{\"name\":\"Non-pregnant, Fixed\"},\"qzsxBXFf5yb\":{\"name\":\"Pregnant/Non-pregnant\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"}},\"dimensions\":{\"dx\":[\"zzHwXqxKYy1\",\"DUTSJE8MGCw\",\"jWaNoH0X0Am\",\"tbudfYMNFS5\",\"B2ocYtYkPBD\"],\"pe\":[\"2021Q1\",\"2021Q2\",\"2021Q3\",\"2021Q4\"],\"ou\":[\"at6UHUQatSo\",\"bL4ooGhyHRQ\",\"TEQlaapDQoK\",\"O6uvpzGd5pu\",\"kJq2mPyFEHo\",\"jUb8gELQApl\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"fdc6uOvgoji\",\"jmIPBj66vD6\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"PMa2VCrupOd\"],\"fMZEcRHuamy\":[\"qkPbeWaFsnU\",\"wbrDrL2aYEc\"],\"qzsxBXFf5yb\":[\"ZggZw2XNDaw\",\"JR4RSJFjNYl\"],\"co\":[\"U1PHVSShuWj\",\"ONiwc2Eg0R1\",\"cBQmyRrEKo3\",\"dcguXUTwenI\",\"r8xySVHExGT\",\"xFRryVwuFWY\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "qzsxBXFf5yb", "Pregnant/Non-pregnant", "TEXT", "java.lang.String",
        false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "fMZEcRHuamy", "Location Fixed/Outreach", "TEXT",
        "java.lang.String", false, true);
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
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "at6UHUQatSo", "wbrDrL2aYEc", "3221", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "jUb8gELQApl", "wbrDrL2aYEc", "2688", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "lc3eMKXaEfw", "wbrDrL2aYEc", "982", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "lc3eMKXaEfw", "wbrDrL2aYEc", "280", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "qhqAxPSTUXp", "qkPbeWaFsnU", "854", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "jUb8gELQApl", "wbrDrL2aYEc", "2803", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "lc3eMKXaEfw", "wbrDrL2aYEc", "107", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "TEQlaapDQoK", "qkPbeWaFsnU", "567", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "at6UHUQatSo", "wbrDrL2aYEc", "4966", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "eIQbndfxQMb", "wbrDrL2aYEc", "1173", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "bL4ooGhyHRQ", "qkPbeWaFsnU", "994", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "Vth0fbpFcsO", "qkPbeWaFsnU", "611", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "fdc6uOvgoji", "wbrDrL2aYEc", "1115", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "fdc6uOvgoji", "qkPbeWaFsnU", "436", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "O6uvpzGd5pu", "wbrDrL2aYEc", "70", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "O6uvpzGd5pu", "qkPbeWaFsnU", "953", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "at6UHUQatSo", "wbrDrL2aYEc", "1317", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "eIQbndfxQMb", "qkPbeWaFsnU", "598", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "PMa2VCrupOd", "qkPbeWaFsnU", "7057", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "bL4ooGhyHRQ", "wbrDrL2aYEc", "895", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "jmIPBj66vD6", "qkPbeWaFsnU", "8012", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "O6uvpzGd5pu", "qkPbeWaFsnU", "11807", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "fdc6uOvgoji", "wbrDrL2aYEc", "6842", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "O6uvpzGd5pu", "qkPbeWaFsnU", "7646", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "jUb8gELQApl", "wbrDrL2aYEc", "2189", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "at6UHUQatSo", "qkPbeWaFsnU", "328", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "at6UHUQatSo", "wbrDrL2aYEc", "1887", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "PMa2VCrupOd", "qkPbeWaFsnU", "2169", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "jUb8gELQApl", "wbrDrL2aYEc", "3790", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "jUb8gELQApl", "qkPbeWaFsnU", "2424", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "jmIPBj66vD6", "wbrDrL2aYEc", "283", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "bL4ooGhyHRQ", "wbrDrL2aYEc", "232", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "jmIPBj66vD6", "qkPbeWaFsnU", "641", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "PMa2VCrupOd", "qkPbeWaFsnU", "5392", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "eIQbndfxQMb", "wbrDrL2aYEc", "71", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "lc3eMKXaEfw", "wbrDrL2aYEc", "8", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "qhqAxPSTUXp", "wbrDrL2aYEc", "924", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "jmIPBj66vD6", "wbrDrL2aYEc", "1416", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "kJq2mPyFEHo", "qkPbeWaFsnU", "1615", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "qhqAxPSTUXp", "qkPbeWaFsnU", "2287", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "fdc6uOvgoji", "qkPbeWaFsnU", "9866", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "kJq2mPyFEHo", "qkPbeWaFsnU", "3860", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "fdc6uOvgoji", "qkPbeWaFsnU", "2943", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "qhqAxPSTUXp", "wbrDrL2aYEc", "1057", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "jmIPBj66vD6", "wbrDrL2aYEc", "718", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "TEQlaapDQoK", "qkPbeWaFsnU", "3072", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "PMa2VCrupOd", "wbrDrL2aYEc", "1378", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "Vth0fbpFcsO", "qkPbeWaFsnU", "4390", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "Vth0fbpFcsO", "wbrDrL2aYEc", "880", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "PMa2VCrupOd", "qkPbeWaFsnU", "499", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "lc3eMKXaEfw", "qkPbeWaFsnU", "4454", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "at6UHUQatSo", "qkPbeWaFsnU", "39708", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "jUb8gELQApl", "qkPbeWaFsnU", "4712", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "PMa2VCrupOd", "wbrDrL2aYEc", "2664", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "O6uvpzGd5pu", "qkPbeWaFsnU", "1578", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "at6UHUQatSo", "qkPbeWaFsnU", "5770", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "O6uvpzGd5pu", "qkPbeWaFsnU", "1935", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "eIQbndfxQMb", "wbrDrL2aYEc", "833", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "TEQlaapDQoK", "wbrDrL2aYEc", "535", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "fdc6uOvgoji", "wbrDrL2aYEc", "4439", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "kJq2mPyFEHo", "wbrDrL2aYEc", "552", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "PMa2VCrupOd", "qkPbeWaFsnU", "417", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "TEQlaapDQoK", "wbrDrL2aYEc", "3484", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "jmIPBj66vD6", "qkPbeWaFsnU", "3324", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "bL4ooGhyHRQ", "wbrDrL2aYEc", "585", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "jUb8gELQApl", "wbrDrL2aYEc", "3326", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "TEQlaapDQoK", "qkPbeWaFsnU", "12800", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "O6uvpzGd5pu", "qkPbeWaFsnU", "4049", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "Vth0fbpFcsO", "wbrDrL2aYEc", "140", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "jUb8gELQApl", "wbrDrL2aYEc", "4036", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "bL4ooGhyHRQ", "qkPbeWaFsnU", "2117", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "PMa2VCrupOd", "wbrDrL2aYEc", "190", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "bL4ooGhyHRQ", "qkPbeWaFsnU", "1780", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "O6uvpzGd5pu", "wbrDrL2aYEc", "270", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "eIQbndfxQMb", "qkPbeWaFsnU", "915", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "jUb8gELQApl", "wbrDrL2aYEc", "2101", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "TEQlaapDQoK", "wbrDrL2aYEc", "44", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "fdc6uOvgoji", "wbrDrL2aYEc", "569", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "fdc6uOvgoji", "qkPbeWaFsnU", "807", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "qhqAxPSTUXp", "wbrDrL2aYEc", "239", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "eIQbndfxQMb", "qkPbeWaFsnU", "2736", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "eIQbndfxQMb", "wbrDrL2aYEc", "220", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "jmIPBj66vD6", "qkPbeWaFsnU", "2178", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "at6UHUQatSo", "wbrDrL2aYEc", "638", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "bL4ooGhyHRQ", "wbrDrL2aYEc", "1215", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "lc3eMKXaEfw", "wbrDrL2aYEc", "104", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "lc3eMKXaEfw", "qkPbeWaFsnU", "51", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "lc3eMKXaEfw", "qkPbeWaFsnU", "3612", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "lc3eMKXaEfw", "qkPbeWaFsnU", "268", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "kJq2mPyFEHo", "wbrDrL2aYEc", "292", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "eIQbndfxQMb", "qkPbeWaFsnU", "1885", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "jmIPBj66vD6", "qkPbeWaFsnU", "1164", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "kJq2mPyFEHo", "wbrDrL2aYEc", "198", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "Vth0fbpFcsO", "wbrDrL2aYEc", "453", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "kJq2mPyFEHo", "wbrDrL2aYEc", "439", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "jmIPBj66vD6", "qkPbeWaFsnU", "1689", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "eIQbndfxQMb", "wbrDrL2aYEc", "1128", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "TEQlaapDQoK", "wbrDrL2aYEc", "356", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "Vth0fbpFcsO", "wbrDrL2aYEc", "89", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "TEQlaapDQoK", "qkPbeWaFsnU", "343", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "fdc6uOvgoji", "wbrDrL2aYEc", "154", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "bL4ooGhyHRQ", "wbrDrL2aYEc", "657", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "Vth0fbpFcsO", "wbrDrL2aYEc", "42", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "qhqAxPSTUXp", "wbrDrL2aYEc", "640", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "bL4ooGhyHRQ", "wbrDrL2aYEc", "874", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "qhqAxPSTUXp", "wbrDrL2aYEc", "632", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "jmIPBj66vD6", "wbrDrL2aYEc", "963", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "O6uvpzGd5pu", "qkPbeWaFsnU", "10084", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "O6uvpzGd5pu", "wbrDrL2aYEc", "42", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "at6UHUQatSo", "wbrDrL2aYEc", "122", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "TEQlaapDQoK", "wbrDrL2aYEc", "1820", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "qhqAxPSTUXp", "qkPbeWaFsnU", "2396", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "TEQlaapDQoK", "qkPbeWaFsnU", "9537", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "jmIPBj66vD6", "wbrDrL2aYEc", "613", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "TEQlaapDQoK", "wbrDrL2aYEc", "4921", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "PMa2VCrupOd", "wbrDrL2aYEc", "485", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "jUb8gELQApl", "qkPbeWaFsnU", "2747", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "Vth0fbpFcsO", "qkPbeWaFsnU", "2407", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "kJq2mPyFEHo", "qkPbeWaFsnU", "8477", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "Vth0fbpFcsO", "qkPbeWaFsnU", "3847", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "qhqAxPSTUXp", "qkPbeWaFsnU", "2108", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "fdc6uOvgoji", "wbrDrL2aYEc", "1476", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "fdc6uOvgoji", "qkPbeWaFsnU", "7098", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "kJq2mPyFEHo", "qkPbeWaFsnU", "11106", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "eIQbndfxQMb", "wbrDrL2aYEc", "570", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "bL4ooGhyHRQ", "qkPbeWaFsnU", "336", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "eIQbndfxQMb", "qkPbeWaFsnU", "4156", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "lc3eMKXaEfw", "wbrDrL2aYEc", "1364", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "at6UHUQatSo", "qkPbeWaFsnU", "28577", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "bL4ooGhyHRQ", "qkPbeWaFsnU", "5787", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "PMa2VCrupOd", "wbrDrL2aYEc", "351", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "bL4ooGhyHRQ", "qkPbeWaFsnU", "2167", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "eIQbndfxQMb", "qkPbeWaFsnU", "10657", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "kJq2mPyFEHo", "wbrDrL2aYEc", "258", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "kJq2mPyFEHo", "qkPbeWaFsnU", "1916", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "jmIPBj66vD6", "wbrDrL2aYEc", "215", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "O6uvpzGd5pu", "wbrDrL2aYEc", "324", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "O6uvpzGd5pu", "wbrDrL2aYEc", "382", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "bL4ooGhyHRQ", "qkPbeWaFsnU", "4925", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "eIQbndfxQMb", "qkPbeWaFsnU", "5228", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "at6UHUQatSo", "qkPbeWaFsnU", "2990", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "eIQbndfxQMb", "qkPbeWaFsnU", "15297", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "jUb8gELQApl", "qkPbeWaFsnU", "3327", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "PMa2VCrupOd", "wbrDrL2aYEc", "1918", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "bL4ooGhyHRQ", "qkPbeWaFsnU", "448", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "O6uvpzGd5pu", "qkPbeWaFsnU", "1383", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "at6UHUQatSo", "qkPbeWaFsnU", "4068", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "fdc6uOvgoji", "qkPbeWaFsnU", "1234", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "at6UHUQatSo", "qkPbeWaFsnU", "1325", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "PMa2VCrupOd", "qkPbeWaFsnU", "794", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "O6uvpzGd5pu", "wbrDrL2aYEc", "293", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "jUb8gELQApl", "qkPbeWaFsnU", "4994", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "jUb8gELQApl", "qkPbeWaFsnU", "4007", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "Vth0fbpFcsO", "wbrDrL2aYEc", "357", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "PMa2VCrupOd", "wbrDrL2aYEc", "1553", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "lc3eMKXaEfw", "wbrDrL2aYEc", "12", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "bL4ooGhyHRQ", "wbrDrL2aYEc", "248", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "jUb8gELQApl", "wbrDrL2aYEc", "3163", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "fdc6uOvgoji", "wbrDrL2aYEc", "567", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "lc3eMKXaEfw", "qkPbeWaFsnU", "997", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "Vth0fbpFcsO", "qkPbeWaFsnU", "1205", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "jmIPBj66vD6", "qkPbeWaFsnU", "1217", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "fdc6uOvgoji", "qkPbeWaFsnU", "1722", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "kJq2mPyFEHo", "wbrDrL2aYEc", "1411", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "Vth0fbpFcsO", "qkPbeWaFsnU", "1449", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "kJq2mPyFEHo", "qkPbeWaFsnU", "10893", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "at6UHUQatSo", "wbrDrL2aYEc", "1822", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "lc3eMKXaEfw", "qkPbeWaFsnU", "1124", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "TEQlaapDQoK", "qkPbeWaFsnU", "978", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "eIQbndfxQMb", "qkPbeWaFsnU", "181", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "at6UHUQatSo", "qkPbeWaFsnU", "554", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "qhqAxPSTUXp", "wbrDrL2aYEc", "588", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "fdc6uOvgoji", "qkPbeWaFsnU", "4213", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "qhqAxPSTUXp", "qkPbeWaFsnU", "1896", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "O6uvpzGd5pu", "wbrDrL2aYEc", "601", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "jUb8gELQApl", "qkPbeWaFsnU", "4421", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "kJq2mPyFEHo", "wbrDrL2aYEc", "575", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "jUb8gELQApl", "qkPbeWaFsnU", "3715", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "PMa2VCrupOd", "qkPbeWaFsnU", "866", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "at6UHUQatSo", "qkPbeWaFsnU", "2133", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "PMa2VCrupOd", "qkPbeWaFsnU", "2543", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "jmIPBj66vD6", "wbrDrL2aYEc", "860", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "TEQlaapDQoK", "qkPbeWaFsnU", "2672", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "jmIPBj66vD6", "qkPbeWaFsnU", "576", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "eIQbndfxQMb", "qkPbeWaFsnU", "212", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "qhqAxPSTUXp", "qkPbeWaFsnU", "1070", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "bL4ooGhyHRQ", "wbrDrL2aYEc", "164", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "TEQlaapDQoK", "wbrDrL2aYEc", "296", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "PMa2VCrupOd", "qkPbeWaFsnU", "3117", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "lc3eMKXaEfw", "wbrDrL2aYEc", "338", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "O6uvpzGd5pu", "wbrDrL2aYEc", "161", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "jUb8gELQApl", "wbrDrL2aYEc", "1938", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "O6uvpzGd5pu", "wbrDrL2aYEc", "365", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "kJq2mPyFEHo", "qkPbeWaFsnU", "588", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "bL4ooGhyHRQ", "wbrDrL2aYEc", "29", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "PMa2VCrupOd", "qkPbeWaFsnU", "1790", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "jmIPBj66vD6", "qkPbeWaFsnU", "387", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "bL4ooGhyHRQ", "qkPbeWaFsnU", "1735", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "fdc6uOvgoji", "wbrDrL2aYEc", "2204", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "jmIPBj66vD6", "wbrDrL2aYEc", "53", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "bL4ooGhyHRQ", "qkPbeWaFsnU", "976", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "at6UHUQatSo", "wbrDrL2aYEc", "5916", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "eIQbndfxQMb", "wbrDrL2aYEc", "2191", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "at6UHUQatSo", "wbrDrL2aYEc", "6332", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "fdc6uOvgoji", "wbrDrL2aYEc", "2181", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "lc3eMKXaEfw", "wbrDrL2aYEc", "268", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "qhqAxPSTUXp", "qkPbeWaFsnU", "908", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "lc3eMKXaEfw", "qkPbeWaFsnU", "699", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "kJq2mPyFEHo", "wbrDrL2aYEc", "378", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "TEQlaapDQoK", "qkPbeWaFsnU", "3733", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "jUb8gELQApl", "qkPbeWaFsnU", "4227", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "kJq2mPyFEHo", "qkPbeWaFsnU", "2387", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "Vth0fbpFcsO", "qkPbeWaFsnU", "239", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "Vth0fbpFcsO", "wbrDrL2aYEc", "310", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "qhqAxPSTUXp", "wbrDrL2aYEc", "1008", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "fdc6uOvgoji", "qkPbeWaFsnU", "4052", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "TEQlaapDQoK", "wbrDrL2aYEc", "1755", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "lc3eMKXaEfw", "wbrDrL2aYEc", "532", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "eIQbndfxQMb", "wbrDrL2aYEc", "628", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "Vth0fbpFcsO", "qkPbeWaFsnU", "1315", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "qhqAxPSTUXp", "qkPbeWaFsnU", "746", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "TEQlaapDQoK", "wbrDrL2aYEc", "1303", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "kJq2mPyFEHo", "qkPbeWaFsnU", "1403", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "O6uvpzGd5pu", "qkPbeWaFsnU", "1873", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "O6uvpzGd5pu", "wbrDrL2aYEc", "58", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "TEQlaapDQoK", "wbrDrL2aYEc", "1864", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "bL4ooGhyHRQ", "wbrDrL2aYEc", "814", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "ZggZw2XNDaw", "at6UHUQatSo", "qkPbeWaFsnU", "5960", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "PMa2VCrupOd", "wbrDrL2aYEc", "519", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "O6uvpzGd5pu", "qkPbeWaFsnU", "1660", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "TEQlaapDQoK", "qkPbeWaFsnU", "1073", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "jmIPBj66vD6", "wbrDrL2aYEc", "87", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "qhqAxPSTUXp", "qkPbeWaFsnU", "1453", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "Vth0fbpFcsO", "qkPbeWaFsnU", "517", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "Vth0fbpFcsO", "wbrDrL2aYEc", "199", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "fdc6uOvgoji", "qkPbeWaFsnU", "4010", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "JR4RSJFjNYl", "PMa2VCrupOd", "wbrDrL2aYEc", "1011", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "PMa2VCrupOd", "wbrDrL2aYEc", "2200", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "jmIPBj66vD6", "wbrDrL2aYEc", "254", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "jUb8gELQApl", "wbrDrL2aYEc", "1388", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "kJq2mPyFEHo", "qkPbeWaFsnU", "835", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "lc3eMKXaEfw", "qkPbeWaFsnU", "93", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "Vth0fbpFcsO", "qkPbeWaFsnU", "1052", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "JR4RSJFjNYl", "qhqAxPSTUXp", "wbrDrL2aYEc", "893", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "qhqAxPSTUXp", "wbrDrL2aYEc", "999", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "qhqAxPSTUXp", "wbrDrL2aYEc", "396", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "lc3eMKXaEfw", "qkPbeWaFsnU", "4", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "lc3eMKXaEfw", "qkPbeWaFsnU", "616", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "jUb8gELQApl", "qkPbeWaFsnU", "5249", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "TEQlaapDQoK", "qkPbeWaFsnU", "3440", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "Vth0fbpFcsO", "wbrDrL2aYEc", "311", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "ZggZw2XNDaw", "eIQbndfxQMb", "wbrDrL2aYEc", "33", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "kJq2mPyFEHo", "wbrDrL2aYEc", "196", "", "", "", "",
            ""));
    validateRow(response,
        List.of("tbudfYMNFS5", "ZggZw2XNDaw", "eIQbndfxQMb", "wbrDrL2aYEc", "115", "", "", "", "",
            ""));
    validateRow(response,
        List.of("jWaNoH0X0Am", "ZggZw2XNDaw", "qhqAxPSTUXp", "qkPbeWaFsnU", "1744", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "at6UHUQatSo", "wbrDrL2aYEc", "4729", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "JR4RSJFjNYl", "jmIPBj66vD6", "qkPbeWaFsnU", "7123", "", "", "", "",
            ""));
    validateRow(response,
        List.of("DUTSJE8MGCw", "ZggZw2XNDaw", "kJq2mPyFEHo", "wbrDrL2aYEc", "822", "", "", "", "",
            ""));
    validateRow(response,
        List.of("B2ocYtYkPBD", "JR4RSJFjNYl", "Vth0fbpFcsO", "wbrDrL2aYEc", "135", "", "", "", "",
            ""));
    validateRow(response,
        List.of("zzHwXqxKYy1", "JR4RSJFjNYl", "fdc6uOvgoji", "wbrDrL2aYEc", "2651", "", "", "", "",
            ""));
  }

  @Test
  public void queryDiseaseWeekwednesday112SierraLeone() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=ou:ImspTQPwCqd")
        .add("skipData=false")
        .add("includeNumDen=true")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:UsSUX0cpKsH;vq2qO3eTrNi;YazgqXbizv1;HS9zqaBdOQ4;noIzB569hTM,pe:2022WedW12;2022WedW11;2022WedW10;2022WedW9;2022WedW8;2022WedW7;2022WedW6;2022WedW5;2022WedW4;2022WedW3;2022WedW2;2022WedW1")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(15)))
        .body("height", equalTo(15))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"2022WedW8\":{\"name\":\"Week 8 2022Wed 2022\"},\"2022WedW7\":{\"name\":\"Week 7 2022Wed 2022\"},\"vq2qO3eTrNi\":{\"name\":\"IDSR Malaria\"},\"2022WedW9\":{\"name\":\"Week 9 2022Wed 2022\"},\"ou\":{\"name\":\"Organisation unit\"},\"noIzB569hTM\":{\"name\":\"IDSR Yellow fever\"},\"2022WedW2\":{\"name\":\"Week 2 2022Wed 2022\"},\"2022WedW1\":{\"name\":\"Week 1 2022Wed 2022\"},\"2022WedW10\":{\"name\":\"Week 10 2022Wed 2022\"},\"2022WedW4\":{\"name\":\"Week 4 2022Wed 2022\"},\"2022WedW11\":{\"name\":\"Week 11 2022Wed 2022\"},\"2022WedW3\":{\"name\":\"Week 3 2022Wed 2022\"},\"UsSUX0cpKsH\":{\"name\":\"IDSR Cholera\"},\"2022WedW6\":{\"name\":\"Week 6 2022Wed 2022\"},\"2022WedW5\":{\"name\":\"Week 5 2022Wed 2022\"},\"HS9zqaBdOQ4\":{\"name\":\"IDSR Plague\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"2022WedW12\":{\"name\":\"Week 12 2022Wed 2022\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"HllvX50cXC0\":{\"name\":\"default\"},\"YazgqXbizv1\":{\"name\":\"IDSR Measles\"}},\"dimensions\":{\"dx\":[\"UsSUX0cpKsH\",\"vq2qO3eTrNi\",\"YazgqXbizv1\",\"HS9zqaBdOQ4\",\"noIzB569hTM\"],\"pe\":[\"2022WedW12\",\"2022WedW11\",\"2022WedW10\",\"2022WedW9\",\"2022WedW8\",\"2022WedW7\",\"2022WedW6\",\"2022WedW5\",\"2022WedW4\",\"2022WedW3\",\"2022WedW2\",\"2022WedW1\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"HllvX50cXC0\"]}}";
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
    validateRow(response, List.of("noIzB569hTM", "2022WedW12", "480", "", "", "", "", ""));
    validateRow(response, List.of("noIzB569hTM", "2022WedW2", "279", "", "", "", "", ""));
    validateRow(response, List.of("YazgqXbizv1", "2022WedW12", "495", "", "", "", "", ""));
    validateRow(response, List.of("YazgqXbizv1", "2022WedW7", "309", "", "", "", "", ""));
    validateRow(response, List.of("UsSUX0cpKsH", "2022WedW7", "294", "", "", "", "", ""));
    validateRow(response, List.of("YazgqXbizv1", "2022WedW2", "258", "", "", "", "", ""));
    validateRow(response, List.of("UsSUX0cpKsH", "2022WedW2", "269", "", "", "", "", ""));
    validateRow(response, List.of("vq2qO3eTrNi", "2022WedW2", "267", "", "", "", "", ""));
    validateRow(response, List.of("UsSUX0cpKsH", "2022WedW12", "496", "", "", "", "", ""));
    validateRow(response, List.of("HS9zqaBdOQ4", "2022WedW2", "237", "", "", "", "", ""));
    validateRow(response, List.of("vq2qO3eTrNi", "2022WedW7", "327", "", "", "", "", ""));
    validateRow(response, List.of("HS9zqaBdOQ4", "2022WedW12", "486", "", "", "", "", ""));
    validateRow(response, List.of("HS9zqaBdOQ4", "2022WedW7", "300", "", "", "", "", ""));
    validateRow(response, List.of("vq2qO3eTrNi", "2022WedW12", "441", "", "", "", "", ""));
    validateRow(response, List.of("noIzB569hTM", "2022WedW7", "277", "", "", "", "", ""));
  }

  @Test
  public void query() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add(
            "filter=ou:O6uvpzGd5pu,pe:LAST_12_MONTHS")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add("dimension=dx:FnYCr2EAzWS")
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
    String expectedMetaData = "{\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"202109\":{\"name\":\"September 2021\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"202107\":{\"name\":\"July 2021\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202101\":{\"name\":\"January 2021\"},\"202112\":{\"name\":\"December 2021\"},\"202102\":{\"name\":\"February 2021\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"FnYCr2EAzWS\":{\"name\":\"BCG Coverage <1y\"}},\"dimensions\":{\"dx\":[\"FnYCr2EAzWS\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"O6uvpzGd5pu\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("FnYCr2EAzWS", "38.0"));
  }

  @Test
  public void queryMalnutritionIndicatorsStackedBar() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=pe:2022")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:X3taFC1HtE5;vhzPbO1eEyr;joIQbN4L1Ok;aGByu8NFs9m,ou:at6UHUQatSo;bL4ooGhyHRQ;TEQlaapDQoK;O6uvpzGd5pu;kJq2mPyFEHo;jUb8gELQApl;eIQbndfxQMb;Vth0fbpFcsO;fdc6uOvgoji;jmIPBj66vD6;ImspTQPwCqd;lc3eMKXaEfw;qhqAxPSTUXp;PMa2VCrupOd")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(56)))
        .body("height", equalTo(56))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"aGByu8NFs9m\":{\"name\":\"Well nourished rate\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"2022\":{\"name\":\"2022\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"joIQbN4L1Ok\":{\"name\":\"Moderate malnutrition rate\"},\"pe\":{\"name\":\"Period\"},\"X3taFC1HtE5\":{\"name\":\"Exclusive breast feeding at Penta 3\"},\"vhzPbO1eEyr\":{\"name\":\"Severe malnutrition rate\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"}},\"dimensions\":{\"dx\":[\"X3taFC1HtE5\",\"vhzPbO1eEyr\",\"joIQbN4L1Ok\",\"aGByu8NFs9m\"],\"pe\":[\"2022\"],\"ou\":[\"at6UHUQatSo\",\"bL4ooGhyHRQ\",\"TEQlaapDQoK\",\"O6uvpzGd5pu\",\"kJq2mPyFEHo\",\"jUb8gELQApl\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"fdc6uOvgoji\",\"jmIPBj66vD6\",\"ImspTQPwCqd\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"PMa2VCrupOd\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("X3taFC1HtE5", "at6UHUQatSo", "63.7"));
    validateRow(response, List.of("X3taFC1HtE5", "bL4ooGhyHRQ", "83.0"));
    validateRow(response, List.of("X3taFC1HtE5", "TEQlaapDQoK", "54.0"));
    validateRow(response, List.of("X3taFC1HtE5", "O6uvpzGd5pu", "61.0"));
    validateRow(response, List.of("X3taFC1HtE5", "kJq2mPyFEHo", "60.8"));
    validateRow(response, List.of("X3taFC1HtE5", "jUb8gELQApl", "68.1"));
    validateRow(response, List.of("X3taFC1HtE5", "eIQbndfxQMb", "101.9"));
    validateRow(response, List.of("X3taFC1HtE5", "Vth0fbpFcsO", "45.6"));
    validateRow(response, List.of("X3taFC1HtE5", "fdc6uOvgoji", "64.1"));
    validateRow(response, List.of("X3taFC1HtE5", "jmIPBj66vD6", "84.9"));
    validateRow(response, List.of("X3taFC1HtE5", "ImspTQPwCqd", "66.0"));
    validateRow(response, List.of("X3taFC1HtE5", "lc3eMKXaEfw", "65.2"));
    validateRow(response, List.of("X3taFC1HtE5", "qhqAxPSTUXp", "67.7"));
    validateRow(response, List.of("X3taFC1HtE5", "PMa2VCrupOd", "59.2"));
    validateRow(response, List.of("vhzPbO1eEyr", "at6UHUQatSo", "7.1"));
    validateRow(response, List.of("vhzPbO1eEyr", "bL4ooGhyHRQ", "2.4"));
    validateRow(response, List.of("vhzPbO1eEyr", "TEQlaapDQoK", "5.1"));
    validateRow(response, List.of("vhzPbO1eEyr", "O6uvpzGd5pu", "5.5"));
    validateRow(response, List.of("vhzPbO1eEyr", "kJq2mPyFEHo", "6.1"));
    validateRow(response, List.of("vhzPbO1eEyr", "jUb8gELQApl", "3.5"));
    validateRow(response, List.of("vhzPbO1eEyr", "eIQbndfxQMb", "8.2"));
    validateRow(response, List.of("vhzPbO1eEyr", "Vth0fbpFcsO", "9.9"));
    validateRow(response, List.of("vhzPbO1eEyr", "fdc6uOvgoji", "2.7"));
    validateRow(response, List.of("vhzPbO1eEyr", "jmIPBj66vD6", "12.9"));
    validateRow(response, List.of("vhzPbO1eEyr", "ImspTQPwCqd", "6.3"));
    validateRow(response, List.of("vhzPbO1eEyr", "lc3eMKXaEfw", "5.8"));
    validateRow(response, List.of("vhzPbO1eEyr", "qhqAxPSTUXp", "8.2"));
    validateRow(response, List.of("vhzPbO1eEyr", "PMa2VCrupOd", "7.2"));
    validateRow(response, List.of("joIQbN4L1Ok", "at6UHUQatSo", "17.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "bL4ooGhyHRQ", "15.4"));
    validateRow(response, List.of("joIQbN4L1Ok", "TEQlaapDQoK", "20.7"));
    validateRow(response, List.of("joIQbN4L1Ok", "O6uvpzGd5pu", "19.6"));
    validateRow(response, List.of("joIQbN4L1Ok", "kJq2mPyFEHo", "22.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "jUb8gELQApl", "17.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "eIQbndfxQMb", "20.3"));
    validateRow(response, List.of("joIQbN4L1Ok", "Vth0fbpFcsO", "21.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "fdc6uOvgoji", "12.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "jmIPBj66vD6", "31.6"));
    validateRow(response, List.of("joIQbN4L1Ok", "ImspTQPwCqd", "19.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "lc3eMKXaEfw", "23.9"));
    validateRow(response, List.of("joIQbN4L1Ok", "qhqAxPSTUXp", "29.0"));
    validateRow(response, List.of("joIQbN4L1Ok", "PMa2VCrupOd", "21.5"));
    validateRow(response, List.of("aGByu8NFs9m", "at6UHUQatSo", "76.0"));
    validateRow(response, List.of("aGByu8NFs9m", "bL4ooGhyHRQ", "82.2"));
    validateRow(response, List.of("aGByu8NFs9m", "TEQlaapDQoK", "74.3"));
    validateRow(response, List.of("aGByu8NFs9m", "O6uvpzGd5pu", "74.9"));
    validateRow(response, List.of("aGByu8NFs9m", "kJq2mPyFEHo", "71.9"));
    validateRow(response, List.of("aGByu8NFs9m", "jUb8gELQApl", "79.5"));
    validateRow(response, List.of("aGByu8NFs9m", "eIQbndfxQMb", "71.5"));
    validateRow(response, List.of("aGByu8NFs9m", "Vth0fbpFcsO", "69.0"));
    validateRow(response, List.of("aGByu8NFs9m", "fdc6uOvgoji", "85.3"));
    validateRow(response, List.of("aGByu8NFs9m", "jmIPBj66vD6", "55.5"));
    validateRow(response, List.of("aGByu8NFs9m", "ImspTQPwCqd", "74.6"));
    validateRow(response, List.of("aGByu8NFs9m", "lc3eMKXaEfw", "70.3"));
    validateRow(response, List.of("aGByu8NFs9m", "qhqAxPSTUXp", "62.8"));
    validateRow(response, List.of("aGByu8NFs9m", "PMa2VCrupOd", "71.3"));
  }

  @Test
  public void queryExpensesByDistrictsArea() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=pe:2021Oct")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:BDuY694ZAFa;ixDKJGrGtFg;M3anTdbJ7iJ;dHrtL2a4EcD;RR538iV9G1X,ou:jUb8gELQApl;TEQlaapDQoK;eIQbndfxQMb;Vth0fbpFcsO;PMa2VCrupOd;O6uvpzGd5pu;bL4ooGhyHRQ;kJq2mPyFEHo;fdc6uOvgoji;at6UHUQatSo;lc3eMKXaEfw;qhqAxPSTUXp;jmIPBj66vD6")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(65)))
        .body("height", equalTo(65))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"BDuY694ZAFa\":{\"name\":\"EXP Cars Expense\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"ixDKJGrGtFg\":{\"name\":\"EXP Drugs Expense\"},\"RR538iV9G1X\":{\"name\":\"EXP Sheets Expense\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"HllvX50cXC0\":{\"name\":\"default\"},\"2021Oct\":{\"name\":\"October 2021 - September 2022\"},\"dHrtL2a4EcD\":{\"name\":\"EXP Security Expense\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"M3anTdbJ7iJ\":{\"name\":\"EXP Salary Expense\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"BDuY694ZAFa\",\"ixDKJGrGtFg\",\"M3anTdbJ7iJ\",\"dHrtL2a4EcD\",\"RR538iV9G1X\"],\"pe\":[\"2021Oct\"],\"ou\":[\"jUb8gELQApl\",\"TEQlaapDQoK\",\"eIQbndfxQMb\",\"Vth0fbpFcsO\",\"PMa2VCrupOd\",\"O6uvpzGd5pu\",\"bL4ooGhyHRQ\",\"kJq2mPyFEHo\",\"fdc6uOvgoji\",\"at6UHUQatSo\",\"lc3eMKXaEfw\",\"qhqAxPSTUXp\",\"jmIPBj66vD6\"],\"co\":[\"HllvX50cXC0\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("ixDKJGrGtFg", "TEQlaapDQoK", "62577"));
    validateRow(response, List.of("dHrtL2a4EcD", "fdc6uOvgoji", "38336"));
    validateRow(response, List.of("dHrtL2a4EcD", "qhqAxPSTUXp", "31054"));
    validateRow(response, List.of("BDuY694ZAFa", "jmIPBj66vD6", "35401"));
    validateRow(response, List.of("BDuY694ZAFa", "PMa2VCrupOd", "38390"));
    validateRow(response, List.of("dHrtL2a4EcD", "lc3eMKXaEfw", "26267"));
    validateRow(response, List.of("M3anTdbJ7iJ", "jUb8gELQApl", "45298"));
    validateRow(response, List.of("M3anTdbJ7iJ", "eIQbndfxQMb", "42078"));
    validateRow(response, List.of("dHrtL2a4EcD", "Vth0fbpFcsO", "61147"));
    validateRow(response, List.of("dHrtL2a4EcD", "O6uvpzGd5pu", "62158"));
    validateRow(response, List.of("ixDKJGrGtFg", "qhqAxPSTUXp", "32323"));
    validateRow(response, List.of("ixDKJGrGtFg", "jUb8gELQApl", "46102"));
    validateRow(response, List.of("BDuY694ZAFa", "kJq2mPyFEHo", "64567"));
    validateRow(response, List.of("RR538iV9G1X", "jUb8gELQApl", "44646"));
    validateRow(response, List.of("BDuY694ZAFa", "lc3eMKXaEfw", "28315"));
    validateRow(response, List.of("M3anTdbJ7iJ", "qhqAxPSTUXp", "31999"));
    validateRow(response, List.of("ixDKJGrGtFg", "kJq2mPyFEHo", "72039"));
    validateRow(response, List.of("BDuY694ZAFa", "O6uvpzGd5pu", "75199"));
    validateRow(response, List.of("RR538iV9G1X", "jmIPBj66vD6", "31417"));
    validateRow(response, List.of("BDuY694ZAFa", "Vth0fbpFcsO", "54983"));
    validateRow(response, List.of("ixDKJGrGtFg", "bL4ooGhyHRQ", "42126"));
    validateRow(response, List.of("M3anTdbJ7iJ", "TEQlaapDQoK", "64540"));
    validateRow(response, List.of("M3anTdbJ7iJ", "O6uvpzGd5pu", "75695"));
    validateRow(response, List.of("M3anTdbJ7iJ", "lc3eMKXaEfw", "29127"));
    validateRow(response, List.of("ixDKJGrGtFg", "fdc6uOvgoji", "44525"));
    validateRow(response, List.of("M3anTdbJ7iJ", "jmIPBj66vD6", "37081"));
    validateRow(response, List.of("ixDKJGrGtFg", "at6UHUQatSo", "122925"));
    validateRow(response, List.of("ixDKJGrGtFg", "eIQbndfxQMb", "41589"));
    validateRow(response, List.of("M3anTdbJ7iJ", "Vth0fbpFcsO", "58827"));
    validateRow(response, List.of("RR538iV9G1X", "lc3eMKXaEfw", "29151"));
    validateRow(response, List.of("ixDKJGrGtFg", "PMa2VCrupOd", "39342"));
    validateRow(response, List.of("RR538iV9G1X", "bL4ooGhyHRQ", "37739"));
    validateRow(response, List.of("RR538iV9G1X", "kJq2mPyFEHo", "62971"));
    validateRow(response, List.of("RR538iV9G1X", "O6uvpzGd5pu", "70704"));
    validateRow(response, List.of("M3anTdbJ7iJ", "at6UHUQatSo", "108621"));
    validateRow(response, List.of("RR538iV9G1X", "Vth0fbpFcsO", "59273"));
    validateRow(response, List.of("BDuY694ZAFa", "qhqAxPSTUXp", "34165"));
    validateRow(response, List.of("M3anTdbJ7iJ", "bL4ooGhyHRQ", "34899"));
    validateRow(response, List.of("RR538iV9G1X", "qhqAxPSTUXp", "28479"));
    validateRow(response, List.of("BDuY694ZAFa", "jUb8gELQApl", "50711"));
    validateRow(response, List.of("M3anTdbJ7iJ", "kJq2mPyFEHo", "64767"));
    validateRow(response, List.of("dHrtL2a4EcD", "at6UHUQatSo", "119706"));
    validateRow(response, List.of("ixDKJGrGtFg", "jmIPBj66vD6", "35815"));
    validateRow(response, List.of("M3anTdbJ7iJ", "PMa2VCrupOd", "40397"));
    validateRow(response, List.of("BDuY694ZAFa", "TEQlaapDQoK", "64696"));
    validateRow(response, List.of("dHrtL2a4EcD", "eIQbndfxQMb", "43414"));
    validateRow(response, List.of("RR538iV9G1X", "TEQlaapDQoK", "69180"));
    validateRow(response, List.of("ixDKJGrGtFg", "Vth0fbpFcsO", "56978"));
    validateRow(response, List.of("BDuY694ZAFa", "bL4ooGhyHRQ", "36435"));
    validateRow(response, List.of("M3anTdbJ7iJ", "fdc6uOvgoji", "40670"));
    validateRow(response, List.of("ixDKJGrGtFg", "lc3eMKXaEfw", "26734"));
    validateRow(response, List.of("dHrtL2a4EcD", "jmIPBj66vD6", "34684"));
    validateRow(response, List.of("ixDKJGrGtFg", "O6uvpzGd5pu", "69544"));
    validateRow(response, List.of("RR538iV9G1X", "PMa2VCrupOd", "37341"));
    validateRow(response, List.of("dHrtL2a4EcD", "kJq2mPyFEHo", "66819"));
    validateRow(response, List.of("dHrtL2a4EcD", "bL4ooGhyHRQ", "40461"));
    validateRow(response, List.of("dHrtL2a4EcD", "PMa2VCrupOd", "38475"));
    validateRow(response, List.of("BDuY694ZAFa", "fdc6uOvgoji", "41500"));
    validateRow(response, List.of("RR538iV9G1X", "fdc6uOvgoji", "41232"));
    validateRow(response, List.of("RR538iV9G1X", "at6UHUQatSo", "104735"));
    validateRow(response, List.of("BDuY694ZAFa", "at6UHUQatSo", "102559"));
    validateRow(response, List.of("dHrtL2a4EcD", "jUb8gELQApl", "43292"));
    validateRow(response, List.of("BDuY694ZAFa", "eIQbndfxQMb", "42688"));
    validateRow(response, List.of("dHrtL2a4EcD", "TEQlaapDQoK", "62135"));
    validateRow(response, List.of("RR538iV9G1X", "eIQbndfxQMb", "35293"));
  }
}