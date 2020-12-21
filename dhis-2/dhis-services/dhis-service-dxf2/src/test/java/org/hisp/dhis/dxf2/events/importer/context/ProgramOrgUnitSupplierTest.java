package org.hisp.dhis.dxf2.events.importer.context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.junit.Before;

import com.google.common.collect.ImmutableList;

/**
 * @author Luciano Fiandesio
 */
public class ProgramOrgUnitSupplierTest extends AbstractSupplierTest<Long>
{
    private ProgramOrgUnitSupplier subject;

    @Before
    public void setUp()
    {
        this.subject = new ProgramOrgUnitSupplier( jdbcTemplate );
    }

    @Override
    public void verifySupplier()
        throws SQLException
    {
        // Org Unit //
        OrganisationUnit ou1 = new OrganisationUnit();
        ou1.setId( 1 );
        ou1.setUid( "abcded" );

        OrganisationUnit ou2 = new OrganisationUnit();
        ou2.setId( 2 );
        ou2.setUid( "fgfgfg" );

        // create 2 events to import - each one pointing to a different org unit
        Event event = new Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setOrgUnit( "abcded" );

        Event event2 = new Event();
        event2.setUid( CodeGenerator.generateUid() );
        event2.setOrgUnit( "fgfgfg" );

        Map<String, OrganisationUnit> organisationUnitMap = new HashMap<>();
        organisationUnitMap.put( event.getOrgUnit(), ou1 );
        organisationUnitMap.put( event.getOrgUnit(), ou2 );

        when( mockResultSet.next() ).thenReturn( true ).thenReturn( true ).thenReturn( false );

        when( mockResultSet.getLong( "programid" ) ).thenReturn( 100L );
        when( mockResultSet.getLong( "organisationunitid" ) ).thenReturn( 1L, 2L );

        // mock result-set extraction
        mockResultSetExtractor( mockResultSet );

        final Map<Long, List<Long>> longListMap = subject.get( ImportOptions.getDefaultImportOptions(),
            ImmutableList.of( event, event2 ), organisationUnitMap );

        assertThat( longListMap.keySet(), hasSize( 1 ) );
        assertThat( longListMap.get( 100L ), hasSize( 2 ) );
        assertThat( longListMap.get( 100L ), containsInAnyOrder( 1L, 2L ) );
    }
}