/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.test.platform;

import static io.gatling.javaapi.core.CoreDsl.listFeeder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.FeederBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Class that provides user feeders for use in scenarios. The user data will be used by the virtual
 * users in the tests. <br>
 * Creating small feeder objects can be done this way, using small files etc., if larger user
 * datasets are required then it may be more suitable to have that data already in a purpose-built
 * database e.g. with 1k/10k/100k users. <br>
 * The data used in the feeder expects that valid underlying users already exist in DHIS2.
 */
public class UserFeeder {

  /**
   * Create an in-memory user feeder. The virtual users are used to send requests in scenarios.
   *
   * @param fileName file name on the class path. e.g. a file like <code>
   *     src/test/resources/platform/filea.txt</code> can be passed as <code>platform/filea.txt
   *     </code>
   * @param strategy feeder type, <a
   *     href="https://docs.gatling.io/concepts/session/feeders/#strategies">read docs</a> for more
   *     info
   * @return the in-memory feeder
   */
  public static FeederBuilder<Object> createUserFeederFromFile(String fileName, Strategy strategy) {
    ObjectMapper mapper = new ObjectMapper();
    InputStream is = UserFeeder.class.getClassLoader().getResourceAsStream(fileName);

    if (is == null) {
      throw new IllegalArgumentException("Resource not found: " + fileName);
    }

    JsonNode root = null;
    try {
      root = mapper.readTree(is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Gatling user feeder expects array of objects, so extract the "users" array
    JsonNode usersArray = root.get("users");
    if (usersArray == null) throw new RuntimeException("No users array found in file: " + fileName);

    List<Map<String, Object>> users =
        mapper.convertValue(
            usersArray, mapper.getTypeFactory().constructCollectionType(List.class, Map.class));

    return switch (strategy) {
      case CIRCULAR -> listFeeder(users).circular();
      case QUEUE -> listFeeder(users).queue();
      case RANDOM -> listFeeder(users).random();
      case SHUFFLE -> listFeeder(users).shuffle();
    };
  }

  public enum Strategy {
    CIRCULAR,
    QUEUE,
    RANDOM,
    SHUFFLE
  }
}
