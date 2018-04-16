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

import com.google.common.collect.Sets;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.notifications.DataSetNotificationEventPublisher;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private CategoryService categoryService;

    public void setCategoryService( CategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataSetNotificationEventPublisher notificationEventPublisher;

    @Autowired
    private AggregateAccessManager accessManager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private MessageService messageService;

    // -------------------------------------------------------------------------
    // CompleteDataSetRegistrationService
    // -------------------------------------------------------------------------

    @Override
    public void saveCompleteDataSetRegistration( CompleteDataSetRegistration registration )
    {
        if ( registration.getAttributeOptionCombo() == null )
        {
            registration.setAttributeOptionCombo( categoryService.getDefaultCategoryOptionCombo() );
        }

        completeDataSetRegistrationStore.saveCompleteDataSetRegistration( registration );

        if ( registration.getDataSet().isNotifyCompletingUser() )
        {
            messageService.sendCompletenessMessage( registration );
        }
        
        notificationEventPublisher.publishEvent( registration );
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
        OrganisationUnit source, CategoryOptionCombo attributeOptionCombo )
    {
        return completeDataSetRegistrationStore.getCompleteDataSetRegistration( dataSet, period, source,
            attributeOptionCombo );
    }

    @Override
    public List<CompleteDataSetRegistration> getAllCompleteDataSetRegistrations()
    {
        return completeDataSetRegistrationStore.getAllCompleteDataSetRegistrations();
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

    @Override
    public List<DataElementOperand> getMissingCompulsoryFields( DataSet dataSet, Period period,
        OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo )
    {
        List<DataElementOperand> missingDataElementOperands = new ArrayList<>();

        if ( !dataSet.getCompulsoryDataElementOperands().isEmpty() )
        {
            DataExportParams params = new DataExportParams();
            params.setDataElementOperands( dataSet.getCompulsoryDataElementOperands() );
            params.setPeriods( Sets.newHashSet( period ) );
            params.setAttributeOptionCombos( Sets.newHashSet( attributeOptionCombo ) );
            params.setOrganisationUnits( Sets.newHashSet( organisationUnit ) );

            List<DeflatedDataValue> deflatedDataValues = dataValueService.getDeflatedDataValues( params );

            MapMapMap<Integer, Integer, Integer, Boolean> dataPresent = new MapMapMap<>();

            for ( DeflatedDataValue dv : deflatedDataValues )
            {
                dataPresent.putEntry( dv.getSourceId(), dv.getDataElementId(), dv.getCategoryOptionComboId(), true );
            }

            User currentUser = currentUserService.getCurrentUser();

            for ( DataElementOperand deo : dataSet.getCompulsoryDataElementOperands() )
            {
                List<String> errors = accessManager.canWrite( currentUser, deo );

                if ( !errors.isEmpty() )
                {
                    continue;
                }

                MapMap<Integer, Integer, Boolean> ouDataPresent = dataPresent.get( organisationUnit.getId() );

                if ( ouDataPresent != null )
                {
                    Map<Integer, Boolean> deDataPresent = ouDataPresent.get( deo.getDataElement().getId() );

                    if ( deDataPresent != null && ( deo.getCategoryOptionCombo() == null || deDataPresent.get( deo.getCategoryOptionCombo().getId() ) != null ) )
                    {
                        continue;
                    }
                }

                missingDataElementOperands.add( deo );
            }
        }

        return missingDataElementOperands;
    }
}
