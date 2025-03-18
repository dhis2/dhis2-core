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

public class TrackedEntityQuery7AutoTest extends AnalyticsApiTest {
  private AnalyticsTrackedEntityActions actions = new AnalyticsTrackedEntityActions();

  @Test
  public void multiProgramStatusMultiDatesMultiDimensionsFilter() throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,TfdH5KvFmMy,B6TnnFMgmCk,created,lastupdated,createdbydisplayname,lastupdatedbydisplayname,CklPZdOd6H1,qDkgAbB5Jlk.programstatus,qDkgAbB5Jlk.enrollmentdate,qDkgAbB5Jlk.ouname,qDkgAbB5Jlk.hYyB7FUS5eR.fazCI2ygYkq,qDkgAbB5Jlk.hYyB7FUS5eR.SzVk2KvkSSd,qDkgAbB5Jlk.hYyB7FUS5eR.GyJHQUWZ9Rl,qDkgAbB5Jlk.C0aLZo75dgJ.lezQpdvvGjY")
            .add("lastUpdated=LAST_5_YEARS")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("enrollmentDate=qDkgAbB5Jlk.THIS_YEAR")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:O6uvpzGd5pu,TfdH5KvFmMy:ILIKE:la,B6TnnFMgmCk,CklPZdOd6H1,qDkgAbB5Jlk.ou:USER_ORGUNIT,qDkgAbB5Jlk.hYyB7FUS5eR.fazCI2ygYkq:IN:PASSIVE,qDkgAbB5Jlk.hYyB7FUS5eR.SzVk2KvkSSd,qDkgAbB5Jlk.hYyB7FUS5eR.GyJHQUWZ9Rl,qDkgAbB5Jlk.C0aLZo75dgJ.lezQpdvvGjY:EQ:5:NE:NV")
            .add("programStatus=qDkgAbB5Jlk.ACTIVE,qDkgAbB5Jlk.COMPLETED")
            .add("relativePeriodDate=2022-07-01");

    // When
    ApiResponse response = actions.query().get("Zy2SEgA61ys", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(15)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(15))
        .body("headerWidth", equalTo(15));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"qDkgAbB5Jlk.hYyB7FUS5eR.GyJHQUWZ9Rl\":{\"uid\":\"GyJHQUWZ9Rl\",\"name\":\"GPS DataElement\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"AZK4rjJCss5\":{\"uid\":\"AZK4rjJCss5\",\"code\":\"FEMALE\",\"name\":\"Female\"},\"GyJHQUWZ9Rl\":{\"uid\":\"GyJHQUWZ9Rl\",\"name\":\"GPS DataElement\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"COORDINATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"D9yTOOLGz0j\":{\"uid\":\"D9yTOOLGz0j\",\"name\":\"Malaria case detection type\",\"options\":[{\"uid\":\"SepVHxunjMN\",\"code\":\"PASSIVE\"}]},\"O6uvpzGd5pu\":{\"uid\":\"O6uvpzGd5pu\",\"code\":\"OU_264\",\"name\":\"Bo\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.hYyB7FUS5eR.fazCI2ygYkq\":{\"uid\":\"fazCI2ygYkq\",\"name\":\"Case detection\",\"description\":\"Determines the method that was used to detect the case\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"SepVHxunjMN\":{\"uid\":\"SepVHxunjMN\",\"code\":\"PASSIVE\",\"name\":\"Passive\"},\"fazCI2ygYkq\":{\"uid\":\"fazCI2ygYkq\",\"name\":\"Case detection\",\"description\":\"Determines the method that was used to detect the case\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"COUNT\",\"totalAggregationType\":\"SUM\"},\"lezQpdvvGjY\":{\"uid\":\"lezQpdvvGjY\",\"name\":\"Residents in household\",\"description\":\"Number of residents in the household as counted during case household investigation\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"C0aLZo75dgJ\":{\"uid\":\"C0aLZo75dgJ\",\"name\":\"Household investigation\",\"description\":\"Nearby household investigations occur when an index case is identified within a specific geographical area.\"},\"V5bPXPrlC6y\":{\"uid\":\"V5bPXPrlC6y\",\"code\":\"SIMPLE\",\"name\":\"Simple\"},\"TfdH5KvFmMy\":{\"uid\":\"TfdH5KvFmMy\",\"name\":\"First Name\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"qDkgAbB5Jlk.C0aLZo75dgJ.lezQpdvvGjY\":{\"uid\":\"lezQpdvvGjY\",\"name\":\"Residents in household\",\"description\":\"Number of residents in the household as counted during case household investigation\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"qDkgAbB5Jlk.ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"B6TnnFMgmCk\":{\"uid\":\"B6TnnFMgmCk\",\"name\":\"Age (years)\",\"description\":\"Age in years\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"AVERAGE\",\"totalAggregationType\":\"SUM\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"hiQ3QFheQ3O\":{\"uid\":\"hiQ3QFheQ3O\",\"name\":\"Sex\",\"options\":[{\"uid\":\"AZK4rjJCss5\",\"code\":\"FEMALE\"}]},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"name\":\"2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-01-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"2021\":{\"uid\":\"2021\",\"code\":\"2021\",\"name\":\"2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"2020\":{\"uid\":\"2020\",\"code\":\"2020\",\"name\":\"2020\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2020-01-01T00:00:00.000\",\"endDate\":\"2020-12-31T00:00:00.000\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"hYyB7FUS5eR\":{\"uid\":\"hYyB7FUS5eR\",\"name\":\"Diagnosis & treatment\",\"description\":\"This stage is used to identify initial diagnosis and treatment. This includes the method of case detection, information about the case include travel history, method of diagnosis, malaria species type and treatment details. \"},\"2019\":{\"uid\":\"2019\",\"code\":\"2019\",\"name\":\"2019\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2019-01-01T00:00:00.000\",\"endDate\":\"2019-12-31T00:00:00.000\"},\"2018\":{\"uid\":\"2018\",\"code\":\"2018\",\"name\":\"2018\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2018-01-01T00:00:00.000\",\"endDate\":\"2018-12-31T00:00:00.000\"},\"CklPZdOd6H1\":{\"uid\":\"CklPZdOd6H1\",\"name\":\"Sex\",\"description\":\"Gender of Person\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"2017\":{\"uid\":\"2017\",\"code\":\"2017\",\"name\":\"2017\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2017-01-01T00:00:00.000\",\"endDate\":\"2017-12-31T00:00:00.000\"},\"qLfiOwxhN3w\":{\"uid\":\"qLfiOwxhN3w\",\"name\":\"Malaria clinical status\",\"options\":[{\"uid\":\"V5bPXPrlC6y\",\"code\":\"SIMPLE\"}]},\"qDkgAbB5Jlk.hYyB7FUS5eR.SzVk2KvkSSd\":{\"uid\":\"SzVk2KvkSSd\",\"code\":\"CS016\",\"name\":\"Clinical status\",\"description\":\"Determines the clinical status of the case's malaria (simple or severs)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"qDkgAbB5Jlk\":{\"uid\":\"qDkgAbB5Jlk\",\"name\":\"Malaria case diagnosis, treatment and investigation\",\"description\":\"All cases in an elimination setting should be registered in this program. Includes relevant case identifiers/details including the ID, Name, Index, Age, Gender, Location,etc..\"},\"qDkgAbB5Jlk.enrollmentdate\":{\"name\":\"Enrollment date\",\"dimensionType\":\"PERIOD\"},\"qDkgAbB5Jlk.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"SzVk2KvkSSd\":{\"uid\":\"SzVk2KvkSSd\",\"code\":\"CS016\",\"name\":\"Clinical status\",\"description\":\"Determines the clinical status of the case's malaria (simple or severs)\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"B6TnnFMgmCk\":[],\"BiTsLcJQ95V\":[],\"GyJHQUWZ9Rl\":[],\"ou\":[\"ImspTQPwCqd\"],\"flGbXLXCrEo\":[],\"Z1rLc1rVHK8\":[],\"CklPZdOd6H1\":[\"AZK4rjJCss5\"],\"h5FuguPFF2j\":[],\"spkM2E9dn2J\":[\"yyeQNBfmO7g\",\"CNnT7FC710W\",\"wuS7cVSEiYA\",\"ALoq1vKJCDr\",\"OvCy05DV6kt\",\"aslBaQPVe9V\",\"rpzJ5jGkUAn\",\"LMzYJEbDEN6\",\"mQ5FOz8JXKs\",\"TRETd1l7n1N\",\"xvYNdt7dLiM\",\"CzdkfAxkAqe\",\"LoTxSO186BO\",\"omWzNDmT2t7\",\"zTtjy8I0bcu\",\"FXk1MDI7CEJ\",\"GGWWOucyQ5L\",\"cc1JEMv0suu\",\"t0fFxYw3Cg4\",\"uM1WgdIueNA\",\"dUEKKaPFcVU\",\"zsS0Xx2iUV6\",\"MdSOAa4C4gW\",\"fEV7BkjJi8V\",\"zITeQ1j7Jmz\",\"RNUEujTD4AN\",\"WIleZf4Cua4\",\"vuiZtzbuwWx\",\"Yf0Gb9nZiQ1\",\"ga22tvzYHEZ\",\"k9qiH4Z3K6m\",\"ZqOOqkOV8Zm\",\"JUY113J4COL\",\"AlKNJVD0Bqv\",\"cGLoDkT864j\",\"OIXRi2caf6J\",\"CIYwznedTto\",\"phN4setkIfq\",\"l8WR0m3GGuB\",\"JqPKssESKSC\",\"zonuQ6g4FFh\",\"pQhtDfYHXlQ\",\"bANs6w1wFgV\",\"rBktLnj3vUY\",\"bQX37dUQTAr\",\"CQR9IijKrgo\",\"m9vCfHK0sLC\",\"Rb9W87URnVe\",\"e4CswJZAFBR\",\"LRjLr9oMe1M\",\"Cy4TaW1hskg\",\"VnmVMbf4mwL\",\"Rx05JdBEHIW\",\"WBvTizbhaXP\",\"iwk6djvOBNV\",\"tLn4hW3TbNZ\",\"DEO4vDvhNEv\",\"C5DXQidiMMc\",\"ccs1kikyZS3\",\"Scdk35fgY12\",\"t0mq3u8SNgz\",\"PMSl473rekw\",\"MpwuGzXBpAk\",\"CedH1TzSPgO\",\"mJexWvdoaXE\",\"RkxuXQTQjxk\",\"CDBeT1lT7n2\",\"KNU7Tm8S245\",\"v16FQ3xwnGN\",\"aOX23O03bBw\",\"W4KroB1nw6P\",\"CWhuePZuC9y\",\"upfjuKBGHq9\",\"wVl5DoJmza2\",\"fQzvkEY9chs\",\"pBNkmU3hDoT\",\"otDBqUSWuzE\",\"THPEMRSnC4G\",\"MKmqLvOYWos\",\"hF9y363enrH\",\"EQULu0IwQNE\",\"I9v1TBhT3OV\",\"CxKFBwhGuJr\",\"N9bYrawJaqR\",\"riIXFPTUnZX\",\"QyJHXS44Xj9\",\"dMRNgoCtogj\",\"gDARdk8cZ3H\",\"AneyNa28ceQ\",\"ELm3SnuBHJZ\",\"Oh3CJhGeaoi\",\"l69MO3y6LuS\",\"T0gujEdp3Z6\",\"I8A7Q4zi1YI\",\"TfyHeFLDOKu\",\"ZyGPejjzvGD\",\"OC0K30ETDLD\",\"FmIGl5AnbxN\",\"ALX1BnV0GrW\",\"S3Dt4ozhM8X\",\"eGQJGkiLamm\",\"vzzPNV6Wu0J\",\"AObPqV4cHPb\",\"kPQes5oG21J\",\"bEj6P1jqHje\",\"UXyOlL9FJ5o\",\"tiwJrxfBoHT\",\"ecANXpkkcPT\",\"VQkdjFxCLNH\",\"QIjTIxTedos\",\"etZCdyFxz4w\",\"H65niFKFuSs\",\"JwslMKjECF2\",\"IqyWsh1pbYf\",\"JWIEjkUmsWH\",\"UJppzPKIQRv\",\"BFMEIXmaqFE\",\"i0Dl3gB8WuY\",\"G9rNnfnVNcB\",\"mCnaSMEODSz\",\"LjRX17TMcTX\",\"SN9NeGsvfmM\",\"CkE4sCvC7zj\",\"THKtWeVTuBk\",\"PFq4nWHt0fM\",\"LfjyJpiu8dL\",\"p0vsNlHuo7N\",\"XQZko5dUFGU\",\"SXD2EhrNaQu\",\"M2XM0PR40oH\",\"IyBPcxO7hfB\",\"cxvpqSjkTjP\",\"kaf9448wuv0\",\"ApCSe2JdIUw\",\"KtR12m8FoT0\",\"Qtp6HW63yqV\",\"nOMNxq2fHGq\",\"C2Ws5NctBqi\",\"BpqJwhPqI9O\",\"V1nDCD6QvPs\",\"HScqQPe1X9u\",\"RmjKEjs388f\",\"jQ3mYwytyZn\",\"sK6lzdZiwIg\",\"nDTKZYmGEvT\",\"EABP62Ce29b\",\"QT9Erxe7UaX\",\"UmW7wmw0AX9\",\"lCww3l79Wem\",\"q36uKrRjq1P\",\"MBVq67Tm1wK\",\"OvzdfV1qrvP\",\"ElBqHdoLnsc\",\"e4HCJJlYOQP\",\"rbvEOlaNkUU\",\"LRsQdDERp1f\",\"ztgG5fQPur9\",\"fg3tUp5fFH1\",\"lBZHFe8qxEL\",\"Y2ze1UBngud\",\"UzC7hScynz0\",\"xsNVICc3jPD\",\"ibFgvQscr1i\",\"jmZqHjwAKxJ\",\"KGoEICL7PmU\",\"JpY27WXUqOI\",\"yxbHuzqF6VS\",\"eHKdzDVgEuj\",\"xuUrPK5b7MP\",\"Publ7A7E6r3\",\"Lyu0GDGZLU3\",\"ovWqm6wr1dP\",\"UvnjescFIbU\",\"hUQDnRc6BKw\",\"nTQRDVcTHr0\",\"oMLQaRXOs1B\",\"mTmjaDWUfDG\",\"NlVuvr5WbKy\",\"ThLyMbT1IvD\",\"zooDgQrnVkm\",\"D9h2axpYFx9\",\"a5ayhZAtUe4\",\"xF48L1VlFrZ\",\"gSPpYw9P7YO\",\"lufRQXPgcJ8\",\"CCzBKhjSPFo\",\"uJH3wMNmMNO\",\"x4Ohep6Q85T\",\"rJTXccXhtTG\",\"hIGURTVsclf\",\"L1FmwJC0u3z\",\"w0S8ngASwmq\",\"CysWbM6JFgj\",\"AMFu6DFAqll\",\"p8Tgra7YJ7h\",\"sPFJiL1BLTy\",\"QtjohlzA8cl\",\"Oprr3by24Zt\",\"rXsByduwzcw\",\"loTOnrxGmCv\",\"Kjon60bpcC4\",\"kdX0FFam8Vz\",\"Jl2tnw6dutF\",\"sQNVDVNINvY\",\"JYfvnvIzM84\",\"OozIOFXlUQB\",\"vMAK8GtCXzE\",\"bhfjgX8aEJQ\",\"SXeG3KaIkU8\",\"qpv7jbqwNTN\",\"ELZXJ7i1DKL\",\"IfwaYvRaIhp\",\"kNow7wcQT6z\",\"HafX2zWjutb\",\"ban72xOWClE\",\"ZcaE9JG9xrr\",\"LuMJaVKadYM\",\"jLIOTZIi0Ou\",\"yxvBpzHn9VN\",\"hB9kKEo5Jav\",\"lGIM5L1ldVZ\",\"txfMqMjfmGK\",\"HOiHQKzyA2h\",\"roEWVrnX17w\",\"vjJWFh1j9U5\",\"zJCV30f9Pix\",\"uNt8kKM8azp\",\"fmGjlRnf0AW\",\"vPk3xrZvimA\",\"xIvocxUNJvn\",\"aGozKfvhGv6\",\"NV1MNzAfPWE\",\"E68TvHpnyp5\",\"qCmj8zWn2RQ\",\"ZOGqtsOOfdP\",\"JwhF38ZDa7Y\",\"RvhGvhkGceD\",\"CzJoxdewhsm\",\"WrnwBUl0Vzt\",\"yqQ0IiG9maE\",\"C3Mf3a5OJa3\",\"CndbUcexjyJ\",\"VEulszgYUL2\",\"sgvVI1jilCg\",\"oZPItrH57Zf\",\"hbiFi85xO2g\",\"lJBsaRbZLZP\",\"WPR1XicphAd\",\"r15gweUYYrk\"],\"pe\":[\"2022\",\"2017\",\"2018\",\"2019\",\"2020\",\"2021\"],\"fazCI2ygYkq\":[\"SepVHxunjMN\",\"fa1IdKtq4VX\",\"xod2M9f6Jgo\"],\"lezQpdvvGjY\":[],\"bJeK4FaRKDS\":[],\"vTKipVM0GsX\":[],\"TfdH5KvFmMy\":[],\"SzVk2KvkSSd\":[\"V5bPXPrlC6y\",\"jv0japmja59\"],\"aW66s2QSosT\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "TfdH5KvFmMy", "First Name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        2,
        "B6TnnFMgmCk",
        "Age (years)",
        "INTEGER_ZERO_OR_POSITIVE",
        "java.lang.Integer",
        false,
        true);
    validateHeader(
        response, 3, "created", "Created", "DATETIME", "java.time.LocalDateTime", false, true);
    validateHeader(
        response,
        4,
        "lastupdated",
        "Last updated",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response, 5, "createdbydisplayname", "Created by", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        6,
        "lastupdatedbydisplayname",
        "Last updated by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(response, 7, "CklPZdOd6H1", "Sex", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        8,
        "qDkgAbB5Jlk.programstatus",
        "Program Status, Malaria case diagnosis, treatment and investigation",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        9,
        "qDkgAbB5Jlk.enrollmentdate",
        "Enrollment date, Malaria case diagnosis, treatment and investigation",
        "DATETIME",
        "java.time.LocalDateTime",
        false,
        true);
    validateHeader(
        response,
        10,
        "qDkgAbB5Jlk.ouname",
        "Organisation Unit Name, Malaria case diagnosis, treatment and investigation",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        11,
        "qDkgAbB5Jlk.hYyB7FUS5eR.fazCI2ygYkq",
        "Case detection, Malaria case diagnosis, treatment and investigation, Diagnosis & treatment",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        12,
        "qDkgAbB5Jlk.hYyB7FUS5eR.SzVk2KvkSSd",
        "Clinical status, Malaria case diagnosis, treatment and investigation, Diagnosis & treatment",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        13,
        "qDkgAbB5Jlk.hYyB7FUS5eR.GyJHQUWZ9Rl",
        "GPS DataElement, Malaria case diagnosis, treatment and investigation, Diagnosis & treatment",
        "COORDINATE",
        "org.opengis.geometry.primitive.Point",
        false,
        true);
    validateHeader(
        response,
        14,
        "qDkgAbB5Jlk.C0aLZo75dgJ.lezQpdvvGjY",
        "Residents in household, Malaria case diagnosis, treatment and investigation, Household investigation",
        "INTEGER_ZERO_OR_POSITIVE",
        "java.lang.Integer",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Ngelehun CHC",
            "Lauren",
            "9",
            "2019-08-21 13:25:51.247",
            "2019-08-21 13:31:41.331",
            ",  ()",
            ",  ()",
            "FEMALE",
            "COMPLETED",
            "2022-03-10 00:00:00.0",
            "Ngelehun CHC",
            "PASSIVE",
            "SIMPLE",
            "",
            "5"));
  }

  @Test
  public void orgUnitGroupsOrgUnitLevelMultiCreatedPeriodWithDimsFilterOffsets()
      throws JSONException {
    // Given
    QueryParamsBuilder params =
        new QueryParamsBuilder()
            .add("includeMetadataDetails=true")
            .add(
                "headers=ouname,iESIqZ0R0R0,NDXw0cluzSw,lw1SqmMlnfh,OvY4VVhSDeJ,ZcBPrXKahq2,WSGAb5XwJ3Y.PFDfvmGpsR3.z8m3llJYuh9,WSGAb5XwJ3Y.edqlbukwRfQ[1].rHgrmXfa57b,WSGAb5XwJ3Y.edqlbukwRfQ[0].rHgrmXfa57b,WSGAb5XwJ3Y.PFDfvmGpsR3.V5PR8Kw8ZnC,WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI,WSGAb5XwJ3Y.PFDfvmGpsR3.MH33VLmOOqm,WSGAb5XwJ3Y.bbKtnxRZKEP.B3bDhNpCcEM,createdbydisplayname,created,WSGAb5XwJ3Y.programstatus,WSGAb5XwJ3Y.ouname")
            .add("created=MONTHS_THIS_YEAR,LAST_5_YEARS,THIS_YEAR,LAST_10_YEARS")
            .add("displayProperty=NAME")
            .add("totalPages=false")
            .add("rowContext=true")
            .add("pageSize=100")
            .add("page=1")
            .add(
                "dimension=ou:USER_ORGUNIT,iESIqZ0R0R0,NDXw0cluzSw,lw1SqmMlnfh,OvY4VVhSDeJ,ZcBPrXKahq2:IEQ:9999,WSGAb5XwJ3Y.PFDfvmGpsR3.z8m3llJYuh9,WSGAb5XwJ3Y.edqlbukwRfQ[1].rHgrmXfa57b:IN:0;NV,WSGAb5XwJ3Y.edqlbukwRfQ[0].rHgrmXfa57b:IN:0;NV,WSGAb5XwJ3Y.PFDfvmGpsR3.V5PR8Kw8ZnC,WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI:IN:notchecked;remaining,WSGAb5XwJ3Y.PFDfvmGpsR3.MH33VLmOOqm,WSGAb5XwJ3Y.bbKtnxRZKEP.B3bDhNpCcEM,WSGAb5XwJ3Y.ou:O6uvpzGd5pu;LEVEL-H1KlN4QIauv;OU_GROUP-CXw2yu5fodb;OU_GROUP-w1Atoz18PCL")
            .add("relativePeriodDate=2022-07-01");

    // When
    ApiResponse response = actions.query().get("nEenWmSyUEp", JSON, JSON, params);

    // Then
    response
        .validate()
        .statusCode(200)
        .body("headers", hasSize(equalTo(17)))
        .body("rows", hasSize(equalTo(1)))
        .body("height", equalTo(1))
        .body("width", equalTo(17))
        .body("headerWidth", equalTo(17));

    // Assert metaData.
    String expectedMetaData =
        "{\"pager\":{\"page\":1,\"pageSize\":100,\"isLastPage\":true},\"items\":{\"lw1SqmMlnfh\":{\"uid\":\"lw1SqmMlnfh\",\"code\":\"Height in cm\",\"name\":\"Height in cm\",\"description\":\"Height in cm\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"mwN7QuEfT8m\":{\"uid\":\"mwN7QuEfT8m\",\"code\":\"OU_820\",\"name\":\"Koribondo CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"CvBAqD6RzLZ\":{\"uid\":\"CvBAqD6RzLZ\",\"code\":\"OU_595\",\"name\":\"Ngalu CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"202208\":{\"uid\":\"202208\",\"code\":\"202208\",\"name\":\"August 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-08-01T00:00:00.000\",\"endDate\":\"2022-08-31T00:00:00.000\"},\"MONTHS_THIS_YEAR\":{\"name\":\"Months this year\"},\"202209\":{\"uid\":\"202209\",\"code\":\"202209\",\"name\":\"September 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-09-01T00:00:00.000\",\"endDate\":\"2022-09-30T00:00:00.000\"},\"202206\":{\"uid\":\"202206\",\"code\":\"202206\",\"name\":\"June 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-06-01T00:00:00.000\",\"endDate\":\"2022-06-30T00:00:00.000\"},\"tSBcgrTDdB8\":{\"uid\":\"tSBcgrTDdB8\",\"code\":\"OU_834\",\"name\":\"Paramedical CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"202207\":{\"uid\":\"202207\",\"code\":\"202207\",\"name\":\"July 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-07-01T00:00:00.000\",\"endDate\":\"2022-07-31T00:00:00.000\"},\"w1Atoz18PCL\":{\"name\":\"District\"},\"202204\":{\"uid\":\"202204\",\"code\":\"202204\",\"name\":\"April 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-04-01T00:00:00.000\",\"endDate\":\"2022-04-30T00:00:00.000\"},\"iESIqZ0R0R0\":{\"uid\":\"iESIqZ0R0R0\",\"name\":\"Date of birth\",\"description\":\"Date of birth\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"DATE\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"202205\":{\"uid\":\"202205\",\"code\":\"202205\",\"name\":\"May 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-05-01T00:00:00.000\",\"endDate\":\"2022-05-31T00:00:00.000\"},\"azRICFoILuh\":{\"uid\":\"azRICFoILuh\",\"code\":\"OU_577\",\"name\":\"Golu MCHP\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"202202\":{\"uid\":\"202202\",\"code\":\"202202\",\"name\":\"February 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-02-01T00:00:00.000\",\"endDate\":\"2022-02-28T00:00:00.000\"},\"jGYT5U5qJP6\":{\"uid\":\"jGYT5U5qJP6\",\"code\":\"OU_653\",\"name\":\"Gbaiima CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"202203\":{\"uid\":\"202203\",\"code\":\"202203\",\"name\":\"March 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-03-01T00:00:00.000\",\"endDate\":\"2022-03-31T00:00:00.000\"},\"202211\":{\"uid\":\"202211\",\"code\":\"202211\",\"name\":\"November 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-11-01T00:00:00.000\",\"endDate\":\"2022-11-30T00:00:00.000\"},\"202212\":{\"uid\":\"202212\",\"code\":\"202212\",\"name\":\"December 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-12-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"PFDfvmGpsR3\":{\"uid\":\"PFDfvmGpsR3\",\"name\":\"Care at birth\",\"description\":\"Intrapartum care / Childbirth / Labour and delivery\"},\"202210\":{\"uid\":\"202210\",\"code\":\"202210\",\"name\":\"October 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-10-01T00:00:00.000\",\"endDate\":\"2022-10-31T00:00:00.000\"},\"LAST_10_YEARS\":{\"name\":\"Last 10 years\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.z8m3llJYuh9\":{\"uid\":\"z8m3llJYuh9\",\"code\":\"EC_TRE_LBR1\",\"name\":\"WHOMCH Continuous supportive presence during labour and birth \",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"TSyzvBiovKh\":{\"uid\":\"TSyzvBiovKh\",\"code\":\"OU_576\",\"name\":\"Gerehun CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"DiszpKrYNg8\":{\"uid\":\"DiszpKrYNg8\",\"code\":\"OU_559\",\"name\":\"Ngelehun CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"E497Rk80ivZ\":{\"uid\":\"E497Rk80ivZ\",\"code\":\"OU_651\",\"name\":\"Bumpe CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"nX05QLraDhO\":{\"uid\":\"nX05QLraDhO\",\"code\":\"OU_585\",\"name\":\"Yamandu CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"Umh4HKqqFp6\":{\"uid\":\"Umh4HKqqFp6\",\"code\":\"OU_578\",\"name\":\"Jembe CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.bbKtnxRZKEP.B3bDhNpCcEM\":{\"uid\":\"B3bDhNpCcEM\",\"code\":\"EP4_LAB_WBC_CD4\",\"name\":\"WHOMCH CD4 count\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"USER_ORGUNIT\":{\"organisationUnits\":[\"ImspTQPwCqd\"]},\"FLjwMPWLrL2\":{\"uid\":\"FLjwMPWLrL2\",\"code\":\"OU_1126\",\"name\":\"Baomahun CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"rm60vuHyQXj\":{\"uid\":\"rm60vuHyQXj\",\"code\":\"OU_1082\",\"name\":\"Nengbema CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"CXw2yu5fodb\":{\"name\":\"CHC\"},\"NDXw0cluzSw\":{\"uid\":\"NDXw0cluzSw\",\"name\":\"Email\",\"description\":\"Email address\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI\":{\"uid\":\"yTDoF5b1OhI\",\"code\":\"EA9_TRE_BRE3\",\"name\":\"WHOMCH ECV conversion remaining\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"PC3Ag91n82e\":{\"uid\":\"PC3Ag91n82e\",\"code\":\"OU_1122\",\"name\":\"Mongere CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.MH33VLmOOqm\":{\"uid\":\"MH33VLmOOqm\",\"code\":\"EC_DEL_IND_DAT\",\"name\":\"WHOMCH Date of induction of labor\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"jhtj3eQa1pM\":{\"uid\":\"jhtj3eQa1pM\",\"code\":\"OU_1100\",\"name\":\"Gondama (Tikonko) CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"pQYCiuosBnZ\":{\"uid\":\"pQYCiuosBnZ\",\"name\":\"ECV conversion remaining\",\"options\":[{\"uid\":\"GxdJJFuMQ5i\",\"code\":\"notchecked\"}]},\"ZcBPrXKahq2\":{\"uid\":\"ZcBPrXKahq2\",\"name\":\"Postal code\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"TEXT\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"WSGAb5XwJ3Y.edqlbukwRfQ.rHgrmXfa57b\":{\"uid\":\"rHgrmXfa57b\",\"code\":\"EA10b_TRE_PPR1\",\"name\":\"WHOMCH Erythromycin given for PPROM\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"QsAwd531Cpd\":{\"uid\":\"QsAwd531Cpd\",\"code\":\"OU_1038\",\"name\":\"Njala CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"H1KlN4QIauv\":{\"name\":\"National\"},\"WSGAb5XwJ3Y.ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"W2KnxOMvmgE\":{\"uid\":\"W2KnxOMvmgE\",\"code\":\"OU_1050\",\"name\":\"Sumbuya CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y.PFDfvmGpsR3.V5PR8Kw8ZnC\":{\"uid\":\"V5PR8Kw8ZnC\",\"code\":\"EC_PRE_OUT\",\"name\":\"WHOMCH Pregnancy outcome\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"MH33VLmOOqm\":{\"uid\":\"MH33VLmOOqm\",\"code\":\"EC_DEL_IND_DAT\",\"name\":\"WHOMCH Date of induction of labor\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"DATE\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"z8m3llJYuh9\":{\"uid\":\"z8m3llJYuh9\",\"code\":\"EC_TRE_LBR1\",\"name\":\"WHOMCH Continuous supportive presence during labour and birth \",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"THIS_YEAR\":{\"name\":\"This year\"},\"2012\":{\"uid\":\"2012\",\"code\":\"2012\",\"name\":\"2012\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2012-01-01T00:00:00.000\",\"endDate\":\"2012-12-31T00:00:00.000\"},\"WSGAb5XwJ3Y.ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"O6uvpzGd5pu\":{\"name\":\"Bo\"},\"KYXbIQBQgP1\":{\"uid\":\"KYXbIQBQgP1\",\"code\":\"OU_1103\",\"name\":\"Tikonko CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"ImspTQPwCqd\":{\"uid\":\"ImspTQPwCqd\",\"code\":\"OU_525\",\"name\":\"Sierra Leone\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"OvY4VVhSDeJ\":{\"uid\":\"OvY4VVhSDeJ\",\"name\":\"Weight in kg\",\"description\":\"Weight in kg\",\"dimensionItemType\":\"PROGRAM_ATTRIBUTE\",\"valueType\":\"NUMBER\",\"aggregationType\":\"NONE\",\"totalAggregationType\":\"NONE\"},\"rHgrmXfa57b\":{\"uid\":\"rHgrmXfa57b\",\"code\":\"EA10b_TRE_PPR1\",\"name\":\"WHOMCH Erythromycin given for PPROM\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"BOOLEAN\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"mzsOsz0NwNY\":{\"uid\":\"mzsOsz0NwNY\",\"code\":\"OU_836\",\"name\":\"New Police Barracks CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"RhJbg8UD75Q\":{\"uid\":\"RhJbg8UD75Q\",\"code\":\"OU_1027\",\"name\":\"Yemoh Town CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"vELbGdEphPd\":{\"uid\":\"vELbGdEphPd\",\"code\":\"OU_614\",\"name\":\"Jimmi CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"Z9ny6QeqsgX\":{\"uid\":\"Z9ny6QeqsgX\",\"code\":\"OU_969\",\"name\":\"Manjama UMC CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"GxdJJFuMQ5i\":{\"uid\":\"GxdJJFuMQ5i\",\"code\":\"notchecked\",\"name\":\"Not checked\"},\"wtdBuXDwZYQ\":{\"uid\":\"wtdBuXDwZYQ\",\"code\":\"OU_1006\",\"name\":\"Praise Foundation CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"wByqtWCCuDJ\":{\"uid\":\"wByqtWCCuDJ\",\"code\":\"OU_1095\",\"name\":\"Damballa CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"PuZOFApTSeo\":{\"uid\":\"PuZOFApTSeo\",\"code\":\"OU_952\",\"name\":\"Sahn CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"bbKtnxRZKEP\":{\"uid\":\"bbKtnxRZKEP\",\"name\":\"Postpartum care visit\",\"description\":\"Provision of care for the mother for some weeks after delivery\"},\"ou\":{\"uid\":\"ou\",\"name\":\"Organisation unit\",\"dimensionType\":\"ORGANISATION_UNIT\"},\"edqlbukwRfQ\":{\"uid\":\"edqlbukwRfQ\",\"name\":\"Second antenatal care visit\",\"description\":\"Antenatal care visit\"},\"k1Y0oNqPlmy\":{\"uid\":\"k1Y0oNqPlmy\",\"code\":\"OU_1161\",\"name\":\"Gboyama CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2022\":{\"uid\":\"2022\",\"code\":\"2022\",\"name\":\"2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-01-01T00:00:00.000\",\"endDate\":\"2022-12-31T00:00:00.000\"},\"2021\":{\"uid\":\"2021\",\"code\":\"2021\",\"name\":\"2021\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2021-01-01T00:00:00.000\",\"endDate\":\"2021-12-31T00:00:00.000\"},\"2020\":{\"uid\":\"2020\",\"code\":\"2020\",\"name\":\"2020\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2020-01-01T00:00:00.000\",\"endDate\":\"2020-12-31T00:00:00.000\"},\"UOJlcpPnBat\":{\"uid\":\"UOJlcpPnBat\",\"code\":\"OU_172174\",\"name\":\"Needy CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"LAST_5_YEARS\":{\"name\":\"Last 5 years\"},\"yTDoF5b1OhI\":{\"uid\":\"yTDoF5b1OhI\",\"code\":\"EA9_TRE_BRE3\",\"name\":\"WHOMCH ECV conversion remaining\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"202201\":{\"uid\":\"202201\",\"code\":\"202201\",\"name\":\"January 2022\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2022-01-01T00:00:00.000\",\"endDate\":\"2022-01-31T00:00:00.000\"},\"2019\":{\"uid\":\"2019\",\"code\":\"2019\",\"name\":\"2019\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2019-01-01T00:00:00.000\",\"endDate\":\"2019-12-31T00:00:00.000\"},\"V5PR8Kw8ZnC\":{\"uid\":\"V5PR8Kw8ZnC\",\"code\":\"EC_PRE_OUT\",\"name\":\"WHOMCH Pregnancy outcome\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"TEXT\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"lOv6IFgr6Fs\":{\"uid\":\"lOv6IFgr6Fs\",\"code\":\"OU_832\",\"name\":\"Manjama Shellmingo CHC\",\"dimensionItemType\":\"ORGANISATION_UNIT\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\"},\"2018\":{\"uid\":\"2018\",\"code\":\"2018\",\"name\":\"2018\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2018-01-01T00:00:00.000\",\"endDate\":\"2018-12-31T00:00:00.000\"},\"2017\":{\"uid\":\"2017\",\"code\":\"2017\",\"name\":\"2017\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2017-01-01T00:00:00.000\",\"endDate\":\"2017-12-31T00:00:00.000\"},\"2016\":{\"uid\":\"2016\",\"code\":\"2016\",\"name\":\"2016\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2016-01-01T00:00:00.000\",\"endDate\":\"2016-12-31T00:00:00.000\"},\"pe\":{\"uid\":\"pe\",\"name\":\"Period\",\"dimensionType\":\"PERIOD\"},\"2015\":{\"uid\":\"2015\",\"code\":\"2015\",\"name\":\"2015\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2015-01-01T00:00:00.000\",\"endDate\":\"2015-12-31T00:00:00.000\"},\"2014\":{\"uid\":\"2014\",\"code\":\"2014\",\"name\":\"2014\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2014-01-01T00:00:00.000\",\"endDate\":\"2014-12-31T00:00:00.000\"},\"2013\":{\"uid\":\"2013\",\"code\":\"2013\",\"name\":\"2013\",\"dimensionItemType\":\"PERIOD\",\"valueType\":\"TEXT\",\"totalAggregationType\":\"SUM\",\"startDate\":\"2013-01-01T00:00:00.000\",\"endDate\":\"2013-12-31T00:00:00.000\"},\"B3bDhNpCcEM\":{\"uid\":\"B3bDhNpCcEM\",\"code\":\"EP4_LAB_WBC_CD4\",\"name\":\"WHOMCH CD4 count\",\"dimensionItemType\":\"DATA_ELEMENT\",\"valueType\":\"NUMBER\",\"aggregationType\":\"SUM\",\"totalAggregationType\":\"SUM\"},\"WSGAb5XwJ3Y\":{\"uid\":\"WSGAb5XwJ3Y\",\"name\":\"WHO RMNCH Tracker\"},\"ouname\":{\"name\":\"Organisation Unit Name\",\"dimensionType\":\"ORGANISATION_UNIT\"}},\"dimensions\":{\"zDhUuAYrxNC\":[],\"lw1SqmMlnfh\":[],\"MH33VLmOOqm\":[],\"z8m3llJYuh9\":[],\"Qo571yj6Zcn\":[],\"DODgdr5Oo2v\":[],\"iESIqZ0R0R0\":[],\"n9nUvfpTsxQ\":[],\"kyIzQsj96BD\":[],\"xs8A6tQJY0s\":[],\"A4xFHyieXys\":[],\"OvY4VVhSDeJ\":[],\"rHgrmXfa57b\":[],\"RG7uGl4w5Jq\":[],\"spFvx9FndA4\":[],\"GUOBQt5K2WI\":[],\"Agywv2JGwuq\":[],\"lZGmxYbs97q\":[],\"VqEFza8wbwA\":[],\"ciq2USN94oJ\":[\"wfkKVdPBzho\",\"Yjte6foKMny\"],\"ou\":[\"FLjwMPWLrL2\",\"O6uvpzGd5pu\",\"E497Rk80ivZ\",\"wByqtWCCuDJ\",\"jGYT5U5qJP6\",\"k1Y0oNqPlmy\",\"TSyzvBiovKh\",\"azRICFoILuh\",\"jhtj3eQa1pM\",\"Umh4HKqqFp6\",\"vELbGdEphPd\",\"mwN7QuEfT8m\",\"lOv6IFgr6Fs\",\"Z9ny6QeqsgX\",\"PC3Ag91n82e\",\"UOJlcpPnBat\",\"rm60vuHyQXj\",\"mzsOsz0NwNY\",\"CvBAqD6RzLZ\",\"DiszpKrYNg8\",\"QsAwd531Cpd\",\"tSBcgrTDdB8\",\"wtdBuXDwZYQ\",\"PuZOFApTSeo\",\"W2KnxOMvmgE\",\"KYXbIQBQgP1\",\"nX05QLraDhO\",\"RhJbg8UD75Q\"],\"w75KJ2mc4zz\":[],\"KmEUg2hHEtx\":[],\"G7vUx908SwP\":[],\"o9odfev2Ty5\":[],\"FO4sWYJ64LQ\":[],\"NDXw0cluzSw\":[],\"ruQQnf6rswq\":[],\"yTDoF5b1OhI\":[\"GxdJJFuMQ5i\",\"j72KsGIAe8h\",\"VqvXhoA5NgX\"],\"cejWyOfXge6\":[\"rBvjJYbMCVx\",\"Mnp3oXrpAbK\"],\"P2cwLGskgxn\":[],\"gHGyrwKPzej\":[],\"V5PR8Kw8ZnC\":[\"PPd9Ch1kJhG\",\"gQuVrCDl0zI\",\"Vh0ZLdC5dWK\"],\"pe\":[\"202201\",\"202202\",\"202203\",\"202204\",\"202205\",\"202206\",\"202207\",\"202208\",\"202209\",\"202210\",\"202211\",\"202212\",\"2017\",\"2018\",\"2019\",\"2020\",\"2021\",\"2022\",\"2012\",\"2013\",\"2014\",\"2015\",\"2016\"],\"VHfUeXpawmE\":[],\"ZcBPrXKahq2\":[],\"B3bDhNpCcEM\":[],\"AuPLng5hLbE\":[],\"H9IlTX2X6SL\":[]}}";
    String actualMetaData = new JSONObject((Map) response.extract("metaData")).toString();
    assertEquals(expectedMetaData, actualMetaData, false);

    // Assert headers.
    validateHeader(
        response, 0, "ouname", "Organisation unit name", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 1, "iESIqZ0R0R0", "Date of birth", "DATE", "java.time.LocalDate", false, true);
    validateHeader(response, 2, "NDXw0cluzSw", "Email", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response, 3, "lw1SqmMlnfh", "Height in cm", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 4, "OvY4VVhSDeJ", "Weight in kg", "NUMBER", "java.lang.Double", false, true);
    validateHeader(
        response, 5, "ZcBPrXKahq2", "Postal code", "TEXT", "java.lang.String", false, true);
    validateHeader(
        response,
        6,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.z8m3llJYuh9",
        "WHOMCH Continuous supportive presence during labour and birth , WHO RMNCH Tracker, Care at birth",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        7,
        "WSGAb5XwJ3Y.edqlbukwRfQ[1].rHgrmXfa57b",
        "WHOMCH Erythromycin given for PPROM, WHO RMNCH Tracker, Second antenatal care visit (1)",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        8,
        "WSGAb5XwJ3Y.edqlbukwRfQ[0].rHgrmXfa57b",
        "WHOMCH Erythromycin given for PPROM, WHO RMNCH Tracker, Second antenatal care visit",
        "BOOLEAN",
        "java.lang.Boolean",
        false,
        true);
    validateHeader(
        response,
        9,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.V5PR8Kw8ZnC",
        "WHOMCH Pregnancy outcome, WHO RMNCH Tracker, Care at birth",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        10,
        "WSGAb5XwJ3Y.edqlbukwRfQ.yTDoF5b1OhI",
        "WHOMCH ECV conversion remaining, WHO RMNCH Tracker, Second antenatal care visit",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        11,
        "WSGAb5XwJ3Y.PFDfvmGpsR3.MH33VLmOOqm",
        "WHOMCH Date of induction of labor, WHO RMNCH Tracker, Care at birth",
        "DATE",
        "java.time.LocalDate",
        false,
        true);
    validateHeader(
        response,
        12,
        "WSGAb5XwJ3Y.bbKtnxRZKEP.B3bDhNpCcEM",
        "WHOMCH CD4 count, WHO RMNCH Tracker, Postpartum care visit",
        "INTEGER_POSITIVE",
        "java.lang.Integer",
        false,
        true);
    validateHeader(
        response,
        13,
        "createdbydisplayname",
        "Created by",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response, 14, "created", "Created", "DATETIME", "java.time.LocalDateTime", false, true);
    validateHeader(
        response,
        15,
        "WSGAb5XwJ3Y.programstatus",
        "Program Status, WHO RMNCH Tracker",
        "TEXT",
        "java.lang.String",
        false,
        true);
    validateHeader(
        response,
        16,
        "WSGAb5XwJ3Y.ouname",
        "Organisation Unit Name, WHO RMNCH Tracker",
        "TEXT",
        "java.lang.String",
        false,
        true);

    // Assert rows.
    validateRow(
        response,
        0,
        List.of(
            "Ngelehun CHC",
            "",
            "",
            "",
            "",
            "9999",
            "",
            "0",
            "0",
            "",
            "notchecked",
            "",
            "",
            ",  ()",
            "2017-01-20 11:54:29.517",
            "ACTIVE",
            "Ngelehun CHC"));
  }
}
