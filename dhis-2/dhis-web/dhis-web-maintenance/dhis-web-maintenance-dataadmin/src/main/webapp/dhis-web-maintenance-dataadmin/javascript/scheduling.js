$(document).ready(function () {

    if($('#currentRunningTaskStatus').val())
    {
        setHeaderDelayMessage( $('#currentRunningTaskStatus').val() );
    }

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
        daily: function (parentClass)
        {
            updateSyncHeader("Daily", parentClass);
            $(parentClass+' .timePickerContainer').show();
        },
        weekly: function (parentClass)
        {
            updateSyncHeader("Weekly", parentClass);
            $(parentClass+' .weekPickerContainer').show();
            $(parentClass+' #weekPicker').show();
            $(parentClass+' .timePickerContainer').show();
        },

        monthly: function (parentClass)
        {
            updateSyncHeader("Monthly", parentClass);
            $(parentClass+' .weekPickerContainer').show();
            $(parentClass+' #weekPeriod').show();
            $(parentClass+' #weekPicker').show();
            $(parentClass+' .timePickerContainer').show();
        },

        yearly: function (parentClass)
        {
            updateSyncHeader("Yearly", parentClass);
            $(parentClass+' .yearPickerContainer').show();
            $(parentClass+' .timePickerContainer').show();
        },

        minute: function (parentClass)
        {
            updateSyncHeader("", parentClass);
        },

        hourly: function (parentClass)
        {
            updateSyncHeader("", parentClass);
        }

    };

    var updateSyncHeader = function (header, parentClass)
    {
        $(parentClass+' #addSchedulerLabel').html(header);
    };

    var defaultScheduler = function () 
    {
        if($("#metadataSyncStrategy").val() == "enabled")
        {
            $(".metadataSyncScheduler").show();
            setScheduler($("#metadataSyncCron").val(),'.metadataSyncScheduler');
        }
        else
        {
            $(".metadataSyncScheduler").hide();
        }

        if($("#dataSynchStrategy").val() == "enabled")
        {
            $(".dataSyncScheduler").show();
            setScheduler($("#dataSyncCron").val(),'.dataSyncScheduler');
        }
        else
        {
            $(".dataSyncScheduler").hide();
        }

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

    var setScheduler = function (cronExpression, parentClass)
    {
        var schedules = getSchedules(cronExpression);
        var setTime = function () {
            var time = getHour(parseInt(schedules.hours)) + ":" + getMinutes(parseInt(schedules.minutes));
            $(parentClass+' #timePicker').val(time);
        };
        var setter = 
        {
            minute: function ()
            {
                if (schedules.day == "*" && schedules.minutes == "0/1" && (schedules.day == schedules.month) && schedules.week == "?")
                {
                    $(parentClass + ' #minute').attr("checked", true);
                    return true;
                }
            },

            hourly: function ()
            {
                if (schedules.day == "*" && schedules.hours == "*" && schedules.minutes == "0" && schedules.seconds == "0" && (schedules.day == schedules.month) && schedules.week == "?")
                {
                    $(parentClass + ' #hourly').attr("checked", true);
                    return true;
                }
            },

            daily: function () {
                if (schedules.day == "*" && (schedules.day == schedules.month) && schedules.week == "?")
                {
                    $(parentClass+' #daily').attr("checked", true);
                    displayOptionsFor["daily"](parentClass);
                    setTime();
                    return true;
                }
            },
            weekly: function () 
            {
                if (schedules.day == "*" && (schedules.day == schedules.month)) 
                {
                    $(parentClass+' #weekly').attr("checked", true);
                    displayOptionsFor["weekly"](parentClass);
                    $(parentClass+' #weekPicker').val(schedules.week);
                    setTime();
                    return true;
                }
            },

            monthly: function () 
            {
                if (schedules.month == "*" && schedules.weekPeriod) {
                    $(parentClass+" #monthly").attr("checked", true);
                    displayOptionsFor["monthly"](parentClass);
                    $(parentClass+' #weekPicker').val(schedules.week);
                    $(parentClass+" #weekPeriodNumber").val(schedules.weekPeriod);
                    setTime();
                    return true;
                }
            },

            yearly: function ()
            {
                if (schedules.week == "?") {
                    $(parentClass+' #yearly').attr("checked", true);
                    displayOptionsFor["yearly"](parentClass);
                    $(parentClass+' #monthPicker').val(schedules.month);
                    $(parentClass+' #dayPicker').val(schedules.day);
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

    var hideOptions = function (parentClass)
    {
        $(parentClass+' .weekPickerContainer').hide();
        $(parentClass+' #weekPicker').hide();
        $(parentClass+' #weekPeriod').hide();

        $(parentClass+' .yearPickerContainer').hide();

        $(parentClass+' .timePickerContainer').hide();
        $(parentClass+' .syncStartButton').hide();

        generate.month("monthPicker", parentClass);
        generate.days("dayPicker", 1, parentClass);
        generate.time("timePicker", parentClass);
    };

    hideOptions('.metadataSyncScheduler');
    hideOptions('.dataSyncScheduler');
    defaultScheduler();

    $(".monthPicker").unbind("change").change(function (e)
    {
        e.stopPropagation();
        var monthIndex = $(this).val();
        var parent = '.' + $(this).parent().parent().parent().attr('id');
        generate.days("dayPicker", monthIndex, parent);
    });

    $(".radio").unbind("change").change(function (e)
    {
        e.stopPropagation();
        var schedulerOption = $(this).val();
        var parent = '.' + $(this).parent().parent().parent().attr('id');
        hideOptions(parent);
        displayOptionsFor[schedulerOption](parent);
    });

    $("#metadataSyncStrategy").unbind("change").change(function (e)
    {
        defaultScheduler();
    });

    $("#dataSynchStrategy").unbind("change").change(function (e)
    {
        defaultScheduler();
    });

    $("#submitSyncNow").unbind("click").click(function (e)
    {
        e.stopPropagation();
        $("#metadataSyncNowForm").submit();
    });

});

var getCronExpression = function (parentClass)
{
    var week = $(parentClass+" #weekPicker").val();
    var time = $(parentClass+" #timePicker").val().split(":");
    var period = $(parentClass+" #weekPeriodNumber").val();
    var month = $(parentClass+" #monthPicker").val();
    var day = $(parentClass+" #dayPicker").val();
    var selection = $('input[class=radio]:checked', parentClass+" .radioButtonGroup").val();
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
        },
        minute: function (month, day, week, period, hours, minutes)
        {
            return '0 0/1 * * * ?';
        },
        hourly: function (month, day, week, period, hours, minutes)
        {
            return '0 0 * * * ?';
        }

    };
    return compile[selection](month, day, week, period, hours, minutes);
};

function setScheduledCron(scheduler, cron)
{
    var syncCron = getCronExpression('.' + scheduler);
    if(syncCron)
    {
        $('#' + cron).val(syncCron);
        $('.scheduling').removeAttr('disabled');
        $('#schedulingForm').submit();
    }
    else
    {
        setHeaderDelayMessage(sync_scheduler_alert);
    }
}

function submitSchedulingForm()
{
    var metadataSyncStrategy = $("#metadataSyncStrategy").val();
    var dataSyncStrategy = $("#dataSynchStrategy").val();
    if(metadataSyncStrategy == "enabled" && dataSyncStrategy == "enabled")
    {
        var metadataSyncCron = getCronExpression('.metadataSyncScheduler');
        var dataSyncCron = getCronExpression('.dataSyncScheduler');

        if(metadataSyncCron && dataSyncCron)
        {
            $('#metadataSyncCron').val(metadataSyncCron);
            $('#dataSyncCron').val(dataSyncCron);
            $('.scheduling').removeAttr('disabled');
            $('#schedulingForm').submit();
        }
        else
        {
            setHeaderDelayMessage(sync_scheduler_alert);
        }

    }
    else if(metadataSyncStrategy == "enabled")
    {
        setScheduledCron('metadataSyncScheduler', 'metadataSyncCron');
    }

    else if(dataSyncStrategy == "enabled")
    {
        setScheduledCron('dataSyncScheduler', 'dataSyncCron');
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
