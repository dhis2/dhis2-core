//====================================================================================
// DATAELEMENT & DATA ELEMENT GROUP & CATEGORY OPTION COMBO
//====================================================================================

function DataDictionary()
{
    var ALL_KEY = 'all';

    var dataElementsGroups = new Array();

    var dataElementGroupList = new Array();

    var indicatorGroups = new Array();

    var indicatorGroupList = new Array();

    var categoryOptionComboList = new Array();

    var params = {};

    this.loadDataElementGroups = function( jQuerySelectionString )
    {
        var target = jQuery( jQuerySelectionString );

        target.children().remove();

        if ( dataElementsGroups.length == 0 )
        {
            jQuery.getJSON( '../dhis-web-commons-ajax-json/getDataElementGroups.action', function( json )
            {
                target.append( '<option value="' + ALL_KEY + '">ALL</option>' );
                dataElementsGroups.push( new DataElementGroup( ALL_KEY, 'ALL' ) );

                jQuery.each( json.dataElementGroups, function( i, item )
                {
                    dataElementsGroups.push( new DataElementGroup( item.id, item.name ) );
                    target.append( '<option value="' + item.id + '">' + item.name + '</option>' );
                } );
            } );
        } else
        {
            jQuery.each( dataElementsGroups, function( i, item )
            {
                target.append( '<option value="' + item.id + '">' + item.name + '</option>' );
            } );
        }
    };

    this.loadAllDataElements = function( jQuerySelectionString )
    {
        this.loadDataElementsByGroup( '0', jQuerySelectionString );
    };

    this.loadDataElementsByGroup = function( id, jQuerySelectionString )
    {
        var target = jQuery( jQuerySelectionString );

        target.children().remove();

        var des = dataElementGroupList[id];

        if ( des == null )
        {
            des = new Array();

            jQuery.getJSON( '../dhis-web-commons-ajax-json/getDataElements.action', {
                id : id
            }, function( json )
            {
                jQuery.each( json.dataElements, function( i, item )
                {
                    des.push( new DataElement( item.id, item.name ) );
                    target.append( '<option value="' + item.id + '">' + item.name + '</option>' );
                } );

                dataElementGroupList[id] = des;
            } );
        } else
        {
            jQuery.each( des, function( i, item )
            {
                target.append( '<option value="' + item.id + '">' + item.name + '</option>' );
            } );
        }
    };

    this.loadIndicatorGroups = function( jQuerySelectionString )
    {
        var target = jQuery( jQuerySelectionString );

        target.children().remove();

        if ( indicatorGroups.length == 0 )
        {
            jQuery.getJSON( '../dhis-web-commons-ajax-json/getIndicatorGroups.action', function( json )
            {
                target.append( '<option value="' + ALL_KEY + '">ALL</option>' );
                indicatorGroups.push( new IndicatorGroup( ALL_KEY, 'ALL' ) );
                jQuery.each( json.indicatorGroups, function( i, item )
                {
                    indicatorGroups.push( new IndicatorGroup( item.id, item.name ) );
                    target.append( '<option value="' + item.id + '">' + item.name + '</option>' );
                } );
            } );
        } else
        {
            jQuery.each( indicatorGroups, function( i, item )
            {
                target.append( '<option value="' + item.id + '">' + item.name + '</option>' );
            } );
        }
    };

    this.loadAllIndicators = function( jQuerySelectionString )
    {
        this.loadIndicatorsByGroup( ALL_KEY, jQuerySelectionString );
    };

    this.loadIndicatorsByGroup = function( id, jQuerySelectionString )
    {
        var target = jQuery( jQuerySelectionString );

        target.children().remove();

        var ins = indicatorGroupList[id];

        if ( ins == null )
        {
            ins = new Array();

            jQuery.getJSON( '../dhis-web-commons-ajax-json/getIndicators.action', {
                id : id
            }, function( json )
            {
                jQuery.each( json.indicators, function( i, item )
                {
                    ins.push( new Indicator( item.id, item.name ) );
                    target.append( '<option value="' + item.id + '">' + item.name + '</option>' );
                } );

                indicatorGroupList[id] = ins;
            } );
        } else
        {
            jQuery.each( ins, function( i, item )
            {
                target.append( '<option value="' + item.id + '">' + item.name + '</option>' );
            } );
        }
    };

    this.loadCategoryOptionComboByDE = function( id, jQuerySelectionString )
    {
        var target = jQuery( jQuerySelectionString );

        target.children().remove();

        var options = categoryOptionComboList[id];

        if ( options == null )
        {
            options = new Array();

            jQuery.getJSON( '../dhis-web-commons-ajax-json/getCategoryOptionCombos.action', {
                id : id
            }, function( json )
            {
                jQuery.each( json.categoryOptionCombos, function( i, item )
                {
                    options.push( new OptionCombo( item.id, item.name ) );
                    target.append( '<option value="' + item.id + '">' + item.name + '</option>' );
                } );

                categoryOptionComboList[id] = options;
            } );
        } else
        {
            jQuery.each( options, function( i, item )
            {
                target.append( '<option value="' + item.id + '">' + item.name + '</option>' );
            } );
        }
    };

    this.reloadOperands = function( currentpage )
    {
        this.params.currentPage = currentpage;
        this.loadOperands( this.jQuerySelectionString, this.params );
    };

    this.changeOperandsPageSize = function( pagesize )
    {
        this.params.pageSize = pagesize;
        this.loadOperands( this.jQuerySelectionString, this.params );
    };

    this.loadOperands = function( jQuerySelectionString, params )
    {
        var target = jQuery( jQuerySelectionString );
        target.children().remove();

        this.params = params;
        this.jQuerySelectionString = jQuerySelectionString;

        jQuery.postJSON(
            '../dhis-web-commons-ajax-json/getOperands.action',
            this.params,
            function( json )
            {
                jQuery.each( json.operands, function( i, item )
                {
                    target.append( '<option value="#{' + item.operandId + '}">' + item.operandName
                            + '</option>' );
                } );

                if ( params.usePaging )
                {
                    var numberOfPages = json.paging.numberOfPages;
                    var currentPage = json.paging.currentPage;
                    var pageSize = json.paging.pageSize;
                    var startPage = json.paging.startPage;

                    var html = '<div id="operandPaging_div">';

                    if ( numberOfPages > 1 )
                    {
                        html += 'Page: <select onchange="dataDictionary.reloadOperands( this.value )" style="width:60px">';

                        for ( var i = 1; i <= numberOfPages; i++ )
                        {
                            html += '<option value="' + i + '"' + (currentPage == i ? 'selected=true' : '')
                                    + '>' + i + '</option>';
                        }

                        html += '</select>';

                        if ( currentPage == startPage )
                        {
                            html += '<button type="button" disabled="disabled">Previous page</button>';
                            html += '<button type="button" onclick="dataDictionary.reloadOperands( '
                                    + (parseInt( currentPage ) + 1) + ' )">Next page</button>';
                        } 
                        else if ( currentPage == numberOfPages )
                        {
                            html += '<button type="button" onclick="dataDictionary.reloadOperands( '
                                    + (parseInt( currentPage ) - 1) + ' )">Previous page</button>';
                            html += '<button type="button" disabled="disabled">Next page</button>';
                        } 
                        else
                        {
                            html += '<button type="button" onclick="dataDictionary.reloadOperands( '
                                    + (parseInt( currentPage ) - 1) + ')">Previous page</button>';
                            html += '<button type="button" onclick="dataDictionary.reloadOperands( '
                                    + (parseInt( currentPage ) + 1) + ')">Next page</button>';
                        }
                    }

                    html += 'Size: <input type="text" style="width:50px" onchange="dataDictionary.changeOperandsPageSize( this.value )" value="'
                            + pageSize + '"/></div>';
                    jQuery( '#operandPaging_div' ).remove();
                    jQuery( html ).insertAfter( target );
                }
            } );
    };
}

var dataDictionary = new DataDictionary();

function DataElementGroup( id_, name_ )
{
    this.id = id_;
    this.name = name_;
}

function DataElement( id_, name_ )
{
    this.id = id_;
    this.name = name_;
}

function OptionCombo( id_, name_ )
{
    this.id = id_;
    this.name = name_;
}

function IndicatorGroup( id_, name_ )
{
    this.id = id_;
    this.name = name_;
}

function Indicator( id_, name_ )
{
    this.id = id_;
    this.name = name_;
}
