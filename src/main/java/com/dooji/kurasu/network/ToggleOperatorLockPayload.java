package com.dooji.kurasu.network;

import com.dooji.kurasu.Kurasu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleOperatorLockPayload(BlockPos blockPos) implements CustomPacketPayload {
	public static final Identifier TOGGLE_OPERATOR_LOCK_PAYLOAD_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "toggle_operator_lock");
	public static final Type<ToggleOperatorLockPayload> TYPE = new Type<>(TOGGLE_OPERATOR_LOCK_PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, ToggleOperatorLockPayload> CODEC = CustomPacketPayload.codec(ToggleOperatorLockPayload::write, ToggleOperatorLockPayload::read);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.blockPos);
	}

	private static ToggleOperatorLockPayload read(RegistryFriendlyByteBuf buffer) {
		return new ToggleOperatorLockPayload(buffer.readBlockPos());
	}
}
