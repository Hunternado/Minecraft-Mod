package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.entity.boss.AuraEntity;
import com.hunternado.frieren.entity.boss.DemonKingEntity;
import com.hunternado.frieren.entity.boss.DrahtEntity;
import com.hunternado.frieren.entity.boss.LinieEntity;
import com.hunternado.frieren.entity.boss.LugnerEntity;
import com.hunternado.frieren.entity.boss.MachtEntity;
import com.hunternado.frieren.entity.boss.MirrorFrierenEntity;
import com.hunternado.frieren.entity.boss.MirrorReplicaEntity;
import com.hunternado.frieren.entity.boss.QualEntity;
import com.hunternado.frieren.entity.boss.SpiegelEntity;
import com.hunternado.frieren.entity.creature.MimicEntity;
import com.hunternado.frieren.entity.creature.StilleEntity;
import com.hunternado.frieren.entity.demon.AuraThrallEntity;
import com.hunternado.frieren.entity.demon.DemonMageEntity;
import com.hunternado.frieren.entity.demon.DemonSoldierEntity;
import com.hunternado.frieren.entity.fx.BarrierShellEntity;
import com.hunternado.frieren.entity.fx.MagicCircleEntity;
import com.hunternado.frieren.entity.fx.SpellBeamEntity;
import com.hunternado.frieren.entity.magic.ManaBoltEntity;
import com.hunternado.frieren.entity.magic.ManaDecoyEntity;
import com.hunternado.frieren.entity.magic.SpellAreaEntity;
import com.hunternado.frieren.entity.npc.CompanionEntity;
import com.hunternado.frieren.entity.npc.ExaminerEntity;
import com.hunternado.frieren.entity.npc.FrierenEntity;
import com.hunternado.frieren.entity.npc.PriestEntity;
import com.hunternado.frieren.entity.npc.SerieEntity;
import com.hunternado.frieren.entity.npc.UbelEntity;
import com.hunternado.frieren.entity.npc.WanderingMageEntity;
import com.hunternado.frieren.entity.statue.HeroStatueEntity;
import com.hunternado.frieren.entity.statue.HimmelEchoEntity;
import com.hunternado.frieren.entity.trial.ExamConstructEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.UnaryOperator;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FrierenMod.MODID);

    // ---- Demons --------------------------------------------------------------------------------
    public static final RegistryObject<EntityType<DemonSoldierEntity>> DEMON_SOLDIER = register("demon_soldier",
        EntityType.Builder.of(DemonSoldierEntity::new, MobCategory.MONSTER), b -> b.sized(0.6F, 1.95F).clientTrackingRange(8));
    public static final RegistryObject<EntityType<DemonMageEntity>> DEMON_MAGE = register("demon_mage",
        EntityType.Builder.of(DemonMageEntity::new, MobCategory.MONSTER), b -> b.sized(0.6F, 1.95F).clientTrackingRange(8));
    public static final RegistryObject<EntityType<AuraThrallEntity>> AURA_THRALL = register("aura_thrall",
        EntityType.Builder.of(AuraThrallEntity::new, MobCategory.MONSTER), b -> b.sized(0.7F, 1.75F).clientTrackingRange(8));

    // ---- Bosses --------------------------------------------------------------------------------
    public static final RegistryObject<EntityType<AuraEntity>> AURA = register("aura",
        EntityType.Builder.of(AuraEntity::new, MobCategory.MONSTER), b -> b.sized(0.7F, 2.1F).fireImmune().clientTrackingRange(10));
    public static final RegistryObject<EntityType<QualEntity>> QUAL = register("qual",
        EntityType.Builder.of(QualEntity::new, MobCategory.MONSTER), b -> b.sized(0.7F, 2.1F).fireImmune().clientTrackingRange(10));
    public static final RegistryObject<EntityType<SpiegelEntity>> SPIEGEL = register("spiegel",
        EntityType.Builder.of(SpiegelEntity::new, MobCategory.MONSTER), b -> b.sized(0.9F, 2.2F).fireImmune().clientTrackingRange(10));
    public static final RegistryObject<EntityType<MirrorReplicaEntity>> MIRROR_REPLICA = register("mirror_replica",
        EntityType.Builder.of(MirrorReplicaEntity::new, MobCategory.MONSTER), b -> b.sized(0.6F, 1.95F).clientTrackingRange(8));
    public static final RegistryObject<EntityType<DemonKingEntity>> DEMON_KING = register("demon_king",
        EntityType.Builder.of(DemonKingEntity::new, MobCategory.MONSTER), b -> b.sized(1.0F, 3.0F).fireImmune().clientTrackingRange(12));
    public static final RegistryObject<EntityType<MachtEntity>> MACHT = register("macht",
        EntityType.Builder.of(MachtEntity::new, MobCategory.MONSTER), b -> b.sized(0.7F, 2.1F).fireImmune().clientTrackingRange(10));
    public static final RegistryObject<EntityType<LinieEntity>> LINIE = register("linie",
        EntityType.Builder.of(LinieEntity::new, MobCategory.MONSTER), b -> b.sized(0.6F, 1.75F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<DrahtEntity>> DRAHT = register("draht",
        EntityType.Builder.of(DrahtEntity::new, MobCategory.MONSTER), b -> b.sized(0.6F, 1.9F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<LugnerEntity>> LUGNER = register("lugner",
        EntityType.Builder.of(LugnerEntity::new, MobCategory.MONSTER), b -> b.sized(0.6F, 1.9F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<MirrorFrierenEntity>> MIRROR_FRIEREN = register("mirror_frieren",
        EntityType.Builder.of(MirrorFrierenEntity::new, MobCategory.MONSTER), b -> b.sized(0.6F, 1.8F).fireImmune().clientTrackingRange(10));

    // ---- Monuments ------------------------------------------------------------------------------
    public static final RegistryObject<EntityType<HeroStatueEntity>> HERO_STATUE = register("hero_statue",
        EntityType.Builder.of(HeroStatueEntity::new, MobCategory.MISC), b -> b.sized(1.4F, 5.2F).fireImmune().clientTrackingRange(12));

    public static final RegistryObject<EntityType<HimmelEchoEntity>> HIMMEL_ECHO = register("himmel_echo",
        EntityType.Builder.of(HimmelEchoEntity::new, MobCategory.MISC), b -> b.sized(0.6F, 1.8F).noSave().noSummon().fireImmune()
            .clientTrackingRange(10));
    /** Eisen and Heiter, appearing beside Himmel's echo: the hero party, together again for a moment. */
    public static final RegistryObject<EntityType<HimmelEchoEntity>> EISEN_ECHO = register("eisen_echo",
        EntityType.Builder.of(HimmelEchoEntity::new, MobCategory.MISC), b -> b.sized(0.7F, 1.4F).noSave().noSummon().fireImmune()
            .clientTrackingRange(10));
    public static final RegistryObject<EntityType<HimmelEchoEntity>> HEITER_ECHO = register("heiter_echo",
        EntityType.Builder.of(HimmelEchoEntity::new, MobCategory.MISC), b -> b.sized(0.6F, 1.8F).noSave().noSummon().fireImmune()
            .clientTrackingRange(10));

    // ---- Exam trials ---------------------------------------------------------------------------
    public static final RegistryObject<EntityType<ExamConstructEntity>> EXAM_CONSTRUCT = register("exam_construct",
        EntityType.Builder.of(ExamConstructEntity::new, MobCategory.MISC), b -> b.sized(0.8F, 0.8F).fireImmune().clientTrackingRange(8).updateInterval(2));

    // ---- Creatures -----------------------------------------------------------------------------
    public static final RegistryObject<EntityType<MimicEntity>> MIMIC = register("mimic",
        EntityType.Builder.of(MimicEntity::new, MobCategory.MONSTER), b -> b.sized(0.9F, 0.9F).clientTrackingRange(8));
    public static final RegistryObject<EntityType<StilleEntity>> STILLE = register("stille",
        EntityType.Builder.of(StilleEntity::new, MobCategory.CREATURE), b -> b.sized(0.4F, 0.4F).clientTrackingRange(8));

    // ---- NPCs ----------------------------------------------------------------------------------
    public static final RegistryObject<EntityType<WanderingMageEntity>> WANDERING_MAGE = register("wandering_mage",
        EntityType.Builder.of(WanderingMageEntity::new, MobCategory.CREATURE), b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<ExaminerEntity>> EXAMINER = register("examiner",
        EntityType.Builder.of(ExaminerEntity::new, MobCategory.CREATURE), b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<PriestEntity>> PRIEST = register("priest",
        EntityType.Builder.of(PriestEntity::new, MobCategory.CREATURE), b -> b.sized(0.6F, 1.95F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<FrierenEntity>> FRIEREN = register("frieren",
        EntityType.Builder.of(FrierenEntity::new, MobCategory.CREATURE), b -> b.sized(0.55F, 1.7F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<CompanionEntity>> FERN = register("fern",
        EntityType.Builder.of(CompanionEntity::fern, MobCategory.CREATURE), b -> b.sized(0.6F, 1.85F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<CompanionEntity>> STARK = register("stark",
        EntityType.Builder.of(CompanionEntity::stark, MobCategory.CREATURE), b -> b.sized(0.65F, 1.95F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<SerieEntity>> SERIE = register("serie",
        EntityType.Builder.of(SerieEntity::new, MobCategory.CREATURE), b -> b.sized(0.55F, 1.7F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<UbelEntity>> UBEL = register("ubel",
        EntityType.Builder.of(UbelEntity::new, MobCategory.CREATURE), b -> b.sized(0.55F, 1.75F).clientTrackingRange(10));

    // ---- Magic constructs ----------------------------------------------------------------------
    public static final RegistryObject<EntityType<ManaBoltEntity>> MANA_BOLT = register("mana_bolt",
        EntityType.Builder.<ManaBoltEntity>of(ManaBoltEntity::new, MobCategory.MISC), b -> b.sized(0.3F, 0.3F).clientTrackingRange(6).updateInterval(2));
    public static final RegistryObject<EntityType<SpellAreaEntity>> SPELL_AREA = register("spell_area",
        EntityType.Builder.<SpellAreaEntity>of(SpellAreaEntity::new, MobCategory.MISC), b -> b.sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(10));
    // ---- Visual effects (never saved) -----------------------------------------------------------
    public static final RegistryObject<EntityType<MagicCircleEntity>> MAGIC_CIRCLE = register("magic_circle",
        EntityType.Builder.<MagicCircleEntity>of(MagicCircleEntity::new, MobCategory.MISC),
        b -> b.sized(0.5F, 0.5F).noSave().noSummon().fireImmune().clientTrackingRange(8).updateInterval(1));
    public static final RegistryObject<EntityType<SpellBeamEntity>> SPELL_BEAM = register("spell_beam",
        EntityType.Builder.<SpellBeamEntity>of(SpellBeamEntity::new, MobCategory.MISC),
        b -> b.sized(0.2F, 0.2F).noSave().noSummon().fireImmune().clientTrackingRange(10).updateInterval(20));
    public static final RegistryObject<EntityType<BarrierShellEntity>> BARRIER_SHELL = register("barrier_shell",
        EntityType.Builder.<BarrierShellEntity>of(BarrierShellEntity::new, MobCategory.MISC),
        b -> b.sized(0.5F, 0.5F).noSave().noSummon().fireImmune().clientTrackingRange(8).updateInterval(1));
    public static final RegistryObject<EntityType<ManaDecoyEntity>> MANA_DECOY = register("mana_decoy",
        EntityType.Builder.of(ManaDecoyEntity::new, MobCategory.MISC), b -> b.sized(0.6F, 1.8F).clientTrackingRange(8));

    private ModEntities() {}

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String name, EntityType.Builder<T> builder,
                                                                            UnaryOperator<EntityType.Builder<T>> configure) {
        return ENTITY_TYPES.register(name, () -> configure.apply(builder).build(ENTITY_TYPES.key(name)));
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DEMON_SOLDIER.get(), DemonSoldierEntity.createAttributes().build());
        event.put(DEMON_MAGE.get(), DemonMageEntity.createAttributes().build());
        event.put(AURA_THRALL.get(), AuraThrallEntity.createAttributes().build());
        event.put(AURA.get(), AuraEntity.createAttributes().build());
        event.put(QUAL.get(), QualEntity.createAttributes().build());
        event.put(SPIEGEL.get(), SpiegelEntity.createAttributes().build());
        event.put(MIRROR_REPLICA.get(), MirrorReplicaEntity.createAttributes().build());
        event.put(MIRROR_FRIEREN.get(), MirrorFrierenEntity.createAttributes().build());
        event.put(DEMON_KING.get(), DemonKingEntity.createAttributes().build());
        event.put(MACHT.get(), MachtEntity.createAttributes().build());
        event.put(LINIE.get(), LinieEntity.createAttributes().build());
        event.put(DRAHT.get(), DrahtEntity.createAttributes().build());
        event.put(LUGNER.get(), LugnerEntity.createAttributes().build());
        event.put(EXAM_CONSTRUCT.get(), ExamConstructEntity.createAttributes().build());
        event.put(HERO_STATUE.get(), HeroStatueEntity.createAttributes().build());
        event.put(HIMMEL_ECHO.get(), HimmelEchoEntity.createAttributes().build());
        event.put(EISEN_ECHO.get(), HimmelEchoEntity.createAttributes().build());
        event.put(HEITER_ECHO.get(), HimmelEchoEntity.createAttributes().build());
        event.put(MIMIC.get(), MimicEntity.createAttributes().build());
        event.put(STILLE.get(), StilleEntity.createAttributes().build());
        event.put(WANDERING_MAGE.get(), WanderingMageEntity.createAttributes().build());
        event.put(EXAMINER.get(), ExaminerEntity.createAttributes().build());
        event.put(PRIEST.get(), PriestEntity.createAttributes().build());
        event.put(FRIEREN.get(), FrierenEntity.createAttributes().build());
        event.put(FERN.get(), CompanionEntity.createAttributes().build());
        event.put(STARK.get(), CompanionEntity.createAttributes().build());
        event.put(SERIE.get(), SerieEntity.createAttributes().build());
        event.put(UBEL.get(), UbelEntity.createAttributes().build());
        event.put(MANA_DECOY.get(), ManaDecoyEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(DEMON_SOLDIER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(DEMON_MAGE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
