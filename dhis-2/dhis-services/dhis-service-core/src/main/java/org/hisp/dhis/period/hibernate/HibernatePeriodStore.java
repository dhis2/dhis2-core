package org.hisp.dhis.period.hibernate;

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

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.RelativePeriods;
import org.springframework.beans.factory.annotation.Autowired;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Implements the PeriodStore interface.
 * 
 * @author Torgeir Lorange Ostby
 * @version $Id: HibernatePeriodStore.java 5983 2008-10-17 17:42:44Z larshelg $
 */
public class HibernatePeriodStore
    extends HibernateIdentifiableObjectStore<Period>
    implements PeriodStore
{
    private static Cache<Integer> PERIOD_ID_CACHE;

    @Autowired
    private CacheProvider cacheProvider;
    
    @PostConstruct
    public void init()
    {

        PERIOD_ID_CACHE = cacheProvider.newCacheBuilder( Integer.class )
            .forRegion( "periodIdCache" )
            .expireAfterWrite( 24, TimeUnit.HOURS )
            .withInitialCapacity( 200 )
            .forceInMemory()
            .withMaximumSize( SystemUtils.isTestRun() ? 0 : 10000 )
            .build();
    }
    
    // -------------------------------------------------------------------------
    // Period
    // -------------------------------------------------------------------------

    @Override
    public void addPeriod( Period period )
    {
        period.setPeriodType( reloadPeriodType( period.getPeriodType() ) );

        save( period );
    }

    @Override
    public Period getPeriod( Date startDate, Date endDate, PeriodType periodType )
    {
        String query = "from Period p where p.startDate =:startDate and p.endDate =:endDate and p.periodType =:periodType";

        return getSingleResult( getQuery( query )
            .setParameter( "startDate", startDate )
            .setParameter( "endDate", endDate )
            .setParameter( "periodType", reloadPeriodType( periodType ) ) );
    }

    @Override
    public List<Period> getPeriodsBetweenDates( Date startDate, Date endDate )
    {
        String query = "from Period p where p.startDate >=:startDate and p.endDate <=:endDate";

        Query<Period> typedQuery = getQuery( query )
            .setParameter( "startDate", startDate )
            .setParameter( "endDate", endDate );
        return getList( typedQuery );
    }

    @Override
    public List<Period> getPeriodsBetweenDates( PeriodType periodType, Date startDate, Date endDate )
    {
        String query = "from Period p where p.startDate >=:startDate and p.endDate <=:endDate and p.periodType.id =:periodType";

        Query<Period> typedQuery = getQuery( query )
            .setParameter( "startDate", startDate )
            .setParameter( "endDate", endDate )
            .setParameter( "periodType", reloadPeriodType( periodType ).getId() );
        return getList( typedQuery );
    }

    @Override
    public List<Period> getPeriodsBetweenOrSpanningDates( Date startDate, Date endDate )
    {
        String hql = "from Period p where ( p.startDate >= :startDate and p.endDate <= :endDate ) or ( p.startDate <= :startDate and p.endDate >= :endDate )";
        
        return getQuery( hql ).setParameter( "startDate", startDate ).setParameter( "endDate", endDate ).list();
    }
    
    @Override
    public List<Period> getIntersectingPeriodsByPeriodType( PeriodType periodType, Date startDate, Date endDate )
    {
        String query = "from Period p where p.startDate <=:endDate and p.endDate >=:startDate and p.periodType.id =:periodType";

        Query<Period> typedQuery = getQuery( query )
            .setParameter( "startDate", startDate )
            .setParameter( "endDate", endDate )
            .setParameter( "periodType", reloadPeriodType( periodType ).getId() );
        return getList( typedQuery );
    }

    @Override
    public List<Period> getIntersectingPeriods( Date startDate, Date endDate )
    {
        String query = "from Period p where p.startDate <=:endDate and p.endDate >=:startDate";

        Query<Period> typedQuery = getQuery( query )
            .setParameter( "startDate", startDate )
            .setParameter( "endDate", endDate );
        return getList( typedQuery );
    }

    @Override
    public List<Period> getPeriodsByPeriodType( PeriodType periodType )
    {
        String query = "from Period p where p.periodType.id =:periodType";

        Query<Period> typedQuery = getQuery( query )
            .setParameter( "periodType", reloadPeriodType( periodType ).getId() );
        return getList( typedQuery );
    }

    @Override
    public Period getPeriodFromDates( Date startDate, Date endDate, PeriodType periodType )
    {
        String query = "from Period p where p.startDate =:startDate and p.endDate =:endDate and p.periodType.id =:periodType";

        Query<Period> typedQuery = getQuery( query )
            .setParameter( "startDate", startDate )
            .setParameter( "endDate", endDate )
            .setParameter( "periodType", reloadPeriodType( periodType ).getId() );
        return getSingleResult( typedQuery );
    }

    @Override
    public Period reloadPeriod( Period period )
    {
        Session session = sessionFactory.getCurrentSession();

        if ( session.contains( period ) )
        {
            return period; // Already in session, no reload needed
        }

        Integer id = PERIOD_ID_CACHE.get( period.getCacheKey(), key -> getPeriodId( period.getStartDate(), period.getEndDate(), period.getPeriodType() ) )
            .orElse( null );  
        
        Period storedPeriod = id != null ? getSession().get( Period.class, id ) : null;

        return storedPeriod != null ? storedPeriod.copyTransientProperties( period ) : null;
    }

    private Integer getPeriodId( Date startDate, Date endDate, PeriodType periodType )
    {
        Period period = getPeriod( startDate, endDate, periodType );
        
        return period != null ? period.getId() : null;
    }
    
    @Override
    public Period reloadForceAddPeriod( Period period )
    {
        Period storedPeriod = reloadPeriod( period );

        if ( storedPeriod == null )
        {
            addPeriod( period );

            return period;
        }

        return storedPeriod;
    }

    // -------------------------------------------------------------------------
    // PeriodType (do not use generic store which is linked to Period)
    // -------------------------------------------------------------------------

    @Override
    public int addPeriodType( PeriodType periodType )
    {
        Session session = sessionFactory.getCurrentSession();

        return (Integer) session.save( periodType );
    }

    @Override
    public void deletePeriodType( PeriodType periodType )
    {
        Session session = sessionFactory.getCurrentSession();

        session.delete( periodType );
    }

    @Override
    public PeriodType getPeriodType( int id )
    {
        Session session = sessionFactory.getCurrentSession();

        return session.get( PeriodType.class, id );
    }

    @Override
    public PeriodType getPeriodType( Class<? extends PeriodType> periodType )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<PeriodType> query = builder.createQuery( PeriodType.class );
        query.select( query.from( periodType ) );

        return getSession().createQuery( query ).setCacheable( true ).uniqueResult();
    }

    @Override
    public List<PeriodType> getAllPeriodTypes()
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        CriteriaQuery<PeriodType> query = builder.createQuery( PeriodType.class );
        query.select( query.from( PeriodType.class ) );

        return getSession().createQuery( query ).setCacheable( true ).getResultList();
    }

    @Override
    public PeriodType reloadPeriodType( PeriodType periodType )
    {
        Session session = sessionFactory.getCurrentSession();

        if ( periodType == null || session.contains( periodType ) )
        {
            return periodType;
        }

        PeriodType reloadedPeriodType = getPeriodType( periodType.getClass() );

        if ( reloadedPeriodType == null )
        {
            throw new InvalidIdentifierReferenceException( "The PeriodType referenced by the Period is not in database: "
                + periodType.getName() );
        }

        return reloadedPeriodType;
    }

    // -------------------------------------------------------------------------
    // RelativePeriods (do not use generic store which is linked to Period)
    // -------------------------------------------------------------------------

    @Override
    public void deleteRelativePeriods( RelativePeriods relativePeriods )
    {
        sessionFactory.getCurrentSession().delete( relativePeriods );
    }
}
