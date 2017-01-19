package org.hisp.dhis.dxf2.dataset;

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

import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.scheduling.TaskId;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Set;

/**
 * Import/export service for {@link CompleteDataSetRegistration data set completion registrations}.
 *
 * @author Halvdan Hoem Grelland
 */
public interface CompleteDataSetRegistrationExchangeService
{
    /**
     * Transform query parameters into an {@link ExportParams} instance.
     *
     * @param dataSets list of {@link org.hisp.dhis.dataset.DataSet} UIDs.
     * @param orgUnits list of {@link org.hisp.dhis.organisationunit.OrganisationUnit} UIDs.
     * @param orgUnitGroups list of {@link org.hisp.dhis.organisationunit.OrganisationUnitGroup} UIDs.
     * @param periods list of {@link org.hisp.dhis.period.Period} names.
     * @param startDate start of limiting date interval.
     * @param endDate end of interval (inclusive).
     * @param includeChildren whether to recursively include descendant organisation units.
     * @param created base created date for query.
     * @param createdDuration duration (relative to {@code created}).
     * @param limit record number limit (minimum 0).
     * @param idSchemes identifier schemes applying to this query.
     * @return an instance of {@link ExportParams} corresponding to the given query parameters.
     */
    ExportParams paramsFromUrl( Set<String> dataSets, Set<String> orgUnits, Set<String> orgUnitGroups, Set<String> periods,
        Date startDate, Date endDate, boolean includeChildren, Date created, String createdDuration, Integer limit, IdSchemes idSchemes );

    /**
     * Queries and writes {@link CompleteDataSetRegistrations} to the given {@link OutputStream} as XML.
     *
     * @param params the export query.
     * @param out the stream to write to.
     */
    void writeCompleteDataSetRegistrationsXml( ExportParams params, OutputStream out );

    /**
     * Queries and writes {@link CompleteDataSetRegistrations} to the given {@link OutputStream} as JSON.
     *
     * @param params the export query.
     * @param out the stream to write to.
     */
    void writeCompleteDataSetRegistrationsJson( ExportParams params, OutputStream out );

    /**
     * Imports {@link CompleteDataSetRegistrations} from an XML payload.
     *
     * @param in the stream providing the XML payload.
     * @param importOptions the options for the import.
     * @return a summary of the import process.
     */
    ImportSummary saveCompleteDataSetRegistrationsXml( InputStream in, ImportOptions importOptions );

    /**
     * Imports {@link CompleteDataSetRegistrations} from an XML payload.
     *
     * @param in the stream providing the XML payload.
     * @param importOptions the options for the import.
     * @param taskId the task (optional).
     * @return a summary of the import process.
     */
    ImportSummary saveCompleteDataSetRegistrationsXml( InputStream in, ImportOptions importOptions, TaskId taskId );


    /**
     * Imports {@link CompleteDataSetRegistrations} from a JSON payload.
     *
     * @param in the stream providing the XML payload.
     * @param importOptions the options for the import.
     * @return a summary of the import process.
     */
    ImportSummary saveCompleteDataSetRegistrationsJson( InputStream in, ImportOptions importOptions );

    /**
     * Imports {@link CompleteDataSetRegistrations} from a JSON payload.
     *
     * @param in the stream providing the XML payload.
     * @param importOptions the options for the import.
     * @param taskId the task (optional).
     * @return a summary of the import process.
     */
    ImportSummary saveCompleteDataSetRegistrationsJson( InputStream in, ImportOptions importOptions, TaskId taskId );
}
