package devious_walker;

import net.runelite.api.CollisionData;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.rsb.wrappers.RSObject;
import net.runelite.rsb.wrappers.RSPlayer;
import net.runelite.rsb.wrappers.RSTile;
import net.runelite.rsb.wrappers.common.Positionable;

import java.util.*;
import java.util.stream.Collectors;

import static net.runelite.rsb.methods.MethodProvider.methods;

public class Reachable
{
	private static final int MAX_ATTEMPTED_TILES = 64 * 64;

	public static boolean check(int flag, int checkFlag)
	{
		return (flag & checkFlag) != 0;
	}

	public static boolean isObstacle(int endFlag)
	{
		return check(endFlag, 0x100 | 0x20000 | 0x200000 | 0x1000000);
	}

	public static boolean isObstacle(WorldPoint worldPoint)
	{
		return isObstacle(getCollisionFlag(worldPoint));
	}

	public static int getCollisionFlag(WorldPoint point)
	{
		CollisionData[] collisionMaps = methods.client.getCollisionMaps();
		if (collisionMaps == null)
		{
			return 0xFFFFFF;
		}

		CollisionData collisionData = collisionMaps[methods.client.getPlane()];
		if (collisionData == null)
		{
			return 0xFFFFFF;
		}

		LocalPoint localPoint = LocalPoint.fromWorld(methods.client, point);
		if (localPoint == null)
		{
			return 0xFFFFFF;
		}

		return collisionData.getFlags()[localPoint.getSceneX()][localPoint.getSceneY()];
	}

	public static boolean isWalled(Direction direction, int startFlag)
	{
		switch (direction)
		{
			case NORTH:
				return check(startFlag, 0x2);
			case SOUTH:
				return check(startFlag, 0x20);
			case WEST:
				return check(startFlag, 0x80);
			case EAST:
				return check(startFlag, 0x8);
			default:
				throw new IllegalArgumentException();
		}
	}

	public static boolean isWalled(WorldPoint source, WorldPoint destination)
	{
		methods.tiles.getTile(methods, source.getX(), source.getY(), source.getPlane());
		return isWalled(methods.tiles.getTile(methods, source.getX(), source.getY(), source.getPlane()), methods.tiles.getTile(methods, destination.getX(), destination.getY(), destination.getPlane()));
	}

	public static boolean isWalled(Tile source, Tile destination)
	{
		WallObject wall = source.getWallObject();
		if (wall == null)
		{
			return false;
		}

		WorldPoint a = source.getWorldLocation();
		WorldPoint b = destination.getWorldLocation();

		switch (wall.getOrientationA())
		{
			case 1:
				return a.dx(-1).equals(b) || a.dx(-1).dy(1).equals(b) || a.dx(-1).dy(-1).equals(b);
			case 2:
				return a.dy(1).equals(b) || a.dx(-1).dy(1).equals(b) || a.dx(1).dy(1).equals(b);
			case 4:
				return a.dx(1).equals(b) || a.dx(1).dy(1).equals(b) || a.dx(1).dy(-1).equals(b);
			case 8:
				return a.dy(-1).equals(b) || a.dx(-1).dy(-1).equals(b) || a.dx(-1).dy(1).equals(b);
			default:
				return false;
		}
	}

	public static boolean hasDoor(WorldPoint source, Direction direction)
	{
		Tile tile = methods.tiles.getTile(methods, source);
		if (tile == null)
		{
			return false;
		}

		return hasDoor(tile, direction);
	}

	public static boolean hasDoor(Tile source, Direction direction)
	{
		WallObject wall = source.getWallObject();
		if (wall == null)
		{
			return false;
		}

		return isWalled(direction, getCollisionFlag(source.getWorldLocation())) && isDoor(wall.getId());
	}

	public static boolean isDoored(Tile source, Tile destination)
	{
		WallObject wall = source.getWallObject();
		if (wall == null)
		{
			return false;
		}

		return isWalled(source, destination) && isClosedDoor(wall.getId());
	}

	public static boolean isDoor(int id)
	{
		return Arrays.stream(methods.client.getObjectDefinition(id).getActions())
				.filter(x -> x != null && (x.equals("Open") || x.equals("Close"))).count() != 0;
	}

	public static boolean isClosedDoor(int id)
	{
		return Arrays.stream(methods.client.getObjectDefinition(id).getActions())
				.filter(x -> x != null && x.equals("Open")).count() != 0;
	}

	public static boolean canWalk(Direction direction, int startFlag, int endFlag)
	{
		if (isObstacle(endFlag))
		{
			return false;
		}

		return !isWalled(direction, startFlag);
	}

	public static WorldPoint getNeighbour(Direction direction, WorldPoint source)
	{
		switch (direction)
		{
			case NORTH:
				return source.dy(1);
			case SOUTH:
				return source.dy(-1);
			case WEST:
				return source.dx(-1);
			case EAST:
				return source.dx(1);
			default:
				throw new IllegalArgumentException();
		}
	}

	public static List<WorldPoint> getNeighbours(Positionable destination, Positionable current)
	{
		List<WorldPoint> out = new ArrayList<>();
		WorldPoint dest = current.getLocation().getWorldLocation();
		for (Direction dir : Direction.values())
		{
			WorldPoint neighbour = getNeighbour(dir, dest);
			if (!neighbour.isInScene(methods.client))
			{
				continue;
			}

			if (destination instanceof Positionable)
			{
				Positionable targetObject = (Positionable) destination;
				boolean containsPoint;
				if (targetObject instanceof RSObject)
				{
					containsPoint = ((RSObject) targetObject).getArea().contains(neighbour);
				}
				else
				{
					containsPoint = targetObject.getLocation().getWorldLocation().equals(neighbour);
				}

				if (containsPoint
						&& (!isWalled(dir, getCollisionFlag(dest)) || targetObject instanceof WallObject))
				{
					out.add(neighbour);
					continue;
				}
			}

			if (!canWalk(dir, getCollisionFlag(dest), getCollisionFlag(neighbour)))
			{
				continue;
			}

			out.add(neighbour);
		}

		return out;
	}

	public static List<WorldPoint> getVisitedTiles(Positionable destination)
	{
		RSPlayer local = methods.players.getMyPlayer();
		WorldPoint dest = destination.getLocation().getWorldLocation();
		if (local == null || !dest.isInScene(methods.client))
		{
			return Collections.emptyList();
		}

		List<WorldPoint> visitedTiles = new ArrayList<>();
		LinkedList<WorldPoint> queue = new LinkedList<>();

		if (local.getLocation().getWorldLocation().getPlane() != dest.getPlane())
		{
			return visitedTiles;
		}

		queue.add(local.getLocation().getWorldLocation());

		while (!queue.isEmpty())
		{
			if (visitedTiles.size() > MAX_ATTEMPTED_TILES)
			{
				return visitedTiles;
			}

			WorldPoint current = queue.pop();
			visitedTiles.add(current);

			if (current.equals(dest))
			{
				return visitedTiles;
			}

			List<WorldPoint> neighbours = getNeighbours(destination, new RSTile(methods, current))
                    .stream().filter(x -> !visitedTiles.contains(x) && !queue.contains(x))
                    .collect(Collectors.toList());
			queue.addAll(neighbours);
		}

		return visitedTiles;
	}

	/*
	 * Temp, for compatibility
	 */
	public static List<WorldPoint> getVisitedTiles(WorldPoint worldPoint)
	{
        return getVisitedTiles(new RSTile(methods, worldPoint));
	}
/*
	public static boolean isInteractable(Positionable locatable)
	{
		return getInteractable(locatable).stream().anyMatch(Reachable::isWalkable);
	}

	public static List<WorldPoint> getInteractable(Positionable locatable)
	{
		WorldArea locatableArea = locatable.getLocation().getWorldLocation().toWorldArea();
		WorldArea surrounding = locatableArea.offset(1);

		// List of tiles that can interact with worldArea and can be walked on
		return surrounding.toWorldPointList().stream()
			.filter(p -> !locatableArea.contains(p))
			.filter(p -> locatableArea.canMelee(methods.client, p.toWorldArea()))
			.filter(p -> !isObstacle(p))
			.collect(Collectors.toList());
	}
 */

	public static boolean isWalkable(WorldPoint worldPoint)
	{
		return getVisitedTiles(worldPoint).contains(worldPoint);
	}
}
