package org.hisp.dhis.dataelement;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.ComplexValueType;
import org.hisp.dhis.common.FileTypeValueType;
import org.hisp.dhis.common.ValueType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DataElementWithComplexValueTypeIntegrationTest
    extends TransactionalIntegrationTest
{
    @Autowired
    private DataElementStore dataElementStore;

    @Test
    public void testDeleteAndGetDataElement()
    {
        DataElement dataElementA = createDataElementWithComplexValueType( 'A', null );

        dataElementStore.save( dataElementA );
        long idA = dataElementA.getId();

        assertNotNull( dataElementStore.get( idA ) );

        dataElementA = dataElementStore.get( idA );
        ComplexValueType complexValueType = dataElementA.getComplexValueType();

        assertNotNull( complexValueType );
        assertEquals( complexValueType.getClass(), FileTypeValueType.class );
        FileTypeValueType fileTypeValueType = (FileTypeValueType) complexValueType;
        assertEquals( fileTypeValueType.getMaxFileSize().longValue(), 100L );

        dataElementStore.delete( dataElementA );
        assertNull( dataElementStore.get( idA ) );
    }

    private DataElement createDataElementWithComplexValueType( char uniqueCharacter, CategoryCombo categoryCombo )
    {
        DataElement dataElement = new DataElement();
        dataElement.setAutoFields();

        dataElement.setUid( BASE_DE_UID + uniqueCharacter );
        dataElement.setName( "DataElement" + uniqueCharacter );
        dataElement.setShortName( "DataElementShort" + uniqueCharacter );
        dataElement.setCode( "DataElementCode" + uniqueCharacter );
        dataElement.setDescription( "DataElementDescription" + uniqueCharacter );
        dataElement.setValueType( ValueType.INTEGER );
        dataElement.setDomainType( DataElementDomain.AGGREGATE );
        dataElement.setAggregationType( AggregationType.SUM );
        dataElement.setZeroIsSignificant( false );

        if ( categoryCombo != null )
        {
            dataElement.setCategoryCombo( categoryCombo );
        }
        else if ( categoryService != null )
        {
            dataElement.setCategoryCombo( categoryService.getDefaultCategoryCombo() );
        }

        FileTypeValueType cvt = new FileTypeValueType();
        cvt.setMaxFileSize( 100L );

        dataElement.setComplexValueType( cvt );


        return dataElement;
    }

}
