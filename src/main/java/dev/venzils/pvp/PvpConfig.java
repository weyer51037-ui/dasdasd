package dev.venzils.pvp;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

/** JSON config at .minecraft/config/venzilspvp.json — edit and restart (or /reload via F3+T no; restart). */
public class PvpConfig {
    public boolean showCps = true, showReach = true, showTntTimer = true, showFps = true;
    public boolean showRightClickCps = false;
    public boolean showSpotify = true;
    public int spotifyMaxChars = 40;
    public int hudX = 6, hudY = 6, lineSpacing = 11;
    public int textColor = 0xFFFFFF, accentColor = 0x55FFFF;
    public boolean textShadow = true;
    public double reachFadeSeconds = 3.0;   // how long last-hit reach stays visible
    public double tntMaxRange = 48.0;       // only track TNT within this many blocks
    public int cpsWindowMs = 1000;

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("venzilspvp.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static PvpConfig load() {
        try {
            if (Files.exists(FILE)) return GSON.fromJson(Files.readString(FILE), PvpConfig.class);
        } catch (Exception e) { System.err.println("[VenzilsPVP] bad config, using defaults: " + e); }
        PvpConfig c = new PvpConfig(); c.save(); return c;
    }
    public void save() {
        try { Files.writeString(FILE, GSON.toJson(this)); } catch (Exception ignored) {}
    }
}
