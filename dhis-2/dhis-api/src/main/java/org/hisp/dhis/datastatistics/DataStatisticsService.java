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

import java.util.Date;
import java.util.List;

import org.hisp.dhis.analytics.SortOrder;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
public interface DataStatisticsService
{
    /**
     * Adds an DataStatistics event.
     *
     * @param event object to be saved
     * @return id of the object in the database
     */
    int addEvent( DataStatisticsEvent event );

    /**
     * Gets number of saved events from a start date to an end date.
     *
     * @param startDate start date
     * @param endDate end date
     * @param eventInterval event interval
     * 
     * @return list of reports
     */
    List<AggregatedStatistics> getReports( Date startDate, Date endDate, EventInterval eventInterval );
    
    /**
     * Returns a DataStatistics instance for the given day.
     * 
     * @param day the day to generate the DataStatistics instance for.
     * @return a DataStatistics instance for the given day.
     */
    DataStatistics getDataStatisticsSnapshot( Date day );
    
    /**
     * Saves a DataStatistics instance.
     *  
     * @param dataStatistics the DataStatistics instance.
     * @return identifier of the persisted DataStatistics object.
     */
    int saveDataStatistics( DataStatistics dataStatistics );
    
    /**
     * Gets all information and creates a DataStatistics object and persists it.
     * 
     * @return identifier of the persisted DataStatistics object.
     */
    int saveDataStatisticsSnapshot();

    /**
     * Returns top favorites by views
     *
     * @param eventType that should be counted
     * @param pageSize number of favorites
     * @param sortOrder sort order of the favorites
     * @param username name of user, makes the query specified to this user
     * @return list of FavoriteStatistics
     */

    List<FavoriteStatistics> getTopFavorites( DataStatisticsEventType eventType, int pageSize, SortOrder sortOrder, String username );
    
    /**
     * Returns data statistics for the favorite with the given identifier.
     * 
     * @param uid the favorite identifier.
     * @return data statistics for the favorite with the given identifier.
     */
    FavoriteStatistics getFavoriteStatistics( String uid );
}
