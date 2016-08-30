package org.hisp.dhis.feedback;

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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TypeReportTest
{
    @Test
    public void testTypeReport()
    {
        ObjectReport objectReport0 = new ObjectReport( DataElement.class, 0 );
        ObjectReport objectReport1 = new ObjectReport( DataElement.class, 1 );
        ObjectReport objectReport2 = new ObjectReport( DataElement.class, 2 );

        objectReport0.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );
        objectReport1.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );
        objectReport2.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );

        TypeReport typeReport0 = new TypeReport( DataElement.class );
        typeReport0.addObjectReport( objectReport0 );
        typeReport0.addObjectReport( objectReport1 );
        typeReport0.addObjectReport( objectReport2 );

        assertEquals( 3, typeReport0.getObjectReports().size() );
        assertEquals( 3, typeReport0.getErrorReports().size() );

        ObjectReport objectReport3 = new ObjectReport( DataElement.class, 3 );
        ObjectReport objectReport4 = new ObjectReport( DataElement.class, 4 );
        ObjectReport objectReport5 = new ObjectReport( DataElement.class, 5 );

        objectReport3.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );
        objectReport4.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );
        objectReport5.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );

        TypeReport typeReport1 = new TypeReport( DataElement.class );
        typeReport1.addObjectReport( objectReport0 );
        typeReport1.addObjectReport( objectReport1 );
        typeReport1.addObjectReport( objectReport2 );

        assertEquals( 3, typeReport1.getObjectReports().size() );
        assertEquals( 3, typeReport1.getErrorReports().size() );
    }

    @Test
    public void testTypeReportMerge()
    {
        ObjectReport objectReport0 = new ObjectReport( DataElement.class, 0 );
        ObjectReport objectReport1 = new ObjectReport( DataElement.class, 1 );
        ObjectReport objectReport2 = new ObjectReport( DataElement.class, 2 );

        objectReport0.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );
        objectReport1.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );
        objectReport2.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );

        TypeReport typeReport0 = new TypeReport( DataElement.class );
        typeReport0.addObjectReport( objectReport0 );
        typeReport0.addObjectReport( objectReport1 );
        typeReport0.addObjectReport( objectReport2 );

        assertEquals( 3, typeReport0.getObjectReports().size() );
        assertEquals( 3, typeReport0.getErrorReports().size() );

        ObjectReport objectReport3 = new ObjectReport( DataElement.class, 3 );
        ObjectReport objectReport4 = new ObjectReport( DataElement.class, 4 );
        ObjectReport objectReport5 = new ObjectReport( DataElement.class, 5 );

        objectReport3.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );
        objectReport4.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );
        objectReport5.addErrorReport( new ErrorReport( DataElementGroup.class, ErrorCode.E3000, "admin", "DataElementGroup" ) );

        TypeReport typeReport1 = new TypeReport( DataElement.class );
        typeReport1.addObjectReport( objectReport3 );
        typeReport1.addObjectReport( objectReport4 );
        typeReport1.addObjectReport( objectReport5 );

        assertEquals( 3, typeReport1.getObjectReports().size() );
        assertEquals( 3, typeReport1.getErrorReports().size() );

        typeReport0.merge( typeReport1 );

        assertEquals( 6, typeReport0.getErrorReports().size() );
        assertEquals( 6, typeReport0.getObjectReports().size() );
    }
}
