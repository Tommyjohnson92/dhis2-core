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

package org.hisp.dhis.tracker.converter;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.ImportException;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Luciano Fiandesio
 */
public class TrackerBundleParamsConverterTest
{
    private ObjectMapper objectMapper = new ObjectMapper();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp()
    {
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
    }

    @Test
    public void verifyNestedTeiStructureIsFlattenedDuringDeserialization()
        throws IOException
    {
        List<Event> events1 = createEvent( 3, "ev1", "enr1" );
        List<Event> events2 = createEvent( 7, "ev2", "enr2" );

        List<Enrollment> enrollments = new ArrayList<>();
        enrollments.add( createEnrollment( "enr1", "teiABC", events1 ) );
        enrollments.add( createEnrollment( "enr2", "teiABC", events2 ) );

        TrackedEntity trackedEntity = createTrackedEntity( "teiABC", enrollments );

        TrackerBundleParams build = TrackerBundleParams.builder()
            .trackedEntities( Collections.singletonList( trackedEntity ) )
            .identifier( TrackerIdentifier.UID )
            .atomicMode( AtomicMode.ALL )
            .build();

        String jsonPayload = toJson( build );
        TrackerBundleParams b2 = this.objectMapper.readValue( jsonPayload, TrackerBundleParams.class );

        assertThat( b2.getTrackedEntities(), hasSize( 1 ) );
        assertThat( b2.getEnrollments(), hasSize( 2 ) );
        assertThat( b2.getEvents(), hasSize( 10 ) );

        assertThat( b2.getTrackedEntities().get(0).getEnrollments(), hasSize( 0 ));
    }

    @Test
    public void verifyNestedTeiStructureHasOrphanEnrollment()
            throws IOException
    {
        List<Event> events1 = createEvent( 3, "ev1", "enr1" );
        List<Event> events2 = createEvent( 7, "ev2", "enr2" );

        List<Enrollment> enrollments = new ArrayList<>();
        enrollments.add( createEnrollment( "enr1", "teiOrphan", events1 ) );
        enrollments.add( createEnrollment( "enr2", "teiABC", events2 ) );

        TrackedEntity trackedEntity = createTrackedEntity( "teiABC", enrollments );

        TrackerBundleParams build = TrackerBundleParams.builder()
            .trackedEntities( Collections.singletonList( trackedEntity ) )
            .identifier( TrackerIdentifier.UID )
            .atomicMode( AtomicMode.ALL )
            .build();

        exception.expect( ImportException.class );
        exception.expectMessage( "Invalid import payload. Enrollment with uid: enr1 is not a child of any Tracked Entity" );

        this.objectMapper.readValue( toJson( build ), TrackerBundleParams.class );

    }

    @Test
    public void verifyNestedTeiStructureHasOrphanEvent()
            throws IOException
    {
        List<Event> events1 = createEvent( 3, "ev1", "enr1" );
        List<Event> events2 = createEvent( 7, "ev2", "enrOrphan" );

        List<Enrollment> enrollments = new ArrayList<>();
        enrollments.add( createEnrollment( "enr1", "teiABC", events1 ) );
        enrollments.add( createEnrollment( "enr2", "teiABC", events2 ) );

        TrackedEntity trackedEntity = createTrackedEntity( "teiABC", enrollments );

        TrackerBundleParams build = TrackerBundleParams.builder()
                .trackedEntities( Collections.singletonList( trackedEntity ) ).identifier( TrackerIdentifier.UID ).build();

        exception.expect( ImportException.class );
        exception.expectMessage( "Invalid import payload. Event with uid: ev20 is not a child of any Enrollment" );

        this.objectMapper.readValue( toJson( build ), TrackerBundleParams.class );
    }

    @Test
    public void verifyNestedTeiStructureHasOrphanEnrollmentAndAtomicModeNONE()
            throws IOException
    {
        List<Event> events1 = createEvent( 3, "ev1", "enr1" );
        List<Event> events2 = createEvent( 7, "ev2", "enr2" );

        List<Enrollment> enrollments = new ArrayList<>();
        enrollments.add( createEnrollment( "enr1", "teiOrphan", events1 ) );
        enrollments.add( createEnrollment( "enr2", "teiABC", events2 ) );

        TrackedEntity trackedEntity = createTrackedEntity( "teiABC", enrollments );

        TrackerBundleParams build = TrackerBundleParams.builder()
                .trackedEntities( Collections.singletonList( trackedEntity ) )
                .identifier( TrackerIdentifier.UID )
                .atomicMode( AtomicMode.NONE )
                .build();

        TrackerBundleParams converted = this.objectMapper.readValue( toJson( build ), TrackerBundleParams.class );

        assertThat( converted.getTrackedEntities(), hasSize( 1 ) );
        assertThat( converted.getEnrollments(), hasSize( 1 ) );
        assertThat( converted.getEvents(), hasSize( 7 ) );
    }
    @Test
    public void verifyNestedTeiStructureHasOrphanEventAndAtomicModeNONE()
            throws IOException
    {
        List<Event> events1 = createEvent( 3, "ev1", "enr1" );
        List<Event> events2 = createEvent( 7, "ev2", "enrOrphan" );

        List<Enrollment> enrollments = new ArrayList<>();
        enrollments.add( createEnrollment( "enr1", "teiABC", events1 ) );
        enrollments.add( createEnrollment( "enr2", "teiABC", events2 ) );

        TrackedEntity trackedEntity = createTrackedEntity( "teiABC", enrollments );

        TrackerBundleParams build = TrackerBundleParams.builder()
            .trackedEntities( Collections.singletonList( trackedEntity ) )
            .atomicMode( AtomicMode.NONE )
            .identifier( TrackerIdentifier.UID ).build();

        TrackerBundleParams converted = this.objectMapper.readValue( toJson( build ), TrackerBundleParams.class );

        assertThat( converted.getTrackedEntities(), hasSize( 1 ) );
        assertThat( converted.getEnrollments(), hasSize( 2 ) );
        assertThat( converted.getEvents(), hasSize( 3 ) );
    }

    private TrackedEntity createTrackedEntity( String uid, List<Enrollment> enrollments )
    {
        TrackedEntity trackedEntity = rnd.randomObject( TrackedEntity.class, "enrollments" );
        trackedEntity.setGeometry( null );
        trackedEntity.setTrackedEntity( uid );
        trackedEntity.setEnrollments( enrollments );
        trackedEntity.setCoordinates( null );
        return trackedEntity;
    }

    private String toJson( TrackerBundleParams bundle )
        throws JsonProcessingException
    {
        return this.objectMapper.writeValueAsString( bundle );
    }

    private Enrollment createEnrollment( String uid, String parent, List<Event> events )
    {
        Enrollment enrollment = rnd.randomObject( Enrollment.class, "events" );
        enrollment.setGeometry( null );
        enrollment.setEnrollment( uid );
        enrollment.setTrackedEntityInstance( parent );
        enrollment.setEvents( events );
        enrollment.setCoordinate( null );
        return enrollment;
    }

    private List<Event> createEvent( int size, String uid, String parent )
    {
        List<Event> events = new ArrayList<>();
        for ( int i = 0; i < size; i++ )
        {
            Event event = rnd.randomObject( Event.class, "relationships" );
            event.setGeometry( null );
            event.setEvent( uid + i );
            event.setEnrollment( parent );
            event.setCoordinate( null );
            events.add( event );
        }

        return events;
    }
}