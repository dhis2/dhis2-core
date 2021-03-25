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
package org.hisp.dhis.tracker.preheat.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
public class FileResourceSupplierTest
    extends DhisConvenienceTest
{
    private FileResourceSupplier supplierToTest;

    private TrackerPreheat preheat = new TrackerPreheat();

    @Mock
    private FileResourceService fileResourceService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp()
    {
        DataElement numericDataElement = createDataElement( 'A' );
        numericDataElement.setUid( "numericDataElement" );
        numericDataElement.setValueType( ValueType.NUMBER );

        DataElement fileResourceDataElement = createDataElement( 'B' );
        fileResourceDataElement.setUid( "fileResourceDataElement" );
        fileResourceDataElement.setValueType( ValueType.FILE_RESOURCE );

        preheat.put( TrackerIdentifier.UID, Lists.newArrayList( numericDataElement, fileResourceDataElement ) );

        TrackedEntityAttribute numericAttribute = createTrackedEntityAttribute( 'A' );
        numericAttribute.setUid( "numericAttribute" );
        numericAttribute.setValueType( ValueType.NUMBER );

        TrackedEntityAttribute fileResourceAttribute = createTrackedEntityAttribute( 'A' );
        fileResourceAttribute.setUid( "fileResourceAttribute" );
        fileResourceAttribute.setValueType( ValueType.FILE_RESOURCE );

        preheat.put( TrackerIdentifier.UID, Lists.newArrayList( numericAttribute, fileResourceAttribute ) );

        supplierToTest = new FileResourceSupplier( fileResourceService );
    }

    @Test
    public void verifySupplier()
    {
        FileResource fileResource = createFileResource( 'A', "FileResource".getBytes() );
        fileResource.setUid( "FileResourceUid" );
        FileResource anotherFileResource = createFileResource( 'B', "AnotherFileResource".getBytes() );
        anotherFileResource.setUid( "AnotherFileResourceUid" );

        when( fileResourceService
            .getFileResources( Lists.newArrayList( "AnotherFileResourceUid", "FileResourceUid" ) ) )
                .thenReturn( Lists.newArrayList( fileResource, anotherFileResource ) );

        final TrackerImportParams params = TrackerImportParams
            .builder()
            .trackedEntities( Lists.newArrayList( getTrackedEntity() ) )
            .enrollments( Lists.newArrayList( getEnrollment() ) )
            .events( Lists.newArrayList( getEvent() ) )
            .build();

        this.supplierToTest.preheatAdd( params, preheat );

        assertThat( preheat.getAll( FileResource.class ), hasSize( 2 ) );
    }

    private TrackedEntity getTrackedEntity()
    {
        Attribute attribute = new Attribute();
        attribute.setAttribute( "numericAttribute" );
        attribute.setValueType( ValueType.NUMBER );

        TrackedEntity trackedEntity = new TrackedEntity();
        trackedEntity.setTrackedEntity( "TrackedEntity" );
        trackedEntity.setAttributes( Lists.newArrayList( attribute ) );

        return trackedEntity;
    }

    private Enrollment getEnrollment()
    {
        Attribute attribute = new Attribute();
        attribute.setAttribute( "fileResourceAttribute" );
        attribute.setValueType( ValueType.FILE_RESOURCE );
        attribute.setValue( "AnotherFileResourceUid" );

        Enrollment enrollment = new Enrollment();
        enrollment.setAttributes( Lists.newArrayList( attribute ) );

        return enrollment;
    }

    private Event getEvent()
    {
        DataValue dataValue = new DataValue();
        dataValue.setDataElement( "fileResourceDataElement" );
        dataValue.setValue( "FileResourceUid" );

        Event event = new Event();
        event.setDataValues( Sets.newHashSet( dataValue ) );

        return event;
    }
}