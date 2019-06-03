package com.openrsc.server.net.rsc.handlers;

import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;
import com.openrsc.server.net.Packet;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.net.rsc.PacketHandler;

public final class GameSettingHandler implements PacketHandler {

	public static final World world = World.getWorld();

	public void handlePacket(Packet p, Player player) {

		int idx = (int) p.readByte();
		if (idx < 0 || idx > 99) {
			player.setSuspiciousPlayer(true);
			return;
		}

		if (idx >= 4) {
			if (idx == 4) {
				player.getCache().set("setting_android_longpress", p.readByte());
			} else if (idx == 7) {
				player.getCache().store("setting_android_holdnchoose", p.readByte() == 1);
			} else if (idx == 8) {
				player.getCache().store("block_tab_messages", p.readByte() == 1);
			} else if (idx == 9) {
				player.getCache().set("setting_block_global", p.readByte());
			} else if (idx == 10) {
				player.getCache().store("p_xp_notifications_enabled", p.readByte() == 1);
			} else if (idx == 11) {
				player.getCache().store("p_block_invites", p.readByte() == 1);
			} else if (idx == 16) {
				player.getCache().store("setting_volume_rotate", p.readByte() == 1);
			} else if (idx == 17) {
				player.getCache().store("setting_swipe_rotate", p.readByte() == 1);
			} else if (idx == 18) {
				player.getCache().store("setting_swipe_scroll", p.readByte() == 1);
			} else if (idx == 19) {
				player.getCache().set("setting_press_delay", p.readByte());
			} else if (idx == 20) {
				player.getCache().set("setting_font_size", p.readByte());
			} else if (idx == 21) {
				player.getCache().store("setting_hold_choose", p.readByte() == 1);
			} else if (idx == 22) {
				player.getCache().store("setting_swipe_zoom", p.readByte() == 1);
			} else if (idx == 23) {
				player.getCache().set("setting_last_zoom", p.readByte());
			} else if (idx == 24) {
				player.getCache().store("setting_batch_progressbar", p.readByte() == 1);
			} else if (idx == 25) {
				player.getCache().store("setting_experience_drops", p.readByte() == 1);
			} else if (idx == 26) {
				player.getCache().store("setting_showroof", p.readByte() == 1);
			} else if (idx == 27) {
				player.getCache().store("setting_showfog", p.readByte() == 1);
			} else if (idx == 28) {
				player.getCache().set("setting_ground_items", p.readByte());
			}
			return;
		}

		boolean on = p.readByte() == 1;
		player.getSettings().setGameSetting(idx, on);
		ActionSender.sendGameSettings(player);
	}
}
