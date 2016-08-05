package com.roche.sequencing.bioinformatics.common.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class CombinedList<T> implements List<T> {

	private final List<List<T>> lists;
	private final int size;

	@SafeVarargs
	public CombinedList(List<T>... lists) {
		this.lists = Arrays.asList(lists);
		int calculatingSize = 0;
		for (List<T> list : lists) {
			calculatingSize += list.size();
		}
		this.size = calculatingSize;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size() > 0;
	}

	@Override
	public boolean contains(Object o) {
		boolean contains = false;
		int i = 0;
		while (!contains && i < lists.size()) {
			contains = lists.get(i).contains(o);
			i++;
		}
		return contains;
	}

	@Override
	public Iterator<T> iterator() {
		return new CombinedListIterator();
	}

	private class CombinedListIterator implements Iterator<T> {

		private int listIndex;
		private int indexInList;

		private int overallIndex;

		public CombinedListIterator() {
			listIndex = 0;
			indexInList = 0;
			overallIndex = 0;
			if (hasNext()) {
				List<T> currentList = lists.get(listIndex);
				while (indexInList >= currentList.size()) {
					listIndex++;
					currentList = lists.get(listIndex);
					indexInList = 0;
				}
			}
		}

		@Override
		public boolean hasNext() {
			return overallIndex < size;
		}

		@Override
		public T next() {
			List<T> currentList = lists.get(listIndex);
			T next = currentList.get(indexInList);
			overallIndex++;

			if (hasNext()) {
				indexInList++;
				while (indexInList >= currentList.size()) {
					listIndex++;
					currentList = lists.get(listIndex);
					indexInList = 0;
				}
			}

			return next;
		}

		@Override
		public void remove() {
			throw new IllegalStateException("This method is not implemented.");
		}

	}

	@Override
	public Object[] toArray() {
		return null;
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] a) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public boolean add(T e) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public boolean remove(Object o) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public void clear() {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public T get(int index) {
		int listIndex = 0;
		int overallIndexAfterCurrentList = lists.get(listIndex).size();
		while (overallIndexAfterCurrentList <= index && listIndex < lists.size()) {
			listIndex++;
			overallIndexAfterCurrentList += lists.get(listIndex).size();
		}
		if (listIndex >= lists.size()) {
			throw new IndexOutOfBoundsException("Unable to access index[" + index + "] on a list of size[" + size + "].");
		}
		List<T> currentList = lists.get(listIndex);
		int indexInList = index - (overallIndexAfterCurrentList - currentList.size());
		return currentList.get(indexInList);
	}

	@Override
	public T set(int index, T element) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public void add(int index, T element) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public T remove(int index) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public int indexOf(Object o) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public ListIterator<T> listIterator() {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		throw new IllegalStateException("This method is not implemented.");
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		throw new IllegalStateException("This method is not implemented.");
	}

	public static void main(String[] args) {
		List<String> list = new ArrayList<String>();
		list.add("A");
		list.add("B");
		list.add("C");
		List<String> list2 = new ArrayList<String>();
		list2.add("D");
		list2.add("E");
		list2.add("F");
		List<String> list3 = new ArrayList<String>();
		list3.add("G");
		list3.add("H");
		list3.add("I");
		List<String> combined = new CombinedList<String>(list, list2, list3);
		for (int i = 0; i < combined.size(); i++) {
			System.out.println(combined.get(i));
		}
	}

}
