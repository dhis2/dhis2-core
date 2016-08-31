/**
	The global variables
	@param currentUrlLink this url string use for "OrganisationUnit" browser option
	@param currentParentId currentParentId is the indentifider of selected organisationunit
*/

currentUrlLink = "";
currentParentId = "";

isAjax = true;
var isOrgUnitSelected = false;

function organisationUnitModeSelected( units )
{
	isOrgUnitSelected = (units && units.length > 0);
}

function modeHandler()
{
    var modeList = byId( "mode" );
    var modeSelection = modeList.value;
    
    var treeSection = byId( "organisationUnitSection" );
    var drillDownCheckBoxDiv = byId( "drillDownCheckBoxDiv" );
    
    if ( modeSelection == "OU" )
    {   
        treeSection.style.display = "block";
        drillDownCheckBoxDiv.style.display = "block";
    }
    else
    {
        treeSection.style.display = "none";
		drillDownCheckBoxDiv.style.display = "none";
		byId( "drillDownCheckBox" ).checked = false;
    }
}

function validateBeforeSubmit( form )
{
	var mode = getFieldValue( "mode" );

	if ( (mode == "OU") && !isOrgUnitSelected )
	{
		setHeaderDelayMessage( i18n_please_select_org_unit );
	}
    else
    {
        $( "#fromDate").removeAttr("disabled");
        $( "#toDate").removeAttr("disabled");

        form.submit();
    }
}

// -----------------------------------------------------------------------------
// Supportive methods
// -----------------------------------------------------------------------------

/**
 * Loads the event listeners for the drill-down table. Called after page is loaded.
 */
function loadListeners()
{
	var table = byId( "drillDownGrid" );

	if ( table != null )
	{
		table.addEventListener( "click", setPosition, false );
	}
}

/**
* This method sets the position of the drill-down menu, and is registered as a 
* callback function for mouse click events.
*/
function setPosition( e )
{
  var left = e.pageX + "px";
  var top = e.pageY + "px";
  
  var drillDownMenu = byId( "drillDownMenu" );
  
  drillDownMenu.style.left = left;
  drillDownMenu.style.top = top;
}

/**
 * This method is called from the UI and will display the drildown menu.
 * 
 * @param urlLink this url string use for "OrganisationUnit" browser option
 * @param parentId parentId is the identifier of selected organisation unit
 */
function viewDrillDownMenu( urlLink, parentId )
{
	currentUrlLink = urlLink;
	currentParentId = parentId;

	showDropDown( "drillDownMenu" );
}

/**
 * This method is called from the UI and will display the drildown data.
 * 
 * @param levelStyle levelStyle is the view style of data
 */
function viewDrillDownData( levelStyle )
{
	if ( levelStyle == "current_level" )
	{
		currentUrlLink = currentUrlLink + "&parent=" + currentParentId;
	}
	
	hideDropDown();  
	window.location.href = currentUrlLink;
}

function exportResult( type )
{
	var url = "exportResult.action?type=" + type;
	
	window.location.href = url;
}


// -----------------------------------------------------------------------------
// Menu functions
// -----------------------------------------------------------------------------

var menuTimeout = 500;
var closeTimer = null;
var dropDownId = null;

function showDropDown( id )
{
    cancelHideDropDownTimeout();
    
    var newDropDownId = "#" + id;
  
    if ( dropDownId != newDropDownId )
    {   
        hideDropDown();

        dropDownId = newDropDownId;
        
        $( dropDownId ).show();
    }
}

function hideDropDown()
{
	if ( dropDownId )
	{
	    $( dropDownId ).hide();
	    
	    dropDownId = null;
	}
}

function hideDropDownTimeout()
{
    closeTimer = window.setTimeout( "hideDropDown()", menuTimeout );
}

function cancelHideDropDownTimeout()
{
    if ( closeTimer )
    {
        window.clearTimeout( closeTimer );
        
        closeTimer = null;
    }
}
