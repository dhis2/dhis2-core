var selectedOrganisationUnitList__;

function addSelectedOrganisationUnit__( id )
{
    selectedOrganisationUnitList__.empty();
    selectedOrganisationUnitList__.append('<option value="' + id + '" selected="selected">' + id + '</option>');
}

function selectOrganisationUnit__( ids )
{
	selectedOrganisationUnitList__.empty();

    if ( ids && ids.length > 0 )  {
        selectedOrganisationUnitList__.append('<option value="' + ids[0] + '" selected="selected">' + ids[0] + '</option>');
    }

	byId('treeSelectedId').selectedIndex = 0;
}

function unSelectChildren()
{
	jQuery.get('../dhis-web-commons/oust/removeorgunit.action', {
		children : true
	}, function( xml )
	{
		selectedOrganisationUnitXML__(xml);
	});
}

function selectChildren()
{
	jQuery.get('../dhis-web-commons/oust/addorgunit.action', {
		children : true
	}, function( xml )
	{
		selectedOrganisationUnitXML__(xml);
	});
}

function selectOrganisationUnitAtLevel()
{
	jQuery.get('../dhis-web-commons/oust/addorgunit.action', {
		level : getFieldValue('levelList')
	}, function( xml )
	{
		selectedOrganisationUnitXML__(xml);
	});
}

function unSelectOrganisationUnitAtLevel()
{
	jQuery.get('../dhis-web-commons/oust/removeorgunit.action', {
		level : getFieldValue('levelList')
	}, function( xml )
	{
		selectedOrganisationUnitXML__(xml);
	});
}

function unSelectAllTree()
{
	jQuery.get('../dhis-web-commons/oust/clearSelectedOrganisationUnits.action', function( xml )
	{
		selectedOrganisationUnitXML__(xml);
	});
}

function selectAllTree()
{
	jQuery.get('../dhis-web-commons/oust/selectallorgunit.action', function( xml )
	{
		selectedOrganisationUnitXML__(xml);
	});
}

function selectOrganisationUnitByGroup()
{
	jQuery.get('../dhis-web-commons/oust/addorgunit.action', {
		organisationUnitGroupId : getFieldValue('groupList')
	}, function( xml )
	{
		selectedOrganisationUnitXML__(xml);
	});
}

function unSelectOrganisationUnitByGroup()
{
	jQuery.get('../dhis-web-commons/oust/removeorgunit.action', {
		organisationUnitGroupId : getFieldValue('groupList')
	}, function( xml )
	{
		selectedOrganisationUnitXML__(xml);
	});
}

function loadOrganisationUnitLevel()
{
	jQuery.getJSON('../dhis-web-commons-ajax-json/getOrganisationUnitLevels.action', function( json )
	{
		var levels = jQuery("#levelList");
		levels.empty();
		jQuery.each(json.levels, function( i, item )
		{
			levels.append('<option value="' + item.level + '">' + item.name + '</option>');
		});
		jQuery("#selectionTreeContainer").fadeIn();
	});
}

function loadOrganisationUnitGroup()
{
	jQuery.getJSON('../dhis-web-commons-ajax-json/getOrganisationUnitGroups.action', function( json )
	{
		var groups = jQuery("#groupList");
		groups.empty();
		jQuery.each(json.organisationUnitGroups, function( i, item )
		{
			groups.append('<option value="' + item.id + '">' + item.name + '</option>');
		});

		loadOrganisationUnitLevel();
	});
}
