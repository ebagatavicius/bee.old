package com.butent.bee.server.concurrency;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.Subscribe;

import com.butent.bee.server.Config;
import com.butent.bee.server.Invocation;
import com.butent.bee.server.modules.ParamHolderBean;
import com.butent.bee.server.modules.ParameterEvent;
import com.butent.bee.server.modules.ParameterEventHandler;
import com.butent.bee.shared.Assert;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
public class ConcurrencyBean {

  public interface HasTimerService {
    TimerService getTimerService();
  }

  public abstract static class AsynchronousRunnable implements Runnable {

    public String getId() {
      return null;
    }

    public long getTimeout() {
      return TimeUtils.MILLIS_PER_HOUR;
    }

    public void onError() {
    }
  }

  private static class Worker extends FutureTask<Void> {

    private long start;
    private final AsynchronousRunnable runnable;

    public Worker(AsynchronousRunnable runnable) {
      super(runnable, null);
      this.runnable = runnable;
    }

    public String getId() {
      String id = runnable.getId();
      return BeeUtils.isEmpty(id) ? runnable.toString()
          : BeeUtils.joinWords(id, Integer.toHexString(hashCode()));
    }

    @Override
    public void run() {
      start = System.currentTimeMillis();
      logger.info("Started:", getId());
      super.run();
    }

    @Override
    protected void done() {
      boolean ok = true;

      try {
        get();
      } catch (Exception e) {
        if (!(e instanceof CancellationException)) {
          logger.error(e, getId());
        }
        ok = false;
      }
      if (ok) {
        logger.info("Ended:", getId(), TimeUtils.elapsedSeconds(started()));
      } else {
        if (started() > 0) {
          logger.info("Canceled:", getId(), TimeUtils.elapsedSeconds(started()));
        } else {
          logger.info("Rejected:", getId());
        }
        runnable.onError();
      }
    }

    private long started() {
      return start;
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(ConcurrencyBean.class);

  private final Map<String, Worker> asyncThreads = new HashMap<>();

  private Multimap<String, Class<? extends HasTimerService>> calendarRegistry;
  private Multimap<String, Class<? extends HasTimerService>> intervalRegistry;

  private final ReentrantLock lock = new ReentrantLock();

  @EJB
  ParamHolderBean prm;
  @Resource
  ManagedExecutorService executor;
  @Resource
  UserTransaction utx;

  public void asynchronousCall(AsynchronousRunnable runnable) {
    Assert.notNull(runnable);
    String id = runnable.getId();
    Worker worker = BeeUtils.isEmpty(id) ? null : asyncThreads.get(id);

    if (worker != null) {
      if (!worker.isDone()) {
        if (BeeUtils.isMore(System.currentTimeMillis() - worker.started(), runnable.getTimeout())) {
          worker.cancel(true);
        } else {
          logger.info("Running:", worker.getId(), TimeUtils.elapsedSeconds(worker.started()));
          runnable.onError();
          return;
        }
      }
      asyncThreads.remove(id);
    }
    Worker newWorker = new Worker(runnable);

    try {
      executor.execute(newWorker);

      if (!BeeUtils.isEmpty(id)) {
        asyncThreads.put(id, newWorker);
      }
    } catch (Exception e) {
      logger.error(e);
      newWorker.cancel(true);
    }
  }

  public <T extends HasTimerService> void createCalendarTimer(Class<T> handler, String parameter) {
    if (calendarRegistry == null) {
      calendarRegistry = HashMultimap.create();

      prm.registerParameterEventHandler(new ParameterEventHandler() {
        @Subscribe
        public void createTimers(ParameterEvent event) {
          String param = event.getParameter();

          if (calendarRegistry.containsKey(param)) {
            ConcurrencyBean concurrency = Invocation.locateRemoteBean(ConcurrencyBean.class);

            if (concurrency != null) {
              for (Class<? extends HasTimerService> clazz : calendarRegistry.get(param)) {
                concurrency.createCalendarTimer(clazz, param);
              }
            }
          }
        }
      });
    }
    if (!calendarRegistry.containsEntry(parameter, handler)) {
      calendarRegistry.put(parameter, handler);
    }
    TimerService timerService = removeTimer(handler, parameter);
    String hours = prm.getText(parameter);

    if (!BeeUtils.isEmpty(hours)) {
      try {
        Timer timer = timerService.createCalendarTimer(new ScheduleExpression().hour(hours),
            new TimerConfig(parameter, false));

        logger.info("Created", NameUtils.getClassName(handler), parameter, "timer on hours [",
            hours, "] starting at", timer.getNextTimeout());
      } catch (IllegalArgumentException ex) {
        logger.error(ex);
      }
    }
  }

  public <T extends HasTimerService> void createIntervalTimer(Class<T> handler, String parameter) {
    if (intervalRegistry == null) {
      intervalRegistry = HashMultimap.create();

      prm.registerParameterEventHandler(new ParameterEventHandler() {
        @Subscribe
        public void createTimers(ParameterEvent event) {
          String param = event.getParameter();

          if (intervalRegistry.containsKey(param)) {
            ConcurrencyBean concurrency = Invocation.locateRemoteBean(ConcurrencyBean.class);

            if (concurrency != null) {
              for (Class<? extends HasTimerService> clazz : intervalRegistry.get(param)) {
                concurrency.createIntervalTimer(clazz, param);
              }
            }
          }
        }
      });
    }
    if (!intervalRegistry.containsEntry(parameter, handler)) {
      intervalRegistry.put(parameter, handler);
    }
    TimerService timerService = removeTimer(handler, parameter);
    Integer minutes = prm.getInteger(parameter);

    if (BeeUtils.isPositive(minutes)) {
      try {
        Timer timer = timerService.createIntervalTimer(minutes * TimeUtils.MILLIS_PER_MINUTE,
            minutes * TimeUtils.MILLIS_PER_MINUTE, new TimerConfig(parameter, false));

        logger.info("Created", NameUtils.getClassName(handler), parameter, "timer every", minutes,
            "minutes starting at", timer.getNextTimeout());
      } catch (IllegalArgumentException ex) {
        logger.error(ex);
      }
    }
  }

  public boolean isParameterTimer(Timer timer, Object parameter) {
    if (!Config.isInitialized()) {
      return false;
    }
    Assert.noNulls(timer, parameter);
    return Objects.equals(timer.getInfo(), parameter);
  }

  @Lock(LockType.READ)
  public void synchronizedCall(Runnable runnable) {
    Assert.notNull(runnable);
    lock.lock();

    try {
      utx.begin();
      runnable.run();
      utx.commit();
    } catch (Exception ex) {
      logger.error(ex);

      try {
        utx.rollback();
      } catch (SystemException ex2) {
        logger.error(ex2);
      }
    } finally {
      lock.unlock();
    }
  }

  private <T extends HasTimerService> TimerService removeTimer(Class<T> handler,
      String parameter) {
    T bean = Assert.notNull(Invocation.locateRemoteBean(handler));
    TimerService timerService = Assert.notNull(bean.getTimerService());

    for (Timer timer : timerService.getTimers()) {
      if (isParameterTimer(timer, parameter)) {
        timer.cancel();
        logger.info("Removed", NameUtils.getClassName(handler), parameter, "timer");
        break;
      }
    }
    return timerService;
  }
}
