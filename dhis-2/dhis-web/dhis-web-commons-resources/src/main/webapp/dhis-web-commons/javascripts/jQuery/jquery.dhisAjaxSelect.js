/*
 * Copyright (c) 2004-2013, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */

// -----------------------------------------------
// Support functions
// -----------------------------------------------

/* perform dblclick action on the sourceId */
function dhisAjaxSelect_moveAllSelected(sourceId)
{
    $("#" + sourceId).dblclick();
}

/* select all options and perform dblclick action on the sourceId */
function dhisAjaxSelect_moveAll(sourceId)
{
    var jqSource = $("#" + sourceId);
    jqSource.find("option").attr("selected", "selected");
    jqSource.dblclick();
}

/**
 * filter a selector on data-key = value
 */
function dhisAjaxSelect_filter_on_kv($target, key, value)
{
    var $ghost_target = dhis2.select.getGhost($target);

    if (key.length === 0) {
        dhis2.select.moveSorted($target, $ghost_target.children());
        return;
    }

    // filter options that do not match on select
    var $options = $target.children();
    var array = []; // array of options to move to ghost

    $options.each(function() {
        var $this = $(this);

        if ( !compare_data_with_kv($this, key, value) ) {
            array.push($this[0]);
        }
    });

    dhis2.select.moveSorted($ghost_target, $(array));

    // filter options that match on ghost
    var $ghost_options = $ghost_target.children();
    var ghost_array = []; // array of options to move to ghost

    $ghost_options.each(function() {
        var $this = $(this);

        if ( compare_data_with_kv($this, key, value) ) {
            ghost_array.push($this[0]);
        }
    });

    dhis2.select.moveSorted($target, $(ghost_array));
}

/**
 * @param $target jQuery object to work on
 * @param key data-entry key, $target.data(key)
 * @param value value to compare to
 * @returns {Boolean} true or false after comparing $target.data(key) with value
 */
function compare_data_with_kv($target, key, value)
{
    var target_value = $target.data(key);
    target_value = target_value ? target_value : [];

    if( !$.isArray(target_value) ) {
        var type = typeof(target_value);

        if(type === "number") {
            target_value = [ target_value.toString() ];
        } else {
            target_value = target_value.split(",");
        }
    }

    if (target_value) {
        if ($.inArray(value.toString(), target_value) !== -1) {
            return true;
        }
    }

    return false;
}

function dhisAjaxSelect_availableList_dblclick(sourceId, targetId)
{
    return function()
    {
        var jqAvailableList = $("#" + sourceId);
        var jqSelectedList = $("#" + targetId);
        var settings = jqAvailableList.data("settings");

        if(settings.sortSelected) {
            dhis2.select.moveSorted(jqSelectedList, jqAvailableList.find(":selected"));
        } else {
            dhis2.select.move(jqSelectedList, jqAvailableList.find(":selected"));
        }
    };
}

function dhisAjaxSelect_selectedList_dblclick(sourceId, targetId)
{
    return function()
    {
        var jqAvailableList = $("#" + targetId);
        var jqSelectedList = $("#" + sourceId);
        var settings = jqAvailableList.data("settings");

        var $children = jqSelectedList.find(":selected");

        if(!settings.sortSelected) {
            $children = dhis2.select.sortNC( $children );
        }

        dhis2.select.moveSorted(jqAvailableList, $children);
    };
}

// -----------------------------------------------
// Plugin
// -----------------------------------------------

(function($)
{
    var templates = {
        wrapper : "<div id='${id}' style='padding: 0; margin: 0; background-color: #fefefe;' />",
        button : "<button id='${id}' type='button' style='width: 70px; margin: 4px;'>${text}</button>",
        option : "<option>${text}</option>",
        option_selected : "<option selected='selected'>${text}</option>",
        filter_input : "<input id='${id}' placeholder='Filter' type='text' style='width: 96%; height: 18px; border: 1px inset gray;' />",
        filter_select : "<select id='${id}' style='width: 100%; margin-bottom: 4px; margin-top: 0;'></select>"
    };

    var methods = {
        load : function(select_id)
        {
            var $select = $("#" + select_id);
            var settings = $select.data("settings");

            var loader_id = select_id + '_loader';
            var $loader = $('#' + loader_id); 
            $loader.css('visibility', 'visible');

            $.getJSON(settings.source, $.param(settings.params), function(json)
            {
                $select.empty();

                $.each(json[settings.iterator], function(i, item)
                {
                    var option = $(settings.handler(item));

                    if(option !== undefined) {
                        $select.append(option);
                    }
                });

                if (settings.connectedTo) {
                    var $connectedTo = $('#' + settings.connectedTo);

                    if ($connectedTo) {
                        $connectedTo.children().each(function()
                        {
                            var value = $(this).attr("value");
                            $select.find("option[value='" + value + "']").remove();
                        });
                    }
                }

                $loader.css('visibility', 'hidden');
            });
        },
        init : function(options)
        {
            var settings = {
                sortAvailable: true,
                sortSelected: true
            };

            var params = {};

            $.extend(settings, options);
            $.extend(params, options.params);

            var $select = $(this);
            var id = $(this).attr("id");
            var wrapper_id = id + "_wrapper";
            var filter_input_id = id + "_filter_input";
            var filter_button_id = id + "_filter_button";
            var clear_button_id = id + "_clear_button";
            var filter_select_id = id + "_filter_select";
            var select_search_table_id = id + "select_search_table";

            $select.wrap($.tmpl(templates.wrapper, {
                "id" : wrapper_id
            }));

            var $wrapper = $("#" + wrapper_id);

            if (settings.filter !== undefined) {
                $wrapper.prepend($.tmpl(templates.filter_select, {
                    "id" : filter_select_id
                }));

                var $filter_select = $("#" + filter_select_id);

                $.getJSON(settings.filter.source, function(json)
                {
                    $filter_select.empty();
                    
                    if( settings.filter.label !== undefined ) {
                        $filter_select.append("<option>[ All / " + settings.filter.label + " ]</option>");
                    } else {
                        $filter_select.append("<option>[ All ]</option>");
                    }

                    $.each(json[settings.filter.iterator], function(i, item)
                    {
                        var option = $(settings.filter.handler(item));
                        $filter_select.append(option);
                    });
                });

                $filter_select.bind("change", {
                    'id' : id,
                    'filter_input_id' : filter_input_id
                }, function(event)
                {
                    var $option = $(this).find(":selected");
                    var key = $option.data("key");
                    var value = $option.data("value");
                    var $filter_input = $('#' + event.data.filter_input_id);

                    key = !!key ? key : "";
                    value = !!value ? value : "";

                    if(key.length === 0) {
                        $filter_input.removeAttr('disabled');
                    } else {
                        $filter_input.attr('disabled', 'disabled');
                        $filter_input.attr('value', '');
                    }
                    
                    dhisAjaxSelect_filter_on_kv($("#" + event.data.id), key, value);
                });
            }

            $('#' + select_search_table_id).remove();
            var $filter_table = $("<table/>").attr('id', select_search_table_id);

            $filter_table.css({
                "padding" : "1px",
                "width" : "100%"
            });

            var $filter_tr = $("<tr/>");

            var $filter_td1 = $("<td/>");
            var $filter_td2 = $("<td/>");

            $filter_td2.css("width", "158px");

            $filter_td1.append($.tmpl(templates.filter_input, {
                "id" : filter_input_id
            }));

            $filter_td2.append($.tmpl(templates.button, {
                "id" : filter_button_id,
                "text" : "Filter"
            }));
            
            $filter_td2.append($.tmpl(templates.button, {
                "id" : clear_button_id,
                "text" : "Clear"
            }));

            $filter_tr.append($filter_td1);
            $filter_tr.append($filter_td2);

            $filter_table.append($filter_tr);

            $wrapper.prepend($filter_table);

            var $filter_input = $("#" + filter_input_id);
            var $filter_button = $("#" + filter_button_id);
            var $clear_button = $("#" + clear_button_id);

            var loader_id = id + '_loader';
            $('<img id="' + loader_id + '" src="../images/ajax-loader-bar.gif" />').insertAfter($wrapper);
            $('#' + loader_id).css('visibility', 'hidden');

            settings.params = params;
            $select.data("settings", settings);
            methods.load("" + id);

            $filter_button.bind('click', function()
            {
                var key = $filter_input.val();
                dhis2.select.filterWithKey($select, key);
            });

            $clear_button.bind('click', function() {
                $filter_input.val('');
                dhis2.select.filterWithKey($select, ''); 
            });
            
            $filter_input.keypress(function(e)
            {
                if (e.keyCode == 13) {
                    $filter_button.click();
                    e.preventDefault();
                }
            });

            if (settings.connectedTo) {
                $select.dblclick(dhisAjaxSelect_availableList_dblclick($select.attr("id"), settings.connectedTo));
                $('#' + settings.connectedTo).dblclick(
                        dhisAjaxSelect_selectedList_dblclick(settings.connectedTo, $select.attr('id')));
            }
        }
    };

    $.fn.dhisAjaxSelect = function(method)
    {
        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
        } else if (typeof method === 'object' || !method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error('Method ' + method + ' does not exist on jQuery.dhisAjaxSelect');
        }
    };
})(jQuery);
