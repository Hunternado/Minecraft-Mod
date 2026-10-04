package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.entity.boss.AuraEntity;
import com.hunternado.frieren.entity.boss.MirrorReplicaEntity;
import com.hunternado.frieren.entity.boss.QualEntity;
import com.hunternado.frieren.entity.boss.SpiegelEntity;
import com.hunternado.frieren.entity.creature.MimicEntity;
import com.hunternado.frieren.entity.creature.StilleEntity;
import com.hunternado.frieren.entity.demon.AuraThrallEntity;
import com.hunternado.frieren.entity.demon.DemonMageEntity;
import com.hunternado.frieren.entity.demon.DemonSoldierEntity;
import com.hunternado.frieren.entity.magic.ManaBoltEntity;
import com.hunternado.frieren.entity.magic.ManaDecoyEntity;
import com.hunternado.frieren.entity.magic.SpellAreaEntity;
import com.hunternado.frieren.entity.npc.CompanionEntity;
import com.hunternado.frieren.entity.npc.ExaminerEntity;
import com.hunternado.frieren.entity.npc.FrierenEntity;
import com.hunternado.frieren.entity.npc.PriestEntity;
import com.hunternado.frieren.entity.npc.SerieEntity;
import com.hunternado.frieren.entity.npc.WanderingMageEntity;
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

    // ---- Magic constructs ----------------------------------------------------------------------
    public static final RegistryObject<EntityType<ManaBoltEntity>> MANA_BOLT = register("mana_bolt",
        EntityType.Builder.<ManaBoltEntity>of(ManaBoltEntity::new, MobCategory.MISC), b -> b.sized(0.3F, 0.3F).clientTrackingRange(6).updateInterval(2));
    public static final RegistryObject<EntityType<SpellAreaEntity>> SPELL_AREA = register("spell_area",
        EntityType.Builder.<SpellAreaEntity>of(SpellAreaEntity::new, MobCategory.MISC), b -> b.sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(10));
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
        event.put(MIMIC.get(), MimicEntity.createAttributes().build());
        event.put(STILLE.get(), StilleEntity.createAttributes().build());
        event.put(WANDERING_MAGE.get(), WanderingMageEntity.createAttributes().build());
        event.put(EXAMINER.get(), ExaminerEntity.createAttributes().build());
        event.put(PRIEST.get(), PriestEntity.createAttributes().build());
        event.put(FRIEREN.get(), FrierenEntity.createAttributes().build());
        event.put(FERN.get(), CompanionEntity.createAttributes().build());
        event.put(STARK.get(), CompanionEntity.createAttributes().build());
        event.put(SERIE.get(), SerieEntity.createAttributes().build());
        event.put(MANA_DECOY.get(), ManaDecoyEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(DEMON_SOLDIER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(DEMON_MAGE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
