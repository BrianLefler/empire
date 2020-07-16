package com.empire;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.FieldNamingPolicy;
import java.util.List;
import java.util.Set;

public class NationSetup {
	public static enum Bonus {
		ARMIES,
		NAVIES,
		FOOD,
		GOLD;
	}

	public static class CharacterSetup {
		public static enum Skill {
			GOVERNOR,
			ADMIRAL,
			GENERAL,
			SPY;
		}

		public String name;
		public Skill skill;
		public int portrait;
	}

	public static String TYPE = "Nation";

	public String name = "";
	public String title;
	public Set<Nation.Tag> traits;
	public Ideology dominantIdeology;
	public String email = "";
	public Bonus bonus;
	public List<CharacterSetup> characters;
	public List<String> regionNames;
	public Set<Nation.ScoreProfile> profiles;

	private static Gson getGson() {
		return new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
	}

	public static NationSetup fromJson(String json) {
		NationSetup s = getGson().fromJson(json, NationSetup.class);
		if (s.profiles.contains(Nation.ScoreProfile.CULTIST)) throw new IllegalArgumentException("Cannot setup with cultist profile.");
		if (s.traits.size() != 2) throw new IllegalArgumentException("Must have two traits.");
		if (s.title.equals("")) throw new IllegalArgumentException("Must have a title.");
		if (s.traits.stream().anyMatch(e -> e == null)) throw new IllegalArgumentException("Cannot submit null traits.");
		if (s.characters.stream().anyMatch(e -> e.name.equals("") || e.skill == null || e.portrait < 0)) throw new IllegalArgumentException("Bad character.");
		return s;
	}

	public boolean hasTag(Nation.Tag tag) {
		return traits.contains(tag);
	}

	public boolean hasScoreProfile(Nation.ScoreProfile profile) {
		return profiles.contains(profile);
	}
}
