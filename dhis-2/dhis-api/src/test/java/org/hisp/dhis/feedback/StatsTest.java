package org.hisp.dhis.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StatsTest {

  @ParameterizedTest
  @MethodSource("statsValues")
  @DisplayName("New Stats have expected values")
  void updatedStatsHaveExpectedValuesTest(Stats newStats, Stats expectedStats) {
    assertEquals(expectedStats, newStats);
  }

  @Test
  @DisplayName("Stats total has correct value")
  void statsTotalHasCorrectValueTest() {
    Stats stats = new Stats(1, 2, 3, 4);
    assertEquals(10, stats.getTotal());
  }

  public static Stream<Arguments> statsValues() {
    return Stream.of(
        Arguments.of(new Stats(0, 0, 0, 0).withCreated(1), new Stats(1, 0, 0, 0)),
        Arguments.of(new Stats(0, 0, 0, 0).withUpdated(1), new Stats(0, 1, 0, 0)),
        Arguments.of(new Stats(0, 0, 0, 0).withDeleted(1), new Stats(0, 0, 1, 0)),
        Arguments.of(new Stats(0, 0, 0, 0).withIgnored(1), new Stats(0, 0, 0, 1)),
        Arguments.of(new Stats(3, 4, 5, 6).withCreated(1), new Stats(4, 4, 5, 6)),
        Arguments.of(new Stats(3, 4, 5, 6).withUpdated(1), new Stats(3, 5, 5, 6)),
        Arguments.of(new Stats(3, 4, 5, 6).withDeleted(1), new Stats(3, 4, 6, 6)),
        Arguments.of(new Stats(3, 4, 5, 6).withIgnored(1), new Stats(3, 4, 5, 7)),
        Arguments.of(new Stats(0, 0, 0, 0).withStats(new Stats(1, 1, 1, 1)), new Stats(1, 1, 1, 1)),
        Arguments.of(
            new Stats(1, 2, 3, 4).withStats(new Stats(9, 9, 9, 0)), new Stats(10, 11, 12, 4)));
  }
}
