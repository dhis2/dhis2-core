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
.add("endDate=2023-01-02")
.add("ou=O6uvpzGd5pu,fdc6uOvgoji")
.add("maxResults=30")
.add("orderBy=modified_zscore")
.add("startDate=2022-10-01")
.add("algorithm=MOD_Z_SCORE")
;

// When
 ApiResponse response = actions.query().get("",JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(4)))
.body("rows", hasSize(equalTo(0)))
.body("height", equalTo(0))
.body("width", equalTo(0))
.body("headerWidth", equalTo(4));

// Assert metaData. 
String expectedMetaData = "{\"count\":0,\"orderBy\":\"z_score\",\"threshold\":\"3.0\",\"maxResults\":30,\"algorithm\":\"MOD_Z_SCORE\"}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"aoc","Attribute option combo","TEXT","java.lang.String",false,false);
validateHeader(response,1,"dx","Data","TEXT","java.lang.String",false,false);
validateHeader(response,2,"ouname","Organisation unit name","TEXT","java.lang.String",false,false);
validateHeader(response,3,"pe","Period","TEXT","java.lang.String",false,false);

// Assert rows.
}@Test
 public void queryOutliertest6() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("endDate=2022-10-26")
.add("ou=LEVEL-m9lBJogzE95")
.add("maxResults=30")
.add("orderBy=z_score")
.add("threshold=3")
.add("startDate=2022-07-26")
.add("ds=BfMAe6Itzgt")
.add("algorithm=Z_SCORE")
;

// When
 ApiResponse response = actions.query().get("",JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(18)))
.body("rows", hasSize(equalTo(3)))
.body("height", equalTo(3))
.body("width", equalTo(18))
.body("headerWidth", equalTo(18));

// Assert metaData. 
String expectedMetaData = "{\"count\":3,\"orderBy\":\"z_score\",\"threshold\":\"3.0\",\"maxResults\":30,\"algorithm\":\"Z_SCORE\"}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"dx","Data","TEXT","java.lang.String",false,false);
validateHeader(response,1,"dxname","Data name","TEXT","java.lang.String",false,false);
validateHeader(response,2,"pe","Period","TEXT","java.lang.String",false,false);
validateHeader(response,3,"pename","Period name","TEXT","java.lang.String",false,false);
validateHeader(response,4,"ou","Organisation unit","TEXT","java.lang.String",false,false);
validateHeader(response,5,"ouname","Organisation unit name","TEXT","java.lang.String",false,false);
validateHeader(response,6,"ounamehierarchy","Organisation unit name hierarchy","TEXT","java.lang.String",false,false);
validateHeader(response,7,"coc","Category option combo","TEXT","java.lang.String",false,false);
validateHeader(response,8,"cocname","Category option combo name","TEXT","java.lang.String",false,false);
validateHeader(response,9,"aoc","Attribute option combo","TEXT","java.lang.String",false,false);
validateHeader(response,10,"aocname","Attribute option combo name","TEXT","java.lang.String",false,false);
validateHeader(response,11,"value","Value","NUMBER","java.lang.Double",false,false);
validateHeader(response,12,"mean","Mean","NUMBER","java.lang.Double",false,false);
validateHeader(response,13,"stddev","Standard deviation","NUMBER","java.lang.Double",false,false);
validateHeader(response,14,"absdev","Absolute deviation","NUMBER","java.lang.Double",false,false);
validateHeader(response,15,"zscore","zScore","NUMBER","java.lang.Double",false,false);
validateHeader(response,16,"lowerbound","Lower boundary","NUMBER","java.lang.Double",false,false);
validateHeader(response,17,"upperbound","Upper boundary","NUMBER","java.lang.Double",false,false);

// Assert rows.
validateRow(response,0, List.of("l6byfWFUGaP","Yellow Fever doses given","202209","September 2022","RhJbg8UD75Q","Yemoh Town CHC","/Sierra Leone/Bo/Kakua/Yemoh Town CHC","Prlt0C1RF0s","Fixed, <1y","HllvX50cXC0","default","466.0","48.18605","114.27966","417.81395","3.65607","-294.65294","391.02504"));
validateRow(response,1, List.of("s46m5MS0hxu","BCG doses given","202208","August 2022","CvBAqD6RzLZ","Ngalu CHC","/Sierra Leone/Bo/Bargbe/Ngalu CHC","Prlt0C1RF0s","Fixed, <1y","HllvX50cXC0","default","220.0","41.64407","57.44954","178.35593","3.10457","-130.70455","213.99269"));
validateRow(response,2, List.of("dU0GquGkGQr","Q_Early breastfeeding (within 1 hr after delivery) at BCG","202209","September 2022","Mi4dWRtfIOC","Sandaru CHC","/Sierra Leone/Kailahun/Penguia/Sandaru CHC","Prlt0C1RF0s","Fixed, <1y","HllvX50cXC0","default","105.0","18.26357","28.70554","86.73643","3.02159","-67.85306","104.38019"));
}@Test
 public void queryOutliertest7() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("headers=dx,dxname,modifiedzscore")
.add("endDate=2023-10-26")
.add("ou=ImspTQPwCqd")
.add("maxResults=30")
.add("orderBy=modified_zscore")
.add("threshold=3.0")
.add("startDate=2022-07-26")
.add("ds=BfMAe6Itzgt")
.add("algorithm=MOD_Z_SCORE")
;

// When
 ApiResponse response = actions.query().get("",JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(3)))
.body("rows", hasSize(equalTo(30)))
.body("height", equalTo(30))
.body("width", equalTo(3))
.body("headerWidth", equalTo(3));

// Assert metaData. 
String expectedMetaData = "{\"count\":30,\"orderBy\":\"z_score\",\"threshold\":\"3.0\",\"maxResults\":30,\"algorithm\":\"MOD_Z_SCORE\"}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"dx","Data","TEXT","java.lang.String",false,false);
validateHeader(response,1,"dxname","Data name","TEXT","java.lang.String",false,false);
validateHeader(response,2,"modifiedzscore","Modified zScore","NUMBER","java.lang.Double",false,false);

// Assert rows.
validateRow(response,0, List.of("O05mAByOgAv","OPV2 doses given","98.477"));
validateRow(response,1, List.of("vI2csg55S9C","OPV3 doses given","70.31663"));
validateRow(response,2, List.of("n6aMJNLdvep","Penta3 doses given","70.148"));
validateRow(response,3, List.of("UOlfIjgN8X6","Fully Immunized child","59.7607"));
validateRow(response,4, List.of("I78gJm4KBo7","Penta2 doses given","58.9513"));
validateRow(response,5, List.of("l6byfWFUGaP","Yellow Fever doses given","54.6345"));
validateRow(response,6, List.of("fClA2Erf6IO","Penta1 doses given","52.72342"));
validateRow(response,7, List.of("l6byfWFUGaP","Yellow Fever doses given","50.81233"));
validateRow(response,8, List.of("YtbsuPPo010","Measles doses given","50.81233"));
validateRow(response,9, List.of("YtbsuPPo010","Measles doses given","49.96921"));
validateRow(response,10, List.of("s46m5MS0hxu","BCG doses given","44.29217"));
validateRow(response,11, List.of("qPVDd87kS9Z","Weight for height 80 percent and above","39.7955"));
validateRow(response,12, List.of("pikOziyCXbM","OPV1 doses given","39.37394"));
validateRow(response,13, List.of("ldGXl6SEdqf","Weight for age between middle and lower line (yellow)","30.3525"));
validateRow(response,14, List.of("UOlfIjgN8X6","Fully Immunized child","29.19621"));
validateRow(response,15, List.of("NLnXLV5YpZF","Weight for age on or above middle line (green)","26.3055"));
validateRow(response,16, List.of("pnL2VG8Bn7N","Weight for height 70-79 percent","24.282"));
validateRow(response,17, List.of("qPVDd87kS9Z","Weight for height 80 percent and above","20.57225"));
validateRow(response,18, List.of("qPVDd87kS9Z","Weight for height 80 percent and above","19.11083"));
validateRow(response,19, List.of("pnL2VG8Bn7N","Weight for height 70-79 percent","18.54875"));
validateRow(response,20, List.of("GCGfEY82Wz6","Q_Slept under LLIN last night Measles","18.2115"));
validateRow(response,21, List.of("qPVDd87kS9Z","Weight for height 80 percent and above","17.19975"));
validateRow(response,22, List.of("qPVDd87kS9Z","Weight for height 80 percent and above","16.8625"));
validateRow(response,23, List.of("qPVDd87kS9Z","Weight for height 80 percent and above","16.75008"));
validateRow(response,24, List.of("qPVDd87kS9Z","Weight for height 80 percent and above","16.188"));
validateRow(response,25, List.of("dU0GquGkGQr","Q_Early breastfeeding (within 1 hr after delivery) at BCG","16.188"));
validateRow(response,26, List.of("qPVDd87kS9Z","Weight for height 80 percent and above","15.85075"));
validateRow(response,27, List.of("pnL2VG8Bn7N","Weight for height 70-79 percent","15.5135"));
validateRow(response,28, List.of("pnL2VG8Bn7N","Weight for height 70-79 percent","15.34488"));
validateRow(response,29, List.of("ldGXl6SEdqf","Weight for age between middle and lower line (yellow)","15.00762"));
}@Test
 public void queryOutliertest8() throws JSONException {
// Given 
QueryParamsBuilder params = new QueryParamsBuilder().add("headers=dx,dxname,zscore")
.add("endDate=2022-10-26")
.add("ou=ImspTQPwCqd")
.add("maxResults=30")
.add("orderBy=z_score")
.add("threshold=3.0")
.add("startDate=2022-07-26")
.add("ds=BfMAe6Itzgt")
.add("algorithm=Z_SCORE")
;

// When
 ApiResponse response = actions.query().get("",JSON, JSON, params);

// Then 
response.validate().statusCode(200)
.body("headers", hasSize(equalTo(3)))
.body("rows", hasSize(equalTo(3)))
.body("height", equalTo(3))
.body("width", equalTo(3))
.body("headerWidth", equalTo(3));

// Assert metaData. 
String expectedMetaData = "{\"count\":3,\"orderBy\":\"z_score\",\"threshold\":\"3.0\",\"maxResults\":30,\"algorithm\":\"Z_SCORE\"}";String actualMetaData = new JSONObject((Map)response.extract("metaData")).toString();
assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
validateHeader(response,0,"dx","Data","TEXT","java.lang.String",false,false);
validateHeader(response,1,"dxname","Data name","TEXT","java.lang.String",false,false);
validateHeader(response,2,"zscore","zScore","NUMBER","java.lang.Double",false,false);

// Assert rows.
validateRow(response,0, List.of("l6byfWFUGaP","Yellow Fever doses given","3.65607"));
validateRow(response,1, List.of("s46m5MS0hxu","BCG doses given","3.10457"));
validateRow(response,2, List.of("dU0GquGkGQr","Q_Early breastfeeding (within 1 hr after delivery) at BCG","3.02159"));
}}