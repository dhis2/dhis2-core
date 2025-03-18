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
public class TrackedEntityQuery8AutoTest extends AnalyticsApiTest {
  private AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void financialYear2018Sep() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add("headers=ouname,B6TnnFMgmCk,created")
            .add("created=2018Sep")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add("asc=created")
            .add("dimension=ou:USER_ORGUNIT,B6TnnFMgmCk");

    // When
    ApiResponse response = actions.query().get("Zy2SEgA61ys", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(3)))
        .body("rows", hasSize(equalTo(37)))
        .body("height", equalTo(37))
        .body("width", equalTo(3))
        .body("headerWidth", equalTo(3));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"B6TnnFMgmCk\":{\"uid\":\"B6TnnFMgmCk\",\"name\":\"Age (years)\",\"description\":\"Age in years\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"BiTsLcJQ95V\":{\"uid\":\"BiTsLcJQ95V\",\"name\":\"Date of birth (mal)\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"eHvTba5ijAh\":{\"uid\":\"eHvTba5ijAh\",\"name\":\"Case outcome\",\"description\":\"This stage details the final outcome of the case\"},\"flGbXLXCrEo\":{\"uid\":\"flGbXLXCrEo\",\"name\":\"System Case ID\",\"description\":\"System Case ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"hYyB7FUS5eR\":{\"uid\":\"hYyB7FUS5eR\",\"name\":\"Diagnosis & treatment\",\"description\":\"This stage is used to identify initial diagnosis and treatment. This includes the method of case detection, information about the case include travel history, method of diagnosis, malaria species type and treatment details. \"},\"Z1rLc1rVHK8\":{\"uid\":\"Z1rLc1rVHK8\",\"name\":\"Date of birth (mal) is estimated\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"CklPZdOd6H1\":{\"uid\":\"CklPZdOd6H1\",\"name\":\"Sex\",\"description\":\"Gender of Person\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"h5FuguPFF2j\":{\"uid\":\"h5FuguPFF2j\",\"name\":\"Local Case ID\",\"description\":\"Local Case ID\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"spkM2E9dn2J\":{\"uid\":\"spkM2E9dn2J\",\"name\":\"Nationality\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"2018Sep\":{\"name\":\"September 2018 - August 2019\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"qDkgAbB5Jlk\":{\"uid\":\"qDkgAbB5Jlk\",\"name\":\"Malaria case diagnosis, treatment and investigation\",\"description\":\"All cases in an elimination setting should be registered in this program. Includes relevant case identifiers/details including the ID, Name, Index, Age, Gender, Location,etc..\"},\"bJeK4FaRKDS\":{\"uid\":\"bJeK4FaRKDS\",\"name\":\"Resident in catchment area\",\"description\":\"Resident in catchment area\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"wYTF0YCHMWr\":{\"uid\":\"wYTF0YCHMWr\",\"name\":\"Case investigation & classification\",\"description\":\"This includes the investigation of the index case (including the confirmation of symptoms, previous malaria history, LLIN usage details, IRS details), and the summary of the results for the case investigation including the final case classification (both the species type and the case classification). \"},\"vTKipVM0GsX\":{\"uid\":\"vTKipVM0GsX\",\"name\":\"GPS attribute\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"DEFAULT\",\"totalAggregationType\":\"SUM\"},\"C0aLZo75dgJ\":{\"uid\":\"C0aLZo75dgJ\",\"name\":\"Household investigation\",\"description\":\"Nearby household investigations occur when an index case is identified within a specific geographical area.\"},\"aW66s2QSosT\":{\"uid\":\"aW66s2QSosT\",\"name\":\"Last Name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"TfdH5KvFmMy\":{\"uid\":\"TfdH5KvFmMy\",\"name\":\"First Name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"B6TnnFMgmCk\":[],\"BiTsLcJQ95V\":[],\"ou\":[\"ImspTQPwCqd\"],\"flGbXLXCrEo\":[],\"Z1rLc1rVHK8\":[],\"CklPZdOd6H1\":[\"AZK4rjJCss5\",\"UrUdMteQzlT\"],\"h5FuguPFF2j\":[],\"spkM2E9dn2J\":[\"yyeQNBfmO7g\",\"CNnT7FC710W\",\"wuS7cVSEiYA\",\"ALoq1vKJCDr\",\"OvCy05DV6kt\",\"aslBaQPVe9V\",\"rpzJ5jGkUAn\",\"LMzYJEbDEN6\",\"mQ5FOz8JXKs\",\"TRETd1l7n1N\",\"xvYNdt7dLiM\",\"CzdkfAxkAqe\",\"LoTxSO186BO\",\"omWzNDmT2t7\",\"zTtjy8I0bcu\",\"FXk1MDI7CEJ\",\"GGWWOucyQ5L\",\"cc1JEMv0suu\",\"t0fFxYw3Cg4\",\"uM1WgdIueNA\",\"dUEKKaPFcVU\",\"zsS0Xx2iUV6\",\"MdSOAa4C4gW\",\"fEV7BkjJi8V\",\"zITeQ1j7Jmz\",\"RNUEujTD4AN\",\"WIleZf4Cua4\",\"vuiZtzbuwWx\",\"Yf0Gb9nZiQ1\",\"ga22tvzYHEZ\",\"k9qiH4Z3K6m\",\"ZqOOqkOV8Zm\",\"JUY113J4COL\",\"AlKNJVD0Bqv\",\"cGLoDkT864j\",\"OIXRi2caf6J\",\"CIYwznedTto\",\"phN4setkIfq\",\"l8WR0m3GGuB\",\"JqPKssESKSC\",\"zonuQ6g4FFh\",\"pQhtDfYHXlQ\",\"bANs6w1wFgV\",\"rBktLnj3vUY\",\"bQX37dUQTAr\",\"CQR9IijKrgo\",\"m9vCfHK0sLC\",\"Rb9W87URnVe\",\"e4CswJZAFBR\",\"LRjLr9oMe1M\",\"Cy4TaW1hskg\",\"VnmVMbf4mwL\",\"Rx05JdBEHIW\",\"WBvTizbhaXP\",\"iwk6djvOBNV\",\"tLn4hW3TbNZ\",\"DEO4vDvhNEv\",\"C5DXQidiMMc\",\"ccs1kikyZS3\",\"Scdk35fgY12\",\"t0mq3u8SNgz\",\"PMSl473rekw\",\"MpwuGzXBpAk\",\"CedH1TzSPgO\",\"mJexWvdoaXE\",\"RkxuXQTQjxk\",\"CDBeT1lT7n2\",\"KNU7Tm8S245\",\"v16FQ3xwnGN\",\"aOX23O03bBw\",\"W4KroB1nw6P\",\"CWhuePZuC9y\",\"upfjuKBGHq9\",\"wVl5DoJmza2\",\"fQzvkEY9chs\",\"pBNkmU3hDoT\",\"otDBqUSWuzE\",\"THPEMRSnC4G\",\"MKmqLvOYWos\",\"hF9y363enrH\",\"EQULu0IwQNE\",\"I9v1TBhT3OV\",\"CxKFBwhGuJr\",\"N9bYrawJaqR\",\"riIXFPTUnZX\",\"QyJHXS44Xj9\",\"dMRNgoCtogj\",\"gDARdk8cZ3H\",\"AneyNa28ceQ\",\"ELm3SnuBHJZ\",\"Oh3CJhGeaoi\",\"l69MO3y6LuS\",\"T0gujEdp3Z6\",\"I8A7Q4zi1YI\",\"TfyHeFLDOKu\",\"ZyGPejjzvGD\",\"OC0K30ETDLD\",\"FmIGl5AnbxN\",\"ALX1BnV0GrW\",\"S3Dt4ozhM8X\",\"eGQJGkiLamm\",\"vzzPNV6Wu0J\",\"AObPqV4cHPb\",\"kPQes5oG21J\",\"bEj6P1jqHje\",\"UXyOlL9FJ5o\",\"tiwJrxfBoHT\",\"ecANXpkkcPT\",\"VQkdjFxCLNH\",\"QIjTIxTedos\",\"etZCdyFxz4w\",\"H65niFKFuSs\",\"JwslMKjECF2\",\"IqyWsh1pbYf\",\"JWIEjkUmsWH\",\"UJppzPKIQRv\",\"BFMEIXmaqFE\",\"i0Dl3gB8WuY\",\"G9rNnfnVNcB\",\"mCnaSMEODSz\",\"LjRX17TMcTX\",\"SN9NeGsvfmM\",\"CkE4sCvC7zj\",\"THKtWeVTuBk\",\"PFq4nWHt0fM\",\"LfjyJpiu8dL\",\"p0vsNlHuo7N\",\"XQZko5dUFGU\",\"SXD2EhrNaQu\",\"M2XM0PR40oH\",\"IyBPcxO7hfB\",\"cxvpqSjkTjP\",\"kaf9448wuv0\",\"ApCSe2JdIUw\",\"KtR12m8FoT0\",\"Qtp6HW63yqV\",\"nOMNxq2fHGq\",\"C2Ws5NctBqi\",\"BpqJwhPqI9O\",\"V1nDCD6QvPs\",\"HScqQPe1X9u\",\"RmjKEjs388f\",\"jQ3mYwytyZn\",\"sK6lzdZiwIg\",\"nDTKZYmGEvT\",\"EABP62Ce29b\",\"QT9Erxe7UaX\",\"UmW7wmw0AX9\",\"lCww3l79Wem\",\"q36uKrRjq1P\",\"MBVq67Tm1wK\",\"OvzdfV1qrvP\",\"ElBqHdoLnsc\",\"e4HCJJlYOQP\",\"rbvEOlaNkUU\",\"LRsQdDERp1f\",\"ztgG5fQPur9\",\"fg3tUp5fFH1\",\"lBZHFe8qxEL\",\"Y2ze1UBngud\",\"UzC7hScynz0\",\"xsNVICc3jPD\",\"ibFgvQscr1i\",\"jmZqHjwAKxJ\",\"KGoEICL7PmU\",\"JpY27WXUqOI\",\"yxbHuzqF6VS\",\"eHKdzDVgEuj\",\"xuUrPK5b7MP\",\"Publ7A7E6r3\",\"Lyu0GDGZLU3\",\"ovWqm6wr1dP\",\"UvnjescFIbU\",\"hUQDnRc6BKw\",\"nTQRDVcTHr0\",\"oMLQaRXOs1B\",\"mTmjaDWUfDG\",\"NlVuvr5WbKy\",\"ThLyMbT1IvD\",\"zooDgQrnVkm\",\"D9h2axpYFx9\",\"a5ayhZAtUe4\",\"xF48L1VlFrZ\",\"gSPpYw9P7YO\",\"lufRQXPgcJ8\",\"CCzBKhjSPFo\",\"uJH3wMNmMNO\",\"x4Ohep6Q85T\",\"rJTXccXhtTG\",\"hIGURTVsclf\",\"L1FmwJC0u3z\",\"w0S8ngASwmq\",\"CysWbM6JFgj\",\"AMFu6DFAqll\",\"p8Tgra7YJ7h\",\"sPFJiL1BLTy\",\"QtjohlzA8cl\",\"Oprr3by24Zt\",\"rXsByduwzcw\",\"loTOnrxGmCv\",\"Kjon60bpcC4\",\"kdX0FFam8Vz\",\"Jl2tnw6dutF\",\"sQNVDVNINvY\",\"JYfvnvIzM84\",\"OozIOFXlUQB\",\"vMAK8GtCXzE\",\"bhfjgX8aEJQ\",\"SXeG3KaIkU8\",\"qpv7jbqwNTN\",\"ELZXJ7i1DKL\",\"IfwaYvRaIhp\",\"kNow7wcQT6z\",\"HafX2zWjutb\",\"ban72xOWClE\",\"ZcaE9JG9xrr\",\"LuMJaVKadYM\",\"jLIOTZIi0Ou\",\"yxvBpzHn9VN\",\"hB9kKEo5Jav\",\"lGIM5L1ldVZ\",\"txfMqMjfmGK\",\"HOiHQKzyA2h\",\"roEWVrnX17w\",\"vjJWFh1j9U5\",\"zJCV30f9Pix\",\"uNt8kKM8azp\",\"fmGjlRnf0AW\",\"vPk3xrZvimA\",\"xIvocxUNJvn\",\"aGozKfvhGv6\",\"NV1MNzAfPWE\",\"E68TvHpnyp5\",\"qCmj8zWn2RQ\",\"ZOGqtsOOfdP\",\"JwhF38ZDa7Y\",\"RvhGvhkGceD\",\"CzJoxdewhsm\",\"WrnwBUl0Vzt\",\"yqQ0IiG9maE\",\"C3Mf3a5OJa3\",\"CndbUcexjyJ\",\"VEulszgYUL2\",\"sgvVI1jilCg\",\"oZPItrH57Zf\",\"hbiFi85xO2g\",\"lJBsaRbZLZP\",\"WPR1XicphAd\",\"r15gweUYYrk\"],\"pe\":[\"2018Sep\"],\"bJeK4FaRKDS\":[],\"vTKipVM0GsX\":[],\"aW66s2QSosT\":[],\"TfdH5KvFmMy\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        1,
        "B6TnnFMgmCk",
        "Age (years)",
        "INTEGER_ZERO_OR_POSITIVE",
        "java.lang.Integer",
        false,
        true);
    validateHeader(
        response, 2, "created", "Created", "DATETIME", "java.time.LocalDateTime", false, true);

    // Assert rows.
    validateRow(response, 0, List.of("Ngelehun CHC", "33", "2019-08-21 13:23:19.551"));
    validateRow(response, 1, List.of("Ngelehun CHC", "64", "2019-08-21 13:23:24.357"));
    validateRow(response, 2, List.of("Njandama MCHP", "36", "2019-08-21 13:23:30.144"));
    validateRow(response, 3, List.of("Ngelehun CHC", "29", "2019-08-21 13:23:34.045"));
    validateRow(response, 4, List.of("Ngelehun CHC", "15", "2019-08-21 13:23:37.63"));
    validateRow(response, 5, List.of("Ngelehun CHC", "33", "2019-08-21 13:23:41.186"));
    validateRow(response, 6, List.of("Ngelehun CHC", "29", "2019-08-21 13:23:45.456"));
    validateRow(response, 7, List.of("Ngelehun CHC", "19", "2019-08-21 13:23:48.994"));
    validateRow(response, 8, List.of("Ngelehun CHC", "0", "2019-08-21 13:23:53.055"));
    validateRow(response, 9, List.of("Njandama MCHP", "0", "2019-08-21 13:23:56.994"));
    validateRow(response, 10, List.of("Ngelehun CHC", "32", "2019-08-21 13:24:00.882"));
    validateRow(response, 11, List.of("Ngelehun CHC", "", "2019-08-21 13:24:04.74"));
    validateRow(response, 12, List.of("Ngelehun CHC", "17", "2019-08-21 13:24:08.457"));
    validateRow(response, 13, List.of("Ngelehun CHC", "30", "2019-08-21 13:24:13.102"));
    validateRow(response, 14, List.of("Njandama MCHP", "11", "2019-08-21 13:24:17.391"));
    validateRow(response, 15, List.of("Ngelehun CHC", "", "2019-08-21 13:24:21.658"));
    validateRow(response, 16, List.of("Ngelehun CHC", "46", "2019-08-21 13:24:26.104"));
    validateRow(response, 17, List.of("Ngelehun CHC", "0", "2019-08-21 13:24:30.66"));
    validateRow(response, 18, List.of("Ngelehun CHC", "0", "2019-08-21 13:24:34.951"));
    validateRow(response, 19, List.of("Ngelehun CHC", "0", "2019-08-21 13:24:38.952"));
    validateRow(response, 20, List.of("Ngelehun CHC", "", "2019-08-21 13:24:43.358"));
    validateRow(response, 21, List.of("Ngelehun CHC", "0", "2019-08-21 13:24:47.119"));
    validateRow(response, 22, List.of("Njandama MCHP", "0", "2019-08-21 13:24:52.073"));
    validateRow(response, 23, List.of("Njandama MCHP", "26", "2019-08-21 13:24:56.022"));
    validateRow(response, 24, List.of("Ngelehun CHC", "21", "2019-08-21 13:24:59.811"));
    validateRow(response, 25, List.of("Ngelehun CHC", "27", "2019-08-21 13:25:03.887"));
    validateRow(response, 26, List.of("Ngelehun CHC", "25", "2019-08-21 13:25:07.634"));
    validateRow(response, 27, List.of("Ngelehun CHC", "27", "2019-08-21 13:25:12.115"));
    validateRow(response, 28, List.of("Ngelehun CHC", "30", "2019-08-21 13:25:16.651"));
    validateRow(response, 29, List.of("Ngelehun CHC", "24", "2019-08-21 13:25:20.729"));
    validateRow(response, 30, List.of("Ngelehun CHC", "23", "2019-08-21 13:25:25.258"));
    validateRow(response, 31, List.of("Ngelehun CHC", "43", "2019-08-21 13:25:29.756"));
    validateRow(response, 32, List.of("Ngelehun CHC", "6", "2019-08-21 13:25:34.059"));
    validateRow(response, 33, List.of("Ngelehun CHC", "30", "2019-08-21 13:25:38.022"));
    validateRow(response, 34, List.of("Ngelehun CHC", "32", "2019-08-21 13:25:42.703"));
    validateRow(response, 35, List.of("Ngelehun CHC", "9", "2019-08-21 13:25:47.06"));
    validateRow(response, 36, List.of("Ngelehun CHC", "9", "2019-08-21 13:25:51.247"));
  }
}
