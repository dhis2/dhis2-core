/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.predictor;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;
import static org.hisp.dhis.datavalue.DataValueStore.DDV_QUEUE_TIMEOUT_UNIT;
import static org.hisp.dhis.datavalue.DataValueStore.DDV_QUEUE_TIMEOUT_VALUE;
import static org.hisp.dhis.datavalue.DataValueStore.END_OF_DDV_DATA;
import static org.hisp.dhis.system.util.MathUtils.addDoubleObjects;
import static org.hisp.dhis.system.util.ValidationUtils.getObjectValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * Fetches DataValues for a prediction.
 * <p>
 * This class fetches values from the datavalue table and returns them for each
 * organisation unit. It assumes that they will be requested in order of
 * organisation unit path and that the values for each organisation unit will be
 * requested only once. For reliability, these assumptions are checked, and
 * exceptions thrown if they are not met.
 * <p>
 * Each time the instance is initialized, it will fetch data for only one
 * organisation unit level.
 * <p>
 * This class maintains performance while limiting memory usage by using a
 * single data request (which means a single SQL query) on a separate thread
 * that provides results to this class through a blocking queue. This class then
 * collects data values until it has all the data for an organisation unit, and
 * then it returns them to the caller.
 * <p>
 * The returned data values may optionally include deleted values, because
 * predictor processing needs to know where the former predicted values are
 * present, even when they are deleted, because they might be replaced with new,
 * undeleted values.
 * <p>
 * For reliability, if there is an exception on the second thread, the thread is
 * terminated and the exception is re-thrown in the main thread when data is
 * next requested from the caller. This makes sure that the exception is
 * properly reported by the main thread.
 *
 * @author Jim Grace
 */
@RequiredArgsConstructor
public class PredictionDataValueFetcher
    implements Runnable
{
    private final DataValueService dataValueService;

    private final CategoryService categoryService;

    private Set<OrganisationUnit> currentUserOrgUnits;

    private int orgUnitLevel;

    private Set<Period> queryPeriods;

    private Set<Period> outputPeriods;

    private Set<DataElement> dataElements;

    private Set<DataElementOperand> dataElementOperands;

    private DataElementOperand outputDataElementOperand;

    private boolean includeDescendants = false;

    private boolean includeDeleted = false;

    private Map<String, OrganisationUnit> orgUnitLookup;

    private Map<Long, DataElement> dataElementLookup;

    private Map<Long, Period> periodLookup;

    private CachingMap<Long, CategoryOptionCombo> cocLookup;

    private DeflatedDataValue nextDeflatedDataValue;

    private OrganisationUnit nextOrgUnit;

    private RuntimeException producerException;

    private ExecutorService executor;

    private BlockingQueue<DeflatedDataValue> blockingQueue;

    /**
     * The blocking queue size was chosen after performance testing. A value of
     * 1 performed slightly better than 2 or larger values. Since most of the
     * values are just buffered after they are pulled from the queue, there is
     * no benefit of incurring the slight overhead of a larger queue size.
     */
    private static final int DDV_BLOCKING_QUEUE_SIZE = 1;

    /**
     * Initializes for datavalue retrieval.
     *
     * @param currentUserOrgUnits orgUnits assigned to current user.
     * @param orgUnitLevel level of organisation units to fetch.
     * @param orgUnits organisation units to fetch.
     * @param queryPeriods periods to fetch.
     * @param outputPeriods predictor output periods.
     * @param dataElements data elements to fetch.
     * @param dataElementOperands data element operands to fetch.
     */
    public void init(
        Set<OrganisationUnit> currentUserOrgUnits, int orgUnitLevel, List<OrganisationUnit> orgUnits,
        Set<Period> queryPeriods, Set<Period> outputPeriods, Set<DataElement> dataElements,
        Set<DataElementOperand> dataElementOperands, DataElementOperand outputDataElementOperand )
    {
        this.currentUserOrgUnits = currentUserOrgUnits;
        this.orgUnitLevel = orgUnitLevel;
        this.queryPeriods = queryPeriods;
        this.outputPeriods = outputPeriods;
        this.dataElements = dataElements;
        this.dataElementOperands = dataElementOperands;
        this.outputDataElementOperand = outputDataElementOperand;

        orgUnitLookup = orgUnits.stream().collect( Collectors.toMap( OrganisationUnit::getPath, ou -> ou ) );
        dataElementLookup = dataElements.stream().collect( Collectors.toMap( DataElement::getId, de -> de ) );
        dataElementLookup.putAll( dataElementOperands.stream().collect(
            Collectors.toMap( d -> d.getDataElement().getId(), DataElementOperand::getDataElement ) ) );
        periodLookup = queryPeriods.stream().collect( Collectors.toMap( Period::getId, p -> p ) );
        cocLookup = new CachingMap<>();

        producerException = null;

        blockingQueue = new ArrayBlockingQueue<>( DDV_BLOCKING_QUEUE_SIZE );

        if ( isEmpty( dataElements ) && isEmpty( dataElementOperands ) )
        {
            nextOrgUnit = null; // There will be no data

            return;
        }

        executor = Executors.newSingleThreadExecutor();
        executor.execute( this ); // Invoke run() on another thread
        executor.shutdown();

        getNextDeflatedDataValue(); // Prime the algorithm with the first value.
    }

    /**
     * In a separate thread, fetches all the requested data values.
     */
    @Override
    public void run()
    {
        DataExportParams params = new DataExportParams();
        params.setDataElements( dataElements );
        params.setDataElementOperands( dataElementOperands );
        params.setPeriods( queryPeriods );
        params.setOrganisationUnits( currentUserOrgUnits );
        params.setOuMode( DESCENDANTS );
        params.setOrgUnitLevel( orgUnitLevel );
        params.setBlockingQueue( blockingQueue );
        params.setOrderByOrgUnitPath( true );
        params.setIncludeDescendants( includeDescendants );
        params.setIncludeDeleted( includeDeleted );

        try
        {
            dataValueService.getDeflatedDataValues( params );
        }
        catch ( RuntimeException ex )
        {
            producerException = ex; // Tell the main thread

            queueEndOfDataMarker(); // Wake up main thread if needed

            throw ex; // Log the exception
        }
    }

    /**
     * In the main thread, gets prediction data for the next organisation unit.
     * <p>
     * Note that "inflating" the data values from their deflated form must be
     * done in the main thread so as not to upset the DataValue's Hibernate
     * properties.
     *
     * @return the prediction data
     */
    public PredictionData getData()
    {
        if ( nextOrgUnit == null )
        {
            return null;
        }

        List<DeflatedDataValue> deflatedDataValues = new ArrayList<>();

        OrganisationUnit startingOrgUnit = nextOrgUnit;

        do
        {
            deflatedDataValues.add( nextDeflatedDataValue );

            getNextDeflatedDataValue();
        }
        while ( startingOrgUnit.equals( nextOrgUnit ) );

        return getPredictionData( startingOrgUnit, deflatedDataValues );
    }

    // -------------------------------------------------------------------------
    // Set methods
    // -------------------------------------------------------------------------

    /**
     * Sets whether the data should return aggregated to the parent org unit.
     *
     * @param includeDescendants whether the data includes descendant orgUnits.
     * @return this object (for method chaining).
     */
    public PredictionDataValueFetcher setIncludeDescendants( boolean includeDescendants )
    {
        this.includeDescendants = includeDescendants;
        return this;
    }

    /**
     * Sets whether the data should include deleted values.
     *
     * @param includeDeleted whether the data should include deleted values.
     * @return this object (for method chaining).
     */
    public PredictionDataValueFetcher setIncludeDeleted( boolean includeDeleted )
    {
        this.includeDeleted = includeDeleted;
        return this;
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Gets the next deflated data value. Remembers it and its path.
     */
    private void getNextDeflatedDataValue()
    {
        nextDeflatedDataValue = dequeueDeflatedDataValue();

        checkForProducerException(); // Check for exception during dequeue

        if ( nextDeflatedDataValue == END_OF_DDV_DATA )
        {
            nextOrgUnit = null; // No more data

            return;
        }

        nextOrgUnit = orgUnitLookup.get( truncatePathToLevel( nextDeflatedDataValue.getSourcePath() ) );
    }

    /**
     * Dequeues the next {@see DeflatedDataValue} from the database feed
     */
    private DeflatedDataValue dequeueDeflatedDataValue()
    {
        try
        {
            return blockingQueue.poll( DDV_QUEUE_TIMEOUT_VALUE, DDV_QUEUE_TIMEOUT_UNIT );
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();

            throw new IllegalStateException( "could not fetch next DeflatedDataValue" );
        }
    }

    /**
     * Truncates a path from a deflated data value to the level of the
     * organisation units we are looking for now.
     */
    private String truncatePathToLevel( String path )
    {
        return path.substring( 0, orgUnitLevel * 12 );
    }

    /**
     * Gets prediction data for an orgUnit from a list of deflated data values.
     */
    private PredictionData getPredictionData( OrganisationUnit orgUnit, List<DeflatedDataValue> deflatedDataValues )
    {
        MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map = new MapMapMap<>();

        List<DataValue> oldPredictions = new ArrayList<>();

        for ( DeflatedDataValue ddv : deflatedDataValues )
        {
            DataValue dv = inflateDataValue( ddv );

            if ( !dv.isDeleted() )
            {
                addValueToMap( dv, map );
            }

            if ( ddv.getSourcePath().equals( dv.getSource().getPath() )
                && ddv.getDataElementId() == outputDataElementOperand.getDataElement().getId()
                && ddv.getCategoryOptionComboId() == (outputDataElementOperand.getCategoryOptionCombo().getId())
                && outputPeriods.contains( dv.getPeriod() ) )
            {
                oldPredictions.add( dv );
            }
        }

        return new PredictionData( orgUnit, mapToValues( orgUnit, map ), oldPredictions );
    }

    /**
     * "Inflates" a deflated data value, using our caches.
     */
    private DataValue inflateDataValue( DeflatedDataValue ddv )
    {
        DataElement dataElement = dataElementLookup.get( ddv.getDataElementId() );

        Period period = periodLookup.get( ddv.getPeriodId() );

        OrganisationUnit orgUnit = orgUnitLookup.get( truncatePathToLevel( ddv.getSourcePath() ) );

        CategoryOptionCombo categoryOptionCombo = cocLookup.get( ddv.getCategoryOptionComboId(),
            () -> categoryService.getCategoryOptionCombo( ddv.getCategoryOptionComboId() ) );

        CategoryOptionCombo attributeOptionCombo = cocLookup.get( ddv.getAttributeOptionComboId(),
            () -> categoryService.getCategoryOptionCombo( ddv.getAttributeOptionComboId() ) );

        return new DataValue( dataElement, period, orgUnit, categoryOptionCombo, attributeOptionCombo, ddv.getValue(),
            ddv.getStoredBy(), ddv.getLastUpdated(), ddv.getComment(), ddv.isFollowup(), ddv.isDeleted() );
    }

    /**
     * Adds a non-deleted value to the value map.
     * <p>
     * The two types of dimensional item object that are needed from the data
     * value table are DataElement (the sum of all category option combos for
     * that data element) and DataElementOperand (a particular combination of
     * DataElement and CategoryOptionCombo).
     */
    private void addValueToMap( DataValue dv,
        MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map )
    {
        Object value = getObjectValue( dv.getValue(), dv.getDataElement().getValueType() );

        if ( value != null )
        {
            DataElementOperand dataElementOperand = new DataElementOperand(
                dv.getDataElement(), dv.getCategoryOptionCombo() );

            addToMap( dataElementOperand, dataElementOperands, dv, value, map );

            addToMap( dv.getDataElement(), dataElements, dv, value, map );
        }
    }

    /**
     * Adds the DataElementOperand or the DataElement value to existing data.
     * <p>
     * This is needed because we may get multiple data values that need to be
     * aggregated to the same item value. In the case of a
     * DimensionalItemObject, this may be multiple values from children
     * organisation units. In the case of a DataElement, this may be multiple
     * value from children organisation units and/or it may be multiple
     * disaggregated values that need to be summed for the data element.
     * <p>
     * Note that a single data value may contribute to a DataElementOperand
     * value, a DataElement value, or both.
     */
    private void addToMap( DimensionalItemObject item, Set<? extends DimensionalItemObject> items,
        DataValue dv, Object value, MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map )
    {
        if ( !items.contains( item ) )
        {
            return;
        }

        Object valueSoFar = map.getValue( dv.getAttributeOptionCombo(), dv.getPeriod(), item );

        Object valueToStore = (valueSoFar == null) ? value : addDoubleObjects( value, valueSoFar );

        map.putEntry( dv.getAttributeOptionCombo(), dv.getPeriod(), item, valueToStore );
    }

    /**
     * Convert the value map to a list of found values.
     */
    private List<FoundDimensionItemValue> mapToValues( OrganisationUnit orgUnit,
        MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map )
    {
        List<FoundDimensionItemValue> values = new ArrayList<>();

        for ( Map.Entry<CategoryOptionCombo, MapMap<Period, DimensionalItemObject, Object>> e1 : map.entrySet() )
        {
            CategoryOptionCombo aoc = e1.getKey();

            for ( Map.Entry<Period, Map<DimensionalItemObject, Object>> e2 : e1.getValue().entrySet() )
            {
                Period period = e2.getKey();

                for ( Map.Entry<DimensionalItemObject, Object> e3 : e2.getValue().entrySet() )
                {
                    DimensionalItemObject obj = e3.getKey();
                    Object value = e3.getValue();

                    values.add( new FoundDimensionItemValue( orgUnit, period, aoc, obj, value ) );
                }
            }
        }

        return values;
    }

    /**
     * Checks for an unexpected exception in the producer thread, and if found,
     * throws it on the main thread.
     */
    private void checkForProducerException()
    {
        if ( producerException != null )
        {
            throw producerException;
        }
    }

    /**
     * Adds the end of data marker to the queue. This is used in case there is
     * an unexpected runtime exception in the producer thread, and the consumer
     * (main) thread is waiting for a data value. This allows the main thread to
     * wake up and handle (rethrow) the exception.
     */
    private void queueEndOfDataMarker()
    {
        try
        {
            blockingQueue.offer( END_OF_DDV_DATA, DDV_QUEUE_TIMEOUT_VALUE, DDV_QUEUE_TIMEOUT_UNIT );
        }
        catch ( InterruptedException ex )
        {
            Thread.currentThread().interrupt();

            throw new IllegalStateException( "could not add end of deflated data values marker" );
        }
    }
}
