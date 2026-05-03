package com.dooji.kurasu.network;

import com.dooji.kurasu.Kurasu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DrawBlackboardPixelPayload(BlockPos blockPos, int x, int y) implements CustomPacketPayload {
	public static final Identifier DRAW_BLACKBOARD_PIXEL_PAYLOAD_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "draw_blackboard_pixel");
	public static final Type<DrawBlackboardPixelPayload> TYPE = new Type<>(DRAW_BLACKBOARD_PIXEL_PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, DrawBlackboardPixelPayload> CODEC = CustomPacketPayload.codec(DrawBlackboardPixelPayload::write, DrawBlackboardPixelPayload::read);

	public DrawBlackboardPixelPayload {
		x = Math.max(0, x);
		y = Math.max(0, y);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.blockPos);
		buffer.writeVarInt(this.x);
		buffer.writeVarInt(this.y);
	}

	private static DrawBlackboardPixelPayload read(RegistryFriendlyByteBuf buffer) {
		return new DrawBlackboardPixelPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
	}
}
