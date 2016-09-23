package org.hisp.dhis.datastatistics.hibernate;

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

import org.hisp.dhis.analytics.SortOrder;

import org.hisp.dhis.datastatistics.DataStatisticsEvent;
import org.hisp.dhis.datastatistics.DataStatisticsEventStore;
import org.hisp.dhis.datastatistics.DataStatisticsEventType;
import org.hisp.dhis.datastatistics.FavoriteStatistics;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.system.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
public class HibernateDataStatisticsEventStore
    extends HibernateGenericStore<DataStatisticsEvent>
    implements DataStatisticsEventStore
{
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<DataStatisticsEventType, Double> getDataStatisticsEventCount( Date startDate, Date endDate )
    {
        Map<DataStatisticsEventType, Double> eventTypeCountMap = new HashMap<>();
        
        final String sql = "select eventtype as eventtype, count(eventtype) as numberofviews from datastatisticsevent where (timestamp between '" +
            DateUtils.getMediumDateString( startDate ) + "' and '" +
            DateUtils.getMediumDateString( endDate ) + "') group by eventtype;";

        final String totalSql = "select count(eventtype) as total from datastatisticsevent where (timestamp between '" + 
            DateUtils.getMediumDateString( startDate ) + "' and '" +
            DateUtils.getMediumDateString( endDate ) + "');";

        jdbcTemplate.query( sql, ( resultSet, i ) -> {
            eventTypeCountMap.put( DataStatisticsEventType.valueOf( resultSet.getString( "eventtype" ) ), resultSet.getDouble( "numberofviews" ) );
            return eventTypeCountMap;
        } );

        jdbcTemplate.query( totalSql, ( resultSet, i ) -> {
            return eventTypeCountMap.put( DataStatisticsEventType.TOTAL_VIEW, resultSet.getDouble( "total" ) );
        } );
        
        return eventTypeCountMap;
    }

    @Override
    public List<FavoriteStatistics> getFavoritesData( DataStatisticsEventType eventType, int pageSize, SortOrder sortOrder, String username )
    {
        Assert.notNull( eventType );
        Assert.notNull( sortOrder );

        String sql =
            "select c.uid, views, c.name, c.created from ( " +
                "select favoriteuid as uid, count(favoriteuid) as views " +
                "from datastatisticsevent ";

        if ( username != null )
        {
            sql += "where username = '" + username + "' ";
        }

        sql +=
            "group by uid) as events " +
            "inner join " + eventType.getTable() + " c on c.uid = events.uid " +
            "order by events.views " + sortOrder.getValue() + " " +
            "limit " + pageSize;

        return jdbcTemplate.query( sql, ( resultSet, i ) -> {
            FavoriteStatistics favoriteStatistics = new FavoriteStatistics();

            favoriteStatistics.setPosition( i + 1 );
            favoriteStatistics.setId( resultSet.getString( "uid" ) );
            favoriteStatistics.setName( resultSet.getString( "name" ) );
            favoriteStatistics.setCreated( resultSet.getDate( "created" ) );
            favoriteStatistics.setViews( resultSet.getInt( "views" ) );

            return favoriteStatistics;
        } );
    }

    @Override
    public FavoriteStatistics getFavoriteStatistics( String uid )
    {
        String sql = "select count(dse.favoriteuid) " +
            "from datastatisticsevent dse " +
            "where dse.favoriteuid = '" + uid + "';";
        
        Integer views = jdbcTemplate.queryForObject( sql, Integer.class );
        
        FavoriteStatistics stats = new FavoriteStatistics();
        stats.setViews( views );
        
        return stats;
    }
}

