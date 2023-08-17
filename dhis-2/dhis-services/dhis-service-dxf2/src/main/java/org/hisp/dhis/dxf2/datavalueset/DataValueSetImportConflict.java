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

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dxf2.importsummary.ImportConflictDescriptor;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Possible conflicts related to imported {@link DataSet} during a
 * {@link DataValueSet} import.
 *
 * @author Jan Bernitt
 */
public enum DataValueSetImportConflict implements ImportConflictDescriptor
{

    DATASET_NOT_FOUND( ErrorCode.E7600, "dataSet", DataSet.class ),
    DATASET_NOT_ACCESSIBLE( ErrorCode.E7601, "dataSet", DataSet.class ),
    ORG_UNIT_NOT_FOUND( ErrorCode.E7603, "orgUnit", OrganisationUnit.class, DataSet.class ),
    ATTR_OPTION_COMBO_NOT_FOUND( ErrorCode.E7604, "attributeOptionCombo", CategoryOptionCombo.class,
        DataSet.class );

    private final ErrorCode errorCode;

    private String property;

    private Class<?>[] objectTypes;

    DataValueSetImportConflict( ErrorCode errorCode, String property, Class<?>... objectTypes )
    {
        this.errorCode = errorCode;
        this.property = property;
        this.objectTypes = objectTypes;
    }

    @Override
    public Class<?>[] getObjectTypes()
    {
        return objectTypes;
    }

    @Override
    public String getProperty()
    {
        return property;
    }

    @Override
    public ErrorCode getErrorCode()
    {
        return errorCode;
    }
}
