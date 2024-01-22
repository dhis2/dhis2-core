package org.hisp.dhis.db.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Index
{
    private final String name;

    private final boolean unique;

    private final List<String> columns = new ArrayList<>();
}
