package org.nkuznetsov.onlineradio;

import com.actionbarsherlock.view.MenuItem;

public class RadioActivity extends AbstractRadioActivity
{
	@Override
	public OrderedHashMap<Integer, Integer> getMenuItemsOrder()
	{
		OrderedHashMap<Integer, Integer> order = new OrderedHashMap<Integer, Integer>();
		
		order.put(MENU_SHARE, MenuItem.SHOW_AS_ACTION_ALWAYS);
		order.put(MENU_STOP, MenuItem.SHOW_AS_ACTION_ALWAYS);
		order.put(MENU_RECORD, MenuItem.SHOW_AS_ACTION_NEVER);
		order.put(MENU_UPDATE, MenuItem.SHOW_AS_ACTION_NEVER);
		order.put(MENU_RATE, MenuItem.SHOW_AS_ACTION_NEVER);
		
		return order;
	}
	
	@Override
	protected Class<?> getServiceClass()
	{
		return RadioService.class;
	}
}
