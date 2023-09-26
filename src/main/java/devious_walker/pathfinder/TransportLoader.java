package devious_walker.pathfinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import devious_walker.DeviousWalker;
import devious_walker.GameThread;
import devious_walker.enums.WalkerVarbits;
import devious_walker.enums.WalkerWidgetInfo;
import devious_walker.pathfinder.model.FairyRingLocation;
import devious_walker.pathfinder.model.Transport;
import devious_walker.pathfinder.model.dto.TransportDto;
import devious_walker.pathfinder.model.requirement.Requirements;
import devious_walker.quests.QuestVarbits;
import devious_walker.quests.Quests;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.rsb.methods.MethodContext;
import net.runelite.rsb.wrappers.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static devious_walker.pathfinder.model.MovementConstants.*;

@Slf4j
public class TransportLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<Transport> ALL_STATIC_TRANSPORTS = new ArrayList<>();
    private static final List<Transport> LAST_TRANSPORT_LIST = new ArrayList<>();
    private final MethodContext ctx;

    public TransportLoader(MethodContext ctx) {
        this.ctx = ctx;
    }

    public Runnable init() {
        log.info("Loading transports");
        try (InputStream stream = Walker.class.getResourceAsStream("/transports.json")) {
            if (stream == null) {
                log.error("Failed to load transports.");
                return null;
            }

            TransportDto[] json = GSON.fromJson(new String(stream.readAllBytes()), TransportDto[].class);

            for (var trans : json) {
                ALL_STATIC_TRANSPORTS.add(trans.toTransport(ctx));
            }
        }
        catch (IOException e)
        {
            log.error("Failed to load transports.", e);
        }

        log.info("Loaded {} transports", ALL_STATIC_TRANSPORTS.size());
        return null;
    }

    public List<Transport> buildTransports() {
        return LAST_TRANSPORT_LIST;
    }

    public void refreshTransports() {
        GameThread.invoke(() ->
        {
            List<Transport> filteredStatic = ALL_STATIC_TRANSPORTS.stream()
                    .filter(it -> it.getRequirements().fulfilled())
                    .collect(Collectors.toList());

            List<Transport> transports = new ArrayList<>();

            int gold = ctx.inventory.getItem(995) != null ? ctx.inventory.getItem(995).getStackSize() : 0;

            if (gold >= 30)
            {
                if (Quests.isFinished(Quest.PIRATES_TREASURE))
                {
                    transports.add(npcTransport(new WorldPoint(3027, 3218, 0), new WorldPoint(2956, 3143, 1), 3644, "Pay-fare"));
                    transports.add(npcTransport(new WorldPoint(2954, 3147, 0), new WorldPoint(3032, 3217, 1), 3648, "Pay-Fare"));
                }
                else
                {
                    transports.add(npcDialogTransport(new WorldPoint(3027, 3218, 0), new WorldPoint(2956, 3143, 1), 3644, "Yes please."));
                    transports.add(npcDialogTransport(new WorldPoint(2954, 3147, 0), new WorldPoint(3032, 3217, 1), 3648, "Can I journey on this ship?", "Search away, I have nothing to hide.", "Ok"));
                }
            }

            if (ctx.worldHopper.isCurrentWorldMembers()) {
                //Shamans
                transports.add(objectTransport(new WorldPoint(1312, 3685, 0), new WorldPoint(1312, 10086, 0), 34405, "Enter"));

                //Doors for shamans
                transports.add(objectTransport(new WorldPoint(1293, 10090, 0), new WorldPoint(1293, 10093, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1293, 10093, 0), new WorldPoint(1293, 10091, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1296, 10096, 0), new WorldPoint(1298, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1298, 10096, 0), new WorldPoint(1296, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1307, 10096, 0), new WorldPoint(1309, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1309, 10096, 0), new WorldPoint(1307, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1316, 10096, 0), new WorldPoint(1318, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1318, 10096, 0), new WorldPoint(1316, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1324, 10096, 0), new WorldPoint(1326, 10096, 0), 34642, "Pass"));
                transports.add(objectTransport(new WorldPoint(1326, 10096, 0), new WorldPoint(1324, 10096, 0), 34642, "Pass"));

                // Crabclaw island
                if (gold >= 10_000)
                {
                    transports.add(npcTransport(new WorldPoint(1782, 3458, 0), new WorldPoint(1778, 3417, 0), 7483, "Travel"));
                }

                transports.add(npcTransport(new WorldPoint(1779, 3418, 0), new WorldPoint(1784, 3458, 0), 7484, "Travel"));

                // Port sarim
                if (ctx.client.getVarbitValue(WalkerVarbits.VEOS_HAS_TALKED_TO_BEFORE) == 0) // First time talking to Veos
                {
                    if (ctx.client.getVarbitValue(QuestVarbits.QUEST_X_MARKS_THE_SPOT.getId()) >= 7) {
                        transports.add(npcDialogTransport(new WorldPoint(3054, 3245, 0),
                                new WorldPoint(1824, 3691, 0),
                                8484,
                                "Can you take me to Great Kourend?"));
                    } else {
                        transports.add(npcDialogTransport(new WorldPoint(3054, 3245, 0),
                                new WorldPoint(1824, 3691, 0),
                                8484,
                                "That's great, can you take me there please?"));
                    }
                }
                else // Has talked to Veos before
                {
                    transports.add(npcTransport(new WorldPoint(3054, 3245, 0),
                            new WorldPoint(1824, 3695, 1),
                            "Veos",
                            "Port Piscarilius"));
                }

                if (Quests.getState(Quest.LUNAR_DIPLOMACY) != QuestState.NOT_STARTED)
                {
                    transports.add(npcTransport(new WorldPoint(2222, 3796, 2), new WorldPoint(2130, 3899, 2), NpcID.CAPTAIN_BENTLEY_6650, "Travel"));
                    transports.add(npcTransport(new WorldPoint(2130, 3899, 2), new WorldPoint(2222, 3796, 2), NpcID.CAPTAIN_BENTLEY_6650, "Travel"));
                }

                // Spirit Trees
                if (Quests.isFinished(Quest.TREE_GNOME_VILLAGE))
                {
                    for (var source : SPIRIT_TREES)
                    {
                        if (source.location.equals("Gnome Stronghold") && !Quests.isFinished(Quest.THE_GRAND_TREE))
                        {
                            continue;
                        }
                        for (var target : SPIRIT_TREES)
                        {
                            if (source == target)
                            {
                                continue;
                            }

                            transports.add(spritTreeTransport(source.position, target.position, target.location));
                        }
                    }
                }

                if (Quests.isFinished(Quest.THE_LOST_TRIBE))
                {
                    transports.add(npcTransport(new WorldPoint(3229, 9610, 0), new WorldPoint(3316, 9613, 0), NpcID.KAZGAR_7301, "Mines"));
                    transports.add(npcTransport(new WorldPoint(3316, 9613, 0), new WorldPoint(3229, 9610, 0), NpcID.MISTAG_7299, "Cellar"));
                }

                // Tree Gnome Village
                if (Quests.getState(Quest.TREE_GNOME_VILLAGE) != QuestState.NOT_STARTED)
                {
                    transports.add(npcTransport(new WorldPoint(2504, 3192, 0), new WorldPoint(2515, 3159, 0), 4968, "Follow"));
                    transports.add(npcTransport(new WorldPoint(2515, 3159, 0), new WorldPoint(2504, 3192, 0), 4968, "Follow"));
                }

                // Eagles peak cave
                if (ctx.client.getVarpValue(934) >= 15) {
                    // Entrance
                    transports.add(objectTransport(new WorldPoint(2328, 3496, 0), new WorldPoint(1994, 4983, 3), 19790,
                            "Enter"));
                    transports.add(objectTransport(new WorldPoint(1994, 4983, 3), new WorldPoint(2328, 3496, 0), 19891,
                            "Exit"));
                }

                // Waterbirth island
                if (Quests.isFinished(Quest.THE_FREMENNIK_TRIALS) || gold >= 1000)
                {
                    transports.add(npcTransport(new WorldPoint(2544, 3760, 0), new WorldPoint(2620, 3682, 0), 10407, "Rellekka"));
                    transports.add(npcTransport(new WorldPoint(2620, 3682, 0), new WorldPoint(2547, 3759, 0), 5937, "Waterbirth Island"));
                }

                // Pirates cove
                transports.add(npcTransport(new WorldPoint(2620, 3692, 0), new WorldPoint(2213, 3794, 0), NpcID.LOKAR_SEARUNNER, "Pirate's Cove"));
                transports.add(npcTransport(new WorldPoint(2213, 3794, 0), new WorldPoint(2620, 3692, 0), NpcID.LOKAR_SEARUNNER_9306, "Rellekka"));

                // Corsair's Cove
                if (ctx.skills.getCurrentLevel(Skill.AGILITY) >= 10) {
                    transports.add(objectTransport(new WorldPoint(2546, 2871, 0), new WorldPoint(2546, 2873, 0), 31757,
                            "Climb"));
                    transports.add(objectTransport(new WorldPoint(2546, 2873, 0), new WorldPoint(2546, 2871, 0), 31757,
                            "Climb"));
                }

                // Lumbridge castle dining room, ignore if RFD is in progress.
                if (Quests.getState(Quest.RECIPE_FOR_DISASTER) != QuestState.IN_PROGRESS)
                {

                    transports.add(objectTransport(new WorldPoint(3213, 3221, 0), new WorldPoint(3212, 3221, 0), 12349, "Open"));
                    transports.add(objectTransport(new WorldPoint(3212, 3221, 0), new WorldPoint(3213, 3221, 0), 12349, "Open"));
                    transports.add(objectTransport(new WorldPoint(3213, 3222, 0), new WorldPoint(3212, 3222, 0), 12350, "Open"));
                    transports.add(objectTransport(new WorldPoint(3212, 3222, 0), new WorldPoint(3213, 3222, 0), 12350, "Open"));
                    transports.add(objectTransport(new WorldPoint(3207, 3218, 0), new WorldPoint(3207, 3217, 0), 12348, "Open"));
                    transports.add(objectTransport(new WorldPoint(3207, 3217, 0), new WorldPoint(3207, 3218, 0), 12348, "Open"));
                }

                // Digsite gate
                if (ctx.client.getVarbitValue(WalkerVarbits.KUDOS) >= 153) {
                    transports.add(objectTransport(new WorldPoint(3295, 3429, 0), new WorldPoint(3296, 3429, 0), 24561,
                            "Open"));
                    transports.add(objectTransport(new WorldPoint(3296, 3429, 0), new WorldPoint(3295, 3429, 0), 24561,
                            "Open"));
                    transports.add(objectTransport(new WorldPoint(3295, 3428, 0), new WorldPoint(3296, 3428, 0), 24561,
                            "Open"));
                    transports.add(objectTransport(new WorldPoint(3296, 3428, 0), new WorldPoint(3295, 3428, 0), 24561,
                            "Open"));
                }

                // Fairy Rings
                if (!ctx.equipment.query().id(ItemID.DRAMEN_STAFF, ItemID.LUNAR_STAFF).isEmpty()
                        && Quests.getState(Quest.FAIRYTALE_II__CURE_A_QUEEN) != QuestState.NOT_STARTED) {
                    for (FairyRingLocation sourceRing : FairyRingLocation.values()) {
                        for (FairyRingLocation destRing : FairyRingLocation.values()) {
                            if (sourceRing != destRing) {
                                transports.add(fairyRingTransport(sourceRing, destRing));
                            }
                        }
                    }
                }

                // Al Kharid to and from Ruins of Unkah
                transports.add(npcTransport(new WorldPoint(3272, 3144, 0), new WorldPoint(3148, 2842, 0), NpcID.FERRYMAN_SATHWOOD, "Ferry"));
                transports.add(npcTransport(new WorldPoint(3148, 2842, 0), new WorldPoint(3272, 3144, 0), NpcID.FERRYMAN_NATHWOOD, "Ferry"));
            }

            // Entrana
            transports.add(npcTransport(new WorldPoint(3041, 3237, 0), new WorldPoint(2834, 3331, 1), 1166, "Take-boat"));
            transports.add(npcTransport(new WorldPoint(2834, 3335, 0), new WorldPoint(3048, 3231, 1), 1170, "Take-boat"));
            transports.add(npcDialogTransport(new WorldPoint(2821, 3374, 0),
                    new WorldPoint(2822, 9774, 0),
                    1164,
                    "Well that is a risk I will have to take."));

            // Fossil Island
            transports.add(npcTransport(new WorldPoint(3362, 3445, 0),
                    new WorldPoint(3724, 3808, 0),
                    8012,
                    "Quick-Travel"));

            transports.add(objectDialogTransport(new WorldPoint(3724, 3808, 0),
                    new WorldPoint(3362, 3445, 0),
                    30914,
                    "Travel",
                    "Row to the barge and travel to the Digsite."));

            // Magic Mushtrees
            for (var source : MUSHTREES)
            {
                for (var target : MUSHTREES)
                {
                    if (source.position != target.position)
                    {
                        transports.add(mushtreeTransport(source.position, target.position, target.widget));
                    }
                }
            }

            // Gnome stronghold
            transports.add(objectDialogTransport(new WorldPoint(2460, 3382, 0), new WorldPoint(2461, 3385, 0), 190, "Open", "Sorry, I'm a bit busy."));
            transports.add(objectDialogTransport(new WorldPoint(2461, 3382, 0), new WorldPoint(2461, 3385, 0), 190, "Open", "Sorry, I'm a bit busy."));
            transports.add(objectDialogTransport(new WorldPoint(2462, 3382, 0), new WorldPoint(2461, 3385, 0), 190, "Open", "Sorry, I'm a bit busy."));

            // Paterdomus
            transports.add(trapDoorTransport(new WorldPoint(3405, 3506, 0), new WorldPoint(3405, 9906, 0), 1579, 1581));
            transports.add(trapDoorTransport(new WorldPoint(3423, 3485, 0), new WorldPoint(3440, 9887, 0), 3432, 3433));
            transports.add(trapDoorTransport(new WorldPoint(3422, 3484, 0), new WorldPoint(3440, 9887, 0), 3432, 3433));

            // Port Piscarilius
            transports.add(npcTransport(new WorldPoint(1824, 3691, 0), new WorldPoint(3055, 3242, 1), 10727, "Port Sarim"));

            // Glarial's tomb
            transports.add(itemUseTransport(new WorldPoint(2557, 3444, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
            transports.add(itemUseTransport(new WorldPoint(2557, 3445, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
            transports.add(itemUseTransport(new WorldPoint(2558, 3443, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
            transports.add(itemUseTransport(new WorldPoint(2559, 3443, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
            transports.add(itemUseTransport(new WorldPoint(2560, 3444, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
            transports.add(itemUseTransport(new WorldPoint(2560, 3445, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
            transports.add(itemUseTransport(new WorldPoint(2558, 3446, 0), new WorldPoint(2555, 9844, 0), 294, 1992));
            transports.add(itemUseTransport(new WorldPoint(2559, 3446, 0), new WorldPoint(2555, 9844, 0), 294, 1992));

            // Waterfall Island
            transports.add(itemUseTransport(new WorldPoint(2512, 3476, 0), new WorldPoint(2513, 3468, 0), 954, 1996));
            transports.add(itemUseTransport(new WorldPoint(2512, 3466, 0), new WorldPoint(2511, 3463, 0), 954, 2020));

            // Edgeville Dungeon
            transports.add(trapDoorTransport(new WorldPoint(3096, 3468, 0), new WorldPoint(3096, 9867, 0), 1579, 1581));

            // Varrock Castle manhole
            transports.add(trapDoorTransport(new WorldPoint(3237, 3459, 0), new WorldPoint(3237, 9859, 0), 881, 882));

            // Draynor manor basement
            for (var entry : DRAYNOR_MANOR_BASEMENT_DOORS.entrySet())
            {
                if (ctx.client.getVarbitValue(entry.getKey()) == 1) {
                    var points = entry.getValue();
                    transports.add(lockingDoorTransport(points.getLeft(), points.getRight(), 11450));
                    transports.add(lockingDoorTransport(points.getRight(), points.getLeft(), 11450));
                }
            }

            // Corsair Cove, Captain Tock's ship's gangplank
            transports.add(objectTransport(new WorldPoint(2578, 2837, 1), new WorldPoint(2578, 2840, 0), 31756, "Cross"));
            transports.add(objectTransport(new WorldPoint(2578, 2840, 0), new WorldPoint(2578, 2837, 1), 31756, "Cross"));

            // Corsair Cove, Ithoi the Navigator's hut stairs
            transports.add(objectTransport(new WorldPoint(2532, 2833, 0), new WorldPoint(2529, 2835, 1), 31735, "Climb"));
            transports.add(objectTransport(new WorldPoint(2529, 2835, 1), new WorldPoint(2532, 2833, 0), 31735, "Climb"));

            // Corsair Cove, Dungeon hole to Ogress Warriors/Vine ladder
            transports.add(objectTransport(new WorldPoint(2523, 2860, 0), new WorldPoint(2012, 9004, 1), 31791, "Enter"));
            transports.add(objectTransport(new WorldPoint(2012, 9004, 1), new WorldPoint(2523, 2860, 0), 31790, "Climb"));

            // Rimmington docks to and from Corsair Cove using Captain Tock's ship
            if (Quests.isFinished(Quest.THE_CORSAIR_CURSE)) {
                transports.add(npcTransport(new WorldPoint(2910, 3226, 0), new WorldPoint(2578, 2837, 1), NpcID.CABIN_BOY_COLIN_7967, "Travel"));
                transports.add(npcTransport(new WorldPoint(2574, 2835, 1), new WorldPoint(2909, 3230, 1), NpcID.CABIN_BOY_COLIN_7967, "Travel"));
            } else if (ctx.client.getVarbitValue(QuestVarbits.QUEST_THE_CORSAIR_CURSE.getId()) >= 15) {
                transports.add(npcTransport(new WorldPoint(2910, 3226, 0), new WorldPoint(2578, 2837, 1), NpcID.CAPTAIN_TOCK_7958, "Travel"));
                transports.add(npcTransport(new WorldPoint(2574, 2835, 1), new WorldPoint(2909, 3230, 1), NpcID.CAPTAIN_TOCK_7958, "Travel"));
            }

            // Draynor Jail
            transports.add(lockingDoorTransport(new WorldPoint(3123, 3244, 0), new WorldPoint(3123, 3243, 0), ObjectID.PRISON_GATE_2881));
            transports.add(lockingDoorTransport(new WorldPoint(3123, 3243, 0), new WorldPoint(3123, 3244, 0), ObjectID.PRISON_GATE_2881));

            if (!ctx.inventory.query().id(SLASH_ITEMS).isEmpty() || !ctx.equipment.query().id(SLASH_ITEMS).isEmpty()) {
                for (Pair<WorldPoint, WorldPoint> pair : SLASH_WEB_POINTS) {
                    transports.add(slashWebTransport(pair.getLeft(), pair.getRight()));
                    transports.add(slashWebTransport(pair.getRight(), pair.getLeft()));
                }
            }

            LAST_TRANSPORT_LIST.clear();
            LAST_TRANSPORT_LIST.addAll(filteredStatic);
            LAST_TRANSPORT_LIST.addAll(transports);
        });
    }

    public Transport lockingDoorTransport(
            WorldPoint source,
            WorldPoint destination,
            int openDoorId
    ) {
        return new Transport(source, destination, 0, 0, () ->
        {
            log.debug("lockingDoorTransport: " + source + " -> " + destination);
            RSObject openDoor = ctx.objects.query().id(openDoorId).distance(new RSTile(ctx, source), 1).first();
            if (openDoor != null)
            {
                openDoor.doAction("Open");
            }
        });
    }

    public Transport trapDoorTransport(
            WorldPoint source,
            WorldPoint destination,
            int closedId,
            int openedId
    ) {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            log.debug("trapDoorTransport: " + source + " -> " + destination);
            RSObject openedTrapdoor = ctx.objects.query().id(openedId).distance(new RSTile(ctx, source), 5).first();
            if (openedTrapdoor != null)
            {
                openedTrapdoor.doAction("");
                return;
            }

            RSObject closedTrapDoor = ctx.objects.query().id(closedId).distance(new RSTile(ctx, source), 5).first();
            if (closedTrapDoor != null)
            {
                closedTrapDoor.doAction("");
            }
        });
    }

    public Transport fairyRingTransport(
            FairyRingLocation source,
            FairyRingLocation destination
    ) {
        return new Transport(source.getLocation(), destination.getLocation(), Integer.MAX_VALUE, 0, () ->
        {
            log.debug("Looking for fairy ring at {} to {}", source.getLocation(), destination.getLocation());
            RSObject ring = ctx.objects.query().named("Fairy ring").distance(new RSTile(ctx, source.getLocation()), 5).first();

            if (ring == null)
            {
                log.debug("Fairy ring at {} is null", source.getLocation());
                return;
            }

            if (destination == FairyRingLocation.ZANARIS)
            {
                ring.doAction("Zanaris");
                return;
            }

            String action = Arrays.stream(ring.getDef().getActions())
                    .filter(x -> x != null && x.contains(destination.getCode()))
                    .findFirst()
                    .orElse(null);

            if (action != null)
            {
                ring.doAction(action);
                return;
            }

            if (ctx.interfaces.getComponent(WidgetInfo.FAIRY_RING).isVisible()) {
                destination.travel(ctx);
                return;
            }

            ring.doAction("Configure");
        });
    }

    public Transport itemUseTransport(
            WorldPoint source,
            WorldPoint destination,
            int itemId,
            int objId
    ) {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            log.debug("itemUseTransport: " + source + " -> " + destination);
            RSItem item = ctx.inventory.getItem(itemId);
            if (item == null)
            {
                return;
            }

            RSObject transport = ctx.objects.query().id(objId).distance(new RSTile(ctx, source), 5).first();
            if (transport != null)
            {
                ctx.inventory.useItem(itemId, transport);
            }
        });
    }

    public Transport npcTransport(
            WorldPoint source,
            WorldPoint destination,
            int npcId,
            String actions
    ) {
        return new Transport(source, destination, 10, 0, () ->
        {
            log.debug("npcTransport: " + source + " -> " + destination);
            RSNPC npc = ctx.npcs.getNearest(x -> x.getLocation().getWorldLocation().distanceTo(source) <= 10 && x.getID() == npcId);
            if (npc != null)
            {
                npc.doAction(actions);
            }
        });
    }

    public Transport npcTransport(
            WorldPoint source,
            WorldPoint destination,
            String npcName,
            String actions
    ) {
        return new Transport(source, destination, 10, 0, () ->
        {
            log.debug("npcTransport: " + source + " -> " + destination);
            RSNPC npc = ctx.npcs.getNearest(x -> x.getLocation().getWorldLocation().distanceTo(source) <= 10 && x.getName().equalsIgnoreCase(npcName));
            if (npc != null)
            {
                npc.doAction(actions);
            }
        });
    }

    public Transport npcDialogTransport(
            WorldPoint source,
            WorldPoint destination,
            int npcId,
            String... chatOptions
    ) {
        return new Transport(source, destination, 10, 0, () ->
        {
            log.debug("npcDialogTransport: " + source + " -> " + destination);
            if (ctx.npcChat.canContinue()) {
                ctx.npcChat.clickContinue(true);
                return;
            }
            if (ctx.npcChat.hasOptions()) {
                for (String option : chatOptions) {
                    if (ctx.npcChat.selectOption(option, true)) {
                        return;
                    }
                }

                return;
            }

            RSNPC npc = ctx.npcs.getNearest(x -> x.getLocation().getWorldLocation().distanceTo(source) <= 10 && x.getID() == npcId);
            if (npc != null)
            {
                npc.doAction("");
            }
        });
    }

    public Transport objectTransport(
            WorldPoint source,
            WorldPoint destination,
            int objId,
            String actions
    ) {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            log.debug("objectTransport: " + source + " -> " + destination);
            RSObject first = ctx.objects.query().id(objId).located(new RSTile(ctx, source)).first();
            if (first != null)
            {
                first.doAction(actions);
                return;
            }

            ctx.objects.query().id(objId).distance(new RSTile(ctx, source), 5).results().stream()
                    .min(Comparator.comparingInt(o -> o.getLocation().getWorldLocation().distanceTo(source)))
                    .ifPresent(obj -> obj.doAction(actions));
        });
    }

    public Transport objectTransport(
            WorldPoint source,
            WorldPoint destination,
            int objId,
            String actions,
            Requirements requirements
    ) {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            log.debug("objectTransport: " + source + " -> " + destination);
            RSObject first = ctx.objects.query().id(objId).located(new RSTile(ctx, source)).first();
            if (first != null)
            {
                log.debug("Transport found {}", first.getLocation().getWorldLocation());
                first.doAction(actions);
                return;
            }

            log.debug("Transport not found {}, {}", source, objId);
            ctx.objects.query().id(objId).distance(new RSTile(ctx, source), 5).results().stream()
                    .min(Comparator.comparingInt(o -> o.getLocation().getWorldLocation().distanceTo(source)))
                    .ifPresent(obj -> obj.doAction(actions));
        }, requirements);
    }

    /*
    public static Transport objectTransport(
            WorldPoint source,
            WorldPoint destination,
            RSObject tileObject,
            int actionIndex
    )
    {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            if (tileObject == null)
            {
                return;
            }

            tileObject.interact(actionIndex);
        });
    }
     */

    public Transport objectDialogTransport(
            WorldPoint source,
            WorldPoint destination,
            int objId,
            String actions,
            String chatOptions
    ) {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            log.debug("objectDialogTransport: " + source + " -> " + destination);
            if (ctx.npcChat.isOpen()) {
                if (ctx.npcChat.canContinue()) {
                    ctx.npcChat.clickContinue(true);
                    return;
                }

                if (ctx.npcChat.selectOption(chatOptions, true)) {
                    return;
                }

                return;
            }

            RSObject transport = ctx.objects.query().id(objId).distance(new RSTile(ctx, source), 5).first();
            if (transport != null)
            {
                transport.doAction(actions);
            }
        });
    }

    public Transport slashWebTransport(
            WorldPoint source,
            WorldPoint destination
    ) {
        return new Transport(source, destination, Integer.MAX_VALUE, 0, () ->
        {
            log.debug("slashWebTransport: " + source + " -> " + destination);
            RSObject transport = ctx.objects.query().namedContains("Web").distance(new RSTile(ctx, source), 5).first();
            // TODO: check has action
            //RSObject transport = TileObjects.getFirstSurrounding(source, 5, it -> it.getName() != null && it.getName().contains("Web") && it.hasAction("Slash"));
            if (transport != null)
            {
                transport.doAction("Slash");
            }
            else
            {
                DeviousWalker.walk(ctx, destination);
            }
        });
    }

    private Transport spritTreeTransport(WorldPoint source, WorldPoint target, String location) {
        return new Transport(
                source,
                target,
                5,
                0,
                () ->
                {
                    log.debug("spritTreeTransport: " + source + " -> " + target);
                    RSWidget treeWidget = ctx.interfaces.getComponent(187, 3);
                    if (treeWidget.isVisible())
                    {
                        // TODO: check if this action works, if not do click
                        Arrays.stream(treeWidget.getComponents())
                                .filter(child -> child.getText().toLowerCase().contains(location.toLowerCase()))
                                .findFirst()
                        //.ifPresent(child -> child.interact(0, MenuAction.WIDGET_CONTINUE.getId(), child.getIndex(), child.getId()));
                                .ifPresent(child -> child.doAction(""));
                        return;
                    }

                    RSObject tree = ctx.objects.query().id(1293, 1294, 1295).distance(new RSTile(ctx, source), 5).first();
                    if (tree != null)
                    {
                        // TODO: check this works
                        /*
                        final Point point = tree.menuPoint();
                        tree.interact(tree.getId(), MenuAction.GAME_OBJECT_FIRST_OPTION.getId(), point.getX(), point.getY());
                         */
                        tree.doAction("");
                    }
                });
    }

    private Transport mushtreeTransport(WorldPoint source, WorldPoint target, WalkerWidgetInfo widget) {
        return new Transport(
                source,
                target,
                5,
                0,
                () ->
                {
                    log.debug("mushtreeTransport: " + source + " -> " + target);
                    RSWidget treeWidget = ctx.interfaces.getComponent(widget.getGroupId(), widget.getChildId());
                    if (treeWidget.isVisible())
                    {
                        // TODO:
                        treeWidget.doAction("");
                        //treeWidget.interact(0, MenuAction.WIDGET_CONTINUE.getId(), treeWidget.getIndex(), treeWidget.getId());
                        return;
                    }

                    RSObject tree = ctx.objects.query().named("Magic Mushtree").distance(new RSTile(ctx, source), 5).first();
                    if (tree != null)
                    {
                        tree.doAction("Use");
                    }
                });
    }

    public static class MagicMushtree
    {
        private final WorldPoint position;
        private final WalkerWidgetInfo widget;

        public MagicMushtree(WorldPoint position, WalkerWidgetInfo widget)
        {
            this.position = position;
            this.widget = widget;
        }
    }

    public static class SpiritTree
    {
        private final WorldPoint position;
        private final String location;

        public SpiritTree(WorldPoint position, String location)
        {
            this.position = position;
            this.location = location;
        }
    }
}
