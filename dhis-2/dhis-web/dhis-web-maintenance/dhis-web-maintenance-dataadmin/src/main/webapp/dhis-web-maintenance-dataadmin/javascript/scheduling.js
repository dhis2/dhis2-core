$(document).ready(function () {

    if ($('#isRunning').val() == 'true') 
    {
        $('.scheduling').attr('disabled', 'disabled');
        $('.scheduling input').attr('disabled', 'disabled');
        $('.scheduling select').attr('disabled', 'disabled');

    }
    else
    {
        $('.scheduling').removeAttr('disabled');
        $('.scheduling input').removeAttr('disabled');
        $('.scheduling select').removeAttr('disabled');
    }

    if ($('#dataMartStrategy').val() == 'disabled')
    {
        $('.dataMart').attr('disabled', 'disabled');
    }

    var displayOptionsFor = 
    {
        daily: function ()
        {
            updateSyncHeader("Daily");
        },
        weekly: function ()
        {
            updateSyncHeader("Weekly");
            $(".weekPickerContainer").show();
            $("#weekPicker").show();
        },

        monthly: function () 
        {
            updateSyncHeader("Monthly");
            $(".weekPickerContainer").show();
            $('#weekPeriod').show();
            $('#weekPicker').show();
        },

        yearly: function () 
        {
            updateSyncHeader("Yearly");
            $(".yearPickerContainer").show();
        },

        default: function ()
        {
            $(".timePickerContainer").show();
            $(".syncStartButton").show();
        }
    };

    var updateSyncHeader = function (header) 
    {
        $('#addSchedulerLabel').html(header);
    };

    var defaultScheduler = function () 
    {
        ($("#metadataSyncStrategy").val() == "enabled") ? $("#scheduler").show() : $("#scheduler").hide();
        $(".syncStartButton").show();
    };

    var getSchedules = function (cronExpression)
    {
        var cronSchedules = {};
        var cronExpressionSignature = ["seconds", "minutes", "hours", "day", "month", "week"];
        cronExpression.split(" ").forEach(function (cronValue, index)
        {
            if (cronValue.indexOf("-") >= 0) 
            {
                cronSchedules.weekPeriod = cronValue;
            } 
            else 
            {
                cronSchedules[cronExpressionSignature[index]] = cronValue;
            }
        });
        return cronSchedules;
    };

    var setScheduler = function (cronExpression)
    {
        var schedules = getSchedules(cronExpression);
        var setTime = function () {
            var time = getHour(parseInt(schedules.hours)) + ":" + getMinutes(parseInt(schedules.minutes));
            $("#timePicker").val(time);
            displayOptionsFor.default();
        };
        var setter = 
        {
            daily: function () {
                if (schedules.day == "*" && (schedules.day == schedules.month) && schedules.week == "?")
                {
                    $("#daily").attr("checked", true);
                    displayOptionsFor["daily"]();
                    setTime();
                    return true;
                }
            },
            weekly: function () 
            {
                if (schedules.day == "*" && (schedules.day == schedules.month)) 
                {
                    $("#weekly").attr("checked", true);
                    displayOptionsFor["weekly"]();
                    $("#weekPicker").val(schedules.week);
                    setTime();
                    return true;
                }
            },

            monthly: function () 
            {
                if (schedules.month == "*" && schedules.weekPeriod) {
                    $("#monthly").attr("checked", true);
                    displayOptionsFor["monthly"]();
                    $("#weekPicker").val(schedules.week);
                    $("#weekPeriodNumber").val(schedules.weekPeriod);
                    setTime();
                    return true;
                }
            },

            yearly: function ()
            {
                if (schedules.week == "?") {
                    $("#yearly").attr("checked", true);
                    displayOptionsFor["yearly"]();
                    $("#monthPicker").val(schedules.month);
                    $("#dayPicker").val(schedules.day);
                    setTime();
                    return true;
                }
            }
        };
        var methods = Object.keys(setter);
        for (var count = 0; count < methods.length; count++) 
        {
            if (setter[methods[count]]() == true)
            {
                return;
            }
        }
    };

    var hideOptions = function ()
    {
        $("#weekPicker").hide();
        $('#weekPeriod').hide();
        $(".yearPickerContainer").hide();
        $(".timePickerContainer").hide();
        $(".weekPickerContainer").hide();
        $(".syncStartButton").hide();
        generate.month("monthPicker");
        generate.days("dayPicker", 1);
        generate.time("timePicker");
    };

    hideOptions();
    defaultScheduler();
    setScheduler($("#metadataSyncCron").val());

    $("#monthPicker").unbind("change").change(function (e) 
    {
        e.stopPropagation();
        var monthIndex = $(this).val();
        generate.days("dayPicker", monthIndex);

    });

    $(".radio").unbind("change").change(function (e) 
    {
        e.stopPropagation();
        hideOptions();
        var schedulerOption = $(this).val();
        displayOptionsFor[schedulerOption]();
        displayOptionsFor.default();
    });

    $("#metadataSyncStrategy").unbind("change").change(function (e)
    {
        defaultScheduler();
    });

    $("#submitSyncSchedule").unbind("click").click(function (e)
    {
        e.stopPropagation();
        var button = this;
        $(button).attr("disabled", "disabled");
        $.post('executeMetaDataSyncTask.action', {
            executeNow: true,
            taskKey: "metadataSyncTask"
        }, function (json) {
            $(button).removeAttr("disabled");
        });
    });

});

var getCronExpression = function ()
{
    var week = $("#weekPicker").val();
    var time = $("#timePicker").val().split(":");
    var period = $("#weekPeriodNumber").val();
    var month = $("#monthPicker").val();
    var day = $("#dayPicker").val();
    var selection = $('input[name=datepick]:checked', ".radioButtonGroup").val();
    if(!selection) return false;

    var hours = parseInt(time[0]);
    var minutes = parseInt(time[1]);
    var SECONDS = "0";
    var compile = 
    {
        daily: function (month, day, week, period, hours, minutes)
        {
            return SECONDS.concat(" ",minutes," ",hours, " *", " * " ,"?");
        },
        monthly: function (month, day, week, period, hours, minutes)
        {
            return SECONDS.concat(" ",minutes, " ", hours," ",period," * ",week);
        },
        yearly: function (month, day, week, period, hours, minutes)
        {
            return SECONDS.concat(" ",minutes," ",hours," ",day," ",month," ?");
        },
        weekly: function (month, day, week, period, hours, minutes)
        {
            return SECONDS.concat(" ",minutes," ",hours," *"," *"," ",week);
        }
    };
    return compile[selection](month, day, week, period, hours, minutes);
};

function submitSchedulingForm() 
{
    
    if($("#metadataSyncStrategy").val() == "enabled")
    {
        var cron = getCronExpression();
        if(cron)
        {
            $('#metadataSyncCron').val(cron);
            $('.scheduling').removeAttr('disabled');
            $('#schedulingForm').submit();
        }
        else 
        {
            alert( metadata_sync_scheduler_alert );
        }
    }
    else
    {
        $('.scheduling').removeAttr('disabled');
        $('#schedulingForm').submit();
    }
}

function toggleMoreOptions()
{
    $("#moreOptionsLink").toggle();
    $("#moreOptionsDiv").toggle();
}

function toggleDataMart() 
{
    
    if ($('#dataMartStrategy').val() == 'never')
    {
        $('.dataMart').attr('disabled', 'disabled');
    }
    else
    {
        $('.dataMart').removeAttr('disabled');
    }
}
