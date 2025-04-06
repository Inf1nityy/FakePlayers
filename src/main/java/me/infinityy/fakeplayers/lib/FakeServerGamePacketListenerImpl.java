package me.infinityy.fakeplayers.lib;

import me.infinityy.fakeplayers.FakePlayers;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class FakeServerGamePacketListenerImpl extends ServerGamePacketListenerImpl {
    private static final String BUNGEE_CORD_CHANNEL = "BungeeCord";
    private static final String BUNGEE_CORD_CORRECTED_CHANNEL = StandardMessenger.validateAndCorrectChannel(BUNGEE_CORD_CHANNEL);

    public FakeServerGamePacketListenerImpl(MinecraftServer server, Connection connection, ServerPlayer player, CommonListenerCookie cookie) {
        super(server, connection, player, cookie);
        Optional.ofNullable(Bukkit.getPlayer(player.getUUID()))
                .map(CraftPlayer.class::cast)
                .ifPresent(p -> p.addChannel(BUNGEE_CORD_CORRECTED_CHANNEL));
    }

    @Override
    public void send(@NotNull Packet<?> packet) {
        if (packet instanceof ClientboundSetEntityMotionPacket p) {
            this.handleClientboundSetEntityMotionPacket(p);
        } else if (packet instanceof ClientboundCustomPayloadPacket p) {
            this.handleCustomPayloadPacket(p);
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

    /**
     * This function ensures plugin messaging works correctly by using a real player's connection instead of a fake one
     * due to the absence of a client with fake players.
     * @param packet the custom payload packet
     */
    private void handleCustomPayloadPacket(ClientboundCustomPayloadPacket packet) {
        var payload = packet.payload();
        var resourceLocation = payload.type().id();
        var channel = resourceLocation.getNamespace() + ":" + resourceLocation.getPath();

        if (!channel.equals(BUNGEE_CORD_CORRECTED_CHANNEL)) {
            return;
        }

        if (!(payload instanceof DiscardedPayload p)) {
            return;
        }

        var recipient = Bukkit
                .getOnlinePlayers()
                .stream()
                .filter(player1 -> !(((CraftPlayer) player1).getHandle() instanceof FakeServerPlayer))
                .findAny()
                .orElse(null);

        if (recipient == null) {
            FakePlayers.getInstance().getLogger().warning("Failed to forward a plugin message because there are no real players in the server");
            return;
        }

        recipient.sendPluginMessage(FakePlayers.getInstance(), BUNGEE_CORD_CHANNEL, p.data());
    }
}
