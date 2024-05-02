package org.hisp.dhis.merge.dataelement;

import java.util.List;
import java.util.function.BiPredicate;
import javax.annotation.Nonnull;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.feedback.MergeReport;
import org.springframework.stereotype.Component;

@Component
public class DataElementMergeValidator {

  /**
   * Validation method that checks for any mismatches between source {@link DataElement} property
   * values and target {@link DataElement} property values, using a {@link
   * DataElementPropertyCheck}. If there are any mismatches then an error is added to the {@link
   * MergeReport}. Any error will provide the details of the mismatches, if any found.
   *
   * @param target target {@link DataElement}
   * @param sources source {@link DataElement}s
   * @param propertyCheck {@link DataElementPropertyCheck} to check against
   * @param report {@link MergeReport}
   * @return report
   */
  public MergeReport validateMismatches(
      @Nonnull DataElement target,
      @Nonnull List<DataElement> sources,
      DataElementPropertyCheck propertyCheck,
      @Nonnull MergeReport report) {
    List<DataElement> mismatches =
        sources.stream().filter(source -> propertyCheck.predicate.test(target, source)).toList();

    if (!mismatches.isEmpty()) {
      report.addErrorMessage(
          new ErrorMessage(
              propertyCheck.errorCode,
              propertyCheck.getProperty(target),
              mismatches.stream().map(propertyCheck::getProperty).distinct().toList()));
    }
    return report;
  }

  /**
   * Enum encapsulating a DataElement property check. It has a {@link DataElementPredicate} and an
   * {@link ErrorCode}. It can also return the property of the {@link DataElement} being checked.
   */
  enum DataElementPropertyCheck implements DataElementProperty {
    VALUE_TYPE_CHECK(DataElementPredicate.VALUE_TYPE_MISMATCH, ErrorCode.E1550) {
      @Override
      public Object getProperty(DataElement de) {
        return de.getValueType();
      }
    },
    DOMAIN_TYPE_CHECK(DataElementPredicate.DOMAIN_TYPE_MISMATCH, ErrorCode.E1551) {
      @Override
      public Object getProperty(DataElement de) {
        return de.getDomainType();
      }
    };

    public final DataElementPredicate predicate;
    public final ErrorCode errorCode;

    DataElementPropertyCheck(DataElementPredicate predicate, ErrorCode errorCode) {
      this.predicate = predicate;
      this.errorCode = errorCode;
    }
  }

  @FunctionalInterface
  public interface DataElementProperty {
    Object getProperty(DataElement de);
  }

  /** Enum of {@link DataElement} mismatch {@link BiPredicate}s */
  enum DataElementPredicate implements BiPredicate<DataElement, DataElement> {
    VALUE_TYPE_MISMATCH {
      @Override
      public boolean test(@Nonnull DataElement target, @Nonnull DataElement source) {
        return target.getValueType() != source.getValueType();
      }
    },
    DOMAIN_TYPE_MISMATCH {
      @Override
      public boolean test(@Nonnull DataElement target, @Nonnull DataElement source) {
        return target.getDomainType() != source.getDomainType();
      }
    }
  }
}
