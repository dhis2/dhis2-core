dhis2.util.namespace('dhis2.db');

dhis2.db.currentKey = undefined;

dhis2.db.current = function () {
    var current = localStorage[dhis2.db.currentKey];
    current = ( current == "undefined" ) ? undefined : current;
    return current;
}

dhis2.db.setCurrent = function (id) {
    localStorage[dhis2.db.currentKey] = id;
}

dhis2.db.clearCurrent = function () {
    localStorage.removeItem(dhis2.db.currentKey);
}

dhis2.db.currentItem;
dhis2.db.currentItemPos;
dhis2.db.currentShareType;
dhis2.db.currentShareId;
dhis2.db.currentMaxType = [];
dhis2.db.currentUserOrgUnit = [];
dhis2.db.contextPath;
dhis2.db.maxItems = 40;
dhis2.db.shapeNormal = "NORMAL";
dhis2.db.shapeDoubleWidth = "DOUBLE_WIDTH";
dhis2.db.shapeFullWidth = "FULL_WIDTH";
dhis2.db.widthNormal = 421;
dhis2.db.widthDouble = 860;
dhis2.db.visualItemTypes = ["CHART", "EVENT_CHART", "MAP", "REPORT_TABLE", "EVENT_REPORT", "APP"];
dhis2.db.itemContentHeight = 317;
dhis2.db.itemScrollbarWidth = /\bchrome\b/.test(navigator.userAgent.toLowerCase()) ? 8 : 17;
dhis2.db.reportTableItems = [];
dhis2.db.chartItems = [];

// TODO support table as link and embedded
// TODO double horizontal size

//------------------------------------------------------------------------------
// Document ready
//------------------------------------------------------------------------------

$(document).ready(function () {
    $("#interpretationArea").autogrow();

    $(document).click(function(){dhis2.db.hideSearch();dhis2.db.hideInterpretationPopup();});

    $(window).resize(dhis2.db.drawWideItems);

    $("#searchField").focus(function () {
        $("#searchDiv").css("border-color", "#999");
    }).blur(function () {
        $("#searchDiv").css("border-color", "#aaa");
    });

    $("#searchField").focus();
    $("#searchField").keyup(dhis2.db.search);

    selectionTreeSelection.setMultipleSelectionAllowed(true);
    selectionTree.clearSelectedOrganisationUnitsAndBuildTree();

    $.getJSON("../api/me/user-account.json?" + dhis2.util.cacheBust(), function (json) {
        dhis2.db.currentKey = "dhis2.dashboard.current." + json.username;
        dhis2.db.renderDashboardListLoadFirst();
    });

    $.getJSON("../api/system/info.json", function (json) {
        dhis2.db.contextPath = json.contextPath;
    });
    
    //Initialize interpretation popup events
    dhis2.db.initInterpretationPopupEvents();
});

//------------------------------------------------------------------------------
// Dashboard
//------------------------------------------------------------------------------

dhis2.db.tmpl = {
    dashboardLink: "<li id='dashboard-${id}'><a href='javascript:dhis2.db.renderDashboardWithEvent( \"${id}\" )'>${name}</a></li>",

    moduleIntro: "<li><div class='dasboardIntro'>${i18n_click}</div></li>",

    dashboardIntro: "<li><div class='dasboardIntro'>${i18n_add}</div><div class='dasboardTip'>${i18n_arrange}</div></li>",

    hitHeader: "<li class='hitHeader'><span class='bold'>${title}</span> &nbsp; " +
    "<a id='hitMore-${type}' href='javascript:dhis2.db.searchMore( \"${type}\" )'>${i18n_see_more_hits} &raquo;</a>" +
    "<a id='hitFewer-${type}' href='javascript:dhis2.db.searchFewer( \"${type}\" )' style='display:none'>&laquo; ${i18n_see_fewer_hits}</a></li>",

    hitItem: "<li><a class='viewLink' href='${link}'><img src='../images/${img}.png'>${name}</a>" +
    "{{if canManage}}<a class='addLink' href='javascript:dhis2.db.addItemContent( \"${type}\", \"${id}\" )'>${i18n_add}</a>{{/if}}</li>",

    appHitItem: "<li><a class='viewLink' href='javascript:dhis2.db.addItemContent( \"${type}\", \"${id}\" )'><img src='../images/${img}.png'>${name}</a>" +
    "{{if canManage}}<a class='addLink' href='javascript:dhis2.db.addItemContent( \"${type}\", \"${id}\" )'>${i18n_add}</a>{{/if}}</li>",

    chartItem: "<li id='liDrop-${itemId}' class='liDropItem'><div class='dropItem' id='drop-${itemId}' style='${style}' data-item='${itemId}'></div></li>" +
    "<li id='li-${itemId}' class='liItem'><div class='item' id='${itemId}' style='${style}'><div class='itemHeader'><a href='javascript:dhis2.db.removeItem( \"${itemId}\" )'>${i18n_remove}</a>|" +
    "<a href='javascript:dhis2.db.resizeItem( \"${itemId}\" )'>${i18n_resize}</a>|" +
    "<a href='javascript:dhis2.db.exploreChart( \"${id}\" )'>${i18n_explore}</a>|" +
    "<a href='javascript:dhis2.db.viewShareForm( \"${id}\", \"chart\", \"${name}\" )'>${i18n_share}</a>" +
    "{{if interpretationCount > 0}}<a href='#' onclick='dhis2.db.showInterpretationPopup( event, \"${id}\", \"CHART\" );return false;' title=\"${interpretationCount} interpretations\"><i class=\"fa fa-comments-o\"></i>${interpretationCount}</a>{{/if}}" +
    "{{if interpretationLikeCount > 0}}<a href='#' onclick='dhis2.db.showInterpretationPopup( event, \"${id}\", \"CHART\" );return false;' title=\"${interpretationLikeCount} likes\"><i class=\"fa fa-thumbs-o-up\"></i>${interpretationLikeCount}</a>{{/if}}" +
    "<i class=\"fa fa-arrows dragIcon\" title=\"${i18n_click_and_drag_to_new_position}\"></i></div>" +
    "<div id='plugin-${itemId}' style='width:100%; height:" + dhis2.db.itemContentHeight + "px'></div>" +
    //"<div id='plugin-${itemId}' style='font-family:sans-serif !important'></div>" +
    "</div></li>",
    
    eventChartItem: "<li id='liDrop-${itemId}' class='liDropItem'><div class='dropItem' id='drop-${itemId}' style='${style}' data-item='${itemId}'></div></li>" +
    "<li id='li-${itemId}' class='liItem'><div class='item' id='${itemId}' style='${style}'><div class='itemHeader'><a href='javascript:dhis2.db.removeItem( \"${itemId}\" )'>${i18n_remove}</a>|" +
    "<a href='javascript:dhis2.db.resizeItem( \"${itemId}\" )'>${i18n_resize}</a>|" +
    "<a href='javascript:dhis2.db.exploreEventChart( \"${id}\" )'>${i18n_explore}</a>|" +
    "<a href='javascript:dhis2.db.viewShareForm( \"${id}\", \"eventChart\", \"${name}\" )'>${i18n_share}</a>" +
    "{{if interpretationCount > 0}}<a href='#' onclick='dhis2.db.showInterpretationPopup( event, \"${id}\", \"EVENT_CHART\" );return false;' title=\"${interpretationCount} interpretations\"><i class=\"fa fa-comments-o\"></i>${interpretationCount}</a>{{/if}}" +
    "{{if interpretationLikeCount > 0}}<a href='#' onclick='dhis2.db.showInterpretationPopup( event, \"${id}\", \"EVENT_CHART\" );return false;' title=\"${interpretationLikeCount} likes\"><i class=\"fa fa-thumbs-o-up\"></i>${interpretationLikeCount}</a>{{/if}}" +
    "<i class=\"fa fa-arrows dragIcon\" title=\"${i18n_click_and_drag_to_new_position}\"></i></div>" +
    "<div id='plugin-${itemId}'></div>" +
    "</div></li>",

    mapItem: "<li id='liDrop-${itemId}' class='liDropItem'><div class='dropItem' id='drop-${itemId}' style='${style}' data-item='${itemId}'></div></li>" +
    "<li id='li-${itemId}' class='liItem'><div class='item' id='${itemId}' style='${style}'><div class='itemHeader'><a href='javascript:dhis2.db.removeItem( \"${itemId}\" )'>${i18n_remove}</a>|" +
    "<a href='javascript:dhis2.db.resizeItem( \"${itemId}\" )'>${i18n_resize}</a>|" +
    "<a href='javascript:dhis2.db.exploreMap( \"${id}\" )'>${i18n_explore}</a>|" +
    "<a href='javascript:dhis2.db.viewShareForm( \"${id}\", \"map\", \"${name}\" )'>${i18n_share}</a>" +
    "{{if interpretationCount > 0}}<a href='#' onclick='dhis2.db.showInterpretationPopup( event, \"${id}\", \"MAP\" );return false;' title=\"${interpretationCount} interpretations\"><i class=\"fa fa-comments-o\"></i>${interpretationCount}</a>{{/if}}" +
    "{{if interpretationLikeCount > 0}}<a href='#' onclick='dhis2.db.showInterpretationPopup( event, \"${id}\", \"MAP\" );return false;' title=\"${interpretationLikeCount} likes\"><i class=\"fa fa-thumbs-o-up\"></i>${interpretationLikeCount}</a>{{/if}}" +
    "<i class=\"fa fa-arrows dragIcon\" title=\"${i18n_click_and_drag_to_new_position}\"></i></div>" +
    "<div id='plugin-${itemId}' style='width:100%; height:${height}px'></div>" +
    "</div></li>",

    reportTableItem: "<li id='liDrop-${itemId}' class='liDropItem'><div class='dropItem' id='drop-${itemId}' data-item='${itemId}'></div></li>" +
    "<li id='li-${itemId}' class='liItem'><div class='item' id='${itemId}' style='${style}'><div class='itemHeader'><a href='javascript:dhis2.db.removeItem( \"${itemId}\" )'>${i18n_remove}</a>|" +
    "<a href='javascript:dhis2.db.resizeItem( \"${itemId}\" )'>${i18n_resize}</a>|" +
    "<a href='javascript:dhis2.db.exploreReportTable( \"${id}\" )'>${i18n_explore}</a>|" +
    "<a href='javascript:dhis2.db.viewShareForm( \"${id}\", \"reportTable\", \"${name}\" )'>${i18n_share}</a>" +
    "{{if interpretationCount > 0}}<a href='#' onclick='dhis2.db.showInterpretationPopup( event, \"${id}\", \"REPORT_TABLE\" );return false;' title=\"${interpretationCount} interpretations\"><i class=\"fa fa-comments-o\"></i>${interpretationCount}</a>{{/if}}" +
    "{{if interpretationLikeCount > 0}}<a href='#' onclick='dhis2.db.showInterpretationPopup( event, \"${id}\", \"REPORT_TABLE\" );return false;' title=\"${interpretationLikeCount} likes\"><i class=\"fa fa-thumbs-o-up\"></i>${interpretationLikeCount}</a>{{/if}}" +
    "<i class=\"fa fa-arrows dragIcon\" title=\"${i18n_click_and_drag_to_new_position}\"></i></div>" +
    "<div id='plugin-${itemId}'></div>" +
    "</div></li>",

    eventReportItem: "<li id='liDrop-${itemId}' class='liDropItem'><div class='dropItem' id='drop-${itemId}' data-item='${itemId}'></div></li>" +
    "<li id='li-${itemId}' class='liItem'><div class='item' id='${itemId}' style='${style}'><div class='itemHeader'><a href='javascript:dhis2.db.removeItem( \"${itemId}\" )'>${i18n_remove}</a>|" +
    "<a href='javascript:dhis2.db.resizeItem( \"${itemId}\", true )'>${i18n_resize}</a>|" +
    "<a href='javascript:dhis2.db.exploreEventReport( \"${id}\" )'>${i18n_explore}</a>|" +
    "<a href='javascript:dhis2.db.viewShareForm( \"${id}\", \"eventReport\", \"${name}\" )'>${i18n_share}</a>" +
    "{{if interpretationCount > 0}}<a href='#' onclick='dhis2.db.showInterpretationPopup( event, \"${id}\", \"EVENT_REPORT\" );return false;' title=\"${interpretationCount} interpretations\"><i class=\"fa fa-comments-o\"></i>${interpretationCount}</a>{{/if}}" +
    "{{if interpretationLikeCount > 0}}<a href='#' onclick='dhis2.db.showInterpretationPopup( event, \"${id}\", \"EVENT_REPORT\" );return false;' title=\"${interpretationLikeCount} likes\"><i class=\"fa fa-thumbs-o-up\"></i>${interpretationLikeCount}</a>{{/if}}" +
    "<i class=\"fa fa-arrows dragIcon\" title=\"${i18n_click_and_drag_to_new_position}\"></i></div>" +
    "<div id='plugin-${itemId}'></div>" +
    "</div></li>",

    loadingAppItem: "<li id='liDrop-${itemId}' class='liDropItem'><div class='dropItem' id='drop-${itemId}' data-item='${itemId}'></div><li>" +
    "<li id='li-${itemId}' class='liItem'><div class='item' id='${itemId}' style='${style}'><div class='itemHeader'>" +
    "<a href='javascript:dhis2.db.removeItem( \"${itemId}\" )'>${i18n_remove}</a>|" +
    "<a href='javascript:dhis2.db.resizeItem( \"${itemId}\", true )'>${i18n_resize}</a>" +
    "<i class=\"fa fa-arrows dragIcon\" title=\"${i18n_click_and_drag_to_new_position}\"></i>" +
    "</div><div style='position:relative'>" +
    "<div id='plugin-${itemId}' style='width:100%;height:${dhis2.db.itemContentHeight}px'></div>" +
    "</div></div></li>",

    missingAppItem: "<li id='liDrop-${itemId}' class='liDropItem'><div class='dropItem' id='drop-${itemId}' data-item='${itemId}'></div><li>" +
    "<li id='li-${itemId}' class='liItem'><div class='item' id='${itemId}' style='${style}'><div class='itemHeader'>" +
    "<a href='javascript:dhis2.db.removeItem( \"${itemId}\" )'>${i18n_remove}</a>|" +
    "<a href='javascript:dhis2.db.resizeItem( \"${itemId}\", true )'>${i18n_resize}</a>" +
    "<i class=\"fa fa-arrows dragIcon\" title=\"${i18n_click_and_drag_to_new_position}\"></i>" +
    "</div><div style='position:relative'>" +
    "<div id='plugin-${itemId}' style='width:100%;height:${dhis2.db.itemContentHeight}px'>${i18n_missing_dashboard_app}: ${appKey}</div>" +
    "</div></div></li>",

    appItem: "<li id='liDrop-${itemId}' class='liDropItem'><div class='dropItem' id='drop-${itemId}' data-item='${itemId}'></div><li>" +
    "<li id='li-${itemId}' class='liItem'><div class='item' id='${itemId}' style='${style}'><div class='itemHeader'>" +
    "<a href='javascript:dhis2.db.removeItem( \"${itemId}\" )'>${i18n_remove}</a>|" +
    "<a href='javascript:dhis2.db.exploreApp( \"${appKey}\", \"${itemId}\" )'>${i18n_explore}</a>|" +
    "<a href='javascript:dhis2.db.resizeItem( \"${itemId}\", true )'>${i18n_resize}</a>" +
    "<i class=\"fa fa-arrows dragIcon\" title=\"${i18n_click_and_drag_to_new_position}\"></i>" +
    "</div><div style='position:relative'>" +
    "<iframe id='plugin-${itemId}' style='width:100%;height:${dhis2.db.itemContentHeight-4}px;border:0' allowfullscreen src='${app.launchUrl}?dashboardItemId=${itemId}'></iframe>" +
    "</div></div></li>",
    
    favoriteDescription : "<div id='favoriteDescription'>${description}</div>",

	interpretation : "<a href='javascript:dhis2.db.${exploreFunction}( \"${favoriteId}\", \"${interpretationId}\" )'><div id='interpretationContainer'><div>" +
	"<label class='interpretationUser'>${userDisplayName}</label>" +
	"<label>${formatDate(lastUpdated, 'yy-MM-dd')}</label></div>" +
	"<div><label>${text}</label></div>" +
	"<div class='likeComment'><label>" +
	"<div style='padding:5px 0;'><i class='fa fa-thumbs-o-up'></i><span style='color:#3162C5;'>${numberLikes} people</span> like this. <span style='color:#3162C5;'>${numberComments} people</span> commented.</div>" +
	"</label></div></div></a>"
};

dhis2.db.dashboardReady = function () {
    $(".item").draggable({
        containment: "#contentDiv",
        helper: "clone",
        stack: ".item",
        revert: "invalid",
        start: dhis2.db.dragStart,
        stop: dhis2.db.dragStop
    });

    $(".item").droppable({
        accept: ".item",
        over: dhis2.db.dropOver
    });

    $(".dropItem").droppable({
        accept: ".item",
        drop: dhis2.db.dropItem
    });

    $(".lastDropItem").droppable({
        accept: ".item",
        over: dhis2.db.lastDropOver,
        out: dhis2.db.lastDropOut
    });
}

dhis2.db.addDragDrop = function (id) {
    $("#" + id).draggable({
        containment: "#contentDiv",
        helper: "clone",
        stack: ".item",
        revert: "invalid",
        start: dhis2.db.dragStart,
        stop: dhis2.db.dragStop
    });

    $("#" + id).droppable({
        accept: ".item",
        over: dhis2.db.dropOver
    });

    $("#drop-" + id).droppable({
        accept: ".item",
        drop: dhis2.db.dropItem
    });
}

dhis2.db.dragStart = function (event, ui) {
    $(this).hide();
    dhis2.db.currentItem = $(this).attr("id");
    dhis2.db.currentItemPos = dhis2.db.getIndex(dhis2.db.currentItem);
}

dhis2.db.dragStop = function (event, ui) {
    $(this).show();
    $(".dropItem").not(".lastDropItem").hide();
    $(".lastDropItem").removeClass("blankDropItem").addClass("blankDropItem");
    dhis2.db.currentItem = undefined;
    dhis2.db.currentItemPos = undefined;
}

dhis2.db.dropOver = function (event, ui) {
    $(".dropItem").not("#dropLast").hide();
    var itemId = $(this).attr("id");
    var dropItemId = "drop-" + itemId;
    $("#" + dropItemId).show();
}

dhis2.db.lastDropOver = function (event, ui) {
    $(".dropItem").not("#dropLast").hide();
    $(this).removeClass("blankDropItem").css("display", "block");
}

dhis2.db.lastDropOut = function (event, ui) {
    $(this).addClass("blankDropItem");
}

dhis2.db.dropItem = function (event, ui) {
    var destItemId = $(this).data("item");
    var position = dhis2.db.getIndex(destItemId);

    dhis2.db.moveItem(dhis2.db.currentItem, destItemId, position);
}

dhis2.db.scrollLeft = function () {
    $("#dashboardListWrapper").animate({
        scrollTop: "-=29"
    }, 180);
}

dhis2.db.scrollRight = function () {
    $("#dashboardListWrapper").animate({
        scrollTop: "+=29"
    }, 180);
}

dhis2.db.openAddDashboardForm = function () {
    $("#addDashboardForm").dialog({
        autoOpen: true,
        modal: true,
        width: 415,
        height: 100,
        resizable: false,
        title: "Add new dashboard"
    });
}

dhis2.db.openManageDashboardForm = function () {
    if (undefined !== dhis2.db.current()) {
        $.getJSON("../api/dashboards/" + dhis2.db.current(), function (data) {
            var name = data.name;
            $("#dashboardRename").val(name);
            $("#manageDashboardForm").dialog({
                autoOpen: true,
                modal: true,
                width: 405,
                height: 430,
                resizable: false,
                title: name
            });
        });
    }
}

dhis2.db.addDashboard = function () {
    var item = '{"name": "' + $("#dashboardName").val() + '"}';

    $("#addDashboardForm").dialog("destroy");

    $.ajax({
        type: "post",
        url: "../api/dashboards",
        data: item,
        contentType: "application/json",
        success: function (data, text, xhr) {
            $("#dashboardName").val("");
            var location = xhr.getResponseHeader("Location");

            if (location !== undefined && location.lastIndexOf("/") != -1) {
                var itemId = location.substring(( location.lastIndexOf("/") + 1 ));
                dhis2.db.setCurrent(itemId);
            }

            dhis2.db.renderDashboardListLoadFirst();
        }
    });
}

dhis2.db.renameDashboard = function () {
    var name = $("#dashboardRename").val();

    $("#manageDashboardForm").dialog("destroy");

    if (undefined !== dhis2.db.current() && undefined !== name && name.trim().length > 0) {
        var data = "{ \"name\": \"" + name + "\"}";

        $.ajax({
            type: "put",
            url: "../api/dashboards/" + dhis2.db.current(),
            contentType: "application/json",
            data: data,
            success: function () {
                $("#dashboardRename").val("");
                dhis2.db.renderDashboardListLoadFirst();
            }
        });
    }
}

dhis2.db.removeDashboard = function () {
    if (undefined !== dhis2.db.current()) {
        $.ajax({
            type: "delete",
            url: "../api/dashboards/" + dhis2.db.current(),
            success: function () {
                $("#manageDashboardForm").dialog("destroy");
                dhis2.db.clearCurrent();
                dhis2.db.renderDashboardListLoadFirst();
            }
        });
    }
}


dhis2.db.translateDashboard = function () {
    if (undefined !== dhis2.db.current()) {
        var currentPage = encodeURI(window.location.href);
        window.location.href = "../dhis-web-commons/i18n.action?className=Dashboard&uid=" + dhis2.db.current() + "&returnUrl=" + currentPage;
    }
}

function updateSharing(dashboard) {
    $('#dashboardMenu').data('canManage', dashboard.access.manage);

    if (dashboard.access.manage) {
        var manageLink = $('<a/>')
            .addClass('bold')
            .text(i18n_manage)
            .attr('href', 'javascript:dhis2.db.openManageDashboardForm()');
        $('#manageDashboard').html(manageLink);

        var sharingLink = $('<a/>')
            .addClass('bold')
            .text(i18n_share)
            .attr('href', 'javascript:showSharingDialog("dashboard", "' + dashboard.id + '")');
        $('#manageSharing').html(sharingLink);
    }
    else {
        var manageLink = $('<a/>')
            .addClass('bold')
            .css({
                'cursor': 'default',
                'text-decoration': 'none',
                'color': '#888'
            })
            .text(i18n_manage);
        $('#manageDashboard').html(manageLink);

        var sharingLink = $('<a/>')
            .addClass('bold')
            .css({
                'cursor': 'default',
                'text-decoration': 'none',
                'color': '#888'
            })
            .text(i18n_share);
        $('#manageSharing').html(sharingLink);
    }
}

dhis2.db.renderDashboardListLoadFirst = function () {
    var $l = $("#dashboardList");

    $l.empty();

    $.getJSON("../api/dashboards.json?fields=id,displayName&paging=false&links=false&" + dhis2.util.cacheBust(), function (data) {
        if (undefined !== data.dashboards) {
            var first;

            $.each(data.dashboards, function (index, dashboard) {
                $l.append($.tmpl(dhis2.db.tmpl.dashboardLink, {"id": dashboard.id, "name": dashboard.displayName}));

                if (index == 0) {
                    first = dashboard.id;
                }
            });

            if (undefined === dhis2.db.current()) {
                dhis2.db.setCurrent(first);
            }

            if (undefined !== dhis2.db.current()) {
                dhis2.db.renderDashboard(dhis2.db.current());
            }
            else {
                dhis2.db.clearDashboard();
            }
        }
        else {
            dhis2.db.clearDashboard();
            $("#contentList").append($.tmpl(dhis2.db.tmpl.moduleIntro, {"i18n_click": i18n_click_add_new_to_get_started}));
        }
    });
}

dhis2.db.clearDashboard = function () {
    $d = $("#contentList").empty();
}

dhis2.db.getFullWidth = function () {
    var viewPortWidth = $(window).width(),
        spacing = 18,
        itemWidth = dhis2.db.widthNormal,
        items = Math.floor(( viewPortWidth - spacing ) / ( itemWidth + spacing )),
        fullWidth = ( items * itemWidth ) + ( ( items - 1 ) * spacing );

    return fullWidth;
}

/**
 * Toggles size of item. The order is 1) normal 2) double 3) full.
 */
dhis2.db.resizeItem = function (id, isScrollbar) {
    $.getJSON("../api/dashboardItems/" + id, function (item) {

        var newShape = dhis2.db.shapeNormal;

        if (dhis2.db.shapeDoubleWidth == item.shape) {
            newShape = dhis2.db.shapeFullWidth;
            dhis2.db.setFullItemWidth(id, isScrollbar);
        }
        else if (dhis2.db.shapeFullWidth == item.shape) {
            newShape = dhis2.db.shapeNormal;
            dhis2.db.setNormalItemWidth(id, isScrollbar);
        }
        else {
            newShape = dhis2.db.shapeDoubleWidth;
            dhis2.db.setDoubleItemWidth(id, isScrollbar);
        }

        if (newShape) {
            $.ajax({
                url: "../api/dashboardItems/" + id + "/shape/" + newShape,
                type: "put"
            });
        }
    });
}

dhis2.db.onSetItemWidth = function (id, width, isScrollbar) {
    var el = Ext.get("plugin-" + id);
    var fn = el.setViewportWidth || el.dom.setViewportWidth;
    if (fn) {
        fn(width - (isScrollbar ? this.itemScrollbarWidth : 0));
    }
}

dhis2.db.setNormalItemWidth = function (id, isScrollbar) {
    $("#" + id).css("width", dhis2.db.widthNormal + "px");
    $("#drop-" + id).css("width", dhis2.db.widthNormal + "px");
    this.onSetItemWidth(id, this.widthNormal, isScrollbar);
}

dhis2.db.setDoubleItemWidth = function (id, isScrollbar) {
    $("#" + id).css("width", dhis2.db.widthDouble + "px");
    $("#drop-" + id).css("width", dhis2.db.widthDouble + "px");
    this.onSetItemWidth(id, this.widthDouble, isScrollbar);
}

dhis2.db.setFullItemWidth = function (id, isScrollbar) {
    var fullWidth = dhis2.db.getFullWidth();
    $("#" + id).css("width", fullWidth + "px");
    $("#drop-" + id).css("width", fullWidth + "px");
    this.onSetItemWidth(id, fullWidth, isScrollbar);
}

dhis2.db.drawWideItems = function () {
    if (undefined !== dhis2.db.current()) {
        var url = "../api/dashboards/" + dhis2.db.current() + "?fields=dashboardItems[id,shape]",
            viewPortWidth = $(window).width(),
            marginAndSpacing = 60,
            realWidth = ( viewPortWidth - marginAndSpacing );

        $.getJSON(url, function (dashboard) {
            $.each(dashboard.dashboardItems, function (i, item) {
                if (dhis2.db.shapeFullWidth == item.shape) {
                    dhis2.db.setFullItemWidth(item.id);
                }
                else if (realWidth <= dhis2.db.widthDouble && dhis2.db.shapeDoubleWidth == item.shape) {
                    dhis2.db.setNormalItemWidth(item.id);
                }
                else if (realWidth > dhis2.db.widthDouble && dhis2.db.shapeDoubleWidth == item.shape) {
                    dhis2.db.setDoubleItemWidth(item.id);
                }
            });
        });
    }
}

dhis2.db.renderDashboardWithEvent = function (id) {
	dhis2.db.renderDashboard(id);
    dhis2.db.registerDashboardViewEvent();
}

dhis2.db.renderDashboard = function (id) {
    if (!id) {
        return;
    }

    dhis2.db.reportTableItems = [];
    dhis2.db.chartItems = [];

    var fullWidth = dhis2.db.getFullWidth();

    $("#dashboard-" + dhis2.db.current()).removeClass("currentDashboard");

    dhis2.db.setCurrent(id);

    $("#dashboard-" + dhis2.db.current()).addClass("currentDashboard");

    $.getJSON("../api/dashboards/" + id + "?fields=:all,dashboardItems[:all,reports[id,displayName],chart[id,displayName],map[id,displayName],reportTable[id,displayName],resources[id,displayName],users[id,displayName]]&" + dhis2.util.cacheBust(), function (data) {
        $( "#dashboardTitle" ).html( data.displayName );
        $d = $("#contentList").empty();

        updateSharing(data);

        if (data.dashboardItems && data.dashboardItems.length) {
            $.each(data.dashboardItems, function (index, dashboardItem) {
                if (!dashboardItem) {
                    return true;
                }

                var width = dhis2.db.widthNormal;

                if (dhis2.db.shapeFullWidth == dashboardItem.shape) {
                    width = fullWidth;
                }
                else if (dhis2.db.shapeDoubleWidth == dashboardItem.shape) {
                    width = dhis2.db.widthDouble;
                }

                dhis2.db.renderItem($d, dashboardItem, width, false);
            });

            // report table
            reportTablePlugin.url = '..';
            reportTablePlugin.dashboard = true;
            reportTablePlugin.showTitles = true;
            reportTablePlugin.load(dhis2.db.reportTableItems);

            // chart
            chartPlugin.url = '..';
            chartPlugin.dashboard = true;
            chartPlugin.showTitles = true;
            chartPlugin.load(dhis2.db.chartItems);

            dhis2.db.renderLastDropItem($d);
        }
        else {
            $d.append($.tmpl(dhis2.db.tmpl.dashboardIntro, {
                "i18n_add": i18n_add_stuff_by_searching,
                "i18n_arrange": i18n_arrange_dashboard_by_dragging_and_dropping
            }));
        }

        dhis2.db.dashboardReady();
    });
}

dhis2.db.linkItemHeaderHtml = function (itemId, title) {
    var html =
        "<li id='liDrop-" + itemId + "' class='liDropItem'><div class='dropItem' id='drop-" + itemId + "' data-item='" + itemId + "'></div></li>" +
        "<li id='li-" + itemId + "' class='liItem'><div class='item' id='" + itemId + "'><div class='itemHeader'><a href='javascript:dhis2.db.removeItem( \"" + itemId + "\" )'>" + i18n_remove + "</a></div>" +
        "<ul id='ul-" + itemId + "' class='itemList'><li class='itemTitle' title='" + i18n_drag_to_new_position + "'>" + title + "</li>";

    return html;
}

dhis2.db.renderItem = function ($d, dashboardItem, width, prepend, autoRender) {
    width = width || dhis2.db.widthNormal;
    prepend = prepend || false;
    autoRender = autoRender || false;

    var graphStyle = "width:" + width + "px; overflow:hidden;";
    var tableStyle = "width:" + width + "px;";
    var userOrgUnit = dhis2.db.currentUserOrgUnit || [];

    if ("CHART" == dashboardItem.type) {
        var pluginItems = dhis2.db.chartItems;
        var content = $.tmpl(dhis2.db.tmpl.chartItem, {
            "itemId": dashboardItem.id,
            "id": dashboardItem.chart.id,
            "name": dashboardItem.chart.displayName,
            "style": graphStyle,
            "i18n_remove": i18n_remove,
            "i18n_get_as_image": i18n_get_as_image,
            "i18n_share": i18n_share_interpretation,
            "i18n_click_and_drag_to_new_position": i18n_click_and_drag_to_new_position,
            "interpretationCount": dashboardItem.interpretationCount,
            "interpretationLikeCount": dashboardItem.interpretationLikeCount
        });
        dhis2.db.preOrAppend($d, content, prepend);

        var pluginItem = {
            url: '..',
            el: 'plugin-' + dashboardItem.id,
            id: dashboardItem.chart.id,
            userOrgUnit: userOrgUnit
        };

        if (autoRender) {
            chartPlugin.load(pluginItem);
        }
        else {
            pluginItems.push(pluginItem);
        }
    }
    else if ("EVENT_CHART" == dashboardItem.type) {
        var content = $.tmpl(dhis2.db.tmpl.eventChartItem, {
            "itemId": dashboardItem.id,
            "id": dashboardItem.eventChart.id,
            "name": dashboardItem.eventChart.displayName,
            "style": graphStyle,
            "i18n_remove": i18n_remove,
            "i18n_get_as_image": i18n_get_as_image,
            "i18n_share": i18n_share_interpretation,
            "i18n_click_and_drag_to_new_position": i18n_click_and_drag_to_new_position,
            "i18n_explore": i18n_explore,
            "interpretationCount": dashboardItem.interpretationCount,
            "interpretationLikeCount": dashboardItem.interpretationLikeCount
        });
        dhis2.db.preOrAppend($d, content, prepend);

        DHIS.getEventChart({
            url: '..',
            el: 'plugin-' + dashboardItem.id,
            id: dashboardItem.eventChart.id,
            width: width,
            height: dhis2.db.itemContentHeight,
            dashboard: true,
            crossDomain: false,
            skipMask: true,
            userOrgUnit: userOrgUnit,
            domainAxisStyle: {
                labelRotation: 45,
                labelFont: '10px sans-serif',
                labelColor: '#111'
            },
            rangeAxisStyle: {
                labelFont: '9px sans-serif'
            },
            legendStyle: {
                labelFont: 'normal 10px sans-serif',
                labelColor: '#222',
                labelMarkerSize: 10,
                titleFont: 'bold 12px sans-serif',
                titleColor: '#333'
            },
            seriesStyle: {
                labelColor: '#333',
                labelFont: '9px sans-serif'
            }
        });
    }
    else if ("MAP" == dashboardItem.type) {
        var content = $.tmpl(dhis2.db.tmpl.mapItem, {
            "itemId": dashboardItem.id,
            "id": dashboardItem.map.id,
            "name": dashboardItem.map.displayName,
            "style": graphStyle,
            "height": dhis2.db.itemContentHeight,
            "i18n_remove": i18n_remove,
            "i18n_get_as_image": i18n_get_as_image,
            "i18n_share": i18n_share_interpretation,
            "i18n_click_and_drag_to_new_position": i18n_click_and_drag_to_new_position,
            "interpretationCount": dashboardItem.interpretationCount,
            "interpretationLikeCount": dashboardItem.interpretationLikeCount
        });
        dhis2.db.preOrAppend($d, content, prepend);

        DHIS.getMap({
            url: '..',
            el: 'plugin-' + dashboardItem.id,
            id: dashboardItem.map.id,
            hideLegend: true,
            dashboard: true,
            crossDomain: false,
            skipMask: true,
            mapViews: [{
                userOrgUnit: userOrgUnit
            }]
        });
    }
    else if ("REPORT_TABLE" == dashboardItem.type) {
        var pluginItems = dhis2.db.reportTableItems;
        var content = $.tmpl(dhis2.db.tmpl.reportTableItem, {
            "itemId": dashboardItem.id,
            "id": dashboardItem.reportTable.id,
            "name": dashboardItem.reportTable.displayName,
            "style": tableStyle,
            "i18n_remove": i18n_remove,
            "i18n_share": i18n_share_interpretation,
            "i18n_click_and_drag_to_new_position": i18n_click_and_drag_to_new_position,
            "interpretationCount": dashboardItem.interpretationCount,
            "interpretationLikeCount": dashboardItem.interpretationLikeCount
        });
        dhis2.db.preOrAppend($d, content, prepend);

        var pluginItem = {
            url: '..',
            el: 'plugin-' + dashboardItem.id,
            id: dashboardItem.reportTable.id,
            userOrgUnit: userOrgUnit
        };

        if (autoRender) {
            reportTablePlugin.load(pluginItem);
        }
        else {
            pluginItems.push(pluginItem);
        }
    }
    else if ("EVENT_REPORT" == dashboardItem.type) {
        var content = $.tmpl(dhis2.db.tmpl.eventReportItem, {
            "itemId": dashboardItem.id,
            "id": dashboardItem.eventReport.id,
            "name": dashboardItem.eventReport.displayName,
            "style": tableStyle,
            "i18n_remove": i18n_remove,
            "i18n_share": i18n_share_interpretation,
            "i18n_click_and_drag_to_new_position": i18n_click_and_drag_to_new_position,
            "interpretationCount": dashboardItem.interpretationCount,
            "interpretationLikeCount": dashboardItem.interpretationLikeCount
        });
        dhis2.db.preOrAppend($d, content, prepend);

        DHIS.getEventReport({
            url: '..',
            el: 'plugin-' + dashboardItem.id,
            id: dashboardItem.eventReport.id,
            dashboard: true,
            crossDomain: false,
            skipMask: true,
            displayDensity: 'compact',
            fontSize: 'small',
            userOrgUnit: userOrgUnit
        });
    }
    else if ("USERS" == dashboardItem.type) {
        dhis2.db.renderLinkItem($d, dashboardItem.id, dashboardItem.users, "Users", "../dhis-web-dashboard-integration/profile.action?id=", "");
    }
    else if ("REPORTS" == dashboardItem.type) {
        dhis2.db.renderLinkItem($d, dashboardItem.id, dashboardItem.reports, "Reports", "../dhis-web-reporting/getReportParams.action?mode=report&uid=", "");
    }
    else if ("RESOURCES" == dashboardItem.type) {
        dhis2.db.renderLinkItem($d, dashboardItem.id, dashboardItem.resources, "Resources", "../api/documents/", "/data");
    }
    else if ("MESSAGES" == dashboardItem.type) {
        dhis2.db.renderMessagesItem($d, dashboardItem.id);
    }
    else if ("APP" == dashboardItem.type) {
        if (undefined === dhis2.db.apps) {
            $.getJSON('../api/apps', function (apps) {
                dhis2.db.apps = {};
                $.each(apps, function (i, app) {
                    dhis2.db.apps[app.key] = app;
                });
                dhis2.db.renderDashboard(dhis2.db.current());
            })
                .fail(function (xhr, status, err) {
                    console.warn('Failed to load apps:', err);
                    dhis2.db.apps = {};
                    dhis2.db.renderDashboard(dhis2.db.current());
                });
        }

        var content = $.tmpl(dhis2.db.tmpl.loadingAppItem, {
            "itemId": dashboardItem.id,
            "appKey": dashboardItem.appKey,
            "style": tableStyle,
            "i18n_remove": i18n_remove,
            "i18n_share": i18n_share_interpretation,
            "i18n_click_and_drag_to_new_position": i18n_click_and_drag_to_new_position
        });

        if (dhis2.db.apps) {
            if (dhis2.db.apps.hasOwnProperty(dashboardItem.appKey)) {
                content = $.tmpl(dhis2.db.tmpl.appItem, {
                    "itemId": dashboardItem.id,
                    "appKey": dashboardItem.appKey,
                    "app": dhis2.db.apps[dashboardItem.appKey],
                    "style": tableStyle,
                    "i18n_remove": i18n_remove,
                    "i18n_share": i18n_share_interpretation,
                    "i18n_click_and_drag_to_new_position": i18n_click_and_drag_to_new_position
                });
            }
            else {
                content = $.tmpl(dhis2.db.tmpl.missingAppItem, {
                    "itemId": dashboardItem.id,
                    "appKey": dashboardItem.appKey,
                    "style": tableStyle,
                    "i18n_remove": i18n_remove,
                    "i18n_share": i18n_share_interpretation,
                    "i18n_click_and_drag_to_new_position": i18n_click_and_drag_to_new_position
                });
            }
        }

        dhis2.db.preOrAppend($d, content, prepend);
    }
}

/**
 * Initializes and populates the Message widget for dashboard
 * @param $d container to append widget
 * @param itemId unique to this widget
 */
dhis2.db.renderMessagesItem = function ($d, itemId) {

    // Create widget container and append to the dashboard
    $d.append(dhis2.db.linkItemHeaderHtml(itemId, "Messages") + "</ul></div></li>");

    // Select widget list
    var $ul = $("#ul-" + itemId);

    // Add paging after list
    dhis2.db.createMessagePaging($ul, itemId);

    // Populate the widget
    dhis2.db.updateMessagesItem($ul, itemId, 1);
}

/**
 * Generates HTML based on the message
 * @param message message data to render
 * @returns {string} HTML representing the message
 */
dhis2.db.getMessageHtml = function (message) {

    function getSender() {
        var firstName = message.lastSenderFirstname || "";
        var surname = message.lastSenderSurname || "";

        return firstName + " " + surname;
    }

    function getCount() {
        return (message.messageCount > 1 ? "(" + message.messageCount + ")" : "");
    }

    return "<li class='message-item'> <a href='readMessage.action?id=" + message.id + "' class='" + (message.read ? "" : "bold") +
        "'> <div> <span>" + getSender() + " " + getCount() + "</span> <span class='tipText' style='float:right'>" + message.lastMessage +
        "</span> </div> <div> <span>" + message.name + "</span> </div> </a> </li>";
}

/**
 * Generates HTML for the message paging
 * @param $ul Element to insert after
 */
dhis2.db.createMessagePaging = function ($ul, itemId) {

    // Add page counter
    $ul.children(".itemTitle").append("<span style='float: right'></span>");

    // Add button container
    $ul.after("<div id='message-dashboard-pager-"+itemId+"' class='message-dashboard-pager'></div>");

    var $div = $("#message-dashboard-pager-"+itemId);

    // Add buttons
    $div.append(
        "<a href='#' id='message-"+itemId+"-prev-page' data-page-link='0' class='material-icons' style='text-decoration:none'>navigate_before</a>" +
        "<a href='#' id='message-"+itemId+"-next-page' data-page-link='0' class='material-icons' style='text-decoration:none'>navigate_next</a>");

    // Add clickHandler
    $div.children("*").click(function() {
        dhis2.db.updateMessagesItem($ul, itemId, $(this).attr("data-page-link"));
    });
}

/**
 * Updates the actual messages based on the current page
 * @param $ul list to update
 * @param page to update to
 */
dhis2.db.updateMessagesItem = function($ul, itemId, page) {

    // Ignore page == -1
    if(page == -1) {
        return false;
    }

    // Clear previous list of messages
    $ul.children( ".message-item").remove();

    // Fetch new list of messages
    $.get("../api/messageConversations.json?fields=:all&pageSize=5&page=" + page, function (json) {
        $.each(json.messageConversations, function (index, message) {

            // Generate HTML for each message
            $ul.append(dhis2.db.getMessageHtml(message));

        });

        // Update counter to reflect new page
        dhis2.db.updateMessagePaging($ul, itemId, page, json.pager.pageCount);
    });
}

/**
 * Updates the page number, and buttons to reflect the current page
 */
dhis2.db.updateMessagePaging = function($ul, itemId, page, pageCount) {
    // Ignore bad values
    if(page < 1 || page > pageCount) {
        return false;
    }

    page = parseInt(page);

    // Update counter
    $ul.children(".itemTitle").children("span").html("Page: " + page + "/" + pageCount);

    // Update buttons
    $("#message-"+itemId+"-prev-page")
        .attr("data-page-link", (page > 1 ? (page-1) : -1))
        .css("color", (page < 2 ? "#ccc" : "#000"));

    $("#message-"+itemId+"-next-page")
        .attr("data-page-link", (page < pageCount ? (page+1) : -1))
        .css("color", (page >= pageCount ? "#ccc" : "#000"));

}

dhis2.db.renderLinkItem = function ($d, itemId, contents, title, baseUrl, urlSuffix) {
    var html = dhis2.db.linkItemHeaderHtml(itemId, title);

    $.each(contents, function (index, content) {
        if (null == content || undefined === content) {
            return true;
        }

        html +=
            "<li><a href='" + baseUrl + content.id + urlSuffix + "'>" + content.displayName + "</a>" +
            "<a class='removeItemLink' href='javascript:dhis2.db.removeItemContent( \"" + itemId + "\", \"" + content.id + "\" )' title='" + i18n_remove + "'>" +
            "<img src='../images/hide.png'></a></li>";
    });

    html += "</ul></div></li>";

    $d.append(html);
}

dhis2.db.renderLastDropItem = function ($d) {
    var html = "<li id='liDrop-dropLast' class='liDropItem'><div class='dropItem lastDropItem blankDropItem' id='dropLast' data-item='dropLast'></div></li>";

    $d.append(html);
}

dhis2.db.moveItem = function (id, destItemId, position) {
    var $targetDropLi = $("#liDrop-" + dhis2.db.currentItem);
    var $targetLi = $("#li-" + dhis2.db.currentItem);

    var $destLi = $("#liDrop-" + destItemId);
    $targetDropLi.insertBefore($destLi);
    $targetLi.insertBefore($destLi);

    var url = "../api/dashboards/" + dhis2.db.current() + "/items/" + id + "/position/" + position;

    $.post(url, function () {
    });
}

dhis2.db.addItemContent = function (type, id) {
    if (undefined !== dhis2.db.current()) {
        $.ajax({
            type: "post",
            url: "../api/dashboards/" + dhis2.db.current() + "/items/content",
            data: {
                type: type,
                id: id
            },
            success: function (data, textStatus, xhr) {

                var location = xhr.getResponseHeader("Location");

                if (location) {
                    $.getJSON("../api" + location, function (item) {
                        if (item && $.inArray(item.type, dhis2.db.visualItemTypes) != -1) {
                            $d = $("#contentList");
                            dhis2.db.renderItem($d, item, undefined, true, true);
                            dhis2.db.addDragDrop(item.id);
                        }
                        else {
                            dhis2.db.renderDashboard(dhis2.db.current());
                        }
                    });
                }
                else {
                    dhis2.db.renderDashboard(dhis2.db.current());
                }
            },
            error: function (xhr) {
                var error = JSON.parse(xhr.responseText);
                setHeaderDelayMessage(error.message);
            }
        });
    }
}

dhis2.db.removeItem = function (id) {
    $.ajax({
        type: "delete",
        url: "../api/dashboards/" + dhis2.db.current() + "/items/" + id,
        success: function () {
            dhis2.db.currentItem = undefined;
            $("#liDrop-" + id).remove();
            $("#li-" + id).remove();
        }
    });
}

dhis2.db.removeItemContent = function (itemId, contentId) {
    $.ajax({
        type: "delete",
        url: "../api/dashboards/" + dhis2.db.current() + "/items/" + itemId + "/content/" + contentId,
        success: function () {
            dhis2.db.renderDashboard(dhis2.db.current());
        }
    });
}

dhis2.db.addMessagesContent = function () {
    dhis2.db.addItemContent("MESSAGES", "");
    $("#manageDashboardForm").dialog("destroy");
}

dhis2.db.getIndex = function (itemId) {
    return parseInt($(".liDropItem").index($("#liDrop-" + itemId)));
}

dhis2.db.exploreChart = function (uid, interpretationId) {
    //window.location.href = "../dhis-web-visualizer/index.html?id=" + uid;
    window.location.href = "../dhis-web-visualizer/index.html?id=" + uid + ((interpretationId)?("&interpretationId=" + interpretationId):"");
}

dhis2.db.exploreEventChart = function (uid) {
    window.location.href = "../dhis-web-event-visualizer/index.html?id=" + uid;
}

dhis2.db.exploreMap = function (uid) {
    window.location.href = "../dhis-web-mapping/index.html?id=" + uid;
}

dhis2.db.exploreReportTable = function (uid, interpretationId) {
    //window.location.href = "../dhis-web-pivot/index.html?id=" + uid;
    window.location.href = "../dhis-web-pivot/index.html?id=" + uid + ((interpretationId)?("&interpretationId=" + interpretationId):"");
}

dhis2.db.exploreEventReport = function (uid) {
    window.location.href = "../dhis-web-event-reports/index.html?id=" + uid;
}

dhis2.db.exploreApp = function (appKey, itemId) {
    window.open(dhis2.db.apps[appKey].launchUrl + '?dashboardItemId=' + itemId, 'win' + dhis2.util.cacheBust());
}

dhis2.db.renderReportTable = function (tableId, itemId) {
    $.get("../api/reportTables/" + tableId + "/data.html", function (data) {
        $("#pivot-" + itemId).html(data);
    });
}

/**
 * Prepends or appends the given content to the given jQuery element.
 *
 * @param $el the jQuery element.
 * @param content the content.
 * @param prepend indicates whether to prepend or append.
 */
dhis2.db.preOrAppend = function ($el, content, prepend) {
    if (prepend && prepend == true) {
        $el.prepend(content);
    }
    else {
        $el.append(content);
    }
}

//------------------------------------------------------------------------------
// Search
//------------------------------------------------------------------------------

dhis2.db.searchMore = function (type) {
    dhis2.db.currentMaxType.push(type);
    dhis2.db.search();
}

dhis2.db.searchFewer = function (type) {
    dhis2.db.currentMaxType.splice($.inArray(type, dhis2.db.currentMaxType), 1);
    dhis2.db.search();
}

dhis2.db.search = function () {
    var query = $.trim($("#searchField").val());

    if (query.length == 0) {
        dhis2.db.hideSearch();
        return undefined;
    }

    var url = "../api/dashboards/q/" + query + dhis2.db.getMaxParams();

    var hits = $.ajax({
        url: url,
        dataType: "json",
        cache: false,
        success: function (data) {
            var $h = $("#hitDiv");
            dhis2.db.renderSearch(data, $h);
            dhis2.db.setHitLinks();
            $h.show();
            $("#searchField").focus();
        }
    });
}

dhis2.db.setHitLinks = function () {
    for (var i = 0; i < dhis2.db.currentMaxType.length; i++) {
        var type = dhis2.db.currentMaxType[i];

        $("#hitMore-" + type).hide();
        $("#hitFewer-" + type).show();
    }
}

dhis2.db.getMaxParams = function () {
    var params = "?";

    if (dhis2.db.currentMaxType.length) {
        for (var i = 0; i < dhis2.db.currentMaxType.length; i++) {
            params += "max=" + dhis2.db.currentMaxType[i] + "&";
        }
    }

    return params.substring(0, ( params.length - 1 ));
}

dhis2.db.renderSearch = function (data, $h) {
    $h.empty().append("<ul>");
    var canManage = $('#dashboardMenu').data('canManage');

    if (data.searchCount > 0) {
        if (data.userCount > 0) {
            $h.append($.tmpl(dhis2.db.tmpl.hitHeader, {
                "title": "Users",
                "type": "USERS",
                "i18n_see_more_hits": i18n_see_more_hits,
                "i18n_see_fewer_hits": i18n_see_fewer_hits
            }));

            for (var i in data.users) {
                var o = data.users[i];
                $h.append($.tmpl(dhis2.db.tmpl.hitItem, {
                    "canManage": canManage,
                    "link": "profile.action?id=" + o.id,
                    "img": "user_small",
                    "name": o.name,
                    "type": "USERS",
                    "id": o.id,
                    "i18n_add": i18n_add
                }));
            }
        }

        if (data.chartCount > 0) {
            $h.append($.tmpl(dhis2.db.tmpl.hitHeader, {
                "title": "Charts",
                "type": "CHART",
                "i18n_see_more_hits": i18n_see_more_hits,
                "i18n_see_fewer_hits": i18n_see_fewer_hits
            }));

            for (var i in data.charts) {
                var o = data.charts[i];
                $h.append($.tmpl(dhis2.db.tmpl.hitItem, {
                    "canManage": canManage,
                    "link": "../dhis-web-visualizer/index.html?id=" + o.id,
                    "img": "chart_small",
                    "name": o.name,
                    "type": "CHART",
                    "id": o.id,
                    "i18n_add": i18n_add
                }));
            }
        }

        if (data.eventChartCount > 0) {
            $h.append($.tmpl(dhis2.db.tmpl.hitHeader, {
                "title": "Event charts",
                "type": "EVENT_CHART",
                "i18n_see_more_hits": i18n_see_more_hits,
                "i18n_see_fewer_hits": i18n_see_fewer_hits
            }));

            for (var i in data.eventCharts) {
                var o = data.eventCharts[i];
                $h.append($.tmpl(dhis2.db.tmpl.hitItem, {
                    "canManage": canManage,
                    "link": "../dhis-web-event-visualizer/index.html?id=" + o.id,
                    "img": "chart_small",
                    "name": o.name,
                    "type": "EVENT_CHART",
                    "id": o.id,
                    "i18n_add": i18n_add
                }));
            }
        }

        if (data.mapCount > 0) {
            $h.append($.tmpl(dhis2.db.tmpl.hitHeader, {
                "title": "Maps",
                "type": "MAP",
                "i18n_see_more_hits": i18n_see_more_hits,
                "i18n_see_fewer_hits": i18n_see_fewer_hits
            }));

            for (var i in data.maps) {
                var o = data.maps[i];
                $h.append($.tmpl(dhis2.db.tmpl.hitItem, {
                    "canManage": canManage,
                    "link": "../dhis-web-mapping/index.html?id=" + o.id,
                    "img": "map_small",
                    "name": o.name,
                    "type": "MAP",
                    "id": o.id,
                    "i18n_add": i18n_add
                }));
            }
        }

        if (data.reportTableCount > 0) {
            $h.append($.tmpl(dhis2.db.tmpl.hitHeader, {
                "title": "Pivot tables",
                "type": "REPORT_TABLE",
                "i18n_see_more_hits": i18n_see_more_hits,
                "i18n_see_fewer_hits": i18n_see_fewer_hits
            }));

            for (var i in data.reportTables) {
                var o = data.reportTables[i];
                $h.append($.tmpl(dhis2.db.tmpl.hitItem, {
                    "canManage": canManage,
                    "link": "../dhis-web-pivot/index.html?id=" + o.id,
                    "img": "table_small",
                    "name": o.name,
                    "type": "REPORT_TABLE",
                    "id": o.id,
                    "i18n_add": i18n_add
                }));
            }
        }

        if (data.eventReportCount > 0) {
            $h.append($.tmpl(dhis2.db.tmpl.hitHeader, {
                "title": "Event reports",
                "type": "EVENT_REPORT",
                "i18n_see_more_hits": i18n_see_more_hits,
                "i18n_see_fewer_hits": i18n_see_fewer_hits
            }));

            for (var i in data.eventReports) {
                var o = data.eventReports[i];
                $h.append($.tmpl(dhis2.db.tmpl.hitItem, {
                    "canManage": canManage,
                    "link": "../dhis-web-event-reports/index.html?id=" + o.id,
                    "img": "table_small",
                    "name": o.name,
                    "type": "EVENT_REPORT",
                    "id": o.id,
                    "i18n_add": i18n_add
                }));
            }
        }

        if (data.reportCount > 0) {
            $h.append($.tmpl(dhis2.db.tmpl.hitHeader, {
                "title": "Standard reports",
                "type": "REPORTS",
                "i18n_see_more_hits": i18n_see_more_hits,
                "i18n_see_fewer_hits": i18n_see_fewer_hits
            }));

            for (var i in data.reports) {
                var o = data.reports[i];
                $h.append($.tmpl(dhis2.db.tmpl.hitItem, {
                    "canManage": canManage,
                    "link": "../dhis-web-reporting/getReportParams.action?uid=" + o.id,
                    "img": "standard_report_small",
                    "name": o.name,
                    "type": "REPORTS",
                    "id": o.id,
                    "i18n_add": i18n_add
                }));
            }
        }

        if (data.resourceCount > 0) {
            $h.append($.tmpl(dhis2.db.tmpl.hitHeader, {
                "title": "Resources",
                "type": "RESOURCES",
                "i18n_see_more_hits": i18n_see_more_hits,
                "i18n_see_fewer_hits": i18n_see_fewer_hits
            }));

            for (var i in data.resources) {
                var o = data.resources[i];
                $h.append($.tmpl(dhis2.db.tmpl.hitItem, {
                    "canManage": canManage,
                    "link": "../api/documents/" + o.id,
                    "img": "document_small",
                    "name": o.name,
                    "type": "RESOURCES",
                    "id": o.id,
                    "i18n_add": i18n_add
                }));
            }
        }

        if (data.appCount > 0) {
            $h.append($.tmpl(dhis2.db.tmpl.hitHeader, {
                "title": "Apps",
                "type": "APP",
                "i18n_see_more_hits": i18n_see_more_hits,
                "i18n_see_fewer_hits": i18n_see_fewer_hits
            }));

            for (var i in data.apps) {
                var o = data.apps[i];
                $h.append($.tmpl(dhis2.db.tmpl.appHitItem, {
                    "canManage": canManage,
                    "link": "../api/apps/" + o.key,
                    "img": "app_small",
                    "name": o.name,
                    "type": "APP",
                    "id": o.key,
                    "i18n_add": i18n_add
                }));
            }
        }
    }
    else {
        $h.append($.tmpl(dhis2.db.tmpl.hitHeader, {"title": "No results found"}));
    }
}

dhis2.db.hideSearch = function () {
    $("#hitDiv").hide();
}

dhis2.db.registerDashboardViewEvent = function () {
    var fav = dhis2.db.current() || "";

    $.post("../api/dataStatistics?eventType=DASHBOARD_VIEW&favorite=" + fav);
}

//------------------------------------------------------------------------------
// Share
//------------------------------------------------------------------------------

dhis2.db.viewShareForm = function (id, type, name) {
    dhis2.db.currentShareId = id;
    dhis2.db.currentShareType = type;

    var title = i18n_share_your_interpretation_of + " " + name;

    $("#shareForm").dialog({
        modal: true,
        width: 550,
        resizable: false,
        title: title
    });
}

dhis2.db.shareInterpretation = function () {
    var text = $("#interpretationArea").val();

    if (text.length && $.trim(text).length) {
        text = $.trim(text);

        var url = "../api/interpretations/" + dhis2.db.currentShareType + "/" + dhis2.db.currentShareId;

        // TODO url += ( ou && ou.length ) ? "?ou=" + ou : "";

        $.ajax({
            type: "post",
            url: url,
            contentType: "text/html",
            data: text,
            success: function () {
                $("#shareForm").dialog("close");
                $("#interpretationArea").val("");
                setHeaderDelayMessage(i18n_interpretation_was_shared);
                
                dhis2.db.renderDashboardListLoadFirst()
            },
            error: function(xhr, status, error) {
				setHeaderDelayMessage(xhr.responseText);
			}
        });
    }
}

dhis2.db.showShareHelp = function () {
    $("#shareHelpForm").dialog({
        modal: true,
        width: 380,
        resizable: false,
        title: "Share your data interpretations"
    });
}

//------------------------------------------------------------------------------
// Full size view
//------------------------------------------------------------------------------

dhis2.db.viewReportDialog = function (url, name) {
    var width = 820,
        height = 550,
        title = i18n_viewing + " " + name;

    $.get(url, function (data) {
        $("#reportDialogView").html(data);
    });

    $("#reportDialogView").dialog({
        autoOpen: true,
        modal: true,
        height: height + 65,
        width: width + 25,
        resizable: false,
        title: title
    });
}

dhis2.db.downloadImage = function () {
    var url = $("#chartImage").attr("src");

    if (url) {
        url = url + "&attachment=true";
        window.location.href = url;
    }
}

//------------------------------------------------------------------------------
// Org unit selector
//------------------------------------------------------------------------------

dhis2.db.viewOrgUnitSelector = function () {
    $("#orgUnitSelectorForm").dialog({
        autoOpen: true,
        modal: true,
        width: 370,
        height: 500,
        resizable: false,
        title: i18n_user_org_unit_filter
    });
}

dhis2.db.updateSelectedOrgUnits = function () {
    var ous = selectionTreeSelection.getSelectedUid();
    dhis2.db.currentUserOrgUnit = ous;
    dhis2.db.renderDashboard(dhis2.db.current());
    $("#orgUnitSelectorForm").dialog("destroy");
}

dhis2.db.clearSelectedOrgUnits = function () {
    dhis2.db.currentUserOrgUnit = [];
    selectionTree.clearSelectedOrganisationUnitsAndBuildTree();
    dhis2.db.renderDashboard(dhis2.db.current());
    $("#orgUnitSelectorForm").dialog("destroy");
}

//------------------------------------------------------------------------------
//Interpretation
//------------------------------------------------------------------------------

dhis2.db.initInterpretationPopupEvents = function(){
	var interpretationPopup = $('#interpretationPopup');
	
	// On mouse leave pop up wait 800 ms and hide panel. If mouse comes back into the panel, pop up will not be hidden 
	interpretationPopup.mouseleave(function(){
		dhis2.db.timer = setTimeout(function(){
			dhis2.db.hideInterpretationPopup();
	    }, 800);
	});
	
	// Clear timeout so that pop up is not hidden if user comes back to the pop up
	interpretationPopup.mouseover(function(){
	    clearTimeout(dhis2.db.timer);
	});
}

dhis2.db.verticalOffscreen = function(element, elementTop) {
	var pageTop = $(window).scrollTop();
	var pageBottom = pageTop + $(window).height();
  var elementBottom = elementTop + element.height();

  return !((pageTop < elementTop) && (pageBottom > elementBottom));
};
dhis2.db.horizontalOffscreen = function(element, elementLeft) {
	var elementWidth = element.width();
	var pageWidth = $(window).width();
	return !(elementLeft + elementWidth < pageWidth);
};

dhis2.db.hideInterpretationPopup = function(){
	$('#interpretationPopup').hide();
}

dhis2.db.showInterpretationPopup = function (event, id, type) {
	// Set api url path and explore function depending on dashboard item type
	var urlPath = '';
	var exploreFunction ='';
	if (type == 'CHART'){
		urlPath = 'charts';
		exploreFunction = 'exploreChart'
	}
	else if (type == 'EVENT_CHART'){
		urlPath = 'eventCharts';
		exploreFunction = 'exploreEventChart'
	}
	else if (type == 'MAP'){
		urlPath = 'maps';
		exploreFunction = 'exploreMap'
	}
	else if (type == 'REPORT_TABLE'){
		urlPath = 'reportTables';
		exploreFunction = 'exploreReportTable'
	}
	else if (type == 'EVENT_REPORT'){
		urlPath = 'eventReports';
		exploreFunction = 'exploreEventReport'
	}
	
	$.getJSON("../api/" + urlPath + "/" + id + ".json?fields=id,displayDescription,interpretations[id, text,lastUpdated,user[displayName],comments,likes]&" + dhis2.util.cacheBust(), function (data) {
		var interpretationPopup = $('#interpretationPopup');
		
		// Remove any previous content
		interpretationPopup.empty();
		
		//Add description panel
		var favoriteDescriptionContent = $.tmpl(dhis2.db.tmpl.favoriteDescription, {
		    "description": data.displayDescription
		});
		dhis2.db.preOrAppend($('#interpretationPopup'), favoriteDescriptionContent, false);
		
		// Add Panel per interpretation
		for (var i = 0; i<data.interpretations.length; i++){
			var interpretation = data.interpretations[i];
			var interpretationContent = $.tmpl(dhis2.db.tmpl.interpretation, {
				"favoriteId": id,
                "interpretationId": interpretation.id,
			    "userDisplayName": interpretation.user.displayName,
			    "lastUpdated": new Date(interpretation.lastUpdated),
			    "text": interpretation.text,
			    "numberLikes": interpretation.likes,
			    "numberComments": interpretation.comments.length,
			    "exploreFunction": exploreFunction
			    
			});
			dhis2.db.preOrAppend(interpretationPopup, interpretationContent, false);
		}

		// Check interpretation pop up is on screen
		interpretationPopup.css('top',event.pageY + 'px').css('left',event.pageX + 'px');
		if (dhis2.db.verticalOffscreen(interpretationPopup, event.pageY)){
			interpretationPopup.css('top',event.pageY - interpretationPopup.height() + 'px');
		}
		if (dhis2.db.horizontalOffscreen(interpretationPopup, event.pageX)){
			interpretationPopup.css('left',event.pageX - interpretationPopup.width() + 'px');
		}
		
		// If user does not enter pop up hide dialog after 4s
		dhis2.db.timer = setTimeout(function(){
			dhis2.db.hideInterpretationPopup();
		}, 4000);
		
		// Show interpretation pop up 
		interpretationPopup.show();
	});
}
