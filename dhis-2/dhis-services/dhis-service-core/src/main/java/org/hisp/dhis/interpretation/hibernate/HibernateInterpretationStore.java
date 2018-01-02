package org.hisp.dhis.interpretation.hibernate;

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

import org.hibernate.Query;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.interpretation.Interpretation;
import org.hisp.dhis.interpretation.InterpretationStore;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.user.User;

import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class HibernateInterpretationStore
    extends HibernateIdentifiableObjectStore<Interpretation> implements InterpretationStore
{
    @SuppressWarnings("unchecked")
    public List<Interpretation> getInterpretations( User user )
    {
        String hql = "select distinct i from Interpretation i left join i.comments c " +
            "where i.user = :user or c.user = :user order by i.lastUpdated desc";

        Query query = getQuery( hql );
        query.setEntity( "user", user );

        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<Interpretation> getInterpretations( User user, int first, int max )
    {
        String hql = "select distinct i from Interpretation i left join i.comments c " +
            "where i.user = :user or c.user = :user order by i.lastUpdated desc";

        Query query = getQuery( hql );
        query.setEntity( "user", user );
        query.setMaxResults( first );
        query.setMaxResults( max );

        return query.list();
    }

    @Override
    public int countMapInterpretations( Map map )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c where c.map=:map" );
        query.setEntity( "map", map );

        return ((Long) query.uniqueResult()).intValue();
    }

    @Override
    public int countChartInterpretations( Chart chart )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c where c.chart=:chart" );
        query.setEntity( "chart", chart );

        return ((Long) query.uniqueResult()).intValue();
    }

    @Override
    public int countReportTableInterpretations( ReportTable reportTable )
    {
        Query query = getQuery( "select count(distinct c) from " + clazz.getName() + " c where c.reportTable=:reportTable" );
        query.setEntity( "reportTable", reportTable );

        return ((Long) query.uniqueResult()).intValue();
    }

    @Override
    public Interpretation getByChartId( int id )
    {
        String hql = "from Interpretation i where i.chart.id = " + id;
        
        Query query = getSession().createQuery( hql );
        
        return (Interpretation) query.uniqueResult();
    }
}
