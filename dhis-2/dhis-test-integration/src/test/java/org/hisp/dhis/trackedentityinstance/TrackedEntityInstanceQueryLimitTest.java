package org.hisp.dhis.trackedentityinstance;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.google.common.collect.Sets;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * @author Zubair Asghar
 */
public class TrackedEntityInstanceQueryLimitTest extends TransactionalIntegrationTest
{
    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityTypeService trackedEntityTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private UserService _userService;

    private OrganisationUnit orgUnitA;

    private Program program;

    private ProgramInstance pi1;
    private ProgramInstance pi2;
    private ProgramInstance pi3;
    private ProgramInstance pi4;

    private TrackedEntityInstance tei1;
    private TrackedEntityInstance tei2;
    private TrackedEntityInstance tei3;
    private TrackedEntityInstance tei4;

    private TrackedEntityType teiType;

    private User user;


    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    protected void setUpTest()
            throws Exception
    {
        userService = _userService;
        user = createAndInjectAdminUser();

        orgUnitA = createOrganisationUnit("A" );
        organisationUnitService.addOrganisationUnit( orgUnitA );

        user.getOrganisationUnits().add( orgUnitA );

        teiType = createTrackedEntityType( 'P' );
        trackedEntityTypeService.addTrackedEntityType( teiType );

        program = createProgram( 'P' );
        programService.addProgram( program );

        tei1 = createTrackedEntityInstance( orgUnitA );
        tei2 = createTrackedEntityInstance( orgUnitA );
        tei3 = createTrackedEntityInstance( orgUnitA );
        tei4 = createTrackedEntityInstance( orgUnitA );
        tei1.setTrackedEntityType( teiType );
        tei2.setTrackedEntityType( teiType );
        tei3.setTrackedEntityType( teiType );
        tei4.setTrackedEntityType( teiType );

        trackedEntityInstanceService.addTrackedEntityInstance( tei1 );
        trackedEntityInstanceService.addTrackedEntityInstance( tei2 );
        trackedEntityInstanceService.addTrackedEntityInstance( tei3 );
        trackedEntityInstanceService.addTrackedEntityInstance( tei4 );

        pi1 = createProgramInstance( program, tei1, orgUnitA );
        pi2 = createProgramInstance( program, tei2, orgUnitA );
        pi3 = createProgramInstance( program, tei3, orgUnitA );
        pi4 = createProgramInstance( program, tei4, orgUnitA );

        programInstanceService.addProgramInstance( pi1 );
        programInstanceService.addProgramInstance( pi2 );
        programInstanceService.addProgramInstance( pi3 );
        programInstanceService.addProgramInstance( pi4 );

        programInstanceService.enrollTrackedEntityInstance( tei1, program, new Date(), new Date(), orgUnitA);
        programInstanceService.enrollTrackedEntityInstance( tei2, program, new Date(), new Date(), orgUnitA);
        programInstanceService.enrollTrackedEntityInstance( tei3, program, new Date(), new Date(), orgUnitA);
        programInstanceService.enrollTrackedEntityInstance( tei4, program, new Date(), new Date(), orgUnitA);

        userService.addUser( user );
    }

    @Test
    public void testTeiQueryLimitDefaultValue()
    {
        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.setProgram( program );
        params.setOrganisationUnits(Sets.newHashSet( orgUnitA ) );
        params.setOrganisationUnitMode( OrganisationUnitSelectionMode.ALL );
        params.setUser( user );
        params.setInternalSearch(false);

        List<Long> teis = trackedEntityInstanceService.getTrackedEntityInstanceIds( params, false, false );

        assertFalse( teis.isEmpty() );
        assertEquals( 4, teis.size() );
    }
}
