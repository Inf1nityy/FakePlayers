package me.infinityy.fakeplayers.lib;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;

public class FakeClientConnection extends Connection
{
    public FakeClientConnection(PacketFlow p) {
        super(p);
        this.channel = new EmbeddedChannel();
    }

    @Override
    public void setReadOnly() {
    }

    @Override
    public void handleDisconnection() {
    }

    @Override
    public void setListenerForServerboundHandshake(@NotNull PacketListener packetListener) {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(@NotNull ProtocolInfo<T> protocolInfo, @NotNull T packetListener) {
    }
}