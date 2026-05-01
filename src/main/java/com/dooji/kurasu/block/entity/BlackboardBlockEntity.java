package com.dooji.kurasu.block.entity;

import com.dooji.kurasu.KurasuBlockEntityTypes;
import com.dooji.kurasu.item.DrawData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlackboardBlockEntity extends AccessoryBlockEntity {
	public static final int DRAW_WIDTH = 60;
	public static final int DRAW_HEIGHT = 28;

	private DrawData drawData = new DrawData(DRAW_WIDTH, DRAW_HEIGHT, new int[DRAW_WIDTH * DRAW_HEIGHT]);

	public BlackboardBlockEntity(BlockPos pos, BlockState blockState) {
		super(KurasuBlockEntityTypes.BLACKBOARD, pos, blockState);
	}

	public DrawData getDrawData() {
		return this.drawData;
	}

	public void setDrawData(DrawData drawData) {
		DrawData normalizedDrawData = drawData.normalized(DRAW_WIDTH, DRAW_HEIGHT);

		if (this.drawData.samePixels(normalizedDrawData)) {
			return;
		}

		this.drawData = normalizedDrawData;
		this.setChanged();

		if (this.level != null) {
			this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		this.drawData = DrawData.read(input.child("board_draw").orElseThrow()).normalized(DRAW_WIDTH, DRAW_HEIGHT);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		this.drawData.write(output.child("board_draw"));
	}
}
