package org.hisp.dhis.hibernate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Encapsulation of a read only data source configuration.
 * 
 * @author Lars Helge Overland
 */
@Getter
@Setter
@RequiredArgsConstructor
public class ReadOnlyDataSourceConfig {
  private final String url;
  
  private final String username;
  
  private final String password;
}
