package net.sf.l2j.gameserver.taskmanager;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.pool.ThreadPool;

import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Monster;

/**
 * Destroys {@link Creature} corpse after specified time.
 */
public final class DecayTaskManager implements Runnable
{
	private static final Map<Creature, Long> DECAY_SCHEDULES = new ConcurrentHashMap<>();
	private static boolean _working = false;

	protected DecayTaskManager()
	{
		// Run task each second.
		ThreadPool.scheduleAtFixedRate(this, 0, 1000);
	}

	@Override
	public final void run()
	{
		if (_working)
			return;

		_working = true;

		final long time = System.currentTimeMillis();
		for (Entry<Creature, Long> entry : DECAY_SCHEDULES.entrySet())
		{
			if (time > entry.getValue().longValue())
			{
				final Creature creature = entry.getKey();
				DECAY_SCHEDULES.remove(creature);
				creature.onDecay();
			}
		}

		_working = false;
	}

	public final Long get(Creature creature)
	{
		return DECAY_SCHEDULES.get(creature);
	}

	/**
	 * Adds a {@link Creature} to the {@link DecayTaskManager} with additional interval.
	 * @param creature : The {@link Creature} to be added.
	 * @param interval : Interval in seconds, after which the decay task is triggered.
	 */
	public final void add(Creature creature, int interval)
	{
		// If character is a Monster.
		if (creature instanceof Monster)
		{
			final Monster monster = ((Monster) creature);

			// If Monster is spoiled or seeded, double the corpse delay.
			if (monster.getSpoilState().isSpoiled() || monster.getSeedState().isSeeded())
				interval *= 2;
		}

		DECAY_SCHEDULES.put(creature, System.currentTimeMillis() + interval * 1000);
	}

	/**
	 * Removes the {@link Creature} passed as parameter from the {@link DecayTaskManager}.
	 * @param creature : The {@link Creature} to be removed.
	 * @return True if an entry was successfully removed or false otherwise.
	 */
	public final boolean cancel(Creature creature)
	{
		return DECAY_SCHEDULES.remove(creature) != null;
	}

	public static final DecayTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static final class SingletonHolder
	{
		protected static final DecayTaskManager INSTANCE = new DecayTaskManager();
	}
}