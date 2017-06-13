package org.hisp.dhis.dataset;

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

import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

import java.util.Collection;
import java.util.List;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public interface CompleteDataSetRegistrationService
{
    String ID = CompleteDataSetRegistrationService.class.getName();

    /**
     * Saves a CompleteDataSetRegistration.
     *
     * @param registration the CompleteDataSetRegistration to save.
     */
    void saveCompleteDataSetRegistration( CompleteDataSetRegistration registration );

    /**
     * Saves a CompleteDataSetRegistration.
     *
     * @param registration the CompleteDataSetRegistration to save.
     * @param notify       indicates whether a notification should be sent.
     */
    void saveCompleteDataSetRegistration( CompleteDataSetRegistration registration, boolean notify );

    /**
     * Saves multiple CompleteDataSetRegistration.
     *
     * @param registrations List of CompleteDataSetRegistrations to save.
     * @param notify        indicates whether a notification should be sent.
     */
    void saveCompleteDataSetRegistrations( List<CompleteDataSetRegistration> registrations, boolean notify );

    /**
     * Updates a CompleteDataSetRegistration.
     *
     * @param registration the CompleteDataSetRegistration to update.
     */
    void updateCompleteDataSetRegistration( CompleteDataSetRegistration registration );

    /**
     * Retrieves the CompleteDataSetRegistration for the given DataSet, Period
     * and Source.
     *
     * @param dataSet              the DataSet.
     * @param period               the Period.
     * @param source               the Source.
     * @param attributeOptionCombo the attribute option combo.
     * @return the CompleteDataSetRegistration.
     */
    CompleteDataSetRegistration getCompleteDataSetRegistration( DataSet dataSet, Period period,
        OrganisationUnit source, DataElementCategoryOptionCombo attributeOptionCombo );

    /**
     * Deletes a CompleteDataSetRegistration.
     *
     * @param registration the CompleteDataSetRegistration to delete.
     */
    void deleteCompleteDataSetRegistration( CompleteDataSetRegistration registration );

    /**
     * Deletes multiple CompleteDataSetRegistration.
     *
     * @param registrations the list of CompleteDataSetRegistration to delete.
     */
    void deleteCompleteDataSetRegistrations( List<CompleteDataSetRegistration> registrations );

    /**
     * Retrieves all CompleteDataSetRegistrations.
     *
     * @return a list of CompleteDataSetRegistrations.
     */
    List<CompleteDataSetRegistration> getAllCompleteDataSetRegistrations();

    /**
     * Retrieves the CompleteDataSetRegistrations for the given Collections of
     * DataSets, Sources and Periods.
     *
     * @param dataSets the Collection of DataSets.
     * @param sources  the Collection of Sources.
     * @param periods  the Collection of Periods.
     * @return a list of CompleteDataSetRegistrations.
     */
    List<CompleteDataSetRegistration> getCompleteDataSetRegistrations(
        Collection<DataSet> dataSets, Collection<OrganisationUnit> sources, Collection<Period> periods );

    /**
     * Deletes the CompleteDataSetRegistrations associated with the given DataSet.
     *
     * @param dataSet the DataSet.
     */
    void deleteCompleteDataSetRegistrations( DataSet dataSet );

    /**
     * Deletes the CompleteDataSetRegistrations associated with the given OrganisationUnit.
     *
     * @param unit the OrganisationUnit.
     */
    void deleteCompleteDataSetRegistrations( OrganisationUnit unit );
}
