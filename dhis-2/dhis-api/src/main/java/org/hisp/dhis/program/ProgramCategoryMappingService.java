/*
 * Copyright (c) 2004-2022, University of Oslo
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

import java.util.List;

/**
 * @author Jim Grace
 */
public interface ProgramCategoryMappingService {
  // -------------------------------------------------------------------------
  // ProgramCategoryMapping
  // -------------------------------------------------------------------------

  /**
   * Adds a {@link ProgramCategoryMapping}
   *
   * @param programCategoryMapping The ProgramCategoryMapping to add.
   * @return A generated unique id of the added {@link ProgramCategoryMapping}.
   */
  long saveProgramCategoryMapping(ProgramCategoryMapping programCategoryMapping);

  /**
   * Deletes a {@link ProgramCategoryMapping}.
   *
   * @param programCategoryMapping the ProgramCategoryMapping to delete.
   */
  void deleteProgramCategoryMapping(ProgramCategoryMapping programCategoryMapping);

  /**
   * Updates an {@link ProgramCategoryMapping}.
   *
   * @param programCategoryMapping the ProgramCategoryMapping to update.
   */
  void updateProgramCategoryMapping(ProgramCategoryMapping programCategoryMapping);

  /**
   * Returns a {@link ProgramCategoryMapping}.
   *
   * @param id the id of the ProgramCategoryMapping to return.
   * @return the ProgramCategoryMapping with the given id
   */
  ProgramCategoryMapping getProgramCategoryMapping(long id);

  /**
   * Returns the {@link ProgramCategoryMapping} with the given UID.
   *
   * @param uid the UID.
   * @return the ProgramCategoryMapping with the given UID, or null if no match.
   */
  ProgramCategoryMapping getProgramCategoryMapping(String uid);

  /**
   * Retrieve all ProgramCategoryMappings associated with the given {@link Program}.
   *
   * @param program the Program.
   * @return a list og ProgramCategoryMappings.
   */
  List<ProgramCategoryMapping> getProgramCategoryMappingsByProgram(Program program);
}
