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
package org.hisp.dhis.period;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Kristian Nordal
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.period.PeriodService" )
public class DefaultPeriodService
    implements PeriodService
{
    private final PeriodStore periodStore;

    // -------------------------------------------------------------------------
    // Period
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addPeriod( Period period )
    {
        periodStore.addPeriod( period );
        return period.getId();
    }

    @Override
    @Transactional
    public void deletePeriod( Period period )
    {
        periodStore.delete( period );
    }

    @Override
    @Transactional( readOnly = true )
    public Period getPeriod( long id )
    {
        return periodStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public Period getPeriod( String isoPeriod )
    {
        Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

        if ( period != null )
        {
            period = periodStore.getPeriod( period.getStartDate(), period.getEndDate(), period.getPeriodType() );
        }

        return period;
    }

    @Override
    @Transactional( readOnly = true )
    public Period getPeriod( Date startDate, Date endDate, PeriodType periodType )
    {
        return periodStore.getPeriod( startDate, endDate, periodType );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Period> getAllPeriods()
    {
        return periodStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public List<Period> getPeriodsByPeriodType( PeriodType periodType )
    {
        return periodStore.getPeriodsByPeriodType( periodType );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Period> getPeriodsBetweenDates( Date startDate, Date endDate )
    {
        return periodStore.getPeriodsBetweenDates( startDate, endDate );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Period> getPeriodsBetweenDates( PeriodType periodType, Date startDate, Date endDate )
    {
        return periodStore.getPeriodsBetweenDates( periodType, startDate, endDate );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Period> getPeriodsBetweenOrSpanningDates( Date startDate, Date endDate )
    {
        return periodStore.getPeriodsBetweenOrSpanningDates( startDate, endDate );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Period> getIntersectingPeriods( Date startDate, Date endDate )
    {
        return periodStore.getIntersectingPeriods( startDate, endDate );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Period> getIntersectionPeriods( Collection<Period> periods )
    {
        Set<Period> intersecting = new HashSet<>();

        for ( Period period : periods )
        {
            intersecting.addAll( getIntersectingPeriods( period.getStartDate(), period.getEndDate() ) );
        }

        return new ArrayList<>( intersecting );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Period> getInclusivePeriods( Period period, Collection<Period> periods )
    {
        List<Period> immutablePeriods = new ArrayList<>( periods );

        Iterator<Period> iterator = immutablePeriods.iterator();

        while ( iterator.hasNext() )
        {
            Period iterated = iterator.next();

            if ( !DateUtils.between( iterated.getStartDate(), period.getStartDate(), period.getEndDate() )
                || !DateUtils.between( iterated.getEndDate(), period.getStartDate(), period.getEndDate() ) )
            {
                iterator.remove();
            }
        }

        return immutablePeriods;
    }

    @Override
    @Transactional
    public List<Period> reloadPeriods( Collection<Period> periods )
    {
        List<Period> reloaded = new ArrayList<>();

        for ( Period period : periods )
        {
            reloaded.add( periodStore.reloadForceAddPeriod( period ) );
        }

        return reloaded;
    }

    @Override
    @Transactional
    public List<Period> getPeriods( Period lastPeriod, int previousPeriods )
    {
        List<Period> periods = new ArrayList<>( previousPeriods );

        lastPeriod = periodStore.reloadForceAddPeriod( lastPeriod );

        PeriodType periodType = lastPeriod.getPeriodType();

        for ( int i = 0; i < previousPeriods; ++i )
        {
            Period pe = getPeriodFromDates( lastPeriod.getStartDate(), lastPeriod.getEndDate(), periodType );

            periods.add( pe != null ? pe : lastPeriod );

            lastPeriod = periodType.getPreviousPeriod( lastPeriod );
        }

        Collections.reverse( periods );

        return periods;
    }

    @Override
    @Transactional( readOnly = true )
    public List<Period> namePeriods( Collection<Period> periods, I18nFormat format )
    {
        for ( Period period : periods )
        {
            period.setName( format.formatPeriod( period ) );
        }

        return new ArrayList<>( periods );
    }

    @Override
    @Transactional( readOnly = true )
    public Period getPeriodFromDates( Date startDate, Date endDate, PeriodType periodType )
    {
        return periodStore.getPeriodFromDates( startDate, endDate, periodType );
    }

    @Override
    @Transactional
    public Period reloadPeriod( Period period )
    {
        return periodStore.reloadForceAddPeriod( period );
    }

    /**
     * Fix issue DHIS2-7539 If period doesn't exist in cache and database. Need
     * to add and sync with database right away in a separate
     * session/transaction. Otherwise will get foreign key constraint error in
     * subsequence calls of batch.flush()
     **/
    @Override
    @Transactional( readOnly = true )
    public Period reloadIsoPeriodInStatelessSession( String isoPeriod )
    {
        Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

        if ( period == null )
        {
            return null;
        }

        Period reloadedPeriod = periodStore.reloadPeriod( period );

        if ( reloadedPeriod != null )
        {
            return reloadedPeriod;
        }

        period.setPeriodType( reloadPeriodType( period.getPeriodType() ) );

        return periodStore.insertIsoPeriodInStatelessSession( period );
    }

    @Override
    @Transactional
    public Period reloadIsoPeriod( String isoPeriod )
    {
        Period period = PeriodType.getPeriodFromIsoString( isoPeriod );

        return period != null ? reloadPeriod( period ) : null;
    }

    @Override
    @Transactional
    public List<Period> reloadIsoPeriods( List<String> isoPeriods )
    {
        List<Period> periods = new ArrayList<>();

        for ( String iso : isoPeriods )
        {
            Period period = reloadIsoPeriod( iso );

            if ( period != null )
            {
                periods.add( period );
            }
        }

        return periods;
    }

    @Override
    @Transactional( readOnly = true )
    public PeriodHierarchy getPeriodHierarchy( Collection<Period> periods )
    {
        PeriodHierarchy hierarchy = new PeriodHierarchy();

        for ( Period period : periods )
        {
            hierarchy.getIntersectingPeriods().put( period.getId(), new HashSet<>(
                getIdentifiers( getIntersectingPeriods( period.getStartDate(), period.getEndDate() ) ) ) );
            hierarchy.getPeriodsBetween().put( period.getId(), new HashSet<>(
                getIdentifiers( getPeriodsBetweenDates( period.getStartDate(), period.getEndDate() ) ) ) );
        }

        return hierarchy;
    }

    @Override
    @Transactional( readOnly = true )
    public int getDayInPeriod( Period period, Date date )
    {
        int days = (int) TimeUnit.DAYS.convert( date.getTime() - period.getStartDate().getTime(),
            TimeUnit.MILLISECONDS );

        return Math.min( Math.max( 0, days ), period.getDaysInPeriod() );
    }

    // -------------------------------------------------------------------------
    // PeriodType
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public PeriodType getPeriodType( int id )
    {
        return periodStore.getPeriodType( id );
    }

    @Override
    @Transactional( readOnly = true )
    public PeriodType getPeriodTypeByName( String name )
    {
        return PeriodType.getPeriodTypeByName( name );
    }

    @Override
    @Transactional( readOnly = true )
    public PeriodType getPeriodTypeByClass( Class<? extends PeriodType> periodType )
    {
        return periodStore.getPeriodType( periodType );
    }

    @Override
    @Transactional( readOnly = true )
    public PeriodType reloadPeriodType( PeriodType periodType )
    {
        return periodStore.reloadPeriodType( periodType );
    }

    // -------------------------------------------------------------------------
    // PeriodType
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteRelativePeriods( RelativePeriods relativePeriods )
    {
        periodStore.deleteRelativePeriods( relativePeriods );
    }
}
