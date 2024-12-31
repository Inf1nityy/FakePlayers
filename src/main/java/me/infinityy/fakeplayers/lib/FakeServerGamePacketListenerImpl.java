package me.infinityy.fakeplayers.lib;

import me.infinityy.fakeplayers.FakePlayers;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class FakeServerGamePacketListenerImpl extends ServerGamePacketListenerImpl {
    public FakeServerGamePacketListenerImpl(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
    }

    @Override
    public void send(@NotNull Packet<?> packet) {
        if (packet instanceof ClientboundSetEntityMotionPacket p) {
            this.handleClientboundSetEntityMotionPacket(p);
        }
    }

    /**
     * fix fake players not taking knockback because knockback is handled client-side
     * (fake players don't have a client)
     */
    public void handleClientboundSetEntityMotionPacket(@NotNull ClientboundSetEntityMotionPacket packet) {
        if (packet.getId() == this.player.getId() && this.player.hurtMarked) {
            Bukkit.getScheduler().runTask(FakePlayers.getPlugin(FakePlayers.class), () -> {
                this.player.hurtMarked = true;
                this.player.lerpMotion(packet.getXa(), packet.getYa(), packet.getZa());
            });
        }
    }
}
