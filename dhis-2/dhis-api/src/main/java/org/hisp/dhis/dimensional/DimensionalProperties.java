package org.hisp.dhis.dimensional;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.eventvisualization.EventRepetition;
import org.hisp.dhis.option.OptionSet;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DimensionalProperties {

  /**
   * The name of this dimension. For the dynamic dimensions this will be equal to dimension
   * identifier. For the period dimension, this will reflect the period type. For the org unit
   * dimension, this will reflect the level.
   */
  private String dimensionName;

  /** The display name to use for this dimension. */
  private String dimensionDisplayName;

  /** Holds the value type of the parent dimension. */
  private ValueType valueType;

  /** The option set associated with the dimension, if any. */
  private OptionSet optionSet;

  /** The dimensional items for this dimension. */
  private List<DimensionalItemObject> items = new ArrayList<>();

  /** Indicates whether all available items in this dimension are included. */
  private boolean allItems;

  /**
   * Filter. Applicable for events. Contains operator and filter on this format:
   * <operator>:<filter>;<operator>:<filter> Operator and filter pairs can be repeated any number of
   * times.
   */
  private String filter;


  /** Applicable only for events. Holds the indexes relate to the repetition object. */
  private EventRepetition eventRepetition;


  /**
   * A {@link DimensionItemKeywords} defines a pre-defined group of items. For instance, all the OU
   * withing a district
   */
  private DimensionItemKeywords dimensionalKeywords;

  /**
   * Indicates whether this dimension is fixed, meaning that the name of the dimension will be
   * returned as is for all dimension items in the response.
   */
  private boolean fixed;
}
