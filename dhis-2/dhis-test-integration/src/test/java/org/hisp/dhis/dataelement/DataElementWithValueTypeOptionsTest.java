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
package org.hisp.dhis.dataelement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.FileTypeValueOptions;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypeOptions;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
class DataElementWithValueTypeOptionsTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    private DataElementStore dataElementStore;

    @Autowired
    @Qualifier( value = "xmlMapper" )
    public ObjectMapper xmlMapper;

    @Test
    void testSaveGetAndDeleteDataElementWithFileValueTypeOption()
    {
        // Save
        final long maxFileSize = 100L;
        DataElement dataElementA = createDataElementWithFileValueTypeOptions( 'A', maxFileSize );
        dataElementStore.save( dataElementA );
        // Get the auto-generated id we should have got after calling save
        long idA = dataElementA.getId();
        assertNotNull( dataElementStore.get( idA ) );
        // Fetch with the auto-generated id
        DataElement fetchedObject = dataElementStore.get( idA );
        // Validate the re-fetched object have the same values as the original
        // version
        ValueTypeOptions valueTypeOptions = fetchedObject.getValueTypeOptions();
        assertNotNull( valueTypeOptions );
        assertEquals( FileTypeValueOptions.class, valueTypeOptions.getClass() );
        assertEquals( maxFileSize, ((FileTypeValueOptions) valueTypeOptions).getMaxFileSize() );
        // Delete the object
        dataElementStore.delete( fetchedObject );
        // Validate the deleted object is actually deleted by trying to re-fetch
        // it
        assertNull( dataElementStore.get( idA ) );
    }

    private DataElement createDataElementWithFileValueTypeOptions( char uniqueCharacter, long maxFileSize )
    {
        FileTypeValueOptions fileTypeValueOptions = new FileTypeValueOptions();
        fileTypeValueOptions.setMaxFileSize( maxFileSize );
        DataElement dataElement = new DataElement();
        dataElement.setAutoFields();
        dataElement.setUid( BASE_DE_UID + uniqueCharacter );
        dataElement.setName( "DataElement" + uniqueCharacter );
        dataElement.setShortName( "DataElementShort" + uniqueCharacter );
        dataElement.setCode( "DataElementCode" + uniqueCharacter );
        dataElement.setDescription( "DataElementDescription" + uniqueCharacter );
        dataElement.setValueType( ValueType.FILE_RESOURCE );
        dataElement.setDomainType( DataElementDomain.AGGREGATE );
        dataElement.setAggregationType( AggregationType.SUM );
        dataElement.setZeroIsSignificant( false );
        dataElement.setValueTypeOptions( fileTypeValueOptions );
        if ( categoryService != null )
        {
            dataElement.setCategoryCombo( categoryService.getDefaultCategoryCombo() );
        }
        return dataElement;
    }

    @Test
    void testDeserialize()
        throws JsonProcessingException
    {
        DataElement dataElementA = createDataElementWithFileValueTypeOptions( 'A', 100L );
        String xml = xmlMapper.writeValueAsString( dataElementA );
        assertNotNull( xml );
        dataElementStore.save( dataElementA );
        long idA = dataElementA.getId();
        DataElement fetchedObject = dataElementStore.get( idA );
        String xmlB = xmlMapper.writeValueAsString( fetchedObject );
        assertNotNull( xmlB );
        log.info( xmlB );
    }
}
