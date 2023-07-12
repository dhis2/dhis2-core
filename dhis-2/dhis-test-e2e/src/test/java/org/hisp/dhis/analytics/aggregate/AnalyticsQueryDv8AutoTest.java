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
public class AnalyticsQueryDv8AutoTest extends AnalyticsApiTest {

  private RestApiActions actions;

  @BeforeAll
  public void setup() {
    actions = new RestApiActions("analytics");
  }

  @Test
  public void queryMortalityDiseaseNarrativesNgelehunOct2014() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=ou:DiszpKrYNg8")
        .add("skipData=false")
        .add("includeNumDen=true")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add("dimension=pe:202210,dx:mxc1T932aWM;a0WhmKHnZ6J;FaVPxpiCab5;iT8n0tI3nRW;rj9mw1J6sBg")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(5)))
        .body("height", equalTo(5))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"DiszpKrYNg8\":{\"name\":\"Ngelehun CHC\"},\"202210\":{\"name\":\"October 2022\"},\"dx\":{\"name\":\"Data\"},\"pe\":{\"name\":\"Period\"},\"ou\":{\"name\":\"Organisation unit\"},\"FaVPxpiCab5\":{\"name\":\"Measles (Deaths < 5 yrs) Narrative\"},\"HllvX50cXC0\":{\"name\":\"default\"},\"rj9mw1J6sBg\":{\"name\":\"Yellow Fever (Deaths < 5 yrs) Narrative\"},\"mxc1T932aWM\":{\"name\":\"Cholera (Deaths < 5 yrs) Narrative\"},\"iT8n0tI3nRW\":{\"name\":\"Plague (Deaths < 5 yrs) Narrative\"},\"a0WhmKHnZ6J\":{\"name\":\"Malaria (Deaths < 5 yrs) Narrative\"}},\"dimensions\":{\"dx\":[\"mxc1T932aWM\",\"a0WhmKHnZ6J\",\"FaVPxpiCab5\",\"iT8n0tI3nRW\",\"rj9mw1J6sBg\"],\"pe\":[\"202210\"],\"ou\":[\"DiszpKrYNg8\"],\"co\":[\"HllvX50cXC0\"]}}";
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
    validateRow(response, List.of("iT8n0tI3nRW", "202210",
        "Plague is a deadly infectious disease that is caused by the enterobacteria Yersinia pestis, named after the French-Swiss bacteriologist Alexandre Yersin.\n\nUntil June 2007, plague was one of the three epidemic diseases specifically reportable to the World Health Organization (the other two being cholera and yellow fever).[1]\n\nDepending on lung infection, or sanitary conditions, plague can be spread in the air, by direct contact, or by contaminated undercooked food or materials. The symptoms of plague depend on the concentrated areas of infection in each person: bubonic plague in lymph nodes, septicemic plague in blood vessels, pneumonic plague in lungs, and so on. It is treatable if detected early. Plague is still endemic in some parts of the world.",
        "", "", "", "", ""));
    validateRow(response, List.of("a0WhmKHnZ6J", "202210",
        "Malaria is caused by a parasite called plasmodium, which is transmitted via the bites of infected mosquitoes. In the human body, the parasites multiply in the liver, and then infect red blood cells.\n\nSymptoms of malaria include fever, headache, and vomiting, and usually appear between 10 and 15 days after the mosquito bite. If not treated, malaria can quickly become life-threatening by disrupting the blood supply to vital organs. In many parts of the world, the parasites have developed resistance to a number of malaria medicines.\n\nKey interventions to control malaria include: prompt and effective treatment with artemisinin-based combination therapies; use of insecticidal nets by people at risk; and indoor residual spraying with insecticide to control the vector mosquitoes.",
        "", "", "", "", ""));
    validateRow(response, List.of("FaVPxpiCab5", "202210",
        "Measles, also known as morbilli, English measles, or rubeola (and not to be confused with rubella or roseola) is an infection of the respiratory system, immune system and skin caused by a virus, specifically a paramyxovirus of the genus Morbillivirus.[1][2] Symptoms usually develop 7–14 days (average 10–12) after exposure to an infected person and the initial symptoms usually include a high fever (often > 40 °C [104 °F]), Koplik's spots (spots in the mouth, these usually appear 1–2 days prior to the rash and last 3–5 days), malaise, loss of appetite, hacking cough (although this may be the last symptom to appear), runny nose and red eyes.[1][3] After this comes a spot-like rash that covers much of the body.[1] The course of measles such as bacterial infections, usually lasts about 7–10 days.[1]",
        "", "", "", "", ""));
    validateRow(response, List.of("mxc1T932aWM", "202210",
        "Cholera is an infection of the small intestine caused by the bacterium Vibrio cholerae.\n\nThe main symptoms are watery diarrhea and vomiting. This may result in dehydration and in severe cases grayish-bluish skin.[1] Transmission occurs primarily by drinking water or eating food that has been contaminated by the feces (waste product) of an infected person, including one with no apparent symptoms.\n\nThe severity of the diarrhea and vomiting can lead to rapid dehydration and electrolyte imbalance, and death in some cases. The primary treatment is oral rehydration therapy, typically with oral rehydration solution, to replace water and electrolytes. If this is not tolerated or does not provide improvement fast enough, intravenous fluids can also be used. Antibacterial drugs are beneficial in those with severe disease to shorten its duration and severity.",
        "", "", "", "", ""));
    validateRow(response, List.of("rj9mw1J6sBg", "202210",
        "Yellow fever, known historically as yellow jack or yellow plague,[1] is an acute viral disease.[2] In most cases, symptoms include fever, chills, loss of appetite, nausea, muscle pains particularly in the back, and headaches.[2] Symptoms typically improve within five days.[2] In some people within a day of improving, the fever comes back, abdominal pain occurs, and liver damage begins causing yellow skin.[2] If this occurs, the risk of bleeding and kidney problems is also increased.[2]\n\nThe disease is caused by the yellow fever virus and is spread by the bite of the female mosquito.[2] It only infects humans, other primates, and several species of mosquitoes.[2] In cities, it is primarily spread by mosquitoes of the Aedes aegypti species.[2] The virus is an RNA virus of the genus Flavivirus.[3] The disease may be difficult to tell apart from other illnesses.",
        "", "", "", "", ""));
  }

  @Test
  public void queryNarrativesWithDataNgelehunOct2014() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=ou:DiszpKrYNg8")
        .add("skipData=false")
        .add("includeNumDen=false")
        .add("displayProperty=NAME")
        .add("skipMeta=true")
        .add(
            "dimension=pe:202210,dx:eY5ehpbEsB7;mxc1T932aWM;r6nrJANOqMw;a0WhmKHnZ6J;f7n9E0hX8qk;FaVPxpiCab5;lXolhoWewYH;iT8n0tI3nRW;USBq0VHSkZq;rj9mw1J6sBg")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(10)))
        .body("height", equalTo(10))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

// Assert metaData.
    String expectedMetaData = "{}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "value", "Value", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response, List.of("iT8n0tI3nRW", "202210",
        "Plague is a deadly infectious disease that is caused by the enterobacteria Yersinia pestis, named after the French-Swiss bacteriologist Alexandre Yersin.\n\nUntil June 2007, plague was one of the three epidemic diseases specifically reportable to the World Health Organization (the other two being cholera and yellow fever).[1]\n\nDepending on lung infection, or sanitary conditions, plague can be spread in the air, by direct contact, or by contaminated undercooked food or materials. The symptoms of plague depend on the concentrated areas of infection in each person: bubonic plague in lymph nodes, septicemic plague in blood vessels, pneumonic plague in lungs, and so on. It is treatable if detected early. Plague is still endemic in some parts of the world."));
    validateRow(response, List.of("a0WhmKHnZ6J", "202210",
        "Malaria is caused by a parasite called plasmodium, which is transmitted via the bites of infected mosquitoes. In the human body, the parasites multiply in the liver, and then infect red blood cells.\n\nSymptoms of malaria include fever, headache, and vomiting, and usually appear between 10 and 15 days after the mosquito bite. If not treated, malaria can quickly become life-threatening by disrupting the blood supply to vital organs. In many parts of the world, the parasites have developed resistance to a number of malaria medicines.\n\nKey interventions to control malaria include: prompt and effective treatment with artemisinin-based combination therapies; use of insecticidal nets by people at risk; and indoor residual spraying with insecticide to control the vector mosquitoes."));
    validateRow(response, List.of("FaVPxpiCab5", "202210",
        "Measles, also known as morbilli, English measles, or rubeola (and not to be confused with rubella or roseola) is an infection of the respiratory system, immune system and skin caused by a virus, specifically a paramyxovirus of the genus Morbillivirus.[1][2] Symptoms usually develop 7–14 days (average 10–12) after exposure to an infected person and the initial symptoms usually include a high fever (often > 40 °C [104 °F]), Koplik's spots (spots in the mouth, these usually appear 1–2 days prior to the rash and last 3–5 days), malaise, loss of appetite, hacking cough (although this may be the last symptom to appear), runny nose and red eyes.[1][3] After this comes a spot-like rash that covers much of the body.[1] The course of measles such as bacterial infections, usually lasts about 7–10 days.[1]"));
    validateRow(response, List.of("r6nrJANOqMw", "202210", "34"));
    validateRow(response, List.of("eY5ehpbEsB7", "202210", "3"));
    validateRow(response, List.of("f7n9E0hX8qk", "202210", "54"));
    validateRow(response, List.of("USBq0VHSkZq", "202210", "7"));
    validateRow(response, List.of("mxc1T932aWM", "202210",
        "Cholera is an infection of the small intestine caused by the bacterium Vibrio cholerae.\n\nThe main symptoms are watery diarrhea and vomiting. This may result in dehydration and in severe cases grayish-bluish skin.[1] Transmission occurs primarily by drinking water or eating food that has been contaminated by the feces (waste product) of an infected person, including one with no apparent symptoms.\n\nThe severity of the diarrhea and vomiting can lead to rapid dehydration and electrolyte imbalance, and death in some cases. The primary treatment is oral rehydration therapy, typically with oral rehydration solution, to replace water and electrolytes. If this is not tolerated or does not provide improvement fast enough, intravenous fluids can also be used. Antibacterial drugs are beneficial in those with severe disease to shorten its duration and severity."));
    validateRow(response, List.of("rj9mw1J6sBg", "202210",
        "Yellow fever, known historically as yellow jack or yellow plague,[1] is an acute viral disease.[2] In most cases, symptoms include fever, chills, loss of appetite, nausea, muscle pains particularly in the back, and headaches.[2] Symptoms typically improve within five days.[2] In some people within a day of improving, the fever comes back, abdominal pain occurs, and liver damage begins causing yellow skin.[2] If this occurs, the risk of bleeding and kidney problems is also increased.[2]\n\nThe disease is caused by the yellow fever virus and is spread by the bite of the female mosquito.[2] It only infects humans, other primates, and several species of mosquitoes.[2] In cities, it is primarily spread by mosquitoes of the Aedes aegypti species.[2] The virus is an RNA virus of the genus Flavivirus.[3] The disease may be difficult to tell apart from other illnesses."));
    validateRow(response, List.of("lXolhoWewYH", "202210", "5"));
  }

  @Test
  public void queryChildHealthAndInpatientIndicators() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=ou:ImspTQPwCqd")
        .add("skipData=false")
        .add("includeNumDen=true")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=pe:LAST_12_MONTHS,dx:luLGbE2WKGP;hAHF3BEHGjM;ST1CDYkWY4g;nq5ohBSWj6E;htr2mMY515K;tUdBD1JDxpn;sGna2pquXOO;Kswd1r4qWLh;gWxh7DiRmG7;ToQVD4irW3Q;ReQEl5V3z6p;HS8QXAJtuKV;vDdRoZYybP2;p2Zxg0wcPQ3;hCYU0G5Ti2T;rXoaHGAXWy9;fM7RZGVndZE;x7PaHGvgWY2;XCMi7Wvnplm;hlPt8H4bUOQ;Thkx2BnO5Kq;Y7hKDSuqEtH")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(8)))
        .body("rows", hasSize(equalTo(264)))
        .body("height", equalTo(264))
        .body("width", equalTo(8))
        .body("headerWidth", equalTo(8));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"ST1CDYkWY4g\":{\"name\":\"Inpatient cases female under 5 y\"},\"hlPt8H4bUOQ\":{\"name\":\"BMI female under 5 y\"},\"202109\":{\"name\":\"September 2021\"},\"202107\":{\"name\":\"July 2021\"},\"hCYU0G5Ti2T\":{\"name\":\"BCG doses low birth weight\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"Kswd1r4qWLh\":{\"name\":\"Average height of boys at 10 years old\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"htr2mMY515K\":{\"name\":\"Inpatient cases male under 5 y\"},\"202112\":{\"name\":\"December 2021\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"202110\":{\"name\":\"October 2021\"},\"ToQVD4irW3Q\":{\"name\":\"Average height under 5 y\"},\"vDdRoZYybP2\":{\"name\":\"Inpatient cases under 5 y\"},\"202111\":{\"name\":\"November 2021\"},\"dx\":{\"name\":\"Data\"},\"tUdBD1JDxpn\":{\"name\":\"Average age of deaths\"},\"sGna2pquXOO\":{\"name\":\"Average age of female discharges\"},\"x7PaHGvgWY2\":{\"name\":\"BMI\"},\"XCMi7Wvnplm\":{\"name\":\"BMI female\"},\"p2Zxg0wcPQ3\":{\"name\":\"BCG doses\"},\"fM7RZGVndZE\":{\"name\":\"Measles + Yellow fever doses\"},\"Thkx2BnO5Kq\":{\"name\":\"BMI male\"},\"nq5ohBSWj6E\":{\"name\":\"Inpatient cases male\"},\"ou\":{\"name\":\"Organisation unit\"},\"hAHF3BEHGjM\":{\"name\":\"Inpatient cases female\"},\"Y7hKDSuqEtH\":{\"name\":\"BMI male under 5 y\"},\"202101\":{\"name\":\"January 2021\"},\"gWxh7DiRmG7\":{\"name\":\"Average height of girls at 5 years old\"},\"202102\":{\"name\":\"February 2021\"},\"luLGbE2WKGP\":{\"name\":\"Inpatient cases\"},\"rXoaHGAXWy9\":{\"name\":\"Health immunization score\"},\"pe\":{\"name\":\"Period\"},\"HS8QXAJtuKV\":{\"name\":\"Inpatient bed days average\"},\"ReQEl5V3z6p\":{\"name\":\"Average weight of women\"}},\"dimensions\":{\"dx\":[\"luLGbE2WKGP\",\"hAHF3BEHGjM\",\"ST1CDYkWY4g\",\"nq5ohBSWj6E\",\"htr2mMY515K\",\"tUdBD1JDxpn\",\"sGna2pquXOO\",\"Kswd1r4qWLh\",\"gWxh7DiRmG7\",\"ToQVD4irW3Q\",\"ReQEl5V3z6p\",\"HS8QXAJtuKV\",\"vDdRoZYybP2\",\"p2Zxg0wcPQ3\",\"hCYU0G5Ti2T\",\"rXoaHGAXWy9\",\"fM7RZGVndZE\",\"x7PaHGvgWY2\",\"XCMi7Wvnplm\",\"hlPt8H4bUOQ\",\"Thkx2BnO5Kq\",\"Y7hKDSuqEtH\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[]}}";
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
    validateRow(response, List.of("luLGbE2WKGP", "202101", "4601.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202102", "4109.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202103", "4700.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202104", "4473.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202105", "4571.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202106", "4398.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202107", "4690.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202108", "4515.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202109", "4435.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202110", "4534.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202111", "4470.0", "", "", "", "", ""));
    validateRow(response, List.of("luLGbE2WKGP", "202112", "4272.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202101", "2290.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202102", "2027.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202103", "2363.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202104", "2290.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202105", "2305.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202106", "2198.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202107", "2362.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202108", "2221.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202109", "2179.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202110", "2252.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202111", "2156.0", "", "", "", "", ""));
    validateRow(response, List.of("hAHF3BEHGjM", "202112", "2171.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202101", "154.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202102", "152.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202103", "157.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202104", "154.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202105", "172.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202106", "145.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202107", "158.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202108", "145.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202109", "161.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202110", "146.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202111", "135.0", "", "", "", "", ""));
    validateRow(response, List.of("ST1CDYkWY4g", "202112", "143.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202101", "2311.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202102", "2082.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202103", "2337.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202104", "2182.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202105", "2266.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202106", "2200.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202107", "2328.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202108", "2294.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202109", "2256.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202110", "2281.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202111", "2314.0", "", "", "", "", ""));
    validateRow(response, List.of("nq5ohBSWj6E", "202112", "2101.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202101", "141.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202102", "136.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202103", "163.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202104", "163.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202105", "126.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202106", "162.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202107", "158.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202108", "154.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202109", "149.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202110", "156.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202111", "181.0", "", "", "", "", ""));
    validateRow(response, List.of("htr2mMY515K", "202112", "139.0", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202101", "42.8", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202102", "43.8", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202103", "43.6", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202104", "42.6", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202105", "42.8", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202106", "42.8", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202107", "43.9", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202108", "44.2", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202109", "43.4", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202110", "43.5", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202111", "43.8", "", "", "", "", ""));
    validateRow(response, List.of("tUdBD1JDxpn", "202112", "42.6", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202101", "43.0", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202102", "43.8", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202103", "44.9", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202104", "44.7", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202105", "45.1", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202106", "44.0", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202107", "42.0", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202108", "45.4", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202109", "44.4", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202110", "43.3", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202111", "44.9", "", "", "", "", ""));
    validateRow(response, List.of("sGna2pquXOO", "202112", "42.3", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202101", "140.4", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202102", "110.7", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202103", "112.1", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202104", "108.7", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202105", "135.3", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202106", "114.2", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202107", "106.4", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202108", "115.9", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202109", "127.5", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202110", "110.1", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202111", "123.3", "", "", "", "", ""));
    validateRow(response, List.of("Kswd1r4qWLh", "202112", "108.4", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202101", "116.5", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202102", "111.5", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202103", "116.2", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202104", "119.3", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202105", "122.2", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202106", "133.4", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202107", "115.5", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202108", "135.3", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202109", "129.3", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202110", "95.3", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202111", "109.5", "", "", "", "", ""));
    validateRow(response, List.of("gWxh7DiRmG7", "202112", "103.1", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202107", "49.9", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202103", "49.0", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202112", "51.3", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202104", "48.2", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202106", "49.3", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202108", "48.7", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202102", "50.4", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202105", "49.0", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202110", "48.2", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202109", "48.9", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202111", "49.1", "", "", "", "", ""));
    validateRow(response, List.of("ToQVD4irW3Q", "202101", "49.2", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202101", "49.9", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202102", "49.5", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202103", "49.3", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202104", "50.3", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202105", "49.5", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202106", "49.9", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202107", "49.3", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202108", "49.7", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202109", "50.1", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202110", "50.1", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202111", "49.1", "", "", "", "", ""));
    validateRow(response, List.of("ReQEl5V3z6p", "202112", "50.2", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202101", "13.6", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202102", "13.6", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202103", "13.6", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202104", "13.6", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202105", "13.4", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202106", "13.6", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202107", "13.6", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202108", "13.6", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202109", "13.6", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202110", "13.6", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202111", "13.5", "", "", "", "", ""));
    validateRow(response, List.of("HS8QXAJtuKV", "202112", "13.6", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202107", "316.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202103", "320.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202112", "282.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202104", "317.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202106", "307.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202108", "299.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202102", "288.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202105", "298.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202110", "303.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202109", "310.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202111", "316.0", "", "", "", "", ""));
    validateRow(response, List.of("vDdRoZYybP2", "202101", "295.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202107", "482.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202103", "493.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202112", "439.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202104", "457.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202106", "474.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202108", "470.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202102", "426.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202105", "430.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202110", "489.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202109", "411.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202111", "466.0", "", "", "", "", ""));
    validateRow(response, List.of("p2Zxg0wcPQ3", "202101", "466.0", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202107", "0.51", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202103", "0.59", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202112", "0.48", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202104", "0.5", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202106", "0.48", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202108", "0.47", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202102", "0.52", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202105", "0.51", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202110", "0.53", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202109", "0.51", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202111", "0.52", "", "", "", "", ""));
    validateRow(response, List.of("hCYU0G5Ti2T", "202101", "0.5", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202107", "1.47", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202103", "1.35", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202112", "1.48", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202104", "1.43", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202106", "1.42", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202108", "1.4", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202102", "1.44", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202105", "1.45", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202110", "1.38", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202109", "1.41", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202111", "1.45", "", "", "", "", ""));
    validateRow(response, List.of("rXoaHGAXWy9", "202101", "1.1", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202107", "1.0", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202103", "0.97", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202112", "1.0", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202104", "1.0", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202106", "1.0", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202108", "1.0", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202102", "1.0", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202105", "1.0", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202110", "0.98", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202109", "0.98", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202111", "1.0", "", "", "", "", ""));
    validateRow(response, List.of("fM7RZGVndZE", "202101", "0.99", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202101", "66.3", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202102", "61.9", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202103", "62.9", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202104", "65.0", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202105", "64.4", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202106", "63.6", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202107", "65.9", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202108", "64.2", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202109", "63.7", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202110", "67.0", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202111", "62.5", "", "", "", "", ""));
    validateRow(response, List.of("x7PaHGvgWY2", "202112", "63.4", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202101", "66.9", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202102", "61.8", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202103", "63.0", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202104", "63.7", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202105", "64.5", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202106", "63.6", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202107", "66.7", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202108", "63.9", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202109", "64.7", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202110", "67.6", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202111", "62.4", "", "", "", "", ""));
    validateRow(response, List.of("XCMi7Wvnplm", "202112", "63.7", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202101", "60.8", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202102", "59.9", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202103", "56.0", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202104", "72.0", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202105", "73.6", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202106", "62.2", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202107", "63.0", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202108", "69.2", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202109", "63.1", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202110", "63.8", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202111", "62.7", "", "", "", "", ""));
    validateRow(response, List.of("hlPt8H4bUOQ", "202112", "60.5", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202101", "65.8", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202102", "61.9", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202103", "62.7", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202104", "66.4", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202105", "64.2", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202106", "63.6", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202107", "65.2", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202108", "64.4", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202109", "62.7", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202110", "66.4", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202111", "62.7", "", "", "", "", ""));
    validateRow(response, List.of("Thkx2BnO5Kq", "202112", "63.1", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202101", "75.5", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202102", "60.8", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202103", "65.1", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202104", "61.9", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202105", "63.6", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202106", "69.2", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202107", "68.5", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202108", "65.7", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202109", "69.8", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202110", "69.2", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202111", "61.1", "", "", "", "", ""));
    validateRow(response, List.of("Y7hKDSuqEtH", "202112", "71.8", "", "", "", "", ""));
  }

  @Test
  public void queryDataByFacilityTypeAndOwnershipallOptions() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("filter=ou:ImspTQPwCqd")
        .add("skipData=false")
        .add("includeNumDen=true")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:s46m5MS0hxu;UOlfIjgN8X6;YtbsuPPo010;x3Do5e7g4Qo;pikOziyCXbM;O05mAByOgAv;vI2csg55S9C;fClA2Erf6IO;I78gJm4KBo7;n6aMJNLdvep;l6byfWFUGaP,pe:LAST_4_QUARTERS,J5jldMd8OHv,Bpx0589u8y0")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(10)))
        .body("rows", hasSize(equalTo(221)))
        .body("height", equalTo(221))
        .body("width", equalTo(10))
        .body("headerWidth", equalTo(10));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"n6aMJNLdvep\":{\"name\":\"Penta3 doses given\"},\"vI2csg55S9C\":{\"name\":\"OPV3 doses given\"},\"J5jldMd8OHv\":{\"name\":\"Facility Type\"},\"O05mAByOgAv\":{\"name\":\"OPV2 doses given\"},\"fClA2Erf6IO\":{\"name\":\"Penta1 doses given\"},\"uYxK4wmcPqA\":{\"name\":\"CHP\"},\"2021Q4\":{\"name\":\"October - December 2021\"},\"MAs88nJc9nL\":{\"name\":\"Private Clinic\"},\"I78gJm4KBo7\":{\"name\":\"Penta2 doses given\"},\"ImspTQPwCqd\":{\"name\":\"Sierra Leone\"},\"tDZVQ1WtwpA\":{\"name\":\"Hospital\"},\"YtbsuPPo010\":{\"name\":\"Measles doses given\"},\"dx\":{\"name\":\"Data\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"s46m5MS0hxu\":{\"name\":\"BCG doses given\"},\"l6byfWFUGaP\":{\"name\":\"Yellow Fever doses given\"},\"w0gFTTmsUcF\":{\"name\":\"Mission\"},\"pikOziyCXbM\":{\"name\":\"OPV1 doses given\"},\"RXL3lPSK8oG\":{\"name\":\"Clinic\"},\"psbwp3CQEhs\":{\"name\":\"Fixed, >1y\"},\"ou\":{\"name\":\"Organisation unit\"},\"Prlt0C1RF0s\":{\"name\":\"Fixed, <1y\"},\"CXw2yu5fodb\":{\"name\":\"CHC\"},\"hEFKSsPV5et\":{\"name\":\"Outreach, >1y\"},\"V6L425pT3A0\":{\"name\":\"Outreach, <1y\"},\"2021Q2\":{\"name\":\"April - June 2021\"},\"2021Q3\":{\"name\":\"July - September 2021\"},\"UOlfIjgN8X6\":{\"name\":\"Fully Immunized child\"},\"x3Do5e7g4Qo\":{\"name\":\"OPV0 doses given\"},\"2021Q1\":{\"name\":\"January - March 2021\"},\"pe\":{\"name\":\"Period\"},\"EYbopBOJWsW\":{\"name\":\"MCHP\"},\"PVLOW4bCshG\":{\"name\":\"NGO\"},\"oRVt7g429ZO\":{\"name\":\"Public facilities\"},\"Bpx0589u8y0\":{\"name\":\"Facility Ownership\"}},\"dimensions\":{\"dx\":[\"s46m5MS0hxu\",\"UOlfIjgN8X6\",\"YtbsuPPo010\",\"x3Do5e7g4Qo\",\"pikOziyCXbM\",\"O05mAByOgAv\",\"vI2csg55S9C\",\"fClA2Erf6IO\",\"I78gJm4KBo7\",\"n6aMJNLdvep\",\"l6byfWFUGaP\"],\"pe\":[\"2021Q1\",\"2021Q2\",\"2021Q3\",\"2021Q4\"],\"J5jldMd8OHv\":[\"CXw2yu5fodb\",\"tDZVQ1WtwpA\",\"RXL3lPSK8oG\",\"uYxK4wmcPqA\",\"EYbopBOJWsW\"],\"ou\":[\"ImspTQPwCqd\"],\"co\":[\"hEFKSsPV5et\",\"V6L425pT3A0\",\"psbwp3CQEhs\",\"Prlt0C1RF0s\"],\"Bpx0589u8y0\":[\"MAs88nJc9nL\",\"PVLOW4bCshG\",\"w0gFTTmsUcF\",\"oRVt7g429ZO\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false,
        true);
    validateHeader(response, 3, "Bpx0589u8y0", "Facility Ownership", "TEXT", "java.lang.String",
        false, true);
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
        List.of("s46m5MS0hxu", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "116", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "3156", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "1810", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "1485", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "43", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "2169", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "1015", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "51", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "1371", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "145", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "55", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "47", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "12317", "", "", "", "",
            ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "3", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "927", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "96", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "110", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "13418", "", "", "", "",
            ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "1251", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "30", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "2207", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "1632", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "55", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "47", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "45", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "44", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "27", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "1530", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "1224", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "11647", "", "", "", "",
            ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "43", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "2215", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "1461", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "12940", "", "", "", "",
            ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "40", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "2122", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "25", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "1573", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "3215", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "2455", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "1867", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "44", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "12246", "", "", "", "",
            ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "1899", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "1705", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "2695", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "51", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "145", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "43", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "3", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "1947", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "38", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "995", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "3645", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "13868", "", "", "", "",
            ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "30", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "47", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "29", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "12224", "", "", "", "",
            ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "2252", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "43", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "1419", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "15660", "", "", "", "",
            ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "3", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "1142", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "78", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "14861", "", "", "", "",
            ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "1976", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "11788", "", "", "", "",
            ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "1545", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "12896", "", "", "", "",
            ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "11209", "", "", "", "",
            ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "2124", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "1132", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "1202", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "55", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "1140", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "1978", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "11029", "", "", "", "",
            ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "1515", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "92", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "7", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "11276", "", "", "", "",
            ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "66", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "1207", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "13362", "", "", "", "",
            ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "3", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "100", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "2205", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "1686", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "43", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "12678", "", "", "", "",
            ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "11514", "", "", "", "",
            ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "1984", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "1226", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "7", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "3", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "84", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "27", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "1410", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "66", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "3513", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "2244", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "1683", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "4", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "18", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "14739", "", "", "", "",
            ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "78", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "739", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "1126", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "10877", "", "", "", "",
            ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "2349", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "17", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "24", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "75", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "43", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "1908", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "1371", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "10374", "", "", "", "",
            ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "2693", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "75", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "2468", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "4", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "92", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "1531", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "1043", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "12630", "", "", "", "",
            ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "27", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "12398", "", "", "", "",
            ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "92", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "108", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "29", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "842", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "57", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "1550", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "87", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "12230", "", "", "", "",
            ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "96", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "1805", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "1959", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "18071", "", "", "", "",
            ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "12449", "", "", "", "",
            ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "12684", "", "", "", "",
            ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "29", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "1894", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "1126", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "1615", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "34", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "55", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "12605", "", "", "", "",
            ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "27", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "27", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "1481", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "1028", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "2208", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "12516", "", "", "", "",
            ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "43", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "2184", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "2102", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "10", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "45", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "1546", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "3412", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "38", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "15", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "22", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "1270", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "36", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "16", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "13033", "", "", "", "",
            ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "2303", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "1529", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "40", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "14488", "", "", "", "",
            ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q1", "EYbopBOJWsW", "oRVt7g429ZO", "1", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "27", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "87", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "1372", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "29", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "13146", "", "", "", "",
            ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "13984", "", "", "", "",
            ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "14508", "", "", "", "",
            ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "11754", "", "", "", "",
            ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "144", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "14554", "", "", "", "",
            ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "1429", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "1151", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "2427", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "108", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "11185", "", "", "", "",
            ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "2810", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "2386", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "934", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "11615", "", "", "", "",
            ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "2740", "", "", "", "", ""));
    validateRow(response,
        List.of("I78gJm4KBo7", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "20", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "29", "", "", "", "", ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "2146", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "55", "", "", "", "", ""));
    validateRow(response,
        List.of("pikOziyCXbM", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "15261", "", "", "", "",
            ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "16", "", "", "", "", ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "2772", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "36", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "1863", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "25", "", "", "", "", ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "2132", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "3300", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "1949", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "16", "", "", "", "", ""));
    validateRow(response,
        List.of("l6byfWFUGaP", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "2056", "", "", "", "", ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "16681", "", "", "", "",
            ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "55", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "15", "", "", "", "", ""));
    validateRow(response,
        List.of("x3Do5e7g4Qo", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "10418", "", "", "", "",
            ""));
    validateRow(response,
        List.of("fClA2Erf6IO", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "15416", "", "", "", "",
            ""));
    validateRow(response,
        List.of("vI2csg55S9C", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "13837", "", "", "", "",
            ""));
    validateRow(response,
        List.of("s46m5MS0hxu", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "2968", "", "", "", "", ""));
    validateRow(response,
        List.of("YtbsuPPo010", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "2066", "", "", "", "", ""));
    validateRow(response,
        List.of("O05mAByOgAv", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "14405", "", "", "", "",
            ""));
    validateRow(response,
        List.of("UOlfIjgN8X6", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "46", "", "", "", "", ""));
    validateRow(response,
        List.of("n6aMJNLdvep", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "13885", "", "", "", "",
            ""));
  }

  @Test
  public void queryBcgDosesCoverageAtFacilityLast12Monthsprogram() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("skipData=false")
        .add("includeNumDen=true")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:Z33QjkcycFQ,pe:LAST_12_MONTHS,ou:O6uvpzGd5pu;fdc6uOvgoji;lc3eMKXaEfw;jUb8gELQApl;PMa2VCrupOd;kJq2mPyFEHo;qhqAxPSTUXp;Vth0fbpFcsO;jmIPBj66vD6;TEQlaapDQoK;bL4ooGhyHRQ;eIQbndfxQMb;at6UHUQatSo")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(9)))
        .body("rows", hasSize(equalTo(156)))
        .body("height", equalTo(156))
        .body("width", equalTo(9))
        .body("headerWidth", equalTo(9));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"jUb8gELQApl\":{\"name\":\"Kailahun\"},\"eIQbndfxQMb\":{\"name\":\"Tonkolili\"},\"Vth0fbpFcsO\":{\"name\":\"Kono\"},\"202109\":{\"name\":\"September 2021\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"bL4ooGhyHRQ\":{\"name\":\"Pujehun\"},\"202107\":{\"name\":\"July 2021\"},\"kJq2mPyFEHo\":{\"name\":\"Kenema\"},\"202108\":{\"name\":\"August 2021\"},\"202105\":{\"name\":\"May 2021\"},\"202106\":{\"name\":\"June 2021\"},\"202103\":{\"name\":\"March 2021\"},\"202104\":{\"name\":\"April 2021\"},\"LAST_12_MONTHS\":{\"name\":\"Last 12 months\"},\"202112\":{\"name\":\"December 2021\"},\"202110\":{\"name\":\"October 2021\"},\"202111\":{\"name\":\"November 2021\"},\"at6UHUQatSo\":{\"name\":\"Western Area\"},\"dx\":{\"name\":\"Data\"},\"Z33QjkcycFQ\":{\"name\":\"BCG doses coverage at facility\"},\"TEQlaapDQoK\":{\"name\":\"Port Loko\"},\"PMa2VCrupOd\":{\"name\":\"Kambia\"},\"ou\":{\"name\":\"Organisation unit\"},\"fdc6uOvgoji\":{\"name\":\"Bombali\"},\"202101\":{\"name\":\"January 2021\"},\"202102\":{\"name\":\"February 2021\"},\"pe\":{\"name\":\"Period\"},\"lc3eMKXaEfw\":{\"name\":\"Bonthe\"},\"qhqAxPSTUXp\":{\"name\":\"Koinadugu\"},\"jmIPBj66vD6\":{\"name\":\"Moyamba\"}},\"dimensions\":{\"dx\":[\"Z33QjkcycFQ\"],\"pe\":[\"202101\",\"202102\",\"202103\",\"202104\",\"202105\",\"202106\",\"202107\",\"202108\",\"202109\",\"202110\",\"202111\",\"202112\"],\"ou\":[\"O6uvpzGd5pu\",\"fdc6uOvgoji\",\"lc3eMKXaEfw\",\"jUb8gELQApl\",\"PMa2VCrupOd\",\"kJq2mPyFEHo\",\"qhqAxPSTUXp\",\"Vth0fbpFcsO\",\"jmIPBj66vD6\",\"TEQlaapDQoK\",\"bL4ooGhyHRQ\",\"eIQbndfxQMb\",\"at6UHUQatSo\"],\"co\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "ou", "Organisation unit", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 3, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 4, "numerator", "Numerator", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 5, "denominator", "Denominator", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 6, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 7, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 8, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "O6uvpzGd5pu", "2.4", "42.0", "20924.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "fdc6uOvgoji", "2.9", "46.0", "18464.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "lc3eMKXaEfw", "2.8", "16.0", "6638.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "jUb8gELQApl", "2.2", "31.0", "16546.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "PMa2VCrupOd", "2.4", "25.0", "12410.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "kJq2mPyFEHo", "2.6", "50.0", "22978.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "qhqAxPSTUXp", "2.2", "23.0", "12089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "Vth0fbpFcsO", "2.9", "38.0", "15629.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "jmIPBj66vD6", "4.1", "42.0", "12168.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "TEQlaapDQoK", "3.0", "54.0", "21293.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "bL4ooGhyHRQ", "3.8", "35.0", "10730.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "eIQbndfxQMb", "2.1", "29.0", "16089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202101", "at6UHUQatSo", "1.2", "35.0", "35823.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "O6uvpzGd5pu", "2.9", "46.0", "20924.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "fdc6uOvgoji", "3.0", "42.0", "18464.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "lc3eMKXaEfw", "4.1", "21.0", "6638.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "jUb8gELQApl", "2.1", "27.0", "16546.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "PMa2VCrupOd", "2.2", "21.0", "12410.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "kJq2mPyFEHo", "2.7", "47.0", "22978.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "qhqAxPSTUXp", "2.5", "23.0", "12089.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "Vth0fbpFcsO", "2.5", "30.0", "15629.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "jmIPBj66vD6", "3.9", "36.0", "12168.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "TEQlaapDQoK", "2.4", "40.0", "21293.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "bL4ooGhyHRQ", "2.2", "18.0", "10730.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "eIQbndfxQMb", "3.2", "39.0", "16089.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202102", "at6UHUQatSo", "1.3", "36.0", "35823.0", "1303.6", "36500",
            "28"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "O6uvpzGd5pu", "2.8", "49.0", "20924.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "fdc6uOvgoji", "2.8", "44.0", "18464.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "lc3eMKXaEfw", "4.6", "26.0", "6638.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "jUb8gELQApl", "2.6", "36.0", "16546.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "PMa2VCrupOd", "2.3", "24.0", "12410.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "kJq2mPyFEHo", "2.8", "54.0", "22978.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "qhqAxPSTUXp", "2.6", "27.0", "12089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "Vth0fbpFcsO", "2.6", "34.0", "15629.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "jmIPBj66vD6", "4.5", "46.0", "12168.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "TEQlaapDQoK", "2.6", "47.0", "21293.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "bL4ooGhyHRQ", "2.7", "25.0", "10730.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "eIQbndfxQMb", "2.9", "39.0", "16089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202103", "at6UHUQatSo", "1.4", "42.0", "35823.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "O6uvpzGd5pu", "2.5", "43.0", "20924.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "fdc6uOvgoji", "2.6", "40.0", "18464.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "lc3eMKXaEfw", "3.1", "17.0", "6638.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "jUb8gELQApl", "2.2", "30.0", "16546.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "PMa2VCrupOd", "2.5", "26.0", "12410.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "kJq2mPyFEHo", "2.4", "46.0", "22978.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "qhqAxPSTUXp", "2.8", "28.0", "12089.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "Vth0fbpFcsO", "3.0", "39.0", "15629.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "jmIPBj66vD6", "3.5", "35.0", "12168.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "TEQlaapDQoK", "2.4", "42.0", "21293.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "bL4ooGhyHRQ", "3.7", "33.0", "10730.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "eIQbndfxQMb", "2.7", "36.0", "16089.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202104", "at6UHUQatSo", "1.4", "42.0", "35823.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "O6uvpzGd5pu", "2.8", "49.0", "20924.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "fdc6uOvgoji", "2.1", "33.0", "18464.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "lc3eMKXaEfw", "5.0", "28.0", "6638.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "jUb8gELQApl", "1.9", "27.0", "16546.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "PMa2VCrupOd", "2.4", "25.0", "12410.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "kJq2mPyFEHo", "2.5", "49.0", "22978.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "qhqAxPSTUXp", "2.6", "27.0", "12089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "Vth0fbpFcsO", "2.3", "30.0", "15629.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "jmIPBj66vD6", "3.9", "40.0", "12168.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "TEQlaapDQoK", "2.5", "46.0", "21293.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "bL4ooGhyHRQ", "2.1", "19.0", "10730.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "eIQbndfxQMb", "1.9", "26.0", "16089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202105", "at6UHUQatSo", "1.0", "31.0", "35823.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "O6uvpzGd5pu", "2.6", "45.0", "20924.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "fdc6uOvgoji", "2.9", "44.0", "18464.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "lc3eMKXaEfw", "3.7", "20.0", "6638.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "jUb8gELQApl", "2.7", "37.0", "16546.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "PMa2VCrupOd", "2.0", "20.0", "12410.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "kJq2mPyFEHo", "2.8", "53.0", "22978.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "qhqAxPSTUXp", "2.9", "29.0", "12089.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "Vth0fbpFcsO", "2.4", "31.0", "15629.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "jmIPBj66vD6", "4.3", "43.0", "12168.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "TEQlaapDQoK", "2.9", "51.0", "21293.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "bL4ooGhyHRQ", "3.9", "34.0", "10730.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "eIQbndfxQMb", "2.0", "27.0", "16089.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202106", "at6UHUQatSo", "1.4", "40.0", "35823.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "O6uvpzGd5pu", "3.8", "67.0", "20924.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "fdc6uOvgoji", "2.4", "38.0", "18464.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "lc3eMKXaEfw", "3.7", "21.0", "6638.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "jUb8gELQApl", "2.2", "31.0", "16546.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "PMa2VCrupOd", "2.4", "25.0", "12410.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "kJq2mPyFEHo", "2.7", "52.0", "22978.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "qhqAxPSTUXp", "2.2", "23.0", "12089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "Vth0fbpFcsO", "2.8", "37.0", "15629.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "jmIPBj66vD6", "4.4", "45.0", "12168.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "TEQlaapDQoK", "2.3", "42.0", "21293.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "bL4ooGhyHRQ", "2.4", "22.0", "10730.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "eIQbndfxQMb", "2.0", "28.0", "16089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202107", "at6UHUQatSo", "1.7", "51.0", "35823.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "O6uvpzGd5pu", "2.7", "48.0", "20924.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "fdc6uOvgoji", "1.9", "30.0", "18464.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "lc3eMKXaEfw", "3.9", "22.0", "6638.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "jUb8gELQApl", "2.6", "36.0", "16546.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "PMa2VCrupOd", "2.7", "28.0", "12410.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "kJq2mPyFEHo", "3.0", "58.0", "22978.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "qhqAxPSTUXp", "2.8", "29.0", "12089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "Vth0fbpFcsO", "2.5", "33.0", "15629.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "jmIPBj66vD6", "3.9", "40.0", "12168.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "TEQlaapDQoK", "2.8", "51.0", "21293.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "bL4ooGhyHRQ", "2.0", "18.0", "10730.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "eIQbndfxQMb", "2.2", "30.0", "16089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202108", "at6UHUQatSo", "1.5", "47.0", "35823.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "O6uvpzGd5pu", "2.9", "50.0", "20924.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "fdc6uOvgoji", "1.6", "25.0", "18464.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "lc3eMKXaEfw", "3.7", "20.0", "6638.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "jUb8gELQApl", "2.2", "30.0", "16546.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "PMa2VCrupOd", "1.9", "19.0", "12410.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "kJq2mPyFEHo", "1.8", "34.0", "22978.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "qhqAxPSTUXp", "2.7", "27.0", "12089.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "Vth0fbpFcsO", "2.8", "36.0", "15629.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "jmIPBj66vD6", "3.9", "39.0", "12168.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "TEQlaapDQoK", "2.5", "44.0", "21293.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "bL4ooGhyHRQ", "2.5", "22.0", "10730.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "eIQbndfxQMb", "2.5", "33.0", "16089.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202109", "at6UHUQatSo", "1.1", "32.0", "35823.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "O6uvpzGd5pu", "3.3", "59.0", "20924.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "fdc6uOvgoji", "2.9", "46.0", "18464.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "lc3eMKXaEfw", "5.1", "29.0", "6638.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "jUb8gELQApl", "2.2", "31.0", "16546.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "PMa2VCrupOd", "2.8", "30.0", "12410.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "kJq2mPyFEHo", "2.6", "50.0", "22978.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "qhqAxPSTUXp", "2.3", "24.0", "12089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "Vth0fbpFcsO", "2.3", "30.0", "15629.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "jmIPBj66vD6", "3.4", "35.0", "12168.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "TEQlaapDQoK", "2.9", "52.0", "21293.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "bL4ooGhyHRQ", "3.4", "31.0", "10730.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "eIQbndfxQMb", "3.0", "41.0", "16089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202110", "at6UHUQatSo", "1.0", "31.0", "35823.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "O6uvpzGd5pu", "3.8", "66.0", "20924.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "fdc6uOvgoji", "2.0", "31.0", "18464.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "lc3eMKXaEfw", "3.3", "18.0", "6638.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "jUb8gELQApl", "1.8", "25.0", "16546.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "PMa2VCrupOd", "2.3", "23.0", "12410.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "kJq2mPyFEHo", "2.4", "46.0", "22978.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "qhqAxPSTUXp", "3.1", "31.0", "12089.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "Vth0fbpFcsO", "2.7", "35.0", "15629.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "jmIPBj66vD6", "4.6", "46.0", "12168.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "TEQlaapDQoK", "2.3", "41.0", "21293.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "bL4ooGhyHRQ", "2.8", "25.0", "10730.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "eIQbndfxQMb", "2.5", "33.0", "16089.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202111", "at6UHUQatSo", "1.6", "46.0", "35823.0", "1216.7", "36500",
            "30"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "O6uvpzGd5pu", "2.3", "40.0", "20924.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "fdc6uOvgoji", "2.7", "43.0", "18464.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "lc3eMKXaEfw", "2.7", "15.0", "6638.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "jUb8gELQApl", "1.9", "27.0", "16546.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "PMa2VCrupOd", "2.5", "26.0", "12410.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "kJq2mPyFEHo", "2.3", "45.0", "22978.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "qhqAxPSTUXp", "1.9", "19.0", "12089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "Vth0fbpFcsO", "2.0", "26.0", "15629.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "jmIPBj66vD6", "4.3", "44.0", "12168.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "TEQlaapDQoK", "2.7", "49.0", "21293.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "bL4ooGhyHRQ", "3.5", "32.0", "10730.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "eIQbndfxQMb", "2.7", "37.0", "16089.0", "1177.4", "36500",
            "31"));
    validateRow(response,
        List.of("Z33QjkcycFQ", "202112", "at6UHUQatSo", "1.2", "36.0", "35823.0", "1177.4", "36500",
            "31"));
  }

  @Test
  public void queryIndicatorsByFacilityTypeOwnershipAndLocation() throws JSONException {
// Given
    QueryParamsBuilder params = new QueryParamsBuilder().add("skipData=false")
        .add("includeNumDen=true")
        .add("displayProperty=NAME")
        .add("skipMeta=false")
        .add(
            "dimension=dx:FnYCr2EAzWS;EdN7qlmI5FS;d9thHOJMROr;eTDtyyaSA7f;FbKK4ofIv5R;n5nS0SmkUpq;YlTWksXEhEO;JoEzWYGdX7s;tUIlpyeeX9N,pe:LAST_4_QUARTERS,J5jldMd8OHv:uYxK4wmcPqA;tDZVQ1WtwpA;EYbopBOJWsW;RXL3lPSK8oG;CXw2yu5fodb,Bpx0589u8y0:w0gFTTmsUcF;PVLOW4bCshG;MAs88nJc9nL;oRVt7g429ZO,Cbuj0VCyDjL:GGghZsfu7qV;f25dqv3Y7Z0")
        .add("relativePeriodDate=2022-01-01");

// When
    ApiResponse response = actions.get(params);

// Then
    response.validate().statusCode(200)
        .body("headers", hasSize(equalTo(11)))
        .body("rows", hasSize(equalTo(288)))
        .body("height", equalTo(288))
        .body("width", equalTo(11))
        .body("headerWidth", equalTo(11));

// Assert metaData.
    String expectedMetaData = "{\"items\":{\"FbKK4ofIv5R\":{\"name\":\"Measles Coverage <1y\"},\"J5jldMd8OHv\":{\"name\":\"Facility Type\"},\"f25dqv3Y7Z0\":{\"name\":\"Urban\"},\"uYxK4wmcPqA\":{\"name\":\"CHP\"},\"YlTWksXEhEO\":{\"name\":\"OPV 1 Coverage <1y\"},\"2021Q4\":{\"name\":\"October - December 2021\"},\"MAs88nJc9nL\":{\"name\":\"Private Clinic\"},\"d9thHOJMROr\":{\"name\":\"Dropout rate Penta 1 - Measles\"},\"JoEzWYGdX7s\":{\"name\":\"OPV 3 Coverage <1y\"},\"tDZVQ1WtwpA\":{\"name\":\"Hospital\"},\"dx\":{\"name\":\"Data\"},\"LAST_4_QUARTERS\":{\"name\":\"Last 4 quarters\"},\"tUIlpyeeX9N\":{\"name\":\"Penta 3 Coverage <1y\"},\"Cbuj0VCyDjL\":{\"name\":\"Location Rural/Urban\"},\"FnYCr2EAzWS\":{\"name\":\"BCG Coverage <1y\"},\"w0gFTTmsUcF\":{\"name\":\"Mission\"},\"RXL3lPSK8oG\":{\"name\":\"Clinic\"},\"GGghZsfu7qV\":{\"name\":\"Rural\"},\"CXw2yu5fodb\":{\"name\":\"CHC\"},\"n5nS0SmkUpq\":{\"name\":\"OPV 0 Coverage <1y\"},\"eTDtyyaSA7f\":{\"name\":\"FIC <1y\"},\"2021Q2\":{\"name\":\"April - June 2021\"},\"2021Q3\":{\"name\":\"July - September 2021\"},\"2021Q1\":{\"name\":\"January - March 2021\"},\"pe\":{\"name\":\"Period\"},\"EYbopBOJWsW\":{\"name\":\"MCHP\"},\"PVLOW4bCshG\":{\"name\":\"NGO\"},\"oRVt7g429ZO\":{\"name\":\"Public facilities\"},\"Bpx0589u8y0\":{\"name\":\"Facility Ownership\"},\"EdN7qlmI5FS\":{\"name\":\"Dropout rate Penta 1 - 3\"}},\"dimensions\":{\"dx\":[\"FnYCr2EAzWS\",\"EdN7qlmI5FS\",\"d9thHOJMROr\",\"eTDtyyaSA7f\",\"FbKK4ofIv5R\",\"n5nS0SmkUpq\",\"YlTWksXEhEO\",\"JoEzWYGdX7s\",\"tUIlpyeeX9N\"],\"pe\":[\"2021Q1\",\"2021Q2\",\"2021Q3\",\"2021Q4\"],\"J5jldMd8OHv\":[\"uYxK4wmcPqA\",\"tDZVQ1WtwpA\",\"EYbopBOJWsW\",\"RXL3lPSK8oG\",\"CXw2yu5fodb\"],\"Cbuj0VCyDjL\":[\"GGghZsfu7qV\",\"f25dqv3Y7Z0\"],\"co\":[],\"Bpx0589u8y0\":[\"w0gFTTmsUcF\",\"PVLOW4bCshG\",\"MAs88nJc9nL\",\"oRVt7g429ZO\"]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

// Assert headers.
    validateHeader(response, 0, "dx", "Data", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 1, "pe", "Period", "TEXT", "java.lang.String", false, true);
    validateHeader(response, 2, "J5jldMd8OHv", "Facility Type", "TEXT", "java.lang.String", false,
        true);
    validateHeader(response, 3, "Bpx0589u8y0", "Facility Ownership", "TEXT", "java.lang.String",
        false, true);
    validateHeader(response, 4, "Cbuj0VCyDjL", "Location Rural/Urban", "TEXT", "java.lang.String",
        false, true);
    validateHeader(response, 5, "value", "Value", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 6, "numerator", "Numerator", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 7, "denominator", "Denominator", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 8, "factor", "Factor", "NUMBER", "java.lang.Double", false, false);
    validateHeader(response, 9, "multiplier", "Multiplier", "NUMBER", "java.lang.Double", false,
        false);
    validateHeader(response, 10, "divisor", "Divisor", "NUMBER", "java.lang.Double", false, false);

// Assert rows.
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "80.5",
            "159.0", "801.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "111.9",
            "2268.0", "8219.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "146.4",
            "466.0", "1291.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "148.9",
            "2502.0", "6815.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "133.9",
            "36.0", "109.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "127.2",
            "16.0", "51.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "105.7",
            "4482.0", "17196.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "123.3",
            "11562.0", "38037.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "55.6",
            "111.0", "801.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "107.0",
            "2192.0", "8219.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "161.2",
            "519.0", "1291.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "184.0",
            "3126.0", "6815.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "161.9",
            "44.0", "109.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "448.3",
            "57.0", "51.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "100.7",
            "4317.0", "17196.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "125.5",
            "11898.0", "38037.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "109.5",
            "221.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "76.7",
            "1589.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "149.4",
            "486.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "170.3",
            "2926.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "273.0",
            "75.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "902.4",
            "116.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "91.8",
            "3981.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "115.4",
            "11061.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "104.5",
            "211.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "83.9",
            "1738.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "91.6",
            "298.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "169.8",
            "2917.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "200.2",
            "55.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "427.9",
            "55.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "65.7",
            "2849.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FnYCr2EAzWS", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "97.6",
            "9356.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "37.6",
            "47.0", "125.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "27.0",
            "389.0", "1439.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "47.1",
            "192.0", "408.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "20.7",
            "378.0", "1829.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "0.0", "0.0",
            "27.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "57.1", "4.0",
            "7.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "25.1",
            "1031.0", "4109.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "20.3",
            "1984.0", "9789.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "10.6",
            "10.0", "94.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "7.8",
            "109.0", "1404.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "49.1",
            "230.0", "468.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "17.2",
            "390.0", "2270.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "-18.6",
            "-8.0", "43.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "37.5", "9.0",
            "24.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "25.4",
            "1007.0", "3971.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "16.5",
            "1695.0", "10250.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "24.2",
            "37.0", "153.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "18.9",
            "266.0", "1410.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "39.9",
            "198.0", "496.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "27.6",
            "595.0", "2155.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "9.4", "9.0",
            "96.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "4.4", "2.0",
            "45.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "12.9",
            "576.0", "4467.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "12.0",
            "1233.0", "10294.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "35.6",
            "64.0", "180.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "17.2",
            "211.0", "1224.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "15.9",
            "51.0", "321.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "22.9",
            "474.0", "2071.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "-51.5",
            "-34.0", "66.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "-72.7",
            "-16.0", "22.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "12.9",
            "519.0", "4012.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("EdN7qlmI5FS", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "10.3",
            "807.0", "7840.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "72.8",
            "91.0", "125.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "33.1",
            "476.0", "1439.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "37.5",
            "153.0", "408.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "33.0",
            "603.0", "1829.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "-7.4",
            "-2.0", "27.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "57.1", "4.0",
            "7.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "30.7",
            "1260.0", "4109.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "19.1",
            "1866.0", "9789.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "73.4",
            "69.0", "94.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "28.9",
            "406.0", "1404.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "11.1",
            "52.0", "468.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "31.8",
            "722.0", "2270.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "23.3",
            "10.0", "43.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "-20.8",
            "-5.0", "24.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "29.1",
            "1156.0", "3971.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "16.8",
            "1721.0", "10250.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "62.7",
            "96.0", "153.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "36.1",
            "509.0", "1410.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "44.0",
            "218.0", "496.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "27.5",
            "592.0", "2155.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "4.2", "4.0",
            "96.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "-22.2",
            "-10.0", "45.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "10.0",
            "446.0", "4467.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "19.9",
            "2053.0", "10294.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "64.4",
            "116.0", "180.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "22.5",
            "275.0", "1224.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "-4.0",
            "-13.0", "321.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "16.5",
            "342.0", "2071.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "-119.7",
            "-79.0", "66.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "-72.7",
            "-16.0", "22.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "15.2",
            "611.0", "4012.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("d9thHOJMROr", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "10.2",
            "800.0", "7840.0", "100.0", "100", "1"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "17.7",
            "35.0", "801.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "37.7",
            "765.0", "8219.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "71.0",
            "226.0", "1291.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "72.1",
            "1212.0", "6815.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "107.9",
            "29.0", "109.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "23.9", "3.0",
            "51.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "62.7",
            "2659.0", "17196.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "75.6",
            "7088.0", "38037.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "11.5",
            "23.0", "801.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "36.8",
            "754.0", "8219.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "107.2",
            "345.0", "1291.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "86.7",
            "1473.0", "6815.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "132.5",
            "36.0", "109.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "78.6",
            "10.0", "51.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "60.7",
            "2603.0", "17196.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "80.4",
            "7623.0", "38037.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "25.8",
            "52.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "32.1",
            "664.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "76.5",
            "249.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "91.6",
            "1573.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "334.9",
            "92.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "427.9",
            "55.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "76.8",
            "3329.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "76.6",
            "7347.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "34.7",
            "70.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "40.4",
            "836.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "94.0",
            "306.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "97.4",
            "1673.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "524.1",
            "144.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "210.0",
            "27.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "73.4",
            "3183.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("eTDtyyaSA7f", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "62.7",
            "6016.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "17.2",
            "34.0", "801.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "47.5",
            "963.0", "8219.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "80.1",
            "255.0", "1291.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "73.0",
            "1226.0", "6815.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "107.9",
            "29.0", "109.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "23.9", "3.0",
            "51.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "67.2",
            "2849.0", "17196.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "84.5",
            "7923.0", "38037.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "12.5",
            "25.0", "801.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "48.7",
            "998.0", "8219.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "129.2",
            "416.0", "1291.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "91.1",
            "1548.0", "6815.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "121.4",
            "33.0", "109.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "228.1",
            "29.0", "51.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "65.7",
            "2815.0", "17196.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "89.9",
            "8529.0", "38037.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "28.2",
            "57.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "43.5",
            "901.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "85.4",
            "278.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "91.0",
            "1563.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "334.9",
            "92.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "427.9",
            "55.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "92.8",
            "4021.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "86.0",
            "8241.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "31.7",
            "64.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "45.8",
            "949.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "102.6",
            "334.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "100.7",
            "1729.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "527.8",
            "145.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "295.6",
            "38.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "78.5",
            "3401.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("FbKK4ofIv5R", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "73.4",
            "7040.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "58.7",
            "116.0", "801.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "86.2",
            "1747.0", "8219.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "141.0",
            "449.0", "1291.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "140.5",
            "2361.0", "6815.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "133.9",
            "36.0", "109.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "127.2",
            "16.0", "51.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "77.3",
            "3279.0", "17196.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "101.0",
            "9471.0", "38037.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "51.1",
            "102.0", "801.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "83.1",
            "1703.0", "8219.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "172.7",
            "556.0", "1291.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "174.0",
            "2957.0", "6815.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "161.9",
            "44.0", "109.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "141.6",
            "18.0", "51.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "77.0",
            "3303.0", "17196.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "101.2",
            "9601.0", "38037.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "109.0",
            "220.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "71.7",
            "1485.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "145.1",
            "472.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "164.6",
            "2828.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "273.0",
            "75.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "855.7",
            "110.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "70.8",
            "3070.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "97.5",
            "9352.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "92.1",
            "186.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "97.6",
            "2022.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "97.4",
            "317.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "165.3",
            "2839.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "200.2",
            "55.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "264.5",
            "34.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "50.4",
            "2183.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("n5nS0SmkUpq", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "83.4",
            "7992.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "63.3",
            "125.0", "801.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "70.2",
            "1422.0", "8219.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "129.1",
            "411.0", "1291.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "108.7",
            "1826.0", "6815.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "100.5",
            "27.0", "109.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "55.7", "7.0",
            "51.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "96.1",
            "4076.0", "17196.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "104.9",
            "9834.0", "38037.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "49.6",
            "99.0", "801.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "65.8",
            "1349.0", "8219.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "135.5",
            "436.0", "1291.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "133.6",
            "2270.0", "6815.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "158.2",
            "43.0", "109.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "369.6",
            "47.0", "51.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "91.2",
            "3912.0", "17196.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "107.6",
            "10207.0", "38037.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "75.8",
            "153.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "67.0",
            "1387.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "152.4",
            "496.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "125.6",
            "2157.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "349.4",
            "96.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "350.1",
            "45.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "103.9",
            "4502.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "105.7",
            "10132.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "89.2",
            "180.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "58.5",
            "1212.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "94.0",
            "306.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "122.2",
            "2099.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "240.2",
            "66.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "194.5",
            "25.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "92.5",
            "4010.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("YlTWksXEhEO", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "80.3",
            "7694.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "40.0",
            "79.0", "801.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "53.9",
            "1093.0", "8219.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "67.5",
            "215.0", "1291.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "86.2",
            "1449.0", "6815.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "100.5",
            "27.0", "109.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "23.9", "3.0",
            "51.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "72.9",
            "3089.0", "17196.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "84.3",
            "7910.0", "38037.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "42.1",
            "84.0", "801.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "60.9",
            "1248.0", "8219.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "74.6",
            "240.0", "1291.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "109.2",
            "1855.0", "6815.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "187.7",
            "51.0", "109.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "133.7",
            "17.0", "51.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "67.7",
            "2903.0", "17196.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "86.8",
            "8228.0", "38037.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "57.5",
            "116.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "53.0",
            "1098.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "91.6",
            "298.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "91.0",
            "1564.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "316.7",
            "87.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "334.5",
            "43.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "88.8",
            "3847.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "95.2",
            "9123.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "58.0",
            "117.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "48.5",
            "1004.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "83.0",
            "270.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "88.3",
            "1517.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "305.7",
            "84.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "116.7",
            "15.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "79.1",
            "3429.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("JoEzWYGdX7s", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "70.0",
            "6715.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "39.5",
            "78.0", "801.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q1", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "51.8",
            "1050.0", "8219.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "67.9",
            "216.0", "1291.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q1", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "86.3",
            "1451.0", "6815.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q1", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "100.5",
            "27.0", "109.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q1", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "23.9", "3.0",
            "51.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "72.6",
            "3078.0", "17196.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q1", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "83.2",
            "7805.0", "38037.0", "405.6", "36500", "90"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "42.1",
            "84.0", "801.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q2", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "63.2",
            "1295.0", "8219.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "73.9",
            "238.0", "1291.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q2", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "110.6",
            "1880.0", "6815.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q2", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "187.7",
            "51.0", "109.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q2", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "118.0",
            "15.0", "51.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "69.1",
            "2964.0", "17196.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q2", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "90.2",
            "8555.0", "38037.0", "401.1", "36500", "91"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "57.5",
            "116.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q3", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "55.2",
            "1144.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "91.6",
            "298.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q3", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "90.8",
            "1560.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q3", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "316.7",
            "87.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q3", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "334.5",
            "43.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "89.8",
            "3891.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q3", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "94.5",
            "9061.0", "38037.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "GGghZsfu7qV", "57.5",
            "116.0", "801.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q4", "tDZVQ1WtwpA", "oRVt7g429ZO", "f25dqv3Y7Z0", "48.9",
            "1013.0", "8219.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "GGghZsfu7qV", "83.0",
            "270.0", "1291.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q4", "RXL3lPSK8oG", "oRVt7g429ZO", "f25dqv3Y7Z0", "93.0",
            "1597.0", "6815.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q4", "CXw2yu5fodb", "PVLOW4bCshG", "GGghZsfu7qV", "364.0",
            "100.0", "109.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q4", "CXw2yu5fodb", "MAs88nJc9nL", "GGghZsfu7qV", "295.6",
            "38.0", "51.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "GGghZsfu7qV", "80.6",
            "3493.0", "17196.0", "396.7", "36500", "92"));
    validateRow(response,
        List.of("tUIlpyeeX9N", "2021Q4", "CXw2yu5fodb", "oRVt7g429ZO", "f25dqv3Y7Z0", "73.4",
            "7033.0", "38037.0", "396.7", "36500", "92"));
  }
}