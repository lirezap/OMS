package software.openex.oms.matching.event;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import software.openex.oms.event.OMSEvent;

/**
 * @author Alireza Pourtaghi
 */
@Description("Event that is used to measure order cancellation.")
@Label("Order Cancelling Duration")
@Name("software.openex.oms.matching.engine.CancelOrderEvent")
public final class CancelOrderEvent extends OMSEvent {
}
