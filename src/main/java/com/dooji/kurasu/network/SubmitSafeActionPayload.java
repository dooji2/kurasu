package com.dooji.kurasu.network;

import com.dooji.kurasu.Kurasu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SubmitSafeActionPayload(BlockPos blockPos, int action, String code) implements CustomPacketPayload {
	public static final int ACTION_SET_CODE = 0;
	public static final int ACTION_ENTER_CODE = 1;
	public static final int ACTION_TOGGLE_OPEN = 2;
	public static final int ACTION_LOCK = 3;
	public static final Identifier SUBMIT_SAFE_ACTION_PAYLOAD_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "submit_safe_action");
	public static final Type<SubmitSafeActionPayload> TYPE = new Type<>(SUBMIT_SAFE_ACTION_PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, SubmitSafeActionPayload> CODEC = CustomPacketPayload.codec(SubmitSafeActionPayload::write, SubmitSafeActionPayload::read);

	public SubmitSafeActionPayload {
		code = code == null ? "" : code;
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.blockPos);
		buffer.writeVarInt(this.action);
		buffer.writeUtf(this.code);
	}

	private static SubmitSafeActionPayload read(RegistryFriendlyByteBuf buffer) {
		return new SubmitSafeActionPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readUtf());
	}
}
