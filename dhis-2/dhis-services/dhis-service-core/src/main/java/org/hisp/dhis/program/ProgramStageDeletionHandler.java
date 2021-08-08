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

import java.util.Iterator;
import java.util.List;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Component( "org.hisp.dhis.program.ProgramStageDeletionHandler" )
public class ProgramStageDeletionHandler
    extends DeletionHandler
{
    private static final DeletionVeto VETO = new DeletionVeto( ProgramStage.class );

    private final ProgramStageService programStageService;

    private final JdbcTemplate jdbcTemplate;

    public ProgramStageDeletionHandler( ProgramStageService programStageService, JdbcTemplate jdbcTemplate )
    {
        checkNotNull( programStageService );
        checkNotNull( jdbcTemplate );
        this.programStageService = programStageService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void register()
    {
        whenDeleting( Program.class, this::deleteProgram );
        whenDeleting( DataEntryForm.class, this::deleteDataEntryForm );
        whenVetoing( DataElement.class, this::allowDeleteDataElement );
    }

    private void deleteProgram( Program program )
    {
        Iterator<ProgramStage> iterator = program.getProgramStages().iterator();

        while ( iterator.hasNext() )
        {
            ProgramStage programStage = iterator.next();
            iterator.remove();
            programStageService.deleteProgramStage( programStage );
        }
    }

    private void deleteDataEntryForm( DataEntryForm dataEntryForm )
    {
        List<ProgramStage> associatedProgramStages = programStageService
            .getProgramStagesByDataEntryForm( dataEntryForm );

        for ( ProgramStage programStage : associatedProgramStages )
        {
            programStage.setDataEntryForm( null );
            programStageService.updateProgramStage( programStage );
        }
    }

    private DeletionVeto allowDeleteDataElement( DataElement dataElement )
    {
        String sql = "SELECT COUNT(*) FROM programstagedataelement WHERE dataelementid=" + dataElement.getId();

        return jdbcTemplate.queryForObject( sql, Integer.class ) == 0 ? ACCEPT : VETO;
    }
}
