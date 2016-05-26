/*
Copyright (c) 2011 Jonathan Leibiusky

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package co.paralleluniverse.fibers.redis;

import java.io.ByteArrayInputStream;

/**
 * Test class the fragment a byte array for testing purpose.
 */
public class FragmentedByteArrayInputStream extends ByteArrayInputStream {
  private int readMethodCallCount = 0;

  public FragmentedByteArrayInputStream(final byte[] buf) {
    super(buf);
  }

  public synchronized int read(final byte[] b, final int off, final int len) {
    readMethodCallCount++;
    if (len <= 10) {
      // if the len <= 10, return as usual ..
      return super.read(b, off, len);
    } else {
      // else return the first half ..
      return super.read(b, off, len / 2);
    }
  }

  public int getReadMethodCallCount() {
    return readMethodCallCount;
  }

}
