package net.blay09.mods.balm.api.menu;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.fmllegacy.network.IContainerFactory;

public interface BalmMenuFactory<T extends AbstractContainerMenu> extends IContainerFactory<T> {
}