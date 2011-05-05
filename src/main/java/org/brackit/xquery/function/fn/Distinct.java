/*
 * [New BSD License]
 * Copyright (c) 2011, Brackit Project Team <info@brackit.org>  
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.brackit.xquery.function.fn;

import java.util.Comparator;

import org.brackit.xquery.ErrorCode;
import org.brackit.xquery.QueryContext;
import org.brackit.xquery.QueryException;
import org.brackit.xquery.Tuple;
import org.brackit.xquery.atomic.Atomic;
import org.brackit.xquery.atomic.Int32;
import org.brackit.xquery.atomic.IntegerNumeric;
import org.brackit.xquery.atomic.QNm;
import org.brackit.xquery.atomic.Str;
import org.brackit.xquery.function.AbstractFunction;
import org.brackit.xquery.function.Signature;
import org.brackit.xquery.sequence.LazySequence;
import org.brackit.xquery.util.TupleSort;
import org.brackit.xquery.xdm.Item;
import org.brackit.xquery.xdm.Iter;
import org.brackit.xquery.xdm.Sequence;
import org.brackit.xquery.xdm.Stream;

/**
 * 
 * @author Sebastian Baechle
 * 
 */
public class Distinct extends AbstractFunction {
	public Distinct(QNm name, Signature signature) {
		super(name, signature, true);
	}

	@Override
	public Sequence execute(final QueryContext ctx, Sequence[] args)
			throws QueryException {
		if (args.length == 2) {
			Str collation = (Str) args[1];

			if (!collation.str
					.equals("http://www.w3.org/2005/xpath-functions/collation/codepoint")) {
				throw new QueryException(ErrorCode.ERR_UNSUPPORTED_COLLATION,
						"Unsupported collation: %s", collation);
			}
		}

		final Sequence s = args[0];

		if ((s == null) || (s instanceof Item)) {
			return s;
		}

		final Comparator<Tuple> comparator = new Comparator<Tuple>() {
			@Override
			public int compare(Tuple o1, Tuple o2) {
				int res = ((Atomic) o1).atomicCmp((Atomic) o2);
				return res;
			}
		};

		return new LazySequence() {
			final Sequence inSeq = s;
			TupleSort sort;

			@Override
			public Iter iterate() {
				return new Iter() {
					Stream<? extends Tuple> sorted;
					IntegerNumeric pos = Int32.ZERO;
					Atomic prev = null;

					@Override
					public Item next() throws QueryException {
						if (sort == null) {
							sort = new TupleSort(comparator, -1); // TODO -1
							// means no
							// external
							// sort

							Iter it = inSeq.iterate();
							try {
								Item runVar;
								while ((runVar = it.next()) != null) {
									sort.add(runVar);
								}
								sort.sort();
							} finally {
								it.close();
							}
						}
						if (sorted == null) {
							sorted = sort.stream();
						}
						Atomic next;
						while ((next = (Atomic) sorted.next()) != null) {
							if ((prev == null) || (prev.atomicCmp(next) != 0)) {
								prev = next;
								return next;
							}
						}
						return null;
					}

					@Override
					public void close() {
						if (sorted != null) {
							sorted.close();
						}
					}
				};
			}
		};
	}
}