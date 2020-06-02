package com.github.ness.check;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.github.ness.CheckManager;
import com.github.ness.NessPlayer;
import com.github.ness.api.Violation;

public class AutoClick extends AbstractCheck<PlayerInteractEvent> {

	private final int hardLimit;
	private final int hardLimitRetentionSecs;

	private final int constancyThreshold;
	private final int constancyDeviation;
	private final int constancyMinSample;

	private final int constancySuperDeviation;
	private final int constancySuperMinSample;

	private final long constancySpan;

	private final int totalRetentionSecs;
	
	private static final Logger logger = LoggerFactory.getLogger(AutoClick.class);
	
	public AutoClick(CheckManager manager) {
		super(manager, CheckInfo.eventWithAsyncPeriodic(PlayerInteractEvent.class, 4, TimeUnit.SECONDS));

		ConfigurationSection section = manager.getNess().getNessConfig().getCheck(AutoClick.class);

		totalRetentionSecs = section.getInt("total-retention-secs", 32);

		hardLimit = section.getInt("hard-limit.cps", 16);
		hardLimitRetentionSecs = section.getInt("hard-limit.retention-span-secs", 4);

		constancyThreshold = section.getInt("constancy.threshold", 4);
		constancyDeviation = section.getInt("constancy.deviation-percent", 20);
		constancyMinSample = section.getInt("constancy.min-sample", 6);

		constancySuperDeviation = section.getInt("constancy.super.deviation-percent", 10);
		constancySuperMinSample = section.getInt("constancy.super.min-sample", 12);

		constancySpan = section.getLong("constancy.span-millis", 800);
		logger.debug("Check configuration: totalRetentionSecs {}, hardLimit {}, hardLimitRetentionSecs {}, "
				+ "constancyThreshold {}, constancyDeviation {}, constancyMinSample {},"
				+ "constancySuperDeviation {}, constancySuperMinSample{}", totalRetentionSecs, hardLimit, hardLimitRetentionSecs,
				constancyThreshold, constancyDeviation, constancyMinSample, constancySuperDeviation, constancySuperMinSample);
	}
	
	private long totalRetentionMillis() {
		return totalRetentionSecs * 1_000L;
	}
	
	private long hardLimitRetentionMillis() {
		return hardLimitRetentionSecs * 1_000L;
	}
	
	private static long monotonicMillis() {
		return System.nanoTime() / 1_000_000L;
	}

	@Override
	void checkAsyncPeriodic(NessPlayer player) {
		// Cleanup old history
		Set<Long> clickHistory = player.getClickHistory();
		long now1 = monotonicMillis();
		long totalRetentionMillis = totalRetentionMillis();
		clickHistory.removeIf((time) -> time - now1 > totalRetentionMillis);

		if (clickHistory.isEmpty()) {
			return; // Don't check players who aren't clicking
		}

		// Copy temporary state
		Set<Long> copy1 = new HashSet<>(clickHistory);
		List<Long> copy2 = new ArrayList<>(copy1);

		// Hard limit check

		long now2 = monotonicMillis();
		long hardLimitRetentionMillis = hardLimitRetentionMillis();
		copy1.removeIf((time) -> time - now2 > hardLimitRetentionMillis);
		int cps = copy1.size() / hardLimitRetentionSecs;
		logger.debug("Clicks Per Second: {}", cps);
		if (cps > hardLimit) {
			player.setViolation(new Violation("AutoClick", Integer.toString(cps)));
			return;
		}

		// Constancy check

		if (cps <= constancyThreshold) {
			return;
		}
		/*
		 * Task: Measure the standard deviation of the intervals between clicks
		 * 
		 */
		copy2.sort(null);
		List<Long> periods = new ArrayList<>();

		/*
		 * Centering our numbers such that the first is zero
		 */
		long initial = copy2.get(0); // we center at this number
		long start = 0L;
		for (int n = 1; n < copy2.size(); n++) {
			long end = copy2.get(n) - initial;
			periods.add(end - start);
			start = end;
		}
		logger.debug("Click periods: {}", periods);

		/*
		 * Sublist of the total list of periods
		 * 
		 */
		List<Long> subPeriods = new ArrayList<>();
		/*
		 * Standard deviation percentages of all the subspans
		 * 
		 */
		List<Long> standardDeviations = new ArrayList<>();
		for (int n = 0; n < periods.size(); n++) {
			long subStart = periods.get(n);
			long subPeriod;
			for (int m = n; (subPeriod = periods.get(m)) - subStart < constancySpan; m++) {
				subPeriods.add(subPeriod);
			}
			if (subPeriods.size() >= constancyMinSample) {
				int stdDevPercent = getStdDevPercent(subPeriods);
				if (stdDevPercent < constancyDeviation) {
					player.setViolation(new Violation("AutoClick", cps + " " + stdDevPercent));
					return;
				}
				standardDeviations.add((long) stdDevPercent);
			}
			logger.debug("Sub click periods: {}", subPeriods);
			subPeriods.clear();
		}
		if (standardDeviations.size() >= constancySuperMinSample) {
			int superStdDevPercent = getStdDevPercent(standardDeviations);
			if (superStdDevPercent < constancySuperDeviation) {
				player.setViolation(new Violation("AutoClick", cps + " " + superStdDevPercent));
			}
		}
	}
	
	/**
	 * Visible for testing. <br>
	 * Calculates the standard deviation as a percent of the average
	 * 
	 * @param periods the numbers from which to calculate the standard deviation percentage
	 * @return the percentage
	 */
	static int getStdDevPercent(List<Long> periods) {
		long average = calculateAverage(periods);

		double standardDeviation = 0;
		for (long period : periods) {
			standardDeviation += Math.pow(period - average, 2);
		}
		standardDeviation = Math.sqrt(standardDeviation / periods.size());
		logger.trace("Standard deviation is calculated to be {}", standardDeviation);
		return (int) (100 * standardDeviation / average);
	}
	
	/**
	 * Visible for testing. <br>
	 * Calculates the average from a list of samples
	 * 
	 * @param samples the numbers from which to calculate the average
	 * @return the average
	 */
	static long calculateAverage(List<Long> samples) {
		long sum = 0L;
		for (long period : samples) {
			sum += period;
		}
		long result = sum / samples.size();
		logger.trace("Calculated average is {}", result);
		return result;
	}

	@Override
	void checkEvent(PlayerInteractEvent evt) {
		Action action = evt.getAction();
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			Player player = evt.getPlayer();
			logger.trace("Added click from {}", player);
			manager.getPlayer(player).getClickHistory().add(monotonicMillis());
		}
	}

}
