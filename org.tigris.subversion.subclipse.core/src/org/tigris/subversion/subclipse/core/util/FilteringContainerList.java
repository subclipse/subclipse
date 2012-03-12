package org.tigris.subversion.subclipse.core.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;

/**
 * Iterable that only returns the top level containers and silently swallowing the childs.
 */
public class FilteringContainerList implements Iterable<IContainer> {
	private SortedMap<String, IContainer> containers = new TreeMap<String, IContainer>();

	public FilteringContainerList() {
	}

	public FilteringContainerList(Collection<IResource> resources) {
		addAll(resources);
	}

	public Iterator<IContainer> iterator() {
		return new FilteringContainerListIterator(new TreeMap<String, IContainer>(containers));
	}

	public void add(IContainer container) {
		containers.put(container.getFullPath().toString(), container);
	}

	public void add(IResource resource) {
		IContainer container;
		if (resource instanceof IContainer) {
			container = (IContainer)resource;
		}
		else {
			container = resource.getParent();
		}
		containers.put(container.getFullPath().toString(), container);
	}

	public void addAll(Collection<IResource> resources) {
		if (resources == null)
			return;
		for (IResource resource : resources)
			add(resource);
	}

	private static class FilteringContainerListIterator implements Iterator<IContainer> {
		private SortedMap<String, IContainer> containers = new TreeMap<String, IContainer>();

		FilteringContainerListIterator(SortedMap<String, IContainer> containers) {
			this.containers = containers;
		}
		
		public boolean hasNext() {
			return !containers.isEmpty();
		}

		public IContainer next() {
			String key = containers.firstKey();
			IContainer result = containers.get(key);
			containers.remove(key);
			key += "/";

			// Remove child containers
    		SortedMap<String, IContainer> childs = containers.tailMap(key);
    		for (Iterator<String> it = childs.keySet().iterator(); it.hasNext(); ) {
    			if (it.next().startsWith(key))
    				it.remove();
    			else
    				break;
    		}
			
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
