package com.empire;

//import com.google.appengine.api.datastore.Text;
//import com.google.appengine.api.datastore.DatastoreService;
//import com.google.appengine.api.datastore.DatastoreServiceFactory;
//import com.google.appengine.api.datastore.EmbeddedEntity;
//import com.google.appengine.api.datastore.Entity;
//import com.google.appengine.api.datastore.EntityNotFoundException;
//import com.google.appengine.api.datastore.Key;
//import com.google.appengine.api.datastore.KeyFactory;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.FieldNamingPolicy;
//import java.util.HashMap;
//
//final class Order {
//	public static String TYPE = "Order";
//
//	public final String kingdom;
//	public final int turn;
//	public final int version;
//	public final String json;
//	public final long gameId;
//
//	public static Order loadOrder(long gameId, String kingdom, int turn, DatastoreService service) throws EntityNotFoundException {
//		Entity e = service.get(KeyFactory.createKey(TYPE, gameId + "_" + turn + "_" + kingdom));
//		return new Order(gameId, kingdom, turn, (int)((Long)e.getProperty("version")).longValue(), ((Text)e.getProperty("json")).getValue());
//	}
//
//	public Order(long gameId, String kingdom, int turn, int version, String json) {
//		this.gameId = gameId;
//		this.kingdom = kingdom;
//		this.turn = turn;
//		this.version = version;
//		this.json = json;
//	}
//
//	public HashMap<String, String> getOrders() {
//		return
//				new GsonBuilder()
//						.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
//						.create()
//				.fromJson(json, OrderGson.class).orders;
//	}
//
//	public Entity toEntity() {
//		Entity e = new Entity(TYPE, gameId + "_" + turn + "_" + kingdom);
//		e.setProperty("json", new Text(json));
//		e.setProperty("version", version);
//		return e;
//	}
//
//	private static class OrderGson {
//		HashMap<String, String> orders = new HashMap<>();
//	}
//}

import java.util.Map;
import java.util.Objects;

public class Orders {
	public final long gameId;
	public final String kingdom;
	public final int turn;
	public final Map<String, String> orders;
	public final int version;

	public Orders(long gameId, String kingdom, int turn, Map<String, String> orders, int version) {
		this.gameId = gameId;
		this.kingdom = kingdom;
		this.turn = turn;
		this.orders = orders;
		this.version = version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Orders orders1 = (Orders) o;
		return gameId == orders1.gameId &&
				turn == orders1.turn &&
				version == orders1.version &&
				Objects.equals(kingdom, orders1.kingdom) &&
				Objects.equals(orders, orders1.orders);
	}

	@Override
	public int hashCode() {
		return Objects.hash(gameId, kingdom, turn, orders, version);
	}
}
