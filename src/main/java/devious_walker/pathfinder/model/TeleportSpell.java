package devious_walker.pathfinder.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.rsb.internal.globval.enums.MagicBook;
import net.runelite.rsb.internal.globval.enums.Spell;
import net.runelite.rsb.methods.MethodContext;

@AllArgsConstructor
@Getter
public enum TeleportSpell
{
	LUMBRIDGE_HOME_TELEPORT(
			MagicBook.Standard.HOME_TELEPORT,
			new WorldPoint(3225, 3219, 0)
	),
	TELEPORT_TO_HOUSE(
			MagicBook.Standard.TELEPORT_TO_HOUSE,
			MovementConstants.HOUSE_POINT
	),
	VARROCK_TELEPORT(
			MagicBook.Standard.VARROCK_TELEPORT,
			new WorldPoint(3212, 3424, 0)
	),
	LUMBRIDGE_TELEPORT(
			MagicBook.Standard.LUMBRIDGE_TELEPORT,
			new WorldPoint(3225, 3219, 0)
	),
	FALADOR_TELEPORT(
			MagicBook.Standard.FALADOR_TELEPORT,
			new WorldPoint(2966, 3379, 0)
	),
	CAMELOT_TELEPORT(
			MagicBook.Standard.CAMELOT_TELEPORT,
			new WorldPoint(2757, 3479, 0)
	),
	ARDOUGNE_TELEPORT(
			MagicBook.Standard.ARDOUGNE_TELEPORT,
			new WorldPoint(2661, 3300, 0)
	),
	WATCHTOWER_TELEPORT(
			MagicBook.Standard.WATCHTOWER_TELEPORT,
			new WorldPoint(2931, 4717, 2)
	),
	TROLLHEIM_TELEPORT(
			MagicBook.Standard.TROLLHEIM_TELEPORT,
			new WorldPoint(2888, 367, 0)
	),
	TELEPORT_TO_KOUREND(
			MagicBook.Standard.KOUREND_CASTLE_TELEPORT,
			new WorldPoint(1643, 3673, 0)
	),
	TELEPORT_TO_APE_ATOLL(
			MagicBook.Standard.APE_ATOLL_TELEPORT_STANDARD,
			new WorldPoint(2796, 2791, 0)
	),

	// ANCIENT TELEPORTS
	EDGEVILLE_HOME_TELEPORT(
			MagicBook.Ancient.EDGEVILLE_HOME_TELEPORT,
			new WorldPoint(3087, 3496, 0)
	),
	PADDEWWA_TELEPORT(
			MagicBook.Ancient.PADDEWWA_TELEPORT,
			new WorldPoint(3098, 9883, 0)
	),
	SENNTISTEN_TELEPORT(
			MagicBook.Ancient.SENNTISTEN_TELEPORT,
			new WorldPoint(3321, 3335, 0)
	),
	KHARYRLL_TELEPORT(
			MagicBook.Ancient.KHARYRLL_TELEPORT,
			new WorldPoint(3493, 3474, 0)
	),
	LASSAR_TELEPORT(
			MagicBook.Ancient.LASSAR_TELEPORT,
			new WorldPoint(3004, 3468, 0)
	),
	DAREEYAK_TELEPORT(
			MagicBook.Ancient.DAREEYAK_TELEPORT,
			new WorldPoint(2969, 3695, 0)
	),
	CARRALLANGER_TELEPORT(
			MagicBook.Ancient.CARRALLANGER_TELEPORT,
			new WorldPoint(3157, 3667, 0)
	),
	ANNAKARL_TELEPORT(
			MagicBook.Ancient.ANNAKARL_TELEPORT,
			new WorldPoint(3288, 3888, 0)
	),
	GHORROCK_TELEPORT(
			MagicBook.Ancient.GHORROCK_TELEPORT,
			new WorldPoint(2977, 3872, 0)
	),

	// LUNAR TELEPORTS
	LUNAR_HOME_TELEPORT(
			MagicBook.Lunar.LUNAR_HOME_TELEPORT,
			new WorldPoint(2094, 3914, 0)
	),
	MOONCLAN_TELEPORT(
			MagicBook.Lunar.MOONCLAN_TELEPORT,
			new WorldPoint(2113, 3917, 0)
	),
	OURANIA_TELEPORT(
			MagicBook.Lunar.OURANIA_TELEPORT,
			new WorldPoint(2470, 3247, 0)),
	WATERBIRTH_TELEPORT(
			MagicBook.Lunar.WATERBIRTH_TELEPORT,
			new WorldPoint(2548, 3758, 0)
	),
	BARBARIAN_TELEPORT(
			MagicBook.Lunar.BARBARIAN_TELEPORT,
			new WorldPoint(2545, 3571, 0)
	),
	KHAZARD_TELEPORT(
			MagicBook.Lunar.KHAZARD_TELEPORT,
			new WorldPoint(2637, 3168, 0)
	),
	FISHING_GUILD_TELEPORT(
			MagicBook.Lunar.FISHING_GUILD_TELEPORT,
			new WorldPoint(2610, 3389, 0)
	),
	CATHERBY_TELEPORT(
			MagicBook.Lunar.CATHERBY_TELEPORT,
			new WorldPoint(2802, 3449, 0)
	),
	ICE_PLATEAU_TELEPORT(
			MagicBook.Lunar.ICE_PLATEAU_TELEPORT,
			new WorldPoint(2973, 3939, 0)
	),

	// NECROMANCY TELEPORTS
	ARCEUUS_HOME_TELEPORT(
			MagicBook.Necromancy.ARCEUUS_HOME_TELEPORT,
			new WorldPoint(1699, 3882, 0)
	),
	ARCEUUS_LIBRARY_TELEPORT(
			MagicBook.Necromancy.ARCEUUS_LIBRARY_TELEPORT,
			new WorldPoint(1634, 3836, 0)
	),
	DRAYNOR_MANOR_TELEPORT(
			MagicBook.Necromancy.DRAYNOR_MANOR_TELEPORT,
			new WorldPoint(3109, 3352, 0)
	),
	BATTLEFRONT_TELEPORT(
			MagicBook.Necromancy.BATTLEFRONT_TELEPORT,
			new WorldPoint(1350, 3740, 0)
	),
	MIND_ALTAR_TELEPORT(
			MagicBook.Necromancy.MIND_ALTAR_TELEPORT,
			new WorldPoint(2978, 3508, 0)
	),
	RESPAWN_TELEPORT(
			MagicBook.Necromancy.RESPAWN_TELEPORT,
			new WorldPoint(3262, 6074, 0)
	),
	SALVE_GRAVEYARD_TELEPORT(
			MagicBook.Necromancy.SALVE_GRAVEYARD_TELEPORT,
			new WorldPoint(3431, 3460, 0)
	),
	FENKENSTRAINS_CASTLE_TELEPORT(
			MagicBook.Necromancy.FENKENSTRAINS_CASTLE_TELEPORT,
			new WorldPoint(3546, 3528, 0)
	),
	WEST_ARDOUGNE_TELEPORT(
			MagicBook.Necromancy.WEST_ARDOUGNE_TELEPORT,
			new WorldPoint(2502, 3291, 0)
	),
	HARMONY_ISLAND_TELEPORT(
			MagicBook.Necromancy.HARMONY_ISLAND_TELEPORT,
			new WorldPoint(3799, 2867, 0)
	),
	CEMETERY_TELEPORT(
			MagicBook.Necromancy.CEMETERY_TELEPORT,
			new WorldPoint(2978, 3763, 0)
	),
	BARROWS_TELEPORT(
			MagicBook.Necromancy.BARROWS_TELEPORT,
			new WorldPoint(3563, 3313, 0)
	),
	APE_ATOLL_TELEPORT(
			MagicBook.Necromancy.APE_ATOLL_TELEPORT_ARCEUUS,
			new WorldPoint(2769, 9100, 0)
	),
	;

	@Getter
	private final Spell spell;
	@Getter
	private final WorldPoint point;

    public boolean canCast(MethodContext ctx) {
        return spell.canCast(ctx);
    }
}
