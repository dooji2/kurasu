package com.dooji.kurasu.network;

import com.dooji.kurasu.Kurasu;
import java.util.Arrays;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SaveStickyNotePayload(int width, int height, int[] pixels) implements CustomPacketPayload {
	public static final Identifier SAVE_STICKY_NOTE_PAYLOAD_ID = Identifier.fromNamespaceAndPath(Kurasu.MOD_ID, "save_sticky_note");
	public static final Type<SaveStickyNotePayload> TYPE = new Type<>(SAVE_STICKY_NOTE_PAYLOAD_ID);
	public static final StreamCodec<RegistryFriendlyByteBuf, SaveStickyNotePayload> CODEC = CustomPacketPayload.codec(SaveStickyNotePayload::write, SaveStickyNotePayload::read);

	public SaveStickyNotePayload {
		pixels = Arrays.copyOf(pixels, width * height);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeVarInt(this.width);
		buffer.writeVarInt(this.height);
		buffer.writeVarIntArray(this.pixels);
	}

	private static SaveStickyNotePayload read(RegistryFriendlyByteBuf buffer) {
		int width = buffer.readVarInt();
		int height = buffer.readVarInt();
		int[] pixels = buffer.readVarIntArray(width * height);
		return new SaveStickyNotePayload(width, height, pixels);
	}
}
