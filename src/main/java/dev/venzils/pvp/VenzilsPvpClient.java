package dev.venzils.pvp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class VenzilsPvpClient implements ClientModInitializer {
    public static PvpConfig CFG;
    private final ArrayDeque<Long> leftClicks = new ArrayDeque<>(), rightClicks = new ArrayDeque<>();
    private boolean prevLeft, prevRight;
    private double lastReach = -1; private long lastReachAt;
    private double tntSeconds = -1;

    @Override public void onInitializeClient() {
        CFG = PvpConfig.load();
        if (CFG.showSpotify) SpotifyNowPlaying.start();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            long now = System.currentTimeMillis();
            boolean l = mc.options.keyAttack.isDown(), r = mc.options.keyUse.isDown();
            if (l && !prevLeft) leftClicks.add(now);
            if (r && !prevRight) rightClicks.add(now);
            prevLeft = l; prevRight = r;
            prune(leftClicks, now); prune(rightClicks, now);
            tntSeconds = CFG.showTntTimer ? nearestTnt(mc) : -1;
        });

        // Reach = eye -> closest point on the target's hitbox, measured on every melee swing that hits an entity.
        AttackEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            if (level.isClientSide()) {
                Vec3 eye = player.getEyePosition();
                AABB b = target.getBoundingBox();
                Vec3 p = new Vec3(clamp(eye.x, b.minX, b.maxX), clamp(eye.y, b.minY, b.maxY), clamp(eye.z, b.minZ, b.maxZ));
                lastReach = eye.distanceTo(p); lastReachAt = System.currentTimeMillis();
            }
            return InteractionResult.PASS;
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("venzilspvp", "hud"), this::renderHud);
    }

    private void prune(ArrayDeque<Long> q, long now) { while (!q.isEmpty() && now - q.peek() > CFG.cpsWindowMs) q.poll(); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private double nearestTnt(Minecraft mc) {
        if (mc.level == null || mc.player == null) return -1;
        double best = Double.MAX_VALUE; int fuse = -1;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof PrimedTnt t) {
                double d = t.distanceTo(mc.player);
                if (d < best && d <= CFG.tntMaxRange) { best = d; fuse = t.getFuse(); }
            }
        }
        return fuse < 0 ? -1 : fuse / 20.0;
    }

    private void renderHud(GuiGraphics g, net.minecraft.client.DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        List<String> lines = new ArrayList<>();
        if (CFG.showFps) lines.add("FPS: " + mc.getFps());
        if (CFG.showCps) lines.add("CPS: " + leftClicks.size() + (CFG.showRightClickCps ? " | " + rightClicks.size() : ""));
        if (CFG.showReach && lastReach >= 0 && System.currentTimeMillis() - lastReachAt < CFG.reachFadeSeconds * 1000)
            lines.add(String.format("Reach: %.2f", lastReach));
        if (tntSeconds >= 0) lines.add(String.format("TNT: %.1fs", tntSeconds));
        if (CFG.showSpotify) {
            String t = SpotifyNowPlaying.get();
            if (!t.isEmpty()) {
                if (t.length() > CFG.spotifyMaxChars) t = t.substring(0, CFG.spotifyMaxChars - 1) + "…";
                lines.add("♪ " + t);
            }
        }
        int y = CFG.hudY;
        for (String s : lines) {
            g.drawString(mc.font, s, CFG.hudX, y, 0xFF000000 | CFG.textColor, CFG.textShadow);
            y += CFG.lineSpacing;
        }
    }
}
