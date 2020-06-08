package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

class Region extends RulesObject {
	enum Type {
		@SerializedName("land") LAND,
		@SerializedName("water") WATER
	}

	String name;
	Type type;
	Culture culture;
	double population;
	Ideology religion;
	Percentage unrestPopular;
	Noble noble;
	List<Construction> constructions = new ArrayList<>();
	double food;
	double crops;
	boolean cultAccessed;

	private String kingdom;

	private transient boolean foodPinned = false;

	//TODO: does this method belong with kingdom/nation?
	static int numUniqueIdeologies(String kingdom, World w) {
		return (int) w.regions.stream()
				.filter(r -> kingdom.equals(r.getKingdom()))
				.map(r -> r.religion)
				.distinct().count();
	}

	public boolean canFoodTransferTo(World w, Region target) {
		Set<Region> legals = new HashSet<>();
		legals.add(this);
		Deque<Region> stack = new ArrayDeque<>();
		stack.push(this);

		for (Region n : getNeighbors(w)) {
			if (n.isSea()) stack.push(n);
			legals.add(n);
		}

		while (!stack.isEmpty()) {
			Region r = stack.pop();
			for (Region n : r.getNeighbors(w)) {
				if (n.isSea() && !legals.contains(n)) stack.push(n);
				legals.add(n);
			}
		}

		return legals.contains(target);
	}

	public List<Army.Tag> getArmyTags() {
		return culture.getArmyTags();
	}

	public double calcImmigrationWeight(World w) {
		double mod = 1;
		if (religion == Ideology.FLAME_OF_KITH) mod += getRules().flameOfKithImmigrationWeightMod;
		if (w.getNation(kingdom).hasTag(Nation.Tag.WELCOMING)) mod += 1;
		if (Ideology.CHALICE_OF_COMPASSION == w.getDominantIruhanIdeology() && religion.religion == Religion.IRUHAN) mod += 2;
		return (1 - unrestPopular.get()) * mod;
	}

	public double calcRecruitment(World w, Character governor, double signingBonus, double rationing, Army largestInRegion) {
		double base = population * getRules().recruitmentPerPop;
		double unrest = calcUnrest(w);
		if (unrest > getRules().unrestRecruitmentEffectThresh) base *= 1.0 - (unrest - getRules().unrestRecruitmentEffectThresh);

		double mods = 1;
		mods += calcSigningBonusMod(signingBonus);

		if (governor != null) governor.calcGovernRecruitMod();
		if (hasNoble()) mods += noble.calcRecruitMod();

		Nation wKingdom = w.getNation(kingdom);
		if (wKingdom.hasTag(Nation.Tag.PATRIOTIC)) mods += getRules().patrioticMod;
		if (wKingdom.hasTag(Nation.Tag.WARLIKE) && wKingdom.coreRegions.contains(w.regions.indexOf(this))) {
			int conquests = 0;
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).kingdom) && !wKingdom.coreRegions.contains(i)) conquests++;
			mods += conquests * getRules().perConquestWarlikeRecruitmentMod;
		}

		if (religion == Ideology.RJINKU) {
			mods += getRules().rjinkuRecruitmentMod;
		} else if (religion == Ideology.SWORD_OF_TRUTH) {
			mods += getRules().swordOfTruthRecruitmentMod;
		} else if (religion == Ideology.TAPESTRY_OF_PEOPLE) {
			boolean getTapestryBonus = false;
			for (Region r : getNeighbors(w)) if (r.isLand() && (r.culture != culture || r.religion != religion)) getTapestryBonus = true;
			if (getTapestryBonus) mods += getRules().tapestryRecruitmentMod;
		} else if (religion == Ideology.RIVER_OF_KUUN && rationing > 1) {
			mods += (rationing - 1) * 3;
		}

		if (largestInRegion != null && !Nation.isFriendly(kingdom, largestInRegion.kingdom, w) && largestInRegion.hasTag(Army.Tag.PILLAGERS)) mods += getRules().armyPillagersRecruitmentMod;

		return Math.max(0, base * mods);
	}

	// TODO: this belongs alongside the game constants, should determine a way to parameterize these function-type rules
	public double calcSigningBonusMod(double signingBonus){
		return signingBonus <= 0 ? signingBonus * 0.5 : (Math.log(signingBonus) / Math.log(2)) * 0.5 + 0.5;
	}

	public double calcTaxIncome(World w, Character governor, double taxRate, double rationing) {
		double base = population * getRules().taxPerPop;
		double unrest = calcUnrest(w);
		if (unrest > getRules().unrestTaxEffectThresh) base *= 1.0 - (unrest - getRules().unrestTaxEffectThresh);

		double mods = 1;

		if (governor != null) mods += governor.calcGovernTaxMod();
		if (hasNoble()) mods += noble.calcTaxMod();

		Nation wKingdom = w.getNation(kingdom);
		if (wKingdom.hasTag(Nation.Tag.MERCANTILE)) mods += getRules().mercantileTaxMod;
		if (wKingdom.hasTag(Nation.Tag.WARLIKE) && wKingdom.coreRegions.contains(w.regions.indexOf(this))) {
			int conquests = 0;
			for (int i = 0; i < w.regions.size(); i++) if (kingdom.equals(w.regions.get(i).kingdom) && !wKingdom.coreRegions.contains(i)) conquests++;
			mods += conquests * getRules().perConquestWarlikeTaxMod;
		}

		boolean neighborKuun = false;
		if (religion == Ideology.SYRJEN) {
			mods += getRules().syrjenTaxMod;
		} else if (religion == Ideology.TAPESTRY_OF_PEOPLE) {
			boolean getTapestryBonus = false;
			for (Region r : getNeighbors(w)) if (r.isLand() && (r.culture != culture || r.religion != religion)) getTapestryBonus = true;
			if (getTapestryBonus) mods += getRules().tapestryTaxMod;
		} else if (religion == Ideology.RIVER_OF_KUUN && rationing > 1) {
			mods += (rationing - 1) * 3;
		} else if (religion == Ideology.CHALICE_OF_COMPASSION) {
			mods += getRules().chaliceOfCompassionTaxMod;
		}

		return Math.max(0, base * mods * taxRate);
	}

	public double calcConsumption() {
		return population;
	}

	public double calcPirateThreat(World w, boolean wasPatrolled) {
		if (isSea()) return 0;
		if (religion == Ideology.ALYRJA) return 0;

		double unrest = calcUnrest(w);
		double mods = 1;
		if (hasNoble()) mods += getRules().noblePirateThreatMod;
		if (wasPatrolled) mods += getRules().patrolledPirateThreatMod;
		mods += Math.pow(2, w.pirate.bribes.getOrDefault(kingdom, 0.0) / getRules().pirateThreatDoubleGold) - 1;
		return Math.max(0, unrest * mods);
	}

	public void setReligion(Ideology bias, World w) {
		if (bias == null) bias = religion;
		HashMap<Ideology, Integer> ideologies = new HashMap<>();

		for (Construction c : constructions) {
			if (c.type == Construction.Type.TEMPLE) ideologies.put(c.religion, ideologies.getOrDefault(c.religion, 0) + 1);
		}
		int maxV = ideologies.getOrDefault(bias, -1);
		Ideology max = bias;
		for (Ideology r : ideologies.keySet()) {
			if (ideologies.get(r) > maxV) {
				maxV = ideologies.get(r);
				max = r;
			}
		}
		if (Ideology.VESSEL_OF_FAITH == max && religion != max) {
			for (String k : w.getNationNames()) {
				if (Ideology.VESSEL_OF_FAITH != Nation.getStateReligion(k, w)) continue;
				for (Region r : w.regions) if (k.equals(r.kingdom)) r.unrestPopular.add(getRules().vesselOfFaithSetRelUnrestMod);
			}
		}
		if (max != religion) {
			for (String k : w.getNationNames()) {
				Ideology si = Nation.getStateReligion(k, w);
				if (max.religion == si.religion) w.score(k, Nation.ScoreProfile.RELIGION, getRules().scoreReligionPerConverted);
				if (religion.religion == si.religion) w.score(k, Nation.ScoreProfile.RELIGION, -getRules().scoreReligionPerConverted);
				if (max == si) w.score(k, Nation.ScoreProfile.IDEOLOGY, getRules().scoreIdeologyPerConverted);
				if (religion == si) w.score(k, Nation.ScoreProfile.IDEOLOGY, -getRules().scoreIdeologyPerConverted);
			}
		}
		religion = max;
	}

	public String getKingdom() {
		return kingdom;
	}

	public void setKingdomNoScore(String kingdom) {
		this.kingdom = kingdom;
	}

	public void setKingdom(World w, String kingdom) {
		w.score(kingdom, Nation.ScoreProfile.TERRITORY, getRules().scorePerConqueredTerritory);
		w.score(this.kingdom, Nation.ScoreProfile.TERRITORY, -getRules().scorePerConqueredTerritory);
		this.kingdom = kingdom;
	}

	public Set<Region> getNeighbors(World w) {
		int id = w.regions.indexOf(this);
		Set<Region> neighbors = new HashSet<>();
		for (Geography.Border b : w.getGeography().borders) {
			if (b.b == null) continue;
			if (b.a == id) neighbors.add(w.regions.get(b.b));
			else if (b.b == id) neighbors.add(w.regions.get(b.a));
		}
		return neighbors;
	}

	public boolean isCoastal(World w) {
		if (isSea()) return false;
		for (Region r : getNeighbors(w)) {
			if (r.isSea()) return true;
		}
		return false;
	}

	public double calcUnrest(GoodwillProvider w) {
		return Math.min(1.0, Math.max(getUnrestPopular(), Math.max(calcUnrestClerical(w), calcUnrestNoble())));
	}

	public double getUnrestPopular(){
		return unrestPopular.get();
	}

	public double calcUnrestClerical(GoodwillProvider w) {
		return Math.min(1, Math.max(0, religion.religion == Religion.IRUHAN && religion != Ideology.VESSEL_OF_FAITH ?
				-w.getGoodwill(kingdom) * getRules().clericalUnrestGoodwillFactor : 0.0));
	}

	// TODO: Some or all of the condition checking into Noble?
	public double calcUnrestNoble() {
		return hasNoble() ? noble.unrest.get() : 0.0;
	}

	// TODO: Enfore min/max, add testing
	public double calcMinConquestStrength(World w) {
		double base = calcBaseConquestStrength(w);
		double mods = 1;
		if (w.getNation(kingdom).hasTag(Nation.Tag.STOIC)) mods += getRules().stoicConqStrengthMod;
		mods += calcFortificationMod();
		return Math.max(0, base * mods);
	}

	// TODO: This is a game rule/equation
	public double calcBaseConquestStrength(GoodwillProvider w) {
		return Math.sqrt(population) * 6 / 100 * (1 - calcUnrest(w) / 2);
	}

	// TODO: This is a game rule/equation
	public double calcMinPatrolStrength(World w) {
		double mods = 1;
		if (w.getNation(kingdom).hasTag(Nation.Tag.DISCIPLINED)) mods += getRules().disciplinedPatrolStrengthMod;
		mods += (2 * calcUnrest(w) - 0.7);
		return 0.03 * Math.sqrt(population) * mods;
	}

	public double calcFortificationPct() {
		double fort = 1;
		for (Construction c : constructions) if (c.type == Construction.Type.FORTIFICATIONS) fort += getRules().perFortMod;
		return Math.min(getRules().maxFortMod, fort);
	}

	public double calcFortificationMod() {
		return calcFortificationPct() - 1;
	}

	public boolean isLand() {
		return type == Type.LAND;
	}

	public boolean isSea() {
		return type == Type.WATER;
	}

	public boolean hasNoble() {
		return noble != null && !"".equals(noble.name);
	}

	/**
	 * Returns a set of region ids that are within {@code limit} edges of this region.
	 */
	Set<Integer> getCloseRegionIds(World w, int limit) {
		Set<Integer> closeRegions = new HashSet<>();
		class Node {
			final int r;
			final int dist;
			Node(int r, int dist) { this.r = r; this.dist = dist; }
		}
		PriorityQueue<Node> queue = new PriorityQueue<>(100, Comparator.comparingInt(n -> n.dist));
		queue.add(new Node(w.regions.indexOf(this), 0));
		while (!queue.isEmpty()) {
			Node n = queue.poll();
			if (closeRegions.contains(n.r)) continue;
			closeRegions.add(n.r);
			for (Geography.Border b : w.getGeography().borders) {
				if (b.b != null && b.a == n.r && b.w + n.dist < limit) {
					queue.add(new Node(b.b, b.w + n.dist));
				} else if (b.b != null && b.b == n.r && b.w + n.dist < limit) {
					queue.add(new Node(b.a, b.w + n.dist));
				}
			}
		}
		return closeRegions;
	}

	void eat(double targetRations, String coreRegionOf, World w) {
		if (isSea()) return;
		double actualEat = Math.min(food, targetRations * population);
		food -= actualEat;
		double actualRations = actualEat / population;
		w.score(getKingdom(), Nation.ScoreProfile.PROSPERITY, actualEat * getRules().foodFedPointFactor);
		if (actualRations > 1) w.score(getKingdom(), Nation.ScoreProfile.PROSPERITY, (actualEat - population) * getRules().foodFedPlentifulPointFactor);
		if (religion != Ideology.ALYRJA || actualRations < 0.75 || actualRations > 1) {
			unrestPopular.add(Math.min(0.35, 1.0 - actualRations));
		}
		if (actualRations < 0.75) {
			double dead = (0.75 - actualRations) * 0.1 * population;
			population -= dead;
			w.score(coreRegionOf, Nation.ScoreProfile.PROSPERITY, -1 / 6000.0 * dead);
			w.notifyPlayer(getKingdom(), "Starvation", dead + " have starved to death in " + name + ".");
		}
	}

	void plant(boolean isHarvestTurn) {
		if (religion == Ideology.CHALICE_OF_COMPASSION) crops += population * getRules().chaliceOfCompassionPlantPerCitizen;
		double mod = 1;
		if (hasNoble()) mod += noble.calcPlantMod();
		if (isHarvestTurn) {
			crops += population * getRules().plantsPerCitizen * mod;
		}
	}

	void harvest(Set<String> stoicNations, GoodwillProvider goodwills) {
		if (!isLand()) return;
		double maxHarvest = population * getRules().harvestPerCitizen;
		double unrest = calcUnrest(goodwills);
		if (unrest > .25 && !stoicNations.contains(getKingdom())) maxHarvest *= 1.25 - unrest;
		maxHarvest = Math.min(crops, maxHarvest);
		food += maxHarvest;
		crops = 0;
	}

	void harvestEarly(Set<String> stoicNations, GoodwillProvider goodwills) {
		if (!isLand()) return;
		double maxHarvest = population * getRules().harvestPerCitizen;
		double unrest = calcUnrest(goodwills);
		if (unrest > .25 && !stoicNations.contains(getKingdom())) maxHarvest *= 1.25 - unrest;
		maxHarvest = Math.min(crops * 0.35, maxHarvest);
		food += maxHarvest * .5;
		crops -= maxHarvest;
	}


	void cultAccess(Collection<Nation> nations, boolean isCriticalCultRegion) {
		if (cultAccessed || !isLand()) return;
		cultAccessed = true;
		for (Nation n : nations) n.score(Nation.ScoreProfile.CULTIST, getRules().scoreCultistRegionAccess);
		if (isCriticalCultRegion) for (Nation n : nations) n.score(Nation.ScoreProfile.CULTIST, getRules().scoreCultistCriticalRegionAccess);
	}

	boolean hasBeenCultAccessed() {
		return cultAccessed;
	}

	void pinFood() {
		foodPinned = true;
	}

	boolean getFoodPinned() {
		return foodPinned;
	}

	private Region(Rules rules) {
		super(rules);
	}

	static Region newRegion(Rules rules) {
		return new Region(rules);
	}
}

class Construction {
	enum Type {
		@SerializedName("fortifications") FORTIFICATIONS,
		@SerializedName("temple") TEMPLE,
		@SerializedName("shipyard") SHIPYARD;
	}
	Type type;
	Ideology religion; // Only for temples.
	double originalCost;

	static Construction makeTemple(Ideology religion, double cost) {
		Construction c = new Construction();
		c.type = Type.TEMPLE;
		c.religion = religion;
		c.originalCost = cost;
		return c;
	}

	static Construction makeFortifications(double cost) {
		Construction c = new Construction();
		c.type = Type.FORTIFICATIONS;
		c.originalCost = cost;
		return c;
	}

	static Construction makeShipyard(double cost) {
		Construction c = new Construction();
		c.type = Type.SHIPYARD;
		c.originalCost = cost;
		return c;
	}

	/** A no-args constructor is needed for GSON. */
	private Construction() {}
}
