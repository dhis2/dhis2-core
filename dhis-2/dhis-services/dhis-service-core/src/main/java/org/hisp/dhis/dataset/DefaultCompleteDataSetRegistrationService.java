package org.hisp.dhis.dataset;

import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Map4;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.notifications.DataSetNotificationEventPublisher;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
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
    private PeriodService periodService;

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
            registration.setAttributeOptionCombo( categoryService.getDefaultDataElementCategoryOptionCombo() );
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
        OrganisationUnit source, DataElementCategoryOptionCombo attributeOptionCombo )
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
        OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo, boolean multiOrgUnit )
    {
        List<DataElementOperand> missingDataElementOperands = new ArrayList<>();

        if ( !dataSet.getCompulsoryDataElementOperands().isEmpty() )
        {
            Period reloadedPeriod = periodService.reloadPeriod( period );

            List<Period> periods = new ArrayList<>();
            periods.add( reloadedPeriod );

            List<OrganisationUnit> organisationUnits = new ArrayList<>();

            if ( multiOrgUnit )
            {
                organisationUnits.addAll( organisationUnit.getChildren() );
            }
            else
            {
                organisationUnits.add( organisationUnit );
            }

            Map4<OrganisationUnit, Period, String, DimensionalItemObject, Double> dataValues = new Map4<>();

            dataValues = dataValueService.getDataElementOperandValues( dataSet.getCompulsoryDataElementOperands(),
                periods, organisationUnits );

            if ( dataValues.isEmpty() )
            {
                missingDataElementOperands.addAll( dataSet.getCompulsoryDataElementOperands() );
            }
            else
            {
                User currentUser = currentUserService.getCurrentUser();

                for ( DataElementOperand dataElementOperand : dataSet.getCompulsoryDataElementOperands() )
                {
                    List<String> errors = accessManager.canWrite( currentUser, dataElementOperand );

                    if ( !errors.isEmpty() )
                    {
                        continue;
                    }

                    if ( multiOrgUnit )
                    {
                        for ( OrganisationUnit child : organisationUnit.getChildren() )
                        {
                            if ( dataValues.getValue( child, period, attributeOptionCombo.getUid(),
                                dataElementOperand ) == null )
                            {
                                missingDataElementOperands.add( dataElementOperand );
                            }
                        }
                    }
                    else
                    {
                        if ( dataValues.getValue( organisationUnit, period, attributeOptionCombo.getUid(), dataElementOperand ) == null )
                        {
                            missingDataElementOperands.add( dataElementOperand );
                        }
                    }
                }
            }

        }

        return missingDataElementOperands;
    }
}
