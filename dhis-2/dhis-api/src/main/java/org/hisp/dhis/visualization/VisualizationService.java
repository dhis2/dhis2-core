package org.hisp.dhis.visualization;

/*
 * Copyright (c) 2004-2021, University of Oslo
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

import org.hisp.dhis.common.AnalyticalObjectService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.user.User;

/**
 * Interface responsible for providing CRUD and business methods related to a
 * Visualization object.
 */
public interface VisualizationService
    extends
    AnalyticalObjectService<Visualization>
{
    /**
     * Saves a Visualization.
     *
     * @param visualization the Visualization to save.
     * @return the generated identifier.
     */
    long save( Visualization visualization );

    /**
     * Retrieves the Visualization with the given id.
     *
     * @param id the id of the Visualization to retrieve.
     * @return the Visualization.
     */
    Visualization loadVisualization( long id );

    /**
     * Retrieves the Visualization with the given uid.
     *
     * @param uid the uid of the Visualization to retrieve.
     * @return the Visualization.
     */
    Visualization loadVisualization( String uid );

    /**
     * Deletes a Visualization.
     *
     * @param visualization the Visualization to delete.
     */
    void delete( Visualization visualization );

    /**
     * Instantiates and populates a Grid populated with data from the given Visualization.
     *
     * @param uid of the Visualization.
     * @param relativePeriodDate the visualization date.
     * @param organisationUnitUid the organisation unit uid.
     * @return a Grid.
     */
    Grid getVisualizationGrid( String uid, Date relativePeriodDate, String organisationUnitUid );

    /**
     * Instantiates and populates a Grid populated with data from the given Visualization.
     *
     * @param uid of the Visualization.
     * @param relativePeriodDate the visualization date.
     * @param organisationUnitUid the organisation unit uid.
     * @param user the current user.
     * @return a Grid.
     */
    Grid getVisualizationGridByUser( String uid, Date relativePeriodDate, String organisationUnitUid, User user );

    /**
     * Retrieves the Visualization with the given uid. Bypasses the ACL system.
     *
     * @param uid the uid of the Visualization to retrieve.
     * @return the Visualization.
     */
    Visualization getVisualizationNoAcl( String uid );
}
