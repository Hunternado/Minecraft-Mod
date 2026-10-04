package com.hunternado.frieren.data;

import com.hunternado.frieren.FrierenIds;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

/** Attaches {@link MagicData} to players and stores it inside the entity's ForgeCaps tag. */
public final class MagicDataProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Identifier KEY = FrierenIds.id("magic_data");
    public static final Capability<MagicData> MAGIC_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private final MagicData data = new MagicData();
    private final LazyOptional<MagicData> optional = LazyOptional.of(() -> data);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return MAGIC_DATA.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registryAccess) {
        return data.save();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registryAccess, CompoundTag nbt) {
        data.load(nbt);
    }

    /** Called when the owning player is removed so stale references cannot be used. */
    public void invalidate() {
        optional.invalidate();
    }
}
