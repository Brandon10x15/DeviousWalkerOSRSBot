package devious_walker.region;

import devious_walker.ConfigManager;
import devious_walker.Reachable;
import devious_walker.enums.WalkerVarbits;
import devious_walker.pathfinder.GlobalCollisionMap;
import devious_walker.pathfinder.TeleportLoader;
import devious_walker.pathfinder.TransportLoader;
import devious_walker.pathfinder.Walker;
import devious_walker.pathfinder.model.Teleport;
import devious_walker.pathfinder.model.Transport;
import devious_walker.pathfinder.model.poh.HousePortal;
import devious_walker.pathfinder.model.poh.JewelryBox;
import devious_walker.quests.QuestVarbits;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.InventoryID;
import net.runelite.api.Tile;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.rsb.methods.MethodContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.runelite.rsb.methods.MethodProvider.methods;

@Slf4j
@Singleton
public class RegionManager {
    @Getter
    public static GlobalCollisionMap globalCollisionMap;

    @Getter
    public static RegionManager singleton;
    private final MethodContext ctx;

    public RegionManager(MethodContext ctx) {
        this.ctx = ctx;
        try {
            globalCollisionMap = GlobalCollisionMap.fetchFromUrl("/regions");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        init(ctx);
    }

    private static final Set<Integer> REFRESH_WIDGET_IDS = Set.of(WidgetInfo.QUEST_COMPLETED_NAME_TEXT.getGroupId(), WidgetInfo.LEVEL_UP_LEVEL.getGroupId());
    private static final Set<String> pathfinderConfigKeys = Set.of("useTransports", "useTeleports", "avoidWilderness", "usePoh", "hasMountedGlory", "hasMountedDigsitePendant", "hasMountedMythicalCape", "hasMountedXericsTalisman", "hasJewelryBox", "housePortals", "useEquipmentJewellery", "useMinigameTeleports");

    private static final Set<Integer> REFRESH_VARBITS = Set.of(
            // Motherlode mine shortcut (54 agility)
            QuestVarbits.ACHIEVEMENT_DIARY_FALADOR_MEDIUM.getId(),

            // Draynor village wall shortcut (42 agility)
            QuestVarbits.ACHIEVEMENT_DIARY_LUMBRIDGE_MEDIUM.getId(),

            // Big windows shortcut in Al Kharid Palace (70 agility)
            QuestVarbits.ACHIEVEMENT_DIARY_DESERT_HARD.getId(),

            // Constructible darkmeyer shortcut on the eastern wall (63 agility)
            WalkerVarbits.DARKMEYER_EAST_WALL_SHORTCUT_1, WalkerVarbits.DARKMEYER_EAST_WALL_SHORTCUT_2,

            // Digsite gate might be accessible when value is >=153
            WalkerVarbits.KUDOS,

            // Zeah transport dialog is different first time talking to Veos
            WalkerVarbits.VEOS_HAS_TALKED_TO_BEFORE,
            // Zeah transport dialog is different when getting to a certain point in X Marks the spot (progress varbit >= 7)
            QuestVarbits.QUEST_X_MARKS_THE_SPOT.getId(),

            // Door states change depending on the lever states
            WalkerVarbits.DRAYNOR_MANOR_BASEMENT_DOOR_1_STATE, WalkerVarbits.DRAYNOR_MANOR_BASEMENT_DOOR_2_STATE, WalkerVarbits.DRAYNOR_MANOR_BASEMENT_DOOR_3_STATE, WalkerVarbits.DRAYNOR_MANOR_BASEMENT_DOOR_4_STATE, WalkerVarbits.DRAYNOR_MANOR_BASEMENT_DOOR_5_STATE, WalkerVarbits.DRAYNOR_MANOR_BASEMENT_DOOR_6_STATE, WalkerVarbits.DRAYNOR_MANOR_BASEMENT_DOOR_7_STATE, WalkerVarbits.DRAYNOR_MANOR_BASEMENT_DOOR_8_STATE, WalkerVarbits.DRAYNOR_MANOR_BASEMENT_DOOR_9_STATE

    );

    private static boolean REFRESH_PATH = false;
    private static boolean INITIAL_LOGIN = true;

    @Inject
    private ScheduledExecutorService executorService;

    public static boolean avoidWilderness()
    {
        return ConfigManager.avoidWilderness;
    }

    public static boolean shouldRefreshPath()
    {
        boolean refreshPath = REFRESH_PATH;
        REFRESH_PATH = false;
        return refreshPath;
    }

    public static boolean useEquipmentJewellery()
    {
        return ConfigManager.useEquipmentJewellery;
    }

    public static boolean usePoh()
    {
        return ConfigManager.usePoh;
    }

    public static boolean hasMountedGlory()
    {
        return ConfigManager.hasMountedGlory;
    }

    public static boolean hasMountedDigsitePendant()
    {
        return ConfigManager.hasMountedDigsitePendant;
    }

    public static boolean hasMountedMythicalCape()
    {
        return ConfigManager.hasMountedMythicalCape;
    }

    public static boolean hasMountedXericsTalisman()
    {
        return ConfigManager.hasMountedXericsTalisman;
    }

    public static JewelryBox hasJewelryBox()
    {
        return ConfigManager.hasJewelryBox;
    }

    public static boolean useMinigameTeleports()
    {
        return ConfigManager.useMinigameTeleports;
    }

    public static Set<HousePortal> getHousePortals()
    {
        return ConfigManager.housePortals;
    }

    public void init(MethodContext ctx) {
        singleton = this;
        singleton.executorService.submit(new TransportLoader(ctx).init());
        ctx.runeLite.eventBus.register(singleton);
        TeleportLoader.refreshTeleports(ctx);
        TransportLoader.refreshTransports(ctx);
    }

    @Subscribe(priority = Integer.MAX_VALUE)
    public void onGameStateChanged(MethodContext ctx, GameStateChanged event) {
        switch (event.getGameState()) {
            case UNKNOWN:
            case STARTING:
            case LOGIN_SCREEN:
            case LOGIN_SCREEN_AUTHENTICATOR:
            case CONNECTION_LOST:
                INITIAL_LOGIN = true;
                break;
            case LOGGED_IN:
                if (INITIAL_LOGIN)
                {
                    INITIAL_LOGIN = false;
                    executorService.schedule(() -> {
                        REFRESH_PATH = true;
                        TeleportLoader.refreshTeleports(ctx);
                        TransportLoader.refreshTransports(ctx);
                    }, 1000, TimeUnit.MILLISECONDS);
                }
        }
    }

    static public boolean isTransport(List<Transport> transports, WorldPoint from, WorldPoint to)
    {
        if (transports == null)
        {
            return false;
        }

        return transports.stream().anyMatch(t -> t.getSource().equals(from) && t.getDestination().equals(to));
    }

    @Subscribe(priority = Integer.MAX_VALUE)
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (REFRESH_WIDGET_IDS.contains(event.getGroupId()))
        {
            REFRESH_PATH = true;
        }
    }

    @Subscribe(priority = Integer.MAX_VALUE)
    public void onVarbitChanged(VarbitChanged event)
    {
        if (REFRESH_VARBITS.contains(event.getVarbitId()))
        {
            log.debug("Path refresh triggered by varbit {} change", event.getVarbitId());
            REFRESH_PATH = true;
        }
    }

    @Subscribe(priority = Integer.MAX_VALUE)
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (event.getContainerId() == InventoryID.INVENTORY.getId() || event.getContainerId() == InventoryID.EQUIPMENT.getId())
        {
            REFRESH_PATH = true;
        }
    }

    /*
    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals(UnethicaliteConfig.CONFIG_GROUP))
        {
            return;
        }
        if (pathfinderConfigKeys.contains(event.getKey()))
        {
            REFRESH_PATH = true;
            if (methods.game.isLoggedIn())
            {
                TransportLoader.refreshTransports();
                TeleportLoader.refreshTeleports();
            }
        }
    }

     */

    public boolean hasChanged(MethodContext ctx) {
        boolean tranChanged = transportsChanged(ctx);
        boolean teleChanged = teleportsChanged(ctx);

        boolean changed = tranChanged || teleChanged;

        if (changed) {
            log.debug("Transports/teleports changed!");
        }

        return changed;
    }

    private boolean transportsChanged(MethodContext ctx) {
        List<WorldPoint> path = Walker.remainingPath(ctx, Walker.buildPath(ctx));

        if (path.isEmpty()) {
            TransportLoader.refreshTransports(ctx);
            return false;
        }

        Map<WorldPoint, List<Transport>> previousTransports = Walker.buildTransportLinksOnPath(path);
        TransportLoader.refreshTransports(ctx);
        Map<WorldPoint, List<Transport>> currentTransports = Walker.buildTransportLinksOnPath(path);

        for (WorldPoint point : path)
        {
            List<Transport> prevTran = previousTransports.getOrDefault(point, new ArrayList<>());
            List<Transport> currTran = currentTransports.getOrDefault(point, new ArrayList<>());
            if (!prevTran.equals(currTran))
            {
                return true;
            }
        }

        return false;
    }

    private boolean teleportsChanged(MethodContext ctx) {
        List<WorldPoint> path = Walker.remainingPath(ctx, Walker.buildPath(ctx));

        if (path.isEmpty()) {
            TeleportLoader.refreshTeleports(ctx);
            return false;
        }

        LinkedHashMap<WorldPoint, Teleport> previousTeleports = Walker.buildTeleportLinksOnPath(ctx, path);
        TeleportLoader.refreshTeleports(ctx);
        LinkedHashMap<WorldPoint, Teleport> currentTeleports = Walker.buildTeleportLinksOnPath(ctx, path);

        for (WorldPoint point : path) {
            Teleport prevTele = previousTeleports.getOrDefault(point, null);
            Teleport currTele = currentTeleports.getOrDefault(point, null);
            if ((prevTele == null && currTele != null) || (prevTele != null && currTele == null)) {
                return true;
            }
        }

        return false;
    }

    public static List<TileFlag> getTileFlags()
    {
        CollisionData[] col = methods.client.getCollisionMaps();
        if (col == null)
        {
            return new ArrayList<>();
        }

        List<TileFlag> tileFlags = new ArrayList<>();
        Map<WorldPoint, List<Transport>> transportLinks = Walker.buildTransportLinks();
        int plane = methods.client.getPlane();
        CollisionData data = col[plane];
        if (data == null)
        {
            return new ArrayList<>();
        }

        int[][] flags = data.getFlags();
        for (int x = 0; x < flags.length; x++)
        {
            for (int y = 0; y < flags.length; y++)
            {
                int tileX = x + methods.client.getBaseX();
                int tileY = y + methods.client.getBaseY();
                int flag = flags[x][y];

                // Stop if we reach any tiles which dont have collision data loaded
                // Usually occurs for tiles which are loaded in the 104x104 scene, but are outside the region
                if (flag == 0xFFFFFF)
                {
                    continue;
                }

                int regionId = ((tileX >> 6) << 8) | (tileY >> 6);

                // Set the full block flag in case tiles are null (ex. on upper levels)
                TileFlag tileFlag = new TileFlag(tileX, tileY, plane, CollisionDataFlag.BLOCK_MOVEMENT_FULL, regionId);
                Tile tile = methods.tiles.getTile(methods, tileX, tileY, plane);
                if (tile == null)
                {
                    tileFlags.add(tileFlag);
                    continue;
                }

                tileFlag.setFlag(flag);
                WorldPoint tileCoords = tile.getWorldLocation();

                // Check if we are blocked by objects
                // We don't need to parse west/south because they're checked by parsing adjacent tiles for north/east
                // We also skip the current tile if an adjacent tile does not have their flags loaded
                WorldPoint northernTile = tileCoords.dy(1);
                if (Reachable.getCollisionFlag(northernTile) == 0xFFFFFF)
                {
                    continue;
                }

                if (Reachable.isObstacle(northernTile) && !Reachable.isWalled(Direction.NORTH, tileFlag.getFlag()))
                {
                    tileFlag.setFlag(tileFlag.getFlag() + CollisionDataFlag.BLOCK_MOVEMENT_NORTH);
                }

                WorldPoint easternTile = tileCoords.dx(1);
                if (Reachable.getCollisionFlag(easternTile) == 0xFFFFFF)
                {
                    continue;
                }

                if (Reachable.isObstacle(easternTile) && !Reachable.isWalled(Direction.EAST, tileFlag.getFlag()))
                {
                    tileFlag.setFlag(tileFlag.getFlag() + CollisionDataFlag.BLOCK_MOVEMENT_EAST);
                }

                List<Transport> transports = transportLinks.get(tileCoords);
                if (plane == methods.client.getPlane())
                {
                    for (Direction direction : Direction.values())
                    {
                        switch (direction)
                        {
                            case NORTH:
                                if ((Reachable.hasDoor(tile, direction) || Reachable.hasDoor(northernTile, Direction.SOUTH)) && !isTransport(transports, tileCoords, northernTile))
                                {
                                    tileFlag.setFlag(tileFlag.getFlag() - CollisionDataFlag.BLOCK_MOVEMENT_NORTH);
                                }
                                break;
                            case EAST:
                                if ((Reachable.hasDoor(tile, direction) || Reachable.hasDoor(easternTile, Direction.WEST)) && !isTransport(transports, tileCoords, easternTile))
                                {
                                    tileFlag.setFlag(tileFlag.getFlag() - CollisionDataFlag.BLOCK_MOVEMENT_EAST);
                                }
                                break;
                        }
                    }
                }

                tileFlags.add(tileFlag);
            }
        }
        return tileFlags;
    }
}
