package software.openex.oms.matching.event;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import software.openex.oms.event.OMSEvent;

/**
 * @author Alireza Pourtaghi
 */
@Description("Event that is used to measure syncing of a trade from events file into database.")
@Label("Trade Syncing Duration")
@Name("software.openex.oms.matching.engine.SyncTradeEvent")
public final class SyncTradeEvent extends OMSEvent {
}
