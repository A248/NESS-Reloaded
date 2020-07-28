package com.github.ness.check;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import com.github.ness.CheckManager;
import com.github.ness.NessPlayer;
import com.github.ness.api.Violation;
import com.github.ness.utility.Utility;

public class Aimbot extends AbstractCheck<PlayerMoveEvent> {

	public Aimbot(CheckManager manager) {
		super(manager, CheckInfo.eventOnly(PlayerMoveEvent.class));
		// TODO Auto-generated constructor stub
	}

	@Override
	void checkEvent(PlayerMoveEvent e) {
		Check(e);
		Check1(e);
		Check3(e);
		Check4(e);
	}

	/**
	 * All changes in pitch should be divisible by a constant. That constant is
	 * determined by your mouse sensitivity in game. By calculating the gcd and
	 * finding that constant, you can detect changes in pitch variation. This Check
	 * was made by Islandscout! (@Islandscout#2588 on Discord) His AntiCheat: Hawk
	 * AntiCheat
	 * https://www.spigotmc.org/resources/hawk-anticheat-mc-1-7-10-1-8-8.40343/
	 * 
	 */
	public boolean Check(PlayerMoveEvent e) {
		int samples = 20;
		int pitchlimit = 10;
		Player p = e.getPlayer();
		NessPlayer player = manager.getPlayer(p);
		if (player == null || Utility.hasVehicleNear(p, 3)) {
			return false;
		}
		float deltaPitch = (float) player.getMovementValues().pitchDiff;
		final List<Float> lastDeltaPitches = player.getPitchdelta();

		// ignore if deltaPitch is 0 or >= 10 or if pitch is +/-90.
		if (deltaPitch != 0 && Math.abs(deltaPitch) <= pitchlimit && Math.abs(e.getTo().getPitch()) != 90) {
			lastDeltaPitches.add(Math.abs(deltaPitch));
		}

		if (lastDeltaPitches.size() >= samples) {
			float deltaPitchGCD = Utility.mcdRational(lastDeltaPitches);
			float lastmcdpitch = player.getLastmcdpitch();
			float lastDeltaPitchGCD = (lastmcdpitch != Float.MIN_VALUE) ? lastmcdpitch : deltaPitchGCD;
			float gcdDiff = Math.abs(deltaPitchGCD - lastDeltaPitchGCD);
			// if GCD is significantly different or if GCD is practically unsolvable
			if (gcdDiff > 0.001 || deltaPitchGCD < 0.00001) {
				manager.getPlayer(e.getPlayer()).setViolation(new Violation("Aimbot", "PitchPattern"));
				return true;
			}
			lastDeltaPitches.clear();
			player.setLastmcdpitch(deltaPitchGCD);
		}
		return false;

	}

	/**
	 * Check for some Aimbot Pattern
	 */
	public boolean Check1(PlayerMoveEvent e) {
		NessPlayer player = manager.getPlayer(e.getPlayer());
		float yawChange = (float) Math.abs(player.getMovementValues().yawDiff);
		float pitchChange = (float) Math.abs(player.getMovementValues().pitchDiff);
		if (yawChange >= 1.0f && yawChange % 0.1f == 0.0f) {
			if (player.shouldCancel(e, this.getClass().getSimpleName())) {
				e.setCancelled(true);
			}
			player.setViolation(new Violation("Aimbot", "PerfectAura"));
			return true;
		} else if (pitchChange >= 1.0f && pitchChange % 0.1f == 0.0f) {
			if (player.shouldCancel(e, this.getClass().getSimpleName())) {
				e.setCancelled(true);
			}
			player.setViolation(new Violation("Aimbot", "PerfectAura1"));
			return true;
		}
		return false;
	}

	public void Check3(PlayerMoveEvent e) {
		NessPlayer np = manager.getPlayer(e.getPlayer());
		float yawChange = (float) Math.abs(np.getMovementValues().yawDiff);
		float pitchChange = (float) Math.abs(np.getMovementValues().pitchDiff);
		if (yawChange >= 1.0F && yawChange % 0.1F == 0.0F) {
			if (yawChange % 1.0F == 0.0F || yawChange % 10.0F == 0.0F || yawChange % 30.0F == 0.0F) {
				np.setViolation(new Violation("Aimbot", "Pattern1"));
				if (manager.getPlayer(e.getPlayer()).shouldCancel(e, this.getClass().getSimpleName())) {
					e.setCancelled(true);
				}
			}
		} else if (pitchChange >= 1.0F && pitchChange % 0.1F == 0.0F) {
			if (pitchChange % 1.0F == 0.0F || pitchChange % 10.0F == 0.0F || pitchChange % 30.0F == 0.0F) {
				np.setViolation(new Violation("Aimbot", "Pattern2"));
				if (manager.getPlayer(e.getPlayer()).shouldCancel(e, this.getClass().getSimpleName())) {
					e.setCancelled(true);
				}
			}
		}
	}

	public void Check4(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		double yaw = Math.abs(this.manager.getPlayer(p).getMovementValues().yawDiff);
		NessPlayer np = this.manager.getPlayer(p);
		if (Math.abs(yaw) > 9356) {
			yaw = 0.0;
		}
		if ((Math.round(yaw) == yaw && yaw != 0.0)) {
			np.AimbotPatternCounter = np.AimbotPatternCounter + 1;
			if (np.AimbotPatternCounter > 4) {
				np.setViolation(new Violation("Aimbot", "Pattern3"));
				if (manager.getPlayer(e.getPlayer()).shouldCancel(e, this.getClass().getSimpleName())) {
					e.setCancelled(true);
				}
				np.AimbotPatternCounter = 0;
			}
		}
	}
}