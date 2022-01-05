package gg.xp.xivsupport.events.triggers.duties.Pandamonium;

import gg.xp.reevent.events.EventContext;
import gg.xp.reevent.scan.FilteredEventHandler;
import gg.xp.reevent.scan.HandleEvents;
import gg.xp.xivsupport.callouts.CalloutRepo;
import gg.xp.xivsupport.callouts.ModifiableCallout;
import gg.xp.xivsupport.events.actlines.events.AbilityCastStart;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.actlines.events.BuffRemoved;
import gg.xp.xivsupport.events.actlines.events.HeadMarkerEvent;
import gg.xp.xivsupport.events.actlines.events.actorcontrol.DutyCommenceEvent;
import gg.xp.xivsupport.events.state.XivState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CalloutRepo("P2S")
public class P2S implements FilteredEventHandler {
	private static final Logger log = LoggerFactory.getLogger(P2S.class);
	// TODO: zone lock
	private final ModifiableCallout leftTide = new ModifiableCallout("Left Push", "{longshort} West Push");
	private final ModifiableCallout rightTide = new ModifiableCallout("Right Push", "{longshort} East Push");
	private final ModifiableCallout foreTide = new ModifiableCallout("Fore Push", "{longshort} North Push");
	private final ModifiableCallout rearTide = new ModifiableCallout("Rear Push", "{longshort} South Push");

	private final ModifiableCallout blue1 = new ModifiableCallout("Blue #1", "Blue 1");
	private final ModifiableCallout blue2 = new ModifiableCallout("Blue #2", "Blue 2");
	private final ModifiableCallout blue3 = new ModifiableCallout("Blue #3", "Blue 3");
	private final ModifiableCallout blue4 = new ModifiableCallout("Blue #4", "Blue 4");
	private final ModifiableCallout purp1 = new ModifiableCallout("Purple #1", "Purple 1");
	private final ModifiableCallout purp2 = new ModifiableCallout("Purple #2", "Purple 2");
	private final ModifiableCallout purp3 = new ModifiableCallout("Purple #3", "Purple 3");
	private final ModifiableCallout purp4 = new ModifiableCallout("Purple #4", "Purple 4");

	private final ModifiableCallout shockwave = new ModifiableCallout("Shockwave", "Knockback");
	private final ModifiableCallout sewageDeluge = new ModifiableCallout("Sewage Deluge", "Raidwide");
	private final ModifiableCallout murkyDepths = new ModifiableCallout("Murky Depths", "Raidwide");

	private final ModifiableCallout stack = new ModifiableCallout("Mark of the Depths", "Stack on {target}");
	private final ModifiableCallout tides = new ModifiableCallout("Mark of the Tides", "Get Away");

	private final List<BuffApplied> stackSpreadBuffs = new ArrayList<>();

	@HandleEvents
	public void stackOrSpread(EventContext context, BuffApplied event) {
		long id = event.getBuff().getId();
		// AD0 = get away, AD1 = stack
		// Use same logic as jails since these always come in sets of 3
		if (id == 0xAD0 || id == 0xAD1) {
			stackSpreadBuffs.add(event);
			if (stackSpreadBuffs.size() == 3) {
				// First check if the player has a spread debuff on them
				Optional<BuffApplied> spreadOnYou = stackSpreadBuffs.stream().filter(ba -> ba.getTarget().isThePlayer() && ba.getBuff().getId() == 0xAD0).findAny();
				if (spreadOnYou.isPresent()) {
					context.accept(tides.getModified());
				}
				else {
					Optional<BuffApplied> anyStack = stackSpreadBuffs.stream().filter(ba -> ba.getBuff().getId() == 0xAD1).findAny();
					if (anyStack.isPresent()) {
						context.accept(stack.getModified(Map.of("target", anyStack.get().getTarget())));
					}
					else {
						log.warn("Found no stack! Events: {}", stackSpreadBuffs);
					}
				}
				resetStackSpread();
			}
		}
	}

	@HandleEvents
	public void stackOrSpreadRemove(EventContext context, BuffRemoved event) {
		long id = event.getBuff().getId();
		if (id == 0xAD0 || id == 0xAD1) {
			resetStackSpread();
		}
	}

	@HandleEvents
	public void resetAll(EventContext context, DutyCommenceEvent event) {
		resetStackSpread();
		firstHeadmark = null;
	}

	private void resetStackSpread() {
		stackSpreadBuffs.clear();
	}

	@HandleEvents
	public void pushBuff(EventContext context, BuffApplied event) {
		if (event.getTarget().isThePlayer()) {
			ModifiableCallout call;
			if (event.getBuff().getId() == 0xAD4) {
				call = leftTide;
			}
			else if (event.getBuff().getId() == 0xAD5) {
				call = rightTide;
			}
			else if (event.getBuff().getId() == 0xAD3) {
				call = rearTide;
			}
			else if (event.getBuff().getId() == 0xAD2) {
				call = foreTide;
			}
			else {
				return;
			}
			String durationText = event.getInitialDuration().getSeconds() > 15 ? "Long" : "Short";
			context.accept(call.getModified(Map.of("longshort", durationText)));
		}
	}

	@HandleEvents
	public void simpleAbilities(EventContext context, AbilityCastStart event) {
		long id = event.getAbility().getId();
		if (id == 0x682F) {
			context.accept(shockwave.getModified());
		}
		else if (id == 0x6810) {
			context.accept(sewageDeluge.getModified());
		}
		else if (id == 0x6833) {
			context.accept(murkyDepths.getModified());
		}
	}

	private Long firstHeadmark;

	@HandleEvents
	public void sequentialHeadmarkSolver(EventContext context, HeadMarkerEvent event) {
		// This is done unconditionally to create the headmarker offset
		int headmarkOffset = getHeadmarkOffset(event);
		// But after that, we only want the actual player
		if (!event.getTarget().isThePlayer()) {
			return;
		}
		ModifiableCallout call = switch (headmarkOffset) {
			case -114: yield blue1;
			case -113: yield blue2;
			case -112: yield blue3;
			case -111: yield blue4;
			case -110: yield purp1;
			case -109: yield purp2;
			case -108: yield purp3;
			case -107: yield purp4;
			default: yield null;
		};
		if (call != null) {
			context.accept(call.getModified());
		}
	}

	private int getHeadmarkOffset(HeadMarkerEvent event) {
		if (firstHeadmark == null) {
			firstHeadmark = event.getMarkerId();
		}
		return (int) (event.getMarkerId() - firstHeadmark);
	}


	@Override
	public boolean enabled(EventContext context) {
		return context.getStateInfo().get(XivState.class).zoneIs(0x3ED);
	}
}