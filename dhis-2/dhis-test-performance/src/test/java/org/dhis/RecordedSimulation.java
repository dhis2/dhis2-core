package org.dhis;

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
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99"),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "Linux")
  );
  
  private Map<CharSequence, String> headers_1 = Map.ofEntries(
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99"),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "Linux")
  );
  
  private Map<CharSequence, String> headers_13 = Map.ofEntries(
    Map.entry("If-None-Match", "07011c07320a03600170c3e9b9e01d0f7"),
    Map.entry("Sec-Fetch-Dest", "empty"),
    Map.entry("Sec-Fetch-Mode", "cors"),
    Map.entry("Sec-Fetch-Site", "same-origin"),
    Map.entry("X-Requested-With", "XMLHttpRequest"),
    Map.entry("sec-ch-ua", "Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99"),
    Map.entry("sec-ch-ua-mobile", "?0"),
    Map.entry("sec-ch-ua-platform", "Linux")
  );


  private ScenarioBuilder scn = scenario("RecordedSimulation")
    .exec(
      http("request_0:POST_http://localhost:8080/api/42/auth/login")
        .post("/api/42/auth/login")
        .headers(headers_0)
        .body(RawFileBody("org/dhis/recordedsimulation/0000_request.json")),
      pause(859),
      http("request_1:GET_http://localhost:8080/api/42/tracker/trackedEntities?order=createdAt_3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=_3Aall_2C_relationships_2CprogramOwners_5BorgUnit_2Cprogram_5D")
        .get("/api/42/tracker/trackedEntities?order=createdAt%3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=%3Aall%2C!relationships%2CprogramOwners%5BorgUnit%2Cprogram%5D")
        .headers(headers_1),
      pause(1),
      http("request_2:GET_http://localhost:8080/api/42/tracker/trackedEntities?enrollmentStatus=ACTIVE&order=createdAt_3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=_3Aall_2C_relationships_2CprogramOwners_5BorgUnit_2Cprogram_5D")
        .get("/api/42/tracker/trackedEntities?enrollmentStatus=ACTIVE&order=createdAt%3Adesc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program=IpHINAT79UW&fields=%3Aall%2C!relationships%2CprogramOwners%5BorgUnit%2Cprogram%5D")
        .headers(headers_1),
      pause(3),
      http("request_3:GET_http://localhost:8080/api/42/tracker/trackedEntities?program=IpHINAT79UW&orgUnitMode=ACCESSIBLE&filter=lZGmxYbs97q_3AEQ_3A0737511")
        .get("/api/42/tracker/trackedEntities?program=IpHINAT79UW&orgUnitMode=ACCESSIBLE&filter=lZGmxYbs97q%3AEQ%3A0737511")
        .headers(headers_1)
        .resources(
          http("request_4:GET_http://localhost:8080/api/42/tracker/trackedEntities?trackedEntityType=nEenWmSyUEp&orgUnitMode=ACCESSIBLE&filter=lZGmxYbs97q_3AEQ_3A0737511")
            .get("/api/42/tracker/trackedEntities?trackedEntityType=nEenWmSyUEp&orgUnitMode=ACCESSIBLE&filter=lZGmxYbs97q%3AEQ%3A0737511")
            .headers(headers_1)
        ),
      pause(12),
      http("request_5:GET_http://localhost:8080/api/42/tracker/trackedEntities/HDq5OTV0q4q?fields=attributes_trackedEntityType_programOwners")
        .get("/api/42/tracker/trackedEntities/HDq5OTV0q4q?fields=attributes,trackedEntityType,programOwners")
        .headers(headers_1)
        .resources(
          http("request_6:GET_http://localhost:8080/api/42/tracker/trackedEntities/HDq5OTV0q4q?program=IpHINAT79UW&fields=programOwners_5Bprogram_2CorgUnit_5D")
            .get("/api/42/tracker/trackedEntities/HDq5OTV0q4q?program=IpHINAT79UW&fields=programOwners%5Bprogram%2CorgUnit%5D")
            .headers(headers_1),
          http("request_7:GET_http://localhost:8080/api/42/tracker/trackedEntities/HDq5OTV0q4q?program=IpHINAT79UW&fields=enrollments")
            .get("/api/42/tracker/trackedEntities/HDq5OTV0q4q?program=IpHINAT79UW&fields=enrollments")
            .headers(headers_1),
          http("request_8:GET_http://localhost:8080/api/42/tracker/enrollments/BrFhFtPfZEf?fields=enrollment_2CtrackedEntity_2Cprogram_2Cstatus_2CorgUnit_2CenrolledAt_2CoccurredAt_2CfollowUp_2Cdeleted_2CcreatedBy_2CupdatedBy_2CupdatedAt_2Cgeometry")
            .get("/api/42/tracker/enrollments/BrFhFtPfZEf?fields=enrollment%2CtrackedEntity%2Cprogram%2Cstatus%2CorgUnit%2CenrolledAt%2CoccurredAt%2CfollowUp%2Cdeleted%2CcreatedBy%2CupdatedBy%2CupdatedAt%2Cgeometry")
            .headers(headers_1),
          http("request_9:GET_http://localhost:8080/api/42/tracker/trackedEntities/HDq5OTV0q4q?program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/HDq5OTV0q4q?program=IpHINAT79UW")
            .headers(headers_1),
          http("request_10:GET_http://localhost:8080/api/42/tracker/relationships?trackedEntity=HDq5OTV0q4q&fields=relationship_2CrelationshipType_2CcreatedAt_2Cfrom_5BtrackedEntity_5BtrackedEntity_2Cattributes_2Cprogram_2CorgUnit_2CtrackedEntityType_5D_2Cevent_5Bevent_2CdataValues_2Cprogram_2CorgUnit_2CorgUnitName_2Cstatus_2CcreatedAt_5D_5D_2Cto_5BtrackedEntity_5BtrackedEntity_2Cattributes_2Cprogram_2CorgUnit_2CtrackedEntityType_5D_2Cevent_5Bevent_2CdataValues_2Cprogram_2CorgUnit_2CorgUnitName_2Cstatus_2CcreatedAt_5D_5D&paging=false")
            .get("/api/42/tracker/relationships?trackedEntity=HDq5OTV0q4q&fields=relationship%2CrelationshipType%2CcreatedAt%2Cfrom%5BtrackedEntity%5BtrackedEntity%2Cattributes%2Cprogram%2CorgUnit%2CtrackedEntityType%5D%2Cevent%5Bevent%2CdataValues%2Cprogram%2CorgUnit%2CorgUnitName%2Cstatus%2CcreatedAt%5D%5D%2Cto%5BtrackedEntity%5BtrackedEntity%2Cattributes%2Cprogram%2CorgUnit%2CtrackedEntityType%5D%2Cevent%5Bevent%2CdataValues%2Cprogram%2CorgUnit%2CorgUnitName%2Cstatus%2CcreatedAt%5D%5D&paging=false")
            .headers(headers_1),
          http("request_11:GET_http://localhost:8080/api/42/tracker/trackedEntities/HDq5OTV0q4q?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/HDq5OTV0q4q?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_1),
          http("request_12:GET_http://localhost:8080/api/42/tracker/trackedEntities/HDq5OTV0q4q?fields=programOwners_5BorgUnit_5D_2Cenrollments&program=IpHINAT79UW")
            .get("/api/42/tracker/trackedEntities/HDq5OTV0q4q?fields=programOwners%5BorgUnit%5D%2Cenrollments&program=IpHINAT79UW")
            .headers(headers_1),
          http("request_13:GET_http://localhost:8080/api/42/tracker/trackedEntities/HDq5OTV0q4q?program=IpHINAT79UW&fields=enrollments_5B__2C_attributes_5D_2Cattributes")
            .get("/api/42/tracker/trackedEntities/HDq5OTV0q4q?program=IpHINAT79UW&fields=enrollments%5B*%2C!attributes%5D%2Cattributes")
            .headers(headers_13)
        )
    );

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
