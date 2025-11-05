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
    .userAgentHeader("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");
  
  private Map<CharSequence, String> headers_0 = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("Origin", "http://localhost:8080"),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_1 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0f9b584f74278e85415f29d9f7722bd57\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_2 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0b427aafa62766224eba55648fc7445a9\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_3 = Map.ofEntries(
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_12 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0f9348d2f08ca9612dd82c1d6c6d743fa\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_18 = Map.ofEntries(
    Map.entry("If-None-Match", "\"04245f38c78e86a781c0bd641d31c3fbd\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_19 = Map.ofEntries(
    Map.entry("If-None-Match", "\"01cc6f2a775c68232a97820fb79670640\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_22 = Map.ofEntries(
    Map.entry("If-None-Match", "\"099473a22e2ff2ffd352658dc0da72352\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_26 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0f0210aeaa628ee043b452fd3b7e0d9ff\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_27 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0356a14bd1fa47ed2c515ce3025cd4cd5\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_28 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0f04e9807e0b6098f61ffbf9bf603afed\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_33 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0b0cb64d9b11d43c652f79bd1d0319faa\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_40 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0085ddec6d5fcb4e5e915cad4873df229\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_43 = Map.ofEntries(
    Map.entry("If-None-Match", "\"0c70320bfe5d69c49efef82903bc1163b\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_53 = Map.ofEntries(
    Map.entry("If-None-Match", "\"08a896fc03d5d67579875fcda8b19b4bd\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );
  
  private Map<CharSequence, String> headers_57 = Map.ofEntries(
    Map.entry("If-None-Match", "\"06e6d93b587e8ef5f7e44e26bc846d415\""),
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\""),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "\"Linux\"")
  );


  private ScenarioBuilder scn = scenario("RecordedSimulation")
    .exec(
      http("request_0:POST_http://localhost:8080/api/42/auth/login")
        .post("/api/42/auth/login")
        .headers(headers_0)
        .body(RawFileBody("org/hisp/dhis/test/generated/recordedsimulation/0000_request.json")),
      pause(13),
      http("request_1:GET_http://localhost:8080/api/42/tracker/trackedEntities?order=createdAt_3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=_3Aall_2C_relationships_2CprogramOwners_5BorgUnit_2Cprogram_5D")
        .get("/api/42/tracker/trackedEntities?order=createdAt%3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=%3Aall%2C!relationships%2CprogramOwners%5BorgUnit%2Cprogram%5D")
        .headers(headers_1),
      pause(1),
      http("request_2:GET_http://localhost:8080/api/42/tracker/trackedEntities?enrollmentStatus=ACTIVE&order=createdAt_3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=_3Aall_2C_relationships_2CprogramOwners_5BorgUnit_2Cprogram_5D")
        .get("/api/42/tracker/trackedEntities?enrollmentStatus=ACTIVE&order=createdAt%3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=%3Aall%2C!relationships%2CprogramOwners%5BorgUnit%2Cprogram%5D")
        .headers(headers_2),
      pause(4),
      http("request_3:GET_http://localhost:8080/api/42/tracker/trackedEntities?enrollmentStatus=ACTIVE&enrollmentOccurredAfter=2025-01-01&enrollmentOccurredBefore=2025-12-31&order=createdAt_3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=_3Aall_2C_relationships_2CprogramOwners_5BorgUnit_2Cprogram_5D")
        .get("/api/42/tracker/trackedEntities?enrollmentStatus=ACTIVE&enrollmentOccurredAfter=2025-01-01&enrollmentOccurredBefore=2025-12-31&order=createdAt%3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=%3Aall%2C!relationships%2CprogramOwners%5BorgUnit%2Cprogram%5D")
        .headers(headers_3),
      pause(1),
      http("request_4:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=attributes_trackedEntityType_programOwners")
        .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=attributes,trackedEntityType,programOwners")
        .headers(headers_3)
        .resources(
          http("request_5:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=programOwners_5Bprogram_2CorgUnit_5D")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=programOwners%5Bprogram%2CorgUnit%5D")
            .headers(headers_3),
          http("request_6:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments")
            .headers(headers_3),
          http("request_7:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW")
            .headers(headers_3),
          http("request_8:GET_http://localhost:8080/api/42/tracker/relationships?trackedEntity=iGIUvkCkaTl&fields=relationship_2CrelationshipType_2CcreatedAt_2Cfrom_5BtrackedEntity_5BtrackedEntity_2Cattributes_2Cprogram_2CorgUnit_2CtrackedEntityType_5D_2Cevent_5Bevent_2CdataValues_2Cprogram_2CorgUnit_2CorgUnitName_2Cstatus_2CcreatedAt_5D_5D_2Cto_5BtrackedEntity_5BtrackedEntity_2Cattributes_2Cprogram_2CorgUnit_2CtrackedEntityType_5D_2Cevent_5Bevent_2CdataValues_2Cprogram_2CorgUnit_2CorgUnitName_2Cstatus_2CcreatedAt_5D_5D&paging=false")
            .get("/api/42/tracker/relationships?trackedEntity=iGIUvkCkaTl&fields=relationship%2CrelationshipType%2CcreatedAt%2Cfrom%5BtrackedEntity%5BtrackedEntity%2Cattributes%2Cprogram%2CorgUnit%2CtrackedEntityType%5D%2Cevent%5Bevent%2CdataValues%2Cprogram%2CorgUnit%2CorgUnitName%2Cstatus%2CcreatedAt%5D%5D%2Cto%5BtrackedEntity%5BtrackedEntity%2Cattributes%2Cprogram%2CorgUnit%2CtrackedEntityType%5D%2Cevent%5Bevent%2CdataValues%2Cprogram%2CorgUnit%2CorgUnitName%2Cstatus%2CcreatedAt%5D%5D&paging=false")
            .headers(headers_3),
          http("request_9:GET_http://localhost:8080/api/42/tracker/enrollments/uXrVPC2ob3X?fields=enrollment_2CtrackedEntity_2Cprogram_2Cstatus_2CorgUnit_2CenrolledAt_2CoccurredAt_2CfollowUp_2Cdeleted_2CcreatedBy_2CupdatedBy_2CupdatedAt_2Cgeometry")
            .get("/api/42/tracker/enrollments/uXrVPC2ob3X?fields=enrollment%2CtrackedEntity%2Cprogram%2Cstatus%2CorgUnit%2CenrolledAt%2CoccurredAt%2CfollowUp%2Cdeleted%2CcreatedBy%2CupdatedBy%2CupdatedAt%2Cgeometry")
            .headers(headers_3),
          http("request_10:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_3),
          http("request_11:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=programOwners_5BorgUnit_5D_2Cenrollments&program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=programOwners%5BorgUnit%5D%2Cenrollments&program=IpHINAT79UW")
            .headers(headers_3),
          http("request_12:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_12)
        ),
      pause(2),
      http("request_13:GET_http://localhost:8080/api/42/tracker/events/BP72AZQDB5x")
        .get("/api/42/tracker/events/BP72AZQDB5x")
        .headers(headers_3)
        .resources(
          http("request_14:GET_http://localhost:8080/api/42/tracker/events/BP72AZQDB5x?fields=program_programStage_enrollment_trackedEntity_event")
            .get("/api/42/tracker/events/BP72AZQDB5x?fields=program,programStage,enrollment,trackedEntity,event")
            .headers(headers_3),
          http("request_15:GET_http://localhost:8080/api/42/tracker/relationships?event=BP72AZQDB5x&fields=from_2Cto_2CrelationshipType_2Crelationship_2CcreatedAt")
            .get("/api/42/tracker/relationships?event=BP72AZQDB5x&fields=from%2Cto%2CrelationshipType%2Crelationship%2CcreatedAt")
            .headers(headers_3),
          http("request_16:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=attributes_trackedEntityType&program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=attributes,trackedEntityType&program=IpHINAT79UW")
            .headers(headers_3),
          http("request_17:GET_http://localhost:8080/api/42/tracker/events/BP72AZQDB5x?fields=event_2Crelationships_5Brelationship_2CrelationshipType_2CrelationshipName_2Cbidirectional_2Cfrom_5Bevent_5Bevent_2CdataValues_2CoccurredAt_2CscheduledAt_2Cstatus_2CorgUnit_2CprogramStage_2Cprogram_5D_5D_2Cto_5Bevent_5Bevent_2CdataValues_2C__2CoccurredAt_2CscheduledAt_2Cstatus_2CorgUnit_2CprogramStage_2Cprogram_5D_5D_5D")
            .get("/api/42/tracker/events/BP72AZQDB5x?fields=event%2Crelationships%5Brelationship%2CrelationshipType%2CrelationshipName%2Cbidirectional%2Cfrom%5Bevent%5Bevent%2CdataValues%2CoccurredAt%2CscheduledAt%2Cstatus%2CorgUnit%2CprogramStage%2Cprogram%5D%5D%2Cto%5Bevent%5Bevent%2CdataValues%2C*%2CoccurredAt%2CscheduledAt%2Cstatus%2CorgUnit%2CprogramStage%2Cprogram%5D%5D%5D")
            .headers(headers_3),
          http("request_18:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW")
            .headers(headers_18),
          http("request_19:GET_http://localhost:8080/api/42/tracker/enrollments/uXrVPC2ob3X?fields=enrollment_2CtrackedEntity_2Cprogram_2Cstatus_2CorgUnit_2CenrolledAt_2CoccurredAt_2CfollowUp_2Cdeleted_2CcreatedBy_2CupdatedBy_2CupdatedAt_2Cgeometry")
            .get("/api/42/tracker/enrollments/uXrVPC2ob3X?fields=enrollment%2CtrackedEntity%2Cprogram%2Cstatus%2CorgUnit%2CenrolledAt%2CoccurredAt%2CfollowUp%2Cdeleted%2CcreatedBy%2CupdatedBy%2CupdatedAt%2Cgeometry")
            .headers(headers_19),
          http("request_20:GET_http://localhost:8080/api/42/tracker/events?program=IpHINAT79UW&programStage=A03MvHHogjR&enrollments=uXrVPC2ob3X&fields=event_2CoccurredAt_2CscheduledAt_2Cstatus_2Crelationships")
            .get("/api/42/tracker/events?program=IpHINAT79UW&programStage=A03MvHHogjR&enrollments=uXrVPC2ob3X&fields=event%2CoccurredAt%2CscheduledAt%2Cstatus%2Crelationships")
            .headers(headers_3),
          http("request_21:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_12),
          http("request_22:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=programOwners_5BorgUnit_5D_2Cenrollments&program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=programOwners%5BorgUnit%5D%2Cenrollments&program=IpHINAT79UW")
            .headers(headers_22)
        ),
      pause(4),
      http("request_23:POST_http://localhost:8080/api/42/tracker/events/BP72AZQDB5x/note")
        .post("/api/42/tracker/events/BP72AZQDB5x/note")
        .headers(headers_0)
        .body(RawFileBody("org/hisp/dhis/test/generated/recordedsimulation/0023_request.json")),
      pause(4),
      http("request_24:POST_http://localhost:8080/api/42/tracker/events/BP72AZQDB5x/note")
        .post("/api/42/tracker/events/BP72AZQDB5x/note")
        .headers(headers_0)
        .body(RawFileBody("org/hisp/dhis/test/generated/recordedsimulation/0024_request.json")),
      pause(2),
      http("request_25:GET_http://localhost:8080/api/42/tracker/enrollments/uXrVPC2ob3X?fields=trackedEntity_program")
        .get("/api/42/tracker/enrollments/uXrVPC2ob3X?fields=trackedEntity,program")
        .headers(headers_3)
        .resources(
          http("request_26:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=attributes_trackedEntityType_programOwners")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=attributes,trackedEntityType,programOwners")
            .headers(headers_26),
          http("request_27:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=programOwners_5Bprogram_2CorgUnit_5D")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=programOwners%5Bprogram%2CorgUnit%5D")
            .headers(headers_27),
          http("request_28:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments")
            .headers(headers_28),
          http("request_29:GET_http://localhost:8080/api/42/tracker/enrollments/uXrVPC2ob3X?fields=enrollment_2CtrackedEntity_2Cprogram_2Cstatus_2CorgUnit_2CenrolledAt_2CoccurredAt_2CfollowUp_2Cdeleted_2CcreatedBy_2CupdatedBy_2CupdatedAt_2Cgeometry")
            .get("/api/42/tracker/enrollments/uXrVPC2ob3X?fields=enrollment%2CtrackedEntity%2Cprogram%2Cstatus%2CorgUnit%2CenrolledAt%2CoccurredAt%2CfollowUp%2Cdeleted%2CcreatedBy%2CupdatedBy%2CupdatedAt%2Cgeometry")
            .headers(headers_19),
          http("request_30:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW")
            .headers(headers_18),
          http("request_31:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=programOwners_5BorgUnit_5D_2Cenrollments&program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?fields=programOwners%5BorgUnit%5D%2Cenrollments&program=IpHINAT79UW")
            .headers(headers_22),
          http("request_32:GET_http://localhost:8080/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/iGIUvkCkaTl?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_12)
        ),
      pause(1),
      http("request_33:GET_http://localhost:8080/api/42/tracker/trackedEntities?enrollmentStatus=ACTIVE&enrollmentOccurredAfter=2025-01-01&enrollmentOccurredBefore=2025-12-31&order=createdAt_3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=_3Aall_2C_relationships_2CprogramOwners_5BorgUnit_2Cprogram_5D")
        .get("/api/42/tracker/trackedEntities?enrollmentStatus=ACTIVE&enrollmentOccurredAfter=2025-01-01&enrollmentOccurredBefore=2025-12-31&order=createdAt%3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=%3Aall%2C!relationships%2CprogramOwners%5BorgUnit%2Cprogram%5D")
        .headers(headers_33),
      pause(3),
      http("request_34:GET_http://localhost:8080/api/42/tracker/trackedEntities/EaOyKGOIGRp?fields=attributes_trackedEntityType_programOwners")
        .get("/api/42/tracker/trackedEntities/EaOyKGOIGRp?fields=attributes,trackedEntityType,programOwners")
        .headers(headers_3)
        .resources(
          http("request_35:GET_http://localhost:8080/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW")
            .headers(headers_3),
          http("request_36:GET_http://localhost:8080/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW&fields=programOwners_5Bprogram_2CorgUnit_5D")
            .get("/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW&fields=programOwners%5Bprogram%2CorgUnit%5D")
            .headers(headers_3),
          http("request_37:GET_http://localhost:8080/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_3),
          http("request_38:GET_http://localhost:8080/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW&fields=enrollments")
            .get("/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW&fields=enrollments")
            .headers(headers_3),
          http("request_39:GET_http://localhost:8080/api/42/tracker/enrollments/wBU0RAsYjKE?fields=enrollment_2CtrackedEntity_2Cprogram_2Cstatus_2CorgUnit_2CenrolledAt_2CoccurredAt_2CfollowUp_2Cdeleted_2CcreatedBy_2CupdatedBy_2CupdatedAt_2Cgeometry")
            .get("/api/42/tracker/enrollments/wBU0RAsYjKE?fields=enrollment%2CtrackedEntity%2Cprogram%2Cstatus%2CorgUnit%2CenrolledAt%2CoccurredAt%2CfollowUp%2Cdeleted%2CcreatedBy%2CupdatedBy%2CupdatedAt%2Cgeometry")
            .headers(headers_3),
          http("request_40:GET_http://localhost:8080/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW")
            .headers(headers_40),
          http("request_41:GET_http://localhost:8080/api/42/tracker/relationships?trackedEntity=EaOyKGOIGRp&fields=relationship_2CrelationshipType_2CcreatedAt_2Cfrom_5BtrackedEntity_5BtrackedEntity_2Cattributes_2Cprogram_2CorgUnit_2CtrackedEntityType_5D_2Cevent_5Bevent_2CdataValues_2Cprogram_2CorgUnit_2CorgUnitName_2Cstatus_2CcreatedAt_5D_5D_2Cto_5BtrackedEntity_5BtrackedEntity_2Cattributes_2Cprogram_2CorgUnit_2CtrackedEntityType_5D_2Cevent_5Bevent_2CdataValues_2Cprogram_2CorgUnit_2CorgUnitName_2Cstatus_2CcreatedAt_5D_5D&paging=false")
            .get("/api/42/tracker/relationships?trackedEntity=EaOyKGOIGRp&fields=relationship%2CrelationshipType%2CcreatedAt%2Cfrom%5BtrackedEntity%5BtrackedEntity%2Cattributes%2Cprogram%2CorgUnit%2CtrackedEntityType%5D%2Cevent%5Bevent%2CdataValues%2Cprogram%2CorgUnit%2CorgUnitName%2Cstatus%2CcreatedAt%5D%5D%2Cto%5BtrackedEntity%5BtrackedEntity%2Cattributes%2Cprogram%2CorgUnit%2CtrackedEntityType%5D%2Cevent%5Bevent%2CdataValues%2Cprogram%2CorgUnit%2CorgUnitName%2Cstatus%2CcreatedAt%5D%5D&paging=false")
            .headers(headers_3),
          http("request_42:GET_http://localhost:8080/api/42/tracker/trackedEntities/EaOyKGOIGRp?fields=programOwners_5BorgUnit_5D_2Cenrollments&program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/EaOyKGOIGRp?fields=programOwners%5BorgUnit%5D%2Cenrollments&program=IpHINAT79UW")
            .headers(headers_3),
          http("request_43:GET_http://localhost:8080/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_43),
          http("request_44:GET_http://localhost:8080/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/EaOyKGOIGRp?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_43)
        ),
      pause(10),
      http("request_45:GET_http://localhost:8080/api/42/tracker/trackedEntities?program=IpHINAT79UW&orgUnitMode=ACCESSIBLE&filter=lZGmxYbs97q_3AEQ_3A1845344")
        .get("/api/42/tracker/trackedEntities?program=IpHINAT79UW&orgUnitMode=ACCESSIBLE&filter=lZGmxYbs97q%3AEQ%3A1845344")
        .headers(headers_3)
        .resources(
          http("request_46:GET_http://localhost:8080/api/42/tracker/trackedEntities?trackedEntityType=nEenWmSyUEp&orgUnitMode=ACCESSIBLE&filter=lZGmxYbs97q_3AEQ_3A1845344")
            .get("/api/42/tracker/trackedEntities?trackedEntityType=nEenWmSyUEp&orgUnitMode=ACCESSIBLE&filter=lZGmxYbs97q%3AEQ%3A1845344")
            .headers(headers_3)
        ),
      pause(12),
      http("request_47:GET_http://localhost:8080/api/42/tracker/trackedEntities?orgUnitMode=ACCESSIBLE&pageSize=5&page=1&filter=w75KJ2mc4zz_3Alike_3AFrank&filter=zDhUuAYrxNC_3Alike_3AOcean&filter=cejWyOfXge6_3Aeq_3AMale&program=IpHINAT79UW")
        .get("/api/42/tracker/trackedEntities?orgUnitMode=ACCESSIBLE&pageSize=5&page=1&filter=w75KJ2mc4zz%3Alike%3AFrank&filter=zDhUuAYrxNC%3Alike%3AOcean&filter=cejWyOfXge6%3Aeq%3AMale&program=IpHINAT79UW")
        .headers(headers_3)
        .resources(
          http("request_48:GET_http://localhost:8080/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW")
            .headers(headers_3),
          http("request_49:GET_http://localhost:8080/api/42/tracker/trackedEntities/FueCGEopcbo?fields=attributes_trackedEntityType_programOwners")
            .get("/api/42/tracker/trackedEntities/FueCGEopcbo?fields=attributes,trackedEntityType,programOwners")
            .headers(headers_3),
          http("request_50:GET_http://localhost:8080/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW&fields=programOwners_5Bprogram_2CorgUnit_5D")
            .get("/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW&fields=programOwners%5Bprogram%2CorgUnit%5D")
            .headers(headers_3),
          http("request_51:GET_http://localhost:8080/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_3),
          http("request_52:GET_http://localhost:8080/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW&fields=enrollments")
            .get("/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW&fields=enrollments")
            .headers(headers_3),
          http("request_53:GET_http://localhost:8080/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW")
            .headers(headers_53),
          http("request_54:GET_http://localhost:8080/api/42/tracker/relationships?trackedEntity=FueCGEopcbo&fields=relationship_2CrelationshipType_2CcreatedAt_2Cfrom_5BtrackedEntity_5BtrackedEntity_2Cattributes_2Cprogram_2CorgUnit_2CtrackedEntityType_5D_2Cevent_5Bevent_2CdataValues_2Cprogram_2CorgUnit_2CorgUnitName_2Cstatus_2CcreatedAt_5D_5D_2Cto_5BtrackedEntity_5BtrackedEntity_2Cattributes_2Cprogram_2CorgUnit_2CtrackedEntityType_5D_2Cevent_5Bevent_2CdataValues_2Cprogram_2CorgUnit_2CorgUnitName_2Cstatus_2CcreatedAt_5D_5D&paging=false")
            .get("/api/42/tracker/relationships?trackedEntity=FueCGEopcbo&fields=relationship%2CrelationshipType%2CcreatedAt%2Cfrom%5BtrackedEntity%5BtrackedEntity%2Cattributes%2Cprogram%2CorgUnit%2CtrackedEntityType%5D%2Cevent%5Bevent%2CdataValues%2Cprogram%2CorgUnit%2CorgUnitName%2Cstatus%2CcreatedAt%5D%5D%2Cto%5BtrackedEntity%5BtrackedEntity%2Cattributes%2Cprogram%2CorgUnit%2CtrackedEntityType%5D%2Cevent%5Bevent%2CdataValues%2Cprogram%2CorgUnit%2CorgUnitName%2Cstatus%2CcreatedAt%5D%5D&paging=false")
            .headers(headers_3),
          http("request_55:GET_http://localhost:8080/api/42/tracker/enrollments/HzmYn1Ma4du?fields=enrollment_2CtrackedEntity_2Cprogram_2Cstatus_2CorgUnit_2CenrolledAt_2CoccurredAt_2CfollowUp_2Cdeleted_2CcreatedBy_2CupdatedBy_2CupdatedAt_2Cgeometry")
            .get("/api/42/tracker/enrollments/HzmYn1Ma4du?fields=enrollment%2CtrackedEntity%2Cprogram%2Cstatus%2CorgUnit%2CenrolledAt%2CoccurredAt%2CfollowUp%2Cdeleted%2CcreatedBy%2CupdatedBy%2CupdatedAt%2Cgeometry")
            .headers(headers_3),
          http("request_56:GET_http://localhost:8080/api/42/tracker/trackedEntities/FueCGEopcbo?fields=programOwners_5BorgUnit_5D_2Cenrollments&program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/FueCGEopcbo?fields=programOwners%5BorgUnit%5D%2Cenrollments&program=IpHINAT79UW")
            .headers(headers_3),
          http("request_57:GET_http://localhost:8080/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_57),
          http("request_58:GET_http://localhost:8080/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/FueCGEopcbo?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_57)
        )
    );

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
