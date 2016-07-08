package com.zarbosoft.luxemj;

import com.zarbosoft.pidgoon.events.Store;
import com.zarbosoft.pidgoon.internal.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Helper {

	public static <T> Stream<T> stream(final T[] values) {
		return Arrays.asList(values).stream();
	}

	public static Store stackSingleElement(Store store) {
		final Object value = store.stackTop();
		store = (Store) store.popStack();
		final int length = store.stackTop();
		store = (Store) store.popStack();
		return (Store) store.pushStack(value).pushStack(length + 1);
	}

	public static Store stackDoubleElement(Store store) {
		final Object value = store.stackTop();
		store = (Store) store.popStack();
		final String name = store.stackTop();
		store = (Store) store.popStack();
		final int length = store.stackTop();
		store = (Store) store.popStack();
		return (Store) store.pushStack(new Pair<>(name, value)).pushStack(length + 1);
	}

	public static Store stackDoubleElement(Store store, final String name) {
		final Object value = store.stackTop();
		store = (Store) store.popStack();
		final int length = store.stackTop();
		store = (Store) store.popStack();
		return (Store) store.pushStack(new Pair<>(name, value)).pushStack(length + 1);
	}

	@FunctionalInterface
	public interface Thrower1<T> {
		T get() throws Throwable;
	}

	@FunctionalInterface
	public interface Thrower2 {
		void get() throws Throwable;
	}

	static class UncheckedException extends RuntimeException {
		private static final long serialVersionUID = 9029838186087025315L;

		public UncheckedException(final Throwable e) {
			super(e);
		}
	}

	public static <T> T uncheck(final Thrower1<T> code) {
		try {
			return code.get();
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Throwable e) {
			throw new UncheckedException(e);
		}
	}

	public static void uncheck(final Thrower2 code) {
		try {
			code.get();
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Throwable e) {
			throw new UncheckedException(e);
		}
	}

	public static <L, R> Store stackPopDoubleList(Store s, final Pair.Consumer<L, R> callback) {
		final Integer count = s.stackTop();
		s = (Store) s.popStack();
		return stackPopDoubleList(s, count, callback);
	}

	public static <L, R> Store stackPopDoubleList(Store s, final int length, final Pair.Consumer<L, R> callback) {
		for (int i = 0; i < length; ++i) {
			final Object l = s.stackTop();
			s = (Store) s.popStack();
			final Object r = s.stackTop();
			s = (Store) s.popStack();
			callback.accept((L) l, (R) r);
		}
		return s;
	}

	public static <T> Store stackPopSingleList(Store s, final Consumer<T> callback) {
		final Integer count = s.stackTop();
		s = (Store) s.popStack();
		return stackPopSingleList(s, count, callback);
	}

	public static <T> Store stackPopSingleList(Store s, final int length, final Consumer<T> callback) {
		for (int i = 0; i < length; ++i) {
			callback.accept(s.stackTop());
			s = (Store) s.popStack();
		}
		return s;
	}

	public static <T> T last(final List<T> values) {
		return values.get(values.size() - 1);
	}

}
