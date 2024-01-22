package org.hisp.dhis.db.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Table
{
    private final String name;

    private final List<Column> columns = new ArrayList<>();

    private final List<Column> primaryKey = new ArrayList<>();
}
