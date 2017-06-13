package org.hisp.dhis.dataset;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

public interface SectionService
{
    String ID = SectionService.class.getName();
    
    /**
     * Adds a Section.
     * 
     * @param section the Section to add.
     * @return the generated identifier.
     */
    int addSection( Section section );

    /**
     * Updates a Section.
     * 
     * @param section the Section to update.
     */
    void updateSection( Section section );

    /**
     * Deletes a Section.
     * 
     * @param section the Section to delete.
     */
    void deleteSection( Section section );
   
    /**
     * Retrieves the Section with the given identifier.
     * 
     * @param id the identifier of the Section to retrieve.
     * @return the Section.
     */
    Section getSection( int id );

    /**
     * Retrieves the Section with the given identifier (uid).
     *
     * @param uid the identifier of the Section to retrieve.
     * @return the Section.
     */
    Section getSection( String uid );

    /**
     * Retrieves the Section with the given name.
     * 
     * @param name the name of the Section to retrieve.
     * @return the Section.
     */
    Section getSectionByName( String name, Integer dataSetId );
    
    /**
     * Retrieves all Sections.
     * 
     * @return a Collection of Sections.
     */
    List<Section> getAllSections();  
    
}
