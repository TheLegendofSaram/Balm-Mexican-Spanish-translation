package net.blay09.mods.balm.api.block.entity;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.datafixers.util.Pair;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.block.BalmBlockEntityContract;
import net.blay09.mods.balm.api.container.BalmContainerProvider;
import net.blay09.mods.balm.api.fluid.BalmFluidTankProvider;
import net.blay09.mods.balm.api.provider.BalmProviderHolder;
import net.blay09.mods.balm.forge.provider.ForgeBalmProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BalmBlockEntity extends BlockEntity implements BalmBlockEntityContract {

    private final Map<Capability<?>, LazyOptional<?>> capabilities = new HashMap<>();
    private final Table<Capability<?>, Direction, LazyOptional<?>> sidedCapabilities = HashBasedTable.create();
    private boolean capabilitiesInitialized;

    public BalmBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return balmGetRenderBoundingBox();
    }

    @Override
    public AABB balmGetRenderBoundingBox() {
        return AABB.unitCubeFromLowerCorner(new Vec3(worldPosition.getX(), worldPosition.getY(), worldPosition.getY()));
    }

    @Override
    public void balmOnLoad() {
    }

    @Override
    public void balmFromClientTag(CompoundTag tag) {
    }

    @Override
    public CompoundTag balmToClientTag(CompoundTag tag) {
        return tag;
    }

    @Override
    public void balmSync() {
        if (level != null && !level.isClientSide) {
            List<? extends Player> playerList = level.players();
            ClientboundBlockEntityDataPacket updatePacket = getUpdatePacket();
            if (updatePacket == null) {
                return;
            }

            for (Object obj : playerList) {
                ServerPlayer player = (ServerPlayer) obj;
                if (Math.hypot(player.getX() - worldPosition.getX() + 0.5, player.getZ() - worldPosition.getZ() + 0.5) < 64) {
                    player.connection.send(updatePacket);
                }
            }
        }
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return new ClientboundBlockEntityDataPacket(worldPosition, 0, getUpdateTag());
    }

    @Override
    public CompoundTag getUpdateTag() {
        return balmToClientTag(super.getUpdateTag());
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);

        balmFromClientTag(pkt.getTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);

        balmFromClientTag(tag);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!capabilitiesInitialized) {
            List<BalmProviderHolder> providers = new ArrayList<>();
            if (this instanceof BalmProviderHolder providerHolder) {
                providers.add(providerHolder);
            }
            balmBuildProviders(providers);

            ForgeBalmProviders forgeProviders = (ForgeBalmProviders) Balm.getProviders();
            for (BalmProviderHolder providerHolder : providers) {
                for (Object provider : providerHolder.getProviders()) {
                    Capability<?> capability = forgeProviders.getCapability(provider.getClass());
                    capabilities.put(capability, LazyOptional.of(() -> provider));
                }

                for (Pair<Direction, Object> pair : providerHolder.getSidedProviders()) {
                    Direction direction = pair.getFirst();
                    Object provider = pair.getSecond();
                    Capability<?> capability = forgeProviders.getCapability(provider.getClass());
                    sidedCapabilities.put(capability, direction, LazyOptional.of(() -> provider));
                }
            }
            capabilitiesInitialized = true;
        }

        LazyOptional<?> result = null;
        if (side != null) {
            result = sidedCapabilities.get(cap, side);
        }
        if (result == null) {
            result = capabilities.get(cap);
        }
        return result != null ? result.cast() : super.getCapability(cap, side);
    }

    @Override
    public void balmBuildProviders(List<BalmProviderHolder> providers) {
        if (this instanceof BalmContainerProvider containerProvider) {
            providers.add(containerProvider);
        }
        if (this instanceof BalmFluidTankProvider fluidTankProvider) {
            providers.add(fluidTankProvider);
        }
    }
}
