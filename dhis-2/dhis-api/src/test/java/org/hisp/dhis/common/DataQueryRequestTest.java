/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.SortOrder;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
class DataQueryRequestTest
{

    @Test
    void t1()
    {
        AggregateAnalyticsQueryCriteria criteria = new AggregateAnalyticsQueryCriteria();
        criteria.setDimension( Sets.newHashSet( "dx:abcde;bcdef", "pe:123435" ) );
        criteria.setFilter( Sets.newHashSet( "ou:abcdef" ) );
        criteria.setAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT );
        criteria.setMeasureCriteria( "GE" );
        criteria.setPreAggregationMeasureCriteria( "LT" );
        criteria.setStartDate( new LocalDateTime( 2010, 10, 10, 0, 0 ).toDate() );
        criteria.setEndDate( new LocalDateTime( 2020, 10, 10, 0, 0 ).toDate() );
        criteria.setUserOrgUnitType( UserOrgUnitType.TEI_SEARCH );
        criteria.setOrder( SortOrder.DESC );
        criteria.setTimeField( "z1z2z3" );
        criteria.setOrgUnitField( "q1q2q3" );
        criteria.setSkipMeta( true );
        criteria.setSkipData( true );
        criteria.setSkipRounding( true );
        criteria.setCompletedOnly( true );
        criteria.setHierarchyMeta( true );
        criteria.setHideEmptyColumns( true );
        criteria.setHideEmptyRows( true );
        criteria.setShowHierarchy( true );
        criteria.setIncludeNumDen( true );
        criteria.setIncludeMetadataDetails( true );
        criteria.setDisplayProperty( DisplayProperty.SHORTNAME );
        criteria.setOutputIdScheme( IdScheme.UUID );
        criteria.setInputIdScheme( IdScheme.UUID );
        criteria.setApprovalLevel( "o1o2o3" );
        criteria.setRelativePeriodDate( new LocalDateTime( 2015, 10, 10, 0, 0 ).toDate() );
        criteria.setUserOrgUnit( "ou1ou2ou3" );
        criteria.setColumns( "cols" );
        criteria.setRows( "rows" );
        final DataQueryRequest build = DataQueryRequest.newBuilder().fromCriteria( criteria ).build();
        assertThat( build.getDimension(), is( criteria.getDimension() ) );
        assertThat( build.getFilter(), is( criteria.getFilter() ) );
        assertThat( build.getAggregationType(), is( criteria.getAggregationType() ) );
        assertThat( build.getMeasureCriteria(), is( criteria.getMeasureCriteria() ) );
        assertThat( build.getPreAggregationMeasureCriteria(), is( criteria.getPreAggregationMeasureCriteria() ) );
        assertThat( build.getStartDate(), is( criteria.getStartDate() ) );
        assertThat( build.getEndDate(), is( criteria.getEndDate() ) );
        assertThat( build.getUserOrgUnitType(), is( criteria.getUserOrgUnitType() ) );
        assertThat( build.getOrder(), is( criteria.getOrder() ) );
        assertThat( build.getTimeField(), is( criteria.getTimeField() ) );
        assertThat( build.getOrgUnitField(), is( criteria.getOrgUnitField() ) );
        assertThat( build.isSkipMeta(), is( criteria.isSkipMeta() ) );
        assertThat( build.isSkipData(), is( criteria.isSkipData() ) );
        assertThat( build.isSkipRounding(), is( criteria.isSkipRounding() ) );
        assertThat( build.isCompletedOnly(), is( criteria.isCompletedOnly() ) );
        assertThat( build.isHierarchyMeta(), is( criteria.isHierarchyMeta() ) );
        assertThat( build.isIgnoreLimit(), is( criteria.isIgnoreLimit() ) );
        assertThat( build.isHideEmptyRows(), is( criteria.isHideEmptyRows() ) );
        assertThat( build.isHideEmptyColumns(), is( criteria.isHideEmptyColumns() ) );
        assertThat( build.isShowHierarchy(), is( criteria.isShowHierarchy() ) );
        assertThat( build.isIncludeNumDen(), is( criteria.isIncludeNumDen() ) );
        assertThat( build.isIncludeMetadataDetails(), is( criteria.isIncludeMetadataDetails() ) );
        assertThat( build.getDisplayProperty(), is( criteria.getDisplayProperty() ) );
        assertThat( build.getOutputIdScheme(), is( criteria.getOutputIdScheme() ) );
        assertThat( build.getInputIdScheme(), is( criteria.getInputIdScheme() ) );
        assertThat( build.getApprovalLevel(), is( criteria.getApprovalLevel() ) );
        assertThat( build.getRelativePeriodDate(), is( criteria.getRelativePeriodDate() ) );
        assertThat( build.getUserOrgUnit(), is( criteria.getUserOrgUnit() ) );
    }
}
