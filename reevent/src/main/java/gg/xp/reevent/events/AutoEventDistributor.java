package gg.xp.reevent.events;

import gg.xp.reevent.context.StateStore;
import gg.xp.reevent.scan.AutoHandler;
import gg.xp.reevent.scan.AutoHandlerScan;
import gg.xp.reevent.topology.Topology;
import gg.xp.reevent.topology.TopologyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AutoEventDistributor extends BasicEventDistributor {
	private static final Logger log = LoggerFactory.getLogger(AutoEventDistributor.class);
	private final AutoHandlerScan scanner;
	private final TopologyInfo topoInfo;
	private final Object loadLock = new Object();
	boolean isLoaded;
	private Topology topology;

	public AutoEventDistributor(StateStore state, AutoHandlerScan scanner, TopologyInfo topoInfo) {
		super(state);
		this.scanner = scanner;
		this.topoInfo = topoInfo;
		topology = Topology.fromHandlers(Collections.emptyList(), this.topoInfo);
	}

	// TODO: this just doesn't work well until event sources are also auto-ified
	// We get double events after reloading
	public void reload() {
		// TODO: kind of jank - maybe just have another list of manually-added handlers?
		List<EventHandler<Event>> handlersToKeep = handlers.stream().filter(e -> !(e instanceof AutoHandler)).collect(Collectors.toList());
		handlers.clear();
		handlers.addAll(handlersToKeep);
		List<AutoHandler> handlers = scanner.build();
		this.handlers.addAll(handlers);
		topology = Topology.fromHandlers(new ArrayList<>(this.handlers), topoInfo);
		isLoaded = true;
	}


	public Topology getTopology() {
		return topology;
	}

	// TODO: is there a better place to put this?
	@Override
	public void acceptEvent(Event event) {
		if (!isLoaded) {
			reload();
		}
		if (event instanceof TopologyReloadEvent) {
			// Disabling hot reloading for now
			// Now that log *providers* such as the WS log source expose handlers via
			// @HandleEvents, reloading also causes them to spin up their log sources
			// twice, with disastrous results.
			log.error("Reloading currently disabled");
//			log.warn("RELOAD REQUESTED - ERRORS LIKELY");
//			reload();
		}
		super.acceptEvent(event);
	}


}