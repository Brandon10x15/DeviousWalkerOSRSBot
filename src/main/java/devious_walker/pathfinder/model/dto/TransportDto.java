package devious_walker.pathfinder.model.dto;

import devious_walker.pathfinder.TransportLoader;
import devious_walker.pathfinder.model.Transport;
import devious_walker.pathfinder.model.requirement.Requirements;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;
import net.runelite.rsb.methods.MethodContext;

@Value
public class TransportDto
{
    WorldPoint source;
    WorldPoint destination;
    String action;
    Integer objectId;
    Requirements requirements;

    public Transport toTransport(MethodContext ctx) {
        return TransportLoader.objectTransport(ctx, source, destination, objectId, action, requirements);
    }
}
