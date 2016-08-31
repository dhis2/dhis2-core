package org.hisp.dhis.interpretation.impl;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationComment;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.interpretation.InterpretationStore;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultInterpretationService
    implements InterpretationService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private InterpretationStore interpretationStore;

    public void setInterpretationStore( InterpretationStore interpretationStore )
    {
        this.interpretationStore = interpretationStore;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    // -------------------------------------------------------------------------
    // InterpretationService implementation
    // -------------------------------------------------------------------------

    @Override
    public int saveInterpretation( Interpretation interpretation )
    {
        User user = currentUserService.getCurrentUser();

        if ( interpretation != null )
        {
            if ( user != null )
            {
                interpretation.setUser( user );
            }

            if ( interpretation.getPeriod() != null )
            {
                interpretation.setPeriod( periodService.reloadPeriod( interpretation.getPeriod() ) );
            }

            interpretation.updateSharing();
        }

        return interpretationStore.save( interpretation );
    }

    @Override
    public Interpretation getInterpretation( int id )
    {
        return interpretationStore.get( id );
    }

    @Override
    public Interpretation getInterpretation( String uid )
    {
        return interpretationStore.getByUid( uid );
    }

    @Override
    public void updateInterpretation( Interpretation interpretation )
    {
        interpretationStore.update( interpretation );
    }

    @Override
    public void deleteInterpretation( Interpretation interpretation )
    {
        interpretationStore.delete( interpretation );
    }

    @Override
    public List<Interpretation> getInterpretations()
    {
        return interpretationStore.getAll();
    }

    @Override
    public List<Interpretation> getInterpretations( Date lastUpdated )
    {
        return interpretationStore.getAllGeLastUpdated( lastUpdated );
    }

    @Override
    public List<Interpretation> getInterpretations( int first, int max )
    {
        return interpretationStore.getAllOrderedLastUpdated( first, max );
    }

    @Override
    public InterpretationComment addInterpretationComment( String uid, String text )
    {
        Interpretation interpretation = getInterpretation( uid );

        User user = currentUserService.getCurrentUser();

        InterpretationComment comment = new InterpretationComment( text );
        comment.setLastUpdated( new Date() );
        comment.setUid( CodeGenerator.generateCode() );

        if ( user != null )
        {
            comment.setUser( user );
        }

        interpretation.addComment( comment );

        interpretationStore.update( interpretation );

        return comment;
    }

    @Override
    public void updateCurrentUserLastChecked()
    {
        User user = currentUserService.getCurrentUser();

        user.setLastCheckedInterpretations( new Date() );

        userService.updateUser( user );
    }

    @Override
    public long getNewInterpretationCount()
    {
        User user = currentUserService.getCurrentUser();

        long count = 0;

        if ( user != null && user.getLastCheckedInterpretations() != null )
        {
            count = interpretationStore.getCountGeLastUpdated( user.getLastCheckedInterpretations() );
        }
        else
        {
            count = interpretationStore.getCount();
        }

        return count;
    }

    @Override
    public int countMapInterpretations( Map map )
    {
        return interpretationStore.countMapInterpretations( map );
    }

    @Override
    public int countChartInterpretations( Chart chart )
    {
        return interpretationStore.countChartInterpretations( chart );
    }

    @Override
    public int countReportTableInterpretations( ReportTable reportTable )
    {
        return interpretationStore.countReportTableInterpretations( reportTable );
    }

    @Override
    public Interpretation getInterpretationByChartId( int id )
    {
        return interpretationStore.getByChartId( id );
    }
}
