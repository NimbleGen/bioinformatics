package com.roche.multithreading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PausableFixedThreadPoolExecutor extends ThreadPoolExecutor {

	private boolean isPaused;
	private ReentrantLock pauseLock = new ReentrantLock();
	private Condition unpaused = pauseLock.newCondition();
	private List<IExceptionListener> exceptionListeners;

	/**
	 * Creates a pausable thread pool executor that is identical to the ThreadPoolExecutor created when Executors.newFixedThreadPool(int nThreads) is called.
	 * 
	 * @param numberOfThreads
	 */
	public PausableFixedThreadPoolExecutor(int numberOfThreads) {
		super(numberOfThreads, numberOfThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>());
		exceptionListeners = new ArrayList<IExceptionListener>();

		isPaused = false;
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		pauseLock.lock();
		try {
			while (isPaused)
				unpaused.await();
		} catch (InterruptedException ie) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}
	}

	@Override
	protected void afterExecute(Runnable runnable, Throwable throwable) {
		super.afterExecute(runnable, throwable);
		if (throwable == null && runnable instanceof Future<?>) {
			try {
				Future<?> future = (Future<?>) runnable;
				if (!future.isCancelled() && future.isDone()) {
					future.get();
				}
			} catch (CancellationException ce) {
				throwable = ce;
			} catch (ExecutionException ee) {
				throwable = ee.getCause();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
		if (throwable != null) {
			for (IExceptionListener exceptionListener : exceptionListeners) {
				exceptionListener.exceptionOccurred(throwable);
			}
		}
	}

	public void addExceptionListener(IExceptionListener exceptionListener) {
		exceptionListeners.add(exceptionListener);
	}

	public void clearExceptionListeners() {
		exceptionListeners.clear();
	}

	private void pause() {
		pauseLock.lock();
		try {
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	private void resume() {
		pauseLock.lock();
		try {
			isPaused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}

	public void setPaused(boolean setToPaused) {
		if (setToPaused != isPaused) {
			if (setToPaused) {
				pause();
				System.out.println("pausing");
			} else {
				resume();
				System.out.println("resuming");
			}
		}
	}

	public boolean isPaused() {
		return isPaused;
	}
}
