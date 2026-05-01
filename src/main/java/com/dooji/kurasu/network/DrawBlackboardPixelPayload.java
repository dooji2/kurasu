package com.dooji.kurasu.network;

import com.dooji.kurasu.Kurasu;
import com.dooji.kurasu.block.entity.BlackboardBlockEntity;
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
		if (x < 0) {
			x = 0;
		} else if (x >= BlackboardBlockEntity.DRAW_WIDTH) {
			x = BlackboardBlockEntity.DRAW_WIDTH - 1;
		}

		if (y < 0) {
			y = 0;
		} else if (y >= BlackboardBlockEntity.DRAW_HEIGHT) {
			y = BlackboardBlockEntity.DRAW_HEIGHT - 1;
		}
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
