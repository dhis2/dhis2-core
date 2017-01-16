package org.hisp.dhis.datastatistics;

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

import org.hisp.dhis.analytics.SortOrder;

import org.hisp.dhis.common.GenericStore;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
public interface DataStatisticsEventStore
    extends GenericStore<DataStatisticsEvent>
{
    /**
     * Method for retrieving aggregated event count data.
     *
     * @param startDate the start date.
     * @param endDate the end date.
     * @return a map between DataStatisticsEventTypes and counts.
     */
    Map<DataStatisticsEventType, Double> getDataStatisticsEventCount( Date startDate, Date endDate );

    /**
     * Returns top favorites by views
     *
     * @param eventType that should be counted
     * @param pageSize number of favorites
     * @param sortOrder sort order of the favorites
     * @param username of user
     * @return list of FavoriteStatistics
     */
    List<FavoriteStatistics> getFavoritesData( DataStatisticsEventType eventType, int pageSize, SortOrder sortOrder, String username );

    /**
     * Returns data statistics for the favorite with the given identifier.
     * 
     * @param uid the favorite identifier.
     * @return data statistics for the favorite with the given identifier.
     */
    FavoriteStatistics getFavoriteStatistics( String uid );
}