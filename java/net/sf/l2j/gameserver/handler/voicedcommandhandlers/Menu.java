package net.sf.l2j.gameserver.handler.voicedcommandhandlers;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.manager.FestivalOfDarknessManager;
import net.sf.l2j.gameserver.data.sql.OfflineTradersTable;
import net.sf.l2j.gameserver.handler.IVoicedCommandHandler;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.trade.TradeList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.taskmanager.AttackStanceTaskManager;
import net.sf.l2j.util.CustomMessage;

public class Menu implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"menu",
		"mod_menu_"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (command.equals("menu") && Config.ENABLE_MENU)
			showHtm(player);
		else if (command.startsWith("mod_menu_"))
		{
			String addcmd = command.substring(9).trim();
			if (addcmd.startsWith("exp"))
			{
				if (player.getStatus().getLevel() < Config.NEWBIE_MAX_LEVEL && player.isStopExp())
				{
					player.sendMessage(new CustomMessage("LOW_LVL"));
					showHtm(player);
					return false;
				}

				if (player.isStopExp())
				{
					player.setStopExp(false);
					player.sendMessage(new CustomMessage("EXP_OFF"));
				}
				else
				{
					player.setStopExp(true);
					player.sendMessage(new CustomMessage("EXP_ON"));
				}
				showHtm(player);
				return true;
			}
			else if (addcmd.startsWith("trade"))
			{
				if (player.isTradeRefusal())
				{
					player.setTradeRefusal(false);
					player.sendMessage(new CustomMessage("TRADE_OFF"));
				}
				else
				{
					player.setTradeRefusal(true);
					player.sendMessage(new CustomMessage("TRADE_ON"));
				}
				showHtm(player);
				return true;
			}
			else if (addcmd.startsWith("autoloot"))
			{
				if (player.isAutoLoot())
				{
					player.setAutoLoot(false);
					player.sendMessage(new CustomMessage("AUTO_LOOT_OFF"));
				}
				else
				{
					player.setAutoLoot(true);
					player.sendMessage(new CustomMessage("AUTO_LOOT_ON"));
				}
				
				showHtm(player);
				return true;
			}
			else if (addcmd.startsWith("offline"))
			{
				if (player == null)
					return false;

				if ((!player.isInStoreMode() && (!player.isCrafting())) || !player.isSitting())
				{
					player.sendMessage(new CustomMessage("OFFLINE_NOT_RUNNING"));
					player.sendPacket(ActionFailed.STATIC_PACKET);
					showHtm(player);
					return false;
				}

				if(player.getStatus().getLevel() < Config.OFFLINE_LEVEL)
				{
					player.sendMessage(new CustomMessage("OFFLINE_LOW_LEVEL"));
					player.sendPacket(ActionFailed.STATIC_PACKET);
					showHtm(player);
					return false;
				}

				final TradeList storeListBuy = player.getBuyList();
				if (storeListBuy == null)
				{
					player.sendMessage(new CustomMessage("OFFLINE_BUY_LIST_EMPTY"));
					player.sendPacket(ActionFailed.STATIC_PACKET);
					showHtm(player);
					return false;
				}

				final TradeList storeListSell = player.getSellList();
				if (storeListSell == null)
				{
					player.sendMessage(new CustomMessage("OFFLINE_SELL_LIST_EMPTY"));
					player.sendPacket(ActionFailed.STATIC_PACKET);
					showHtm(player);
					return false;
				}

				player.getInventory().updateDatabase();

				if (AttackStanceTaskManager.getInstance().isInAttackStance(player))
				{
					player.sendPacket(SystemMessageId.CANT_OPERATE_PRIVATE_STORE_DURING_COMBAT);
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}

				// Dont allow leaving if player is in combat
				if (player.isInCombat() && !player.isGM())
				{
					player.sendMessage(new CustomMessage("OFFLINE_COMBAT_MODE"));
					player.sendPacket(ActionFailed.STATIC_PACKET);
					showHtm(player);
					return false;
				}

				// Dont allow leaving if player is teleporting
				if (player.isTeleporting() && !player.isGM())
				{
					player.sendMessage(new CustomMessage("OFFLINE_TELEPORT"));
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return false;
				}

				if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
				{
					player.sendMessage(new CustomMessage("OFFLINE_OLYMPIAD_MODE"));
					return false;
				}

				// Prevent player from logging out if they are a festival participant nd it is in progress, otherwise notify party members that the player is not longer a participant.
				if (player.isFestivalParticipant())
				{
					if (FestivalOfDarknessManager.getInstance().isFestivalInitialized())
					{
						player.sendMessage(new CustomMessage("OFFLINE_FESTIVAL"));
						return false;
					}

					Party playerParty = player.getParty();
					if (playerParty != null)
						player.getParty().broadcastToPartyMembers(player, SystemMessage.sendString(player.getName() + new CustomMessage("OFFLINE_REMOVED_FESTIVAL")));
				}

				if (!OfflineTradersTable.offlineMode(player))
				{
					player.sendMessage(new CustomMessage("OFFLINE_LOGOUT"));
					return false;
				}

				if (player.isInStoreMode() && Config.OFFLINE_TRADE_ENABLE || player.isCrafting() && Config.OFFLINE_CRAFT_ENABLE)
				{
					player.logout(false);
					return true;
				}

				OfflineTradersTable.saveOfflineTraders(player);
				return false;
			}
			else if (addcmd.startsWith("lang_"))
			{
				player.setLang(addcmd.substring(5).trim());
				showHtm(player);
				return true;
			}
		}
		return true;
	}
	
	private static void showHtm(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile(player.isLang() + "mods/menu/menu.htm");
		
		final String ACTIVED = player.isLangString().equals("en") ? "<font color=1E90FF>ON</font>" : "<font color=1E90FF>ВКЛ</font>";
		final String DESAСTIVED = player.isLangString().equals("en") ? "<font color=FF0000>OFF</font>" : "<font color=FF0000>ВЫКЛ</font>";
		
		htm.replace("%online%", World.getInstance().getPlayers().size() * Config.FAKE_ONLINE_AMOUNT);
		htm.replace("%gainexp%", player.isStopExp() ? ACTIVED : DESAСTIVED);
		htm.replace("%trade%", player.isTradeRefusal() ? ACTIVED : DESAСTIVED);
		htm.replace("%autoloot%", player.isAutoLoot() ? ACTIVED : DESAСTIVED);
		
		player.sendPacket(htm);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}