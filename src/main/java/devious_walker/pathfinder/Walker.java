package devious_walker.pathfinder;

import devious_walker.ConfigManager;
import devious_walker.DeviousWalker;
import devious_walker.Reachable;
import devious_walker.pathfinder.model.Teleport;
import devious_walker.pathfinder.model.Transport;
import devious_walker.region.RegionManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.CollisionData;
import net.runelite.api.NpcID;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.rsb.methods.MethodContext;
import net.runelite.rsb.util.StdRandom;
import net.runelite.rsb.wrappers.*;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static net.runelite.rsb.methods.Methods.sleep;

@Singleton
@Slf4j
public class Walker
{
    public final int MAX_INTERACT_DISTANCE = 20;
    private static final int MIN_TILES_WALKED_IN_STEP = 7;
    private static final int MAX_TILES_WALKED_IN_STEP = 20;
    private static final int MAX_MIN_ENERGY = 50;
    private static final int MIN_ENERGY = 5;
    private static final int MAX_NEAREST_SEARCH_ITERATIONS = 10;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static Future<List<WorldPoint>> pathFuture = null;
    private static WorldArea currentDestination = null;
    private final MethodContext ctx;

    public Walker(MethodContext ctx) {
        this.ctx = ctx;
    }

    public boolean walkTo(WorldPoint destination) {
        return walkTo(ctx, destination.toWorldArea());
    }

    public boolean walkTo(WorldArea destination) {
        log.debug("Walking to " + destination.getX() + ", " + destination.getY());
        RSPlayer local = ctx.players.getMyPlayer();
        WorldPoint playerPoint = WorldPoint.fromLocalInstance(ctx.client, local.getLocation().getLocalLocation());

        if (destination.contains(playerPoint)) {
            currentDestination = null;
            return true;
        }

        if (ctx.npcChat.isCutsceneActive() || ctx.interfaces.getComponent(299, 0).isVisible()) {
            sleep(1200);
            return false;
        }

        Map<WorldPoint, List<Transport>> transports = buildTransportLinks();
        LinkedHashMap<WorldPoint, Teleport> teleports = buildTeleportLinks(ctx, destination);
        List<WorldPoint> path = buildPath(ctx, destination);

        // TODO:
        //Static.getEntityRenderer().setCurrentPath(path);

        if (path == null || path.isEmpty()) {
            log.error(path == null ? "Path is null" : "Path is empty");
            return false;
        }

        List<WorldPoint> instancedPath = path.stream().map(point -> WorldPoint.toLocalInstance(ctx.client, point).stream().findFirst().orElse(point)).toList();

        log.debug("InstancedPath: " + instancedPath);

        WorldPoint startPosition = path.get(0);
        Teleport teleport = teleports.get(startPosition);
        WorldPoint localWP = local.getLocation().getWorldLocation();
        boolean offPath = instancedPath.stream().noneMatch(t -> t.distanceTo(localWP) <= 5 && canPathTo(ctx, localWP, t));

        // Teleport or refresh path if our direction changed
        if (offPath)
        {
            if (teleport != null)
            {
                log.debug("Casting teleport {}", teleport);
                if (ctx.players.getMyPlayer().isIdle() || ctx.players.getMyPlayer().isInCombat()) {
                    teleport.getHandler().run();
                    sleep(1000);
                }
                sleep(1000, () -> ctx.players.getMyPlayer().getLocation().getWorldLocation().distanceTo(teleport.getDestination()) < 10);
                return false;
            }

            path = buildPath(ctx, destination, true);
            instancedPath = path.stream().map(point -> WorldPoint.toLocalInstance(ctx.client, point).stream().findFirst().orElseThrow()).toList();
            log.debug("Refreshed path {}", path.size() - 1);
        }

        return walkAlong(ctx, instancedPath, transports);
    }

    public boolean walkAlong(List<WorldPoint> path, Map<WorldPoint, List<Transport>> transports) {
        List<WorldPoint> remainingPath = remainingPath(ctx, path);

        if (handleTransports(ctx, remainingPath, transports)) {
            return false;
        }

        return stepAlong(ctx, remainingPath);
    }

    public boolean stepAlong(List<WorldPoint> path) {
        List<WorldPoint> reachablePath = reachablePath(ctx, path);
        log.debug("reachablePath: " + reachablePath);
        if (reachablePath.isEmpty()) {
            return false;
        }
        int nextTileIdx = reachablePath.size() - 1;
        if (nextTileIdx <= MIN_TILES_WALKED_IN_STEP) {
            return step(ctx, reachablePath.get(nextTileIdx));
        }

        if (nextTileIdx > MAX_TILES_WALKED_IN_STEP)
        {
            nextTileIdx = MAX_TILES_WALKED_IN_STEP;
        }

        int targetDistance = StdRandom.uniform(MIN_TILES_WALKED_IN_STEP, nextTileIdx);
        return step(ctx, reachablePath.get(targetDistance));
    }

    public List<WorldPoint> reachablePath(List<WorldPoint> remainingPath) {
        RSPlayer local = ctx.players.getMyPlayer();
        List<WorldPoint> out = new ArrayList<>();
        for (WorldPoint p : remainingPath) {
            Tile tile = ctx.tiles.getTile(ctx, p);
            if (tile == null) {
                break;
            }

            out.add(p);
        }

        if (out.isEmpty() || out.size() == 1 && out.get(0).equals(local.getLocation().getWorldLocation()))
        {
            return Collections.emptyList();
        }

        return out;
    }

    public boolean step(WorldPoint destination) {
        RSPlayer local = ctx.players.getMyPlayer();
        log.debug("Stepping towards " + destination);
        DeviousWalker.walk(ctx, destination);

        if (local.getLocation().getWorldLocation().equals(destination)) {
            return false;
        }

        if (!DeviousWalker.isRunEnabled(ctx) && (DeviousWalker.getRunEnergy(ctx) >= StdRandom.uniform(MIN_ENERGY, MAX_MIN_ENERGY) || (local.getAccessor().getHealthScale() > -1 && DeviousWalker.getRunEnergy(ctx) > 0))) {
            DeviousWalker.toggleRun(ctx);
            while (!DeviousWalker.isRunEnabled(ctx)) {
                sleep(2000);
            }
            return true;
        }

        if (!DeviousWalker.isRunEnabled(ctx) && DeviousWalker.getRunEnergy(ctx) > 0 && DeviousWalker.isStaminaBoosted(ctx)) {
            DeviousWalker.toggleRun(ctx);
            while (!DeviousWalker.isRunEnabled(ctx)) {
                sleep(2000);
            }
            return true;
        }

        // Handles when stuck on those trees next to draynor manor
        if (!local.isMoving())
        {
            RSNPC tree = ctx.npcs.getNearest(n -> n.getID() == NpcID.TREE_4416 && n.getInteracting() == local && n.getLocation().getWorldLocation().distanceTo2D(local.getLocation().getWorldLocation()) <= 1);
            if (tree != null)
            {
                WorldArea area = new WorldArea(tree.getLocation().getWorldLocation().dx(-1).dy(-1), 3, 3);
                area.toWorldPointList().stream()
                        .filter(wp -> !wp.equals(local.getLocation().getWorldLocation()) && !wp.equals(tree.getLocation().getWorldLocation()) && canPathTo(ctx, local.getLocation().getWorldLocation(), wp))
                        .unordered()
                        .min(Comparator.comparingInt(wp -> wp.distanceTo2D(tree.getLocation().getWorldLocation())))
                        .ifPresent(wp -> DeviousWalker.walk(ctx, wp));
                return false;
            }
        }

        return true;
    }

    public boolean handleTransports(List<WorldPoint> path, Map<WorldPoint, List<Transport>> transports) {
        // Edgeville/ardy wilderness lever warning
        RSWidget leverWarningWidget = ctx.interfaces.getComponent(299, 1);
        if (leverWarningWidget.isVisible()) {
            log.debug("Handling Wilderness lever warning widget");
            ctx.npcChat.clickContinue(true);
            return true;
        }

        // Wilderness ditch warning
        RSWidget wildyDitchWidget = ctx.interfaces.getComponent(475, 11);
        if (wildyDitchWidget.isVisible())
        {
            log.debug("Handling Wilderness warning widget");
            wildyDitchWidget.doAction("Enter Wilderness");
            return true;
        }
        String[] options = ctx.npcChat.getOptions();
        if (options != null && Arrays.stream(options)
                .anyMatch(text -> text != null && text.contains("Eeep! The Wilderness")))
        {
            log.debug("Handling wilderness warning dialog");
            ctx.npcChat.selectOption("Yes, I'm brave", true);
            return true;
        }

        for (int i = 0; i < MAX_INTERACT_DISTANCE; i++) {
            if (i + 1 >= path.size()) {
                break;
            }

            WorldPoint a = path.get(i);
            WorldPoint b = path.get(i + 1);

            Tile tileA = ctx.tiles.getTile(ctx, a);
            Tile tileB = ctx.tiles.getTile(ctx, b);

            if (a.distanceTo(b) > 1
                    || (tileA != null && tileB != null && !Reachable.isWalkable(b))) {
                Transport transport = transports.getOrDefault(a, List.of()).stream()
                        .filter(x -> x.getSource().equals(a) && x.getDestination().equals(b))
                        .findFirst()
                        .orElse(null);

                if (transport != null)
                {
                    if (ignoreObstacle(ctx, transport.getSource(), 2)) {
                        return true;
                    }
                    log.debug("Trying to use transport at {} to move {} -> {}", transport.getSource(), a, b);
                    transport.getHandler().run();
                    sleep(1000);
                    return true;
                }
            }

            // MLM Rocks
            RSObject rockfall = ctx.objects.query().named("Rockfall").located(new RSTile(ctx, a)).first();
            boolean hasPickaxe = !ctx.inventory.query().namedContains("pickaxe").isEmpty() || !ctx.equipment.query().namedContains("pickaxe").isEmpty();
            if (rockfall != null && hasPickaxe) {
                log.debug("Handling MLM rockfall");
                if (!ctx.players.getMyPlayer().isIdle()) {
                    return true;
                }
                rockfall.doAction("Mine");
                return true;
            }

            if (tileA == null)
            {
                return false;
            }

            // Diagonal door bullshit
            if (Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() + b.getY()) > 1 && a.getPlane() == b.getPlane())
            {
                RSObject wall = ctx.objects.query().named("Door").located(new RSTile(ctx, a)).filter(x -> x.getType() != RSObject.Type.WALL).first();

                if (wall != null && Reachable.isClosedDoor(wall.getID()))
                {
                    if (ignoreObstacle(ctx, wall.getLocation().getWorldLocation(), 1)) {
                        return true;
                    }
                    log.debug("Handling diagonal door at {}", wall.getLocation().getWorldLocation());
                    wall.doAction("Open");
                    sleep(2000, () -> !Reachable.isClosedDoor(wall.getID()));
                    return true;
                }
            }

            if (tileB == null)
            {
                return false;
            }

            // Normal doors
            if (Reachable.isDoored(tileA, tileB)) {
                RSObject wall = new RSObject(ctx, tileA.getWallObject(), RSObject.Type.WALL, tileA.getPlane());
                if (ignoreObstacle(ctx, wall.getLocation().getWorldLocation(), 1)) {
                    return true;
                }
                wall.doAction("Open");
                log.debug("Handling door at {}", wall.getLocation().getWorldLocation());
                sleep(5000, () -> tileA.getWallObject() == null
                        || !Reachable.isClosedDoor(tileA.getWallObject().getId()));
                return true;
            }

            if (Reachable.isDoored(tileB, tileA)) {
                RSObject wall = new RSObject(ctx, tileB.getWallObject(), RSObject.Type.WALL, tileA.getPlane());
                if (ignoreObstacle(ctx, wall.getLocation().getWorldLocation(), 1)) {
                    return true;
                }
                wall.doAction("Open");
                log.debug("Handling door at {}", wall.getLocation().getWorldLocation());
                sleep(5000, () -> tileB.getWallObject() == null
                        || !Reachable.isClosedDoor(tileB.getWallObject().getId()));
                return true;
            }
        }

        return false;
    }

    private boolean ignoreObstacle(WorldPoint point, int distance) {
        if (ctx.players.getMyPlayer().isMoving()) {
            LocalPoint localDesti = ctx.client.getLocalDestinationLocation();
            if (localDesti != null) {
                WorldPoint desti = WorldPoint.fromLocal(ctx.client, localDesti);
                return desti.distanceTo2D(point) <= distance;
            }
        }
        return false;
    }

    public WorldPoint nearestWalkableTile(WorldPoint source, Predicate<WorldPoint> filter) {
        CollisionMap cm = RegionManager.getGlobalCollisionMap();

        if (!cm.fullBlock(source) && filter.test(source)) {
            return source;
        }

        int currentIteration = 1;
        for (int radius = currentIteration; radius < MAX_NEAREST_SEARCH_ITERATIONS; radius++)
        {
            for (int x = -radius; x < radius; x++)
            {
                for (int y = -radius; y < radius; y++)
                {
                    WorldPoint p = source.dx(x).dy(y);
                    if (cm.fullBlock(p) || !filter.test(p))
                    {
                        continue;
                    }
                    return p;
                }
            }
        }
        log.debug("Could not find a walkable tile near {}", source);
        return null;
    }

    public WorldPoint nearestWalkableTile(WorldPoint source) {
        return nearestWalkableTile(source, x -> true);
    }

    public List<WorldPoint> remainingPath(List<WorldPoint> path) {
        RSPlayer local = ctx.players.getMyPlayer();
        if (local == null) {
            return Collections.emptyList();
        }

        var nearest = path.stream().min(Comparator.comparingInt(x -> x.distanceTo(local.getLocation().getWorldLocation())))
                .orElse(null);
        if (nearest == null)
        {
            return Collections.emptyList();
        }

        return path.subList(path.indexOf(nearest), path.size());
    }

    public List<WorldPoint> calculatePath(WorldArea destination) {
        RSPlayer local = ctx.players.getMyPlayer();
        LinkedHashMap<WorldPoint, Teleport> teleports = buildTeleportLinks(ctx, destination);
        List<WorldPoint> startPoints = new ArrayList<>(teleports.keySet());
        startPoints.add(local.getLocation().getWorldLocation());
        return calculatePath(ctx, startPoints, destination);
    }

    public List<WorldPoint> calculatePath(List<WorldPoint> startPoints, WorldArea destination) {
        if (ctx.client.isClientThread()) {
            throw new RuntimeException("Calculate path cannot be called on client thread");
        }
        return new Pathfinder(RegionManager.getGlobalCollisionMap(), buildTransportLinks(), startPoints, destination, RegionManager.avoidWilderness()).find();
    }

    public List<WorldPoint> calculatePath(WorldPoint destination) {
        return calculatePath(ctx, destination.toWorldArea());
    }

    public List<WorldPoint> calculatePath(List<WorldPoint> startPoints, WorldPoint destination) {
        return calculatePath(ctx, startPoints, destination.toWorldArea());
    }

    private List<WorldPoint> buildPath(

            List<WorldPoint> startPoints,
            WorldArea destination,
            boolean avoidWilderness,
            boolean forced
    ) {
        if (pathFuture == null) {
            pathFuture = executor.submit(new Pathfinder(RegionManager.getGlobalCollisionMap(), buildTransportLinks(), startPoints, destination, avoidWilderness));
            currentDestination = destination;
        }

        boolean sameDestination = currentDestination != null
                && destination.getX() == currentDestination.getX()
                && destination.getY() == currentDestination.getY()
                && destination.getPlane() == currentDestination.getPlane()
                && destination.getWidth() == currentDestination.getWidth()
                && destination.getHeight() == currentDestination.getHeight();
        boolean shouldRefresh = RegionManager.shouldRefreshPath();

		if (shouldRefresh)
		{
			log.debug("Path should refresh!");
            if (!RegionManager.singleton.hasChanged(ctx)) {
                shouldRefresh = false;
            }
		}

        if (!sameDestination || shouldRefresh || forced)
        {
            log.debug("Cancelling current path");
            pathFuture.cancel(true);
            pathFuture = executor.submit(new Pathfinder(RegionManager.getGlobalCollisionMap(), buildTransportLinks(), startPoints, destination, avoidWilderness));
            currentDestination = destination;
        }

        try
        {
            if (ctx.client.isClientThread()) {
                // 16-17ms for 60fps, 6-7ms for 144fps
                return pathFuture.get(10, TimeUnit.MILLISECONDS);
            }
            return pathFuture.get();
        }
        catch (Exception e)
        {
            log.debug("Path is loading");
            return List.of();
        }
    }

    public List<WorldPoint> buildPath() {
        if (currentDestination == null) {
            return List.of();
        }
        return buildPath(ctx, currentDestination);
    }

    public List<WorldPoint> buildPath(WorldArea destination, boolean avoidWilderness, boolean forced) {
        RSPlayer local = ctx.players.getMyPlayer();
        WorldPoint playerPoint = WorldPoint.fromLocalInstance(ctx.client, local.getLocation().getLocalLocation());
        LinkedHashMap<WorldPoint, Teleport> teleports = buildTeleportLinks(ctx, destination);
        List<WorldPoint> startPoints = new ArrayList<>(teleports.keySet());
        startPoints.add(playerPoint);

        return buildPath(ctx, startPoints, destination, avoidWilderness, forced);
    }

    public List<WorldPoint> buildPath(WorldArea destination) {
        return buildPath(ctx, destination, RegionManager.avoidWilderness(), false);
    }

    public List<WorldPoint> buildPath(WorldArea destination, boolean forced) {
        return buildPath(ctx, destination, RegionManager.avoidWilderness(), forced);
    }

    public List<WorldPoint> buildPath(WorldPoint destination) {
        return buildPath(ctx, destination.toWorldArea());
    }

    public List<WorldPoint> buildPath(WorldPoint destination, boolean forced) {
        return buildPath(ctx, destination.toWorldArea(), forced);
    }

    public List<WorldPoint> buildPath(WorldPoint destination, boolean avoidWilderness, boolean forced) {
        return buildPath(ctx, destination.toWorldArea(), avoidWilderness, forced);
    }

    public List<WorldPoint> buildPath(List<WorldPoint> startPoints, WorldPoint destination, boolean avoidWilderness, boolean forced) {
        return buildPath(ctx, startPoints, destination.toWorldArea(), avoidWilderness, forced);
    }

    public Map<WorldPoint, List<Transport>> buildTransportLinks() {
        Map<WorldPoint, List<Transport>> out = new HashMap<>();
        if (!ConfigManager.useTransports) {
            return out;
        }

        for (Transport transport : TransportLoader.buildTransports()) {
            out.computeIfAbsent(transport.getSource(), x -> new ArrayList<>()).add(transport);
        }

        return out;
    }

    public LinkedHashMap<WorldPoint, Teleport> buildTeleportLinks(WorldArea destination) {
        LinkedHashMap<WorldPoint, Teleport> out = new LinkedHashMap<>();
        if (!ConfigManager.useTeleports) {
            return out;
        }

        RSPlayer local = ctx.players.getMyPlayer();
        WorldPoint playerPoint = WorldPoint.fromLocalInstance(ctx.client, local.getLocation().getLocalLocation());

        for (Teleport teleport : TeleportLoader.buildTeleports(ctx)) {
            if (teleport.getDestination().distanceTo(playerPoint) > 50
                    && destination.distanceTo(playerPoint) > destination.distanceTo(teleport.getDestination()) + 20) {
                out.putIfAbsent(teleport.getDestination(), teleport);
            }
        }

        return out;
    }

    public Map<WorldPoint, List<Transport>> buildTransportLinksOnPath(List<WorldPoint> path) {
        Map<WorldPoint, List<Transport>> out = new HashMap<>();
        for (Transport transport : TransportLoader.buildTransports()) {
            WorldPoint destination = transport.getDestination();
            if (path.contains(destination)) {
                out.computeIfAbsent(transport.getSource(), x -> new ArrayList<>()).add(transport);
            }
        }
        return out;
    }

    public LinkedHashMap<WorldPoint, Teleport> buildTeleportLinksOnPath(List<WorldPoint> path) {
        LinkedHashMap<WorldPoint, Teleport> out = new LinkedHashMap<>();
        for (Teleport teleport : TeleportLoader.buildTeleports(ctx)) {
            WorldPoint destination = teleport.getDestination();
            if (path.contains(destination)) {
                out.putIfAbsent(destination, teleport);
            }
        }
        return out;
    }

    public List<Tile> pathTo(Tile start, Tile end) {

        int z = start.getPlane();
        if (z != end.getPlane()) {
            return null;
        }

        CollisionData[] collisionData = ctx.client.getCollisionMaps();
        if (collisionData == null)
        {
            return null;
        }

        int[][] directions = new int[128][128];
        int[][] distances = new int[128][128];
        int[] bufferX = new int[4096];
        int[] bufferY = new int[4096];

        // Initialise directions and distances
        for (int i = 0; i < 128; ++i)
        {
            for (int j = 0; j < 128; ++j)
            {
                directions[i][j] = 0;
                distances[i][j] = Integer.MAX_VALUE;
            }
        }

        Point p1 = start.getSceneLocation();
        Point p2 = end.getSceneLocation();

        int middleX = p1.getX();
        int middleY = p1.getY();
        int currentX = middleX;
        int currentY = middleY;
        int offsetX = 64;
        int offsetY = 64;
        // Initialise directions and distances for starting tile
        directions[offsetX][offsetY] = 99;
        distances[offsetX][offsetY] = 0;
        int index1 = 0;
        bufferX[0] = currentX;
        int index2 = 1;
        bufferY[0] = currentY;
        int[][] collisionDataFlags = collisionData[z].getFlags();

        boolean isReachable = false;

        while (index1 != index2)
        {
            currentX = bufferX[index1];
            currentY = bufferY[index1];
            index1 = index1 + 1 & 4095;
            // currentX is for the local coordinate while currentMapX is for the index in the directions and distances arrays
            int currentMapX = currentX - middleX + offsetX;
            int currentMapY = currentY - middleY + offsetY;
            if ((currentX == p2.getX()) && (currentY == p2.getY()))
            {
                isReachable = true;
                break;
            }

            int currentDistance = distances[currentMapX][currentMapY] + 1;
            if (currentMapX > 0 && directions[currentMapX - 1][currentMapY] == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0)
            {
                // Able to move 1 tile west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY;
                index2 = index2 + 1 & 4095;
                directions[currentMapX - 1][currentMapY] = 2;
                distances[currentMapX - 1][currentMapY] = currentDistance;
            }

            if (currentMapX < 127 && directions[currentMapX + 1][currentMapY] == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0)
            {
                // Able to move 1 tile east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY;
                index2 = index2 + 1 & 4095;
                directions[currentMapX + 1][currentMapY] = 8;
                distances[currentMapX + 1][currentMapY] = currentDistance;
            }

            if (currentMapY > 0 && directions[currentMapX][currentMapY - 1] == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0)
            {
                // Able to move 1 tile south
                bufferX[index2] = currentX;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX][currentMapY - 1] = 1;
                distances[currentMapX][currentMapY - 1] = currentDistance;
            }

            if (currentMapY < 127 && directions[currentMapX][currentMapY + 1] == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0)
            {
                // Able to move 1 tile north
                bufferX[index2] = currentX;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX][currentMapY + 1] = 4;
                distances[currentMapX][currentMapY + 1] = currentDistance;
            }

            if (currentMapX > 0 && currentMapY > 0 && directions[currentMapX - 1][currentMapY - 1] == 0 && (collisionDataFlags[currentX - 1][currentY - 1] & 19136782) == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0)
            {
                // Able to move 1 tile south-west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX - 1][currentMapY - 1] = 3;
                distances[currentMapX - 1][currentMapY - 1] = currentDistance;
            }

            if (currentMapX > 0 && currentMapY < 127 && directions[currentMapX - 1][currentMapY + 1] == 0 && (collisionDataFlags[currentX - 1][currentY + 1] & 19136824) == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0)
            {
                // Able to move 1 tile north-west
                bufferX[index2] = currentX - 1;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX - 1][currentMapY + 1] = 6;
                distances[currentMapX - 1][currentMapY + 1] = currentDistance;
            }

            if (currentMapX < 127 && currentMapY > 0 && directions[currentMapX + 1][currentMapY - 1] == 0 && (collisionDataFlags[currentX + 1][currentY - 1] & 19136899) == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0)
            {
                // Able to move 1 tile south-east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY - 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX + 1][currentMapY - 1] = 9;
                distances[currentMapX + 1][currentMapY - 1] = currentDistance;
            }

            if (currentMapX < 127 && currentMapY < 127 && directions[currentMapX + 1][currentMapY + 1] == 0 && (collisionDataFlags[currentX + 1][currentY + 1] & 19136992) == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0)
            {
                // Able to move 1 tile north-east
                bufferX[index2] = currentX + 1;
                bufferY[index2] = currentY + 1;
                index2 = index2 + 1 & 4095;
                directions[currentMapX + 1][currentMapY + 1] = 12;
                distances[currentMapX + 1][currentMapY + 1] = currentDistance;
            }
        }
        if (!isReachable)
        {
            // Try find a different reachable tile in the 21x21 area around the target tile, as close as possible to the target tile
            int upperboundDistance = Integer.MAX_VALUE;
            int pathLength = Integer.MAX_VALUE;
            int checkRange = 10;
            int approxDestinationX = p2.getX();
            int approxDestinationY = p2.getY();
            for (int i = approxDestinationX - checkRange; i <= checkRange + approxDestinationX; ++i)
            {
                for (int j = approxDestinationY - checkRange; j <= checkRange + approxDestinationY; ++j)
                {
                    int currentMapX = i - middleX + offsetX;
                    int currentMapY = j - middleY + offsetY;
                    if (currentMapX >= 0 && currentMapY >= 0 && currentMapX < 128 && currentMapY < 128 && distances[currentMapX][currentMapY] < 100)
                    {
                        int deltaX = 0;
                        if (i < approxDestinationX)
                        {
                            deltaX = approxDestinationX - i;
                        }
                        else if (i > approxDestinationX)
                        {
                            deltaX = i - (approxDestinationX);
                        }

                        int deltaY = 0;
                        if (j < approxDestinationY)
                        {
                            deltaY = approxDestinationY - j;
                        }
                        else if (j > approxDestinationY)
                        {
                            deltaY = j - (approxDestinationY);
                        }

                        int distanceSquared = deltaX * deltaX + deltaY * deltaY;
                        if (distanceSquared < upperboundDistance || distanceSquared == upperboundDistance && distances[currentMapX][currentMapY] < pathLength)
                        {
                            upperboundDistance = distanceSquared;
                            pathLength = distances[currentMapX][currentMapY];
                            currentX = i;
                            currentY = j;
                        }
                    }
                }
            }
            if (upperboundDistance == Integer.MAX_VALUE)
            {
                // No path found
                return null;
            }
        }

        // Getting path from directions and distances
        bufferX[0] = currentX;
        bufferY[0] = currentY;
        int index = 1;
        int directionNew;
        int directionOld;
        for (directionNew = directionOld = directions[currentX - middleX + offsetX][currentY - middleY + offsetY]; p1.getX() != currentX || p1.getY() != currentY; directionNew = directions[currentX - middleX + offsetX][currentY - middleY + offsetY])
        {
            if (directionNew != directionOld)
            {
                // "Corner" of the path --> new checkpoint tile
                directionOld = directionNew;
                bufferX[index] = currentX;
                bufferY[index++] = currentY;
            }

            if ((directionNew & 2) != 0)
            {
                ++currentX;
            }
            else if ((directionNew & 8) != 0)
            {
                --currentX;
            }

            if ((directionNew & 1) != 0)
            {
                ++currentY;
            }
            else if ((directionNew & 4) != 0)
            {
                --currentY;
            }
        }

        int checkpointTileNumber = 1;
        Tile[][][] tiles = ctx.client.getScene().getTiles();
        List<Tile> checkpointTiles = new ArrayList<>();
        while (index-- > 0)
        {
            checkpointTiles.add(tiles[start.getPlane()][bufferX[index]][bufferY[index]]);
            if (checkpointTileNumber == 25)
            {
                // Pathfinding only supports up to the 25 first checkpoint tiles
                break;
            }
            checkpointTileNumber++;
        }
        return checkpointTiles;
    }

    public List<WorldPoint> pathTo(WorldPoint start, WorldPoint end) {
        if (start.getPlane() != end.getPlane()) {
            return null;
        }

        LocalPoint sourceLp = LocalPoint.fromWorld(ctx.client, start.getX(), start.getY());
        LocalPoint targetLp = LocalPoint.fromWorld(ctx.client, end.getX(), end.getY());
        if (sourceLp == null || targetLp == null) {
            return null;
        }

        int thisX = sourceLp.getSceneX();
        int thisY = sourceLp.getSceneY();
        int otherX = targetLp.getSceneX();
        int otherY = targetLp.getSceneY();

        Tile[][][] tiles = ctx.client.getScene().getTiles();
        Tile sourceTile = tiles[start.getPlane()][thisX][thisY];

        Tile targetTile = tiles[start.getPlane()][otherX][otherY];
        List<Tile> checkpointTiles = pathTo(ctx, sourceTile, targetTile);
        if (checkpointTiles == null)
        {
            return null;
        }
        List<WorldPoint> checkpointWPs = new ArrayList<>();
        for (Tile checkpointTile : checkpointTiles)
        {
            if (checkpointTile == null)
            {
                break;
            }
            checkpointWPs.add(checkpointTile.getWorldLocation());
        }
        return checkpointWPs;
    }

    public boolean canPathTo(WorldPoint start, WorldPoint destination) {
        List<WorldPoint> pathTo = pathTo(ctx, start, destination);
        return pathTo != null && pathTo.contains(destination);
    }

    public boolean canWalk(WorldPoint destination) {
        return canWalk(ctx, destination.toWorldArea());
    }

    public boolean canWalk(WorldArea destination) {
        Map<WorldPoint, List<Transport>> transports = buildTransportLinks();
        LinkedHashMap<WorldPoint, Teleport> teleports = buildTeleportLinks(ctx, destination);
        List<WorldPoint> path = buildPath(ctx, destination);
        return path != null && path.stream().anyMatch(p -> destination.contains(p));
    }
}
