package me.infinityy.fakeplayers.listener;

import me.infinityy.fakeplayers.lib.FakeServerGamePacketListenerImpl;
import me.infinityy.fakeplayers.lib.FakeServerPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ServerPlayer handle = ((CraftPlayer) event.getPlayer()).getHandle();
        if (handle instanceof FakeServerPlayer player) {
            player.fixStartingPosition.run();
            player.connection = new FakeServerGamePacketListenerImpl(player.connection.cserver.getServer(), player.connection.connection, player, CommonListenerCookie.createInitial(player.gameProfile, false));
        }
    }
}