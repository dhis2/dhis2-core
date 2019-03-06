package org.hisp.dhis.programstagefilter;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.List;
import org.hisp.dhis.user.User;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 *
 */
public interface ProgramStageInstanceFilterService
{
    String ID = ProgramStageInstanceFilter.class.getName();
    
    /** 
     * Adds programStageInstanceFilter
     * 
     * @param programStageInstanceFilter
     * @return id of added programStageInstanceFilter
     */
    long add( ProgramStageInstanceFilter programStageInstanceFilter );
    
    /**
     * Deletes programStageInstanceFilter
     * 
     * @param programStageInstanceFilter
     */
    void delete( ProgramStageInstanceFilter programStageInstanceFilter );
    
    /**
     * Updates programStageInstanceFilter
     * 
     * @param programStageInstanceFilter
     */
    void update( ProgramStageInstanceFilter programStageInstanceFilter );
    
    /**
     * Gets programStageInstanceFilter 
     * @param id id of programStageInstanceFilter to be fetched
     * @return programStageInstanceFilter
     */
    ProgramStageInstanceFilter get( long id );
    
    /**
     * Gets programStageInstanceFilter
     * @param user user who created the programStageInstanceFilter to be fetched
     * @return programStageInstanceFilter
     */
    List<ProgramStageInstanceFilter> get( User user );
    
    /**
     * Gets all programStageInstanceFilters
     * @return list of programStageInstanceFilters
     */
    List<ProgramStageInstanceFilter> getAll();

}
