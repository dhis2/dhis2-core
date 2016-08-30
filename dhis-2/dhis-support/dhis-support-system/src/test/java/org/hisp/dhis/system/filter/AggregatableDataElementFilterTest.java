package org.hisp.dhis.system.filter;

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

import com.google.common.collect.Sets;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Lars Helge Overland
 */
public class AggregatableDataElementFilterTest
    extends DhisSpringTest
{
    @Test
    public void filter()
    {
        DataElement elA = createDataElement( 'A' );
        DataElement elB = createDataElement( 'B' );
        DataElement elC = createDataElement( 'C' );
        DataElement elD = createDataElement( 'D' );
        DataElement elE = createDataElement( 'E' );
        DataElement elF = createDataElement( 'F' );

        elA.setValueType( ValueType.BOOLEAN );
        elB.setValueType( ValueType.INTEGER );
        elC.setValueType( ValueType.DATE );
        elD.setValueType( ValueType.BOOLEAN );
        elE.setValueType( ValueType.INTEGER );
        elF.setValueType( ValueType.DATE );

        Set<DataElement> set = Sets.newHashSet( elA, elB, elC, elD, elE, elF );

        Set<DataElement> reference = Sets.newHashSet( elA, elB, elD, elE );

        FilterUtils.filter( set, AggregatableDataElementFilter.INSTANCE );

        assertEquals( reference.size(), set.size() );
        assertEquals( reference, set );

        set = Sets.newHashSet( elA, elB, elC, elD, elE, elF );

        Set<DataElement> inverseReference = Sets.newHashSet( elC, elF );

        FilterUtils.inverseFilter( set, AggregatableDataElementFilter.INSTANCE );

        assertEquals( inverseReference.size(), set.size() );
        assertEquals( inverseReference, set );
    }
}
