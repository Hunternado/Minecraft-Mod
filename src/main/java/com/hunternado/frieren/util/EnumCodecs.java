package com.hunternado.frieren.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Small helper to build string codecs for enums without depending on vanilla interfaces that move
 * between versions.
 */
public final class EnumCodecs {
    private EnumCodecs() {}

    public static <E extends Enum<E>> Codec<E> byName(E[] values, Function<E, String> nameOf) {
        Map<String, E> lookup = new HashMap<>();
        for (E value : values) {
            lookup.put(nameOf.apply(value), value);
        }
        return Codec.STRING.comapFlatMap(
            name -> {
                E value = lookup.get(name);
                return value != null
                    ? DataResult.success(value)
                    : DataResult.error(() -> "Unknown value '" + name + "', expected one of " + lookup.keySet());
            },
            nameOf
        );
    }
}
