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
public class EnrollmentsAggregate4AutoTest extends AnalyticsApiTest {
  private final AnalyticsEnrollmentsActions actions = new AnalyticsEnrollmentsActions();
@Test
 public void queryAggregatedenrollmentsmacase1() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("includeMetadataDetails=true")
.add("headers=A03MvHHogjR.a3kGcGDCuk6,lZGmxYbs97q,cejWyOfXge6")
.add("displayProperty=NAME")
.add("totalPages=false")
.add("enrollmentDate=LAST_12_MONTHS")
.add("outputType=ENROLLMENT")
.add("pageSize=100")
.add("page=1")
.add("dimension=ou:lc3eMKXaEfw,A03MvHHogjR.a3kGcGDCuk6,lZGmxYbs97q,cejWyOfXge6:IN:Female")
;

// When
 ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(3)))
.body("rows", hasSize(equalTo(36)))
.body("height", equalTo(36))
.body("width", equalTo(3))
.body("headerWidth", equalTo(3));

// Assert metaData. 
String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"lZGmxYbs97q\":{\"uid\":\"lZGmxYbs97q\",\"code\":\"MMD_PER_ID\",\"name\":\"Unique ID\",\"description\":\"Unique identiifer\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"Mnp3oXrpAbK\":{\"uid\":\"Mnp3oXrpAbK\",\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"uid\":\"rBvjJYbMCVx\",\"code\":\"Male\",\"name\":\"Male\"},\"IpHINAT79UW\":{\"uid\":\"IpHINAT79UW\",\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"uid\":\"ZzYYXq4fJie\",\"name\":\"Baby Postnatal\",\"description\":\"Baby Postnatal\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"202208\":{\"uid\":\"202208\",\"code\":\"202208\",\"name\":\"August 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-08-01T00:00:00.000\",\"endDate\":\"2022-08-31T00:00:00.000\"},\"202307\":{\"uid\":\"202307\",\"code\":\"202307\",\"name\":\"July 2023\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2023-07-01T00:00:00.000\",\"endDate\":\"2023-07-31T00:00:00.000\"},\"202209\":{\"uid\":\"202209\",\"code\":\"202209\",\"name\":\"September 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-09-01T00:00:00.000\",\"endDate\":\"2022-09-30T00:00:00.000\"},\"a3kGcGDCuk6\":{\"uid\":\"a3kGcGDCuk6\",\"code\":\"DE_2006098\",\"name\":\"MCH Apgar Score\",\"description\":\"Apgar is a quick test performed on a baby at 1 and 5 minutes after birth. The 1-minute score determines how well the baby tolerated the birthing process. The 5-minute score tells the doctor how well the baby is doing outside the mother's womb.\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"202305\":{\"uid\":\"202305\",\"code\":\"202305\",\"name\":\"May 2023\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2023-05-01T00:00:00.000\",\"endDate\":\"2023-05-31T00:00:00.000\"},\"202306\":{\"uid\":\"202306\",\"code\":\"202306\",\"name\":\"June 2023\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2023-06-01T00:00:00.000\",\"endDate\":\"2023-06-30T00:00:00.000\"},\"202303\":{\"uid\":\"202303\",\"code\":\"202303\",\"name\":\"March 2023\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2023-03-01T00:00:00.000\",\"endDate\":\"2023-03-31T00:00:00.000\"},\"202304\":{\"uid\":\"202304\",\"code\":\"202304\",\"name\":\"April 2023\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2023-04-01T00:00:00.000\",\"endDate\":\"2023-04-30T00:00:00.000\"},\"202301\":{\"uid\":\"202301\",\"code\":\"202301\",\"name\":\"January 2023\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2023-01-01T00:00:00.000\",\"endDate\":\"2023-01-31T00:00:00.000\"},\"202302\":{\"uid\":\"202302\",\"code\":\"202302\",\"name\":\"February 2023\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2023-02-01T00:00:00.000\",\"endDate\":\"2023-02-28T00:00:00.000\"},\"cejWyOfXge6\":{\"uid\":\"cejWyOfXge6\",\"name\":\"Gender\",\"description\":\"Gender\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"SUM\"},\"202211\":{\"uid\":\"202211\",\"code\":\"202211\",\"name\":\"November 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-11-01T00:00:00.000\",\"endDate\":\"2022-11-30T00:00:00.000\"},\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"name\":\"December 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-12-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"202210\":{\"uid\":\"202210\",\"code\":\"202210\",\"name\":\"October 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-10-01T00:00:00.000\",\"endDate\":\"2022-10-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"dimensionType\":\"PERIOD\"},\"A03MvHHogjR\":{\"uid\":\"A03MvHHogjR\",\"name\":\"Birth\",\"description\":\"Birth of the baby\"},\"lc3eMKXaEfw\":{\"uid\":\"lc3eMKXaEfw\",\"code\":\"OU_197385\",\"name\":\"Bonthe\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"NUMBER\",\"totalAggregationType\":\"SUM\"}},\"dimensions\":{\"lZGmxYbs97q\":[],\"A03MvHHogjR.a3kGcGDCuk6\":[],\"pe\":[\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\",\"202307\"],\"ou\":[\"lc3eMKXaEfw\"],\"cejWyOfXge6\":[\"Mnp3oXrpAbK\"]}}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"A03MvHHogjR.a3kGcGDCuk6","MCH Apgar Score","NUMBER","java.lang.Double",false,true);
validateHeader(response,1,"lZGmxYbs97q","Unique ID","TEXT","java.lang.String",false,true);
validateHeader(response,2,"cejWyOfXge6","Gender","TEXT","java.lang.String",false,true);

// Assert rows.
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
validateRow(response, List.of("0.0","","Female"));
validateRow(response, List.of("1.0","","Female"));
validateRow(response, List.of("2.0","","Female"));
}@Test
 public void queryAggregatedenrollmentsmacase2() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("filter=pTo4uMt3xur.F3ogKBuviRA")
.add("stage=pTo4uMt3xur")
.add("displayProperty=NAME")
.add("dimension=ou:ImspTQPwCqd,pe:LAST_12_MONTHS")
;

// When
 ApiResponse response = actions.aggregate().get("VBqh0ynB2wv", JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(3)))
.body("rows", hasSize(equalTo(0)))
.body("height", equalTo(0))
.body("width", equalTo(0))
.body("headerWidth", equalTo(3));

// Assert metaData. 
String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ou\":{\"name\":\"Organisation unit\"},\"202208\":{\"name\":\"August 2022\"},\"202307\":{\"name\":\"July 2023\"},\"202209\":{\"name\":\"September 2022\"},\"202305\":{\"name\":\"May 2023\"},\"202306\":{\"name\":\"June 2023\"},\"202303\":{\"name\":\"March 2023\"},\"202304\":{\"name\":\"April 2023\"},\"202301\":{\"name\":\"January 2023\"},\"202302\":{\"name\":\"February 2023\"},\"202211\":{\"name\":\"November 2022\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202212\":{\"name\":\"December 2022\"},\"F3ogKBuviRA\":{\"name\":\"Household location\"},\"202210\":{\"name\":\"October 2022\"},\"pe\":{},\"VBqh0ynB2wv\":{\"name\":\"Malaria case registration\"},\"pTo4uMt3xur\":{\"name\":\"Malaria case registration\"}},\"dimensions\":{\"F3ogKBuviRA\":[],\"pe\":[\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"202301\",\"202302\",\"202303\",\"202304\",\"202305\",\"202306\",\"202307\"],\"ou\":[\"ImspTQPwCqd\"]}}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"value","Value","NUMBER","java.lang.Double",false,false);
validateHeader(response,1,"ou","Organisation unit","TEXT","java.lang.String",false,true);
validateHeader(response,2,"pe","Period","TEXT","java.lang.String",false,true);

// Assert rows.
}@Test
 public void queryAggregatedenrollmentsmacase3() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("stage=pTo4uMt3xur")
.add("endDate=2023-08-29")
.add("displayProperty=NAME")
.add("totalPages=false")
.add("dimension=ou:ImspTQPwCqd,pTo4uMt3xur.oZg33kd9taw:IN:Female")
.add("startDate=2023-05-29")
;

// When
 ApiResponse response = actions.aggregate().get("VBqh0ynB2wv", JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(3)))
.body("rows", hasSize(equalTo(0)))
.body("height", equalTo(0))
.body("width", equalTo(0))
.body("headerWidth", equalTo(3));

// Assert metaData. 
String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"Mnp3oXrpAbK\":{\"code\":\"Female\",\"name\":\"Female\"},\"rBvjJYbMCVx\":{\"code\":\"Male\",\"name\":\"Male\"},\"VBqh0ynB2wv\":{\"name\":\"Malaria case registration\"},\"ou\":{\"name\":\"Organisation unit\"},\"oZg33kd9taw\":{\"name\":\"Gender\"},\"pTo4uMt3xur\":{\"name\":\"Malaria case registration\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"],\"oZg33kd9taw\":[\"Mnp3oXrpAbK\"]}}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"value","Value","NUMBER","java.lang.Double",false,false);
validateHeader(response,1,"ou","Organisation unit","TEXT","java.lang.String",false,true);
validateHeader(response,2,"pTo4uMt3xur.oZg33kd9taw","Gender","TEXT","java.lang.String",false,true);

// Assert rows.
}@Test
 public void queryAggregatedenrollmentsmacase4() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("stage=ZzYYXq4fJie")
.add("endDate=2023-08-29")
.add("displayProperty=NAME")
.add("totalPages=false")
.add("outputType=ENROLLMENT")
.add("dimension=ou:ImspTQPwCqd")
.add("startDate=2023-05-29")
;

// When
 ApiResponse response = actions.aggregate().get("IpHINAT79UW", JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(2)))
.body("rows", hasSize(equalTo(1)))
.body("height", equalTo(1))
.body("width", equalTo(2))
.body("headerWidth", equalTo(2));

// Assert metaData. 
String expectedMetaData = "{\"pager\":{\"page\":1,\"pageSize\":50,\"isLastPage\":true},\"items\":{\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"ZzYYXq4fJie\":{\"name\":\"Baby Postnatal\"},\"ou\":{\"name\":\"Organisation unit\"},\"A03MvHHogjR\":{\"name\":\"Birth\"}},\"dimensions\":{\"pe\":[],\"ou\":[\"ImspTQPwCqd\"]}}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"value","Value","NUMBER","java.lang.Double",false,false);
validateHeader(response,1,"ou","Organisation unit","TEXT","java.lang.String",false,true);

// Assert rows.
validateRow(response, List.of("2006","ImspTQPwCqd"));
}}