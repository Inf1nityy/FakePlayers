package me.infinityy.fakeplayers.lib;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class FakeNetHandler extends ServerGamePacketListenerImpl {
    public FakeNetHandler(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie clientData) {
        super(server, connection, player, clientData);
    }

    @Override
    public void send(@NotNull Packet<?> packet) {

    }

    @Override
    public void disconnect(Component message) {
        if (message.getContents() instanceof TranslatableContents text && (text.getKey().equals("multiplayer.disconnect.idling") || text.getKey().equals("multiplayer.disconnect.duplicate_login"))) {
            //((EntityPlayerMPFake) player).kill(message);
        }
    }

    @Override
    public void teleport(@NotNull PositionMoveRotation positionMoveRotation, @NotNull Set<Relative> set) {
        super.teleport(positionMoveRotation, set);
        if (player.serverLevel().getPlayerByUUID(player.getUUID()) != null) {
            resetPosition();
            player.serverLevel().getChunkSource().move(player);
        }
    }
}
