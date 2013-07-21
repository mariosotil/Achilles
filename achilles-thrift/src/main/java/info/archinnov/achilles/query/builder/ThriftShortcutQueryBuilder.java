package info.archinnov.achilles.query.builder;

import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.operations.ThriftQueryExecutor;
import java.util.Iterator;
import java.util.List;

/**
 * ThriftShortcutQueryBuilder
 * 
 * @author DuyHai DOAN
 * 
 */
public class ThriftShortcutQueryBuilder<T> extends AbstractQueryBuilder<T> {

    public ThriftShortcutQueryBuilder(ThriftQueryExecutor queryExecutor, Class<T> entityClass,
            EntityMeta meta, Object partitionKey) {
        super(queryExecutor, entityClass, meta);
        super.partitionKey(partitionKey);
    }

    /**
     * Set from clustering components<br/>
     * <br/>
     * 
     * @param clusteringComponents
     *            From clustering components
     * 
     * @return ThriftFromClusteringsBuilder<T>
     */
    public ThriftFromClusteringsBuilder<T> fromClusterings(Object... clusteringComponents)
    {
        return new ThriftFromClusteringsBuilder<T>(queryExecutor, entityClass, meta, partitionKey,
                clusteringComponents);
    }

    /**
     * Set to clustering components<br/>
     * <br/>
     * 
     * @param clusteringComponents
     *            To clustering components
     * 
     * @return ThriftToClusteringsBuilder<T>
     */
    public ThriftToClusteringsBuilder<T> toClusterings(Object... clusteringComponents)
    {
        return new ThriftToClusteringsBuilder<T>(queryExecutor, entityClass, meta, clusteringComponents,
                clusteringComponents);
    }

    /**
     * Get first n matching entities<br/>
     * <br/>
     * 
     * @param n
     *            first n matching entities
     * 
     * @return list of found entities or empty list
     */
    public List<T> get(int n)
    {
        return super.get(n);
    }

    /**
     * Get first matching entity, using ASCENDING order<br/>
     * <br/>
     * 
     * @param clusteringComponents
     *            optional clustering components for filtering
     * 
     * @return first matching entity, filtered by provided clustering components if any, or null if no matching entity
     *         is found
     */
    public T getFirstOccurence(Object... clusteringComponents)
    {
        return super.getFirstOccurence(clusteringComponents);
    }

    /**
     * Get first n matching entities, using ASCENDING order<br/>
     * <br/>
     * 
     * @param n
     *            first n matching entities
     * 
     * @param clusteringComponents
     *            optional clustering components for filtering
     * 
     * @return list of n first matching entities, filtered by provided clustering components if any, or empty list
     */
    public List<T> getFirst(int n, Object... clusteringComponents)
    {
        return super.getFirst(n, clusteringComponents);
    }

    /**
     * Get last matching entity, using ASCENDING order<br/>
     * <br/>
     * 
     * @param clusteringComponents
     *            optional clustering components for filtering
     * 
     * @return last matching entity, filtered by provided clustering components if any, or null if no matching entity
     *         is found
     */
    public T getLastOccurence(Object... clusteringComponents)
    {
        return super.getLastOccurence(clusteringComponents);
    }

    /**
     * Get last n matching entities, using ASCENDING order<br/>
     * <br/>
     * 
     * @param n
     *            last n matching entities
     * 
     * @param clusteringComponents
     *            optional clustering components for filtering
     * 
     * @return list of last n matching entities, filtered by provided clustering components if any, or empty list
     */
    public List<T> getLast(int n, Object... clusteringComponents)
    {
        return super.getLast(n, clusteringComponents);
    }

    /**
     * Get entities iterator, using ASCENDING order<br/>
     * <br/>
     * 
     * @param clusteringComponents
     *            optional clustering components for filtering
     * 
     * @return iterator on found entities
     */
    public Iterator<T> iterator(Object... clusteringComponents)
    {
        return super.iteratorWithComponents(clusteringComponents);
    }

    /**
     * Get entities iterator, using ASCENDING order<br/>
     * <br/>
     * 
     * @param batchSize
     *            batch loading size for iterator
     * 
     * @param clusteringComponents
     *            optional clustering components for filtering
     * 
     * @return iterator on found entities
     */
    public Iterator<T> iterator(int batchSize, Object... clusteringComponents)
    {
        return super.iteratorWithComponents(batchSize, clusteringComponents);
    }

    /**
     * Remove first n entities, using ASCENDING order<br/>
     * <br/>
     * 
     * @param n
     *            first n entities
     */
    public void remove(int n)
    {
        super.remove(n);
    }

    /**
     * Remove first matching entity, using ASCENDING order<br/>
     * <br/>
     * 
     * @param clusteringComponents
     *            optional clustering components for filtering
     */
    public void removeFirstOccurence(Object... clusteringComponents)
    {
        super.removeFirstOccurence(clusteringComponents);
    }

    /**
     * Remove first n matching entities, using ASCENDING order<br/>
     * <br/>
     * 
     * @param n
     *            first n matching entities
     * 
     * @param clusteringComponents
     *            optional clustering components for filtering
     */
    public void removeFirst(int n, Object... clusteringComponents)
    {
        super.removeFirst(n, clusteringComponents);
    }

    /**
     * Remove last matching entity, using ASCENDING order<br/>
     * <br/>
     * 
     * @param clusteringComponents
     *            optional clustering components for filtering
     */
    public void removeLastOccurence(Object... clusteringComponents)
    {
        super.removeLastOccurence(clusteringComponents);
    }

    /**
     * Remove last n matching entities, using ASCENDING order<br/>
     * <br/>
     * 
     * @param n
     *            last n matching entities
     * 
     * @param clusteringComponents
     *            optional clustering components for filtering
     */
    public void removeLast(int n, Object... clusteringComponents)
    {
        super.removeLast(n, clusteringComponents);
    }
}