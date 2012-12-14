package org.nkuznetsov.onlineradio;

import java.util.HashMap;
import java.util.Vector;

public class OrderedHashMap<K, V> extends HashMap<K, V>
{
	private static final long serialVersionUID = 7161877868202070915L;
	
	private Vector<K> order = new Vector<K>();
	
	@Override
	public V put(K key, V value)
	{
		order.add(key);
		return super.put(key, value);
	}
	
	@Override
	public V get(Object key)
	{
		V value = super.get(key);
		if (value == null) order.remove(key);
		return value;
	}
	
	/**
	 * Returns a vector of the keys contained in this map. Keys sorted in adding order. 
	 * @return map keys vector in adding order.
	 */
	public Vector<K> keyOrder()
	{
		return new Vector<K>(order);
	}
	
	public Vector<V> valueOrder()
	{
		Vector<V> values = new Vector<V>();
		
		for (K k : order) values.add(get(k));
		
		return values;
	}
	
	@Override
	public V remove(Object key)
	{
		order.remove(key);
		return super.remove(key);
	}
	
	@Override
	public void clear()
	{
		order.clear();
		super.clear();
	}
}
