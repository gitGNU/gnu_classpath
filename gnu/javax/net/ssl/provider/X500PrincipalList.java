/* X500PrincipalList.java -- A list of X.500 names.
   Copyright (C) 2006  Free Software Foundation, Inc.

This file is a part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
USA

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version.  */


package gnu.javax.net.ssl.provider;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.nio.ByteBuffer;

import java.util.ConcurrentModificationException;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import javax.security.auth.x500.X500Principal;

class X500PrincipalList
{
  private final ByteBuffer buffer;
  private int modCount;

  X500PrincipalList (final ByteBuffer buffer)
  {
    this.buffer = buffer;
    modCount = 0;
  }

  int size ()
  {
    return (buffer.getShort (0) & 0xFFFF);
  }

  int count ()
  {
    int size = size ();
    int i = 0;
    for (int offset = 2; offset < size; i++)
      {
        int _size = (buffer.getShort (offset) & 0xFFFF);
        // We don't want this going into an infinite loop if
        // you mistakenly put a zero-length name.
        if (_size == 0)
          break;
        offset += _size + 2;
      }
    return i;
  }

  X500Principal get (final int index)
  {
    if (index < 0)
      throw new IndexOutOfBoundsException ("negative index");
    int size = size ();
    int i = 0;
    for (int offset = 2; offset < size; i++)
      {
        int _size = (buffer.getShort (offset) & 0xFFFF);
        if (_size == 0)
          throw new IndexOutOfBoundsException ("zero-length name encountered");
        if (i == index)
          {
            byte[] buf = new byte[_size];
            buffer.position (offset + 2);
            buffer.get (buf);
            return new X500Principal (buf);
          }
        offset += 2 + _size;
      }
    throw new IndexOutOfBoundsException ("limit: " + i + "; requested: " + index);
  }

  void put (final int index, final X500Principal principal)
  {
    put (index, principal.getEncoded ());
  }

  void put (final int index, final byte[] encoded)
  {
    if (index < 0)
      throw new IndexOutOfBoundsException ("negative index");
    int size = size ();
    int i = 0;
    for (int offset = 2; offset < size; i++)
      {
        int off = (buffer.getShort (offset) & 0xFFFF);
        if (i == index)
          {
            buffer.putShort (offset, (short) encoded.length);
            buffer.position (offset + 2);
            buffer.put (encoded);
            modCount++;
            return;
          }
        offset += 2 + off;
      }
    throw new IndexOutOfBoundsException ("limit: " + (i-1) + "; requested: " + index);
  }

  void setSize (final int numNames, final int namesSize)
  {
    if (numNames < 1)
      throw new IllegalArgumentException ("must have at least one name");
    int size = (numNames * 2) + namesSize;
    if (size < 3 || size > buffer.capacity () || size > 0xFFFF)
      throw new IllegalArgumentException ("size out of range; maximum: "
                                          + Math.min (buffer.capacity (), 0xFFFF));
    buffer.putShort (0, (short) size);
  }

  public String toString ()
  {
    return toString (null);
  }

  public String toString (final String prefix)
  {
    StringWriter str = new StringWriter ();
    PrintWriter out = new PrintWriter (str);
    if (prefix != null) out.print (prefix);
    out.print ("[");
    out.print (count ());
    out.println ("] {");
    for (Iterator it = new Iterator (); it.hasNext (); )
      {
        if (prefix != null) out.print (prefix);
        out.print ("  ");
        out.println (it.next ());
      }
    if (prefix != null) out.print (prefix);
    out.print ("};");
    return str.toString ();
  }

  class Iterator implements ListIterator
  {
    private final int modCount;
    private int index;
    private final int count;

    Iterator ()
    {
      this.modCount = X500PrincipalList.this.modCount;
      index = 0;
      count = count ();
    }

    public void add (Object o)
    {
      throw new UnsupportedOperationException ();
    }

    public boolean hasNext ()
    {
      return (index < count);
    }

    public boolean hasPrevious ()
    {
      return (index > 0);
    }

    public Object next () throws NoSuchElementException
    {
      if (modCount != X500PrincipalList.this.modCount)
        throw new ConcurrentModificationException ();
      try
        {
          return get (index++);
        }
      catch (IndexOutOfBoundsException ioobe)
        {
          throw new NoSuchElementException ();
        }
    }

    public int nextIndex ()
    {
      if (hasNext ())
        return (index + 1);
      return -1;
    }

    public Object previous () throws NoSuchElementException
    {
      if (index == 0)
        throw new NoSuchElementException ();
      if (modCount != X500PrincipalList.this.modCount)
        throw new ConcurrentModificationException ();
      try
        {
          return get (--index);
        }
      catch (IndexOutOfBoundsException ioobe)
        {
          throw new NoSuchElementException ();
        }
    }

    public int previousIndex ()
    {
      return (index - 1);
    }

    public void remove ()
    {
      throw new UnsupportedOperationException ();
    }

    public void set (final Object o)
    {
      throw new UnsupportedOperationException ();
    }
  }
}