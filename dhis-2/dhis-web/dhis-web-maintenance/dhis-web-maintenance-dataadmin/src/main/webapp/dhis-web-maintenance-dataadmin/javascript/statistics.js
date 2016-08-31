$( document ).ready( function()
{
    showLoader();

    $( '#contentDiv' ).load( 'getStatistics.action?' + getDC(), function()
    {
        hideLoader();
    } );
} );
