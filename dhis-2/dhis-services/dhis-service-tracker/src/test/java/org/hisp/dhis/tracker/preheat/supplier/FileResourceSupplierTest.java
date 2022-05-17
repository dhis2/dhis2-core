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
package org.hisp.dhis.tracker.preheat.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
@ExtendWith( MockitoExtension.class )
class FileResourceSupplierTest extends DhisConvenienceTest
{

    private static final String NUMERIC_DATA_ELEMENT_UID = "numericDataElement";

    private static final String FILE_RESOURCE_DATA_ELEMENT_UID = "fileResourceDataElement";

    private static final String EMPTY_FILE_RESOURCE_DATA_ELEMENT_UID = "emptyFileResourceDataElement";

    private static final String NULL_FILE_RESOURCE_DATA_ELEMENT_UID = "nullFileResourceDataElement";

    private static final String NUMERIC_ATTRIBUTE_UID = "numericAttribute";

    private static final String FILE_RESOURCE_ATTRIBUTE_UID = "fileResourceAttribute";

    private static final String FILE_RESOURCE_UID = "FileResourceUid";

    private static final String ANOTHER_FILE_RESOURCE_UID = "AnotherFileResourceUid";

    private FileResourceSupplier supplierToTest;

    private TrackerPreheat preheat = new TrackerPreheat();

    @Mock
    private FileResourceService fileResourceService;

    @BeforeEach
    public void setUp()
    {
        DataElement numericDataElement = createDataElement( 'A' );
        numericDataElement.setUid( NUMERIC_DATA_ELEMENT_UID );
        numericDataElement.setValueType( ValueType.NUMBER );

        DataElement fileResourceDataElement = createDataElement( 'B' );
        fileResourceDataElement.setUid( FILE_RESOURCE_DATA_ELEMENT_UID );
        fileResourceDataElement.setValueType( ValueType.FILE_RESOURCE );

        DataElement emptyFileResourceDataElement = createDataElement( 'C' );
        emptyFileResourceDataElement.setUid( EMPTY_FILE_RESOURCE_DATA_ELEMENT_UID );
        emptyFileResourceDataElement.setValueType( ValueType.FILE_RESOURCE );

        DataElement nullFileResourceDataElement = createDataElement( 'D' );
        nullFileResourceDataElement.setUid( NULL_FILE_RESOURCE_DATA_ELEMENT_UID );
        nullFileResourceDataElement.setValueType( ValueType.FILE_RESOURCE );

        preheat.put( TrackerIdSchemeParam.UID, Lists
            .newArrayList( numericDataElement, fileResourceDataElement, emptyFileResourceDataElement,
                nullFileResourceDataElement ) );

        TrackedEntityAttribute numericAttribute = createTrackedEntityAttribute( 'A' );
        numericAttribute.setUid( NUMERIC_ATTRIBUTE_UID );
        numericAttribute.setValueType( ValueType.NUMBER );

        TrackedEntityAttribute fileResourceAttribute = createTrackedEntityAttribute( 'A' );
        fileResourceAttribute.setUid( FILE_RESOURCE_ATTRIBUTE_UID );
        fileResourceAttribute.setValueType( ValueType.FILE_RESOURCE );

        preheat.put( TrackerIdSchemeParam.UID, Lists.newArrayList( numericAttribute, fileResourceAttribute ) );

        supplierToTest = new FileResourceSupplier( fileResourceService );
    }

    @Test
    void verifySupplier()
    {
        FileResource fileResource = createFileResource( 'A', "FileResource".getBytes() );
        fileResource.setUid( FILE_RESOURCE_UID );
        FileResource anotherFileResource = createFileResource( 'B', "AnotherFileResource".getBytes() );
        anotherFileResource.setUid( ANOTHER_FILE_RESOURCE_UID );

        when( fileResourceService
            .getFileResources( Lists.newArrayList( ANOTHER_FILE_RESOURCE_UID, FILE_RESOURCE_UID ) ) )
                .thenReturn( Lists.newArrayList( fileResource, anotherFileResource ) );

        final TrackerImportParams params = TrackerImportParams
            .builder()
            .trackedEntities( Lists.newArrayList( getTrackedEntity() ) )
            .enrollments( Lists.newArrayList( getEnrollment() ) )
            .events( Lists.newArrayList( getEvent() ) )
            .build();

        this.supplierToTest.preheatAdd( params, preheat );

        assertThat( preheat.getAll( FileResource.class ), hasSize( 2 ) );
        assertThat( preheat.getAll( FileResource.class ), containsInAnyOrder( fileResource, anotherFileResource ) );
    }

    private TrackedEntity getTrackedEntity()
    {
        Attribute attribute = new Attribute();
        attribute.setAttribute( MetadataIdentifier.ofUid( NUMERIC_ATTRIBUTE_UID ) );
        attribute.setValueType( ValueType.NUMBER );

        TrackedEntity trackedEntity = new TrackedEntity();
        trackedEntity.setAttributes( Lists.newArrayList( attribute ) );

        return trackedEntity;
    }

    private Enrollment getEnrollment()
    {
        Attribute attribute = new Attribute();
        attribute.setAttribute( MetadataIdentifier.ofUid( FILE_RESOURCE_ATTRIBUTE_UID ) );
        attribute.setValueType( ValueType.FILE_RESOURCE );
        attribute.setValue( ANOTHER_FILE_RESOURCE_UID );

        Enrollment enrollment = new Enrollment();
        enrollment.setAttributes( Lists.newArrayList( attribute ) );

        return enrollment;
    }

    private Event getEvent()
    {
        DataValue fileResourceDataValue = new DataValue();
        fileResourceDataValue.setDataElement( FILE_RESOURCE_DATA_ELEMENT_UID );
        fileResourceDataValue.setValue( FILE_RESOURCE_UID );

        DataValue emptyFileResourceDataValue = new DataValue();
        emptyFileResourceDataValue.setDataElement( EMPTY_FILE_RESOURCE_DATA_ELEMENT_UID );
        emptyFileResourceDataValue.setValue( "" );

        DataValue nullFileResourceDataValue = new DataValue();
        nullFileResourceDataValue.setDataElement( NULL_FILE_RESOURCE_DATA_ELEMENT_UID );
        nullFileResourceDataValue.setValue( null );

        DataValue numericDataValue = new DataValue();
        numericDataValue.setDataElement( NUMERIC_DATA_ELEMENT_UID );
        numericDataValue.setValue( "3" );

        Event event = new Event();
        event.setDataValues(
            Sets.newHashSet( fileResourceDataValue, emptyFileResourceDataValue, nullFileResourceDataValue,
                numericDataValue ) );

        return event;
    }
}