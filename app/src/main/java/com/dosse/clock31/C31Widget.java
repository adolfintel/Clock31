package com.dosse.clock31;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.text.format.DateFormat;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link C31WidgetConfigureActivity C31WidgetConfigureActivity}
 */
public class C31Widget extends AppWidgetProvider {

    private static final String TAG="C31WidgetProvider";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.v(TAG,"Intent received: "+intent.toString());
        onUpdate(context,AppWidgetManager.getInstance(context),AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, C31Widget.class)));
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        Log.v(TAG,"updateAppWidget: "+appWidgetId);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.c31_widget);
        Bundle options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId);
        boolean hideAlarm=false, hideCalendar=false;
        if(options!=null) {
            int h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            Log.v(TAG, "Resize: "+w + "x" + h);
            float clockFontScale = Math.max(0.4f, Math.min(1, Math.min(h, w) / 275f));
            float dateFontScale = Math.max(0.7f, Math.min(1, Math.min(h, w) / 275f));
            if (w < 220) {
                hideAlarm = true;
                dateFontScale = Math.min(1, dateFontScale * 1.25f);
            }
            if (h < 80) {
                hideCalendar = true;
                clockFontScale = Math.min(1, clockFontScale * 1.5f);
                dateFontScale = Math.min(1, dateFontScale * 1.25f);
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                clockFontScale = clockFontScale * 0.8f;
            }
            if (DateFormat.is24HourFormat(context)) {
                views.setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_DIP, 80 * clockFontScale);
            } else {
                views.setTextViewTextSize(R.id.clock, TypedValue.COMPLEX_UNIT_DIP, 60 * clockFontScale);
            }
            views.setTextViewTextSize(R.id.date, TypedValue.COMPLEX_UNIT_DIP, 18 * dateFontScale);
            views.setTextViewTextSize(R.id.alarm, TypedValue.COMPLEX_UNIT_DIP, 18 * dateFontScale);
        }
        PackageManager pm=context.getPackageManager();
        PendingIntent openClockApp=PendingIntent.getActivity(context, 0, pm.getLaunchIntentForPackage("com.android.deskclock"), PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M?PendingIntent.FLAG_IMMUTABLE:0));
        views.setOnClickPendingIntent(R.id.clock,openClockApp);
        PendingIntent openCalendarApp=PendingIntent.getActivity(context,0,new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR), PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M?PendingIntent.FLAG_IMMUTABLE:0));
        views.setOnClickPendingIntent(R.id.calendar_icon,openCalendarApp);
        if(!hideAlarm){
            AlarmManager am =(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            AlarmManager.AlarmClockInfo alarmClock = am.getNextAlarmClock();
            if (alarmClock != null) {
                views.setViewVisibility(R.id.alarm, View.VISIBLE);
                CharSequence alarm;
                if(DateFormat.is24HourFormat(context)){
                    alarm=DateFormat.format("⏰ E HH:mm",alarmClock.getTriggerTime());
                }else{
                    alarm=DateFormat.format("⏰ E hh:mma",alarmClock.getTriggerTime());
                }
                views.setTextViewText(R.id.alarm,alarm);
                views.setOnClickPendingIntent(R.id.alarm,openClockApp);
            }else {
                views.setViewVisibility(R.id.alarm, View.GONE);
            }
        }else{
            views.setViewVisibility(R.id.alarm, View.GONE);
        }
        if(!hideCalendar) {
            views.setViewVisibility(R.id.calendar_container, View.VISIBLE);
            views.setRemoteAdapter(R.id.calendar, new Intent(context, CalendarRemoteViewsService.class));
            Intent eventClickTemplate = new Intent(Intent.ACTION_VIEW);
            PendingIntent eventClickPendingIntent = PendingIntent.getActivity(context, 0, eventClickTemplate, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0));
            views.setPendingIntentTemplate(R.id.calendar, eventClickPendingIntent);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.calendar);
        }else{
            views.setViewVisibility(R.id.calendar_container, View.GONE);
        }
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    public static final String ACTION_REFRESH="com.dosse.clock31.ACTION_REFRESH";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.v(TAG,"onUpdate");
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        onUpdate(context,AppWidgetManager.getInstance(context),AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, C31Widget.class)));
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

}