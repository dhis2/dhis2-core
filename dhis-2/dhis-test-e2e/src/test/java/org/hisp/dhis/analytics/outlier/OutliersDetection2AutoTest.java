package org.hisp.dhis.analytics.outlier;

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
import org.hisp.dhis.actions.analytics.AnalyticsOutlierDetectionActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
/**
 * Groups e2e tests for "/analytics/outlierDetection" endpoint.
 */
public class OutliersDetection2AutoTest extends AnalyticsApiTest {
  private AnalyticsOutlierDetectionActions actions = new AnalyticsOutlierDetectionActions();
@Test
 public void queryOutliertest5() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("headers=aoc,dx,ouname,pe")
.add("dx=Y7Oq71I3ASg")
.add("endDate=2024-01-02")
.add("ou=O6uvpzGd5pu,fdc6uOvgoji")
.add("maxResults=100")
.add("startDate=2020-10-01")
.add("algorithm=MOD_Z_SCORE")
;

// When
 ApiResponse response = actions.query().get("",JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(4)))
.body("rows", hasSize(equalTo(6)))
.body("height", equalTo(6))
.body("width", equalTo(4))
.body("headerWidth", equalTo(4));

// Assert metaData. 
String expectedMetaData = "{\"count\":6,\"orderBy\":\"middle_value_abs_dev\",\"threshold\":\"3.0\",\"maxResults\":100,\"algorithm\":\"MOD_Z_SCORE\"}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"aoc","Attribute option combo","TEXT","java.lang.String",false,false);
validateHeader(response,1,"dx","Data","TEXT","java.lang.String",false,false);
validateHeader(response,2,"ouname","Organisation unit name","TEXT","java.lang.String",false,false);
validateHeader(response,3,"pe","Period","TEXT","java.lang.String",false,false);

// Assert rows.
validateRow(response,0, List.of("HllvX50cXC0","Y7Oq71I3ASg","Baoma Station CHP","202104"));
validateRow(response,1, List.of("HllvX50cXC0","Y7Oq71I3ASg","Baoma Station CHP","202203"));
validateRow(response,2, List.of("HllvX50cXC0","Y7Oq71I3ASg","Gerehun CHC","202102"));
validateRow(response,3, List.of("HllvX50cXC0","Y7Oq71I3ASg","Gerehun CHC","202202"));
validateRow(response,4, List.of("HllvX50cXC0","Y7Oq71I3ASg","Gondama (Tikonko) CHC","202108"));
validateRow(response,5, List.of("HllvX50cXC0","Y7Oq71I3ASg","Gondama (Tikonko) CHC","202208"));
}}