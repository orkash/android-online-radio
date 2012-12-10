package org.nkuznetsov.onlineradio;

public class RadioService extends AbstractRadioService
{
	@Override
	protected String wrapStream(String stream)
	{
		return stream;
	}
}
