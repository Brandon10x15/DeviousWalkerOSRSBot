package devious_walker.pathfinder;

import devious_walker.GameThread;
import devious_walker.pathfinder.model.Teleport;
import devious_walker.pathfinder.model.TeleportItem;
import devious_walker.pathfinder.model.TeleportMinigame;
import devious_walker.pathfinder.model.TeleportSpell;
import devious_walker.pathfinder.model.poh.HousePortal;
import devious_walker.quests.Quests;
import devious_walker.region.RegionManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;
import net.runelite.api.Quest;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.rsb.internal.globval.enums.InterfaceTab;
import net.runelite.rsb.internal.globval.enums.MagicBook;
import net.runelite.rsb.internal.globval.enums.Spell;
import net.runelite.rsb.methods.MethodContext;
import net.runelite.rsb.wrappers.RSItem;
import net.runelite.rsb.wrappers.RSObject;
import net.runelite.rsb.wrappers.RSWidget;

import java.util.*;
import java.util.regex.Pattern;

import static devious_walker.pathfinder.model.MovementConstants.*;
import static net.runelite.rsb.methods.Methods.sleep;

@Slf4j
public class TeleportLoader {
    private Pattern WILDY_PATTERN = Pattern.compile("Okay, teleport to level [\\d,]* Wilderness\\.");
    private List<Teleport> LAST_TELEPORT_LIST = new ArrayList<>();
    private final MethodContext ctx;

    public TeleportLoader(MethodContext ctx) {
        this.ctx = ctx;
    }


    public List<Teleport> buildTeleports() {
        List<Teleport> teleports = new ArrayList<>();
        teleports.addAll(LAST_TELEPORT_LIST);
        teleports.addAll(buildTimedTeleports());
        return teleports;
    }

    private List<Teleport> buildTimedTeleports() {
        return GameThread.invokeLater(() -> {
            List<Teleport> teleports = new ArrayList<>();
            if (ctx.worldHopper.isCurrentWorldMembers()) {
                if (ctx.combat.getWildernessLevel() == 0) {
                    // Minigames
                    if (RegionManager.useMinigameTeleports() && TeleportMinigame.canTeleport()) {
                        for (TeleportMinigame tp : TeleportMinigame.values()) {
                            if (tp.canUse()) {
                                teleports.add(new Teleport(tp.getLocation(), 2, () -> TeleportMinigame.teleport(tp)));
                            }
						}
					}
				}
			}

            if (ctx.combat.getWildernessLevel() <= 20) {
                for (TeleportSpell teleportSpell : TeleportSpell.values()) {
                    if (!teleportSpell.canCast(ctx) || teleportSpell.getPoint() == null) {
                        continue;
                    }

                    if (teleportSpell.getPoint().distanceTo(ctx.players.getMyPlayer().getLocation().getWorldLocation()) > 50) {
                        teleports.add(new Teleport(teleportSpell.getPoint(), 5, () ->
                        {
                            log.debug("Teleporting to {}", teleportSpell.getSpell());

                            final Spell spell = teleportSpell.getSpell();
                            if (teleportSpell == TeleportSpell.TELEPORT_TO_HOUSE) {
                                // Tele to outside
                                RSWidget widget = spell.getWidget(ctx);
								if (widget == null)
								{
									return;
								}
								// TODO: what action string or maybe just click
								widget.doAction("");
							}
							else
							{
                                ctx.magic.castSpell(spell);
							}
						}));
					}
				}
			}

			return teleports;
		});
	}

    public void refreshTeleports() {
        GameThread.invoke(() ->
        {
            List<Teleport> teleports = new ArrayList<>();
            if (ctx.worldHopper.isCurrentWorldMembers()) {
                // One click teleport items
                for (TeleportItem tele : TeleportItem.values()) {
                    if (tele.canUse() && tele.getDestination().distanceTo(ctx.players.getMyPlayer().getLocation().getWorldLocation()) > 20) {
                        if (tele == TeleportItem.ROYAL_SEED_POD) {
                            if (ctx.combat.getWildernessLevel() <= 30) {
                                teleports.add(itemTeleport(tele));
                            }
                        }
                        if (ctx.combat.getWildernessLevel() <= 20) {
                            teleports.add(itemTeleport(tele));
                        }
                    }
                }

                if (ctx.combat.getWildernessLevel() <= 20) {
                    if (ringOfDueling()) {
                        teleports.add(new Teleport(new WorldPoint(3315, 3235, 0), 6,
                                () -> jewelryTeleport("PvP Arena", RING_OF_DUELING)));
                        teleports.add(new Teleport(new WorldPoint(2440, 3090, 0), 2,
                                () -> jewelryTeleport("Castle Wars", RING_OF_DUELING)));
                        teleports.add(new Teleport(new WorldPoint(3151, 3635, 0), 2,
                                () -> jewelryTeleport("Ferox Enclave", RING_OF_DUELING)));
                    }

					if (gamesNecklace())
					{
						teleports.add(new Teleport(new WorldPoint(2898, 3553, 0), 2,
							() -> jewelryTeleport("Burthorpe", GAMES_NECKLACE)));
						teleports.add(new Teleport(new WorldPoint(2520, 3571, 0), 6,
							() -> jewelryTeleport("Barbarian Outpost", GAMES_NECKLACE)));
						teleports.add(new Teleport(new WorldPoint(2964, 4382, 2), 2,
							() -> jewelryTeleport("Corporeal Beast", GAMES_NECKLACE)));
						teleports.add(new Teleport(new WorldPoint(3244, 9501, 2), 2,
							() -> jewelryTeleport("Tears of Guthix", GAMES_NECKLACE)));
						teleports.add(new Teleport(new WorldPoint(1624, 3938, 0), 1,
							() -> jewelryTeleport("Wintertodt Camp", GAMES_NECKLACE)));
					}

					if (necklaceOfPassage())
					{
						teleports.add(new Teleport(new WorldPoint(3114, 3179, 0), 2,
							() -> jewelryTeleport("Wizards' Tower", NECKLACE_OF_PASSAGE)));
						teleports.add(new Teleport(new WorldPoint(2430, 3348, 0), 2,
							() -> jewelryTeleport("The Outpost", NECKLACE_OF_PASSAGE)));
						teleports.add(new Teleport(new WorldPoint(3405, 3157, 0), 2,
							() -> jewelryTeleport("Eagle's Eyrie", NECKLACE_OF_PASSAGE)));
					}

					if (xericsTalisman())
					{
						teleports.add(new Teleport(new WorldPoint(1576, 3530, 0), 6,
												   () -> jewelryTeleport("Xeric's Lookout", XERICS_TALISMAN)));
						teleports.add(new Teleport(new WorldPoint(1752, 3566, 0), 6,
												   () -> jewelryTeleport("Xeric's Glade", XERICS_TALISMAN)));
						teleports.add(new Teleport(new WorldPoint(1504, 3817, 0), 6,
												   () -> jewelryTeleport("Xeric's Inferno", XERICS_TALISMAN)));
						if (Quests.isFinished(Quest.ARCHITECTURAL_ALLIANCE))
						{
							teleports.add(new Teleport(new WorldPoint(1640, 3674, 0), 6,
														() -> jewelryTeleport("Xeric's Heart", XERICS_TALISMAN)));
						}
					}

					if (digsitePendant())
					{
						teleports.add(new Teleport(new WorldPoint(3341, 3445, 0), 6,
								() -> jewelryTeleport("Digsite", DIGSITE_PENDANT)));
						teleports.add(new Teleport(new WorldPoint(3764, 3869, 1), 6,
								() -> jewelryTeleport("Fossil Island", DIGSITE_PENDANT)));
						if (Quests.isFinished(Quest.DRAGON_SLAYER_II))
						{
							teleports.add(new Teleport(new WorldPoint(3549, 10456, 0), 6,
								() -> jewelryTeleport("Lithkren", DIGSITE_PENDANT)));
						}
					}
				}

				if (drakansMedalion())
				{
					teleports.add(new Teleport(new WorldPoint(3649, 3230, 0), 6,
							() -> jewelryTeleport("Ver Sinhaza", DRAKANS_MEDALLION)));
					if (Quests.isFinished(Quest.SINS_OF_THE_FATHER)) {
						teleports.add(new Teleport(new WorldPoint(3592, 3337, 0), 6,
								() -> jewelryTeleport("Darkmeyer", DRAKANS_MEDALLION)));
					}
					// TODO: figure out this varbit
					/*
					if (ctx.client.getVarbitValue()) {
						teleports.add(new Teleport(new WorldPoint(3652, 3214, 0), 6,
								() -> jewelryTeleport("Slepe", DRAKANS_MEDALLION)));
					}
					 */
				}

                if (ctx.combat.getWildernessLevel() <= 30) {
                    if (combatBracelet()) {
                        teleports.add(new Teleport(new WorldPoint(2882, 3548, 0), 2,
                                () -> jewelryTeleport("Warriors' Guild", COMBAT_BRACELET)));
                        teleports.add(new Teleport(new WorldPoint(3191, 3367, 0), 2,
                                () -> jewelryTeleport("Champions' Guild", COMBAT_BRACELET)));
                        teleports.add(new Teleport(new WorldPoint(3052, 3488, 0), 2,
                                () -> jewelryTeleport("Monastery", COMBAT_BRACELET)));
                        teleports.add(new Teleport(new WorldPoint(2655, 3441, 0), 2,
							() -> jewelryTeleport("Ranging Guild", COMBAT_BRACELET)));
					}

					if (skillsNecklace())
					{
						teleports.add(new Teleport(new WorldPoint(2611, 3390, 0), 6,
							() -> jewelryPopupTeleport("Fishing Guild", SKILLS_NECKLACE)));
						teleports.add(new Teleport(new WorldPoint(3050, 9763, 0), 6,
							() -> jewelryPopupTeleport("Mining Guild", SKILLS_NECKLACE)));
						teleports.add(new Teleport(new WorldPoint(2933, 3295, 0), 6,
							() -> jewelryPopupTeleport("Crafting Guild", SKILLS_NECKLACE)));
						teleports.add(new Teleport(new WorldPoint(3143, 3440, 0), 6,
							() -> jewelryPopupTeleport("Cooking Guild", SKILLS_NECKLACE)));
						teleports.add(new Teleport(new WorldPoint(1662, 3505, 0), 6,
							() -> jewelryPopupTeleport("Woodcutting Guild", SKILLS_NECKLACE)));
						teleports.add(new Teleport(new WorldPoint(1249, 3718, 0), 6,
							() -> jewelryPopupTeleport("Farming Guild", SKILLS_NECKLACE)));
					}

					if (ringOfWealth())
					{
						teleports.add(new Teleport(new WorldPoint(3163, 3478, 0), 2,
							() -> jewelryTeleport("Grand Exchange", RING_OF_WEALTH)));
						teleports.add(new Teleport(new WorldPoint(2996, 3375, 0), 2,
								() -> jewelryTeleport("Falador", RING_OF_WEALTH)));

						if (Quests.isFinished(Quest.THRONE_OF_MISCELLANIA))
						{
							teleports.add(new Teleport(new WorldPoint(2538, 3863, 0), 2,
									() -> jewelryTeleport("Miscellania", RING_OF_WEALTH)));
						}
						if (Quests.isFinished(Quest.BETWEEN_A_ROCK))
						{
							teleports.add(new Teleport(new WorldPoint(2828, 10166, 0), 2,
									() -> jewelryTeleport("Miscellania", RING_OF_WEALTH)));
						}

					}

					if (amuletOfGlory())
					{
						teleports.add(new Teleport(new WorldPoint(3087, 3496, 0), 0,
							() -> jewelryTeleport("Edgeville", AMULET_OF_GLORY)));
						teleports.add(new Teleport(new WorldPoint(2918, 3176, 0), 0,
							() -> jewelryTeleport("Karamja", AMULET_OF_GLORY)));
						teleports.add(new Teleport(new WorldPoint(3105, 3251, 0), 0,
							() -> jewelryTeleport("Draynor Village", AMULET_OF_GLORY)));
						teleports.add(new Teleport(new WorldPoint(3293, 3163, 0), 0,
							() -> jewelryTeleport("Al Kharid", AMULET_OF_GLORY)));
					}

					if (burningAmulet())
					{
                        teleports.add(new Teleport(new WorldPoint(3235, 3636, 0), 5,
                                () -> jewelryWildernessTeleport("Chaos Temple", BURNING_AMULET)));
                        teleports.add(new Teleport(new WorldPoint(3038, 3651, 0), 5,
                                () -> jewelryWildernessTeleport("Bandit Camp", BURNING_AMULET)));
                        teleports.add(new Teleport(new WorldPoint(3028, 3842, 0), 5,
                                () -> jewelryWildernessTeleport("Lava Maze", BURNING_AMULET)));
					}

					if (slayerRing())
					{
						teleports.add(new Teleport(new WorldPoint(2432, 3423, 0), 2,
								() -> slayerRingTeleport("Stronghold Slayer Cave", SLAYER_RING)));
						teleports.add(new Teleport(new WorldPoint(3422, 3537, 0), 2,
								() -> slayerRingTeleport("Slayer Tower", SLAYER_RING)));
						teleports.add(new Teleport(new WorldPoint(2802, 10000, 0), 2,
								() -> slayerRingTeleport("Fremennik Slayer Dungeon", SLAYER_RING)));
						teleports.add(new Teleport(new WorldPoint(3185, 4601, 0), 2,
								() -> slayerRingTeleport("Tarn's Lair", SLAYER_RING)));
						if (Quests.isFinished(Quest.MOURNINGS_END_PART_II))
						{
							teleports.add(new Teleport(new WorldPoint(2028, 4636, 0), 2,
								() -> slayerRingTeleport("Dark Beasts", SLAYER_RING)));
						}
					}
				}

                if (RegionManager.usePoh() && (canEnterHouse() || ctx.objects.getNearest(ObjectID.PORTAL_4525) != null)) {
                    if (RegionManager.hasMountedGlory()) {
                        teleports.add(mountedPohTeleport(new WorldPoint(3087, 3496, 0), ObjectID.AMULET_OF_GLORY, "Edgeville"));
                        teleports.add(mountedPohTeleport(new WorldPoint(2918, 3176, 0), ObjectID.AMULET_OF_GLORY, "Karamja"));
                        teleports.add(mountedPohTeleport(new WorldPoint(3105, 3251, 0), ObjectID.AMULET_OF_GLORY, "Draynor Village"));
                        teleports.add(mountedPohTeleport(new WorldPoint(3293, 3163, 0), ObjectID.AMULET_OF_GLORY, "Al Kharid"));
                    }

                    if (RegionManager.hasMountedDigsitePendant())
					{
						teleports.add(pohDigsitePendantTeleport(new WorldPoint(3341, 3445, 0), 1));
						teleports.add(pohDigsitePendantTeleport(new WorldPoint(3766, 3870, 1), 2));
						if (Quests.isFinished(Quest.DRAGON_SLAYER_II))
						{
							teleports.add(pohDigsitePendantTeleport(new WorldPoint(3549, 10456, 0), 3));
						}
					}

					switch (RegionManager.hasJewelryBox())
					{
						case ORNATE:
							if (Quests.isFinished(Quest.THRONE_OF_MISCELLANIA))
							{
								teleports.add(pohWidgetTeleport(new WorldPoint(2538, 3863, 0), 'j'));
							}
							teleports.add(pohWidgetTeleport(new WorldPoint(3163, 3478, 0), 'k'));
							teleports.add(pohWidgetTeleport(new WorldPoint(2996, 3375, 0), 'l'));
							if (Quests.isFinished(Quest.BETWEEN_A_ROCK))
							{
								teleports.add(pohWidgetTeleport(new WorldPoint(2828, 10166, 0), 'm'));
							}
							teleports.add(pohWidgetTeleport(new WorldPoint(3087, 3496, 0), 'n'));
							teleports.add(pohWidgetTeleport(new WorldPoint(2918, 3176, 0), 'o'));
							teleports.add(pohWidgetTeleport(new WorldPoint(3105, 3251, 0), 'p'));
							teleports.add(pohWidgetTeleport(new WorldPoint(3293, 3163, 0), 'q'));
						case FANCY:
							teleports.add(pohWidgetTeleport(new WorldPoint(2882, 3548, 0), '9'));
							teleports.add(pohWidgetTeleport(new WorldPoint(3191, 3367, 0), 'a'));
							teleports.add(pohWidgetTeleport(new WorldPoint(3052, 3488, 0), 'b'));
							teleports.add(pohWidgetTeleport(new WorldPoint(2655, 3441, 0), 'c'));
							teleports.add(pohWidgetTeleport(new WorldPoint(2611, 3390, 0), 'd'));
							teleports.add(pohWidgetTeleport(new WorldPoint(3050, 9763, 0), 'e'));
							teleports.add(pohWidgetTeleport(new WorldPoint(2933, 3295, 0), 'f'));
							teleports.add(pohWidgetTeleport(new WorldPoint(3143, 3440, 0), 'g'));
							teleports.add(pohWidgetTeleport(new WorldPoint(1662, 3505, 0), 'h'));
							teleports.add(pohWidgetTeleport(new WorldPoint(1249, 3718, 0), 'i'));
						case BASIC:
							teleports.add(pohWidgetTeleport(new WorldPoint(3315, 3235, 0), '1'));
							teleports.add(pohWidgetTeleport(new WorldPoint(2440, 3090, 0), '2'));
							teleports.add(pohWidgetTeleport(new WorldPoint(3151, 3635, 0), '3'));
							teleports.add(pohWidgetTeleport(new WorldPoint(2898, 3553, 0), '4'));
							teleports.add(pohWidgetTeleport(new WorldPoint(2520, 3571, 0), '5'));
							teleports.add(pohWidgetTeleport(new WorldPoint(2964, 4382, 2), '6'));
							teleports.add(pohWidgetTeleport(new WorldPoint(3244, 9501, 2), '7'));
							teleports.add(pohWidgetTeleport(new WorldPoint(1624, 3938, 0), '8'));
							break;
						default:
					}

					//nexus portal
					List<Teleport> nexusTeleports = getNexusTeleports();
					teleports.addAll(nexusTeleports);

					//normal house portals (remove duplicate teleports)
					RegionManager.getHousePortals().stream().
						filter(housePortal -> nexusTeleports.stream().
							noneMatch(teleport -> teleport.getDestination().equals(housePortal.getDestination()))).
						forEach(housePortal -> teleports.add(pohPortalTeleport(housePortal)));
				}
			}

			LAST_TELEPORT_LIST.clear();
			LAST_TELEPORT_LIST.addAll(teleports);
		});
	}

    public boolean canEnterHouse() {
        return ctx.inventory.contains(ItemID.TELEPORT_TO_HOUSE) || TeleportSpell.TELEPORT_TO_HOUSE.canCast(ctx);
    }

    public void enterHouse() {
        log.debug("Entering house");
        if (TeleportSpell.TELEPORT_TO_HOUSE.canCast(ctx)) {
            ctx.magic.castSpell(MagicBook.Standard.TELEPORT_TO_HOUSE);
            return;
        }

        RSItem teleTab = ctx.inventory.getItem(ItemID.TELEPORT_TO_HOUSE);
        if (teleTab != null) {
            if (ctx.game.getCurrentTab() != InterfaceTab.INVENTORY) {
                ctx.game.openTab(InterfaceTab.INVENTORY, true);
            }

            RSItem selectedItem = ctx.inventory.getSelectedItem();
            if (selectedItem != null) {
                selectedItem.doAction("Cancel");
            }

            teleTab.doAction("Break");
        }
	}

    public void jewelryTeleport(String target, int... ids) {
        log.debug("Jewelry teleporting to {} with {}", target, Arrays.toString(ids));
        RSItem inv = ctx.inventory.getItem(ids);

        if (inv != null) {
            if (!ctx.npcChat.hasOptions()) {
                if (ctx.game.getCurrentTab() != InterfaceTab.INVENTORY) {
                    ctx.game.openTab(InterfaceTab.INVENTORY, true);
                }

                RSItem selectedItem = ctx.inventory.getSelectedItem();
                if (selectedItem != null) {
                    selectedItem.doAction("Cancel");
                }

                String[] options = inv.getDefinition().getInterfaceOptions();

                if (options != null && Arrays.asList(options).contains(target)) {
                    inv.doAction(target);
                } else {
                    inv.doAction("Rub");
                }
                sleep(1200, ctx.npcChat::hasOptions);
				return;
			}
            ctx.npcChat.selectOption(target, true);
			return;
		}

		if (!RegionManager.useEquipmentJewellery())
		{
			return;
		}

        RSItem equipped = ctx.equipment.query().id(ids).first();
		if (equipped != null) {
            if (ctx.game.getCurrentTab() != InterfaceTab.EQUIPMENT) {
                ctx.game.openTab(InterfaceTab.EQUIPMENT, true);
            }
            equipped.doAction(target);
        }
	}

    public Teleport pohPortalTeleport(HousePortal housePortal) {
        return new Teleport(housePortal.getDestination(), 10, () ->
        {
            log.debug("Entering house portal");
            if (!ctx.players.getMyPlayer().isIdle() || ctx.client.getGameState() == GameState.LOADING) {
                return;
            }

            RSObject portal = ctx.objects.getNearest(housePortal.getPortalName());
			if (portal != null)
			{
				// TODO:
				//portal.doAction("Enter", "Varrock", "Seers' Village", "Watchtower");
				portal.doAction("");
				return;
			}

			enterHouse();
		});
	}

    public List<Teleport> getNexusTeleports() {
        List<Teleport> result = new ArrayList<>();
        int[] varbitArray = {
                6672, 6673, 6674, 6675, 6676, 6677, 6678, 6679, 6680,
                6681, 6682, 6683, 6684, 6685, 6686, 6568, 6569, 6582,
                10092, 10093, 10094, 10095, 10096, 10097, 10098,
                10099, 10100, 10101, 10102, 10103
        };

        for (int varbit : varbitArray) {
            int id = ctx.client.getVarbitValue(varbit);
            switch (id) {
                case 0 -> {
                }
                case 1 -> result.add(pohNexusTeleport(HousePortal.VARROCK));
                case 2 -> result.add(pohNexusTeleport(HousePortal.LUMBRIDGE));
                case 3 -> result.add(pohNexusTeleport(HousePortal.FALADOR));
                case 4 -> result.add(pohNexusTeleport(HousePortal.CAMELOT));
                case 5 -> result.add(pohNexusTeleport(HousePortal.EAST_ARDOUGNE));
                case 6 -> result.add(pohNexusTeleport(HousePortal.WATCHTOWER));
                case 7 -> result.add(pohNexusTeleport(HousePortal.SENNTISTEN));
                case 8 -> result.add(pohNexusTeleport(HousePortal.MARIM));
                case 9 -> result.add(pohNexusTeleport(HousePortal.KHARYRLL));
                case 10 -> result.add(pohNexusTeleport(HousePortal.LUNAR_ISLE));
                case 11 -> result.add(pohNexusTeleport(HousePortal.KOUREND));
                case 12 -> result.add(pohNexusTeleport(HousePortal.WATERBIRTH_ISLAND));
                case 13 -> result.add(pohNexusTeleport(HousePortal.FISHING_GUILD));
                case 14 -> result.add(pohNexusTeleport(HousePortal.ANNAKARL)); //wilderness
                case 15 -> result.add(pohNexusTeleport(HousePortal.TROLL_STRONGHOLD));
                case 16 -> result.add(pohNexusTeleport(HousePortal.CATHERBY));
                case 17 -> result.add(pohNexusTeleport(HousePortal.GHORROCK)); //wilderness
                case 18 -> result.add(pohNexusTeleport(HousePortal.CARRALLANGAR)); //wilderness
                case 19 -> result.add(pohNexusTeleport(HousePortal.WEISS));
                case 20 -> result.add(pohNexusTeleport(HousePortal.ARCEUUS_LIBRARY));
                case 21 -> result.add(pohNexusTeleport(HousePortal.DRAYNOR_MANOR));
                case 22 -> result.add(pohNexusTeleport(HousePortal.BATTLEFRONT));
                case 23 -> result.add(pohNexusTeleport(HousePortal.MIND_ALTAR));
                case 24 -> result.add(pohNexusTeleport(HousePortal.SALVE_GRAVEYARD));
                case 25 -> result.add(pohNexusTeleport(HousePortal.FENKENSTRAINS_CASTLE));
                case 26 -> result.add(pohNexusTeleport(HousePortal.WEST_ARDOUGNE));
                case 27 -> result.add(pohNexusTeleport(HousePortal.HARMONY_ISLAND));
                case 28 -> result.add(pohNexusTeleport(HousePortal.CEMETERY)); //wilderness
                case 29 -> result.add(pohNexusTeleport(HousePortal.BARROWS));
                case 30 -> result.add(pohNexusTeleport(HousePortal.APE_ATOLL_DUNGEON));
            }
        }

		return result;
	}

    public Teleport pohNexusTeleport(HousePortal housePortal) {
        WorldPoint destination = housePortal.getDestination();
        return new Teleport(destination, 10, () ->
        {
            log.debug("Teleporting with POH Nexus: {}", housePortal);
            if (!ctx.players.getMyPlayer().isIdle() || ctx.client.getGameState() == GameState.LOADING) {
                return;
            }

            RSObject nexusPortal = ctx.objects.getNearest("Portal Nexus");
			if (nexusPortal == null)
			{
				enterHouse();
				return;
			}

            Widget teleportInterface = ctx.client.getWidget(17, 12);
			if (teleportInterface == null || teleportInterface.isHidden())
			{
				nexusPortal.doAction("Teleport Menu");
				return;
			}

			Widget[] teleportChildren = teleportInterface.getDynamicChildren();
			if (teleportChildren == null || teleportChildren.length == 0)
			{
				//no teleports in the portal
				return;
			}

			Optional<Widget> optionalTeleportWidget = Arrays.stream(teleportChildren).
				filter(Objects::nonNull).
				filter(widget -> widget.getText() != null).
				filter(widget -> widget.getText().contains(housePortal.getNexusTarget())).
				findFirst();

			if (optionalTeleportWidget.isEmpty())
			{
				//the teleport is not in the list
				return;
			}

			Widget teleportWidget = optionalTeleportWidget.get();
			String teleportChar = teleportWidget.getText().substring(12, 13);
            ctx.keyboard.sendText(teleportChar, false);
		});
	}

    public void jewelryPopupTeleport(String target, int... ids) {
        log.debug("Teleporting with jewelry: {}", target);
        RSItem inv = ctx.inventory.getItem(ids);

        if (inv != null) {
            if (ctx.game.getCurrentTab() != InterfaceTab.INVENTORY) {
                ctx.game.openTab(InterfaceTab.INVENTORY, true);
            }

            RSItem selectedItem = ctx.inventory.getSelectedItem();
            if (selectedItem != null) {
                selectedItem.doAction("Cancel");
            }

            RSWidget baseWidget = ctx.interfaces.getComponent(187, 3);
            if (baseWidget.isVisible()) {
				RSWidget[] children = baseWidget.getComponents();
				if (children == null)
				{
					return;
				}

				for (int i = 0; i < children.length; i++)
				{
					RSWidget teleportItem = children[i];
					if (teleportItem.getText().contains(target))
					{
                        ctx.keyboard.sendText("" + (i + 1), false);
						return;
					}
				}
			}

			inv.doAction("Rub");
			return;
		}

		if (!RegionManager.useEquipmentJewellery())
		{
			return;
		}

        RSItem equipped = ctx.equipment.query().id(ids).first();
		if (equipped != null)
		{
			equipped.doAction(target);
		}
	}

    public Teleport pohDigsitePendantTeleport(
            WorldPoint destination,
            int action
    ) {
        return new Teleport(destination, 10, () ->
        {
            log.debug("Teleporting with POH Digsite Pendant: {}", destination);
            if (!ctx.players.getMyPlayer().isIdle() || ctx.client.getGameState() == GameState.LOADING) {
                return;
            }

            if (ctx.interfaces.getComponent(WidgetInfo.ADVENTURE_LOG).isVisible()) {
                ctx.keyboard.sendText("" + action, false);
                return;
            }

            RSObject digsitePendant = ctx.objects.getNearest("Digsite Pendant");
			if (digsitePendant != null)
			{
				digsitePendant.doAction("Teleport menu");
				return;
			}

			enterHouse();
		});
	}

    public Teleport itemTeleport(TeleportItem teleportItem) {
        return new Teleport(teleportItem.getDestination(), 5, () ->
        {
            log.debug("Teleporting with item: {}", teleportItem);
            RSItem item = ctx.inventory.getItem(teleportItem.getItemId());
            if (item != null) {
                if (ctx.game.getCurrentTab() != InterfaceTab.INVENTORY) {
                    ctx.game.openTab(InterfaceTab.INVENTORY, true);
                }

                RSItem selectedItem = ctx.inventory.getSelectedItem();
                if (selectedItem != null) {
                    selectedItem.doAction("Cancel");
                }

                item.doAction(teleportItem.getAction());
            }
			});
	}


    public Teleport pohWidgetTeleport(
            WorldPoint destination,
            char action
    ) {
        return new Teleport(destination, 10, () ->
        {
            log.debug("Teleporting with POH Widget: {}", destination);
            if (!ctx.players.getMyPlayer().isIdle() || ctx.client.getGameState() == GameState.LOADING) {
                return;
            }
            if (ctx.interfaces.getComponent(590, 0).isVisible()) {
                ctx.keyboard.sendText("" + action, false);
                return;
            }

            RSObject box = ctx.objects.getNearest(to -> to.getName() != null && to.getName().contains("Jewellery Box"));
			if (box != null)
			{
				box.doAction("Teleport Menu");
				return;
			}

			enterHouse();
		});
	}

    public Teleport mountedPohTeleport(
            WorldPoint destination,
            int objId,
            String action
    ) {
        return new Teleport(destination, 10, () ->
        {
            log.debug("Teleporting with POH Mounted: {}", destination);
            if (!ctx.players.getMyPlayer().isIdle() || ctx.client.getGameState() == GameState.LOADING) {
                return;
            }

            RSObject first = ctx.objects.getNearest(objId);
			if (first != null)
			{
				first.doAction(action);
				return;
			}

			enterHouse();
		});
	}

    public void jewelryWildernessTeleport(String target, int... ids) {
        jewelryTeleport(target, ids);
        sleep(1000);
        if (ctx.npcChat.hasOptions() && Arrays.stream(ctx.npcChat.getOptions())
                .anyMatch(it -> it != null && WILDY_PATTERN.matcher(it).matches())) {
            ctx.npcChat.selectOption(1, true, true);
        }
    }

    public void slayerRingTeleport(String target, int... ids) {
        log.debug("Teleporting with Slayer Ring: {}", target);
        RSItem ring = ctx.inventory.getItem(ids);
        if (ring != null) {
            if (ctx.game.getCurrentTab() != InterfaceTab.INVENTORY) {
                ctx.game.openTab(InterfaceTab.INVENTORY, true);
            }

            RSItem selectedItem = ctx.inventory.getSelectedItem();
            if (selectedItem != null) {
                selectedItem.doAction("Cancel");
            }
        }
		else if (ring == null && RegionManager.useEquipmentJewellery())
		{
            ring = ctx.equipment.query().id(ids).first();
			if (ring != null) {
                if (ctx.game.getCurrentTab() != InterfaceTab.EQUIPMENT) {
                    ctx.game.openTab(InterfaceTab.EQUIPMENT, true);
                }
            }
		}

		if (ring != null)
		{
            if (!ctx.npcChat.hasOptions()) {
                ring.doAction("Teleport");
                sleep(1200, ctx.npcChat::hasOptions);
                return;
            }
            if (ctx.npcChat.containsOption("Teleport")) {
                ctx.npcChat.selectOption("Teleport", true);
                return;
            }
            ctx.npcChat.selectOption(target, true);
		}
	}

    public boolean ringOfDueling() {
        return ctx.inventory.getItem(RING_OF_DUELING) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(RING_OF_DUELING).first() != null);
    }

    public boolean gamesNecklace() {
        return ctx.inventory.getItem(GAMES_NECKLACE) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(GAMES_NECKLACE).first() != null);
    }

    public boolean combatBracelet() {
        return ctx.inventory.getItem(COMBAT_BRACELET) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(COMBAT_BRACELET).first() != null);
    }

    public boolean skillsNecklace() {
        return ctx.inventory.getItem(SKILLS_NECKLACE) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(SKILLS_NECKLACE).first() != null);
    }

    public boolean ringOfWealth() {
        return ctx.inventory.getItem(RING_OF_WEALTH) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(RING_OF_WEALTH).first() != null);
    }

    public boolean amuletOfGlory() {
        return ctx.inventory.getItem(AMULET_OF_GLORY) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(AMULET_OF_GLORY).first() != null);
    }

    public boolean necklaceOfPassage() {
        return ctx.inventory.getItem(NECKLACE_OF_PASSAGE) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(NECKLACE_OF_PASSAGE).first() != null);
    }

    public boolean xericsTalisman() {
        return ctx.inventory.getItem(XERICS_TALISMAN) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(XERICS_TALISMAN).first() != null);
    }

    public boolean slayerRing() {
        return ctx.inventory.getItem(SLAYER_RING) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(SLAYER_RING).first() != null);
    }

    public boolean digsitePendant() {
        return ctx.inventory.getItem(DIGSITE_PENDANT) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(DIGSITE_PENDANT).first() != null);
    }

    public boolean drakansMedalion() {
        return ctx.inventory.getItem(DRAKANS_MEDALLION) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(DRAKANS_MEDALLION).first() != null);
    }

    public boolean burningAmulet() {
        return ctx.inventory.getItem(BURNING_AMULET) != null
                || (RegionManager.useEquipmentJewellery() && ctx.equipment.query().id(BURNING_AMULET).first() != null);
    }
}
