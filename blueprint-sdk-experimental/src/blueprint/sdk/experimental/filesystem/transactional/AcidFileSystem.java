/*
 License:

 blueprint-sdk is licensed under the terms of Eclipse Public License(EPL) v1.0
 (http://www.eclipse.org/legal/epl-v10.html)


 Distribution:

 Repository - https://github.com/lempel/blueprint-sdk.git
 Blog - http://lempel.egloos.com
 */

package blueprint.sdk.experimental.filesystem.transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import blueprint.sdk.core.concurrent.lock.timestamped.TimestampedLock;
import blueprint.sdk.core.filesystem.FileSystem;
import blueprint.sdk.core.filesystem.GenericFileSystem;
import blueprint.sdk.util.Terminatable;
import blueprint.sdk.util.jvm.shutdown.TerminatableThread;
import blueprint.sdk.util.jvm.shutdown.Terminator;

/**
 * ACIDic file system with transcational operations.
 * 
 * @author Sangmin Lee
 * @since 2014. 4. 23.
 */
public class AcidFileSystem extends GenericFileSystem {
	// TODO Not durable yet
	//
	// To achieve durability:
	//
	//
	// - Temporary file first:
	//
	// 1. Make every changes to temporary files and then rename it to desired
	// path.
	// 2. Re-implement Transaction class according to this new policy.
	// 3. To to '2', Must implement ProxyFile to link between original file and
	// temporary file which is a to-be commited file.
	// 4. To do '2' and '3', probably need to implement some TOC mechanism.
	//
	//
	// - Keep track of un-committed or un-rollbacked changes.
	//
	// 1. Add a filename extenstion as a directive for uncommitted files.
	// But, what about rollback?
	// 2. Some sort of summary/log files for unexecuted changes.

	/** logger */
	private static final Logger L = Logger.getLogger(AcidFileSystem.class);

	/** Monitor Objects of currently open files (key: path, value: monitor) */
	protected Map<String, TimestampedLock> openFiles = new ConcurrentHashMap<String, TimestampedLock>();
	/** lock for openFiles */
	protected ReentrantLock openFilesLock = new ReentrantLock();

	/**
	 * TTL(Time-To-Live) for {@link AcidFileSystem#openFiles} in millisecounds.<br/>
	 * Default value is 10 minutes.
	 */
	protected static long EVICTOR_TTL = 10 * 60 * 1000;

	/** maximum evictor interval (10 minutes) */
	protected static final long MAX_INTERVAL = 10 * 60 * 1000;
	/** minimun evictor interval (5 seconds) */
	protected static final long MIN_INTERVAL = 5 * 1000;

	/**
	 * Peroidic evictor thread for openFiles
	 */
	// XXX Is there anyway to eliminate EvictorThread?
	protected TerminatableThread evictor = new TerminatableThread() {
		@Override
		public void run() {
			running = true;

			long interval = EVICTOR_TTL / 2;
			if (interval >= MAX_INTERVAL) {
				interval = MAX_INTERVAL;
			} else if (interval <= MIN_INTERVAL) {
				interval = MIN_INTERVAL;
			}

			while (isValid() && !isTerminated()) {
				try {
					sleep(interval);
				} catch (InterruptedException ignored) {
				}

				long limit = System.currentTimeMillis() - EVICTOR_TTL;
				openFilesLock.lock();
				try {
					Set<String> keySet = openFiles.keySet();
					for (String key : keySet) {
						TimestampedLock wrapper = openFiles.get(key);
						// evict timed-out and unlocked mutex
						if (wrapper.getTimestamp() <= limit && !wrapper.isLocked()) {
							openFiles.remove(key);
						}
					}
				} finally {
					openFilesLock.unlock();
				}
			}

			terminated = true;
		}
	};

	public AcidFileSystem() {
		evictor.start();
	}

	/**
	 * Gets a lock for specified path
	 * 
	 * @param path
	 *            file path
	 * @return existing lock or new lock
	 */
	protected TimestampedLock getLock(String path) {
		if (path == null) {
			throw new NullPointerException("specified path is null");
		}

		TimestampedLock result = null;

		openFilesLock.lock();
		try {
			result = openFiles.get(path);
			if (result == null) {
				result = new TimestampedLock(true);
				openFiles.put(path, result);
			}
		} finally {
			openFilesLock.unlock();
		}

		return result;
	}

	// XXX maybe I have to override exists() to check locks too.

	@Override
	public boolean deleteFile(String path) {
		if (path == null) {
			throw new NullPointerException("specified path is null");
		}

		boolean result = false;

		if (exists(path)) {
			TimestampedLock monitor = getLock(path);
			monitor.lock();
			try {
				result = doDelete(path);
			} finally {
				monitor.unlock();
			}
		}

		return result;
	}

	/**
	 * Primitive delete operation
	 * 
	 * @param path
	 *            file path
	 * @return true if and only if the file or directory is successfully deleted
	 */
	private boolean doDelete(String path) {
		return super.deleteFile(path);
	}

	@Override
	public boolean renameFile(String orgPath, String newPath) {
		if (orgPath == null || newPath == null) {
			throw new NullPointerException("at least one of specified path is null");
		}

		boolean result = false;

		if (!orgPath.equals(newPath)) {
			TimestampedLock orgMtx;
			TimestampedLock newMtx;

			openFilesLock.lock();
			try {
				orgMtx = getLock(orgPath);
				newMtx = getLock(newPath);
			} finally {
				openFilesLock.unlock();
			}

			newMtx.lock();
			orgMtx.lock();
			try {
				result = doRename(orgPath, newPath);
			} finally {
				newMtx.unlock();
				orgMtx.unlock();
			}
		}

		return result;
	}

	/**
	 * Primitive rename opeartion
	 * 
	 * @param orgPath
	 *            file path
	 * @param newPath
	 *            new path
	 * @return true if and only if the renaming succeeded
	 */
	private boolean doRename(String orgPath, String newPath) {
		// can't rename if newPath is currently opened
		return super.renameFile(orgPath, newPath);
	}

	@Override
	public byte[] readFile(String path) throws IOException {
		if (path == null) {
			throw new NullPointerException("specified path is null");
		}

		byte[] result = null;

		TimestampedLock monitor = getLock(path);
		monitor.lock();
		try {
			result = doRead(path);
		} finally {
			monitor.unlock();
		}

		return result;
	}

	/**
	 * Primitive read operation
	 * 
	 * @param path
	 *            file path
	 * @return File's contents
	 * @throws IOException
	 *             Can't access given path
	 */
	private byte[] doRead(String path) throws IOException {
		return super.readFile(path);
	}

	@Override
	public void writeToFile(String path, byte[] contents, boolean append) throws IOException {
		if (path == null) {
			throw new NullPointerException("specified path is null");
		}

		TimestampedLock monitor = getLock(path);
		monitor.lock();
		try {
			doWrite(path, contents, append);
		} catch (IOException e) {
			throw e;
		} finally {
			monitor.unlock();
		}
	}

	/**
	 * Primitive write operation
	 * 
	 * @param path
	 *            file path
	 * @param contents
	 *            file's contents
	 * @param append
	 *            true for append to current contents, false for create a new
	 *            file
	 * @throws IOException
	 *             Can't access given path
	 */
	private void doWrite(String path, byte[] contents, boolean append) throws IOException {
		super.writeToFile(path, contents, append);
	}

	/**
	 * Create a new transaction
	 * 
	 * @return tranasctional interface
	 */
	public Transaction newTransaction() {
		return new Transaction(this);
	}

	/**
	 * Transactional interface for {@link AcidFileSystem}.<br/>
	 * <br/>
	 * <b>Transaction is not designed for concurrent access, so use as an
	 * instance variable.</b><br/>
	 * 
	 * @author Sangmin Lee
	 * @since 2014. 5. 7.
	 */
	public class Transaction extends FileSystem implements Terminatable {
		boolean running = true;

		AcidFileSystem filesystem;

		/**
		 * Map of {@link TimestampedLock} owned this transaction
		 */
		Map<String, TimestampedLock> locks = new ConcurrentHashMap<String, TimestampedLock>();

		/** Update actions */
		List<Action> actions = new ArrayList<Action>();

		/**
		 * @param filesystem
		 */
		private Transaction(AcidFileSystem filesystem) {
			this.filesystem = filesystem;

			Terminator.getInstance().register(this);
		}

		@Override
		public boolean isValid() {
			return running;
		}

		@Override
		public boolean isTerminated() {
			return !running;
		}

		@Override
		public void terminate() {
			running = false;

			// forced termination. must rollback everything.
			rollback();
		}

		@Override
		public void dispose() {
			terminate();
		}
		

		/**
		 * reserve a write lock for specified path
		 * 
		 * @param path
		 *            file path to lock
		 * @param requester
		 *            requester of lock
		 */
		private void reserveLock(String path) {
			// FIXME of monitor is already locked by other transaction, waiting
			// for acqusition may result in starvation.

			TimestampedLock lock = filesystem.getLock(path);
			lock.lock();

			L.debug(Thread.currentThread().hashCode() + " lock acquired - " + path);

			locks.put(path, lock);
		}

		/**
		 * release all locks owned by this transaction
		 */
		private void releaseLocks() {
			{
				// Collection<ReentrantStampedMutex> col = locks.values();
				// for (ReentrantStampedMutex lock : col) {
				// lock.unlock();
				// }
				// locks.clear();
			}
			{
				Set<String> keyset = locks.keySet();
				for (String key : keyset) {
					locks.get(key).unlock();

					L.debug(Thread.currentThread().hashCode() + " lock released - " + key);
				}
				locks.clear();
			}
		}

		/**
		 * @param path
		 *            original path
		 * @return temporary backup path for specified path
		 */
		private String toTempPath(String path) {
			String result = path;

			// FIXME must generate unique name every time
			// ---- multiple temp files for a same file can be created during
			// same transation

			if (path != null) {
				result = path + ".temp";
			}

			return result;
		}

		/**
		 * Rollback current transaction
		 */
		public void rollback() {
			for (Action action : actions) {
				action.rollback();
			}

			actions.clear();
			releaseLocks();

			Terminator.getInstance().unregister(this);
		}

		/**
		 * Commit current transaction
		 */
		public void commit() {
			for (Action action : actions) {
				action.commit();
			}

			actions.clear();
			releaseLocks();

			Terminator.getInstance().unregister(this);
		}

		@Override
		public boolean exists(String path) throws IOException {
			return filesystem.exists(path);
		}

		@Override
		public boolean deleteFile(String path) throws IOException {
			boolean result = false;

			if (isValid()) {
				String tempPath = toTempPath(path);

				reserveLock(path);
				// reserve temp file too
				reserveLock(tempPath);

				result = filesystem.doRename(path, tempPath);

				Action action = new Action();
				// commit: delete temp path
				action.commit.add(new Operation(Operation.DELETE, tempPath, null));
				// rollback: rename to org path
				action.rollback.add(new Operation(Operation.RENAME, tempPath, path));
				actions.add(action);
			}

			return result;
		}

		@Override
		public boolean renameFile(String orgPath, String newPath) throws IOException {
			boolean result = false;

			if (isValid()) {
				reserveLock(orgPath);
				reserveLock(newPath);

				result = filesystem.doRename(orgPath, newPath);

				Action action = new Action();
				// commit: no-op
				// rollback: rename back
				action.rollback.add(new Operation(Operation.RENAME, newPath, orgPath));
				actions.add(action);
			}

			return result;
		}

		@Override
		public byte[] readFile(String path) throws IOException {
			reserveLock(path);

			return filesystem.doRead(path);
		}

		@Override
		public void writeToFile(String path, byte[] contents, boolean append) throws IOException {
			if (isValid()) {
				String tempPath = toTempPath(path);

				reserveLock(path);
				// reserve temp file too
				reserveLock(tempPath);

				Action action = new Action();

				if (append) {
					// create a backup
					filesystem.writeToFile(tempPath, filesystem.readFile(path), false);
					// append
					filesystem.doWrite(path, contents, true);

					// commit: delete backup
					action.rollback.add(new Operation(Operation.DELETE, tempPath, null));
					// rollback: delete new file and rename backup to original
					action.commit.add(new Operation(Operation.DELETE, path, null));
					action.commit.add(new Operation(Operation.RENAME, tempPath, path));
				} else {
					// write to file
					filesystem.doWrite(path, contents, append);

					// commit: no-op
					// rollback: delete new file
					action.rollback.add(new Operation(Operation.DELETE, path, null));
				}

				actions.add(action);
			}
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		
		evictor.terminate();
	}

	/**
	 * Update action for a file I/O.<br/>
	 * Each action has lists of operations for commit and rollback.<br/>
	 * 
	 * @author Sangmin Lee
	 * @since 2014. 5. 12.
	 */
	class Action {
		/** Operations to do a commit */
		List<Operation> commit = new ArrayList<Operation>();
		/** Operations to do a rollback */
		List<Operation> rollback = new ArrayList<Operation>();

		/**
		 * Do commit.
		 */
		public void commit() {
			Operator oper = new Operator();

			for (Operation op : commit) {
				L.debug(Thread.currentThread().hashCode() + " commit - " + op.operation + ": " + op.source + " -> "
						+ op.target);

				oper.execute(op);
			}
		}

		/**
		 * Do rollback.
		 */
		public void rollback() {
			Operator oper = new Operator();

			for (Operation op : commit) {
				L.debug(Thread.currentThread().hashCode() + " rollback - " + op.operation + ": " + op.source + " -> "
						+ op.target);

				oper.execute(op);
			}
		}
	}

	/**
	 * A file operation for commit/rollback.<br/>
	 * Currently, delete/rename operations are adequate for commit/rollback.<br/>
	 * 
	 * @author Sangmin Lee
	 * @since 2014. 5. 12.
	 */
	class Operation {
		/** delete source */
		static final int DELETE = 1;
		/** rename source as target */
		static final int RENAME = 2;

		/** file operation */
		int operation;
		/** source file path */
		String source;
		/** target file path */
		String target;

		/**
		 * @param operation
		 *            operation type
		 * @param source
		 *            source file path (can't be null)
		 * @param target
		 *            target file path (can't be null for rename)
		 */
		public Operation(int operation, String source, String target) {
			switch (operation) {
			case DELETE:
			case RENAME:
				break;
			default:
				throw new IllegalArgumentException("Unsupported operation - " + operation);
			}

			if (source == null) {
				throw new NullPointerException("source path can't be null");
			} else if (operation == RENAME && target == null) {
				throw new NullPointerException("target path can't be null for rename operation");
			}

			this.operation = operation;
			this.source = source;
			this.target = target;
		}
	}

	/**
	 * Executes an {@link Operation}
	 * 
	 * @author Sangmin Lee
	 * @since 2014. 5. 12.
	 */
	class Operator {
		// TODO Make it as a fundamental class for every FileSystem descendants,
		// only if possible...

		/**
		 * Execute an operation
		 * 
		 * @param op
		 *            operation to execute
		 */
		public void execute(Operation op) {
			switch (op.operation) {
			case Operation.DELETE:
				break;
			case Operation.RENAME:
				break;
			default:
				throw new IllegalArgumentException("Unsupported operation - " + op.operation);
			}
		}
	}
}