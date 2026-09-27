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
package org.hisp.dhis.tracker.imports.preheat;

import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;

/**
 * Data elements of a program stage, projected instead of loaded as entities.
 *
 * <p>These replace walking {@link org.hisp.dhis.program.ProgramStage#getProgramStageDataElements()}
 * on a preheated stage, which is no longer mapped. A program can have thousands of data elements
 * while an event references a handful, so loading them all as entities dominated preheat on wide
 * programs. Only the data elements needed to answer the validations are projected: the compulsory
 * ones, plus those referenced by the payload or by program rules. See {@link
 * org.hisp.dhis.tracker.imports.preheat.supplier.ProgramStageDataElementsSupplier}.
 *
 * @param compulsory identifiers of the compulsory data elements, used to report a missing mandatory
 *     value
 * @param members identifiers of the projected data elements, used to check that a payload data
 *     element belongs to the stage
 * @param memberUids uids of the projected data elements. Program rules only know uids, so this is
 *     kept separately from {@code members} which is in the requested idScheme
 */
public record ProgramStageDataElements(
    Set<MetadataIdentifier> compulsory, Set<MetadataIdentifier> members, Set<UID> memberUids) {

  public static final ProgramStageDataElements EMPTY =
      new ProgramStageDataElements(Set.of(), Set.of(), Set.of());
}
