package org.hisp.dhis.analytics;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.Objects;
import org.hisp.dhis.util.ObjectUtils;
import com.google.common.base.MoreObjects;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsAggregationType
{
    public static final AnalyticsAggregationType SUM = new AnalyticsAggregationType( AggregationType.SUM, AggregationType.SUM );
    public static final AnalyticsAggregationType AVERAGE = new AnalyticsAggregationType( AggregationType.AVERAGE, AggregationType.AVERAGE );
    public static final AnalyticsAggregationType COUNT = new AnalyticsAggregationType( AggregationType.COUNT, AggregationType.COUNT );
    public static final AnalyticsAggregationType LAST = new AnalyticsAggregationType( AggregationType.LAST, AggregationType.LAST );
    
    /**
     * General aggregation type.
     */
    private AggregationType aggregationType;
    
    /**
     * Aggregation type for the period dimension.
     */
    private AggregationType periodAggregationType;
    
    /**
     * Analytics data type.
     */
    private DataType dataType;
    
    /**
     * Indicates whether to perform data disaggregation.
     */
    private boolean disaggregation;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public AnalyticsAggregationType( AggregationType aggregationType )
    {
        this.aggregationType = aggregationType;
    }
    
    public AnalyticsAggregationType( AggregationType aggregationType, AggregationType periodAggregationType )
    {
        this( aggregationType );
        this.periodAggregationType = periodAggregationType;
    }
    
    public AnalyticsAggregationType( AggregationType aggregationType, AggregationType periodAggregationType, DataType dataType, boolean disaggregation )
    {
        this( aggregationType, periodAggregationType );
        this.dataType = dataType;
        this.disaggregation = disaggregation;
    }

    // -------------------------------------------------------------------------
    // Logic methods
    // -------------------------------------------------------------------------

    public static AnalyticsAggregationType fromAggregationType( AggregationType aggregationType )
    {
        if ( AggregationType.AVERAGE_SUM_ORG_UNIT == aggregationType )
        {
            return new AnalyticsAggregationType( AggregationType.SUM, AggregationType.AVERAGE );
        }
        else if ( AggregationType.LAST == aggregationType )
        {
            return new AnalyticsAggregationType( AggregationType.SUM, AggregationType.LAST );
        }
        else if ( AggregationType.LAST_AVERAGE_ORG_UNIT == aggregationType )
        {
            return new AnalyticsAggregationType( AggregationType.AVERAGE, AggregationType.LAST );
        }
        else
        {
            return new AnalyticsAggregationType( aggregationType, aggregationType );
        }
    }
    
    public boolean isAggregationType( AggregationType type )
    {
        return this.aggregationType == type;
    }
    
    public boolean isPeriodAggregationType( AggregationType type )
    {
        return this.periodAggregationType == type;
    }
    
    public boolean isLastPeriodAggregationType()
    {
        return AggregationType.LAST == periodAggregationType || AggregationType.LAST_AVERAGE_ORG_UNIT == periodAggregationType;
    }
    
    public boolean isNumericDataType()
    {
        return this.dataType == DataType.NUMERIC;
    }
    
    public boolean isBooleanDataType()
    {
        return this.dataType == DataType.BOOLEAN;
    }

    // -------------------------------------------------------------------------
    // Get methods
    // -------------------------------------------------------------------------

    public AggregationType getAggregationType()
    {
        return aggregationType;
    }

    public AggregationType getPeriodAggregationType()
    {
        return ObjectUtils.firstNonNull( periodAggregationType, aggregationType );
    }

    public DataType getDataType()
    {
        return dataType;
    }

    public boolean isDisaggregation()
    {
        return disaggregation;
    }

    // -------------------------------------------------------------------------
    // toString, equals, hash code
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "aggregation type", aggregationType )
            .add( "period dim aggregation type", periodAggregationType )
            .add( "data type", dataType )
            .add( "disaggregation", disaggregation ).toString();
    }
    
    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( !getClass().isAssignableFrom( object.getClass() ) )
        {
            return false;
        }

        final AnalyticsAggregationType other = (AnalyticsAggregationType) object;

        return Objects.equals( this.aggregationType, other.aggregationType ) &&
            Objects.equals( this.periodAggregationType, other.periodAggregationType ) &&
            Objects.equals( this.dataType, other.dataType ) &&
            Objects.equals( this.disaggregation, other.disaggregation );
    }
    

    @Override
    public int hashCode()
    {
        return 31 * Objects.hash( aggregationType, periodAggregationType, dataType, disaggregation );
    }
}
