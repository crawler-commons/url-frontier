// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import java.io.Closeable;
import java.util.Iterator;

/**
 * Adds close to the Iterator Needed when we need to close resources used by the Iterator (e.g. The
 * RocksDBIterator in case of RocksDb implementation).
 *
 * @param <T>
 */
public interface CloseableIterator<T> extends Closeable, Iterator<T> {}
