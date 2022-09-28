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
package org.hisp.dhis.dataset;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.SetValuedMap;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface DataSetService extends DataSetDataIntegrityProvider
{
    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    /**
     * Adds a DataSet.
     *
     * @param dataSet The DataSet to add.
     * @return The generated unique identifier for this DataSet.
     */
    long addDataSet( DataSet dataSet );

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
    DataSet getDataSet( long id );

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
     * Returns the data sets which given user have READ access. If the current
     * user has the ALL authority then all data sets are returned.
     *
     * @param user the user to query for data set list.
     * @return a list of data sets which the given user has data read access to.
     */
    List<DataSet> getUserDataRead( User user );

    /**
     * Returns the data sets which current user have WRITE access. If the
     * current user has the ALL authority then all data sets are returned.
     *
     * @param user the user to query for data set list.
     * @return a list of data sets which given user has data write access to.
     */
    List<DataSet> getUserDataWrite( User user );

    // -------------------------------------------------------------------------
    // DataSet LockExceptions
    // -------------------------------------------------------------------------

    /**
     * Adds new lock exception.
     *
     * @param lockException LockException instance to add.
     * @return identifier of lock exception.
     */
    long addLockException( LockException lockException );

    /**
     * Updates lock exception.
     *
     * @param lockException lock exception instance to update.
     */
    void updateLockException( LockException lockException );

    /**
     * Deletes lock exception.
     *
     * @param lockException lock exception instance to delete.
     */
    void deleteLockException( LockException lockException );

    /**
     * Get lock exception by ID.
     *
     * @param id the ID of lock exception to get.
     * @return lock exception with given ID, or null if not found.
     */
    LockException getLockException( long id );

    /**
     * Get number of LockExceptions in total.
     *
     * @return the total count of lock exceptions.
     */
    int getLockExceptionCount();

    /**
     * Returns all lock exceptions.
     *
     * @return a list of all lock exceptions.
     */
    List<LockException> getAllLockExceptions();

    /**
     * Returns lock exceptions associated with the data sets for which the
     * current user has data write sharing access to.
     *
     * @return a list of lock exceptions.
     */
    List<LockException> getDataWriteLockExceptions();

    /**
     * Find all unique data set and period combinations (mainly used for batch
     * removal). The returned lock exceptions are generated and not persisted.
     *
     * @return list of all unique combinations (only data set and period are
     *         set).
     */
    List<LockException> getLockExceptionCombinations();

    /**
     * Checks whether the system is locked for data entry for the given input,
     * checking expiry days, lock exceptions and data approvals.
     *
     * @param dataSet the data set
     * @param period the period.
     * @param organisationUnit the organisation unit.
     * @param attributeOptionCombo the attribute option combo.
     * @return the {@link LockStatus}.
     */
    LockStatus getLockStatus( DataSet dataSet, Period period,
        OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo );

    /**
     * Checks whether the system is locked for data entry for the given input,
     * checking expiry days, lock exceptions and data approvals.
     *
     * @param dataSet the data set
     * @param period the period.
     * @param organisationUnit the organisation unit.
     * @param attributeOptionCombo the attribute option combo.
     * @param user the user for deciding lock status.
     * @param now the base date for deciding locked date, current date if null.
     * @return the {@link LockStatus}.
     */
    LockStatus getLockStatus( DataSet dataSet, Period period, OrganisationUnit organisationUnit,
        CategoryOptionCombo attributeOptionCombo, User user, Date now );

    /**
     * Checks whether the system is locked for data entry for the given input,
     * checking expiry days, lock exceptions and data approvals.
     *
     * @param dataSet the data set
     * @param period the period.
     * @param organisationUnit the organisation unit.
     * @param attributeOptionCombo the attribute option combo.
     * @param user the user for deciding lock status.
     * @param now the base date for deciding locked date, current date if null.
     * @param useOrgUnitChildren whether to check children of the given org unit
     *        or the org unit only.
     * @return the {@link LockStatus}.
     */
    LockStatus getLockStatus( DataSet dataSet, Period period, OrganisationUnit organisationUnit,
        CategoryOptionCombo attributeOptionCombo, User user, Date now, boolean useOrgUnitChildren );

    /**
     * Checks whether the system is locked for data entry for the given input,
     * checking expiry days, lock exceptions and data approvals.
     *
     * @param dataElement the data element.
     * @param period the period.
     * @param organisationUnit the organisation unit.
     * @param attributeOptionCombo the attribute option combo.
     * @param user the user for deciding lock status.
     * @param now the base date for deciding locked date, current date if null.
     * @return the {@link LockStatus}.
     */
    LockStatus getLockStatus( DataElement dataElement, Period period, OrganisationUnit organisationUnit,
        CategoryOptionCombo attributeOptionCombo, User user, Date now );

    /**
     * Deletes a dataSet and period combination, used for batch removal, e.g.
     * when you have a lock exception set on many org units with the same data
     * set and period combination.
     *
     * @param dataSet DataSet part of the combination
     * @param period Period part of the combination
     */
    void deleteLockExceptionCombination( DataSet dataSet, Period period );

    /**
     * Delete a data set, period and organisation unit combination
     *
     * @param dataSet the data set part of the combination
     * @param period the period part of the combination
     * @param organisationUnit the organisationUnit part of the combination
     */
    void deleteLockExceptionCombination( DataSet dataSet, Period period, OrganisationUnit organisationUnit );

    /**
     * Delete lock exceptions for the given organisation unit.
     *
     * @param organisationUnit the {@link OrganisationUnit}.
     */
    void deleteLockExceptions( OrganisationUnit organisationUnit );

    /**
     * Checks whether the period is locked for data entry for the given input,
     * checking the dataset's expiryDays and lockExceptions.
     *
     * @param dataSet the data set
     * @param period the period.
     * @param organisationUnit the organisation unit.
     * @param now the base date for deciding locked date, current date if null.
     * @return true or false indicating whether the system is locked or not.
     */
    boolean isLocked( User user, DataSet dataSet, Period period, OrganisationUnit organisationUnit, Date now );

    /**
     * Return a list of LockException with given filter list
     *
     * @param filters
     * @return a list of LockException with given filter list
     */
    List<LockException> filterLockExceptions( List<String> filters );

    /**
     * Return a mapping between the given data sets and the associated
     * organisation units. Only data sets for which the current user has data
     * write sharing access to are returned.
     *
     * @param dataSetUids the data set identifiers.
     * @return a {@link SetValuedMap} between data sets and organisation unit
     *         identifiers.
     */
    SetValuedMap<String, String> getDataSetOrganisationUnitsAssociations();
}
