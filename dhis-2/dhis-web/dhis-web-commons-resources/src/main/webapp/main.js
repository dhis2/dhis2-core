
// -----------------------------------------------------------------------------
// Page init
// -----------------------------------------------------------------------------

$( document ).ready( function() { pageInit(); } );

function pageInit()
{
    setTableStyles();

    // Hover on rightbar close image
    
    $( "#hideRightBarImg" ).mouseover( function()
    {
        $( this ).attr( "src", "../images/hide_active.png" );
    } );
    $( "#hideRightBarImg" ).mouseout( function()
    {
        $( this ).attr( "src", "../images/hide.png" );
    } );
}

function setTableStyles()
{
    // Zebra stripes in lists

    $( "table.listTable tbody tr:odd" ).addClass( "listAlternateRow" );
    $( "table.listTable tbody tr:even" ).addClass( "listRow" );

    // Hover rows in lists

    $( "table.listTable tbody tr" ).mouseover( function()
    {
        $( this ).addClass( "listHoverRow" );
    } );
    $( "table.listTable tbody tr" ).mouseout( function()
    {
        $( this ).removeClass( "listHoverRow" );
    } );
}

// -----------------------------------------------------------------------------
// Leftbar
// -----------------------------------------------------------------------------

dhis2.util.namespace( 'dhis2.leftBar' );

dhis2.leftBar.setLinks = function( showLeftBarLink, showExtendMenuLink )
{
    $( '#showLeftBar' ).css( 'display', ( showLeftBarLink ? 'inline' : 'none' ) );
    $( '#extendMainMenuLink' ).css( 'display', ( showExtendMenuLink ? 'inline' : 'none' ) );
};

dhis2.leftBar.showAnimated = function()
{
    dhis2.leftBar.setMenuVisible();
    dhis2.leftBar.setLinks( false, true );
    $( '#leftBar, #orgUnitTree' ).css( 'width', '' ).show( 'slide', { direction: 'left', duration: 200 } );
    $( '#mainPage' ).css( 'margin-left', '' );
};

dhis2.leftBar.extendAnimated = function()
{
    dhis2.leftBar.setMenuExtended();
    dhis2.leftBar.setLinks( false, false );
    $( '#leftBar, #orgUnitTree' ).show().animate( { direction: 'left', width: '+=150px', duration: 20 } );
    $( '#mainPage' ).animate( { direction: 'left', marginLeft: '+=150px', duration: 20 } );
    $( '#hideMainMenuLink' ).attr( 'href', 'javascript:dhis2.leftBar.retract()' );
};

dhis2.leftBar.extend = function()
{
    dhis2.leftBar.setMenuExtended();
    dhis2.leftBar.setLinks( false, false );
    $( '#leftBar, #orgUnitTree' ).show().css( "width", "+=150px" );
    $( '#mainPage' ).css( "margin-left", "+=150px" );
    $( '#hideMainMenuLink' ).attr( 'href', 'javascript:dhis2.leftBar.retract()' );
};

dhis2.leftBar.retract = function()
{
    dhis2.leftBar.setMenuVisible();
    dhis2.leftBar.setLinks( false, true );
    $( '#leftBar, #orgUnitTree' ).show().animate( { direction: 'right', width: '-=150px', duration: 20 } );
    $( '#mainPage' ).animate( { direction: 'right', marginLeft: '-=150px', duration: 20 } );
    $( '#hideMainMenuLink' ).attr( 'href', 'javascript:javascript:dhis2.leftBar.hideAnimated()' );
};

dhis2.leftBar.hideAnimated = function()
{
    dhis2.leftBar.setMenuHidden();
    dhis2.leftBar.setLinks( true, false );
    $( '#leftBar' ).hide( 'slide', { direction: 'left', duration: 200 } );
    $( '#mainPage' ).animate( { direction: 'right', marginLeft: '20px', duration: 200 } );
};

dhis2.leftBar.hide = function()
{
    dhis2.leftBar.setMenuHidden();
    dhis2.leftBar.setLinks( true, false );
    $( '#leftBar' ).hide();
    $( '#mainPage' ).css( 'margin-left', '20px' );
};

/**
 * Resets the left bar to its initial position (but does not change the kept state).
 * Useful for avoiding left bar extended/retracted position skewing the dashboard view.
 */
dhis2.leftBar.resetPosition = function()
{
    $( '#leftBar' ).hide();
    $( '#showLeftBar').hide();
    $( '#mainPage' ).css( 'margin-left', '20px' );
};

dhis2.leftBar.setMenuVisible = function()
{
    $.get( '../dhis-web-commons/menu/setMenuState.action?state=VISIBLE' );        
};
    
dhis2.leftBar.setMenuExtended = function()
{
    $.get( '../dhis-web-commons/menu/setMenuState.action?state=EXTENDED' );
};
    
dhis2.leftBar.setMenuHidden = function()
{        
    $.get( '../dhis-web-commons/menu/setMenuState.action?state=HIDDEN' );
};
    
dhis2.leftBar.openHelpForm = function( id )
{
    window.open( "../dhis-web-commons/help/viewDynamicHelp.action?id=" + id, "Help", "width=800,height=600,scrollbars=yes" );
};
