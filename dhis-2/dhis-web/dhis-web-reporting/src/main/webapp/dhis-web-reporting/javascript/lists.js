// -------------------------------------------------------------------------
// Data retrieval methods
// -------------------------------------------------------------------------

function getDataElements()
{
    var dataElementGroupList = document.getElementById( "dataElementGroupId" );
    var dataElementGroupId = dataElementGroupList.options[dataElementGroupList.selectedIndex].value;

    if ( dataElementGroupId != null )
    {
		$.get( '../dhis-web-commons-ajax/getDataElements.action',
		{
			id: dataElementGroupId,
			aggregate: 'true'
		},getDataElementsReceived );
    }
}

function getDataElementsReceived( xmlObject )
{
    var availableDataElements = byId( "availableDataElements" );
    var selectedDataElements = byId( "selectedDataElements" );

    clearList( availableDataElements );

    var dataElements = xmlObject.getElementsByTagName( "dataElement" );

    for ( var i = 0; i < dataElements.length; i++ )
    {
        var id = dataElements[i].getElementsByTagName( "id" )[0].firstChild.nodeValue;
        var dataElementName = dataElements[i].getElementsByTagName( "name" )[0].firstChild.nodeValue;

        if ( listContains( selectedDataElements, id ) == false )
        {
            var option = document.createElement( "option" );
            option.value = id;
            option.text = dataElementName;
            availableDataElements.add( option, null );
        }
    }
}

function getCategoryComboDataElements()
{
    var categoryComboId = getListValue( "categoryComboId" );

    if ( categoryComboId != null )
    {
		$.get( 'getCategoryComboDataElements.action',
		{
			categoryComboId: categoryComboId
		}, getCategoryComboDataElementsReceived );
    }
}

function getCategoryComboDataElementsReceived( xmlObject )
{
    var availableDataElements = byId( "availableDataElements" );
    var selectedDataElements = byId( "selectedDataElements" );

    clearList( availableDataElements );
    clearList( selectedDataElements );

    var dataElements = xmlObject.getElementsByTagName( "dataElement" );

    for ( var i = 0; i < dataElements.length; i++ )
    {
        var id = dataElements[i].getElementsByTagName( "id" )[0].firstChild.nodeValue;
        var dataElementName = dataElements[i].getElementsByTagName( "name" )[0].firstChild.nodeValue;

        var option = document.createElement( "option" );
        option.value = id;
        option.text = dataElementName;
        availableDataElements.add( option, null );
    }
}

function getIndicators()
{
    var indicatorGroupList = byId( "indicatorGroupId" );
    var indicatorGroupId = indicatorGroupList.options[indicatorGroupList.selectedIndex].value;

    if ( indicatorGroupId != null )
    {
		$.get( '../dhis-web-commons-ajax/getIndicators.action',
		{
			id: indicatorGroupId
		},getIndicatorsReceived );
    }
}

function getIndicatorsReceived( xmlObject )
{
    var availableIndicators = document.getElementById( "availableIndicators" );
    var selectedIndicators = document.getElementById( "selectedIndicators" );

    clearList( availableIndicators );

    var indicators = xmlObject.getElementsByTagName( "indicator" );

    for ( var i = 0; i < indicators.length; i++ )
    {
        var id = indicators[i].getElementsByTagName( "id" )[0].firstChild.nodeValue;
        var indicatorName = indicators[i].getElementsByTagName( "name" )[0].firstChild.nodeValue;

        if ( listContains( selectedIndicators, id ) == false )
        {
            var option = document.createElement( "option" );
            option.value = id;
            option.text = indicatorName;
            availableIndicators.add( option, null );
        }
    }
}

function getOrganisationUnitChildren()
{
    var organisationUnitList = byId( "availableOrganisationUnits" );
	if( organisationUnitList.selectedIndex != -1 )
	{
		var organisationUnitId = organisationUnitList.options[organisationUnitList.selectedIndex].value;

		if ( organisationUnitId != null )
		{
			$.get( '../dhis-web-commons-ajax/getOrganisationUnitChildren.action',
				{
					id: organisationUnitId
				},getOrganisationUnitChildrenReceived );
		}
	}
}

function getOrganisationUnitChildrenReceived( xmlObject )
{
    var selectedOrganisationUnits = byId( "selectedOrganisationUnits" );

    var organisationUnits = xmlObject.getElementsByTagName( "organisationUnit" );

    for ( var i = 0; i < organisationUnits.length; i++ )
    {
        var id = organisationUnits[i].getElementsByTagName( "id" )[0].firstChild.nodeValue;

        var organisationUnitName = organisationUnits[i].getElementsByTagName( "name" )[0].firstChild.nodeValue;

        if ( listContains( selectedOrganisationUnits, id ) == false )
        {
            var option = document.createElement( "option" );
            option.value = id;
            option.text = organisationUnitName;
            selectedOrganisationUnits.add( option, null );
        }
    }
}
