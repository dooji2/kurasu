package com.dooji.kurasu.network;

import com.dooji.kurasu.Kurasu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PlaceAccessoryPayload(
	BlockPos blockPos,
	String accessoryId,
	String partName,
	float localX,
	float localY,
	float localZ,
	float normalX,
	float normalY,
	float normalZ,
	float tangentX,
	float tangentY,
	float tangentZ,
	float bitangentX,
	float bitangentY,
	float bitangentZ,
	float rotation,
	float scale
) implements CustomPacketPayload {
	public static final Identifier PLACE_ACCESSORY_PAYLOAD_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "place_accessory");
	public static final Type<PlaceAccessoryPayload> TYPE = new Type<>(PLACE_ACCESSORY_PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, PlaceAccessoryPayload> CODEC = CustomPacketPayload.codec(PlaceAccessoryPayload::write, PlaceAccessoryPayload::read);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(this.blockPos);
		buffer.writeUtf(this.accessoryId);
		buffer.writeUtf(this.partName);
		buffer.writeFloat(this.localX);
		buffer.writeFloat(this.localY);
		buffer.writeFloat(this.localZ);
		buffer.writeFloat(this.normalX);
		buffer.writeFloat(this.normalY);
		buffer.writeFloat(this.normalZ);
		buffer.writeFloat(this.tangentX);
		buffer.writeFloat(this.tangentY);
		buffer.writeFloat(this.tangentZ);
		buffer.writeFloat(this.bitangentX);
		buffer.writeFloat(this.bitangentY);
		buffer.writeFloat(this.bitangentZ);
		buffer.writeFloat(this.rotation);
		buffer.writeFloat(this.scale);
	}

	private static PlaceAccessoryPayload read(RegistryFriendlyByteBuf buffer) {
		return new PlaceAccessoryPayload(
			buffer.readBlockPos(),
			buffer.readUtf(),
			buffer.readUtf(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat(),
			buffer.readFloat()
		);
	}
}
