package com.hunternado.frieren.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ContextAwarePredicate;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Generic "milestone reached" advancement trigger, fired from code for ranks, quests and discoveries.
 * JSON: {@code {"trigger": "frieren:milestone", "conditions": {"milestone": "rank_first_class"}}}
 */
public final class MilestoneTrigger extends SimpleCriterionTrigger<MilestoneTrigger.Instance> {
    private static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("milestone").forGetter(Instance::milestone)
    ).apply(instance, Instance::new));

    @Override
    public Codec<Instance> codec() {
        return CODEC;
    }

    public Criterion<Instance> criterion(String milestone) {
        return this.createCriterion(new Instance(milestone));
    }

    public void trigger(ServerPlayer player, String milestone) {
        this.trigger(player, instance -> instance.milestone().equals(milestone));
    }

    public record Instance(String milestone) implements SimpleCriterionTrigger.SimpleInstance {
        @Override
        public Optional<ContextAwarePredicate> player() {
            return Optional.empty();
        }
    }
}
