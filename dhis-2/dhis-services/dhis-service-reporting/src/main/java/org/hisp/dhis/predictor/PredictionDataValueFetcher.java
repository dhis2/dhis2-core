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

import java.util.ArrayList;
import java.util.Collections;
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

    private Set<Period> periods;

    private Set<DataElement> dataElements;

    private Set<DataElementOperand> dataElementOperands;

    private boolean includeChildren = false;

    private boolean includeDeleted = false;

    private Map<String, OrganisationUnit> orgUnitLookup;

    private Map<Long, DataElement> dataElementLookup;

    private Map<Long, Period> periodLookup;

    private CachingMap<Long, CategoryOptionCombo> cocLookup;

    private DataValue nextDataValue;

    private String producerOrgUnitPath;

    private String consumerOrgUnitPath;

    private RuntimeException producerException;

    ExecutorService executor;

    private BlockingQueue<DeflatedDataValue> blockingQueue;

    private static final String BEFORE_PATHS = "."; // Lexically before '/'

    private static final String AFTER_PATHS = "0"; // Lexically after '/'

    /**
     * The blocking queue size was chosen after performance testing. Five
     * performed better than 1 or 2 (more parallelism), and better than than 10
     * or 20 (less queue management overhead).
     */
    private static final int DDV_BLOCKING_QUEUE_SIZE = 5;

    /**
     * Initializes for datavalue retrieval.
     *
     * @param currentUserOrgUnits orgUnits assigned to current user.
     * @param orgUnitLevel level of organisation units to fetch.
     * @param orgUnits organisation units to fetch.
     * @param periods periods to fetch.
     * @param dataElements data elements to fetch.
     * @param dataElementOperands data element operands to fetch.
     */
    public void init(
        Set<OrganisationUnit> currentUserOrgUnits, int orgUnitLevel, List<OrganisationUnit> orgUnits,
        Set<Period> periods, Set<DataElement> dataElements, Set<DataElementOperand> dataElementOperands )
    {
        this.currentUserOrgUnits = currentUserOrgUnits;
        this.orgUnitLevel = orgUnitLevel;
        this.periods = periods;
        this.dataElements = dataElements;
        this.dataElementOperands = dataElementOperands;

        orgUnitLookup = orgUnits.stream().collect( Collectors.toMap( OrganisationUnit::getPath, ou -> ou ) );
        dataElementLookup = dataElements.stream().collect( Collectors.toMap( DataElement::getId, de -> de ) );
        dataElementLookup.putAll( dataElementOperands.stream().collect(
            Collectors.toMap( d -> d.getDataElement().getId(), DataElementOperand::getDataElement ) ) );
        periodLookup = periods.stream().collect( Collectors.toMap( Period::getId, p -> p ) );
        cocLookup = new CachingMap<>();

        consumerOrgUnitPath = BEFORE_PATHS;
        producerException = null;

        blockingQueue = new ArrayBlockingQueue<>( DDV_BLOCKING_QUEUE_SIZE );

        if ( isEmpty( dataElements ) && isEmpty( dataElementOperands ) )
        {
            producerOrgUnitPath = AFTER_PATHS; // There will be no data

            return;
        }

        executor = Executors.newSingleThreadExecutor();
        executor.execute( this ); // Invoke run() on another thread
        executor.shutdown();

        getNextDataValue(); // Prime the algorithm with the first data value.
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
        params.setPeriods( periods );
        params.setOrganisationUnits( currentUserOrgUnits );
        params.setOuMode( DESCENDANTS );
        params.setOrgUnitLevel( orgUnitLevel );
        params.setBlockingQueue( blockingQueue );
        params.setOrderByOrgUnitPath( true );
        params.setIncludeChildren( includeChildren );
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
     * In the main thread, gets the data values for a single organisation unit.
     * <p>
     * Note that "inflating" the data values from their deflated form must be
     * done in the main thread so as not to upset the DataValue's Hibernate
     * properties.
     *
     * @param orgUnit the organisation unit to get the data values for
     * @return the list of data values
     */
    public List<DataValue> getDataValues( OrganisationUnit orgUnit )
    {
        checkForProducerException();

        if ( orgUnit.getPath().compareTo( consumerOrgUnitPath ) <= 0 )
        {
            throw new IllegalArgumentException( "getDataValues out of order, after " + consumerOrgUnitPath
                + " called with " + orgUnit.toString() );
        }

        consumerOrgUnitPath = orgUnit.getPath();

        if ( consumerOrgUnitPath.compareTo( producerOrgUnitPath ) < 0 )
        {
            return Collections.emptyList(); // No data fetched for this orgUnit
        }

        if ( !consumerOrgUnitPath.equals( producerOrgUnitPath ) )
        {
            throw new IllegalArgumentException( "getDataValues ready for " + producerOrgUnitPath
                + " but called with " + orgUnit.toString() );
        }

        return getDataValuesForProducerOrgUnit();
    }

    // -------------------------------------------------------------------------
    // Set methods
    // -------------------------------------------------------------------------

    /**
     * Sets whether the data should return aggregated to the parent org unit.
     *
     * @param includeChildren whether the data should include child orgUnits.
     * @return this object (for method chaining).
     */
    public PredictionDataValueFetcher setIncludeChildren( boolean includeChildren )
    {
        this.includeChildren = includeChildren;
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
     * Called once we have a match between the requested (consumer) organisation
     * unit and the organisation unit of the next data value (producer), this
     * method returns a list containing the next data value and all subsequent
     * data values having the same organisation unit.
     */
    private List<DataValue> getDataValuesForProducerOrgUnit()
    {
        List<DataValue> dataValues = new ArrayList<>();

        String startingOrgUnitPath = producerOrgUnitPath;

        do
        {
            dataValues.add( nextDataValue );

            getNextDataValue();
        }
        while ( producerOrgUnitPath.equals( startingOrgUnitPath ) );

        return dataValues;
    }

    /**
     * Gets the next data value. Remembers it and its path
     */
    private void getNextDataValue()
    {
        DeflatedDataValue ddv = dequeueDeflatedDataValue();

        checkForProducerException(); // Check for exception during dequeue

        if ( ddv == END_OF_DDV_DATA )
        {
            producerOrgUnitPath = AFTER_PATHS; // No more data

            return;
        }

        nextDataValue = inflateDataValue( ddv );

        producerOrgUnitPath = truncatePathToLevel( nextDataValue.getSource().getPath() );
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
     * "Inflate" a deflated data value, using our caches.
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
