/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import static com.android.launcher3.icons.BitmapInfo.FLAG_THEMED;
import static com.android.launcher3.model.data.ItemInfoWithIcon.FLAG_ICON_BADGED;
import static com.android.launcher3.util.SplitConfigurationOptions.STAGE_POSITION_BOTTOM_OR_RIGHT;
import static com.android.launcher3.util.SplitConfigurationOptions.STAGE_POSITION_TOP_OR_LEFT;
import static com.android.launcher3.util.SplitConfigurationOptions.STAGE_TYPE_MAIN;
import static com.android.launcher3.util.Executors.MODEL_EXECUTOR;
import com.android.launcher3.icons.IconProvider;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Person;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.TransactionTooLargeException;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import com.android.launcher3.LauncherModel;
import com.android.launcher3.dragndrop.FolderAdaptiveIcon;
import com.android.launcher3.graphics.TintedDrawableSpan;
import com.android.launcher3.icons.ShortcutCachingLogic;
import com.android.launcher3.icons.ThemedIconDrawable;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.ItemInfoWithIcon;
import com.android.launcher3.pm.ShortcutConfigActivityInfo;
import com.android.launcher3.shortcuts.ShortcutKey;
import com.android.launcher3.shortcuts.ShortcutRequest;
import com.android.launcher3.testing.shared.ResourceUtils;
import com.android.launcher3.util.IntArray;
import com.android.launcher3.util.SplitConfigurationOptions.SplitPositionOption;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;
import com.android.launcher3.views.BaseDragLayer;
import com.android.launcher3.widget.PendingAddShortcutInfo;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.android.launcher3.util.Executors.MODEL_EXECUTOR;

/**
 * Various utilities shared amongst the Launcher's classes.
 */
public final class Utilities {

    private static final String TAG = "Launcher.Utilities";

    private static final Pattern sTrimPattern =
            Pattern.compile("^[\\s|\\p{javaSpaceChar}]*(.*)[\\s|\\p{javaSpaceChar}]*$");

    private static final Matrix sMatrix = new Matrix();
    private static final Matrix sInverseMatrix = new Matrix();

    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final Person[] EMPTY_PERSON_ARRAY = new Person[0];

    @ChecksSdkIntAtLeast(api = VERSION_CODES.P)
    public static final boolean ATLEAST_P = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;

    @ChecksSdkIntAtLeast(api = VERSION_CODES.Q)
    public static final boolean ATLEAST_Q = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;

    @ChecksSdkIntAtLeast(api = VERSION_CODES.R)
    public static final boolean ATLEAST_R = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;

    @ChecksSdkIntAtLeast(api = VERSION_CODES.S)
    public static final boolean ATLEAST_S = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;

    @ChecksSdkIntAtLeast(api = VERSION_CODES.TIRAMISU, codename = "T")
    public static final boolean ATLEAST_T = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;

    public static final boolean ATLEAST_OREO =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;

    /**
     * Set on a motion event dispatched from the nav bar. See {@link MotionEvent#setEdgeFlags(int)}.
     */
    public static final int EDGE_NAV_BAR = 1 << 8;

    public static final String KEY_DT_GESTURE = "pref_dt_gesture";

    /**
     * Indicates if the device has a debug build. Should only be used to store additional info or
     * add extra logging and not for changing the app behavior.
     */
    public static final boolean IS_DEBUG_DEVICE =
            Build.TYPE.toLowerCase(Locale.ROOT).equals("eng");

    /**
     * Returns true if theme is dark.
     */
    public static boolean isDarkTheme(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        int nightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static boolean isDevelopersOptionsEnabled(Context context) {
        return Settings.Global.getInt(context.getApplicationContext().getContentResolver(),
                        Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
    }

    public static boolean IS_RUNNING_IN_TEST_HARNESS =
                    ActivityManager.isRunningInTestHarness();

    public static void enableRunningInTestHarnessForTests() {
        IS_RUNNING_IN_TEST_HARNESS = true;
    }

    public static boolean isPropertyEnabled(String propertyName) {
        return Log.isLoggable(propertyName, Log.VERBOSE);
    }

    /**
     * Given a coordinate relative to the descendant, find the coordinate in a parent view's
     * coordinates.
     *
     * @param descendant The descendant to which the passed coordinate is relative.
     * @param ancestor The root view to make the coordinates relative to.
     * @param coord The coordinate that we want mapped.
     * @param includeRootScroll Whether or not to account for the scroll of the descendant:
     *          sometimes this is relevant as in a child's coordinates within the descendant.
     * @return The factor by which this descendant is scaled relative to this DragLayer. Caution
     *         this scale factor is assumed to be equal in X and Y, and so if at any point this
     *         assumption fails, we will need to return a pair of scale factors.
     */
    public static float getDescendantCoordRelativeToAncestor(
            View descendant, View ancestor, float[] coord, boolean includeRootScroll) {
        return getDescendantCoordRelativeToAncestor(descendant, ancestor, coord, includeRootScroll,
                false);
    }

    /**
     * Given a coordinate relative to the descendant, find the coordinate in a parent view's
     * coordinates.
     *
     * @param descendant The descendant to which the passed coordinate is relative.
     * @param ancestor The root view to make the coordinates relative to.
     * @param coord The coordinate that we want mapped.
     * @param includeRootScroll Whether or not to account for the scroll of the descendant:
     *          sometimes this is relevant as in a child's coordinates within the descendant.
     * @param ignoreTransform If true, view transform is ignored
     * @return The factor by which this descendant is scaled relative to this DragLayer. Caution
     *         this scale factor is assumed to be equal in X and Y, and so if at any point this
     *         assumption fails, we will need to return a pair of scale factors.
     */
    public static float getDescendantCoordRelativeToAncestor(View descendant, View ancestor,
            float[] coord, boolean includeRootScroll, boolean ignoreTransform) {
        float scale = 1.0f;
        View v = descendant;
        while(v != ancestor && v != null) {
            // For TextViews, scroll has a meaning which relates to the text position
            // which is very strange... ignore the scroll.
            if (v != descendant || includeRootScroll) {
                offsetPoints(coord, -v.getScrollX(), -v.getScrollY());
            }

            if (!ignoreTransform) {
                v.getMatrix().mapPoints(coord);
            }
            offsetPoints(coord, v.getLeft(), v.getTop());
            scale *= v.getScaleX();

            v = v.getParent() instanceof View ? (View) v.getParent() : null;
        }
        return scale;
    }

    /**
     * Returns bounds for a child view of DragLayer, in drag layer coordinates.
     *
     * see {@link com.android.launcher3.dragndrop.DragLayer}.
     *
     * @param viewBounds Bounds of the view wanted in drag layer coordinates, relative to the view
     *                   itself. eg. (0, 0, view.getWidth, view.getHeight)
     * @param ignoreTransform If true, view transform is ignored
     * @param outRect The out rect where we return the bounds of {@param view} in drag layer coords.
     */
    public static void getBoundsForViewInDragLayer(BaseDragLayer dragLayer, View view,
            Rect viewBounds, boolean ignoreTransform, float[] recycle, RectF outRect) {
        float[] points = recycle == null ? new float[4] : recycle;
        points[0] = viewBounds.left;
        points[1] = viewBounds.top;
        points[2] = viewBounds.right;
        points[3] = viewBounds.bottom;

        Utilities.getDescendantCoordRelativeToAncestor(view, dragLayer, points,
                false, ignoreTransform);
        outRect.set(
                Math.min(points[0], points[2]),
                Math.min(points[1], points[3]),
                Math.max(points[0], points[2]),
                Math.max(points[1], points[3]));
    }

    /**
     * Similar to {@link #mapCoordInSelfToDescendant(View descendant, View root, float[] coord)}
     * but accepts a Rect instead of float[].
     */
    public static void mapRectInSelfToDescendant(View descendant, View root, Rect rect) {
        float[] coords = new float[]{rect.left, rect.top, rect.right, rect.bottom};
        mapCoordInSelfToDescendant(descendant, root, coords);
        rect.set((int) coords[0], (int) coords[1], (int) coords[2], (int) coords[3]);
    }

    /**
     * Inverse of {@link #getDescendantCoordRelativeToAncestor(View, View, float[], boolean)}.
     */
    public static void mapCoordInSelfToDescendant(View descendant, View root, float[] coord) {
        sMatrix.reset();
        View v = descendant;
        while(v != root) {
            sMatrix.postTranslate(-v.getScrollX(), -v.getScrollY());
            sMatrix.postConcat(v.getMatrix());
            sMatrix.postTranslate(v.getLeft(), v.getTop());
            v = (View) v.getParent();
        }
        sMatrix.postTranslate(-v.getScrollX(), -v.getScrollY());
        sMatrix.invert(sInverseMatrix);
        sInverseMatrix.mapPoints(coord);
    }

    /**
     * Sets {@param out} to be same as {@param in} by rounding individual values
     */
    public static void roundArray(float[] in, int[] out) {
        for (int i = 0; i < in.length; i++) {
            out[i] = Math.round(in[i]);
        }
    }

    public static void offsetPoints(float[] points, float offsetX, float offsetY) {
        for (int i = 0; i < points.length; i += 2) {
            points[i] += offsetX;
            points[i + 1] += offsetY;
        }
    }

    /**
     * Utility method to determine whether the given point, in local coordinates,
     * is inside the view, where the area of the view is expanded by the slop factor.
     * This method is called while processing touch-move events to determine if the event
     * is still within the view.
     */
    public static boolean pointInView(View v, float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < (v.getWidth() + slop) &&
                localY < (v.getHeight() + slop);
    }

    public static void scaleRectFAboutCenter(RectF r, float scale) {
        scaleRectFAboutCenter(r, scale, scale);
    }

    /**
     * Similar to {@link #scaleRectAboutCenter(Rect, float)} except this allows different scales
     * for X and Y
     */
    public static void scaleRectFAboutCenter(RectF r, float scaleX, float scaleY) {
        float px = r.centerX();
        float py = r.centerY();
        r.offset(-px, -py);
        r.left = r.left * scaleX;
        r.top = r.top * scaleY;
        r.right = r.right * scaleX;
        r.bottom = r.bottom * scaleY;
        r.offset(px, py);
    }

    public static void scaleRectAboutCenter(Rect r, float scale) {
        if (scale != 1.0f) {
            int cx = r.centerX();
            int cy = r.centerY();
            r.offset(-cx, -cy);
            r.left = (int) (r.left * scale + 0.5f);
            r.top = (int) (r.top * scale + 0.5f);
            r.right = (int) (r.right * scale + 0.5f);
            r.bottom = (int) (r.bottom * scale + 0.5f);
            r.offset(cx, cy);
        }
    }

    public static float shrinkRect(Rect r, float scaleX, float scaleY) {
        float scale = Math.min(Math.min(scaleX, scaleY), 1.0f);
        if (scale < 1.0f) {
            int deltaX = (int) (r.width() * (scaleX - scale) * 0.5f);
            r.left += deltaX;
            r.right -= deltaX;

            int deltaY = (int) (r.height() * (scaleY - scale) * 0.5f);
            r.top += deltaY;
            r.bottom -= deltaY;
        }
        return scale;
    }

    /**
     * Maps t from one range to another range.
     * @param t The value to map.
     * @param fromMin The lower bound of the range that t is being mapped from.
     * @param fromMax The upper bound of the range that t is being mapped from.
     * @param toMin The lower bound of the range that t is being mapped to.
     * @param toMax The upper bound of the range that t is being mapped to.
     * @return The mapped value of t.
     */
    public static float mapToRange(float t, float fromMin, float fromMax, float toMin, float toMax,
            Interpolator interpolator) {
        if (fromMin == fromMax || toMin == toMax) {
            Log.e(TAG, "mapToRange: range has 0 length");
            return toMin;
        }
        float progress = getProgress(t, fromMin, fromMax);
        return mapRange(interpolator.getInterpolation(progress), toMin, toMax);
    }

    /** Bounds t between a lower and upper bound and maps the result to a range. */
    public static float mapBoundToRange(float t, float lowerBound, float upperBound,
            float toMin, float toMax, Interpolator interpolator) {
        return mapToRange(boundToRange(t, lowerBound, upperBound), lowerBound, upperBound,
                toMin, toMax, interpolator);
    }

    public static float getProgress(float current, float min, float max) {
        return Math.abs(current - min) / Math.abs(max - min);
    }

    public static float mapRange(float value, float min, float max) {
        return min + (value * (max - min));
    }

    /**
     * Trims the string, removing all whitespace at the beginning and end of the string.
     * Non-breaking whitespaces are also removed.
     */
    @NonNull
    public static String trim(CharSequence s) {
        if (s == null) {
            return "";
        }

        // Just strip any sequence of whitespace or java space characters from the beginning and end
        Matcher m = sTrimPattern.matcher(s);
        return m.replaceAll("$1");
    }

    /**
     * Calculates the height of a given string at a specific text size.
     */
    public static int calculateTextHeight(float textSizePx) {
        Paint p = new Paint();
        p.setTextSize(textSizePx);
        Paint.FontMetrics fm = p.getFontMetrics();
        return (int) Math.ceil(fm.bottom - fm.top);
    }

    public static boolean isRtl(Resources res) {
        return res.getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    /** Converts a pixel value (px) to scale pixel value (SP) for the current device. */
    public static float pxToSp(float size) {
        return size / Resources.getSystem().getDisplayMetrics().scaledDensity;
    }

    public static float dpiFromPx(float size, int densityDpi) {
        float densityRatio = (float) densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        return (size / densityRatio);
    }

    /** Converts a dp value to pixels for the current device. */
    public static int dpToPx(float dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    /** Converts a dp value to pixels for a certain density. */
    public static int dpToPx(float dp, int densityDpi) {
        float densityRatio = (float) densityDpi / DisplayMetrics.DENSITY_DEFAULT;
        return (int) (dp * densityRatio);
    }

    public static int pxFromSp(float size, DisplayMetrics metrics) {
        return pxFromSp(size, metrics, 1f);
    }

    public static int pxFromSp(float size, DisplayMetrics metrics, float scale) {
        float value = scale * TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, metrics);
        return ResourceUtils.roundPxValueFromFloat(value);
    }

    public static String createDbSelectionQuery(String columnName, IntArray values) {
        return String.format(Locale.ENGLISH, "%s IN (%s)", columnName, values.toConcatString());
    }

    public static boolean isBootCompleted() {
        return "1".equals(getSystemProperty("sys.boot_completed", "1"));
    }

    public static String getSystemProperty(String property, String defaultValue) {
        try {
            Class clazz = Class.forName("android.os.SystemProperties");
            Method getter = clazz.getDeclaredMethod("get", String.class);
            String value = (String) getter.invoke(null, property);
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        } catch (Exception e) {
            Log.d(TAG, "Unable to read system properties");
        }
        return defaultValue;
    }

    /**
     * Ensures that a value is within given bounds. Specifically:
     * If value is less than lowerBound, return lowerBound; else if value is greater than upperBound,
     * return upperBound; else return value unchanged.
     */
    public static int boundToRange(int value, int lowerBound, int upperBound) {
        return Math.max(lowerBound, Math.min(value, upperBound));
    }

    /**
     * @see #boundToRange(int, int, int).
     */
    public static float boundToRange(float value, float lowerBound, float upperBound) {
        return Math.max(lowerBound, Math.min(value, upperBound));
    }

    /**
     * @see #boundToRange(int, int, int).
     */
    public static long boundToRange(long value, long lowerBound, long upperBound) {
        return Math.max(lowerBound, Math.min(value, upperBound));
    }

    /**
     * Wraps a message with a TTS span, so that a different message is spoken than
     * what is getting displayed.
     * @param msg original message
     * @param ttsMsg message to be spoken
     */
    public static CharSequence wrapForTts(CharSequence msg, String ttsMsg) {
        SpannableString spanned = new SpannableString(msg);
        spanned.setSpan(new TtsSpan.TextBuilder(ttsMsg).build(),
                0, spanned.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return spanned;
    }

    /**
     * Prefixes a text with the provided icon
     */
    public static CharSequence prefixTextWithIcon(Context context, int iconRes, CharSequence msg) {
        // Update the hint to contain the icon.
        // Prefix the original hint with two spaces. The first space gets replaced by the icon
        // using span. The second space is used for a singe space character between the hint
        // and the icon.
        SpannableString spanned = new SpannableString("  " + msg);
        spanned.setSpan(new TintedDrawableSpan(context, iconRes),
                0, 1, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        return spanned;
    }

    public static boolean isWallpaperSupported(Context context) {
        return context.getSystemService(WallpaperManager.class).isWallpaperSupported();
    }

    public static boolean isWallpaperAllowed(Context context) {
        return context.getSystemService(WallpaperManager.class).isSetWallpaperAllowed();
    }

    public static boolean isBinderSizeError(Exception e) {
        return e.getCause() instanceof TransactionTooLargeException
                || e.getCause() instanceof DeadObjectException;
    }

    /**
     * Utility method to post a runnable on the handler, skipping the synchronization barriers.
     */
    public static void postAsyncCallback(Handler handler, Runnable callback) {
        Message msg = Message.obtain(handler, callback);
        msg.setAsynchronous(true);
        handler.sendMessage(msg);
    }

    /**
     * Returns the full drawable for info without any flattening or pre-processing.
     *
     * @param shouldThemeIcon If true, will theme icons when applicable
     * @param outObj this is set to the internal data associated with {@code info},
     *               eg {@link LauncherActivityInfo} or {@link ShortcutInfo}.
     */
    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    public static Drawable getFullDrawable(Context context, ItemInfo info, int width, int height,
            boolean shouldThemeIcon, Object[] outObj) {
        Drawable icon = loadFullDrawableWithoutTheme(context, info, width, height, outObj);
        if (ATLEAST_T && icon instanceof AdaptiveIconDrawable && shouldThemeIcon) {
            AdaptiveIconDrawable aid = (AdaptiveIconDrawable) icon.mutate();
            Drawable mono = aid.getMonochrome();
            if (mono != null && Themes.isThemedIconEnabled(context)) {
                int[] colors = ThemedIconDrawable.getColors(context);
                mono = mono.mutate();
                mono.setTint(colors[1]);
                return new AdaptiveIconDrawable(new ColorDrawable(colors[0]), mono);
            }
        }
        return icon;
    }

    private static Drawable loadFullDrawableWithoutTheme(Context context, ItemInfo info,
            int width, int height, Object[] outObj) {
        ActivityContext activity = ActivityContext.lookupContext(context);
        LauncherAppState appState = LauncherAppState.getInstance(context);
        if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION) {
            LauncherActivityInfo activityInfo = context.getSystemService(LauncherApps.class)
                    .resolveActivity(info.getIntent(), info.user);
            outObj[0] = activityInfo;
            return activityInfo == null ? null : IconProvider.INSTANCE.get(context).getIcon(
                            activityInfo, activity.getDeviceProfile().inv.fillResIconDpi);
        } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
            if (info instanceof PendingAddShortcutInfo) {
                ShortcutConfigActivityInfo activityInfo =
                        ((PendingAddShortcutInfo) info).activityInfo;
                outObj[0] = activityInfo;
                return activityInfo.getFullResIcon(appState.getIconCache());
            }
            List<ShortcutInfo> si = ShortcutKey.fromItemInfo(info)
                    .buildRequest(context)
                    .query(ShortcutRequest.ALL);
            if (si.isEmpty()) {
                return null;
            } else {
                outObj[0] = si.get(0);
                return ShortcutCachingLogic.getIcon(context, si.get(0),
                        appState.getInvariantDeviceProfile().fillResIconDpi);
            }
        } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
            FolderAdaptiveIcon icon = FolderAdaptiveIcon.createFolderAdaptiveIcon(
                    activity, info.id, new Point(width, height));
            if (icon == null) {
                return null;
            }
            outObj[0] = icon;
            return icon;
        } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SEARCH_ACTION
                && info instanceof ItemInfoWithIcon) {
            return ((ItemInfoWithIcon) info).bitmap.newIcon(context);
        } else {
            return null;
        }
    }

    /**
     * For apps icons and shortcut icons that have badges, this method creates a drawable that can
     * later on be rendered on top of the layers for the badges. For app icons, work profile badges
     * can only be applied. For deep shortcuts, when dragged from the pop up container, there's no
     * badge. When dragged from workspace or folder, it may contain app AND/OR work profile badge
     **/
    @TargetApi(Build.VERSION_CODES.O)
    public static Drawable getBadge(Context context, ItemInfo info, Object obj) {
        LauncherAppState appState = LauncherAppState.getInstance(context);
        if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
            boolean iconBadged = (info instanceof ItemInfoWithIcon)
                    && (((ItemInfoWithIcon) info).runtimeStatusFlags & FLAG_ICON_BADGED) > 0;
            if ((info.id == ItemInfo.NO_ID && !iconBadged)
                    || !(obj instanceof ShortcutInfo)) {
                // The item is not yet added on home screen.
                return new ColorDrawable(Color.TRANSPARENT);
            }
            ShortcutInfo si = (ShortcutInfo) obj;
            return LauncherAppState.getInstance(appState.getContext())
                    .getIconCache().getShortcutInfoBadge(si).newIcon(context, FLAG_THEMED);
        } else if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_FOLDER) {
            return ((FolderAdaptiveIcon) obj).getBadge();
        } else {
            return Process.myUserHandle().equals(info.user)
                    ? new ColorDrawable(Color.TRANSPARENT)
                    : context.getDrawable(R.drawable.ic_work_app_badge);
        }
    }

    /**
     * @param context the context to use for resources
     * @return true if the user is currently using gesture navigation
     */
    public static boolean isUsingGestureNav(Context context) {
        final Resources res = context.getResources();
        final int resID = res.getIdentifier(
            "config_navBarInteractionMode", "integer", "android");
        return res.getInteger(resID) == 2;
    }

    public static float squaredHypot(float x, float y) {
        return x * x + y * y;
    }

    public static float squaredTouchSlop(Context context) {
        float slop = ViewConfiguration.get(context).getScaledTouchSlop();
        return slop * slop;
    }

    /**
     * Rotates `inOutBounds` by `delta` 90-degree increments. Rotation is visually CCW. Parent
     * sizes represent the "space" that will rotate carrying inOutBounds along with it to determine
     * the final bounds.
     */
    public static void rotateBounds(Rect inOutBounds, int parentWidth, int parentHeight,
            int delta) {
        int rdelta = ((delta % 4) + 4) % 4;
        int origLeft = inOutBounds.left;
        switch (rdelta) {
            case 0:
                return;
            case 1:
                inOutBounds.left = inOutBounds.top;
                inOutBounds.top = parentWidth - inOutBounds.right;
                inOutBounds.right = inOutBounds.bottom;
                inOutBounds.bottom = parentWidth - origLeft;
                return;
            case 2:
                inOutBounds.left = parentWidth - inOutBounds.right;
                inOutBounds.right = parentWidth - origLeft;
                return;
            case 3:
                inOutBounds.left = parentHeight - inOutBounds.bottom;
                inOutBounds.bottom = inOutBounds.right;
                inOutBounds.right = parentHeight - inOutBounds.top;
                inOutBounds.top = origLeft;
                return;
        }
    }

    /**
     * Make a color filter that blends a color into the destination based on a scalable amout.
     *
     * @param color to blend in.
     * @param tintAmount [0-1] 0 no tinting, 1 full color.
     * @return ColorFilter for tinting, or {@code null} if no filter is needed.
     */
    public static ColorFilter makeColorTintingColorFilter(int color, float tintAmount) {
        if (tintAmount == 0f) {
            return null;
        }
        return new LightingColorFilter(
                // This isn't blending in white, its making a multiplication mask for the base color
                ColorUtils.blendARGB(Color.WHITE, 0, tintAmount),
                ColorUtils.blendARGB(0, color, tintAmount));
    }

    public static Rect getViewBounds(@NonNull View v) {
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        return new Rect(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight());
    }

    /**
     * Returns a list of screen-splitting options depending on the device orientation (split top for
     * portrait, split left for landscape, split left and right for landscape tablets, etc.)
     */
    public static List<SplitPositionOption> getSplitPositionOptions(
            DeviceProfile dp) {
        List<SplitPositionOption> options = new ArrayList<>();
        // Add both left and right options if we're in tablet mode
        if (dp.isTablet && dp.isLandscape) {
            options.add(new SplitPositionOption(
                    R.drawable.ic_split_left, R.string.split_screen_position_left,
                    STAGE_POSITION_TOP_OR_LEFT, STAGE_TYPE_MAIN));
            options.add(new SplitPositionOption(
                    R.drawable.ic_split_right, R.string.split_screen_position_right,
                    STAGE_POSITION_BOTTOM_OR_RIGHT, STAGE_TYPE_MAIN));
        } else {
            if (dp.isSeascape()) {
                // Add left/right options
                options.add(new SplitPositionOption(
                        R.drawable.ic_split_right, R.string.split_screen_position_right,
                        STAGE_POSITION_BOTTOM_OR_RIGHT, STAGE_TYPE_MAIN));
            } else if (dp.isLandscape) {
                options.add(new SplitPositionOption(
                        R.drawable.ic_split_left, R.string.split_screen_position_left,
                        STAGE_POSITION_TOP_OR_LEFT, STAGE_TYPE_MAIN));
            } else {
                // Only add top option
                options.add(new SplitPositionOption(
                        R.drawable.ic_split_top, R.string.split_screen_position_top,
                        STAGE_POSITION_TOP_OR_LEFT, STAGE_TYPE_MAIN));
            }
        }
        return options;
    }

    /** Logs the Scale and Translate properties of a matrix. Ignores skew and perspective. */
    public static void logMatrix(String label, Matrix matrix) {
        float[] matrixValues = new float[9];
        matrix.getValues(matrixValues);
        Log.d(label, String.format("%s: %s\nscale (x,y) = (%f, %f)\ntranslate (x,y) = (%f, %f)",
                label, matrix, matrixValues[Matrix.MSCALE_X], matrixValues[Matrix.MSCALE_Y],
                matrixValues[Matrix.MTRANS_X], matrixValues[Matrix.MTRANS_Y]
        ));
    }

    public static boolean isDoubleTapGestureEnabled(Context context) {
        SharedPreferences prefs = LauncherPrefs.getPrefs(context.getApplicationContext());
        return prefs.getBoolean(KEY_DT_GESTURE, true);
    }

    public static boolean isWorkspaceEditAllowed(Context context) {
        SharedPreferences prefs = LauncherPrefs.getPrefs(context.getApplicationContext());
        return !prefs.getBoolean(InvariantDeviceProfile.KEY_WORKSPACE_LOCK, false);
    }
}
