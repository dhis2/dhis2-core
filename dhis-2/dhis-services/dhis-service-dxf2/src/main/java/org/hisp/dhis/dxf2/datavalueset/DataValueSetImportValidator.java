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
package org.hisp.dhis.dxf2.datavalueset;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;

import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.security.acl.AclService;
import org.springframework.stereotype.Component;

/**
 * Validates {@link DataValueSet} import.
 *
 * @author Jan Bernitt
 */
@Component
@AllArgsConstructor
public class DataValueSetImportValidator
{

    private final AclService aclService;

    /**
     * Validation on the {@link DataSet} level
     */
    interface DataSetValidation
    {
        void validate( DataValueSet dataValueSet, ImportContext context, DataSetImportContext dataSetContext );
    }

    private final List<DataSetValidation> dataSetValidations = new ArrayList<>();

    private void register( DataSetValidation validation )
    {
        dataSetValidations.add( validation );
    }

    @PostConstruct
    public void init()
    {
        // OBS! Order is important as validation occurs in order of registration
        register( DataValueSetImportValidator::validateDataSetExists );
        register( this::validateDataSetIsAccessibleByUser );
        register( DataValueSetImportValidator::validateDataSetExistsStrictDataElements );
        register( DataValueSetImportValidator::validateDataSetOrgUnitExists );
        register( DataValueSetImportValidator::validateDataSetAttrOptionComboExists );
    }

    /*
     * DataSet validation
     */

    /**
     * Validate {@link DataSet} level of the import.
     *
     * @return true when valid, false when not valid and import should be
     *         aborted
     */
    public boolean validateDataSet( DataValueSet dataValueSet, ImportContext context,
        DataSetImportContext dataSetContext )
    {
        for ( DataSetValidation validation : dataSetValidations )
        {
            validation.validate( dataValueSet, context, dataSetContext );
        }
        return !context.getSummary().isStatus( ImportStatus.ERROR );
    }

    private static void validateDataSetExists( DataValueSet dataValueSet, ImportContext context,
        DataSetImportContext dataSetContext )
    {
        if ( dataSetContext.getDataSet() == null && trimToNull( dataValueSet.getDataSet() ) != null )
        {
            context.error().addConflict( dataValueSet.getDataSet(), "Data set not found or not accessible" );
        }
    }

    private void validateDataSetIsAccessibleByUser( DataValueSet dataValueSet, ImportContext context,
        DataSetImportContext dataSetContext )
    {
        DataSet dataSet = dataSetContext.getDataSet();
        if ( dataSet != null && !aclService.canDataWrite( context.getCurrentUser(), dataSet ) )
        {
            context.error().addConflict( dataValueSet.getDataSet(),
                "User does not have write access for DataSet: " + dataSet.getUid() );
        }
    }

    private static void validateDataSetExistsStrictDataElements( DataValueSet dataValueSet, ImportContext context,
        DataSetImportContext dataSetContext )
    {
        if ( dataSetContext.getDataSet() == null && context.isStrictDataElements() )
        {
            context.error().addConflict( "DATA_IMPORT_STRICT_DATA_ELEMENTS", "A valid dataset is required" );
        }
    }

    private static void validateDataSetOrgUnitExists( DataValueSet dataValueSet, ImportContext context,
        DataSetImportContext dataSetContext )
    {
        if ( dataSetContext.getOuterOrgUnit() == null && trimToNull( dataValueSet.getOrgUnit() ) != null )
        {
            context.error().addConflict( dataValueSet.getOrgUnit(), "Org unit not found or not accessible" );
        }
    }

    private static void validateDataSetAttrOptionComboExists( DataValueSet dataValueSet, ImportContext context,
        DataSetImportContext dataSetContext )
    {
        if ( dataSetContext.getOuterAttrOptionCombo() == null
            && trimToNull( dataValueSet.getAttributeOptionCombo() ) != null )
        {
            context.error().addConflict( dataValueSet.getAttributeOptionCombo(),
                "Attribute option combo not found or not accessible" );
        }
    }
}
