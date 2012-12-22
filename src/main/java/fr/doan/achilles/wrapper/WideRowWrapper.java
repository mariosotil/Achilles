package fr.doan.achilles.wrapper;

import java.util.List;

import me.prettyprint.cassandra.service.ColumnSliceIterator;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import fr.doan.achilles.composite.factory.CompositeKeyFactory;
import fr.doan.achilles.dao.GenericMultiKeyWideRowDao;
import fr.doan.achilles.entity.metadata.PropertyMeta;
import fr.doan.achilles.entity.type.KeyValueIterator;
import fr.doan.achilles.entity.type.WideMap;
import fr.doan.achilles.helper.CompositeHelper;
import fr.doan.achilles.holder.KeyValue;
import fr.doan.achilles.holder.factory.KeyValueFactory;
import fr.doan.achilles.iterator.factory.IteratorFactory;

/**
 * WideMap
 * 
 * @author DuyHai DOAN
 * 
 */
public class WideRowWrapper<ID, K, V> implements WideMap<K, V>
{
	private ID id;
	private GenericMultiKeyWideRowDao<ID> dao;
	private PropertyMeta<K, V> wideMapMeta;

	private CompositeHelper helper = new CompositeHelper();
	private KeyValueFactory keyValueFactory = new KeyValueFactory();
	private IteratorFactory iteratorFactory = new IteratorFactory();
	private CompositeKeyFactory compositeKeyFactory = new CompositeKeyFactory();

	@Override
	public V get(K key)
	{
		Composite comp = compositeKeyFactory.createBaseForQuery(key);
		Object value = dao.getValue(id, comp);
		return wideMapMeta.getValue(value);
	}

	@Override
	public void insert(K key, V value)
	{
		Composite comp = compositeKeyFactory.createBaseForQuery(key);
		dao.setValue(id, comp, value);
	}

	@Override
	public void insert(K key, V value, int ttl)
	{
		Composite comp = compositeKeyFactory.createBaseForQuery(key);
		dao.setValue(id, comp, value, ttl);
	}

	@Override
	public List<KeyValue<K, V>> findRange(K start, K end, boolean reverse, int count)
	{
		return findRange(start, end, true, reverse, count);
	}

	@Override
	public List<KeyValue<K, V>> findRange(K start, K end, boolean inclusiveBounds, boolean reverse,
			int count)
	{

		return findRange(start, inclusiveBounds, end, inclusiveBounds, reverse, count);
	}

	@Override
	public List<KeyValue<K, V>> findRange(K start, boolean inclusiveStart, K end,
			boolean inclusiveEnd, boolean reverse, int count)
	{
		helper.checkBounds(wideMapMeta, start, end, reverse);

		Composite[] composites = compositeKeyFactory.createForQuery(start, inclusiveStart, end,
				inclusiveEnd, reverse);

		List<HColumn<Composite, Object>> hColumns = dao.findRawColumnsRange(id, composites[0],
				composites[1], reverse, count);

		return keyValueFactory.createFromColumnList(hColumns, wideMapMeta);
	}

	@Override
	public KeyValueIterator<K, V> iterator(K start, K end, boolean reverse, int count)
	{
		return iterator(start, end, true, reverse, count);
	}

	@Override
	public KeyValueIterator<K, V> iterator(K start, K end, boolean inclusiveBounds,
			boolean reverse, int count)
	{
		return iterator(start, inclusiveBounds, end, inclusiveBounds, reverse, count);
	}

	@Override
	public KeyValueIterator<K, V> iterator(K start, boolean inclusiveStart, K end,
			boolean inclusiveEnd, boolean reverse, int count)
	{

		Composite[] composites = compositeKeyFactory.createForQuery(start, inclusiveStart, end,
				inclusiveEnd, reverse);

		ColumnSliceIterator<ID, Composite, Object> columnSliceIterator = dao.getColumnsIterator(id,
				composites[0], composites[1], reverse, count);

		return iteratorFactory.createKeyValueIteratorForWideRow(columnSliceIterator, wideMapMeta);

	}

	@Override
	public void remove(K key)
	{

		Composite comp = compositeKeyFactory.createBaseForQuery(key);
		dao.removeColumn(id, comp);
	}

	@Override
	public void removeRange(K start, K end)
	{
		removeRange(start, end, true);
	}

	@Override
	public void removeRange(K start, K end, boolean inclusiveBounds)
	{
		removeRange(start, inclusiveBounds, end, inclusiveBounds);
	}

	@Override
	public void removeRange(K start, boolean inclusiveStart, K end, boolean inclusiveEnd)
	{
		helper.checkBounds(wideMapMeta, start, end, false);
		Composite[] composites = compositeKeyFactory.createForQuery(start, inclusiveStart, end,
				inclusiveEnd, false);
		dao.removeColumnRange(id, composites[0], composites[1]);
	}

	public void setId(ID id)
	{
		this.id = id;
	}

	public void setDao(GenericMultiKeyWideRowDao<ID> dao)
	{
		this.dao = dao;
	}

	public void setWideMapMeta(PropertyMeta<K, V> wideMapMeta)
	{
		this.wideMapMeta = wideMapMeta;
	}
}