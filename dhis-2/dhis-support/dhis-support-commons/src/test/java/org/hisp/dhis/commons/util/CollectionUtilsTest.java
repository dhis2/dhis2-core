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
package org.hisp.dhis.commons.util;

import static org.hisp.dhis.commons.collection.CollectionUtils.flatMapToSet;
import static org.hisp.dhis.commons.collection.CollectionUtils.mapToList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

class CollectionUtilsTest
{
    @Test
    void testFlatMapToSet()
    {
        DataElement deA = new DataElement();
        DataElement deB = new DataElement();
        DataElement deC = new DataElement();
        DataSet dsA = new DataSet();
        DataSet dsB = new DataSet();

        deA.setAutoFields();
        deB.setAutoFields();
        deC.setAutoFields();
        dsA.setAutoFields();
        dsB.setAutoFields();

        dsA.addDataSetElement( deA );
        dsA.addDataSetElement( deB );
        dsB.addDataSetElement( deB );
        dsB.addDataSetElement( deC );

        List<DataSet> dataSets = List.of( dsA, dsB );

        Set<DataElement> dataElements = flatMapToSet( dataSets, DataSet::getDataElements );

        assertEquals( 3, dataElements.size() );
    }

    @Test
    void testDifference()
    {
        List<String> collection1 = Lists.newArrayList( "One", "Two", "Three" );
        List<String> collection2 = Lists.newArrayList( "One", "Two", "Four" );
        List<String> difference = CollectionUtils.difference( collection1, collection2 );

        assertEquals( 1, difference.size() );
        assertEquals( "Three", difference.get( 0 ) );
    }

    @Test
    void testMapToList()
    {
        List<String> collection = Lists.newArrayList( "1", "2", "3" );

        assertEquals( 3, mapToList( collection, Integer::parseInt ).size() );
        assertEquals( 1, mapToList( collection, Integer::parseInt ).get( 0 ) );
    }
}
