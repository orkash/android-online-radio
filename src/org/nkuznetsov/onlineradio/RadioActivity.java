package org.nkuznetsov.onlineradio;

public class RadioActivity extends AbstractRadioActivity
{
	@Override
	protected Class<?> getServiceClass()
	{
		return RadioService.class;
	}
}
