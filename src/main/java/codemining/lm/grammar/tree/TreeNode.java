/**
 * 
 */
package codemining.lm.grammar.tree;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.esotericsoftware.kryo.DefaultSerializer;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * A generic tree node
 * 
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 * 
 */
@DefaultSerializer(JavaSerializer.class)
public final class TreeNode<T extends Serializable> implements Serializable {
	/**
	 * A struct class containing from and to pair of nodes to copy.
	 * 
	 */
	public static final class NodePair<T extends Serializable> {
		public final TreeNode<T> fromNode;

		public final TreeNode<T> toNode;

		public NodePair(final TreeNode<T> from, final TreeNode<T> to) {
			fromNode = from;
			toNode = to;
		}
	}

	/**
	 * A struct computing and containing the parent nodes of a given target node
	 * in a tree. The lists contain the nodes and the "directions" to reach the
	 * target node. The first node in the list is the parent of the target of
	 * the node, while the last is the root.
	 * 
	 * The implementation includes a slow, recursive solution. But it is the
	 * easiest for understanding.
	 */
	public static class NodeParents<T extends Serializable> {
		public final TreeNode<T> targetNode;

		public final List<TreeNode<T>> throughNodes = Lists.newArrayList();

		public final List<Integer> nextProperty = Lists.newArrayList();

		public final List<Integer> nextChildNum = Lists.newArrayList();

		public NodeParents(final TreeNode<T> root, final TreeNode<T> targetNode) {
			this.targetNode = targetNode;
			final boolean pathFound = reachTarget(root);
			checkArgument(pathFound);
		}

		private boolean reachTarget(final TreeNode<T> currentNode) {
			if (currentNode == targetNode) {
				return true;
			}

			final List<List<TreeNode<T>>> children = currentNode.childrenProperties;

			for (int propertyId = 0; propertyId < children.size(); propertyId++) {
				final List<TreeNode<T>> propertyChildren = children
						.get(propertyId);
				for (int i = 0; i < propertyChildren.size(); i++) {
					final TreeNode<T> currentChild = propertyChildren.get(i);
					final boolean isInPath = reachTarget(currentChild);
					if (isInPath) {
						throughNodes.add(currentNode);
						nextProperty.add(propertyId);
						nextChildNum.add(i);
						return true;
					}
				}
			}

			return false;
		}

	}

	/**
	 * A struct for passing a tree along with references to some nodes.
	 * 
	 * @param <T>
	 */
	public static final class NodeWithRef<T extends Serializable> {
		public static <T extends Serializable> NodeWithRef<T> createNodeCompare(
				final TreeNode<T> node, final Set<TreeNode<T>> references,
				final TreeNode<T> currentReference) {
			final NodeWithRef<T> cmp = new NodeWithRef<T>();
			cmp.node = node;
			cmp.references = references;
			cmp.currentReference = currentReference;
			return cmp;
		}

		public TreeNode<T> node;
		public TreeNode<T> currentReference;
		public Set<TreeNode<T>> references;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3543181013512815033L;

	/**
	 * A static constant used for String conversion
	 */
	public static final String SUB_NODE_STRING_PREFIX = "-";

	/**
	 * Copy the children (and all (grand+)children) to the given toNode. This
	 * will copy only the structure. The data will be the same.
	 * 
	 * @param fromNode
	 * @param toNode
	 */
	private static <T extends Serializable> void copyChildren(
			final TreeNode<T> fromNode, final TreeNode<T> toNode) {
		final ArrayDeque<NodePair<T>> stack = new ArrayDeque<NodePair<T>>();

		stack.push(new NodePair<T>(fromNode, toNode));

		while (!stack.isEmpty()) {
			final NodePair<T> pair = stack.pop();
			final TreeNode<T> currentFrom = pair.fromNode;
			final TreeNode<T> currentTo = pair.toNode;

			final List<List<TreeNode<T>>> children = currentFrom
					.getChildrenByProperty();

			for (int i = 0; i < children.size(); i++) {
				for (final TreeNode<T> fromChild : children.get(i)) {
					final TreeNode<T> toChild = TreeNode.create(
							fromChild.getData(), fromChild.nProperties());
					currentTo.addChildNode(toChild, i);

					stack.push(new NodePair<T>(fromChild, toChild));
				}
			}
		}
	}

	/**
	 * Copy the children (and all (grand+)children) to the given toNode. This
	 * will copy only the structure. The data will be the same.
	 * 
	 * @param fromNode
	 * @param toNode
	 * @param stopOnRoots
	 */
	private static <T extends Serializable> NodeWithRef<T> copyChildren(
			final TreeNode<T> fromNode, final TreeNode<T> toNode,
			final Set<TreeNode<T>> references,
			final TreeNode<T> currentReference) {
		final ArrayDeque<NodePair<T>> stack = new ArrayDeque<NodePair<T>>();
		final Set<TreeNode<T>> referencesCopy = Sets.newHashSet();
		if (references.contains(fromNode)) {
			referencesCopy.add(toNode);
		}

		TreeNode<T> currentReferenceCopy = null;
		if (currentReference == fromNode) {
			currentReferenceCopy = toNode;
		}

		stack.push(new NodePair<T>(fromNode, toNode));

		while (!stack.isEmpty()) {
			final NodePair<T> pair = stack.pop();
			final TreeNode<T> currentFrom = pair.fromNode;
			final TreeNode<T> currentTo = pair.toNode;

			final List<List<TreeNode<T>>> children = currentFrom
					.getChildrenByProperty();

			for (int i = 0; i < children.size(); i++) {
				for (final TreeNode<T> fromChild : children.get(i)) {
					final TreeNode<T> toChild = TreeNode.create(
							fromChild.getData(), fromChild.nProperties());
					currentTo.addChildNode(toChild, i);

					stack.push(new NodePair<T>(fromChild, toChild));
					if (references.contains(fromChild)) {
						referencesCopy.add(toChild);
					}
					if (currentReference == fromChild) {
						currentReferenceCopy = toChild;
					}
				}
			}
		}

		return NodeWithRef.createNodeCompare(toNode, referencesCopy,
				currentReferenceCopy);
	}

	/**
	 * Static utility to create TreeNode.
	 * 
	 * @param data
	 * @param size
	 * @return
	 */
	public static final <T extends Serializable> TreeNode<T> create(
			final T data, final int size) {
		return new TreeNode<T>(data, size);
	}

	/**
	 * Static utility to create TreeNode.
	 * 
	 * @return
	 */
	public static final <T extends Serializable> TreeNode<T> create(
			final TreeNode<T> other) {
		return new TreeNode<T>(other.getData(), other.nProperties());
	}

	/**
	 * Return the total number of nodes in the subtree.
	 * 
	 * @param subtree
	 * @return
	 */
	public static <T extends Serializable> int getTreeSize(
			final TreeNode<T> subtree) {
		checkNotNull(subtree);

		final ArrayDeque<TreeNode<T>> toLook = new ArrayDeque<TreeNode<T>>();
		int size = 1;
		toLook.push(subtree);
		while (!toLook.isEmpty()) {
			final TreeNode<T> currentNode = toLook.pop();

			for (final List<TreeNode<T>> childProperties : currentNode
					.getChildrenByProperty()) {
				size += childProperties.size();
				for (final TreeNode<T> child : childProperties) {
					toLook.push(child);
				}
			}
		}
		return size;
	}

	/**
	 * The children of this node. This is a list of lists. One list for each
	 * property.
	 */
	private final List<List<TreeNode<T>>> childrenProperties;

	/**
	 * The details of the tree node.
	 */
	private final T nodeData;

	/**
	 * Construct a Node give its data.
	 * 
	 * @param name
	 *            the name/data of the node
	 */
	private TreeNode(final T name, final int nProperties) {
		nodeData = name;
		childrenProperties = Lists.newArrayListWithCapacity(nProperties);
		for (int i = 0; i < nProperties; i++) {
			final List<TreeNode<T>> childrenElements = Lists.newArrayList();
			childrenProperties.add(childrenElements);
		}
	}

	/**
	 * Create an immutable node with this data.
	 * 
	 * @param name
	 * @param children
	 */
	private TreeNode(final T name, final List<List<TreeNode<T>>> children) {
		nodeData = name;
		this.childrenProperties = ImmutableList.copyOf(children);
	}

	/**
	 * Add a child to this node.
	 * 
	 * @param child
	 */
	public synchronized void addChildNode(final TreeNode<T> child,
			final int propertyIndex) {
		final List<TreeNode<T>> childrenPlaceholder = childrenProperties
				.get(checkElementIndex(propertyIndex, childrenProperties.size()));
		childrenPlaceholder.add(child);
	}

	/**
	 * Create a deep copy of the TreeNode structure. Data of each node, still
	 * refers to the same element.
	 * 
	 * @return
	 */
	public TreeNode<T> deepCopy() {
		final TreeNode<T> toChild = TreeNode.create(nodeData, nProperties());
		TreeNode.copyChildren(this, toChild);
		return toChild;
	}

	/**
	 * Return a deep copy of this tree node and a reference to a child in the
	 * copied tree that matches the node in this tree.
	 * 
	 * @param references
	 * @return a pair of nodes. The first one is the copied tree, the second is
	 *         the reference.
	 */
	public NodeWithRef<T> deepCopyWithReferences(
			final Set<TreeNode<T>> references,
			final TreeNode<T> currentReference) {
		final TreeNode<T> toChild = TreeNode.create(nodeData, nProperties());
		return TreeNode.copyChildren(this, toChild, references,
				currentReference);

	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final TreeNode<T> other = (TreeNode<T>) obj;
		if (nodeData == null) {
			if (other.nodeData != null) {
				return false;
			}
		} else if (!nodeData.equals(other.nodeData)) {
			return false;
		}
		if (childrenProperties == null) {
			if (other.childrenProperties != null) {
				return false;
			}
		} else if (!childrenProperties.equals(other.childrenProperties)) {
			return false;
		}
		return true;
	}

	/**
	 * Get the i-th child
	 * 
	 * @param i
	 * @return
	 */
	public TreeNode<T> getChild(final int i, final int propertyId) {
		return childrenProperties.get(propertyId).get(i);
	}

	/**
	 * Return all the children of this node.
	 * 
	 * @return
	 */
	public List<List<TreeNode<T>>> getChildrenByProperty() {
		return Collections.unmodifiableList(childrenProperties);
	}

	/**
	 * Return the node data.
	 * 
	 * @return
	 */
	public T getData() {
		return nodeData;
	}

	/**
	 * Return the parents of this node from a root node.
	 * 
	 * @param fromRoot
	 * @return
	 */
	public NodeParents<T> getNodeParents(final TreeNode<T> fromRoot) {
		return new NodeParents<T>(fromRoot, this);
	}

	/**
	 * Return the tree size of this tree.
	 * 
	 * @return
	 */
	public int getTreeSize() {
		return getTreeSize(this);
	}

	@Override
	public int hashCode() {
		if (childrenProperties.size() > 0) {
			return Objects.hashCode(nodeData, childrenProperties.get(0));
		} else {
			return Objects.hashCode(nodeData);
		}
	}

	public boolean isLeaf() {
		for (final List<TreeNode<T>> childProperty : childrenProperties) {
			if (!childProperty.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public int nProperties() {
		return childrenProperties.size();
	}

	/**
	 * Returns true if this is a partial match. Avoid using this function
	 * frequently since it instantiates the predicate on the fly.
	 * 
	 * @param other
	 * @return
	 */
	public boolean partialMatch(final TreeNode<T> other,
			final boolean requireAllChildren) {
		return partialMatch(other, new Predicate<NodePair<T>>() {
			@Override
			public boolean apply(final NodePair<T> arg) {
				return arg.fromNode.nodeData.equals(arg.toNode.nodeData);
			}
		}, requireAllChildren);
	}

	/**
	 * returns true if it partially matches the other tree. A partial match is
	 * defined when this node's children are a subset of the other's children
	 * and have matching data. Node data equality is defined by the given
	 * predicate.
	 * 
	 * @param other
	 * @param equalityComparator
	 * @param requireAllChildren
	 *            require to match all children (if a node has one)
	 * @return
	 */
	public boolean partialMatch(final TreeNode<T> other,
			final Predicate<NodePair<T>> equalityComparator,
			final boolean requireAllChildren) {
		if (!equalityComparator.apply(new NodePair<T>(this, other))) {
			return false;
		}

		if (nProperties() != other.nProperties()) {
			return false;
		}

		for (int i = 0; i < childrenProperties.size(); i++) {
			final List<TreeNode<T>> children = childrenProperties.get(i);
			final List<TreeNode<T>> otherChildren = other.childrenProperties
					.get(i);
			if (children.size() > otherChildren.size() && !requireAllChildren) {
				return false;
			} else if (requireAllChildren
					&& children.size() != otherChildren.size()
					&& children.size() > 0) {
				return false;
			}
			for (int j = 0; j < children.size(); j++) {
				if (!children.get(j).partialMatch(otherChildren.get(j),
						equalityComparator, requireAllChildren)) {
					return false;
				}
			}

		}

		return true;
	}

	/**
	 * Return an immutable copy of this the subtree rooted at this node.
	 * 
	 * @return
	 */
	public TreeNode<T> toImmutable() {
		final List<List<TreeNode<T>>> immutableProperties = Lists
				.newArrayList();
		for (int i = 0; i < childrenProperties.size(); i++) {
			final List<TreeNode<T>> immutableChildren = Lists.newArrayList();
			for (final TreeNode<T> child : childrenProperties.get(i)) {
				immutableChildren.add(child.toImmutable());
			}
			immutableProperties.add(immutableChildren);
		}
		return new TreeNode<T>(nodeData, immutableProperties);
	}

	@Override
	public String toString() {
		final StringBuffer buffer = new StringBuffer();
		treePrinterHelper(buffer, this, "", new Function<T, String>() {

			@Override
			public String apply(final T element) {
				return element.toString();
			}
		});
		return buffer.toString();
	}

	/**
	 * Use a functional to convert the data to a string.
	 * 
	 * @param toStringConverter
	 * @return
	 */
	public String toString(final Function<T, String> toStringConverter) {
		final StringBuffer buffer = new StringBuffer();
		treePrinterHelper(buffer, this, "", toStringConverter);
		return buffer.toString();
	}

	/**
	 * Helper recursive function for printing the tree.
	 * 
	 * @param buffer
	 * @param currentNode
	 * @param prefix
	 */
	public void treePrinterHelper(final StringBuffer buffer,
			final TreeNode<T> currentNode, final String prefix,
			final Function<T, String> dataToString) {
		buffer.append(prefix);
		if (currentNode != null) {
			buffer.append(dataToString.apply(currentNode.getData()));
			buffer.append('\n');
			for (int i = 0; i < currentNode.childrenProperties.size(); i++) {
				for (final TreeNode<T> child : currentNode.childrenProperties
						.get(i)) {
					treePrinterHelper(buffer, child, prefix
							+ SUB_NODE_STRING_PREFIX + "(" + i + ")",
							dataToString);
				}
			}
		} else {
			buffer.append("NULL\n");
		}
	}
}