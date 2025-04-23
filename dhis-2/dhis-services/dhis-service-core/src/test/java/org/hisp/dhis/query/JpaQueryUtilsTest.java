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
package org.hisp.dhis.query;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JpaQueryUtils}.
 *
 * @author volsch
 */
class JpaQueryUtilsTest {

  @Test
  void testGenerateSQlQueryForSharingCheck() {
    UserGroup groupA = new UserGroup();
    groupA.setUid("aUserGroupA");
    UserGroup groupB = new UserGroup();
    groupB.setUid("aUserGroupB");
    User userA = new User();
    userA.setUid("randomUserA");
    userA.setGroups(Sets.newLinkedHashSet(Lists.newArrayList(groupA, groupB)));
    String expected =
        " ( x.sharing->>'owner' is null or x.sharing->>'owner' = 'randomUserA')  "
            + "or x.sharing->>'public' like '__r_____' or x.sharing->>'public' is null  "
            + "or (jsonb_has_user_id( x.sharing, 'randomUserA') = true  "
            + "and jsonb_check_user_access( x.sharing, 'randomUserA', '__r_____' ) = true )   "
            + "or ( jsonb_has_user_group_ids( x.sharing, '{aUserGroupA,aUserGroupB}') = true  "
            + "and jsonb_check_user_groups_access( x.sharing, '__r_____', '{aUserGroupA,aUserGroupB}') = true )";
    String actual =
        JpaQueryUtils.generateSQlQueryForSharingCheck(
            "x.sharing", UserDetails.fromUser(userA), "__r_____");
    Assertions.assertEquals(expected, actual);
  }
}
