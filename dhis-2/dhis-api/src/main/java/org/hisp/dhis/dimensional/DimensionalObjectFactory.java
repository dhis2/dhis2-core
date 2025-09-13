package org.hisp.dhis.dimensional;

import java.util.List;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.ProgramStage;

public interface DimensionalObjectFactory {

  DimensionalObject create(String dimension, DimensionType dimensionType, List<? extends DimensionalItemObject> items);
  
  DimensionalObject create(String dimension,
      DimensionType dimensionType,
      String dimensionDisplayName,
      List<? extends DimensionalItemObject> items);
  
  DimensionalObject create(String dimension,
      DimensionType dimensionType,
      String dimensionName,
      String dimensionDisplayName,
      List<? extends DimensionalItemObject> items);
  
  DimensionalObject create(String dimension,
      DimensionType dimensionType,
      List<? extends DimensionalItemObject> items,
      ValueType valueType);
  
  DimensionalObject create( String dimension,
      DimensionType dimensionType,
      String dimensionName,
      String dimensionDisplayName,
      List<? extends DimensionalItemObject> items,
      DimensionItemKeywords dimensionalKeywords);
  
  DimensionalObject create( String dimension,
      DimensionType dimensionType,
      String dimensionName,
      String dimensionDisplayName,
      List<? extends DimensionalItemObject> items,
      boolean allItems);
  
  DimensionalObject create( String dimension,
      DimensionType dimensionType,
      String dimensionName,
      String dimensionDisplayName,
      LegendSet legendSet,
      ProgramStage programStage,
      String filter);
  
  DimensionalObject create( String dimension,
      DimensionType dimensionType,
      String dimensionName,
      String dimensionDisplayName,
      LegendSet legendSet,
      ProgramStage programStage,
      String filter,
      ValueType valueType,
      OptionSet optionSet);
  
  
}
