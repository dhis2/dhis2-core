package org.hisp.dhis.analytics.data;

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

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class QueryPlannerUtils
{
    /**
     * Creates a mapping between level and organisation unit for the given 
     * organisation units.
     * 
     * @param orgUnits list of organisation units.
     */
    public static ListMap<Integer, DimensionalItemObject> getLevelOrgUnitMap( List<DimensionalItemObject> orgUnits )
    {
        ListMap<Integer, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject orgUnit : orgUnits )
        {
            OrganisationUnit ou = (OrganisationUnit) orgUnit;

            int level = ou.getLevel();

            map.putValue( level, orgUnit );
        }

        return map;
    }

    /**
     * Creates a mapping between data type and data element for the given data 
     * elements.
     * 
     * @param dataElements list of data elements.
     */
    public static ListMap<DataType, DimensionalItemObject> getDataTypeDataElementMap( List<DimensionalItemObject> dataElements )
    {
        ListMap<DataType, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject element : dataElements )
        {
            DataElement dataElement = (DataElement) element;

            ValueType valueType = dataElement.getValueType();

            // Both Text and Date types are recognized as TEXT
            DataType dataType = ( valueType.isText() || valueType.isDate() ) ? DataType.TEXT : DataType.NUMERIC;

            map.putValue( dataType, dataElement );
        }

        return map;
    }

    /**
     * Creates a mapping between the aggregation type and data element for the
     * given data elements and period type.
     * 
     * @param params the data query parameters.
     */
    public static ListMap<AggregationType, DimensionalItemObject> getAggregationTypeDataElementMap( DataQueryParams params )
    {
        List<DimensionalItemObject> dataElements = params.getDataElements();
        PeriodType aggregationPeriodType = PeriodType.getPeriodTypeByName( params.getPeriodType() );
        
        ListMap<AggregationType, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject element : dataElements )
        {
            DataElement de = (DataElement) element;
            
            AggregationType type = ObjectUtils.firstNonNull( params.getAggregationType(), de.getAggregationType() );

            AggregationType aggregationType = getAggregationType( de.getValueType(), 
                type, aggregationPeriodType, de.getPeriodType() );

            map.putValue( aggregationType, de );
        }

        return map;
    }

    /**
     * Creates a mapping between the number of days in the period interval and period
     * for the given periods.
     * 
     * @param periods
     */
    public static ListMap<Integer, DimensionalItemObject> getDaysPeriodMap( List<DimensionalItemObject> periods )
    {
        ListMap<Integer, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject period : periods )
        {
            Period pe = (Period) period;

            int days = pe.getDaysInPeriod();

            map.putValue( days, pe );
        }

        return map;
    }

    /**
     * Puts the given element into the map according to the value type, aggregation
     * operator, aggregation period type and data period type.
     * 
     * @param valueType the value type.
     * @param aggregationType the aggregation operator.
     * @param aggregationPeriodType the aggregation period type.
     * @param dataPeriodType the data period type.
     */
    public static AggregationType getAggregationType( ValueType valueType, AggregationType aggregationType,
        PeriodType aggregationPeriodType, PeriodType dataPeriodType )
    {
        AggregationType type;

        boolean disaggregation = isDisaggregation( aggregationPeriodType, dataPeriodType );
        boolean number = valueType.isNumeric();

        if ( aggregationType.isAverage() && ValueType.BOOLEAN == valueType )
        {
            type = AggregationType.AVERAGE_BOOL;
        }
        else if ( AggregationType.AVERAGE_SUM_ORG_UNIT == aggregationType && number && disaggregation )
        {
            type = AggregationType.AVERAGE_SUM_INT_DISAGGREGATION;
        }
        else if ( AggregationType.AVERAGE_SUM_ORG_UNIT == aggregationType && number )
        {
            type = AggregationType.AVERAGE_SUM_INT;
        }
        else if ( AggregationType.AVERAGE == aggregationType && number && disaggregation )
        {
            type = AggregationType.AVERAGE_INT_DISAGGREGATION;
        }
        else if ( AggregationType.AVERAGE == aggregationType && number )
        {
            type = AggregationType.AVERAGE_INT;
        }
        else
        {
            type = aggregationType;
        }

        return type;
    }

    /**
     * Indicates whether disaggregation is allowed for the given input.
     * 
     * @param aggregationPeriodType the aggregation period type.
     * @param dataPeriodType the data period type.
     */
    public static boolean isDisaggregation( PeriodType aggregationPeriodType, PeriodType dataPeriodType )
    {
        return dataPeriodType != null && aggregationPeriodType != null && aggregationPeriodType.getFrequencyOrder() < dataPeriodType.getFrequencyOrder();
    }

    /**
     * Creates a mapping between the period type and the data element for the
     * given data elements.
     * 
     * @param dataElements list of data elements.
     */
    public static ListMap<PeriodType, DimensionalItemObject> getPeriodTypeDataElementMap( 
        Collection<DimensionalItemObject> dataElements )
    {
        ListMap<PeriodType, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject element : dataElements )
        {
            DataElement dataElement = (DataElement) element;

            map.putValue( dataElement.getPeriodType(), element );
        }

        return map;
    }

    /**
     * Converts a list of data query parameters to a list of event query parameters.
     * 
     * @param params list of data query parameters.
     */
    public static List<EventQueryParams> convert( List<DataQueryParams> params )
    {
        List<EventQueryParams> eventParams = new ArrayList<>();
        
        for ( DataQueryParams param : params )
        {
            eventParams.add( (EventQueryParams) param );
        }
        
        return eventParams;
    }
}
