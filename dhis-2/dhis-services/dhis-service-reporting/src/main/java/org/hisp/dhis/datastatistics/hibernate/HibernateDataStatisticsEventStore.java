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
package org.hisp.dhis.datastatistics.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.setting.SettingKey.COUNT_PASSIVE_DASHBOARD_VIEWS_IN_USAGE_ANALYTICS;
import static org.hisp.dhis.system.util.SqlUtils.escapeSql;
import static org.hisp.dhis.util.DateUtils.asSqlDate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.datastatistics.DataStatisticsEvent;
import org.hisp.dhis.datastatistics.DataStatisticsEventStore;
import org.hisp.dhis.datastatistics.DataStatisticsEventType;
import org.hisp.dhis.datastatistics.FavoriteStatistics;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
@Repository( "org.hisp.dhis.datastatistics.DataStatisticsEventStore" )
public class HibernateDataStatisticsEventStore
    extends HibernateGenericStore<DataStatisticsEvent>
    implements DataStatisticsEventStore
{
    private final SystemSettingManager systemSettingManager;

    private final UserSettingService userSettingService;

    public HibernateDataStatisticsEventStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, SystemSettingManager systemSettingManager,
        UserSettingService userSettingService )
    {
        super( sessionFactory, jdbcTemplate, publisher, DataStatisticsEvent.class, false );

        checkNotNull( systemSettingManager );
        checkNotNull( userSettingService );

        this.systemSettingManager = systemSettingManager;
        this.userSettingService = userSettingService;
    }

    @Override
    public Map<DataStatisticsEventType, Double> getDataStatisticsEventCount( Date startDate, Date endDate )
    {
        Map<DataStatisticsEventType, Double> eventTypeCountMap = new HashMap<>();

        final String sql = "select eventtype as eventtype, count(eventtype) as numberofviews " +
            "from datastatisticsevent " +
            "where timestamp between ? and ? " +
            "group by eventtype;";

        PreparedStatementSetter pss = ( ps ) -> {
            int i = 1;
            ps.setDate( i++, asSqlDate( startDate ) );
            ps.setDate( i++, asSqlDate( endDate ) );
        };

        jdbcTemplate.query( sql, pss, ( rs, i ) -> {
            eventTypeCountMap.put( DataStatisticsEventType.valueOf( rs.getString( "eventtype" ) ),
                rs.getDouble( "numberofviews" ) );
            return eventTypeCountMap;
        } );

        final String totalSql = "select count(eventtype) as total " +
            "from datastatisticsevent " +
            "where timestamp between ? and ?;";

        jdbcTemplate.query( totalSql, pss, ( resultSet, i ) -> {
            return eventTypeCountMap.put( DataStatisticsEventType.TOTAL_VIEW, resultSet.getDouble( "total" ) );
        } );

        return eventTypeCountMap;
    }

    @Override
    public List<FavoriteStatistics> getFavoritesData( DataStatisticsEventType eventType, int pageSize,
        SortOrder sortOrder, String username )
    {
        Assert.notNull( eventType, "Data statistics event type cannot be null" );
        Assert.notNull( sortOrder, "Sort order cannot be null" );

        final Locale currentLocale = (Locale) defaultIfNull(
            userSettingService.getUserSetting( UserSettingKey.DB_LOCALE ),
            userSettingService.getUserSetting( UserSettingKey.UI_LOCALE ) );

        String sql = "select c.uid, views, (case when value is not null then value else c.name end) as name, c.created"
            + " from (select favoriteuid as uid, count(favoriteuid) as views "
            + " from datastatisticsevent where eventtype = '" + eventType.name() + "' ";

        if ( username != null )
        {
            sql += " and username = ? ";
        }

        sql += " group by uid) as events"
            + " inner join " + escapeSql( eventType.getTable() ) + " c on c.uid = events.uid"
            + " left join jsonb_to_recordset(c.translations) as i18name(value TEXT, locale TEXT, property TEXT)"
            + " on i18name.locale = ?"
            + " and i18name.property = 'NAME'"
            + " order by events.views " + escapeSql( sortOrder.getValue() )
            + " limit ?;";

        PreparedStatementSetter pss = ( ps ) -> {
            int i = 1;

            if ( username != null )
            {
                ps.setString( i++, username );
            }

            ps.setString( i++, currentLocale.getLanguage() );
            ps.setInt( i++, pageSize );
        };

        return jdbcTemplate.query( sql, pss, ( rs, i ) -> {
            FavoriteStatistics stats = new FavoriteStatistics();

            stats.setPosition( i + 1 );
            stats.setId( rs.getString( "uid" ) );
            stats.setName( rs.getString( "name" ) );
            stats.setCreated( rs.getDate( "created" ) );
            stats.setViews( rs.getInt( "views" ) );

            return stats;
        } );
    }

    @Override
    public FavoriteStatistics getFavoriteStatistics( String uid )
    {
        String sql = "select count(dse.favoriteuid) " +
            "from datastatisticsevent dse " +
            "where dse.favoriteuid = ?";

        if ( !systemSettingManager.getBoolSetting( COUNT_PASSIVE_DASHBOARD_VIEWS_IN_USAGE_ANALYTICS ) )
        {
            sql += " and dse.eventtype != '" + DataStatisticsEventType.PASSIVE_DASHBOARD_VIEW.name() + "'";
        }

        Object[] args = Lists.newArrayList( uid ).toArray();

        Integer views = jdbcTemplate.queryForObject( sql, args, Integer.class );

        FavoriteStatistics stats = new FavoriteStatistics();
        stats.setViews( views );
        return stats;
    }
}
