package net.osmand.plus.views.controls.maphudbuttons;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.fragments.profileappearance.ProfileAppearanceFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.ProfileAppearanceButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;

public class ProfileAppearanceButton extends MapButton {

	private final ProfileAppearanceButtonState buttonState;

	public ProfileAppearanceButton(@NonNull Context context) {
		this(context, null);
	}

	public ProfileAppearanceButton(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ProfileAppearanceButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		buttonState = app.getMapButtonsHelper().getProfileAppearanceButtonState();

		setOnClickListener(v -> {
			if (AndroidUtils.isActivityNotDestroyed(mapActivity)) {
				ProfileAppearanceFragment.showInstance(mapActivity, settings.getApplicationMode().getStringKey(), false);
			}
		});
	}

	@Nullable
	@Override
	public MapButtonState getButtonState() {
		return buttonState;
	}

	@Override
	protected void updateColors(boolean nightMode) {
		setIconColor(settings.getApplicationMode().getProfileColor(nightMode));
		setBackgroundColors(ColorUtilities.getMapButtonBackgroundColor(getContext(), nightMode),
				ColorUtilities.getMapButtonBackgroundPressedColor(getContext(), nightMode));
	}

	@Override
	protected boolean shouldShow() {
		return !routeDialogOpened && visibilityHelper.shouldShowTopButtons();
	}
}
