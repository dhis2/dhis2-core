package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.legend.LegendSet;

import java.util.List;

import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "reportingRate", namespace = DxfNamespaces.DXF_2_0 )
public class ReportingRate
    extends BaseDimensionalItemObject implements EmbeddedObject
{
    private DataSet dataSet;

    private ReportingRateMetric metric;

    public ReportingRate()
    {
    }

    public ReportingRate( DataSet dataSet )
    {
        this.dataSet = dataSet;
        this.metric = ReportingRateMetric.REPORTING_RATE;
    }

    public ReportingRate( DataSet dataSet, ReportingRateMetric metric )
    {
        this.dataSet = dataSet;
        this.metric = metric;
    }

    // -------------------------------------------------------------------------
    // DimensionalItemObject
    // -------------------------------------------------------------------------

    @Override
    public String getUid()
    {
        return dataSet.getUid();
    }

    @Override
    public String getName()
    {
        String metricName = metric != null ? metric.displayName() : ReportingRateMetric.REPORTING_RATE.displayName();

        return dataSet.getName() + " " + metricName;
    }

    @Override
    public String getShortName()
    {
        String metricName = metric != null ? metric.displayName() : ReportingRateMetric.REPORTING_RATE.displayName();

        return dataSet.getShortName() + " " + metricName;
    }

    @Override
    public String getDimensionItem()
    {
        return dataSet.getUid() + COMPOSITE_DIM_OBJECT_PLAIN_SEP + metric.name();
    }

    @Override
    public DimensionItemType getDimensionItemType()
    {
        return DimensionItemType.REPORTING_RATE;
    }

    @Override
    public List<LegendSet> getLegendSets()
    {
        return dataSet.getLegendSets();
    }
    
    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( as = BaseNameableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataSet getDataSet()
    {
        return dataSet;
    }

    public void setDataSet( DataSet dataSet )
    {
        this.dataSet = dataSet;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ReportingRateMetric getMetric()
    {
        return metric;
    }

    public void setMetric( ReportingRateMetric metric )
    {
        this.metric = metric;
    }
}
