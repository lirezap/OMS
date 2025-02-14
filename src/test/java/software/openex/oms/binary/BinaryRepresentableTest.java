package software.openex.oms.binary;

import org.junit.jupiter.api.Test;
import software.openex.oms.binary.base.ErrorMessage;
import software.openex.oms.binary.base.ErrorMessageBinaryRepresentation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alireza Pourtaghi
 */
public class BinaryRepresentableTest {

    @Test
    public void testBaseMethods() {
        var message = new ErrorMessage("code", "message");
        try (var binaryRepresentation = new ErrorMessageBinaryRepresentation(message)) {
            binaryRepresentation.encodeV1();
            assertEquals(BinaryRepresentable.version(binaryRepresentation.segment()), 1);
            assertEquals(BinaryRepresentable.flags(binaryRepresentation.segment()), 0b00000000);
            assertEquals(BinaryRepresentable.id(binaryRepresentation.segment()), -1);
            assertEquals(BinaryRepresentable.size(binaryRepresentation.segment()), 21);
        }
    }

    @Test
    public void testStringSize() {
        assertEquals(BinaryRepresentable.representationSize("Testing"), 12);
    }

    @Test
    public void testByteArraySize() {
        assertEquals(BinaryRepresentable.representationSize(new byte[]{1, 2, 3, 4, 5}), 9);
    }

    @Test
    public void testBinaryRepresentationsSize() {
        var m1 = new ErrorMessage("code", "message");
        var m2 = new ErrorMessage("another_code", "another message");

        try (var binaryRepresentation1 = new ErrorMessageBinaryRepresentation(m1);
             var binaryRepresentation2 = new ErrorMessageBinaryRepresentation(m2)) {

            binaryRepresentation1.encodeV1();
            binaryRepresentation2.encodeV1();
            assertEquals(BinaryRepresentable.representationSize(List.of(binaryRepresentation1, binaryRepresentation2)), 82);
        }
    }
}
