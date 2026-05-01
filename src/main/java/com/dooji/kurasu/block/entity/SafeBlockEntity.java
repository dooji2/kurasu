package com.dooji.kurasu.block.entity;

import com.dooji.kurasu.KurasuBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class SafeBlockEntity extends LockerBlockEntity {
	private String code = "";

	public SafeBlockEntity(BlockPos pos, BlockState blockState) {
		super(KurasuBlockEntityTypes.SAFE, pos, blockState);
	}

	public boolean hasCode() {
		return !this.code.isBlank();
	}

	public boolean matchesCode(String code) {
		return this.code.equals(code);
	}

	public void setCode(String code) {
		if (this.code.equals(code)) {
			return;
		}

		this.code = code;
		this.setChanged();

		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.code = input.getString("safe_code").orElseThrow();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("safe_code", this.code);
	}
}
