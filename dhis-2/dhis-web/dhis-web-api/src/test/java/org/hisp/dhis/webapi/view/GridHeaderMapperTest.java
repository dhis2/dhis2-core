package org.hisp.dhis.webapi.view;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hisp.dhis.analytics.event.HeaderName.NAME_ENROLLMENT_DATE;
import static org.hisp.dhis.analytics.event.HeaderName.NAME_EVENT_DATE;
import static org.hisp.dhis.analytics.event.HeaderName.NAME_INCIDENT_DATE;
import static org.hisp.dhis.analytics.event.HeaderName.NAME_PROGRAM_STAGE;
import static org.mockito.Mockito.when;

import org.hisp.dhis.analytics.event.HeaderName;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageStore;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Unit tests for GridHeaderMapper.
 * 
 * @author maikel arabori
 */
public class GridHeaderMapperTest
{
    private static final int HEADER_PROGRAM_STAGE_INDEX = 0;

    private static final int HEADER_LABEL_NAME_INDEX = 1;

    @Mock
    private ProgramStageStore programStageStore;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private GridHeaderMapper gridHeaderMapper;

    @Before
    public void setUp()
    {
        gridHeaderMapper = new GridHeaderMapper( programStageStore );
    }

    @Test
    public void testMaybeOverrideHeaderNamesWhenHeaderNameIs_NAME_EVENT_DATE()
    {
        // Given
        final ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();
        final String theProgramStageUid = mockProgramStageUid();
        final Grid aMockedGrid = mockGridWith( NAME_EVENT_DATE, NAME_PROGRAM_STAGE );

        // When
        when( programStageStore.getByUid( theProgramStageUid ) ).thenReturn( aMockedProgramStageWithLabels );
        gridHeaderMapper.maybeOverrideHeaderNames( aMockedGrid );

        // Then
        assertThat( aMockedGrid.getHeaders().get( HEADER_LABEL_NAME_INDEX ).getColumn(),
            is( aMockedProgramStageWithLabels.getExecutionDateLabel() ) );
    }

    @Test
    public void testMaybeOverrideHeaderNamesWhenHeaderNameIs_NAME_ENROLLMENT_DATE()
    {
        // Given
        final ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();
        final String theProgramStageUid = mockProgramStageUid();
        final Grid aMockedGrid = mockGridWith( NAME_ENROLLMENT_DATE, NAME_PROGRAM_STAGE );

        // When
        when( programStageStore.getByUid( theProgramStageUid ) ).thenReturn( aMockedProgramStageWithLabels );
        gridHeaderMapper.maybeOverrideHeaderNames( aMockedGrid );

        // Then
        assertThat( aMockedGrid.getHeaders().get( HEADER_LABEL_NAME_INDEX ).getColumn(),
            is( aMockedProgramStageWithLabels.getProgram().getEnrollmentDateLabel() ) );
    }

    @Test
    public void testMaybeOverrideHeaderNamesWhenHeaderNameIs_NAME_INCIDENT_DATE()
    {
        // Given
        final ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();
        final String theProgramStageUid = mockProgramStageUid();
        final Grid aMockedGrid = mockGridWith( NAME_INCIDENT_DATE, NAME_PROGRAM_STAGE );

        // When
        when( programStageStore.getByUid( theProgramStageUid ) ).thenReturn( aMockedProgramStageWithLabels );
        gridHeaderMapper.maybeOverrideHeaderNames( aMockedGrid );

        // Then
        assertThat( aMockedGrid.getHeaders().get( HEADER_LABEL_NAME_INDEX ).getColumn(),
            is( aMockedProgramStageWithLabels.getProgram().getIncidentDateLabel() ) );
    }

    @Test
    public void testMaybeOverrideHeaderNamesWhenThereIsNoProgramStage()
    {
        // Given
        final ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();
        final String theProgramStageUid = mockProgramStageUid();
        final Grid aMockedGrid = mockGridWith( NAME_INCIDENT_DATE, null );

        // When
        when( programStageStore.getByUid( theProgramStageUid ) ).thenReturn( aMockedProgramStageWithLabels );
        gridHeaderMapper.maybeOverrideHeaderNames( aMockedGrid );

        // Then
        assertThat( aMockedGrid.getHeaders().get( HEADER_PROGRAM_STAGE_INDEX ).getColumn(),
            is( nullValue() ) );
    }

    private String mockProgramStageUid()
    {
        return "abcdefgh";
    }

    private Grid mockGridWith( final HeaderName headerName, final HeaderName programStageHeader )
    {
        final Grid grid = new ListGrid();
        grid.addRow();
        grid.getRow( 0 ).add( mockProgramStageUid() );

        final GridHeader programStageGridHeader = new GridHeader();
        programStageGridHeader.setColumn( programStageHeader != null ? programStageHeader.value() : null );

        final GridHeader eventDataHeader = new GridHeader();
        eventDataHeader.setColumn( headerName.value() );

        grid.addHeader( programStageGridHeader );
        grid.addHeader( eventDataHeader );

        return grid;
    }

    private ProgramStage mockProgramStageWithLabels()
    {
        final ProgramStage programStage = new ProgramStage();
        programStage.setExecutionDateLabel( "execution date label" );

        final Program program = new Program();
        program.setEnrollmentDateLabel( "enrollment date label" );
        program.setIncidentDateLabel( "incident date label" );

        programStage.setProgram( program );

        return programStage;
    }

    private ProgramStage mockProgramStageWithoutLabels()
    {
        final ProgramStage programStage = new ProgramStage();
        final Program program = new Program();

        programStage.setProgram( program );

        return programStage;
    }
}
