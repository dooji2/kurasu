package com.dooji.kurasu.block.entity;

import com.dooji.kurasu.Kurasu;
import com.dooji.kurasu.item.DrawData;
import com.dooji.kurasu.network.PlaceAccessoryPayload;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AccessoryBlockEntity extends BlockEntity {
	private static final int MAX_ACCESSORIES = 24;
	private List<PlacedAccessory> accessories = new ArrayList<>();
	private boolean operatorLocked;

	public AccessoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
	}

	public List<PlacedAccessory> getAccessories() {
		return this.accessories;
	}

	public boolean isOperatorLocked() {
		return this.isLocalOperatorLocked();
	}

	public void setOperatorLocked(boolean operatorLocked) {
		if (!this.setLocalOperatorLocked(operatorLocked)) {
			return;
		}

		this.setChanged();

		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	protected boolean isLocalOperatorLocked() {
		return this.operatorLocked;
	}

	protected boolean setLocalOperatorLocked(boolean operatorLocked) {
		if (this.operatorLocked == operatorLocked) {
			return false;
		}

		this.operatorLocked = operatorLocked;
		return true;
	}

	public boolean addAccessory(PlacedAccessory accessory) {
		if (this.accessories.size() >= MAX_ACCESSORIES) {
			return false;
		}

		this.accessories.add(accessory);
		this.setChanged();

		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}

		return true;
	}

	public PlacedAccessory removeAccessory(int index) {
		if (index < 0 || index >= this.accessories.size()) {
			return null;
		}

		PlacedAccessory removed = this.accessories.remove(index);
		this.setChanged();

		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}

		return removed;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.accessories = new ArrayList<>();
		this.operatorLocked = input.getInt("operator_locked").orElse(0) != 0;

		for (ValueInput accessoryInput : input.childrenListOrEmpty("accessories")) {
			this.accessories.add(PlacedAccessory.read(accessoryInput));
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("operator_locked", this.operatorLocked ? 1 : 0);
		ValueOutput.ValueOutputList accessoriesOutput = output.childrenList("accessories");

		for (PlacedAccessory accessory : this.accessories) {
			accessory.write(accessoriesOutput.addChild());
		}
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithoutMetadata(registries);
	}

	public record PlacedAccessory(
		String accessoryId,
		String partName,
		float localX,
		float localY,
		float localZ,
		float normalX,
		float normalY,
		float normalZ,
		float tangentX,
		float tangentY,
		float tangentZ,
		float bitangentX,
		float bitangentY,
		float bitangentZ,
		float rotation,
		float scale,
		ItemStack itemStack,
		DrawData drawData
	) {
		public PlacedAccessory {
			drawData = resolveDrawData(accessoryId, itemStack, drawData);
			itemStack = normalizeItemStack(accessoryId, itemStack);
		}

		@Override
		public ItemStack itemStack() {
			ItemStack copy = this.itemStack.copy();

			if (isStickyNoteAccessory(this.accessoryId) && !copy.isEmpty()) {
				DrawData.set(copy, this.drawData);
			}

			return copy;
		}

		public void write(ValueOutput output) {
			output.putString("accessory", this.accessoryId);
			output.putString("part", this.partName);
			output.putFloat("local_x", this.localX);
			output.putFloat("local_y", this.localY);
			output.putFloat("local_z", this.localZ);
			output.putFloat("normal_x", this.normalX);
			output.putFloat("normal_y", this.normalY);
			output.putFloat("normal_z", this.normalZ);
			output.putFloat("tangent_x", this.tangentX);
			output.putFloat("tangent_y", this.tangentY);
			output.putFloat("tangent_z", this.tangentZ);
			output.putFloat("bitangent_x", this.bitangentX);
			output.putFloat("bitangent_y", this.bitangentY);
			output.putFloat("bitangent_z", this.bitangentZ);
			output.putFloat("rotation", this.rotation);
			output.putFloat("scale", this.scale);
			output.store("item", ItemStack.OPTIONAL_CODEC, this.itemStack);

			if (isStickyNoteAccessory(this.accessoryId)) {
				this.drawData.write(output.child("draw"));
			}
		}

		public PlaceAccessoryPayload toPayload(BlockPos blockPos) {
			return new PlaceAccessoryPayload(blockPos, this.accessoryId, this.partName, this.localX, this.localY, this.localZ, this.normalX, this.normalY, this.normalZ, this.tangentX, this.tangentY, this.tangentZ, this.bitangentX, this.bitangentY, this.bitangentZ, this.rotation, this.scale);
		}

		public PlacedAccessory withRotationAndScale(float rotation, float scale) {
			return new PlacedAccessory(this.accessoryId, this.partName, this.localX, this.localY, this.localZ, this.normalX, this.normalY, this.normalZ, this.tangentX, this.tangentY, this.tangentZ, this.bitangentX, this.bitangentY, this.bitangentZ, rotation, scale, this.itemStack, this.drawData);
		}

		public static PlacedAccessory fromPayload(PlaceAccessoryPayload payload, ItemStack itemStack) {
			return new PlacedAccessory(payload.accessoryId(), payload.partName(), payload.localX(), payload.localY(), payload.localZ(), payload.normalX(), payload.normalY(), payload.normalZ(), payload.tangentX(), payload.tangentY(), payload.tangentZ(), payload.bitangentX(), payload.bitangentY(), payload.bitangentZ(), payload.rotation(), payload.scale(), itemStack, null);
		}

		public static PlacedAccessory read(ValueInput input) {
			String accessoryId = input.getString("accessory").orElseThrow();
			ItemStack itemStack = input.read("item", ItemStack.OPTIONAL_CODEC).orElseThrow();
			DrawData drawData = null;

			if (isStickyNoteAccessory(accessoryId)) {
				drawData = DrawData.read(input.child("draw").orElseThrow());
			}

			return new PlacedAccessory(
				accessoryId,
				input.getString("part").orElseThrow(),
				input.read("local_x", Codec.FLOAT).orElseThrow(),
				input.read("local_y", Codec.FLOAT).orElseThrow(),
				input.read("local_z", Codec.FLOAT).orElseThrow(),
				input.read("normal_x", Codec.FLOAT).orElseThrow(),
				input.read("normal_y", Codec.FLOAT).orElseThrow(),
				input.read("normal_z", Codec.FLOAT).orElseThrow(),
				input.read("tangent_x", Codec.FLOAT).orElseThrow(),
				input.read("tangent_y", Codec.FLOAT).orElseThrow(),
				input.read("tangent_z", Codec.FLOAT).orElseThrow(),
				input.read("bitangent_x", Codec.FLOAT).orElseThrow(),
				input.read("bitangent_y", Codec.FLOAT).orElseThrow(),
				input.read("bitangent_z", Codec.FLOAT).orElseThrow(),
				input.read("rotation", Codec.FLOAT).orElseThrow(),
				input.read("scale", Codec.FLOAT).orElseThrow(),
				itemStack,
				drawData
			);
		}

		private static ItemStack normalizeItemStack(String accessoryId, ItemStack itemStack) {
			if (itemStack == null || itemStack.isEmpty()) {
				return ItemStack.EMPTY;
			}

			ItemStack normalized = itemStack.copyWithCount(1);

			if (isStickyNoteAccessory(accessoryId)) {
				DrawData.set(normalized, DrawData.empty());
			}

			return normalized;
		}

		private static DrawData resolveDrawData(String accessoryId, ItemStack itemStack, DrawData drawData) {
			if (!isStickyNoteAccessory(accessoryId)) {
				return DrawData.empty();
			}

			return drawData != null ? drawData : DrawData.fromStack(itemStack);
		}

		private static boolean isStickyNoteAccessory(String accessoryId) {
			return Kurasu.STICKY_NOTE_ACCESSORY_ID.equals(accessoryId);
		}
	}
}
