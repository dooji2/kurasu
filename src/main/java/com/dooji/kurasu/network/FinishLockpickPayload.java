package com.dooji.kurasu.network;

import com.dooji.kurasu.Kurasu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record FinishLockpickPayload(BlockPos blockPos, boolean success) implements CustomPacketPayload {
	public static final Identifier FINISH_LOCKPICK_PAYLOAD_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "finish_lockpick");
	public static final Type<FinishLockpickPayload> TYPE = new Type<>(FINISH_LOCKPICK_PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, FinishLockpickPayload> CODEC = CustomPacketPayload.codec(FinishLockpickPayload::write, FinishLockpickPayload::read);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.blockPos);
		buffer.writeBoolean(this.success);
	}

	private static FinishLockpickPayload read(RegistryFriendlyByteBuf buffer) {
		return new FinishLockpickPayload(buffer.readBlockPos(), buffer.readBoolean());
	}
}
