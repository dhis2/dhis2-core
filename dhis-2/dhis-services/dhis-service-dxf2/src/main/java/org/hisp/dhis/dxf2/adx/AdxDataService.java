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
package org.hisp.dhis.dxf2.adx;

import java.io.InputStream;
import java.io.OutputStream;

import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetQueryParams;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.scheduling.JobConfiguration;

/**
 * @author bobj
 */
public interface AdxDataService
{
    // --------------------------------------------------------------------------
    // ADX standard constants
    // --------------------------------------------------------------------------

    String NAMESPACE = "urn:ihe:qrph:adx:2015";

    String ROOT = "adx";

    String GROUP = "group";

    String DATASET = "dataSet";

    String PERIOD = "period";

    String ORGUNIT = "orgUnit";

    String DATAELEMENT = "dataElement";

    String DATAVALUE = "dataValue";

    String VALUE = "value";

    String ANNOTATION = "annotation";

    String ERROR = "error";

    // --------------------------------------------------------------------------
    // DHIS 2 specific constants
    // --------------------------------------------------------------------------

    String CATOPTCOMBO = "categoryOptionCombo";

    String ATTOPTCOMBO = "attributeOptionCombo";

    // --------------------------------------------------------------------------
    // Methods
    // --------------------------------------------------------------------------

    DataExportParams getFromUrl( DataValueSetQueryParams params );

    /**
     * Post data. Takes ADX Data from input stream and saves a series of DXF2
     * DataValueSets.
     *
     * @param in the InputStream.
     * @param importOptions the importOptions.
     * @param id the task id, can be null.
     *
     * @return an ImportSummaries collection of ImportSummary for each
     *         DataValueSet.
     */
    ImportSummary saveDataValueSet( InputStream in, ImportOptions importOptions, JobConfiguration id );

    /**
     * Get data. Writes adx export data to output stream.
     *
     * @param params the data export params.
     * @param out the output stream to write to.
     *
     * @return an ImportSummaries collection of ImportSummary for each
     *         DataValueSet.
     * @throws AdxException for conflicts during export process.
     */
    void writeDataValueSet( DataExportParams params, OutputStream out )
        throws AdxException;
}
