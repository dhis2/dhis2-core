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
package org.hisp.dhis.program;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Chau Thu Tran
 */
@Component( "org.hisp.dhis.program.ProgramStageInstanceDeletionHandler" )
public class ProgramStageInstanceDeletionHandler
    extends DeletionHandler
{
    private static final DeletionVeto VETO = new DeletionVeto( ProgramStageInstance.class );

    private final JdbcTemplate jdbcTemplate;

    private final ProgramStageInstanceService programStageInstanceService;

    public ProgramStageInstanceDeletionHandler( JdbcTemplate jdbcTemplate,
        ProgramStageInstanceService programStageInstanceService )
    {
        checkNotNull( jdbcTemplate );
        checkNotNull( programStageInstanceService );
        this.jdbcTemplate = jdbcTemplate;
        this.programStageInstanceService = programStageInstanceService;
    }

    @Override
    protected void register()
    {
        whenVetoing( ProgramStage.class, this::allowDeleteProgramStage );
        whenDeleting( ProgramInstance.class, this::deleteProgramInstance );
        whenVetoing( Program.class, this::allowDeleteProgram );
        whenVetoing( DataElement.class, this::allowDeleteDataElement );
    }

    private DeletionVeto allowDeleteProgramStage( ProgramStage programStage )
    {
        String sql = "SELECT COUNT(*) FROM programstageinstance WHERE programstageid = " + programStage.getId();

        return jdbcTemplate.queryForObject( sql, Integer.class ) == 0 ? ACCEPT : VETO;
    }

    private void deleteProgramInstance( ProgramInstance programInstance )
    {
        for ( ProgramStageInstance programStageInstance : programInstance.getProgramStageInstances() )
        {
            programStageInstanceService.deleteProgramStageInstance( programStageInstance );
        }
    }

    private DeletionVeto allowDeleteProgram( Program program )
    {
        String sql = "SELECT COUNT(*) FROM programstageinstance psi join programinstance pi on pi.programinstanceid=psi.programinstanceid where pi.programid = "
            + program.getId();

        return jdbcTemplate.queryForObject( sql, Integer.class ) == 0 ? ACCEPT : VETO;
    }

    private DeletionVeto allowDeleteDataElement( DataElement dataElement )
    {
        String sql = "select count(*) from programstageinstance where eventdatavalues ? '" + dataElement.getUid() + "'";

        return jdbcTemplate.queryForObject( sql, Integer.class ) == 0 ? ACCEPT : VETO;
    }
}
