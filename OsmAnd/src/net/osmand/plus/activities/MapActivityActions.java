package net.osmand.plus.activities;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.*;
import static net.osmand.plus.search.ShowQuickSearchMode.CURRENT;
import static net.osmand.plus.widgets.ctxmenu.ViewCreator.PROFILES_CHOSEN_PROFILE_TAG;
import static net.osmand.plus.widgets.ctxmenu.ViewCreator.PROFILES_CONTROL_BUTTON_TAG;
import static net.osmand.plus.widgets.ctxmenu.ViewCreator.PROFILES_NORMAL_PROFILE_TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import net.osmand.ApiClient;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.SpecialPointType;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.GeocodingLookupService;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.backup.ui.BackupAuthorizationFragment;
import net.osmand.plus.backup.ui.BackupCloudFragment;
import net.osmand.plus.dashboard.DashboardType;
import net.osmand.plus.dialogs.SpeedCamerasBottomSheet;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.help.HelpActivity;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.liveupdates.LiveUpdatesFragment;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment;
import net.osmand.plus.mapcontextmenu.AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.measurementtool.StartPlanRouteBottomSheet;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.plugins.PluginsFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.audionotes.AudioVideoNotesPlugin;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.dialogs.DismissRouteBottomSheetFragment;
import net.osmand.plus.profiles.data.ProfileDataObject;
import net.osmand.plus.profiles.data.RoutingDataUtils;
import net.osmand.plus.profiles.data.RoutingProfilesHolder;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu;
import net.osmand.plus.routepreparationmenu.WaypointsFragment;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.search.ShowQuickSearchMode;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.DeviceUtils;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.MapActions;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapControlsLayer;
import net.osmand.plus.views.layers.POIMapLayer;
import net.osmand.plus.views.mapwidgets.configure.dialogs.ConfigureScreenFragment;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.ContextMenuListAdapter;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.callback.ItemClickListener;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.wikivoyage.WikivoyageWelcomeDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreActivity;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingContext;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MapActivityActions extends MapActions {

	private static final Log LOG = PlatformUtil.getLog(MapActivityActions.class);

	public static final String KEY_LONGITUDE = "longitude";
	public static final String KEY_LATITUDE = "latitude";
	public static final String KEY_NAME = "name";

	public static final int REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION = 203;

	// Constants for determining the order of items in the additional actions context menu
	public static final int DIRECTIONS_FROM_ITEM_ORDER = 1000;
	public static final int SEARCH_NEAR_ITEM_ORDER = 2000;
	public static final int CHANGE_POSITION_ITEM_ORDER = 3000;
	public static final int EDIT_GPX_WAYPOINT_ITEM_ORDER = 9000;
	public static final int ADD_GPX_WAYPOINT_ITEM_ORDER = 9000;
	public static final int MEASURE_DISTANCE_ITEM_ORDER = 13000;
	public static final int AVOID_ROAD_ITEM_ORDER = 14000;

	private static final int DRAWER_MODE_NORMAL = 0;
	private static final int DRAWER_MODE_SWITCH_PROFILE = 1;

	private final RoutingDataUtils routingDataUtils;

	@Nullable
	private ImageView drawerLogoHeader;

	private int drawerMode = DRAWER_MODE_NORMAL;

	public MapActivityActions(@NonNull OsmandApplication app) {
		super(app);
		this.routingDataUtils = new RoutingDataUtils(app);
	}

	@Override
	public void setMapActivity(@Nullable MapActivity activity) {
		super.setMapActivity(activity);

		if (activity != null) {
			this.drawerLogoHeader = new ImageView(activity);
			this.drawerLogoHeader.setPadding(-AndroidUtils.dpToPx(activity, 8f),
					AndroidUtils.dpToPx(activity, 16f), 0, 0);
		}
	}

	public void addAsTarget(double latitude, double longitude, PointDescription pd) {
		TargetPointsHelper targets = app.getTargetPointsHelper();
		targets.navigateToPoint(new LatLon(latitude, longitude), true, targets.getIntermediatePoints().size() + 1, pd);
		openIntermediatePointsDialog();
	}


	public void addMapMarker(double latitude, double longitude, PointDescription pd,
							 @Nullable String mapObjectName) {
		MapMarkersHelper markersHelper = app.getMapMarkersHelper();
		markersHelper.addMapMarker(new LatLon(latitude, longitude), pd, mapObjectName);
	}

	public void editWaypoints() {
		openIntermediatePointsDialog();
	}

	protected String getString(@StringRes int resId) {
		return app.getString(resId);
	}

	public void addActionsToAdapter(double latitude, double longitude,
									ContextMenuAdapter adapter, Object selectedObj, boolean configureMenu) {
		MapActivity activity = getMapActivity();
		if (activity == null) {
			return;
		}
		GpxSelectionHelper gpxHelper = app.getSelectedGpxHelper();

		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_ADD_ID)
				.setTitleId(selectedObj instanceof FavouritePoint ? R.string.favourites_context_menu_edit : R.string.shared_string_add, activity)
				.setIcon(selectedObj instanceof FavouritePoint ? R.drawable.ic_action_edit_dark : R.drawable.ic_action_favorite_stroke)
				.setOrder(10));
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_MARKER_ID)
				.setTitleId(selectedObj instanceof MapMarker ? R.string.shared_string_edit : R.string.shared_string_marker, activity)
				.setIcon(selectedObj instanceof MapMarker ? R.drawable.ic_action_edit_dark : R.drawable.ic_action_flag_stroke)
				.setOrder(20));
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_SHARE_ID)
				.setTitleId(R.string.shared_string_share, activity)
				.setIcon(R.drawable.ic_action_gshare_dark)
				.setOrder(30));
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_MORE_ID)
				.setTitleId(R.string.shared_string_actions, activity)
				.setIcon(R.drawable.ic_actions_menu)
				.setOrder(40));

		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_DIRECTIONS_FROM_ID)
				.setTitleId(R.string.context_menu_item_directions_from, activity)
				.setIcon(R.drawable.ic_action_route_direction_from_here)
				.setOrder(DIRECTIONS_FROM_ITEM_ORDER));
		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_SEARCH_NEARBY)
				.setTitleId(R.string.context_menu_item_search, activity)
				.setIcon(R.drawable.ic_action_search_dark)
				.setOrder(SEARCH_NEAR_ITEM_ORDER));

		PluginsHelper.registerMapContextMenu(activity, latitude, longitude, adapter, selectedObj, configureMenu);

		ItemClickListener listener = (callback, view, item, isChecked) -> {
			int resId = item.getTitleId();
			if (resId == R.string.context_menu_item_add_waypoint) {
				activity.getContextMenu().addWptPt();
			} else if (resId == R.string.context_menu_item_edit_waypoint) {
				activity.getContextMenu().editWptPt();
			}
			return true;
		};

		ContextMenuItem editGpxItem = new ContextMenuItem(MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT)
				.setTitleId(R.string.context_menu_item_edit_waypoint, activity)
				.setIcon(R.drawable.ic_action_edit_dark)
				.setOrder(EDIT_GPX_WAYPOINT_ITEM_ORDER)
				.setListener(listener);
		ContextMenuItem addGpxItem = new ContextMenuItem(MAP_CONTEXT_MENU_ADD_GPX_WAYPOINT)
				.setTitleId(R.string.context_menu_item_add_waypoint, activity)
				.setIcon(R.drawable.ic_action_gnew_label_dark)
				.setOrder(ADD_GPX_WAYPOINT_ITEM_ORDER)
				.setListener(listener);

		if (configureMenu) {
			adapter.addItem(addGpxItem);
		} else if (selectedObj instanceof WptPt
				&& gpxHelper.getSelectedGPXFile((WptPt) selectedObj) != null) {
			adapter.addItem(editGpxItem);
		} else if (!gpxHelper.getSelectedGPXFiles().isEmpty()
				|| (PluginsHelper.isActive(OsmandMonitoringPlugin.class))) {
			adapter.addItem(addGpxItem);
		}

		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_MEASURE_DISTANCE)
				.setTitleId(R.string.plan_route, activity)
				.setIcon(R.drawable.ic_action_ruler)
				.setOrder(MEASURE_DISTANCE_ITEM_ORDER));

		adapter.addItem(new ContextMenuItem(MAP_CONTEXT_MENU_AVOID_ROAD)
				.setTitleId(R.string.avoid_road, activity)
				.setIcon(R.drawable.ic_action_alert)
				.setOrder(AVOID_ROAD_ITEM_ORDER));
	}

	public void contextMenuPoint(MapActivity activity, double latitude, double longitude,
								 ContextMenuAdapter _adapter,
								 Object selectedObj) {
		ContextMenuAdapter adapter = _adapter == null ? new ContextMenuAdapter(app) : _adapter;
		addActionsToAdapter(latitude, longitude, adapter, selectedObj, false);
		showAdditionalActionsFragment(adapter, getContextMenuItemClickListener(activity, latitude, longitude, adapter));
	}

	public void showAdditionalActionsFragment(ContextMenuAdapter adapter,
											  AdditionalActionsBottomSheetDialogFragment.ContextMenuItemClickListener listener) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			AdditionalActionsBottomSheetDialogFragment actionsBottomSheetDialogFragment = new AdditionalActionsBottomSheetDialogFragment();
			actionsBottomSheetDialogFragment.setAdapter(adapter, listener);
			actionsBottomSheetDialogFragment.show(activity.getSupportFragmentManager(), AdditionalActionsBottomSheetDialogFragment.TAG);
		}
	}

	public ContextMenuItemClickListener getContextMenuItemClickListener(MapActivity activity, double latitude,
																		double longitude, ContextMenuAdapter adapter) {
		ViewCreator viewCreator = new ViewCreator(activity, !settings.isLightContent());
		ContextMenuListAdapter listAdapter = adapter.toListAdapter(activity, viewCreator);

		return (view, position) -> {
			ContextMenuItem item = adapter.getItem(position);
			int standardId = item.getTitleId();
			ItemClickListener click = item.getItemClickListener();
			if (click != null) {
				click.onContextMenuClick(listAdapter, view, item, false);
			} else if (standardId == R.string.context_menu_item_search) {
				activity.getFragmentsHelper().showQuickSearch(latitude, longitude);
			} else if (standardId == R.string.context_menu_item_directions_from) {
				//if (OsmAndLocationProvider.isLocationPermissionAvailable(activity)) {
				enterDirectionsFromPoint(latitude, longitude);
				//} else {
				//	ActivityCompat.requestPermissions(activity,
				//			new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
				//			REQUEST_LOCATION_FOR_DIRECTIONS_NAVIGATION_PERMISSION);
				//}
			} else if (standardId == R.string.plan_route) {
				activity.getContextMenu().close();
				MeasurementToolFragment.showInstance(activity.getSupportFragmentManager(), new LatLon(latitude, longitude));
			} else if (standardId == R.string.avoid_road) {
				app.getAvoidSpecificRoads().addImpassableRoad(activity, new LatLon(latitude, longitude), true, false, null);
			} else if (standardId == R.string.shared_string_add || standardId == R.string.favourites_context_menu_edit) {
				activity.getContextMenu().hide();
				activity.getContextMenu().buttonFavoritePressed();
			} else if (standardId == R.string.shared_string_marker || standardId == R.string.shared_string_edit) {
				activity.getContextMenu().buttonWaypointPressed();
			} else if (standardId == R.string.shared_string_share) {
				activity.getContextMenu().buttonSharePressed();
			}
		};
	}

	public void enterDirectionsFromPoint(double latitude, double longitude) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.getContextMenu().hide();
			if (!activity.getRoutingHelper().isFollowingMode() && !activity.getRoutingHelper().isRoutePlanningMode()) {
				enterRoutePlanningMode(new LatLon(latitude, longitude), activity.getContextMenu().getPointDescription());
			} else {
				app.getTargetPointsHelper().setStartPoint(new LatLon(latitude, longitude),
						true, activity.getContextMenu().getPointDescription());
			}
		}
	}

	public void enterRoutePlanningModeGivenGpx(GpxFile gpxFile, ApplicationMode appMode,
											   LatLon from,
											   PointDescription fromName, boolean useIntermediatePointsByDefault,
											   boolean showMenu, int menuState) {
		enterRoutePlanningModeGivenGpx(gpxFile, appMode, from, fromName,
				useIntermediatePointsByDefault, showMenu, menuState, null);
	}

	@Override
	public void enterRoutePlanningModeGivenGpx(GpxFile gpxFile, ApplicationMode appMode,
											   LatLon from, PointDescription fromName, boolean useIntermediatePointsByDefault,
											   boolean showMenu, int menuState, @Nullable Boolean passWholeRoute) {
		super.enterRoutePlanningModeGivenGpx(gpxFile, appMode, from, fromName,
				useIntermediatePointsByDefault, showMenu, menuState, passWholeRoute);
		MapActivity activity = getMapActivity();
		if (activity != null) {
			if (showMenu) {
				activity.getMapRouteInfoMenu().setShowMenu(menuState);
			}
		}
	}

	@Override
	public void recalculateRoute(boolean showDialog) {
		super.recalculateRoute(showDialog);
		if (showDialog) {
			showRouteInfoMenu();
		}
	}

	@Override
	protected void initVoiceCommandPlayer(@NonNull ApplicationMode mode, boolean showMenu) {
		Context context = activity != null ? activity : app;
		app.initVoiceCommandPlayer(context, mode, null, true, false, false, showMenu);
	}

	public void contextMenuPoint(double latitude, double longitude) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			contextMenuPoint(activity, latitude, longitude, null, null);
		}
	}

	@NonNull
	public ContextMenuAdapter createMainOptionsMenu() {
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		MapActivity activity = getMapActivity();
		if (activity != null) {
			boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
			if (drawerMode == DRAWER_MODE_SWITCH_PROFILE) {
				return createSwitchProfileOptionsMenu(activity, adapter, nightMode);
			}
			return createNormalOptionsMenu(app, adapter, nightMode);
		}
		return adapter;
	}

	@NonNull
	public ContextMenuAdapter createNormalOptionsMenu() {
		ContextMenuAdapter adapter = new ContextMenuAdapter(app);
		MapActivity activity = getMapActivity();
		if (activity != null) {
			boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
			return createNormalOptionsMenu(app, adapter, nightMode);
		}
		return adapter;
	}

	@NonNull
	private ContextMenuAdapter createSwitchProfileOptionsMenu(@NonNull MapActivity activity,
															  @NonNull ContextMenuAdapter adapter,
															  boolean nightMode) {
		drawerMode = DRAWER_MODE_NORMAL;
		createProfilesController(activity, adapter, nightMode, true);

		adapter.addItem(new ContextMenuItem(DRAWER_MAP_MARKERS_ID)
				.setTitleId(R.string.map_markers, activity)
				.setIcon(R.drawable.ic_action_flag)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_markers_open");
					MapActivity.clearPrevActivityIntent();
					MapMarkersDialogFragment.showInstance(activity);
					return true;
				}));

		adapter.addItem(new ContextMenuItem(DRAWER_BACKUP_RESTORE_ID)
				.setTitleId(R.string.backup_and_restore, activity)
				.setIcon(R.drawable.ic_action_cloud_upload)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_backup_restore_open");
					if (app.getBackupHelper().isRegistered()) {
						BackupCloudFragment.showInstance(activity.getSupportFragmentManager());
					} else {
						BackupAuthorizationFragment.showInstance(activity.getSupportFragmentManager());
					}
					return true;
				}));

		adapter.addItem(new ContextMenuItem(null)
				.setLayout(R.layout.profile_list_item)
				.setColor(ColorUtilities.getActiveColor(app, nightMode))
				.setTag(PROFILES_CONTROL_BUTTON_TAG)
				.setTitle(getString(R.string.shared_string_manage))
				.setListener((uiAdapter, view, item, isChecked) -> {
					BaseSettingsFragment.showInstance(activity, SettingsScreenType.MAIN_SETTINGS);
					return true;
				}));

		adapter.addItem(new ContextMenuItem(DRAWER_CONFIGURE_MAP_ID)
				.setTitleId(R.string.configure_map, activity)
				.setIcon(R.drawable.ic_action_layers)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_config_map_open");
					MapActivity.clearPrevActivityIntent();
					activity.getDashboard().setDashboardVisibility(true, DashboardType.CONFIGURE_MAP, AndroidUtils.getCenterViewCoordinates(view));
					return false;
				}));

		String d = getString(R.string.welmode_download_maps);
		if (app.getDownloadThread().getIndexes().isDownloadedFromInternet) {
			List<IndexItem> updt = app.getDownloadThread().getIndexes().getItemsToUpdate();
			if (updt != null && updt.size() > 0) {
				d += " (" + updt.size() + ")";
			}
		}
		adapter.addItem(new ContextMenuItem(DRAWER_DOWNLOAD_MAPS_ID)
				.setTitleId(R.string.welmode_download_maps, null)
				.setTitle(d).setIcon(R.drawable.ic_type_archive)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_download_maps_open");
					Intent newIntent = new Intent(activity, app.getAppCustomization().getDownloadActivity());
					newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					activity.startActivity(newIntent);
					return true;
				}));

		adapter.addItem(new ContextMenuItem(DRAWER_LIVE_UPDATES_ID)
				.setTitleId(R.string.live_updates, activity)
				.setIcon(R.drawable.ic_action_map_update)
				.setListener((uiAdapter, view, item, isChecked) -> {
					LiveUpdatesFragment.showInstance(activity.getSupportFragmentManager(), null);
					return true;
				}));

		app.getAidlApi().registerNavDrawerItems(activity, adapter);

		adapter.addItem(new ContextMenuItem(DRAWER_DIVIDER_ID)
				.setLayout(R.layout.drawer_divider));

		adapter.addItem(new ContextMenuItem(DRAWER_CONFIGURE_SCREEN_ID)
				.setTitleId(R.string.layer_map_appearance, activity)
				.setIcon(R.drawable.ic_configure_screen_dark)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_config_screen_open");
					MapActivity.clearPrevActivityIntent();
					ConfigureScreenFragment.showInstance(activity);
					return true;
				}));
		adapter.addItem(new ContextMenuItem(DRAWER_PLUGINS_ID)
				.setTitleId(R.string.prefs_plugins, activity)
				.setIcon(R.drawable.ic_extension_dark)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_plugins_open");
					PluginsFragment.showInstance(activity.getSupportFragmentManager());
					return true;
				}));


		adapter.addItem(new ContextMenuItem(DRAWER_SETTINGS_ID)
				.setTitle(getString(R.string.shared_string_settings))
				.setIcon(R.drawable.ic_action_settings)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_settings_new_open");
					activity.getFragmentsHelper().showSettings();
					return true;
				}));


		return adapter;
	}

	@SuppressLint("ResourceAsColor")
	private ContextMenuAdapter createNormalOptionsMenu(OsmandApplication app, ContextMenuAdapter optionsMenuHelper, boolean nightMode) {

		createProfilesController(activity, optionsMenuHelper, nightMode, false);



		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_MY_PLACES_ID)
				.setTitleId(R.string.shared_string_my_places, activity)
				.setIcon(R.drawable.ic_action_favorite)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_myplaces_open");
					Intent newIntent = new Intent(activity, app.getAppCustomization()
							.getMyPlacesActivity());
					newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					activity.startActivity(newIntent);
					return true;
				}));

		addMyPlacesTabToDrawer(activity, optionsMenuHelper, R.string.shared_string_my_favorites,
				R.drawable.ic_action_folder_favorites, DRAWER_FAVORITES_ID);
		addMyPlacesTabToDrawer(activity, optionsMenuHelper, R.string.shared_string_tracks,
				R.drawable.ic_action_folder_tracks, DRAWER_TRACKS_ID);
		if (PluginsHelper.isActive(AudioVideoNotesPlugin.class)) {
			addMyPlacesTabToDrawer(activity, optionsMenuHelper, R.string.notes,
					R.drawable.ic_action_folder_av_notes, DRAWER_AV_NOTES_ID);
		}
		if (PluginsHelper.isActive(OsmEditingPlugin.class)) {
			addMyPlacesTabToDrawer(activity, optionsMenuHelper, R.string.osm_edits,
					R.drawable.ic_action_folder_osm_notes, DRAWER_OSM_EDITS_ID);
		}

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_SEARCH_ID)
				.setTitleId(R.string.search_button, activity)
				.setIcon(R.drawable.ic_action_search_dark)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_search_open");
					activity.getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);
					return true;
				}));

		OsmandMonitoringPlugin monitoringPlugin = PluginsHelper.getActivePlugin(OsmandMonitoringPlugin.class);
		if (monitoringPlugin != null) {
			optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_TRIP_RECORDING_ID)
					.setTitleId(R.string.map_widget_monitoring, activity)
					.setIcon(R.drawable.ic_action_track_recordable)
					.setListener((uiAdapter, view, item, isChecked) -> {
						app.logEvent("trip_recording_open");
						MapActivity.clearPrevActivityIntent();
						monitoringPlugin.askShowTripRecordingDialog(activity);
						return true;
					}));
		}

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_DIRECTIONS_ID)
				.setTitleId(R.string.shared_string_navigation, activity)
				.setIcon(R.drawable.ic_action_gdirections_dark)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_directions_open");
					activity.getMapActions().doRoute();
					return true;
				}));

		//Divider
		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_DIVIDER_ID)
				.setLayout(R.layout.drawer_divider));

		optionsMenuHelper.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.work_button, activity)
				.setIcon(R.drawable.mx_amenity_fire_station)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_intervention_navigate");
					FavouritesHelper favorites = app.getFavoritesHelper();
					String androidId = DeviceUtils.getAndroidId(app.getApplicationContext());
					String url = "https://67d97c3900348dd3e2ab4af0.mockapi.io/api/lieu_intervention/intervention?android_id=" + androidId; // Fake API
					ApiClient.sendGetRequest(url, new ApiClient.ApiCallback() {
						public void onSuccess(LatLon resultLatLon) {
							new Handler(Looper.getMainLooper()).post(() -> {
								try {
									List<FavouritePoint> pointFav = favorites.getGroup("personal").getPoints();
									FavouritePoint inter = null;
									for (int i = 0; i < pointFav.size(); i++) {
										String nom = pointFav.get(i).getName();
										if (nom == "Intervention") {
											inter = pointFav.get(i);
										}
									}
									LatLon coordonnees = new LatLon(inter.getLatitude(), inter.getLongitude());
									if (coordonnees != resultLatLon) {
										favorites.deleteFavourite(inter, false);
									}
								} catch (Exception e) {}
								app.showToastMessage("Récupération du lieu d'intervention.");
								favorites.setSpecialPoint(resultLatLon, SpecialPointType.WORK, "Intervention");
								activity.getMapActions().targetHelper.navigateToPoint(resultLatLon, false, 1);
								activity.getMapActions().startRoutePlanningWithDestination(resultLatLon, new PointDescription(resultLatLon.getLatitude(), resultLatLon.getLongitude()), new TargetPointsHelper(app));
//								activity.getMapActions().doNavigate();
							});
						}
						public void onError(Exception e) {
							new Handler(Looper.getMainLooper()).post(() -> {
								String message = String.valueOf(e);
								if (message.contains("java.net.UnknownHostException: Unable to resolve host")) {
									app.showToastMessage("Problème réseau ou serveur.");
								} else if (message.contains("java.lang.Exception: Erreur HTTP : 404")) {
									app.showToastMessage("Pas d'intervention en cours.");
								} else if (message.contains("org.json.JSONException: No value for")) {
									app.showToastMessage("Coordonnées non renseignées.");
								} else {
									app.showToastMessage(message);
								}
							});
						}
					});
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.shared_string_navigation_firehouse, activity)
				.setIcon(R.drawable.mx_emergency)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_firestation_open");
					MapControlsLayer mapControlsLayer = activity.getMapLayers().getMapControlsLayer();
					if (mapControlsLayer != null) {
						getMapActivity().getFragmentsHelper().showQuickSearch("Caserne de pompiers");
					}
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(null)
				.setTitleId(R.string.shared_string_navigation_hospital, activity)
				.setIcon(R.drawable.mm_healthcare)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_hospital_open");
					MapControlsLayer mapControlsLayer = activity.getMapLayers().getMapControlsLayer();
					if (mapControlsLayer != null) {
						getMapActivity().getFragmentsHelper().showQuickSearch("Hôpital");
					}
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_MEASURE_DISTANCE_ID)
				.setTitleId(R.string.shared_string_measure_distance, activity)
				.setIcon(R.drawable.ic_action_plan_route)
				.setListener((uiAdapter, view, item, isChecked) -> {
					StartPlanRouteBottomSheet.showInstance(activity.getSupportFragmentManager());
					return true;
				}));

		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_HELP_ID)
				.setTitleId(R.string.shared_string_help, activity)
				.setIcon(R.drawable.ic_action_help)
				.setListener((uiAdapter, view, item, isChecked) -> {
					app.logEvent("drawer_help_open");
					Intent intent = new Intent(activity, HelpActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					activity.startActivity(intent);
					return true;
				}));

		//////////// Others
		PluginsHelper.registerOptionsMenu(activity, optionsMenuHelper);

		optionsMenuHelper.addItem(createOsmAndVersionDrawerItem());

		return optionsMenuHelper;
	}

	private void createProfilesController(@NonNull MapActivity activity,
										  ContextMenuAdapter optionsMenuHelper, boolean nightMode, boolean listExpanded) {
		//switch profile button
		ApplicationMode currentMode = settings.APPLICATION_MODE.get();

		int icArrowResId = listExpanded ? R.drawable.ic_action_arrow_drop_up : R.drawable.ic_action_arrow_drop_down;
		int nextMode = listExpanded ? DRAWER_MODE_NORMAL : DRAWER_MODE_SWITCH_PROFILE;
		optionsMenuHelper.addItem(new ContextMenuItem(DRAWER_SWITCH_PROFILE_ID)
				.setLayout(R.layout.main_menu_drawer_sdis_header)
				.setListener((uiAdapter, view, item, isChecked) -> {
					drawerMode = nextMode;
					updateDrawerMenu();
					return false;
				}));
	}

	@NonNull
	private String getProfileDescription(@NonNull ApplicationMode mode) {
		return getProfileDescription(mode, routingDataUtils.getRoutingProfiles());
	}

	@NonNull
	private String getProfileDescription(@NonNull ApplicationMode mode,
										 @NonNull RoutingProfilesHolder profiles) {
		String type = getString(mode.isCustomProfile() ? R.string.profile_type_user_string : R.string.profile_type_osmand_string);
		return getProfileDescription(mode, profiles, type);
	}

	@NonNull
	private String getProfileDescription(@NonNull ApplicationMode mode,
										 @NonNull RoutingProfilesHolder profiles, @NonNull String defValue) {
		String description = defValue;
		String routingProfileKey = mode.getRoutingProfile();
		String derivedProfile = mode.getDerivedProfile();
		if (!Algorithms.isEmpty(routingProfileKey)) {
			ProfileDataObject profile = profiles.get(routingProfileKey, derivedProfile);
			if (profile != null) {
				description = String.format(app.getString(R.string.profile_type_descr_string),
						Algorithms.capitalizeFirstLetterAndLowercase(profile.getName()));
			}
		}
		return description;
	}

	private void addMyPlacesTabToDrawer(@NonNull MapActivity activity,
										@NonNull ContextMenuAdapter adapter, @StringRes int titleRes, @DrawableRes int iconRes,
										String drawerId) {
		adapter.addItem(new ContextMenuItem(drawerId)
				.setTitleId(titleRes, activity)
				.setIcon(iconRes)
				.setListener((uiAdapter, view, item, isChecked) -> {
					String itemLogName = drawerId.replace(DRAWER_ITEM_ID_SCHEME, "");
					app.logEvent("drawer_" + itemLogName + "_open");
					Intent intent = new Intent(activity, app.getAppCustomization()
							.getMyPlacesActivity());
					intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					settings.FAVORITES_TAB.set(titleRes);
					activity.startActivity(intent);
					return true;

				}));
	}

	@NonNull
	private ContextMenuItem createOsmAndVersionDrawerItem() {
		String osmAndVersion = Version.getFullVersion(app);
		String releasedString = getString(R.string.shared_string_release);
		String releaseDate = getString(R.string.app_edition);
		String releaseText = Algorithms.isEmpty(releaseDate)
				? null
				: app.getString(R.string.ltr_or_rtl_combine_via_colon, releasedString, releaseDate);

		return new ContextMenuItem(DRAWER_OSMAND_VERSION_ID)
				.setLayout(R.layout.main_menu_drawer_osmand_version)
				.setTitle(osmAndVersion)
				.setDescription(releaseText)
				.setListener((uiAdapter, view, item, isChecked) -> {
					String text = releaseText == null
							? osmAndVersion
							: app.getString(R.string.ltr_or_rtl_combine_via_comma, osmAndVersion, releaseText);
					ShareMenu.copyToClipboardWithToast(app, text, Toast.LENGTH_SHORT);
					return true;
				});
	}

	public void openIntermediatePointsDialog() {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			activity.hideContextAndRouteInfoMenues();
			WaypointsFragment.showInstance(activity.getSupportFragmentManager());
		}
	}

	public void stopNavigationActionConfirm(@Nullable OnDismissListener listener) {
		stopNavigationActionConfirm(listener, null);
	}

	public void stopNavigationActionConfirm(@Nullable OnDismissListener listener,
											@Nullable Runnable onStopAction) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			DismissRouteBottomSheetFragment.showInstance(activity.getSupportFragmentManager(), listener, onStopAction);
		}
	}

	public void whereAmIDialog() {
		MapActivity activity = getMapActivity();
		if (activity == null) {
			return;
		}
		List<String> items = new ArrayList<>();
		items.add(getString(R.string.show_location));
		items.add(getString(R.string.shared_string_show_details));
		AlertDialog.Builder menu = new AlertDialog.Builder(activity);
		menu.setItems(items.toArray(new String[0]), (dialog, item) -> {
			dialog.dismiss();
			switch (item) {
				case 0:
					mapTrackingUtilities.backToLocationImpl();
					break;
				case 1:
					OsmAndLocationProvider locationProvider = app.getLocationProvider();
					locationProvider.showNavigationInfo(activity.getPointToNavigate(), activity);
					break;
				default:
					break;
			}
		});
		menu.show();
	}

	public void updateDrawerMenu() {
		MapActivity activity = getMapActivity();
		if (activity == null) {
			return;
		}
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		ListView menuItemsListView = activity.findViewById(R.id.menuItems);
		menuItemsListView.setBackgroundColor(ColorUtilities.getListBgColor(activity, nightMode));
		if (drawerLogoHeader != null) {
			menuItemsListView.removeHeaderView(drawerLogoHeader);
			Bitmap navDrawerLogo = app.getAppCustomization().getNavDrawerLogo();
			if (navDrawerLogo != null) {
				drawerLogoHeader.setImageBitmap(navDrawerLogo);
				menuItemsListView.addHeaderView(drawerLogoHeader);
			}
		}
		menuItemsListView.setDivider(null);

		ContextMenuAdapter adapter = createMainOptionsMenu();
		ViewCreator viewCreator = new ViewCreator(activity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.simple_list_menu_item);
		ContextMenuListAdapter listAdapter = adapter.toListAdapter(activity, viewCreator);

		menuItemsListView.setAdapter(listAdapter);
		menuItemsListView.setOnItemClickListener((parent, view, position, id) -> {
			activity.getFragmentsHelper().dismissCardDialog();
			if(position != 0) {
				activity.getFragmentsHelper().dismissCardDialog();
				boolean hasHeader = menuItemsListView.getHeaderViewsCount() > 0;
				boolean hasFooter = menuItemsListView.getFooterViewsCount() > 0;
				if (hasHeader && position == 0 || (hasFooter && position == menuItemsListView.getCount() - 1)) {
					String drawerLogoParams = app.getAppCustomization().getNavDrawerLogoUrl();
					if (!Algorithms.isEmpty(drawerLogoParams)) {
						AndroidUtils.openUrl(activity, Uri.parse(drawerLogoParams), nightMode);
					}
				} else {
					position -= menuItemsListView.getHeaderViewsCount();
					ContextMenuItem item = adapter.getItem(position);
					ItemClickListener click = item.getItemClickListener();
					if (click != null && click.onContextMenuClick(listAdapter, view, item, false)) {
						activity.closeDrawer();
					}
				}
			}
		});

		menuItemsListView.setOnItemLongClickListener((parent, view, position, id) -> {
			if(position == 0) {
				activity.getFragmentsHelper().dismissCardDialog();
				boolean hasHeader = menuItemsListView.getHeaderViewsCount() > 0;
				boolean hasFooter = menuItemsListView.getFooterViewsCount() > 0;
				if (hasHeader && position == 0 || (hasFooter && position == menuItemsListView.getCount() - 1)) {
					String drawerLogoParams = app.getAppCustomization().getNavDrawerLogoUrl();
					if (!Algorithms.isEmpty(drawerLogoParams)) {
						AndroidUtils.openUrl(activity, Uri.parse(drawerLogoParams), nightMode);
					}
				} else {
					position -= menuItemsListView.getHeaderViewsCount();
					ContextMenuItem item = adapter.getItem(position);
					ItemClickListener click = item.getItemClickListener();
					if (click != null && click.onContextMenuClick(listAdapter, view, item, false)) {
						activity.closeDrawer();
					}
				}
			}
			return true;
		});
	}
}
