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
import com.butent.bee.shared.Pair;
import com.butent.bee.shared.logging.BeeLogger;
import com.butent.bee.shared.logging.LogUtils;
import com.butent.bee.shared.time.TimeUtils;
import com.butent.bee.shared.utils.BeeUtils;
import com.butent.bee.shared.utils.NameUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;

@Singleton
public class ConcurrencyBean {

  public interface HasTimerService {
    TimerService getTimerService();
  }

  public abstract static class AsynchronousRunnable implements Runnable {
    public abstract String getId();

    public long getTimeout() {
      return TimeUtils.MILLIS_PER_HOUR;
    }

    public void onError() {
    }
  }

  private static final BeeLogger logger = LogUtils.getLogger(ConcurrencyBean.class);

  private final Map<String, Pair<Future<?>, Long>> asyncThreads = new HashMap<>();

  private Multimap<String, Class<? extends HasTimerService>> calendarRegistry;
  private Multimap<String, Class<? extends HasTimerService>> intervalRegistry;

  @EJB
  ParamHolderBean prm;
  @Resource
  ManagedExecutorService executor;

  public void asynchronousCall(AsynchronousRunnable runnable) {
    Assert.notNull(runnable);
    String id = Assert.notEmpty(runnable.getId());
    Pair<Future<?>, Long> pair = asyncThreads.get(id);

    if (pair != null) {
      Future<?> future = pair.getA();
      boolean isTimedOut = BeeUtils.isMore(System.currentTimeMillis() - pair.getB(),
          runnable.getTimeout());

      if (!future.isDone() && (!isTimedOut || !future.cancel(true))) {
        runnable.onError();
        return;
      }
    }
    asyncThreads.put(id, Pair.of(executor.submit(runnable), System.currentTimeMillis()));
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
      Timer timer = timerService.createCalendarTimer(new ScheduleExpression().hour(hours),
          new TimerConfig(parameter, false));

      logger.info("Created", NameUtils.getClassName(handler), parameter, "timer on hours [", hours,
          "] starting at", timer.getNextTimeout());
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
      Timer timer = timerService.createIntervalTimer(minutes * TimeUtils.MILLIS_PER_MINUTE,
          minutes * TimeUtils.MILLIS_PER_MINUTE, new TimerConfig(parameter, false));

      logger.info("Created", NameUtils.getClassName(handler), parameter, "timer every", minutes,
          "minutes starting at", timer.getNextTimeout());
    }
  }

  public boolean isParameterTimer(Timer timer, Object parameter) {
    if (!Config.isInitialized()) {
      return false;
    }
    Assert.noNulls(timer, parameter);
    return Objects.equals(timer.getInfo(), parameter);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void synchronizedCall(Runnable runnable) {
    Assert.notNull(runnable);
    runnable.run();
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
