package devious_walker;

import devious_walker.pathfinder.Walker;
import devious_walker.pathfinder.model.BankLocation;
import devious_walker.region.RegionManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.rsb.methods.MethodContext;
import net.runelite.rsb.wrappers.RSPlayer;
import net.runelite.rsb.wrappers.RSTile;
import net.runelite.rsb.wrappers.RSWidget;
import net.runelite.rsb.wrappers.common.Positionable;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static net.runelite.rsb.methods.Methods.sleep;

@Slf4j
public class DeviousWalker {
    @Inject
    RegionManager regionManager;

    private static final int STAMINA_VARBIT = 25;
    private static final int RUN_VARP = 173;

    public DeviousWalker() {

    }

    public static WorldPoint getDestination(MethodContext ctx) {
        Client client = ctx.client;
        LocalPoint destination = client.getLocalDestinationLocation();
        if (destination == null || (destination.getX() == 0 && destination.getY() == 0)) {
            return null;
        }

        return new WorldPoint(
                destination.getX() + client.getBaseX(),
				destination.getY() + client.getBaseY(),
				client.getPlane()
		);
	}

    public static boolean isWalking(MethodContext ctx) {
        RSPlayer local = ctx.players.getMyPlayer();
        WorldPoint destination = getDestination(ctx);
        return local.isMoving()
                && destination != null
                && destination.distanceTo(local.getPosition().getWorldLocation()) > 4;
    }

    public static void walk(MethodContext ctx, WorldPoint worldPoint) {
        Client client = ctx.client;
        Player local = client.getLocalPlayer();
        if (local == null) {
            return;
        }

        WorldPoint walkPoint = worldPoint;
        Tile destinationTile = ctx.tiles.getTile(ctx, worldPoint);
		// Check if tile is in loaded client scene
		if (destinationTile == null)
		{
			log.debug("Destination {} is not in scene", worldPoint);
            Tile nearestInScene = Arrays.stream(ctx.client.getScene().getTiles()[ctx.client.getPlane()])
                    .flatMap(Arrays::stream)
                    .filter(Objects::nonNull)
                    .min(Comparator.comparingInt(x -> x.getWorldLocation().distanceTo(worldPoint)))
                    .orElse(null);

            // TODO: check which of these is better
            //Tile nearestInScene = ctx.calc.getTileOnScreen(new RSTile(worldPoint)).getTile(ctx);

			if (nearestInScene == null)
			{
				log.debug("Couldn't find nearest walkable tile");
				return;
			}

			walkPoint = nearestInScene.getWorldLocation();
			destinationTile = nearestInScene;
		}
        RSTile walkTile = new RSTile(ctx, destinationTile);
		/*
		int sceneX = walkPoint.getX() - client.getBaseX();
		int sceneY = walkPoint.getY() - client.getBaseY();
		Point canv = Perspective.localToCanvas(client, LocalPoint.fromScene(sceneX, sceneY), client.getPlane());
		int x = canv != null ? canv.getX() : -1;
		int y = canv != null ? canv.getY() : -1;

		client.interact(
				0,
				MenuAction.WALK.getId(),
				sceneX,
				sceneY,
				x,
				y
		);
		 */
        walkTile.getClickBox().doAction("Walk here");
        log.debug("Walking to: " + walkTile.toString());
    }

    public static boolean walkTo(MethodContext ctx, WorldArea worldArea) {
        return Walker.walkTo(ctx, worldArea);
    }

    public static void walk(MethodContext ctx, Positionable locatable) {
        walk(ctx, locatable.getLocation().getWorldLocation());
    }

    public static boolean walkTo(MethodContext ctx, WorldPoint worldPoint) {
        return Walker.walkTo(ctx, worldPoint);
    }

    public static boolean walkTo(MethodContext ctx, Positionable locatable) {
        return walkTo(ctx, locatable.getLocation().getWorldLocation());
    }

    public static boolean walkTo(MethodContext ctx, BankLocation bankLocation) {
        return walkTo(ctx, bankLocation.getArea());
    }

    public static boolean walkTo(MethodContext ctx, int x, int y) {
        return walkTo(ctx, x, y, ctx.client.getPlane());
    }

    public static boolean walkTo(MethodContext ctx, int x, int y, int plane) {
        return walkTo(ctx, new WorldPoint(x, y, plane));
    }


    public static boolean walkToCompletion(MethodContext ctx, WorldArea worldArea) {
        if (!Walker.canWalk(ctx, worldArea)) {
            log.warn("Failed to generate path attempting to walk to {}", worldArea);
            return false;
        }
        long standingTimerStart = -
                1;
        RSPlayer local = ctx.players.getMyPlayer();
        while (worldArea.distanceTo(WorldPoint.fromLocalInstance(ctx.client, local.getLocation().getLocalLocation())) > 3 &&
                worldArea.getPlane() == ctx.players.getMyPlayer().getLocation().getPlane()) {
            sleep(4000, () -> getDestination(ctx) == null || getDestination(ctx).distanceTo(ctx.players.getMyPlayer().getPosition().getWorldLocation()) < 15);
            Walker.walkTo(ctx, worldArea);
            if (!ctx.players.getMyPlayer().isMoving()) {
                if (standingTimerStart == -1) {
                    standingTimerStart = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - standingTimerStart > 10000) {
                    log.warn("Player is stuck attempting to walk to {}", worldArea);
                    return false;
                }
            } else {
                standingTimerStart = -1;
			}
		}
		return true;
	}

    public static boolean walkToCompletion(MethodContext ctx, WorldPoint worldPoint) {
        return walkToCompletion(ctx, worldPoint.toWorldArea());
    }

    public static boolean walkToCompletion(MethodContext ctx, Positionable locatable) {
        return walkToCompletion(ctx, locatable.getLocation().getWorldLocation());
    }

    public static boolean walkToCompletion(MethodContext ctx, BankLocation bankLocation) {
        return walkToCompletion(ctx, bankLocation.getArea());
    }

    public static boolean walkToCompletion(MethodContext ctx, int x, int y) {
        return walkToCompletion(ctx, x, y, ctx.client.getPlane());
    }

    public static boolean walkToCompletion(MethodContext ctx, int x, int y, int plane) {
        return walkToCompletion(ctx, new WorldPoint(x, y, plane));
    }


	/**
	 * Walk next to a Locatable.
	 * This will first attempt to walk to tile that can interact with the locatable.
	 */
/*
	public static boolean walkNextTo(Positionable locatable)
	{
		// WorldPoints that can interact with the locatable
		List<WorldPoint> interactPoints = Reachable.getInteractable(locatable);

		// If no tiles are interactable, use un-interactable tiles instead  (exclusing self)
		if (interactPoints.isEmpty())
		{
			interactPoints.addAll(locatable.getWorldArea().offset(1).toWorldPointList());
			interactPoints.removeIf(p -> locatable.getWorldArea().contains(p));
		}

		// First WorldPoint that is walkable from the list of interactPoints
		WorldPoint walkableInteractPoint = interactPoints.stream()
			.filter(Reachable::isWalkable)
			.findFirst()
			.orElse(null);

		// Priority to a walkable tile, otherwise walk to the first tile next to locatable
		return (walkableInteractPoint != null) ? walkTo(walkableInteractPoint) : walkTo(interactPoints.get(0));
	}
 */

    /**
     * Returns true if run is toggled on
     */
    public static boolean isRunEnabled(MethodContext ctx) {
        return ctx.client.getVarpValue(RUN_VARP) == 1;
    }

    public static void toggleRun(MethodContext ctx) {
        RSWidget widget = ctx.interfaces.getComponent(WidgetInfo.MINIMAP_TOGGLE_RUN_ORB);
        if (widget != null) {
            widget.doAction("Toggle Run");
        }
    }

    public static boolean isStaminaBoosted(MethodContext ctx) {
        return ctx.client.getVarbitValue(STAMINA_VARBIT) == 1;
    }

    public static int getRunEnergy(MethodContext ctx) {
        return ctx.client.getEnergy() / 100;
    }

    public static int calculateDistance(MethodContext ctx, WorldArea destination) {
        return Walker.calculatePath(ctx, destination).size();
    }

    public static int calculateDistance(MethodContext ctx, WorldPoint start, WorldArea destination) {
        return calculateDistance(ctx, List.of(start), destination);
    }

    public static int calculateDistance(MethodContext ctx, List<WorldPoint> start, WorldArea destination) {
        return Walker.calculatePath(ctx, start, destination).size();
    }

    public static int calculateDistance(MethodContext ctx, WorldPoint destination) {
        return calculateDistance(ctx, destination.toWorldArea());
    }

    public static int calculateDistance(MethodContext ctx, WorldPoint start, WorldPoint destination) {
        return calculateDistance(ctx, start, destination.toWorldArea());
    }

    public static int calculateDistance(MethodContext ctx, List<WorldPoint> start, WorldPoint destination) {
        return calculateDistance(ctx, start, destination.toWorldArea());
	}
}
