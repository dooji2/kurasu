package com.dooji.kurasu;

import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;

public final class KurasuPermissions {
	private KurasuPermissions() {
	}

	public static boolean canSwitchGameModes(Player player) {
		return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}
}
