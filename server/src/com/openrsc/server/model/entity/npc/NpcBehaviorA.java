package com.openrsc.server.model.entity.npc;

import com.openrsc.server.Constants;
import com.openrsc.server.event.rsc.impl.HealEventNpc;
import com.openrsc.server.event.rsc.impl.RangeEventNpc;
import com.openrsc.server.external.ItemId;
import com.openrsc.server.external.NpcId;
import com.openrsc.server.model.Point;
import com.openrsc.server.model.Skills.SKILLS;
import com.openrsc.server.model.container.Item;
import com.openrsc.server.model.entity.Mob;
import com.openrsc.server.model.entity.player.Player;
import com.openrsc.server.model.states.CombatState;
import com.openrsc.server.model.world.World;
import com.openrsc.server.net.rsc.ActionSender;
import com.openrsc.server.util.rsc.DataConversions;
import com.openrsc.server.util.rsc.MessageType;

import static com.openrsc.server.plugins.Functions.hasItem;
import static com.openrsc.server.plugins.Functions.inArray;
import static com.openrsc.server.plugins.Functions.npcYell;
import static com.openrsc.server.plugins.Functions.playerTalk;
import static com.openrsc.server.plugins.Functions.removeItem;
import static com.openrsc.server.plugins.Functions.showBubble;

public class NpcBehaviorA {

	private long lastMovement;
	private long lastHeal;
	private long lastTackleAttempt;
	private static final int[] TACKLING_XP = {7, 10, 15, 20};

	protected Npc npc;

	protected Mob target;
	private State state = State.ROAM;

	public NpcBehaviorA(Npc npc) {
		this.npc = npc;
	}

	public void tick() {
		for (Player p3 : npc.getViewArea().getPlayersInView()) {
			if (npc.getHitsMade() >= 3 && npc.inCombat() && npc.getLocation().inWilderness()) {
				retreat();
				npc.setHealEventNpc(new HealEventNpc(npc));
				setRoaming();
			}
			if (npc.inCombat() && npc.getID() == 210) {
				target = npc.getOpponent();
				if (npc.getLocation().inWilderness()) {
					npc.setRangeEventNpc(new RangeEventNpc(npc, target));
				} else {
					setRoaming();
				}
			}
			if (npc.getLocation().inWilderness() && npc.getID() == 210 && npc.getSkills().getLevel(SKILLS.HITS.id()) < npc.getSkills().getMaxStat(SKILLS.HITS.id()) * 0.67) {
				npc.setHealEventNpc(new HealEventNpc(npc));
				return;
			}
			int combDiff = Math.abs(npc.getCombatLevel() - p3.getCombatLevel());
			int targetWildLvl = p3.getLocation().wildernessLevel();
			int myWildLvl = npc.getLocation().wildernessLevel();
			if (combDiff < targetWildLvl && combDiff < myWildLvl && npc.withinRange(p3, 5) && npc.getLocation().inWilderness() && p3.getLocation().inWilderness() && npc.getID() == 210) {
				if (npc.getLocation().inWilderness() && target.getLocation().inWilderness() && target != null && !npc.isRespawning() && !npc.isRemoved() && !target.isRemoved()) {
					npc.setRangeEventNpc(new RangeEventNpc(npc, target));
				} else {
					setRoaming();
					return;
				}
			} else if (combDiff < targetWildLvl && combDiff < myWildLvl && npc.withinRange(target, 8) && npc.getLocation().inWilderness() && target.getLocation().inWilderness() && npc.getID() == 210) {
				if (npc.getLocation().inWilderness() && target.getLocation().inWilderness() && target != null && !npc.isRespawning() && !npc.isRemoved() && !target.isRemoved()) {
					npc.walkToEntity(target.getX(), target.getY());
				} else {
					setRoaming();
					return;
				}
			} else if (npc.getID() == 210 && (target == null || npc.isRespawning() || npc.isRemoved() || target.isRemoved() || combDiff > targetWildLvl || combDiff > myWildLvl || !npc.getLocation().inWilderness())) {
				setRoaming();
			} else
				// Target is not in range.
				if (npc.getID() == 210 && (combDiff > targetWildLvl || !npc.getLocation().inWilderness() || target.getX() < npc.getLoc().minX() - 4 || target.getX() > npc.getLoc().maxX() + 4 || target.getY() < npc.getLoc().minY() - 4 || target.getY() > npc.getLoc().maxY() + 4)) {
					setRoaming();
					World.getWorld();
				} else if (!p3.getLocation().inWilderness() && npc.getID() == 210) {
					for (Player p28 : npc.getViewArea().getPlayersInView()) {
						World.getWorld();
						if (p28.getLocation().inWilderness()) {
							World.getWorld();
							target = p28;
							state = State.AGGRO;
						} else
							target = null;
						setRoaming();
					}
				}
			Mob lastTarget;
			if (state == State.ROAM) {
				if (npc.getHitsMade() >= 3 && npc.inCombat() && npc.getLocation().inWilderness()) {
					retreat();
					npc.setHealEventNpc(new HealEventNpc(npc));
					setRoaming();
				}
				if (npc.inCombat() && npc.getID() != 210) {
					state = State.COMBAT;
					return;
				} else if (npc.isBusy() && npc.getID() != 210) {
					return;
				}

				target = null;
				if (System.currentTimeMillis() - lastMovement > 3000 && System.currentTimeMillis() - npc.getCombatTimer() > 3000 && npc.finishedPath()) {
					lastMovement = System.currentTimeMillis();
					lastTarget = null;
					int rand = DataConversions.random(0, 1);
					if (!npc.isBusy() && rand == 1 && !npc.isRemoved()) {
						int newX = DataConversions.random(npc.getLoc().minX(), npc.getLoc().maxX());
						int newY = DataConversions.random(npc.getLoc().minY(), npc.getLoc().maxY());
						if (!grandTreeGnome(npc) || npc.getLocation().equals(new Point(0, 0))) {
							npc.walk(newX, newY);
						} else {
							Point p = walkablePoint(npc, Point.location(npc.getLoc().minX(), npc.getLoc().minY()),
								Point.location(npc.getLoc().maxX(), npc.getLoc().maxY()));
							npc.walk(p.getX(), p.getY());
						}
					}
				}
				if (System.currentTimeMillis() - npc.getCombatTimer() > 3000 && ((npc.getDef().isAggressive() && !(npc.getID() == NpcId.SKELETON_LVL21.id() && npc.getX() >= 208 && npc.getX() <= 211 && npc.getY() >= 545 && npc.getY() <= 546)) || (npc.getLocation().inWilderness())) || (npc.getX() > 274 && npc.getX() < 283 && npc.getY() > 432 && npc.getY() < 441) // Black Knight's Fortress
				) {

					// We loop through all players in view.
					for (Player p : npc.getViewArea().getPlayersInView()) {

						int range = 1;
						switch (NpcId.getById(npc.getID())) {
							case ADVENTURER_ARCHER:
								range = 8;
								break;
							case BANDIT_AGGRESSIVE:
								range = 5;
								break;
							case BLACK_KNIGHT:
								range = 10;
								break;
							default:
								break;
						}

						if (!canAggro(p) || !p.withinRange(npc, range) || !p.getLocation().inWilderness() || combDiff > targetWildLvl || combDiff > myWildLvl)
							continue; // Can't aggro or is not in range.
						if (p.getLocation().inWilderness()) {
							target = p;
							state = State.AGGRO;
						} else {
							target = null;
							continue;
						}
						if (npc.getLastOpponent() == p && (p.getLastOpponent() != npc || expiredLastTargetCombatTimer())) {
							if (npc.getID() != 210) {
								npc.setLastOpponent(null);
								setRoaming();
							}
						}
						break;
					}
				}
				if (System.currentTimeMillis() - lastTackleAttempt > 3000 &&
					npc.getDef().getName().toLowerCase().equals("gnome baller")
					&& !(npc.getID() == NpcId.GNOME_BALLER_TEAMNORTH.id() || npc.getID() == NpcId.GNOME_BALLER_TEAMSOUTH.id())) {
					for (Player p : npc.getViewArea().getPlayersInView()) {
						int range = 1;
						if (!p.withinRange(npc, range) || !hasItem(p, ItemId.GNOME_BALL.id())
							|| !inArray(p.getSyncAttribute("gnomeball_npc", -1), -1, 0))
							continue; // Not in range, does not have a gnome ball or a gnome baller already has ball.

						//set tackle
						state = State.TACKLE;
						target = p;
					}
				}
			} else if (state == State.AGGRO) {
				if (npc.getHitsMade() >= 3 && npc.inCombat() && npc.getLocation().inWilderness()) {
					retreat();
					npc.setHealEventNpc(new HealEventNpc(npc));
					setRoaming();
				} else if (combDiff < targetWildLvl && combDiff < myWildLvl && npc.withinRange(target, 5) && npc.getLocation().inWilderness() && target.getLocation().inWilderness() && npc.getID() == 210) {
					if (npc.getLocation().inWilderness() && target.getLocation().inWilderness() && target != null && !npc.isRespawning() && !npc.isRemoved() && !target.isRemoved()) {
						npc.setRangeEventNpc(new RangeEventNpc(npc, target));
					} else {
						setRoaming();
						return;
					}
				} else if (combDiff < targetWildLvl && combDiff < myWildLvl && npc.withinRange(target, 8) && npc.getLocation().inWilderness() && target.getLocation().inWilderness() && npc.getID() == 210) {
					if (npc.getLocation().inWilderness() && target.getLocation().inWilderness() && target != null && !npc.isRespawning() && !npc.isRemoved() && !target.isRemoved()) {
						npc.walkToEntity(target.getX(), target.getY());
					} else {
						setRoaming();
						return;
					}
				} else if ((target == null || npc.isRespawning() || npc.isRemoved() || target.isRemoved() || combDiff > targetWildLvl || combDiff > myWildLvl || !npc.getLocation().inWilderness())) {
					setRoaming();
				} else
					// Target is not in range.
					if (combDiff > targetWildLvl || combDiff > myWildLvl || !npc.getLocation().inWilderness() || target.getX() < (npc.getLoc().minX() - 4) || target.getX() > (npc.getLoc().maxX() + 4) || target.getY() < (npc.getLoc().minY() - 4) || target.getY() > (npc.getLoc().maxY() + 4)) {
						setRoaming();
					} else if (!target.getLocation().inWilderness()) {
						for (Player p28 : npc.getViewArea().getPlayersInView()) {
							if (p28.getLocation().inWilderness()) {
								target = p28;
								state = State.AGGRO;
							} else
								target = null;
							setRoaming();
						}
					}
					// Combat with another target - set state.
					else if (combDiff > targetWildLvl || !npc.getLocation().inWilderness() || npc.getID() != 210) {
						if (npc.getID() != 210 && npc.inCombat() && npc.getOpponent() != target) {
							npc.setLastOpponent(null);
							target = npc.getOpponent();
							state = State.COMBAT;
						}

						lastMovement = System.currentTimeMillis();
						if (!checkTargetCombatTimer()) {
							if (npc.getID() != 210) {
								npc.walkToEntity(target.getX(), target.getY());
								if (npc.withinRange(target, 1) && npc.canReach(target) && !target.inCombat() && npc.getID() != 210) {
									setFighting(target);
								}
							}
						}
					} else {
						target = null;
						setRoaming();
					}

			} else if (state == State.COMBAT) {
				lastTarget = target;
				if (npc.inCombat()) {
					target = npc.getOpponent();
				} else {
					target = npc.getOpponent();
				}
				if (target == null || npc.isRemoved() || target.isRemoved()) {
					if (npc.getHitsMade() >= 3 && npc.getLocation().inWilderness()) {
						retreat();
						npc.setHealEventNpc(new HealEventNpc(npc));
					}
					target = null;
					setRoaming();
				}
				if (npc.inCombat() && npc.getHitsMade() >= 3 && npc.getLocation().inWilderness()) {
					retreat();
					npc.setHealEventNpc(new HealEventNpc(npc));
					setRoaming();
				} else if (!target.getLocation().inWilderness()) {
					state = State.AGGRO;
				} else if (!npc.inCombat()) {
					npc.setExecutedAggroScript(false);
					if (npc.getID() != 210 && npc.getDef().isAggressive() && ((lastTarget != null && lastTarget.getCombatLevel() < ((npc.getNPCCombatLevel() * 2) + 1)) || npc.getLocation().inWilderness())) {
						setRoaming();
						if (lastTarget != null && npc.getID() != 210)
							target = lastTarget;
					} else {
						if (!npc.isFollowing() && npc.getID() != 210)
							setRoaming();
					}
				} else if (npc.getHitsMade() >= 3 && npc.inCombat() && npc.getLocation().inWilderness()) {
					retreat();
					npc.setHealEventNpc(new HealEventNpc(npc));
					setRoaming();
				} else if (npc.inCombat() && !target.getLocation().inWilderness()) {
					target = null;
					setRoaming();
				} else {
					target = null;
					setRoaming();
				}

			} else if (state == State.TACKLE) {
				// There should not be tackle. Let's resume roaming.
				if (target == null || npc.isRespawning() || npc.isRemoved() || target.isRemoved() || target.inCombat() || target.isBusy()) {
					setRoaming();
				}
				// Target is not in range.
				else if (target.getX() < (npc.getLoc().minX() - 4) || target.getX() > (npc.getLoc().maxX() + 4)
					|| target.getY() < (npc.getLoc().minY() - 4) || target.getY() > (npc.getLoc().maxY() + 4)) {
					setRoaming();
				}

				if (target.isPlayer()) {
					attemptTackle(npc, (Player) target);
					tackle_retreat();
				}
			} else if (state == State.RETREAT) {
				if (npc.getHitsMade() >= 3 && npc.inCombat() && npc.getLocation().inWilderness()) {
					retreat();
					npc.setHealEventNpc(new HealEventNpc(npc));
					setRoaming();
				}
			} else if (state == State.TACKLE_RETREAT) {
				if (npc.finishedPath()) {
					setRoaming();
				}
			}
		}
	}

	private synchronized void attemptTackle(Npc n, Player p) {
		int otherNpcId = p.getSyncAttribute("gnomeball_npc", -1);
		if ((!inArray(otherNpcId, -1, 0) && npc.getID() != otherNpcId) || p.getSyncAttribute("throwing_ball_game", false)) {
			return;
		}
		lastTackleAttempt = System.currentTimeMillis();
		showBubble(p, new Item(ItemId.GNOME_BALL.id()));
		p.message("the gnome trys to tackle you");
		if (DataConversions.random(0, 1) == 0) {
			//successful avoiding tackles gives agility xp
			p.playerServerMessage(MessageType.QUEST, "You manage to push him away");
			npcYell(p, npc, "grrrrr");
			p.incExp(SKILLS.AGILITY.id(), TACKLING_XP[DataConversions.random(0, 3)], true);
		} else {
			if (!inArray(p.getSyncAttribute("gnomeball_npc", -1), -1, 0) || p.getSyncAttribute("throwing_ball_game", false)) {
				// some other gnome beat here or player is shooting at goal
				return;
			}
			p.setSyncAttribute("gnomeball_npc", npc.getID());
			removeItem(p, ItemId.GNOME_BALL.id(), 1);
			p.playerServerMessage(MessageType.QUEST, "he takes the ball...");
			p.playerServerMessage(MessageType.QUEST, "and pushes you to the floor");
			p.damage((int) (Math.ceil(p.getSkills().getLevel(SKILLS.HITS.id()) * 0.05)));
			playerTalk(p, null, "ouch");
			npcYell(p, npc, "yeah");
		}
	}

	public void retreat() {
		state = State.RETREAT;
		npc.getOpponent().setLastOpponent(npc);
		npc.setLastOpponent(npc.getOpponent());
		npc.setRanAwayTimer();
		if (npc.getOpponent().isPlayer()) {
			Player victimPlayer = ((Player) npc.getOpponent());
			victimPlayer.resetAll();
			victimPlayer.message("Your opponent is retreating");
			ActionSender.sendSound(victimPlayer, "retreat");
		}
		//npc.setLastCombatState(CombatState.RUNNING);
		npc.getOpponent().setLastCombatState(CombatState.WAITING);
		npc.resetCombatEvent();

		Point walkTo = Point.location(DataConversions.random(npc.getLoc().minX(), npc.getLoc().maxX()),
			DataConversions.random(npc.getLoc().minY(), npc.getLoc().maxY()));
		npc.walk(walkTo.getX(), walkTo.getY());
	}

	private void tackle_retreat() {
		state = State.TACKLE_RETREAT;
		npc.setLastCombatState(CombatState.RUNNING);
		target.setLastCombatState(CombatState.WAITING);
		npc.resetCombatEvent();

		Point walkTo = Point.location(DataConversions.random(npc.getLoc().minX(), npc.getLoc().maxX()),
			DataConversions.random(npc.getLoc().minY(), npc.getLoc().maxY()));
		npc.walk(walkTo.getX(), walkTo.getY());
	}

	private boolean canAggro(Mob p) {
		boolean outOfBounds = !p.getLocation().inBounds(npc.getLoc().minX - 4, npc.getLoc().minY - 4,
			npc.getLoc().maxX + 4, npc.getLoc().maxY + 4);

		boolean playerOccupied = p.inCombat();
		boolean playerCombatTimeout = System.currentTimeMillis()
			- p.getCombatTimer() < (p.getCombatState() == CombatState.RUNNING
			|| p.getCombatState() == CombatState.WAITING ? 3000 : 1500);

		boolean shouldAttack = (npc.getDef().isAggressive() && (p.getCombatLevel() < ((npc.getNPCCombatLevel() * 2) + 1)
			|| npc.getLocation().inWilderness())) || (npc.getLastOpponent() == p && !shouldRetreat(npc));

		boolean closeEnough = npc.canReach(p);

		return closeEnough && shouldAttack
			&& (p instanceof Player && (!((Player) p).isInvulnerable(npc) && !((Player) p).isInvisible(npc)))
			&& !outOfBounds && !playerOccupied && !playerCombatTimeout;
	}

	private boolean canAggro2(Mob p) {
		boolean outOfBounds = !p.getLocation().inBounds(npc.getLoc().minX - 5, npc.getLoc().minY - 5,
			npc.getLoc().maxX + 5, npc.getLoc().maxY + 5);

		boolean playerOccupied = p.inCombat();
		boolean playerCombatTimeout = System.currentTimeMillis()
			- p.getCombatTimer() < (p.getCombatState() == CombatState.RUNNING
			|| p.getCombatState() == CombatState.WAITING ? 3000 : 1500);

		boolean shouldAttack = (npc.getDef().isAggressive() && npc.getLocation().inWilderness()) || (npc.getLastOpponent() == p && !shouldRetreat2(npc));

		boolean closeEnough = npc.canReach(p);

		return closeEnough && shouldAttack
			&& (p instanceof Player && (!((Player) p).isInvulnerable(npc) && !((Player) p).isInvisible(npc)))
			&& !outOfBounds && !playerOccupied && !playerCombatTimeout;
	}

	private boolean grandTreeGnome(Npc npc) {
		String npcName = npc.getDef().getName();
		return npcName.equalsIgnoreCase("gnome child") || npcName.equalsIgnoreCase("gnome local");
	}

	private Point walkablePoint(Npc npc, Point minP, Point maxP) {
		int currX = npc.getX();
		int currY = npc.getY();
		int radius = 8;
		int newX = DataConversions.random(Math.max(minP.getX(), currX - radius), Math.min(maxP.getX(), currX + radius));
		int newY = DataConversions.random(Math.max(minP.getY(), currY - radius), Math.min(maxP.getY(), currY + radius));
		if (Point.location(newX, newY).inBounds(680, 491, 696, 511)) {
			return Point.location(currX, currY);
		}
		return Point.location(newX, newY);
	}

	public State getBehaviorState() {
		return state;
	}

	boolean isChasing() {
		return state == State.AGGRO;
	}

	public void setChasing(Player player) {
		state = State.AGGRO;
		target = player;
	}

	public void setChasing(Npc npc) {
		state = State.AGGRO;
		target = npc;
	}

	Player getChasedPlayer() {
		if (target.isPlayer())
			return (Player) target;
		return null;
	}

	Npc getChasedNpc() {
		if (target.isNpc())
			return (Npc) target;
		return null;
	}

	private boolean checkTargetCombatTimer() {
		return (System.currentTimeMillis() - target.getCombatTimer()
			< (target.getCombatState() == CombatState.RUNNING
			|| target.getCombatState() == CombatState.WAITING ? 3000 : 1500)
		);
	}

	private boolean expiredLastTargetCombatTimer() {
		return (System.currentTimeMillis() - npc.getLastOpponent().getRanAwayTimer() > 10000);
	}

	public Mob getChaseTarget() {
		return target;
	}

	private void setRoaming() {
		npc.setExecutedAggroScript(false);
		state = State.ROAM;
	}

	private void setFighting(Mob target) {
		npc.startCombat(target);
		state = State.COMBAT;
	}

	private boolean shouldRetreat(Npc npc) {
		if (DataConversions.inArray(Constants.GameServer.NPCS_THAT_RETREAT_NORM, npc.getID())) {
			return npc.getSkills().getLevel(SKILLS.HITS.id()) <=
				Math.ceil(npc.getSkills().getMaxStat(SKILLS.HITS.id()) * 0.20);
		} else if (DataConversions.inArray(Constants.GameServer.NPCS_THAT_RETREAT_LOW, npc.getID())) {
			return npc.getSkills().getLevel(SKILLS.HITS.id()) <=
				Math.ceil(npc.getSkills().getMaxStat(SKILLS.HITS.id()) * 0.05);
		}
		return false;
	}

	private boolean shouldRetreat2(Npc npc) {
		if (npc.getID() == 668) {
			return npc.getSkills().getLevel(SKILLS.HITS.id()) <= Math.ceil(npc.getSkills().getMaxStat(SKILLS.HITS.id()) * 0.20);
		}
		return false;
	}

	public void onKill(Mob killed) {

	}

	enum State {
		ROAM, AGGRO, COMBAT, RETREAT, TACKLE, TACKLE_RETREAT;
	}
}
