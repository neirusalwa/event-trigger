package gg.xp.xivsupport.events.triggers.jobs.gui;

import gg.xp.reevent.scan.ScanMe;
import gg.xp.xivsupport.events.actlines.events.AbilityUsedEvent;
import gg.xp.xivsupport.events.actlines.events.BuffApplied;
import gg.xp.xivsupport.events.state.combatstate.CdTracker;
import gg.xp.xivsupport.events.state.combatstate.StatusEffectRepository;
import gg.xp.xivsupport.models.CdTrackingKey;
import gg.xp.xivsupport.persistence.PersistenceProvider;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ScanMe
public class CdTrackerOverlay extends BaseCdTrackerOverlay {

	private final CdTracker cdTracker;
	private final StatusEffectRepository buffRepo;

	public CdTrackerOverlay(PersistenceProvider persistence, CdTracker cdTracker, StatusEffectRepository buffRepo) {
		super("Cd Tracker", "cd-tracker.overlay", persistence, cdTracker.getOverlayMaxPersonal());
		this.cdTracker = cdTracker;
		this.buffRepo = buffRepo;
	}

	@Override
	protected @Nullable Instant chargesReplenishedAt(CdTrackingKey key) {
		return cdTracker.getReplenishedAt(key);
	}

	@Override
	protected Map<CdTrackingKey, AbilityUsedEvent> getCooldowns() {
		return cdTracker.getOverlayPersonalCds();
	}

	@Override
	protected List<BuffApplied> getBuffs() {
		return this.buffRepo.getBuffsAndPreapps().stream()
				.filter(buff -> buff.getSource().walkParentChain().isThePlayer())
				// TODO: maybe filter out stuff on pets here?
//				.filter(buff -> buff.getTarget())
				.collect(Collectors.toList());
	}
}
