package com.dooji.kurasu.network;

import com.dooji.kurasu.Kurasu;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SaveBlackboardPayload(BlockPos blockPos, int width, int height, int[] pixels) implements CustomPacketPayload {
	public static final Identifier SAVE_BLACKBOARD_PAYLOAD_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "save_blackboard");
	public static final Type<SaveBlackboardPayload> TYPE = new Type<>(SAVE_BLACKBOARD_PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, SaveBlackboardPayload> CODEC = CustomPacketPayload.codec(SaveBlackboardPayload::write, SaveBlackboardPayload::read);

	public SaveBlackboardPayload {
		width = Math.max(1, width);
		height = Math.max(1, height);
		pixels = Arrays.copyOf(pixels, width * height);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.blockPos);
		buffer.writeVarInt(this.width);
		buffer.writeVarInt(this.height);
		buffer.writeVarIntArray(this.pixels);
	}

	private static SaveBlackboardPayload read(RegistryFriendlyByteBuf buffer) {
		BlockPos blockPos = buffer.readBlockPos();
		int width = buffer.readVarInt();
		int height = buffer.readVarInt();
		int[] pixels = buffer.readVarIntArray(width * height);
		return new SaveBlackboardPayload(blockPos, width, height, pixels);
	}
}
