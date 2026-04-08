package org.hisp.dhis.test.generated;

import java.time.Duration;
import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.gatling.javaapi.jdbc.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static io.gatling.javaapi.jdbc.JdbcDsl.*;

public class RecordedSimulation extends Simulation {

  private HttpProtocolBuilder httpProtocol = http
    .baseUrl("http://localhost:8080")
    .inferHtmlResources(AllowList(".*/api/.*/auth/login.*", ".*/api/.*/tracker/.*", ".*/api/me.*", ".*/dhis-web-commons/security/login.*"), DenyList(".*\\.js", ".*\\.css", ".*\\.gif", ".*\\.jpeg", ".*\\.jpg", ".*\\.ico", ".*\\.woff", ".*\\.woff2", ".*\\.(t|o)tf", ".*\\.png", ".*\\.svg", ".*detectportal\\.firefox\\.com.*"))
    .acceptHeader("application/json")
    .acceptEncodingHeader("gzip, deflate, br")
    .acceptLanguageHeader("en-US,en;q=0.9")
    .userAgentHeader("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36");
  
  private Map<CharSequence, String> headers_0 = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("Origin", "http://localhost:8080"),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_1 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0819822de41407e89fe60b99663edbc20\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_2 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0477e2f8b694ce6bd75fdba047f6018ba\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_13 = Map.ofEntries(
    Map.entry("If-None-Match", "\"05b32e8abb22e54bd3c429396fb12c455\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_14 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0ea975d0cd5f48632c761e046d9912455\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_15 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0c803974723eb8bba2b0ffa84e485f1ef\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_16 = Map.ofEntries(
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_22 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0bc5984ebeb66a977b5a3b5c09b8c3c67\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_27 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0e61484cb8b09d6f4b7b63c74330f8968\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_29 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0541c0e41c8ba7165d41b200498f80878\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_32 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0019ec7127255f2155850f6f89db8be7d\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"146\", \"Not-A.Brand\";v=\"24\", \"Google Chrome\";v=\"146\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );


  private ScenarioBuilder scn = scenario("RecordedSimulation")
    .exec(
      http("request_0:POST_http://localhost:8080/api/44/auth/login")
        .post("/api/44/auth/login")
        .headers(headers_0)
        .body(RawFileBody("org/hisp/dhis/test/generated/recordedsimulation/0000_request.json"))
        .resources(
          http("request_1:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_1),
          http("request_2:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_2),
          http("request_3:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_2),
          http("request_4:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_2),
          http("request_5:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_2),
          http("request_6:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_2),
          http("request_7:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_2),
          http("request_8:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_2),
          http("request_9:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_2),
          http("request_10:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_2),
          http("request_11:GET_http://localhost:8080/api/me")
            .get("/api/me")
            .headers(headers_2)
        ),
      pause(2),
      http("request_12:GET_http://localhost:8080/api/me")
        .get("/api/me")
        .headers(headers_2),
      pause(6),
      http("request_13:GET_http://localhost:8080/api/44/tracker/trackedEntities?order=createdAt_3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=_3Aall_2C_relationships_2CprogramOwners_5BorgUnit_2Cprogram_5D")
        .get("/api/44/tracker/trackedEntities?order=createdAt%3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=%3Aall%2C!relationships%2CprogramOwners%5BorgUnit%2Cprogram%5D")
        .headers(headers_13),
      pause(1),
      http("request_14:GET_http://localhost:8080/api/44/tracker/trackedEntities?enrollmentStatus=ACTIVE&order=createdAt_3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=_3Aall_2C_relationships_2CprogramOwners_5BorgUnit_2Cprogram_5D")
        .get("/api/44/tracker/trackedEntities?enrollmentStatus=ACTIVE&order=createdAt%3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=%3Aall%2C!relationships%2CprogramOwners%5BorgUnit%2Cprogram%5D")
        .headers(headers_14),
      pause(2),
      http("request_15:GET_http://localhost:8080/api/44/tracker/trackedEntities?enrollmentStatus=ACTIVE&order=createdAt_3Adesc&page=2&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=_3Aall_2C_relationships_2CprogramOwners_5BorgUnit_2Cprogram_5D")
        .get("/api/44/tracker/trackedEntities?enrollmentStatus=ACTIVE&order=createdAt%3Adesc&page=2&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=%3Aall%2C!relationships%2CprogramOwners%5BorgUnit%2Cprogram%5D")
        .headers(headers_15),
      pause(2),
      http("request_16:GET_http://localhost:8080/api/44/tracker/trackedEntities/d5ycKDeLr8F?fields=attributes_trackedEntityType_programOwners_5Bprogram_2CorgUnit_5D")
        .get("/api/44/tracker/trackedEntities/d5ycKDeLr8F?fields=attributes,trackedEntityType,programOwners%5Bprogram%2CorgUnit%5D")
        .headers(headers_16)
        .resources(
          http("request_17:GET_http://localhost:8080/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW&fields=enrollments_5Benrollment_2Cprogram_2CtrackedEntity_2Cstatus_2CenrolledAt_5D")
            .get("/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW&fields=enrollments%5Benrollment%2Cprogram%2CtrackedEntity%2Cstatus%2CenrolledAt%5D")
            .headers(headers_16),
          http("request_18:GET_http://localhost:8080/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_16),
          http("request_19:GET_http://localhost:8080/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW")
            .get("/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW")
            .headers(headers_16),
          http("request_20:GET_http://localhost:8080/api/44/tracker/trackedEntities/d5ycKDeLr8F?fields=programOwners_5BorgUnit_5D_2Cenrollments_5Bprogram_2Cstatus_5D&program=IpHINAT79UW")
            .get("/api/44/tracker/trackedEntities/d5ycKDeLr8F?fields=programOwners%5BorgUnit%5D%2Cenrollments%5Bprogram%2Cstatus%5D&program=IpHINAT79UW")
            .headers(headers_16),
          http("request_21:GET_http://localhost:8080/api/44/tracker/enrollments/H6pezQwOxsG?fields=enrollment_2CtrackedEntity_2Cprogram_2Cstatus_2CorgUnit_2CenrolledAt_2CoccurredAt_2CfollowUp_2Cdeleted_2CcreatedBy_2CupdatedBy_2CupdatedAt_2Cgeometry")
            .get("/api/44/tracker/enrollments/H6pezQwOxsG?fields=enrollment%2CtrackedEntity%2Cprogram%2Cstatus%2CorgUnit%2CenrolledAt%2CoccurredAt%2CfollowUp%2Cdeleted%2CcreatedBy%2CupdatedBy%2CupdatedAt%2Cgeometry")
            .headers(headers_16),
          http("request_22:GET_http://localhost:8080/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_22),
          http("request_23:GET_http://localhost:8080/api/44/tracker/relationships?trackedEntity=d5ycKDeLr8F&fields=relationship_2CrelationshipType_2CcreatedAt_2Cfrom_5BtrackedEntity_5BtrackedEntity_2Cattributes_2Cprogram_2CorgUnit_2CtrackedEntityType_5D_2Cevent_5Bevent_2CdataValues_2Cprogram_2CorgUnit_2CorgUnitName_2Cstatus_2CcreatedAt_5D_5D_2Cto_5BtrackedEntity_5BtrackedEntity_2Cattributes_2Cprogram_2CorgUnit_2CtrackedEntityType_5D_2Cevent_5Bevent_2CdataValues_2Cprogram_2CorgUnit_2CorgUnitName_2Cstatus_2CcreatedAt_5D_5D&paging=false")
            .get("/api/44/tracker/relationships?trackedEntity=d5ycKDeLr8F&fields=relationship%2CrelationshipType%2CcreatedAt%2Cfrom%5BtrackedEntity%5BtrackedEntity%2Cattributes%2Cprogram%2CorgUnit%2CtrackedEntityType%5D%2Cevent%5Bevent%2CdataValues%2Cprogram%2CorgUnit%2CorgUnitName%2Cstatus%2CcreatedAt%5D%5D%2Cto%5BtrackedEntity%5BtrackedEntity%2Cattributes%2Cprogram%2CorgUnit%2CtrackedEntityType%5D%2Cevent%5Bevent%2CdataValues%2Cprogram%2CorgUnit%2CorgUnitName%2Cstatus%2CcreatedAt%5D%5D&paging=false")
            .headers(headers_16)
        ),
      pause(2),
      http("request_24:GET_http://localhost:8080/api/44/tracker/events/SUe2Avt2bU4")
        .get("/api/44/tracker/events/SUe2Avt2bU4")
        .headers(headers_16)
        .resources(
          http("request_25:GET_http://localhost:8080/api/44/tracker/relationships?event=SUe2Avt2bU4&fields=from_2Cto_2CrelationshipType_2Crelationship_2CcreatedAt")
            .get("/api/44/tracker/relationships?event=SUe2Avt2bU4&fields=from%2Cto%2CrelationshipType%2Crelationship%2CcreatedAt")
            .headers(headers_16),
          http("request_26:GET_http://localhost:8080/api/44/tracker/events/SUe2Avt2bU4?fields=program_programStage_enrollment_trackedEntity_event")
            .get("/api/44/tracker/events/SUe2Avt2bU4?fields=program,programStage,enrollment,trackedEntity,event")
            .headers(headers_16),
          http("request_27:GET_http://localhost:8080/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW")
            .get("/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW")
            .headers(headers_27),
          http("request_28:GET_http://localhost:8080/api/44/tracker/trackedEntities/d5ycKDeLr8F?fields=attributes_trackedEntityType&program=IpHINAT79UW")
            .get("/api/44/tracker/trackedEntities/d5ycKDeLr8F?fields=attributes,trackedEntityType&program=IpHINAT79UW")
            .headers(headers_16),
          http("request_29:GET_http://localhost:8080/api/44/tracker/trackedEntities/d5ycKDeLr8F?fields=programOwners_5BorgUnit_5D_2Cenrollments_5Bprogram_2Cstatus_5D&program=IpHINAT79UW")
            .get("/api/44/tracker/trackedEntities/d5ycKDeLr8F?fields=programOwners%5BorgUnit%5D%2Cenrollments%5Bprogram%2Cstatus%5D&program=IpHINAT79UW")
            .headers(headers_29),
          http("request_30:GET_http://localhost:8080/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/44/tracker/trackedEntities/d5ycKDeLr8F?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_22),
          http("request_31:GET_http://localhost:8080/api/44/tracker/events/SUe2Avt2bU4?fields=event_2Crelationships_5Brelationship_2CrelationshipType_2CrelationshipName_2Cbidirectional_2Cfrom_5Bevent_5Bevent_2CdataValues_2CoccurredAt_2CscheduledAt_2Cstatus_2CorgUnit_2CprogramStage_2Cprogram_5D_5D_2Cto_5Bevent_5Bevent_2CdataValues_2C__2CoccurredAt_2CscheduledAt_2Cstatus_2CorgUnit_2CprogramStage_2Cprogram_5D_5D_5D")
            .get("/api/44/tracker/events/SUe2Avt2bU4?fields=event%2Crelationships%5Brelationship%2CrelationshipType%2CrelationshipName%2Cbidirectional%2Cfrom%5Bevent%5Bevent%2CdataValues%2CoccurredAt%2CscheduledAt%2Cstatus%2CorgUnit%2CprogramStage%2Cprogram%5D%5D%2Cto%5Bevent%5Bevent%2CdataValues%2C*%2CoccurredAt%2CscheduledAt%2Cstatus%2CorgUnit%2CprogramStage%2Cprogram%5D%5D%5D")
            .headers(headers_16),
          http("request_32:GET_http://localhost:8080/api/44/tracker/enrollments/H6pezQwOxsG?fields=enrollment_2CtrackedEntity_2Cprogram_2Cstatus_2CorgUnit_2CenrolledAt_2CoccurredAt_2CfollowUp_2Cdeleted_2CcreatedBy_2CupdatedBy_2CupdatedAt_2Cgeometry")
            .get("/api/44/tracker/enrollments/H6pezQwOxsG?fields=enrollment%2CtrackedEntity%2Cprogram%2Cstatus%2CorgUnit%2CenrolledAt%2CoccurredAt%2CfollowUp%2Cdeleted%2CcreatedBy%2CupdatedBy%2CupdatedAt%2Cgeometry")
            .headers(headers_32),
          http("request_33:GET_http://localhost:8080/api/44/tracker/events?program=IpHINAT79UW&programStage=ZzYYXq4fJie&enrollments=H6pezQwOxsG&fields=event_2CoccurredAt_2CscheduledAt_2Cstatus_2Crelationships")
            .get("/api/44/tracker/events?program=IpHINAT79UW&programStage=ZzYYXq4fJie&enrollments=H6pezQwOxsG&fields=event%2CoccurredAt%2CscheduledAt%2Cstatus%2Crelationships")
            .headers(headers_16)
        )
    );

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
