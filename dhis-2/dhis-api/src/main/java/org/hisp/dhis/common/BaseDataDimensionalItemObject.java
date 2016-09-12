package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * @author Lars Helge Overland
 */
public class BaseDataDimensionalItemObject
    extends BaseDimensionalItemObject implements DataDimensionalItemObject
{
    /**
     * The category option combo identifier used for aggregated data exports through
     * analytics, can be null.
     */
    protected String aggregateExportCategoryOptionCombo;

    /**
     * The attribute option combo identifier used for aggregated data exports through
     * analytics, can be null.
     */
    protected String aggregateExportAttributeOptionCombo;

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @Override
    public boolean hasAggregateExportCategoryOptionCombo()
    {
        return aggregateExportCategoryOptionCombo != null;
    }

    @Override
    public boolean hasAggregateExportAttributeOptionCombo()
    {
        return aggregateExportAttributeOptionCombo != null;
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getAggregateExportCategoryOptionCombo()
    {
        return aggregateExportCategoryOptionCombo;
    }

    public void setAggregateExportCategoryOptionCombo( String aggregateExportCategoryOptionCombo )
    {
        this.aggregateExportCategoryOptionCombo = aggregateExportCategoryOptionCombo;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getAggregateExportAttributeOptionCombo()
    {
        return aggregateExportAttributeOptionCombo;
    }

    public void setAggregateExportAttributeOptionCombo( String aggregateExportAttributeOptionCombo )
    {
        this.aggregateExportAttributeOptionCombo = aggregateExportAttributeOptionCombo;
    }

    // -------------------------------------------------------------------------
    // Merge
    // -------------------------------------------------------------------------

    @Override
    public void mergeWith( IdentifiableObject other, MergeMode mergeMode )
    {
        super.mergeWith( other, mergeMode );

        if ( other.getClass().isInstance( this ) )
        {
            DataDimensionalItemObject object = (DataDimensionalItemObject) other;

            if ( mergeMode.isReplace() )
            {
                aggregateExportCategoryOptionCombo = object.getAggregateExportCategoryOptionCombo();
                aggregateExportAttributeOptionCombo = object.getAggregateExportAttributeOptionCombo();
            }
            else if ( mergeMode.isMerge() )
            {
                aggregateExportCategoryOptionCombo = object.getAggregateExportCategoryOptionCombo() == null ? 
                    aggregateExportCategoryOptionCombo : object.getAggregateExportCategoryOptionCombo();
                aggregateExportAttributeOptionCombo = object.getAggregateExportAttributeOptionCombo() == null ?
                    aggregateExportAttributeOptionCombo : object.getAggregateExportAttributeOptionCombo();
            }
        }
    }
}
