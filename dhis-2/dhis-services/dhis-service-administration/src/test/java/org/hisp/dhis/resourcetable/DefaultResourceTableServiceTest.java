package org.hisp.dhis.resourcetable;

import static java.time.temporal.ChronoUnit.YEARS;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_PERIOD_YEARS_OFFSET;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.time.Year;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsExportSettings;
import org.hisp.dhis.period.PeriodDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultResourceTableServiceTest {

  @InjectMocks private DefaultResourceTableService defaultResourceTableService;

  @Mock private PeriodDataProvider periodDataProvider;

  @Mock private AnalyticsExportSettings analyticsExportSettings;

  @Mock private ResourceTableStore resourceTableStore;

  @Test
  void generateDatePeriodTableWhenYearIsOutOfRange() {
    // Given
    List<Integer> yearsToCheck = List.of(2000, 2001, 2002, 2003, 2004);

    // When
    when(periodDataProvider.getAvailableYears()).thenReturn(yearsToCheck);
    when(analyticsExportSettings.getPeriodYearsOffset())
        .thenReturn((Integer) ANALYTICS_PERIOD_YEARS_OFFSET.getDefaultValue());

    // Then
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> defaultResourceTableService.generateDatePeriodTable());

    assertTrue(
        exception.getMessage().contains("Your database contains years out of the allowed offset"));
  }

  @Test
  void generateDatePeriodTableWhenOffsetIsZeroWithPreviousYears() {
    // Given
    List<Integer> yearsToCheck = List.of(2000, 2001, 2002, 2003, 2004);
    int zeroOffset = 0;

    // When
    when(periodDataProvider.getAvailableYears()).thenReturn(yearsToCheck);
    when(analyticsExportSettings.getPeriodYearsOffset()).thenReturn(zeroOffset);

    // Then
    RuntimeException exception =
        assertThrows(
            RuntimeException.class, () -> defaultResourceTableService.generateDatePeriodTable());

    assertTrue(
        exception.getMessage().contains("Your database contains years out of the allowed offset"));
  }

  @Test
  void generateDatePeriodTableWhenOffsetIsZeroWithCurrentYear() {
    // Given
    List<Integer> yearsToCheck = List.of(Year.now().getValue());
    int zeroOffset = 0;

    // When
    when(periodDataProvider.getAvailableYears()).thenReturn(yearsToCheck);
    when(analyticsExportSettings.getPeriodYearsOffset()).thenReturn(zeroOffset);
    doNothing().when(resourceTableStore).generateResourceTable(any());

    // Then
    assertDoesNotThrow(() -> defaultResourceTableService.generateDatePeriodTable());
  }

  @Test
  void generateDatePeriodTableWhenYearIsInExpectedRange() {
    // Given
    List<Integer> yearsToCheck =
        List.of(Year.now().getValue(), Year.now().plus(1, YEARS).getValue());

    // When
    when(periodDataProvider.getAvailableYears()).thenReturn(yearsToCheck);
    when(analyticsExportSettings.getPeriodYearsOffset())
        .thenReturn((Integer) ANALYTICS_PERIOD_YEARS_OFFSET.getDefaultValue());
    doNothing().when(resourceTableStore).generateResourceTable(any());

    // Then
    assertDoesNotThrow(() -> defaultResourceTableService.generateDatePeriodTable());
  }
}
