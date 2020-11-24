package org.hisp.dhis.program;

import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 *
 */
@Service( "org.hisp.dhis.program.ProgramTempOwnerService" )
public class DefaultProgramTempOwnerService implements ProgramTempOwnerService
{

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private ProgramTempOwnerStore programTempOwnerStore;

    // -------------------------------------------------------------------------
    // ProgramTempOwnershipAuditService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void addProgramTempOwner( ProgramTempOwner programTempOwner )
    {
        programTempOwnerStore.addProgramTempOwner( programTempOwner );
    }
    
    @Override
    @Transactional( readOnly = true )
    public int getValidTempOwnerRecordCount( Program program, TrackedEntityInstance entityInstance, User user )
    {
        return programTempOwnerStore.getValidTempOwnerCount( program, entityInstance, user);
    }

}