package com.hunternado.frieren.item;

import com.hunternado.frieren.FrierenIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;

/**
 * Mage's Attire: weak as armour, strong as a mana focus. The mana bonuses are applied by ManaHelper.
 */
public final class ModArmorMaterials {
    public static final TagKey<Item> REPAIRS_MAGE_ATTIRE = TagKey.create(Registries.ITEM, FrierenIds.id("repairs_mage_attire"));
    public static final ResourceKey<EquipmentAsset> MAGE_ATTIRE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, FrierenIds.id("mage_attire"));

    public static final ArmorMaterial MAGE_ATTIRE = new ArmorMaterial(
        18,
        defense(1, 3, 2, 1),
        25,
        SoundEvents.ARMOR_EQUIP_LEATHER,
        0.0F,
        0.0F,
        REPAIRS_MAGE_ATTIRE,
        MAGE_ATTIRE_ASSET
    );

    private ModArmorMaterials() {}

    private static Map<ArmorType, Integer> defense(int helmet, int chest, int legs, int boots) {
        Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.BODY, chest);
        return map;
    }
}
