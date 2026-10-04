package com.hunternado.frieren.registry;

import com.hunternado.frieren.FrierenIds;
import com.hunternado.frieren.FrierenMod;
import com.hunternado.frieren.item.GrimoireItem;
import com.hunternado.frieren.spell.SpellManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FrierenMod.MODID);

    public static final RegistryObject<CreativeModeTab> FRIEREN = TABS.register("frieren", () -> CreativeModeTab.builder()
        .title(Component.translatable(FrierenIds.key("itemGroup", "frieren")))
        .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
        .icon(() -> new ItemStack(ModItems.APPRENTICE_STAFF.get()))
        .displayItems((parameters, output) -> {
            for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                output.accept(item.get());
            }
            // One identified grimoire per known spell definition (client-side copy, so it reflects the server's datapacks).
            for (var definition : SpellManager.clientOrServerDefinitions()) {
                output.accept(GrimoireItem.forSpell(definition));
            }
        })
        .build());

    private ModCreativeTabs() {}
}
