package devious_walker.pathfinder.model;

import devious_walker.GameThread;
import devious_walker.quests.Quests;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.rsb.internal.globval.enums.InterfaceTab;
import net.runelite.rsb.wrappers.RSWidget;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static net.runelite.rsb.methods.MethodProvider.methods;

@Getter
@AllArgsConstructor
@Slf4j
public enum TeleportMinigame {
    BARBARIAN_ASSAULT(1, "Barbarian Assault", new WorldPoint(2531, 3577, 0), false),
    BLAST_FURNACE(2, "Blast Furnace", new WorldPoint(2933, 10183, 0), true),
    BURTHORPE_GAMES_ROOM(3, "Burthorpe Games Room", new WorldPoint(2208, 4938, 0), true),
    CASTLE_WARS(4, "Castle Wars", new WorldPoint(2439, 3092, 0), false),
    CLAN_WARS(5, "Clan Wars", new WorldPoint(3151, 3636, 0), false),
    DAGANNOTH_KINGS(6, "Dagannoth Kings", null, true),
    FISHING_TRAWLER(7, "Fishing Trawler", new WorldPoint(2658, 3158, 0), true),
    GIANTS_FOUNDARY(8, "Giants' Foundry", new WorldPoint(3361, 3147, 0), true),
    GOD_WARS(9, "God Wars", null, true),
    GUARDIANS_OF_THE_RIFT(10, "Guardians of the Rift", new WorldPoint(3616, 9478, 0), true),
    LAST_MAN_STANDING(11, "Last Man Standing", new WorldPoint(3149, 3635, 0), false),
    NIGHTMARE_ZONE(12, "Nightmare Zone", new WorldPoint(2611, 3121, 0), true),
    PEST_CONTROL(13, "Pest Control", new WorldPoint(2653, 2655, 0), true),
    PLAYER_OWNED_HOUSES(14, "Player Owned Houses", null, false),
    RAT_PITS(15, "Rat Pits", new WorldPoint(3263, 3406, 0), true),
    SHADES_OF_MORTTON(16, "Shades of Mort'ton", new WorldPoint(3500, 3300, 0), true),
    SHIELD_OF_ARRAV(17, "Shield of Arrav", null, true),
    SHOOTING_STARS(18, "Shooting Stars", null, true),
    SOUL_WARS(19, "Soul Wars", new WorldPoint(2209, 2857, 0), true),
    THEATRE_OF_BLOOD(20, "Theatre of Blood", null, true),
    TITHE_FARM(21, "Tithe Farm", new WorldPoint(1793, 3501, 0), true),
    TROUBLE_BREWING(22, "Trouble Brewing", new WorldPoint(3811, 3021, 0), true),
    TZHAAR_FIGHT_PIT(23, "TzHaar Fight Pit", new WorldPoint(2402, 5181, 0), true),
    VOLCANIC_MINE(24, "Volcanic Mine", null, true),
    NONE(-1, "None", null, false);

    private final int index;
    private final String name;
    private final WorldPoint location;
    private final boolean members;

    public static void teleport(TeleportMinigame destination)
    {
        if (!canTeleport())
        {
            log.warn("Tried to minigame teleport, but it's on cooldown.");
            return;
        }

        RSWidget minigamesTeleportButton = methods.interfaces.getComponent(WidgetInfo.MINIGAME_TELEPORT_BUTTON);
        List<Integer> teleportGraphics = List.of(800, 802, 803, 804);
        if (isOpen() && minigamesTeleportButton != null)
        {
            if (destination.getCurrent() != destination)
            {
                GameThread.invoke(() -> methods.client.runScript(124, destination.index));
                return;
            }

            if (teleportGraphics.contains(methods.players.getMyPlayer().getGraphic()))
            {
                return;
            }

            RSWidget button = minigamesTeleportButton.getDynamicComponent(destination.index);
            if (button.isVisible())
            {
                // TODO:
                //button.doAction(Predicates.textContains("Teleport to"));
                button.doClick();
            }
        }
        else
        {
            open();
        }
    }

    public static boolean open()
    {
        if (!isTabOpen())
        {
            methods.game.openTab(InterfaceTab.CHAT); // TODO:
            return false;
        }

        if (!isOpen())
        {
            RSWidget widget = methods.interfaces.getComponent(707, 6);
            if (widget.isVisible())
            {
                widget.doAction("Grouping");
                return false;
            }
        }

        return isOpen();
    }

    public static boolean isOpen()
    {
        return methods.interfaces.getComponent(WidgetInfo.MINIGAME_TELEPORT_BUTTON).isVisible();
    }

    public static boolean isTabOpen()
    {
        return methods.game.getCurrentTab() == InterfaceTab.CHAT;
    }


    public static boolean canTeleport()
    {
        return getLastMinigameTeleportUsage().plus(20, ChronoUnit.MINUTES).isBefore(Instant.now());
    }
    public static Instant getLastMinigameTeleportUsage()
    {
        return Instant.ofEpochSecond(methods.client.getVarpValue(VarPlayer.LAST_MINIGAME_TELEPORT) * 60L);
    }



    public boolean canUse()
    {
        Set<Quest> NMZ_QUESTS = Set.of(
                Quest.THE_ASCENT_OF_ARCEUUS,
                Quest.CONTACT,
                Quest.THE_CORSAIR_CURSE,
                Quest.THE_DEPTHS_OF_DESPAIR,
                Quest.DESERT_TREASURE_I,
                Quest.DRAGON_SLAYER_I,
                Quest.DREAM_MENTOR,
                Quest.FAIRYTALE_I__GROWING_PAINS,
                Quest.FAMILY_CREST,
                Quest.FIGHT_ARENA,
                Quest.THE_FREMENNIK_ISLES,
                Quest.GETTING_AHEAD,
                Quest.THE_GRAND_TREE,
                Quest.THE_GREAT_BRAIN_ROBBERY,
                Quest.GRIM_TALES,
                Quest.HAUNTED_MINE,
                Quest.HOLY_GRAIL,
                Quest.HORROR_FROM_THE_DEEP,
                Quest.IN_SEARCH_OF_THE_MYREQUE,
                Quest.LEGENDS_QUEST,
                Quest.LOST_CITY,
                Quest.LUNAR_DIPLOMACY,
                Quest.MONKEY_MADNESS_I,
                Quest.MOUNTAIN_DAUGHTER,
                Quest.MY_ARMS_BIG_ADVENTURE,
                Quest.ONE_SMALL_FAVOUR,
                Quest.RECIPE_FOR_DISASTER,
                Quest.ROVING_ELVES,
                Quest.SHADOW_OF_THE_STORM,
                Quest.SHILO_VILLAGE,
                Quest.SONG_OF_THE_ELVES,
                Quest.TALE_OF_THE_RIGHTEOUS,
                Quest.TREE_GNOME_VILLAGE,
                Quest.TROLL_ROMANCE,
                Quest.TROLL_STRONGHOLD,
                Quest.VAMPYRE_SLAYER,
                Quest.WHAT_LIES_BELOW,
                Quest.WITCHS_HOUSE
        );
        if (!hasDestination())
        {
            return false;
        }

        if (members && !methods.worldHopper.isCurrentWorldMembers())
        {
            return false;
        }

        switch (this)
        {
            case BURTHORPE_GAMES_ROOM:
            case CASTLE_WARS:
            case CLAN_WARS:
            case LAST_MAN_STANDING:
            case SOUL_WARS:
            case TZHAAR_FIGHT_PIT:
            case GIANTS_FOUNDARY:
                return true;
            case BARBARIAN_ASSAULT:
                return methods.client.getVarbitValue(3251) >= 1;
            case BLAST_FURNACE:
                return methods.client.getVarbitValue(575) >= 1;
            case FISHING_TRAWLER:
                return methods.skills.getRealLevel(Skill.FISHING) >= 15;
            case GUARDIANS_OF_THE_RIFT:
                return Quests.isFinished(Quest.TEMPLE_OF_THE_EYE);
            case NIGHTMARE_ZONE:
                return NMZ_QUESTS.stream().filter(Quests::isFinished).count() >= 5;
            case PEST_CONTROL:
                return methods.players.getMyPlayer().getCombatLevel() >= 40;
            case RAT_PITS:
                return Quests.isFinished(Quest.RATCATCHERS);
            case SHADES_OF_MORTTON:
                return Quests.isFinished(Quest.SHADES_OF_MORTTON);
            case TROUBLE_BREWING:
                return Quests.isFinished(Quest.CABIN_FEVER) && methods.skills.getRealLevel(Skill.COOKING) >= 40;
            case TITHE_FARM:
                return false;
//					return Skills.getLevel(Skill.FARMING) >= 34 && (Vars.getBit(Varbits.KOUREND_FAVOR_HOSIDIUS) / 10) >= 100;
        }
        return false;
    }

    public boolean hasDestination()
    {
        return location != null;
    }

    public static TeleportMinigame getCurrent()
    {
        RSWidget selectedTeleport = methods.interfaces.getComponent(76, 11);
        if (selectedTeleport.isVisible())
        {
            return byName(selectedTeleport.getText());
        }

        return NONE;
    }

    public static TeleportMinigame byName(String name)
    {
        return Arrays.stream(values())
                .filter(x -> x.getName().equals(name))
                .findFirst()
                .orElse(NONE);
    }
}
