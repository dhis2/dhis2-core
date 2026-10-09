/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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

import javax.annotation.Nonnull;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;

/**
 * A value of an attribute unique within an org unit, either stored in the DB or sent for more than
 * one tracked entity in the payload. A value of the attribute equal to it, ignoring case, is a
 * duplicate if it is in the same org unit, unless it belongs to the same tracked entity. A value
 * whose org unit is unknown (e.g. of an enrollment of a tracked entity that does not exist) can't
 * conflict with any other, so it has no entry.
 *
 * @param te the tracked entity owning the value
 * @param attribute the attribute, in the id scheme of the import
 * @param value the value
 * @param orgUnit the org unit of the tracked entity owning the value
 */
public record UniqueAttributeValueInOrgUnit(
    @Nonnull UID te,
    @Nonnull MetadataIdentifier attribute,
    @Nonnull String value,
    @Nonnull MetadataIdentifier orgUnit) {}
