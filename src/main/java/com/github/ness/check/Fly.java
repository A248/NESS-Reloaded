package com.github.ness.check;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.github.ness.CheckManager;
import com.github.ness.NessPlayer;
import com.github.ness.Violation;
import com.github.ness.utility.Utilities;
import com.github.ness.utility.Utility;

public class Fly extends AbstractCheck<PlayerMoveEvent> {

	protected HashMap<String, Integer> noground = new HashMap<String, Integer>();

	public Fly(CheckManager manager) {
		super(manager, CheckInfo.eventOnly(PlayerMoveEvent.class));
	}

	@Override
	void checkEvent(PlayerMoveEvent e) {
		Check(e);
		Check1(e);
		Check3(e);
		Check4(e);
		Check8(e);
		Check9(e);
		Check10(e);
		Check11(e);
		Check12(e);
		Check16(e);
		Check17(e);
		Check18(e);
		Check18(e);
		Check19(e);
	}

	protected List<String> bypasses = Arrays.asList("slab", "stair", "snow", "bed", "skull", "step", "slime");
	
	public void punish(Player p, String module) {
		if(!Utility.hasflybypass(p)) {
			manager.getPlayer(p).setViolation(new Violation("Fly", module));
		}
	}
	
    /**
     * Check to detect no velocity(normal velocity should be upper than -0.07) This detect stupid fly
     * @param event
     */
	public void Check(PlayerMoveEvent event) {
		if (!bypass(event.getPlayer()) && !Utility.hasBlock(event.getPlayer(), Material.SLIME_BLOCK)) {
			Player player = event.getPlayer();
			if (!event.getPlayer().isOnGround()) {
				double fallDist = event.getPlayer().getFallDistance();
				if (event.getPlayer().getVelocity().getY() < -1.0D && fallDist == 0.0D) {
					if (player.getHealth() > 2) {
						player.setHealth(player.getHealth() - 1.0D);
					}
					punish(player, "NoVelocity");
				}
			}

		}
	}
    /**
     * Check to detect max distance on ladder
     * @param event
     */
	public void Check1(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		if (!bypass(event.getPlayer())) {
			if (Utilities.isClimbableBlock(p.getLocation().getBlock()) && !Utilities.isInWater(p)) {
				double distance = Utility.around(event.getTo().getY() - event.getFrom().getY(), 6);
				if (distance > 0.12D) {
					if (distance == 0.164D || distance == 0.248D || distance == 0.333D || distance == 0.419D) {
						return;
					}
					punish(p, "FastLadder");
				}
			}

		}
	}
    /**
     * Check for high y distance
     * @param event
     */
	public void Check3(PlayerMoveEvent event) {
		int airBuffer = 0;
		double lastYOffset = 0.0D;
		Player p = event.getPlayer();
		if (!bypass(event.getPlayer())) {
			double deltaY = event.getFrom().getY() - event.getTo().getY();
			if (deltaY > 1.0D && p.getFallDistance() < 1.0F
					|| deltaY > 3.0D && !Utility.hasBlock(p, Material.SLIME_BLOCK)) {
				punish(p, "AirCheck");
			} else {
				if (p.getFallDistance() >= 1.0F) {
					airBuffer = 10;
				}

				if (airBuffer > 0) {
					return;
				}

				Location playerLoc = p.getLocation();
				double change = Math.abs(deltaY - lastYOffset);
				float maxChange = 0.8F;
				if (Utility.isInAir(p) && playerLoc.getBlock().getType() == Material.AIR && change > maxChange
						&& change != 0.5D && !Utility.hasBlock(p, Material.SLIME_BLOCK)) {
					punish(p, "AirCheck");
				}
			}

		}
	}
    /**
     * Check to detect AirJump
     * @param e
     */
	public void Check4(PlayerMoveEvent e) {
		final Location from = e.getFrom();
		final Location to = e.getTo();
		double fromy = e.getFrom().getY();
		double toy = e.getTo().getY();
		final Player p = e.getPlayer();
		final double defaultvalue = 0.08307781780646906D;
		final double defaultjump = 0.41999998688697815D;
		final double distance = toy - fromy;
		if (!bypass(e.getPlayer()) && !from.getBlock().getType().isSolid() && !to.getBlock().getType().isSolid()) {
			Bukkit.getScheduler().runTaskLater(manager.getNess(), () -> {
				if (to.getY() > from.getY()) {
					if (distance > defaultjump) {
						ArrayList<Block> blocchivicini = Utility.getSurrounding(Utilities.getPlayerUnderBlock(p), true);
						boolean bypass = Utility.hasBlock(p, Material.SLIME_BLOCK);
						Iterator<Block> var4 = blocchivicini.iterator();

						while (var4.hasNext()) {
							Block s = var4.next();
							Iterator<String> var6 = bypasses.iterator();

							while (var6.hasNext()) {
								String b = var6.next();
								if (s.getType().toString().toLowerCase().contains(b)) {
									bypass = true;
								}
							}
						}

						if (!bypass) {
							punish(p, "AirJump");
						}
					} else if (distance == defaultvalue || distance == defaultjump) {
						Location loc = p.getLocation();
						Location loc1 = p.getLocation();
						loc1.setY(loc.getY() - 2.0D);
						if (loc1.getBlock().getType() == Material.AIR
								&& Utilities.getPlayerUnderBlock(p).getType().equals(Material.AIR)
								&& p.getVelocity().getY() <= -0.078D
								&& !loc.getBlock().getType().name().contains("STAIR")
								&& !loc1.getBlock().getType().name().contains("STAIR") && p.getNoDamageTicks() <= 1) {
							punish(p, "AirJump");
						}
					}
				}

			}, 2L);
		}
	}
    /**
     * Check for abnormal ground packet
     * @param e
     */
	public void Check8(PlayerMoveEvent e) {
		Player player = e.getPlayer();
		if (!bypass(e.getPlayer())) {
			if (player.isOnline() && !Utility.hasBlock(player, Material.SLIME_BLOCK) && player.isOnGround()
					&& !Utility.checkGround(e.getTo().getY())) {
				punish(player, "FalseGround");
			}

		}
	} 
    /**
     * Check for Invalid Velocity
     * @param event
     */
	public void Check9(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (player.isOnline() && !Utility.hasBlock(player, Material.SLIME_BLOCK)) {
			Location to = event.getTo();
			Location from = event.getFrom();
			if (!player.getAllowFlight() && player.getVehicle() == null
					&& !player.hasPotionEffect(PotionEffectType.SPEED)) {
				double dist = Math.pow(to.getX() - from.getX(), 2.0D) + Math.pow(to.getZ() - from.getZ(), 2.0D);
				double motion = dist / 0.9800000190734863D;
				if (motion >= 1.0D && dist >= 0.8D && !bypass(player)) {
					punish(player, "InvalidMotion");
				}
			}
		}

	}
    /**
     * Check for High Distance
     * @param e
     */
	public void Check10(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		double diff = e.getTo().getY() - e.getFrom().getY();
		if (e.getTo().getY() >= e.getFrom().getY()) {
			if (p.getLocation().getBlock().getType() != Material.CHEST
					&& p.getLocation().getBlock().getType() != Material.TRAPPED_CHEST
					&& p.getLocation().getBlock().getType() != Material.ENDER_CHEST
					&& !Utility.checkGround(p.getLocation().getY()) && !Utility.isOnGround(p)
					&& Math.abs(p.getVelocity().getY() - diff) > 1.0E-6D && e.getFrom().getY() < e.getTo().getY()
					&& (p.getVelocity().getY() >= 0.0D || p.getVelocity().getY() < -0.392D)
					&& p.getNoDamageTicks() == 0.0D && bypass(p)) {
				punish(p, "");
			}

		}
	}
    /**
     * Check for glide cheat
     * @param e
     */
	public void Check11(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (!bypass(e.getPlayer())) {
			if (!p.getLocation().getBlock().isLiquid()) {
				if (!Utility.checkGround(p.getLocation().getY()) && !Utility.isOnGround(p)) {
					ArrayList<Block> blocks = Utility.getSurrounding(p.getLocation().getBlock(), true);
					Iterator<Block> var4 = blocks.iterator();

					while (var4.hasNext()) {
						Block b = var4.next();
						if (b.isLiquid()) {
							return;
						}

						if (b.getType().isSolid()) {
							return;
						}
					}

					if (!bypass(e.getPlayer()) && !e.getFrom().getBlock().getType().isSolid()
							&& !e.getTo().getBlock().getType().isSolid()) {
						double dist = e.getFrom().getY() - e.getTo().getY();
						NessPlayer player = manager.getPlayer(p);
						double oldY = player.getOldY();
						player.setOldY(dist);
						if (e.getFrom().getY() > e.getTo().getY()) {
							if (oldY >= dist && oldY != 0.0D) {
								punish(p, "Glide");
							}
						} else {
							player.setOldY(0.0D);
						}

					}
				}
			}
		}
	}
    /**
     * Check for NoGroundFly
     * @param e
     */
	public void Check12(PlayerMoveEvent e) {
		Player player = e.getPlayer();
		Location from = e.getFrom();
		Location to = e.getTo();
		boolean groundAround = Utility.groundAround(player.getLocation());
		if (player.isInsideVehicle() && !groundAround && from.getY() <= to.getY() && (!player.isInsideVehicle()
				|| (player.isInsideVehicle() && player.getVehicle().getType() != EntityType.HORSE))) {
			punish(player, "NoGround");
		}
	}
    /**
     * Check for high pitch and yaw
     * @param e
     */
	public void Check16(PlayerMoveEvent e) {
		Player player = e.getPlayer();
		if (player.getLocation().getYaw() > 360.0f || player.getLocation().getYaw() < -360.0f
				|| player.getLocation().getPitch() > 90.0f || player.getLocation().getPitch() < -90.0f) {
			punish(player, "IllegalMovement");
		}
	}
    /**
     * Check for high web distance
     * @param e
     */
	public void Check17(PlayerMoveEvent e) {
		Player player = e.getPlayer();
		Location from = e.getFrom();
		Location to = e.getTo();
		if (player.isFlying() || player.hasPotionEffect(PotionEffectType.SPEED)) {
			return;
		}
		Double hozDist = Utility.getMaxSpeed(from, to);
		if (from.getBlock().getType() == Material.WEB && hozDist > 0.01) {
			punish(player, "NoWeb");
			// player.sendMessage("NoWebDist: " + hozDist);
		}
	}
    /**
     * Another Velocity Check
     * @param e
     */
	public void Check18(PlayerMoveEvent e) {
		Location to = e.getTo();
		Location from = e.getFrom();
		Player p = e.getPlayer();
		double x = to.getX() - from.getX();
		double y = to.getY() - from.getY();
		if (Double.toString(y).length() > 4) {
			y = Utility.around(y, 5);
		}
		double z = to.getZ() - from.getZ();
		Vector v = new Vector(x, y, z);
		// Vector result = v.subtract(p.getVelocity());
		Vector result = v.subtract(p.getVelocity().setY(Utility.around(p.getVelocity().getY(), 5)));
		double yresult = 0.0;
		if (p.isOnGround()) {
			return;
		}
		try {
			yresult = Utility.around(result.getY(), 5);
		} catch (Exception ex) {
			yresult = result.getY();
		}
		if (!Utilities.isAround(to, to.getBlock().getType())) {
			return;
		}
		if (!(yresult == 0.07)) {
			if (!(yresult == 0.0)) {
				if (!(yresult == -0.01)) {
					if (!(yresult == -0.03)) {
						if (yresult > 0.06 && Utilities.getPlayerUnderBlock(p).getType().equals(Material.AIR)) {
							p.sendMessage("Dist:" + yresult);
							// manager.getPlayer(e.getPlayer()).setViolation(new Violation("Fly",
							// "InvalidVelocity"));
						}
					}
				}
			}
		}
	}
   /**
    * Check to detect NoFall
    * @param e
    */
	public void Check19(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (p.isFlying())
			return;
		if (e.getTo().getY() > e.getFrom().getY())
			return;
		if (p.isOnGround() && !Utility.isOnGroundBypassNoFall(p)) {
			for (int i = 0; i < 256; i++) {
				Location l = (new Location(p.getWorld(), p.getLocation().getX(), p.getLocation().getY() - 1.0D,
						p.getLocation().getZ())).getBlock().getLocation();
				if (l.getBlock().getType() == Material.AIR) {
					punish(p, "NoFall");
				}
			}

		}
	}

	public boolean bypass(Player p) {
		if (p.isInsideVehicle()) {
			return false;
		}
		if (p.hasPotionEffect(PotionEffectType.SPEED) || p.hasPotionEffect(PotionEffectType.JUMP)) {
			return false;
		}
		if (Utilities.isInWeb(p)) {
			return false;
		}
		if (Utility.hasflybypass(p)) {
			return false;
		}
		if (Utilities.getPlayerUnderBlock(p).getType().equals(Material.LADDER)
				&& !Utilities.getPlayerUnderBlock(p).getType().equals(Material.VINE)
				&& Utilities.getPlayerUnderBlock(p).getType().equals(Material.WATER)) {
			return false;
		}
		for (Block b : Utility.getSurrounding(p.getLocation().getBlock(), true)) {
			if (b.getType().isSolid()) {
				return false;
			}
		}
		return true;
	}
}
