package net.osmand.plus.settings.backend;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.helpers.AvoidSpecificRoads;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

class ImpassableRoadsStorage extends SettingsMapPointsStorage {

	private OsmandSettings osmandSettings;
	protected String roadsIdsKey;
	protected String appModeKey;

	public ImpassableRoadsStorage(OsmandSettings osmandSettings) {
		this.osmandSettings = osmandSettings;
		pointsKey = OsmandSettings.IMPASSABLE_ROAD_POINTS;
		descriptionsKey = OsmandSettings.IMPASSABLE_ROADS_DESCRIPTIONS;
		roadsIdsKey = OsmandSettings.IMPASSABLE_ROADS_IDS;
		appModeKey = OsmandSettings.IMPASSABLE_ROADS_APP_MODE_KEYS;
	}

	public List<Long> getRoadIds(int size) {
		List<Long> list = new ArrayList<>();
		String roadIds = osmandSettings.settingsAPI.getString(osmandSettings.globalPreferences, roadsIdsKey, "");
		if (roadIds.trim().length() > 0) {
			StringTokenizer tok = new StringTokenizer(roadIds, ",");
			while (tok.hasMoreTokens() && list.size() <= size) {
				list.add(Long.parseLong(tok.nextToken()));
			}
		}
		while (list.size() < size) {
			list.add(0L);
		}
		return list;
	}

	public List<String> getAppModeKeys(int size) {
		List<String> list = new ArrayList<>();
		String roadIds = osmandSettings.settingsAPI.getString(osmandSettings.globalPreferences, appModeKey, "");
		if (roadIds.trim().length() > 0) {
			StringTokenizer tok = new StringTokenizer(roadIds, ",");
			while (tok.hasMoreTokens() && list.size() <= size) {
				list.add(tok.nextToken());
			}
		}
		while (list.size() < size) {
			list.add("");
		}
		return list;
	}

	public List<AvoidSpecificRoads.AvoidRoadInfo> getImpassableRoadsInfo() {
		List<LatLon> points = getPoints();
		List<Long> roadIds = getRoadIds(points.size());
		List<String> appModeKeys = getAppModeKeys(points.size());
		List<String> descriptions = getPointDescriptions(points.size());

		List<AvoidSpecificRoads.AvoidRoadInfo> avoidRoadsInfo = new ArrayList<>();

		for (int i = 0; i < points.size(); i++) {
			LatLon latLon = points.get(i);
			PointDescription description = PointDescription.deserializeFromString(descriptions.get(i), null);

			AvoidSpecificRoads.AvoidRoadInfo avoidRoadInfo = new AvoidSpecificRoads.AvoidRoadInfo();
			avoidRoadInfo.id = roadIds.get(i);
			avoidRoadInfo.latitude = latLon.getLatitude();
			avoidRoadInfo.longitude = latLon.getLongitude();
			avoidRoadInfo.name = description.getName();
			avoidRoadInfo.appModeKey = appModeKeys.get(i);
			avoidRoadsInfo.add(avoidRoadInfo);
		}

		return avoidRoadsInfo;
	}

	public boolean addImpassableRoadInfo(AvoidSpecificRoads.AvoidRoadInfo avoidRoadInfo) {
		List<LatLon> points = getPoints();
		List<Long> roadIds = getRoadIds(points.size());
		List<String> appModeKeys = getAppModeKeys(points.size());
		List<String> descriptions = getPointDescriptions(points.size());

		roadIds.add(0, avoidRoadInfo.id);
		points.add(0, new LatLon(avoidRoadInfo.latitude, avoidRoadInfo.longitude));
		appModeKeys.add(0, avoidRoadInfo.appModeKey);
		descriptions.add(0, PointDescription.serializeToString(new PointDescription("", avoidRoadInfo.name)));

		return saveAvoidRoadData(points, descriptions, roadIds, appModeKeys);
	}

	public boolean updateImpassableRoadInfo(AvoidSpecificRoads.AvoidRoadInfo avoidRoadInfo) {
		List<LatLon> points = getPoints();

		int index = points.indexOf(new LatLon(avoidRoadInfo.latitude, avoidRoadInfo.longitude));
		if (index != -1) {
			List<Long> roadIds = getRoadIds(points.size());
			List<String> appModeKeys = getAppModeKeys(points.size());
			List<String> descriptions = getPointDescriptions(points.size());

			roadIds.set(index, avoidRoadInfo.id);
			appModeKeys.set(index, avoidRoadInfo.appModeKey);
			descriptions.set(index, PointDescription.serializeToString(new PointDescription("", avoidRoadInfo.name)));
			return saveAvoidRoadData(points, descriptions, roadIds, appModeKeys);
		}
		return false;
	}

	@Override
	public boolean deletePoint(int index) {
		List<LatLon> points = getPoints();
		List<Long> roadIds = getRoadIds(points.size());
		List<String> appModeKeys = getAppModeKeys(points.size());
		List<String> descriptions = getPointDescriptions(points.size());

		if (index < points.size()) {
			points.remove(index);
			roadIds.remove(index);
			appModeKeys.remove(index);
			descriptions.remove(index);
			return saveAvoidRoadData(points, descriptions, roadIds, appModeKeys);
		}
		return false;
	}

	@Override
	public boolean deletePoint(LatLon latLon) {
		List<LatLon> points = getPoints();
		List<Long> roadIds = getRoadIds(points.size());
		List<String> appModeKeys = getAppModeKeys(points.size());
		List<String> descriptions = getPointDescriptions(points.size());

		int index = points.indexOf(latLon);
		if (index != -1) {
			points.remove(index);
			roadIds.remove(index);
			appModeKeys.remove(index);
			descriptions.remove(index);
			return saveAvoidRoadData(points, descriptions, roadIds, appModeKeys);
		}
		return false;
	}

	@Override
	public boolean movePoint(LatLon latLonEx, LatLon latLonNew) {
		List<LatLon> points = getPoints();
		List<Long> roadIds = getRoadIds(points.size());
		List<String> appModeKeys = getAppModeKeys(points.size());
		List<String> descriptions = getPointDescriptions(points.size());

		int i = points.indexOf(latLonEx);
		if (i != -1) {
			points.set(i, latLonNew);
			return saveAvoidRoadData(points, descriptions, roadIds, appModeKeys);
		} else {
			return false;
		}
	}

	public boolean saveAvoidRoadData(List<LatLon> points, List<String> descriptions,
									 List<Long> roadIds, List<String> appModeKeys) {
		return savePoints(points, descriptions) && saveRoadIds(roadIds) && saveAppModeKeys(appModeKeys);
	}

	public boolean saveRoadIds(List<Long> roadIds) {
		StringBuilder stringBuilder = new StringBuilder();
		Iterator<Long> iterator = roadIds.iterator();
		while (iterator.hasNext()) {
			stringBuilder.append(iterator.next());
			if (iterator.hasNext()) {
				stringBuilder.append(",");
			}
		}
		return osmandSettings.settingsAPI.edit(osmandSettings.globalPreferences)
				.putString(roadsIdsKey, stringBuilder.toString())
				.commit();
	}

	public boolean saveAppModeKeys(List<String> appModeKeys) {
		StringBuilder stringBuilder = new StringBuilder();
		Iterator<String> iterator = appModeKeys.iterator();
		while (iterator.hasNext()) {
			stringBuilder.append(iterator.next());
			if (iterator.hasNext()) {
				stringBuilder.append(",");
			}
		}
		return osmandSettings.settingsAPI.edit(osmandSettings.globalPreferences)
				.putString(appModeKey, stringBuilder.toString())
				.commit();
	}
}
