package org.hisp.dhis.analytics.outlier.data;

import org.hisp.dhis.analytics.data.DimensionalObjectProducer;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hisp.dhis.DhisConvenienceTest.createDataElement;
import static org.hisp.dhis.DhisConvenienceTest.createDataSet;
import static org.hisp.dhis.DhisConvenienceTest.createOrganisationUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OutlierQueryParserTest {
    @Mock private IdentifiableObjectManager idObjectManager;
    @Mock private DimensionalObjectProducer dimensionalObjectProducer;
    @Mock private CurrentUserService currentUserService;
    private OutlierQueryParser subject;

    @BeforeEach
    public void setup(){
        DataSet dataSet = createDataSet('A');
        when(idObjectManager.getByUid(eq(DataSet.class), anyCollection())).thenReturn(List.of(dataSet));

        DataElement dataElement = createDataElement('B');
        when(idObjectManager.getByUid(eq(DataElement.class), anyCollection())).thenReturn(List.of(dataElement));

        OrganisationUnit organisationUnit = createOrganisationUnit('O');
        BaseDimensionalObject baseDimensionalObject = new BaseDimensionalObject();
        baseDimensionalObject.setItems(List.of(organisationUnit));
        when(dimensionalObjectProducer.getOrgUnitDimension(anyList(), eq(DisplayProperty.NAME), anyList(), eq(IdScheme.UID))).thenReturn(baseDimensionalObject);

        subject = new OutlierQueryParser(idObjectManager, dimensionalObjectProducer, currentUserService);
    }

    @Test
    void testGetFromQueryDataElement(){
        //given
        OutlierQueryParams params = new OutlierQueryParams();
        //when
        OutlierRequest request = subject.getFromQuery(params, false);
        //then
        assertEquals(1, (long) request.getDataElements().size());
        assertEquals("deabcdefghB", request.getDataElements().get(0).getUid());
    }

    @Test
    void testGetFromQueryOrgUnit(){
        //given
        OutlierQueryParams params = new OutlierQueryParams();
        //when
        OutlierRequest request = subject.getFromQuery(params, false);
        //then
        assertEquals(1, (long) request.getOrgUnits().size());
        assertEquals("ouabcdefghO", request.getOrgUnits().get(0).getUid());
    }
}
