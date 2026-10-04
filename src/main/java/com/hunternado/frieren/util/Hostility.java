package com.hunternado.frieren.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;

/** Single definition of "hostile mob" used by exams, quests and friendly AI. */
public final class Hostility {
    private Hostility() {}

    public static boolean isHostile(Entity entity) {
        return entity instanceof Monster || entity.getType().getCategory() == MobCategory.MONSTER;
    }
}
