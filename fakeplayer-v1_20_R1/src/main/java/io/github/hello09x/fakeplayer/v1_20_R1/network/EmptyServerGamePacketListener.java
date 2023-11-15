package io.github.hello09x.fakeplayer.v1_20_R1.network;

import io.github.hello09x.fakeplayer.api.spi.NMSGamePacketListener;
import io.github.hello09x.fakeplayer.api.utils.ClientboundSystemChatPackets;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EmptyServerGamePacketListener extends ServerGamePacketListenerImpl implements NMSGamePacketListener {

    private final LinkedList<ReceivedMessage> messages = new LinkedList<>();
    private int messageId = 0;

    public EmptyServerGamePacketListener(
            @NotNull MinecraftServer server,
            @NotNull Connection connection,
            @NotNull ServerPlayer player
    ) {
        super(server, connection, player);
    }

    @Override
    public void send(Packet<?> packet) {
        if (packet instanceof ClientboundSystemChatPacket chat) {
            this.handleSystemChatPacket(chat);
        }
    }

    private void handleSystemChatPacket(@NotNull ClientboundSystemChatPacket packet) {
        if (this.messages.size() >= MESSAGE_HISTORY_SIZE) {
            this.messages.removeFirst();
        }

        var content = ClientboundSystemChatPackets.getAdventureContent(packet);
        if (content == null) {
            content = Optional.ofNullable(packet.content()).map(Component::text).orElse(null);
        }
        if (content == null) {
            return;
        }

        this.messages.addLast(new ReceivedMessage(++messageId, content));
    }

    @Override
    public @Nullable ReceivedMessage getLastMessage() {
        if (this.messages.isEmpty()) {
            return null;
        }
        return this.messages.getLast();
    }

    @Override
    public @NotNull List<ReceivedMessage> getRecentMessages(int skip, int size) {
        var stream = this.messages.stream();
        if (skip > 0) {
            stream = stream.skip(skip);
        }
        if (size != Integer.MAX_VALUE) {
            stream = stream.limit(size);
        }
        return stream.collect(Collectors.toList());
    }
}