package net.blay09.mods.forbic.network;

import net.blay09.mods.forbic.network.ForbicMessageRegistration;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ForbicServerboundMessageRegistration<T> extends ForbicMessageRegistration<T> {

    private final BiConsumer<ServerPlayer, T> handler;

    public ForbicServerboundMessageRegistration(ResourceLocation identifier, Class<T> clazz, BiConsumer<T, FriendlyByteBuf> encodeFunc, Function<FriendlyByteBuf, T> decodeFunc, BiConsumer<ServerPlayer, T> handler) {
        super(identifier, clazz, encodeFunc, decodeFunc);
        this.handler = handler;
    }

    public BiConsumer<ServerPlayer, T> getHandler() {
        return handler;
    }
}