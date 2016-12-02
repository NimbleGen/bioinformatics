package com.roche.sequencing.bioinformatics.common.graph.layout;

public class Node<T> {

	private final T contents;

	public Node(T contents) {
		super();
		this.contents = contents;
	}

	public T getContents() {
		return contents;
	}

	@Override
	public String toString() {
		return "Node [contents=" + contents + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contents == null) ? 0 : contents.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node<?> other = (Node<?>) obj;
		if (contents == null) {
			if (other.contents != null)
				return false;
		} else if (!contents.equals(other.contents))
			return false;
		return true;
	}

}
