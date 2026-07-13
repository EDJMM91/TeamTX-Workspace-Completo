package net.osmand.plus.views.mapwidgets.configure.buttons;

import static net.osmand.plus.quickaction.ButtonAppearanceParams.SMALL_SIZE_DP;
import static net.osmand.shared.grid.ButtonPositionSize.POS_LEFT;
import static net.osmand.shared.grid.ButtonPositionSize.POS_TOP;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.shared.grid.ButtonPositionSize;

public class ProfileAppearanceButtonState extends MapButtonState {

	private final CommonPreference<Boolean> visibilityPref;

	public ProfileAppearanceButtonState(@NonNull OsmandApplication app) {
		super(app, "profile_appearance_hud_id");
		this.visibilityPref = addPreference(settings.registerBooleanPreference(id + "_state", true)).makeProfile();
	}

	@NonNull
	@Override
	public String getName() {
		return app.getString(R.string.profile_appearance);
	}

	@NonNull
	@Override
	public String getDescription() {
		return app.getString(R.string.profile_appearance);
	}

	@Override
	public int getDefaultLayoutId() {
		return R.layout.profile_appearance_button;
	}

	@Override
	public int getDefaultSize() {
		return SMALL_SIZE_DP;
	}

	@Override
	public boolean isEnabled() {
		return visibilityPref.get();
	}

	@NonNull
	@Override
	public CommonPreference<Boolean> getVisibilityPref() {
		return visibilityPref;
	}

	@NonNull
	@Override
	public String getDefaultIconName(@Nullable Boolean nightMode) {
		ApplicationMode appMode = settings.getApplicationMode();
		return appMode.getIconName();
	}

	@Override
	protected void updatePosition(@NonNull ButtonPositionSize position) {
		super.updatePosition(position);
		if (!portrait) {
			position.setMoveHorizontal();
		}
	}

	@NonNull
	@Override
	protected ButtonPositionSize setupButtonPosition(@NonNull ButtonPositionSize position) {
		return setupButtonPosition(position, POS_LEFT, POS_TOP, false, true);
	}
}
