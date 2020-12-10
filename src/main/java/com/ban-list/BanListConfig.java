package com.ban-list;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("banlist")

public interface BanListConfig extends Config
{

	@ConfigItem(
			keyName = "bannedPlayers",
			name = "Manual Scammer List",
			description = "Players you add to this list will be shown when you join a clan.",
			position = 0

	)
	default String getBannedPlayers()
	{
		return "";
	}

	@ConfigItem(
			keyName = "bannedPlayers",
			name = "",
			description = ""
	)
	void setBannedPlayers(String key);

	@ConfigItem(
			keyName = "bannedUrl",
			name = "Url List",
			description = "Url you add to this list will be shown when you join a clan.",
			position = 1

	)
	default String getBannedUrl()
	{
		return "";
	}

	@ConfigItem(
			keyName = "bannedUrl",
			name = "",
			description = ""
	)
	void setbannedUrl(String key);

	@ConfigItem(
			position = 2,
			keyName = "enableUrl",
			name = "Enable Url Scammer List",
			description = "Incorporate Url Scammer list"
	)
	default boolean enableUrl()
	{
		return true;
	}

	@ConfigItem(
			position = 3,
			keyName = "highlightInClan",
			name = "Highlight red in Clan Chat",
			description = "Highlights Scammer\'s name in your current clan chat."
	)
	default boolean highlightInClan()
	{
		return true;
	}

	@ConfigItem(
			position = 4,
			keyName = "highlightInTrade",
			name = "Highlight red in trade screen",
			description = "Highlights Scammer\'s name in your trade window"
	)
	default boolean highlightInTrade()
	{
		return true;
	}

}
