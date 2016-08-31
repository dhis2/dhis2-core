
$( document ).ready( function()
{	
    $.get( 
       'getHelpItems.action',
       function( data )
       {
           $( "div#helpMenu" ).html( data );
           $( "div#helpMenu" ).accordion();
       } );
} );

function getHelpItemContent( id )
{
	$.get( 
       'getHelpContent.action',
       { "id": id },
       function( data )
       {
           $( "div#helpContent" ).html( data );
       } );
}
