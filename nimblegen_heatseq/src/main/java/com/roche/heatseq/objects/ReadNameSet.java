package com.roche.heatseq.objects;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReadNameSet implements Set<String> {

	private final Set<String> set;
	private final String commonBeginnning;

	public ReadNameSet(String commonBeginning) {
		set = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		this.commonBeginnning = commonBeginning;
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return set.contains(o);
	}

	@Override
	public Iterator<String> iterator() {
		return set.iterator();
	}

	@Override
	public Object[] toArray() {
		return set.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return set.toArray(a);
	}

	@Override
	public boolean add(String e) {
		if (!e.startsWith(commonBeginnning)) {
			throw new IllegalStateException("The provided readName[" + e + "] does not start with the expected read name beginning[" + commonBeginnning + "].");
		}
		e = e.replaceFirst(commonBeginnning, "");
		return set.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return set.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return set.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends String> c) {
		return set.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return set.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return set.removeAll(c);
	}

	@Override
	public void clear() {
		set.clear();
	}
}
