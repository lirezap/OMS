/*
 * ISC License
 *
 * Copyright (c) 2025, Alireza Pourtaghi <lirezap@protonmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package software.openex.oms.lib.lz4;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

import static java.lang.foreign.Arena.ofShared;
import static java.lang.foreign.FunctionDescriptor.of;
import static java.lang.foreign.Linker.nativeLinker;
import static java.lang.foreign.SymbolLookup.libraryLookup;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static software.openex.oms.lib.std.CString.strlen;

/**
 * Java FFM wrapper of the LZ4 C library functions.
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

    /**
     * Creates memory allocator, native linker and library lookup instance to load the shared object (or dynamic) LZ4 C
     * library from provided path.
     *
     * @param path the shared object (or dynamic) LZ4 C library's path
     */
    public LZ4(final Path path) {
        this.memory = ofShared();
        final var linker = nativeLinker();
        final var lib = libraryLookup(path, memory);

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
        LZ4_versionNumber(of(JAVA_INT)),
        LZ4_versionString(of(ADDRESS)),
        LZ4_compressBound(of(JAVA_INT, JAVA_INT)),
        LZ4_compress_default(of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT)),
        LZ4_decompress_safe(of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT));

        public final FunctionDescriptor fd;

        FUNCTION(final FunctionDescriptor fd) {
            this.fd = fd;
        }
    }
}
