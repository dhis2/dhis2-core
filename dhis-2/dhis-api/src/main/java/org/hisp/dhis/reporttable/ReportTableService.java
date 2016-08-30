package org.hisp.dhis.reporttable;

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

import org.hisp.dhis.common.AnalyticalObjectService;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.user.User;

import java.util.Date;
import java.util.List;

/**
 * @author Lars Helge Overland
 */
public interface ReportTableService
    extends AnalyticalObjectService<ReportTable>
{
    String ID = ReportTableService.class.getName();

    String MODE_REPORT = "report";
    String MODE_REPORT_TABLE = "table";

    /**
     * Saves a ReportTable.
     *
     * @param reportTable the ReportTable to save.
     * @return the generated identifier.
     */
    int saveReportTable( ReportTable reportTable );

    /**
     * Updates a ReportTable.
     *
     * @param reportTable the ReportTable to update.
     */
    void updateReportTable( ReportTable reportTable );

    /**
     * Deletes a ReportTable.
     *
     * @param reportTable the ReportTable to delete.
     */
    void deleteReportTable( ReportTable reportTable );

    /**
     * Retrieves the ReportTable with the given identifier.
     *
     * @param id the identifier of the ReportTable to retrieve.
     * @return the ReportTable.
     */
    ReportTable getReportTable( int id );

    /**
     * Retrieves the ReportTable with the given uid.
     *
     * @param uid the uid of the ReportTable to retrieve.
     * @return the ReportTable.
     */
    ReportTable getReportTable( String uid );

    /**
     * Retrieves the ReportTable with the given uid. Bypasses the ACL system.
     *
     * @param uid the uid of the ReportTable to retrieve.
     * @return the ReportTable.
     */
    ReportTable getReportTableNoAcl( String uid );

    /**
     * Retrieves a Collection of all ReportTables.
     *
     * @return a Collection of ReportTables.
     */
    List<ReportTable> getAllReportTables();

    /**
     * Retrieves ReportTables with the given uids.
     * 
     * @param uids the list of uids.
     * @return a list of ReportTables.
     */
    List<ReportTable> getReportTablesByUid( List<String> uids );

    /**
     * Retrieves the ReportTable with the given name.
     *
     * @param name the name of the ReportTable.
     * @return the ReportTable.
     */
    List<ReportTable> getReportTableByName( String name );

    /**
     * Instantiates and populates a Grid populated with data from the ReportTable
     * with the given identifier.
     *
     * @param uid                 the ReportTable unique identifier.
     * @param reportingPeriod     the reporting date.
     * @param organisationUnitUid the organisation unit uid.
     * @return a Grid.
     */
    Grid getReportTableGrid( String uid, Date reportingPeriod, String organisationUnitUid );

    Grid getReportTableGridByUser( String uid, Date reportingPeriod, String organisationUnitUid, User user );

    ReportTable getReportTable( String uid, String mode );

    List<ReportTable> getReportTablesBetween( int first, int max );

    List<ReportTable> getReportTablesBetweenByName( String name, int first, int max );

    int getReportTableCount();

    int getReportTableCountByName( String name );
}
