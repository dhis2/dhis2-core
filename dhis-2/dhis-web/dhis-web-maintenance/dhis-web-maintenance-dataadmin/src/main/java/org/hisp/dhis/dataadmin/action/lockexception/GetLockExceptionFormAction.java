package org.hisp.dhis.dataadmin.action.lockexception;

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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.oust.manager.SelectionTreeManager;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.filter.PastAndCurrentPeriodFilter;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserCredentials;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class GetLockExceptionFormAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private SelectionTreeManager selectionTreeManager;

    public void setSelectionTreeManager( SelectionTreeManager selectionTreeManager )
    {
        this.selectionTreeManager = selectionTreeManager;
    }

    private I18nFormat format;

    public void setFormat( I18nFormat format )
    {
        this.format = format;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private Integer id;

    public void setId( Integer id )
    {
        this.id = id;
    }

    private LockException lockException;

    public LockException getLockException()
    {
        return lockException;
    }

    private List<DataSet> dataSets;

    public List<DataSet> getDataSets()
    {
        return dataSets;
    }

    private List<Period> periods;

    public List<Period> getPeriods()
    {
        return periods;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute() throws Exception
    {
        if ( id != null )
        {
            lockException = dataSetService.getLockException( id );

            if ( lockException == null )
            {
                return INPUT;
            }

            selectionTreeManager.setSelectedOrganisationUnit( lockException.getOrganisationUnit() );
            dataSets = getDataSetsForCurrentUser( lockException.getOrganisationUnit().getId() );
            periods = getPeriodsForDataSet( lockException.getDataSet().getId() );

            for ( Period period : periods )
            {
                period.setName( format.formatPeriod( period ) );
            }
        }

        return SUCCESS;
    }

    private List<Period> getPeriodsForDataSet( int id )
    {
        DataSet dataSet = dataSetService.getDataSet( id );

        if ( dataSet == null )
        {
            return new ArrayList<>();
        }

        CalendarPeriodType periodType = (CalendarPeriodType) dataSet.getPeriodType();
        List<Period> periods = periodType.generateLast5Years( new Date() );
        FilterUtils.filter( periods, new PastAndCurrentPeriodFilter() );
        Collections.reverse( periods );

        if ( periods.size() > 10 )
        {
            periods = periods.subList( 0, 10 );
        }

        return periods;
    }

    private List<DataSet> getDataSetsForCurrentUser( int id )
    {
        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( id );

        if ( organisationUnit == null )
        {
            return new ArrayList<>();
        }

        List<DataSet> dataSets = new ArrayList<>();

        if ( organisationUnit.getDataSets() != null )
        {
            dataSets.addAll( organisationUnit.getDataSets() );
        }

        UserCredentials userCredentials = currentUserService.getCurrentUser().getUserCredentials();

        if ( !userCredentials.isSuper() )
        {
            dataSets.retainAll( userCredentials.getAllDataSets() );
        }

        return dataSets;
    }
}
