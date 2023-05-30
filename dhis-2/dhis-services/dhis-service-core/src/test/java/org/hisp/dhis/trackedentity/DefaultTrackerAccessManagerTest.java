package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith( MockitoExtension.class )
class DefaultTrackerAccessManagerTest {

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private DefaultTrackerAccessManager trackerAccessManager;

    @Test
    void shouldHaveAccessWhenProgramOpenAndSearchAccessAvailable() {
        User user = new User();
        Program program = new Program();
        program.setAccessLevel(OPEN);
        OrganisationUnit orgUnit = new OrganisationUnit();

        when(organisationUnitService.isInUserSearchHierarchy(user, orgUnit)).thenReturn(true);

        assertTrue(trackerAccessManager.canAccess(user, program, orgUnit),
                "User should have access to open program");
    }

    @Test
    void shouldNotHaveAccessWhenProgramOpenAndSearchAccessNotAvailable() {
        User user = new User();
        Program program = new Program();
        program.setAccessLevel(OPEN);
        OrganisationUnit orgUnit = new OrganisationUnit();

        when(organisationUnitService.isInUserSearchHierarchy(user, orgUnit)).thenReturn(false);

        assertFalse(trackerAccessManager.canAccess(user, program, orgUnit),
                "User should not have access to open program");
    }

    @Test
    void shouldHaveAccessWhenProgramNullAndSearchAccessAvailable() {
        User user = new User();
        OrganisationUnit orgUnit = new OrganisationUnit();

        when(organisationUnitService.isInUserSearchHierarchy(user, orgUnit)).thenReturn(true);

        assertTrue(trackerAccessManager.canAccess(user, null, orgUnit),
                "User should have access to unspecified program");
    }

    @Test
    void shouldNotHaveAccessWhenProgramNullAndSearchAccessNotAvailable() {
        User user = new User();
        OrganisationUnit orgUnit = new OrganisationUnit();

        when(organisationUnitService.isInUserSearchHierarchy(user, orgUnit)).thenReturn(false);

        assertFalse(trackerAccessManager.canAccess(user, null, orgUnit),
                "User should not have access to unspecified program");
    }

    @Test
    void shouldHaveAccessWhenProgramClosedAndCaptureAccessAvailable() {
        User user = new User();
        Program program = new Program();
        program.setAccessLevel(CLOSED);
        OrganisationUnit orgUnit = new OrganisationUnit();

        when(organisationUnitService.isInUserHierarchy(user, orgUnit)).thenReturn(true);

        assertTrue(trackerAccessManager.canAccess(user, program, orgUnit),
                "User should have access to closed program");
    }

    @Test
    void shouldNotHaveAccessWhenProgramClosedAndCaptureAccessNotAvailable() {
        User user = new User();
        Program program = new Program();
        program.setAccessLevel(CLOSED);
        OrganisationUnit orgUnit = new OrganisationUnit();

        when(organisationUnitService.isInUserHierarchy(user, orgUnit)).thenReturn(false);

        assertFalse(trackerAccessManager.canAccess(user, program, orgUnit),
                "User should not have access to closed program");
    }

    @Test
    void shouldHaveAccessWhenProgramProtectedAndCaptureAccessAvailable() {
        User user = new User();
        Program program = new Program();
        program.setAccessLevel(PROTECTED);
        OrganisationUnit orgUnit = new OrganisationUnit();

        when(organisationUnitService.isInUserHierarchy(user, orgUnit)).thenReturn(true);

        assertTrue(trackerAccessManager.canAccess(user, program, orgUnit),
                "User should have access to protected program");
    }

    @Test
    void shouldNotHaveAccessWhenProgramProtectedAndCaptureAccessNotAvailable() {
        User user = new User();
        Program program = new Program();
        program.setAccessLevel(PROTECTED);
        OrganisationUnit orgUnit = new OrganisationUnit();

        when(organisationUnitService.isInUserHierarchy(user, orgUnit)).thenReturn(false);

        assertFalse(trackerAccessManager.canAccess(user, program, orgUnit),
                "User should not have access to protected program");
    }
}