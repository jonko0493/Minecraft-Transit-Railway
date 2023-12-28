package mtr.packet;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import mtr.data.AreaBase;
import mtr.data.IGui;
import mtr.data.RailwayData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.Set;

public class UpdateBlueMap implements IGui, IUpdateWebMap {

	public static void setUpIcons(BlueMapMap map, ServerLevel serverWorld, String iconKey, String iconPath) {
		try  {
			OutputStream iconStream = map.getAssetStorage().writeAsset(iconKey + ".png");
			IUpdateWebMap.readResource(iconPath, inputStream -> {
				try {
					BufferedImage icon = ImageIO.read(inputStream);
					BufferedImage resizedIcon = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g2d = resizedIcon.createGraphics();
					g2d.drawImage(icon, 0, 0, ICON_SIZE, ICON_SIZE, null);

					ImageIO.write(resizedIcon, "png", iconStream);
					iconStream.flush();
					iconStream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			final RailwayData railwayData = RailwayData.getInstance(serverWorld);
			UpdateBlueMap.updateBlueMap(serverWorld, railwayData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void updateBlueMap(Level world, RailwayData railwayData) {
		try {
			updateBlueMap(world, railwayData.stations, MARKER_SET_STATIONS_ID, MARKER_SET_STATIONS_TITLE, MARKER_SET_STATION_AREAS_ID, MARKER_SET_STATION_AREAS_TITLE, STATION_ICON_KEY);
			updateBlueMap(world, railwayData.depots, MARKER_SET_DEPOTS_ID, MARKER_SET_DEPOTS_TITLE, MARKER_SET_DEPOT_AREAS_ID, MARKER_SET_DEPOT_AREAS_TITLE, DEPOT_ICON_KEY);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static <T extends AreaBase> void updateBlueMap(Level world, Set<T> areas, String poisId, String poisTitle, String areasId, String areasTitle, String iconKey) {
		final BlueMapAPI api = BlueMapAPI.getInstance().orElse(null);
		if (api == null) {
			return;
		}

		final String worldId = world.dimension().location().getPath();
		final BlueMapMap map = api.getMaps().stream().filter(map1 -> worldId.contains(map1.getId())).findFirst().orElse(null);
		if (map == null) {
			return;
		}

		final String iconUrl = map.getAssetStorage().getAssetUrl(iconKey + ".png");
		final int areaY = world.getSeaLevel();

		final MarkerSet markerSetPOIs = MarkerSet.builder().label(poisTitle).build();
		markerSetPOIs.getMarkers().clear();
		map.getMarkerSets().put(poisId, markerSetPOIs);

		final MarkerSet markerSetAreas = MarkerSet.builder().label(areasTitle).defaultHidden(true).build();
		markerSetAreas.getMarkers().clear();
		map.getMarkerSets().put(areasId, markerSetAreas);

		IUpdateWebMap.iterateAreas(areas, (id, name, color, areaCorner1X, areaCorner1Z, areaCorner2X, areaCorner2Z, areaX, areaZ) -> {
			final POIMarker markerPOI = POIMarker.builder()
					.label(name)
					.position(areaX, areaY, (double)areaZ)
					.icon(iconUrl, ICON_SIZE / 2, ICON_SIZE / 2)
					.build();
			markerSetPOIs.getMarkers().put("1_" + worldId + id, markerPOI);
			final ShapeMarker markerArea = ShapeMarker.builder()
					.shape(Shape.createRect(areaCorner1X, areaCorner1Z, areaCorner2X, areaCorner2Z), areaY)
					.centerPosition()
					.label(name)
					.fillColor(new Color(color.getRGB() & RGB_WHITE, 0.5F))
					.lineColor(new Color(color.darker().getRGB()))
					.build();
			markerSetAreas.getMarkers().put("2_" + worldId + id, markerArea);
		});
	}
}
