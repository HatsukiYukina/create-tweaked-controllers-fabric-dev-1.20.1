package com.llyycci.create_tweaked_controllers.item;

import com.llyycci.create_tweaked_controllers.block.ModBlocks;
import com.llyycci.create_tweaked_controllers.controller.TweakedLinkedControllerClientHandler;
import com.llyycci.create_tweaked_controllers.controller.TweakedLinkedControllerMenu;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.foundation.utility.AdventureUtil;
import com.simibubi.create.foundation.utility.Couple;

import com.tterrag.registrate.fabric.EnvExecutor;

import io.github.fabricators_of_create.porting_lib.item.UseFirstBehaviorItem;
import io.github.fabricators_of_create.porting_lib.util.NetworkHooks;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

public class TweakedLinkedControllerItem extends Item implements MenuProvider, UseFirstBehaviorItem {

	public TweakedLinkedControllerItem(Properties properties) {
		super(properties);
	}

	@Override//交互结果
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
		Player player = ctx.getPlayer();
		if (player == null)
			return InteractionResult.PASS;
		Level world = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		BlockState hitState = world.getBlockState(pos);

		if (player.mayBuild()) {
			if (player.isShiftKeyDown()) {
				if (ModBlocks.TWEAKED_LECTERN_CONTROLLER.has(hitState)) {
					if (!world.isClientSide)
						ModBlocks.TWEAKED_LECTERN_CONTROLLER.get().withBlockEntityDo(world, pos, be ->
								be.swapControllers(stack, player, ctx.getHand(), hitState));
					return InteractionResult.SUCCESS;
				}
			} else {
				if (AllBlocks.REDSTONE_LINK.has(hitState)) {
					if (world.isClientSide)
						EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> this.toggleBindMode(ctx.getClickedPos()));
					player.getCooldowns()
							.addCooldown(this, 2);
					return InteractionResult.SUCCESS;
				}

				if (hitState.is(Blocks.LECTERN) && !hitState.getValue(LecternBlock.HAS_BOOK)) {
					if (!world.isClientSide) {
						ItemStack lecternStack = player.isCreative() ? stack.copy() : stack.split(1);
						ModBlocks.TWEAKED_LECTERN_CONTROLLER.get().replaceLectern(hitState, world, pos, lecternStack);
					}
					return InteractionResult.SUCCESS;
				}

				if (ModBlocks.TWEAKED_LECTERN_CONTROLLER.has(hitState))
					return InteractionResult.PASS;
			}
		}

		return use(world, player, ctx.getHand()).getResult();
	}

	@Override//保持交互结果
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack heldItem = player.getItemInHand(hand);

		if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
			if (!world.isClientSide && player instanceof ServerPlayer && player.mayBuild())
				NetworkHooks.openScreen((ServerPlayer) player, this, buf -> {
					buf.writeItem(heldItem);
				});
			return InteractionResultHolder.success(heldItem);
		}

		if (!player.isShiftKeyDown()) {
			if (world.isClientSide)
				EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::toggleActive);
			player.getCooldowns()
					.addCooldown(this, 2);
		}

		return InteractionResultHolder.pass(heldItem);
	}

	@Environment(EnvType.CLIENT)
	private void toggleBindMode(BlockPos pos) {
		TweakedLinkedControllerClientHandler.toggleBindMode(pos);
	}

	@Environment(EnvType.CLIENT)
	private void toggleActive() {
		TweakedLinkedControllerClientHandler.toggle();
	}
	//获取频率物品
	public static ItemStackHandler getFrequencyItems(ItemStack stack) {
		ItemStackHandler newInv = new ItemStackHandler(50);
		if (ModItems.TWEAKED_LINKED_CONTROLLER.get() != stack.getItem())
			throw new IllegalArgumentException("Cannot get frequency items from non-controller: " + stack);
		CompoundTag invNBT = stack.getOrCreateTagElement("Items");
		if (!invNBT.isEmpty())
			newInv.deserializeNBT(invNBT);
		return newInv;
	}
	//获取组合频率
	public static Couple<RedstoneLinkNetworkHandler.Frequency> toFrequency(ItemStack controller, int slot) {
		ItemStackHandler frequencyItems = getFrequencyItems(controller);
		return Couple.create(RedstoneLinkNetworkHandler.Frequency.of(frequencyItems.getStackInSlot(slot * 2)),
				RedstoneLinkNetworkHandler.Frequency.of(frequencyItems.getStackInSlot(slot * 2 + 1)));
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		if (AdventureUtil.isAdventure(player))
			return null;
		ItemStack heldItem = player.getMainHandItem();
		return TweakedLinkedControllerMenu.create(id, inv, heldItem);
	}

	@Override
	public Component getDisplayName() {
		return getDescription();
	}

}
