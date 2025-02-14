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
            assertEquals(1, BinaryRepresentable.version(binaryRepresentation.segment()));
            assertEquals(0b00000000, BinaryRepresentable.flags(binaryRepresentation.segment()));
            assertEquals(-1, BinaryRepresentable.id(binaryRepresentation.segment()));
            assertEquals(21, BinaryRepresentable.size(binaryRepresentation.segment()));
        }
    }

    @Test
    public void testStringSize() {
        assertEquals(12, BinaryRepresentable.representationSize("Testing"));
    }

    @Test
    public void testByteArraySize() {
        assertEquals(9, BinaryRepresentable.representationSize(new byte[]{1, 2, 3, 4, 5}));
    }

    @Test
    public void testBinaryRepresentationsSize() {
        var m1 = new ErrorMessage("code", "message");
        var m2 = new ErrorMessage("another_code", "another message");

        try (var binaryRepresentation1 = new ErrorMessageBinaryRepresentation(m1);
             var binaryRepresentation2 = new ErrorMessageBinaryRepresentation(m2)) {

            binaryRepresentation1.encodeV1();
            binaryRepresentation2.encodeV1();
            assertEquals(82, BinaryRepresentable.representationSize(List.of(binaryRepresentation1, binaryRepresentation2)));
        }
    }
}
