package org.hisp.dhis.dxf2.metadata.feedback;

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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundleReportTest
{
    @Test
    public void testObjectBundleValidationReport()
    {
        TypeReport typeReport0 = createTypeReport( DataElement.class, DataElementGroup.class );
        TypeReport typeReport1 = createTypeReport( Indicator.class, IndicatorGroup.class );

        ObjectBundleValidationReport validationReport = new ObjectBundleValidationReport();
        validationReport.addTypeReport( typeReport0 );
        validationReport.addTypeReport( typeReport1 );

        assertEquals( 6, validationReport.getErrorReports().size() );
        assertEquals( 3, validationReport.getErrorReportsByCode( DataElement.class, ErrorCode.E3000 ).size() );
        assertEquals( 3, validationReport.getErrorReportsByCode( Indicator.class, ErrorCode.E3000 ).size() );
    }

    @Test
    public void testObjectBundleCommitReport()
    {
        TypeReport typeReport0 = createTypeReport( DataElement.class, DataElementGroup.class );
        TypeReport typeReport1 = createTypeReport( Indicator.class, IndicatorGroup.class );

        ObjectBundleCommitReport validationReport = new ObjectBundleCommitReport();
        validationReport.addTypeReport( typeReport0 );
        validationReport.addTypeReport( typeReport1 );

        assertEquals( 6, validationReport.getErrorReports().size() );
        assertEquals( 3, validationReport.getErrorReportsByCode( DataElement.class, ErrorCode.E3000 ).size() );
        assertEquals( 3, validationReport.getErrorReportsByCode( Indicator.class, ErrorCode.E3000 ).size() );
    }

    @Test
    public void testImportReportMerge()
    {
        TypeReport typeReport0 = createTypeReport( DataElement.class, DataElementGroup.class );
        TypeReport typeReport1 = createTypeReport( Indicator.class, IndicatorGroup.class );
        TypeReport typeReport2 = createTypeReport( Indicator.class, IndicatorGroup.class );
        TypeReport typeReport3 = createTypeReport( OrganisationUnit.class, OrganisationUnitGroup.class );

        ObjectBundleValidationReport objectBundleValidationReport = new ObjectBundleValidationReport();
        objectBundleValidationReport.addTypeReport( typeReport0 );
        objectBundleValidationReport.addTypeReport( typeReport1 );

        assertEquals( 6, objectBundleValidationReport.getErrorReports().size() );
        assertEquals( 3, objectBundleValidationReport.getErrorReportsByCode( DataElement.class, ErrorCode.E3000 ).size() );
        assertEquals( 3, objectBundleValidationReport.getErrorReportsByCode( Indicator.class, ErrorCode.E3000 ).size() );

        ObjectBundleCommitReport objectBundleCommitReport = new ObjectBundleCommitReport();
        objectBundleCommitReport.addTypeReport( typeReport2 );
        objectBundleCommitReport.addTypeReport( typeReport3 );

        assertEquals( 6, objectBundleCommitReport.getErrorReports().size() );
        assertEquals( 3, objectBundleCommitReport.getErrorReportsByCode( Indicator.class, ErrorCode.E3000 ).size() );
        assertEquals( 3, objectBundleCommitReport.getErrorReportsByCode( OrganisationUnit.class, ErrorCode.E3000 ).size() );

        ImportReport importReport = new ImportReport();
        importReport.addTypeReports( objectBundleValidationReport.getTypeReportMap() );
        importReport.addTypeReports( objectBundleCommitReport.getTypeReportMap() );

        assertEquals( 3, importReport.getTypeReportMap().size() );

        assertEquals( 3, importReport.getTypeReportMap().get( DataElement.class ).getErrorReports().size() );
        assertEquals( 3, importReport.getTypeReportMap().get( OrganisationUnit.class ).getErrorReports().size() );
        assertEquals( 6, importReport.getTypeReportMap().get( Indicator.class ).getErrorReports().size() );

        assertEquals( 3, importReport.getTypeReportMap().get( DataElement.class ).getObjectReports().size() );
        assertEquals( 3, importReport.getTypeReportMap().get( Indicator.class ).getObjectReports().size() );
        assertEquals( 3, importReport.getTypeReportMap().get( OrganisationUnit.class ).getObjectReports().size() );
    }

    private TypeReport createTypeReport( Class<?> mainKlass, Class<?> errorKlass )
    {
        ObjectReport objectReport0 = new ObjectReport( mainKlass, 0 );
        ObjectReport objectReport1 = new ObjectReport( mainKlass, 1 );
        ObjectReport objectReport2 = new ObjectReport( mainKlass, 2 );

        objectReport0.addErrorReport( new ErrorReport( errorKlass, ErrorCode.E3000, "admin", errorKlass.getSimpleName() ) );
        objectReport1.addErrorReport( new ErrorReport( errorKlass, ErrorCode.E3000, "admin", errorKlass.getSimpleName() ) );
        objectReport2.addErrorReport( new ErrorReport( errorKlass, ErrorCode.E3000, "admin", errorKlass.getSimpleName() ) );

        TypeReport typeReport = new TypeReport( mainKlass );
        typeReport.addObjectReport( objectReport0 );
        typeReport.addObjectReport( objectReport1 );
        typeReport.addObjectReport( objectReport2 );

        return typeReport;
    }
}
