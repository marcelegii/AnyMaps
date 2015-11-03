/*
 * Copyright (c) 2015 Daimler AG / Moovel GmbH
 *
 * All rights reserved
 */

package net.doo.maps.baidu;

import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;

import net.doo.maps.AnyMap;
import net.doo.maps.CameraUpdate;
import net.doo.maps.Projection;
import net.doo.maps.UiSettings;
import net.doo.maps.baidu.overlay.BaiduPolygon;
import net.doo.maps.baidu.overlay.Converter;
import net.doo.maps.baidu.overlay.OutConverter;
import net.doo.maps.model.CameraPosition;
import net.doo.maps.model.Circle;
import net.doo.maps.model.CircleOptions;
import net.doo.maps.model.LatLng;
import net.doo.maps.model.LatLngBounds;
import net.doo.maps.model.Marker;
import net.doo.maps.model.MarkerOptions;
import net.doo.maps.model.Polygon;
import net.doo.maps.model.PolygonOptions;
import net.doo.maps.model.Polyline;
import net.doo.maps.model.PolylineOptions;
import net.doo.maps.model.VisibleRegion;

/**
 * Implementation of {@link AnyMap} which works with Open Street Maps
 */
public class BaiduMap implements AnyMap {

	private final MapView mapView;
	private final com.baidu.mapapi.map.BaiduMap map;
	private final CameraUpdateHandler cameraUpdateHandler;

	BaiduMap(MapView mapView) {
		this.mapView = mapView;

		map = mapView.getMap();
		map.getUiSettings().setCompassEnabled(false);
		map.getUiSettings().setRotateGesturesEnabled(false);

		cameraUpdateHandler = new CameraUpdateHandler(map);
	}

	@Override
	public void moveCamera(CameraUpdate cameraUpdate) {
		cameraUpdateHandler.animateMapStatus((BaiduCameraUpdate) cameraUpdate, false, null);
	}

	@Override
	public void animateCamera(CameraUpdate cameraUpdate) {
		cameraUpdateHandler.animateMapStatus((BaiduCameraUpdate) cameraUpdate, true, null);
	}

	@Override
	public void animateCamera(CameraUpdate cameraUpdate, CancelableCallback callback) {
		cameraUpdateHandler.animateMapStatus((BaiduCameraUpdate) cameraUpdate, true, null);
		callback.onFinish();
	}

	@Override
	public void animateCamera(CameraUpdate cameraUpdate, int duration, CancelableCallback callback) {
		cameraUpdateHandler.animateMapStatus((BaiduCameraUpdate) cameraUpdate, true, duration);
		callback.onFinish();
	}

	@Override
	public CameraPosition getCameraPosition() {
		return cameraUpdateHandler.currentCameraPosition();
	}

	@Override
	public Projection getProjection() {
		return new BaiduProjection(
				new VisibleRegion(
						new LatLngBounds(
								OutConverter.convert(map.getMapStatus().bound.southwest),
								OutConverter.convert(map.getMapStatus().bound.northeast)
						)
				)
		);
	}

	@Override
	public Marker addMarker(MarkerOptions options) {
		com.baidu.mapapi.map.Marker marker = (com.baidu.mapapi.map.Marker) map.addOverlay(Converter.convert(options));
		return OutConverter.convert(marker);
	}

	@Override
	public Circle addCircle(CircleOptions options) {
		com.baidu.mapapi.map.Circle circle = (com.baidu.mapapi.map.Circle) map.addOverlay(Converter.convert(options));
		return OutConverter.convert(circle);
	}

	@Override
	public Polygon addPolygon(PolygonOptions options) {
		if (options.isOutsider()) {
			// return empty polygon
			// it is used for holes later
			return new BaiduPolygon(map);
		}

		com.baidu.mapapi.map.Polygon polygon = (com.baidu.mapapi.map.Polygon) map.addOverlay(Converter.convert(options));
		// draw it on top of "outsider" polygon
		polygon.setZIndex(1);
		return OutConverter.convert(polygon);
	}

	@Override
	public Polyline addPolyline(final PolylineOptions options) {
		com.baidu.mapapi.map.Polyline polyline = (com.baidu.mapapi.map.Polyline) map.addOverlay(Converter.convert(options));
		return OutConverter.convert(polyline);
	}

	@Override
	public UiSettings getUiSettings() {
		return new UiSettings() {
			@Override
			public void setAllGesturesEnabled(boolean enabled) {
				map.getUiSettings().setAllGesturesEnabled(enabled);
			}

			@Override
			public void setMyLocationButtonEnabled(boolean enabled) {
				map.setMyLocationEnabled(enabled);
			}

			@Override
			public void setMapToolbarEnabled(boolean enabled) {
				// Do nothing
			}
		};
	}

	@Override
	public void setOnMapClickListener(final OnMapClickListener listener) {
		map.setOnMapClickListener(new com.baidu.mapapi.map.BaiduMap.OnMapClickListener() {
			@Override
			public void onMapClick(com.baidu.mapapi.model.LatLng latLng) {
				listener.onMapClick(convert(latLng));
			}

			@Override
			public boolean onMapPoiClick(MapPoi mapPoi) {
				return false;
			}
		});
	}

	@Override
	public void setOnMapLongClickListener(final OnMapLongClickListener listener) {
		map.setOnMapLongClickListener(new com.baidu.mapapi.map.BaiduMap.OnMapLongClickListener() {
			@Override
			public void onMapLongClick(com.baidu.mapapi.model.LatLng latLng) {
				listener.onMapLongClick(convert(latLng));
			}
		});
	}

	private LatLng convert(com.baidu.mapapi.model.LatLng latLng) {
		return new LatLng(latLng.latitude, latLng.longitude);
	}

	@Override
	public void setOnCameraChangeListener(final OnCameraChangeListener listener) {
		map.setOnMapStatusChangeListener(new com.baidu.mapapi.map.BaiduMap.OnMapStatusChangeListener() {
			@Override
			public void onMapStatusChangeStart(MapStatus mapStatus) {
				listener.onCameraChange(OutConverter.convert(mapStatus));
			}

			@Override
			public void onMapStatusChange(MapStatus mapStatus) {
				listener.onCameraChange(OutConverter.convert(mapStatus));
			}

			@Override
			public void onMapStatusChangeFinish(MapStatus mapStatus) {
				listener.onCameraChange(OutConverter.convert(mapStatus));
			}
		});
	}

	@Override
	public void setOnMarkerClickListener(final OnMarkerClickListener listener) {
		map.setOnMarkerClickListener(new com.baidu.mapapi.map.BaiduMap.OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(com.baidu.mapapi.map.Marker marker) {
				listener.onMarkerClick(OutConverter.convert(marker));
				return true;
			}
		});
	}

	@Override
	public void setInfoWindowAdapter(InfoWindowAdapter adapter) {
		// Do nothing
	}

	@Override
	public void setTrafficEnabled(boolean enabled) {
		map.setTrafficEnabled(enabled);
	}

	@Override
	public void setMyLocationEnabled(boolean enabled) {
		map.setMyLocationEnabled(enabled);
	}

	@Override
	public void setMapType(Type type) {
		switch (type) {
			case SATELLITE:
				map.setMapType(com.baidu.mapapi.map.BaiduMap.MAP_TYPE_SATELLITE);
				break;
			case NORMAL:
			default:
				map.setMapType(com.baidu.mapapi.map.BaiduMap.MAP_TYPE_NORMAL);
				break;
		}
	}

	@Override
	public void setPadding(int left, int top, int right, int bottom) {
		mapView.setPadding(left, top, right, bottom);
	}

	/**
	 * @return native MapView reference
	 */
	MapView getNativeMapView() {
		return mapView;
	}

	@Override
	public void onUserLocationChanged(LatLng location, float accuracy) {
		map.setMyLocationData(new MyLocationData.Builder().latitude(location.latitude).longitude(location.longitude).accuracy(accuracy).build());
	}

}
