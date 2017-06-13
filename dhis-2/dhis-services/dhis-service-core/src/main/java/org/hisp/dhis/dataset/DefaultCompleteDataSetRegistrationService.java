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
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
@Transactional
public class DefaultCompleteDataSetRegistrationService
    implements CompleteDataSetRegistrationService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CompleteDataSetRegistrationStore completeDataSetRegistrationStore;

    public void setCompleteDataSetRegistrationStore( CompleteDataSetRegistrationStore completeDataSetRegistrationStore )
    {
        this.completeDataSetRegistrationStore = completeDataSetRegistrationStore;
    }

    private MessageService messageService;

    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    // -------------------------------------------------------------------------
    // CompleteDataSetRegistrationService
    // -------------------------------------------------------------------------

    @Override
    public void saveCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
        if ( registration.getAttributeOptionCombo() == null )
        {
            registration.setAttributeOptionCombo( categoryService.getDefaultDataElementCategoryOptionCombo() );
        }

        completeDataSetRegistrationStore.saveCompleteDataSetRegistration( registration );
    }

    @Override
    public void saveCompleteDataSetRegistration( CompleteDataSetRegistration registration, boolean notify )
    {
        saveCompleteDataSetRegistration( registration );

        if ( notify )
        {
            messageService.sendCompletenessMessage( registration );
        }
    }

    @Override
    public void saveCompleteDataSetRegistrations( List<CompleteDataSetRegistration> registrations, boolean notify )
    {
        for ( CompleteDataSetRegistration registration : registrations )
        {
            saveCompleteDataSetRegistration( registration, notify );
        }
    }

    @Override
    public void updateCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
        completeDataSetRegistrationStore.updateCompleteDataSetRegistration( registration );
    }

    @Override
    public void deleteCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
        completeDataSetRegistrationStore.deleteCompleteDataSetRegistration( registration );
    }

    @Override
    public void deleteCompleteDataSetRegistrations( List<CompleteDataSetRegistration> registrations )
    {
        for ( CompleteDataSetRegistration registration : registrations )
        {
            completeDataSetRegistrationStore.deleteCompleteDataSetRegistration( registration );
        }
    }

    @Override
    public CompleteDataSetRegistration getCompleteDataSetRegistration( DataSet dataSet, Period period,
        OrganisationUnit source, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        return completeDataSetRegistrationStore
            .getCompleteDataSetRegistration( dataSet, period, source, attributeOptionCombo );
    }

    @Override
    public List<CompleteDataSetRegistration> getAllCompleteDataSetRegistrations()
    {
        return completeDataSetRegistrationStore.getAllCompleteDataSetRegistrations();
    }

    @Override
    public List<CompleteDataSetRegistration> getCompleteDataSetRegistrations(
        Collection<DataSet> dataSets, Collection<OrganisationUnit> sources, Collection<Period> periods )
    {
        return completeDataSetRegistrationStore.getCompleteDataSetRegistrations( dataSets, sources, periods );
    }

    @Override
    public void deleteCompleteDataSetRegistrations( DataSet dataSet )
    {
        completeDataSetRegistrationStore.deleteCompleteDataSetRegistrations( dataSet );
    }

    @Override
    public void deleteCompleteDataSetRegistrations( OrganisationUnit unit )
    {
        completeDataSetRegistrationStore.deleteCompleteDataSetRegistrations( unit );
    }
}
