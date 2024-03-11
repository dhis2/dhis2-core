package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2018; University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms; with or without
 * modification; are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice; this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice;
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES; INCLUDING; BUT NOT LIMITED TO; THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT; INDIRECT; INCIDENTAL; SPECIAL; EXEMPLARY; OR CONSEQUENTIAL DAMAGES
 * (INCLUDING; BUT NOT LIMITED TO; PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE; DATA; OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY; WHETHER IN CONTRACT; STRICT LIABILITY; OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE; EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.Date;
import java.util.Set;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.SortOrder;

public class DataQueryRequest
{
    protected Set<String> dimension;

    protected Set<String> filter;

    protected AggregationType aggregationType;

    protected String measureCriteria;

    protected String preAggregationMeasureCriteria;

    protected Date startDate;

    protected Date endDate;

    protected SortOrder order;
    
    protected String timeField;

    protected boolean skipMeta;

    protected boolean skipData;

    protected boolean skipRounding;

    protected boolean completedOnly;

    protected boolean hierarchyMeta;

    protected boolean ignoreLimit;

    protected boolean hideEmptyRows;

    protected boolean hideEmptyColumns;

    protected boolean showHierarchy;

    protected boolean includeNumDen;

    protected boolean includeMetadataDetails;

    protected boolean duplicatesOnly;

    protected boolean allowAllPeriods;

    protected DisplayProperty displayProperty;

    protected IdScheme outputIdScheme;

    protected IdScheme inputIdScheme;

    protected String approvalLevel;

    protected Date relativePeriodDate;

    protected String userOrgUnit;

    protected DhisApiVersion apiVersion;

    public Set<String> getDimension()
    {
        return dimension;
    }

    public Set<String> getFilter()
    {
        return filter;
    }

    public AggregationType getAggregationType()
    {
        return aggregationType;
    }

    public String getMeasureCriteria()
    {
        return measureCriteria;
    }

    public String getPreAggregationMeasureCriteria()
    {
        return preAggregationMeasureCriteria;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public SortOrder getOrder()
    {
        return order;
    }

    public String getTimeField()
    {
        return timeField;
    }
    
    public boolean isSkipMeta()
    {
        return skipMeta;
    }

    public boolean isSkipData()
    {
        return skipData;
    }

    public boolean isSkipRounding()
    {
        return skipRounding;
    }

    public boolean isCompletedOnly()
    {
        return completedOnly;
    }

    public boolean isHierarchyMeta()
    {
        return hierarchyMeta;
    }

    public boolean isIgnoreLimit()
    {
        return ignoreLimit;
    }

    public boolean isHideEmptyRows()
    {
        return hideEmptyRows;
    }

    public boolean isHideEmptyColumns()
    {
        return hideEmptyColumns;
    }

    public boolean isShowHierarchy()
    {
        return showHierarchy;
    }

    public boolean isIncludeNumDen()
    {
        return includeNumDen;
    }

    public boolean isIncludeMetadataDetails()
    {
        return includeMetadataDetails;
    }

    public DisplayProperty getDisplayProperty()
    {
        return displayProperty;
    }

    public IdScheme getOutputIdScheme()
    {
        return outputIdScheme;
    }

    public IdScheme getInputIdScheme()
    {
        return inputIdScheme;
    }

    public String getApprovalLevel()
    {
        return approvalLevel;
    }

    public Date getRelativePeriodDate()
    {
        return relativePeriodDate;
    }

    public String getUserOrgUnit()
    {
        return userOrgUnit;
    }

    public DhisApiVersion getApiVersion()
    {
        return apiVersion;
    }

    public boolean isDuplicatesOnly()
    {
        return duplicatesOnly;
    }

    public boolean isAllowAllPeriods()
    {
        return allowAllPeriods;
    }

    public static DataQueryRequestBuilder newBuilder()
    {
        return new DataQueryRequest.DataQueryRequestBuilder();
    }

    protected DataQueryRequest()
    {
    }

    public static class DataQueryRequestBuilder
    {
        private DataQueryRequest request;

        protected DataQueryRequestBuilder()
        {
            this.request = new DataQueryRequest();
        }

        public DataQueryRequestBuilder dimension( Set<String> dimension )
        {
            this.request.dimension = dimension;
            return this;
        }

        public DataQueryRequestBuilder filter( Set<String> filter )
        {
            this.request.filter = filter;
            return this;
        }

        public DataQueryRequestBuilder aggregationType( AggregationType aggregationType )
        {
            this.request.aggregationType = aggregationType;
            return this;
        }

        public DataQueryRequestBuilder measureCriteria( String measureCriteria )
        {
            this.request.measureCriteria = measureCriteria;
            return this;
        }

        public DataQueryRequestBuilder preAggregationMeasureCriteria( String preAggregationMeasureCriteria )
        {
            this.request.preAggregationMeasureCriteria = preAggregationMeasureCriteria;
            return this;
        }

        public DataQueryRequestBuilder startDate( Date startDate )
        {
            this.request.startDate = startDate;
            return this;
        }

        public DataQueryRequestBuilder endDate( Date endDate )
        {
            this.request.endDate = endDate;
            return this;
        }

        public DataQueryRequestBuilder order( SortOrder order )
        {
            this.request.order = order;
            return this;
        }
        
        public DataQueryRequestBuilder timeField( String timeField )
        {
            this.request.timeField = timeField;
            return this;
        }

        public DataQueryRequestBuilder skipMeta( boolean skipMeta )
        {
            this.request.skipMeta = skipMeta;
            return this;
        }

        public DataQueryRequestBuilder skipData( boolean skipData )
        {
            this.request.skipData = skipData;
            return this;
        }

        public DataQueryRequestBuilder skipRounding( boolean skipRounding )
        {
            this.request.skipRounding = skipRounding;
            return this;
        }

        public DataQueryRequestBuilder completedOnly( boolean completedOnly )
        {
            this.request.completedOnly = completedOnly;
            return this;
        }

        public DataQueryRequestBuilder hierarchyMeta( boolean hierarchyMeta )
        {
            this.request.hierarchyMeta = hierarchyMeta;
            return this;
        }

        public DataQueryRequestBuilder ignoreLimit( boolean ignoreLimit )
        {
            this.request.ignoreLimit = ignoreLimit;
            return this;
        }

        public DataQueryRequestBuilder hideEmptyRows( boolean hideEmptyRows )
        {
            this.request.hideEmptyRows = hideEmptyRows;
            return this;
        }

        public DataQueryRequestBuilder hideEmptyColumns( boolean hideEmptyColumns )
        {
            this.request.hideEmptyColumns = hideEmptyColumns;
            return this;
        }

        public DataQueryRequestBuilder showHierarchy( boolean showHierarchy )
        {
            this.request.showHierarchy = showHierarchy;
            return this;
        }

        public DataQueryRequestBuilder includeNumDen( boolean includeNumDen )
        {
            this.request.includeNumDen = includeNumDen;
            return this;
        }

        public DataQueryRequestBuilder includeMetadataDetails( boolean includeMetadataDetails )
        {
            this.request.includeMetadataDetails = includeMetadataDetails;
            return this;
        }

        public DataQueryRequestBuilder displayProperty( DisplayProperty displayProperty )
        {
            this.request.displayProperty = displayProperty;
            return this;
        }

        public DataQueryRequestBuilder outputIdScheme( IdScheme outputIdScheme )
        {
            this.request.outputIdScheme = outputIdScheme;
            return this;
        }

        public DataQueryRequestBuilder inputIdScheme( IdScheme inputIdScheme )
        {
            this.request.inputIdScheme = inputIdScheme;
            return this;
        }

        public DataQueryRequestBuilder approvalLevel( String approvalLevel )
        {
            this.request.approvalLevel = approvalLevel;
            return this;
        }

        public DataQueryRequestBuilder relativePeriodDate( Date relativePeriodDate )
        {
            this.request.relativePeriodDate = relativePeriodDate;
            return this;
        }

        public DataQueryRequestBuilder userOrgUnit( String userOrgUnit )
        {
            this.request.userOrgUnit = userOrgUnit;
            return this;
        }

        public DataQueryRequestBuilder apiVersion( DhisApiVersion apiVersion )
        {
            this.request.apiVersion = apiVersion;
            return this;
        }

        public DataQueryRequestBuilder duplicatesOnly( boolean duplicatesOnly )
        {
            this.request.duplicatesOnly = duplicatesOnly;
            return this;
        }

        public DataQueryRequestBuilder allowAllPeriods( boolean allowAllPeriods )
        {
            this.request.allowAllPeriods = allowAllPeriods;
            return this;
        }

        public DataQueryRequest build()
        {
            return request;
        }

    }

}
