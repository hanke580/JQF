/*
 The MIT License

 Copyright (c) 2017 University of California, Berkeley

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package edu.berkeley.cs.jqf.fuzz.guidance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;


/**
 * This class extends {@link Random} to act as a generator of
 * "random" values, which themselves are read from a static file.
 *
 * The file-backed random number generator can be used for tuning the
 * "random" choices made by various <tt>junit-quickcheck</tt>
 * generators using a mutation-based genetic algorithm, in order to
 * maximize some objective function that can be measured from the
 * execution of each trial, such as code coverage.
 *
 *
 */
public class StreamBackedRandom extends Random {
    private final InputStream inputStream;
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
    private int totalBytesRead = 0;
    private int leadingBytesToIgnore = 0;

    /**
     * Constructs a stream-backed random generator.
     *
     * Also sets the seed of the underlying pseudo-random number
     * generator deterministically to zero.
     *
     * @param source  a generator of "random" bytes
     */
    public StreamBackedRandom(InputStream source) {
        super(0x5DEECE66DL);
        // Open the backing file source as a buffered input stream
        this.inputStream = source;
        // Force encoding to little-endian so that we can read small ints
        // by reading partially into the start of the buffer
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Convenience constructor for use with the junit-quickcheck framework.
     *
     * The junit-quickcheck SourceOfRandomness annoyingly reads 8 bytes of
     * random data as soon as it is instantiated, only to configure its own
     * seed. As the seed is meaningless here, the 8 bytes get wasted. For
     * this purpose, this constructor allows specifying how many bytes of
     * requested random data to skip before starting to read from file.
     *
     * @param source  a generator of "random" bytes
     * @param leadinBytesToIgnore the number of leading bytes to ignore
     */
    public StreamBackedRandom(InputStream source, int leadinBytesToIgnore) {
        this(source);
        this.leadingBytesToIgnore = leadinBytesToIgnore;
    }

    /**
     * Generates upto 32 bits of random data for internal use by the Random
     * class.
     *
     * Attempts to read up to 4 bytes of data from the input file, and
     * returns the requested lower order bits as a pseudo-random value.
     *
     * If end-of-file is reached before reading 4 bytes,
     * an {@link IllegalStateException} is thrown.
     *
     * If the backing file source has not yet been set, it defaults to
     * the pseudo-random number generation algorithm from
     * {@link Random}. This is still deterministic, as the seed
     * of the pseudo-random number generator is deterministically set in the
     * constructor.
     *
     * @param bits   the number of random bits to retain (1 to 32 inclusive)
     * @return the integer value whose lower <tt>bits</tt> bits contain the
     *    next random data available in the backing source
     * @throws IllegalStateException  if EOF has already been reached
     */
    @Override
    public int next(int bits) {
        // Ensure that up to 32 bits are being requested
        if (bits < 0 || bits > 32) {
            throw new IllegalArgumentException("Must read 1-32 bits at a time");
        }

        // Zero out the byte buffer before reading from the source
        byteBuffer.putInt(0, 0);

        try {
            // Read up to 4 bytes from the backing source
            int maxBytesToRead = ((bits + 7) / 8);
            assert(maxBytesToRead*8 >= bits && maxBytesToRead <= 4);

            if (this.leadingBytesToIgnore > 0) {
                int bytesToIgnore = Math.min(maxBytesToRead, this.leadingBytesToIgnore);
                this.leadingBytesToIgnore -= bytesToIgnore;
                maxBytesToRead -= bytesToIgnore;
            }

            // If fewer bytes are read (because EOF is reached), the buffer
            // just keeps containing zeros
            int actualBytesRead = inputStream.read(byteBuffer.array(), 0, maxBytesToRead);
            totalBytesRead += actualBytesRead;

            // If EOF was reached, throw an exception
            if (actualBytesRead != maxBytesToRead) {
                String message = String.format("EOF reached; total bytes read = %d, " +
                                "last read got %d of %d bytes",
                        totalBytesRead, actualBytesRead, maxBytesToRead);
                throw new IllegalStateException(message);

            }

        } catch (IOException e) {
            throw new GuidanceException(e);
        }

        // Interpret the bytes read as an integer
        int value = byteBuffer.getInt(0);

        // Return only the lower order bits as requested
        int mask = bits < 32 ? (1 << bits) - 1 : -1;
        return value & mask;

    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0)
            throw new IllegalArgumentException("bound must be positive");
        return next(31) % bound;
    }

    public byte nextByte() {
        return (byte) next(Byte.SIZE);
    }

    public short nextShort() {
        return (short) next(Short.SIZE);
    }

    public int getTotalBytesRead() {
        return this.totalBytesRead;
    }

}