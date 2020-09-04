package com.github.ness.check.movement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import com.github.ness.CheckManager;
import com.github.ness.NESSAnticheat;
import com.github.ness.NessPlayer;
import com.github.ness.api.Violation;
import com.github.ness.check.AbstractCheck;
import com.github.ness.check.CheckInfo;
import com.github.ness.data.ImmutableLoc;
import com.github.ness.data.PlayerAction;
import com.github.ness.utility.ReflectionUtility;
import com.github.ness.utility.Utility;

public class Fly extends AbstractCheck<PlayerMoveEvent> {

	double maxInvalidVelocity;

	public Fly(CheckManager manager) {
		super(manager, CheckInfo.eventOnly(PlayerMoveEvent.class));
		this.maxInvalidVelocity = this.manager.getNess().getNessConfig().getCheck(this.getClass())
				.getDouble("maxinvalidvelocity", 0.9);
	}

	@Override
	protected void checkEvent(PlayerMoveEvent e) {
		Check(e);
		Check1(e);
		Check2(e);
		Check3(e);
		Check4(e);
	}

	public void punish(PlayerMoveEvent e, String module) {
		if (!Utility.hasflybypass(e.getPlayer())) {
			manager.getPlayer(e.getPlayer()).setViolation(new Violation("Fly", module), e);
		}
	}

	/**
	 * Check for Invalid Upper Motion
	 */
	public void Check(PlayerMoveEvent e) {
		NessPlayer nessPlayer = this.manager.getPlayer(e.getPlayer());
		Player p = e.getPlayer();
		double y = nessPlayer.getMovementValues().yDiff;
		if (Utility.isMathematicallyOnGround(e.getTo().getY()) || Utility.hasflybypass(p)
				|| Utility.hasBlock(p, "slime") || p.getAllowFlight() || Utility.isInWater(p)
				|| Utility.specificBlockNear(e.getTo().clone(), "lily")
				|| Utility.specificBlockNear(e.getTo().clone(), "sea")
				|| Utility.specificBlockNear(e.getTo().clone(), "slab")
				|| Utility.specificBlockNear(e.getTo().clone(), "stair")
				|| Utility.specificBlockNear(e.getTo().clone(), "water")
				|| ReflectionUtility.getBlockName(p, ImmutableLoc.of(p.getLocation().clone().add(0, -0.5, 0)))
						.contains("scaffolding")
				|| ReflectionUtility.getBlockName(p, ImmutableLoc.of(p.getLocation().clone().add(0, 0.5, 0)))
						.contains("scaffolding")
				|| Utility.getMaterialName(e.getTo()).contains("ladder")
				|| Utility.specificBlockNear(e.getTo(), "ladder") || nessPlayer.isTeleported()) {
			nessPlayer.flyYSum = 0;
			return;
		}
		if (nessPlayer.nanoTimeDifference(PlayerAction.VELOCITY) < 1500) {
			y -= Math.abs(nessPlayer.velocity.getY());
		}
		if (y > 0) {
			nessPlayer.flyYSum += y;
			double max = 1.30;
			double jumpBoost = Utility.getPotionEffectLevel(p, PotionEffectType.JUMP);
			max += jumpBoost * (max / 2);
			if (nessPlayer.flyYSum > max && p.getVelocity().getY() < 0) {
				punish(e, " HighJump ySum: " + nessPlayer.flyYSum);
			}
		}
	}

	/**
	 * Check for abnormal ground packet
	 * 
	 * @param e
	 */
	public void Check1(PlayerMoveEvent e) {
		Player player = e.getPlayer();
		if (Bukkit.getVersion().contains("1.8")
				|| Utility.getMaterialName(e.getTo().clone().add(0, -0.5, 0)).contains("lily")
				|| Utility.getMaterialName(e.getTo().clone().add(0, -0.5, 0)).contains("carpet")
				|| Utility.specificBlockNear(e.getTo(), "carpet") || Utility.specificBlockNear(e.getTo(), "lily")
				|| Utility.specificBlockNear(e.getTo(), "snow") || NESSAnticheat.getInstance().getVersion() > 1152) {
			return;
		}
		NessPlayer nessPlayer = this.getNessPlayer(player);
		if (nessPlayer.nanoTimeDifference(PlayerAction.VELOCITY) < 1500 && nessPlayer.velocity.getY() > 0.34) {
			return;
		}
		if (Utility.getMaterialName(e.getTo().clone()).contains("lily")
				|| Utility.getMaterialName(e.getTo().clone()).contains("carpet")
				|| ReflectionUtility.getBlockName(player, ImmutableLoc.of(player.getLocation().clone().add(0, -0.5, 0)))
						.contains("scaffolding")
				|| Utility.getMaterialName(e.getTo().clone()).contains("ladder")) {
			return;
		}
		if (!nessPlayer.isTeleported() && player.getNearbyEntities(2, 2, 2).isEmpty() && !Utility.hasflybypass(player) && player.isOnline() && !Utility.hasBlock(player, "slime") && !player.isInsideVehicle() && !Utility.specificBlockNear(e.getTo().clone(), "web")) {
			if (player.isOnGround() && !Utility.groundAround(e.getTo())) {
				punish(e, "FalseGround");
			} else if (player.isOnGround() && !Utility.isMathematicallyOnGround(e.getTo().getY())) {
				punish(e, "FalseGround1");
			}
		}
	}

	/**
	 * Check for Invalid Gravity
	 * 
	 * @param e
	 */
	public void Check2(PlayerMoveEvent e) {
		NessPlayer np = this.manager.getPlayer(e.getPlayer());
		Player p = e.getPlayer();
		double y = np.getMovementValues().yDiff;
		double yresult = y - p.getVelocity().getY();
		if (Utility.hasflybypass(p) || Utility.hasBlock(p, "slime") || p.getAllowFlight()
				|| Utility.specificBlockNear(e.getTo().clone().add(0, -0.3, 0), "lily") || p.isInsideVehicle()) {
			return;
		}
		double max = maxInvalidVelocity;
		float pingresult = Utility.getPing(p) / 100;
		float toAdd = pingresult / 6;
		max += toAdd;
		if (np.nanoTimeDifference(PlayerAction.VELOCITY) < 3000) {
			y -= Math.abs(np.velocity.getY());
		}
		if (Math.abs(yresult) > max && !manager.getPlayer(e.getPlayer()).isTeleported()) {
			punish(e, "InvalidVelocity: " + yresult);
		}
	}

	/**
	 * Check for Invalid Jump Motion (normal should be 0.42F)
	 * 
	 * @param event
	 */
	public void Check3(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location to = event.getTo().clone();
		Location from = event.getFrom().clone();
		final double yDiff = event.getTo().getY() - event.getFrom().getY();
		if (Utility.getMaterialName(event.getTo().clone().add(0, -0.3, 0)).contains("slab")
				|| event.getTo().getBlock().isLiquid()
				|| event.getTo().clone().add(0, 1.9, 0).getBlock().getType().isSolid()
				|| event.getTo().clone().add(0.3, 1.9, 0).getBlock().getType().isSolid()
				|| event.getTo().clone().add(0, 1.9, 0.3).getBlock().getType().isSolid()
				|| event.getTo().clone().add(-0.3, 1.9, 0).getBlock().getType().isSolid()
				|| event.getTo().clone().add(0, 1.9, -0.3).getBlock().getType().isSolid()
				|| event.getTo().clone().add(0.3, 1.9, 0.3).getBlock().getType().isSolid()
				|| event.getTo().clone().add(-0.3, 1.9, -0.3).getBlock().getType().isSolid()
				|| event.getTo().clone().add(0.3, 1.9, -0.3).getBlock().getType().isSolid()
				|| event.getTo().clone().add(-0.3, 1.9, 0.3).getBlock().getType().isSolid()
				|| Utility.specificBlockNear(event.getTo().clone(), "liquid") || Utility.hasflybypass(player)
				|| Utility.specificBlockNear(player.getLocation().clone(), "snow")
				|| Utility.specificBlockNear(player.getLocation().clone(), "chest")
				|| Utility.specificBlockNear(player.getLocation().clone(), "ladder")
				|| Utility.specificBlockNear(player.getLocation().clone(), "pot")
				|| Utility.specificBlockNear(player.getLocation().clone(), "bed")
				|| Utility.specificBlockNear(player.getLocation().clone(), "detector")
				|| Utility.specificBlockNear(player.getLocation().clone(), "stair")
				|| Utility.getMaterialName(to.clone().add(0, -1, 0)).contains("chest")
				|| Utility.getMaterialName(from.clone().add(0, -1, 0)).contains("chest")
				|| Utility.getMaterialName(to.clone().add(0, 1.8, 0)).contains("chorus")
				|| Utility.getMaterialName(from.clone().add(0, 1.6, 0)).contains("chorus")
				|| Utility.getMaterialName(to).toLowerCase().contains("ladder")
				|| Utility.getMaterialName(from).toLowerCase().contains("ladder")
				|| Utility.getMaterialName(to).toLowerCase().contains("vine")
				|| Utility.getMaterialName(from).toLowerCase().contains("vine")
				|| Utility.getMaterialName(to).toLowerCase().contains("sea")
				|| Utility.getMaterialName(from).toLowerCase().contains("sea")
				|| Utility.getMaterialName(to.clone().add(0, 0.3, 0)).toLowerCase().contains("sea")
				|| Utility.getMaterialName(to.clone().add(0, -0.2, 0)).toLowerCase().contains("sea")
				|| Utility.getMaterialName(to).toLowerCase().contains("pot")
				|| Utility.getMaterialName(from).toLowerCase().contains("pot")
				|| Utility.getMaterialName(to.clone().add(0, 0.5, 0)).toLowerCase().contains("ladder")
				|| Utility.getMaterialName(from.clone().add(0, 0.5, 0)).toLowerCase().contains("ladder")
				|| Utility.getMaterialName(to.clone().add(0, 0.5, 0)).toLowerCase().contains("bed")
				|| Utility.getMaterialName(from.clone().add(0, 0.5, 0)).toLowerCase().contains("bed")
				|| Utility.getMaterialName(to.clone().add(0, -1, 0)).contains("detector")
				|| Utility.getMaterialName(to.clone().add(0, -0.5, 0)).contains("slime")
				|| Utility.getMaterialName(from.clone().add(0, -1, 0)).contains("detector")
				|| Utility.hasBlock(player, "slime")
				|| Utility.specificBlockNear(to.clone(), "ice") || Utility.hasflybypass(player)) {
			return;
		}
		// !player.getNearbyEntities(4, 4, 4).isEmpty()
		if (yDiff > 0 && !player.isInsideVehicle()) {
			if (player.getVelocity().getY() == 0.42f && !Utility.isMathematicallyOnGround(event.getTo().getY())
					&& Utility.isMathematicallyOnGround(event.getFrom().getY())) {
				double yResult = Math.abs(yDiff - player.getVelocity().getY());
				if (yResult != 0.0 && this.manager.getPlayer(player).nanoTimeDifference(PlayerAction.DAMAGE) > 1000) {
					punish(event, "InvalidJumpMotion yResult: " + yResult + "  yDiff: " + yDiff);
				}
			}
		}
	}

	public void Check4(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (player.isDead()) {
			NessPlayer np = this.manager.getPlayer(player);
			if ((np.getMovementValues().XZDiff > 0.3 || np.getMovementValues().yDiff > 0.16) && !np.isTeleported()) {
				punish(event, "GhostMode");
			}
		}
	}

	public void Check5(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (player.getFallDistance() < 7 && player.getVelocity().getY() < -2.0D) {
			punish(event, "InvalidMove" + "FallDist: " + (float) player.getFallDistance() + " Velocity: "
					+ (float) player.getVelocity().getY());
		}
	}

}
