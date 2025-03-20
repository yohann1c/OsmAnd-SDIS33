package net.osmand.plus.settings.backend;

import android.provider.Settings;
import android.content.Context;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;

public class DeviceUtils {
    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
