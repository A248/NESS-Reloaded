package com.github.ness.check.movement;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;

import com.github.ness.NESSAnticheat;
import com.github.ness.NessPlayer;
import com.github.ness.check.CheckInfos;
import com.github.ness.check.ListeningCheck;
import com.github.ness.check.ListeningCheckFactory;
import com.github.ness.check.ListeningCheckInfo;
import com.github.ness.data.PlayerAction;
import com.github.ness.utility.Utility;

public class Jesus extends ListeningCheck<PlayerMoveEvent> {
    
	public static final ListeningCheckInfo<PlayerMoveEvent> checkInfo = CheckInfos
			.forEvent(PlayerMoveEvent.class);

	public Jesus(ListeningCheckFactory<?, PlayerMoveEvent> factory, NessPlayer player) {
		super(factory, player);
	}

    @Override
    protected void checkEvent(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        NessPlayer nessPlayer = this.player();
        float dist = (float) nessPlayer.getMovementValues().XZDiff; // Our XZ Distance
        double walkSpeed = p.getWalkSpeed() * 0.7;
        if (NESSAnticheat.getInstance().getMinecraftVersion() > 1122) {
            walkSpeed = p.getWalkSpeed() * 0.9;
        }
        dist -= (dist / 100.0) * (Utility.getPotionEffectLevel(p, PotionEffectType.SPEED) * 20.0);
        if (nessPlayer.nanoTimeDifference(PlayerAction.VELOCITY) < 1300) {
            dist -= Math.abs(nessPlayer.velocity.getX()) + Math.abs(nessPlayer.velocity.getZ());
        }
        final double yVelocity = Math.abs(p.getVelocity().getY()) * 0.30;
        walkSpeed += yVelocity;
        if (dist > walkSpeed && event.getTo().getBlock().isLiquid()
                && event.getTo().clone().add(0, 0.01, 0).getBlock().isLiquid()
                && event.getTo().clone().add(0, -0.01, 0).getBlock().isLiquid() && event.getFrom().getBlock().isLiquid()
                && !Utility.hasflybypass(p)) {
        	flagEvent(event);
        }
    }

}
