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
import net.runelite.rsb.wrappers.RSPlayer;
import net.runelite.rsb.wrappers.RSTile;
import net.runelite.rsb.wrappers.RSWidget;
import net.runelite.rsb.wrappers.common.Positionable;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static net.runelite.rsb.methods.MethodProvider.methods;
import static net.runelite.rsb.methods.Methods.sleep;

@Slf4j
public class DeviousWalker
{
	@Inject
	RegionManager regionManager;

	private static final int STAMINA_VARBIT = 25;
	private static final int RUN_VARP = 173;

	public static WorldPoint getDestination()
	{
		Client client = methods.client;
		LocalPoint destination = client.getLocalDestinationLocation();
		if (destination == null || (destination.getX() == 0 && destination.getY() == 0))
		{
			return null;
		}

		return new WorldPoint(
				destination.getX() + client.getBaseX(),
				destination.getY() + client.getBaseY(),
				client.getPlane()
		);
	}

	public static boolean isWalking()
	{
		RSPlayer local = methods.players.getMyPlayer();
		WorldPoint destination = getDestination();
		return local.isMoving()
				&& destination != null
				&& destination.distanceTo(local.getPosition().getWorldLocation()) > 4;
	}

	public static void walk(WorldPoint worldPoint)
	{
		Client client = methods.client;
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}

		WorldPoint walkPoint = worldPoint;
		Tile destinationTile = methods.tiles.getTile(methods, worldPoint);
		// Check if tile is in loaded client scene
		if (destinationTile == null)
		{
			log.debug("Destination {} is not in scene", worldPoint);
			Tile nearestInScene = Arrays.stream(methods.client.getScene().getTiles()[methods.client.getPlane()])
					.flatMap(Arrays::stream)
					.filter(tile -> tile != null)
					.min(Comparator.comparingInt(x -> x.getWorldLocation().distanceTo(worldPoint)))
					.orElse(null);

			// TODO: check which of these is better
			//Tile nearestInScene = methods.calc.getTileOnScreen(new RSTile(worldPoint)).getTile(methods);

			if (nearestInScene == null)
			{
				log.debug("Couldn't find nearest walkable tile");
				return;
			}

			walkPoint = nearestInScene.getWorldLocation();
			destinationTile = nearestInScene;
		}
		RSTile walkTile = new RSTile(methods, destinationTile);
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

	public static boolean walkTo(WorldArea worldArea)
	{
		return Walker.walkTo(worldArea);
	}

	public static void walk(Positionable locatable)
	{
		walk(locatable.getLocation().getWorldLocation());
	}

	public static boolean walkTo(WorldPoint worldPoint)
	{
		return Walker.walkTo(worldPoint);
	}

	public static boolean walkTo(Positionable locatable)
	{
		return walkTo(locatable.getLocation().getWorldLocation());
	}

	public static boolean walkTo(BankLocation bankLocation)
	{
		return walkTo(bankLocation.getArea());
	}

	public static boolean walkTo(int x, int y)
	{
		return walkTo(x, y, methods.client.getPlane());
	}

	public static boolean walkTo(int x, int y, int plane)
	{
		return walkTo(new WorldPoint(x, y, plane));
	}


	public static boolean walkToCompletion(WorldArea worldArea)
	{
		if (!Walker.canWalk(worldArea)) {
			log.warn("Failed to generate path attempting to walk to {}", worldArea);
			return false;
		}
		long standingTimerStart = -
				1;
		RSPlayer local = methods.players.getMyPlayer();
		while (worldArea.distanceTo(WorldPoint.fromLocalInstance(methods.client, local.getLocation().getLocalLocation())) > 3 &&
				worldArea.getPlane() == methods.players.getMyPlayer().getLocation().getPlane()) {
			sleep(4000, () -> getDestination() == null || getDestination().distanceTo(methods.players.getMyPlayer().getPosition().getWorldLocation()) < 15);
			Walker.walkTo(worldArea);
			if (!methods.players.getMyPlayer().isMoving()) {
				if (standingTimerStart == -1) {
					standingTimerStart = System.currentTimeMillis();
				}
				else if (System.currentTimeMillis() - standingTimerStart > 10000) {
					log.warn("Player is stuck attempting to walk to {}", worldArea);
					return false;
				}
			}
			else {
				standingTimerStart = -1;
			}
		}
		return true;
	}

	public static boolean walkToCompletion(WorldPoint worldPoint)
	{
		return walkToCompletion(worldPoint.toWorldArea());
	}

	public static boolean walkToCompletion(Positionable locatable)
	{
		return walkToCompletion(locatable.getLocation().getWorldLocation());
	}

	public static boolean walkToCompletion(BankLocation bankLocation)
	{
		return walkToCompletion(bankLocation.getArea());
	}

	public static boolean walkToCompletion(int x, int y)
	{
		return walkToCompletion(x, y, methods.client.getPlane());
	}

	public static boolean walkToCompletion(int x, int y, int plane)
	{
		return walkToCompletion(new WorldPoint(x, y, plane));
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
	public static boolean isRunEnabled()
	{
		return methods.client.getVarpValue(RUN_VARP) == 1;
	}

	public static void toggleRun()
	{
		RSWidget widget = methods.interfaces.getComponent(WidgetInfo.MINIMAP_TOGGLE_RUN_ORB);
		if (widget != null)
		{
			widget.doAction("Toggle Run");
		}
	}

	public static boolean isStaminaBoosted()
	{
		return methods.client.getVarbitValue(STAMINA_VARBIT) == 1;
	}

	public static int getRunEnergy()
	{
		return methods.client.getEnergy() / 100;
	}

	public static int calculateDistance(WorldArea destination)
	{
		return Walker.calculatePath(destination).size();
	}

	public static int calculateDistance(WorldPoint start, WorldArea destination)
	{
		return calculateDistance(List.of(start), destination);
	}

	public static int calculateDistance(List<WorldPoint> start, WorldArea destination)
	{
		return Walker.calculatePath(start, destination).size();
	}

	public static int calculateDistance(WorldPoint destination)
	{
		return calculateDistance(destination.toWorldArea());
	}

	public static int calculateDistance(WorldPoint start, WorldPoint destination)
	{
		return calculateDistance(start, destination.toWorldArea());
	}

	public static int calculateDistance(List<WorldPoint> start, WorldPoint destination)
	{
		return calculateDistance(start, destination.toWorldArea());
	}
}
