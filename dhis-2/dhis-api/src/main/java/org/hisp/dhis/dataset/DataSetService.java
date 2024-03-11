package org.hisp.dhis.dataset;

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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.User;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Lars Helge Overland
 * @version $Id: DataSetService.java 6255 2008-11-10 16:01:24Z larshelg $
 */
public interface DataSetService
{
    String ID = DataSetService.class.getName();

    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    /**
     * Adds a DataSet.
     *
     * @param dataSet The DataSet to add.
     * @return The generated unique identifier for this DataSet.
     */
    int addDataSet( DataSet dataSet );

    /**
     * Updates a DataSet.
     *
     * @param dataSet The DataSet to update.
     */
    void updateDataSet( DataSet dataSet );

    /**
     * Deletes a DataSet.
     *
     * @param dataSet The DataSet to delete.
     */
    void deleteDataSet( DataSet dataSet );

    /**
     * Get a DataSet
     *
     * @param id The unique identifier for the DataSet to get.
     * @return The DataSet with the given id or null if it does not exist.
     */
    DataSet getDataSet( int id );

    /**
     * Returns the DataSet with the given UID.
     *
     * @param uid the UID.
     * @return the DataSet with the given UID, or null if no match.
     */
    DataSet getDataSet( String uid );

    /**
     * Returns the DataSet with the given UID. Bypasses the ACL system.
     *
     * @param uid the UID.
     * @return the DataSet with the given UID, or null if no match.
     */
    DataSet getDataSetNoAcl( String uid );

    /**
     * Returns all DataSets associated with the given DataEntryForm.
     *
     * @param dataEntryForm the DataEntryForm.
     * @return a list of DataSets.
     */
    List<DataSet> getDataSetsByDataEntryForm( DataEntryForm dataEntryForm );

    /**
     * Get all DataSets.
     *
     * @return A list containing all DataSets.
     */
    List<DataSet> getAllDataSets();

    /**
     * Gets all DataSets associated with the given PeriodType.
     *
     * @param periodType the PeriodType.
     * @return a list of DataSets.
     */
    List<DataSet> getDataSetsByPeriodType( PeriodType periodType );

    /**
     * Returns a list of data sets with the given uids.
     *
     * @param uids the collection of uids.
     * @return a list of data sets.
     */
    List<DataSet> getDataSetsByUid( Collection<String> uids );

    /**
     * Returns all DataSets that can be collected through mobile (one
     * organisation unit).
     */
    List<DataSet> getDataSetsForMobile( OrganisationUnit source );

    /**
     * Returns the data sets which current user have READ access. If the current
     * user has the ALL authority then all data sets are returned.
     */
    List<DataSet> getAllDataRead();

    /**
     * Returns the data sets which given user have READ access. If the current
     * user has the ALL authority then all data sets are returned.
     * @param User  user to query for data set list
     * @return List of dataset which given user has data read access
     */
    List<DataSet> getUserDataRead( User user );

    /**
     * Returns the data sets which current user have WRITE access. If the current
     * user has the ALL authority then all data sets are returned.
     */
    List<DataSet> getAllDataWrite();

    /**
     * Returns the data sets which current user have WRITE access. If the current
     * user has the ALL authority then all data sets are returned.
     * @param User  user to query for data set list
     * @return List of dataset which given User has data write access
     */
    List<DataSet> getUserDataWrite( User user );

    // -------------------------------------------------------------------------
    // DataSet LockExceptions
    // -------------------------------------------------------------------------

    /**
     * Add new lock exception
     *
     * @param lockException LockException instance to add
     * @return Database ID of LockException
     */
    int addLockException( LockException lockException );

    /**
     * Update lock exception
     *
     * @param lockException LockException instance to update
     */
    void updateLockException( LockException lockException );

    /**
     * Delete lock exception
     *
     * @param lockException LockException instance to delete
     */
    void deleteLockException( LockException lockException );

    /**
     * Get LockException by ID
     *
     * @param id ID of LockException to get
     * @return LockException with given ID, or null if not found
     */
    LockException getLockException( int id );

    /**
     * Get number of LockExceptions in total
     *
     * @return Total count of LockExceptions
     */
    int getLockExceptionCount();

    /**
     * Returns all lock exceptions
     *
     * @return List of all the lock exceptions
     */
    List<LockException> getAllLockExceptions();

    /**
     * Get all LockExceptions within a specific range
     *
     * @param first Index to start at
     * @param max   Number of results wanted
     * @return List of LockExceptions withing the range specified
     */
    List<LockException> getLockExceptionsBetween( int first, int max );

    /**
     * Find all unique dataSet + period combinations
     * (mainly used for batch removal)
     *
     * @return A list of all unique combinations (only dataSet and period is set)
     */
    List<LockException> getLockExceptionCombinations();

    /**
     * Delete a dataSet + period combination, used for batch removal, e.g. when you
     * have a lock exception set on 100 OUs with the same dataSet + period combination.
     *
     * @param dataSet DataSet part of the combination
     * @param period  Period part of the combination
     */
    void deleteLockExceptionCombination( DataSet dataSet, Period period );

    /**
     * Delete a dataSet + period + organisationUnit combination
     *
     * @param dataSet          DataSet part of the combination
     * @param period           Period part of the combination
     * @param organisationUnit OrganisationUnit part of the combination
     */
    void deleteLockExceptionCombination( DataSet dataSet, Period period, OrganisationUnit organisationUnit );

    /**
     * Checks whether the period is locked for data entry for the given input,
     * checking the dataset's expiryDays and lockExceptions.
     *
     * @param dataSet          the data set
     * @param period           the period.
     * @param organisationUnit the organisation unit.
     * @param now              the base date for deciding locked date, current date if null.
     * @return true or false indicating whether the system is locked or not.
     */
    boolean isLocked( User user, DataSet dataSet, Period period, OrganisationUnit organisationUnit, Date now );

    /**
     * Checks whether the system is locked for data entry for the given input,
     * checking expiryDays, lockExceptions and approvals.
     *
     * @param dataSet              the data set
     * @param period               the period.
     * @param organisationUnit     the organisation unit.
     * @param attributeOptionCombo the attribute option combo.
     * @param now                  the base date for deciding locked date, current date if null.
     * @return true or false indicating whether the system is locked or not.
     */
    boolean isLocked( User user, DataSet dataSet, Period period, OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo, Date now );

    /**
     * Checks whether the system is locked for data entry for the given input,
     * checking expiryDays, lockExceptions and approvals.
     *
     * @param dataSet              the data set
     * @param period               the period.
     * @param organisationUnit     the organisation unit.
     * @param attributeOptionCombo the attribute option combo.
     * @param now                  the base date for deciding locked date, current date if null.
     * @param useOrgUnitChildren   whether to check children of the given org unit or the org unit only.
     * @return true or false indicating whether the system is locked or not.
     */
    boolean isLocked( User user, DataSet dataSet, Period period, OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo, Date now, boolean useOrgUnitChildren );

    /**
     * Checks whether the system is locked for data entry for the given input,
     * checking expiryDays, lockExceptions and approvals.
     *
     * @param dataElement          the data element.
     * @param period               the period.
     * @param organisationUnit     the organisation unit.
     * @param attributeOptionCombo the attribute option combo.
     * @param now                  the base date for deciding locked date, current date if null.
     * @return true or false indicating whether the system is locked or not.
     */
    boolean isLocked( User user, DataElement dataElement, Period period, OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo, Date now );

    /**
     * Return a list of LockException with given filter list
     *
     * @param filters
     * @return a list of LockException with given filter list
     */
    List<LockException> filterLockExceptions( List<String> filters );
}
