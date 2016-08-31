package org.hisp.dhis.dataset;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.Test;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class DataSetTest
{
    @Test
    public void testUpdateOrganisationUnits()
    {
        DataSet dsA = new DataSet( "dsA" );
        
        OrganisationUnit ouA = new OrganisationUnit( "ouA" );
        OrganisationUnit ouB = new OrganisationUnit( "ouB" );
        OrganisationUnit ouC = new OrganisationUnit( "ouC" );
        OrganisationUnit ouD = new OrganisationUnit( "ouD" );
        
        dsA.addOrganisationUnit( ouA );
        dsA.addOrganisationUnit( ouB );
        
        assertEquals( 2, dsA.getSources().size() );
        assertTrue( dsA.getSources().containsAll( Sets.newHashSet( ouA, ouB ) ) );
        assertTrue( ouA.getDataSets().contains( dsA ) );
        assertTrue( ouB.getDataSets().contains( dsA ) );
        assertTrue( ouC.getDataSets().isEmpty() );
        assertTrue( ouD.getDataSets().isEmpty() );
        
        dsA.updateOrganisationUnits( Sets.newHashSet( ouB, ouC ) );

        assertEquals( 2, dsA.getSources().size() );
        assertTrue( dsA.getSources().containsAll( Sets.newHashSet( ouB, ouC ) ) );
        assertTrue( ouA.getDataSets().isEmpty() );
        assertTrue( ouB.getDataSets().contains( dsA ) );
        assertTrue( ouC.getDataSets().contains( dsA ) );
        assertTrue( ouD.getDataSets().isEmpty() );
    }
    
    @Test
    public void testUpdateDataElements()
    {
        DataSet dsA = new DataSet( "dsA" );
        
        DataElement deA = new DataElement( "deA" );
        DataElement deB = new DataElement( "deB" );
        DataElement deC = new DataElement( "deC" );
        DataElement deD = new DataElement( "deD" );
        
        dsA.addDataElement( deA );
        dsA.addDataElement( deB );
        
        assertEquals( 2, dsA.getDataElements().size() );
        assertTrue( dsA.getDataElements().containsAll( Sets.newHashSet( deA, deB ) ) );
        assertTrue( deA.getDataSets().contains( dsA ) );
        assertTrue( deB.getDataSets().contains( dsA ) );
        assertTrue( deC.getDataSets().isEmpty() );
        assertTrue( deD.getDataSets().isEmpty() );
        
        dsA.updateDataElements( Sets.newHashSet( deB, deC ) );
        
        assertEquals( 2, dsA.getDataElements().size() );
        assertTrue( dsA.getDataElements().containsAll( Sets.newHashSet( deB, deC ) ) );
        assertTrue( deA.getDataSets().isEmpty() );
        assertTrue( deB.getDataSets().contains( dsA ) );
        assertTrue( deC.getDataSets().contains( dsA ) );
        assertTrue( deD.getDataSets().isEmpty() );
    }
}
