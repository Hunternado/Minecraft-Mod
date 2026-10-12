package com.hunternado.frieren.client.dev;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.server.IntegratedServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Development-only scripted showcase used by CI to take in-game screenshots. Inactive unless the environment
 * variable {@code FRIEREN_SHOWCASE} names a script file (relative to the game directory). The client opens the save
 * called "showcase", then runs the script line by line:
 * <ul>
 *   <li>{@code /command} - executed on the integrated server with full permissions,</li>
 *   <li>{@code @wait <ticks>}, {@code @hud} (hide the HUD), {@code @shot <name>} (screenshot), {@code @quit}.</li>
 * </ul>
 */
public final class ShowcaseDirector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SCRIPT = System.getenv("FRIEREN_SHOWCASE");
    private static final int GIVE_UP_TICKS = 20 * 240;

    private static List<String> lines;
    private static int index;
    private static int wait;
    private static int ticksWithoutWorld;
    private static boolean openRequested;

    private static final Set<String> PROCEED_KEYS = Set.of("selectWorld.backupJoinSkipButton", "gui.proceed", "gui.yes");

    private ShowcaseDirector() {}

    /**
     * Opening a copied world can stop on confirmation prompts (backup before loading, experimental features); press
     * the button that proceeds without changing anything.
     */
    private static void confirmPrompts(Screen screen) {
        if (screen == null || screen instanceof TitleScreen) {
            return;
        }
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button button && button.getMessage().getContents() instanceof TranslatableContents contents
                    && PROCEED_KEYS.contains(contents.getKey())) {
                LOGGER.info("[showcase] pressing '{}' on {}", contents.getKey(), screen.getClass().getSimpleName());
                button.onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                return;
            }
        }
    }

    public static boolean active() {
        return SCRIPT != null && !SCRIPT.isBlank();
    }

    public static void tick() {
        if (!active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            // First launches show onboarding screens before the title screen; open the world from any screen.
            if (!openRequested && (minecraft.gui.screen() instanceof TitleScreen || ticksWithoutWorld > 100)) {
                openRequested = true;
                LOGGER.info("[showcase] opening world 'showcase'");
                minecraft.createWorldOpenFlows().openWorld("showcase", () -> LOGGER.warn("[showcase] world open cancelled"));
            }
            if (openRequested && ticksWithoutWorld % 20 == 0) {
                confirmPrompts(minecraft.gui.screen());
            }
            if (++ticksWithoutWorld % 200 == 0) {
                LOGGER.info("[showcase] waiting for world, screen = {}",
                    minecraft.gui.screen() == null ? "none" : minecraft.gui.screen().getClass().getName());
            }
            if (ticksWithoutWorld > GIVE_UP_TICKS) {
                LOGGER.error("[showcase] world never loaded, giving up");
                minecraft.stop();
            }
            return;
        }
        if (lines == null) {
            try {
                lines = Files.readAllLines(minecraft.gameDirectory.toPath().resolve(SCRIPT));
            } catch (IOException e) {
                LOGGER.error("[showcase] cannot read script {}", SCRIPT, e);
                minecraft.stop();
                return;
            }
            LOGGER.info("[showcase] running {} lines from {}", lines.size(), SCRIPT);
            wait = 60;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        while (index < lines.size()) {
            String line = lines.get(index++).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("@wait")) {
                wait = Integer.parseInt(line.substring(5).trim());
                return;
            }
            if (line.equals("@hud")) {
                if (!minecraft.gui.hud.isHidden()) {
                    minecraft.gui.hud.toggle();
                }
                continue;
            }
            if (line.startsWith("@shot")) {
                String name = line.substring(5).trim();
                Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1,
                    message -> LOGGER.info("[showcase] {}: {}", name, message.getString()));
                wait = 10;
                return;
            }
            if (line.equals("@quit")) {
                break;
            }
            if (line.startsWith("/")) {
                runCommand(minecraft, line.substring(1));
                continue;
            }
            LOGGER.warn("[showcase] unknown line: {}", line);
        }
        LOGGER.info("[showcase] script finished");
        minecraft.stop();
    }

    private static void runCommand(Minecraft minecraft, String command) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            LOGGER.warn("[showcase] no integrated server for: {}", command);
            return;
        }
        server.execute(() -> {
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            } catch (RuntimeException e) {
                LOGGER.error("[showcase] command failed: {}", command, e);
            }
        });
    }
}
