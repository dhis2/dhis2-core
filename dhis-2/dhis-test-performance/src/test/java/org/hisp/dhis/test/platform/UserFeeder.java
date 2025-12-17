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
