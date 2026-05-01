package com.dooji.kurasu.network;

import com.dooji.kurasu.Kurasu;
import com.dooji.kurasu.block.entity.BlackboardBlockEntity;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SaveBlackboardPayload(BlockPos blockPos, int[] pixels) implements CustomPacketPayload {
	public static final Identifier SAVE_BLACKBOARD_PAYLOAD_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "save_blackboard");
	public static final Type<SaveBlackboardPayload> TYPE = new Type<>(SAVE_BLACKBOARD_PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, SaveBlackboardPayload> CODEC = CustomPacketPayload.codec(SaveBlackboardPayload::write, SaveBlackboardPayload::read);

	public SaveBlackboardPayload {
		pixels = Arrays.copyOf(pixels, BlackboardBlockEntity.DRAW_WIDTH * BlackboardBlockEntity.DRAW_HEIGHT);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.blockPos);
		buffer.writeVarIntArray(this.pixels);
	}

	private static SaveBlackboardPayload read(RegistryFriendlyByteBuf buffer) {
		BlockPos blockPos = buffer.readBlockPos();
		int[] pixels = buffer.readVarIntArray(BlackboardBlockEntity.DRAW_WIDTH * BlackboardBlockEntity.DRAW_HEIGHT);
		return new SaveBlackboardPayload(blockPos, pixels);
	}
}
