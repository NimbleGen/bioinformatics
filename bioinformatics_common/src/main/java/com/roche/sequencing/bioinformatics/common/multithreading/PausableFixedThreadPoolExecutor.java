/*
 *    Copyright 2016 Roche NimbleGen Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.roche.sequencing.bioinformatics.common.multithreading;

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
	public PausableFixedThreadPoolExecutor(int numberOfThreads, String threadNamePrefix) {
		super(numberOfThreads, numberOfThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<Runnable>());
		exceptionListeners = new ArrayList<IExceptionListener>();

		isPaused = false;

		setThreadFactory(new NamePrependingThreadFactory(threadNamePrefix));
	}

	@Override
	protected void beforeExecute(Thread thread, Runnable runnable) {
		super.beforeExecute(thread, runnable);
		pauseLock.lock();
		try {
			while (isPaused) {
				unpaused.await();
			}
		} catch (InterruptedException ie) {
			thread.interrupt();
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
				exceptionListener.exceptionOccurred(runnable, throwable);
			}
		}
	}

	public void addExceptionListener(IExceptionListener exceptionListener) {
		exceptionListeners.add(exceptionListener);
	}

	public void clearExceptionListeners() {
		exceptionListeners.clear();
	}

	public void pause() {
		pauseLock.lock();
		try {
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	public void resume() {
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

	public void removeExceptionListener(IExceptionListener exceptionListener) {
		exceptionListeners.remove(exceptionListener);
	}
}
