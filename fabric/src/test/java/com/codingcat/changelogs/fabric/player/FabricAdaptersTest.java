package com.codingcat.changelogs.fabric.player;

import net.kyori.adventure.util.TriState;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricAdaptersTest {
    @Test
    void parsesVanillaClientLanguage() {
        assertEquals(Locale.US, FabricPlayerWrapper.parseLocale("en_us"));
        assertEquals(Locale.of("ru", "RU"), FabricPlayerWrapper.parseLocale("ru_ru"));
        assertEquals(Locale.US, FabricPlayerWrapper.parseLocale(null));
    }

    @Test
    void convertsFabricTriState() {
        assertEquals(TriState.TRUE, FabricPermissionUtil.convert(net.fabricmc.fabric.api.util.TriState.TRUE));
        assertEquals(TriState.FALSE, FabricPermissionUtil.convert(net.fabricmc.fabric.api.util.TriState.FALSE));
        assertEquals(TriState.NOT_SET, FabricPermissionUtil.convert(net.fabricmc.fabric.api.util.TriState.DEFAULT));
    }
}
