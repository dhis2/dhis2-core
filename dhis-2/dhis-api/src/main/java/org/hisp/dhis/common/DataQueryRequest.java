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
    private Set<String> dimension;

    private Set<String> filter;

    private AggregationType aggregationType;

    private String measureCriteria;

    private String preAggregationMeasureCriteria;

    private Date startDate;

    private Date endDate;

    private SortOrder order;

    private boolean skipMeta;

    private boolean skipData;

    private boolean skipRounding;

    private boolean completedOnly;

    private boolean hierarchyMeta;

    private boolean ignoreLimit;

    private boolean hideEmptyRows;

    private boolean hideEmptyColumns;

    private boolean showHierarchy;

    private boolean includeNumDen;

    private boolean includeMetadataDetails;

    private boolean duplicatesOnly;

    private boolean allowAllPeriods;

    private DisplayProperty displayProperty;

    private IdScheme outputIdScheme;

    private IdScheme inputIdScheme;

    private String approvalLevel;

    private Date relativePeriodDate;

    private String userOrgUnit;

    private DhisApiVersion apiVersion;

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

    private DataQueryRequest( DataQueryRequestBuilder builder )
    {
        this.dimension = builder.dimension;
        this.filter = builder.filter;
        this.aggregationType = builder.aggregationType;
        this.measureCriteria = builder.measureCriteria;
        this.preAggregationMeasureCriteria = builder.preAggregationMeasureCriteria;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.order = builder.order;
        this.skipMeta = builder.skipMeta;
        this.skipData = builder.skipData;
        this.skipRounding = builder.skipRounding;
        this.completedOnly = builder.completedOnly;
        this.hierarchyMeta = builder.hierarchyMeta;
        this.ignoreLimit = builder.ignoreLimit;
        this.hideEmptyRows = builder.hideEmptyRows;
        this.hideEmptyColumns = builder.hideEmptyColumns;
        this.showHierarchy = builder.showHierarchy;
        this.includeNumDen = builder.includeNumDen;
        this.includeMetadataDetails = builder.includeMetadataDetails;
        this.displayProperty = builder.displayProperty;
        this.outputIdScheme = builder.outputIdScheme;
        this.inputIdScheme = builder.inputIdScheme;
        this.approvalLevel = builder.approvalLevel;
        this.relativePeriodDate = builder.relativePeriodDate;
        this.userOrgUnit = builder.userOrgUnit;
        this.apiVersion = builder.apiVersion;
        this.allowAllPeriods = builder.allowAllPeriods;
        this.duplicatesOnly = builder.duplicatesOnly;
    }
    
    public static DataQueryRequestBuilder newBuilder()
    {
        return new DataQueryRequest.DataQueryRequestBuilder();
    }
    

    public static class DataQueryRequestBuilder
    {
        private Set<String> dimension;

        private Set<String> filter;

        private AggregationType aggregationType;

        private String measureCriteria;

        private String preAggregationMeasureCriteria;

        private Date startDate;

        private Date endDate;

        private SortOrder order;

        private boolean skipMeta;

        private boolean skipData;

        private boolean skipRounding;

        private boolean completedOnly;

        private boolean hierarchyMeta;

        private boolean ignoreLimit;

        private boolean hideEmptyRows;

        private boolean hideEmptyColumns;

        private boolean showHierarchy;

        private boolean includeNumDen;

        private boolean includeMetadataDetails;

        private DisplayProperty displayProperty;

        private IdScheme outputIdScheme;

        private IdScheme inputIdScheme;

        private String approvalLevel;

        private Date relativePeriodDate;

        private String userOrgUnit;

        private DhisApiVersion apiVersion;

        private boolean duplicatesOnly;

        private boolean allowAllPeriods;

        protected DataQueryRequestBuilder()
        {
        }

        public DataQueryRequestBuilder dimension( Set<String> dimension )
        {
            this.dimension = dimension;
            return this;
        }

        public DataQueryRequestBuilder filter( Set<String> filter )
        {
            this.filter = filter;
            return this;
        }

        public DataQueryRequestBuilder aggregationType( AggregationType aggregationType )
        {
            this.aggregationType = aggregationType;
            return this;
        }

        public DataQueryRequestBuilder measureCriteria( String measureCriteria )
        {
            this.measureCriteria = measureCriteria;
            return this;
        }

        public DataQueryRequestBuilder preAggregationMeasureCriteria( String preAggregationMeasureCriteria )
        {
            this.preAggregationMeasureCriteria = preAggregationMeasureCriteria;
            return this;
        }

        public DataQueryRequestBuilder startDate( Date startDate )
        {
            this.startDate = startDate;
            return this;
        }

        public DataQueryRequestBuilder endDate( Date endDate )
        {
            this.endDate = endDate;
            return this;
        }

        public DataQueryRequestBuilder order( SortOrder order )
        {
            this.order = order;
            return this;
        }

        public DataQueryRequestBuilder skipMeta( boolean skipMeta )
        {
            this.skipMeta = skipMeta;
            return this;
        }

        public DataQueryRequestBuilder skipData( boolean skipData )
        {
            this.skipData = skipData;
            return this;
        }

        public DataQueryRequestBuilder skipRounding( boolean skipRounding )
        {
            this.skipRounding = skipRounding;
            return this;
        }

        public DataQueryRequestBuilder completedOnly( boolean completedOnly )
        {
            this.completedOnly = completedOnly;
            return this;
        }

        public DataQueryRequestBuilder hierarchyMeta( boolean hierarchyMeta )
        {
            this.hierarchyMeta = hierarchyMeta;
            return this;
        }

        public DataQueryRequestBuilder ignoreLimit( boolean ignoreLimit )
        {
            this.ignoreLimit = ignoreLimit;
            return this;
        }

        public DataQueryRequestBuilder hideEmptyRows( boolean hideEmptyRows )
        {
            this.hideEmptyRows = hideEmptyRows;
            return this;
        }

        public DataQueryRequestBuilder hideEmptyColumns( boolean hideEmptyColumns )
        {
            this.hideEmptyColumns = hideEmptyColumns;
            return this;
        }

        public DataQueryRequestBuilder showHierarchy( boolean showHierarchy )
        {
            this.showHierarchy = showHierarchy;
            return this;
        }

        public DataQueryRequestBuilder includeNumDen( boolean includeNumDen )
        {
            this.includeNumDen = includeNumDen;
            return this;
        }

        public DataQueryRequestBuilder includeMetadataDetails( boolean includeMetadataDetails )
        {
            this.includeMetadataDetails = includeMetadataDetails;
            return this;
        }

        public DataQueryRequestBuilder displayProperty( DisplayProperty displayProperty )
        {
            this.displayProperty = displayProperty;
            return this;
        }

        public DataQueryRequestBuilder outputIdScheme( IdScheme outputIdScheme )
        {
            this.outputIdScheme = outputIdScheme;
            return this;
        }

        public DataQueryRequestBuilder inputIdScheme( IdScheme inputIdScheme )
        {
            this.inputIdScheme = inputIdScheme;
            return this;
        }

        public DataQueryRequestBuilder approvalLevel( String approvalLevel )
        {
            this.approvalLevel = approvalLevel;
            return this;
        }

        public DataQueryRequestBuilder relativePeriodDate( Date relativePeriodDate )
        {
            this.relativePeriodDate = relativePeriodDate;
            return this;
        }

        public DataQueryRequestBuilder userOrgUnit( String userOrgUnit )
        {
            this.userOrgUnit = userOrgUnit;
            return this;
        }

        public DataQueryRequestBuilder apiVersion( DhisApiVersion apiVersion )
        {
            this.apiVersion = apiVersion;
            return this;
        }

        public DataQueryRequestBuilder duplicatesOnly( boolean duplicatesOnly )
        {
            this.duplicatesOnly = duplicatesOnly;
            return this;
        }

        public DataQueryRequestBuilder allowAllPeriods( boolean allowAllPeriods )
        {
            this.allowAllPeriods = allowAllPeriods;
            return this;
        }

        public DataQueryRequest build()
        {
            return new DataQueryRequest( this );
        }

    }

}
