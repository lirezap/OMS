package com.lirezap.nex.lib.lz4;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

import static com.lirezap.nex.lib.std.CString.strlen;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Java FFM used wrapper of LZ4 C library functions.
 *
 * @author Alireza Pourtaghi
 */
public final class LZ4 implements AutoCloseable {
    private final Arena memory;
    private final MethodHandle versionNumber;
    private final MethodHandle versionString;
    private final MethodHandle compressBound;
    private final MethodHandle compressDefault;
    private final MethodHandle decompressSafe;

    public LZ4(final Path path) {
        this.memory = Arena.ofShared();
        final var linker = Linker.nativeLinker();
        final var lib = SymbolLookup.libraryLookup(path, memory);

        this.versionNumber =
                linker.downcallHandle(lib.find(FUNCTION.LZ4_versionNumber.name()).orElseThrow(), FUNCTION.LZ4_versionNumber.fd);

        this.versionString =
                linker.downcallHandle(lib.find(FUNCTION.LZ4_versionString.name()).orElseThrow(), FUNCTION.LZ4_versionString.fd);

        this.compressBound =
                linker.downcallHandle(lib.find(FUNCTION.LZ4_compressBound.name()).orElseThrow(), FUNCTION.LZ4_compressBound.fd);

        this.compressDefault =
                linker.downcallHandle(lib.find(FUNCTION.LZ4_compress_default.name()).orElseThrow(), FUNCTION.LZ4_compress_default.fd);

        this.decompressSafe =
                linker.downcallHandle(lib.find(FUNCTION.LZ4_decompress_safe.name()).orElseThrow(), FUNCTION.LZ4_decompress_safe.fd);
    }

    public int versionNumber() throws Throwable {
        return (int) versionNumber.invokeExact();
    }

    public String versionString() throws Throwable {
        final var versionPtr = (MemorySegment) versionString.invokeExact();
        return versionPtr.reinterpret(strlen(versionPtr) + 1).getString(0);
    }

    public int compressBound(final int inputSize) throws Throwable {
        return (int) compressBound.invokeExact(inputSize);
    }

    public int compressDefault(final MemorySegment src, final MemorySegment dst, final int srcSize,
                               final int dstCapacity) throws Throwable {

        return (int) compressDefault.invokeExact(src, dst, srcSize, dstCapacity);
    }

    public int decompressSafe(final MemorySegment src, final MemorySegment dst, final int compressedSize,
                              final int dstCapacity) throws Throwable {

        return (int) decompressSafe.invokeExact(src, dst, compressedSize, dstCapacity);
    }

    @Override
    public void close() throws Exception {
        memory.close();
    }

    /**
     * Name and descriptor of loaded C functions.
     *
     * @author Alireza Pourtaghi
     */
    private enum FUNCTION {
        LZ4_versionNumber(FunctionDescriptor.of(JAVA_INT)),
        LZ4_versionString(FunctionDescriptor.of(ADDRESS)),
        LZ4_compressBound(FunctionDescriptor.of(JAVA_INT, JAVA_INT)),
        LZ4_compress_default(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT)),
        LZ4_decompress_safe(FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT));

        public final FunctionDescriptor fd;

        FUNCTION(final FunctionDescriptor fd) {
            this.fd = fd;
        }
    }
}
