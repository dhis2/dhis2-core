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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.datavalue.DeflatedDataValueHandler;
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
 * single data request (which means a single SQL query) with a callback method
 * for each data value. It collects data values until it has all the data for an
 * organisation unit, and then it returns them to the caller.
 * <p>
 * To allow for callbacks from below, but synchronous returns to above, a second
 * thread is used and coordinated with semaphores in a producer-consumer
 * pattern. Without the two threads, the caller (such as the predictor logic)
 * would have to be written to accept callbacks, meaning that any method local
 * variables needed between the initializing of data fetching and the fetching
 * of data values would have to be converted to instance variables. It also
 * means that the caller could not be doing anything else while waiting for the
 * callback, such as processing two data streams at once which the predictor
 * service does.
 * <p>
 * The returned data values may optionally include deleted values, because
 * predictor processing needs to know where the former predicted values are
 * present, even when they are deleted, because they might be replaced with new,
 * undeleted values.
 * <p>
 * For reliability, if there is an exception on the second thread, the thread is
 * terminated and the exception is re-thrown in the main thread when data is
 * next requested from the caller. This makes sure that the exception is not
 * properly handled by the main thread.
 *
 * @author Jim Grace
 */
public class PredictionDataValueFetcher
    implements Runnable, DeflatedDataValueHandler
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

    private CachingMap<Long, CategoryOptionCombo> cocLookup = new CachingMap<>();

    List<DeflatedDataValue> deflatedDataValues;

    private String producerOrgUnitPath;

    private String consumerOrgUnitPath;

    private RuntimeException producerException;

    ExecutorService executor;

    private Semaphore producerSemaphore; // Acquired while producing data

    private Semaphore consumerSemaphore; // Acquired until data is consumed

    private boolean endOfData; // No more data to fetch

    private long semaphoreTimeout = 10;

    private TimeUnit semaphoreTimeoutUnit = TimeUnit.MINUTES;

    private static final String BEFORE_PATHS = "."; // Lexically before '/'

    private static final String AFTER_PATHS = "0"; // Lexically after '/'

    public PredictionDataValueFetcher( DataValueService dataValueService,
        CategoryService categoryService )
    {
        checkNotNull( dataValueService );
        checkNotNull( categoryService );

        this.dataValueService = dataValueService;
        this.categoryService = categoryService;
    }

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
    public void init( Set<OrganisationUnit> currentUserOrgUnits, int orgUnitLevel, List<OrganisationUnit> orgUnits,
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

        deflatedDataValues = new ArrayList<>();
        producerOrgUnitPath = AFTER_PATHS;
        consumerOrgUnitPath = BEFORE_PATHS;
        endOfData = false;
        producerException = null;

        producerSemaphore = new Semaphore( 1 );
        consumerSemaphore = new Semaphore( 1 );

        producerSemaphore.acquireUninterruptibly();
        consumerSemaphore.acquireUninterruptibly();

        executor = Executors.newSingleThreadExecutor();
        executor.execute( this ); // Invoke run() on another thread
        executor.shutdown();
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
        params.setOrgUnitLevel( orgUnitLevel );
        params.setOrgUnitParents( currentUserOrgUnits );
        params.setCallback( this );
        params.setOrderByOrgUnitPath( true );
        params.setIncludeChildren( includeChildren );
        params.setIncludeDeleted( includeDeleted );

        try
        {
            dataValueService.getDeflatedDataValues( params );
        }
        catch ( RuntimeException ex )
        {
            producerException = ex;
        }

        endOfData = true; // No more data will be produced

        producerSemaphore.release(); // The last orgUnit is ready to consume
    }

    /**
     * In a separate thread, handles a callback with a fetched data value.
     *
     * @param ddv the deflated data value fetched.
     */
    @Override
    public void handle( DeflatedDataValue ddv )
    {
        if ( producerOrgUnitPath.equals( AFTER_PATHS ) )
        {
            producerOrgUnitPath = truncatePathToLevel( ddv.getSourcePath() );
        }

        if ( !ddv.getSourcePath().startsWith( producerOrgUnitPath ) )
        {
            producerSemaphore.release(); // Data is ready to consume

            if ( !tryAcquire( consumerSemaphore ) ) // Wait until data consumed
            {
                throw new IllegalStateException( "handle could not acquire consumer semaphore" );
            }

            deflatedDataValues = new ArrayList<>();

            producerOrgUnitPath = truncatePathToLevel( ddv.getSourcePath() );
        }

        deflatedDataValues.add( ddv );
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
        if ( !tryAcquire( producerSemaphore ) ) // Wait until data is ready
        {
            throw new IllegalStateException( "getDataValues could not acquire producer semaphore" );
        }

        if ( producerException != null )
        {
            throw producerException;
        }

        if ( orgUnit.getPath().compareTo( consumerOrgUnitPath ) <= 0 )
        {
            throw new IllegalArgumentException( "getDataValues out of order, after " + consumerOrgUnitPath
                + " called with " + orgUnit.toString() );
        }

        consumerOrgUnitPath = orgUnit.getPath();

        if ( consumerOrgUnitPath.compareTo( producerOrgUnitPath ) < 0 )
        {
            producerSemaphore.release(); // Release so we can acquire it next
                                         // time

            return new ArrayList<>(); // No data produced for this orgUnit
        }

        if ( !consumerOrgUnitPath.equals( producerOrgUnitPath ) )
        {
            throw new IllegalArgumentException( "getDataValues ready for " + producerOrgUnitPath
                + " but called with " + orgUnit.toString() );
        }

        List<DataValue> dataValues = deflatedDataValues.stream()
            .map( this::inflateDataValue ).collect( Collectors.toList() );

        consumerSemaphore.release(); // Data now consumed (in local variable)

        if ( endOfData )
        {
            producerOrgUnitPath = AFTER_PATHS;

            producerSemaphore.release(); // No more data will be produced
        }

        return dataValues;
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

    /**
     * Sets the internal semaphore timeout value. This can be used to test the
     * timeout functionality faster than if the test had to wait for the default
     * timeout value.
     *
     * @param semaphoreTimeout semaphore timeout value.
     * @param semaphoreTimeoutUnit units of the timeout value.
     */
    public void setSemaphoreTimeout( long semaphoreTimeout, TimeUnit semaphoreTimeoutUnit )
    {
        this.semaphoreTimeout = semaphoreTimeout;
        this.semaphoreTimeoutUnit = semaphoreTimeoutUnit;
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Truncates a path from a deflated data value to the level of the
     * organisation units we are looking for now.
     *
     * @param path the orgUnit path from the deflated data value.
     * @return the path truncated to the level we are now processing.
     */
    private String truncatePathToLevel( String path )
    {
        return path.substring( 0, orgUnitLevel * 12 );
    }

    /**
     * Try to acquire a semaphore, within a certain time period.
     *
     * @param semaphore the semaphore to acquire.
     * @return true if it was acquired, else false.
     */
    private boolean tryAcquire( Semaphore semaphore )
    {
        try
        {
            return semaphore.tryAcquire( semaphoreTimeout, semaphoreTimeoutUnit );
        }
        catch ( InterruptedException e )
        {
            return false;
        }
    }

    /**
     * "Inflate" a deflated data value, using our caches.
     *
     * @param ddv the deflated data value.
     * @return the regular ("inflated") data value.
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
}
