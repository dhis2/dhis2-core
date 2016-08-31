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

/*
 * @author mortenoh
 */

// -----------------------------------------------
// Support functions
// -----------------------------------------------
// Array Remove - By John Resig (MIT Licensed)
Array.remove = function(array, from, to)
{
    var rest = array.slice((to || from) + 1 || array.length);
    array.length = from < 0 ? array.length + from : from;
    return array.push.apply(array, rest);
};

/* perform dblclick action on the sourceId */
function dhisPaging_moveAllSelected(sourceId)
{
    jQuery("#" + sourceId).dblclick();
}

/* select all options and perform dblclick action on the sourceId */
function dhisPaging_moveAll(sourceId)
{
    var jqSource = jQuery("#" + sourceId);
    jqSource.find("option").attr("selected", "selected");
    jqSource.dblclick();
}

/*
 * @param {String} sourceId Name of source select.
 * @param {String} targetId Name of target select.
 * @param {String} removeArray Name of query parameter to use to remove a set of selected IDs.
 *  This must be supported on the server.
 */
function dhisPaging_availableList_dblclick(sourceId, targetId, removeArray)
{
    return function()
    {
        var jqAvailableList = jQuery("#" + sourceId);
        var jqSelectedList = jQuery("#" + targetId);

        var settings = jqAvailableList.data("settings");

        jqAvailableList.find(":selected").each(function(i)
        {
            var jqThis = jQuery(this);
            var option_id = +jqThis.attr("value");

            jqSelectedList.append(this);

            if (jQuery.isArray(settings[removeArray])) {
                settings[removeArray].push(option_id);
            } else {
                settings[removeArray] = [ option_id ];
            }
        });

        if (settings[removeArray] && settings[removeArray].length > 0) {
            settings.params[removeArray] = settings[removeArray].join(",");
        } else {
            delete settings[removeArray];
            delete settings.params[removeArray];
        }

        jqAvailableList.dhisPaging("load", sourceId);
    };
}

/*
 * @param {String} sourceId Name of source select.
 * @param {String} targetId Name of target select.
 * @param {String} removeArray Name of query parameter to use to remove a set of selected IDs.
 *  This must be supported on the server.
 */
function dhisPaging_selectedList_dblclick(sourceId, targetId, removeArray)
{
    return function()
    {
        var jqAvailableList = jQuery("#" + targetId);
        var jqSelectedList = jQuery("#" + sourceId);

        var settings = jQuery("#" + targetId).data("settings");

        jqSelectedList.find(":selected").each(function(i)
        {
            var jqThis = jQuery(this);
            var option_id = +jqThis.attr("value");
            jqThis.remove();

            if (jQuery.isArray(settings[removeArray])) {
                var remove_idx = jQuery.inArray(option_id, settings[removeArray]);
                Array.remove(settings[removeArray], remove_idx, remove_idx);
            }
        });

        if (settings[removeArray] && settings[removeArray].length > 0) {
            settings.params[removeArray] = settings[removeArray].join(",");
        } else {
            delete settings[removeArray];
            delete settings.params[removeArray];
        }

        jqAvailableList.dhisPaging("load", targetId);
    };
}

// -----------------------------------------------
// Plugin
// -----------------------------------------------

(function($)
{
    var templates = {
        wrapper : "<div id='${id}' style='padding: 0; margin: 0; background-color: #fefefe; border: 1px solid gray;' />",
        button : "<button id='${id}' type='button' style='width: 70px; margin: 4px;'>${text}</button>",
        option : "<option>${text}</option>",
        option_selected : "<option selected='selected'>${text}</option>",
        pagesize_input : "&nbsp;Page size <input id='${id}' type='text' style='width: 50px;'/>",
        filter_input : "<input id='${id}' placeholder='Filter' type='text' style='width: 100%; height: 18px; border: 1px inset gray;' />",
        filter_select : "<select id='${id}' style='width: 100%; margin-bottom: 4px; margin-top: 0;'></select>",
        select_page : "Page <select id='${id}' style='width: 50px;'></select>"
    };

    var methods = {
        load : function(select_id)
        {
            var $select = $("#" + select_id);
            var settings = $select.data("settings");
            var params = settings.params;

            var id = $select.attr("id");
            var wrapper_id = id + "_wrapper";
            $wrapper = $("#" + wrapper_id);
            var select_page_id = id + "_select_page";
            var $select_page = $("#" + select_page_id);
            var next_button_id = id + "_next_button";
            var $next_button = $("#" + next_button_id);
            var previous_button_id = id + "_previous_button";
            var $previous_button = $("#" + previous_button_id);
            var filter_input_id = id + "_filter_input";
            var $filter_input = $("#" + filter_input_id);
            var filter_select_id = id + "_filter_select";
            var pagesize_input_id = id + "_pagesize_input";
            var $pagesize_input = $("#" + pagesize_input_id);

            $.post(settings.source, $.param(settings.params), function(json)
            {
                $select.empty();
                $select_page.empty();

                if (params.usePaging === true) {
                    params.currentPage = json.paging.currentPage == 0 ? 1 : json.paging.currentPage;
                    params.numberOfPages = json.paging.numberOfPages == 0 ? 1 : json.paging.numberOfPages;
                    params.pageSize = json.paging.pageSize;
                    params.startPage = json.paging.startPage;

                    $("#" + pagesize_input_id).val(params.pageSize);

                    $previous_button.removeAttr("disabled");
                    $next_button.removeAttr("disabled");

                    if (params.currentPage == params.startPage) {
                        $previous_button.attr("disabled", "disabled");
                    }

                    if (params.currentPage == params.numberOfPages) {
                        $next_button.attr("disabled", "disabled");
                    }
                }

                $.each(json[settings.iterator], function(i, item)
                {
                    var option = jQuery(settings.handler(item));
                    $select.append(option);
                });

                if (params.usePaging === true) {
                    for ( var j = 1; j <= params.numberOfPages; j++) {
                        if (params.currentPage == j) {
                            $select_page.append($.tmpl(templates.option_selected, {
                                "text" : j
                            }));
                        } else {
                            $select_page.append($.tmpl(templates.option, {
                                "text" : j
                            }));
                        }
                    }
                }
            });
        },
        init : function(options)
        {
            var settings = {}
            var params = {
                usePaging : true
            }

            $.extend(settings, options);
            $.extend(params, options.params);

            var $select = $(this);
            $select.css("border", "none");
            var id = $(this).attr("id");
            var wrapper_id = id + "_wrapper";
            var select_page_id = id + "_select_page";
            var next_button_id = id + "_next_button";
            var previous_button_id = id + "_previous_button";
            var filter_input_id = id + "_filter_input";
            var filter_button_id = id + "_filter_button";
            var filter_select_id = id + "_filter_select";
            var pagesize_input_id = id + "_pagesize_input";

            $select.wrap($.tmpl(templates.wrapper, {
                "id" : wrapper_id
            }));

            $select.css("border-top", "1px solid gray");

            var $wrapper = $("#" + wrapper_id);

            if (settings.filter !== undefined) {
                $wrapper.prepend($.tmpl(templates.filter_select, {
                    "id" : filter_select_id
                }));

                if (settings.filter.label !== undefined) {
                    $wrapper.prepend("<div style='width: 100%; padding-left: 4px;'>Filter by " + settings.filter.label
                            + ":</div>");
                } else {
                    $wrapper.prepend("<div style='width: 100%; padding-left: 4px;'>Filter by:</div>");
                }

                var $filter_select = $("#" + filter_select_id);

                $.getJSON(settings.filter.source, function(json)
                {
                    $filter_select.empty();
                    $filter_select.append("<option>All</option>");

                    $.each(json[settings.filter.iterator], function(i, item)
                    {
                        var option = jQuery(settings.filter.handler(item));
                        $filter_select.append(option);
                    });
                });

                $filter_select.bind("change", {
                    "id" : id
                }, function(event)
                {
                    var $option = $(this).find(":selected");
                    var key = $option.data("key");
                    var value = $option.data("value");

                    key = !!key ? key : "";
                    value = !!value ? value : "";

                    var settings = $("#" + event.data.id).data("settings");

                    if (key !== "") {
                        settings.params[key] = value;
                        settings.filter_select_key = key;
                    } else {
                        if (settings.filter_select_key !== undefined) {
                            delete settings.params[settings.filter_select_key];
                            delete settings.filter_select_key;
                        }
                    }

                    settings.params.currentPage = 1;
                    methods.load(event.data.id);
                });
            }

            var $filter_table = $("<table/>");
            $filter_table.css({
                "padding" : "1px",
                "width" : "100%"
            });

            var $filter_tr = $("<tr/>");

            var $filter_td1 = $("<td/>");
            var $filter_td2 = $("<td/>");
            $filter_td2.css("width", "70px");

            $filter_td1.append($.tmpl(templates.filter_input, {
                "id" : filter_input_id
            }));

            $filter_td2.append($.tmpl(templates.button, {
                "id" : filter_button_id,
                "text" : "filter"
            }));

            $filter_tr.append($filter_td1);
            $filter_tr.append($filter_td2);

            $filter_table.append($filter_tr);

            $wrapper.prepend($filter_table);

            var $filter_input = $("#" + filter_input_id);
            var $filter_button = $("#" + filter_button_id);

            if (params.usePaging === true) {
                $select.css({
                    "border-bottom" : "1px solid gray",
                    "margin-bottom" : "1px"
                });

                $wrapper.append($.tmpl(templates.select_page, {
                    "id" : select_page_id
                }));

                $wrapper.append($.tmpl(templates.button, {
                    "id" : previous_button_id,
                    "text" : "previous"
                }));

                $wrapper.append($.tmpl(templates.button, {
                    "id" : next_button_id,
                    "text" : "next"
                }));

                $wrapper.append($.tmpl(templates.pagesize_input, {
                    "id" : pagesize_input_id
                }));

                var $select_page = $("#" + select_page_id);
                var $previous_button = $("#" + previous_button_id);
                var $next_button = $("#" + next_button_id);
                var $pagesize_input = $("#" + pagesize_input_id);

                $select_page.change(function()
                {
                    params.currentPage = +$(this).find(":selected").val();
                    methods.load("" + id);
                });

                $next_button.click(function()
                {
                    params.currentPage = +params.currentPage + 1;
                    methods.load("" + id);
                });

                $previous_button.click(function()
                {
                    params.currentPage = +params.currentPage - 1;
                    methods.load("" + id);
                });

                $pagesize_input.change(function()
                {
                    params.pageSize = +$(this).val();
                    params.currentPage = 1;
                    methods.load("" + id);
                });
            }

            settings.params = params;
            $select.data("settings", settings);
            methods.load("" + id);

            $filter_button.click(function()
            {
                params.key = $filter_input.val();

                if (params.key.length === 0) {
                    delete params.key;
                }

                params.currentPage = 1;
                methods.load("" + id);
            });

            $filter_input.keypress(function(e)
            {
                if (e.keyCode == 13) {
                    $filter_button.click();
                    e.preventDefault();
                }
            });
        }
    };

    $.fn.dhisPaging = function(method)
    {
        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
        } else if (typeof method === 'object' || !method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error('Method ' + method + ' does not exist on jQuery.dhisPaging');
        }
    };
})(jQuery, undefined);
