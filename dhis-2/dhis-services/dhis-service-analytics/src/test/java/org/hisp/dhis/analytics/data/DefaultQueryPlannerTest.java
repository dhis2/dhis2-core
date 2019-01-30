/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

package org.hisp.dhis.analytics.data;

import com.google.common.collect.Lists;
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.common.*;
import org.hisp.dhis.period.PeriodType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.xml.transform.Source;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.DhisConvenienceTest.createPeriod;


/**
 * @author Luciano Fiandesio
 */
public class DefaultQueryPlannerTest {

    @Mock
    private QueryValidator queryValidator;
    @Mock
    private PartitionManager partitionManager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private QueryPlanner subject;

    @Before
    public void setUp() {

        subject = new DefaultQueryPlanner(queryValidator, partitionManager);
    }

    @Test
    public void t1() throws ParseException {

        BaseDimensionalObject data = new BaseDimensionalObject(DimensionalObject.DATA_X_DIM_ID);
        data.setDimensionType(DimensionType.DATA_X);
        data.setName("Data");
        data.setItems(Lists.newArrayList(
                makeDimItemObject("fbfJHSPpUQD", "ANC 1st visit"),
                makeDimItemObject("h0xKKjijTdI", "Expected pregnancies")
        ));

        BaseDimensionalObject period = new BaseDimensionalObject(DimensionalObject.PERIOD_DIM_ID);
        period.setDimensionType(DimensionType.PERIOD);
        period.setName("Period");
        period.setItems(Lists.newArrayList(
                createPeriod(PeriodType.getPeriodTypeFromIsoString("2017Oct"), asDate("01/10/2017"), asDate("30/09/2018"))
        ));

        DataQueryParams dataQueryParams = DataQueryParams.newBuilder()
                .addDimension(data)
                .addDimension(period)
                .build();

        QueryPlannerParams queryPlannerParams = QueryPlannerParams.newBuilder()
                .withTableName("analytics")
                .withOptimalQueries(4)
                .build();

        DataQueryGroups dataQueryGroups = subject.planQuery(dataQueryParams, queryPlannerParams);

        assertThat(dataQueryGroups.getSequentialQueries(), hasSize(1));
        assertThat(dataQueryGroups.getAllQueries(), hasSize(2));

        // TODO assert that the next financial year was correctly added


    }

    private DimensionalItemObject makeDimItemObject(String uid, String name) {
        BaseDimensionalItemObject dimensionalItemObject = new BaseDimensionalItemObject();
        dimensionalItemObject.setUid(uid);
        dimensionalItemObject.setName(name);


        return dimensionalItemObject;
    }

    private Date asDate(String date) throws ParseException {

        return new SimpleDateFormat("dd/MM/yyyy").parse(date);
    }

}