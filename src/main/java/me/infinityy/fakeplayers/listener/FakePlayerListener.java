package me.infinityy.fakeplayers.listener;

import me.infinityy.fakeplayers.lib.FakeServerPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

public class FakePlayerListener implements Listener {
    // prevent PacketEvents from kicking fake players
    @EventHandler
    public void preventKicking(PlayerKickEvent event) {
        ServerPlayer handle = ((CraftPlayer) event.getPlayer()).getHandle();
        if (handle instanceof FakeServerPlayer) {
            event.setCancelled(true);
        }
    }
}
