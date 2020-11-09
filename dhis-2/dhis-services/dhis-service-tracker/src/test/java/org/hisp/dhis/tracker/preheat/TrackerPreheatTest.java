package org.hisp.dhis.tracker.preheat;

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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerPreheatTest
{
    @Test
    public void testAllEmpty()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        assertTrue( preheat.isEmpty() );
        assertTrue( preheat.isEmpty( TrackerIdScheme.UID ) );
        assertTrue( preheat.isEmpty( TrackerIdScheme.CODE ) );
    }

    @Test
    public void testPutUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( TrackerIdentifier.UID, de1 );
        preheat.put( TrackerIdentifier.UID, de2 );
        preheat.put( TrackerIdentifier.UID, de3 );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( TrackerIdScheme.UID ) );
        assertTrue( preheat.isEmpty( TrackerIdScheme.CODE ) );

        assertTrue( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de3.getUid() ) );

        assertEquals( de1.getUid(), preheat.get( TrackerIdScheme.UID, DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de2.getUid(), preheat.get( TrackerIdScheme.UID, DataElement.class, de2.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( TrackerIdScheme.UID, DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    public void testPutCode()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de1.setCode( "Code1" );
        de2.setAutoFields();
        de2.setCode( "Code2" );
        de3.setAutoFields();
        de3.setCode( "Code3" );

        preheat.put( TrackerIdentifier.CODE, de1 );
        preheat.put( TrackerIdentifier.CODE, de2 );
        preheat.put( TrackerIdentifier.CODE, de3 );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( TrackerIdScheme.CODE ) );
        assertTrue( preheat.isEmpty( TrackerIdScheme.UID ) );

        assertTrue( preheat.containsKey( TrackerIdScheme.CODE, DataElement.class, de1.getCode() ) );
        assertTrue( preheat.containsKey( TrackerIdScheme.CODE, DataElement.class, de2.getCode() ) );
        assertTrue( preheat.containsKey( TrackerIdScheme.CODE, DataElement.class, de3.getCode() ) );

        assertEquals( de1.getCode(), preheat.get( TrackerIdScheme.CODE, DataElement.class, de1.getCode() ).getCode() );
        assertEquals( de2.getCode(), preheat.get( TrackerIdScheme.CODE, DataElement.class, de2.getCode() ).getCode() );
        assertEquals( de3.getCode(), preheat.get( TrackerIdScheme.CODE, DataElement.class, de3.getCode() ).getCode() );
    }

    @Test
    public void testPutCollectionUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( TrackerIdentifier.UID, Lists.newArrayList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( TrackerIdScheme.UID ) );
        assertTrue( preheat.isEmpty( TrackerIdScheme.CODE ) );

        assertTrue( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de3.getUid() ) );

        assertEquals( de1.getUid(), preheat.get( TrackerIdScheme.UID, DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de2.getUid(), preheat.get( TrackerIdScheme.UID, DataElement.class, de2.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( TrackerIdScheme.UID, DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    public void testPutCollectionRemoveOneUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( TrackerIdentifier.UID, Lists.newArrayList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( TrackerIdScheme.UID ) );
        assertTrue( preheat.isEmpty( TrackerIdScheme.CODE ) );

        preheat.remove( TrackerIdScheme.UID, DataElement.class, de2.getUid() );

        assertTrue( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de1.getUid() ) );
        assertFalse( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de3.getUid() ) );

        assertEquals( de1.getUid(), preheat.get( TrackerIdScheme.UID, DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( TrackerIdScheme.UID, DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    public void testPutCollectionRemoveAllUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( TrackerIdentifier.UID, Lists.newArrayList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( TrackerIdScheme.UID ) );
        assertTrue( preheat.isEmpty( TrackerIdScheme.CODE ) );

        preheat.remove( TrackerIdScheme.UID, DataElement.class,
            Lists.newArrayList( de1.getUid(), de2.getUid(), de3.getUid() ) );

        assertFalse( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de1.getUid() ) );
        assertFalse( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de2.getUid() ) );
        assertFalse( preheat.containsKey( TrackerIdScheme.UID, DataElement.class, de3.getUid() ) );
    }
    
    @Test
    public void testReferenceInvalidation()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        // Create root TEI
        TrackedEntityInstance tei = new TrackedEntityInstance();
        tei.setUid( CodeGenerator.generateUid() );
        List<TrackedEntityInstance> teiList = new ArrayList<>();
        teiList.add( tei );

        List<String> allEntities = new ArrayList<>();
        allEntities.add( CodeGenerator.generateUid() );

        preheat.putTrackedEntities( TrackerIdScheme.UID, teiList, allEntities );

        // Create 2 Enrollments, where TEI is parent
        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setUid( CodeGenerator.generateUid() );
        List<ProgramInstance> psList = new ArrayList<>();
        psList.add( programInstance );

        List<Enrollment> allPs = new ArrayList<>();
        allPs.add( new Enrollment()
        {
            {
                String uid = CodeGenerator.generateUid();
                setUid( uid );
                setEnrollment( uid );
                setTrackedEntity( allEntities.get( 0 ) );
            }
        } );
        allPs.add( new Enrollment()
        {
            {
                String uid = CodeGenerator.generateUid();
                setUid( uid );
                setEnrollment( uid );
                setTrackedEntity( allEntities.get( 0 ) );
            }
        } );

        preheat.putEnrollments( TrackerIdScheme.UID, psList, allPs );

        // Create 4 Enrollments, where TEI is parent
        ProgramStageInstance psi = new ProgramStageInstance();
        psi.setUid( CodeGenerator.generateUid() );
        List<ProgramStageInstance> psiList = new ArrayList<>();
        psiList.add( psi );

        List<Event> allEvents = new ArrayList<>();
        allEvents.add( new Event()
        {
            {
                String uid = CodeGenerator.generateUid();
                setUid( uid );
                setEvent( uid );
                setEnrollment( allPs.get( 0 ).getEnrollment() );
            }
        } );
        allEvents.add( new Event()
        {
            {
                String uid = CodeGenerator.generateUid();
                setUid( uid );
                setEvent( uid );
                setEnrollment( allPs.get( 0 ).getEnrollment() );
            }
        } );
        allEvents.add( new Event()
        {
            {
                String uid = CodeGenerator.generateUid();
                setUid( uid );
                setEvent( uid );
                setEnrollment( allPs.get( 1 ).getEnrollment() );
            }
        } );
        allEvents.add( new Event()
        {
            {
                String uid = CodeGenerator.generateUid();
                setUid( uid );
                setEvent( uid );
                setEnrollment( allPs.get( 1 ).getEnrollment() );
            }
        } );

        preheat.putEvents( TrackerIdScheme.UID, psiList, allEvents );

        Optional<ReferenceTrackerEntity> reference = preheat.getReference( allEvents.get( 0 ).getUid() );
        assertThat( reference.get().getUid(), is( allEvents.get( 0 ).getUid() ) );
        assertThat( reference.get().getParentUid(), is( allPs.get( 0 ).getUid() ) );

        Optional<ReferenceTrackerEntity> reference2 = preheat.getReference( allEvents.get( 1 ).getUid() );
        assertThat( reference2.get().getUid(), is( allEvents.get( 1 ).getUid() ) );
        assertThat( reference2.get().getParentUid(), is( allPs.get( 0 ).getUid() ) );

        Optional<ReferenceTrackerEntity> reference3 = preheat.getReference( allEvents.get( 2 ).getUid() );
        assertThat( reference3.get().getUid(), is( allEvents.get( 2 ).getUid() ) );
        assertThat( reference3.get().getParentUid(), is( allPs.get( 1 ).getUid() ) );

        Optional<ReferenceTrackerEntity> reference4 = preheat.getReference( allEvents.get( 3 ).getUid() );
        assertThat( reference4.get().getUid(), is( allEvents.get( 3 ).getUid() ) );
        assertThat( reference4.get().getParentUid(), is( allPs.get( 1 ).getUid() ) );
    }
}
