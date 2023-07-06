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
package org.hisp.dhis;

import java.util.Arrays;
import org.hisp.dhis.dto.Program;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class Constants {
  public static final String USER_PASSWORD = "Test1212?";

  public static final String TRACKED_ENTITY_TYPE = "Q9GufDoplCL";

  public static String ORG_UNIT_GROUP_ID = "n9bh3KM5wmu";

  public static String SUPER_USER_ID = "PQD6wXJ2r5j";

  public static String ADMIN_ID = "PQD6wXJ2r5k";

  public static String USER_GROUP_ID = "OPVIvvXzNTw";

  public static String USER_ROLE_ID = "yrB6vc5Ip7r";

  public static String EVENT_PROGRAM_ID = "Zd2rkv8FsWq";

  public static String EVENT_PROGRAM_STAGE_ID = "jKLB23QZS4I";

  public static Program TRACKER_PROGRAM =
      new Program()
          .setUid("f1AyMswryyQ")
          .setProgramStages(Arrays.asList("PaOOjwLVW23", "nlXNK4b7LVr"));

  public static String TRACKER_PROGRAM_ID = "f1AyMswryyQ"; // todo: remove and
  // use
  // TRACKER_PROGRAM
  // with associated
  // program stages
  // to avoid GET
  // /programs/id/programStages
  // calls

  public static String ANOTHER_TRACKER_PROGRAM_ID = "f1AyMswryyX";

  public static String[] ORG_UNIT_IDS = {
    "DiszpKrYNg8", "g8upMTyEZGZ", "O6uvpzGd5pu", "YuQRtpLP10I"
  };
}
