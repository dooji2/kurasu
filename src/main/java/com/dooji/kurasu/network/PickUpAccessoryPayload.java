package com.dooji.kurasu.network;

import com.dooji.kurasu.Kurasu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PickUpAccessoryPayload(BlockPos blockPos, int accessoryIndex) implements CustomPacketPayload {
	public static final Identifier PICK_UP_ACCESSORY_PAYLOAD_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "pick_up_accessory");
	public static final Type<PickUpAccessoryPayload> TYPE = new Type<>(PICK_UP_ACCESSORY_PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, PickUpAccessoryPayload> CODEC = CustomPacketPayload.codec(PickUpAccessoryPayload::write, PickUpAccessoryPayload::read);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.blockPos);
		buffer.writeVarInt(this.accessoryIndex);
	}

	private static PickUpAccessoryPayload read(RegistryFriendlyByteBuf buffer) {
		return new PickUpAccessoryPayload(buffer.readBlockPos(), buffer.readVarInt());
	}
}
