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
package org.hisp.dhis.db.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.UnaryOperator;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.util.SharingUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests database migration changes connected to use of JSONB columns and changes of values in these
 * columns.
 *
 * @author Jan Bernitt
 */
class JsonbTest {

  /**
   * Note that the update of the sharing access strings itself is tested more thorough in dedicated
   * tests for {@link Sharing#withAccess(UnaryOperator)}. Here we only want to verify that the
   * mapping from and to JSON happening around it also works.
   */
  @Test
  void updateSharing() throws Exception {
    String actual =
        SharingUtils.withAccess(
            "{\"owner\": \"Rbh43X53NBP\", \"users\": {}, \"public\": \"rw------\", \"external\": false, \"userGroups\": {}}",
            Sharing::copyMetadataToData);
    assertEquals(
        "{\"external\":false,\"owner\":\"Rbh43X53NBP\",\"public\":\"rwrw----\",\"userGroups\":{},\"users\":{}}",
        actual);
  }
}
