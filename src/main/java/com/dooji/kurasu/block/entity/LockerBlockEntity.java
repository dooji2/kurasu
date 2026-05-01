package com.dooji.kurasu.block.entity;

import com.dooji.kurasu.KurasuBlockEntityTypes;
import com.dooji.kurasu.block.LockerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class LockerBlockEntity extends AccessoryBlockEntity {
	private BlockPos controllerPos;
	private boolean open;
	private boolean locked;
	private boolean pickedOpen;
	private String lockId = "";
	private float openProgress;
	private float lastOpenProgress;
	private boolean animationInitialized;

	public LockerBlockEntity(BlockPos pos, BlockState blockState) {
		this(KurasuBlockEntityTypes.LOCKER, pos, blockState);
	}

	protected LockerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
		super(type, pos, blockState);
		this.controllerPos = pos;
	}

	public static void tick(Level unusedLevel, BlockPos unusedPos, BlockState unusedState, LockerBlockEntity blockEntity) {
		blockEntity.lastOpenProgress = blockEntity.openProgress;
		float target = blockEntity.isStructureOpen() ? 1.0f : 0.0f;
		blockEntity.openProgress += (target - blockEntity.openProgress) * 0.35f;

		if (Math.abs(blockEntity.openProgress - target) < 0.001f) {
			blockEntity.openProgress = target;
		}
	}

	public float getOpenProgress(float partialTick) {
		return this.lastOpenProgress + (this.openProgress - this.lastOpenProgress) * partialTick;
	}

	public boolean isStructureOpen() {
		return this.getController().open;
	}

	public boolean isStructureLocked() {
		return this.getController().locked;
	}

	public boolean isStructurePickedOpen() {
		return this.getController().pickedOpen;
	}

	public String getStructureLockId() {
		return this.getController().lockId;
	}

	public void setStructureData(BlockPos controllerPos, boolean open, boolean locked, String lockId, boolean pickedOpen) {
		boolean changed = !this.controllerPos.equals(controllerPos)
			|| this.open != open
			|| this.locked != locked
			|| !this.lockId.equals(lockId)
			|| this.pickedOpen != pickedOpen;
		this.controllerPos = controllerPos;
		this.open = open;
		this.locked = locked;
		this.lockId = lockId;
		this.pickedOpen = pickedOpen;

		if (!changed) {
			return;
		}

		this.setChanged();

		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	public void setStructureOpen(boolean open) {
		if (this.level == null || this.level.isClientSide()) {
			return;
		}

		this.setStructureState(open, this.isStructureLocked(), this.getStructureLockId(), open && this.isStructurePickedOpen());
	}

	public void setStructureState(boolean open, boolean locked, String lockId, boolean pickedOpen) {
		if (this.level == null || this.level.isClientSide()) {
			return;
		}

		this.applyStructureState(this.resolveStructureRootPos(), open, locked, lockId, pickedOpen);
	}

	public void pickStructureOpen() {
		this.setStructureState(true, true, this.getStructureLockId(), true);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.controllerPos = new BlockPos(input.getInt("controller_x").orElseThrow(), input.getInt("controller_y").orElseThrow(), input.getInt("controller_z").orElseThrow());
		this.open = input.getInt("open").orElseThrow() != 0;
		this.locked = input.getInt("locked").orElseThrow() != 0;
		this.pickedOpen = input.getInt("picked_open").orElseThrow() != 0;
		this.lockId = input.getString("lock_id").orElseThrow();

		if (!this.animationInitialized) {
			this.openProgress = this.open ? 1.0f : 0.0f;
			this.lastOpenProgress = this.openProgress;
			this.animationInitialized = true;
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putInt("controller_x", this.controllerPos.getX());
		output.putInt("controller_y", this.controllerPos.getY());
		output.putInt("controller_z", this.controllerPos.getZ());
		output.putInt("open", this.open ? 1 : 0);
		output.putInt("locked", this.locked ? 1 : 0);
		output.putInt("picked_open", this.pickedOpen ? 1 : 0);
		output.putString("lock_id", this.lockId);
	}

	private BlockPos resolveStructureRootPos() {
		BlockPos rootPos = this.controllerPos;

		if (!(this.level.getBlockEntity(rootPos) instanceof LockerBlockEntity)) {
			rootPos = this.worldPosition;
		}

		BlockState rootState = this.level.getBlockState(rootPos);

		if (!(rootState.getBlock() instanceof LockerBlock locker)) {
			return rootPos;
		}

		Direction facing = rootState.getValue(BlockStateProperties.HORIZONTAL_FACING);

		while (matchesStackNeighbor(this.level.getBlockState(rootPos.below()), locker, facing)) {
			rootPos = rootPos.below().immutable();
		}

		return rootPos;
	}

	private void applyStructureState(BlockPos rootPos, boolean open, boolean locked, String lockId, boolean pickedOpen) {
		BlockState rootState = this.level.getBlockState(rootPos);

		if (!(rootState.getBlock() instanceof LockerBlock locker)) {
			if (this.level.getBlockEntity(rootPos) instanceof LockerBlockEntity blockEntity) {
				blockEntity.setStructureData(rootPos, open, locked, lockId, pickedOpen);
			}

			return;
		}

		Direction facing = rootState.getValue(BlockStateProperties.HORIZONTAL_FACING);

		for (BlockPos scanPos = rootPos; matchesStackNeighbor(this.level.getBlockState(scanPos), locker, facing); scanPos = scanPos.above()) {
			if (this.level.getBlockEntity(scanPos) instanceof LockerBlockEntity blockEntity) {
				blockEntity.setStructureData(rootPos, open, locked, lockId, pickedOpen);
			}
		}
	}

	private boolean matchesStackNeighbor(BlockState state, LockerBlock locker, Direction facing) {
		return state.getBlock() == locker && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing;
	}

	private LockerBlockEntity getController() {
		if (this.level == null || this.worldPosition.equals(this.controllerPos)) {
			return this;
		}

		if (this.level.getBlockEntity(this.controllerPos) instanceof LockerBlockEntity controller) {
			return controller;
		}

		return this;
	}
}
