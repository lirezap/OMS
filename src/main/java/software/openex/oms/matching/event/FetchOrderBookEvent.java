package software.openex.oms.matching.event;

import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import software.openex.oms.event.OMSEvent;

/**
 * @author Alireza Pourtaghi
 */
@Description("Event that is used to measure order book fetching.")
@Label("Order Book Fetching Duration")
@Name("software.openex.oms.matching.engine.FetchOrderBookEvent")
public final class FetchOrderBookEvent extends OMSEvent {
}
