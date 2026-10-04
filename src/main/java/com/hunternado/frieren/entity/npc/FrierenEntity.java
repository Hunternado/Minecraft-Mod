package com.hunternado.frieren.entity.npc;

import com.hunternado.frieren.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Frieren the Slayer, travelling with Fern and Stark. A rare encounter: she trades the everyday spells she
 * collects and conceals her mana so well that detection barely notices her.
 */
public class FrierenEntity extends FrierenNpcBase {
    private static final long STAY_TICKS = 24000L;

    public FrierenEntity(EntityType<? extends FrierenEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected String npcId() {
        return "frieren";
    }

    @Override
    protected int dialogueLines() {
        return 5;
    }

    @Override
    public float trueMana() {
        return 3000.0F;
    }

    @Override
    public int concealLevel() {
        return 9;
    }

    @Override
    public List<NpcTrades.Offer> offers() {
        return NpcTrades.frieren(this.getUUID().getLeastSignificantBits());
    }

    @Override
    public String tradeTitleKey() {
        return "screen.frieren.trades.frieren";
    }

    @Override
    public void onInteract(ServerPlayer player) {
        sayRandomLine(player);
        NpcInteractions.openTrades(player, this);
    }

    /** Called about once a day: with a 1 in 3 chance, the party shows up near a random player. */
    public static void trySpawnTravellingParty(ServerLevel level) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty() || level.getRandom().nextInt(3) != 0) {
            return;
        }
        ServerPlayer player = players.get(level.getRandom().nextInt(players.size()));
        if (!level.getEntitiesOfClass(FrierenEntity.class, new AABB(player.blockPosition()).inflate(160.0D)).isEmpty()) {
            return;
        }
        double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
        int distance = 24 + level.getRandom().nextInt(16);
        BlockPos column = player.blockPosition().offset((int) (Math.cos(angle) * distance), 0, (int) (Math.sin(angle) * distance));
        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        if (!level.getBlockState(ground.below()).isSolid()) {
            return;
        }
        long departAt = level.getGameTime() + STAY_TICKS;
        FrierenEntity frieren = ModEntities.FRIEREN.get().create(level, EntitySpawnReason.EVENT);
        if (frieren == null) {
            return;
        }
        frieren.snapTo(ground.getX() + 0.5D, ground.getY(), ground.getZ() + 0.5D, 0.0F, 0.0F);
        frieren.setDepartAt(departAt);
        level.addFreshEntity(frieren);
        spawnCompanion(level, ModEntities.FERN.get(), frieren, ground.offset(1, 0, 0), departAt);
        spawnCompanion(level, ModEntities.STARK.get(), frieren, ground.offset(-1, 0, 0), departAt);
    }

    private static void spawnCompanion(ServerLevel level, EntityType<CompanionEntity> type, FrierenEntity leader, BlockPos pos, long departAt) {
        CompanionEntity companion = type.create(level, EntitySpawnReason.EVENT);
        if (companion == null) {
            return;
        }
        companion.snapTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        companion.setLeader(leader);
        companion.setDepartAt(departAt);
        level.addFreshEntity(companion);
    }
}
