package org.hisp.dhis.analytics.enrollment.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import org.hisp.dhis.actions.analytics.AnalyticsEventActions;
import org.hisp.dhis.actions.analytics.AnalyticsEnrollmentsActions;
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
 * Groups e2e tests for "/enrollments/aggregate" endpoint.
 */
public class EnrollmentsAggregate2AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();
@Test
 public void queryAggregatedenrollmentsbirthgenderthisyearlastyearlevel3org() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("stage=A03MvHHogjR")
.add("displayProperty=SHORTNAME")
.add("totalPages=false")
.add("outputType=ENROLLMENT")
.add("dimension=ou:YuQRtpLP10I,pe:THIS_YEAR;LAST_YEAR,A03MvHHogjR.cejWyOfXge6")
;

// When
 ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(4)))
.body("rows", hasSize(equalTo(5)))
.body("height", equalTo(5))
.body("width", equalTo(4))
.body("headerWidth", equalTo(4));

// Assert metaData. 
String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2023\":{\"name\":\"2023\"},\"2022\":{\"name\":\"2022\"},\"YuQRtpLP10I\":{\"name\":\"Badija\"},\"cejWyOfXge6\":{\"name\":\"Gender\"}},\"dimensions\":{\"pe\":[\"2022\",\"2023\"],\"ou\":[\"YuQRtpLP10I\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"value","Value","NUMBER","java.lang.Double",false,false);
validateHeader(response,1,"ou","Organisation unit","TEXT","java.lang.String",false,true);
validateHeader(response,2,"pe","Period","TEXT","java.lang.String",false,true);
validateHeader(response,3,"A03MvHHogjR.cejWyOfXge6","Gender","TEXT","java.lang.String",false,true);

// Assert rows.
validateRow(response, List.of("16","YuQRtpLP10I","2022","Female"));
validateRow(response, List.of("12","YuQRtpLP10I","2022","Male"));
validateRow(response, List.of("2","YuQRtpLP10I","2022",""));
validateRow(response, List.of("10","YuQRtpLP10I","2023","Female"));
validateRow(response, List.of("8","YuQRtpLP10I","2023","Male"));
}@Test
 public void queryAggregatedenrollmentsbirthgenderthisyearlast12monthslevel3org() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("stage=A03MvHHogjR")
.add("displayProperty=SHORTNAME")
.add("totalPages=false")
.add("outputType=ENROLLMENT")
.add("dimension=ou:YuQRtpLP10I,pe:LAST_12_MONTHS;THIS_YEAR,A03MvHHogjR.cejWyOfXge6")
;

// When
 ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(4)))
.body("rows", hasSize(equalTo(23)))
.body("height", equalTo(23))
.body("width", equalTo(4))
.body("headerWidth", equalTo(4));

// Assert metaData. 
String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"202208\":{\"name\":\"August 2022\"},\"202307\":{\"name\":\"July 2023\"},\"2023\":{\"name\":\"2023\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"YuQRtpLP10I\":{\"name\":\"Badija\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"cejWyOfXge6\":{\"name\":\"Gender\"},\"202211\":{\"name\":\"November 2022\"},\"202212\":{\"name\":\"December 2022\"},\"202210\":{\"name\":\"October 2022\"},\"pe\":{},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"pe\":[\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\",\"202307\",\"2023\"],\"ou\":[\"YuQRtpLP10I\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"value","Value","NUMBER","java.lang.Double",false,false);
validateHeader(response,1,"ou","Organisation unit","TEXT","java.lang.String",false,true);
validateHeader(response,2,"pe","Period","TEXT","java.lang.String",false,true);
validateHeader(response,3,"A03MvHHogjR.cejWyOfXge6","Gender","TEXT","java.lang.String",false,true);

// Assert rows.
validateRow(response, List.of("2","YuQRtpLP10I","202208","Female"));
validateRow(response, List.of("2","YuQRtpLP10I","202209","Female"));
validateRow(response, List.of("2","YuQRtpLP10I","202209","Male"));
validateRow(response, List.of("1","YuQRtpLP10I","202209",""));
validateRow(response, List.of("1","YuQRtpLP10I","202210","Male"));
validateRow(response, List.of("5","YuQRtpLP10I","202211","Female"));
validateRow(response, List.of("5","YuQRtpLP10I","202211","Male"));
validateRow(response, List.of("3","YuQRtpLP10I","202301","Female"));
validateRow(response, List.of("5","YuQRtpLP10I","202301","Male"));
validateRow(response, List.of("1","YuQRtpLP10I","202302","Male"));
validateRow(response, List.of("1","YuQRtpLP10I","202303","Female"));
validateRow(response, List.of("1","YuQRtpLP10I","202305","Female"));
validateRow(response, List.of("1","YuQRtpLP10I","202305","Male"));
validateRow(response, List.of("1","YuQRtpLP10I","202306","Female"));
validateRow(response, List.of("1","YuQRtpLP10I","202309","Male"));
validateRow(response, List.of("1","YuQRtpLP10I","202310","Female"));
validateRow(response, List.of("1","YuQRtpLP10I","202311","Female"));
validateRow(response, List.of("2","YuQRtpLP10I","202312","Female"));
validateRow(response, List.of("9","YuQRtpLP10I","2022","Female"));
validateRow(response, List.of("8","YuQRtpLP10I","2022","Male"));
validateRow(response, List.of("1","YuQRtpLP10I","2022",""));
validateRow(response, List.of("10","YuQRtpLP10I","2023","Female"));
validateRow(response, List.of("8","YuQRtpLP10I","2023","Male"));
}@Test
 public void queryAggregatedenrollmentsbirthgenderthisyearlevel2twice() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("stage=A03MvHHogjR")
.add("displayProperty=SHORTNAME")
.add("totalPages=false")
.add("outputType=ENROLLMENT")
.add("dimension=ou:kJq2mPyFEHo;O6uvpzGd5pu,pe:THIS_YEAR,A03MvHHogjR.cejWyOfXge6")
;

// When
 ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(4)))
.body("rows", hasSize(equalTo(4)))
.body("height", equalTo(4))
.body("width", equalTo(4))
.body("headerWidth", equalTo(4));

// Assert metaData. 
String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"pe\":{},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2023\":{\"name\":\"2023\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"cejWyOfXge6\":{\"name\":\"Gender\"}},\"dimensions\":{\"pe\":[\"2023\"],\"ou\":[\"kJq2mPyFEHo\",\"O6uvpzGd5pu\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"]}}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"value","Value","NUMBER","java.lang.Double",false,false);
validateHeader(response,1,"ou","Organisation unit","TEXT","java.lang.String",false,true);
validateHeader(response,2,"pe","Period","TEXT","java.lang.String",false,true);
validateHeader(response,3,"A03MvHHogjR.cejWyOfXge6","Gender","TEXT","java.lang.String",false,true);

// Assert rows.
validateRow(response, List.of("421","O6uvpzGd5pu","2023","Female"));
validateRow(response, List.of("441","O6uvpzGd5pu","2023","Male"));
validateRow(response, List.of("421","kJq2mPyFEHo","2023","Female"));
validateRow(response, List.of("452","kJq2mPyFEHo","2023","Male"));
}}