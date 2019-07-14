package com.openrsc.server.plugins.skills;

import com.openrsc.server.Constants;
import com.openrsc.server.Server;
import com.openrsc.server.event.MiniEvent;
import com.openrsc.server.event.ShortEvent;
import com.openrsc.server.event.SingleEvent;
import com.openrsc.server.event.custom.BatchEvent;
import com.openrsc.server.external.EntityHandler;
import com.openrsc.server.external.ItemCraftingDef;
import com.openrsc.server.external.ItemGemDef;
import com.openrsc.server.external.ItemId;
import com.openrsc.server.model.MenuOptionListener;
import com.openrsc.server.model.Skills.SKILLS;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.GameObject;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.world.World;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.plugins.listeners.action.InvUseOnItemListener;
import com.openrsc.server.plugins.listeners.action.InvUseOnObjectListener;
import com.openrsc.server.plugins.listeners.executive.InvUseOnItemExecutiveListener;
import com.openrsc.server.plugins.listeners.executive.InvUseOnObjectExecutiveListener;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.Formulae;

import java.util.concurrent.atomic.AtomicReference;

import static com.openrsc.server.plugins.Functions.getCurrentLevel;
import static com.openrsc.server.plugins.Functions.message;
import static com.openrsc.server.plugins.Functions.showBubble;

public class Crafting implements InvUseOnItemListener,
	InvUseOnItemExecutiveListener, InvUseOnObjectListener,
	InvUseOnObjectExecutiveListener {

	/**
	 * World instance
	 */
	public static final World world = World.getWorld();

	@Override
	public void onInvUseOnObject(GameObject obj, final Item item, Player owner) {
		switch (obj.getID()) {
			case 118:
			case 813: // Furnace
				if (item.getID() == ItemId.GOLD_BAR.id() || item.getID() == ItemId.GOLD_BAR_FAMILYCREST.id()) {
					Server.getServer().getEventHandler().add(new MiniEvent(owner, "Craft Jewelry") {
						public void action() {
							owner.message("What would you like to make?");
							String[] options = new String[]{
								"Ring",
								"Necklace",
								"Amulet"
							};
							owner.setMenuHandler(new MenuOptionListener(options) {
								public void handleReply(int option, String reply) {
									if (owner.isBusy() || option < 0 || option > 2) {
										return;
									}
									final int[] moulds = {
										ItemId.RING_MOULD.id(),
										ItemId.NECKLACE_MOULD.id(),
										ItemId.AMULET_MOULD.id(),
									};
									final int[] gems = {
										-1,
										ItemId.SAPPHIRE.id(),
										ItemId.EMERALD.id(),
										ItemId.RUBY.id(),
										ItemId.DIAMOND.id(),
										ItemId.DRAGONSTONE.id()
									};
									String[] options = {
										"Gold",
										"Sapphire",
										"Emerald",
										"Ruby",
										"Diamond"
									};
									if (Constants.GameServer.MEMBER_WORLD) {
										options = new String[]{
											"Gold",
											"Sapphire",
											"Emerald",
											"Ruby",
											"Diamond",
											"Dragonstone"
										};
									}
									final int craftType = option;
									if (owner.getInventory().countId(moulds[craftType]) < 1) {
										owner.message("You need a " + EntityHandler.getItemDef(moulds[craftType]).getName() + " to make a " + reply);
										return;
									}
									owner.message("What type of " + reply + " would you like to make?");
									owner.setMenuHandler(new MenuOptionListener(options) {
										public void handleReply(final int option, final String reply) {
											owner.setBatchEvent(new BatchEvent(owner, 1200, "Craft Jewelry", Formulae.getRepeatTimes(owner, SKILLS.CRAFTING.id()), false) {

												public void action() {
													if (option < 0 || option > (Constants.GameServer.MEMBER_WORLD ? 5 : 4)) {
														owner.checkAndInterruptBatchEvent();
														return;
													}
													if (Constants.GameServer.WANT_FATIGUE) {
														if (owner.getFatigue() >= owner.MAX_FATIGUE) {
															owner.message("You are too tired to craft");
															interrupt();
															return;
														}
													}
													if (option != 0 && owner.getInventory().countId(gems[option]) < 1) {
														owner.message("You don't have a " + reply + ".");
														owner.checkAndInterruptBatchEvent();
														return;
													}
													ItemCraftingDef def = EntityHandler.getCraftingDef((option * 3) + craftType);
													if (def == null) {
														owner.message("Nothing interesting happens");
														owner.checkAndInterruptBatchEvent();
														return;
													}
													if (owner.getSkills().getLevel(SKILLS.CRAFTING.id()) < def.getReqLevel()) {
														owner.message("You need a crafting skill of level " + def.getReqLevel() + " to make this");
														owner.checkAndInterruptBatchEvent();
														return;
													}
													if (owner.getInventory().remove(item) > -1 && (option == 0 || owner.getInventory().remove(gems[option], 1) > -1)) {
														showBubble(owner, item);
														Item result;
														if (item.getID() == ItemId.GOLD_BAR_FAMILYCREST.id() && option == 3 && craftType == 0) {
															result = new Item(ItemId.RUBY_RING_FAMILYCREST.id(), 1);
														} else if (item.getID() == ItemId.GOLD_BAR_FAMILYCREST.id() && option == 3 && craftType == 1) {
															result = new Item(ItemId.RUBY_NECKLACE_FAMILYCREST.id(), 1);
														} else {
															result = new Item(def.getItemID(), 1);
														}
														owner.message("You make a " + result.getDef().getName());
														owner.getInventory().add(result);
														owner.incExp(SKILLS.CRAFTING.id(), def.getExp(), true);
													} else {
														owner.message("You don't have a " + reply + ".");
														owner.checkAndInterruptBatchEvent();
													}
												}
											});
										}
									});
									ActionSender.sendMenu(owner, options);
								}
							});
							ActionSender.sendMenu(owner, options);
						}
					});
					return;
				}
				if (item.getID() == ItemId.SILVER_BAR.id()) {
					Server.getServer().getEventHandler().add(new MiniEvent(owner, "Craft Silver") {
						public void action() {
							owner.message("What would you like to make?");
							String[] options = new String[]{
								"Holy Symbol of Saradomin",
								"Unholy symbol of Zamorak"
							};
							owner.setMenuHandler(new MenuOptionListener(options) {
								public void handleReply(final int option, String reply) {
									if (owner.isBusy() || option < 0 || option > 1) {
										return;
									}
									int[] moulds = {
										ItemId.HOLY_SYMBOL_MOULD.id(),
										ItemId.UNHOLY_SYMBOL_MOULD.id(),
									};
									final int[] results = {
										ItemId.UNSTRUNG_HOLY_SYMBOL_OF_SARADOMIN.id(),
										ItemId.UNSTRUNG_UNHOLY_SYMBOL_OF_ZAMORAK.id()
									};
									if (owner.getInventory().countId(moulds[option]) < 1) {
										owner.message("You need a " + EntityHandler.getItemDef(moulds[option]).getName() + " to make a " + reply + "!");
										return;
									}

									owner.setBatchEvent(new BatchEvent(owner, 1200, "Craft Silver", Formulae.getRepeatTimes(owner, SKILLS.CRAFTING.id()), false) {

										@Override
										public void action() {
											if (owner.getSkills().getLevel(SKILLS.CRAFTING.id()) < 16) {
												owner.message("You need a crafting skill of level 16 to make this");
												interrupt();
												return;
											}
											if (owner.getInventory().remove(item) > -1) {
												showBubble(owner, item);
												Item result = new Item(results[option]);
												owner.message("You make a " + result.getDef().getName());
												owner.getInventory().add(result);
												owner.incExp(SKILLS.CRAFTING.id(), 200, true);
											} else {
												interrupt();
											}
										}
									});
								}
							});
							ActionSender.sendMenu(owner, options);
						}
					});
					return;
				} else if (item.getID() == ItemId.SODA_ASH.id() || item.getID() == ItemId.SAND.id()) { // Soda Ash or Sand (Glass)
					if (owner.getInventory().countId(ItemId.SODA_ASH.id()) < 1) {
						owner.message("You need some soda ash to make glass");
						return;
					} else if (owner.getInventory().countId(ItemId.SAND.id()) < 1) {
						owner.message("You need some sand to make glass");
						return;
					}
					owner.setBusy(true);
					showBubble(owner, item);
					int otherItem = item.getID() == ItemId.SAND.id() ? ItemId.SODA_ASH.id() : ItemId.SAND.id();
					owner.message("you heat the sand and soda ash in the furnace to make glass");
					Server.getServer().getEventHandler().add(new ShortEvent(owner, "Craft Glass") {
						public void action() {
							if (owner.getInventory().remove(otherItem, 1) > -1
								&& owner.getInventory().remove(item) > -1) {
								owner.getInventory().add(new Item(ItemId.MOLTEN_GLASS.id(), 1));
								owner.getInventory().add(new Item(ItemId.BUCKET.id(), 1));
								owner.incExp(SKILLS.CRAFTING.id(), 80, true);
							}
							owner.setBusy(false);
						}
					});
					return;
				}
				break;
			case 179: // Potters Wheel & Soft Clay
				if (item.getID() != ItemId.SOFT_CLAY.id()) {
					owner.message("Nothing interesting happens");
					return;
				}
				owner.message("What would you like to make?");
				String[] options = new String[]{"Pie dish", "Pot", "Bowl"};
				owner.setMenuHandler(new MenuOptionListener(options) {
					public void handleReply(int option, String reply) {
						if (owner.isBusy()) {
							return;
						}
						int reqLvl, exp;
						Item result;
						AtomicReference<String> msg = new AtomicReference<String>();
						switch (option) {
							case 1:
								result = new Item(ItemId.UNFIRED_POT.id(), 1);
								reqLvl = 1;
								exp = 25;
								// should not use this, as pot is made at level 1
								msg.set("a pot");
								break;
							case 0:
								result = new Item(ItemId.UNFIRED_PIE_DISH.id(), 1);
								reqLvl = 4;
								exp = 60;
								msg.set("pie dishes");
								break;
							case 2:
								result = new Item(ItemId.UNFIRED_BOWL.id(), 1);
								reqLvl = 7;
								exp = 40;
								msg.set("a bowl");
								break;
							default:
								owner.message("Nothing interesting happens");
								return;
						}
						owner.setBatchEvent(new BatchEvent(owner, 600, "Craft Clay", Formulae.getRepeatTimes(owner, SKILLS.CRAFTING.id()), false) {

							@Override
							public void action() {
								if (owner.getSkills().getLevel(SKILLS.CRAFTING.id()) < reqLvl) {
									owner.message("You need to have a crafting of level " + reqLvl + " or higher to make " + msg.get());
									interrupt();
									return;
								}
								if (owner.getInventory().remove(item) > -1) {
									showBubble(owner, item);
									owner.message("you make the clay into a " + potteryItemName(result.getDef().getName()));
									owner.getInventory().add(result);
									owner.incExp(SKILLS.CRAFTING.id(), exp, true);
								} else {
									interrupt();
								}
							}
						});
					}
				});
				ActionSender.sendMenu(owner, options);
				break;

			case 178: // Potters Oven & Unfired Clay
				int reqLvl, xp;
				Item result;
				String msg = "";
				switch (ItemId.getById(item.getID())) {
					case UNFIRED_POT:
						result = new Item(ItemId.POT.id(), 1);
						reqLvl = 1;
						xp = 25;
						// should not use this, as pot is made at level 1
						msg = "a pot";
						break;
					case UNFIRED_PIE_DISH:
						result = new Item(ItemId.PIE_DISH.id(), 1);
						reqLvl = 4;
						xp = 40;
						msg = "pie dishes";
						break;
					case UNFIRED_BOWL:
						result = new Item(ItemId.BOWL.id(), 1);
						reqLvl = 7;
						xp = 60;
						msg = "a bowl";
						break;
					default:
						owner.message("Nothing interesting happens");
						return;
				}
				if (owner.getSkills().getLevel(SKILLS.CRAFTING.id()) < reqLvl) {
					owner.message("You need to have a crafting of level " + reqLvl + " or higher to make " + msg);
					return;
				}
				final int exp = xp;
				final boolean fail = Formulae.crackPot(reqLvl, owner.getSkills().getLevel(SKILLS.CRAFTING.id()));
				showBubble(owner, item);
				String potteryItem = potteryItemName(item.getDef().getName());
				owner.message("You put the " + potteryItem + " in the oven");
				owner.setBatchEvent(new BatchEvent(owner, 1800, "Craft Clay", Formulae.getRepeatTimes(owner, SKILLS.CRAFTING.id()), false) {

					@Override
					public void action() {
						if (Constants.GameServer.WANT_FATIGUE) {
							if (owner.getFatigue() >= owner.MAX_FATIGUE) {
								owner.message("You are too tired to craft");
								interrupt();
								return;
							}
						}
						showBubble(owner, item);
						if (owner.getInventory().remove(item) > -1) {
							if (fail) {
								owner.message("The " // TODO: Check if is authentic message
									+ potteryItem + " cracks in the oven, you throw it away.");
							} else {
								owner.message("the "
									+ potteryItem + " hardens in the oven");
								Server.getServer().getEventHandler().add(new SingleEvent(owner, 1800, "Remove Clay From Oven") {
									@Override
									public void action() {
										owner.message("You remove a "
												+ result.getDef().getName().toLowerCase()
												+ " from the oven");
										owner.getInventory().add(result);
										owner.incExp(SKILLS.CRAFTING.id(), exp, true);
									}
									
								});
							}
						} else {
							interrupt();
						}
					}
				});
				break;
		}
	}

	@Override
	public void onInvUseOnItem(Player player, Item item1, Item item2) {
		if (item1.getID() == ItemId.CHISEL.id() && doCutGem(player, item1, item2)) {
			return;
		} else if (item2.getID() == ItemId.CHISEL.id() && doCutGem(player, item2, item1)) {
			return;
		} else if (item1.getID() == ItemId.GLASSBLOWING_PIPE.id() && doGlassBlowing(player, item1, item2)) {
			return;
		} else if (item2.getID() == ItemId.GLASSBLOWING_PIPE.id() && doGlassBlowing(player, item2, item1)) {
			return;
		}
		if (item1.getID() == ItemId.NEEDLE.id() && makeLeather(player, item1, item2)) {
			return;
		} else if (item2.getID() == ItemId.NEEDLE.id() && makeLeather(player, item2, item1)) {
			return;
		} else if (item1.getID() == ItemId.BALL_OF_WOOL.id() && useWool(player, item1, item2)) {
			return;
		} else if (item2.getID() == ItemId.BALL_OF_WOOL.id() && useWool(player, item2, item1)) {
			return;
		} else if ((item1.getID() == ItemId.BUCKET_OF_WATER.id() || item1.getID() == ItemId.JUG_OF_WATER.id() || item1.getID() == ItemId.BOWL_OF_WATER.id()) && useWater(player, item1, item2)) {
			return;
		} else if ((item2.getID() == ItemId.BUCKET_OF_WATER.id() || item2.getID() == ItemId.JUG_OF_WATER.id() || item2.getID() == ItemId.BOWL_OF_WATER.id()) && useWater(player, item2, item1)) {
			return;
		} else if (item1.getID() == ItemId.MOLTEN_GLASS.id() && item2.getID() == ItemId.LENS_MOULD.id() || item1.getID() == ItemId.LENS_MOULD.id() && item2.getID() == ItemId.MOLTEN_GLASS.id()) {
			if (getCurrentLevel(player, SKILLS.CRAFTING.id()) < 10) {
				player.message("You need a crafting level of 10 to make the lens");
				return;
			}
			if (player.getInventory().remove(new Item(ItemId.MOLTEN_GLASS.id())) > -1) {
				player.message("You pour the molten glass into the mould");
				player.message("And clasp it together");
				player.message("It produces a small convex glass disc");
				player.getInventory().add(new Item(ItemId.LENS.id()));
			}
			return;
		}
		player.message("Nothing interesting happens");
	}

	private boolean doCutGem(Player player, final Item chisel, final Item gem) {
		final ItemGemDef gemDef = EntityHandler.getItemGemDef(gem.getID());
		if (gemDef == null) {
			return false;
		}

		player.setBatchEvent(new BatchEvent(player, 600, "Cut Gem", Formulae.getRepeatTimes(player, SKILLS.CRAFTING.id()), false) {

			@Override
			public void action() {
				if (owner.getSkills().getLevel(SKILLS.CRAFTING.id()) < gemDef.getReqLevel()) {
					boolean pluralize = gemDef.getGemID() <= ItemId.UNCUT_DRAGONSTONE.id();
					owner.message(
						"you need a crafting level of " + gemDef.getReqLevel()
							+ " to cut " + (gem.getDef().getName().contains("ruby") ? "rubies" : gem.getDef().getName().replaceFirst("(?i)uncut ", "") + (pluralize ? "s" : "")));
					interrupt();
					return;
				}
				if (owner.getInventory().remove(gem) > -1) {
					Item cutGem = new Item(gemDef.getGemID(), 1);
					/* Jade, Opal and red topaz fail handler - 25% chance to fail **/
					int[] gemsThatFail = new int[]{
						ItemId.UNCUT_RED_TOPAZ.id(),
						ItemId.UNCUT_JADE.id(),
						ItemId.UNCUT_OPAL.id(),
					};
					if (DataConversions.inArray(gemsThatFail, gem.getID()) &&
							Formulae.smashGem(gem.getID(), gemDef.getReqLevel(), owner.getSkills().getLevel(SKILLS.CRAFTING.id()))) {
						owner.message("You miss hit the chisel and smash the " + cutGem.getDef().getName() + " to pieces!");
						owner.getInventory().add(new Item(ItemId.CRUSHED_GEMSTONE.id()));
						if (gem.getID() == ItemId.UNCUT_RED_TOPAZ.id()) {
							owner.incExp(SKILLS.CRAFTING.id(), 25, true);
						} else if (gem.getID() == ItemId.UNCUT_JADE.id()) {
							owner.incExp(SKILLS.CRAFTING.id(), 20, true);
						} else {
							owner.incExp(SKILLS.CRAFTING.id(), 15, true);
						}
					} else {
						owner.message("You cut the " + cutGem.getDef().getName().toLowerCase());
						owner.playSound("chisel");
						owner.getInventory().add(cutGem);
						owner.incExp(SKILLS.CRAFTING.id(), gemDef.getExp(), true);
					}
				} else {
					interrupt();
				}
			}
		});
		return true;
	}

	private boolean doGlassBlowing(Player player, final Item pipe, final Item glass) {
		if (glass.getID() != ItemId.MOLTEN_GLASS.id()) {
			return false;
		}
		player.message("what would you like to make?");
		Server.getServer().getEventHandler().add(new MiniEvent(player, "Craft Glass Blowing") {
			public void action() {
				String[] options = new String[]{
					"Vial",
					"orb",
					"Beer glass"
				};
				owner.setMenuHandler(new MenuOptionListener(options) {
					public void handleReply(final int option, final String reply) {
						Item result;
						int reqLvl, exp;
						String resultGen;
						switch (option) {
							case 0:
								result = new Item(ItemId.EMPTY_VIAL.id(), 1);
								reqLvl = 33;
								exp = 140;
								resultGen = "vials";
								break;
							case 1:
								result = new Item(ItemId.UNPOWERED_ORB.id(), 1);
								reqLvl = 46;
								exp = 210;
								resultGen = "orbs";
								break;
							case 2:
								result = new Item(ItemId.BEER_GLASS.id(), 1);
								reqLvl = 1;
								exp = 70;
								// should not use this, as beer glass is made at level 1
								resultGen = "beer glasses";
								break;
							default:
								return;
						}
						if (owner.getSkills().getLevel(SKILLS.CRAFTING.id()) < reqLvl) {
							owner.message(
								"You need a crafting level of " + reqLvl + " to make " + resultGen);
							return;
						}
						player.setBatchEvent(new BatchEvent(player, 600, "Craft Glass Blowing", player.getInventory().countId(glass.getID()), false) {
							@Override
							public void action() {
								if (Constants.GameServer.WANT_FATIGUE) {
									if (owner.getFatigue() >= owner.MAX_FATIGUE) {
										owner.message("You are too tired to craft");
										interrupt();
										return;
									}
								}
								if (owner.getInventory().remove(glass) > -1) {
									owner.message("You make a " + result.getDef().getName());
									owner.getInventory().add(result);
									owner.incExp(SKILLS.CRAFTING.id(), exp, true);
								}
							}
						});
					}
				});
				ActionSender.sendMenu(owner, options);
			}
		});
		return true;
	}

	private boolean makeLeather(Player player, final Item needle, final Item leather) {
		if (leather.getID() != ItemId.LEATHER.id()) {
			return false;
		}
		if (player.getInventory().countId(ItemId.THREAD.id()) < 1) {
			player.message("You need some thread to make anything out of leather");
			return true;
		}
		Server.getServer().getEventHandler().add(new MiniEvent(player, "Craft Leather") {
			public void action() {
				String[] options = new String[]{
					"Armour",
					"Gloves",
					"Boots",
					"Cancel"
				};
				owner.setMenuHandler(new MenuOptionListener(options) {
					public void handleReply(final int option, final String reply) {
						Item result;
						int reqLvl, exp;
						switch (option) {
							case 0:
								result = new Item(ItemId.LEATHER_ARMOUR.id(), 1);
								reqLvl = 14;
								exp = 100;
								break;
							case 1:
								result = new Item(ItemId.LEATHER_GLOVES.id(), 1);
								reqLvl = 1;
								exp = 55;
								break;
							case 2:
								result = new Item(ItemId.BOOTS.id(), 1);
								reqLvl = 7;
								exp = 65;
								break;
							default:
								return;
						}
						if (owner.getSkills().getLevel(SKILLS.CRAFTING.id()) < reqLvl) {
							owner.message("You need to have a crafting of level " + reqLvl + " or higher to make " + result.getDef().getName());
							return;
						}
						player.setBatchEvent(new BatchEvent(player, 600, "Craft Leather", player.getInventory().countId(leather.getID()), false) {
							@Override
							public void action() {
								if (Constants.GameServer.WANT_FATIGUE) {
									if (owner.getFatigue() >= owner.MAX_FATIGUE) {
										owner.message("You are too tired to craft");
										interrupt();
										return;
									}
								}
								if (owner.getInventory().remove(leather) > -1) {
									owner.message("You make some " + result.getDef().getName());
									owner.getInventory().add(result);
									owner.incExp(SKILLS.CRAFTING.id(), exp, true);
									//a reel of thread accounts for 5 uses
									if (!owner.getCache().hasKey("part_reel_thread")) {
										owner.getCache().set("part_reel_thread", 1);
									} else {
										int parts = owner.getCache().getInt("part_reel_thread");
										if (parts >= 4) {
											owner.message("You use up one of your reels of thread");
											owner.getInventory().remove(ItemId.THREAD.id(), 1);
											owner.getCache().remove("part_reel_thread");
										} else {
											owner.getCache().put("part_reel_thread", parts + 1);
										}
									}
								}
							}
						});
					}
				});
				ActionSender.sendMenu(owner, options);
			}
		});
		return true;
	}

	private boolean useWool(Player player, final Item woolBall, final Item item) {
		int newID;
		switch (ItemId.getById(item.getID())) {
			case UNSTRUNG_HOLY_SYMBOL_OF_SARADOMIN:
				newID = ItemId.UNBLESSED_HOLY_SYMBOL.id();
				break;
			case UNSTRUNG_UNHOLY_SYMBOL_OF_ZAMORAK:
				newID = ItemId.UNBLESSED_UNHOLY_SYMBOL_OF_ZAMORAK.id();
				break;
			case UNSTRUNG_GOLD_AMULET:
				newID = ItemId.GOLD_AMULET.id();
				break;
			case UNSTRUNG_SAPPHIRE_AMULET:
				newID = ItemId.SAPPHIRE_AMULET.id();
				break;
			case UNSTRUNG_EMERALD_AMULET:
				newID = ItemId.EMERALD_AMULET.id();
				break;
			case UNSTRUNG_RUBY_AMULET:
				newID = ItemId.RUBY_AMULET.id();
				break;
			case UNSTRUNG_DIAMOND_AMULET:
				newID = ItemId.DIAMOND_AMULET.id();
				break;
			case UNSTRUNG_DRAGONSTONE_AMULET:
				newID = ItemId.UNENCHANTED_DRAGONSTONE_AMULET.id();
				break;
			default:
				return false;
		}
		final int newId = newID;
		player.setBatchEvent(new BatchEvent(player, 600, "Craft String Amulet", 
			Formulae.getRepeatTimes(player, SKILLS.CRAFTING.id()), false) {

			@Override
			public void action() {
				if (owner.getInventory().countId(item.getID()) <= 0 || owner.getInventory().countId(ItemId.BALL_OF_WOOL.id()) <= 0) {
					interrupt();
					return;
				}
				if (owner.getInventory().remove(woolBall) > -1 && owner.getInventory().remove(item) > -1) {
					owner.message("You put some string on your " + item.getDef().getName().toLowerCase());
					owner.getInventory().add(new Item(newId, 1));
				} else {
					interrupt();
				}
			}
		});
		return true;
	}

	private boolean useWater(Player player, final Item water, final Item item) {
		int jugID = Formulae.getEmptyJug(water.getID());
		if (jugID == -1) { // This shouldn't happen
			return false;
		}
		// Clay and water is not bowl of water
		if (item.getID() == ItemId.CLAY.id() && water.getID() != ItemId.BOWL_OF_WATER.id()) {
			if (player.getInventory().remove(water) > -1
				&& player.getInventory().remove(item) > -1) {
				message(player, 1200, "You mix the clay and water");
				player.message("You now have some soft workable clay");
				player.getInventory().add(new Item(jugID, 1));
				player.getInventory().add(new Item(ItemId.SOFT_CLAY.id(), 1));
			}
		} else {
			return false;
		}
		return true;
	}
	
	private String potteryItemName(String rawName) {
		String uncapName = rawName.toLowerCase();
		if (uncapName.startsWith("unfired ")) {
			return uncapName.substring(8);
		}
		return uncapName;
	}

	@Override
	public boolean blockInvUseOnItem(Player player, Item item1, Item item2) {
		if (item1.getID() == ItemId.CHISEL.id() && doCutGem(player, item1, item2)) {
			return true;
		} else if (item2.getID() == ItemId.CHISEL.id() && doCutGem(player, item2, item1)) {
			return true;
		} else if (item1.getID() == ItemId.GLASSBLOWING_PIPE.id()) {
			return true;
		} else if (item2.getID() == ItemId.GLASSBLOWING_PIPE.id()) {
			return true;
		} else if (item1.getID() == ItemId.NEEDLE.id()) {
			return true;
		} else if (item2.getID() == ItemId.NEEDLE.id()) {
			return true;
		} else if (item1.getID() == ItemId.BALL_OF_WOOL.id()) {
			return true;
		} else if (item2.getID() == ItemId.BALL_OF_WOOL.id()) {
			return true;
		} else if ((item1.getID() == ItemId.BUCKET_OF_WATER.id() || item1.getID() == ItemId.JUG_OF_WATER.id()) && item2.getID() == ItemId.CLAY.id()) {
			return true;
		} else if ((item2.getID() == ItemId.BUCKET_OF_WATER.id() || item2.getID() == ItemId.JUG_OF_WATER.id()) && item1.getID() == ItemId.CLAY.id()) {
			return true;
		} else
			return item1.getID() == ItemId.MOLTEN_GLASS.id() && item2.getID() == ItemId.LENS_MOULD.id() || item1.getID() == ItemId.LENS_MOULD.id() && item2.getID() == ItemId.MOLTEN_GLASS.id();
	}

	@Override
	public boolean blockInvUseOnObject(GameObject obj, Item item, Player player) {
		int[] blockItemsFurnance = new int[]{
			ItemId.SILVER_BAR.id(),
			ItemId.GOLD_BAR.id(),
			ItemId.SODA_ASH.id(),
			ItemId.SAND.id(),
			ItemId.GOLD_BAR_FAMILYCREST.id(),
		};
		int[] blockItemsOven = new int[]{
			ItemId.UNFIRED_POT.id(),
			ItemId.UNFIRED_PIE_DISH.id(),
			ItemId.UNFIRED_BOWL.id()
		};
		return ((obj.getID() == 118 || obj.getID() == 813) && DataConversions.inArray(blockItemsFurnance, item.getID())) 
				|| (obj.getID() == 178 && DataConversions.inArray(blockItemsOven, item.getID()))
				|| (obj.getID() == 179 && item.getID() == ItemId.SOFT_CLAY.id());
	}
}
