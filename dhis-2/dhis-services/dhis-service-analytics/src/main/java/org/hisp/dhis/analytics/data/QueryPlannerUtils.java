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
package org.hisp.dhis.analytics.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.util.ObjectUtils;

/**
 * Utilities for analytics query planning.
 *
 * @author Lars Helge Overland
 */
public class QueryPlannerUtils
{
    /**
     * Creates a mapping between level and organisation units for the given
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

            map.putValue( ou.getLevel(), orgUnit );
        }

        return map;
    }

    /**
     * Creates a mapping between level and organisation units for the given
     * organisation units.
     *
     * @param orgUnits list of organisation units.
     */
    public static ListMap<Integer, OrganisationUnit> getLevelOrgUnitTypedMap( List<OrganisationUnit> orgUnits )
    {
        ListMap<Integer, OrganisationUnit> map = new ListMap<>();
        orgUnits.stream().forEach( ou -> map.putValue( ou.getLevel(), ou ) );
        return map;
    }

    /**
     * Creates a mapping between data type and data elements for the given data
     * elements.
     *
     * @param dataElements list of data elements.
     */
    public static ListMap<DataType, DimensionalItemObject> getDataTypeDataElementMap(
        List<DimensionalItemObject> dataElements )
    {
        ListMap<DataType, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject element : dataElements )
        {
            DataElement dataElement = (DataElement) element;

            ValueType valueType = dataElement.getValueType();

            // Both text and date types are recognized as DataType.TEXT

            DataType dataType = (valueType.isText() || valueType.isDate()) ? DataType.TEXT : DataType.NUMERIC;

            map.putValue( dataType, dataElement );
        }

        return map;
    }

    /**
     * Creates a mapping between query modifiers and data elements for the given
     * data elements.
     *
     * @param dataElements list of data elements.
     */
    public static ListMap<QueryModifiers, DimensionalItemObject> getQueryModsElementMap(
        List<DimensionalItemObject> dataElements )
    {
        ListMap<QueryModifiers, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject element : dataElements )
        {
            QueryModifiers queryMods = (element.getQueryMods() != null)
                ? element.getQueryMods().withQueryModsForAnalyticsGrouping()
                : QueryModifiers.builder().build();

            map.putValue( queryMods, element );
        }

        return map;
    }

    /**
     * Creates a mapping between the aggregation type and data elements for the
     * given data elements and period type.
     *
     * @param dataElements a List of {@see DimensionalItemObject}
     * @param aggregationType an {@see AnalyticsAggregationType}
     * @param periodType a String representing a Period Type (e.g. 201901)
     */
    public static ListMap<AnalyticsAggregationType, DimensionalItemObject> getAggregationTypeDataElementMap(
        List<DimensionalItemObject> dataElements, AnalyticsAggregationType aggregationType, String periodType )
    {
        PeriodType aggregationPeriodType = PeriodType.getPeriodTypeByName( periodType );

        ListMap<AnalyticsAggregationType, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject element : dataElements )
        {
            DataElement de = (DataElement) element;

            AnalyticsAggregationType aggType = ObjectUtils.firstNonNull( aggregationType,
                AnalyticsAggregationType.fromAggregationType( de.getAggregationType() ) );

            AnalyticsAggregationType analyticsAggregationType = getAggregationType( aggType, de.getValueType(),
                aggregationPeriodType, de.getPeriodType() );

            map.putValue( analyticsAggregationType, de );
        }

        return map;
    }

    /**
     * Creates a mapping between the number of days in the period interval and
     * periods for the given periods.
     *
     * @param periods
     */
    public static ListMap<Integer, DimensionalItemObject> getDaysPeriodMap( List<DimensionalItemObject> periods )
    {
        ListMap<Integer, DimensionalItemObject> map = new ListMap<>();

        for ( DimensionalItemObject period : periods )
        {
            Period pe = (Period) period;

            map.putValue( pe.getDaysInPeriod(), pe );
        }

        return map;
    }

    /**
     * Returns the {@link AnalyticsAggregationType} according to the given value
     * type, aggregation type, value type aggregation period type and data
     * period type.
     *
     * @param aggregationType the aggregation type.
     * @param valueType the value type.
     * @param aggregationPeriodType the aggregation period type.
     * @param dataPeriodType the data period type.
     */
    public static AnalyticsAggregationType getAggregationType( AnalyticsAggregationType aggregationType,
        ValueType valueType,
        PeriodType aggregationPeriodType, PeriodType dataPeriodType )
    {
        DataType dataType = DataType.fromValueType( valueType );
        boolean disaggregation = isDisaggregation( aggregationType, aggregationPeriodType, dataPeriodType );

        return new AnalyticsAggregationType( aggregationType.getAggregationType(),
            aggregationType.getPeriodAggregationType(), dataType, disaggregation );
    }

    /**
     * Indicates whether disaggregation is allowed for the given input.
     * Disaggregation implies that the frequency order of the aggregation period
     * type is lower than the data period type.
     *
     * @param aggregationPeriodType the aggregation period type.
     * @param dataPeriodType the data period type.
     */
    public static boolean isDisaggregation( AnalyticsAggregationType aggregationType,
        PeriodType aggregationPeriodType, PeriodType dataPeriodType )
    {
        if ( dataPeriodType == null || aggregationPeriodType == null )
        {
            return false;
        }

        if ( aggregationType == null || AggregationType.AVERAGE != aggregationType.getPeriodAggregationType() )
        {
            return false;
        }

        if ( aggregationPeriodType.getFrequencyOrder() < dataPeriodType.getFrequencyOrder() )
        {
            return true;
        }

        if ( aggregationPeriodType.getFrequencyOrder() == dataPeriodType.getFrequencyOrder() &&
            !aggregationPeriodType.equals( dataPeriodType ) )
        {
            return true;
        }

        return false;
    }

    /**
     * Creates a mapping between the period type and data elements for the given
     * list of data elements.
     *
     * @param dataElements the list of data elements.
     */
    public static ListMap<PeriodType, DimensionalItemObject> getPeriodTypeDataElementMap(
        Collection<DimensionalItemObject> dataElements )
    {
        ListMap<PeriodType, DimensionalItemObject> map = new ListMap<>();
        dataElements.forEach( de -> map.putValue( ((DataElement) de).getPeriodType(), de ) );
        return map;
    }

    /**
     * Converts a list of data query parameters to a list of event query
     * parameters.
     *
     * @param params the list of data query parameters.
     */
    public static List<EventQueryParams> convert( List<DataQueryParams> params )
    {
        List<EventQueryParams> eventParams = new ArrayList<>();
        params.forEach( p -> eventParams.add( (EventQueryParams) p ) );
        return eventParams;
    }
}
