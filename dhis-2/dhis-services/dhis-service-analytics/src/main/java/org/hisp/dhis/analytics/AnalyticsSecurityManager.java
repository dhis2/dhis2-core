package org.hisp.dhis.analytics;

import org.hisp.dhis.analytics.event.EventQueryParams;

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

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface AnalyticsSecurityManager
{    
    /**
     * Decides whether the current user has privileges to execute the given query.
     * 
     * @param params the data query parameters.
     * @throws IllegalQueryException if the current user does not have privileges
     *         to execute the given query.
     */
    void decideAccess( DataQueryParams params );
    
    /**
     * Decides whether the current user has privileges to execute the given event
     * query.
     * 
     * @param params the event data query parameters.
     * @throws IllegalQueryException if the current user does not have privileges
     *         to execute the given query.
     */
    void decideAccessEventQuery( EventQueryParams params );
    
    /**
     * Returns the current user. Looks for a current user to be specified for the
     * given query, if not uses the current user from the security context.
     * 
     * @param params the data query parameters.
     * @return the current user.
     */
    User getCurrentUser( DataQueryParams params );
    
    /**
     * Returns a query with relevant data approval levels if system is configured
     * to hide unapproved data from analytics and if there are relevant approval
     * levels for current user. Populates the approvalLevels property of the given
     * query and sets the level property of each related organisation unit.
     * 
     * @param params the data query parameters.
     * @throws IllegalQueryException is the specified approval level does not exist.
     */
    DataQueryParams withDataApprovalConstraints( DataQueryParams params );
    
    /**
     * Returns a query with dimension constraints. Dimension constraints with 
     * all accessible dimension items will be added as filters to this query.
     * If current user has no dimension constraints, no action is taken. If the 
     * constraint dimensions are already specified with accessible items in the 
     * query, no action is taken. If the current user does not have accessible 
     * items in any dimension constraint, an IllegalQueryException is thrown.
     * 
     * @param pamrams the data query parameters.
     * @throws IllegalQueryException is the specified approval level does not exist.
     */
    DataQueryParams withDimensionConstraints( DataQueryParams params );
}
