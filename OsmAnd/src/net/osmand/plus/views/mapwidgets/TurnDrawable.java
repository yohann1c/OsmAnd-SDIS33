package net.osmand.plus.views.mapwidgets;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.TurnPathHelper;
import net.osmand.router.TurnType;

import org.apache.commons.logging.Log;

public class TurnDrawable extends Drawable {

	protected static final Log log = PlatformUtil.getLog(TurnDrawable.class);

	protected Paint paintTurnOutlayStroke;
	protected Paint paintTurnOutlayFill;
	protected Paint paintRouteDirection;
	protected Path pathForTurn = new Path();
	protected Path pathForTurnOutlay = new Path();
	private final Path originalPathForTurn = new Path();
	private final Path originalPathForTurnOutlay = new Path();
	protected TurnType turnType;
	protected int turnImminent;
	protected boolean deviatedFromRoute;
	private final Context ctx;
	private final boolean mini;
	private final PointF centerText;
	private TextPaint textPaint;
	private Boolean nightMode;
	private int clr;

	public TurnDrawable(Context ctx, boolean mini) {
		this.ctx = ctx;
		this.mini = mini;
		centerText = new PointF();
		paintTurnOutlayStroke = new Paint();
		paintTurnOutlayStroke.setStyle(Paint.Style.STROKE);
		paintTurnOutlayStroke.setColor(Color.BLACK);
		paintTurnOutlayStroke.setAntiAlias(true);
		paintTurnOutlayStroke.setStrokeWidth(2.5f);

		int fillColor = AndroidUtils.getColorFromAttr(ctx, R.attr.nav_arrow_circle_color);
		paintTurnOutlayFill = new Paint();
		paintTurnOutlayFill.setStyle(Paint.Style.FILL);
		paintTurnOutlayFill.setColor(fillColor);
		paintTurnOutlayFill.setAntiAlias(true);

		paintRouteDirection = new Paint();
		paintRouteDirection.setStyle(Paint.Style.FILL);
		paintRouteDirection.setAntiAlias(true);
		setColor(R.color.nav_arrow);
	}

	public void setColor(@ColorRes int clr) {
		if (clr != this.clr) {
			this.clr = clr;
			paintRouteDirection.setColor(ContextCompat.getColor(ctx, clr));
		}
	}

	@Override
	protected void onBoundsChange(Rect bounds) {
		Matrix m = new Matrix();
		float scaleX = bounds.width() / 72f;
		float scaleY = bounds.height() / 72f;
		m.setScale(scaleX, scaleY);

		pathForTurn.set(originalPathForTurn);
		pathForTurn.transform(m);

		pathForTurnOutlay.set(originalPathForTurnOutlay);
		pathForTurnOutlay.transform(m);

		centerText.x = scaleX * centerText.x;
		centerText.y = scaleY * centerText.y;
	}

	public int getTurnImminent() {
		return turnImminent;
	}

	public boolean isDeviatedFromRoute() {
		return deviatedFromRoute;
	}

	public void setTurnImminent(int turnImminent, boolean deviatedFromRoute) {
		//if user deviates from route that we should draw grey arrow
		this.turnImminent = turnImminent;
		this.deviatedFromRoute = deviatedFromRoute;
		if (deviatedFromRoute) {
			paintRouteDirection.setColor(ctx.getColor(R.color.nav_arrow_distant));
		} else if (turnImminent > 0) {
			paintRouteDirection.setColor(ctx.getColor(R.color.nav_arrow));
		} else if (turnImminent == 0) {
			paintRouteDirection.setColor(ctx.getColor(R.color.nav_arrow_imminent));
		} else {
			paintRouteDirection.setColor(ctx.getColor(R.color.nav_arrow_distant));
		}
		invalidateSelf();
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		/// small indent
		// canvas.translate(0, 3 * scaleCoefficient);
		canvas.drawPath(pathForTurnOutlay, paintTurnOutlayFill);
		canvas.drawPath(pathForTurnOutlay, paintTurnOutlayStroke);
		canvas.drawPath(pathForTurn, paintRouteDirection);
		canvas.drawPath(pathForTurn, paintTurnOutlayStroke);
		if (textPaint != null) {
			if (turnType != null && !mini && turnType.getExitOut() > 0) {
				canvas.drawText(turnType.getExitOut() + "", centerText.x,
						centerText.y - (textPaint.descent() + textPaint.ascent()) / 2, textPaint);
			}
		}
	}

	public void setTextPaint(@NonNull TextPaint textPaint) {
		this.textPaint = textPaint;
		this.textPaint.setTextAlign(Paint.Align.CENTER);
	}

	public void updateNightMode(boolean nightMode) {
		if (this.nightMode == null || this.nightMode != nightMode) {
			this.nightMode = nightMode;
			int fillColor = ctx.getColor(nightMode
					? R.color.nav_arrow_circle_color_dark
					: R.color.nav_arrow_circle_color_light);
			paintTurnOutlayFill.setColor(fillColor);
		}
	}

	@Override
	public void setAlpha(int alpha) {
		paintRouteDirection.setAlpha(alpha);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {
		paintRouteDirection.setColorFilter(cf);
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Nullable
	public TurnType getTurnType() {
		return turnType;
	}

	public boolean setTurnType(@Nullable TurnType turnType) {
		if (turnType != this.turnType && !getBounds().isEmpty()) {
			this.turnType = turnType;
			TurnPathHelper.calcTurnPath(originalPathForTurn, originalPathForTurnOutlay, turnType, null,
					centerText, mini, false, true, false);
			onBoundsChange(getBounds());
			return true;
		}
		return false;
	}
}