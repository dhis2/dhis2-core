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
package org.hisp.dhis.dataitem.query.shared;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;

/**
 * This class keeps the list of possible query params.
 *
 * @author maikel arabori
 */
@NoArgsConstructor( access = PRIVATE )
public class QueryParam
{
    public static final String NAME = "name";

    public static final String SHORT_NAME = "shortName";

    public static final String DISPLAY_NAME = "displayName";

    public static final String DISPLAY_SHORT_NAME = "displayShortName";

    public static final String LOCALE = "locale";

    public static final String VALUE_TYPES = "valueTypes";

    public static final String USER_GROUP_UIDS = "userGroupUids";

    public static final String USER_UID = "userUid";

    public static final String PROGRAM_ID = "programId";

    public static final String MAX_LIMIT = "maxLimit";

    public static final String NAME_ORDER = "nameOrder";

    public static final String SHORT_NAME_ORDER = "shortNameOrder";

    public static final String DISPLAY_NAME_ORDER = "displayNameOrder";

    public static final String DISPLAY_SHORT_NAME_ORDER = "displayShortNameOrder";

    public static final String UID = "uid";

    public static final String ROOT_JUNCTION = "rootJunction";

    public static final String IDENTIFIABLE_TOKEN_COMPARISON = "identifiableTokenComparison";
}
