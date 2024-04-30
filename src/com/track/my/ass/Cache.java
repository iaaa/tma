package com.track.my.ass;

import java.lang.System;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import android.util.Log;

import com.track.my.ass.Database;
import com.track.my.ass.view.Tile;
import com.track.my.ass.view.TiledMap;
import com.iaaa.Subscriber;

public class Cache
{
	final static String TAG = "Cache";

	static AtomicInteger ai = new AtomicInteger(0);
	// connection.setRequestProperty("Referer", "http://www.jguru.com/");
	// А это кеш собственной персоной:
	static int size = 0;// действительный размер кеша
	static Tile[] tiles = new Tile[512];   // todo: use Preferences
	static ThreadPool downloader = new ThreadPool();
	
	// get and create new, if still not present
	public static Tile get(final int x, final int y, final int z)
	throws InterruptedException
	{
		final int s = TiledMap.NumTiles[z];
		if (x < 0 || y < 0 || x >= s || y >= s)
			return new Tile();
		
		//Log.i(TAG, "get(" + x + ", " + y + ", " + z + "): " + ai.addAndGet(1));
		final long key = Database.zxy(x, y, z);
		
		int min = 0;
		int max = size;
		int mid = 0;
		// бинарный поиск,
		// не синхронизируем, потому как его вызов будет в любом случае синхронным
		while (min < max) {
			mid = min + ((max - min) / 2);
			if (key > tiles[mid].Position)
				min = mid + 1;
			else
				max = mid;
		}
		if (max < size && key == tiles[max].Position)	// нашли!
			return tiles[max];
		
		// не нашли, добавим
		Tile tile = new Tile(x, y, z, key);
		// а вот тут надо локнуться, потому как будет коллизия с find и clear
		synchronized (tiles) {
			System.arraycopy(tiles, max, tiles, max + 1, size - max);
			tiles[max] = tile;	// поместим новую текстуру в список
			
			size++;
			if (size == tiles.length) {	// отсортировать - выкинуть - отсортировать
				Log.i(TAG, "exceed cached size, reducing");
				sortByLastused(0, size - 1);
				size -= size / 3;	// треть нафиг!
				sortByPosition(0, size - 1);

				// принудительно почистим более не используемые ресурсы
				for (int i = size; i < tiles.length; i++) {
					if (tiles[i] != null)
						tiles[i].Free();
					tiles[i] = null;
				}
			}
		}

		// очередь переполнена?!
		while (begin == (end + 1) % queue.length) {
			synchronized (queue) {
				while (begin == (end + 1) % queue.length)
					queue.wait();
			}
		}
		queue[end] = key;
		//Log.i(TAG, "add(" + x + ", " + y + ", " + z + ")(=" + key + ") to " + end);
		synchronized (queue) {
			end = (end + 1) % queue.length;
			queue.notifyAll();	// разбудим поток загрузчика текстур, если он спал
		}
		return tile; 
	}

	/**
	 * Функция двоичного поиска (синхронизированного) тайла в кеше. 
	 * @param key
	 * @return null, если тайл не найден
	 */
	static final Tile find(long key)
	{
		int min = 0;
		int max;
		int mid = 0;
		synchronized (tiles) {
			max = size;

			while (min < max) {
				mid = min + ((max - min) / 2);
				if (key > tiles[mid].Position)
					min = mid + 1;
				else
					max = mid;
			}
			if (max < size && key == tiles[max].Position)
				return tiles[max];
		}
		return null;
	}
	
	// Тут лежит кольцевой список тайлов, ожидающих загрузки из БД
	//  список пуст, если end = begin -1 
	static volatile int begin = 0;	// начало списка
	static volatile int end = 0;	// конец списка
	static final long[] queue = new long[256]; // надеюсь, хватит
	
	static final void swap(final int i, final int j)
	{
		final
		Tile tmp = tiles[i];
		tiles[i] = tiles[j];
		tiles[j] = tmp;
	}
	
	static void sortByPosition(int min, int max)	// (от меньшего к большему)
	{
		int i = min;
		int j = max;
		long position = tiles[(min + max) / 2].Position;
		do {
			while ((tiles[i].Position < position) && (i < max)) i++;
			while ((position < tiles[j].Position) && (j > min)) j--;
			if (i <= j)
				swap(i++, j--);
		} while (i <= j);

		if (min < j) sortByPosition(min, j);
		if (i < max) sortByPosition(i, max);
	}

	static void sortByLastused(int min, int max)	// (от большего к меньшему)
	{
		int i = min;
		int j = max;
		long lastUsed = tiles[(min + max) / 2].Lastused;
		do {
			while ((tiles[i].Lastused > lastUsed) && (i < max)) i++;
			while ((lastUsed > tiles[j].Lastused) && (j > min)) j--;
			if (i <= j)
				swap(i++, j--);
		} while (i <= j);

		if (min < j) sortByLastused(min, j);
		if (i < max) sortByLastused(i, max);
	}
	

	static Subscriber Subscriber = null; 
	public static void Subscribe(Subscriber subscriber)
	{
		Subscriber = subscriber;
	}
	public static void Unsubscribe()
	{
		Subscriber = null;
	}
	public static void Knock()
	{
		if (Subscriber != null)
			Subscriber.Knock(true);
	}
	
	public static void update(int x, int y, int z)
	{
		try {
			Tile tile = get(x, y, z);
			if (tile == null)
				return;
			
			tile.Update();
		} catch (InterruptedException e) { } 
	}

	static {
		new Thread() {
			@Override
			public void run()
			{
				Thread.currentThread().setName("Cache Tile Loader");
				try {
					while (true) {
						while (begin == end) {
							synchronized (queue) {
								while (begin == end)
									queue.wait();
							}
						}
						
						long key = queue[begin];
						Tile bitmap = find(key);
//						if (bitmap != null)
//							Log.i("cache", "load(" + bitmap.x + ", " + bitmap.y + ", " + bitmap.z + ")(" + key + ")");
//						else
//							Log.i("cache", "no (" + key + ") more required");
						if (bitmap != null)		// а нужен ли еще этот имидж (вдруг сделали clear)?
							if (bitmap.map == Database.Generation()) // если все та же база даных
								bitmap.Load();
						queue[begin] = -1;	// на всякий случай...
						
						synchronized (queue) {
							begin = (begin + 1) % queue.length;
							queue.notifyAll();	// разбудим поток загрузчика текстур, если он спал
						}
						Knock();
					}
				} catch (InterruptedException e){}
			}
		}.start();
	}

	public static void Clear()
	{
		synchronized (tiles) {
			for (int i = 0; i < size; i++)
				tiles[i] = null;
			size = 0;
		}
	}
	
	public static void Download(Tile tile)
	{
		downloader.Download(tile);
	}
}

class ThreadPool
{
	public boolean Download(Tile tile)
	{
		for (Element q : queue)
			if (q.compareAndSet(EMPTY, READY))
				return q.set(tile);
		// упс, массив переполнен!
		// todo: обработать этот случай
		return false;
	}
	
	
	final static int DOWNLOADERS = 4;
	
	final static int EMPTY = 0;
	final static int READY = 1;
	final static int BUZY = 2;
	
	@SuppressWarnings("serial")
	class Element extends AtomicInteger {
		public Tile tile;
		public Element(int value) { super(value); }
		public boolean set(Tile tile) {
			this.tile = tile;
			available.release();
			return true;
		}
	}
	
	Semaphore available = new Semaphore(0);
	@SuppressWarnings("serial")
	List<Element> queue = new ArrayList<Element>() {{
		for (int i = 0; i < 128; i++)
			add(new Element(0));
	}};
	
	
	ThreadPool() {
		for (int i = 0; i < DOWNLOADERS; i++)
			new Downloader().start();
	}
	class Downloader extends Thread
	{
		@Override
		public void run() {
			try {
				Thread.currentThread().setName("Tile Downloader");
				available.acquire();
				while (true) {
					for (Element q : queue) {
						if (q.compareAndSet(READY, BUZY)) {
							q.tile.Download();
							q.set(EMPTY);
							Cache.Knock();
							
							available.acquire();
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
