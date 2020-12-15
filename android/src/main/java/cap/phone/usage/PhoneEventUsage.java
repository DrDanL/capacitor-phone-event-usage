package cap.phone.usage;

import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.util.Log;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

@NativePlugin
public class PhoneEventUsage extends Plugin {

    /**
     * Sample method
     */
    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", value);
        call.success(ret);
    }

    /**
     * Enable phone usage app tracking by directing user to give permission
     */
    @PluginMethod()
    public void enable(PluginCall call) {
        Context context = this.getContext();
        context.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));

        call.resolve();
    }

    /**
     * Check android permission status
     * @param call - getPermissionStatus: boolean
     */
    @PluginMethod()
    public void getPermissionStatus(PluginCall call) {
        JSObject ret = new JSObject();
        Context context = this.getContext();
        AppOpsManager appOps = (AppOpsManager) context.getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.getApplicationContext().getPackageName());

        boolean result_return = false;

        if (mode == AppOpsManager.MODE_DEFAULT) {
            Log.d("Test Mode Default", Boolean.toString((context.getApplicationContext().checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED)));
            result_return = (context.getApplicationContext().checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            Log.d("Test Mode Allowed", Boolean.toString((mode == MODE_ALLOWED)));
            result_return = (mode == MODE_ALLOWED);
        }

        ret.put("getPermissionStatus", result_return);
        call.success(ret);
    }

    /**
     * Get current app usage
     * @param call - getAppUsage: array
     */
    @PluginMethod()
    public void getAppUsage(PluginCall call) {
        JSObject ret = new JSObject();
        Context context = this.getContext();

        Integer days = call.getInt("duration", 2);
        Log.d("Number of days requested", Integer.toString(days));

        //Create usage manager
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(context.USAGE_STATS_SERVICE);
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  System.currentTimeMillis() - 1000 * 3600 * 24 * days,  System.currentTimeMillis());

        // Group the usageStats by application and sort them by total time in foreground
        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getTotalTimeInForeground(), usageStats);
            }

            List<UsageStats> usageStatsList = mySortedMap.values().stream().filter(this::isAppInfoAvailable).collect(Collectors.toList());

            // get total time of apps usage to calculate the usagePercentage for each app
            long totalTime = usageStatsList.stream().map(UsageStats::getTotalTimeInForeground).mapToLong(Long::longValue).sum();

            //fill the appsList
            JSObject appsList = new JSObject();

            for (UsageStats usageStats : usageStatsList) {
                try {
                    String packageName = usageStats.getPackageName();
                    ApplicationInfo ai = context.getApplicationContext().getPackageManager().getApplicationInfo(packageName, 0);
                    Drawable icon = context.getApplicationContext().getPackageManager().getApplicationIcon(ai);
                    String appName = context.getApplicationContext().getPackageManager().getApplicationLabel(ai).toString();
                    int usagePercentage = (int) (usageStats.getTotalTimeInForeground() * 100 / totalTime);

                    //Pull app information and store in JSON
                    JSObject app_element = new JSObject();
                    app_element.put("packageName", packageName);
                    app_element.put("appName", appName);
                    app_element.put("usageDuration", getDurationBreakdown(usageStats.getTotalTimeInForeground()));
                    app_element.put("lastTimeForegroundServiceUsed", usageStats.getLastTimeForegroundServiceUsed());
                    app_element.put("lastTimeUsed", usageStats.getLastTimeUsed());
                    app_element.put("lastTimeVisible", usageStats.getLastTimeVisible());
                    app_element.put("totalTimeForegroundServiceUsed", getDurationBreakdown(usageStats.getTotalTimeForegroundServiceUsed()));
                    app_element.put("totalTimeInForeground", getDurationBreakdown(usageStats.getTotalTimeInForeground()));
                    app_element.put("totalTimeVisible", getDurationBreakdown(usageStats.getTotalTimeVisible()));

                    appsList.put(packageName, app_element);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }

            ret.put("getAppUsage", appsList);
        }

        call.success(ret);
    }

    /**
     * helper method to get string in format hh:mm:ss from miliseconds
     *
     * @param millis (application time in foreground)
     * @return string in format hh:mm:ss from miliseconds
     */
    private String getDurationBreakdown(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        return (hours + " h " +  minutes + " m " + seconds + " s");
    }

    /**
     * check if the application info is still existing in the device / otherwise it's not possible to show app detail
     * @return true if application info is available
     */
    private boolean isAppInfoAvailable(UsageStats usageStats) {
        try {
            Context context = this.getContext();
            context.getApplicationContext().getPackageManager().getApplicationInfo(usageStats.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
}
