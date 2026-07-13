package net.osmand.plus.profiles.data;

import android.content.Context;

import androidx.core.os.ConfigurationCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfileDataUtils {

	public static List<ProfileDataObject> getDataObjects(OsmandApplication app,
	                                                     List<ApplicationMode> appModes) {
		List<ProfileDataObject> profiles = new ArrayList<>();
		for (ApplicationMode mode : appModes) {
			String description = mode.getDescription();
			if (Algorithms.isEmpty(description)) {
				description = getAppModeDescription(app, mode);
			}
			profiles.add(new ProfileDataObject(mode.toHumanString(), description,
					mode.getStringKey(), mode.getIconRes(), false, mode.getProfileColor(false), mode.getProfileColor(true)));
		}
		return profiles;
	}

	public static String getAppModeDescription(Context ctx, ApplicationMode mode) {
		if (mode.isCustomProfile()) {
			return ctx.getString(R.string.profile_type_user_string);
		}
		Locale locale = ConfigurationCompat.getLocales(ctx.getResources().getConfiguration()).get(0);
		String lang = locale != null ? locale.getLanguage() : "en";
		boolean isEnOrEs = lang.equals("en") || lang.equals("es");
		return isEnOrEs ? ctx.getString(R.string.profile_type_osmand_string) : "OsmAnd profile";
	}


}
