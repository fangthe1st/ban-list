package com.ban-list;

import com.google.inject.Provides;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.FriendsChatMemberJoined;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.events.WidgetHiddenChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.task.Schedule;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import java.time.temporal.ChronoUnit;

@PluginDescriptor(
		name = "Ban List",
		description = "Displays warning in chat when you join a" +
				"clan chat/new member join your clan chat and he is in a Url list/Manual List",
		tags = {"banlist"},
		enabledByDefault = false
)

@Slf4j
public class BanListPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BanListConfig config;

	@Inject
	private ChatMessageManager chatMessageManager;

	private ArrayList<String> UrlArrayList = new ArrayList<>();
	private ArrayList<String> manualBans = new ArrayList<>();

	@Provides
	BanListConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BanListConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		manualBans.addAll(Text.fromCSV(Text.standardize(config.getBannedPlayers())));
		fetchFromWebsites();
	}

	@Override
	protected void shutDown() throws Exception
	{

		UrlArrayList.clear();
		manualBans.clear();
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("banlist") && event.getKey().equals("bannedPlayers"))
		{
			manualBans.clear();
			UrlArrayList.clear();
			String newValue = event.getNewValue();

			manualBans.addAll(Text.fromCSV(Text.standardize(newValue)));
			fetchFromWebsites();
		}
	}


	/**
	 * Event to keep making sure player names are highlighted red in clan chat, since the red name goes away frequently
	 */
	@Subscribe
	public void onWidgetHiddenChanged(WidgetHiddenChanged widgetHiddenChanged)
	{
		if (client.getGameState() != GameState.LOGGED_IN
				|| client.getWidget(WidgetInfo.LOGIN_CLICK_TO_PLAY_SCREEN) != null
				|| client.getViewportWidget() == null
				|| client.getWidget(WidgetInfo.FRIENDS_CHAT) == null
				|| !config.highlightInClan())
		{
			return;
		}

		clientThread.invokeLater(() ->
		{
			if (!client.getWidget(WidgetInfo.FRIENDS_CHAT).isHidden())
			{
				highlightRedInCC();
			}
		});
	}

	@Subscribe
	private void onFriendsChatMemberJoined(FriendsChatMemberJoined event)
	{
		FriendsChatMember member = event.getMember();
		String memberUsername = Text.standardize(member.getName().toLowerCase());

		ListType scamList = checkBanList(memberUsername);

		if (scamList != null)
		{
			sendWarning(memberUsername, scamList);
			if (config.highlightInClan())
			{
				highlightRedInCC();
			}
		}
	}
	/**
	 * refresh list for url
	 */
	@Schedule(period = 1, unit = ChronoUnit.MINUTES)
	public void refreshList() {
		UrlArrayList.clear();
		fetchFromWebsites();

	}
	/**
	 * If a trade window is opened and the person trading us is on the list, modify "trading with"
	 */
	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		if (config.highlightInTrade())
		{
			if (widgetLoaded.getGroupId() == 335)
			{ //if trading window was loaded
				clientThread.invokeLater(() ->
				{
					Widget tradingWith = client.getWidget(335, 31);
					String name = tradingWith.getText().replaceAll("Trading With: ", "");
					ListType tradeList = checkBanList(name);
					if (checkBanList(name) != null)
					{
						sendWarning(name, tradeList);
						tradingWith.setText(tradingWith.getText().replaceAll(name, "<col=ff0000>" + name + " (on banlist)" + "</col>"));
					}
				});
			}
		}
	}

	/**
	 * Compares player name to everything in the ban lists
	 */
	private ListType checkBanList(String nameToBeChecked)
	{


		if (UrlArrayList.size() > 0 && config.enableUrl())
		{
			if (UrlArrayList.stream().anyMatch(nameToBeChecked::equalsIgnoreCase))
			{
				return ListType.Url_LIST;
			}
		}

		if (manualBans.size() > 0)
		{
			if (manualBans.stream().anyMatch(nameToBeChecked::equalsIgnoreCase))
			{
				return ListType.MANUAL_LIST;
			}
		}

		return null;
	}

	/**
	 * Sends a warning to our player, notifying them that a player is on a ban list
	 */
	private void sendWarning(String playerName, ListType listType)
	{
		switch (listType)
		{

			case Url_LIST:
				final String url_message = new ChatMessageBuilder()
						.append(ChatColorType.HIGHLIGHT)
						.append("Warning! " + playerName + " is on the Url\'s  list!")
						.build();

				chatMessageManager.queue(
						QueuedMessage.builder()
								.type(ChatMessageType.CONSOLE)
								.runeLiteFormattedMessage(url_message)
								.build());
				break;
			case MANUAL_LIST:
				final String manual_message = new ChatMessageBuilder()
						.append(ChatColorType.HIGHLIGHT)
						.append("Warning! " + playerName + " is on your manual scammer list!")
						.build();

				chatMessageManager.queue(
						QueuedMessage.builder()
								.type(ChatMessageType.CONSOLE)
								.runeLiteFormattedMessage(manual_message)
								.build());
				break;
		}
	}

	/**
	 * Pulls data from wdr and runewatch to build a list of blacklisted usernames
	 */
	private void fetchFromWebsites()
	{



		Request Request = new Request.Builder()
				.url(config.getBannedUrl())
				.build();
		RuneLiteAPI.CLIENT.newCall(Request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("error retrieving names from Url");
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				String text = response.body().string();
				text = text.replace("\n", ",");

				ArrayList<String> urlList = new ArrayList<>(Arrays.asList(text.split(",")));
				ArrayList<String>urlList2 = new ArrayList<>();
				urlList.forEach((name) -> urlList2.add(Text.standardize(name)));

				UrlArrayList.addAll(urlList2);
			}


		});


	}

	/**
	 * Iterates through the clan chat list widget and checks if a string (name) is on any of the ban lists to highlight them red.
	 */
	private void highlightRedInCC()
	{
		clientThread.invokeLater(() ->
		{
			Widget widget = client.getWidget(WidgetInfo.FRIENDS_CHAT_LIST);
			for (Widget widgetChild : widget.getDynamicChildren())
			{
				ListType listType = checkBanList(widgetChild.getText());

				if (listType != null)
				{
					widgetChild.setText("<col=ff0000>" + widgetChild.getText() + "</col>");
				}
			}
		});
	}
}
