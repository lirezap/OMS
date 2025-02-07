package software.openex.oms.matching.event;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import software.openex.oms.event.OMSEvent;

/**
 * @author Alireza Pourtaghi
 */
@Description("Event that is used to measure syncing of a canceled order from events file into database.")
@Label("Canceled Order Syncing Duration")
@Name("software.openex.oms.matching.engine.SyncCanceledOrderEvent")
public final class SyncCanceledOrderEvent extends OMSEvent {
}
