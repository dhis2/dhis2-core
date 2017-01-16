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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AddLockExceptionAction
    implements Action
{
    private static final Log log = LogFactory.getLog( AddLockExceptionAction.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

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

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private String organisationUnitId;

    public void setOrganisationUnitId( String organisationUnitId )
    {
        this.organisationUnitId = organisationUnitId;
    }

    private int dataSetId;

    public void setDataSetId( int dataSetId )
    {
        this.dataSetId = dataSetId;
    }

    private String periodId;

    public void setPeriodId( String periodId )
    {
        this.periodId = periodId;
    }

    // -------------------------------------------------------------------------
    // Action Implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute() throws Exception
    {
        if ( organisationUnitId.length() == 0 )
        {
            return INPUT;
        }

        DataSet dataSet = dataSetService.getDataSet( dataSetId );
        Period period = periodService.reloadPeriod( PeriodType.getPeriodFromIsoString( periodId ) );

        if ( dataSet == null || period == null )
        {
            return ERROR;
        }

        for ( String id : organisationUnitId.split( "," ) )
        {
            OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( id );

            if ( organisationUnit == null )
            {
                return ERROR;
            }

            if ( organisationUnit.getDataSets().contains( dataSet ) )
            {
                LockException lockException = new LockException();

                lockException.setOrganisationUnit( organisationUnit );
                lockException.setDataSet( dataSet );
                lockException.setPeriod( period );
                dataSetService.addLockException( lockException );
            }
            else
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "OrganisationUnit " + organisationUnit.getName() + " does not contain DataSet " +
                        dataSet.getName() + ", ignoring." );
                }
            }
        }

        return SUCCESS;
    }
}
