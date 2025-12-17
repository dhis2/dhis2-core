package org.hisp.dhis.test.platform;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulation that tests the /api/organisationUnitGroups endpoint in multiple ways:
 *
 * <p>1. Calls the GET /api/organisationUnitGroups endpoint
 *
 * <p>2. Calls the GET /api/organisationUnitGroups?fields=* endpoint
 *
 * <p>10 concurrent users in total, make calls every second for 60 seconds. Users start at 0 and
 * ramp up every second to a max of 5 per endpoint.
 */
public class OrganisationUnitGroupsTest extends Simulation {

  public static final Logger logger = LoggerFactory.getLogger(OrganisationUnitGroupsTest.class);
  public static final String BASE_URL = "http://localhost:8080";
  private static final String GET_ORG_UNIT_GROUPS = "GET Organisation Unit Groups";
  private static final String GET_ORG_UNIT_GROUPS_ALL_FIELDS =
      "GET Organisation Unit Groups - all fields";

  /**
   * Setup users before simulation runs. This could be a test scenario in its own right, but the aim
   * is to isolate this problematic endpoint in its own test.
   */
  @Override
  public void before() {
    MetadataImporter.importJsonFile("platform/superuser-data-sl-db.json", "admin", "district");
  }

  public OrganisationUnitGroupsTest() {
    // setup http protocol
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .warmUp(BASE_URL + "/api/ping")
            .disableCaching();

    // user feeder
    logger.debug("creating user feeder");
    FeederBuilder<Object> circularUserFeeder =
        UserFeeder.createUserFeederFromFile(
            "platform/superuser-data-sl-db.json", UserFeeder.Strategy.CIRCULAR);

    // setup scenarios
    logger.debug("Building scenarios");
    ScenarioBuilder getOrgUnitGroupsScenario =
        scenario(GET_ORG_UNIT_GROUPS)
            .feed(circularUserFeeder)
            .repeat(1)
            .on(
                exec(http(GET_ORG_UNIT_GROUPS)
                        .get("/api/organisationUnitGroups")
                        .basicAuth("#{username}", "#{password}"))
                    .pause(1));

    ScenarioBuilder getOrgUnitGroupsAllFieldsScenario =
        scenario(GET_ORG_UNIT_GROUPS_ALL_FIELDS)
            .feed(circularUserFeeder)
            .repeat(1)
            .on(
                exec(http(GET_ORG_UNIT_GROUPS_ALL_FIELDS)
                        .get("/api/organisationUnitGroups")
                        .queryParam("fields", "*")
                        .basicAuth("#{username}", "#{password}"))
                    .pause(1));

    // how users should enter the tests
    ClosedInjectionStep closedInjection = rampConcurrentUsers(0).to(5).during(10);

    // setup simulation with scenarios, users, protocol and assertions
    setUp(
            getOrgUnitGroupsScenario.injectClosed(closedInjection),
            getOrgUnitGroupsAllFieldsScenario.injectClosed(closedInjection))
        .protocols(httpProtocol)
        .assertions(
            details(GET_ORG_UNIT_GROUPS).responseTime().percentile(95).lt(400),
            details(GET_ORG_UNIT_GROUPS).responseTime().max().lt(800),
            details(GET_ORG_UNIT_GROUPS).successfulRequests().percent().is(100D),
            details(GET_ORG_UNIT_GROUPS_ALL_FIELDS).responseTime().percentile(95).lt(600),
            details(GET_ORG_UNIT_GROUPS_ALL_FIELDS).responseTime().max().lt(1000),
            details(GET_ORG_UNIT_GROUPS_ALL_FIELDS).successfulRequests().percent().is(100D));
  }
}
