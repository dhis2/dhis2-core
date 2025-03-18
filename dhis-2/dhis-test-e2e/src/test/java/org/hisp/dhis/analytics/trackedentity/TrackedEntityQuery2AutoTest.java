/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.analytics.trackedentity;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.analytics.ValidationHelper.validateHeader;
import static org.hisp.dhis.analytics.ValidationHelper.validateRow;
import static org.hisp.dhis.analytics.ValidationHelper.validateRowContext;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.hisp.dhis.AnalyticsApiTest;
import org.hisp.dhis.test.e2e.actions.analytics.AnalyticsTrackedEntityActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/** Groups e2e tests for "/trackedEntities/query" endpoint. */
public class TrackedEntityQuery2AutoTest extends AnalyticsApiTest {
  private AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void queryTrackedentityquerywithrowcontext5() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add(
                "headers=IpHINAT79UW.A03MvHHogjR[1].bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR[2].bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR[0].bx6fsa0t90x,created")
            .add("lastUpdated=LAST_5_YEARS")
            .add("pageSize=3")
            .add(
                "dimension=IpHINAT79UW.A03MvHHogjR[1].bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR[2].bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR[0].bx6fsa0t90x")
            .add("desc=created")
            .add("relativePeriodDate=2016-01-08");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(4)))
        .body("rows", hasSize(equalTo(3)))
        .body("height", equalTo(3))
        .body("width", equalTo(4))
        .body("headerWidth", equalTo(4));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"isLastPage\":false,\"pageSize\":3,\"page\":1},\"items\":{\"IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x\":{\"name\":\"MCH BCG dose\"},\"bx6fsa0t90x\":{\"name\":\"MCH BCG dose\"},\"2015\":{\"name\":\"2015\"},\"pe\":{\"name\":\"Period\"},\"IpHINAT79UW\":{\"name\":\"Child Programme\"},\"2014\":{\"name\":\"2014\"},\"2013\":{\"name\":\"2013\"},\"A03MvHHogjR\":{\"name\":\"Birth\"},\"2012\":{\"name\":\"2012\"},\"2011\":{\"name\":\"2011\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"bx6fsa0t90x\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"pe\":[\"2011\",\"2012\",\"2013\",\"2014\",\"2015\"],\"VHfUeXpawmE\":[],\"AuPLng5hLbE\":[],\"ZcBPrXKahq2\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response,
        0,
        "IpHINAT79UW.A03MvHHogjR[1].bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth (1)",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        1,
        "IpHINAT79UW.A03MvHHogjR[2].bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth (2)",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        2,
        "IpHINAT79UW.A03MvHHogjR[0].bx6fsa0t90x",
        "MCH BCG dose, Child Programme, Birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response, 3, "created", "Created", "DATETIME", "java.time.LocalDateTime", false, true);

    // Assert row context
    validateRowContext(response, 0, 0, "ND");
    validateRowContext(response, 0, 1, "ND");
    validateRowContext(response, 0, 2, "ND");
    validateRowContext(response, 1, 1, "ND");
    validateRowContext(response, 2, 1, "ND");

    // Assert rows.
    validateRow(response, 0, List.of("", "", "", "2015-10-14 14:18:23.02"));
    validateRow(response, 1, List.of("1", "", "1", "2015-08-07 15:47:29.301"));
    validateRow(response, 2, List.of("0", "", "0", "2015-08-07 15:47:29.3"));
  }

  @Test
  public void queryTrackedEntityQueryHeaderEnrollmentDate() {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("headers=IpHINAT79UW.enrollmentdate,IpHINAT79UW.A03MvHHogjR.occurreddate")
            .add("desc=IpHINAT79UW.enrollmentdate")
            .add("lastUpdated=LAST_5_YEARS")
            .add("relativePeriodDate=2023-06-01");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert headers.
    validateHeader(
        response,
        0,
        "IpHINAT79UW.enrollmentdate",
        "Date of enrollment, Child Programme",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);

    validateHeader(
        response,
        1,
        "IpHINAT79UW.A03MvHHogjR.occurreddate",
        "Report date, Child Programme, Birth",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
  }

  @Test
  public void queryWithDhisDataFormat() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,qDkgAbB5Jlk.wYTF0YCHMWr.AZLp9Shoab9")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,qDkgAbB5Jlk.wYTF0YCHMWr.AZLp9Shoab9:GT:2018-07-01T15.00:LT:2018-07-31");

    // When
    ApiResponse response = actions.query().get("Zy2SEgA61ys", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(2)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(2))
        .body("headerWidth", equalTo(2));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"AZLp9Shoab9\":{\"uid\":\"AZLp9Shoab9\",\"name\":\"Date onset of Symptoms\",\"description\":\"The date of onset of symptoms for the case\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk\":{\"uid\":\"qDkgAbB5Jlk\",\"name\":\"Malaria case diagnosis, treatment and investigation\",\"description\":\"All cases in an elimination setting should be registered in this program. Includes relevant case identifiers/details including the ID, Name, Index, Age, Gender, Location,etc..\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"qDkgAbB5Jlk.wYTF0YCHMWr.AZLp9Shoab9\":{\"uid\":\"AZLp9Shoab9\",\"name\":\"Date onset of Symptoms\",\"description\":\"The date of onset of symptoms for the case\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"wYTF0YCHMWr\":{\"uid\":\"wYTF0YCHMWr\",\"name\":\"Case investigation & classification\",\"description\":\"This includes the investigation of the index case (including the confirmation of symptoms, previous malaria history, LLIN usage details, IRS details), and the summary of the results for the case investigation including the final case classification (both the species type and the case classification). \"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"B6TnnFMgmCk\":[],\"BiTsLcJQ95V\":[],\"ou\":[\"ImspTQPwCqd\"],\"flGbXLXCrEo\":[],\"Z1rLc1rVHK8\":[],\"AZLp9Shoab9\":[],\"CklPZdOd6H1\":[\"AZK4rjJCss5\",\"UrUdMteQzlT\"],\"h5FuguPFF2j\":[],\"spkM2E9dn2J\":[\"yyeQNBfmO7g\",\"CNnT7FC710W\",\"wuS7cVSEiYA\",\"ALoq1vKJCDr\",\"OvCy05DV6kt\",\"aslBaQPVe9V\",\"rpzJ5jGkUAn\",\"LMzYJEbDEN6\",\"mQ5FOz8JXKs\",\"TRETd1l7n1N\",\"xvYNdt7dLiM\",\"CzdkfAxkAqe\",\"LoTxSO186BO\",\"omWzNDmT2t7\",\"zTtjy8I0bcu\",\"FXk1MDI7CEJ\",\"GGWWOucyQ5L\",\"cc1JEMv0suu\",\"t0fFxYw3Cg4\",\"uM1WgdIueNA\",\"dUEKKaPFcVU\",\"zsS0Xx2iUV6\",\"MdSOAa4C4gW\",\"fEV7BkjJi8V\",\"zITeQ1j7Jmz\",\"RNUEujTD4AN\",\"WIleZf4Cua4\",\"vuiZtzbuwWx\",\"Yf0Gb9nZiQ1\",\"ga22tvzYHEZ\",\"k9qiH4Z3K6m\",\"ZqOOqkOV8Zm\",\"JUY113J4COL\",\"AlKNJVD0Bqv\",\"cGLoDkT864j\",\"OIXRi2caf6J\",\"CIYwznedTto\",\"phN4setkIfq\",\"l8WR0m3GGuB\",\"JqPKssESKSC\",\"zonuQ6g4FFh\",\"pQhtDfYHXlQ\",\"bANs6w1wFgV\",\"rBktLnj3vUY\",\"bQX37dUQTAr\",\"CQR9IijKrgo\",\"m9vCfHK0sLC\",\"Rb9W87URnVe\",\"e4CswJZAFBR\",\"LRjLr9oMe1M\",\"Cy4TaW1hskg\",\"VnmVMbf4mwL\",\"Rx05JdBEHIW\",\"WBvTizbhaXP\",\"iwk6djvOBNV\",\"tLn4hW3TbNZ\",\"DEO4vDvhNEv\",\"C5DXQidiMMc\",\"ccs1kikyZS3\",\"Scdk35fgY12\",\"t0mq3u8SNgz\",\"PMSl473rekw\",\"MpwuGzXBpAk\",\"CedH1TzSPgO\",\"mJexWvdoaXE\",\"RkxuXQTQjxk\",\"CDBeT1lT7n2\",\"KNU7Tm8S245\",\"v16FQ3xwnGN\",\"aOX23O03bBw\",\"W4KroB1nw6P\",\"CWhuePZuC9y\",\"upfjuKBGHq9\",\"wVl5DoJmza2\",\"fQzvkEY9chs\",\"pBNkmU3hDoT\",\"otDBqUSWuzE\",\"THPEMRSnC4G\",\"MKmqLvOYWos\",\"hF9y363enrH\",\"EQULu0IwQNE\",\"I9v1TBhT3OV\",\"CxKFBwhGuJr\",\"N9bYrawJaqR\",\"riIXFPTUnZX\",\"QyJHXS44Xj9\",\"dMRNgoCtogj\",\"gDARdk8cZ3H\",\"AneyNa28ceQ\",\"ELm3SnuBHJZ\",\"Oh3CJhGeaoi\",\"l69MO3y6LuS\",\"T0gujEdp3Z6\",\"I8A7Q4zi1YI\",\"TfyHeFLDOKu\",\"ZyGPejjzvGD\",\"OC0K30ETDLD\",\"FmIGl5AnbxN\",\"ALX1BnV0GrW\",\"S3Dt4ozhM8X\",\"eGQJGkiLamm\",\"vzzPNV6Wu0J\",\"AObPqV4cHPb\",\"kPQes5oG21J\",\"bEj6P1jqHje\",\"UXyOlL9FJ5o\",\"tiwJrxfBoHT\",\"ecANXpkkcPT\",\"VQkdjFxCLNH\",\"QIjTIxTedos\",\"etZCdyFxz4w\",\"H65niFKFuSs\",\"JwslMKjECF2\",\"IqyWsh1pbYf\",\"JWIEjkUmsWH\",\"UJppzPKIQRv\",\"BFMEIXmaqFE\",\"i0Dl3gB8WuY\",\"G9rNnfnVNcB\",\"mCnaSMEODSz\",\"LjRX17TMcTX\",\"SN9NeGsvfmM\",\"CkE4sCvC7zj\",\"THKtWeVTuBk\",\"PFq4nWHt0fM\",\"LfjyJpiu8dL\",\"p0vsNlHuo7N\",\"XQZko5dUFGU\",\"SXD2EhrNaQu\",\"M2XM0PR40oH\",\"IyBPcxO7hfB\",\"cxvpqSjkTjP\",\"kaf9448wuv0\",\"ApCSe2JdIUw\",\"KtR12m8FoT0\",\"Qtp6HW63yqV\",\"nOMNxq2fHGq\",\"C2Ws5NctBqi\",\"BpqJwhPqI9O\",\"V1nDCD6QvPs\",\"HScqQPe1X9u\",\"RmjKEjs388f\",\"jQ3mYwytyZn\",\"sK6lzdZiwIg\",\"nDTKZYmGEvT\",\"EABP62Ce29b\",\"QT9Erxe7UaX\",\"UmW7wmw0AX9\",\"lCww3l79Wem\",\"q36uKrRjq1P\",\"MBVq67Tm1wK\",\"OvzdfV1qrvP\",\"ElBqHdoLnsc\",\"e4HCJJlYOQP\",\"rbvEOlaNkUU\",\"LRsQdDERp1f\",\"ztgG5fQPur9\",\"fg3tUp5fFH1\",\"lBZHFe8qxEL\",\"Y2ze1UBngud\",\"UzC7hScynz0\",\"xsNVICc3jPD\",\"ibFgvQscr1i\",\"jmZqHjwAKxJ\",\"KGoEICL7PmU\",\"JpY27WXUqOI\",\"yxbHuzqF6VS\",\"eHKdzDVgEuj\",\"xuUrPK5b7MP\",\"Publ7A7E6r3\",\"Lyu0GDGZLU3\",\"ovWqm6wr1dP\",\"UvnjescFIbU\",\"hUQDnRc6BKw\",\"nTQRDVcTHr0\",\"oMLQaRXOs1B\",\"mTmjaDWUfDG\",\"NlVuvr5WbKy\",\"ThLyMbT1IvD\",\"zooDgQrnVkm\",\"D9h2axpYFx9\",\"a5ayhZAtUe4\",\"xF48L1VlFrZ\",\"gSPpYw9P7YO\",\"lufRQXPgcJ8\",\"CCzBKhjSPFo\",\"uJH3wMNmMNO\",\"x4Ohep6Q85T\",\"rJTXccXhtTG\",\"hIGURTVsclf\",\"L1FmwJC0u3z\",\"w0S8ngASwmq\",\"CysWbM6JFgj\",\"AMFu6DFAqll\",\"p8Tgra7YJ7h\",\"sPFJiL1BLTy\",\"QtjohlzA8cl\",\"Oprr3by24Zt\",\"rXsByduwzcw\",\"loTOnrxGmCv\",\"Kjon60bpcC4\",\"kdX0FFam8Vz\",\"Jl2tnw6dutF\",\"sQNVDVNINvY\",\"JYfvnvIzM84\",\"OozIOFXlUQB\",\"vMAK8GtCXzE\",\"bhfjgX8aEJQ\",\"SXeG3KaIkU8\",\"qpv7jbqwNTN\",\"ELZXJ7i1DKL\",\"IfwaYvRaIhp\",\"kNow7wcQT6z\",\"HafX2zWjutb\",\"ban72xOWClE\",\"ZcaE9JG9xrr\",\"LuMJaVKadYM\",\"jLIOTZIi0Ou\",\"yxvBpzHn9VN\",\"hB9kKEo5Jav\",\"lGIM5L1ldVZ\",\"txfMqMjfmGK\",\"HOiHQKzyA2h\",\"roEWVrnX17w\",\"vjJWFh1j9U5\",\"zJCV30f9Pix\",\"uNt8kKM8azp\",\"fmGjlRnf0AW\",\"vPk3xrZvimA\",\"xIvocxUNJvn\",\"aGozKfvhGv6\",\"NV1MNzAfPWE\",\"E68TvHpnyp5\",\"qCmj8zWn2RQ\",\"ZOGqtsOOfdP\",\"JwhF38ZDa7Y\",\"RvhGvhkGceD\",\"CzJoxdewhsm\",\"WrnwBUl0Vzt\",\"yqQ0IiG9maE\",\"C3Mf3a5OJa3\",\"CndbUcexjyJ\",\"VEulszgYUL2\",\"sgvVI1jilCg\",\"oZPItrH57Zf\",\"hbiFi85xO2g\",\"lJBsaRbZLZP\",\"WPR1XicphAd\",\"r15gweUYYrk\"],\"pe\":[],\"bJeK4FaRKDS\":[],\"vTKipVM0GsX\":[],\"aW66s2QSosT\":[],\"TfdH5KvFmMy\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "qDkgAbB5Jlk.wYTF0YCHMWr.AZLp9Shoab9",
        "Date onset of Symptoms, Malaria case diagnosis, treatment and investigation, Case investigation & classification",
        "DATE",
        "java.time.LocalDate",
        false,
        true);

    // Assert rows.
    validateRow(response, 0, List.of("Ngelehun CHC", "2018-07-25"));
  }
}
